package com.vraj.khetihisab.notify

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import androidx.core.app.NotificationCompat
import com.vraj.khetihisab.R
import com.vraj.khetihisab.data.repo.ReminderRepository
import com.vraj.khetihisab.panchang.AlertPurpose
import com.vraj.khetihisab.panchang.PanchangAlert
import com.vraj.khetihisab.panchang.PanchangAlertEngine
import java.time.LocalDate

/** Evening-before Panchang notification (doc 10 scheduling §2). */
object PanchangNotifier {
    const val NOTIFICATION_ID = 2001

    /** "{tithiGu} · {first doLine or avoidLine}" (guidance part only if enabled). */
    fun shortText(alert: PanchangAlert): String {
        val first = if (alert.showGuidance) (alert.doLines.firstOrNull() ?: alert.avoidLines.firstOrNull()) else null
        return listOfNotNull(alert.special.day.tithiGu, first).joinToString(" · ")
    }

    /** BigTextStyle body: tithi, then "કરવા જેવું" / "ટાળવા જેવું" lines, then the belief note. */
    fun bigText(alert: PanchangAlert, doLabel: String, avoidLabel: String, beliefNote: String): String = buildString {
        append(alert.special.day.tithiGu)
        if (alert.showGuidance) {
            if (alert.doLines.isNotEmpty()) append("\n").append(doLabel).append(": ").append(alert.doLines.joinToString(" · "))
            if (alert.avoidLines.isNotEmpty()) append("\n").append(avoidLabel).append(": ").append(alert.avoidLines.joinToString(" · "))
            append("\n").append(beliefNote)
        }
    }

    suspend fun alertFor(context: Context, date: LocalDate, purpose: AlertPurpose): PanchangAlert? {
        val repos = NotifyGraph.repos(context)
        val settings = NotifyGraph.settings(context).current()
        return PanchangAlertEngine.alertFor(
            date = date,
            prefs = repos.panchang.prefsMapNow(),
            custom = repos.panchang.customFestivals(),
            showGuidance = settings.showGuidance,
            includeVadBij = settings.includeVadBij,
            purpose = purpose,
        )
    }

    /** Posts "આવતીકાલે {titleGu}" for [target] if an alert applies. Records the target day either way. */
    suspend fun notifyFor(context: Context, target: LocalDate): Boolean {
        val alert = alertFor(context, target, AlertPurpose.EVENING_NOTIFICATION)
        NotifyGraph.settings(context).setLastNotifiedTargetDay(target.toEpochDay())
        if (alert == null) return false
        val make = VkNotify.broadcast(
            context,
            CreatePanchangReminderReceiver::class.java,
            CreatePanchangReminderReceiver.ACTION,
            "vraj-action://panchang/reminder/${target.toEpochDay()}",
        ) { putExtra(CreatePanchangReminderReceiver.EXTRA_EPOCH_DAY, target.toEpochDay()) }
        val n = VkNotify.builder(context, NotificationChannels.PANCHANG, VkNotify.accent(context))
            .setContentTitle(context.getString(R.string.nt_tomorrow_title, alert.special.titleGu))
            .setContentText(shortText(alert))
            .setStyle(
                NotificationCompat.BigTextStyle().bigText(
                    bigText(
                        alert,
                        context.getString(R.string.festival_do),
                        context.getString(R.string.festival_avoid),
                        context.getString(R.string.nt_belief_note),
                    ),
                ),
            )
            .setContentIntent(VkDeepLinks.pending(context, VkDeepLinks.panchangDay(target)))
            .addAction(0, context.getString(R.string.nt_make_reminder), make)
            .build()
        return VkNotify.post(context, NOTIFICATION_ID, n)
    }

    /** Re-arms (or cancels) the evening alarm from the current settings + prefs. */
    suspend fun reschedule(context: Context): Long? {
        val settings = NotifyGraph.settings(context).current()
        val prefs = NotifyGraph.repos(context).panchang.prefsMapNow()
        val any = prefs.values.any { it.enabled && it.notifyEveningBefore }
        return NotifyGraph.panchangScheduler(context).schedule(settings, any)
    }
}

/** The daily evening alarm. */
class PanchangAlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val app = context.applicationContext
        launchAsync(TAG) {
            val tomorrow = LocalDate.now(ReminderTiming.IST).plusDays(1)
            PanchangNotifier.notifyFor(app, tomorrow)
            PanchangNotifier.reschedule(app)
        }
    }

    private companion object {
        const val TAG = "PanchangAlarm"
    }
}

/**
 * "રિમાઇન્ડર બનાવો" from the notification (doc 10 scheduling §6). Deduplicated by the repository;
 * shows "રિમાઇન્ડર પહેલેથી છે" when one exists.
 */
class CreatePanchangReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val epochDay = intent.getLongExtra(EXTRA_EPOCH_DAY, Long.MIN_VALUE)
        if (epochDay == Long.MIN_VALUE) return
        val app = context.applicationContext
        launchAsync(TAG) {
            val date = LocalDate.ofEpochDay(epochDay)
            val alert = PanchangNotifier.alertFor(app, date, AlertPurpose.ANY) ?: return@launchAsync
            val reminders = NotifyGraph.repos(app).reminders
            val msg = when (val res = reminders.createPanchangReminder(date, alert.special.titleGu, alert.special.day.tithiGu)) {
                is ReminderRepository.CreateResult.Created -> {
                    reminders.byId(res.id)?.let { NotifyGraph.reminderScheduler(app).schedule(it) }
                    R.string.nt_reminder_created
                }
                is ReminderRepository.CreateResult.AlreadyExists -> R.string.nt_reminder_exists
            }
            VkNotify.cancel(app, PanchangNotifier.NOTIFICATION_ID)
            Handler(Looper.getMainLooper()).post { Toast.makeText(app, msg, Toast.LENGTH_SHORT).show() }
        }
    }

    companion object {
        private const val TAG = "PanchangReminder"
        const val ACTION = "com.vraj.khetihisab.action.PANCHANG_REMINDER"
        const val EXTRA_EPOCH_DAY = "epochDay"
    }
}
