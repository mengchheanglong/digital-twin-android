package com.transcendiverse.digitaltwin.launcher

import android.Manifest
import android.app.SearchManager
import android.content.Context
import android.content.Intent
import android.content.pm.LauncherApps
import android.net.Uri
import android.os.Bundle
import android.os.UserManager
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import com.transcendiverse.digitaltwin.CompanionDestination
import com.transcendiverse.digitaltwin.CompanionLaunchRequest
import com.transcendiverse.digitaltwin.MainActivity
import com.transcendiverse.digitaltwin.data.NetworkChatRepository
import com.transcendiverse.digitaltwin.data.NetworkCheckInRepository
import com.transcendiverse.digitaltwin.data.NetworkJournalRepository
import com.transcendiverse.digitaltwin.data.NetworkQuestRepository
import com.transcendiverse.digitaltwin.data.NetworkTodayRepository
import com.transcendiverse.digitaltwin.data.SharedPreferencesJournalDraftStore
import com.transcendiverse.digitaltwin.data.SharedPreferencesTodayCacheStore
import com.transcendiverse.digitaltwin.data.SharedPreferencesTodaySettingsStore
import com.transcendiverse.digitaltwin.data.TodaySettings
import com.transcendiverse.digitaltwin.data.generatedJournalTitle
import com.transcendiverse.digitaltwin.data.resolveActiveQuest
import com.transcendiverse.digitaltwin.putCompanionLaunchRequest
import com.transcendiverse.digitaltwin.sync.TodaySyncScheduler
import com.transcendiverse.digitaltwin.widget.TodayWidgetUpdater
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class LauncherActivity : ComponentActivity() {
    private val homeIntentNonceState = mutableStateOf(0)
    private val appIconIntentNonceState = mutableStateOf(0)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val appContext = applicationContext
        val settingsStore = SharedPreferencesTodaySettingsStore(this)
        val cacheStore = SharedPreferencesTodayCacheStore(this)
        val journalDraftStore = SharedPreferencesJournalDraftStore(this)
        val appsRepository = LauncherAppsRepository(
            packageManager = packageManager,
            launcherApps = getSystemService(LauncherApps::class.java),
            userManager = getSystemService(UserManager::class.java),
            selfPackageName = packageName,
        )
        val favoritesStore = LauncherFavoritesStore(this)

        fun loadSummary(): LauncherTodaySummary = LauncherTodaySummary.from(
            cachedToday = cacheStore.load(),
            nowEpochMillis = System.currentTimeMillis(),
        )

        setContent {
            val coroutineScope = rememberCoroutineScope()
            var todaySummary by remember { mutableStateOf(loadSummary()) }
            var apps by remember { mutableStateOf(emptyList<LauncherApp>()) }
            var favoritePackageNames by remember { mutableStateOf(favoritesStore.load()) }
            var isAppDrawerOpen by remember { mutableStateOf(false) }
            var inlineActionStatus by remember { mutableStateOf<String?>(null) }
            var inlineActionBusy by remember { mutableStateOf(false) }
            var journalDraft by remember { mutableStateOf(journalDraftStore.load()) }
            var askPrompt by remember { mutableStateOf("") }
            var askReplyPreview by remember { mutableStateOf<String?>(null) }
            var activeChatId by remember { mutableStateOf<String?>(null) }
            var calendarPermissionGranted by remember { mutableStateOf(appContext.hasLauncherCalendarPermission()) }
            var calendarEvents by remember { mutableStateOf(emptyList<LauncherCalendarEvent>()) }
            val homeIntentNonce by homeIntentNonceState
            val appIconIntentNonce by appIconIntentNonceState
            val startOnTwinPage = remember { intent.isLauncherAppIconIntent() }

            val calendarPermissionLauncher = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.RequestPermission(),
            ) { granted ->
                calendarPermissionGranted = granted
                if (granted) {
                    coroutineScope.launch {
                        calendarEvents = withContext(Dispatchers.IO) {
                            loadUpcomingLauncherCalendarEvents(appContext)
                        }
                    }
                } else {
                    calendarEvents = emptyList()
                }
            }

            suspend fun loadLaunchableApps(): List<LauncherApp> =
                withContext(Dispatchers.IO) { appsRepository.listLaunchableApps() }

            fun refreshLaunchableApps() {
                coroutineScope.launch {
                    apps = loadLaunchableApps()
                }
            }

            fun refreshCalendarEvents() {
                calendarPermissionGranted = appContext.hasLauncherCalendarPermission()
                if (!calendarPermissionGranted) {
                    calendarEvents = emptyList()
                    return
                }

                coroutineScope.launch {
                    calendarEvents = withContext(Dispatchers.IO) {
                        loadUpcomingLauncherCalendarEvents(appContext)
                    }
                }
            }

            suspend fun refreshTodayAfterInlineAction(settings: TodaySettings, successStatus: String) {
                try {
                    val loadedToday = NetworkTodayRepository(
                        baseUrl = settings.baseUrl,
                        token = settings.token,
                    ).getToday()
                    cacheStore.save(loadedToday)
                    TodayWidgetUpdater.update(appContext)
                    todaySummary = loadSummary()
                    TodaySyncScheduler.enqueueOneTime(appContext)
                    inlineActionStatus = successStatus
                } catch (_: Exception) {
                    todaySummary = loadSummary()
                    inlineActionStatus = "$successStatus - refresh failed; cached Today kept"
                }
            }

            fun runInlineAction(action: suspend () -> Unit) {
                if (inlineActionBusy) return

                coroutineScope.launch {
                    inlineActionBusy = true
                    try {
                        action()
                    } finally {
                        inlineActionBusy = false
                    }
                }
            }

            LaunchedEffect(Unit) {
                apps = loadLaunchableApps()
                calendarPermissionGranted = appContext.hasLauncherCalendarPermission()
                if (calendarPermissionGranted) {
                    calendarEvents = withContext(Dispatchers.IO) {
                        loadUpcomingLauncherCalendarEvents(appContext)
                    }
                }
            }

            LauncherScreen(
                todaySummary = todaySummary,
                apps = apps,
                favoriteApps = favoriteLauncherApps(apps, favoritePackageNames),
                favoritePackageNames = favoritePackageNames,
                isAppDrawerOpen = isAppDrawerOpen,
                homeIntentNonce = homeIntentNonce,
                appIconIntentNonce = appIconIntentNonce,
                startOnTwinPage = startOnTwinPage,
                journalDraft = journalDraft,
                askPrompt = askPrompt,
                inlineActionStatus = inlineActionStatus,
                inlineActionBusy = inlineActionBusy,
                askReplyPreview = askReplyPreview,
                calendarEvents = calendarEvents,
                calendarPermissionGranted = calendarPermissionGranted,
                onRequestCalendarPermission = {
                    calendarPermissionLauncher.launch(Manifest.permission.READ_CALENDAR)
                },
                onRefreshCalendarEvents = ::refreshCalendarEvents,
                onOpenAppDrawer = {
                    isAppDrawerOpen = true
                    refreshLaunchableApps()
                },
                onCloseAppDrawer = {
                    isAppDrawerOpen = false
                },
                onOpenGoogleSearch = ::openGoogleSearch,
                onLaunchApp = { app ->
                    if (appsRepository.launchApp(app, ::startActivity)) {
                        isAppDrawerOpen = false
                    }
                },
                onToggleFavorite = { app ->
                    favoritePackageNames = favoritesStore.toggle(app.packageName)
                },
                onOpenSettings = {
                    startActivity(Intent(Settings.ACTION_SETTINGS))
                },
                onJournalDraftChange = { draft ->
                    journalDraft = draft
                    if (inlineActionStatus == "Local draft saved") {
                        inlineActionStatus = "Draft kept on this phone"
                    } else if (inlineActionStatus == "Journal synced to web") {
                        inlineActionStatus = "Draft changed; save again to sync"
                    }
                },
                onAskPromptChange = { prompt ->
                    askPrompt = prompt
                    askReplyPreview = null
                },
                onOpenCompanionDestination = { request ->
                    startActivity(companionTaskIntent(this, request))
                },
                onSaveInlineCheckIn = { ratings ->
                    runInlineAction {
                        val cleanRatings = ratings.take(5).map { it.coerceIn(1, 5) }
                        if (cleanRatings.size != 5) {
                            inlineActionStatus = "Check-in needs five ratings"
                            return@runInlineAction
                        }

                        val settings = settingsStore.load()
                        if (!settings.hasCredentials()) {
                            inlineActionStatus = "Sign in from companion to save check-in"
                            return@runInlineAction
                        }

                        try {
                            val result = NetworkCheckInRepository(
                                baseUrl = settings.baseUrl,
                                token = settings.token,
                            ).submitDaily(cleanRatings)
                            refreshTodayAfterInlineAction(
                                settings = settings,
                                successStatus = "Check-in saved: ${result.percentage}%",
                            )
                        } catch (error: Exception) {
                            inlineActionStatus = "Check-in failed: ${safeInlineStatusError(error, "Unable to save check-in")}"
                        }
                    }
                },
                onNudgeInlineQuest = {
                    runInlineAction {
                        val settings = settingsStore.load()
                        if (!settings.hasCredentials()) {
                            inlineActionStatus = "Sign in from companion to update quest"
                            return@runInlineAction
                        }

                        val currentQuest = cacheStore.load()?.today?.quest?.current
                        if (currentQuest == null) {
                            inlineActionStatus = "No active quest in cached Today"
                            return@runInlineAction
                        }

                        try {
                            val repository = NetworkQuestRepository(
                                baseUrl = settings.baseUrl,
                                token = settings.token,
                            )
                            val activeQuest = resolveActiveQuest(currentQuest, repository.listQuests())
                            if (activeQuest == null || activeQuest.id.isBlank()) {
                                inlineActionStatus = "No active quest id found"
                                return@runInlineAction
                            }

                            val nextProgress = (activeQuest.progress + 10).coerceIn(0, 100)
                            repository.updateProgress(activeQuest.id, nextProgress)
                            refreshTodayAfterInlineAction(
                                settings = settings,
                                successStatus = "Quest nudged to $nextProgress%",
                            )
                        } catch (error: Exception) {
                            inlineActionStatus = "Quest update failed: ${safeInlineStatusError(error, "Unable to update quest")}"
                        }
                    }
                },
                onSaveInlineJournalDraft = { draft ->
                    runInlineAction {
                        val cleanDraft = draft.trim()
                        if (cleanDraft.isBlank()) {
                            inlineActionStatus = "Write a local draft first"
                            return@runInlineAction
                        }

                        journalDraftStore.save(cleanDraft)
                        journalDraft = cleanDraft
                        val settings = settingsStore.load()
                        if (!settings.hasCredentials()) {
                            inlineActionStatus = "Local draft saved"
                            return@runInlineAction
                        }

                        try {
                            NetworkJournalRepository(
                                baseUrl = settings.baseUrl,
                                token = settings.token,
                            ).createEntry(
                                title = generatedJournalTitle(cleanDraft),
                                content = cleanDraft,
                                mood = cacheStore.load()?.today?.user?.mood?.label,
                            )
                            inlineActionStatus = "Journal synced to web"
                            try {
                                val loadedToday = NetworkTodayRepository(
                                    baseUrl = settings.baseUrl,
                                    token = settings.token,
                                ).getToday()
                                cacheStore.save(loadedToday)
                                TodayWidgetUpdater.update(appContext)
                            } catch (_: Exception) {
                                // The journal save succeeded; keep the user-facing status on the completed sync.
                            }
                            todaySummary = loadSummary()
                            TodaySyncScheduler.enqueueOneTime(appContext)
                        } catch (error: Exception) {
                            inlineActionStatus = "Journal sync failed: ${safeInlineStatusError(error, "Unable to save journal")}"
                        }
                    }
                },
                onSendInlineAsk = { prompt ->
                    runInlineAction {
                        val message = prompt.trim()
                        if (message.isBlank()) {
                            inlineActionStatus = "Write an Ask prompt first"
                            return@runInlineAction
                        }

                        val settings = settingsStore.load()
                        if (!settings.hasCredentials()) {
                            inlineActionStatus = "Sign in from companion to send"
                            return@runInlineAction
                        }

                        try {
                            val result = NetworkChatRepository(
                                baseUrl = settings.baseUrl,
                                token = settings.token,
                            ).sendMessage(message = message, chatId = activeChatId)
                            activeChatId = result.chatId
                            val reply = safeInlinePreview(result.reply, fallback = "Twin replied")
                            askReplyPreview = reply
                            inlineActionStatus = reply
                        } catch (error: Exception) {
                            inlineActionStatus = "Ask failed: ${safeInlineStatusError(error, "Unable to send Ask")}"
                        }
                    }
                },
            )
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        when {
            intent.isLauncherHomeIntent() -> homeIntentNonceState.value += 1
            intent.isLauncherAppIconIntent() -> appIconIntentNonceState.value += 1
        }
    }

    private fun openGoogleSearch() {
        val webSearchIntent = Intent(Intent.ACTION_WEB_SEARCH)
            .putExtra(SearchManager.QUERY, "")
        val googleFallbackIntent = Intent(
            Intent.ACTION_VIEW,
            Uri.parse("https://www.google.com"),
        )

        runCatching {
            startActivity(webSearchIntent)
        }.recoverCatching {
            startActivity(googleFallbackIntent)
        }
    }
}

private fun Intent.isLauncherHomeIntent(): Boolean =
    action == Intent.ACTION_MAIN &&
        (hasCategory(Intent.CATEGORY_HOME) || hasCategory(Intent.CATEGORY_DEFAULT) || categories.isNullOrEmpty())

private fun Intent.isLauncherAppIconIntent(): Boolean =
    action == Intent.ACTION_MAIN &&
        hasCategory(Intent.CATEGORY_LAUNCHER) &&
        !hasCategory(Intent.CATEGORY_HOME)

private fun companionTaskIntent(
    context: Context,
    request: CompanionLaunchRequest = CompanionLaunchRequest(destination = CompanionDestination.Today),
): Intent =
    Intent(context, MainActivity::class.java)
        .addFlags(
            Intent.FLAG_ACTIVITY_NEW_TASK or
                Intent.FLAG_ACTIVITY_CLEAR_TOP or
                Intent.FLAG_ACTIVITY_SINGLE_TOP,
        )
        .putCompanionLaunchRequest(request)

private fun safeInlineStatusError(error: Exception, fallback: String): String =
    error.message?.takeUnless(::containsPrivateInlineStatusText) ?: fallback

private fun safeInlinePreview(text: String, fallback: String): String {
    val clean = text.trim().replace(Regex("\\s+"), " ")
    return clean.takeUnless { it.isBlank() || containsPrivateInlineStatusText(it) } ?: fallback
}

private fun containsPrivateInlineStatusText(text: String): Boolean {
    val lower = text.lowercase()
    return listOf("token", "password", "bearer", "http://", "https://", "/api/", "@", "{", "}").any {
        lower.contains(it)
    }
}
