package com.transcendiverse.digitaltwin.launcher

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun LauncherAppDrawer(
    apps: List<LauncherApp>,
    favoritePackageNames: Set<String>,
    onLaunchApp: (LauncherApp) -> Unit,
    onToggleFavorite: (LauncherApp) -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var query by rememberSaveable { mutableStateOf("") }
    val visibleApps = searchLauncherApps(apps, query)

    Surface(
        modifier = modifier,
        color = Color.Transparent,
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            DrawerHeader(
                query = query,
                resultCount = visibleApps.size,
                onQueryChange = { query = it },
                onClose = onClose,
            )
            HorizontalDivider(color = Color(0xFFE5E7EB))
            if (visibleApps.isEmpty()) {
                EmptyDrawerState(query = query)
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                ) {
                    items(
                        items = visibleApps,
                        key = { app -> "${app.packageName}/${app.activityName.orEmpty()}" },
                    ) { app ->
                        LauncherAppRow(
                            app = app,
                            isAllowed = app.packageName in favoritePackageNames,
                            onLaunchApp = onLaunchApp,
                            onToggleAllowed = onToggleFavorite,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DrawerHeader(
    query: String,
    resultCount: Int,
    onQueryChange: (String) -> Unit,
    onClose: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = "Apps",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF111827),
                )
                Text(
                    text = "$resultCount apps",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF6B7280),
                )
            }
            LauncherTextAction(text = "close", onClick = onClose)
        }
        OutlinedTextField(
            value = query,
            onValueChange = onQueryChange,
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            label = { Text("search") },
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Color(0xFF4B5563),
                unfocusedBorderColor = Color(0xFFD1D5DB),
                focusedLabelColor = Color(0xFF4B5563),
                unfocusedLabelColor = Color(0xFF6B7280),
                cursorColor = Color(0xFF374151),
            ),
        )
    }
}

@Composable
private fun EmptyDrawerState(query: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 18.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(
            text = if (query.isBlank()) "no apps found" else "no matching apps",
            style = MaterialTheme.typography.bodyLarge,
            color = Color(0xFF1F2937),
        )
        Text(
            text = if (query.isBlank()) "launchable apps will appear here" else "try a shorter app name",
            style = MaterialTheme.typography.bodyMedium,
            color = Color(0xFF6B7280),
        )
    }
}

@Composable
private fun LauncherAppRow(
    app: LauncherApp,
    isAllowed: Boolean,
    onLaunchApp: (LauncherApp) -> Unit,
    onToggleAllowed: (LauncherApp) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onLaunchApp(app) }
            .padding(vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = app.label,
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.bodyLarge,
            color = Color(0xFF1F2937),
        )
        LauncherTextAction(
            text = if (isAllowed) "hide" else "allow",
            onClick = { onToggleAllowed(app) },
        )
    }
    HorizontalDivider(color = Color(0xFFE5E7EB))
}
