package com.vraj.khetihisab.panchang

import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlin.math.abs

/**
 * Offline Gujarati Panchang for Ahmedabad (doc 02 §5).
 *
 * The day's tithi is the **Udaya tithi**: the tithi in force at local sunrise of the civil date.
 * Always pass the civil [LocalDate] (an entry made at 02:00 belongs to that date, but shows the
 * previous sunrise's tithi). Calculation always happens in Asia/Kolkata regardless of device zone.
 */
object GujaratiPanchangHelper {

    const val LATITUDE = 23.0225
    const val LONGITUDE = 72.5714
    val ZONE: ZoneId = ZoneId.of("Asia/Kolkata")

    private val MIN_DATE: LocalDate = LocalDate.of(1990, 1, 1)
    private val MAX_DATE: LocalDate = LocalDate.of(2100, 12, 31)
    private val NEAR_BOUNDARY: Duration = Duration.ofMinutes(10)

    private const val CACHE_SIZE = 366
    private val cache = object : LinkedHashMap<LocalDate, PanchangDay>(CACHE_SIZE, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<LocalDate, PanchangDay>?): Boolean =
            size > CACHE_SIZE
    }

    private val DDMMYYYY: DateTimeFormatter = DateTimeFormatter.ofPattern("dd-MM-yyyy")

    /** Panchang of a civil date. Throws [IllegalArgumentException] outside 1990–2100. */
    fun day(date: LocalDate): PanchangDay {
        require(!date.isBefore(MIN_DATE) && !date.isAfter(MAX_DATE)) { "Date out of supported range (1990–2100): $date" }
        synchronized(cache) { cache[date]?.let { return it } }
        val computed = compute(date)
        synchronized(cache) { cache[date] = computed }
        return computed
    }

    /** Every day of a Gregorian month (calendar grid, doc 10). */
    fun month(year: Int, month: Int): List<PanchangDay> {
        val ym = YearMonth.of(year, month)
        return (1..ym.lengthOfMonth()).map { day(ym.atDay(it)) }
    }

    /** "આસો વદ નોમ" for list rows. */
    fun shortLabel(date: LocalDate): String = day(date).tithiGu

    /** List row: "02-11-2026 · આસો વદ આઠમ". */
    fun listLabel(date: LocalDate): String = "${date.format(DDMMYYYY)} · ${day(date).tithiGu}"

    /** Home banner: "03 નવેમ્બર 2026 | મંગળવાર | આસો વદ નોમ". */
    fun bannerLabel(date: LocalDate): String {
        val d = day(date)
        val dd = date.dayOfMonth.toString().padStart(2, '0')
        return "$dd ${PanchangNames.gregorianMonths[date.monthValue - 1]} ${date.year} | ${d.weekdayGu} | ${d.tithiGu}"
    }

    internal fun clearCache() = synchronized(cache) { cache.clear() }

    private fun compute(date: LocalDate): PanchangDay {
        val sun = Astro.sunTimes(date, LATITUDE, LONGITUDE)
        val sunrise = sun.sunrise

        val e = Astro.elongation(sunrise)
        val tithiIndex = Astro.tithiAt(sunrise)
        val (paksha, inPaksha) = PanchangRules.pakshaOf(tithiIndex)

        // Tithi boundaries: start at (idx-1)*12°, end at idx*12°.
        val toStart = (e - (tithiIndex - 1) * 12.0) / Astro.SYNODIC_RATE
        val toEnd = (tithiIndex * 12.0 - e) / Astro.SYNODIC_RATE
        val start = Astro.solveElongation((tithiIndex - 1) * 12.0, sunrise.minusDays(toStart))
        val end = Astro.solveElongation((tithiIndex % 30) * 12.0, sunrise.plusDays(toEnd))

        // Amanta month from the lunation that contains this sunrise.
        val (nm0, nm1) = PanchangRules.lunationAround(sunrise)
        val r0 = PanchangRules.rashiOf(Astro.siderealSunLongitude(nm0))
        val r1 = PanchangRules.rashiOf(Astro.siderealSunLongitude(nm1))
        val m = PanchangRules.monthFromRashis(r0, r1)
        val monthGu = PanchangNames.month(m.monthIndex, m.isAdhik)

        val near = abs(Duration.between(start, sunrise).toMillis()) < NEAR_BOUNDARY.toMillis() ||
            abs(Duration.between(sunrise, end).toMillis()) < NEAR_BOUNDARY.toMillis()

        return PanchangDay(
            date = date,
            weekdayGu = PanchangNames.weekday(date.dayOfWeek),
            vikramSamvat = PanchangRules.vikramSamvat(date, m.monthIndex, m.isAdhik),
            monthIndex = m.monthIndex,
            isAdhik = m.isAdhik,
            monthGu = monthGu,
            paksha = paksha,
            tithiInPaksha = inPaksha,
            tithiIndex = tithiIndex,
            tithiGu = "$monthGu ${paksha.gu} ${PanchangNames.tithiName(paksha, inPaksha)}",
            tithiStartsAt = start.atZone(ZONE),
            tithiEndsAt = end.atZone(ZONE),
            sunrise = sunrise.atZone(ZONE),
            sunset = sun.sunset.atZone(ZONE),
            moonIllumination = Astro.moonIllumination(e).toFloat(),
            waxing = e < 180.0,
            nearBoundary = near,
        )
    }

    private fun Instant.plusDays(days: Double): Instant = plusMillis((days * Astro.MS_PER_DAY).toLong())
    private fun Instant.minusDays(days: Double): Instant = minusMillis((days * Astro.MS_PER_DAY).toLong())
}
