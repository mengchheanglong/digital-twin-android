package com.transcendiverse.digitaltwin

import com.transcendiverse.digitaltwin.data.NetworkTodayRepository
import com.transcendiverse.digitaltwin.data.TodayFixture
import com.transcendiverse.digitaltwin.data.TodayHttpRequest
import com.transcendiverse.digitaltwin.data.TodayHttpResponse
import com.transcendiverse.digitaltwin.data.TodayHttpTransport
import com.transcendiverse.digitaltwin.data.TodayNetworkException
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.net.URI

class NetworkTodayRepositoryTest {
    @Test
    fun buildsExpectedMobileTodayRequest() {
        val transport = RecordingTransport(TodayHttpResponse(200, TodayFixture.json))
        val repository = NetworkTodayRepository(
            baseUrl = "https://api.example.test/",
            token = "test-token",
            transport = transport,
        )

        runBlocking { repository.getToday() }

        val request = requireNotNull(transport.lastRequest)
        assertEquals("GET", request.method)
        assertEquals("/api/mobile/today", URI(request.url).path)
        assertEquals("Bearer test-token", request.headers["Authorization"])
        assertEquals("application/json", request.headers["Accept"])
    }

    @Test
    fun parsesSuccessEnvelopeAndReturnsToday() {
        val repository = NetworkTodayRepository(
            baseUrl = "https://api.example.test",
            token = "test-token",
            transport = RecordingTransport(TodayHttpResponse(200, TodayFixture.json)),
        )

        val today = runBlocking { repository.getToday() }

        assertEquals("mobile-today.v0", today.version)
        assertEquals("Alex", today.user.name)
        assertEquals("rising", today.insight.trend)
    }

    @Test
    fun reportsClearErrorOnNon2xxStatus() {
        val repository = NetworkTodayRepository(
            baseUrl = "https://api.example.test",
            token = "test-token",
            transport = RecordingTransport(TodayHttpResponse(503, """{"error":"down"}""")),
        )

        val error = assertThrowsCompat<TodayNetworkException> {
            runBlocking { repository.getToday() }
        }

        assertTrue(error.message.orEmpty().contains("HTTP 503"))
        assertTrue(error.message.orEmpty().contains("/api/mobile/today"))
    }

    @Test
    fun transportErrorTextDoesNotExposeSecretsOrConnectionDetails() {
        val repository = NetworkTodayRepository(
            baseUrl = "https://api.example.test",
            token = "secret-token",
            transport = ThrowingTransport("https://api.example.test secret-token alex@example.com password"),
        )

        val error = assertThrowsCompat<TodayNetworkException> {
            runBlocking { repository.getToday() }
        }

        val rendered = error.toString().lowercase()
        assertTrue(rendered.contains("/api/mobile/today"))
        listOf(
            "secret-token",
            "password",
            "alex@example.com",
            "api.example.test",
            "https://",
        ).forEach { forbidden ->
            assertFalse("Today exception must not contain $forbidden", rendered.contains(forbidden))
        }
    }

    @Test
    fun invalidJsonErrorTextDoesNotExposeRawPayload() {
        val repository = NetworkTodayRepository(
            baseUrl = "https://api.example.test",
            token = "secret-token",
            transport = RecordingTransport(TodayHttpResponse(200, """{"token":"secret-token","today":""")),
        )

        val error = assertThrowsCompat<TodayNetworkException> {
            runBlocking { repository.getToday() }
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

        override fun execute(request: TodayHttpRequest): TodayHttpResponse {
            lastRequest = request
            return response
        }
    }

    private class ThrowingTransport(
        private val message: String,
    ) : TodayHttpTransport {
        override fun execute(request: TodayHttpRequest): TodayHttpResponse {
            throw IllegalStateException(message)
        }
    }
}

private inline fun <reified T : Throwable> assertThrowsCompat(block: () -> Unit): T {
    try {
        block()
    } catch (throwable: Throwable) {
        if (throwable is T) return throwable
        throw AssertionError("Expected ${T::class.java.name}, got ${throwable::class.java.name}", throwable)
    }
    throw AssertionError("Expected ${T::class.java.name}")
}
