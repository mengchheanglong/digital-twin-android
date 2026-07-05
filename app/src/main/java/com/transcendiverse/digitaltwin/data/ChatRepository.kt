package com.transcendiverse.digitaltwin.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

interface ChatRepository {
    suspend fun sendMessage(message: String, chatId: String? = null): ChatSendResult
}

data class ChatSendResult(
    val reply: String,
    val chatId: String,
)

class ChatNetworkException(
    message: String,
    val statusCode: Int? = null,
    cause: Throwable? = null,
) : Exception(message, cause)

class NetworkChatRepository(
    private val baseUrl: String,
    private val token: String,
    private val transport: TodayHttpTransport = OkHttpTodayTransport(),
    private val json: Json = Json {
        ignoreUnknownKeys = true
        explicitNulls = false
    },
) : ChatRepository {
    override suspend fun sendMessage(message: String, chatId: String?): ChatSendResult {
        val cleanMessage = message.trim()
        require(cleanMessage.isNotBlank()) { "Message is required" }

        val request = TodayHttpRequest(
            method = "POST",
            url = buildChatSendUrl(baseUrl),
            headers = mapOf(
                "Authorization" to "Bearer ${token.trim()}",
                "Content-Type" to "application/json",
                "Accept" to "application/json",
            ),
            body = json.encodeToString(
                ChatSendRequest(
                    message = cleanMessage,
                    chatId = chatId?.trim()?.takeIf(String::isNotBlank),
                ),
            ),
        )
        val response = try {
            withContext(Dispatchers.IO) {
                transport.execute(request)
            }
        } catch (error: Exception) {
            throw ChatNetworkException("POST $CHAT_SEND_PATH failed", cause = error)
        }

        if (response.statusCode !in 200..299) {
            throw ChatNetworkException(
                message = buildFailureMessage(response),
                statusCode = response.statusCode,
            )
        }

        val sendResponse = try {
            json.decodeFromString<ChatSendResponse>(response.body)
        } catch (error: SerializationException) {
            throw ChatNetworkException("POST $CHAT_SEND_PATH returned invalid JSON", cause = error)
        } catch (error: IllegalArgumentException) {
            throw ChatNetworkException("POST $CHAT_SEND_PATH returned invalid JSON", cause = error)
        }

        val reply = sendResponse.reply.trim()
        val returnedChatId = sendResponse.chatId.trim()
        if (reply.isBlank()) {
            throw ChatNetworkException("POST $CHAT_SEND_PATH returned blank reply")
        }
        if (returnedChatId.isBlank()) {
            throw ChatNetworkException("POST $CHAT_SEND_PATH returned blank chat id")
        }

        return ChatSendResult(reply = reply, chatId = returnedChatId)
    }

    private fun buildFailureMessage(response: TodayHttpResponse): String {
        val fallback = "POST $CHAT_SEND_PATH failed with HTTP ${response.statusCode}"
        val backendMessage = parseSafeBackendMessage(response.body) ?: return fallback
        return "$fallback: $backendMessage"
    }

    private fun parseSafeBackendMessage(body: String): String? {
        val message = try {
            json.decodeFromString<ChatErrorResponse>(body).safeMessage()
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

fun buildChatSendUrl(baseUrl: String): String = baseUrl.trim().trimEnd('/') + CHAT_SEND_PATH

private const val CHAT_SEND_PATH = "/api/chat/send"

@Serializable
private data class ChatSendRequest(
    val message: String,
    val chatId: String? = null,
)

@Serializable
private data class ChatSendResponse(
    val reply: String,
    val chatId: String,
)

@Serializable
private data class ChatErrorResponse(
    val msg: String? = null,
    val error: String? = null,
) {
    fun safeMessage(): String? = msg ?: error
}
