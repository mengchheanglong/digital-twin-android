package com.transcendiverse.digitaltwin.launcher

import android.text.format.DateFormat
import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.transcendiverse.digitaltwin.CompanionDestination
import com.transcendiverse.digitaltwin.CompanionLaunchRequest
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

internal val LauncherBackgroundColor = Color(0xFF050607)
internal val LauncherElevatedColor = Color(0x0CFFFFFF)
internal val LauncherInputColor = Color(0xFF090A0D)
internal val LauncherAccentColor = Color(0xFFA78BFA)
internal val LauncherTwinSignalColor = Color(0xFF38BDF8)
internal val LauncherTextColor = Color(0xFFF4F1EA)
internal val LauncherSecondaryTextColor = Color(0xFFB8B0A3)
internal val LauncherMutedActionColor = Color(0xFF8F8A80)
internal val LauncherWarmColor = Color(0xFFF6C177)
internal val LauncherBorderColor = Color(0x10FFFFFF)
internal val LauncherHairlineColor = Color(0x13FFFFFF)
internal val LauncherAccentBorderColor = Color(0x22A78BFA)
internal val LauncherSurfaceSoftColor = Color(0x08FFFFFF)
internal val LauncherSelectedSurfaceColor = Color(0x10FFFFFF)

private val LauncherPageAnimationSpec = tween<Float>(
    durationMillis = 280,
    easing = FastOutSlowInEasing,
)

private enum class TwinInlineModule {
    CheckIn,
    Quest,
    Journal,
    AskTwin,
}

private enum class TwinPanelTab(
    val label: String,
) {
    Twin("Twin"),
    Flow("Flow"),
}

private data class TwinTodoItem(
    val label: String,
    val done: Boolean,
)

private data class TwinChatMessage(
    val fromTwin: Boolean,
    val text: String,
)

private data class TwinMoodOption(
    val label: String,
    val sublabel: String,
    val icon: String,
    val accentColor: Color,
)

private data class TwinCheckInDimension(
    val label: String,
    val question: String,
    val icon: String,
)

private val TwinMoodOptions = listOf(
    TwinMoodOption("Drained", "Struggling today", "☹", Color(0xFFEF4444)),
    TwinMoodOption("Low", "Not my best", "—", Color(0xFFF59E0B)),
    TwinMoodOption("Okay", "Holding steady", "☺", Color(0xFF22C55E)),
    TwinMoodOption("Good", "Feeling strong", "🔥", Color(0xFF38BDF8)),
    TwinMoodOption("Energized", "On top of the world", "⚡", Color(0xFFA78BFA)),
)

private val TwinCheckInDimensions = listOf(
    TwinCheckInDimension("Energy", "How has your emotional energy been today?", "⚡"),
    TwinCheckInDimension("Focus", "How focused did you feel on key priorities?", "🧠"),
    TwinCheckInDimension("Stress Control", "How steady was your stress level today?", "☺"),
    TwinCheckInDimension("Social Connection", "How connected did you feel to people around you?", "💬"),
    TwinCheckInDimension("Optimism", "How positive do you feel about tomorrow?", "↗"),
)

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun LauncherScreen(
    todaySummary: LauncherTodaySummary,
    apps: List<LauncherApp>,
    favoriteApps: List<LauncherApp>,
    favoritePackageNames: Set<String>,
    isAppDrawerOpen: Boolean,
    homeIntentNonce: Int,
    appIconIntentNonce: Int,
    startOnTwinPage: Boolean,
    journalDraft: String,
    askPrompt: String,
    inlineActionStatus: String?,
    inlineActionBusy: Boolean,
    askReplyPreview: String?,
    calendarEvents: List<LauncherCalendarEvent>,
    calendarPermissionGranted: Boolean,
    onRequestCalendarPermission: () -> Unit,
    onRefreshCalendarEvents: () -> Unit,
    onOpenAppDrawer: () -> Unit,
    onCloseAppDrawer: () -> Unit,
    onOpenGoogleSearch: () -> Unit,
    onLaunchApp: (LauncherApp) -> Unit,
    onToggleFavorite: (LauncherApp) -> Unit,
    onOpenSettings: () -> Unit,
    onJournalDraftChange: (String) -> Unit,
    onAskPromptChange: (String) -> Unit,
    onOpenCompanionDestination: (CompanionLaunchRequest) -> Unit,
    onSaveInlineCheckIn: (List<Int>) -> Unit,
    onNudgeInlineQuest: () -> Unit,
    onSaveInlineJournalDraft: (String) -> Unit,
    onSendInlineAsk: (String) -> Unit,
) {
    val initialPage = if (startOnTwinPage) TWIN_PAGE_INDEX else HOME_PAGE_INDEX
    val pagerState = rememberPagerState(initialPage = initialPage, pageCount = { LAUNCHER_PAGE_COUNT })
    val scope = rememberCoroutineScope()

    fun openTwinPage() {
        onCloseAppDrawer()
        scope.launch {
            pagerState.animateScrollToPage(
                page = TWIN_PAGE_INDEX,
                animationSpec = LauncherPageAnimationSpec,
            )
        }
    }

    fun openHomePage() {
        onCloseAppDrawer()
        scope.launch {
            pagerState.animateScrollToPage(
                page = HOME_PAGE_INDEX,
                animationSpec = LauncherPageAnimationSpec,
            )
        }
    }

    fun openAppsPage() {
        onOpenAppDrawer()
        scope.launch {
            pagerState.animateScrollToPage(
                page = APPS_PAGE_INDEX,
                animationSpec = LauncherPageAnimationSpec,
            )
        }
    }

    BackHandler(enabled = pagerState.currentPage != HOME_PAGE_INDEX || isAppDrawerOpen) {
        openHomePage()
    }

    LaunchedEffect(isAppDrawerOpen) {
        if (isAppDrawerOpen && pagerState.currentPage != APPS_PAGE_INDEX) {
            pagerState.animateScrollToPage(
                page = APPS_PAGE_INDEX,
                animationSpec = LauncherPageAnimationSpec,
            )
        }
    }

    LaunchedEffect(homeIntentNonce) {
        if (homeIntentNonce > 0) {
            onCloseAppDrawer()
            pagerState.animateScrollToPage(
                page = HOME_PAGE_INDEX,
                animationSpec = LauncherPageAnimationSpec,
            )
        }
    }

    LaunchedEffect(appIconIntentNonce) {
        if (appIconIntentNonce > 0) {
            onCloseAppDrawer()
            pagerState.animateScrollToPage(
                page = TWIN_PAGE_INDEX,
                animationSpec = LauncherPageAnimationSpec,
            )
        }
    }

    MaterialTheme {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = LauncherBackgroundColor,
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                LauncherBackgroundColor,
                                Color(0xFF07080A),
                                Color(0xFF050607),
                            ),
                        ),
                    ),
            ) {
                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier.fillMaxSize(),
                ) { page ->
                    when (page) {
                        TWIN_PAGE_INDEX -> TwinLauncherPage(
                            todaySummary = todaySummary,
                            onOpenSettings = onOpenSettings,
                            journalDraft = journalDraft,
                            askPrompt = askPrompt,
                            inlineActionStatus = inlineActionStatus,
                            inlineActionBusy = inlineActionBusy,
                            askReplyPreview = askReplyPreview,
                            calendarEvents = calendarEvents,
                            calendarPermissionGranted = calendarPermissionGranted,
                            onRequestCalendarPermission = onRequestCalendarPermission,
                            onRefreshCalendarEvents = onRefreshCalendarEvents,
                            onJournalDraftChange = onJournalDraftChange,
                            onAskPromptChange = onAskPromptChange,
                            onOpenCompanionDestination = onOpenCompanionDestination,
                            onSaveInlineCheckIn = onSaveInlineCheckIn,
                            onNudgeInlineQuest = onNudgeInlineQuest,
                            onSaveInlineJournalDraft = onSaveInlineJournalDraft,
                            onSendInlineAsk = onSendInlineAsk,
                        )

                        HOME_PAGE_INDEX -> LauncherHomePage(
                            todaySummary = todaySummary,
                            apps = apps,
                            favoriteApps = favoriteApps,
                            onOpenAppsPage = ::openAppsPage,
                            onOpenGoogleSearch = onOpenGoogleSearch,
                            onLaunchApp = onLaunchApp,
                            onOpenCompanionDestination = onOpenCompanionDestination,
                            onOpenSettings = onOpenSettings,
                        )

                        APPS_PAGE_INDEX -> LauncherAppsPage(
                            apps = apps,
                            favoritePackageNames = favoritePackageNames,
                            onLaunchApp = onLaunchApp,
                            onToggleFavorite = onToggleFavorite,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun LauncherHomePage(
    todaySummary: LauncherTodaySummary,
    apps: List<LauncherApp>,
    favoriteApps: List<LauncherApp>,
    onOpenAppsPage: () -> Unit,
    onOpenGoogleSearch: () -> Unit,
    onLaunchApp: (LauncherApp) -> Unit,
    onOpenCompanionDestination: (CompanionLaunchRequest) -> Unit,
    onOpenSettings: () -> Unit,
) {
    val clock = rememberLauncherClockLabels()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(start = 30.dp, top = 52.dp, end = 30.dp, bottom = 18.dp),
    ) {
        LauncherIdentityBlock(
            clock = clock,
            state = todaySummary.launcherStateLine(),
            next = todaySummary.launcherNextLine(),
        )
        Spacer(modifier = Modifier.height(22.dp))
        LauncherGoogleSearchWidget(onOpenGoogleSearch = onOpenGoogleSearch)
        Spacer(modifier = Modifier.height(24.dp))
        LauncherHomeRows(
            apps = apps,
            favoriteApps = favoriteApps,
            onLaunchApp = onLaunchApp,
            onOpenAppsPage = onOpenAppsPage,
            modifier = Modifier.weight(1f),
        )
        LauncherHomeFooter(
            onOpenAppsPage = onOpenAppsPage,
            onOpenCompanion = {
                onOpenCompanionDestination(
                    CompanionLaunchRequest(destination = CompanionDestination.Today),
                )
            },
            onOpenSettings = onOpenSettings,
        )
    }
}

@Composable
private fun LauncherHomeFooter(
    onOpenAppsPage: () -> Unit,
    onOpenCompanion: () -> Unit,
    onOpenSettings: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 44.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        LauncherTextAction(text = "Apps", onClick = onOpenAppsPage)
        LauncherTextAction(text = "Companion", onClick = onOpenCompanion)
        LauncherTextAction(text = "Settings", onClick = onOpenSettings)
    }
}

@Composable
private fun LauncherIdentityBlock(
    clock: LauncherClockLabels,
    state: String,
    next: String,
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(3.dp),
    ) {
        Text(
            text = clock.day.uppercase(Locale.US),
            style = MaterialTheme.typography.headlineLarge,
            color = LauncherTextColor,
            fontSize = 34.sp,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center,
            maxLines = 1,
        )
        Text(
            text = "${clock.time} \u00B7 ${clock.date}",
            style = MaterialTheme.typography.titleMedium,
            color = LauncherSecondaryTextColor,
            fontSize = 20.sp,
            fontWeight = FontWeight.Normal,
            textAlign = TextAlign.Center,
            maxLines = 1,
        )
        Spacer(modifier = Modifier.height(18.dp))
        Text(
            text = state,
            style = MaterialTheme.typography.bodyMedium,
            color = LauncherSecondaryTextColor,
            fontSize = 15.sp,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            text = "next: $next",
            style = MaterialTheme.typography.bodySmall,
            color = LauncherMutedActionColor,
            fontSize = 14.sp,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun LauncherGoogleSearchWidget(
    onOpenGoogleSearch: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 50.dp)
            .clickable(onClick = onOpenGoogleSearch),
        shape = RoundedCornerShape(999.dp),
        color = LauncherSurfaceSoftColor,
        border = BorderStroke(1.dp, LauncherBorderColor),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "G",
                style = MaterialTheme.typography.titleMedium,
                color = LauncherTextColor,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
            )
            Text(
                text = "Search with Google",
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.bodyMedium,
                color = LauncherSecondaryTextColor,
                fontSize = 15.sp,
                fontWeight = FontWeight.Normal,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun LauncherAppsPage(
    apps: List<LauncherApp>,
    favoritePackageNames: Set<String>,
    onLaunchApp: (LauncherApp) -> Unit,
    onToggleFavorite: (LauncherApp) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 28.dp, vertical = 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        LauncherAppDrawer(
            apps = apps,
            favoritePackageNames = favoritePackageNames,
            onLaunchApp = onLaunchApp,
            onToggleFavorite = onToggleFavorite,
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
        )
    }
}

@Composable
private fun LauncherHomeRows(
    apps: List<LauncherApp>,
    favoriteApps: List<LauncherApp>,
    onLaunchApp: (LauncherApp) -> Unit,
    onOpenAppsPage: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val homeApps = favoriteApps.takeIf { it.isNotEmpty() } ?: apps.take(MAX_HOME_FAVORITE_APPS)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        if (homeApps.isEmpty()) {
            LauncherEmptyHomeApps(onOpenAppsPage = onOpenAppsPage)
        } else {
            homeApps.take(MAX_HOME_FAVORITE_APPS).forEach { app ->
                LauncherHomeTextRow(
                    text = app.displayLabel,
                    onClick = { onLaunchApp(app) },
                )
            }
            if (favoriteApps.size > MAX_HOME_FAVORITE_APPS) {
                Text(
                    text = "+${favoriteApps.size - MAX_HOME_FAVORITE_APPS} more in Apps",
                    modifier = Modifier.padding(top = 4.dp, bottom = 8.dp),
                    style = MaterialTheme.typography.bodySmall,
                    color = LauncherMutedActionColor,
                )
            }
        }
    }
}

@Composable
private fun LauncherEmptyHomeApps(
    onOpenAppsPage: () -> Unit,
) {
    Column(
        modifier = Modifier.padding(bottom = 16.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(
            text = "No allowed apps yet",
            style = MaterialTheme.typography.bodyLarge,
            color = LauncherTextColor,
            fontWeight = FontWeight.Medium,
        )
        TextButton(
            onClick = onOpenAppsPage,
            contentPadding = PaddingValues(0.dp),
            colors = ButtonDefaults.textButtonColors(
                contentColor = LauncherMutedActionColor,
            ),
        ) {
            Text(
                text = "search apps",
                style = MaterialTheme.typography.bodySmall,
                color = LauncherMutedActionColor,
            )
        }
    }
}

@Composable
private fun LauncherHomeTextRow(
    text: String,
    color: Color = LauncherTextColor,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 42.dp)
            .clickable(onClick = onClick)
            .padding(vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodyLarge,
            color = color,
            fontSize = 21.sp,
            fontWeight = FontWeight.Normal,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun TwinLauncherPage(
    todaySummary: LauncherTodaySummary,
    onOpenSettings: () -> Unit,
    journalDraft: String,
    askPrompt: String,
    inlineActionStatus: String?,
    inlineActionBusy: Boolean,
    askReplyPreview: String?,
    calendarEvents: List<LauncherCalendarEvent>,
    calendarPermissionGranted: Boolean,
    onRequestCalendarPermission: () -> Unit,
    onRefreshCalendarEvents: () -> Unit,
    onJournalDraftChange: (String) -> Unit,
    onAskPromptChange: (String) -> Unit,
    onOpenCompanionDestination: (CompanionLaunchRequest) -> Unit,
    onSaveInlineCheckIn: (List<Int>) -> Unit,
    onNudgeInlineQuest: () -> Unit,
    onSaveInlineJournalDraft: (String) -> Unit,
    onSendInlineAsk: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    var selectedTab by rememberSaveable { mutableStateOf(TwinPanelTab.Twin) }
    var checkInRatings by rememberSaveable { mutableStateOf(listOf(3, 3, 3, 3, 3)) }
    var checkInCompleted by rememberSaveable { mutableStateOf(false) }
    var todoItems by remember {
        mutableStateOf(
            listOf(
                TwinTodoItem("Check in", false),
                TwinTodoItem("Ask Twin", false),
                TwinTodoItem("Review calendar", false),
            ),
        )
    }
    var todoDraft by rememberSaveable { mutableStateOf("") }
    var todoComposerOpen by rememberSaveable { mutableStateOf(false) }
    var flowNoteDraft by rememberSaveable { mutableStateOf("") }
    var chatMessages by remember {
        mutableStateOf(
            listOf(
                TwinChatMessage(
                    fromTwin = true,
                    text = "Ask me anything. I’ll keep this chat here.",
                ),
            ),
        )
    }
    var lastTwinReply by remember { mutableStateOf<String?>(null) }
    var chatAwaitingStatus by remember { mutableStateOf(false) }
    var chatStatusBaseline by remember { mutableStateOf<String?>(null) }

    fun sendLauncherChatPrompt(prompt: String) {
        val message = prompt.trim()
        if (message.isBlank() || inlineActionBusy) return

        chatMessages = chatMessages + TwinChatMessage(fromTwin = false, text = message)
        chatStatusBaseline = inlineActionStatus?.trim()
        chatAwaitingStatus = true
        onSendInlineAsk(message)
        onAskPromptChange("")
    }

    LaunchedEffect(askReplyPreview) {
        val reply = askReplyPreview?.trim().orEmpty()
        if (reply.isNotBlank() && reply != lastTwinReply) {
            chatMessages = chatMessages + TwinChatMessage(fromTwin = true, text = reply)
            lastTwinReply = reply
            chatAwaitingStatus = false
        }
    }

    LaunchedEffect(inlineActionStatus, chatAwaitingStatus) {
        val status = inlineActionStatus?.trim().orEmpty()
        val baseline = chatStatusBaseline.orEmpty()
        if (chatAwaitingStatus && status.isNotBlank() && status != baseline && status != lastTwinReply) {
            chatMessages = chatMessages + TwinChatMessage(fromTwin = true, text = status)
            lastTwinReply = status
            chatAwaitingStatus = false
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(start = 10.dp, top = 42.dp, end = 10.dp, bottom = 14.dp),
    ) {
        TwinWidgetHeader(
            selectedTab = selectedTab,
            onTabSelected = { selectedTab = it },
            onOpenCompanion = {
                onOpenCompanionDestination(
                    CompanionLaunchRequest(destination = CompanionDestination.Today),
                )
            },
            onOpenSettings = onOpenSettings,
        )
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            when (selectedTab) {
                TwinPanelTab.Twin -> {
                    TwinQuickCheckInWidget(
                        ratings = checkInRatings,
                        completed = checkInCompleted,
                        status = inlineActionStatus,
                        busy = inlineActionBusy,
                        onRatingsChange = {
                            checkInRatings = it
                            checkInCompleted = false
                        },
                        onSave = {
                            checkInCompleted = true
                            onSaveInlineCheckIn(checkInRatings)
                        },
                        onEdit = { checkInCompleted = false },
                        onOpenFullCheckIn = {
                            onOpenCompanionDestination(
                                CompanionLaunchRequest(destination = CompanionDestination.CheckIn),
                            )
                        },
                    )
                    TwinChatWidget(
                        messages = chatMessages,
                        draft = askPrompt,
                        busy = inlineActionBusy,
                        onDraftChange = onAskPromptChange,
                        onSend = { sendLauncherChatPrompt(askPrompt) },
                        onOpenChat = {
                            onOpenCompanionDestination(
                                CompanionLaunchRequest(
                                    destination = CompanionDestination.Ask,
                                    payload = askPrompt.takeIf { it.isNotBlank() },
                                ),
                            )
                        },
                    )
                }

                TwinPanelTab.Flow -> {
                    TwinTodoWidget(
                        items = todoItems,
                        draft = todoDraft,
                        composerOpen = todoComposerOpen,
                        onDraftChange = { todoDraft = it },
                        onComposerOpenChange = { todoComposerOpen = it },
                        onToggle = { index ->
                            todoItems = todoItems.mapIndexed { itemIndex, item ->
                                if (itemIndex == index) item.copy(done = !item.done) else item
                            }
                        },
                        onDelete = { index ->
                            todoItems = todoItems.filterIndexed { itemIndex, _ -> itemIndex != index }
                        },
                        onAdd = {
                            val label = todoDraft.trim()
                            if (label.isNotEmpty()) {
                                todoItems = listOf(TwinTodoItem(label, false)) + todoItems
                                todoDraft = ""
                                todoComposerOpen = false
                            }
                        },
                    )
                    TwinNotesWidget(
                        note = flowNoteDraft,
                        busy = inlineActionBusy,
                        onNoteChange = { flowNoteDraft = it },
                        onDone = {
                            val note = flowNoteDraft.trim()
                            if (note.isNotEmpty()) {
                                onSaveInlineJournalDraft(note)
                            }
                        },
                    )
                    TwinCalendarWidget(
                        events = calendarEvents,
                        permissionGranted = calendarPermissionGranted,
                        onConnect = onRequestCalendarPermission,
                        onRefresh = onRefreshCalendarEvents,
                    )
                }
            }
        }
    }
}

@Composable
private fun TwinWidgetHeader(
    selectedTab: TwinPanelTab,
    onTabSelected: (TwinPanelTab) -> Unit,
    onOpenCompanion: () -> Unit,
    onOpenSettings: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Surface(
            modifier = Modifier
                .weight(1f)
                .height(50.dp),
            shape = RoundedCornerShape(18.dp),
            color = LauncherInputColor,
            border = BorderStroke(1.dp, LauncherHairlineColor),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                TwinHeaderTab(
                    tab = TwinPanelTab.Twin,
                    selected = selectedTab == TwinPanelTab.Twin,
                    modifier = Modifier.weight(1f),
                    onSelect = { onTabSelected(TwinPanelTab.Twin) },
                )
                TwinHeaderTab(
                    tab = TwinPanelTab.Flow,
                    selected = selectedTab == TwinPanelTab.Flow,
                    modifier = Modifier.weight(1f),
                    onSelect = { onTabSelected(TwinPanelTab.Flow) },
                )
            }
        }
        LauncherPanelTinyAction(text = "open", onClick = onOpenCompanion)
        LauncherPanelTinyAction(text = "⚙", onClick = onOpenSettings)
    }
}

@Composable
private fun TwinHeaderTab(
    tab: TwinPanelTab,
    selected: Boolean,
    modifier: Modifier = Modifier,
    onSelect: () -> Unit,
) {
    Surface(
        modifier = modifier
            .height(42.dp)
            .clickable(onClick = onSelect),
        shape = RoundedCornerShape(14.dp),
        color = if (selected) LauncherSelectedSurfaceColor else Color.Transparent,
        border = BorderStroke(
            width = 1.dp,
            color = if (selected) LauncherAccentBorderColor else Color.Transparent,
        ),
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = tab.label,
                style = MaterialTheme.typography.titleMedium,
                color = if (selected) LauncherTextColor else LauncherSecondaryTextColor,
                fontSize = 18.sp,
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
                maxLines = 1,
            )
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .width(if (selected) 34.dp else 18.dp)
                    .height(2.dp)
                    .background(
                        if (selected) LauncherAccentColor else LauncherMutedActionColor.copy(alpha = 0.36f),
                        RoundedCornerShape(999.dp),
                    ),
            )
        }
    }
}

@Composable
private fun TwinWidgetSection(
    label: String,
    caption: String,
    content: @Composable () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Bottom,
        ) {
            Text(
                text = label.uppercase(Locale.US),
                style = MaterialTheme.typography.labelLarge,
                color = LauncherAccentColor,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
            )
            Text(
                text = caption,
                style = MaterialTheme.typography.labelSmall,
                color = LauncherMutedActionColor,
                fontSize = 11.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        content()
    }
}

@Composable
private fun TwinWidgetFrame(
    title: String,
    modifier: Modifier = Modifier,
    trailingText: String? = null,
    onTrailingClick: (() -> Unit)? = null,
    twinAccent: Boolean = false,
    content: @Composable () -> Unit,
) {
    TwinCommandSurface(modifier = modifier.fillMaxWidth(), emphasized = twinAccent) {
        Column(
            modifier = Modifier.padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                TwinMicroLabel(text = title)
                if (trailingText != null && onTrailingClick != null) {
                    LauncherPanelTinyAction(text = trailingText, onClick = onTrailingClick)
                } else if (trailingText != null) {
                    TwinLabelPill(text = trailingText)
                }
            }
            content()
        }
    }
}

@Composable
private fun TwinQuickCheckInWidget(
    ratings: List<Int>,
    completed: Boolean,
    status: String?,
    busy: Boolean,
    onRatingsChange: (List<Int>) -> Unit,
    onSave: () -> Unit,
    onEdit: () -> Unit,
    onOpenFullCheckIn: () -> Unit,
) {
    val safeRatings = remember(ratings) {
        List(TwinCheckInDimensions.size) { index -> ratings.getOrElse(index) { 3 }.coerceIn(1, 5) }
    }
    var currentIndex by rememberSaveable { mutableStateOf(0) }
    val activeIndex = currentIndex.coerceIn(0, TwinCheckInDimensions.lastIndex)
    val activeDimension = TwinCheckInDimensions[activeIndex]
    val selectedRating = safeRatings[activeIndex]
    val selectedMood = TwinMoodOptions[selectedRating - 1]
    val isLastQuestion = activeIndex == TwinCheckInDimensions.lastIndex

    fun updateCurrentRating(value: Int) {
        onRatingsChange(
            safeRatings.mapIndexed { itemIndex, oldValue ->
                if (itemIndex == activeIndex) value.coerceIn(1, 5) else oldValue
            },
        )
    }

    TwinWidgetFrame(
        title = "CHECK-IN",
        trailingText = "open",
        onTrailingClick = onOpenFullCheckIn,
        twinAccent = true,
    ) {
        if (completed) {
            TwinCheckInDoneState(
                ratings = safeRatings,
                status = status,
                busy = busy,
                onEdit = onEdit,
            )
        } else {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "How are you today?",
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.bodyLarge,
                    color = LauncherTextColor,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = "${activeIndex + 1}/${TwinCheckInDimensions.size}",
                    style = MaterialTheme.typography.labelLarge,
                    color = LauncherSecondaryTextColor,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                )
            }
            TwinOasisProgress(currentIndex = activeIndex, count = TwinCheckInDimensions.size)
            TwinCheckInQuestionCard(
                dimension = activeDimension,
                selectedRating = selectedRating,
                selectedMood = selectedMood,
                onRatingSelected = ::updateCurrentRating,
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (activeIndex > 0) {
                    TwinCommandButton(
                        text = "◀ back",
                        modifier = Modifier.weight(1f),
                        enabled = !busy,
                        onClick = { currentIndex = (activeIndex - 1).coerceAtLeast(0) },
                    )
                } else {
                    status
                        ?.takeUnless { it.contains("send", ignoreCase = true) || it.contains("ask", ignoreCase = true) }
                        ?.let { currentStatus ->
                            Text(
                                text = currentStatus,
                                modifier = Modifier.weight(1f),
                                style = MaterialTheme.typography.bodySmall,
                                color = LauncherMutedActionColor,
                                fontSize = 11.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        } ?: Spacer(modifier = Modifier.weight(1f))
                }
                TwinCommandButton(
                    text = when {
                        busy -> "saving"
                        isLastQuestion -> "complete"
                        else -> "next ▶"
                    },
                    modifier = Modifier.weight(1f),
                    emphasized = true,
                    enabled = !busy,
                    onClick = {
                        if (isLastQuestion) {
                            onSave()
                        } else {
                            currentIndex = (activeIndex + 1).coerceAtMost(TwinCheckInDimensions.lastIndex)
                        }
                    },
                )
            }
        }
    }
}

@Composable
private fun TwinCheckInDoneState(
    ratings: List<Int>,
    status: String?,
    busy: Boolean,
    onEdit: () -> Unit,
) {
    val summary = TwinCheckInDimensions.mapIndexed { index, dimension ->
        "${dimension.label} ${ratings.getOrElse(index) { 3 }.coerceIn(1, 5)}/5"
    }.joinToString("  •  ")
    val statusText = status
        ?.trim()
        ?.takeUnless { it.contains("send", ignoreCase = true) || it.contains("ask", ignoreCase = true) }
        .orEmpty()
    val caption = when {
        busy -> "Saving check-in…"
        statusText.isNotBlank() -> statusText
        else -> "Five signals recorded. Your Twin can move on."
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(9.dp),
    ) {
        TwinOasisProgress(currentIndex = TwinCheckInDimensions.lastIndex, count = TwinCheckInDimensions.size)
        Text(
            text = "✓ Check-in complete",
            style = MaterialTheme.typography.titleMedium,
            color = LauncherTextColor,
            fontSize = 20.sp,
            fontWeight = FontWeight.SemiBold,
        )
        Text(
            text = caption,
            style = MaterialTheme.typography.bodyMedium,
            color = LauncherSecondaryTextColor,
            fontSize = 13.sp,
            lineHeight = 17.sp,
        )
        Text(
            text = summary,
            style = MaterialTheme.typography.labelSmall,
            color = LauncherMutedActionColor,
            fontSize = 11.sp,
            lineHeight = 15.sp,
        )
        TwinCommandButton(
            text = "edit check-in",
            modifier = Modifier.fillMaxWidth(),
            enabled = !busy,
            onClick = onEdit,
        )
    }
}

@Composable
private fun TwinOasisProgress(
    currentIndex: Int,
    count: Int,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(5.dp),
    ) {
        repeat(count) { index ->
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(5.dp)
                    .background(
                        if (index <= currentIndex) LauncherAccentColor.copy(alpha = 0.74f) else LauncherSurfaceSoftColor,
                        RoundedCornerShape(999.dp),
                    ),
            )
        }
    }
}

@Composable
private fun TwinCheckInQuestionCard(
    dimension: TwinCheckInDimension,
    selectedRating: Int,
    selectedMood: TwinMoodOption,
    onRatingSelected: (Int) -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(7.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = dimension.label,
                style = MaterialTheme.typography.labelLarge,
                color = LauncherSecondaryTextColor,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = selectedMood.sublabel,
                style = MaterialTheme.typography.labelSmall,
                color = LauncherMutedActionColor,
                fontSize = 11.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Text(
            text = dimension.question,
            style = MaterialTheme.typography.bodyMedium,
            color = LauncherTextColor,
            fontSize = 15.sp,
            fontWeight = FontWeight.Medium,
            lineHeight = 18.sp,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            TwinMoodOptions.forEachIndexed { index, option ->
                TwinGuidedMoodButton(
                    option = option,
                    value = index + 1,
                    selected = selectedRating == index + 1,
                    modifier = Modifier.weight(1f),
                    onClick = { onRatingSelected(index + 1) },
                )
            }
        }
    }
}

@Composable
private fun TwinGuidedMoodButton(
    option: TwinMoodOption,
    value: Int,
    selected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    Surface(
        modifier = modifier
            .heightIn(min = 48.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(11.dp),
        color = if (selected) LauncherSelectedSurfaceColor else LauncherSurfaceSoftColor,
        border = BorderStroke(
            width = 1.dp,
            color = if (selected) LauncherAccentColor else LauncherMutedActionColor.copy(alpha = 0.45f),
        ),
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 2.dp, vertical = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(
                text = value.toString(),
                style = MaterialTheme.typography.labelLarge,
                color = if (selected) LauncherTextColor else LauncherSecondaryTextColor,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
            )
            Text(
                text = option.label,
                style = MaterialTheme.typography.labelSmall,
                color = if (selected) LauncherTextColor else LauncherMutedActionColor,
                fontSize = 8.sp,
                fontWeight = FontWeight.Medium,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun TwinChatWidget(
    messages: List<TwinChatMessage>,
    draft: String,
    busy: Boolean,
    onDraftChange: (String) -> Unit,
    onSend: () -> Unit,
    onOpenChat: () -> Unit,
) {
    TwinWidgetFrame(
        title = "THREAD",
        trailingText = "open",
        onTrailingClick = onOpenChat,
        twinAccent = true,
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            messages.takeLast(12).forEach { message ->
                TwinChatBubble(message = message)
            }
            if (busy) {
                TwinChatBubble(
                    message = TwinChatMessage(fromTwin = true, text = "Twin is typing…"),
                    muted = true,
                )
            }
        }
        OutlinedTextField(
            value = draft,
            onValueChange = onDraftChange,
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Command Twin") },
            minLines = 1,
            maxLines = 3,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
            keyboardActions = KeyboardActions(onSend = {
                if (!busy && draft.trim().isNotEmpty()) onSend()
            }),
            colors = launcherTextFieldColors(),
        )
        TwinCommandButton(
            text = if (busy) "Sending" else "Ask",
            modifier = Modifier.fillMaxWidth(),
            emphasized = true,
            enabled = !busy && draft.isNotBlank(),
            onClick = onSend,
        )
    }
}

@Composable
private fun TwinChatBubble(
    message: TwinChatMessage,
    muted: Boolean = false,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (message.fromTwin) Arrangement.Start else Arrangement.End,
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth(0.86f),
            shape = RoundedCornerShape(
                topStart = 16.dp,
                topEnd = 16.dp,
                bottomStart = if (message.fromTwin) 6.dp else 16.dp,
                bottomEnd = if (message.fromTwin) 16.dp else 6.dp,
            ),
            color = if (message.fromTwin) LauncherElevatedColor else LauncherSelectedSurfaceColor,
            border = BorderStroke(1.dp, if (message.fromTwin) LauncherHairlineColor else LauncherAccentBorderColor),
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp),
                verticalArrangement = Arrangement.spacedBy(3.dp),
            ) {
                Text(
                    text = if (message.fromTwin) "Twin" else "You",
                    style = MaterialTheme.typography.labelSmall,
                    color = if (message.fromTwin) LauncherMutedActionColor else LauncherAccentColor,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                )
                Text(
                    text = message.text,
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (muted) LauncherMutedActionColor else LauncherTextColor,
                    fontSize = 13.sp,
                    lineHeight = 16.sp,
                )
            }
        }
    }
}

@Composable
private fun TwinTodoWidget(
    items: List<TwinTodoItem>,
    draft: String,
    composerOpen: Boolean,
    onDraftChange: (String) -> Unit,
    onComposerOpenChange: (Boolean) -> Unit,
    onToggle: (Int) -> Unit,
    onDelete: (Int) -> Unit,
    onAdd: () -> Unit,
) {
    val taskFocusRequester = remember { FocusRequester() }

    LaunchedEffect(composerOpen) {
        if (composerOpen) taskFocusRequester.requestFocus()
    }

    TwinWidgetFrame(title = "TO-DO") {
        items.take(5).forEachIndexed { index, item ->
            TwinTodoRow(
                item = item,
                onToggle = { onToggle(index) },
                onDelete = { onDelete(index) },
            )
        }
        if (composerOpen) {
            TwinTodoDraftRow(
                draft = draft,
                focusRequester = taskFocusRequester,
                onDraftChange = onDraftChange,
                onAdd = onAdd,
                onCancel = { onComposerOpenChange(false) },
            )
        }
        TwinTodoAddControl(
            composerOpen = composerOpen,
            draftNotBlank = draft.trim().isNotEmpty(),
            onOpen = { onComposerOpenChange(true) },
            onAdd = onAdd,
        )
    }
}

@Composable
private fun TwinTodoRow(
    item: TwinTodoItem,
    onToggle: () -> Unit,
    onDelete: () -> Unit,
) {
    TwinTodoLine(
        checked = item.done,
        text = item.label,
        textColor = if (item.done) LauncherMutedActionColor else LauncherTextColor,
        onCheckClick = onToggle,
        onTextClick = onToggle,
        onDelete = onDelete,
    )
}

@Composable
private fun TwinTodoDraftRow(
    draft: String,
    focusRequester: FocusRequester,
    onDraftChange: (String) -> Unit,
    onAdd: () -> Unit,
    onCancel: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        TwinTodoCheckbox(checked = false, onClick = {
            if (draft.trim().isNotEmpty()) onAdd()
        })
        Column(modifier = Modifier.weight(1f)) {
            BasicTextField(
                value = draft,
                onValueChange = onDraftChange,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 30.dp)
                    .focusRequester(focusRequester),
                singleLine = true,
                textStyle = MaterialTheme.typography.bodyLarge.copy(
                    color = LauncherTextColor,
                    fontSize = 20.sp,
                    lineHeight = 24.sp,
                ),
                cursorBrush = SolidColor(LauncherTextColor),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = {
                    if (draft.trim().isNotEmpty()) onAdd()
                }),
                decorationBox = { innerTextField ->
                    Box(
                        modifier = Modifier.fillMaxWidth(),
                        contentAlignment = Alignment.CenterStart,
                    ) {
                        if (draft.isBlank()) {
                            Text(
                                text = "New task",
                                style = MaterialTheme.typography.bodyLarge,
                                color = LauncherMutedActionColor,
                                fontSize = 20.sp,
                                maxLines = 1,
                            )
                        }
                        innerTextField()
                    }
                },
            )
            TwinTodoUnderline()
        }
        TwinTodoDelete(text = "×", onClick = onCancel)
    }
}

@Composable
private fun TwinTodoLine(
    checked: Boolean,
    text: String,
    textColor: Color,
    onCheckClick: () -> Unit,
    onTextClick: () -> Unit,
    onDelete: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        TwinTodoCheckbox(checked = checked, onClick = onCheckClick)
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = text,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 30.dp)
                    .clickable(onClick = onTextClick),
                style = MaterialTheme.typography.bodyLarge,
                color = textColor,
                fontSize = 20.sp,
                lineHeight = 24.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            TwinTodoUnderline()
        }
        TwinTodoDelete(text = "×", onClick = onDelete)
    }
}

@Composable
private fun TwinTodoCheckbox(
    checked: Boolean,
    onClick: () -> Unit,
) {
    Surface(
        modifier = Modifier
            .width(20.dp)
            .height(20.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(1.dp),
        color = Color.Transparent,
        border = BorderStroke(1.5.dp, LauncherTextColor.copy(alpha = 0.78f)),
    ) {
        Box(contentAlignment = Alignment.Center) {
            if (checked) {
                Text(
                    text = "✓",
                    color = LauncherTextColor,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                )
            }
        }
    }
}

@Composable
private fun TwinTodoUnderline() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(LauncherTextColor.copy(alpha = 0.56f)),
    )
}

@Composable
private fun TwinTodoDelete(
    text: String,
    onClick: () -> Unit,
) {
    Text(
        text = text,
        modifier = Modifier
            .width(28.dp)
            .clickable(onClick = onClick),
        style = MaterialTheme.typography.titleLarge,
        color = LauncherTextColor,
        fontSize = 25.sp,
        textAlign = TextAlign.Center,
        maxLines = 1,
    )
}

@Composable
private fun TwinTodoAddControl(
    composerOpen: Boolean,
    draftNotBlank: Boolean,
    onOpen: () -> Unit,
    onAdd: () -> Unit,
) {
    Box(
        modifier = Modifier.fillMaxWidth(),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = "+",
            modifier = Modifier
                .width(54.dp)
                .clickable(onClick = {
                    if (composerOpen && draftNotBlank) onAdd() else onOpen()
                }),
            style = MaterialTheme.typography.headlineSmall,
            color = LauncherTextColor,
            fontSize = 32.sp,
            lineHeight = 32.sp,
            textAlign = TextAlign.Center,
            maxLines = 1,
        )
    }
}

@Composable
private fun TwinNotesWidget(
    note: String,
    busy: Boolean,
    onNoteChange: (String) -> Unit,
    onDone: () -> Unit,
) {
    TwinWidgetFrame(title = "NOTES") {
        OutlinedTextField(
            value = note,
            onValueChange = onNoteChange,
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("Write note — Done saves") },
            minLines = 1,
            maxLines = 3,
            enabled = !busy,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { onDone() }),
            colors = launcherLineTextFieldColors(),
        )
        TwinOasisDivider()
    }
}
@Composable
private fun TwinCalendarWidget(
    events: List<LauncherCalendarEvent>,
    permissionGranted: Boolean,
    onConnect: () -> Unit,
    onRefresh: () -> Unit,
) {
    var dayOffset by rememberSaveable { mutableStateOf(0) }
    val selectedDayMillis = remember(dayOffset) {
        Calendar.getInstance(Locale.getDefault()).apply {
            add(Calendar.DAY_OF_YEAR, dayOffset)
            set(Calendar.HOUR_OF_DAY, 12)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
    }
    val dateLabel = remember(selectedDayMillis) {
        if (dayOffset == 0) {
            "Today, " + SimpleDateFormat("MMM d", Locale.getDefault()).format(Date(selectedDayMillis))
        } else {
            SimpleDateFormat("EEE, MMM d", Locale.getDefault()).format(Date(selectedDayMillis))
        }
    }
    val selectedEvents = remember(events, selectedDayMillis) {
        events.filter { event -> event.occursOnLauncherCalendarDay(selectedDayMillis) }.take(4)
    }

    TwinWidgetFrame(
        title = "CALENDAR",
        trailingText = if (permissionGranted) "refresh" else "connect",
        onTrailingClick = if (permissionGranted) onRefresh else onConnect,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            LauncherPanelTinyAction(text = "◀", onClick = { dayOffset -= 1 })
            Text(
                text = dateLabel,
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.titleMedium,
                color = LauncherTextColor,
                fontSize = 17.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                textAlign = TextAlign.Center,
                overflow = TextOverflow.Ellipsis,
            )
            LauncherPanelTinyAction(text = "▶", onClick = { dayOffset += 1 })
        }

        when {
            !permissionGranted -> {
                TwinFlowCallout(
                    title = "Connect Google Calendar",
                    detail = "Allow access to show real synced events here.",
                    action = "connect",
                    onAction = onConnect,
                )
            }

            events.isEmpty() -> {
                TwinFlowCallout(
                    title = "No upcoming events",
                    detail = "Real Android calendar only — no fake schedule.",
                    action = "refresh",
                    onAction = onRefresh,
                )
            }

            selectedEvents.isEmpty() -> {
                TwinFlowCallout(
                    title = "No events on this date",
                    detail = "Use arrows to browse synced calendar days.",
                    action = "refresh",
                    onAction = onRefresh,
                )
            }

            else -> {
                selectedEvents.forEach { event ->
                    TwinCalendarEventRow(event)
                }
            }
        }
    }
}

@Composable
private fun TwinFlowCallout(
    title: String,
    detail: String,
    action: String,
    onAction: () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(5.dp),
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.bodyLarge,
            color = LauncherTextColor,
            fontSize = 17.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            text = detail,
            style = MaterialTheme.typography.bodySmall,
            color = LauncherMutedActionColor,
            fontSize = 12.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        TwinCommandButton(
            text = action,
            modifier = Modifier.fillMaxWidth(),
            emphasized = true,
            onClick = onAction,
        )
    }
}

@Composable
private fun TwinCalendarEventRow(event: LauncherCalendarEvent) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(3.dp),
    ) {
        Text(
            text = event.title,
            style = MaterialTheme.typography.bodyLarge,
            color = LauncherTextColor,
            fontSize = 18.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            lineHeight = 20.sp,
        )
        Text(
            text = event.timeLabel,
            style = MaterialTheme.typography.bodyMedium,
            color = LauncherSecondaryTextColor,
            fontSize = 15.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        TwinOasisDivider()
    }
}


private fun LauncherCalendarEvent.occursOnLauncherCalendarDay(dayMillis: Long): Boolean {
    val locale = Locale.getDefault()
    val startOfDay = Calendar.getInstance(locale).apply {
        timeInMillis = dayMillis
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }.timeInMillis
    val endOfDay = Calendar.getInstance(locale).apply {
        timeInMillis = startOfDay
        add(Calendar.DAY_OF_YEAR, 1)
    }.timeInMillis
    val safeEndMillis = endMillis.takeIf { it > beginMillis } ?: beginMillis

    return beginMillis < endOfDay && safeEndMillis >= startOfDay
}


@Composable
private fun TwinOasisDivider() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(LauncherTextColor.copy(alpha = 0.38f)),
    )
}

@Composable
private fun TwinStatusMatrix(
    todaySummary: LauncherTodaySummary,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val state = todaySummary.twinStateLabel().lowercase(Locale.US).replaceFirstChar { it.uppercase() }
    val readiness = todaySummary.twinReadinessScore()
    val continuity = todaySummary.twinContinuityLabel()

    TwinCommandSurface(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 13.dp),
            verticalArrangement = Arrangement.spacedBy(7.dp),
        ) {
            TwinMicroLabel(text = "NOW")
            Text(
                text = state,
                style = MaterialTheme.typography.titleLarge,
                color = LauncherTextColor,
                fontSize = 25.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = "next: ${todaySummary.launcherNextLine()}",
                style = MaterialTheme.typography.bodyMedium,
                color = LauncherSecondaryTextColor,
                fontSize = 14.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = "readiness $readiness · $continuity",
                style = MaterialTheme.typography.bodySmall,
                color = LauncherMutedActionColor,
                fontSize = 12.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun TwinMatrixCell(
    label: String,
    value: String,
    detail: String,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.heightIn(min = 88.dp),
        shape = RoundedCornerShape(14.dp),
        color = LauncherElevatedColor.copy(alpha = 0.96f),
        border = BorderStroke(1.dp, LauncherBorderColor),
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 11.dp),
            verticalArrangement = Arrangement.spacedBy(5.dp),
        ) {
            TwinMicroLabel(text = label)
            Text(
                text = value,
                style = MaterialTheme.typography.titleLarge,
                color = LauncherTextColor,
                fontSize = if (value.length <= 3) 25.sp else 21.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = detail,
                style = MaterialTheme.typography.bodySmall,
                color = LauncherMutedActionColor,
                fontSize = 12.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun TwinTelemetryRail(
    todaySummary: LauncherTodaySummary,
    modifier: Modifier = Modifier,
) {
    val telemetry = todaySummary.twinTelemetryValues()

    TwinCommandSurface(modifier = modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(9.dp),
        ) {
            TwinMicroLabel(text = "SIGNAL TELEMETRY")
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                TwinMetricBar(
                    label = "Energy",
                    value = telemetry.energy,
                    modifier = Modifier.weight(1f),
                )
                TwinMetricBar(
                    label = "Focus",
                    value = telemetry.focus,
                    modifier = Modifier.weight(1f),
                )
                TwinMetricBar(
                    label = "Stress",
                    value = telemetry.stress,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
private fun TwinSimpleStack(
    todaySummary: LauncherTodaySummary,
    selectedModule: TwinInlineModule,
    onSelectModule: (TwinInlineModule) -> Unit,
    modifier: Modifier = Modifier,
) {
    TwinCommandSurface(modifier = modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            TwinMicroLabel(text = "STACK")
            TwinSimpleStackRow(
                label = "Check in",
                detail = todaySummary.twinLauncherTodoLine(),
                action = "start",
                selected = selectedModule == TwinInlineModule.CheckIn,
                onClick = { onSelectModule(TwinInlineModule.CheckIn) },
            )
            TwinSimpleStackRow(
                label = "Quest",
                detail = todaySummary.twinLauncherQuestLine(),
                action = "continue",
                selected = selectedModule == TwinInlineModule.Quest,
                onClick = { onSelectModule(TwinInlineModule.Quest) },
            )
            TwinSimpleStackRow(
                label = "Journal",
                detail = "Capture one thing",
                action = "write",
                selected = selectedModule == TwinInlineModule.Journal,
                onClick = { onSelectModule(TwinInlineModule.Journal) },
            )
            TwinSimpleStackRow(
                label = "Ask",
                detail = "Plan the next hour",
                action = "ask",
                selected = selectedModule == TwinInlineModule.AskTwin,
                onClick = { onSelectModule(TwinInlineModule.AskTwin) },
            )
        }
    }
}

@Composable
private fun TwinSimpleStackRow(
    label: String,
    detail: String,
    action: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 43.dp)
            .clickable(onClick = onClick),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(1.dp),
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                color = if (selected) LauncherTextColor else LauncherSecondaryTextColor,
                fontSize = 15.sp,
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = detail,
                style = MaterialTheme.typography.bodySmall,
                color = LauncherMutedActionColor,
                fontSize = 12.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Text(
            text = action,
            style = MaterialTheme.typography.labelSmall,
            color = if (selected) LauncherAccentColor else LauncherMutedActionColor,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
        )
    }
}

@Composable
private fun TwinMetricBar(
    label: String,
    value: Int,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(5.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = LauncherSecondaryTextColor,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
            )
            Text(
                text = value.toString(),
                style = MaterialTheme.typography.labelSmall,
                color = LauncherMutedActionColor,
                fontSize = 10.sp,
                maxLines = 1,
            )
        }
        TwinSegmentRail(value = value, segments = 5, heightDp = 5)
    }
}

@Composable
private fun TwinActiveProtocolCard(
    todaySummary: LauncherTodaySummary,
    selectedModule: TwinInlineModule,
    journalDraft: String,
    askPrompt: String,
    inlineActionStatus: String?,
    inlineActionBusy: Boolean,
    askReplyPreview: String?,
    onJournalDraftChange: (String) -> Unit,
    onAskPromptChange: (String) -> Unit,
    onOpenCompanionDestination: (CompanionLaunchRequest) -> Unit,
    onSaveInlineCheckIn: () -> Unit,
    onNudgeInlineQuest: () -> Unit,
    onSaveInlineJournalDraft: (String) -> Unit,
    onSendInlineAsk: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    TwinCommandSurface(modifier = modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 13.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            TwinMicroLabel(text = "ACTIVE PROTOCOL")
            Text(
                text = selectedModule.title(),
                style = MaterialTheme.typography.titleMedium,
                color = LauncherTextColor,
                fontSize = 20.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = selectedModule.summaryLine(),
                style = MaterialTheme.typography.bodyMedium,
                color = LauncherSecondaryTextColor,
                fontSize = 14.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = selectedModule.statusLine(todaySummary),
                style = MaterialTheme.typography.bodySmall,
                color = LauncherMutedActionColor,
                fontSize = 13.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            selectedModule.nextLine(todaySummary)?.let { next ->
                Text(
                    text = next,
                    style = MaterialTheme.typography.bodySmall,
                    color = LauncherMutedActionColor,
                    fontSize = 13.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            when (selectedModule) {
                TwinInlineModule.Journal -> {
                    OutlinedTextField(
                        value = journalDraft,
                        onValueChange = onJournalDraftChange,
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 72.dp),
                        label = { Text("Local draft") },
                        minLines = 1,
                        maxLines = 3,
                        colors = launcherTextFieldColors(),
                    )
                    Text(
                        text = "Draft kept on this phone.",
                        style = MaterialTheme.typography.bodySmall,
                        color = LauncherMutedActionColor,
                        fontSize = 13.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }

                TwinInlineModule.AskTwin -> {
                    OutlinedTextField(
                        value = askPrompt,
                        onValueChange = onAskPromptChange,
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 56.dp),
                        label = { Text("Prompt") },
                        minLines = 1,
                        maxLines = 3,
                        colors = launcherTextFieldColors(),
                    )
                    askReplyPreview?.let { reply ->
                        Text(
                            text = reply,
                            style = MaterialTheme.typography.bodySmall,
                            color = LauncherSecondaryTextColor,
                            fontSize = 13.sp,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }

                TwinInlineModule.CheckIn,
                TwinInlineModule.Quest -> Unit
            }
            inlineActionStatus?.let { status ->
                Text(
                    text = status,
                    style = MaterialTheme.typography.bodySmall,
                    color = LauncherSecondaryTextColor,
                    fontSize = 13.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                TwinCommandButton(
                    text = selectedModule.inlineActionLabel(inlineActionBusy),
                    modifier = Modifier.weight(1f),
                    enabled = !inlineActionBusy,
                    onClick = {
                        when (selectedModule) {
                            TwinInlineModule.CheckIn -> onSaveInlineCheckIn()
                            TwinInlineModule.Quest -> onNudgeInlineQuest()
                            TwinInlineModule.Journal -> onSaveInlineJournalDraft(journalDraft)
                            TwinInlineModule.AskTwin -> onSendInlineAsk(askPrompt)
                        }
                    },
                )
                TwinCommandButton(
                    text = selectedModule.ctaLabel(),
                    modifier = Modifier.weight(0.72f),
                    emphasized = true,
                    enabled = !inlineActionBusy,
                    onClick = {
                        onOpenCompanionDestination(
                            selectedModule.companionLaunchRequest(
                                journalDraft = journalDraft,
                                askPrompt = askPrompt,
                            ),
                        )
                    },
                )
            }
        }
    }
}

@Composable
private fun TwinQuestArcCard(
    todaySummary: LauncherTodaySummary,
    selected: Boolean,
    inlineActionBusy: Boolean,
    onSelect: () -> Unit,
    onNudgeInlineQuest: () -> Unit,
    onOpenQuest: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val progress = todaySummary.twinQuestProgress()

    TwinCommandSurface(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onSelect),
        emphasized = selected,
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 13.dp),
            verticalArrangement = Arrangement.spacedBy(9.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                TwinMicroLabel(text = "QUEST ARC")
                Text(
                    text = "$progress%",
                    style = MaterialTheme.typography.labelMedium,
                    color = LauncherAccentColor,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                )
            }
            Text(
                text = todaySummary.twinLauncherQuestLine(),
                style = MaterialTheme.typography.bodyLarge,
                color = LauncherTextColor,
                fontSize = 17.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            TwinSegmentRail(value = progress, segments = 16, heightDp = 6)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = todaySummary.twinContinuityLabel(),
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.bodySmall,
                    color = LauncherMutedActionColor,
                    fontSize = 12.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                TwinCommandButton(
                    text = "+10%",
                    enabled = !inlineActionBusy,
                    onClick = onNudgeInlineQuest,
                )
                TwinCommandButton(
                    text = "open",
                    emphasized = true,
                    enabled = !inlineActionBusy,
                    onClick = onOpenQuest,
                )
            }
        }
    }
}

@Composable
private fun TwinCaptureStrip(
    selectedModule: TwinInlineModule,
    onSelectModule: (TwinInlineModule) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        TwinCaptureCell(
            label = "Journal seed",
            detail = "Capture one thing",
            selected = selectedModule == TwinInlineModule.Journal,
            onClick = { onSelectModule(TwinInlineModule.Journal) },
            modifier = Modifier.weight(1f),
        )
        TwinCaptureCell(
            label = "Ask next hour",
            detail = "Plan with context",
            selected = selectedModule == TwinInlineModule.AskTwin,
            onClick = { onSelectModule(TwinInlineModule.AskTwin) },
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun TwinCaptureCell(
    label: String,
    detail: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    TwinCommandSurface(
        modifier = modifier
            .heightIn(min = 64.dp)
            .clickable(onClick = onClick),
        emphasized = selected,
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = if (selected) LauncherTextColor else LauncherSecondaryTextColor,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = detail,
                style = MaterialTheme.typography.bodySmall,
                color = LauncherMutedActionColor,
                fontSize = 12.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun TwinCommandSurface(
    modifier: Modifier = Modifier,
    emphasized: Boolean = false,
    content: @Composable () -> Unit,
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        color = if (emphasized) LauncherElevatedColor else LauncherSurfaceSoftColor,
        border = BorderStroke(1.dp, if (emphasized) LauncherAccentBorderColor else LauncherHairlineColor),
        content = content,
    )
}

@Composable
private fun TwinMicroLabel(text: String) {
    Text(
        text = text.uppercase(Locale.US),
        style = MaterialTheme.typography.labelSmall,
        color = LauncherMutedActionColor.copy(alpha = 0.86f),
        fontFamily = FontFamily.Monospace,
        fontWeight = FontWeight.Medium,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
    )
}

@Composable
private fun TwinLabelPill(text: String) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = LauncherSurfaceSoftColor,
        border = BorderStroke(1.dp, LauncherHairlineColor),
    ) {
        Text(
            text = text.uppercase(Locale.US),
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            style = MaterialTheme.typography.labelSmall,
            color = LauncherSecondaryTextColor,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
        )
    }
}

@Composable
private fun TwinSegmentRail(
    value: Int,
    segments: Int,
    heightDp: Int,
    modifier: Modifier = Modifier,
) {
    val filledSegments = ((value.coerceIn(0, 100) * segments) + 99) / 100

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        repeat(segments) { index ->
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(heightDp.dp)
                    .background(
                        color = if (index < filledSegments) {
                            LauncherAccentColor.copy(alpha = 0.74f)
                        } else {
                            LauncherSurfaceSoftColor
                        },
                        shape = RoundedCornerShape(999.dp),
                    ),
            )
        }
    }
}

@Composable
private fun TwinCommandButton(
    text: String,
    modifier: Modifier = Modifier,
    emphasized: Boolean = false,
    enabled: Boolean = true,
    onClick: () -> Unit,
) {
    Surface(
        modifier = modifier
            .heightIn(min = 46.dp)
            .clickable(enabled = enabled, onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        color = when {
            emphasized && enabled -> LauncherAccentColor
            else -> LauncherSurfaceSoftColor
        },
        border = BorderStroke(1.dp, if (emphasized && enabled) LauncherAccentBorderColor else LauncherHairlineColor),
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = text,
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                style = MaterialTheme.typography.labelMedium,
                color = when {
                    !enabled -> LauncherMutedActionColor
                    emphasized -> LauncherBackgroundColor
                    else -> LauncherSecondaryTextColor
                },
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
private fun LauncherPanelTinyAction(
    text: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    onClick: () -> Unit,
) {
    TextButton(
        onClick = onClick,
        modifier = modifier.heightIn(min = 36.dp),
        enabled = enabled,
        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 4.dp),
        colors = ButtonDefaults.textButtonColors(
            contentColor = LauncherAccentColor,
            disabledContentColor = LauncherMutedActionColor.copy(alpha = 0.45f),
        ),
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium,
            color = if (enabled) LauncherSecondaryTextColor else LauncherMutedActionColor.copy(alpha = 0.45f),
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun launcherTextFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedTextColor = LauncherTextColor,
    unfocusedTextColor = LauncherTextColor,
    disabledTextColor = LauncherMutedActionColor,
    focusedBorderColor = LauncherAccentColor,
    unfocusedBorderColor = LauncherHairlineColor,
    disabledBorderColor = LauncherHairlineColor,
    focusedLabelColor = LauncherAccentColor,
    unfocusedLabelColor = LauncherMutedActionColor,
    focusedPlaceholderColor = LauncherMutedActionColor,
    unfocusedPlaceholderColor = LauncherMutedActionColor,
    cursorColor = LauncherAccentColor,
    focusedContainerColor = LauncherInputColor,
    unfocusedContainerColor = LauncherInputColor,
    disabledContainerColor = LauncherInputColor,
)

@Composable
private fun launcherLineTextFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedTextColor = LauncherTextColor,
    unfocusedTextColor = LauncherTextColor,
    disabledTextColor = LauncherMutedActionColor,
    focusedBorderColor = Color.Transparent,
    unfocusedBorderColor = Color.Transparent,
    disabledBorderColor = Color.Transparent,
    focusedLabelColor = LauncherAccentColor,
    unfocusedLabelColor = LauncherMutedActionColor,
    focusedPlaceholderColor = LauncherMutedActionColor,
    unfocusedPlaceholderColor = LauncherMutedActionColor,
    cursorColor = LauncherAccentColor,
    focusedContainerColor = Color.Transparent,
    unfocusedContainerColor = Color.Transparent,
    disabledContainerColor = Color.Transparent,
)

@Composable
private fun rememberLauncherClockLabels(): LauncherClockLabels {
    val context = LocalContext.current
    var nowMillis by remember { mutableStateOf(System.currentTimeMillis()) }

    LaunchedEffect(Unit) {
        while (true) {
            delay(30_000L)
            nowMillis = System.currentTimeMillis()
        }
    }

    return remember(context, nowMillis) {
        val now = Date(nowMillis)
        LauncherClockLabels(
            day = SimpleDateFormat("EEEE", Locale.getDefault()).format(now),
            time = DateFormat.getTimeFormat(context).format(now),
            date = SimpleDateFormat("MMM d", Locale.getDefault()).format(now),
        )
    }
}

private fun LauncherTodaySummary.launcherStateLine(): String =
    mood.cleanLauncherLine()
        ?.lowercase(Locale.getDefault())
        ?: "quiet"

private fun LauncherTodaySummary.launcherNextLine(): String {
    val next = nextAction.cleanLauncherLine()
        ?: checkInStatus.cleanLauncherLine()
        ?: "open companion"

    return next.lowercase(Locale.getDefault())
}

private fun LauncherTodaySummary.twinLauncherTodayLine(): String =
    listOfNotNull(
        mood.cleanLauncherLine()?.lowercase(Locale.getDefault()),
        streak.cleanLauncherLine()?.lowercase(Locale.getDefault()),
    ).joinToString(separator = " \u00B7 ")
        .ifBlank { launcherStateLine() }

private fun LauncherTodaySummary.twinLauncherTodoLine(): String =
    checkInStatus.cleanLauncherLine()
        ?: nextAction.cleanLauncherLine()
        ?: "Check in pending"

private fun LauncherTodaySummary.twinLauncherQuestLine(): String =
    currentQuest.cleanLauncherLine()
        ?: nextAction.cleanLauncherLine()
        ?: "Ship the smallest useful companion"

private fun LauncherTodaySummary.twinStateLabel(): String =
    mood.cleanLauncherLine()
        ?.uppercase(Locale.US)
        ?: "QUIET"

private fun LauncherTodaySummary.twinStreakDays(): Int =
    Regex("\\d+").find(streak.orEmpty())?.value?.toIntOrNull() ?: 0

private fun LauncherTodaySummary.twinContinuityLabel(): String {
    val days = twinStreakDays()
    return if (days > 0) "${days}d continuity" else "continuity pending"
}

private fun LauncherTodaySummary.twinReadinessScore(): Int {
    val moodText = mood.orEmpty().lowercase(Locale.US)
    val moodBase = when {
        "focused" in moodText -> 78
        "strong" in moodText || "energized" in moodText -> 80
        "calm" in moodText || "steady" in moodText -> 74
        "tired" in moodText || "low" in moodText -> 58
        "stressed" in moodText || "overload" in moodText -> 55
        else -> 68
    }
    val checkInBoost = if (checkInStatus.orEmpty().contains("complete", ignoreCase = true)) 4 else -2
    val continuityBoost = twinStreakDays().coerceAtMost(6)

    return (moodBase + checkInBoost + continuityBoost).coerceIn(44, 94)
}

private fun LauncherTodaySummary.twinQuestProgress(): Int {
    val quest = currentQuest.cleanLauncherLine() ?: return 0
    if (quest.equals("No active quest", ignoreCase = true)) return 0

    val stateBoost = when {
        mood.orEmpty().contains("focused", ignoreCase = true) -> 8
        mood.orEmpty().contains("steady", ignoreCase = true) -> 5
        else -> 2
    }

    return (38 + (twinStreakDays() * 4) + stateBoost).coerceIn(24, 86)
}

private fun LauncherTodaySummary.twinTelemetryValues(): TwinTelemetryValues {
    val readiness = twinReadinessScore()
    val moodText = mood.orEmpty().lowercase(Locale.US)
    val checkInComplete = checkInStatus.orEmpty().contains("complete", ignoreCase = true)
    val energy = when {
        "tired" in moodText || "low" in moodText -> readiness - 12
        "strong" in moodText || "energized" in moodText -> readiness + 8
        else -> readiness - 2
    }.coerceIn(24, 96)
    val focus = when {
        "focused" in moodText -> readiness + 10
        "stressed" in moodText || "overload" in moodText -> readiness - 10
        else -> readiness + 2
    }.coerceIn(24, 96)
    val stress = when {
        "stressed" in moodText || "overload" in moodText -> 72
        "calm" in moodText || checkInComplete -> 36
        else -> 48
    }.coerceIn(18, 88)

    return TwinTelemetryValues(
        energy = energy,
        focus = focus,
        stress = stress,
    )
}

private fun TwinInlineModule.title(): String =
    when (this) {
        TwinInlineModule.CheckIn -> "Check in"
        TwinInlineModule.Quest -> "Quest"
        TwinInlineModule.Journal -> "Journal"
        TwinInlineModule.AskTwin -> "Ask"
    }

private fun TwinInlineModule.summaryLine(): String =
    when (this) {
        TwinInlineModule.CheckIn -> "30-sec baseline before routing"
        TwinInlineModule.Quest -> "Continue the current quest arc"
        TwinInlineModule.Journal -> "Capture one thing while it is fresh"
        TwinInlineModule.AskTwin -> "Plan the next hour with current context"
    }

private fun TwinInlineModule.statusLine(todaySummary: LauncherTodaySummary): String =
    when (this) {
        TwinInlineModule.CheckIn -> "status: ${todaySummary.twinLauncherTodoLine()}"
        TwinInlineModule.Quest -> "quest: ${todaySummary.twinLauncherQuestLine()}"
        TwinInlineModule.Journal -> "prompt: Capture one thing while it is fresh"
        TwinInlineModule.AskTwin -> "prompt: Plan the next hour with context"
    }

private fun TwinInlineModule.nextLine(todaySummary: LauncherTodaySummary): String? =
    when (this) {
        TwinInlineModule.CheckIn -> "next: ${todaySummary.launcherNextLine()}"
        TwinInlineModule.Quest,
        TwinInlineModule.Journal,
        TwinInlineModule.AskTwin -> null
    }

private fun TwinInlineModule.ctaLabel(): String =
    "open"

private fun TwinInlineModule.inlineActionLabel(busy: Boolean): String =
    if (busy) {
        "working"
    } else {
        when (this) {
            TwinInlineModule.CheckIn -> "save baseline"
            TwinInlineModule.Quest -> "+10%"
            TwinInlineModule.Journal -> "save local"
            TwinInlineModule.AskTwin -> "send ask"
        }
    }

private fun TwinInlineModule.companionLaunchRequest(
    journalDraft: String,
    askPrompt: String,
): CompanionLaunchRequest =
    CompanionLaunchRequest(
        destination = when (this) {
            TwinInlineModule.CheckIn -> CompanionDestination.CheckIn
            TwinInlineModule.Quest -> CompanionDestination.Quest
            TwinInlineModule.Journal -> CompanionDestination.Journal
            TwinInlineModule.AskTwin -> CompanionDestination.Ask
        },
        payload = when (this) {
            TwinInlineModule.Journal -> journalDraft.takeIf { it.isNotBlank() }
            TwinInlineModule.AskTwin -> askPrompt.takeIf { it.isNotBlank() }
            TwinInlineModule.CheckIn,
            TwinInlineModule.Quest -> null
        },
    )

private fun Int.floorMod(size: Int): Int =
    ((this % size) + size) % size

private fun String?.cleanLauncherLine(): String? =
    this?.trim()?.takeIf { it.isNotBlank() }

private data class TwinTelemetryValues(
    val energy: Int,
    val focus: Int,
    val stress: Int,
)

@Composable
internal fun LauncherTextAction(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    TextButton(
        onClick = onClick,
        modifier = modifier,
        colors = ButtonDefaults.textButtonColors(contentColor = LauncherMutedActionColor),
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
        )
    }
}

private data class LauncherClockLabels(
    val day: String,
    val time: String,
    val date: String,
)

@Preview(showBackground = true)
@Composable
private fun LauncherScreenPreview() {
    val notes = LauncherApp(
        packageName = "com.example.notes",
        label = "Notes",
        activityName = "com.example.notes.MainActivity",
    )
    LauncherScreen(
        todaySummary = LauncherTodaySummary.from(null),
        apps = listOf(notes),
        favoriteApps = listOf(notes),
        favoritePackageNames = setOf(notes.packageName),
        isAppDrawerOpen = false,
        homeIntentNonce = 0,
        appIconIntentNonce = 0,
        startOnTwinPage = true,
        journalDraft = "",
        askPrompt = "",
        inlineActionStatus = null,
        inlineActionBusy = false,
        askReplyPreview = null,
        calendarEvents = listOf(
            LauncherCalendarEvent(
                title = "Deep Learning with Python",
                timeLabel = "8:00 AM-9:30 AM",
                sourceLabel = "Google Calendar",
                beginMillis = System.currentTimeMillis(),
                endMillis = System.currentTimeMillis() + 90 * 60 * 1000,
                allDay = false,
            ),
        ),
        calendarPermissionGranted = true,
        onRequestCalendarPermission = {},
        onRefreshCalendarEvents = {},
        onOpenAppDrawer = {},
        onCloseAppDrawer = {},
        onOpenGoogleSearch = {},
        onLaunchApp = {},
        onToggleFavorite = {},
        onOpenSettings = {},
        onJournalDraftChange = {},
        onAskPromptChange = {},
        onOpenCompanionDestination = {},
        onSaveInlineCheckIn = { _ -> },
        onNudgeInlineQuest = {},
        onSaveInlineJournalDraft = {},
        onSendInlineAsk = {},
    )
}

private const val MAX_HOME_FAVORITE_APPS = 5

// Page mapping: 0 = Twin, 1 = Home, 2 = Apps.
private const val TWIN_PAGE_INDEX = 0
private const val HOME_PAGE_INDEX = 1
private const val APPS_PAGE_INDEX = 2
private const val LAUNCHER_PAGE_COUNT = 3
