package com.vraj.khetihisab.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.LocalContext
import androidx.glance.action.ActionParameters
import androidx.glance.action.actionParametersOf
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.provideContent
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.width
import com.vraj.khetihisab.R
import com.vraj.khetihisab.data.entity.Priority
import com.vraj.khetihisab.notify.Deadline
import com.vraj.khetihisab.notify.VkDeepLinks
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** doc 13: 4×2 "જરૂરી કામ"; up to 3 reminders (HIGH first) with deadline + a DONE pill. */
class ReminderGlanceWidget : GlanceAppWidget() {
    override val sizeMode: SizeMode = SizeMode.Single

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val colors = WidgetTheme.colors(context)
        val rows = withContext(Dispatchers.IO) {
            runCatching { WidgetLogic.reminderRows(WidgetGraph.source(context).activeReminders()) }.getOrDefault(emptyList())
        }
        val now = System.currentTimeMillis()
        provideContent { Content(colors, rows, now) }
    }

    @Composable
    private fun Content(c: WidgetColors, rows: List<ReminderRow>, now: Long) {
        val ctx = LocalContext.current
        WidgetFrame(c) {
            WidgetHeader(
                c,
                R.drawable.ic_vk_reminder,
                ctx.getString(R.string.w_rem),
                onClick = actionStartActivity(VkDeepLinks.intent(ctx, VkDeepLinks.REMINDERS)),
            )
            Spacer(GlanceModifier.height(4.dp))
            if (rows.isEmpty()) Line(ctx.getString(R.string.w_rem_empty), c.onSurfaceVariant, 16.sp)
            rows.forEach { r ->
                val deadline = Deadline.of(r.dueAt, now)
                val tag = ctx.getString(if (r.priority == Priority.HIGH) R.string.rem_high else R.string.rem_normal)
                val meta = "$tag · ${deadline.label { res, arg -> ctx.getString(res, arg) }}"
                Row(GlanceModifier.fillMaxWidth().padding(vertical = 3.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(GlanceModifier.defaultWeight()) {
                        Line(r.title, c.onSurface, 16.sp, bold = true)
                        Line(meta, if (deadline is Deadline.Overdue) c.error else c.onSurfaceVariant, 13.sp)
                    }
                    Spacer(GlanceModifier.width(8.dp))
                    Pill(
                        ctx.getString(R.string.w_done),
                        c.primary,
                        c.onPrimary,
                        onClick = actionRunCallback<MarkReminderDoneAction>(actionParametersOf(MarkReminderDoneAction.REMINDER_ID to r.id)),
                    )
                }
            }
        }
    }
}

/** DONE pill → archive the reminder, cancel its alarm/notification, redraw. */
class MarkReminderDoneAction : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        val id = parameters[REMINDER_ID] ?: return
        WidgetActions.markReminderDone(WidgetGraph.source(context), id) {
            ReminderGlanceWidget().update(context, glanceId)
        }
    }

    companion object {
        val REMINDER_ID = ActionParameters.Key<Long>("reminder_id")
    }
}

class ReminderWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = ReminderGlanceWidget()
}
