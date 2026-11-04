package com.vraj.khetihisab.widget

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.LocalContext
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.provideContent
import androidx.glance.appwidget.updateAll
import androidx.glance.layout.Alignment
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.size
import androidx.glance.layout.width
import com.vraj.khetihisab.R
import com.vraj.khetihisab.notify.NotifyGraph
import com.vraj.khetihisab.notify.ReminderTiming
import com.vraj.khetihisab.notify.VkDeepLinks
import com.vraj.khetihisab.notify.launchAsync
import com.vraj.khetihisab.panchang.GujaratiPanchangHelper
import com.vraj.khetihisab.panchang.PanchangAlertEngine
import com.vraj.khetihisab.panchang.PanchangNames
import com.vraj.khetihisab.panchang.SpecialDay
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.LocalTime

/** What the 2×2 Today widget shows. */
data class TodayWidgetState(
    val dayNumber: String,
    /** "નવેમ્બર · મંગળ". */
    val monthWeekday: String,
    /** "આસો વદ નોમ". */
    val tithi: String,
    /** "આજે દિવાળી", "આવતીકાલે દિવાળી" or "05 · રમા અગિયારસ"; null if none soon. */
    val special: String?,
) {
    companion object {
        /** [fmt] is (resId, arg) → text; [next] is the first special day on/after [today]. */
        fun build(today: LocalDate, tithiGu: String, next: SpecialDay?, fmt: (Int, String) -> String): TodayWidgetState {
            val weekday = PanchangNames.weekday(today.dayOfWeek).removeSuffix("વાર")
            val special = next?.let {
                when (it.date) {
                    today -> fmt(R.string.w_today_special, it.titleGu)
                    today.plusDays(1) -> fmt(R.string.w_tomorrow_special, it.titleGu)
                    else -> it.date.dayOfMonth.toString().padStart(2, '0') + " · " + it.titleGu
                }
            }
            return TodayWidgetState(
                dayNumber = today.dayOfMonth.toString().padStart(2, '0'),
                monthWeekday = "${PanchangNames.gregorianMonths[today.monthValue - 1]} · $weekday",
                tithi = tithiGu,
                special = special,
            )
        }
    }
}

/** doc 13: 2×2 date number (30sp), month · weekday, tithi, and the next special day (doc 10). */
class TodayGlanceWidget : GlanceAppWidget() {
    override val sizeMode: SizeMode = SizeMode.Single

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val colors = WidgetTheme.colors(context)
        val state = withContext(Dispatchers.Default) { load(context) }
        provideContent { Content(colors, state) }
    }

    private suspend fun load(context: Context): TodayWidgetState {
        val today = LocalDate.now(ReminderTiming.IST)
        val day = GujaratiPanchangHelper.day(today)
        val next = runCatching {
            val repos = NotifyGraph.repos(context)
            val settings = NotifyGraph.settings(context).current()
            val custom = withContext(Dispatchers.IO) { repos.panchang.customFestivals() }
            PanchangAlertEngine.nextSpecialDay(today, custom, settings.includeVadBij)
        }.getOrNull()
        return TodayWidgetState.build(today, day.tithiGu, next) { res, arg -> context.getString(res, arg) }
    }

    @Composable
    private fun Content(c: WidgetColors, s: TodayWidgetState) {
        val ctx = LocalContext.current
        val open = actionStartActivity(VkDeepLinks.intent(ctx, VkDeepLinks.PANCHANG))
        WidgetFrame(c) {
            Row(GlanceModifier.fillMaxWidth().clickable(open), verticalAlignment = Alignment.CenterVertically) {
                Image(ImageProvider(R.drawable.ic_vk_panchang), contentDescription = null, modifier = GlanceModifier.size(30.dp))
                Spacer(GlanceModifier.width(8.dp))
                Line(s.dayNumber, c.onSurface, 30.sp, bold = true)
            }
            Spacer(GlanceModifier.height(2.dp))
            Line(s.monthWeekday, c.onSurfaceVariant, 14.sp, modifier = GlanceModifier.clickable(open))
            Line(s.tithi, c.panchang, 15.sp, bold = true, modifier = GlanceModifier.clickable(open))
            if (s.special != null) Line(s.special, c.primary, 14.sp, bold = true, modifier = GlanceModifier.clickable(open))
        }
    }
}

class TodayWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = TodayGlanceWidget()

    override fun onEnabled(context: Context) {
        super.onEnabled(context)
        TodayMidnightReceiver.schedule(context)
    }
}

/** Redraws the Today widget just after midnight IST, then re-arms itself (doc 13). */
class TodayMidnightReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val app = context.applicationContext
        launchAsync("TodayMidnight") {
            TodayGlanceWidget().updateAll(app)
            schedule(app)
        }
    }

    companion object {
        /** Widgets are not time-critical: a 10-min window starting 00:00:30 IST. */
        const val WINDOW_MS = 10 * 60_000L

        fun nextMidnight(now: Long): Long {
            val today = ReminderTiming.istDate(now)
            return ReminderTiming.istMillis(today.plusDays(1).atTime(LocalTime.of(0, 0, 30)))
        }

        fun schedule(context: Context) {
            NotifyGraph.alarmPort(context).setWindow(
                com.vraj.khetihisab.notify.AlarmKey.TodayMidnight,
                nextMidnight(System.currentTimeMillis()),
                WINDOW_MS,
            )
        }
    }
}
