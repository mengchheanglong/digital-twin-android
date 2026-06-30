package com.transcendiverse.digitaltwin

import com.transcendiverse.digitaltwin.launcher.DIGITAL_TWIN_PACKAGE_NAME
import com.transcendiverse.digitaltwin.launcher.LauncherApp
import com.transcendiverse.digitaltwin.launcher.filterSelfPackage
import com.transcendiverse.digitaltwin.launcher.filterValidLauncherApps
import com.transcendiverse.digitaltwin.launcher.sortLauncherApps
import org.junit.Assert.assertEquals
import org.junit.Test

class LauncherAppsRepositoryTest {
    @Test
    fun invalidBlankEntriesRemoved() {
        val apps = listOf(
            LauncherApp(packageName = "com.example.keep", label = "Keep", activityName = ".MainActivity"),
            LauncherApp(packageName = "", label = "Missing package", activityName = ".MainActivity"),
            LauncherApp(packageName = "   ", label = "Blank package", activityName = ".MainActivity"),
            LauncherApp(packageName = "com.example.missinglabel", label = "", activityName = ".MainActivity"),
            LauncherApp(packageName = "com.example.blanklabel", label = "   ", activityName = ".MainActivity"),
        )

        assertEquals(
            listOf(LauncherApp(packageName = "com.example.keep", label = "Keep", activityName = ".MainActivity")),
            filterValidLauncherApps(apps),
        )
    }

    @Test
    fun sortingStableCaseInsensitiveThenPackageName() {
        val firstCalculator = LauncherApp(
            packageName = "com.example.alpha",
            label = "Calculator",
            activityName = ".MainActivity",
        )
        val secondCalculator = LauncherApp(
            packageName = "com.example.beta",
            label = "calculator",
            activityName = ".MainActivity",
        )
        val camera = LauncherApp(
            packageName = "com.example.camera",
            label = "Camera",
            activityName = ".MainActivity",
        )
        val browser = LauncherApp(
            packageName = "com.example.browser",
            label = "Browser",
            activityName = ".MainActivity",
        )

        assertEquals(
            listOf(browser, firstCalculator, secondCalculator, camera),
            sortLauncherApps(listOf(secondCalculator, camera, firstCalculator, browser)),
        )
    }

    @Test
    fun selfPackageFilterRemovesDigitalTwin() {
        val self = LauncherApp(
            packageName = DIGITAL_TWIN_PACKAGE_NAME,
            label = "Digital Twin",
            activityName = ".MainActivity",
        )
        val other = LauncherApp(
            packageName = "com.example.other",
            label = "Other",
            activityName = ".MainActivity",
        )

        assertEquals(listOf(other), filterSelfPackage(listOf(self, other)))
    }
}
