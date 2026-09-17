package nl.svsit.pepperquest

enum class QuestState { IDLE, TOUCH_COMBO, VOICE_CLASS, RESULT, SAFETY_STOP }
enum class TouchZone { HEAD, LEFT_HAND, RIGHT_HAND }
enum class TechClass(val label: String, val spokenName: String, val resultTitle: String, val resultCopy: String) {
    SOFTWARE("Software", "software", "CODE ARCHITECT", "Jij bouwt ideeen om tot systemen. Bij SIT vind je makers om mee te shippen."),
    CYBER("Cyber Security", "cyber security", "DIGITAL GUARDIAN", "Jij ziet risico's voor ze problemen worden. Bij SIT scherp je je skills aan."),
    DESIGN("UX & Design", "design", "EXPERIENCE ENGINEER", "Jij maakt techniek helder en menselijk. Bij SIT geef je ideeen samen vorm."),
    GAMES("Game Development", "games", "WORLD BUILDER", "Jij combineert code, verhaal en spel. Bij SIT vind je je volgende crew.")
}

data class QuestRenderModel(
    val state: QuestState,
    val comboIndex: Int,
    val expectedTouch: TouchZone?,
    val selectedClass: TechClass?
)

sealed class QuestEffect {
    data object Started : QuestEffect()
    data class TouchAccepted(val completedSteps: Int) : QuestEffect()
    data object TouchComboCompleted : QuestEffect()
    data class IncorrectTouch(val expected: TouchZone) : QuestEffect()
    data class Completed(val techClass: TechClass) : QuestEffect()
    data object EmergencyStopped : QuestEffect()
    data object Reset : QuestEffect()
    data object Ignored : QuestEffect()
}

/** Pure in-memory state machine. Android and QiSDK types do not enter this class. */
class QuestEngine {
    private val combo = listOf(TouchZone.HEAD, TouchZone.LEFT_HAND, TouchZone.RIGHT_HAND)
    private var state = QuestState.IDLE
    private var comboIndex = 0
    private var selectedClass: TechClass? = null

    @Synchronized
    fun start(): QuestEffect {
        if (state == QuestState.SAFETY_STOP) return QuestEffect.Ignored
        state = QuestState.TOUCH_COMBO
        comboIndex = 0
        selectedClass = null
        return QuestEffect.Started
    }

    @Synchronized
    fun touch(zone: TouchZone): QuestEffect {
        if (state != QuestState.TOUCH_COMBO) return QuestEffect.Ignored
        val expected = combo[comboIndex]
        if (zone != expected) return QuestEffect.IncorrectTouch(expected)
        comboIndex += 1
        return if (comboIndex == combo.size) {
            state = QuestState.VOICE_CLASS
            QuestEffect.TouchComboCompleted
        } else {
            QuestEffect.TouchAccepted(comboIndex)
        }
    }

    @Synchronized
    fun chooseClass(choice: TechClass): QuestEffect {
        if (state != QuestState.VOICE_CLASS) return QuestEffect.Ignored
        selectedClass = choice
        state = QuestState.RESULT
        return QuestEffect.Completed(choice)
    }

    @Synchronized
    fun emergencyStop(): QuestEffect {
        state = QuestState.SAFETY_STOP
        return QuestEffect.EmergencyStopped
    }

    @Synchronized
    fun reset(): QuestEffect {
        state = QuestState.IDLE
        comboIndex = 0
        selectedClass = null
        return QuestEffect.Reset
    }

    @Synchronized
    fun snapshot(): QuestRenderModel = QuestRenderModel(
        state = state,
        comboIndex = comboIndex,
        expectedTouch = if (state == QuestState.TOUCH_COMBO) combo[comboIndex] else null,
        selectedClass = selectedClass
    )
}
