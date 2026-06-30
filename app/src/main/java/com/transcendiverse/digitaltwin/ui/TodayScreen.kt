package com.transcendiverse.digitaltwin.ui

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
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.TextButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
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
import com.transcendiverse.digitaltwin.data.NetworkTodayRepository
import com.transcendiverse.digitaltwin.data.NetworkLoginRepository
import com.transcendiverse.digitaltwin.data.PRODUCTION_BACKEND_BASE_URL
import com.transcendiverse.digitaltwin.data.TodayCacheStore
import com.transcendiverse.digitaltwin.data.TodayRepository
import com.transcendiverse.digitaltwin.data.TodaySettings
import com.transcendiverse.digitaltwin.data.TodaySettingsStore
import com.transcendiverse.digitaltwin.data.useProductionBackend
import com.transcendiverse.digitaltwin.data.validateDailyCheckInRatings
import com.transcendiverse.digitaltwin.model.MobileToday
import java.text.DateFormat
import java.util.Date
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
            return true
        } catch (error: Exception) {
            val cached = cacheStore.load()
            today = cached?.today
            status = "Refresh failed: ${safeStatusErrorMessage(error, "Unable to load Today")}"
            return false
        }
    }

    LaunchedEffect(Unit) {
        loadToday(savedSettings)
    }

    MaterialTheme {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = Color(0xFFF8FAFC),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                today?.let { loadedToday ->
                    Header(loadedToday)
                    StatusCards(loadedToday)
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
                    QuestCard(loadedToday)
                    InsightCard(loadedToday)
                    LauncherActions(loadedToday)
                } ?: LoadingState(status)
                SettingsPanel(
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
                            return@SettingsPanel
                        }

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
private fun Header(today: MobileToday) {
    val mood = today.user.mood

    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
            text = "Digital Twin",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
        )
        Text(
            text = "${mood.emoji} ${today.user.name} is ${mood.label}",
            style = MaterialTheme.typography.titleMedium,
            color = Color(0xFF334155),
        )
        Text(
            text = "Level ${today.user.level} - ${today.user.currentXP}/${today.user.requiredXP} XP",
            style = MaterialTheme.typography.bodyMedium,
            color = Color(0xFF475569),
        )
    }
}

@Composable
private fun StatusCards(today: MobileToday) {
    val dimensions = today.checkIn.dimensions

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        MetricCard(
            label = "Streak",
            value = "${today.user.streak} days",
            modifier = Modifier.weight(1f),
        )
        MetricCard(
            label = "Check-in",
            value = if (today.checkIn.completedToday) {
                "Done ${today.checkIn.score ?: 0}"
            } else {
                "Open ${today.checkIn.score ?: 0}"
            },
            modifier = Modifier.weight(1f),
        )
    }
    if (dimensions != null) {
        Text(
            text = "Energy ${dimensions.energy} - Focus ${dimensions.focus} - Stress control ${dimensions.stressControl}",
            style = MaterialTheme.typography.bodySmall,
            color = Color(0xFF64748B),
        )
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
                Button(
                    onClick = { ratings = checkInPresetRatings(preset) },
                    modifier = Modifier.weight(1f),
                    enabled = !submitting,
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
        TextButton(onClick = onDecrease, enabled = enabled && rating > 1) {
            Text("-")
        }
        Text(
            text = rating.toString(),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
        )
        TextButton(onClick = onIncrease, enabled = enabled && rating < 5) {
            Text("+")
        }
    }
}

@Composable
private fun MetricCard(label: String, value: String, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(8.dp),
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(text = label, style = MaterialTheme.typography.labelMedium, color = Color(0xFF64748B))
            Text(text = value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
private fun QuestCard(today: MobileToday) {
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
        Text(
            text = today.quest.nextAction.href,
            style = MaterialTheme.typography.bodySmall,
            color = Color(0xFF64748B),
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
        Button(onClick = {}, modifier = Modifier.weight(1f)) {
            Text(today.launcher.primaryLabel)
        }
        Button(onClick = {}, modifier = Modifier.weight(1f)) {
            Text(today.launcher.secondaryLabel)
        }
    }
}

@Composable
private fun SettingsPanel(
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
    InfoCard(title = "Settings") {
        Text(
            text = "Status: $status",
            style = MaterialTheme.typography.bodyMedium,
            color = Color(0xFF475569),
        )
        Text(
            text = if (signedIn) "Signed in" else "Signed out - fixture mode",
            style = MaterialTheme.typography.bodySmall,
            color = if (signedIn) Color(0xFF0F766E) else Color(0xFF64748B),
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = "Backend: ${baseUrl.ifBlank { PRODUCTION_BACKEND_BASE_URL }}",
            style = MaterialTheme.typography.bodySmall,
            color = Color(0xFF334155),
        )
        Spacer(modifier = Modifier.height(6.dp))
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
            Button(onClick = onSaveUrl, modifier = Modifier.weight(1f)) {
                Text("Save URL")
            }
            Button(
                onClick = onUseProduction,
                modifier = Modifier.weight(1f),
                enabled = baseUrl.trim() != PRODUCTION_BACKEND_BASE_URL,
            ) {
                Text("Use production")
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Button(onClick = onLogin, modifier = Modifier.weight(1f)) {
                Text("Login")
            }
            Button(onClick = onRefresh, modifier = Modifier.weight(1f), enabled = signedIn) {
                Text("Refresh")
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        Button(onClick = onClearToken, modifier = Modifier.fillMaxWidth(), enabled = signedIn) {
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
            color = Color(0xFF475569),
        )
    }
}

@Composable
private fun InfoCard(title: String, content: @Composable () -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(8.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(text = title, style = MaterialTheme.typography.labelLarge, color = Color(0xFF64748B))
            content()
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun TodayScreenPreview() {
    TodayScreen(settingsStore = InMemoryTodaySettingsStore())
}
