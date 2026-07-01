package com.transcendiverse.digitaltwin.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.action.ActionParameters
import androidx.glance.background
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.provideContent
import androidx.glance.appwidget.updateAll
import androidx.glance.layout.Column
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.padding
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.transcendiverse.digitaltwin.MainActivity
import com.transcendiverse.digitaltwin.data.SharedPreferencesTodayCacheStore
import com.transcendiverse.digitaltwin.sync.TodaySyncScheduler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class TodayGlanceWidget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val summary = withContext(Dispatchers.IO) {
            TodayWidgetSummary.from(SharedPreferencesTodayCacheStore(context).load())
        }

        provideContent {
            TodayWidgetContent(summary)
        }
    }
}

class TodayGlanceWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = TodayGlanceWidget()
}

object TodayWidgetUpdater {
    suspend fun update(context: Context) {
        TodayGlanceWidget().updateAll(context)
    }
}

class RefreshTodayActionCallback : ActionCallback {
    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters,
    ) {
        TodaySyncScheduler.enqueueOneTime(context)
        TodayWidgetUpdater.update(context)
    }
}

private val WidgetSurfaceColor = ColorProvider(Color(0xFF111827))
private val WidgetTitleColor = ColorProvider(Color(0xFFF8FAFC))
private val WidgetBodyColor = ColorProvider(Color(0xFFE5E7EB))
private val WidgetMutedColor = ColorProvider(Color(0xFFCBD5E1))
private val WidgetActionColor = ColorProvider(Color(0xFF5EEAD4))

@Composable
private fun TodayWidgetContent(summary: TodayWidgetSummary) {
    Column(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(WidgetSurfaceColor)
            .clickable(actionStartActivity<MainActivity>())
            .padding(10.dp),
    ) {
        Text(
            text = summary.title,
            style = TextStyle(
                color = WidgetTitleColor,
                fontWeight = FontWeight.Medium,
                fontSize = 15.sp,
            ),
        )

        if (summary.emptyMessage != null) {
            WidgetTextLine(
                label = "State",
                text = summary.emptyMessage,
                muted = true,
            )
        } else {
            WidgetTextLine(
                label = "Mood",
                text = summary.mood,
            )
            WidgetTextLine(
                label = "Streak",
                text = summary.streak,
            )
            WidgetTextLine(
                label = "Quest",
                text = summary.quest,
            )
            WidgetTextLine(
                label = "Next",
                text = summary.nextAction,
            )
        }

        Text(
            text = WidgetRefreshText(summary),
            modifier = GlanceModifier
                .padding(top = 5.dp)
                .clickable(actionRunCallback<RefreshTodayActionCallback>()),
            style = TextStyle(
                color = WidgetActionColor,
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp,
            ),
        )
    }
}

@Composable
private fun WidgetTextLine(
    label: String,
    text: String?,
    muted: Boolean = false,
) {
    if (text == null) return

    Text(
        text = "$label: $text",
        modifier = GlanceModifier.padding(top = 3.dp),
        style = TextStyle(
            color = if (muted) WidgetMutedColor else WidgetBodyColor,
            fontSize = 13.sp,
        ),
    )
}

private fun WidgetRefreshText(summary: TodayWidgetSummary): String {
    return "${summary.cacheLabel} • ${summary.refreshLabel.replaceFirstChar(Char::lowercase)}"
}
