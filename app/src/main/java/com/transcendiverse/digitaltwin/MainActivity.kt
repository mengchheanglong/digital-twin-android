package com.transcendiverse.digitaltwin

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.transcendiverse.digitaltwin.data.SharedPreferencesTodaySettingsStore
import com.transcendiverse.digitaltwin.ui.TodayScreen

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val settingsStore = SharedPreferencesTodaySettingsStore(this)

        setContent {
            TodayScreen(settingsStore = settingsStore)
        }
    }
}
