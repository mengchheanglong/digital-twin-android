package com.transcendiverse.digitaltwin

import com.transcendiverse.digitaltwin.data.CachedToday
import com.transcendiverse.digitaltwin.data.TodayFixture
import com.transcendiverse.digitaltwin.launcher.LauncherTodaySummary
import com.transcendiverse.digitaltwin.model.MobileCheckIn
import com.transcendiverse.digitaltwin.model.MobileTodayResponse
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Test

class LauncherTodaySummaryTest {
    private val json = Json {
        ignoreUnknownKeys = true
        explicitNulls = false
    }
    private val today = json.decodeFromString<MobileTodayResponse>(TodayFixture.json).today

    @Test
    fun nullCacheReturnsSafeEmptyState() {
        val summary = LauncherTodaySummary.from(null)

        assertEquals("Digital Twin Today", summary.title)
        assertEquals("No cached Today", summary.cacheLabel)
        assertEquals("No Today snapshot yet. Open companion or refresh to load your latest state.", summary.emptyState)
        assertNotNull(summary.emptyState)
    }

    @Test
    fun cachedTodayShowsDailyOperatingFields() {
        val summary = LauncherTodaySummary.from(
            CachedToday(today = today, cachedAtEpochMillis = 1_798_588_800_000),
            nowEpochMillis = 1_798_592_400_000,
        )

        assertEquals("Digital Twin Today", summary.title)
        assertEquals("focused", summary.mood)
        assertEquals("5-day streak", summary.streak)
        assertEquals("Check-in pending", summary.checkInStatus)
        assertEquals("Ship the smallest useful companion", summary.currentQuest)
        assertEquals("Check in from companion", summary.nextAction)
        assertEquals("Cached 1 hr ago", summary.cacheLabel)
    }

    @Test
    fun incompleteCheckInEmphasizesCheckInAsNextAction() {
        val summary = LauncherTodaySummary.from(
            CachedToday(today = today, cachedAtEpochMillis = 1_798_588_800_000),
        )

        assertEquals("Check in from companion", summary.nextAction)
    }

    @Test
    fun completedCheckInCanUseSafeBackendNextActionLabel() {
        val summary = LauncherTodaySummary.from(
            CachedToday(
                today = today.copy(checkIn = MobileCheckIn(completedToday = true, score = 85)),
                cachedAtEpochMillis = 1_798_588_800_000,
            ),
        )

        assertEquals("Check-in complete", summary.checkInStatus)
        assertEquals("Check in", summary.nextAction)
    }

    @Test
    fun summaryStringsDoNotExposeObviousPrivateTerms() {
        val unsafeToday = today.copy(
            user = today.user.copy(mood = today.user.mood.copy(label = "token@example.com")),
            quest = today.quest.copy(
                current = today.quest.current?.copy(goal = """{"password":"secret"}"""),
                nextAction = today.quest.nextAction.copy(label = "Bearer https://example.test"),
            ),
            checkIn = today.checkIn.copy(completedToday = true),
        )
        val summary = LauncherTodaySummary.from(
            CachedToday(today = unsafeToday, cachedAtEpochMillis = 1_798_588_800_000),
        )
        val rendered = summary.toDisplayText().lowercase()
        val forbidden = listOf("token", "password", "bearer", "http://", "https://", "@", "{", "}")

        forbidden.forEach { value ->
            assertFalse("Launcher summary must not contain $value", rendered.contains(value))
        }
    }
}
