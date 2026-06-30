package com.transcendiverse.digitaltwin

import com.transcendiverse.digitaltwin.data.NetworkQuestRepository
import com.transcendiverse.digitaltwin.data.QuestNetworkException
import com.transcendiverse.digitaltwin.data.QuestSummary
import com.transcendiverse.digitaltwin.data.TodayHttpRequest
import com.transcendiverse.digitaltwin.data.TodayHttpResponse
import com.transcendiverse.digitaltwin.data.TodayHttpTransport
import com.transcendiverse.digitaltwin.data.resolveActiveQuest
import com.transcendiverse.digitaltwin.model.MobileQuest
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.net.URI

class NetworkQuestRepositoryTest {
    @Test
    fun buildsExpectedQuestListRequest() {
        val transport = RecordingTransport(TodayHttpResponse(200, questListJson()))
        val repository = NetworkQuestRepository(
            baseUrl = "https://api.example.test/",
            token = "test-token",
            transport = transport,
        )

        runBlocking { repository.listQuests() }

        val request = requireNotNull(transport.lastRequest)
        val uri = URI(request.url)
        assertEquals("GET", request.method)
        assertEquals("/api/quest/all", uri.path)
        assertEquals("limit=20&skip=0", uri.query)
        assertEquals("Bearer test-token", request.headers["Authorization"])
        assertEquals("application/json", request.headers["Accept"])
        assertNull(request.body)
    }

    @Test
    fun buildsExpectedProgressRequest() {
        val transport = RecordingTransport(TodayHttpResponse(200, progressJson(progress = 75)))
        val repository = NetworkQuestRepository(
            baseUrl = "https://api.example.test",
            token = "  trimmed-token  ",
            transport = transport,
        )

        runBlocking { repository.updateProgress("quest-123", 75) }

        val request = requireNotNull(transport.lastRequest)
        assertEquals("PUT", request.method)
        assertEquals("/api/quest/progress/quest-123", URI(request.url).path)
        assertEquals("Bearer trimmed-token", request.headers["Authorization"])
        assertEquals("application/json", request.headers["Content-Type"])
        assertEquals("application/json", request.headers["Accept"])

        val body = Json.parseToJsonElement(requireNotNull(request.body)).jsonObject
        assertEquals(setOf("progress"), body.keys)
        assertEquals("75", body.getValue("progress").jsonPrimitive.content)
    }

    @Test
    fun buildsExpectedCompleteRequest() {
        val transport = RecordingTransport(TodayHttpResponse(200, completeJson(completed = true)))
        val repository = NetworkQuestRepository(
            baseUrl = "https://api.example.test",
            token = "test-token",
            transport = transport,
        )

        runBlocking { repository.toggleComplete("quest-123") }

        val request = requireNotNull(transport.lastRequest)
        assertEquals("PUT", request.method)
        assertEquals("/api/quest/complete/quest-123", URI(request.url).path)
        assertEquals("Bearer test-token", request.headers["Authorization"])
        assertEquals("application/json", request.headers["Accept"])
        assertFalse(request.headers.containsKey("Content-Type"))
        assertEquals("", request.body)
    }

    @Test
    fun parsesListProgressAndArchivedCompleteResponses() {
        val listRepository = NetworkQuestRepository(
            baseUrl = "https://api.example.test",
            token = "test-token",
            transport = RecordingTransport(TodayHttpResponse(200, questListJson())),
        )
        val progressRepository = NetworkQuestRepository(
            baseUrl = "https://api.example.test",
            token = "test-token",
            transport = RecordingTransport(TodayHttpResponse(200, progressJson(progress = 80))),
        )
        val completeRepository = NetworkQuestRepository(
            baseUrl = "https://api.example.test",
            token = "test-token",
            transport = RecordingTransport(TodayHttpResponse(200, archivedCompleteJson())),
        )

        val quests = runBlocking { listRepository.listQuests() }
        val progress = runBlocking { progressRepository.updateProgress("quest-123", 80) }
        val complete = runBlocking { completeRepository.toggleComplete("quest-123") }

        assertEquals("quest-123", quests.first().id)
        assertEquals("Ship the smallest useful companion", quests.first().goal)
        assertEquals(40, quests.first().progress)
        assertFalse(quests.first().completed)
        assertEquals("Progress updated.", progress.message)
        assertEquals(80, progress.quest?.progress)
        assertEquals("Quest completed and archived.", complete.message)
        assertNull(complete.quest)
        assertTrue(complete.deleted)
    }

    @Test
    fun rejectsInvalidProgressWithoutNetworkCall() {
        val transport = RecordingTransport(TodayHttpResponse(200, progressJson(progress = 80)))
        val repository = NetworkQuestRepository(
            baseUrl = "https://api.example.test",
            token = "test-token",
            transport = transport,
        )

        val error = assertThrowsQuest<IllegalArgumentException> {
            runBlocking { repository.updateProgress("quest-123", 101) }
        }

        assertTrue(error.message.orEmpty().contains("0 to 100"))
        assertEquals(0, transport.callCount)
    }

    @Test
    fun rejectsBlankQuestIdWithoutNetworkCall() {
        val transport = RecordingTransport(TodayHttpResponse(200, progressJson(progress = 80)))
        val repository = NetworkQuestRepository(
            baseUrl = "https://api.example.test",
            token = "test-token",
            transport = transport,
        )

        val error = assertThrowsQuest<IllegalArgumentException> {
            runBlocking { repository.toggleComplete("   ") }
        }

        assertTrue(error.message.orEmpty().contains("Quest id is required"))
        assertEquals(0, transport.callCount)
    }

    @Test
    fun non2xxErrorTextDoesNotExposeSecretsOrRawJson() {
        val repository = NetworkQuestRepository(
            baseUrl = "https://api.example.test",
            token = "secret-token",
            transport = RecordingTransport(
                TodayHttpResponse(
                    400,
                    """{"msg":"bad","token":"secret-token","email":"alex@example.com","password":"plain"}""",
                ),
            ),
        )

        val error = assertThrowsQuest<QuestNetworkException> {
            runBlocking { repository.updateProgress("quest-123", 75) }
        }

        val rendered = error.toString().lowercase()
        assertTrue(rendered.contains("http 400"))
        listOf(
            "secret-token",
            "password",
            "alex@example.com",
            "api.example.test",
            "{",
            "}",
        ).forEach { forbidden ->
            assertFalse("Quest exception must not contain $forbidden", rendered.contains(forbidden))
        }
    }

    @Test
    fun resolvesMatchingCurrentQuestBeforeFirstIncompleteQuest() {
        val current = MobileQuest(
            goal = "Ship the smallest useful companion",
            duration = "daily",
            progress = 40,
        )
        val quests = listOf(
            QuestSummary(id = "first-incomplete", goal = "Different", duration = "daily", progress = 10, completed = false),
            QuestSummary(id = "matching", goal = current.goal, duration = current.duration, progress = 40, completed = false),
        )

        assertEquals("matching", resolveActiveQuest(current, quests)?.id)
    }

    @Test
    fun resolvesFirstIncompleteQuestWhenCurrentDoesNotMatch() {
        val current = MobileQuest(
            goal = "Ship the smallest useful companion",
            duration = "daily",
            progress = 40,
        )
        val quests = listOf(
            QuestSummary(id = "done", goal = "Done", duration = "daily", progress = 100, completed = true),
            QuestSummary(id = "first-incomplete", goal = "Different", duration = "weekly", progress = 10, completed = false),
        )

        assertEquals("first-incomplete", resolveActiveQuest(current, quests)?.id)
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

private fun questListJson(): String =
    """
    [
      {
        "_id": "quest-123",
        "goal": "Ship the smallest useful companion",
        "duration": "daily",
        "progress": 40,
        "completed": false,
        "date": "2026-06-30T00:00:00.000Z"
      }
    ]
    """.trimIndent()

private fun progressJson(progress: Int): String =
    """
    {
      "msg": "Progress updated.",
      "quest": {
        "_id": "quest-123",
        "goal": "Ship the smallest useful companion",
        "duration": "daily",
        "progress": $progress,
        "completed": ${progress >= 100}
      },
      "progression": null
    }
    """.trimIndent()

private fun completeJson(completed: Boolean): String =
    """
    {
      "msg": "${if (completed) "Quest completed." else "Quest reopened."}",
      "quest": {
        "_id": "quest-123",
        "goal": "Ship the smallest useful companion",
        "duration": "daily",
        "progress": ${if (completed) 100 else 0},
        "completed": $completed
      },
      "progression": {}
    }
    """.trimIndent()

private fun archivedCompleteJson(): String =
    """
    {
      "msg": "Quest completed and archived.",
      "quest": null,
      "progression": {},
      "deleted": true
    }
    """.trimIndent()

private inline fun <reified T : Throwable> assertThrowsQuest(block: () -> Unit): T {
    try {
        block()
    } catch (throwable: Throwable) {
        if (throwable is T) return throwable
        throw AssertionError("Expected ${T::class.java.name}, got ${throwable::class.java.name}", throwable)
    }
    throw AssertionError("Expected ${T::class.java.name}")
}
