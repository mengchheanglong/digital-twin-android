package com.transcendiverse.digitaltwin.launcher

import android.content.Context

private const val LAUNCHER_PREFS_NAME = "digital_twin_launcher"
private const val FAVORITE_PACKAGES_KEY = "favorite_packages"

class LauncherFavoritesStore(context: Context) {
    private val preferences = context.getSharedPreferences(LAUNCHER_PREFS_NAME, Context.MODE_PRIVATE)

    fun load(): Set<String> = preferences
        .getStringSet(FAVORITE_PACKAGES_KEY, emptySet())
        .orEmpty()
        .filterTo(linkedSetOf()) { packageName -> packageName.isNotBlank() }

    fun toggle(packageName: String): Set<String> {
        val next = toggleFavoritePackage(load(), packageName)
        preferences.edit().putStringSet(FAVORITE_PACKAGES_KEY, next).apply()
        return next
    }
}

fun toggleFavoritePackage(
    favoritePackageNames: Set<String>,
    packageName: String,
): Set<String> {
    val cleaned = packageName.trim()
    if (cleaned.isBlank()) return favoritePackageNames

    return favoritePackageNames
        .toMutableSet()
        .also { favorites ->
            if (!favorites.add(cleaned)) favorites.remove(cleaned)
        }
        .toSet()
}

fun toggleAllowedPackage(
    allowedPackageNames: Set<String>,
    packageName: String,
): Set<String> = toggleFavoritePackage(allowedPackageNames, packageName)

fun favoriteLauncherApps(
    apps: List<LauncherApp>,
    favoritePackageNames: Set<String>,
): List<LauncherApp> = apps.filter { app -> app.packageName in favoritePackageNames }

fun allowedLauncherApps(
    apps: List<LauncherApp>,
    allowedPackageNames: Set<String>,
): List<LauncherApp> = favoriteLauncherApps(apps, allowedPackageNames)
