package com.transcendiverse.digitaltwin.model

import kotlinx.serialization.Serializable

@Serializable
data class MobileTodayResponse(
    val success: Boolean,
    val today: MobileToday,
)

@Serializable
data class MobileToday(
    val version: String,
    val generatedAt: String,
    val dayKey: String,
    val user: MobileUser,
    val checkIn: MobileCheckIn,
    val quest: MobileQuestSection,
    val insight: MobileInsight,
    val launcher: MobileLauncher,
)

@Serializable
data class MobileUser(
    val name: String,
    val level: Int,
    val currentXP: Int,
    val requiredXP: Int,
    val streak: Int,
    val mood: MobileMood,
)

@Serializable
data class MobileMood(
    val emoji: String,
    val label: String,
)

@Serializable
data class MobileCheckIn(
    val completedToday: Boolean,
    val score: Int? = null,
    val dimensions: MobileDimensions? = null,
)

@Serializable
data class MobileDimensions(
    val energy: Int,
    val focus: Int,
    val stressControl: Int,
    val socialConnection: Int,
    val optimism: Int,
)

@Serializable
data class MobileQuestSection(
    val current: MobileQuest? = null,
    val nextAction: MobileNextAction,
)

@Serializable
data class MobileQuest(
    val goal: String,
    val duration: String,
    val progress: Int,
)

@Serializable
data class MobileNextAction(
    val label: String,
    val href: String,
    val reason: String,
)

@Serializable
data class MobileInsight(
    val trend: String,
    val topInterest: String,
    val productivityScore: Double,
    val entertainmentRatio: Double,
    val reflection: String,
)

@Serializable
data class MobileLauncher(
    val primaryLabel: String,
    val primaryHref: String,
    val secondaryLabel: String,
    val secondaryHref: String,
)

enum class TodayAction(val label: String) {
    CHECK_IN("Check in"),
    CONTINUE_QUEST("Continue quest"),
    REFLECT("Reflect"),
}

fun recommendedTodayAction(today: MobileToday): TodayAction = when {
    !today.checkIn.completedToday -> TodayAction.CHECK_IN
    today.quest.current != null -> TodayAction.CONTINUE_QUEST
    else -> TodayAction.REFLECT
}
