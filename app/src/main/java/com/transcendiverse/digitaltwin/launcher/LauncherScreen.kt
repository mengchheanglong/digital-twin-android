package com.transcendiverse.digitaltwin.launcher

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
    onOpenAppDrawer: () -> Unit,
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
                TodayPlaceholder()
                AppDrawerPlaceholder(onOpenAppDrawer = onOpenAppDrawer)
                LauncherActions(
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
            text = "This launcher is selectable for testing and will not make itself your default Home app.",
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
private fun TodayPlaceholder() {
    InfoCard(title = "Today") {
        Text(
            text = "Daily surface placeholder",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "Cached mood, streak, quest, and next action will appear here in launcher mode.",
            style = MaterialTheme.typography.bodyMedium,
            color = Color(0xFF475569),
        )
    }
}

@Composable
private fun AppDrawerPlaceholder(onOpenAppDrawer: () -> Unit) {
    InfoCard(title = "App drawer") {
        Text(
            text = "Installed apps will appear here after the app drawer task lands.",
            style = MaterialTheme.typography.bodyMedium,
            color = Color(0xFF475569),
        )
        Spacer(modifier = Modifier.height(10.dp))
        Button(
            onClick = onOpenAppDrawer,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("Open app drawer")
        }
    }
}

@Composable
private fun LauncherActions(
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
            Text("Settings")
        }
        Button(
            onClick = onOpenCompanion,
            modifier = Modifier.weight(1f),
        ) {
            Text("Companion")
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
        onOpenAppDrawer = {},
        onOpenSettings = {},
        onOpenCompanion = {},
    )
}
