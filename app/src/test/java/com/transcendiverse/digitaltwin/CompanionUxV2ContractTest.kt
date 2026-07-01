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
        assertTrue(screen.contains("text = \"Next step\""))
        assertTrue(screen.contains("Start with a 30-second check-in."))
        assertTrue(screen.contains("Pick the closest preset. Adjust only if needed."))
        assertTrue(screen.contains("QuickCheckInCard("))
        assertTrue(screen.contains("signedIn = savedSettings.hasCredentials()"))
        assertTrue(screen.contains("Text(if (submitting) \"Submitting\" else \"Save check-in\")"))
    }

    @Test
    fun fixtureModeHasLocalCheckInPreviewWithoutFakeSubmit() {
        val screen = source("app/src/main/java/com/transcendiverse/digitaltwin/ui/TodayScreen.kt")

        assertTrue(screen.contains("Preview mode: pick a preset and tune the ratings. Sign in from Connection settings to save."))
        assertTrue(screen.contains("This is a local preview. Nothing is sent or saved until you sign in."))
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
        assertTrue(screen.contains("After check-in: continue one small step."))
        assertTrue(screen.contains("checkInIsPrimary"))
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
