package com.transcendiverse.digitaltwin.launcher

data class LauncherSafetyActions(
    val refreshLabel: String = REFRESH_LABEL,
    val openCompanionLabel: String = OPEN_COMPANION_LABEL,
    val openAppDrawerLabel: String = OPEN_APP_DRAWER_LABEL,
    val openSettingsLabel: String = OPEN_SETTINGS_LABEL,
) {
    fun toDisplayText(): String = listOf(
        refreshLabel,
        openCompanionLabel,
        openAppDrawerLabel,
        openSettingsLabel,
    ).joinToString(separator = "\n")

    companion object {
        const val REFRESH_LABEL = "Refresh"
        const val OPEN_COMPANION_LABEL = "Open companion"
        const val OPEN_APP_DRAWER_LABEL = "Open app drawer"
        const val OPEN_SETTINGS_LABEL = "Open settings"
    }
}
