package com.transcendiverse.digitaltwin.launcher

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

@Composable
fun LauncherAppDrawer(
    apps: List<LauncherApp>,
    favoritePackageNames: Set<String>,
    onLaunchApp: (LauncherApp) -> Unit,
    onToggleFavorite: (LauncherApp) -> Unit,
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
                onQueryChange = { query = it },
            )
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
                        key = { app -> app.stableKey },
                    ) { app ->
                        LauncherAppRow(
                            app = app,
                            isAllowed = app.packageName in favoritePackageNames,
                            onLaunchApp = onLaunchApp,
                            onToggleAllowed = onToggleFavorite,
                        )
                    }
                    item {
                        DrawerManageHomeHint()
                    }
                }
            }
        }
    }
}

@Composable
private fun DrawerHeader(
    query: String,
    onQueryChange: (String) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = "Apps",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.SemiBold,
            color = DrawerInk,
        )
        OutlinedTextField(
            value = query,
            onValueChange = onQueryChange,
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            shape = RoundedCornerShape(8.dp),
            placeholder = { Text("Search apps") },
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = DrawerInk,
                unfocusedTextColor = DrawerInk,
                focusedContainerColor = DrawerInput,
                unfocusedContainerColor = DrawerInput,
                focusedBorderColor = DrawerAccent,
                unfocusedBorderColor = DrawerBorder,
                focusedPlaceholderColor = DrawerMuted,
                unfocusedPlaceholderColor = DrawerMuted,
                cursorColor = DrawerAccent,
            ),
        )
        HorizontalDivider(color = DrawerBorder)
    }
}

@Composable
private fun EmptyDrawerState(query: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(
            text = if (query.isBlank()) "no apps found" else "no matching apps",
            style = MaterialTheme.typography.bodyLarge,
            color = DrawerInk,
        )
        Text(
            text = if (query.isBlank()) "launchable apps will appear here" else "try a shorter app name",
            style = MaterialTheme.typography.bodyMedium,
            color = DrawerMuted,
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
    Column(
        modifier = Modifier
            .fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 48.dp)
                .clickable { onLaunchApp(app) }
                .padding(vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = app.displayLabel,
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.bodyLarge,
                color = DrawerInk,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            LauncherTextAction(
                text = if (isAllowed) "Hide" else "Allow",
                onClick = { onToggleAllowed(app) },
            )
        }
        HorizontalDivider(color = DrawerHairline)
    }
}

@Composable
private fun DrawerManageHomeHint() {
    Text(
        text = "Manage home apps",
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 14.dp, bottom = 4.dp),
        style = MaterialTheme.typography.labelMedium,
        color = DrawerMuted,
        fontWeight = FontWeight.Medium,
    )
}

private val DrawerInput = LauncherSurfaceSoftColor
private val DrawerAccent = LauncherAccentColor
private val DrawerInk = LauncherTextColor
private val DrawerMuted = LauncherMutedActionColor
private val DrawerBorder = LauncherBorderColor
private val DrawerHairline = Color(0x0FFFFFFF)
