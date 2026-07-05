package com.transcendiverse.digitaltwin

import com.transcendiverse.digitaltwin.data.LoginNetworkException
import com.transcendiverse.digitaltwin.data.NetworkLoginRepository
import com.transcendiverse.digitaltwin.data.TodayHttpRequest
import com.transcendiverse.digitaltwin.data.TodayHttpResponse
import com.transcendiverse.digitaltwin.data.TodayHttpTransport
import com.transcendiverse.digitaltwin.data.buildLoginUrl
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.net.URI

class NetworkLoginRepositoryTest {
    @Test
    fun buildsExpectedLoginRequest() {
        val transport = RecordingTransport(
            TodayHttpResponse(200, loginSuccessJson(token = "jwt-token")),
        )
        val repository = NetworkLoginRepository(transport = transport)

        runBlocking {
            repository.login(
                baseUrl = "https://api.example.test/",
                email = "user@example.com",
                password = "plain text password entered by user",
            )
        }

        val request = requireNotNull(transport.lastRequest)
        assertEquals("POST", request.method)
        assertEquals("/api/auth/login", URI(request.url).path)
        assertEquals("application/json", request.headers["Content-Type"])
        assertEquals("application/json", request.headers["Accept"])

        val body = Json.parseToJsonElement(requireNotNull(request.body)).jsonObject
        assertEquals(setOf("email", "password"), body.keys)
        assertEquals("user@example.com", body.getValue("email").jsonPrimitive.content)
        assertEquals("plain text password entered by user", body.getValue("password").jsonPrimitive.content)
    }

    @Test
    fun parsesSuccessResponseTokenAndUser() {
        val repository = NetworkLoginRepository(
            transport = RecordingTransport(TodayHttpResponse(200, loginSuccessJson(token = "jwt-token"))),
        )

        val response = runBlocking {
            repository.login(
                baseUrl = "https://api.example.test",
                email = "user@example.com",
                password = "secret",
            )
        }

        assertEquals("jwt-token", response.token)
        assertEquals("mongo-user-id", response.user.id)
        assertEquals("user@example.com", response.user.email)
        assertEquals("User Name", response.user.name)
    }

    @Test
    fun rejectsNon2xxStatusWithPathAndStatusCode() {
        val repository = NetworkLoginRepository(
            transport = RecordingTransport(TodayHttpResponse(401, """{"error":"Invalid credentials"}""")),
        )

        val error = assertThrowsLogin<LoginNetworkException> {
            runBlocking {
                repository.login(
                    baseUrl = "https://api.example.test",
                    email = "user@example.com",
                    password = "wrong",
                )
            }
        }

        assertTrue(error.message.orEmpty().contains("/api/auth/login"))
        assertTrue(error.message.orEmpty().contains("HTTP 401"))
    }

    @Test
    fun rejectsBlankTokenInSuccessResponse() {
        val repository = NetworkLoginRepository(
            transport = RecordingTransport(TodayHttpResponse(200, loginSuccessJson(token = "   "))),
        )

        val error = assertThrowsLogin<LoginNetworkException> {
            runBlocking {
                repository.login(
                    baseUrl = "https://api.example.test",
                    email = "user@example.com",
                    password = "secret",
                )
            }
        }

        assertTrue(error.message.orEmpty().contains("blank token"))
    }

    @Test
    fun transportErrorTextDoesNotExposeCredentialsOrConnectionDetails() {
        val repository = NetworkLoginRepository(
            transport = ThrowingTransport("https://api.example.test user@example.com plain text password"),
        )

        val error = assertThrowsLogin<LoginNetworkException> {
            runBlocking {
                repository.login(
                    baseUrl = "https://api.example.test",
                    email = "user@example.com",
                    password = "plain text password",
                )
            }
        }

        val rendered = error.toString().lowercase()
        assertTrue(rendered.contains("/api/auth/login"))
        listOf(
            "plain text password",
            "user@example.com",
            "api.example.test",
            "https://",
        ).forEach { forbidden ->
            assertFalse("Login exception must not contain $forbidden", rendered.contains(forbidden))
        }
    }

    @Test
    fun invalidJsonErrorTextDoesNotExposeRawPayload() {
        val repository = NetworkLoginRepository(
            transport = RecordingTransport(TodayHttpResponse(200, """{"token":"secret-token","user":""")),
        )

        val error = assertThrowsLogin<LoginNetworkException> {
            runBlocking {
                repository.login(
                    baseUrl = "https://api.example.test",
                    email = "user@example.com",
                    password = "plain text password",
                )
            }
        }

        val rendered = error.toString().lowercase()
        assertTrue(rendered.contains("invalid json"))
        assertFalse(rendered.contains("secret-token"))
        assertFalse(rendered.contains("user@example.com"))
        assertFalse(rendered.contains("plain text password"))
        assertFalse(rendered.contains("{"))
    }

    @Test
    fun loginUrlTrimsTrailingSlash() {
        assertEquals("https://api.example.test/api/auth/login", buildLoginUrl("https://api.example.test/"))
        assertEquals("https://api.example.test/api/auth/login", buildLoginUrl(" https://api.example.test/// "))
        assertFalse(buildLoginUrl("https://api.example.test/").contains("//api/auth/login"))
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

private fun loginSuccessJson(token: String): String =
    """
    {
      "token": "$token",
      "user": {
        "id": "mongo-user-id",
        "email": "user@example.com",
        "name": "User Name"
      }
    }
    """.trimIndent()

private inline fun <reified T : Throwable> assertThrowsLogin(block: () -> Unit): T {
    try {
        block()
    } catch (throwable: Throwable) {
        if (throwable is T) return throwable
        throw AssertionError("Expected ${T::class.java.name}, got ${throwable::class.java.name}", throwable)
    }
    throw AssertionError("Expected ${T::class.java.name}")
}
