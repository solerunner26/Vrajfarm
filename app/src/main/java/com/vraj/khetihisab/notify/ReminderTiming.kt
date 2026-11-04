package com.vraj.khetihisab.notify

import com.vraj.khetihisab.data.entity.Priority
import com.vraj.khetihisab.data.entity.ReminderEntity
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

/** Pure scheduling maths for reminders (doc 13) and deadline labels. */
object ReminderTiming {
    val IST: ZoneId = ZoneId.of("Asia/Kolkata")

    const val SLOT_EARLY = 0
    const val SLOT_DUE = 1
    const val SLOT_SNOOZE = 2
    val ALL_SLOTS = listOf(SLOT_EARLY, SLOT_DUE, SLOT_SNOOZE)

    /** HIGH reminders also ring 1 h early. */
    const val HIGH_EARLY_MS = 60 * 60_000L
    const val SNOOZE_MS = 60 * 60_000L

    /** setWindow fallback length for reminders. */
    const val WINDOW_MS = 15 * 60_000L

    val DATE: DateTimeFormatter = DateTimeFormatter.ofPattern("dd-MM-yyyy")
    val TIME: DateTimeFormatter = DateTimeFormatter.ofPattern("h:mm a", Locale.ENGLISH)

    /** (slot, triggerAt) pairs still in the future. Done / deleted reminders get none. */
    fun triggers(r: ReminderEntity, now: Long): List<Pair<Int, Long>> {
        if (r.isDone || r.isDeleted) return emptyList()
        val out = ArrayList<Pair<Int, Long>>(2)
        if (r.priority == Priority.HIGH) {
            val early = r.dueAt - HIGH_EARLY_MS
            if (early > now) out += SLOT_EARLY to early
        }
        if (r.dueAt > now) out += SLOT_DUE to r.dueAt
        return out
    }

    fun istDate(millis: Long): LocalDate = Instant.ofEpochMilli(millis).atZone(IST).toLocalDate()

    fun istDateTime(millis: Long): LocalDateTime = Instant.ofEpochMilli(millis).atZone(IST).toLocalDateTime()

    fun istMillis(dateTime: LocalDateTime): Long = dateTime.atZone(IST).toInstant().toEpochMilli()

    fun dateText(millis: Long): String = istDate(millis).format(DATE)

    fun timeText(millis: Long): String = istDateTime(millis).format(TIME)
}

/** Deadline wording on cards, notifications and the widget (doc 13 screen §3). */
sealed interface Deadline {
    val dueAt: Long

    /** "મુદત વીતી · 30-10-2026" (error colour). */
    data class Overdue(override val dueAt: Long) : Deadline

    /** "આજે 5:00 PM". */
    data class Today(override val dueAt: Long) : Deadline

    /** "આવતીકાલે · 04-11-2026". */
    data class Tomorrow(override val dueAt: Long) : Deadline

    /** "15-11-2026". */
    data class Later(override val dueAt: Long) : Deadline

    /** Gujarati label. [fmt] is `Context.getString`-like: (resId, arg) → text. */
    fun label(fmt: (Int, String) -> String): String = when (this) {
        is Overdue -> fmt(com.vraj.khetihisab.R.string.rem_overdue, ReminderTiming.dateText(dueAt))
        is Today -> fmt(com.vraj.khetihisab.R.string.rem_today, ReminderTiming.timeText(dueAt))
        is Tomorrow -> fmt(com.vraj.khetihisab.R.string.rem_tomorrow, ReminderTiming.dateText(dueAt))
        is Later -> ReminderTiming.dateText(dueAt)
    }

    companion object {
        fun of(dueAt: Long, now: Long): Deadline {
            if (dueAt < now) return Overdue(dueAt)
            val today = ReminderTiming.istDate(now)
            return when (ReminderTiming.istDate(dueAt)) {
                today -> Today(dueAt)
                today.plusDays(1) -> Tomorrow(dueAt)
                else -> Later(dueAt)
            }
        }
    }
}
