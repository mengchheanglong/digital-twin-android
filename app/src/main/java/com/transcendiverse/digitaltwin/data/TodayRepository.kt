package com.transcendiverse.digitaltwin.data

import com.transcendiverse.digitaltwin.model.MobileToday
import kotlinx.serialization.json.Json

interface TodayRepository {
    fun getToday(): MobileToday
}

class FakeTodayRepository(
    private val json: Json = Json {
        ignoreUnknownKeys = true
        explicitNulls = false
    },
) : TodayRepository {
    override fun getToday(): MobileToday = json.decodeFromString(TodayFixture.json)
}

object TodayFixture {
    val json: String =
        """
        {
          "version": "mobile-today.v0",
          "generatedAt": "2026-06-30T09:00:00.000Z",
          "dayKey": "2026-06-30",
          "user": {
            "name": "Alex",
            "level": 7,
            "currentXP": 340,
            "requiredXP": 500,
            "streak": 5,
            "mood": "focused"
          },
          "checkIn": {
            "completedToday": false,
            "score": 72,
            "dimensions": {
              "energy": 7,
              "focus": 8,
              "mood": 6
            }
          },
          "quest": {
            "current": {
              "title": "Ship the smallest useful companion",
              "progress": 2,
              "target": 4
            },
            "nextAction": {
              "label": "Check in",
              "reason": "Start with today's state before continuing quests."
            }
          },
          "insight": {
            "trend": "steady",
            "topInterest": "mobile companion",
            "productivityScore": 81,
            "entertainmentRatio": 0.22,
            "reflection": "You work best today by keeping the loop small: check in, continue one quest, then reflect briefly."
          },
          "launcher": {
            "primaryLabel": "Open check-in",
            "primaryHref": "/dashboard/checkin",
            "secondaryLabel": "View profile",
            "secondaryHref": "/dashboard/profile"
          }
        }
        """.trimIndent()
}
