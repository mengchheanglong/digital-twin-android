package com.transcendiverse.digitaltwin

import com.transcendiverse.digitaltwin.launcher.LauncherApp
import com.transcendiverse.digitaltwin.launcher.favoriteLauncherApps
import com.transcendiverse.digitaltwin.launcher.searchLauncherApps
import com.transcendiverse.digitaltwin.launcher.toggleFavoritePackage
import org.junit.Assert.assertEquals
import org.junit.Test

class LauncherUxModelTest {
    @Test
    fun searchLauncherAppsMatchesLabelAndPackage() {
        val apps = listOf(
            LauncherApp(packageName = "com.todo.notes", label = "Notes", activityName = null),
            LauncherApp(packageName = "org.camera", label = "Camera", activityName = null),
            LauncherApp(packageName = "net.mail", label = "Mail", activityName = null),
        )

        assertEquals(listOf("Notes"), searchLauncherApps(apps, "note").map { it.label })
        assertEquals(listOf("Camera"), searchLauncherApps(apps, "ORG.CAM").map { it.label })
        assertEquals(apps, searchLauncherApps(apps, "   "))
    }

    @Test
    fun favoritePackageToggleAddsAndRemovesCleanPackageNames() {
        val added = toggleFavoritePackage(emptySet(), " com.todo.notes ")
        assertEquals(setOf("com.todo.notes"), added)

        val removed = toggleFavoritePackage(added, "com.todo.notes")
        assertEquals(emptySet<String>(), removed)

        val unchanged = toggleFavoritePackage(setOf("org.camera"), "   ")
        assertEquals(setOf("org.camera"), unchanged)
    }

    @Test
    fun favoriteLauncherAppsKeepRepositoryOrder() {
        val apps = listOf(
            LauncherApp(packageName = "a", label = "Alpha", activityName = null),
            LauncherApp(packageName = "b", label = "Beta", activityName = null),
            LauncherApp(packageName = "c", label = "Charlie", activityName = null),
        )

        assertEquals(
            listOf("Alpha", "Charlie"),
            favoriteLauncherApps(apps, setOf("c", "a")).map { it.label },
        )
    }
}
