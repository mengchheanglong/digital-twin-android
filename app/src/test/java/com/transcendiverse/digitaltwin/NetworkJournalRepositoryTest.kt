package com.transcendiverse.digitaltwin

import com.transcendiverse.digitaltwin.data.JournalNetworkException
import com.transcendiverse.digitaltwin.data.NetworkJournalRepository
import com.transcendiverse.digitaltwin.data.TodayHttpRequest
import com.transcendiverse.digitaltwin.data.TodayHttpResponse
import com.transcendiverse.digitaltwin.data.TodayHttpTransport
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.net.URI

class NetworkJournalRepositoryTest {
    @Test
    fun buildsExpectedCreateRequest() {
        val transport = RecordingTransport(TodayHttpResponse(201, journalSuccessJson()))
        val repository = NetworkJournalRepository(
            baseUrl = "https://api.example.test/",
            token = "  test-token  ",
            transport = transport,
        )

        runBlocking {
            repository.createEntry(
                title = "  First line  ",
                content = "  First line\n\nBody signal  ",
                mood = " focused ",
                tags = listOf(" mobile ", "", " sync "),
            )
        }

        val request = requireNotNull(transport.lastRequest)
        assertEquals("POST", request.method)
        assertEquals("/api/journal", URI(request.url).path)
        assertEquals("Bearer test-token", request.headers["Authorization"])
        assertEquals("application/json", request.headers["Content-Type"])
        assertEquals("application/json", request.headers["Accept"])

        val body = Json.parseToJsonElement(requireNotNull(request.body)).jsonObject
        assertEquals(setOf("title", "content", "mood", "tags"), body.keys)
        assertEquals("First line", body.getValue("title").jsonPrimitive.content)
        assertEquals("First line\n\nBody signal", body.getValue("content").jsonPrimitive.content)
        assertEquals("focused", body.getValue("mood").jsonPrimitive.content)
        assertEquals(
            listOf("mobile", "sync"),
            body.getValue("tags").jsonArray.map { it.jsonPrimitive.content },
        )
    }

    @Test
    fun rejectsBlankTitleWithoutNetworkCall() {
        val transport = RecordingTransport(TodayHttpResponse(201, journalSuccessJson()))
        val repository = NetworkJournalRepository(
            baseUrl = "https://api.example.test",
            token = "test-token",
            transport = transport,
        )

        val error = assertThrowsJournal<IllegalArgumentException> {
            runBlocking { repository.createEntry(title = "  ", content = "Signal") }
        }

        assertTrue(error.message.orEmpty().contains("Journal title is required"))
        assertEquals(0, transport.callCount)
    }

    @Test
    fun rejectsBlankContentWithoutNetworkCall() {
        val transport = RecordingTransport(TodayHttpResponse(201, journalSuccessJson()))
        val repository = NetworkJournalRepository(
            baseUrl = "https://api.example.test",
            token = "test-token",
            transport = transport,
        )

        val error = assertThrowsJournal<IllegalArgumentException> {
            runBlocking { repository.createEntry(title = "Signal", content = "  ") }
        }

        assertTrue(error.message.orEmpty().contains("Journal content is required"))
        assertEquals(0, transport.callCount)
    }

    @Test
    fun rejectsOverlongContentWithoutNetworkCall() {
        val transport = RecordingTransport(TodayHttpResponse(201, journalSuccessJson()))
        val repository = NetworkJournalRepository(
            baseUrl = "https://api.example.test",
            token = "test-token",
            transport = transport,
        )

        val error = assertThrowsJournal<IllegalArgumentException> {
            runBlocking { repository.createEntry(title = "Signal", content = "x".repeat(5001)) }
        }

        assertTrue(error.message.orEmpty().contains("5000 characters or less"))
        assertEquals(0, transport.callCount)
    }

    @Test
    fun parses201SuccessResponse() {
        val repository = NetworkJournalRepository(
            baseUrl = "https://api.example.test",
            token = "test-token",
            transport = RecordingTransport(TodayHttpResponse(201, journalSuccessJson())),
        )

        val result = runBlocking {
            repository.createEntry(title = "First line", content = "First line\n\nBody signal")
        }

        assertEquals("journal-123", result.id)
        assertEquals("First line", result.title)
    }

    @Test
    fun non2xxErrorTextDoesNotExposeSecretsOrRawPayloads() {
        val repository = NetworkJournalRepository(
            baseUrl = "https://api.example.test",
            token = "secret-token",
            transport = RecordingTransport(
                TodayHttpResponse(
                    400,
                    """{"msg":"bad","token":"secret-token","email":"alex@example.com","password":"plain","url":"https://api.example.test/api/journal"}""",
                ),
            ),
        )

        val error = assertThrowsJournal<JournalNetworkException> {
            runBlocking { repository.createEntry(title = "Signal", content = "Body") }
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
            assertFalse("Journal exception must not contain $forbidden", rendered.contains(forbidden))
        }
    }

    @Test
    fun invalidJsonThrowsSafeJournalException() {
        val repository = NetworkJournalRepository(
            baseUrl = "https://api.example.test",
            token = "secret-token",
            transport = RecordingTransport(TodayHttpResponse(201, """{"entry":""")),
        )

        val error = assertThrowsJournal<JournalNetworkException> {
            runBlocking { repository.createEntry(title = "Signal", content = "Body") }
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

private fun journalSuccessJson(): String =
    """
    {
      "entry": {
        "_id": "journal-123",
        "title": "First line",
        "content": "First line\n\nBody signal",
        "mood": "focused",
        "tags": ["mobile", "sync"]
      }
    }
    """.trimIndent()

private inline fun <reified T : Throwable> assertThrowsJournal(block: () -> Unit): T {
    try {
        block()
    } catch (throwable: Throwable) {
        if (throwable is T) return throwable
        throw AssertionError("Expected ${T::class.java.name}, got ${throwable::class.java.name}", throwable)
    }
    throw AssertionError("Expected ${T::class.java.name}")
}
