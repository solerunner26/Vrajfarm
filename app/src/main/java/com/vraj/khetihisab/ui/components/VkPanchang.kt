package com.vraj.khetihisab.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.vraj.khetihisab.R
import com.vraj.khetihisab.panchang.PanchangAlert
import com.vraj.khetihisab.panchang.PanchangAlertType
import com.vraj.khetihisab.panchang.PanchangDay
import com.vraj.khetihisab.ui.decor.VkLeafDivider
import com.vraj.khetihisab.ui.decor.VkMoonPhase
import com.vraj.khetihisab.ui.decor.VkRangoliDots
import com.vraj.khetihisab.ui.theme.VkIcon
import com.vraj.khetihisab.ui.theme.VkIconImage
import java.time.format.DateTimeFormatter

private val HHMM: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm")

/** Today's Panchang on Home (tertiaryContainer, moon-phase glyph, rangoli dots). */
@Composable
fun VkPanchangCard(day: PanchangDay, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val cs = MaterialTheme.colorScheme
    Box(modifier.fillMaxWidth()) {
        VkCard(Modifier.fillMaxWidth(), tone = VkTone.Panchang, onClick = onClick) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                VkMoonPhase(day.moonIllumination, day.waxing, size = 64.dp)
                Spacer(Modifier.width(14.dp))
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(stringResource(R.string.panchang_today), style = MaterialTheme.typography.labelMedium, color = cs.onTertiaryContainer.copy(alpha = 0.85f), maxLines = 1)
                    Text(day.tithiGu, style = MaterialTheme.typography.headlineSmall, color = cs.onTertiaryContainer, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(
                        "${day.weekdayGu} · ${day.vsLabel} · " + stringResource(R.string.panchang_sunrise, day.sunrise.format(HHMM)),
                        style = MaterialTheme.typography.bodySmall,
                        color = cs.onTertiaryContainer.copy(alpha = 0.85f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
        VkRangoliDots(Modifier.align(Alignment.TopEnd).padding(10.dp), size = 18.dp)
    }
}

@Composable
private fun alertTypeLabel(t: PanchangAlertType): String = stringResource(
    when (t) {
        PanchangAlertType.FESTIVAL -> R.string.alert_type_festival
        PanchangAlertType.AGIYARAS -> R.string.alert_type_agiyaras
        PanchangAlertType.AMAS -> R.string.alert_type_amas
        PanchangAlertType.POONAM -> R.string.alert_type_poonam
        PanchangAlertType.BIJ -> R.string.alert_type_bij
    },
)

/**
 * Panchang / festival dialog (doc 10, artboard 07). Actions are always stacked full width
 * (filled → tonal → text, doc 17 #7). Guidance is framed "માન્યતા પ્રમાણે".
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun VkFestivalDialog(
    alert: PanchangAlert,
    onDismiss: () -> Unit,
    onCreateReminder: () -> Unit,
    onOpenCalendar: () -> Unit,
    isTomorrow: Boolean = false,
) {
    val cs = MaterialTheme.colorScheme
    val special = alert.special
    val day = special.day
    BasicAlertDialog(onDismissRequest = onDismiss) {
        Surface(shape = RoundedCornerShape(28.dp), color = cs.surfaceContainerLowest, contentColor = cs.onSurface) {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                // 1. Band
                Box(Modifier.fillMaxWidth().heightIn(min = 120.dp).background(cs.tertiaryContainer)) {
                    VkRangoliDots(Modifier.align(Alignment.TopEnd).padding(12.dp))
                    VkRangoliDots(Modifier.align(Alignment.BottomStart).padding(8.dp), size = 16.dp)
                    Row(Modifier.padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
                        if (PanchangAlertType.FESTIVAL in special.types) {
                            VkIconImage(VkIcon.DIYA, size = 56.dp)
                        } else {
                            VkMoonPhase(day.moonIllumination, day.waxing, size = 72.dp)
                        }
                        Spacer(Modifier.width(14.dp))
                        Column(Modifier.weight(1f)) {
                            Text(
                                stringResource(if (isTomorrow) R.string.festival_tomorrow else R.string.festival_today),
                                style = MaterialTheme.typography.labelLarge, color = cs.onTertiaryContainer, maxLines = 1,
                            )
                            Text(special.titleGu, style = MaterialTheme.typography.headlineSmall, color = cs.onTertiaryContainer, maxLines = 2, overflow = TextOverflow.Ellipsis)
                            Text("${day.tithiGu} · ${day.vsLabel}", style = MaterialTheme.typography.bodySmall, color = cs.onTertiaryContainer, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                    }
                }
                Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    // 2. Type chips (neutral, outlined)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        special.types.sortedBy { it.ordinal }.forEach { t ->
                            AssistChip(
                                onClick = {},
                                label = { Text(alertTypeLabel(t), maxLines = 1, softWrap = false) },
                                colors = AssistChipDefaults.assistChipColors(labelColor = cs.onSurface),
                            )
                        }
                    }
                    // 3. Guidance
                    if (alert.showGuidance) {
                        GuidanceList(stringResource(R.string.festival_do), alert.doLines.take(4), avoid = false)
                        if (alert.avoidLines.isNotEmpty()) {
                            GuidanceList(stringResource(R.string.festival_avoid), alert.avoidLines.take(4), avoid = true)
                        }
                        Text(stringResource(R.string.panchang_belief_footer), style = MaterialTheme.typography.bodySmall, color = cs.onSurfaceVariant)
                    }
                    // 4. Actions, stacked
                    VkPrimaryButton(stringResource(R.string.festival_ok), onDismiss, Modifier.fillMaxWidth())
                    VkTonalButton(stringResource(R.string.festival_create_reminder), onCreateReminder, Modifier.fillMaxWidth())
                    VkTextButton(stringResource(R.string.festival_open_calendar), onOpenCalendar, Modifier.fillMaxWidth())
                }
            }
        }
    }
}

@Composable
private fun GuidanceList(title: String, lines: List<String>, avoid: Boolean) {
    val cs = MaterialTheme.colorScheme
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(title, style = MaterialTheme.typography.titleMedium, color = if (avoid) cs.error else cs.primary)
        lines.forEach { line ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (avoid) {
                    Canvas(Modifier.size(width = 24.dp, height = 14.dp)) {
                        drawCircle(cs.error, radius = size.height / 2 - 1.dp.toPx(), style = Stroke(1.5.dp.toPx()))
                    }
                } else {
                    VkLeafDivider()
                }
                Spacer(Modifier.width(8.dp))
                Text(line, style = MaterialTheme.typography.bodyLarge, color = cs.onSurface)
            }
        }
    }
}
