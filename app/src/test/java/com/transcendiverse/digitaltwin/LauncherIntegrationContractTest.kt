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
        assertTrue(activity.contains("LauncherFavoritesStore"))
        assertTrue(activity.contains("favoriteLauncherApps"))
        assertTrue(activity.contains("LauncherTodaySummary.from"))
        assertTrue(activity.contains("TodaySyncScheduler.enqueueOneTime"))
        assertTrue(screen.contains("LauncherDailySurface"))
        assertTrue(screen.contains("LauncherAppDrawer"))
        assertTrue(screen.contains("LauncherFavoritesCard"))
        assertTrue(screen.contains("favoritePackageNames"))
        assertTrue(screen.contains("onToggleFavorite"))
        assertTrue(!screen.contains("Daily surface placeholder"))
        assertTrue(!screen.contains("Installed apps will appear here after the app drawer task lands"))
    }

    @Test
    fun launcherUxStaysPrototypeOnlyWithVisibleEscapeControls() {
        val screen = source("app/src/main/java/com/transcendiverse/digitaltwin/launcher/LauncherScreen.kt")
        val activity = source("app/src/main/java/com/transcendiverse/digitaltwin/launcher/LauncherActivity.kt")
        val drawer = source("app/src/main/java/com/transcendiverse/digitaltwin/launcher/LauncherAppDrawer.kt")

        assertTrue(screen.contains("Prototype home shell"))
        assertTrue(screen.contains("LauncherSafetyActions.OPEN_SETTINGS_LABEL"))
        assertTrue(screen.contains("LauncherSafetyActions.OPEN_COMPANION_LABEL"))
        assertTrue(screen.contains("Choose favorites"))
        assertTrue(drawer.contains("Search apps"))
        assertTrue(!screen.contains("verticalScroll"))
        assertTrue(!screen.contains("rememberScrollState"))
        assertTrue(!activity.contains("ACTION_MANAGE_DEFAULT_APPS_SETTINGS"))
        assertTrue(!activity.contains("ROLE_HOME"))
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
