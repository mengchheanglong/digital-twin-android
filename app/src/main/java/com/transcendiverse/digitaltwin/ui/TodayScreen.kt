package com.transcendiverse.digitaltwin.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.TextButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.transcendiverse.digitaltwin.CompanionDestination
import com.transcendiverse.digitaltwin.CompanionLaunchRequest
import com.transcendiverse.digitaltwin.data.ChatRepository
import com.transcendiverse.digitaltwin.data.CheckInRepository
import com.transcendiverse.digitaltwin.data.FakeTodayRepository
import com.transcendiverse.digitaltwin.data.InMemoryTodayCacheStore
import com.transcendiverse.digitaltwin.data.InMemoryTodaySettingsStore
import com.transcendiverse.digitaltwin.data.JournalRepository
import com.transcendiverse.digitaltwin.data.LoginRepository
import com.transcendiverse.digitaltwin.data.NetworkChatRepository
import com.transcendiverse.digitaltwin.data.NetworkCheckInRepository
import com.transcendiverse.digitaltwin.data.NetworkJournalRepository
import com.transcendiverse.digitaltwin.data.NetworkLoginRepository
import com.transcendiverse.digitaltwin.data.NetworkQuestRepository
import com.transcendiverse.digitaltwin.data.NetworkTodayRepository
import com.transcendiverse.digitaltwin.data.PRODUCTION_BACKEND_BASE_URL
import com.transcendiverse.digitaltwin.data.QuestRepository
import com.transcendiverse.digitaltwin.data.QuestSummary
import com.transcendiverse.digitaltwin.data.TodayCacheStore
import com.transcendiverse.digitaltwin.data.TodayRepository
import com.transcendiverse.digitaltwin.data.TodaySettings
import com.transcendiverse.digitaltwin.data.TodaySettingsStore
import com.transcendiverse.digitaltwin.data.generatedJournalTitle
import com.transcendiverse.digitaltwin.data.resolveActiveQuest
import com.transcendiverse.digitaltwin.data.useProductionBackend
import com.transcendiverse.digitaltwin.data.validateDailyCheckInRatings
import com.transcendiverse.digitaltwin.model.MobileToday
import com.transcendiverse.digitaltwin.model.recommendedTodayAction
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.launch

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun TodayScreen(
    settingsStore: TodaySettingsStore,
    cacheStore: TodayCacheStore = InMemoryTodayCacheStore(),
    fakeRepository: TodayRepository = FakeTodayRepository(),
    loginRepository: LoginRepository = NetworkLoginRepository(),
    networkRepositoryFactory: (String, String) -> TodayRepository = { baseUrl, token ->
        NetworkTodayRepository(baseUrl = baseUrl, token = token)
    },
    checkInRepositoryFactory: (String, String) -> CheckInRepository = { baseUrl, token ->
        NetworkCheckInRepository(baseUrl = baseUrl, token = token)
    },
    questRepositoryFactory: (String, String) -> QuestRepository = { baseUrl, token ->
        NetworkQuestRepository(baseUrl = baseUrl, token = token)
    },
    chatRepositoryFactory: (String, String) -> ChatRepository = { baseUrl, token ->
        NetworkChatRepository(baseUrl = baseUrl, token = token)
    },
    journalRepositoryFactory: (String, String) -> JournalRepository = { baseUrl, token ->
        NetworkJournalRepository(baseUrl = baseUrl, token = token)
    },
    initialLaunchRequest: CompanionLaunchRequest = CompanionLaunchRequest(),
    onTodayCacheUpdated: suspend () -> Unit = {},
    onScheduleBackgroundSync: () -> Unit = {},
    onEnqueueBackgroundSync: () -> Unit = {},
) {
    var savedSettings by remember { mutableStateOf(settingsStore.load()) }
    val cachedToday = remember { cacheStore.load() }
    var baseUrl by remember { mutableStateOf(savedSettings.baseUrl) }
    var email by remember { mutableStateOf(savedSettings.lastUserEmail) }
    var password by remember { mutableStateOf("") }
    var today by remember { mutableStateOf<MobileToday?>(cachedToday?.today) }
    var checkInSubmitting by remember { mutableStateOf(false) }
    var activeQuest by remember { mutableStateOf<QuestSummary?>(null) }
    var questActionSubmitting by remember { mutableStateOf(false) }
    var chatComposer by remember { mutableStateOf("") }
    var chatSubmitting by remember { mutableStateOf(false) }
    var activeChatId by remember { mutableStateOf<String?>(null) }
    var chatMessages by remember { mutableStateOf<List<CompanionChatMessage>>(emptyList()) }
    var chatNotice by remember { mutableStateOf<String?>(null) }
    var journalDraft by remember { mutableStateOf("") }
    var journalNotice by remember { mutableStateOf("Local preview. Draft stays on this screen only.") }
    var journalSubmitting by remember { mutableStateOf(false) }
    var status by remember {
        mutableStateOf(
            when {
                cachedToday != null -> cachedStatus(cachedToday.cachedAtEpochMillis)
                savedSettings.hasCredentials() -> "Loading"
                else -> "Fixture mode"
            },
        )
    }
    val scope = rememberCoroutineScope()
    val pages = remember { listOf("Today", "Ask", "Journal", "Quest", "Me") }
    val pagerState = rememberPagerState(pageCount = { pages.size })

    fun pageIndexFor(destination: CompanionDestination): Int {
        val page = when (destination) {
            CompanionDestination.Today,
            CompanionDestination.CheckIn -> "Today"
            CompanionDestination.Ask -> "Ask"
            CompanionDestination.Journal -> "Journal"
            CompanionDestination.Quest -> "Quest"
            CompanionDestination.Me -> "Me"
        }

        return pages.indexOf(page).coerceAtLeast(0)
    }

    suspend fun resolveActiveQuestControls(settings: TodaySettings, loadedToday: MobileToday?) {
        activeQuest = null
        val currentQuest = loadedToday?.quest?.current
        if (!settings.hasCredentials() || currentQuest == null) return

        try {
            val quests = questRepositoryFactory(settings.baseUrl, settings.token).listQuests()
            activeQuest = resolveActiveQuest(currentQuest, quests)
        } catch (error: Exception) {
            status = "Quest controls unavailable: ${safeStatusErrorMessage(error, "Unable to load quests")}"
        }
    }

    suspend fun loadToday(settings: TodaySettings, loadedStatus: String? = null): Boolean {
        status = if (today == null) "Loading" else cacheStore.load()?.cachedAtEpochMillis?.let(::cachedStatus) ?: "Cached"
        val repository = if (settings.hasCredentials()) {
            networkRepositoryFactory(settings.baseUrl, settings.token)
        } else {
            fakeRepository
        }

        try {
            val loadedToday = repository.getToday()
            today = loadedToday
            cacheStore.save(loadedToday)
            onTodayCacheUpdated()
            status = loadedStatus ?: if (settings.hasCredentials()) {
                cacheStore.load()?.cachedAtEpochMillis?.let(::lastUpdatedStatus) ?: "Loaded"
            } else {
                "Fixture mode"
            }
            resolveActiveQuestControls(settings, loadedToday)
            return true
        } catch (error: Exception) {
            val cached = cacheStore.load()
            today = cached?.today
            status = "Refresh failed: ${safeStatusErrorMessage(error, "Unable to load Today")}"
            return false
        }
    }

    fun runQuestAction(
        successStatus: (QuestSummary) -> String,
        action: suspend (QuestRepository, QuestSummary) -> Unit,
    ) {
        val quest = activeQuest ?: return
        scope.launch {
            questActionSubmitting = true
            try {
                val repository = questRepositoryFactory(savedSettings.baseUrl, savedSettings.token)
                action(repository, quest)
                val statusText = successStatus(quest)
                val refreshed = loadToday(savedSettings, loadedStatus = statusText)
                if (!refreshed) {
                    status = "$statusText - refresh failed; cached Today kept"
                }
                onEnqueueBackgroundSync()
            } catch (error: Exception) {
                status = "Quest action failed: ${safeStatusErrorMessage(error, "Unable to update quest")}"
            } finally {
                questActionSubmitting = false
            }
        }
    }

    fun sendChatMessage() {
        val message = chatComposer.trim()
        if (message.isBlank() || chatSubmitting) return

        if (!savedSettings.hasCredentials()) {
            chatComposer = ""
            chatMessages = chatMessages + CompanionChatMessage(role = ChatRole.USER, text = message)
            chatNotice = "Sign in to send this to your Twin."
            return
        }

        chatComposer = ""
        chatNotice = null
        chatSubmitting = true
        chatMessages = chatMessages + CompanionChatMessage(role = ChatRole.USER, text = message)
        scope.launch {
            try {
                val result = chatRepositoryFactory(
                    savedSettings.baseUrl,
                    savedSettings.token,
                ).sendMessage(message = message, chatId = activeChatId)
                activeChatId = result.chatId
                chatMessages = chatMessages + CompanionChatMessage(
                    role = ChatRole.ASSISTANT,
                    text = result.reply,
                )
            } catch (error: Exception) {
                chatNotice = "Ask failed: ${safeStatusErrorMessage(error, "Unable to send message")}"
            } finally {
                chatSubmitting = false
            }
        }
    }

    fun saveJournalEntry() {
        val content = journalDraft.trim()
        if (content.isBlank()) {
            journalNotice = "Write a journal draft first."
            return
        }

        if (!savedSettings.hasCredentials()) {
            journalNotice = "Draft kept locally on this screen. Sign in to save to web."
            return
        }

        if (journalSubmitting) return

        journalSubmitting = true
        scope.launch {
            try {
                journalRepositoryFactory(
                    savedSettings.baseUrl,
                    savedSettings.token,
                ).createEntry(
                    title = generatedJournalTitle(content),
                    content = content,
                    mood = today?.user?.mood?.label,
                )
                journalNotice = "Journal synced to web."
                status = "Journal synced to web"
                onEnqueueBackgroundSync()
            } catch (error: Exception) {
                journalNotice = "Journal sync failed: ${safeStatusErrorMessage(error, "Unable to save journal")}"
            } finally {
                journalSubmitting = false
            }
        }
    }

    fun openChatWithPrompt(prompt: String) {
        chatComposer = prompt
        chatNotice = if (savedSettings.hasCredentials()) {
            null
        } else {
            "Sign in to send this to your Twin."
        }
        scope.launch { pagerState.animateScrollToPage(pages.indexOf("Ask")) }
    }

    LaunchedEffect(Unit) {
        loadToday(savedSettings)
    }

    LaunchedEffect(initialLaunchRequest) {
        val payload = initialLaunchRequest.payload?.takeIf { it.isNotBlank() }
        when (initialLaunchRequest.destination) {
            CompanionDestination.Ask -> {
                if (payload != null) {
                    chatComposer = payload
                    chatNotice = if (savedSettings.hasCredentials()) {
                        null
                    } else {
                        "Sign in to send this to your Twin."
                    }
                }
            }
            CompanionDestination.Journal -> {
                if (payload != null) {
                    journalDraft = payload
                    journalNotice = if (savedSettings.hasCredentials()) {
                        "Draft ready to save to web."
                    } else {
                        "Draft kept on this phone."
                    }
                }
            }
            CompanionDestination.Today,
            CompanionDestination.CheckIn,
            CompanionDestination.Quest,
            CompanionDestination.Me -> Unit
        }

        val targetPage = pageIndexFor(initialLaunchRequest.destination)
        if (pagerState.currentPage != targetPage) {
            pagerState.animateScrollToPage(targetPage)
        }
    }

    LaunchedEffect(today?.dayKey, today?.insight?.reflection) {
        val loadedToday = today ?: return@LaunchedEffect
        if (chatMessages.isEmpty()) {
            chatMessages = listOf(
                CompanionChatMessage(
                    role = ChatRole.LOCAL,
                    text = "Today's context: ${loadedToday.insight.reflection}",
                ),
            )
        }
    }

    MaterialTheme {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = CompanionBackground,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .padding(top = 4.dp, bottom = 8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                CompanionShellHeader(today = today, status = status)
                CompanionPageNavigation(
                    pages = pages,
                    currentPage = pagerState.currentPage,
                    onPageSelected = { page ->
                        scope.launch { pagerState.animateScrollToPage(page) }
                    },
                )
                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                ) { page ->
                    when (pages[page]) {
                        "Today" -> CompanionPage {
                            today?.let { loadedToday ->
                                DailyHero(loadedToday)
                                TodayNextStepCard(
                                    today = loadedToday,
                                    signedIn = savedSettings.hasCredentials(),
                                )
                                QuickCheckInCard(
                                    today = loadedToday,
                                    signedIn = savedSettings.hasCredentials(),
                                    submitting = checkInSubmitting,
                                    onSubmit = { ratings ->
                                        val validationError = validateQuickCheckInRatings(ratings)
                                        if (validationError != null) {
                                            status = "Check-in failed: $validationError"
                                            return@QuickCheckInCard
                                        }

                                        scope.launch {
                                            checkInSubmitting = true
                                            try {
                                                val result = checkInRepositoryFactory(
                                                    savedSettings.baseUrl,
                                                    savedSettings.token,
                                                ).submitDaily(ratings)
                                                val successStatus = "Check-in submitted: ${result.percentage}%"
                                                val refreshed = loadToday(savedSettings, loadedStatus = successStatus)
                                                if (!refreshed) {
                                                    status = "$successStatus - refresh failed; cached Today kept"
                                                }
                                                onEnqueueBackgroundSync()
                                            } catch (error: Exception) {
                                                status = "Check-in failed: ${safeStatusErrorMessage(error, "Unable to submit check-in")}"
                                            } finally {
                                                checkInSubmitting = false
                                            }
                                        }
                                    },
                                )
                            } ?: LoadingState(status)
                        }

                        "Ask" -> CompanionPage {
                            AskPage(
                                today = today,
                                signedIn = savedSettings.hasCredentials(),
                                messages = chatMessages,
                                composer = chatComposer,
                                submitting = chatSubmitting,
                                notice = chatNotice,
                                onComposerChange = {
                                    chatComposer = it
                                    if (!savedSettings.hasCredentials()) {
                                        chatNotice = "Sign in to send this to your Twin."
                                    }
                                },
                                onSend = ::sendChatMessage,
                                onSuggestion = { suggestion ->
                                    chatComposer = suggestion
                                    chatNotice = if (savedSettings.hasCredentials()) {
                                        null
                                    } else {
                                        "Sign in to send this to your Twin."
                                    }
                                },
                            )
                        }

                        "Journal" -> CompanionPage {
                            JournalPage(
                                draft = journalDraft,
                                notice = journalNotice,
                                signedIn = savedSettings.hasCredentials(),
                                submitting = journalSubmitting,
                                mood = today?.user?.mood?.label,
                                onDraftChange = {
                                    journalDraft = it
                                    journalNotice = if (savedSettings.hasCredentials()) {
                                        "Ready to save to web."
                                    } else {
                                        "Local preview. Draft stays on this screen only."
                                    }
                                },
                                onSaveJournal = ::saveJournalEntry,
                            )
                        }

                        "Quest" -> CompanionPage {
                            today?.let { loadedToday ->
                                QuestCard(
                                    today = loadedToday,
                                    activeQuest = activeQuest.takeIf { savedSettings.hasCredentials() },
                                    signedIn = savedSettings.hasCredentials(),
                                    submitting = questActionSubmitting,
                                    onDecreaseProgress = {
                                        runQuestAction(successStatus = { "Quest progress updated" }) { repository, quest ->
                                            repository.updateProgress(quest.id, (quest.progress - 10).coerceIn(0, 100))
                                        }
                                    },
                                    onIncreaseProgress = {
                                        runQuestAction(successStatus = { "Quest progress updated" }) { repository, quest ->
                                            repository.updateProgress(quest.id, (quest.progress + 10).coerceIn(0, 100))
                                        }
                                    },
                                    onToggleComplete = {
                                        runQuestAction(
                                            successStatus = { quest ->
                                                if (quest.completed) "Quest reopened" else "Quest completed"
                                            },
                                        ) { repository, quest ->
                                            repository.toggleComplete(quest.id)
                                        }
                                    },
                                    onPlanQuest = {
                                        openChatWithPrompt("Help me plan a small quest for today")
                                    },
                                    onAskTwin = {
                                        openChatWithPrompt("Suggest one quest based on today")
                                    },
                                    onReflectFirst = {
                                        openChatWithPrompt("Help me reflect before choosing a quest")
                                    },
                                )
                            } ?: LoadingState(status)
                        }

                        "Me" -> CompanionPage {
                            MeProfileCard(
                                today = today,
                                signedIn = savedSettings.hasCredentials(),
                                status = status,
                            )
                            ConnectionPanel(
                                baseUrl = baseUrl,
                                email = email,
                                password = password,
                                signedIn = savedSettings.token.isNotBlank(),
                                status = status,
                                onBaseUrlChange = { baseUrl = it },
                                onEmailChange = { email = it },
                                onPasswordChange = { password = it },
                                onSaveUrl = {
                                    val settings = savedSettings.copy(baseUrl = baseUrl.trim())
                                    settingsStore.save(settings)
                                    savedSettings = settings
                                    baseUrl = settings.baseUrl
                                    status = "URL saved"
                                },
                                onLogin = {
                                    val validationError = validateLoginInput(baseUrl, email, password)
                                    if (validationError != null) {
                                        status = "Login failed: $validationError"
                                    } else {
                                        scope.launch {
                                            status = "Logging in"
                                            try {
                                                val response = loginRepository.login(
                                                    baseUrl = baseUrl.trim(),
                                                    email = email.trim(),
                                                    password = password,
                                                )
                                                password = ""
                                                val settings = TodaySettings(
                                                    baseUrl = baseUrl.trim(),
                                                    token = response.token,
                                                    lastUserEmail = response.user.email,
                                                    lastUserName = response.user.name,
                                                )
                                                settingsStore.save(settings)
                                                savedSettings = settings
                                                email = settings.lastUserEmail
                                                chatNotice = null
                                                onScheduleBackgroundSync()
                                                onEnqueueBackgroundSync()
                                                loadToday(settings, loadedStatus = "Signed in")
                                            } catch (error: Exception) {
                                                status = "Login failed: ${safeStatusErrorMessage(error, "Unable to sign in")}"
                                            }
                                        }
                                    }
                                },
                                onRefresh = {
                                    val settings = savedSettings.copy(baseUrl = baseUrl.trim())
                                    savedSettings = settings
                                    onEnqueueBackgroundSync()
                                    scope.launch { loadToday(settings) }
                                },
                                onUseProduction = {
                                    val settings = savedSettings.useProductionBackend()
                                    settingsStore.save(settings)
                                    savedSettings = settings
                                    baseUrl = settings.baseUrl
                                    status = "Production URL set"
                                },
                                onClearToken = {
                                    val settings = savedSettings.copy(
                                        token = "",
                                        lastUserEmail = "",
                                        lastUserName = "",
                                    )
                                    settingsStore.save(settings)
                                    savedSettings = settings
                                    email = ""
                                    activeChatId = null
                                    chatNotice = "Signed out. Ask is local-only until you sign in."
                                    status = "Signed out - fixture mode"
                                },
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CompanionShellHeader(today: MobileToday?, status: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 18.dp, vertical = 0.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(0.dp),
        ) {
            Text(
                text = "Twin",
                style = MaterialTheme.typography.titleMedium,
                color = CompanionInk,
                fontWeight = FontWeight.Medium,
            )
            Text(
                text = today?.let { "${formatTodayDay(it.dayKey)} · ${it.user.name}" } ?: status,
                style = MaterialTheme.typography.bodySmall,
                color = CompanionMuted,
                maxLines = 1,
            )
        }
        CompanionStatusPill(label = status.commandStatusLabel())
    }
}

@Composable
private fun CompanionPageNavigation(
    pages: List<String>,
    currentPage: Int,
    onPageSelected: (Int) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(42.dp)
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        pages.forEachIndexed { index, label ->
            CommandSegmentTab(
                label = label,
                selected = index == currentPage,
                onClick = { onPageSelected(index) },
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun CommandSegmentTab(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier
            .heightIn(min = 40.dp, max = 40.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(999.dp),
        color = if (selected) CompanionSelectedTab.copy(alpha = 0.12f) else Color.Transparent,
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = label.uppercase(Locale.US),
                modifier = Modifier.padding(horizontal = 4.dp),
                style = MaterialTheme.typography.labelSmall,
                color = if (selected) CompanionPrimary else CompanionMuted.copy(alpha = 0.76f),
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
                maxLines = 1,
                textAlign = TextAlign.Center,
            )
            if (selected) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth(0.48f)
                        .height(2.dp)
                        .background(CompanionPrimary, RoundedCornerShape(2.dp)),
                )
            }
        }
    }
}

@Composable
private fun CompanionStatusPill(label: String) {
    Surface(
        modifier = Modifier.padding(start = 8.dp),
        shape = RoundedCornerShape(12.dp),
        color = CompanionSurfaceSoft,
        border = BorderStroke(1.dp, CompanionHairline),
    ) {
        Text(
            text = "● $label",
            modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp),
            style = MaterialTheme.typography.labelSmall,
            color = CompanionSecondary,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
        )
    }
}

@Composable
private fun CompanionPage(content: @Composable () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 3.dp),
        verticalArrangement = Arrangement.spacedBy(7.dp),
    ) {
        content()
    }
}

@Composable
private fun AskPage(
    today: MobileToday?,
    signedIn: Boolean,
    messages: List<CompanionChatMessage>,
    composer: String,
    submitting: Boolean,
    notice: String?,
    onComposerChange: (String) -> Unit,
    onSend: () -> Unit,
    onSuggestion: (String) -> Unit,
) {
    val suggestions = listOf(
        "Plan hour" to "Plan the next hour from today's state",
        "Reduce load" to "Suggest the smallest step to reduce load",
    )
    val contextLine = today?.let {
        "readiness ${it.commandReadinessScore()} · quest ${it.commandQuestProgress()}% · continuity ${it.user.streak}d"
    } ?: "Load Today to attach live context."

    CommandPageIntro(
        label = "ASK ENGINE",
        title = "Answer layer",
        body = "Direct, bounded answers using Today state, quest, journal draft, and current check-in context.",
        status = if (signedIn) "context live" else "local preview",
    )

    CommandConsoleCard(label = "CONTEXT") {
        Text(
            text = contextLine,
            style = MaterialTheme.typography.labelMedium,
            color = CompanionSecondary,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        suggestions.forEach { (label, prompt) ->
            CommandShortcutChip(
                label = label,
                modifier = Modifier.weight(1f),
                onClick = { onSuggestion(prompt) },
            )
        }
    }

    CommandConsoleCard(label = "THREAD") {
        Text(
            text = if (signedIn) "Ask for one clear decision, plan, or explanation." else "Local preview. Sign in to send this to your Twin.",
            style = MaterialTheme.typography.bodySmall,
            color = CompanionMuted,
        )
        if (messages.isEmpty()) {
            Text(
                text = today?.insight?.reflection ?: "No thread context yet.",
                style = MaterialTheme.typography.bodyMedium,
                color = CompanionSecondary,
            )
        } else {
            messages.takeLast(4).forEach { message ->
                ChatBubble(message)
            }
        }
        if (notice != null) {
            Text(
                text = notice,
                style = MaterialTheme.typography.bodySmall,
                color = CompanionWarm,
                fontWeight = FontWeight.Medium,
            )
        }
    }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        color = CompanionHero,
        border = BorderStroke(1.dp, CompanionHairline),
    ) {
        Row(
            modifier = Modifier.padding(8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            OutlinedTextField(
                value = composer,
                onValueChange = onComposerChange,
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = 52.dp),
                label = { Text("Command Twin") },
                minLines = 1,
                maxLines = 3,
                colors = companionTextFieldColors(),
            )
            Button(
                onClick = onSend,
                modifier = Modifier
                    .widthIn(min = 68.dp)
                    .heightIn(min = 52.dp),
                enabled = composer.trim().isNotBlank() && !submitting,
                shape = RoundedCornerShape(12.dp),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = CompanionPrimary,
                    contentColor = CompanionBackground,
                ),
            ) {
                Text(
                    text = if (submitting) "Sending" else if (signedIn) "Ask" else "Keep",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                )
            }
        }
    }
}

@Composable
private fun JournalPage(
    draft: String,
    notice: String?,
    signedIn: Boolean,
    submitting: Boolean,
    mood: String?,
    onDraftChange: (String) -> Unit,
    onSaveJournal: () -> Unit,
) {
    val promptSeeds = listOf(
        "Signal" to "What changed in my state today?",
        "Win" to "What tiny win should I protect?",
    )

    CommandPageIntro(
        label = "LOG BUFFER",
        title = "Journal capture",
        body = if (signedIn) {
            "Capture one useful signal before it disappears. Signed-in entries save to the web journal."
        } else {
            "Capture one useful signal before it disappears. This draft stays local until you sign in."
        },
        status = if (signedIn) "web sync" else "local draft",
    )

    CommandConsoleCard(label = "PROMPTS") {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            promptSeeds.forEach { (label, seed) ->
                CommandShortcutChip(
                    label = label,
                    modifier = Modifier.weight(1f),
                    onClick = { onDraftChange(appendDraftPrompt(draft, seed)) },
                )
            }
        }
    }

    CommandConsoleCard(label = "CAPTURE", accent = true) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "one useful sentence is enough",
                style = MaterialTheme.typography.bodySmall,
                color = CompanionSecondary,
            )
            CommandLabelPill(text = mood?.commandTitleCase() ?: "${draft.length} chars")
        }
        OutlinedTextField(
            value = draft,
            onValueChange = onDraftChange,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 132.dp),
            label = { Text(if (signedIn) "Journal entry" else "Local draft") },
            minLines = 4,
            maxLines = 7,
            colors = companionTextFieldColors(),
        )
        Button(
            onClick = onSaveJournal,
            modifier = Modifier.fillMaxWidth(),
            enabled = draft.isNotBlank() && !submitting,
            shape = RoundedCornerShape(12.dp),
            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 11.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = CompanionPrimary,
                contentColor = CompanionBackground,
                disabledContainerColor = CompanionSurfaceSoft,
                disabledContentColor = CompanionMuted,
            ),
        ) {
            Text(
                text = if (submitting) "Saving" else if (signedIn) "Save to web" else "Keep local draft",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
            )
        }
        if (notice != null) {
            Text(
                text = notice,
                style = MaterialTheme.typography.bodySmall,
                color = CompanionMuted,
            )
        }
    }

    CommandConsoleCard(label = "RECENT SIGNALS") {
        Text(
            text = if (signedIn) {
                "New entries are saved to the web journal. This screen keeps the draft visible after sync."
            } else {
                "Local preview only. Notes are not persisted yet."
            },
            style = MaterialTheme.typography.bodySmall,
            color = CompanionSecondary,
        )
    }
}

@Composable
private fun CommandPageIntro(
    label: String,
    title: String,
    body: String,
    status: String,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = CompanionHero,
        border = BorderStroke(1.dp, CompanionHairline),
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                CommandMicroLabel(text = label)
                CommandLabelPill(text = status)
            }
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = CompanionInk,
                fontWeight = FontWeight.SemiBold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = body,
                style = MaterialTheme.typography.bodySmall,
                color = CompanionSecondary,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun CommandConsoleCard(
    label: String,
    modifier: Modifier = Modifier,
    accent: Boolean = false,
    content: @Composable () -> Unit,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = if (accent) CompanionElevated else CompanionPanel,
        border = BorderStroke(1.dp, if (accent) CompanionAccentBorder else CompanionHairline),
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            CommandMicroLabel(text = label)
            content()
        }
    }
}

private fun appendDraftPrompt(current: String, prompt: String): String {
    val trimmed = current.trim()
    return if (trimmed.isBlank()) prompt else "$trimmed\n\n$prompt"
}

@Composable
private fun CommandShortcutChip(
    label: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    Surface(
        modifier = modifier
            .heightIn(min = 42.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(999.dp),
        color = CompanionPanel,
        border = BorderStroke(1.dp, CompanionHairline),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = CompanionSecondary,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
private fun ChatBubble(message: CompanionChatMessage) {
    val isUser = message.role == ChatRole.USER
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start,
    ) {
        Surface(
            modifier = Modifier.widthIn(max = 300.dp),
            color = when (message.role) {
                ChatRole.USER -> CompanionSelectedTab
                ChatRole.ASSISTANT -> CompanionChatAssistant
                ChatRole.LOCAL -> CompanionChip
            },
            shape = RoundedCornerShape(
                topStart = 16.dp,
                topEnd = 16.dp,
                bottomStart = if (isUser) 16.dp else 6.dp,
                bottomEnd = if (isUser) 6.dp else 16.dp,
            ),
            border = BorderStroke(1.dp, if (isUser) CompanionAccentBorder else CompanionHairline),
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp),
                verticalArrangement = Arrangement.spacedBy(3.dp),
            ) {
                Text(
                    text = when (message.role) {
                        ChatRole.USER -> "You"
                        ChatRole.ASSISTANT -> "Twin"
                        ChatRole.LOCAL -> "Context"
                    },
                    style = MaterialTheme.typography.labelSmall,
                    color = if (isUser) CompanionPrimary else CompanionMuted,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = message.text,
                    style = MaterialTheme.typography.bodyMedium,
                    color = CompanionInk,
                )
            }
        }
    }
}

private data class CompanionChatMessage(
    val role: ChatRole,
    val text: String,
)

private enum class ChatRole {
    USER,
    ASSISTANT,
    LOCAL,
}

private fun MobileToday?.askContextLine(): String {
    val today = this ?: return "Today context unavailable"
    val questState = if (today.quest.current != null) "quest active" else "no active quest"
    val checkInState = if (today.checkIn.completedToday) "checked in" else "check-in pending"
    return listOf(
        today.user.mood.label.lowercase(Locale.getDefault()),
        questState,
        checkInState,
    ).joinToString(separator = " \u00B7 ")
}

@Composable
private fun DailyHero(today: MobileToday) {
    val readiness = today.commandReadinessScore()
    val load = today.commandLoadScore()
    val recovery = today.commandRecoveryScore()

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        color = CompanionHero,
        border = BorderStroke(1.dp, CompanionHairline),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(7.dp),
        ) {
            CommandMicroLabel(text = "DAILY SIGNAL")
            Text(
                text = today.user.mood.label.commandTitleCase(),
                style = MaterialTheme.typography.titleMedium,
                color = CompanionInk,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = "readiness $readiness \u00B7 load $load \u00B7 recovery $recovery",
                style = MaterialTheme.typography.labelMedium,
                color = CompanionSecondary,
                fontWeight = FontWeight.Medium,
            )
            Text(
                text = today.commandInterpretation(),
                style = MaterialTheme.typography.bodySmall,
                color = CompanionSecondary,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun TodayVitalsRow(today: MobileToday) {
    val dimensions = today.checkIn.dimensions ?: return

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        SoftChip(
            label = "Energy ${dimensions.energy}",
            modifier = Modifier.weight(1f),
        )
        SoftChip(
            label = "Focus ${dimensions.focus}",
            modifier = Modifier.weight(1f),
        )
        SoftChip(
            label = "Stress ${dimensions.stressControl}",
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun SoftChip(label: String, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        color = CompanionPanel,
        shape = RoundedCornerShape(10.dp),
        border = BorderStroke(1.dp, CompanionHairline),
    ) {
        Text(
            text = label,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
            style = MaterialTheme.typography.labelSmall,
            color = CompanionSecondary,
            fontWeight = FontWeight.Medium,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun CommandMicroLabel(text: String) {
    Text(
        text = text.uppercase(Locale.US),
        style = MaterialTheme.typography.labelSmall,
        color = CompanionMuted.copy(alpha = 0.86f),
        fontFamily = FontFamily.Monospace,
        fontWeight = FontWeight.Medium,
        maxLines = 1,
    )
}

@Composable
private fun CommandLabelPill(text: String) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = CompanionPanel,
        border = BorderStroke(1.dp, CompanionHairline),
    ) {
        Text(
            text = text.uppercase(Locale.US),
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            style = MaterialTheme.typography.labelSmall,
            color = CompanionSecondary,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
        )
    }
}

@Composable
private fun CommandMetricCell(
    label: String,
    value: Int,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        color = CompanionPanel,
        border = BorderStroke(1.dp, CompanionHairline),
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 9.dp, vertical = 7.dp),
            verticalArrangement = Arrangement.spacedBy(5.dp),
        ) {
            CommandMicroLabel(text = label)
            Text(
                text = value.toString(),
                style = MaterialTheme.typography.titleSmall,
                color = CompanionInk,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
            )
            CommandProgressRail(value = value, segments = 10, compact = true)
        }
    }
}

@Composable
private fun CommandProgressRail(
    value: Int,
    modifier: Modifier = Modifier,
    segments: Int = 14,
    compact: Boolean = false,
) {
    val filledSegments = ((value.coerceIn(0, 100) * segments) + 99) / 100

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        repeat(segments) { index ->
            Surface(
                modifier = Modifier
                    .weight(1f)
                    .height(if (compact) 4.dp else 5.dp),
                shape = RoundedCornerShape(999.dp),
                color = if (index < filledSegments) {
                    CompanionPrimary.copy(alpha = if (compact) 0.66f else 0.74f)
                } else {
                    CompanionPanel
                },
            ) {}
        }
    }
}

@Composable
private fun TodayNextStepCard(
    today: MobileToday,
    signedIn: Boolean,
) {
    val action = recommendedTodayAction(today)
    val protocolTitle = when (action.label) {
        "Check in" -> "30-sec check-in"
        "Continue quest" -> "Quest continuation"
        else -> "Short reflection"
    }
    val detail = when (action.label) {
        "Check in" -> "Capture a baseline before routing the day."
        "Continue quest" -> today.quest.current?.goal ?: "Pick up where you left off."
        else -> "Close the loop with one useful sentence."
    }
    val commandLabel = when {
        action.label == "Check in" && signedIn -> "Start check-in"
        action.label == "Check in" -> "Preview check-in"
        action.label == "Continue quest" -> "Continue"
        else -> "Reflect"
    }
    val quest = today.quest.current
    val progress = today.commandQuestProgress()

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        color = CompanionElevated,
        border = BorderStroke(1.dp, CompanionHairline),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                CommandMicroLabel(text = "CURRENT PROTOCOL")
                CommandLabelPill(text = commandLabel)
            }
            Text(
                text = protocolTitle,
                style = MaterialTheme.typography.titleSmall,
                color = CompanionInk,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = detail,
                style = MaterialTheme.typography.bodyMedium,
                color = CompanionSecondary,
            )
            Text(
                text = if (action.label == "Check in" && !signedIn) {
                    "Preview only. Sign in from Me when you want to save."
                } else {
                    today.quest.nextAction.reason
                },
                style = MaterialTheme.typography.bodySmall,
                color = CompanionMuted,
            )
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp),
                color = CompanionHairline,
            ) {}
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                CommandMicroLabel(text = "QUEST ARC")
                Text(
                    text = "$progress%",
                    style = MaterialTheme.typography.labelMedium,
                    color = CompanionPrimary,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                )
            }
            Text(
                text = quest?.goal ?: "No active quest",
                style = MaterialTheme.typography.bodyMedium,
                color = CompanionInk,
                fontWeight = FontWeight.Medium,
            )
            CommandProgressRail(value = progress)
            Text(
                text = "Continuity ${today.user.streak}d \u00B7 Calibration +${today.commandCalibrationGain()}",
                style = MaterialTheme.typography.bodySmall,
                color = CompanionMuted,
            )
        }
    }
}

@Composable
private fun QuickCheckInCard(
    today: MobileToday,
    signedIn: Boolean,
    submitting: Boolean,
    onSubmit: (List<Int>) -> Unit,
) {
    if (today.checkIn.completedToday) {
        InfoCard(title = "Check-in matrix") {
            Text(
                text = "Check-in complete",
                style = MaterialTheme.typography.bodyMedium,
                color = CompanionSuccess,
                fontWeight = FontWeight.SemiBold,
            )
        }
        return
    }

    var selectedPreset by remember(today.dayKey) { mutableStateOf<CheckInPreset?>(CheckInPreset.OKAY) }
    var ratings by remember(today.dayKey) { mutableStateOf(checkInPresetRatings(CheckInPreset.OKAY)) }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        color = CompanionPanel,
        border = BorderStroke(1.dp, CompanionHairline),
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                CommandMicroLabel(text = "CHECK-IN")
                CommandLabelPill(text = if (signedIn) "ready" else "preview")
            }
            Text(
                text = if (signedIn) {
                    "Pick the baseline that fits right now."
                } else {
                    "Preview only. Sign in from Me to save."
                },
                style = MaterialTheme.typography.bodySmall,
                color = CompanionSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                CheckInPreset.entries.forEach { preset ->
                    PresetPill(
                        preset = preset,
                        selected = selectedPreset == preset,
                        enabled = !submitting,
                        modifier = Modifier.weight(1f),
                        onClick = {
                            ratings = checkInPresetRatings(preset)
                            selectedPreset = preset
                        },
                    )
                }
            }
            Text(
                text = checkInShapeSummary(ratings, selectedPreset),
                style = MaterialTheme.typography.bodySmall,
                color = CompanionMuted,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            CHECK_IN_DIMENSIONS.forEachIndexed { index, label ->
                RatingControlRow(
                    label = label,
                    rating = ratings[index],
                    enabled = !submitting,
                    onDecrease = {
                        val updated = updateCheckInRating(ratings, index, -1)
                        ratings = updated
                        selectedPreset = checkInPresetForRatings(updated)
                    },
                    onIncrease = {
                        val updated = updateCheckInRating(ratings, index, 1)
                        ratings = updated
                        selectedPreset = checkInPresetForRatings(updated)
                    },
                )
            }
            if (signedIn) {
                Button(
                    onClick = { onSubmit(ratings) },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !submitting,
                    shape = RoundedCornerShape(12.dp),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = CompanionPrimary, contentColor = CompanionBackground),
                ) {
                    Text(if (submitting) "Submitting" else "Save check-in")
                }
            } else {
                Text(
                    text = "This is a local preview. Sign in from Me to save check-ins.",
                    style = MaterialTheme.typography.bodySmall,
                    color = CompanionMuted,
                )
            }
        }
    }
}

@Composable
private fun PresetPill(
    preset: CheckInPreset,
    selected: Boolean,
    enabled: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    if (selected) {
        OutlinedButton(
            onClick = onClick,
            modifier = modifier.heightIn(min = 44.dp),
            enabled = enabled,
            shape = RoundedCornerShape(11.dp),
            border = BorderStroke(1.dp, CompanionAccentBorder),
            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp),
            colors = ButtonDefaults.outlinedButtonColors(
                containerColor = CompanionSelectedTab,
                contentColor = CompanionInk,
            ),
        ) {
            Text(
                text = preset.label.uppercase(Locale.US),
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.SemiBold,
            )
        }
    } else {
        OutlinedButton(
            onClick = onClick,
            modifier = modifier.heightIn(min = 44.dp),
            enabled = enabled,
            shape = RoundedCornerShape(11.dp),
            border = BorderStroke(1.dp, CompanionHairline),
            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = CompanionInk),
        ) {
            Text(
                text = preset.label.uppercase(Locale.US),
                style = MaterialTheme.typography.labelSmall,
            )
        }
    }
}

@Composable
private fun RatingControlRow(
    label: String,
    rating: Int,
    enabled: Boolean,
    onDecrease: () -> Unit,
    onIncrease: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 44.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.bodySmall,
            color = CompanionSecondary,
            fontWeight = FontWeight.Medium,
        )
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = CompanionPanel,
            border = BorderStroke(1.dp, CompanionHairline),
        ) {
            Row(
                modifier = Modifier.padding(2.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                RatingStepperButton(
                    label = "-",
                    enabled = enabled && rating > 1,
                    onClick = onDecrease,
                )
                Box(
                    modifier = Modifier
                        .width(28.dp)
                        .height(34.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = rating.toString(),
                        style = MaterialTheme.typography.titleSmall,
                        color = CompanionInk,
                        fontWeight = FontWeight.SemiBold,
                        textAlign = TextAlign.Center,
                    )
                }
                RatingStepperButton(
                    label = "+",
                    enabled = enabled && rating < 5,
                    onClick = onIncrease,
                )
            }
        }
    }
}

@Composable
private fun RatingStepperButton(
    label: String,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    TextButton(
        onClick = onClick,
        modifier = Modifier.size(48.dp),
        enabled = enabled,
        shape = RoundedCornerShape(11.dp),
        colors = ButtonDefaults.textButtonColors(
            contentColor = CompanionPrimary,
            disabledContentColor = CompanionMuted,
        ),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center,
        )
    }
}


@Composable
private fun QuestCard(
    today: MobileToday,
    activeQuest: QuestSummary?,
    signedIn: Boolean,
    submitting: Boolean,
    onDecreaseProgress: () -> Unit,
    onIncreaseProgress: () -> Unit,
    onToggleComplete: () -> Unit,
    onPlanQuest: () -> Unit,
    onAskTwin: () -> Unit,
    onReflectFirst: () -> Unit,
) {
    val quest = today.quest.current
    val progress = (activeQuest?.progress ?: quest?.progress ?: today.commandQuestProgress()).coerceIn(0, 100)
    val goal = activeQuest?.goal?.takeIf { it.isNotBlank() } ?: quest?.goal ?: "No active quest"
    val duration = activeQuest?.duration?.takeIf { it.isNotBlank() } ?: quest?.duration ?: "daily"
    val liveControls = signedIn && activeQuest != null

    CommandPageIntro(
        label = "QUEST ARC",
        title = if (quest == null) "Choose one finishable objective" else goal,
        body = if (quest == null) {
            "Use today's signal to pick a small route before adding progress."
        } else {
            "Current objective, progress rail, and next command in one compact loop."
        },
        status = when {
            liveControls -> "live controls"
            signedIn -> "sync pending"
            else -> "preview only"
        },
    )

    CommandConsoleCard(label = "OBJECTIVE", accent = true) {
        Text(
            text = goal,
            style = MaterialTheme.typography.titleMedium,
            color = CompanionInk,
            fontWeight = FontWeight.SemiBold,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            text = if (quest == null) {
                "Preview only. Plan, ask, or reflect before committing the quest."
            } else {
                "$progress% complete - ${duration.commandTitleCase()}"
            },
            style = MaterialTheme.typography.bodySmall,
            color = CompanionSecondary,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
    }

    CommandConsoleCard(label = "PROGRESS") {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            CommandMicroLabel(text = if (liveControls) "LIVE CONTROLS" else "PREVIEW CONTROLS")
            Text(
                text = "$progress%",
                style = MaterialTheme.typography.labelMedium,
                color = CompanionPrimary,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
            )
        }
        CommandProgressRail(value = progress, segments = 12)

        if (liveControls) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                QuestControlButton(
                    label = if (submitting) "Saving" else if (activeQuest.completed) "Reopen" else "Complete",
                    primary = true,
                    enabled = !submitting,
                    modifier = Modifier.weight(1.35f),
                    onClick = onToggleComplete,
                )
                QuestControlButton(
                    label = "-10%",
                    enabled = !submitting && activeQuest.progress > 0,
                    modifier = Modifier.weight(1f),
                    onClick = onDecreaseProgress,
                )
                QuestControlButton(
                    label = "+10%",
                    enabled = !submitting && activeQuest.progress < 100,
                    modifier = Modifier.weight(1f),
                    onClick = onIncreaseProgress,
                )
            }
        } else {
            Text(
                text = if (signedIn) {
                    "Live quest controls are unavailable. Use the command shortcuts while sync reconnects."
                } else {
                    "Preview only. Sign in to update progress."
                },
                style = MaterialTheme.typography.bodySmall,
                color = CompanionMuted,
            )
        }
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        CommandShortcutChip(
            label = "Plan step",
            modifier = Modifier.weight(1f),
            onClick = onPlanQuest,
        )
        CommandShortcutChip(
            label = "Ask Twin",
            modifier = Modifier.weight(1f),
            onClick = onAskTwin,
        )
    }
    CommandShortcutChip(
        label = "Reflect first",
        modifier = Modifier.fillMaxWidth(),
        onClick = onReflectFirst,
    )
}

@Composable
private fun QuestControlButton(
    label: String,
    enabled: Boolean,
    modifier: Modifier = Modifier,
    primary: Boolean = false,
    onClick: () -> Unit,
) {
    if (primary) {
        Button(
            onClick = onClick,
            modifier = modifier.heightIn(min = 46.dp),
            enabled = enabled,
            shape = RoundedCornerShape(12.dp),
            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = CompanionPrimary,
                contentColor = CompanionBackground,
                disabledContainerColor = CompanionPanel,
                disabledContentColor = CompanionMuted,
            ),
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center,
            )
        }
    } else {
        OutlinedButton(
            onClick = onClick,
            modifier = modifier.heightIn(min = 46.dp),
            enabled = enabled,
            shape = RoundedCornerShape(12.dp),
            border = BorderStroke(1.dp, CompanionHairline),
            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp),
            colors = ButtonDefaults.outlinedButtonColors(
                contentColor = CompanionSecondary,
                disabledContentColor = CompanionMuted,
            ),
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
private fun InsightCard(today: MobileToday) {
    InfoCard(title = "Reflection") {
        Text(
            text = today.insight.reflection,
            style = MaterialTheme.typography.bodyMedium,
            color = CompanionInk,
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Context: ${today.insight.trend} trend - ${today.insight.topInterest} focus",
            style = MaterialTheme.typography.bodySmall,
            color = CompanionMuted,
        )
    }
}

private data class WebBridgeRoute(
    val label: String,
    val detail: String,
    val path: String,
)

private val WebBridgeRoutes = listOf(
    WebBridgeRoute(
        label = "Focus",
        detail = "Deep work planner",
        path = "/dashboard/focus",
    ),
    WebBridgeRoute(
        label = "Analytics",
        detail = "Progress patterns",
        path = "/dashboard/analytics",
    ),
    WebBridgeRoute(
        label = "Timeline",
        detail = "Life map context",
        path = "/dashboard/timeline",
    ),
    WebBridgeRoute(
        label = "History",
        detail = "Past days archive",
        path = "/dashboard/history",
    ),
    WebBridgeRoute(
        label = "Data export",
        detail = "Profile data controls",
        path = "/dashboard/profile",
    ),
    WebBridgeRoute(
        label = "Full dashboard",
        detail = "Today source of truth",
        path = "/dashboard/insight",
    ),
)

private fun WebBridgeRoute.webUri(): String =
    PRODUCTION_BACKEND_BASE_URL.trimEnd('/') + path

@Composable
private fun MeProfileCard(
    today: MobileToday?,
    signedIn: Boolean,
    status: String,
) {
    val uriHandler = LocalUriHandler.current

    CommandPageIntro(
        label = "OPERATOR",
        title = today?.user?.name ?: "Me",
        body = if (today == null) {
            if (signedIn) "Loading your latest profile layer." else "Fixture profile. Sign in to connect live state."
        } else {
            "Mastery, continuity, and system connection in one place."
        },
        status = if (signedIn) "connected" else "fixture",
    )

    if (today == null) {
        CommandConsoleCard(label = "PROFILE STATE") {
            Text(
                text = status,
                style = MaterialTheme.typography.bodySmall,
                color = CompanionMuted,
            )
        }
        CommandConsoleCard(label = "MORE FROM WEB") {
            WebBridgeRoutes.forEach { route ->
                WebBridgeRouteRow(
                    route = route,
                    onClick = { uriHandler.openUri(route.webUri()) },
                )
            }
        }
        return
    }

    CommandConsoleCard(label = "ACCOUNT", accent = true) {
        Text(
            text = "Level ${today.user.level} · ${today.user.streak}d streak · ${today.user.mood.label.commandTitleCase()}",
            style = MaterialTheme.typography.titleSmall,
            color = CompanionInk,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            text = "${today.user.currentXP}/${today.user.requiredXP} XP · readiness ${today.commandReadinessScore()} · recovery ${today.commandRecoveryScore()}",
            style = MaterialTheme.typography.bodySmall,
            color = CompanionSecondary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        CommandProgressRail(value = today.commandQuestProgress(), segments = 10, compact = true)
        Text(
            text = if (signedIn) status else "Fixture mode",
            style = MaterialTheme.typography.bodySmall,
            color = CompanionMuted,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }

    CommandConsoleCard(label = "MORE FROM WEB") {
        WebBridgeRoutes.forEach { route ->
            WebBridgeRouteRow(
                route = route,
                onClick = { uriHandler.openUri(route.webUri()) },
            )
        }
    }
}

@Composable
private fun WebBridgeRouteRow(
    route: WebBridgeRoute,
    onClick: () -> Unit,
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 46.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(11.dp),
        color = CompanionPanel,
        border = BorderStroke(1.dp, CompanionHairline),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(1.dp),
            ) {
                Text(
                    text = route.label,
                    style = MaterialTheme.typography.labelLarge,
                    color = CompanionInk,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                )
                Text(
                    text = route.detail,
                    style = MaterialTheme.typography.bodySmall,
                    color = CompanionMuted,
                    maxLines = 1,
                )
            }
            CommandMicroLabel(text = "WEB")
        }
    }
}

@Composable
private fun CommandMiniStat(
    label: String,
    value: String,
    detail: String,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.heightIn(min = 78.dp),
        shape = RoundedCornerShape(12.dp),
        color = CompanionPanel,
        border = BorderStroke(1.dp, CompanionHairline),
    ) {
        Column(
            modifier = Modifier.padding(9.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            CommandMicroLabel(text = label)
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium,
                color = CompanionInk,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
            )
            Text(
                text = detail,
                style = MaterialTheme.typography.bodySmall,
                color = CompanionMuted,
                maxLines = 1,
            )
        }
    }
}

@Composable
private fun ConnectionPanel(
    baseUrl: String,
    email: String,
    password: String,
    signedIn: Boolean,
    status: String,
    onBaseUrlChange: (String) -> Unit,
    onEmailChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onSaveUrl: () -> Unit,
    onLogin: () -> Unit,
    onRefresh: () -> Unit,
    onUseProduction: () -> Unit,
    onClearToken: () -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }

    InfoCard(title = "System link") {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Text(
                    text = if (signedIn) "Signed in" else "Fixture mode",
                    style = MaterialTheme.typography.titleSmall,
                    color = if (signedIn) CompanionSuccess else CompanionSecondary,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = status,
                    style = MaterialTheme.typography.bodySmall,
                    color = CompanionMuted,
                )
            }
            TextButton(
                onClick = { expanded = !expanded },
                colors = ButtonDefaults.textButtonColors(contentColor = CompanionPrimary),
            ) {
                Text(if (expanded) "Hide" else "Connection settings")
            }
        }

        if (!expanded) return@InfoCard

        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(
            value = baseUrl,
            onValueChange = onBaseUrlChange,
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Backend base URL") },
            singleLine = true,
            colors = companionTextFieldColors(),
        )
        Spacer(modifier = Modifier.height(10.dp))
        OutlinedTextField(
            value = email,
            onValueChange = onEmailChange,
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Email") },
            singleLine = true,
            colors = companionTextFieldColors(),
        )
        Spacer(modifier = Modifier.height(10.dp))
        OutlinedTextField(
            value = password,
            onValueChange = onPasswordChange,
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Password") },
            visualTransformation = PasswordVisualTransformation(),
            singleLine = true,
            colors = companionTextFieldColors(),
        )
        Spacer(modifier = Modifier.height(10.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            OutlinedButton(
                onClick = onSaveUrl,
                modifier = Modifier.weight(1f),
                border = BorderStroke(1.dp, CompanionBorder),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = CompanionInk),
            ) {
                Text("Save URL")
            }
            OutlinedButton(
                onClick = onUseProduction,
                modifier = Modifier.weight(1f),
                enabled = baseUrl.trim() != PRODUCTION_BACKEND_BASE_URL,
                border = BorderStroke(1.dp, CompanionBorder),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = CompanionInk),
            ) {
                Text("Use production")
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Button(
                onClick = onLogin,
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(containerColor = CompanionPrimary, contentColor = CompanionBackground),
            ) {
                Text("Login")
            }
            OutlinedButton(
                onClick = onRefresh,
                modifier = Modifier.weight(1f),
                enabled = signedIn,
                border = BorderStroke(1.dp, CompanionBorder),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = CompanionInk),
            ) {
                Text("Refresh")
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        TextButton(
            onClick = onClearToken,
            modifier = Modifier.fillMaxWidth(),
            enabled = signedIn,
            colors = ButtonDefaults.textButtonColors(contentColor = CompanionSecondary),
        ) {
            Text("Clear login")
        }
    }
}

fun validateLoginInput(baseUrl: String, email: String, password: String): String? = when {
    baseUrl.isBlank() -> "Backend base URL is required"
    email.isBlank() -> "Email is required"
    password.isBlank() -> "Password is required"
    else -> null
}

private val CommandCanvas = Color(0xFF050607)
private val CommandPanel = Color(0x08FFFFFF)
private val CommandElevated = Color(0x0CFFFFFF)
private val CommandInput = Color(0xFF090A0D)
private val CommandHairline = Color(0x13FFFFFF)
private val CommandAccentHairline = Color(0x22A78BFA)
private val CommandAccent = Color(0xFFA78BFA)
private val CommandText = Color(0xFFF4F1EA)
private val CommandSecondaryText = Color(0xFFB8B0A3)
private val CommandMutedText = Color(0xFF8F8A80)
private val CommandWarning = Color(0xFFF6C177)
private val CommandSuccess = Color(0xFFA7F3D0)

private val CompanionBackground = CommandCanvas
private val CompanionSurface = CommandElevated
private val CompanionHero = Color(0xFF07080A)
private val CompanionPanel = CommandPanel
private val CompanionElevated = CommandElevated
private val CompanionInput = CommandInput
private val CompanionChatAssistant = CommandElevated
private val CompanionChip = CommandPanel
private val CompanionNavSurface = Color(0xFF08090B)
private val CompanionSurfaceSoft = CommandPanel
private val CompanionSelectedTab = Color(0x10FFFFFF)
private val CompanionAccentBorder = CommandAccentHairline
private val CompanionPrimary = CommandAccent
private val CompanionWarm = CommandWarning
private val CompanionInk = CommandText
private val CompanionSecondary = CommandSecondaryText
private val CompanionMuted = CommandMutedText
private val CompanionBorder = Color(0x10FFFFFF)
private val CompanionHairline = CommandHairline
private val CompanionSuccess = CommandSuccess

private fun String.commandStatusLabel(): String {
    val lower = lowercase(Locale.US)
    return when {
        "loading" in lower -> "loading"
        "fixture" in lower -> "fixture"
        "failed" in lower -> "cached"
        "cached" in lower -> "cached"
        "last updated" in lower || "loaded" in lower || "submitted" in lower -> "sync"
        else -> "status"
    }
}

private fun String.commandTitleCase(): String {
    val trimmed = trim()
    if (trimmed.isEmpty()) return "Quiet"

    return trimmed.take(1).uppercase(Locale.US) + trimmed.drop(1).lowercase(Locale.US)
}

private fun MobileToday.commandReadinessScore(): Int {
    val dimensionsScore = checkIn.dimensions?.let { dimensions ->
        ((dimensions.energy + dimensions.focus + dimensions.stressControl + dimensions.optimism) * 10) / 4
    }
    val base = checkIn.score ?: dimensionsScore ?: insight.productivityScore.toInt()
    val productivity = insight.productivityScore.toInt().coerceIn(0, 100)
    val completedBoost = if (checkIn.completedToday) 3 else 0

    return (((base.coerceIn(0, 100) * 2) + productivity) / 3 + completedBoost).coerceIn(0, 100)
}

private fun MobileToday.commandLoadScore(): Int {
    val stressLoad = checkIn.dimensions?.let { dimensions ->
        100 - (dimensions.stressControl.coerceIn(1, 10) * 10)
    } ?: (100 - commandReadinessScore())
    val activeQuestLoad = if (quest.current != null) 6 else 0
    val pendingCheckInLoad = if (checkIn.completedToday) 0 else 5

    return (stressLoad + activeQuestLoad + pendingCheckInLoad).coerceIn(0, 100)
}

private fun MobileToday.commandRecoveryScore(): Int {
    val recovery = checkIn.dimensions?.let { dimensions ->
        ((dimensions.energy + dimensions.optimism + dimensions.stressControl) * 10) / 3
    } ?: (commandReadinessScore() - (commandLoadScore() / 4))

    return recovery.coerceIn(0, 100)
}

private fun MobileToday.commandQuestProgress(): Int {
    quest.current?.let { return it.progress.coerceIn(0, 100) }
    if (user.requiredXP <= 0) return 0

    return ((user.currentXP * 100) / user.requiredXP).coerceIn(0, 100)
}

private fun MobileToday.commandCalibrationGain(): Int {
    if (user.requiredXP <= 0) return (user.level + 5).coerceIn(4, 24)

    return ((user.currentXP * 24) / user.requiredXP).coerceIn(4, 24)
}

private fun MobileToday.commandInterpretation(): String {
    val readiness = commandReadinessScore()
    val load = commandLoadScore()
    val action = recommendedTodayAction(this)

    return when {
        action.label == "Check in" -> "Capture a quick baseline before choosing the next route."
        readiness >= 74 && quest.current != null -> "You are clear enough to ship the smallest useful slice."
        load >= 62 -> "Keep the loop narrow and protect recovery before adding scope."
        else -> "One focused protocol is enough to keep the day moving."
    }
}

private fun formatTodayDay(dayKey: String): String {
    val parsed = runCatching {
        SimpleDateFormat("yyyy-MM-dd", Locale.US).parse(dayKey)
    }.getOrNull()
    return parsed?.let { SimpleDateFormat("EEEE, MMM d", Locale.US).format(it) } ?: dayKey
}

enum class CheckInPreset(val label: String) {
    LOW("Low"),
    OKAY("Okay"),
    STRONG("Strong"),
}

fun checkInPresetRatings(preset: CheckInPreset): List<Int> = when (preset) {
    CheckInPreset.LOW -> listOf(2, 2, 2, 2, 2)
    CheckInPreset.OKAY -> listOf(3, 3, 3, 3, 3)
    CheckInPreset.STRONG -> listOf(4, 4, 4, 4, 4)
}

fun checkInPresetForRatings(ratings: List<Int>): CheckInPreset? =
    CheckInPreset.entries.firstOrNull { preset -> ratings == checkInPresetRatings(preset) }

fun checkInShapeSummary(ratings: List<Int>, selectedPreset: CheckInPreset?): String {
    if (ratings.isEmpty()) return "Current shape: custom - no ratings yet"

    val preset = selectedPreset ?: checkInPresetForRatings(ratings)
    val shape = when (preset) {
        CheckInPreset.LOW -> "low energy"
        CheckInPreset.OKAY -> "steady baseline"
        CheckInPreset.STRONG -> "strong day"
        null -> {
            val average = ratings.average()
            when {
                average < 2.75 -> "lower than usual"
                average > 3.75 -> "higher than usual"
                else -> "mixed baseline"
            }
        }
    }
    val detail = if (ratings.all { it == ratings.first() }) {
        "all ${ratings.first()}/5"
    } else {
        val mostCommon = ratings
            .groupingBy { it }
            .eachCount()
            .maxWithOrNull(compareBy<Map.Entry<Int, Int>> { it.value }.thenBy { it.key })
        if (mostCommon != null && mostCommon.value >= 3) {
            "mostly ${mostCommon.key}/5"
        } else {
            "mixed ${ratings.minOrNull()}-${ratings.maxOrNull()}/5"
        }
    }

    return "Current shape: $shape - $detail"
}

fun updateCheckInRating(ratings: List<Int>, index: Int, delta: Int): List<Int> =
    ratings.mapIndexed { currentIndex, rating ->
        if (currentIndex == index) (rating + delta).coerceIn(1, 5) else rating
    }

fun validateQuickCheckInRatings(ratings: List<Int>): String? =
    try {
        validateDailyCheckInRatings(ratings)
        null
    } catch (_: IllegalArgumentException) {
        "Must provide exactly 5 ratings from 1 to 5."
    }

fun safeStatusErrorMessage(error: Exception, fallback: String): String =
    error.message?.takeUnless(::containsPrivateStatusText) ?: fallback

private fun containsPrivateStatusText(message: String): Boolean {
    val lower = message.lowercase()
    return listOf("token", "password", "bearer", "http://", "https://", "/api/", "@", "{", "}").any { lower.contains(it) }
}

fun cachedStatus(cachedAtEpochMillis: Long): String = "Cached - last updated ${formatTimestamp(cachedAtEpochMillis)}"

fun lastUpdatedStatus(cachedAtEpochMillis: Long): String = "Last updated ${formatTimestamp(cachedAtEpochMillis)}"

private fun formatTimestamp(epochMillis: Long): String =
    DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT).format(Date(epochMillis))

private val CHECK_IN_DIMENSIONS = listOf(
    "Energy",
    "Focus",
    "Stress control",
    "Social connection",
    "Optimism",
)

@Composable
private fun LoadingState(status: String) {
    InfoCard(title = "Today") {
        Text(
            text = status,
            style = MaterialTheme.typography.bodyLarge,
            color = CompanionSecondary,
        )
    }
}

@Composable
private fun InfoCard(title: String, content: @Composable () -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = CompanionPanel),
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.dp, CompanionHairline),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(5.dp),
        ) {
            Text(
                text = title.uppercase(Locale.US),
                style = MaterialTheme.typography.labelSmall,
                color = CompanionMuted,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Medium,
            )
            content()
        }
    }
}

@Composable
private fun companionTextFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedTextColor = CompanionInk,
    unfocusedTextColor = CompanionInk,
    disabledTextColor = CompanionMuted,
    focusedBorderColor = CompanionPrimary,
    unfocusedBorderColor = CompanionBorder,
    disabledBorderColor = CompanionBorder,
    focusedLabelColor = CompanionPrimary,
    unfocusedLabelColor = CompanionMuted,
    cursorColor = CompanionPrimary,
    focusedContainerColor = CompanionInput,
    unfocusedContainerColor = CompanionInput,
    disabledContainerColor = CompanionInput,
)

@Preview(showBackground = true)
@Composable
private fun TodayScreenPreview() {
    TodayScreen(settingsStore = InMemoryTodaySettingsStore())
}
