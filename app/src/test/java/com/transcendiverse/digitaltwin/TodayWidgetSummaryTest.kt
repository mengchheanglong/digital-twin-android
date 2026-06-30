package com.transcendiverse.digitaltwin

import com.transcendiverse.digitaltwin.data.CachedToday
import com.transcendiverse.digitaltwin.data.TodayFixture
import com.transcendiverse.digitaltwin.model.MobileCheckIn
import com.transcendiverse.digitaltwin.model.MobileTodayResponse
import com.transcendiverse.digitaltwin.widget.TodayWidgetSummary
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class TodayWidgetSummaryTest {
    private val json = Json {
        ignoreUnknownKeys = true
        explicitNulls = false
    }
    private val today = json.decodeFromString<MobileTodayResponse>(TodayFixture.json).today

    @Test
    fun mapsTodayToSafeDisplayFields() {
        val summary = TodayWidgetSummary.from(
            CachedToday(today = today, cachedAtEpochMillis = 1_798_588_800_000),
        )

        assertEquals("Digital Twin", summary.title)
        assertEquals("🎯 focused", summary.mood)
        assertEquals("5 day streak", summary.streak)
        assertEquals("Ship the smallest useful companion", summary.quest)
        assertEquals("Check in", summary.nextAction)
        assertEquals("Cached", summary.cacheLabel)
        assertEquals("Tap to refresh", summary.refreshLabel)
    }

    @Test
    fun fallbackSummaryWhenNoCachedTodayExists() {
        val summary = TodayWidgetSummary.from(null)

        assertEquals("Digital Twin", summary.title)
        assertEquals("Open app to refresh Today", summary.emptyMessage)
        assertEquals("Tap to refresh", summary.refreshLabel)
    }

    @Test
    fun noQuestFallsBackToNoActiveQuest() {
        val summary = TodayWidgetSummary.from(
            CachedToday(
                today = today.copy(
                    checkIn = MobileCheckIn(completedToday = true, score = 85),
                    quest = today.quest.copy(current = null),
                ),
                cachedAtEpochMillis = 1_798_588_800_000,
            ),
        )

        assertEquals("No active quest", summary.quest)
        assertEquals("Check in", summary.nextAction)
    }

    @Test
    fun widgetSummaryDoesNotExposePrivateFields() {
        val summary = TodayWidgetSummary.from(
            CachedToday(today = today, cachedAtEpochMillis = 1_798_588_800_000),
        )
        val rendered = summary.toDisplayText().lowercase()
        val forbidden = listOf(
            "token",
            "password",
            "alex@example.com",
            "api.example.test",
            "mobile-today.v0",
            "journal",
            "chat",
            "reflection",
            "{",
            "}",
        )

        forbidden.forEach { value ->
            assertFalse("Widget summary must not contain $value", rendered.contains(value))
        }
    }
}
