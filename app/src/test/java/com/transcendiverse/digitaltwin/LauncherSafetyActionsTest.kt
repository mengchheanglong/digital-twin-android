package com.transcendiverse.digitaltwin

import com.transcendiverse.digitaltwin.launcher.LauncherSafetyActions
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class LauncherSafetyActionsTest {
    @Test
    fun exposesSafeLauncherActionLabels() {
        val actions = LauncherSafetyActions()

        assertEquals("Refresh", actions.refreshLabel)
        assertEquals("Open companion", actions.openCompanionLabel)
        assertEquals("Open app drawer", actions.openAppDrawerLabel)
        assertEquals("Open settings", actions.openSettingsLabel)
    }

    @Test
    fun actionLabelsDoNotExposePrivateTerms() {
        val rendered = LauncherSafetyActions().toDisplayText().lowercase()
        val forbidden = listOf("token", "password", "bearer", "http://", "https://", "@", "{", "}")

        forbidden.forEach { value ->
            assertFalse("Launcher action labels must not contain $value", rendered.contains(value))
        }
    }
}
