package com.transcendiverse.digitaltwin.sync

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.transcendiverse.digitaltwin.data.NetworkTodayRepository
import com.transcendiverse.digitaltwin.data.SharedPreferencesTodayCacheStore
import com.transcendiverse.digitaltwin.data.SharedPreferencesTodaySettingsStore
import com.transcendiverse.digitaltwin.widget.TodayWidgetUpdater
import java.util.concurrent.TimeUnit

class TodaySyncWorker(
    appContext: Context,
    params: WorkerParameters,
) : CoroutineWorker(appContext, params) {
    override suspend fun doWork(): Result {
        val syncResult = TodaySyncEngine(
            settingsStore = SharedPreferencesTodaySettingsStore(applicationContext),
            cacheStore = SharedPreferencesTodayCacheStore(applicationContext),
            repositoryFactory = { baseUrl, token ->
                NetworkTodayRepository(baseUrl = baseUrl, token = token)
            },
            requestWidgetUpdate = { TodayWidgetUpdater.update(applicationContext) },
        ).sync()

        return when (syncResult.status) {
            TodaySyncStatus.SUCCESS,
            TodaySyncStatus.SKIPPED_NOT_AUTHENTICATED -> Result.success()
            TodaySyncStatus.FAILURE -> Result.retry()
        }
    }
}

object TodaySyncScheduler {
    fun enqueueOneTime(context: Context) {
        val request = OneTimeWorkRequestBuilder<TodaySyncWorker>()
            .addTag(WORK_TAG)
            .build()
        WorkManager.getInstance(context.applicationContext)
            .enqueueUniqueWork(ONE_TIME_WORK_NAME, ExistingWorkPolicy.REPLACE, request)
    }

    fun enqueueOneTimeIfAuthenticated(context: Context) {
        if (SharedPreferencesTodaySettingsStore(context.applicationContext).load().hasCredentials()) {
            enqueueOneTime(context)
        }
    }

    fun schedulePeriodic(context: Context) {
        val request = PeriodicWorkRequestBuilder<TodaySyncWorker>(15, TimeUnit.MINUTES)
            .addTag(WORK_TAG)
            .build()
        WorkManager.getInstance(context.applicationContext)
            .enqueueUniquePeriodicWork(PERIODIC_WORK_NAME, ExistingPeriodicWorkPolicy.KEEP, request)
    }

    private const val WORK_TAG = "today_sync"
    private const val ONE_TIME_WORK_NAME = "today_sync_one_time"
    private const val PERIODIC_WORK_NAME = "today_sync_periodic"
}
