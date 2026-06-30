package com.transcendiverse.digitaltwin

import com.transcendiverse.digitaltwin.ui.safeStatusErrorMessage
import com.transcendiverse.digitaltwin.ui.validateLoginInput
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class TodayLoginValidationTest {
    @Test
    fun rejectsBlankBaseUrlBeforeLogin() {
        assertEquals(
            "Backend base URL is required",
            validateLoginInput(baseUrl = " ", email = "user@example.com", password = "secret"),
        )
    }

    @Test
    fun rejectsBlankEmailBeforeLogin() {
        assertEquals(
            "Email is required",
            validateLoginInput(baseUrl = "https://api.example.test", email = "", password = "secret"),
        )
    }

    @Test
    fun rejectsBlankPasswordBeforeLogin() {
        assertEquals(
            "Password is required",
            validateLoginInput(baseUrl = "https://api.example.test", email = "user@example.com", password = " "),
        )
    }

    @Test
    fun acceptsCompleteLoginInput() {
        assertNull(
            validateLoginInput(
                baseUrl = "https://api.example.test",
                email = "user@example.com",
                password = "secret",
            ),
        )
    }

    @Test
    fun statusErrorSanitizerHidesPrivateBackendAndJsonDetails() {
        val message = """
            token abc password secret email user@example.com
            backend https://digital-twin-orcin-omega.vercel.app
            {"token":"abc","password":"secret","email":"user@example.com"}
        """.trimIndent()

        assertEquals(
            "Unable to sign in",
            safeStatusErrorMessage(Exception(message), "Unable to sign in"),
        )
    }

    @Test
    fun statusErrorSanitizerKeepsPlainOperationalDetails() {
        assertEquals(
            "HTTP 503",
            safeStatusErrorMessage(Exception("HTTP 503"), "Unable to load Today"),
        )
    }
}
