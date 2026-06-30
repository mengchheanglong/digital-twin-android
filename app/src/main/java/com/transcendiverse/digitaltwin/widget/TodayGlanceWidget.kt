package com.transcendiverse.digitaltwin.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.action.ActionParameters
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

@Composable
private fun TodayWidgetContent(summary: TodayWidgetSummary) {
    Column(
        modifier = GlanceModifier
            .fillMaxSize()
            .clickable(actionStartActivity<MainActivity>())
            .padding(14.dp),
    ) {
        Text(
            text = summary.title,
            style = TextStyle(
                color = ColorProvider(Color(0xFF0F172A)),
                fontWeight = FontWeight.Bold,
            ),
        )
        if (summary.emptyMessage != null) {
            Text(
                text = summary.emptyMessage,
                style = TextStyle(color = ColorProvider(Color(0xFF475569))),
            )
        } else {
            WidgetLine(summary.mood)
            WidgetLine(summary.streak)
            WidgetLine(summary.quest)
            WidgetLine(summary.nextAction)
            WidgetLine(summary.cacheLabel, muted = true)
        }
        Text(
            text = summary.refreshLabel,
            modifier = GlanceModifier.clickable(actionRunCallback<RefreshTodayActionCallback>()),
            style = TextStyle(
                color = ColorProvider(Color(0xFF0F766E)),
                fontWeight = FontWeight.Bold,
            ),
        )
    }
}

@Composable
private fun WidgetLine(text: String?, muted: Boolean = false) {
    if (text != null) {
        Text(
            text = text,
            style = TextStyle(
                color = ColorProvider(Color(if (muted) 0xFF64748B else 0xFF334155)),
            ),
        )
    }
}
