package com.transcendiverse.digitaltwin

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.mutableStateOf
import com.transcendiverse.digitaltwin.data.SharedPreferencesTodayCacheStore
import com.transcendiverse.digitaltwin.data.SharedPreferencesTodaySettingsStore
import com.transcendiverse.digitaltwin.sync.TodaySyncScheduler
import com.transcendiverse.digitaltwin.ui.TodayScreen
import com.transcendiverse.digitaltwin.widget.TodayWidgetUpdater

class MainActivity : ComponentActivity() {
    private var launchRequestSequence = 0L
    private val launchRequestState = mutableStateOf(CompanionLaunchRequest())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        launchRequestState.value = intent.toCompanionLaunchRequest(launchRequestSequence)

        val settingsStore = SharedPreferencesTodaySettingsStore(this)
        val cacheStore = SharedPreferencesTodayCacheStore(this)
        val appContext = applicationContext
        if (settingsStore.load().hasCredentials()) {
            TodaySyncScheduler.schedulePeriodic(appContext)
            TodaySyncScheduler.enqueueOneTimeIfAuthenticated(appContext)
        }

        setContent {
            TodayScreen(
                settingsStore = settingsStore,
                cacheStore = cacheStore,
                initialLaunchRequest = launchRequestState.value,
                onTodayCacheUpdated = { TodayWidgetUpdater.update(appContext) },
                onScheduleBackgroundSync = { TodaySyncScheduler.schedulePeriodic(appContext) },
                onEnqueueBackgroundSync = { TodaySyncScheduler.enqueueOneTime(appContext) },
            )
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        launchRequestSequence += 1
        launchRequestState.value = intent.toCompanionLaunchRequest(launchRequestSequence)
    }
}
