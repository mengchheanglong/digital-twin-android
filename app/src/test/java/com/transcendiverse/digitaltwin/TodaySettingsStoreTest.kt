package com.transcendiverse.digitaltwin

import com.transcendiverse.digitaltwin.data.InMemoryTodaySettingsStore
import com.transcendiverse.digitaltwin.data.TodaySettings
import org.junit.Assert.assertEquals
import org.junit.Test

class TodaySettingsStoreTest {
    @Test
    fun savesAndLoadsBackendBaseUrlAndToken() {
        val store = InMemoryTodaySettingsStore()

        store.save(TodaySettings(baseUrl = "https://api.example.test", token = "test-token"))

        val loaded = store.load()
        assertEquals("https://api.example.test", loaded.baseUrl)
        assertEquals("test-token", loaded.token)
    }
}
