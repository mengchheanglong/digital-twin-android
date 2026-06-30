package com.transcendiverse.digitaltwin.widget

import com.transcendiverse.digitaltwin.data.CachedToday

data class TodayWidgetSummary(
    val title: String,
    val mood: String? = null,
    val streak: String? = null,
    val quest: String? = null,
    val nextAction: String? = null,
    val cacheLabel: String? = null,
    val emptyMessage: String? = null,
) {
    fun toDisplayText(): String = listOfNotNull(
        title,
        mood,
        streak,
        quest,
        nextAction,
        cacheLabel,
        emptyMessage,
    ).joinToString(separator = "\n")

    companion object {
        fun from(cachedToday: CachedToday?): TodayWidgetSummary {
            if (cachedToday == null) {
                return TodayWidgetSummary(
                    title = TITLE,
                    emptyMessage = "Open app to refresh Today",
                )
            }

            val today = cachedToday.today
            return TodayWidgetSummary(
                title = TITLE,
                mood = "${today.user.mood.emoji} ${today.user.mood.label}",
                streak = "${today.user.streak} day streak",
                quest = today.quest.current?.goal ?: "No active quest",
                nextAction = today.quest.nextAction.label,
                cacheLabel = "Open app to refresh",
            )
        }

        private const val TITLE = "Digital Twin"
    }
}
