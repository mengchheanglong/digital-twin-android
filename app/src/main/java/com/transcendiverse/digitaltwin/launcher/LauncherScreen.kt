package com.transcendiverse.digitaltwin.launcher

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun LauncherScreen(
    todaySummary: LauncherTodaySummary,
    apps: List<LauncherApp>,
    favoriteApps: List<LauncherApp>,
    favoritePackageNames: Set<String>,
    isAppDrawerOpen: Boolean,
    onRefresh: () -> Unit,
    onOpenAppDrawer: () -> Unit,
    onCloseAppDrawer: () -> Unit,
    onLaunchApp: (LauncherApp) -> Unit,
    onToggleFavorite: (LauncherApp) -> Unit,
    onOpenSettings: () -> Unit,
    onOpenCompanion: () -> Unit,
) {
    MaterialTheme {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = Color(0xFFFAFAF8),
        ) {
            if (isAppDrawerOpen) {
                LauncherDrawerMode(
                    apps = apps,
                    favoritePackageNames = favoritePackageNames,
                    onCloseAppDrawer = onCloseAppDrawer,
                    onLaunchApp = onLaunchApp,
                    onToggleFavorite = onToggleFavorite,
                    onOpenSettings = onOpenSettings,
                    onOpenCompanion = onOpenCompanion,
                )
            } else {
                LauncherDailyMode(
                    todaySummary = todaySummary,
                    favoriteApps = favoriteApps,
                    onRefresh = onRefresh,
                    onOpenAppDrawer = onOpenAppDrawer,
                    onLaunchApp = onLaunchApp,
                    onOpenSettings = onOpenSettings,
                    onOpenCompanion = onOpenCompanion,
                )
            }
        }
    }
}

@Composable
private fun LauncherDailyMode(
    todaySummary: LauncherTodaySummary,
    favoriteApps: List<LauncherApp>,
    onRefresh: () -> Unit,
    onOpenAppDrawer: () -> Unit,
    onLaunchApp: (LauncherApp) -> Unit,
    onOpenSettings: () -> Unit,
    onOpenCompanion: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 28.dp, vertical = 30.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp),
    ) {
        LauncherHeader()
        LauncherDailySurface(
            summary = todaySummary,
            modifier = Modifier.fillMaxWidth(),
        )
        LauncherAllowedAppsSection(
            favoriteApps = favoriteApps,
            onLaunchApp = onLaunchApp,
            onOpenAppDrawer = onOpenAppDrawer,
        )
        LauncherEscapeActions(
            onRefresh = onRefresh,
            onOpenAppDrawer = onOpenAppDrawer,
            onOpenSettings = onOpenSettings,
            onOpenCompanion = onOpenCompanion,
        )
    }
}

@Composable
private fun LauncherDrawerMode(
    apps: List<LauncherApp>,
    favoritePackageNames: Set<String>,
    onCloseAppDrawer: () -> Unit,
    onLaunchApp: (LauncherApp) -> Unit,
    onToggleFavorite: (LauncherApp) -> Unit,
    onOpenSettings: () -> Unit,
    onOpenCompanion: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp, vertical = 24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        LauncherHeader()
        LauncherAppDrawer(
            apps = apps,
            favoritePackageNames = favoritePackageNames,
            onLaunchApp = onLaunchApp,
            onToggleFavorite = onToggleFavorite,
            onClose = onCloseAppDrawer,
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
        )
        LauncherEscapeActions(
            onRefresh = null,
            onOpenAppDrawer = null,
            onOpenSettings = onOpenSettings,
            onOpenCompanion = onOpenCompanion,
        )
    }
}

@Composable
private fun LauncherHeader() {
    val dateLabel = rememberLauncherDateLabel()
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            text = "Digital Twin",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.SemiBold,
            color = Color(0xFF111827),
        )
        Text(
            text = dateLabel,
            style = MaterialTheme.typography.bodyLarge,
            color = Color(0xFF4B5563),
        )
    }
}

@Composable
private fun rememberLauncherDateLabel(): String {
    return remember {
        SimpleDateFormat("EEE, MMM d", Locale.getDefault()).format(Date())
    }
}

@Composable
private fun LauncherAllowedAppsSection(
    favoriteApps: List<LauncherApp>,
    onLaunchApp: (LauncherApp) -> Unit,
    onOpenAppDrawer: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = "Allowed",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = Color(0xFF111827),
        )
        if (favoriteApps.isEmpty()) {
            Text(
                text = "choose allowed apps",
                modifier = Modifier.clickable(onClick = onOpenAppDrawer),
                style = MaterialTheme.typography.bodyLarge,
                color = Color(0xFF6B7280),
            )
        } else {
            favoriteApps.take(MAX_HOME_ALLOWED_APPS).forEach { app ->
                AllowedAppShortcut(app = app, onLaunchApp = onLaunchApp)
            }
            if (favoriteApps.size > MAX_HOME_ALLOWED_APPS) {
                Text(
                    text = "+${favoriteApps.size - MAX_HOME_ALLOWED_APPS} more in apps",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF6B7280),
                )
            }
        }
    }
}

@Composable
private fun AllowedAppShortcut(
    app: LauncherApp,
    onLaunchApp: (LauncherApp) -> Unit,
) {
    Text(
        text = app.label,
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onLaunchApp(app) }
            .padding(vertical = 4.dp),
        style = MaterialTheme.typography.bodyLarge,
        color = Color(0xFF1F2937),
    )
}

@Composable
private fun LauncherEscapeActions(
    onRefresh: (() -> Unit)?,
    onOpenAppDrawer: (() -> Unit)?,
    onOpenSettings: () -> Unit,
    onOpenCompanion: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        onOpenAppDrawer?.let { openAppDrawer ->
            TextButton(onClick = openAppDrawer) {
                Text("all apps")
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            TextButton(onClick = onOpenCompanion) {
                Text("companion")
            }
            TextButton(onClick = onOpenSettings) {
                Text("settings")
            }
            onRefresh?.let { refresh ->
                TextButton(onClick = refresh) {
                    Text("refresh")
                }
            }
        }
    }
}

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
        onRefresh = {},
        onOpenAppDrawer = {},
        onCloseAppDrawer = {},
        onLaunchApp = {},
        onToggleFavorite = {},
        onOpenSettings = {},
        onOpenCompanion = {},
    )
}

private const val MAX_HOME_ALLOWED_APPS = 7
