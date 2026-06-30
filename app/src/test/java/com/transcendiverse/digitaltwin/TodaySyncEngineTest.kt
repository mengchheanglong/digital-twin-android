package com.transcendiverse.digitaltwin

import com.transcendiverse.digitaltwin.data.InMemoryTodayCacheStore
import com.transcendiverse.digitaltwin.data.InMemoryTodaySettingsStore
import com.transcendiverse.digitaltwin.data.TodayFixture
import com.transcendiverse.digitaltwin.data.TodayRepository
import com.transcendiverse.digitaltwin.data.TodaySettings
import com.transcendiverse.digitaltwin.model.MobileToday
import com.transcendiverse.digitaltwin.model.MobileTodayResponse
import com.transcendiverse.digitaltwin.sync.TodaySyncEngine
import com.transcendiverse.digitaltwin.sync.TodaySyncStatus
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class TodaySyncEngineTest {
    private val json = Json {
        ignoreUnknownKeys = true
        explicitNulls = false
    }
    private val today = json.decodeFromString<MobileTodayResponse>(TodayFixture.json).today

    @Test
    fun missingCredentialsSkipsWithoutNetworkCall() {
        var repositoryCreated = false
        val engine = TodaySyncEngine(
            settingsStore = InMemoryTodaySettingsStore(),
            cacheStore = InMemoryTodayCacheStore(),
            repositoryFactory = { _, _ ->
                repositoryCreated = true
                StaticTodayRepository(today)
            },
            clock = { 123L },
        )

        val result = runBlocking { engine.sync() }

        assertEquals(TodaySyncStatus.SKIPPED_NOT_AUTHENTICATED, result.status)
        assertFalse(repositoryCreated)
    }

    @Test
    fun credentialsFetchAndCacheToday() {
        val cacheStore = InMemoryTodayCacheStore()
        val engine = TodaySyncEngine(
            settingsStore = InMemoryTodaySettingsStore(
                TodaySettings(baseUrl = "https://api.example.test", token = "secret-token"),
            ),
            cacheStore = cacheStore,
            repositoryFactory = { _, _ -> StaticTodayRepository(today) },
            clock = { 1_798_588_800_000 },
        )

        val result = runBlocking { engine.sync() }

        val cached = requireNotNull(cacheStore.load())
        assertEquals(TodaySyncStatus.SUCCESS, result.status)
        assertEquals(today, cached.today)
        assertEquals(1_798_588_800_000, cached.cachedAtEpochMillis)
    }

    @Test
    fun successfulSyncInvokesWidgetUpdateAfterCacheSave() {
        val cacheStore = InMemoryTodayCacheStore()
        val observed = mutableListOf<MobileToday?>()
        val engine = TodaySyncEngine(
            settingsStore = InMemoryTodaySettingsStore(
                TodaySettings(baseUrl = "https://api.example.test", token = "secret-token"),
            ),
            cacheStore = cacheStore,
            repositoryFactory = { _, _ -> StaticTodayRepository(today) },
            clock = { 1_798_588_800_000 },
            requestWidgetUpdate = { observed += cacheStore.load()?.today },
        )

        runBlocking { engine.sync() }

        assertEquals(listOf(today), observed)
    }

    @Test
    fun failureKeepsExistingCache() {
        val cacheStore = InMemoryTodayCacheStore()
        cacheStore.save(today, cachedAtEpochMillis = 99L)
        val engine = TodaySyncEngine(
            settingsStore = InMemoryTodaySettingsStore(
                TodaySettings(baseUrl = "https://api.example.test", token = "secret-token"),
            ),
            cacheStore = cacheStore,
            repositoryFactory = { _, _ -> FailingTodayRepository("backend down secret-token") },
            clock = { 1_798_588_800_000 },
        )

        val result = runBlocking { engine.sync() }

        val cached = requireNotNull(cacheStore.load())
        assertEquals(TodaySyncStatus.FAILURE, result.status)
        assertEquals(today, cached.today)
        assertEquals(99L, cached.cachedAtEpochMillis)
    }

    @Test
    fun syncResultTextDoesNotExposeSecrets() {
        val settings = TodaySettings(
            baseUrl = "https://api.example.test",
            token = "secret-token",
            lastUserEmail = "alex@example.com",
        )
        val cases = listOf(
            TodaySyncEngine(
                settingsStore = InMemoryTodaySettingsStore(),
                cacheStore = InMemoryTodayCacheStore(),
                repositoryFactory = { _, _ -> StaticTodayRepository(today) },
            ),
            TodaySyncEngine(
                settingsStore = InMemoryTodaySettingsStore(settings),
                cacheStore = InMemoryTodayCacheStore(),
                repositoryFactory = { _, _ -> StaticTodayRepository(today) },
            ),
            TodaySyncEngine(
                settingsStore = InMemoryTodaySettingsStore(settings),
                cacheStore = InMemoryTodayCacheStore(),
                repositoryFactory = { _, _ ->
                    FailingTodayRepository("https://api.example.test secret-token alex@example.com password")
                },
            ),
        )

        cases.map { engine -> runBlocking { engine.sync() } }.forEach { result ->
            val rendered = result.toString().lowercase()
            listOf(
                "secret-token",
                "password",
                "alex@example.com",
                "api.example.test",
                "backend_base_url",
                "jwt_token",
            ).forEach { forbidden ->
                assertFalse("Sync result must not contain $forbidden", rendered.contains(forbidden))
            }
        }
    }

    private class StaticTodayRepository(
        private val today: MobileToday,
    ) : TodayRepository {
        override suspend fun getToday(): MobileToday = today
    }

    private class FailingTodayRepository(
        private val message: String,
    ) : TodayRepository {
        override suspend fun getToday(): MobileToday {
            throw IllegalStateException(message)
        }
    }
}
