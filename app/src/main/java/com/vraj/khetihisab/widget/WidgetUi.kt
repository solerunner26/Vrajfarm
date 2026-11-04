package com.vraj.khetihisab.widget

import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceModifier
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.action.Action
import androidx.glance.action.clickable
import androidx.glance.appwidget.cornerRadius
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.ColumnScope
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider

/** 24dp rounded card on a solid theme surface (doc 13 common rules). */
@Composable
internal fun WidgetFrame(c: WidgetColors, content: @Composable ColumnScope.() -> Unit) {
    Column(
        GlanceModifier
            .fillMaxSize()
            .cornerRadius(24.dp)
            .background(c.surface)
            .padding(14.dp),
        content = content,
    )
}

/** Header: ic_vk icon + single-line title + optional count badge. */
@Composable
internal fun WidgetHeader(c: WidgetColors, iconRes: Int, title: String, badge: String? = null, onClick: Action? = null) {
    val m = GlanceModifier.fillMaxWidth().let { if (onClick != null) it.clickable(onClick) else it }
    Row(m, verticalAlignment = Alignment.CenterVertically) {
        Image(ImageProvider(iconRes), contentDescription = null, modifier = GlanceModifier.size(32.dp))
        Spacer(GlanceModifier.width(8.dp))
        Text(
            title,
            modifier = GlanceModifier.defaultWeight(),
            style = TextStyle(color = c.onSurface, fontSize = 18.sp, fontWeight = FontWeight.Bold),
            maxLines = 1,
        )
        if (badge != null) Pill(badge, c.primaryContainer, c.onPrimaryContainer)
    }
}

@Composable
internal fun Pill(text: String, bg: ColorProvider, fg: ColorProvider, onClick: Action? = null, minWidth: Dp = 0.dp) {
    val m = GlanceModifier
        .cornerRadius(16.dp)
        .background(bg)
        .padding(horizontal = 12.dp, vertical = 6.dp)
        .let { if (onClick != null) it.clickable(onClick) else it }
    Box(m, contentAlignment = Alignment.Center) {
        Text(text, style = TextStyle(color = fg, fontSize = 14.sp, fontWeight = FontWeight.Bold), maxLines = 1)
    }
}

@Composable
internal fun Line(text: String, color: ColorProvider, size: TextUnit = 15.sp, bold: Boolean = false, modifier: GlanceModifier = GlanceModifier) {
    Text(
        text,
        modifier = modifier,
        style = TextStyle(color = color, fontSize = size, fontWeight = if (bold) FontWeight.Bold else FontWeight.Normal),
        maxLines = 1,
    )
}
