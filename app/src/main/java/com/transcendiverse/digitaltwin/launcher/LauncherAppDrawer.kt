package com.transcendiverse.digitaltwin.launcher

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun LauncherAppDrawer(
    apps: List<LauncherApp>,
    onLaunchApp: (LauncherApp) -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier,
        color = Color.White,
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = "Apps",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold,
                )
                TextButton(onClick = onClose) {
                    Text("Close")
                }
            }
            HorizontalDivider()
            if (apps.isEmpty()) {
                EmptyDrawerState()
            } else {
                LazyColumn {
                    items(
                        items = apps,
                        key = { app -> "${app.packageName}/${app.activityName.orEmpty()}" },
                    ) { app ->
                        LauncherAppRow(app = app, onLaunchApp = onLaunchApp)
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptyDrawerState() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(
            text = "No apps found",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
        )
        Text(
            text = "Launchable apps will appear here.",
            style = MaterialTheme.typography.bodyMedium,
            color = Color(0xFF64748B),
        )
    }
}

@Composable
private fun LauncherAppRow(
    app: LauncherApp,
    onLaunchApp: (LauncherApp) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onLaunchApp(app) }
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Text(
            text = app.label,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
        )
        Text(
            text = app.packageName,
            style = MaterialTheme.typography.bodySmall,
            color = Color(0xFF64748B),
        )
    }
    HorizontalDivider()
}
