package com.transcendiverse.digitaltwin.launcher

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
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
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = Color.Transparent,
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            LauncherTodayLines(summary)
        }
    }
}

@Composable
private fun LauncherTodayLines(summary: LauncherTodaySummary) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text(
            text = "Today",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = Color(0xFF111827),
        )
        SummaryLine(label = "Mood", value = summary.mood ?: "open companion")
        SummaryLine(label = "Quest", value = summary.currentQuest ?: "open companion")
        SummaryLine(label = "Check-in", value = summary.checkInStatus ?: "pending")
    }
}

@Composable
private fun SummaryLine(label: String, value: String) {
    Text(
        text = "$label: $value",
        style = MaterialTheme.typography.bodyLarge,
        color = Color(0xFF1F2937),
    )
}

@Preview(showBackground = true)
@Composable
private fun LauncherDailySurfacePreview() {
    LauncherDailySurface(
        summary = LauncherTodaySummary.from(null),
    )
}
