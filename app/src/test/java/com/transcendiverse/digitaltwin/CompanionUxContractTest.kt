package com.transcendiverse.digitaltwin

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CompanionUxContractTest {
    @Test
    fun companionShellUsesDarkPagerPages() {
        val screen = source("app/src/main/java/com/transcendiverse/digitaltwin/ui/TodayScreen.kt")

        assertTrue(screen.contains("HorizontalPager"))
        assertTrue(screen.contains("rememberPagerState"))
        assertTrue(screen.contains("listOf(\"Today\", \"Ask\", \"Journal\", \"Quest\", \"Me\")"))
        assertTrue(screen.contains("CompanionPageNavigation"))
        assertTrue(screen.contains("CompanionBackground"))
        assertTrue(screen.contains("CompanionHero"))

        assertTrue(screen.contains("DailyHero"))
        assertTrue(screen.contains("TodayNextStepCard"))
        assertTrue(screen.contains("QuickCheckInCard"))
        assertTrue(screen.contains("AskPage"))
        assertTrue(screen.contains("JournalPage"))
        assertTrue(screen.contains("QuestCard"))
        assertTrue(screen.contains("InsightCard"))
        assertTrue(screen.contains("ConnectionPanel"))

        assertFalse(screen.contains("StatusCards"))
        assertFalse(screen.contains("MetricCard("))
        assertFalse(screen.contains("SettingsPanel"))
        assertFalse(screen.contains("LauncherActions"))
    }

    @Test
    fun companionAcceptsExactLauncherEntryRequests() {
        val main = source("app/src/main/java/com/transcendiverse/digitaltwin/MainActivity.kt")
        val screen = source("app/src/main/java/com/transcendiverse/digitaltwin/ui/TodayScreen.kt")
        val request = source("app/src/main/java/com/transcendiverse/digitaltwin/CompanionLaunchRequest.kt")

        assertTrue(request.contains("enum class CompanionDestination"))
        assertTrue(request.contains("CheckIn"))
        assertTrue(request.contains("Quest"))
        assertTrue(request.contains("Journal"))
        assertTrue(request.contains("Ask"))
        assertTrue(request.contains("putCompanionLaunchRequest"))
        assertTrue(request.contains("toCompanionLaunchRequest"))

        assertTrue(main.contains("override fun onNewIntent(intent: Intent)"))
        assertTrue(main.contains("launchRequestState.value = intent.toCompanionLaunchRequest"))
        assertTrue(main.contains("initialLaunchRequest = launchRequestState.value"))

        assertTrue(screen.contains("initialLaunchRequest: CompanionLaunchRequest"))
        assertTrue(screen.contains("fun pageIndexFor(destination: CompanionDestination): Int"))
        assertTrue(screen.contains("CompanionDestination.CheckIn -> \"Today\""))
        assertTrue(screen.contains("CompanionDestination.Ask -> \"Ask\""))
        assertTrue(screen.contains("CompanionDestination.Journal -> \"Journal\""))
        assertTrue(screen.contains("CompanionDestination.Quest -> \"Quest\""))
        assertTrue(screen.contains("chatComposer = payload"))
        assertTrue(screen.contains("journalDraft = payload"))
    }

    @Test
    fun nativeAskPageHasRealSignedInPathAndLocalPreview() {
        val screen = source("app/src/main/java/com/transcendiverse/digitaltwin/ui/TodayScreen.kt")
        val repository = source("app/src/main/java/com/transcendiverse/digitaltwin/data/ChatRepository.kt")

        assertTrue(screen.contains("AskPage("))
        assertTrue(screen.contains("chatRepositoryFactory"))
        assertTrue(screen.contains("sendChatMessage"))
        assertTrue(screen.contains("ChatRole.USER"))
        assertTrue(screen.contains("ChatRole.ASSISTANT"))
        assertTrue(screen.contains("ChatRole.LOCAL"))
        assertTrue(screen.contains("Today's context:"))
        assertTrue(screen.contains("Sign in to send this to your Twin."))
        assertTrue(screen.contains("Plan the next hour from today's state"))
        assertTrue(screen.contains("Suggest the smallest step to reduce load"))
        assertTrue(screen.contains("CommandShortcutChip("))

        assertTrue(repository.contains("interface ChatRepository"))
        assertTrue(repository.contains("class NetworkChatRepository"))
        assertTrue(repository.contains("TodayHttpRequest"))
        assertTrue(repository.contains("TodayHttpTransport"))
        assertTrue(repository.contains("\"/api/chat/send\""))
        assertTrue(repository.contains("ChatSendRequest"))
        assertTrue(repository.contains("ChatSendResponse"))
        assertTrue(repository.contains("parseSafeBackendMessage"))
    }

    @Test
    fun journalUsesWebSyncWhenSignedInAndHonestLocalFallbackWhenSignedOut() {
        val screen = source("app/src/main/java/com/transcendiverse/digitaltwin/ui/TodayScreen.kt")
        val repository = source("app/src/main/java/com/transcendiverse/digitaltwin/data/JournalRepository.kt")

        assertTrue(screen.contains("JournalPage("))
        assertTrue(screen.contains("journalRepositoryFactory"))
        assertTrue(screen.contains("NetworkJournalRepository"))
        assertTrue(screen.contains("fun saveJournalEntry()"))
        assertTrue(screen.contains("signedIn = savedSettings.hasCredentials()"))
        assertTrue(screen.contains("submitting = journalSubmitting"))
        assertTrue(screen.contains("mood = today?.user?.mood?.label"))
        assertTrue(screen.contains("generatedJournalTitle(content)"))
        assertTrue(screen.contains("Save to web"))
        assertTrue(screen.contains("Saving"))
        assertTrue(screen.contains("Journal synced to web"))

        assertTrue(screen.contains("Local preview. Draft stays on this screen only."))
        assertTrue(screen.contains("Local preview only. Notes are not persisted yet."))

        assertTrue(repository.contains("interface JournalRepository"))
        assertTrue(repository.contains("class NetworkJournalRepository"))
        assertTrue(repository.contains("JournalCreateResult"))
        assertTrue(repository.contains("TodayHttpRequest"))
        assertTrue(repository.contains("TodayHttpTransport"))
        assertTrue(repository.contains("\"/api/journal\""))
        assertTrue(repository.contains("JournalCreateRequest"))
        assertTrue(repository.contains("JournalCreateResponse"))
        assertTrue(repository.contains("parseSafeBackendMessage"))
    }

    @Test
    fun mainCompanionFlowHidesDebugBackendAndRawLinks() {
        val screen = source("app/src/main/java/com/transcendiverse/digitaltwin/ui/TodayScreen.kt")

        assertFalse(screen.contains("text = today.quest.nextAction.href"))
        assertFalse(screen.contains("Backend: ${'$'}{baseUrl"))
        assertFalse(screen.contains("text = savedSettings.token"))
        assertFalse(screen.contains("text = response.token"))
        assertFalse(screen.contains("Bearer "))

        assertTrue(screen.contains("val action = recommendedTodayAction(today)"))
        assertTrue(screen.contains("action.label"))
        assertTrue(screen.contains("today.quest.nextAction.reason"))
        assertTrue(screen.contains("Preview only. Sign in from Me when you want to save."))
        assertTrue(screen.contains("Backend base URL"))
        assertTrue(screen.contains("PasswordVisualTransformation"))
    }

    @Test
    fun connectionSettingsAreCollapsedByDefault() {
        val screen = source("app/src/main/java/com/transcendiverse/digitaltwin/ui/TodayScreen.kt")

        assertTrue(screen.contains("var expanded by remember { mutableStateOf(false) }"))
        assertTrue(screen.contains("if (!expanded) return@InfoCard"))
        assertTrue(screen.contains("Text(if (expanded) \"Hide\" else \"Connection settings\")"))
        assertTrue(screen.contains("Fixture mode"))
        assertTrue(screen.contains("Signed in"))
    }

    private fun source(path: String): String {
        val candidates = listOf(
            File(path),
            File(path.removePrefix("app/")),
        )
        return candidates.firstOrNull(File::isFile)?.readText()
            ?: error("Unable to read $path")
    }
}
