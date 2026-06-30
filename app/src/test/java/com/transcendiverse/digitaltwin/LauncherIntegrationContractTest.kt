package com.transcendiverse.digitaltwin

import java.io.File
import org.junit.Assert.assertTrue
import org.junit.Test

class LauncherIntegrationContractTest {
    @Test
    fun launcherShellUsesMergedDailySurfaceAndAppDrawer() {
        val activity = source("app/src/main/java/com/transcendiverse/digitaltwin/launcher/LauncherActivity.kt")
        val screen = source("app/src/main/java/com/transcendiverse/digitaltwin/launcher/LauncherScreen.kt")

        assertTrue(activity.contains("SharedPreferencesTodayCacheStore"))
        assertTrue(activity.contains("LauncherAppsRepository"))
        assertTrue(activity.contains("LauncherTodaySummary.from"))
        assertTrue(activity.contains("TodaySyncScheduler.enqueueOneTime"))
        assertTrue(screen.contains("LauncherDailySurface"))
        assertTrue(screen.contains("LauncherAppDrawer"))
        assertTrue(!screen.contains("Daily surface placeholder"))
        assertTrue(!screen.contains("Installed apps will appear here after the app drawer task lands"))
    }

    @Test
    fun manifestDeclaresLauncherAppVisibilityQuery() {
        val manifest = source("app/src/main/AndroidManifest.xml")

        assertTrue(manifest.contains("<queries>"))
        assertTrue(manifest.contains("android.intent.category.LAUNCHER"))
    }

    private fun source(path: String): String {
        val candidates = listOf(
            File(path),
            File(path.removePrefix("app/")),
        )
        return candidates.firstOrNull(File::isFile)?.readText()
            ?: error("Unable to read $path")
    }
}
