package com.transcendiverse.digitaltwin

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LauncherIntegrationContractTest {
    @Test
    fun launcherShellUsesMinimalistTextHomeAndDrawer() {
        val activity = source("app/src/main/java/com/transcendiverse/digitaltwin/launcher/LauncherActivity.kt")
        val screen = source("app/src/main/java/com/transcendiverse/digitaltwin/launcher/LauncherScreen.kt")
        val dailySurface = source("app/src/main/java/com/transcendiverse/digitaltwin/launcher/LauncherDailySurface.kt")

        assertTrue(activity.contains("SharedPreferencesTodayCacheStore"))
        assertTrue(activity.contains("LauncherAppsRepository"))
        assertTrue(activity.contains("LauncherFavoritesStore"))
        assertTrue(activity.contains("favoriteLauncherApps"))
        assertTrue(activity.contains("LauncherTodaySummary.from"))
        assertTrue(activity.contains("TodaySyncScheduler.enqueueOneTime"))

        assertTrue(screen.contains("Digital Twin"))
        assertTrue(screen.contains("LauncherAllowedAppsSection"))
        assertTrue(screen.contains("Allowed"))
        assertTrue(screen.contains("choose allowed apps"))
        assertTrue(screen.contains("all apps"))
        assertTrue(screen.contains("companion"))
        assertTrue(screen.contains("settings"))
        assertTrue(screen.contains("MAX_HOME_ALLOWED_APPS = 7"))
        assertTrue(screen.contains("LauncherMutedActionColor"))
        assertTrue(screen.contains("LauncherTextAction(text = \"all apps\""))
        assertTrue(screen.contains("LauncherTextAction(text = \"companion\""))
        assertTrue(screen.contains("LauncherTextAction(text = \"settings\""))
        assertTrue(screen.contains("LauncherTextAction(text = \"refresh\""))

        assertTrue(dailySurface.contains("SummaryLine(label = \"Mood\""))
        assertTrue(dailySurface.contains("SummaryLine(label = \"Quest\""))
        assertTrue(dailySurface.contains("SummaryLine(label = \"Check-in\""))
        assertTrue(dailySurface.contains("quietTodayValue"))
        assertFalse(dailySurface.contains("Card("))
        assertFalse(screen.contains("LauncherFavoritesCard"))
        assertFalse(screen.contains("FavoriteAppShortcut"))
        assertFalse(screen.contains("packageName,"))
        assertFalse(screen.contains("\u2605"))
        assertFalse(screen.contains("\u2606"))
    }

    @Test
    fun appDrawerStaysSearchableAndUsesAllowHideCopy() {
        val drawer = source("app/src/main/java/com/transcendiverse/digitaltwin/launcher/LauncherAppDrawer.kt")
        val repository = source("app/src/main/java/com/transcendiverse/digitaltwin/launcher/LauncherAppsRepository.kt")

        assertTrue(drawer.contains("OutlinedTextField"))
        assertTrue(drawer.contains("label = { Text(\"search\") }"))
        assertTrue(drawer.contains("searchLauncherApps(apps, query)"))
        assertTrue(repository.contains("app.label.contains(cleaned, ignoreCase = true)"))
        assertTrue(repository.contains("app.packageName.contains(cleaned, ignoreCase = true)"))
        assertTrue(drawer.contains("LauncherTextAction(text = \"close\""))
        assertTrue(drawer.contains("text = if (isAllowed) \"hide\" else \"allow\""))
        assertTrue(drawer.contains(".weight(1f)"))
        assertFalse(drawer.contains("TextButton("))
        assertFalse(drawer.contains("\u2605"))
        assertFalse(drawer.contains("\u2606"))
        assertFalse(drawer.contains("text = app.packageName"))
        assertFalse(drawer.contains("package name"))
    }

    @Test
    fun launcherUxStaysPrototypeOnlyWithoutRootScrollCrashPath() {
        val screen = source("app/src/main/java/com/transcendiverse/digitaltwin/launcher/LauncherScreen.kt")
        val activity = source("app/src/main/java/com/transcendiverse/digitaltwin/launcher/LauncherActivity.kt")
        val drawer = source("app/src/main/java/com/transcendiverse/digitaltwin/launcher/LauncherAppDrawer.kt")

        assertTrue(screen.contains("LauncherTextAction(text = \"companion\", onClick = onOpenCompanion)"))
        assertTrue(screen.contains("LauncherTextAction(text = \"settings\", onClick = onOpenSettings)"))
        assertFalse(screen.contains("verticalScroll"))
        assertFalse(screen.contains("rememberScrollState"))
        assertFalse(drawer.contains("verticalScroll"))
        assertFalse(drawer.contains("rememberScrollState"))
        assertFalse(activity.contains("ACTION_MANAGE_DEFAULT_APPS_SETTINGS"))
        assertFalse(activity.contains("RoleManager"))
        assertFalse(activity.contains("ROLE_HOME"))
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
