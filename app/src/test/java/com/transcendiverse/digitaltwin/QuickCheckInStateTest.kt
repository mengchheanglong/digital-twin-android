package com.transcendiverse.digitaltwin

import com.transcendiverse.digitaltwin.ui.CheckInPreset
import com.transcendiverse.digitaltwin.ui.checkInPresetRatings
import com.transcendiverse.digitaltwin.ui.updateCheckInRating
import com.transcendiverse.digitaltwin.ui.validateQuickCheckInRatings
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class QuickCheckInStateTest {
    @Test
    fun presetsMapToExpectedRatings() {
        assertEquals(listOf(2, 2, 2, 2, 2), checkInPresetRatings(CheckInPreset.LOW))
        assertEquals(listOf(3, 3, 3, 3, 3), checkInPresetRatings(CheckInPreset.OKAY))
        assertEquals(listOf(4, 4, 4, 4, 4), checkInPresetRatings(CheckInPreset.STRONG))
    }

    @Test
    fun ratingUpdateClampsToOneThroughFive() {
        assertEquals(listOf(1, 3, 3, 3, 3), updateCheckInRating(listOf(3, 3, 3, 3, 3), index = 0, delta = -9))
        assertEquals(listOf(3, 3, 3, 3, 5), updateCheckInRating(listOf(3, 3, 3, 3, 3), index = 4, delta = 9))
    }

    @Test
    fun validationRejectsInvalidQuickCheckInRatings() {
        assertNull(validateQuickCheckInRatings(listOf(1, 2, 3, 4, 5)))
        assertEquals("Must provide exactly 5 ratings from 1 to 5.", validateQuickCheckInRatings(listOf(3, 3, 3)))
        assertEquals("Must provide exactly 5 ratings from 1 to 5.", validateQuickCheckInRatings(listOf(3, 3, 6, 3, 3)))
    }
}
