package com.transcendiverse.digitaltwin.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

interface JournalRepository {
    suspend fun createEntry(
        title: String,
        content: String,
        mood: String? = null,
        tags: List<String> = emptyList(),
    ): JournalCreateResult
}

data class JournalCreateResult(
    val id: String?,
    val title: String,
)

class JournalNetworkException(
    message: String,
    val statusCode: Int? = null,
    cause: Throwable? = null,
) : Exception(message, cause)

class NetworkJournalRepository(
    private val baseUrl: String,
    private val token: String,
    private val transport: TodayHttpTransport = OkHttpTodayTransport(),
    private val json: Json = Json {
        ignoreUnknownKeys = true
        explicitNulls = false
    },
) : JournalRepository {
    override suspend fun createEntry(
        title: String,
        content: String,
        mood: String?,
        tags: List<String>,
    ): JournalCreateResult {
        val cleanTitle = validateJournalTitle(title)
        val cleanContent = validateJournalContent(content)
        val cleanMood = mood?.trim()?.take(50)?.takeIf(String::isNotBlank)
        val cleanTags = tags.mapNotNull { it.trim().takeIf(String::isNotBlank) }.take(10)

        val request = TodayHttpRequest(
            method = "POST",
            url = buildJournalCreateUrl(baseUrl),
            headers = mapOf(
                "Authorization" to "Bearer ${token.trim()}",
                "Content-Type" to "application/json",
                "Accept" to "application/json",
            ),
            body = json.encodeToString(
                JournalCreateRequest(
                    title = cleanTitle,
                    content = cleanContent,
                    mood = cleanMood,
                    tags = cleanTags,
                ),
            ),
        )
        val response = try {
            withContext(Dispatchers.IO) {
                transport.execute(request)
            }
        } catch (error: Exception) {
            throw JournalNetworkException("POST $JOURNAL_PATH failed", cause = error)
        }

        if (response.statusCode !in 200..299) {
            throw JournalNetworkException(
                message = buildFailureMessage(response),
                statusCode = response.statusCode,
            )
        }

        val createResponse = try {
            json.decodeFromString<JournalCreateResponse>(response.body)
        } catch (error: SerializationException) {
            throw JournalNetworkException("POST $JOURNAL_PATH returned invalid JSON", cause = error)
        } catch (error: IllegalArgumentException) {
            throw JournalNetworkException("POST $JOURNAL_PATH returned invalid JSON", cause = error)
        }

        val entry = createResponse.entry
            ?: throw JournalNetworkException("POST $JOURNAL_PATH returned invalid JSON")
        val returnedTitle = entry.title?.trim()?.takeIf(String::isNotBlank) ?: cleanTitle

        return JournalCreateResult(
            id = (entry.id ?: entry.mongoId)?.trim()?.takeIf(String::isNotBlank),
            title = returnedTitle,
        )
    }

    private fun buildFailureMessage(response: TodayHttpResponse): String {
        val fallback = "POST $JOURNAL_PATH failed with HTTP ${response.statusCode}"
        val backendMessage = parseSafeBackendMessage(response.body) ?: return fallback
        return "$fallback: $backendMessage"
    }

    private fun parseSafeBackendMessage(body: String): String? {
        val message = try {
            json.decodeFromString<JournalErrorResponse>(body).safeMessage()
        } catch (_: Exception) {
            null
        }?.trim().orEmpty()

        return message.takeIf(::isSafeBackendMessage)
    }

    private fun isSafeBackendMessage(message: String): Boolean {
        if (message.isBlank()) return false
        val lower = message.lowercase()
        return listOf("token", "password", "bearer", "http://", "https://", "/api/", "@", "{", "}").none {
            lower.contains(it)
        }
    }
}

fun buildJournalCreateUrl(baseUrl: String): String = baseUrl.trim().trimEnd('/') + JOURNAL_PATH

fun generatedJournalTitle(content: String): String {
    val firstLine = content
        .trim()
        .lineSequence()
        .firstOrNull { it.isNotBlank() }
        .orEmpty()
        .replace(Regex("\\s+"), " ")
    val title = firstLine.ifBlank { "Journal entry" }
    return title.take(JOURNAL_TITLE_MAX_LENGTH).trimEnd()
}

private fun validateJournalTitle(title: String): String {
    val cleanTitle = title.trim().replace(Regex("\\s+"), " ")
    require(cleanTitle.isNotBlank()) { "Journal title is required." }
    return cleanTitle.take(JOURNAL_TITLE_MAX_LENGTH).trimEnd()
}

private fun validateJournalContent(content: String): String {
    val cleanContent = content.trim()
    require(cleanContent.isNotBlank()) { "Journal content is required." }
    require(cleanContent.length <= JOURNAL_CONTENT_MAX_LENGTH) {
        "Journal content must be $JOURNAL_CONTENT_MAX_LENGTH characters or less."
    }
    return cleanContent
}

private const val JOURNAL_PATH = "/api/journal"
private const val JOURNAL_TITLE_MAX_LENGTH = 200
private const val JOURNAL_CONTENT_MAX_LENGTH = 5000

@Serializable
private data class JournalCreateRequest(
    val title: String,
    val content: String,
    val mood: String? = null,
    val tags: List<String> = emptyList(),
)

@Serializable
private data class JournalCreateResponse(
    val entry: JournalEntryDto? = null,
)

@Serializable
private data class JournalEntryDto(
    val id: String? = null,
    @SerialName("_id")
    val mongoId: String? = null,
    val title: String? = null,
)

@Serializable
private data class JournalErrorResponse(
    val msg: String? = null,
    val error: String? = null,
    val message: String? = null,
) {
    fun safeMessage(): String? = msg ?: error ?: message
}
