package com.transcendiverse.digitaltwin.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.transcendiverse.digitaltwin.data.CheckInRepository
import com.transcendiverse.digitaltwin.data.NetworkCheckInRepository
import com.transcendiverse.digitaltwin.data.FakeTodayRepository
import com.transcendiverse.digitaltwin.data.InMemoryTodayCacheStore
import com.transcendiverse.digitaltwin.data.InMemoryTodaySettingsStore
import com.transcendiverse.digitaltwin.data.LoginRepository
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

    LaunchedEffect(Unit) {
        loadToday(savedSettings)
    }

    MaterialTheme {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = CompanionBackground,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 22.dp, vertical = 24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                today?.let { loadedToday ->
                    DailyHero(loadedToday)
                    PrimaryActionCard(
                        today = loadedToday,
                        signedIn = savedSettings.hasCredentials(),
                    )
                    if (savedSettings.hasCredentials()) {
                        QuickCheckInCard(
                            today = loadedToday,
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
                    }
                    QuestCard(
                        today = loadedToday,
                        activeQuest = activeQuest.takeIf { savedSettings.hasCredentials() },
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
                    )
                    InsightCard(loadedToday)
                    LauncherActions(loadedToday)
                } ?: LoadingState(status)
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
                        status = "Signed out - fixture mode"
                    },
                )
            }
        }
    }
}

@Composable
private fun DailyHero(today: MobileToday) {
    val mood = today.user.mood

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = "Digital Twin",
            style = MaterialTheme.typography.headlineMedium,
            color = CompanionInk,
            fontWeight = FontWeight.SemiBold,
        )
        Text(
            text = formatTodayDay(today.dayKey),
            style = MaterialTheme.typography.bodyMedium,
            color = CompanionMuted,
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = "${today.user.name} is ${mood.label}",
            style = MaterialTheme.typography.headlineSmall,
            color = CompanionInk,
            fontWeight = FontWeight.SemiBold,
        )
        Text(
            text = "Level ${today.user.level} - ${today.user.currentXP} / ${today.user.requiredXP} XP - ${today.user.streak}-day streak",
            style = MaterialTheme.typography.bodyMedium,
            color = CompanionSecondary,
        )
        TodayVitalsRow(today)
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
        color = CompanionChip,
        shape = RoundedCornerShape(999.dp),
        border = BorderStroke(1.dp, CompanionBorder),
    ) {
        Text(
            text = label,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
            style = MaterialTheme.typography.labelMedium,
            color = CompanionSecondary,
            fontWeight = FontWeight.Medium,
        )
    }
}

@Composable
private fun PrimaryActionCard(
    today: MobileToday,
    signedIn: Boolean,
) {
    val action = recommendedTodayAction(today)
    val detail = when (action.label) {
        "Check in" -> if (signedIn) {
            "Start with a quick read on energy, focus, and stress."
        } else {
            "Sign in from Connection settings to save today's check-in."
        }
        "Continue quest" -> today.quest.current?.goal ?: "Pick up where you left off."
        else -> "Use the reflection below to close the loop for today."
    }

    Card(
        colors = CardDefaults.cardColors(containerColor = CompanionPrimary),
        shape = RoundedCornerShape(18.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = "Now",
                style = MaterialTheme.typography.labelLarge,
                color = Color.White.copy(alpha = 0.78f),
            )
            Text(
                text = action.label,
                style = MaterialTheme.typography.headlineSmall,
                color = Color.White,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = detail,
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White.copy(alpha = 0.88f),
            )
        }
    }
}

@Composable
private fun QuickCheckInCard(
    today: MobileToday,
    submitting: Boolean,
    onSubmit: (List<Int>) -> Unit,
) {
    if (today.checkIn.completedToday) {
        InfoCard(title = "Daily check-in") {
            Text(
                text = "Check-in complete",
                style = MaterialTheme.typography.bodyMedium,
                color = Color(0xFF0F766E),
                fontWeight = FontWeight.SemiBold,
            )
        }
        return
    }

    var ratings by remember(today.dayKey) { mutableStateOf(checkInPresetRatings(CheckInPreset.OKAY)) }

    InfoCard(title = "Daily check-in") {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            CheckInPreset.entries.forEach { preset ->
                OutlinedButton(
                    onClick = { ratings = checkInPresetRatings(preset) },
                    modifier = Modifier.weight(1f),
                    enabled = !submitting,
                    border = BorderStroke(1.dp, CompanionBorder),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = CompanionInk),
                ) {
                    Text(preset.label)
                }
            }
        }
        Spacer(modifier = Modifier.height(6.dp))
        CHECK_IN_DIMENSIONS.forEachIndexed { index, label ->
            RatingRow(
                label = label,
                rating = ratings[index],
                enabled = !submitting,
                onDecrease = { ratings = updateCheckInRating(ratings, index, -1) },
                onIncrease = { ratings = updateCheckInRating(ratings, index, 1) },
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        Button(
            onClick = { onSubmit(ratings) },
            modifier = Modifier.fillMaxWidth(),
            enabled = !submitting,
            colors = ButtonDefaults.buttonColors(containerColor = CompanionPrimary, contentColor = Color.White),
        ) {
            Text(if (submitting) "Submitting" else "Submit check-in")
        }
    }
}

@Composable
private fun RatingRow(
    label: String,
    rating: Int,
    enabled: Boolean,
    onDecrease: () -> Unit,
    onIncrease: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = label,
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.bodyMedium,
            color = Color(0xFF334155),
        )
        TextButton(
            onClick = onDecrease,
            enabled = enabled && rating > 1,
            colors = ButtonDefaults.textButtonColors(contentColor = CompanionPrimary),
        ) {
            Text("-")
        }
        Text(
            text = rating.toString(),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
        )
        TextButton(
            onClick = onIncrease,
            enabled = enabled && rating < 5,
            colors = ButtonDefaults.textButtonColors(contentColor = CompanionPrimary),
        ) {
            Text("+")
        }
    }
}


@Composable
private fun QuestCard(
    today: MobileToday,
    activeQuest: QuestSummary?,
    submitting: Boolean,
    onDecreaseProgress: () -> Unit,
    onIncreaseProgress: () -> Unit,
    onToggleComplete: () -> Unit,
) {
    val quest = today.quest.current
    InfoCard(title = "Quest") {
        Text(
            text = quest?.goal ?: "No active quest",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = if (quest == null) {
                "Reflect or pick a new quest from the web app."
            } else {
                "${quest.progress}% complete - ${quest.duration}"
            },
            style = MaterialTheme.typography.bodyMedium,
            color = Color(0xFF475569),
        )
        if (quest != null && activeQuest != null) {
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                OutlinedButton(
                    onClick = onDecreaseProgress,
                    modifier = Modifier.weight(1f),
                    enabled = !submitting && activeQuest.progress > 0,
                    border = BorderStroke(1.dp, CompanionBorder),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = CompanionInk),
                ) {
                    Text("-10%")
                }
                OutlinedButton(
                    onClick = onIncreaseProgress,
                    modifier = Modifier.weight(1f),
                    enabled = !submitting && activeQuest.progress < 100,
                    border = BorderStroke(1.dp, CompanionBorder),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = CompanionInk),
                ) {
                    Text("+10%")
                }
                Button(
                    onClick = onToggleComplete,
                    modifier = Modifier.weight(1f),
                    enabled = !submitting,
                    colors = ButtonDefaults.buttonColors(containerColor = CompanionPrimary, contentColor = Color.White),
                ) {
                    Text(if (activeQuest.completed) "Reopen" else "Complete")
                }
            }
        }
        Spacer(modifier = Modifier.height(10.dp))
        Text(
            text = today.quest.nextAction.label,
            style = MaterialTheme.typography.labelLarge,
            color = Color(0xFF0F766E),
            fontWeight = FontWeight.SemiBold,
        )
        Text(
            text = today.quest.nextAction.reason,
            style = MaterialTheme.typography.bodyMedium,
            color = Color(0xFF475569),
        )
    }
}

@Composable
private fun InsightCard(today: MobileToday) {
    InfoCard(title = "Reflection") {
        Text(text = today.insight.reflection, style = MaterialTheme.typography.bodyLarge)
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Trend: ${today.insight.trend} - Focus: ${today.insight.topInterest}",
            style = MaterialTheme.typography.bodyMedium,
            color = Color(0xFF475569),
        )
    }
}

@Composable
private fun LauncherActions(today: MobileToday) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        OutlinedButton(
            onClick = {},
            modifier = Modifier.weight(1f),
            border = BorderStroke(1.dp, CompanionBorder),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = CompanionInk),
        ) {
            Text(today.launcher.primaryLabel)
        }
        TextButton(
            onClick = {},
            modifier = Modifier.weight(1f),
            colors = ButtonDefaults.textButtonColors(contentColor = CompanionSecondary),
        ) {
            Text(today.launcher.secondaryLabel)
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

    InfoCard(title = "Connection") {
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
        )
        Spacer(modifier = Modifier.height(10.dp))
        OutlinedTextField(
            value = email,
            onValueChange = onEmailChange,
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Email") },
            singleLine = true,
        )
        Spacer(modifier = Modifier.height(10.dp))
        OutlinedTextField(
            value = password,
            onValueChange = onPasswordChange,
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Password") },
            visualTransformation = PasswordVisualTransformation(),
            singleLine = true,
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
                colors = ButtonDefaults.buttonColors(containerColor = CompanionPrimary, contentColor = Color.White),
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

private val CompanionBackground = Color(0xFFFAF9F6)
private val CompanionSurface = Color(0xFFFFFFFF)
private val CompanionChip = Color(0xFFF1EEE8)
private val CompanionPrimary = Color(0xFF31302E)
private val CompanionInk = Color(0xFF1F1E1B)
private val CompanionSecondary = Color(0xFF625F58)
private val CompanionMuted = Color(0xFF8A857B)
private val CompanionBorder = Color(0x1A000000)
private val CompanionSuccess = Color(0xFF2A7C62)

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
    return listOf("token", "password", "bearer", "http://", "https://", "@", "{", "}").any { lower.contains(it) }
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
        colors = CardDefaults.cardColors(containerColor = CompanionSurface),
        shape = RoundedCornerShape(18.dp),
        border = BorderStroke(1.dp, CompanionBorder),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text(text = title, style = MaterialTheme.typography.labelLarge, color = CompanionMuted)
            content()
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun TodayScreenPreview() {
    TodayScreen(settingsStore = InMemoryTodaySettingsStore())
}
