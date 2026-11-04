package com.vraj.khetihisab.ui.panchang

import com.vraj.khetihisab.data.entity.CustomFestivalEntity
import com.vraj.khetihisab.panchang.Paksha
import com.vraj.khetihisab.panchang.PanchangAlertType
import com.vraj.khetihisab.panchang.PanchangDay
import com.vraj.khetihisab.panchang.PanchangNames
import com.vraj.khetihisab.panchang.SpecialDay
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

/** Compact tithi label under the date; [n] is 1..15 within the paksha (formatted with resources). */
data class ShortTithi(val kind: Kind, val n: Int) {
    enum class Kind { SUD, VAD, POONAM, AMAS }
}

/** Part of day for "તિથિ સમાપ્તિ: 06-11 સવારે 04:12" (labels come from resources). */
enum class DayPeriod { MORNING, AFTERNOON, EVENING, NIGHT }

/** Calendar markers, distinguished by **shape** (doc 10 acceptance: not colour-only). */
enum class DayMarker { FESTIVAL, POONAM, AMAS, AGIYARAS, BIJ }

data class CalendarCell(
    val date: LocalDate,
    val day: PanchangDay?,
    val shortTithi: ShortTithi?,
    val markers: List<DayMarker>,
    val special: SpecialDay?,
    val isToday: Boolean,
) {
    val isSpecial: Boolean get() = special != null
}

data class CalendarMonth(
    val month: YearMonth,
    /** Empty cells before the 1st; the week starts on Sunday. */
    val leadingBlanks: Int,
    val cells: List<CalendarCell>,
    /** Both lunar months + VS year(s), e.g. Aso / Kartak · VS 2082–83 (from engine names). */
    val lunarSubtitle: String,
    val specialDays: List<SpecialDay>,
) {
    /** Rows of 7 (null = blank), for the grid. */
    val weeks: List<List<CalendarCell?>>
        get() {
            val all: List<CalendarCell?> = List(leadingBlanks) { null } + cells
            val padded = all + List((7 - all.size % 7) % 7) { null }
            return padded.chunked(7)
        }
}

/**
 * Pure month builder for PanchangCalendarScreen (artboard 08). Special days come from
 * [PanchangSource] → PanchangAlertEngine, so the calendar and the alerts always agree.
 */
object PanchangCalendarModel {

    private val DM: DateTimeFormatter = DateTimeFormatter.ofPattern("dd-MM")
    private val HM12: DateTimeFormatter = DateTimeFormatter.ofPattern("hh:mm")
    private val HM24: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm")

    /** Sunday-first column of [date] (0 = Sunday … 6 = Saturday). */
    fun column(date: LocalDate): Int = date.dayOfWeek.value % 7

    fun build(
        month: YearMonth,
        today: LocalDate,
        custom: List<CustomFestivalEntity> = emptyList(),
        includeVadBij: Boolean = false,
        special: (LocalDate) -> SpecialDay? = { PanchangSource.special(it, custom, includeVadBij) },
        dayOf: (LocalDate) -> PanchangDay? = { runCatching { PanchangSource.day(it) }.getOrNull() },
    ): CalendarMonth {
        val cells = (1..month.lengthOfMonth()).map { n ->
            val date = month.atDay(n)
            val d = dayOf(date)
            val sp = special(date)
            CalendarCell(
                date = date,
                day = d,
                shortTithi = d?.let(::shortTithi),
                markers = sp?.let(::markersOf).orEmpty(),
                special = sp,
                isToday = date == today,
            )
        }
        return CalendarMonth(
            month = month,
            leadingBlanks = column(month.atDay(1)),
            cells = cells,
            lunarSubtitle = lunarSubtitle(cells.mapNotNull { it.day }),
            specialDays = cells.mapNotNull { it.special },
        )
    }

    /** Marker order: festival, poonam, amas, agiyaras, bij. */
    fun markersOf(s: SpecialDay): List<DayMarker> = buildList {
        if (PanchangAlertType.FESTIVAL in s.types) add(DayMarker.FESTIVAL)
        if (PanchangAlertType.POONAM in s.types) add(DayMarker.POONAM)
        if (PanchangAlertType.AMAS in s.types) add(DayMarker.AMAS)
        if (PanchangAlertType.AGIYARAS in s.types) add(DayMarker.AGIYARAS)
        if (PanchangAlertType.BIJ in s.types) add(DayMarker.BIJ)
    }

    fun shortTithi(d: PanchangDay): ShortTithi = when {
        d.isAmas -> ShortTithi(ShortTithi.Kind.AMAS, 15)
        d.isPoonam -> ShortTithi(ShortTithi.Kind.POONAM, 15)
        d.paksha == Paksha.SUD -> ShortTithi(ShortTithi.Kind.SUD, d.tithiInPaksha)
        else -> ShortTithi(ShortTithi.Kind.VAD, d.tithiInPaksha)
    }

    /** Both lunar months overlapping the Gregorian month, plus the VS year(s): "A / B · <vs> 2082–83". */
    fun lunarSubtitle(days: List<PanchangDay>): String {
        if (days.isEmpty()) return ""
        val months = days.map { it.monthGu }.distinct()
        val years = days.map { it.vikramSamvat }.distinct().sorted()
        val vs = if (years.size == 1) "${years[0]}" else "${years.first()}–${years.last().toString().takeLast(2)}"
        return "${months.joinToString(" / ")} · ${PanchangNames.VS_PREFIX} $vs"
    }

    /**
     * List / sheet title: "<festival> (<tithi type>)" when a festival coincides with Poonam / Amas / Bij,
     * else [SpecialDay.titleGu]. [typeLabel] maps a type to its Gujarati chip label.
     */
    fun listTitle(s: SpecialDay, typeLabel: (PanchangAlertType) -> String): String {
        val others = s.types.filter { it != PanchangAlertType.FESTIVAL && it != PanchangAlertType.AGIYARAS }
        return if (s.festivals.isNotEmpty() && others.isNotEmpty()) {
            "${s.titleGu} (${others.joinToString(", ") { typeLabel(it) }})"
        } else {
            s.titleGu
        }
    }

    /** "06-11" date part of "તિથિ સમાપ્તિ". */
    fun dm(z: ZonedDateTime): String = z.toLocalDate().format(DM)

    /** 12-hour "04:12" time part. */
    fun hm12(z: ZonedDateTime): String = z.format(HM12)

    /** 24-hour "06:52" for sunrise / sunset. */
    fun hm(z: ZonedDateTime): String = z.format(HM24)

    fun periodOfDay(hour: Int): DayPeriod = when (hour) {
        in 4..11 -> DayPeriod.MORNING
        in 12..15 -> DayPeriod.AFTERNOON
        in 16..19 -> DayPeriod.EVENING
        else -> DayPeriod.NIGHT
    }
}
