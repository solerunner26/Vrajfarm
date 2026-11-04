package com.vraj.khetihisab.reminders

import com.vraj.khetihisab.data.entity.Priority
import com.vraj.khetihisab.data.entity.ReminderEntity
import com.vraj.khetihisab.notify.ReminderTiming
import com.vraj.khetihisab.ui.reminders.ReminderDraftLogic
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

/** Due-time maths of the add/edit sheet (always Asia/Kolkata, regardless of the JVM zone). */
class ReminderDraftLogicTest {

    @Test fun dueAt_isIstWallClock() {
        val d = ReminderDraftLogic.newDraft(Instant.parse("2026-11-04T04:30:00Z").toEpochMilli()) // 10:00 IST
            .copy(date = LocalDate.of(2026, 11, 5), time = LocalTime.of(9, 0))
        // 05-11-2026 09:00 IST = 03:30 UTC
        assertEquals(Instant.parse("2026-11-05T03:30:00Z").toEpochMilli(), ReminderDraftLogic.dueAt(d))
    }

    @Test fun newDraft_tomorrowIsIstTomorrow() {
        // 04-11-2026 20:00 UTC is already 05-11 01:30 IST → default date is 06-11.
        val d = ReminderDraftLogic.newDraft(Instant.parse("2026-11-04T20:00:00Z").toEpochMilli())
        assertEquals(LocalDate.of(2026, 11, 6), d.date)
        assertEquals(LocalTime.of(9, 0), d.time)
        assertEquals(Priority.NORMAL, d.priority)
    }

    @Test fun isPast() {
        val now = ReminderTiming.istMillis(LocalDateTime.of(2026, 11, 4, 10, 0))
        val base = ReminderDraftLogic.newDraft(now)
        assertTrue(ReminderDraftLogic.isPast(base.copy(date = LocalDate.of(2026, 11, 4), time = LocalTime.of(9, 59)), now))
        assertTrue(ReminderDraftLogic.isPast(base.copy(date = LocalDate.of(2026, 11, 4), time = LocalTime.of(10, 0)), now))
        assertFalse(ReminderDraftLogic.isPast(base.copy(date = LocalDate.of(2026, 11, 4), time = LocalTime.of(10, 1)), now))
    }

    @Test fun roundTrip_entityToDraftToEntity() {
        val due = ReminderTiming.istMillis(LocalDateTime.of(2026, 11, 15, 17, 30))
        val r = ReminderEntity(id = 9, title = "CCTV", note = "", imagePath = null, priority = Priority.HIGH, dueAt = due, sourceType = "LOAN", sourceId = 4, createdAt = 1, updatedAt = 1)
        val d = ReminderDraftLogic.fromEntity(r)
        assertEquals(LocalDate.of(2026, 11, 15), d.date)
        assertEquals(LocalTime.of(17, 30), d.time)
        val back = ReminderDraftLogic.toEntity(d, now = 50)
        assertEquals(r.copy(note = null, updatedAt = 50), back)
        assertNull(back.note)
    }
}
