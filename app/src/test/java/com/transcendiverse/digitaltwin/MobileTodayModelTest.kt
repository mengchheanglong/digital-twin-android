package com.transcendiverse.digitaltwin

import com.transcendiverse.digitaltwin.data.TodayFixture
import com.transcendiverse.digitaltwin.model.MobileTodayResponse
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MobileTodayModelTest {
    private val json = Json {
        ignoreUnknownKeys = true
        explicitNulls = false
    }

    @Test
    fun fixtureDeserializesAndSerializes() {
        val response = json.decodeFromString<MobileTodayResponse>(TodayFixture.json)
        val today = response.today

        assertTrue(response.success)
        assertEquals("mobile-today.v0", today.version)
        assertEquals("Alex", today.user.name)
        assertEquals("focused", today.user.mood.label)
        assertEquals(8, today.checkIn.dimensions?.focus)
        assertEquals("Ship the smallest useful companion", today.quest.current?.goal)
        assertEquals("daily", today.quest.current?.duration)
        assertEquals("Check in", today.quest.nextAction.label)
        assertEquals("/dashboard/checkin", today.quest.nextAction.href)
        assertEquals("Open check-in", today.launcher.primaryLabel)

        val encoded = json.encodeToString(response)
        assertTrue(encoded.contains("\"success\":true"))
        assertTrue(encoded.contains("\"version\":\"mobile-today.v0\""))
        assertTrue(encoded.contains("\"primaryHref\":\"/dashboard/checkin\""))
    }

    @Test
    fun fixtureDoesNotIncludeRawPrivateFields() {
        val forbidden = listOf(
            "email",
            "journal",
            "chatMessages",
            "password",
            "literal token value",
            "token",
            "deepseek",
            "mongodb",
            "hermes",
        )
        val lowerFixture = TodayFixture.json.lowercase()

        forbidden.forEach { field ->
            assertFalse("Fixture must not contain $field", lowerFixture.contains(field.lowercase()))
        }
    }

    @Test
    fun liveBackendShapeAllowsDecimalProductivityScore() {
        val payload =
            """
            {
              "success": true,
              "today": {
                "version": "mobile-today.v0",
                "generatedAt": "2026-07-01T02:16:25.460Z",
                "dayKey": "2026-07-01",
                "user": {
                  "name": "Kaze",
                  "level": 3,
                  "currentXP": 131,
                  "requiredXP": 150,
                  "streak": 3,
                  "mood": { "emoji": "🤩", "label": "Excellent" }
                },
                "checkIn": {
                  "completedToday": true,
                  "score": 20,
                  "dimensions": {
                    "energy": 4,
                    "focus": 4,
                    "stressControl": 4,
                    "socialConnection": 4,
                    "optimism": 4
                  }
                },
                "quest": {
                  "current": null,
                  "nextAction": {
                    "label": "Reflect",
                    "href": "/dashboard/chat",
                    "reason": "Reflect on today and choose your next focused action."
                  }
                },
                "insight": {
                  "trend": "stable",
                  "topInterest": "Daily",
                  "productivityScore": 67.9,
                  "entertainmentRatio": 0,
                  "reflection": "No reflection yet. Reflect this evening."
                },
                "launcher": {
                  "primaryLabel": "Reflect",
                  "primaryHref": "/dashboard/chat",
                  "secondaryLabel": "Create quest",
                  "secondaryHref": "/dashboard/quest"
                }
              }
            }
            """.trimIndent()

        val response = json.decodeFromString<MobileTodayResponse>(payload)

        assertTrue(response.success)
        assertEquals("Kaze", response.today.user.name)
        assertEquals(67.9, response.today.insight.productivityScore, 0.0)
        assertEquals("Reflect", response.today.launcher.primaryLabel)
    }
}
