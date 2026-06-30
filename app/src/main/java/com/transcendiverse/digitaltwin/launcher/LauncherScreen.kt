package com.transcendiverse.digitaltwin.launcher

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
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
fun LauncherScreen(
    todaySummary: LauncherTodaySummary,
    apps: List<LauncherApp>,
    isAppDrawerOpen: Boolean,
    onRefresh: () -> Unit,
    onOpenAppDrawer: () -> Unit,
    onCloseAppDrawer: () -> Unit,
    onLaunchApp: (LauncherApp) -> Unit,
    onOpenSettings: () -> Unit,
    onOpenCompanion: () -> Unit,
) {
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
                LauncherHeader()
                SafetyCard()
                if (isAppDrawerOpen) {
                    LauncherAppDrawer(
                        apps = apps,
                        onLaunchApp = onLaunchApp,
                        onClose = onCloseAppDrawer,
                        modifier = Modifier.fillMaxWidth(),
                    )
                } else {
                    LauncherDailySurface(
                        summary = todaySummary,
                        onRefresh = onRefresh,
                        onOpenCompanion = onOpenCompanion,
                        onOpenAppDrawer = onOpenAppDrawer,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
                LauncherEscapeActions(
                    onOpenSettings = onOpenSettings,
                    onOpenCompanion = onOpenCompanion,
                )
            }
        }
    }
}

@Composable
private fun LauncherHeader() {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
            text = "Digital Twin Launcher",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
        )
        Text(
            text = "Safe home shell prototype",
            style = MaterialTheme.typography.titleMedium,
            color = Color(0xFF334155),
        )
    }
}

@Composable
private fun SafetyCard() {
    InfoCard(title = "Prototype mode") {
        Text(
            text = "This selectable Home app will not make itself your default launcher.",
            style = MaterialTheme.typography.bodyMedium,
            color = Color(0xFF475569),
        )
        Text(
            text = "Use Settings or Companion anytime to leave this shell.",
            style = MaterialTheme.typography.bodyMedium,
            color = Color(0xFF475569),
        )
    }
}

@Composable
private fun LauncherEscapeActions(
    onOpenSettings: () -> Unit,
    onOpenCompanion: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        OutlinedButton(
            onClick = onOpenSettings,
            modifier = Modifier.weight(1f),
        ) {
            Text(LauncherSafetyActions.OPEN_SETTINGS_LABEL)
        }
        Button(
            onClick = onOpenCompanion,
            modifier = Modifier.weight(1f),
        ) {
            Text(LauncherSafetyActions.OPEN_COMPANION_LABEL)
        }
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
private fun LauncherScreenPreview() {
    LauncherScreen(
        todaySummary = LauncherTodaySummary.from(null),
        apps = listOf(
            LauncherApp(
                packageName = "com.example.notes",
                label = "Notes",
                activityName = "com.example.notes.MainActivity",
            ),
        ),
        isAppDrawerOpen = false,
        onRefresh = {},
        onOpenAppDrawer = {},
        onCloseAppDrawer = {},
        onLaunchApp = {},
        onOpenSettings = {},
        onOpenCompanion = {},
    )
}
