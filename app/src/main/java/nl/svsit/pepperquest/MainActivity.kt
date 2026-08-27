package nl.svsit.pepperquest

import android.os.Bundle
import android.view.View
import android.view.WindowInsets
import android.view.WindowInsetsController
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import com.aldebaran.qi.sdk.QiContext
import com.aldebaran.qi.sdk.QiSDK
import com.aldebaran.qi.sdk.RobotLifecycleCallbacks
import com.aldebaran.qi.sdk.design.activity.RobotActivity

class MainActivity : RobotActivity(), RobotLifecycleCallbacks {
    private val engine = QuestEngine()
    private lateinit var pepper: PepperController

    private lateinit var statusText: TextView
    private lateinit var missionLabel: TextView
    private lateinit var headline: TextView
    private lateinit var description: TextView
    private lateinit var feedback: TextView
    private lateinit var primaryButton: Button
    private lateinit var resetButton: Button
    private lateinit var stopButton: Button
    private lateinit var touchPanel: View
    private lateinit var voicePanel: View
    private lateinit var resultPanel: View
    private lateinit var idlePanel: View
    private lateinit var touchHead: Button
    private lateinit var touchLeft: Button
    private lateinit var touchRight: Button
    private lateinit var resultTitle: TextView
    private lateinit var resultCopy: TextView
    private lateinit var progressOne: View
    private lateinit var progressTwo: View
    private lateinit var progressThree: View

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        hideSystemUi()
        bindViews()
        pepper = PepperController(::setRobotStatus)
        wireControls()
        render(engine.snapshot())
        QiSDK.register(this, this)
    }

    override fun onDestroy() {
        QiSDK.unregister(this, this)
        pepper.destroy()
        super.onDestroy()
    }

    override fun onRobotFocusGained(qiContext: QiContext) {
        pepper.attach(qiContext)
        pepper.bindTouch(::onPhysicalTouch)
    }

    override fun onRobotFocusLost() {
        pepper.detach()
    }

    override fun onRobotFocusRefused(reason: String) {
        pepper.detach()
        setRobotStatus("PREVIEW MODE // focus geweigerd // $reason")
    }

    private fun bindViews() {
        statusText = findViewById(R.id.robotStatus)
        missionLabel = findViewById(R.id.missionLabel)
        headline = findViewById(R.id.headline)
        description = findViewById(R.id.description)
        feedback = findViewById(R.id.feedback)
        primaryButton = findViewById(R.id.primaryButton)
        resetButton = findViewById(R.id.resetButton)
        stopButton = findViewById(R.id.stopButton)
        touchPanel = findViewById(R.id.touchPanel)
        voicePanel = findViewById(R.id.voicePanel)
        resultPanel = findViewById(R.id.resultPanel)
        idlePanel = findViewById(R.id.idlePanel)
        touchHead = findViewById(R.id.touchHead)
        touchLeft = findViewById(R.id.touchLeft)
        touchRight = findViewById(R.id.touchRight)
        resultTitle = findViewById(R.id.resultTitle)
        resultCopy = findViewById(R.id.resultCopy)
        progressOne = findViewById(R.id.progressOne)
        progressTwo = findViewById(R.id.progressTwo)
        progressThree = findViewById(R.id.progressThree)
    }

    private fun wireControls() {
        primaryButton.setOnClickListener { onStartQuest() }
        resetButton.setOnClickListener { onResetQuest() }
        stopButton.setOnClickListener { onEmergencyStop() }
        touchHead.setOnClickListener { onTouchInput(TouchZone.HEAD) }
        touchLeft.setOnClickListener { onTouchInput(TouchZone.LEFT_HAND) }
        touchRight.setOnClickListener { onTouchInput(TouchZone.RIGHT_HAND) }
        findViewById<Button>(R.id.classSoftware).setOnClickListener { onVoiceChoice(TechClass.SOFTWARE) }
        findViewById<Button>(R.id.classCyber).setOnClickListener { onVoiceChoice(TechClass.CYBER) }
        findViewById<Button>(R.id.classDesign).setOnClickListener { onVoiceChoice(TechClass.DESIGN) }
        findViewById<Button>(R.id.classGames).setOnClickListener { onVoiceChoice(TechClass.GAMES) }
    }

    private fun onStartQuest() {
        val effect = engine.start()
        if (effect != QuestEffect.Started) {
            feedback.text = "SAFETY STOP // druk eerst RESET"
            render(engine.snapshot())
            return
        }
        pepper.stopCurrentAction()
        feedback.text = "LINK OPEN // voer de touch-sequentie uit"
        render(engine.snapshot())
        if (pepper.isAttached()) {
            pepper.lookAtHuman {
                pepper.animate(R.raw.welcome) {
                    pepper.speak("Welkom bij S I T Quest. Student mode is vergrendeld. Raak eerst mijn hoofd aan.")
                }
            }
        }
    }

    private fun onPhysicalTouch(zone: TouchZone) {
        // Physical sensors are always bound for diagnostics, but only affect the engine
        // while its touch mission is active.
        if (engine.snapshot().state == QuestState.TOUCH_COMBO) onTouchInput(zone)
    }

    private fun onTouchInput(zone: TouchZone) {
        when (val effect = engine.touch(zone)) {
            is QuestEffect.TouchAccepted -> {
                feedback.text = "TOUCH OK // ${effect.completedSteps} van 3 // volgende doel gemarkeerd"
                flash(feedback)
            }
            QuestEffect.TouchComboCompleted -> {
                feedback.text = "COMBO COMPLETE // voice channel geopend"
                pepper.stopCurrentAction()
                if (pepper.isAttached()) {
                    pepper.animate(R.raw.hyped) {
                        pepper.speak("Sterk. Kies nu je tech class. Zeg software, cyber security, design of games.") {
                            pepper.listenForClass { choice ->
                                if (choice == null) {
                                    feedback.text = "VOICE TIMEOUT // kies hieronder op het tablet"
                                } else {
                                    onVoiceChoice(choice)
                                }
                            }
                        }
                    }
                } else {
                    feedback.text = "PREVIEW MODE // kies hieronder je tech class"
                }
            }
            is QuestEffect.IncorrectTouch -> {
                feedback.text = "MISS // ${zoneLabel(effect.expected)} was aan de beurt // voortgang bewaard"
                feedback.setTextColor(getColor(R.color.sit_red))
                feedback.postDelayed({ feedback.setTextColor(getColor(R.color.sit_green)) }, 500)
            }
            QuestEffect.Ignored -> feedback.text = "INPUT LOCKED // start of volg de actieve missie"
            else -> Unit
        }
        render(engine.snapshot())
    }

    private fun onVoiceChoice(choice: TechClass) {
        val effect = engine.chooseClass(choice)
        if (effect is QuestEffect.Completed) {
            pepper.stopCurrentAction()
            feedback.text = "CLASS MATCH // ${choice.label.uppercase()}"
            render(engine.snapshot())
            if (pepper.isAttached()) {
                pepper.speak("Match gevonden. Jij bent een ${choice.resultTitle.lowercase()}. Welkom bij Studievereniging I T.") {
                    // A short, crowded-space result gesture replaces the 20.8 second dance.
                    pepper.animate(R.raw.nod)
                }
            }
        }
    }

    private fun onResetQuest() {
        pepper.resetRound()
        engine.reset()
        feedback.text = "SYSTEM RESET // klaar voor de volgende speler"
        render(engine.snapshot())
    }

    private fun onEmergencyStop() {
        engine.emergencyStop()
        pepper.emergencyStop()
        feedback.text = "STOP ACTIVE // alle input vergrendeld // druk RESET"
        render(engine.snapshot())
        setRobotStatus("SAFETY STOP // druk RESET voor een nieuwe ronde")
    }

    private fun render(model: QuestRenderModel) {
        idlePanel.visibility = if (model.state == QuestState.IDLE) View.VISIBLE else View.GONE
        touchPanel.visibility = if (model.state == QuestState.TOUCH_COMBO) View.VISIBLE else View.GONE
        voicePanel.visibility = if (model.state == QuestState.VOICE_CLASS) View.VISIBLE else View.GONE
        resultPanel.visibility = if (model.state == QuestState.RESULT) View.VISIBLE else View.GONE
        resetButton.visibility = View.VISIBLE

        when (model.state) {
            QuestState.IDLE -> {
                missionLabel.text = "QUEST 00 // STANDBY"
                headline.text = "UNLOCK\nSTUDENT MODE"
                description.text = "Drie korte missies. Eén community. Start de link en ontdek jouw tech class."
                primaryButton.visibility = View.VISIBLE
            }
            QuestState.TOUCH_COMBO -> {
                missionLabel.text = "QUEST 01 // HUMAN INPUT"
                headline.text = "TOUCH\nTHE SYSTEM"
                description.text = "Gebruik Pepper of de tablet fallback. Volg: hoofd, linkerhand, rechterhand."
                primaryButton.visibility = View.GONE
            }
            QuestState.VOICE_CLASS -> {
                missionLabel.text = "QUEST 02 // VOICE CHANNEL"
                headline.text = "CHOOSE\nYOUR CLASS"
                description.text = "Zeg je keuze hardop. Niet verstaan? Elke class blijft ook als tabletknop beschikbaar."
                primaryButton.visibility = View.GONE
            }
            QuestState.RESULT -> {
                missionLabel.text = "QUEST 03 // ACCESS GRANTED"
                headline.text = "YOU ARE\nIN THE SYSTEM"
                description.text = "Jouw SIT-profiel is geladen. Geen account, geen opslag, alleen deze ronde."
                primaryButton.visibility = View.GONE
                model.selectedClass?.let {
                    resultTitle.text = it.resultTitle
                    resultCopy.text = it.resultCopy
                }
            }
            QuestState.SAFETY_STOP -> {
                missionLabel.text = "SAFETY // STOP ACTIVE"
                headline.text = "ROBOT\nLOCKED"
                description.text = "Alle invoer en robotacties zijn gestopt. Druk RESET om bewust een nieuwe ronde vrij te geven."
                primaryButton.visibility = View.GONE
            }
        }

        updateProgress(model)
        updateTouchPads(model.expectedTouch, model.comboIndex)
    }

    private fun updateProgress(model: QuestRenderModel) {
        val stage = when (model.state) {
            QuestState.IDLE -> 0
            QuestState.TOUCH_COMBO -> 1
            QuestState.VOICE_CLASS -> 2
            QuestState.RESULT -> 3
            QuestState.SAFETY_STOP -> 0
        }
        listOf(progressOne, progressTwo, progressThree).forEachIndexed { index, view ->
            view.setBackgroundResource(
                when {
                    index < stage -> R.drawable.progress_done
                    index == stage && stage < 3 -> R.drawable.progress_active
                    else -> R.drawable.progress_idle
                }
            )
        }
    }

    private fun updateTouchPads(expected: TouchZone?, completed: Int) {
        val pads = listOf(
            TouchZone.HEAD to touchHead,
            TouchZone.LEFT_HAND to touchLeft,
            TouchZone.RIGHT_HAND to touchRight
        )
        pads.forEachIndexed { index, (zone, button) ->
            button.setBackgroundResource(
                when {
                    index < completed -> R.drawable.touch_pad_done
                    zone == expected -> R.drawable.touch_pad_active
                    else -> R.drawable.touch_pad
                }
            )
        }
    }

    private fun zoneLabel(zone: TouchZone): String = when (zone) {
        TouchZone.HEAD -> "HOOFD"
        TouchZone.LEFT_HAND -> "LINKERHAND"
        TouchZone.RIGHT_HAND -> "RECHTERHAND"
    }

    private fun setRobotStatus(message: String) {
        if (android.os.Looper.myLooper() == android.os.Looper.getMainLooper()) {
            statusText.text = message
        } else {
            runOnUiThread { statusText.text = message }
        }
    }

    private fun flash(view: View) {
        view.animate().cancel()
        view.alpha = 0.45f
        view.animate().alpha(1f).setDuration(320).start()
    }

    private fun hideSystemUi() {
        if (android.os.Build.VERSION.SDK_INT >= 30) {
            window.insetsController?.apply {
                hide(WindowInsets.Type.statusBars() or WindowInsets.Type.navigationBars())
                systemBarsBehavior = WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            }
        } else {
            @Suppress("DEPRECATION")
            window.decorView.systemUiVisibility = (
                View.SYSTEM_UI_FLAG_FULLSCREEN or
                    View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or
                    View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                )
        }
    }
}
