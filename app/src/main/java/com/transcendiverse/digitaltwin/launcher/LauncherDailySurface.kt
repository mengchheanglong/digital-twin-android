package com.transcendiverse.digitaltwin.launcher

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

@Composable
fun LauncherDailySurface(
    summary: LauncherTodaySummary,
    actions: LauncherSafetyActions = LauncherSafetyActions(),
    onRefresh: () -> Unit,
    onOpenCompanion: () -> Unit,
    onOpenAppDrawer: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = Color.Transparent,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            LauncherSummaryCard(summary)
            LauncherActionButtons(
                actions = actions,
                onRefresh = onRefresh,
                onOpenCompanion = onOpenCompanion,
                onOpenAppDrawer = onOpenAppDrawer,
            )
        }
    }
}

@Composable
private fun LauncherSummaryCard(summary: LauncherTodaySummary) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(8.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = summary.title,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
            )
            summary.emptyState?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodyLarge,
                    color = Color(0xFF475569),
                )
            }
            summary.mood?.let { SummaryLine(label = "Mood", value = it) }
            summary.streak?.let { SummaryLine(label = "Streak", value = it) }
            summary.checkInStatus?.let { SummaryLine(label = "Check-in", value = it) }
            summary.currentQuest?.let { SummaryLine(label = "Quest", value = it) }
            summary.nextAction?.let { SummaryLine(label = "Next", value = it) }
            Text(
                text = summary.cacheLabel,
                style = MaterialTheme.typography.labelMedium,
                color = Color(0xFF64748B),
            )
        }
    }
}

@Composable
private fun SummaryLine(label: String, value: String) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = Color(0xFF64748B),
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyLarge,
            color = Color(0xFF0F172A),
        )
    }
}

@Composable
private fun LauncherActionButtons(
    actions: LauncherSafetyActions,
    onRefresh: () -> Unit,
    onOpenCompanion: () -> Unit,
    onOpenAppDrawer: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Button(
            onClick = onRefresh,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(actions.refreshLabel)
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            OutlinedButton(
                onClick = onOpenCompanion,
                modifier = Modifier.weight(1f),
            ) {
                Text(actions.openCompanionLabel)
            }
            OutlinedButton(
                onClick = onOpenAppDrawer,
                modifier = Modifier.weight(1f),
            ) {
                Text(actions.openAppDrawerLabel)
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun LauncherDailySurfacePreview() {
    LauncherDailySurface(
        summary = LauncherTodaySummary.from(null),
        onRefresh = {},
        onOpenCompanion = {},
        onOpenAppDrawer = {},
    )
}
