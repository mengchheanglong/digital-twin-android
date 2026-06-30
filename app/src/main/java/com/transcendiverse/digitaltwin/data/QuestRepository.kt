package com.transcendiverse.digitaltwin.data

import com.transcendiverse.digitaltwin.model.MobileQuest
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement

interface QuestRepository {
    suspend fun listQuests(limit: Int = 20, skip: Int = 0): List<QuestSummary>
    suspend fun updateProgress(id: String, progress: Int): QuestActionResult
    suspend fun toggleComplete(id: String): QuestActionResult
}

data class QuestSummary(
    val id: String,
    val goal: String,
    val duration: String,
    val progress: Int,
    val completed: Boolean,
)

data class QuestActionResult(
    val message: String,
    val quest: QuestSummary?,
    val progression: JsonElement?,
    val deleted: Boolean,
)

class QuestNetworkException(
    message: String,
    val statusCode: Int? = null,
    cause: Throwable? = null,
) : Exception(message, cause)

class NetworkQuestRepository(
    private val baseUrl: String,
    private val token: String,
    private val transport: TodayHttpTransport = OkHttpTodayTransport(),
    private val json: Json = Json {
        ignoreUnknownKeys = true
        explicitNulls = false
    },
) : QuestRepository {
    override suspend fun listQuests(limit: Int, skip: Int): List<QuestSummary> {
        val path = "$QUEST_ALL_PATH?limit=$limit&skip=$skip"
        val request = TodayHttpRequest(
            method = "GET",
            url = buildUrl(path),
            headers = authHeaders(),
        )
        val response = execute(request, "GET $QUEST_ALL_PATH")
        if (response.statusCode !in 200..299) {
            throw QuestNetworkException(
                message = buildFailureMessage("GET $QUEST_ALL_PATH", response),
                statusCode = response.statusCode,
            )
        }

        return try {
            json.decodeFromString<List<QuestDto>>(response.body).map { it.toSummary() }
        } catch (error: SerializationException) {
            throw QuestNetworkException("GET $QUEST_ALL_PATH returned invalid JSON", cause = error)
        } catch (error: IllegalArgumentException) {
            throw QuestNetworkException("GET $QUEST_ALL_PATH returned invalid JSON", cause = error)
        }
    }

    override suspend fun updateProgress(id: String, progress: Int): QuestActionResult {
        val cleanId = validateQuestId(id)
        validateQuestProgress(progress)

        val path = "$QUEST_PROGRESS_PATH/${encodePathSegment(cleanId)}"
        val request = TodayHttpRequest(
            method = "PUT",
            url = buildUrl(path),
            headers = authHeaders() + ("Content-Type" to "application/json"),
            body = json.encodeToString(QuestProgressRequest(progress = progress)),
        )
        return executeAction(request, "PUT $QUEST_PROGRESS_PATH")
    }

    override suspend fun toggleComplete(id: String): QuestActionResult {
        val cleanId = validateQuestId(id)
        val path = "$QUEST_COMPLETE_PATH/${encodePathSegment(cleanId)}"
        val request = TodayHttpRequest(
            method = "PUT",
            url = buildUrl(path),
            headers = authHeaders(),
            body = "",
        )
        return executeAction(request, "PUT $QUEST_COMPLETE_PATH")
    }

    private suspend fun executeAction(request: TodayHttpRequest, operation: String): QuestActionResult {
        val response = execute(request, operation)
        if (response.statusCode !in 200..299) {
            throw QuestNetworkException(
                message = buildFailureMessage(operation, response),
                statusCode = response.statusCode,
            )
        }

        val actionResponse = try {
            json.decodeFromString<QuestActionResponse>(response.body)
        } catch (error: SerializationException) {
            throw QuestNetworkException("$operation returned invalid JSON", cause = error)
        } catch (error: IllegalArgumentException) {
            throw QuestNetworkException("$operation returned invalid JSON", cause = error)
        }

        return QuestActionResult(
            message = actionResponse.msg,
            quest = actionResponse.quest?.toSummary(),
            progression = actionResponse.progression,
            deleted = actionResponse.deleted,
        )
    }

    private suspend fun execute(request: TodayHttpRequest, operation: String): TodayHttpResponse =
        try {
            withContext(Dispatchers.IO) {
                transport.execute(request)
            }
        } catch (error: Exception) {
            throw QuestNetworkException("$operation failed", cause = error)
        }

    private fun authHeaders(): Map<String, String> = mapOf(
        "Authorization" to "Bearer ${token.trim()}",
        "Accept" to "application/json",
    )

    private fun buildUrl(path: String): String = baseUrl.trim().trimEnd('/') + path

    private fun buildFailureMessage(operation: String, response: TodayHttpResponse): String {
        val fallback = "$operation failed with HTTP ${response.statusCode}"
        val backendMessage = parseSafeBackendMessage(response.body) ?: return fallback
        return "$fallback: $backendMessage"
    }

    private fun parseSafeBackendMessage(body: String): String? {
        val message = try {
            json.decodeFromString<QuestErrorResponse>(body).msg
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

fun validateQuestId(id: String): String {
    val cleanId = id.trim()
    require(cleanId.isNotBlank()) { "Quest id is required" }
    return cleanId
}

fun validateQuestProgress(progress: Int) {
    require(progress in 0..100) { "Progress must be a number from 0 to 100." }
}

fun resolveActiveQuest(current: MobileQuest?, quests: List<QuestSummary>): QuestSummary? {
    if (current == null) return null
    val candidates = quests.filter { it.id.isNotBlank() }
    return candidates.firstOrNull { quest ->
        quest.goal.trim() == current.goal.trim() && quest.duration.trim() == current.duration.trim()
    } ?: candidates.firstOrNull { !it.completed }
}

private fun encodePathSegment(segment: String): String =
    URLEncoder.encode(segment, StandardCharsets.UTF_8.toString()).replace("+", "%20")

private fun QuestDto.toSummary(): QuestSummary = QuestSummary(
    id = id.orEmpty(),
    goal = goal.orEmpty(),
    duration = duration.orEmpty(),
    progress = progress ?: 0,
    completed = completed ?: false,
)

private const val QUEST_ALL_PATH = "/api/quest/all"
private const val QUEST_PROGRESS_PATH = "/api/quest/progress"
private const val QUEST_COMPLETE_PATH = "/api/quest/complete"

@Serializable
private data class QuestDto(
    @SerialName("_id")
    val id: String? = null,
    val goal: String? = null,
    val duration: String? = null,
    val progress: Int? = null,
    val completed: Boolean? = null,
)

@Serializable
private data class QuestProgressRequest(
    val progress: Int,
)

@Serializable
private data class QuestActionResponse(
    val msg: String,
    val quest: QuestDto? = null,
    val progression: JsonElement? = null,
    val deleted: Boolean = false,
)

@Serializable
private data class QuestErrorResponse(
    val msg: String? = null,
)
