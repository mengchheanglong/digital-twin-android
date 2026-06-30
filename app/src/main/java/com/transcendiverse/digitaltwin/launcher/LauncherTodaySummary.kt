package com.transcendiverse.digitaltwin.launcher

import com.transcendiverse.digitaltwin.data.CachedToday
import kotlin.math.max

data class LauncherTodaySummary(
    val title: String,
    val mood: String? = null,
    val streak: String? = null,
    val checkInStatus: String? = null,
    val currentQuest: String? = null,
    val nextAction: String? = null,
    val cacheLabel: String,
    val emptyState: String? = null,
) {
    fun toDisplayText(): String = listOfNotNull(
        title,
        mood,
        streak,
        checkInStatus,
        currentQuest,
        nextAction,
        cacheLabel,
        emptyState,
    ).joinToString(separator = "\n")

    companion object {
        fun from(
            cachedToday: CachedToday?,
            nowEpochMillis: Long? = null,
        ): LauncherTodaySummary {
            if (cachedToday == null) {
                return LauncherTodaySummary(
                    title = TITLE,
                    cacheLabel = "No cached Today",
                    emptyState = "Open the companion app or refresh to load Today.",
                )
            }

            val today = cachedToday.today
            val quest = today.quest.current
            val nextAction = if (!today.checkIn.completedToday) {
                "Check in from companion"
            } else {
                today.quest.nextAction.label.safeLauncherText("Open companion")
            }

            return LauncherTodaySummary(
                title = TITLE,
                mood = "${today.user.mood.emoji} ${today.user.mood.label}".safeLauncherText("Mood unavailable"),
                streak = "${today.user.streak} ${if (today.user.streak == 1) "day" else "day"} streak",
                checkInStatus = if (today.checkIn.completedToday) {
                    today.checkIn.score?.let { "Check-in complete: $it" } ?: "Check-in complete"
                } else {
                    "Check-in pending"
                },
                currentQuest = quest?.goal.safeLauncherText("No active quest"),
                nextAction = nextAction,
                cacheLabel = cacheLabel(cachedToday.cachedAtEpochMillis, nowEpochMillis),
            )
        }

        private const val TITLE = "Digital Twin Today"
    }
}

private fun cacheLabel(cachedAtEpochMillis: Long, nowEpochMillis: Long?): String {
    if (nowEpochMillis == null) return "Cached Today"

    val ageMillis = max(0, nowEpochMillis - cachedAtEpochMillis)
    val ageMinutes = ageMillis / 60_000
    val ageHours = ageMinutes / 60
    val ageDays = ageHours / 24

    return when {
        ageMinutes < 1 -> "Cached just now"
        ageMinutes < 60 -> "Cached $ageMinutes min ago"
        ageHours < 24 -> "Cached $ageHours hr ago"
        ageDays == 1L -> "Cached 1 day ago"
        else -> "Cached $ageDays days ago"
    }
}

private fun String?.safeLauncherText(fallback: String): String {
    val value = this?.trim().orEmpty()
    return if (value.isBlank() || value.containsPrivateLauncherText()) fallback else value
}

private fun String.containsPrivateLauncherText(): Boolean {
    val lower = lowercase()
    return listOf("token", "password", "bearer", "http://", "https://", "@", "{", "}").any { lower.contains(it) }
}
