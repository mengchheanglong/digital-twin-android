package com.transcendiverse.digitaltwin.launcher

import android.Manifest
import android.content.ContentUris
import android.content.Context
import android.content.pm.PackageManager
import android.provider.CalendarContract
import android.text.format.DateFormat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

private const val CALENDAR_ACCOUNT_TYPE_COLUMN = "account_type"

data class LauncherCalendarEvent(
    val title: String,
    val timeLabel: String,
    val sourceLabel: String,
    val beginMillis: Long,
    val endMillis: Long,
    val allDay: Boolean,
)

internal fun Context.hasLauncherCalendarPermission(): Boolean =
    checkSelfPermission(Manifest.permission.READ_CALENDAR) == PackageManager.PERMISSION_GRANTED

internal fun loadUpcomingLauncherCalendarEvents(
    context: Context,
    limit: Int = 24,
): List<LauncherCalendarEvent> {
    if (!context.hasLauncherCalendarPermission()) return emptyList()

    val now = System.currentTimeMillis()
    val horizon = now + TimeUnit.DAYS.toMillis(14)
    val uriBuilder = CalendarContract.Instances.CONTENT_URI.buildUpon()
    ContentUris.appendId(uriBuilder, now)
    ContentUris.appendId(uriBuilder, horizon)

    val projection = arrayOf(
        CalendarContract.Instances.TITLE,
        CalendarContract.Instances.BEGIN,
        CalendarContract.Instances.END,
        CalendarContract.Instances.ALL_DAY,
        CalendarContract.Instances.CALENDAR_DISPLAY_NAME,
        CALENDAR_ACCOUNT_TYPE_COLUMN,
    )

    val events = mutableListOf<LauncherCalendarEvent>()
    return try {
        context.contentResolver.query(
            uriBuilder.build(),
            projection,
            null,
            null,
            "${CalendarContract.Instances.BEGIN} ASC",
        )?.use { cursor ->
            val titleIndex = cursor.getColumnIndexOrThrow(CalendarContract.Instances.TITLE)
            val beginIndex = cursor.getColumnIndexOrThrow(CalendarContract.Instances.BEGIN)
            val endIndex = cursor.getColumnIndexOrThrow(CalendarContract.Instances.END)
            val allDayIndex = cursor.getColumnIndexOrThrow(CalendarContract.Instances.ALL_DAY)
            val displayNameIndex = cursor.getColumnIndexOrThrow(CalendarContract.Instances.CALENDAR_DISPLAY_NAME)
            val accountTypeIndex = cursor.getColumnIndex(CALENDAR_ACCOUNT_TYPE_COLUMN)

            while (cursor.moveToNext() && events.size < limit) {
                val title = cursor.getString(titleIndex)?.trim().orEmpty().ifBlank { "Untitled event" }
                val beginMillis = cursor.getLong(beginIndex)
                val endMillis = cursor.getLong(endIndex)
                val allDay = cursor.getInt(allDayIndex) == 1
                val displayName = cursor.getString(displayNameIndex).orEmpty()
                val accountType = if (accountTypeIndex >= 0) cursor.getString(accountTypeIndex).orEmpty() else ""
                val sourceLabel = if (accountType.contains("google", ignoreCase = true)) {
                    "Google Calendar"
                } else {
                    safeCalendarSourceLabel(displayName)
                }

                events += LauncherCalendarEvent(
                    title = title,
                    timeLabel = formatLauncherCalendarTime(context, beginMillis, endMillis, allDay),
                    sourceLabel = sourceLabel,
                    beginMillis = beginMillis,
                    endMillis = endMillis,
                    allDay = allDay,
                )
            }
        }
        events
    } catch (_: SecurityException) {
        emptyList()
    } catch (_: Exception) {
        emptyList()
    }
}

private fun formatLauncherCalendarTime(
    context: Context,
    beginMillis: Long,
    endMillis: Long,
    allDay: Boolean,
): String {
    val locale = Locale.getDefault()
    val now = Calendar.getInstance(locale)
    val start = Calendar.getInstance(locale).apply { timeInMillis = beginMillis }
    val sameDay = now.get(Calendar.YEAR) == start.get(Calendar.YEAR) &&
        now.get(Calendar.DAY_OF_YEAR) == start.get(Calendar.DAY_OF_YEAR)

    if (allDay) {
        return if (sameDay) {
            "Today • all day"
        } else {
            SimpleDateFormat("EEE, MMM d", locale).format(Date(beginMillis))
        }
    }

    val timePattern = if (DateFormat.is24HourFormat(context)) "HH:mm" else "h:mm a"
    val startPattern = if (sameDay) timePattern else "EEE, MMM d • $timePattern"
    val startLabel = SimpleDateFormat(startPattern, locale).format(Date(beginMillis))
    if (endMillis <= beginMillis) return startLabel

    val endLabel = SimpleDateFormat(timePattern, locale).format(Date(endMillis))
    return "$startLabel-$endLabel"
}

private fun safeCalendarSourceLabel(displayName: String): String {
    val clean = displayName.trim()
    return when {
        clean.isBlank() -> "Calendar"
        "@" in clean -> "Calendar"
        clean.length > 28 -> clean.take(27).trimEnd() + "…"
        else -> clean
    }
}
