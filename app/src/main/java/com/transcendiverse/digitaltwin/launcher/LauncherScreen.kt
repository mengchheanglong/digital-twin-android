package com.transcendiverse.digitaltwin.launcher

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
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
            color = Color(0xFFF8FAFC),
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
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        LauncherHeader()
        SafetyCard()
        LauncherDailySurface(
            summary = todaySummary,
            onRefresh = onRefresh,
            onOpenCompanion = onOpenCompanion,
            onOpenAppDrawer = onOpenAppDrawer,
            modifier = Modifier.fillMaxWidth(),
        )
        LauncherFavoritesCard(
            favoriteApps = favoriteApps,
            onLaunchApp = onLaunchApp,
            onOpenAppDrawer = onOpenAppDrawer,
        )
        LauncherEscapeActions(
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
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        LauncherHeader()
        SafetyCard()
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
            onOpenSettings = onOpenSettings,
            onOpenCompanion = onOpenCompanion,
        )
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
            text = "Prototype home shell — improve first, make real later",
            style = MaterialTheme.typography.titleMedium,
            color = Color(0xFF334155),
        )
    }
}

@Composable
private fun SafetyCard() {
    InfoCard(title = "Prototype mode") {
        Text(
            text = "This launcher is for direct testing before becoming your default Home app.",
            style = MaterialTheme.typography.bodyMedium,
            color = Color(0xFF475569),
        )
        Text(
            text = "Settings and Companion stay visible so you always have an escape path.",
            style = MaterialTheme.typography.bodyMedium,
            color = Color(0xFF475569),
        )
    }
}

@Composable
private fun LauncherFavoritesCard(
    favoriteApps: List<LauncherApp>,
    onLaunchApp: (LauncherApp) -> Unit,
    onOpenAppDrawer: () -> Unit,
) {
    InfoCard(title = "Favorites") {
        if (favoriteApps.isEmpty()) {
            Text(
                text = "No favorite apps yet. Open Apps and tap ☆ to pin the apps you use most.",
                style = MaterialTheme.typography.bodyMedium,
                color = Color(0xFF475569),
            )
            OutlinedButton(onClick = onOpenAppDrawer) {
                Text("Choose favorites")
            }
        } else {
            favoriteApps.take(MAX_HOME_FAVORITES).forEach { app ->
                FavoriteAppShortcut(app = app, onLaunchApp = onLaunchApp)
            }
            if (favoriteApps.size > MAX_HOME_FAVORITES) {
                Text(
                    text = "+${favoriteApps.size - MAX_HOME_FAVORITES} more in Apps",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF64748B),
                )
            }
        }
    }
}

@Composable
private fun FavoriteAppShortcut(
    app: LauncherApp,
    onLaunchApp: (LauncherApp) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onLaunchApp(app) }
            .padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(text = "★", color = Color(0xFFF59E0B))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = app.label,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = app.packageName,
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFF64748B),
            )
        }
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
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(text = title, style = MaterialTheme.typography.labelLarge, color = Color(0xFF64748B))
            content()
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

private const val MAX_HOME_FAVORITES = 5
