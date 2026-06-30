package com.transcendiverse.digitaltwin.launcher

import android.content.ComponentName
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.os.Build

const val DIGITAL_TWIN_PACKAGE_NAME = "com.transcendiverse.digitaltwin"

data class LauncherApp(
    val packageName: String,
    val label: String,
    val activityName: String?,
)

fun filterValidLauncherApps(apps: List<LauncherApp>): List<LauncherApp> =
    apps.filter { app -> app.packageName.isNotBlank() && app.label.isNotBlank() }

fun sortLauncherApps(apps: List<LauncherApp>): List<LauncherApp> =
    apps.sortedWith(
        compareBy<LauncherApp, String>(String.CASE_INSENSITIVE_ORDER) { it.label }
            .thenBy { it.packageName },
    )

fun filterSelfPackage(
    apps: List<LauncherApp>,
    selfPackageName: String = DIGITAL_TWIN_PACKAGE_NAME,
): List<LauncherApp> =
    apps.filterNot { app -> app.packageName == selfPackageName }

class LauncherAppsRepository(
    private val packageManager: PackageManager,
    private val selfPackageName: String? = null,
) {
    fun listLaunchableApps(): List<LauncherApp> {
        val launcherIntent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        val apps = queryLaunchableActivities(launcherIntent).mapNotNull { resolveInfo ->
            resolveInfo.toLauncherApp(packageManager)
        }
        val validApps = filterValidLauncherApps(apps)
        val visibleApps = selfPackageName?.let { filterSelfPackage(validApps, it) } ?: validApps
        return sortLauncherApps(visibleApps)
    }

    fun buildLaunchIntent(app: LauncherApp): Intent? =
        packageManager.getLaunchIntentForPackage(app.packageName)
            ?: app.activityName
                ?.takeIf { it.isNotBlank() }
                ?.let { activityName ->
                    Intent(Intent.ACTION_MAIN)
                        .addCategory(Intent.CATEGORY_LAUNCHER)
                        .setComponent(ComponentName(app.packageName, activityName))
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }

    private fun queryLaunchableActivities(intent: Intent): List<ResolveInfo> =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            packageManager.queryIntentActivities(intent, PackageManager.ResolveInfoFlags.of(0))
        } else {
            @Suppress("DEPRECATION")
            packageManager.queryIntentActivities(intent, 0)
        }
}

private fun ResolveInfo.toLauncherApp(packageManager: PackageManager): LauncherApp? {
    val activityInfo = activityInfo ?: return null
    return LauncherApp(
        packageName = activityInfo.packageName.orEmpty(),
        label = loadLabel(packageManager).toString(),
        activityName = activityInfo.name,
    )
}
