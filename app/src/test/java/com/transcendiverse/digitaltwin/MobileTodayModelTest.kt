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
        val today = json.decodeFromString<MobileTodayResponse>(TodayFixture.json)

        assertEquals("mobile-today.v0", today.version)
        assertEquals("Alex", today.user.name)
        assertEquals("focused", today.user.mood)
        assertEquals("Check in", today.quest.nextAction.label)
        assertEquals("Open check-in", today.launcher.primaryLabel)

        val encoded = json.encodeToString(today)
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
