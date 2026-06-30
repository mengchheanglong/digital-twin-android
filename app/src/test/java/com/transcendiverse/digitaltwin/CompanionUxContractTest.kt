package com.transcendiverse.digitaltwin

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CompanionUxContractTest {
    @Test
    fun todayScreenUsesCompanionFirstHierarchy() {
        val screen = source("app/src/main/java/com/transcendiverse/digitaltwin/ui/TodayScreen.kt")

        assertTrue(screen.contains("DailyHero"))
        assertTrue(screen.contains("PrimaryActionCard"))
        assertTrue(screen.contains("recommendedTodayAction(today)"))
        assertTrue(screen.contains("TodayVitalsRow"))
        assertTrue(screen.contains("ConnectionPanel"))
        assertTrue(screen.contains("Connection settings"))
        assertTrue(screen.contains("CompanionBackground"))
        assertTrue(screen.contains("CompanionPrimary"))

        assertFalse(screen.contains("StatusCards"))
        assertFalse(screen.contains("MetricCard("))
        assertFalse(screen.contains("SettingsPanel"))
    }

    @Test
    fun mainCompanionFlowHidesDebugBackendAndRawLinks() {
        val screen = source("app/src/main/java/com/transcendiverse/digitaltwin/ui/TodayScreen.kt")

        assertFalse(screen.contains("text = today.quest.nextAction.href"))
        assertFalse(screen.contains("Backend: ${'$'}{baseUrl"))
        assertFalse(screen.contains("text = savedSettings.token"))
        assertFalse(screen.contains("text = response.token"))
        assertFalse(screen.contains("Bearer "))

        assertTrue(screen.contains("today.quest.nextAction.label"))
        assertTrue(screen.contains("today.quest.nextAction.reason"))
        assertTrue(screen.contains("Backend base URL"))
        assertTrue(screen.contains("PasswordVisualTransformation"))
    }

    @Test
    fun connectionSettingsAreCollapsedByDefault() {
        val screen = source("app/src/main/java/com/transcendiverse/digitaltwin/ui/TodayScreen.kt")

        assertTrue(screen.contains("var expanded by remember { mutableStateOf(false) }"))
        assertTrue(screen.contains("if (!expanded) return@InfoCard"))
        assertTrue(screen.contains("Text(if (expanded) \"Hide\" else \"Connection settings\")"))
        assertTrue(screen.contains("Fixture mode"))
        assertTrue(screen.contains("Signed in"))
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
