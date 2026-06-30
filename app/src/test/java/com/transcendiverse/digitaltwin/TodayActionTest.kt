package com.transcendiverse.digitaltwin

import com.transcendiverse.digitaltwin.data.TodayFixture
import com.transcendiverse.digitaltwin.model.MobileCheckIn
import com.transcendiverse.digitaltwin.model.MobileTodayResponse
import com.transcendiverse.digitaltwin.model.TodayAction
import com.transcendiverse.digitaltwin.model.recommendedTodayAction
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Test

class TodayActionTest {
    private val json = Json { ignoreUnknownKeys = true }
    private val baseToday = json.decodeFromString<MobileTodayResponse>(TodayFixture.json).today

    @Test
    fun checkInHasPriorityWhenNotCompleted() {
        val today = baseToday.copy(checkIn = baseToday.checkIn.copy(completedToday = false))

        assertEquals(TodayAction.CHECK_IN, recommendedTodayAction(today))
        assertEquals("Check in", recommendedTodayAction(today).label)
    }

    @Test
    fun continueQuestWhenCheckInIsDoneAndQuestExists() {
        val today = baseToday.copy(checkIn = baseToday.checkIn.copy(completedToday = true))

        assertEquals(TodayAction.CONTINUE_QUEST, recommendedTodayAction(today))
        assertEquals("Continue quest", recommendedTodayAction(today).label)
    }

    @Test
    fun reflectWhenCheckInIsDoneAndNoQuestExists() {
        val today = baseToday.copy(
            checkIn = MobileCheckIn(completedToday = true, score = 85),
            quest = baseToday.quest.copy(current = null),
        )

        assertEquals(TodayAction.REFLECT, recommendedTodayAction(today))
        assertEquals("Reflect", recommendedTodayAction(today).label)
    }
}
