package com.transcendiverse.digitaltwin.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

interface CheckInRepository {
    suspend fun submitDaily(ratings: List<Int>): CheckInSubmitResult
}

data class CheckInSubmitResult(
    val message: String,
    val totalScore: Int,
    val maxScore: Int,
    val percentage: Int,
)

class CheckInNetworkException(
    message: String,
    val statusCode: Int? = null,
    cause: Throwable? = null,
) : Exception(message, cause)

class NetworkCheckInRepository(
    private val baseUrl: String,
    private val token: String,
    private val transport: TodayHttpTransport = OkHttpTodayTransport(),
    private val json: Json = Json {
        ignoreUnknownKeys = true
        explicitNulls = false
    },
) : CheckInRepository {
    override suspend fun submitDaily(ratings: List<Int>): CheckInSubmitResult {
        validateDailyCheckInRatings(ratings)

        val request = TodayHttpRequest(
            method = "POST",
            url = buildCheckInSubmitUrl(baseUrl),
            headers = mapOf(
                "Authorization" to "Bearer ${token.trim()}",
                "Content-Type" to "application/json",
                "Accept" to "application/json",
            ),
            body = json.encodeToString(CheckInSubmitRequest(ratings = ratings)),
        )
        val response = try {
            withContext(Dispatchers.IO) {
                transport.execute(request)
            }
        } catch (error: Exception) {
            throw CheckInNetworkException("POST $CHECK_IN_SUBMIT_PATH failed", cause = error)
        }

        if (response.statusCode !in 200..299) {
            throw CheckInNetworkException(
                message = buildSubmitFailureMessage(response),
                statusCode = response.statusCode,
            )
        }

        val submitResponse = try {
            json.decodeFromString<CheckInSubmitResponse>(response.body)
        } catch (error: SerializationException) {
            throw CheckInNetworkException("POST $CHECK_IN_SUBMIT_PATH returned invalid JSON", cause = error)
        } catch (error: IllegalArgumentException) {
            throw CheckInNetworkException("POST $CHECK_IN_SUBMIT_PATH returned invalid JSON", cause = error)
        }

        return CheckInSubmitResult(
            message = submitResponse.msg,
            totalScore = submitResponse.result.totalScore,
            maxScore = submitResponse.result.maxScore,
            percentage = submitResponse.result.percentage,
        )
    }

    private fun buildSubmitFailureMessage(response: TodayHttpResponse): String {
        val fallback = "POST $CHECK_IN_SUBMIT_PATH failed with HTTP ${response.statusCode}"
        val backendMessage = parseSafeBackendMessage(response.body) ?: return fallback
        return "$fallback: $backendMessage"
    }

    private fun parseSafeBackendMessage(body: String): String? {
        val message = try {
            json.decodeFromString<CheckInErrorResponse>(body).msg
        } catch (_: Exception) {
            null
        }?.trim().orEmpty()

        return message.takeIf(::isSafeBackendMessage)
    }

    private fun isSafeBackendMessage(message: String): Boolean {
        if (message.isBlank()) return false
        val lower = message.lowercase()
        return listOf("token", "password", "bearer", "http://", "https://", "@", "{", "}").none { lower.contains(it) }
    }
}

fun validateDailyCheckInRatings(ratings: List<Int>) {
    require(ratings.size == 5 && ratings.all { it in 1..5 }) {
        "Must provide exactly 5 ratings from 1 to 5."
    }
}

fun buildCheckInSubmitUrl(baseUrl: String): String = baseUrl.trim().trimEnd('/') + CHECK_IN_SUBMIT_PATH

private const val CHECK_IN_SUBMIT_PATH = "/api/checkin/submit"

@Serializable
private data class CheckInSubmitRequest(
    val ratings: List<Int>,
)

@Serializable
private data class CheckInSubmitResponse(
    val msg: String,
    val result: CheckInSubmitPayload,
)

@Serializable
private data class CheckInSubmitPayload(
    val totalScore: Int,
    val maxScore: Int,
    val percentage: Int,
)

@Serializable
private data class CheckInErrorResponse(
    val msg: String? = null,
)
