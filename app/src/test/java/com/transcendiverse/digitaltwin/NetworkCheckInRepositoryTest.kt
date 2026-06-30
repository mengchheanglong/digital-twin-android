package com.transcendiverse.digitaltwin

import com.transcendiverse.digitaltwin.data.CheckInNetworkException
import com.transcendiverse.digitaltwin.data.NetworkCheckInRepository
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

class NetworkCheckInRepositoryTest {
    @Test
    fun buildsExpectedSubmitRequest() {
        val transport = RecordingTransport(TodayHttpResponse(200, submitSuccessJson(80)))
        val repository = NetworkCheckInRepository(
            baseUrl = "https://api.example.test/",
            token = "test-token",
            transport = transport,
        )

        runBlocking { repository.submitDaily(listOf(4, 4, 4, 4, 4)) }

        val request = requireNotNull(transport.lastRequest)
        assertEquals("POST", request.method)
        assertEquals("/api/checkin/submit", URI(request.url).path)
        assertEquals("Bearer test-token", request.headers["Authorization"])
        assertEquals("application/json", request.headers["Content-Type"])
        assertEquals("application/json", request.headers["Accept"])

        val body = Json.parseToJsonElement(requireNotNull(request.body)).jsonObject
        assertEquals(setOf("ratings"), body.keys)
        assertEquals(listOf("4", "4", "4", "4", "4"), body.getValue("ratings").jsonArray.map { it.jsonPrimitive.content })
    }

    @Test
    fun trimsTokenInAuthorizationHeader() {
        val transport = RecordingTransport(TodayHttpResponse(200, submitSuccessJson(60)))
        val repository = NetworkCheckInRepository(
            baseUrl = "https://api.example.test",
            token = "  trimmed-token  ",
            transport = transport,
        )

        runBlocking { repository.submitDaily(listOf(3, 3, 3, 3, 3)) }

        assertEquals("Bearer trimmed-token", requireNotNull(transport.lastRequest).headers["Authorization"])
    }

    @Test
    fun rejectsWrongRatingCountWithoutNetworkCall() {
        val transport = RecordingTransport(TodayHttpResponse(200, submitSuccessJson(60)))
        val repository = NetworkCheckInRepository(
            baseUrl = "https://api.example.test",
            token = "test-token",
            transport = transport,
        )

        val error = assertThrowsCheckIn<IllegalArgumentException> {
            runBlocking { repository.submitDaily(listOf(3, 3, 3, 3)) }
        }

        assertTrue(error.message.orEmpty().contains("exactly 5"))
        assertEquals(0, transport.callCount)
    }

    @Test
    fun rejectsOutOfRangeRatingWithoutNetworkCall() {
        val transport = RecordingTransport(TodayHttpResponse(200, submitSuccessJson(60)))
        val repository = NetworkCheckInRepository(
            baseUrl = "https://api.example.test",
            token = "test-token",
            transport = transport,
        )

        val error = assertThrowsCheckIn<IllegalArgumentException> {
            runBlocking { repository.submitDaily(listOf(3, 3, 0, 3, 3)) }
        }

        assertTrue(error.message.orEmpty().contains("1 to 5"))
        assertEquals(0, transport.callCount)
    }

    @Test
    fun parsesSuccessPercentageAndMessage() {
        val repository = NetworkCheckInRepository(
            baseUrl = "https://api.example.test",
            token = "test-token",
            transport = RecordingTransport(TodayHttpResponse(200, submitSuccessJson(80))),
        )

        val result = runBlocking { repository.submitDaily(listOf(4, 4, 4, 4, 4)) }

        assertEquals("Check-in submitted.", result.message)
        assertEquals(20, result.totalScore)
        assertEquals(25, result.maxScore)
        assertEquals(80, result.percentage)
    }

    @Test
    fun mapsDuplicateCheckInToSafeStatusAndMessage() {
        val repository = NetworkCheckInRepository(
            baseUrl = "https://api.example.test",
            token = "test-token",
            transport = RecordingTransport(TodayHttpResponse(400, """{"msg":"Daily check-in already completed."}""")),
        )

        val error = assertThrowsCheckIn<CheckInNetworkException> {
            runBlocking { repository.submitDaily(listOf(3, 3, 3, 3, 3)) }
        }

        assertEquals(400, error.statusCode)
        assertTrue(error.message.orEmpty().contains("HTTP 400"))
        assertTrue(error.message.orEmpty().contains("Daily check-in already completed."))
    }

    @Test
    fun exceptionTextDoesNotExposeSecretsOrRawJson() {
        val repository = NetworkCheckInRepository(
            baseUrl = "https://api.example.test",
            token = "secret-token",
            transport = RecordingTransport(
                TodayHttpResponse(
                    400,
                    """{"msg":"bad","token":"secret-token","email":"alex@example.com","password":"plain"}""",
                ),
            ),
        )

        val error = assertThrowsCheckIn<CheckInNetworkException> {
            runBlocking { repository.submitDaily(listOf(3, 3, 3, 3, 3)) }
        }

        val rendered = error.toString().lowercase()
        listOf(
            "secret-token",
            "password",
            "alex@example.com",
            "api.example.test",
            "{",
            "}",
        ).forEach { forbidden ->
            assertFalse("Check-in exception must not contain $forbidden", rendered.contains(forbidden))
        }
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

private fun submitSuccessJson(percentage: Int): String =
    """
    {
      "msg": "Check-in submitted.",
      "result": {
        "totalScore": ${percentage / 4},
        "maxScore": 25,
        "percentage": $percentage
      },
      "progression": {}
    }
    """.trimIndent()

private inline fun <reified T : Throwable> assertThrowsCheckIn(block: () -> Unit): T {
    try {
        block()
    } catch (throwable: Throwable) {
        if (throwable is T) return throwable
        throw AssertionError("Expected ${T::class.java.name}, got ${throwable::class.java.name}", throwable)
    }
    throw AssertionError("Expected ${T::class.java.name}")
}
