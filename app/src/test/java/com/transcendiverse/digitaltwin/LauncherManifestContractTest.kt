package com.transcendiverse.digitaltwin

import java.io.File
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LauncherManifestContractTest {
    private val manifest = readManifest()
    private val applicationIdPlaceholder = "\${applicationId}"

    @Test
    fun appDataBackupStaysDisabledWhileSecretsUseSharedPreferences() {
        assertTrue(manifest.contains("""android:allowBackup="false""""))
    }

    @Test
    fun mainActivityRemainsCompanionTaskOnly() {
        val mainActivity = activityTag(".MainActivity")

        assertTrue(mainActivity.contains("""android:name=".MainActivity""""))
        assertFalse(mainActivity.contains("""android.intent.category.LAUNCHER""""))
        assertFalse(mainActivity.contains("""android.intent.category.HOME""""))
        assertTrue(mainActivity.contains("""android:taskAffinity="$applicationIdPlaceholder.companion""""))
    }

    @Test
    fun launcherActivityIsRegisteredAsHomeCandidate() {
        val launcherActivity = activityTag(".launcher.LauncherActivity")

        assertTrue(launcherActivity.contains("""android:name=".launcher.LauncherActivity""""))
        assertTrue(launcherActivity.contains("""android.intent.category.LAUNCHER""""))
        assertTrue(launcherActivity.contains("""android.intent.category.HOME""""))
        assertTrue(launcherActivity.contains("""android.intent.category.DEFAULT""""))
        assertTrue(launcherActivity.contains("""android:launchMode="singleTask""""))
        assertTrue(launcherActivity.contains("""android:taskAffinity="$applicationIdPlaceholder.home""""))
    }

    @Test
    fun launcherAndCompanionUseDifferentTaskAffinities() {
        val launcherActivity = activityTag(".launcher.LauncherActivity")
        val mainActivity = activityTag(".MainActivity")

        assertNotEquals(taskAffinity(launcherActivity), taskAffinity(mainActivity))
    }

    private fun activityTag(activityName: String): String {
        val nameIndex = manifest.indexOf("""android:name="$activityName"""")
            .takeIf { it >= 0 }
            ?: error("Activity $activityName is not registered in AndroidManifest.xml")
        val startIndex = manifest.lastIndexOf("<activity", nameIndex)
            .takeIf { it >= 0 }
            ?: error("Activity $activityName is missing an activity tag")
        val openingTagEnd = manifest.indexOf(">", startIndex)
            .takeIf { it >= 0 }
            ?: error("Activity $activityName has an incomplete activity tag")
        val openingTag = manifest.substring(startIndex, openingTagEnd + 1)
        if (openingTag.trimEnd().endsWith("/>")) return openingTag

        val closingTagEnd = manifest.indexOf("</activity>", openingTagEnd)
            .takeIf { it >= 0 }
            ?: error("Activity $activityName is missing a closing activity tag")
        return manifest.substring(startIndex, closingTagEnd + "</activity>".length)
    }

    private fun taskAffinity(activityBlock: String): String =
        Regex("""android:taskAffinity="([^"]+)"""")
            .find(activityBlock)
            ?.groupValues
            ?.get(1)
            ?: error("Activity block is missing android:taskAffinity")

    private fun readManifest(): String {
        val candidates = listOf(
            File("src/main/AndroidManifest.xml"),
            File("app/src/main/AndroidManifest.xml"),
        )
        return candidates.firstOrNull(File::isFile)?.readText()
            ?: error("Unable to locate app/src/main/AndroidManifest.xml")
    }
}
