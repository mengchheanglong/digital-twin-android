package com.transcendiverse.digitaltwin.launcher

import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import com.transcendiverse.digitaltwin.MainActivity
import kotlinx.coroutines.launch
import com.transcendiverse.digitaltwin.data.SharedPreferencesTodayCacheStore
import com.transcendiverse.digitaltwin.sync.TodaySyncScheduler
import com.transcendiverse.digitaltwin.widget.TodayWidgetUpdater

class LauncherActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val appContext = applicationContext
        val cacheStore = SharedPreferencesTodayCacheStore(this)
        val appsRepository = LauncherAppsRepository(
            packageManager = packageManager,
            selfPackageName = packageName,
        )

        fun loadSummary(): LauncherTodaySummary = LauncherTodaySummary.from(
            cachedToday = cacheStore.load(),
            nowEpochMillis = System.currentTimeMillis(),
        )

        setContent {
            val coroutineScope = rememberCoroutineScope()
            var todaySummary by remember { mutableStateOf(loadSummary()) }
            var apps by remember { mutableStateOf(appsRepository.listLaunchableApps()) }
            var isAppDrawerOpen by remember { mutableStateOf(false) }

            LauncherScreen(
                todaySummary = todaySummary,
                apps = apps,
                isAppDrawerOpen = isAppDrawerOpen,
                onRefresh = {
                    TodaySyncScheduler.enqueueOneTime(appContext)
                    todaySummary = loadSummary()
                    coroutineScope.launch {
                        TodayWidgetUpdater.update(appContext)
                        todaySummary = loadSummary()
                    }
                },
                onOpenAppDrawer = {
                    apps = appsRepository.listLaunchableApps()
                    isAppDrawerOpen = true
                },
                onCloseAppDrawer = {
                    isAppDrawerOpen = false
                },
                onLaunchApp = { app ->
                    appsRepository.buildLaunchIntent(app)?.let { launchIntent ->
                        startActivity(launchIntent)
                        isAppDrawerOpen = false
                    }
                },
                onOpenSettings = {
                    startActivity(Intent(Settings.ACTION_SETTINGS))
                },
                onOpenCompanion = {
                    startActivity(Intent(this, MainActivity::class.java))
                },
            )
        }
    }
}
