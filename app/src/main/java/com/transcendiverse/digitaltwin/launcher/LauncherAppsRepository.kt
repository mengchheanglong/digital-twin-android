package com.transcendiverse.digitaltwin.launcher

import android.content.ComponentName
import android.content.Intent
import android.content.pm.LauncherActivityInfo
import android.content.pm.LauncherApps
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.os.Build
import android.os.Process
import android.os.UserHandle
import android.os.UserManager
import java.util.Locale

const val DIGITAL_TWIN_PACKAGE_NAME = "com.transcendiverse.digitaltwin"

data class LauncherApp(
    val packageName: String,
    val label: String,
    val activityName: String?,
    val userHandle: UserHandle? = null,
    val userSerial: Long? = null,
    val isOwnerProfile: Boolean? = null,
    val profileLabel: String? = null,
)

val LauncherApp.stableKey: String
    get() = listOf(
        packageName,
        activityName.orEmpty(),
        userSerial?.toString().orEmpty(),
        profileLabel.orEmpty(),
    ).joinToString("/")

fun filterValidLauncherApps(apps: List<LauncherApp>): List<LauncherApp> =
    apps.filter { app -> app.packageName.isNotBlank() && app.label.isNotBlank() }

fun sortLauncherApps(apps: List<LauncherApp>): List<LauncherApp> =
    apps.sortedWith(
        compareBy<LauncherApp, String>(String.CASE_INSENSITIVE_ORDER) { it.displayLabel }
            .thenBy { it.packageName }
            .thenBy { it.activityName.orEmpty() }
            .thenBy { it.userSerial ?: Long.MIN_VALUE },
    )

fun filterSelfPackage(
    apps: List<LauncherApp>,
    selfPackageName: String = DIGITAL_TWIN_PACKAGE_NAME,
): List<LauncherApp> =
    apps.filterNot { app -> app.packageName == selfPackageName }

fun searchLauncherApps(
    apps: List<LauncherApp>,
    query: String,
): List<LauncherApp> {
    val cleaned = query.trim()
    if (cleaned.isBlank()) return apps

    return apps.filter { app ->
        app.displayLabel.contains(cleaned, ignoreCase = true) ||
            app.label.contains(cleaned, ignoreCase = true) ||
            app.packageName.contains(cleaned, ignoreCase = true) ||
            app.profileLabel?.contains(cleaned, ignoreCase = true) == true
    }
}

val LauncherApp.displayLabel: String
    get() {
        val baseLabel = label.toLauncherDisplayLabel()
        val profile = profileLabel
            ?.takeIf { it.isNotBlank() }
            ?.toLauncherDisplayLabel()
        return if (profile == null) baseLabel else "$baseLabel · $profile"
    }

private fun String.toLauncherDisplayLabel(): String {
    val cleaned = replace('_', ' ')
        .replace('-', ' ')
        .trim()
        .replace(Regex("\\s+"), " ")

    if (cleaned.isBlank()) return trim()

    return cleaned.split(' ').joinToString(" ") { word ->
        word.replaceFirstChar { first ->
            if (first.isLowerCase()) first.titlecase(Locale.getDefault()) else first.toString()
        }
    }
}

class LauncherAppsRepository(
    private val packageManager: PackageManager,
    private val launcherApps: LauncherApps? = null,
    private val userManager: UserManager? = null,
    private val selfPackageName: String? = null,
) {
    fun listLaunchableApps(): List<LauncherApp> {
        val profileApps = queryLauncherProfileActivities()
        val apps = profileApps.ifEmpty { queryPackageManagerLaunchableActivities() }
        val validApps = filterValidLauncherApps(apps)
        val visibleApps = selfPackageName?.let { filterSelfPackage(validApps, it) } ?: validApps
        return sortLauncherApps(labelDuplicateProfileApps(visibleApps))
    }

    fun launchApp(
        app: LauncherApp,
        startActivity: (Intent) -> Unit,
    ): Boolean {
        val component = app.componentName()
        val service = launcherApps
        if (component != null && app.userHandle != null && service != null) {
            val launchedFromProfile = runCatching {
                service.startMainActivity(component, app.userHandle, null, null)
                true
            }.getOrDefault(false)

            if (launchedFromProfile) return true
            if (app.isOwnerProfile == false) return false
        }

        val launchIntent = buildLaunchIntent(app) ?: return false
        return runCatching {
            startActivity(launchIntent)
            true
        }.getOrDefault(false)
    }

    fun buildLaunchIntent(app: LauncherApp): Intent? =
        app.componentName()
            ?.let { component ->
                Intent(Intent.ACTION_MAIN)
                    .addCategory(Intent.CATEGORY_LAUNCHER)
                    .setComponent(component)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            ?: packageManager.getLaunchIntentForPackage(app.packageName)

    private fun queryLauncherProfileActivities(): List<LauncherApp> {
        val service = launcherApps ?: return emptyList()
        val ownerHandle = Process.myUserHandle()
        val profiles = runCatching { service.profiles }.getOrDefault(emptyList())
        if (profiles.isEmpty()) return emptyList()

        return profiles.flatMap { userHandle ->
            val serial = userManager?.serialFor(userHandle)
            val isOwner = userHandle == ownerHandle
            runCatching {
                service.getActivityList(null, userHandle).map { activity ->
                    activity.toLauncherApp(
                        userHandle = userHandle,
                        userSerial = serial,
                        isOwnerProfile = isOwner,
                    )
                }
            }.getOrDefault(emptyList())
        }
    }

    private fun queryPackageManagerLaunchableActivities(): List<LauncherApp> {
        val launcherIntent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        return queryLaunchableActivities(launcherIntent).mapNotNull { resolveInfo ->
            resolveInfo.toLauncherApp(packageManager)
        }
    }

    private fun queryLaunchableActivities(intent: Intent): List<ResolveInfo> =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            packageManager.queryIntentActivities(intent, PackageManager.ResolveInfoFlags.of(0))
        } else {
            @Suppress("DEPRECATION")
            packageManager.queryIntentActivities(intent, 0)
        }
}

private fun LauncherApp.componentName(): ComponentName? =
    activityName
        ?.takeIf { it.isNotBlank() }
        ?.let { activityName -> ComponentName(packageName, activityName) }

internal fun labelDuplicateProfileApps(apps: List<LauncherApp>): List<LauncherApp> {
    val duplicateKeys = apps
        .groupingBy { app -> app.packageName to app.activityName.orEmpty() }
        .eachCount()
        .filterValues { count -> count > 1 }
        .keys

    if (duplicateKeys.isEmpty()) return apps

    return apps.map { app ->
        val duplicateKey = app.packageName to app.activityName.orEmpty()
        if (duplicateKey !in duplicateKeys || !app.profileLabel.isNullOrBlank()) return@map app

        app.copy(
            profileLabel = if (app.isOwnerProfile == false) {
                "clone"
            } else {
                "main"
            },
        )
    }
}

private fun UserManager.serialFor(userHandle: UserHandle): Long? =
    runCatching { getSerialNumberForUser(userHandle).takeIf { serial -> serial >= 0L } }
        .getOrNull()

private fun LauncherActivityInfo.toLauncherApp(
    userHandle: UserHandle,
    userSerial: Long?,
    isOwnerProfile: Boolean,
): LauncherApp =
    LauncherApp(
        packageName = componentName.packageName,
        label = label.toString(),
        activityName = componentName.className,
        userHandle = userHandle,
        userSerial = userSerial,
        isOwnerProfile = isOwnerProfile,
    )

private fun ResolveInfo.toLauncherApp(packageManager: PackageManager): LauncherApp? {
    val activityInfo = activityInfo ?: return null
    return LauncherApp(
        packageName = activityInfo.packageName.orEmpty(),
        label = loadLabel(packageManager).toString(),
        activityName = activityInfo.name,
        isOwnerProfile = true,
    )
}
