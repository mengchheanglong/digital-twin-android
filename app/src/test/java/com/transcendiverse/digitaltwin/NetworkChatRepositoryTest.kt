package com.transcendiverse.digitaltwin

import com.transcendiverse.digitaltwin.data.ChatNetworkException
import com.transcendiverse.digitaltwin.data.NetworkChatRepository
import com.transcendiverse.digitaltwin.data.TodayHttpRequest
import com.transcendiverse.digitaltwin.data.TodayHttpResponse
import com.transcendiverse.digitaltwin.data.TodayHttpTransport
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.net.URI

class NetworkChatRepositoryTest {
    @Test
    fun buildsExpectedSendRequest() {
        val transport = RecordingTransport(TodayHttpResponse(200, chatSuccessJson()))
        val repository = NetworkChatRepository(
            baseUrl = "https://api.example.test/",
            token = "  test-token  ",
            transport = transport,
        )

        runBlocking {
            repository.sendMessage(
                message = "  What should I do next?  ",
                chatId = "  chat-123  ",
            )
        }

        val request = requireNotNull(transport.lastRequest)
        assertEquals("POST", request.method)
        assertEquals("/api/chat/send", URI(request.url).path)
        assertEquals("Bearer test-token", request.headers["Authorization"])
        assertEquals("application/json", request.headers["Content-Type"])
        assertEquals("application/json", request.headers["Accept"])

        val body = Json.parseToJsonElement(requireNotNull(request.body)).jsonObject
        assertEquals(setOf("message", "chatId"), body.keys)
        assertEquals("What should I do next?", body.getValue("message").jsonPrimitive.content)
        assertEquals("chat-123", body.getValue("chatId").jsonPrimitive.content)
    }

    @Test
    fun omitsBlankChatId() {
        val transport = RecordingTransport(TodayHttpResponse(200, chatSuccessJson()))
        val repository = NetworkChatRepository(
            baseUrl = "https://api.example.test",
            token = "test-token",
            transport = transport,
        )

        runBlocking { repository.sendMessage(message = "Hello", chatId = "  ") }

        val body = Json.parseToJsonElement(requireNotNull(transport.lastRequest?.body)).jsonObject
        assertEquals(setOf("message"), body.keys)
    }

    @Test
    fun rejectsBlankMessageWithoutNetworkCall() {
        val transport = RecordingTransport(TodayHttpResponse(200, chatSuccessJson()))
        val repository = NetworkChatRepository(
            baseUrl = "https://api.example.test",
            token = "test-token",
            transport = transport,
        )

        val error = assertThrowsChat<IllegalArgumentException> {
            runBlocking { repository.sendMessage(message = "  ") }
        }

        assertTrue(error.message.orEmpty().contains("Message is required"))
        assertEquals(0, transport.callCount)
    }

    @Test
    fun parsesSuccessResponse() {
        val repository = NetworkChatRepository(
            baseUrl = "https://api.example.test",
            token = "test-token",
            transport = RecordingTransport(TodayHttpResponse(200, chatSuccessJson())),
        )

        val result = runBlocking { repository.sendMessage(message = "Hello") }

        assertEquals("Start with a two-minute check-in.", result.reply)
        assertEquals("chat-123", result.chatId)
    }

    @Test
    fun rejectsBlankReply() {
        val repository = NetworkChatRepository(
            baseUrl = "https://api.example.test",
            token = "test-token",
            transport = RecordingTransport(TodayHttpResponse(200, """{"reply":"  ","chatId":"chat-123"}""")),
        )

        val error = assertThrowsChat<ChatNetworkException> {
            runBlocking { repository.sendMessage(message = "Hello") }
        }

        assertTrue(error.message.orEmpty().contains("blank reply"))
    }

    @Test
    fun rejectsBlankChatIdInSuccessResponse() {
        val repository = NetworkChatRepository(
            baseUrl = "https://api.example.test",
            token = "test-token",
            transport = RecordingTransport(TodayHttpResponse(200, """{"reply":"Hi","chatId":"  "}""")),
        )

        val error = assertThrowsChat<ChatNetworkException> {
            runBlocking { repository.sendMessage(message = "Hello") }
        }

        assertTrue(error.message.orEmpty().contains("blank chat id"))
    }

    @Test
    fun non2xxErrorTextDoesNotExposeSecretsOrRawPayloads() {
        val repository = NetworkChatRepository(
            baseUrl = "https://api.example.test",
            token = "secret-token",
            transport = RecordingTransport(
                TodayHttpResponse(
                    400,
                    """{"msg":"bad","token":"secret-token","email":"alex@example.com","password":"plain","url":"https://api.example.test/api/chat/send"}""",
                ),
            ),
        )

        val error = assertThrowsChat<ChatNetworkException> {
            runBlocking { repository.sendMessage(message = "Hello") }
        }

        val rendered = error.toString().lowercase()
        assertEquals(400, error.statusCode)
        assertTrue(rendered.contains("http 400"))
        listOf(
            "secret-token",
            "password",
            "alex@example.com",
            "api.example.test",
            "https://",
            "{",
            "}",
        ).forEach { forbidden ->
            assertFalse("Chat exception must not contain $forbidden", rendered.contains(forbidden))
        }
    }

    @Test
    fun invalidJsonThrowsSafeChatException() {
        val repository = NetworkChatRepository(
            baseUrl = "https://api.example.test",
            token = "secret-token",
            transport = RecordingTransport(TodayHttpResponse(200, """{"reply":""")),
        )

        val error = assertThrowsChat<ChatNetworkException> {
            runBlocking { repository.sendMessage(message = "Hello") }
        }

        val rendered = error.toString().lowercase()
        assertTrue(rendered.contains("invalid json"))
        assertFalse(rendered.contains("secret-token"))
        assertFalse(rendered.contains("api.example.test"))
        assertFalse(rendered.contains("{"))
    }

    private class RecordingTransport(
        private val response: TodayHttpResponse,
    ) : TodayHttpTransport {
        var lastRequest: TodayHttpRequest? = null
        var callCount = 0

        override fun execute(request: TodayHttpRequest): TodayHttpResponse {
            callCount += 1
            lastRequest = request
            return response
        }
    }
}

private fun chatSuccessJson(): String =
    """
    {
      "reply": "Start with a two-minute check-in.",
      "chatId": "chat-123"
    }
    """.trimIndent()

private inline fun <reified T : Throwable> assertThrowsChat(block: () -> Unit): T {
    try {
        block()
    } catch (throwable: Throwable) {
        if (throwable is T) return throwable
        throw AssertionError("Expected ${T::class.java.name}, got ${throwable::class.java.name}", throwable)
    }
    throw AssertionError("Expected ${T::class.java.name}")
}
