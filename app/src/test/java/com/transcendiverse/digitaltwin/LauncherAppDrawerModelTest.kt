package com.transcendiverse.digitaltwin

import com.transcendiverse.digitaltwin.launcher.LauncherApp
import org.junit.Assert.assertEquals
import org.junit.Test

class LauncherAppDrawerModelTest {
    @Test
    fun launchModelPreservesPackageActivityLabels() {
        val app = LauncherApp(
            packageName = "com.example.notes",
            label = "Notes",
            activityName = "com.example.notes.MainActivity",
        )

        assertEquals("com.example.notes", app.packageName)
        assertEquals("Notes", app.label)
        assertEquals("com.example.notes.MainActivity", app.activityName)
    }
}
