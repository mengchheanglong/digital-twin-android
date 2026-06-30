package com.transcendiverse.digitaltwin

import java.io.File
import org.junit.Assert.assertTrue
import org.junit.Test

class LauncherManifestContractTest {
    private val manifest = readManifest()

    @Test
    fun mainActivityRemainsNormalLauncherEntryPoint() {
        val mainActivity = activityBlock(".MainActivity")

        assertTrue(mainActivity.contains("""android:name=".MainActivity""""))
        assertTrue(mainActivity.contains("""android.intent.category.LAUNCHER""""))
    }

    @Test
    fun launcherActivityIsRegisteredAsHomeCandidate() {
        val launcherActivity = activityBlock(".launcher.LauncherActivity")

        assertTrue(launcherActivity.contains("""android:name=".launcher.LauncherActivity""""))
        assertTrue(launcherActivity.contains("""android.intent.category.HOME""""))
        assertTrue(launcherActivity.contains("""android.intent.category.DEFAULT""""))
    }

    private fun activityBlock(activityName: String): String {
        val pattern = Regex("""<activity\b[\s\S]*?</activity>""")
        return pattern.findAll(manifest)
            .map { it.value }
            .firstOrNull { it.contains("""android:name="$activityName"""") }
            ?: error("Activity $activityName is not registered in AndroidManifest.xml")
    }

    private fun readManifest(): String {
        val candidates = listOf(
            File("src/main/AndroidManifest.xml"),
            File("app/src/main/AndroidManifest.xml"),
        )
        return candidates.firstOrNull(File::isFile)?.readText()
            ?: error("Unable to locate app/src/main/AndroidManifest.xml")
    }
}
