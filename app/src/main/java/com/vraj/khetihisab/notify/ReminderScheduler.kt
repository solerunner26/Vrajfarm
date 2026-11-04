package com.vraj.khetihisab.notify

import com.vraj.khetihisab.data.entity.ReminderEntity

/**
 * doc 13 `notify/ReminderScheduler`: exact alarm when `canScheduleExactAlarms()`, otherwise a 15-min
 * `setWindow`. HIGH reminders ring 1 h early and again at the due time. Past due → no alarm.
 */
class ReminderScheduler(
    private val port: AlarmPort,
    private val clock: () -> Long = System::currentTimeMillis,
) {
    /** Cancels old alarms of [r] and schedules its future ones. Returns what was scheduled. */
    fun schedule(r: ReminderEntity): List<Scheduled> {
        cancelDue(r.id)
        return ReminderTiming.triggers(r, clock()).map { (slot, at) ->
            val key = AlarmKey.Reminder(r.id, slot)
            Scheduled(key, at, port.scheduleBest(key, at, ReminderTiming.WINDOW_MS))
        }
    }

    /** Edit / delete / done: removes every alarm of reminder [id], including a snooze. */
    fun cancel(id: Long) {
        ReminderTiming.ALL_SLOTS.forEach { port.cancel(AlarmKey.Reminder(id, it)) }
    }

    /** "1 કલાક પછી": one alarm at now + 1 h. */
    fun snooze(id: Long): Scheduled {
        val at = clock() + ReminderTiming.SNOOZE_MS
        val key = AlarmKey.Reminder(id, ReminderTiming.SLOT_SNOOZE)
        return Scheduled(key, at, port.scheduleBest(key, at, ReminderTiming.WINDOW_MS))
    }

    /** Boot / time change / data change: re-arms every active reminder (idempotent). */
    fun rescheduleAll(active: List<ReminderEntity>): Int = active.sumOf { schedule(it).size }

    private fun cancelDue(id: Long) {
        port.cancel(AlarmKey.Reminder(id, ReminderTiming.SLOT_EARLY))
        port.cancel(AlarmKey.Reminder(id, ReminderTiming.SLOT_DUE))
    }

    data class Scheduled(val key: AlarmKey.Reminder, val atMillis: Long, val mode: AlarmMode)
}
