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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.transcendiverse.digitaltwin.data.FakeTodayRepository
import com.transcendiverse.digitaltwin.data.TodayRepository
import com.transcendiverse.digitaltwin.model.MobileToday

@Composable
fun TodayScreen(repository: TodayRepository) {
    val today = remember { repository.getToday() }

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
                Header(today)
                StatusCards(today)
                QuestCard(today)
                InsightCard(today)
                LauncherActions(today)
                SettingsPanel()
            }
        }
    }
}

@Composable
private fun Header(today: MobileToday) {
    val (emoji, label) = moodDisplay(today.user.mood)

    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
            text = "Digital Twin",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
        )
        Text(
            text = "$emoji ${today.user.name} is $label",
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
            text = quest?.title ?: "No active quest",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = if (quest == null) {
                "Reflect or pick a new quest from the web app."
            } else {
                "${quest.progress}/${quest.target} complete"
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
private fun SettingsPanel() {
    var baseUrl by remember { mutableStateOf("") }
    var token by remember { mutableStateOf("") }

    InfoCard(title = "Settings") {
        OutlinedTextField(
            value = baseUrl,
            onValueChange = { baseUrl = it },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Backend base URL") },
            singleLine = true,
        )
        Spacer(modifier = Modifier.height(10.dp))
        OutlinedTextField(
            value = token,
            onValueChange = { token = it },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("JWT token placeholder") },
            visualTransformation = PasswordVisualTransformation(),
            singleLine = true,
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

private fun moodDisplay(mood: String): Pair<String, String> = when (mood.lowercase()) {
    "focused" -> "\uD83C\uDFAF" to "focused"
    "good", "steady" -> "\uD83D\uDE42" to mood
    "low" -> "\uD83C\uDF27" to "low"
    else -> "\u2728" to mood.ifBlank { "present" }
}

@Preview(showBackground = true)
@Composable
private fun TodayScreenPreview() {
    TodayScreen(repository = FakeTodayRepository())
}
