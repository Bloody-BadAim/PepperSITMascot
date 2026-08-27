package nl.svsit.pepperquest

import android.os.Handler
import android.os.Looper
import android.util.Log
import com.aldebaran.qi.Future
import com.aldebaran.qi.sdk.QiContext
import com.aldebaran.qi.sdk.builder.AnimateBuilder
import com.aldebaran.qi.sdk.builder.AnimationBuilder
import com.aldebaran.qi.sdk.builder.ListenBuilder
import com.aldebaran.qi.sdk.builder.LookAtBuilder
import com.aldebaran.qi.sdk.builder.PhraseSetBuilder
import com.aldebaran.qi.sdk.builder.SayBuilder
import com.aldebaran.qi.sdk.`object`.actuation.LookAtMovementPolicy
import com.aldebaran.qi.sdk.`object`.conversation.BodyLanguageOption
import com.aldebaran.qi.sdk.`object`.locale.Language
import com.aldebaran.qi.sdk.`object`.locale.Locale
import com.aldebaran.qi.sdk.`object`.locale.Region
import com.aldebaran.qi.sdk.`object`.touch.TouchSensor
import java.util.concurrent.CancellationException
import java.util.concurrent.ExecutionException
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledFuture
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicLong

/**
 * Small, focus-scoped QiSDK adapter.
 *
 * One worker serializes Say, Listen, LookAt and Animate. Every queued closure carries
 * both a focus generation and a round epoch. A reset or focus change therefore makes
 * old work inert. Every Qi Future is adopted by exact identity and is cancellable.
 */
class PepperController(
    private val onStatus: (String) -> Unit
) {
    private data class FocusScope(val generation: Long, val context: QiContext)
    private enum class ActionKind { SAY, LISTEN, LOOK_AT, ANIMATE }
    private data class OwnedAction(
        val scope: FocusScope,
        val epoch: Long,
        val kind: ActionKind,
        val future: Future<*>
    )
    private data class TouchBinding(
        val scope: FocusScope,
        val sensor: TouchSensor,
        val listener: TouchSensor.OnStateChangedListener
    )

    private val lock = Any()
    private val focusGeneration = AtomicLong(0)
    private val roundEpoch = AtomicLong(0)
    private val robotWorker = Executors.newSingleThreadExecutor()
    private val scheduler = Executors.newSingleThreadScheduledExecutor()
    private val cleanupWorker = Executors.newSingleThreadExecutor()
    private val mainHandler = Handler(Looper.getMainLooper())

    @Volatile private var activeScope: FocusScope? = null
    @Volatile private var destroyed = false
    @Volatile private var safetyStopped = false
    private var ownedAction: OwnedAction? = null
    private val touchBindings = mutableListOf<TouchBinding>()

    fun attach(context: QiContext) {
        if (destroyed) return
        val oldAction: OwnedAction?
        val oldBindings: List<TouchBinding>
        synchronized(lock) {
            activeScope = null
            roundEpoch.incrementAndGet()
            oldAction = ownedAction
            ownedAction = null
            oldBindings = touchBindings.toList()
            touchBindings.clear()
            activeScope = FocusScope(focusGeneration.incrementAndGet(), context)
        }
        cancel(oldAction)
        removeBindingsOffUi(oldBindings)
        status(
            if (safetyStopped) "SAFETY STOP // focus hersteld // druk RESET"
            else "ROBOT ONLINE // focus ${activeScope?.generation}"
        )
    }

    fun detach() {
        val action: OwnedAction?
        val bindings: List<TouchBinding>
        synchronized(lock) {
            focusGeneration.incrementAndGet()
            activeScope = null
            roundEpoch.incrementAndGet()
            action = ownedAction
            ownedAction = null
            bindings = touchBindings.toList()
            touchBindings.clear()
        }
        cancel(action)
        removeBindingsOffUi(bindings)
        status(
            if (safetyStopped) "SAFETY STOP // robotfocus weg // druk RESET"
            else "PREVIEW MODE // robotfocus niet beschikbaar"
        )
    }

    fun isAttached(): Boolean = activeScope != null && !destroyed

    /** Cancels Listen first and invalidates all queued work from the current round. */
    fun stopCurrentAction() {
        val action: OwnedAction?
        synchronized(lock) {
            roundEpoch.incrementAndGet()
            action = ownedAction
            ownedAction = null
        }
        cancel(action)
    }

    /** Latches a stop-all. No later QiSDK action is accepted until resetRound. */
    fun emergencyStop() {
        val action: OwnedAction?
        synchronized(lock) {
            safetyStopped = true
            roundEpoch.incrementAndGet()
            action = ownedAction
            ownedAction = null
        }
        cancel(action)
        status("SAFETY STOP // reset vereist")
    }

    /** Explicit operator action that releases the safety latch and clears old work. */
    fun resetRound() {
        val action: OwnedAction?
        synchronized(lock) {
            roundEpoch.incrementAndGet()
            action = ownedAction
            ownedAction = null
            safetyStopped = false
        }
        cancel(action)
        status(if (isAttached()) "ROBOT ONLINE // nieuwe ronde" else "PREVIEW MODE // nieuwe ronde")
    }

    fun speak(text: String, onComplete: (() -> Unit)? = null) {
        // Explicitly cancel an active Listen before Say. The single lane waits for its
        // cancellation to settle before the Say action can start.
        val listenToCancel = synchronized(lock) {
            ownedAction?.takeIf { it.kind == ActionKind.LISTEN }?.also {
                roundEpoch.incrementAndGet()
                ownedAction = null
            }
        }
        cancel(listenToCancel)
        submit(ActionKind.SAY) { scope, epoch ->
            val say = SayBuilder.with(scope.context)
                .withText(text)
                .withLocale(DUTCH_LOCALE)
                .withBodyLanguageOption(BodyLanguageOption.DISABLED)
                .build()
            val future = say.async().run()
            if (!adopt(scope, epoch, ActionKind.SAY, future)) return@submit
            try {
                future.get()
                deliver(scope, epoch, onComplete)
            } finally {
                release(future)
            }
        }
    }

    fun listenForClass(onChoice: (TechClass?) -> Unit) {
        submit(ActionKind.LISTEN) { scope, epoch ->
            val phraseSet = PhraseSetBuilder.with(scope.context)
                .withTexts(*VOICE_PHRASES)
                .build()
            val listen = ListenBuilder.with(scope.context)
                .withPhraseSet(phraseSet)
                .withLocale(DUTCH_LOCALE)
                .withBodyLanguageOption(BodyLanguageOption.DISABLED)
                .build()
            val future = listen.async().run()
            if (!adopt(scope, epoch, ActionKind.LISTEN, future)) return@submit
            val timedOut = AtomicBoolean(false)
            val delivered = AtomicBoolean(false)
            var timeout: ScheduledFuture<*>? = null
            try {
                timeout = scheduler.schedule({
                    if (isCurrent(scope, epoch)) {
                        timedOut.set(true)
                        future.requestCancellation()
                    }
                }, LISTEN_TIMEOUT_SECONDS, TimeUnit.SECONDS)
                val result = try {
                    future.get()
                } catch (error: Exception) {
                    if (timedOut.get()) null else throw error
                }
                val choice = result?.heardPhrase?.text?.let(::classFromPhrase)
                // Exactly one winner: a heard phrase or the timeout, never both.
                if (!timedOut.get() && delivered.compareAndSet(false, true)) {
                    deliver(scope, epoch) { onChoice(choice) }
                }
            } finally {
                timeout?.cancel(false)
                release(future)
                if (delivered.compareAndSet(false, true)) {
                    deliver(scope, epoch) { onChoice(null) }
                }
            }
        }
    }

    fun animate(resourceId: Int, onComplete: (() -> Unit)? = null) {
        submit(ActionKind.ANIMATE) { scope, epoch ->
            val animation = AnimationBuilder.with(scope.context).withResources(resourceId).build()
            val animate = AnimateBuilder.with(scope.context).withAnimation(animation).build()
            val future = animate.async().run()
            if (!adopt(scope, epoch, ActionKind.ANIMATE, future)) return@submit
            try {
                future.get()
                deliver(scope, epoch, onComplete)
            } finally {
                release(future)
            }
        }
    }

    /** Looks at an engaged or recommended human for at most four seconds, head only. */
    fun lookAtHuman(onComplete: (() -> Unit)? = null) {
        submit(ActionKind.LOOK_AT) { scope, epoch ->
            val awareness = scope.context.humanAwareness
            val human = awareness.engagedHuman
                ?: awareness.recommendedHumanToEngage
                ?: awareness.humansAround.firstOrNull()
            if (human == null) {
                status("GAZE // geen mens gedetecteerd, veilig overgeslagen")
                deliver(scope, epoch, onComplete)
                return@submit
            }
            val lookAt = LookAtBuilder.with(scope.context).withFrame(human.headFrame).build()
            lookAt.policy = LookAtMovementPolicy.HEAD_ONLY
            val future = lookAt.async().run()
            if (!adopt(scope, epoch, ActionKind.LOOK_AT, future)) return@submit
            var timeout: ScheduledFuture<*>? = null
            val timedOut = AtomicBoolean(false)
            try {
                timeout = scheduler.schedule({
                    if (isCurrent(scope, epoch)) {
                        timedOut.set(true)
                        future.requestCancellation()
                    }
                }, LOOK_AT_TIMEOUT_SECONDS, TimeUnit.SECONDS)
                // Do not let any later animation enter the serialized lane until the
                // HEAD_ONLY action has acknowledged cancellation.
                try {
                    future.get()
                } catch (error: Exception) {
                    if (!timedOut.get()) throw error
                }
            } finally {
                timeout?.cancel(false)
                future.requestCancellation()
                release(future)
                deliver(scope, epoch, onComplete)
            }
        }
    }

    /** Binds each physical sensor once for this focus generation. */
    fun bindTouch(onTouch: (TouchZone) -> Unit) {
        val scope = activeScope ?: return
        robotWorker.execute {
            if (!isScopeCurrent(scope)) return@execute
            val specs = listOf(
                Triple("Head/Touch", TouchZone.HEAD, "HEAD"),
                Triple("LHand/Touch", TouchZone.LEFT_HAND, "LEFT HAND"),
                Triple("RHand/Touch", TouchZone.RIGHT_HAND, "RIGHT HAND")
            )
            var count = 0
            for ((sensorName, zone, label) in specs) {
                if (!isScopeCurrent(scope)) return@execute
                try {
                    val sensor = scope.context.touch.getSensor(sensorName) ?: continue
                    var lastAcceptedAt = 0L
                    val listener = TouchSensor.OnStateChangedListener { state ->
                        val now = android.os.SystemClock.elapsedRealtime()
                        if (!state.touched || now - lastAcceptedAt < TOUCH_DEBOUNCE_MS) return@OnStateChangedListener
                        if (!isScopeCurrent(scope) || safetyStopped) return@OnStateChangedListener
                        lastAcceptedAt = now
                        mainHandler.post {
                            if (isScopeCurrent(scope) && !safetyStopped) {
                                status("SENSOR // $label touch")
                                onTouch(zone)
                            }
                        }
                    }
                    sensor.addOnStateChangedListener(listener)
                    val binding = TouchBinding(scope, sensor, listener)
                    val keep = synchronized(lock) {
                        if (isScopeCurrentLocked(scope) && touchBindings.none { it.scope === scope && it.sensor === sensor }) {
                            touchBindings.add(binding)
                            true
                        } else false
                    }
                    if (keep) count++ else sensor.removeOnStateChangedListener(listener)
                } catch (error: Exception) {
                    Log.w(TAG, "Sensor $sensorName niet beschikbaar", error)
                }
            }
            status("SENSORS // $count van 3 gekoppeld // tablet fallback actief")
        }
    }

    fun destroy() {
        if (destroyed) return
        destroyed = true
        detach()
        robotWorker.shutdownNow()
        scheduler.shutdownNow()
        cleanupWorker.shutdown()
        mainHandler.removeCallbacksAndMessages(null)
    }

    private fun submit(kind: ActionKind, block: (FocusScope, Long) -> Unit) {
        val scope = activeScope
        val epoch = roundEpoch.get()
        if (safetyStopped) {
            status("SAFETY STOP // druk RESET voor robotacties")
            return
        }
        if (scope == null || destroyed) {
            status("PREVIEW MODE // robotactie overgeslagen")
            return
        }
        robotWorker.execute {
            if (!isCurrent(scope, epoch)) return@execute
            try {
                block(scope, epoch)
            } catch (error: Exception) {
                if (isCurrent(scope, epoch)) {
                    Log.w(TAG, "$kind mislukt", error)
                    status("ROBOT WARNING // ${kind.name.lowercase()} niet beschikbaar")
                }
            }
        }
    }

    private fun adopt(scope: FocusScope, epoch: Long, kind: ActionKind, future: Future<*>): Boolean {
        val accepted = synchronized(lock) {
            if (isCurrentLocked(scope, epoch) && ownedAction == null) {
                ownedAction = OwnedAction(scope, epoch, kind, future)
                true
            } else false
        }
        if (!accepted) cancelAndSettle(future)
        return accepted
    }

    /** Runs only on robotWorker and forms a bounded barrier before later work. */
    private fun cancelAndSettle(future: Future<*>) {
        try {
            future.requestCancellation()
            future.get(CANCEL_SETTLE_TIMEOUT_SECONDS, TimeUnit.SECONDS)
        } catch (_: CancellationException) {
            // Planned cancellation settled.
        } catch (_: ExecutionException) {
            // A rejected action may finish with an SDK error while cancellation wins.
        } catch (error: TimeoutException) {
            Log.w(TAG, "Geannuleerde actie settle niet binnen timeout", error)
        }
    }

    private fun release(future: Future<*>) {
        synchronized(lock) {
            if (ownedAction?.future === future) ownedAction = null
        }
    }

    private fun cancel(action: OwnedAction?) {
        try { action?.future?.requestCancellation() } catch (error: Exception) {
            Log.w(TAG, "Actie annuleren mislukt", error)
        }
    }

    private fun removeBindingsOffUi(bindings: List<TouchBinding>) {
        if (bindings.isEmpty()) return
        cleanupWorker.execute {
            bindings.forEach { binding ->
                try { binding.sensor.removeOnStateChangedListener(binding.listener) }
                catch (error: Exception) { Log.w(TAG, "Touchlistener verwijderen mislukt", error) }
            }
        }
    }

    private fun isCurrent(scope: FocusScope, epoch: Long): Boolean = synchronized(lock) {
        isCurrentLocked(scope, epoch)
    }

    private fun isCurrentLocked(scope: FocusScope, epoch: Long): Boolean =
        !destroyed && !safetyStopped && activeScope === scope && roundEpoch.get() == epoch

    private fun isScopeCurrent(scope: FocusScope): Boolean = synchronized(lock) {
        isScopeCurrentLocked(scope)
    }

    private fun isScopeCurrentLocked(scope: FocusScope): Boolean = !destroyed && activeScope === scope

    private fun deliver(scope: FocusScope, epoch: Long, callback: (() -> Unit)?) {
        if (callback == null) return
        mainHandler.post { if (isCurrent(scope, epoch)) callback() }
    }

    private fun status(message: String) {
        val generation = focusGeneration.get()
        mainHandler.post {
            if (!destroyed && focusGeneration.get() == generation) onStatus(message)
        }
    }

    private fun classFromPhrase(raw: String): TechClass? {
        val phrase = raw.lowercase()
        return when {
            "cyber" in phrase || "security" in phrase -> TechClass.CYBER
            "design" in phrase || "ux" in phrase -> TechClass.DESIGN
            "game" in phrase -> TechClass.GAMES
            "software" in phrase || "code" in phrase -> TechClass.SOFTWARE
            else -> null
        }
    }

    private companion object {
        const val TAG = "PepperQuest"
        const val TOUCH_DEBOUNCE_MS = 650L
        const val LISTEN_TIMEOUT_SECONDS = 9L
        const val LOOK_AT_TIMEOUT_SECONDS = 4L
        const val CANCEL_SETTLE_TIMEOUT_SECONDS = 2L
        val DUTCH_LOCALE = Locale(Language.DUTCH, Region.NETHERLANDS)
        val VOICE_PHRASES = arrayOf(
            "software", "cyber security", "design", "games",
            "software development", "ux design", "game development"
        )
    }
}
