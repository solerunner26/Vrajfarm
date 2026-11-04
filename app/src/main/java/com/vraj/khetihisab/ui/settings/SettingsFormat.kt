package com.vraj.khetihisab.ui.settings

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

/** Pure formatting for the settings package (JVM-testable). All times are IST. */
object SettingsFormat {
    val IST: ZoneId = ZoneId.of("Asia/Kolkata")
    private val HM12: DateTimeFormatter = DateTimeFormatter.ofPattern("h:mm a", Locale.ENGLISH)
    private val DM: DateTimeFormatter = DateTimeFormatter.ofPattern("dd-MM")

    /** 19:00 → "7:00 PM" (canvas badge). */
    fun clock12(hour: Int, minute: Int): String =
        java.time.LocalTime.of(hour.coerceIn(0, 23), minute.coerceIn(0, 59)).format(HM12)

    /**
     * Backup times on the Settings card: "02-11 · 11:45 PM", or "[todayWord] · 11:45 PM" /
     * "[tomorrowWord] · …" relative to [today].
     */
    fun backupWhen(epochMillis: Long, today: LocalDate, todayWord: String, tomorrowWord: String): String {
        val z: ZonedDateTime = Instant.ofEpochMilli(epochMillis).atZone(IST)
        val d = z.toLocalDate()
        val day = when (d) {
            today -> todayWord
            today.plusDays(1) -> tomorrowWord
            else -> d.format(DM)
        }
        return "$day · ${z.format(HM12)}"
    }

    /** Parses a typed Vigha value ("12", "7.5"); null when blank or not a number. */
    fun parseVigha(text: String): Double? = text.trim().replace(',', '.').takeIf { it.isNotEmpty() }?.toDoubleOrNull()

    /** Vigha ok when 0 ≤ v ≤ 500 (doc 04 §6). */
    fun vighaValid(v: Double?): Boolean = v != null && !v.isNaN() && v >= 0.0 && v <= 500.0

    /** 12.0 → "12", 7.5 → "7.5" (edit field prefill). */
    fun vighaText(v: Double): String =
        if (v == Math.floor(v)) v.toLong().toString() else v.toBigDecimal().stripTrailingZeros().toPlainString()
}
