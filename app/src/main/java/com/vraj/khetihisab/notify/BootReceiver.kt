package com.vraj.khetihisab.notify

import android.app.AlarmManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.vraj.khetihisab.widget.TodayMidnightReceiver
import com.vraj.khetihisab.widget.WidgetRefresher

/**
 * Re-arms every alarm after BOOT_COMPLETED, TIME_SET, TIMEZONE_CHANGED, MY_PACKAGE_REPLACED and
 * SCHEDULE_EXACT_ALARM_PERMISSION_STATE_CHANGED (docs 10 + 13). After a boot it also posts a missed
 * Panchang evening notification if it is still the same evening.
 */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return
        if (action !in HANDLED) return
        val app = context.applicationContext
        launchAsync(TAG) { AlarmRescheduler.rescheduleEverything(app, bootCatchUp = action == Intent.ACTION_BOOT_COMPLETED) }
    }

    companion object {
        private const val TAG = "VkBoot"
        val HANDLED = setOf(
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_TIME_CHANGED, // "android.intent.action.TIME_SET"
            Intent.ACTION_TIMEZONE_CHANGED,
            Intent.ACTION_MY_PACKAGE_REPLACED,
            AlarmManager.ACTION_SCHEDULE_EXACT_ALARM_PERMISSION_STATE_CHANGED,
        )
    }
}

object AlarmRescheduler {
    /** Also call from `Application.onCreate` (cheap, idempotent) and after Panchang settings change. */
    suspend fun rescheduleEverything(context: Context, bootCatchUp: Boolean = false) {
        val repos = NotifyGraph.repos(context)
        NotifyGraph.reminderScheduler(context).rescheduleAll(repos.reminders.activeNow())
        PanchangNotifier.reschedule(context)
        TodayMidnightReceiver.schedule(context)
        if (bootCatchUp) {
            val store = NotifyGraph.settings(context)
            val s = store.current()
            PanchangAlarmScheduler.missedTarget(System.currentTimeMillis(), s.eveningHour, s.eveningMinute, store.lastNotifiedTargetDay())
                ?.let { PanchangNotifier.notifyFor(context, it) }
        }
        WidgetRefresher.get(context).refreshNow()
    }
}
