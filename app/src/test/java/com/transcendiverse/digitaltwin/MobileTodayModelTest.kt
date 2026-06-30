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
}
