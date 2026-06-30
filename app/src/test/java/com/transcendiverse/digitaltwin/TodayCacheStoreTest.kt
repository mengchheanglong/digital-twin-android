package com.transcendiverse.digitaltwin

import com.transcendiverse.digitaltwin.data.CachedToday
import com.transcendiverse.digitaltwin.data.InMemoryTodayCacheStore
import com.transcendiverse.digitaltwin.data.TodayFixture
import com.transcendiverse.digitaltwin.model.MobileTodayResponse
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class TodayCacheStoreTest {
    private val json = Json {
        ignoreUnknownKeys = true
        explicitNulls = false
    }
    private val today = json.decodeFromString<MobileTodayResponse>(TodayFixture.json).today

    @Test
    fun roundTripsTodayAndTimestampMetadata() {
        val store = InMemoryTodayCacheStore()

        store.save(today, cachedAtEpochMillis = 1_798_588_800_000)

        val cached = requireNotNull(store.load())
        assertEquals(today, cached.today)
        assertEquals(1_798_588_800_000, cached.cachedAtEpochMillis)
    }

    @Test
    fun invalidCachedJsonIsClearedGracefully() {
        val store = InMemoryTodayCacheStore(
            rawTodayJson = """{"version":42""",
            cachedAtEpochMillis = 1_798_588_800_000,
        )

        assertNull(store.load())
        assertNull(store.load())
    }

    @Test
    fun cachedTodayContractDoesNotStoreCredentialsOrConnectionDetails() {
        val fieldNames = CachedToday::class.java.declaredFields.map { it.name.lowercase() }
        val forbidden = listOf("token", "password", "email", "baseurl", "backendurl", "journal", "chat")

        forbidden.forEach { value ->
            assertEquals(false, fieldNames.any { it.contains(value) })
        }
    }
}
