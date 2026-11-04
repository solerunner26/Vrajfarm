package com.vraj.khetihisab.notify

import com.vraj.khetihisab.data.entity.Priority
import com.vraj.khetihisab.data.entity.ReminderEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** doc 13 ReminderSchedulerTest, on a fake [AlarmPort] instead of ShadowAlarmManager. */
class ReminderSchedulerTest {
    private val now = 1_800_000_000_000L
    private val hour = 60 * 60_000L

    private class FakePort(var exact: Boolean = true) : AlarmPort {
        val armed = mutableMapOf<AlarmKey, Pair<Long, Long?>>() // at, window (null = exact)
        val cancelled = mutableListOf<AlarmKey>()
        override fun canScheduleExact() = exact
        override fun setExact(key: AlarmKey, atMillis: Long) {
            armed[key] = atMillis to null
        }
        override fun setWindow(key: AlarmKey, atMillis: Long, windowMillis: Long) {
            armed[key] = atMillis to windowMillis
        }
        override fun cancel(key: AlarmKey) {
            cancelled += key
            armed.remove(key)
        }
    }

    private fun rem(priority: Priority, dueAt: Long, done: Boolean = false) =
        ReminderEntity(id = 42, title = "t", note = null, imagePath = null, priority = priority, dueAt = dueAt, isDone = done, createdAt = 0, updatedAt = 0)

    @Test fun high_ringsOneHourEarlyAndAtDue_exact() {
        val port = FakePort()
        val out = ReminderScheduler(port) { now }.schedule(rem(Priority.HIGH, now + 3 * hour))
        assertEquals(listOf(now + 2 * hour, now + 3 * hour), out.map { it.atMillis })
        assertTrue(out.all { it.mode == AlarmMode.EXACT })
        assertEquals(now + 2 * hour to null, port.armed[AlarmKey.Reminder(42, ReminderTiming.SLOT_EARLY)])
        assertEquals(now + 3 * hour to null, port.armed[AlarmKey.Reminder(42, ReminderTiming.SLOT_DUE)])
    }

    @Test fun normal_ringsOnlyAtDue() {
        val port = FakePort()
        val out = ReminderScheduler(port) { now }.schedule(rem(Priority.NORMAL, now + 3 * hour))
        assertEquals(listOf(ReminderTiming.SLOT_DUE), out.map { it.key.slot })
    }

    @Test fun windowFallback_whenExactNotAllowed() {
        val port = FakePort(exact = false)
        val out = ReminderScheduler(port) { now }.schedule(rem(Priority.HIGH, now + 3 * hour))
        assertTrue(out.all { it.mode == AlarmMode.WINDOW })
        assertEquals(now + 3 * hour to ReminderTiming.WINDOW_MS, port.armed[AlarmKey.Reminder(42, ReminderTiming.SLOT_DUE)])
    }

    @Test fun high_dueWithinTheHour_skipsEarlySlot() {
        val out = ReminderScheduler(FakePort()) { now }.schedule(rem(Priority.HIGH, now + 30 * 60_000L))
        assertEquals(listOf(ReminderTiming.SLOT_DUE), out.map { it.key.slot })
    }

    @Test fun pastDue_orDone_armsNothing() {
        val s = ReminderScheduler(FakePort()) { now }
        assertTrue(s.schedule(rem(Priority.HIGH, now - 1)).isEmpty())
        assertTrue(s.schedule(rem(Priority.NORMAL, now + hour, done = true)).isEmpty())
    }

    @Test fun cancel_removesAllSlotsIncludingSnooze() {
        val port = FakePort()
        val s = ReminderScheduler(port) { now }
        s.schedule(rem(Priority.HIGH, now + 3 * hour))
        s.snooze(42)
        assertEquals(3, port.armed.size)
        s.cancel(42)
        assertTrue(port.armed.isEmpty())
    }

    @Test fun snooze_isNowPlusOneHour() {
        val out = ReminderScheduler(FakePort()) { now }.snooze(42)
        assertEquals(now + hour, out.atMillis)
        assertEquals(ReminderTiming.SLOT_SNOOZE, out.key.slot)
    }

    @Test fun reschedule_replacesOldDueTime() {
        val port = FakePort()
        val s = ReminderScheduler(port) { now }
        s.schedule(rem(Priority.NORMAL, now + 3 * hour))
        s.schedule(rem(Priority.NORMAL, now + 5 * hour))
        assertEquals(now + 5 * hour to null, port.armed[AlarmKey.Reminder(42, ReminderTiming.SLOT_DUE)])
        assertEquals(1, port.armed.size)
    }

    @Test fun deadlineKinds() {
        val istNoon = ReminderTiming.istMillis(java.time.LocalDateTime.of(2026, 11, 4, 12, 0))
        assertTrue(Deadline.of(istNoon - 1, istNoon) is Deadline.Overdue)
        assertTrue(Deadline.of(istNoon + hour, istNoon) is Deadline.Today)
        assertTrue(Deadline.of(istNoon + 20 * hour, istNoon) is Deadline.Tomorrow)
        assertTrue(Deadline.of(istNoon + 48 * hour, istNoon) is Deadline.Later)
    }
}
