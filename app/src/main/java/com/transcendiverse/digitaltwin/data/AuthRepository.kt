package com.transcendiverse.digitaltwin.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

interface LoginRepository {
    suspend fun login(baseUrl: String, email: String, password: String): LoginResponse
}

@Serializable
data class LoginResponse(
    val token: String,
    val user: LoginUser,
)

@Serializable
data class LoginUser(
    val id: String,
    val email: String,
    val name: String,
)

class LoginNetworkException(
    message: String,
    cause: Throwable? = null,
) : Exception(message, cause)

class NetworkLoginRepository(
    private val transport: TodayHttpTransport = OkHttpTodayTransport(),
    private val json: Json = Json {
        ignoreUnknownKeys = true
        explicitNulls = false
    },
) : LoginRepository {
    override suspend fun login(baseUrl: String, email: String, password: String): LoginResponse {
        val request = TodayHttpRequest(
            method = "POST",
            url = buildLoginUrl(baseUrl),
            headers = mapOf(
                "Content-Type" to "application/json",
                "Accept" to "application/json",
            ),
            body = json.encodeToString(LoginRequest(email = email, password = password)),
        )
        val response = try {
            withContext(Dispatchers.IO) {
                transport.execute(request)
            }
        } catch (error: Exception) {
            throw LoginNetworkException("POST $LOGIN_PATH failed: ${error.message}", error)
        }

        if (response.statusCode !in 200..299) {
            throw LoginNetworkException("POST $LOGIN_PATH failed with HTTP ${response.statusCode}")
        }

        val loginResponse = try {
            json.decodeFromString<LoginResponse>(response.body)
        } catch (error: SerializationException) {
            throw LoginNetworkException("POST $LOGIN_PATH returned invalid JSON: ${error.message}", error)
        } catch (error: IllegalArgumentException) {
            throw LoginNetworkException("POST $LOGIN_PATH returned invalid JSON: ${error.message}", error)
        }

        if (loginResponse.token.isBlank()) {
            throw LoginNetworkException("POST $LOGIN_PATH returned blank token")
        }

        return loginResponse.copy(token = loginResponse.token.trim())
    }
}

fun buildLoginUrl(baseUrl: String): String = baseUrl.trim().trimEnd('/') + LOGIN_PATH

private const val LOGIN_PATH = "/api/auth/login"

@Serializable
private data class LoginRequest(
    val email: String,
    val password: String,
)
