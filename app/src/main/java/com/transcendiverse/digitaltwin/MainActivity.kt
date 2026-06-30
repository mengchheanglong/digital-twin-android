package com.transcendiverse.digitaltwin

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.transcendiverse.digitaltwin.data.SharedPreferencesTodayCacheStore
import com.transcendiverse.digitaltwin.data.SharedPreferencesTodaySettingsStore
import com.transcendiverse.digitaltwin.sync.TodaySyncScheduler
import com.transcendiverse.digitaltwin.ui.TodayScreen
import com.transcendiverse.digitaltwin.widget.TodayWidgetUpdater

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val settingsStore = SharedPreferencesTodaySettingsStore(this)
        val cacheStore = SharedPreferencesTodayCacheStore(this)
        val appContext = applicationContext
        TodaySyncScheduler.schedulePeriodic(appContext)
        TodaySyncScheduler.enqueueOneTimeIfAuthenticated(appContext)

        setContent {
            TodayScreen(
                settingsStore = settingsStore,
                cacheStore = cacheStore,
                onTodayCacheUpdated = { TodayWidgetUpdater.update(appContext) },
                onScheduleBackgroundSync = { TodaySyncScheduler.schedulePeriodic(appContext) },
                onEnqueueBackgroundSync = { TodaySyncScheduler.enqueueOneTime(appContext) },
            )
        }
    }
}
