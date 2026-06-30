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
import com.transcendiverse.digitaltwin.data.FakeTodayRepository
import com.transcendiverse.digitaltwin.data.InMemoryTodayCacheStore
import com.transcendiverse.digitaltwin.data.InMemoryTodaySettingsStore
import com.transcendiverse.digitaltwin.data.LoginRepository
import com.transcendiverse.digitaltwin.data.NetworkTodayRepository
import com.transcendiverse.digitaltwin.data.NetworkLoginRepository
import com.transcendiverse.digitaltwin.data.TodayCacheStore
import com.transcendiverse.digitaltwin.data.TodayRepository
import com.transcendiverse.digitaltwin.data.TodaySettings
import com.transcendiverse.digitaltwin.data.TodaySettingsStore
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

    suspend fun loadToday(settings: TodaySettings, loadedStatus: String? = null) {
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
        } catch (error: Exception) {
            val cached = cacheStore.load()
            today = cached?.today
            status = "Refresh failed: ${error.message ?: "Unable to load Today"}"
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
                                loadToday(settings, loadedStatus = "Login successful - background sync scheduled")
                            } catch (error: Exception) {
                                status = "Login failed: ${error.message ?: "Unable to sign in"}"
                            }
                        }
                    },
                    onRefresh = {
                        val settings = savedSettings.copy(baseUrl = baseUrl.trim())
                        savedSettings = settings
                        onEnqueueBackgroundSync()
                        scope.launch { loadToday(settings) }
                    },
                    onClearToken = {
                        val settings = savedSettings.copy(token = "", lastUserName = "")
                        settingsStore.save(settings)
                        savedSettings = settings
                        status = if (settings.baseUrl.isBlank()) "Fixture mode" else "Token cleared"
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
    onClearToken: () -> Unit,
) {
    InfoCard(title = "Login") {
        Text(
            text = "Status: $status",
            style = MaterialTheme.typography.bodyMedium,
            color = Color(0xFF475569),
        )
        if (signedIn) {
            Text(
                text = "Signed in",
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFF0F766E),
            )
        }
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
            Button(onClick = onLogin, modifier = Modifier.weight(1f)) {
                Text("Login")
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Button(onClick = onRefresh, modifier = Modifier.weight(1f), enabled = signedIn) {
                Text("Refresh")
            }
            Button(onClick = onClearToken, modifier = Modifier.weight(1f), enabled = signedIn) {
                Text("Clear token")
            }
        }
    }
}

fun validateLoginInput(baseUrl: String, email: String, password: String): String? = when {
    baseUrl.isBlank() -> "Backend base URL is required"
    email.isBlank() -> "Email is required"
    password.isBlank() -> "Password is required"
    else -> null
}

fun cachedStatus(cachedAtEpochMillis: Long): String = "Cached - last updated ${formatTimestamp(cachedAtEpochMillis)}"

fun lastUpdatedStatus(cachedAtEpochMillis: Long): String = "Last updated ${formatTimestamp(cachedAtEpochMillis)}"

private fun formatTimestamp(epochMillis: Long): String =
    DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT).format(Date(epochMillis))

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
