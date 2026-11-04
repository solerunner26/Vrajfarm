package com.vraj.khetihisab.notify

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import com.vraj.khetihisab.widget.TodayMidnightReceiver

/** What an alarm is for. Each key maps to one unique PendingIntent (by data URI). */
sealed interface AlarmKey {
    /** [slot]: [ReminderTiming.SLOT_EARLY], [ReminderTiming.SLOT_DUE] or [ReminderTiming.SLOT_SNOOZE]. */
    data class Reminder(val id: Long, val slot: Int) : AlarmKey
    data object PanchangEvening : AlarmKey
    data object TodayMidnight : AlarmKey
}

enum class AlarmMode { EXACT, WINDOW }

/** The AlarmManager seam: schedulers are pure Kotlin on top of it (unit-tested with a fake). */
interface AlarmPort {
    /** `Build.VERSION.SDK_INT < 31 || AlarmManager.canScheduleExactAlarms()`. */
    fun canScheduleExact(): Boolean

    fun setExact(key: AlarmKey, atMillis: Long)

    fun setWindow(key: AlarmKey, atMillis: Long, windowMillis: Long)

    fun cancel(key: AlarmKey)
}

/** Exact when allowed, else a `setWindow` of [windowMillis]. Returns the mode used. */
fun AlarmPort.scheduleBest(key: AlarmKey, atMillis: Long, windowMillis: Long): AlarmMode =
    if (canScheduleExact()) {
        setExact(key, atMillis)
        AlarmMode.EXACT
    } else {
        setWindow(key, atMillis, windowMillis)
        AlarmMode.WINDOW
    }

/** Real AlarmManager. `setExactAndAllowWhileIdle(RTC_WAKEUP)` or `setWindow(RTC_WAKEUP)`. */
class AndroidAlarmPort(private val context: Context) : AlarmPort {
    private val am: AlarmManager get() = context.getSystemService(AlarmManager::class.java)

    override fun canScheduleExact(): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.S || am.canScheduleExactAlarms()

    override fun setExact(key: AlarmKey, atMillis: Long) {
        try {
            am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, atMillis, pending(key))
        } catch (_: SecurityException) {
            // Permission revoked between the check and the call: degrade to a window.
            setWindow(key, atMillis, ReminderTiming.WINDOW_MS)
        }
    }

    override fun setWindow(key: AlarmKey, atMillis: Long, windowMillis: Long) {
        am.setWindow(AlarmManager.RTC_WAKEUP, atMillis, windowMillis, pending(key))
    }

    override fun cancel(key: AlarmKey) {
        am.cancel(pending(key))
    }

    private fun pending(key: AlarmKey): PendingIntent =
        PendingIntent.getBroadcast(
            context,
            0,
            intentFor(context, key),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )

    companion object {
        const val ACTION_REMINDER_ALARM = "com.vraj.khetihisab.action.REMINDER_ALARM"
        const val ACTION_PANCHANG_ALARM = "com.vraj.khetihisab.action.PANCHANG_ALARM"
        const val ACTION_TODAY_MIDNIGHT = "com.vraj.khetihisab.action.TODAY_MIDNIGHT"
        const val EXTRA_ID = "id"
        const val EXTRA_SLOT = "slot"

        fun intentFor(context: Context, key: AlarmKey): Intent = when (key) {
            is AlarmKey.Reminder -> Intent(context, ReminderReceiver::class.java)
                .setAction(ACTION_REMINDER_ALARM)
                .setData(Uri.parse("vraj-alarm://reminder/${key.id}/${key.slot}"))
                .putExtra(EXTRA_ID, key.id)
                .putExtra(EXTRA_SLOT, key.slot)
            AlarmKey.PanchangEvening -> Intent(context, PanchangAlarmReceiver::class.java)
                .setAction(ACTION_PANCHANG_ALARM)
                .setData(Uri.parse("vraj-alarm://panchang/evening"))
            AlarmKey.TodayMidnight -> Intent(context, TodayMidnightReceiver::class.java)
                .setAction(ACTION_TODAY_MIDNIGHT)
                .setData(Uri.parse("vraj-alarm://widget/today-midnight"))
        }
    }
}
