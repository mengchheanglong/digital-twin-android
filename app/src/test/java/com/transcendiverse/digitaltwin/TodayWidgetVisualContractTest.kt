package com.transcendiverse.digitaltwin

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TodayWidgetVisualContractTest {
    @Test
    fun widgetUsesHighContrastSurfaceInsteadOfTransparentDarkText() {
        val widget = source("app/src/main/java/com/transcendiverse/digitaltwin/widget/TodayGlanceWidget.kt")

        assertTrue(widget.contains(".background(WidgetSurfaceColor)"))
        assertTrue(widget.contains("WidgetSurfaceColor = ColorProvider(Color(0xFF111827))"))
        assertTrue(widget.contains("WidgetTitleColor = ColorProvider(Color(0xFFF8FAFC))"))
        assertTrue(widget.contains("WidgetBodyColor = ColorProvider(Color(0xFFE5E7EB))"))
        assertTrue(widget.contains("WidgetMutedColor = ColorProvider(Color(0xFFCBD5E1))"))
        assertTrue(widget.contains("WidgetActionColor = ColorProvider(Color(0xFF5EEAD4))"))
        listOf("Mood", "Streak", "Quest", "Next", "State").forEach { label ->
            assertTrue(widget.contains("label = \"$label\""))
        }
        assertTrue(widget.contains("WidgetRefreshText(summary)"))
        assertTrue(widget.contains("summary.cacheLabel"))
        assertTrue(widget.contains("summary.refreshLabel.replaceFirstChar(Char::lowercase)"))
        assertFalse(widget.contains("ColorProvider(Color(if (muted) 0xFF64748B else 0xFF334155))"))
    }

    private fun source(path: String): String {
        val candidates = listOf(
            File(path),
            File(path.removePrefix("app/")),
        )
        return candidates.firstOrNull(File::isFile)?.readText()
            ?: error("Unable to read $path")
    }
}
