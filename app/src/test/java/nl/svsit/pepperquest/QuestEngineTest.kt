package nl.svsit.pepperquest

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class QuestEngineTest {

    @Test
    fun startOpensTouchMissionAtHeadStep() {
        val engine = QuestEngine()

        val effect = engine.start()

        assertEquals(QuestEffect.Started, effect)
        assertEquals(QuestState.TOUCH_COMBO, engine.snapshot().state)
        assertEquals(0, engine.snapshot().comboIndex)
        assertEquals(TouchZone.HEAD, engine.snapshot().expectedTouch)
    }

    @Test
    fun correctComboAdvancesFromHeadToLeftHandToRightHand() {
        val engine = QuestEngine()
        engine.start()

        assertEquals(QuestEffect.TouchAccepted(1), engine.touch(TouchZone.HEAD))
        assertEquals(TouchZone.LEFT_HAND, engine.snapshot().expectedTouch)
        assertEquals(QuestEffect.TouchAccepted(2), engine.touch(TouchZone.LEFT_HAND))
        assertEquals(TouchZone.RIGHT_HAND, engine.snapshot().expectedTouch)
        assertEquals(QuestEffect.TouchComboCompleted, engine.touch(TouchZone.RIGHT_HAND))
        assertEquals(QuestState.VOICE_CLASS, engine.snapshot().state)
    }

    @Test
    fun wrongTouchReportsExpectedZoneWithoutErasingProgress() {
        val engine = QuestEngine()
        engine.start()
        engine.touch(TouchZone.HEAD)

        val effect = engine.touch(TouchZone.RIGHT_HAND)

        assertEquals(QuestEffect.IncorrectTouch(TouchZone.LEFT_HAND), effect)
        assertEquals(1, engine.snapshot().comboIndex)
        assertEquals(TouchZone.LEFT_HAND, engine.snapshot().expectedTouch)
    }

    @Test
    fun voiceChoiceSelectsTheRequestedTechClass() {
        val engine = engineAtVoiceMission()

        engine.chooseClass(TechClass.DESIGN)

        assertEquals(TechClass.DESIGN, engine.snapshot().selectedClass)
    }

    @Test
    fun resetReturnsToIdleAndClearsQuestProgress() {
        val engine = engineAtVoiceMission()
        engine.chooseClass(TechClass.CYBER)

        val effect = engine.reset()

        assertEquals(QuestEffect.Reset, effect)
        assertEquals(QuestState.IDLE, engine.snapshot().state)
        assertEquals(0, engine.snapshot().comboIndex)
        assertNull(engine.snapshot().selectedClass)
    }

    @Test
    fun finishReturnsResultForTheChosenClass() {
        val engine = engineAtVoiceMission()

        val effect = engine.chooseClass(TechClass.GAMES)

        assertEquals(QuestEffect.Completed(TechClass.GAMES), effect)
        assertEquals(QuestState.RESULT, engine.snapshot().state)
        assertEquals(TechClass.GAMES, engine.snapshot().selectedClass)
    }

    @Test
    fun emergencyStopLocksEveryInputUntilExplicitReset() {
        val engine = QuestEngine()
        engine.start()

        assertEquals(QuestEffect.EmergencyStopped, engine.emergencyStop())
        assertEquals(QuestState.SAFETY_STOP, engine.snapshot().state)
        assertEquals(QuestEffect.Ignored, engine.start())
        assertEquals(QuestEffect.Ignored, engine.touch(TouchZone.HEAD))
        assertEquals(QuestEffect.Ignored, engine.chooseClass(TechClass.SOFTWARE))
        assertEquals(QuestState.SAFETY_STOP, engine.snapshot().state)
    }

    @Test
    fun resetReleasesSafetyStopButDoesNotStartRobotWork() {
        val engine = QuestEngine()
        engine.emergencyStop()

        assertEquals(QuestEffect.Reset, engine.reset())
        assertEquals(QuestState.IDLE, engine.snapshot().state)
        assertEquals(QuestEffect.Started, engine.start())
    }

    private fun engineAtVoiceMission(): QuestEngine {
        return QuestEngine().apply {
            start()
            touch(TouchZone.HEAD)
            touch(TouchZone.LEFT_HAND)
            touch(TouchZone.RIGHT_HAND)
        }
    }
}
