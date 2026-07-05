package com.transcendiverse.digitaltwin

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CompanionUxV2ContractTest {
    @Test
    fun v2ShowsOneNextStepThenUsableCheckIn() {
        val screen = source("app/src/main/java/com/transcendiverse/digitaltwin/ui/TodayScreen.kt")

        assertTrue(screen.contains("TodayNextStepCard"))
        assertTrue(screen.contains("CommandMicroLabel(text = \"CURRENT PROTOCOL\")"))
        assertTrue(screen.contains("Preview only. Sign in from Me when you want to save."))
        assertTrue(screen.contains("Pick the baseline that fits right now."))
        assertTrue(screen.contains("QuickCheckInCard("))
        assertTrue(screen.contains("signedIn = savedSettings.hasCredentials()"))
        assertTrue(screen.contains("Text(if (submitting) \"Submitting\" else \"Save check-in\")"))
    }

    @Test
    fun fixtureModeHasLocalCheckInPreviewWithoutFakeSubmit() {
        val screen = source("app/src/main/java/com/transcendiverse/digitaltwin/ui/TodayScreen.kt")

        assertTrue(screen.contains("Preview only. Sign in from Me to save."))
        assertTrue(screen.contains("This is a local preview. Sign in from Me to save check-ins."))
        assertTrue(screen.contains("CheckInPreset.entries.forEach"))
        assertTrue(screen.contains("RatingControlRow("))
        assertFalse(screen.contains("Sign in from Connection settings to save today's check-in."))
    }

    @Test
    fun oldDeadActionsAndDuplicateQuestCtaAreRemoved() {
        val screen = source("app/src/main/java/com/transcendiverse/digitaltwin/ui/TodayScreen.kt")

        assertFalse(screen.contains("LauncherActions"))
        assertFalse(screen.contains("today.launcher.primaryLabel"))
        assertFalse(screen.contains("today.launcher.secondaryLabel"))
        assertFalse(screen.contains("onClick = {}"))
        assertTrue(screen.contains("today.quest.nextAction.reason"))
        assertTrue(screen.contains("CommandMicroLabel(text = \"CURRENT PROTOCOL\")"))
    }

    @Test
    fun privacySensitiveDetailsStayOutOfMainDailyFlow() {
        val screen = source("app/src/main/java/com/transcendiverse/digitaltwin/ui/TodayScreen.kt")

        assertFalse(screen.contains("text = today.quest.nextAction.href"))
        assertFalse(screen.contains("text = savedSettings.token"))
        assertFalse(screen.contains("text = response.token"))
        assertFalse(screen.contains("Bearer "))
        assertTrue(screen.contains("PasswordVisualTransformation"))
        assertTrue(screen.contains("var expanded by remember { mutableStateOf(false) }"))
        assertTrue(screen.contains("if (!expanded) return@InfoCard"))
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
