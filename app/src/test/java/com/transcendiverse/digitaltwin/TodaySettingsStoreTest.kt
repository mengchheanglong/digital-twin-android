package com.transcendiverse.digitaltwin

import com.transcendiverse.digitaltwin.data.InMemoryTodaySettingsStore
import com.transcendiverse.digitaltwin.data.PRODUCTION_BACKEND_BASE_URL
import com.transcendiverse.digitaltwin.data.TodaySettings
import com.transcendiverse.digitaltwin.data.useProductionBackend
import org.junit.Assert.assertEquals
import org.junit.Test

class TodaySettingsStoreTest {
    @Test
    fun freshInMemorySettingsUseProductionBackendUrl() {
        val store = InMemoryTodaySettingsStore()

        assertEquals(PRODUCTION_BACKEND_BASE_URL, store.load().baseUrl)
    }

    @Test
    fun savesAndLoadsBackendBaseUrlAndToken() {
        val store = InMemoryTodaySettingsStore()

        store.save(TodaySettings(baseUrl = "https://api.example.test", token = "test-token"))

        val loaded = store.load()
        assertEquals("https://api.example.test", loaded.baseUrl)
        assertEquals("test-token", loaded.token)
    }

    @Test
    fun savedCustomBackendBaseUrlIsPreserved() {
        val store = InMemoryTodaySettingsStore()

        store.save(TodaySettings(baseUrl = "https://custom.example.test"))

        assertEquals("https://custom.example.test", store.load().baseUrl)
    }

    @Test
    fun useProductionBackendResetsOnlyBackendUrl() {
        val settings = TodaySettings(
            baseUrl = "https://custom.example.test",
            token = "saved-token",
            lastUserEmail = "saved@example.test",
            lastUserName = "Saved User",
        )

        val reset = settings.useProductionBackend()

        assertEquals(PRODUCTION_BACKEND_BASE_URL, reset.baseUrl)
        assertEquals("saved-token", reset.token)
        assertEquals("saved@example.test", reset.lastUserEmail)
        assertEquals("Saved User", reset.lastUserName)
    }

    @Test
    fun settingsDoNotHavePasswordStorage() {
        val fieldNames = TodaySettings::class.java.declaredFields.map { it.name }

        assertEquals(false, fieldNames.any { it.contains("password", ignoreCase = true) })
    }
}
