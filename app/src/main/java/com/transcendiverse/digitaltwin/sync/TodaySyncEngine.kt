package com.transcendiverse.digitaltwin.sync

import com.transcendiverse.digitaltwin.data.TodayCacheStore
import com.transcendiverse.digitaltwin.data.TodayRepository
import com.transcendiverse.digitaltwin.data.TodaySettingsStore

enum class TodaySyncStatus {
    SKIPPED_NOT_AUTHENTICATED,
    SUCCESS,
    FAILURE,
}

data class TodaySyncResult(
    val status: TodaySyncStatus,
    val message: String,
    val cachedAtEpochMillis: Long? = null,
)

class TodaySyncEngine(
    private val settingsStore: TodaySettingsStore,
    private val cacheStore: TodayCacheStore,
    private val repositoryFactory: (baseUrl: String, token: String) -> TodayRepository,
    private val clock: () -> Long = { System.currentTimeMillis() },
    private val requestWidgetUpdate: suspend () -> Unit = {},
) {
    suspend fun sync(): TodaySyncResult {
        val settings = settingsStore.load()
        if (!settings.hasCredentials()) {
            return TodaySyncResult(
                status = TodaySyncStatus.SKIPPED_NOT_AUTHENTICATED,
                message = "Sync skipped: sign in required",
            )
        }

        return try {
            val today = repositoryFactory(settings.baseUrl, settings.token).getToday()
            val cachedAt = clock()
            cacheStore.save(today, cachedAt)
            requestWidgetUpdate()
            TodaySyncResult(
                status = TodaySyncStatus.SUCCESS,
                message = "Today synced",
                cachedAtEpochMillis = cachedAt,
            )
        } catch (error: Exception) {
            TodaySyncResult(
                status = TodaySyncStatus.FAILURE,
                message = "Sync failed; cached Today kept",
                cachedAtEpochMillis = cacheStore.load()?.cachedAtEpochMillis,
            )
        }
    }
}
