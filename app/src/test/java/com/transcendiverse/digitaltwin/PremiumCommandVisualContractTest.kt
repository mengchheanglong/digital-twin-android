package com.transcendiverse.digitaltwin

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PremiumCommandVisualContractTest {
    @Test
    fun premiumCommandTokensExist() {
        val screen = source("app/src/main/java/com/transcendiverse/digitaltwin/ui/TodayScreen.kt")

        assertTrue(screen.contains("private val CommandCanvas = Color(0xFF050607)"))
        assertTrue(screen.contains("private val CommandPanel = Color(0x08FFFFFF)"))
        assertTrue(screen.contains("private val CommandElevated = Color(0x0CFFFFFF)"))
        assertTrue(screen.contains("private val CommandHairline = Color(0x13FFFFFF)"))
        assertTrue(screen.contains("private val CommandAccent = Color(0xFFA78BFA)"))
        assertTrue(screen.contains("private val CommandText = Color(0xFFF4F1EA)"))
        assertTrue(screen.contains("private val CommandSecondaryText = Color(0xFFB8B0A3)"))
        assertTrue(screen.contains("private val CommandMutedText = Color(0xFF8F8A80)"))
    }

    @Test
    fun shellUsesCompactCommandHelpers() {
        val screen = source("app/src/main/java/com/transcendiverse/digitaltwin/ui/TodayScreen.kt")

        assertTrue(screen.contains("private fun CommandSegmentTab("))
        assertTrue(screen.contains("height(42.dp)"))
        assertTrue(screen.contains("heightIn(min = 40.dp, max = 40.dp)"))
        assertTrue(screen.contains("label.uppercase(Locale.US)"))
        assertTrue(screen.contains("private fun CommandMicroLabel("))
        assertTrue(screen.contains("FontFamily.Monospace"))
        assertTrue(screen.contains("MaterialTheme.typography.titleMedium"))
        assertFalse(screen.contains("MaterialTheme.typography.headlineSmall"))
    }

    @Test
    fun askAndJournalUseCommandShortcutChips() {
        val screen = source("app/src/main/java/com/transcendiverse/digitaltwin/ui/TodayScreen.kt")

        assertTrue(screen.contains("private fun CommandShortcutChip("))
        assertTrue(screen.contains("text = label"))
        assertTrue(screen.contains("Plan the next hour from today's state"))
        assertTrue(screen.contains("What tiny win should I protect?"))
        assertFalse(screen.contains("private fun CompanionPromptChip("))
    }

    @Test
    fun questUsesCommandConsoleContract() {
        val screen = source("app/src/main/java/com/transcendiverse/digitaltwin/ui/TodayScreen.kt")

        assertFalse(screen.contains("InfoCard(title = if (quest == null) \"Quest\" else \"Current quest\")"))
        assertTrue(screen.contains("private fun QuestCard("))
        assertTrue(screen.contains("label = \"QUEST ARC\""))
        assertTrue(screen.contains("CommandConsoleCard(label = \"OBJECTIVE\", accent = true)"))
        assertTrue(screen.contains("CommandConsoleCard(label = \"PROGRESS\")"))
        assertTrue(screen.contains("Preview only. Sign in to update progress."))
        assertTrue(screen.contains("onClick = onDecreaseProgress"))
        assertTrue(screen.contains("onClick = onIncreaseProgress"))
        assertTrue(screen.contains("onClick = onToggleComplete"))
    }

    @Test
    fun checkInStateAndGroupedControlsArePreserved() {
        val screen = source("app/src/main/java/com/transcendiverse/digitaltwin/ui/TodayScreen.kt")

        assertTrue(screen.contains("selectedPreset == preset"))
        assertTrue(screen.contains("selectedPreset = preset"))
        assertTrue(screen.contains("selectedPreset = checkInPresetForRatings(updated)"))
        assertTrue(screen.contains("RatingControlRow("))
        assertTrue(screen.contains("RatingStepperButton("))
        assertTrue(screen.contains("Modifier.size(48.dp)"))
        assertTrue(screen.contains("checkInShapeSummary(ratings, selectedPreset)"))
    }

    @Test
    fun privacySensitiveDetailsStayOutOfMainUi() {
        val screen = source("app/src/main/java/com/transcendiverse/digitaltwin/ui/TodayScreen.kt")

        assertFalse(screen.contains("text = savedSettings.token"))
        assertFalse(screen.contains("text = response.token"))
        assertFalse(screen.contains("text = today.quest.nextAction.href"))
        assertFalse(screen.contains("Bearer "))
        assertFalse(screen.contains("Backend: ${'$'}{baseUrl"))
        assertTrue(screen.contains("PasswordVisualTransformation"))
        assertTrue(screen.contains("containsPrivateStatusText"))
    }

    @Test
    fun meProfileCardExposesWebDashboardBridgeWithoutTokenHandoff() {
        val screen = source("app/src/main/java/com/transcendiverse/digitaltwin/ui/TodayScreen.kt")

        assertTrue(screen.contains("CommandConsoleCard(label = \"MORE FROM WEB\")"))
        assertTrue(screen.contains("private data class WebBridgeRoute("))
        assertTrue(screen.contains("private val WebBridgeRoutes = listOf("))

        listOf(
            "Focus",
            "Analytics",
            "Timeline",
            "History",
            "Data export",
            "Full dashboard",
        ).forEach { label ->
            assertTrue(screen.contains("label = \"$label\""))
        }

        listOf(
            "/dashboard/focus",
            "/dashboard/analytics",
            "/dashboard/timeline",
            "/dashboard/history",
            "/dashboard/profile",
            "/dashboard/insight",
        ).forEach { path ->
            assertTrue(screen.contains("path = \"$path\""))
        }

        assertTrue(screen.contains("LocalUriHandler.current"))
        assertTrue(screen.contains("openUri(route.webUri())"))

        val bridgeSource = screen
            .substringAfter("private data class WebBridgeRoute(")
            .substringBefore("private fun CommandMiniStat(")

        assertFalse(screen.contains("?token"))
        assertFalse(screen.contains("jwt", ignoreCase = true))
        assertFalse(screen.contains("Bearer "))
        assertFalse(screen.contains("Authorization"))
        assertFalse(bridgeSource.contains("savedSettings.token"))
        assertFalse(bridgeSource.contains("response.token"))
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
