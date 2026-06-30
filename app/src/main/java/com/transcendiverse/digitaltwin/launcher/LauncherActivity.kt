package com.transcendiverse.digitaltwin.launcher

import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.transcendiverse.digitaltwin.MainActivity

class LauncherActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            LauncherScreen(
                onOpenAppDrawer = {},
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
