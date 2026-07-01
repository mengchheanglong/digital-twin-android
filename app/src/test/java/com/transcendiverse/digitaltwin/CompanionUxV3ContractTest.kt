package com.transcendiverse.digitaltwin

import com.transcendiverse.digitaltwin.ui.CheckInPreset
import com.transcendiverse.digitaltwin.ui.checkInPresetForRatings
import com.transcendiverse.digitaltwin.ui.checkInShapeSummary
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CompanionUxV3ContractTest {
    @Test
    fun v3ShowsSelectedPresetStateByDefault() {
        val screen = source("app/src/main/java/com/transcendiverse/digitaltwin/ui/TodayScreen.kt")

        assertTrue(screen.contains("var selectedPreset by remember(today.dayKey) { mutableStateOf<CheckInPreset?>(CheckInPreset.OKAY) }"))
        assertTrue(screen.contains("PresetPill("))
        assertTrue(screen.contains("selected = selectedPreset == preset"))
        assertTrue(screen.contains("border = BorderStroke(1.dp, CompanionPrimary)"))
        assertTrue(screen.contains("Text(preset.label, fontWeight = FontWeight.SemiBold)"))
    }

    @Test
    fun presetTapsAndRatingChangesKeepPresetStateHonest() {
        val screen = source("app/src/main/java/com/transcendiverse/digitaltwin/ui/TodayScreen.kt")

        assertTrue(screen.contains("ratings = checkInPresetRatings(preset)"))
        assertTrue(screen.contains("selectedPreset = preset"))
        assertTrue(screen.contains("selectedPreset = checkInPresetForRatings(ratings)"))
        assertEquals(CheckInPreset.OKAY, checkInPresetForRatings(listOf(3, 3, 3, 3, 3)))
        assertNull(checkInPresetForRatings(listOf(3, 4, 3, 3, 3)))
    }

    @Test
    fun ratingControlsAreGroupedAndUseRealHandlers() {
        val screen = source("app/src/main/java/com/transcendiverse/digitaltwin/ui/TodayScreen.kt")

        assertTrue(screen.contains("RatingControlRow("))
        assertTrue(screen.contains("RatingStepperButton("))
        assertTrue(screen.contains("modifier = Modifier.size(46.dp)"))
        assertTrue(screen.contains("Surface("))
        assertTrue(screen.contains("onDecrease = {"))
        assertTrue(screen.contains("onIncrease = {"))
        assertTrue(screen.contains("updateCheckInRating(ratings, index, -1)"))
        assertTrue(screen.contains("updateCheckInRating(ratings, index, 1)"))
    }

    @Test
    fun checkInSummaryIsDeterministicAndHumble() {
        assertEquals(
            "Current shape: steady baseline - all 3/5",
            checkInShapeSummary(listOf(3, 3, 3, 3, 3), CheckInPreset.OKAY),
        )
        assertEquals(
            "Current shape: strong day - all 4/5",
            checkInShapeSummary(listOf(4, 4, 4, 4, 4), CheckInPreset.STRONG),
        )
        assertEquals(
            "Current shape: mixed baseline - mostly 3/5",
            checkInShapeSummary(listOf(3, 4, 3, 3, 4), null),
        )
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
