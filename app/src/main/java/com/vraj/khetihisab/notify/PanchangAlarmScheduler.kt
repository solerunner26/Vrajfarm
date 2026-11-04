package com.vraj.khetihisab.notify

import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime

/**
 * doc 10 scheduling §1: one daily alarm at the evening time (default 19:00 IST) that posts the
 * "આવતીકાલે …" notification. Exact if allowed, otherwise a 30-min `setWindow`.
 */
class PanchangAlarmScheduler(
    private val port: AlarmPort,
    private val clock: () -> Long = System::currentTimeMillis,
) {
    /** Schedules the next evening alarm, or cancels it when every type is off. Returns the trigger time. */
    fun schedule(settings: PanchangAlertSettings, anyTypeEnabled: Boolean): Long? {
        if (!anyTypeEnabled) {
            cancel()
            return null
        }
        val at = nextTrigger(clock(), settings.eveningHour, settings.eveningMinute)
        port.scheduleBest(AlarmKey.PanchangEvening, at, WINDOW_MS)
        return at
    }

    fun cancel() = port.cancel(AlarmKey.PanchangEvening)

    companion object {
        const val WINDOW_MS = 30 * 60_000L

        /** Next hh:mm IST strictly after [now]. */
        fun nextTrigger(now: Long, hour: Int, minute: Int): Long {
            val nowIst = Instant.ofEpochMilli(now).atZone(ReminderTiming.IST)
            var t = nowIst.toLocalDate().atTime(LocalTime.of(hour, minute)).atZone(ReminderTiming.IST)
            if (!t.toInstant().isAfter(nowIst.toInstant())) t = t.plusDays(1)
            return t.toInstant().toEpochMilli()
        }

        /**
         * Boot catch-up (doc 10 edge case): the phone was off at the evening time. Post the missed
         * notification only when it is now past today's evening time but still before midnight
         * (always true for "today"), and tomorrow's notification has not been posted yet.
         * Returns the target date (tomorrow) or null.
         */
        fun missedTarget(now: Long, hour: Int, minute: Int, lastNotifiedTargetDay: Long?): LocalDate? {
            val nowIst = Instant.ofEpochMilli(now).atZone(ReminderTiming.IST)
            val todayEvening = nowIst.toLocalDate().atTime(LocalTime.of(hour, minute))
            if (nowIst.toLocalDateTime().isBefore(todayEvening)) return null
            val target = nowIst.toLocalDate().plusDays(1)
            return if (lastNotifiedTargetDay == target.toEpochDay()) null else target
        }
    }
}
