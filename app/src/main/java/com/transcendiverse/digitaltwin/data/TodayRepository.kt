package com.transcendiverse.digitaltwin.data

import android.content.Context
import com.transcendiverse.digitaltwin.model.MobileToday
import com.transcendiverse.digitaltwin.model.MobileTodayResponse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.Request

interface TodayRepository {
    suspend fun getToday(): MobileToday
}

class FakeTodayRepository(
    private val json: Json = Json {
        ignoreUnknownKeys = true
        explicitNulls = false
    },
) : TodayRepository {
    override suspend fun getToday(): MobileToday = json.decodeFromString<MobileTodayResponse>(TodayFixture.json).today
}

data class TodayHttpRequest(
    val method: String,
    val url: String,
    val headers: Map<String, String>,
)

data class TodayHttpResponse(
    val statusCode: Int,
    val body: String,
)

interface TodayHttpTransport {
    fun execute(request: TodayHttpRequest): TodayHttpResponse
}

class OkHttpTodayTransport(
    private val client: OkHttpClient = OkHttpClient(),
) : TodayHttpTransport {
    override fun execute(request: TodayHttpRequest): TodayHttpResponse {
        val builder = Request.Builder().url(request.url)
        request.headers.forEach { (name, value) -> builder.header(name, value) }

        client.newCall(builder.method(request.method, null).build()).execute().use { response ->
            return TodayHttpResponse(
                statusCode = response.code,
                body = response.body.string(),
            )
        }
    }
}

class TodayNetworkException(
    message: String,
    cause: Throwable? = null,
) : Exception(message, cause)

class NetworkTodayRepository(
    private val baseUrl: String,
    private val token: String,
    private val transport: TodayHttpTransport = OkHttpTodayTransport(),
    private val json: Json = Json {
        ignoreUnknownKeys = true
        explicitNulls = false
    },
) : TodayRepository {
    override suspend fun getToday(): MobileToday {
        val request = TodayHttpRequest(
            method = "GET",
            url = buildTodayUrl(baseUrl),
            headers = mapOf(
                "Authorization" to "Bearer ${token.trim()}",
                "Accept" to "application/json",
            ),
        )
        val response = try {
            withContext(Dispatchers.IO) {
                transport.execute(request)
            }
        } catch (error: Exception) {
            throw TodayNetworkException("GET $TODAY_PATH failed: ${error.message}", error)
        }

        if (response.statusCode !in 200..299) {
            throw TodayNetworkException("GET $TODAY_PATH failed with HTTP ${response.statusCode}")
        }

        val envelope = try {
            json.decodeFromString<MobileTodayResponse>(response.body)
        } catch (error: SerializationException) {
            throw TodayNetworkException("GET $TODAY_PATH returned invalid JSON: ${error.message}", error)
        } catch (error: IllegalArgumentException) {
            throw TodayNetworkException("GET $TODAY_PATH returned invalid JSON: ${error.message}", error)
        }

        if (!envelope.success) {
            throw TodayNetworkException("GET $TODAY_PATH returned success=false")
        }

        return envelope.today
    }

    private fun buildTodayUrl(baseUrl: String): String = baseUrl.trim().trimEnd('/') + TODAY_PATH

    private companion object {
        const val TODAY_PATH = "/api/mobile/today"
    }
}

data class TodaySettings(
    val baseUrl: String = "",
    val token: String = "",
) {
    fun hasCredentials(): Boolean = baseUrl.isNotBlank() && token.isNotBlank()
}

interface TodaySettingsStore {
    fun load(): TodaySettings
    fun save(settings: TodaySettings)
}

class InMemoryTodaySettingsStore(
    initial: TodaySettings = TodaySettings(),
) : TodaySettingsStore {
    private var settings = initial

    override fun load(): TodaySettings = settings

    override fun save(settings: TodaySettings) {
        this.settings = settings
    }
}

class SharedPreferencesTodaySettingsStore(
    context: Context,
) : TodaySettingsStore {
    private val preferences = context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)

    override fun load(): TodaySettings = TodaySettings(
        baseUrl = preferences.getString(KEY_BASE_URL, "").orEmpty(),
        token = preferences.getString(KEY_TOKEN, "").orEmpty(),
    )

    override fun save(settings: TodaySettings) {
        preferences.edit()
            .putString(KEY_BASE_URL, settings.baseUrl.trim())
            .putString(KEY_TOKEN, settings.token.trim())
            .apply()
    }

    private companion object {
        const val PREFERENCES_NAME = "mobile_today_settings"
        const val KEY_BASE_URL = "backend_base_url"
        const val KEY_TOKEN = "jwt_token"
    }
}

object TodayFixture {
    val json: String =
        """
        {
          "success": true,
          "today": {
            "version": "mobile-today.v0",
            "generatedAt": "2026-06-30T09:00:00.000Z",
            "dayKey": "2026-06-30",
            "user": {
              "name": "Alex",
              "level": 7,
              "currentXP": 340,
              "requiredXP": 500,
              "streak": 5,
              "mood": {
                "emoji": "\uD83C\uDFAF",
                "label": "focused"
              }
            },
            "checkIn": {
              "completedToday": false,
              "score": 72,
              "dimensions": {
                "energy": 7,
                "focus": 8,
                "stressControl": 6,
                "socialConnection": 5,
                "optimism": 7
              }
            },
            "quest": {
              "current": {
                "goal": "Ship the smallest useful companion",
                "duration": "daily",
                "progress": 40
              },
              "nextAction": {
                "label": "Check in",
                "href": "/dashboard/checkin",
                "reason": "Start with today's state before continuing quests."
              }
            },
            "insight": {
              "trend": "rising",
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
        }
        """.trimIndent()
}
