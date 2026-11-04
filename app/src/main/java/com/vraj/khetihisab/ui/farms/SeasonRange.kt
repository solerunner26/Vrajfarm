package com.vraj.khetihisab.ui.farms

import com.vraj.khetihisab.data.entity.Season
import com.vraj.khetihisab.data.repo.DateRange
import java.time.LocalDate

/** Farms period chips (doc 07 §2). */
enum class FarmPeriod { KHARIF, RABI, SUMMER, FULL_YEAR }

/**
 * A period of one crop year. [cropYear] is the year the crop year starts in (April). Crop year 2026 =
 * ખરીફ 2026 (Jun–Oct 2026) → રવી 2026 (Nov 2026 → Mar 2027) → ઉનાળુ 2027 (Mar–Jun 2027), and
 * આખું વર્ષ = Apr 2026 → Mar 2027.
 */
data class PeriodChoice(val period: FarmPeriod, val cropYear: Int) {
    /** Calendar year printed on the chip and passed to [SeasonRange.of]. */
    val seasonYear: Int get() = if (period == FarmPeriod.SUMMER) cropYear + 1 else cropYear
    val range: DateRange get() = SeasonRange.of(period, seasonYear)

    companion object {
        /** From a season label year ("ઉનાળુ 2027" → crop year 2026). */
        fun ofSeason(period: FarmPeriod, seasonYear: Int) =
            PeriodChoice(period, if (period == FarmPeriod.SUMMER) seasonYear - 1 else seasonYear)
    }
}

object SeasonRange {

    /**
     * Inclusive date range (doc 07):
     * - ખરીફ: 01-06 → 31-10
     * - રવી: 01-11 → 31-03 of the next year (crosses the calendar year)
     * - ઉનાળુ: 01-03 → 30-06
     * - આખું વર્ષ: 01-04 → 31-03 of the next year
     */
    fun of(period: FarmPeriod, year: Int): DateRange = when (period) {
        FarmPeriod.KHARIF -> DateRange(LocalDate.of(year, 6, 1), LocalDate.of(year, 10, 31))
        FarmPeriod.RABI -> DateRange(LocalDate.of(year, 11, 1), LocalDate.of(year + 1, 3, 31))
        FarmPeriod.SUMMER -> DateRange(LocalDate.of(year, 3, 1), LocalDate.of(year, 6, 30))
        FarmPeriod.FULL_YEAR -> DateRange(LocalDate.of(year, 4, 1), LocalDate.of(year + 1, 3, 31))
    }

    /** Crop (financial) year Apr–Mar that contains [d]. */
    fun cropYearOf(d: LocalDate): Int = if (d.monthValue >= 4) d.year else d.year - 1

    /** The season containing [today]: Jun–Oct Kharif, Nov–Mar Rabi, Apr–May Summer. */
    fun current(today: LocalDate): PeriodChoice = when (today.monthValue) {
        in 6..10 -> PeriodChoice(FarmPeriod.KHARIF, today.year)
        11, 12 -> PeriodChoice(FarmPeriod.RABI, today.year)
        in 1..3 -> PeriodChoice(FarmPeriod.RABI, today.year - 1)
        else -> PeriodChoice.ofSeason(FarmPeriod.SUMMER, today.year)
    }

    /** Season string stored on a crop cycle ([Season]) → period. Unknown → null. */
    fun fromCycleSeason(season: String?): FarmPeriod? = when (season?.uppercase()) {
        Season.KHARIF -> FarmPeriod.KHARIF
        Season.RABI -> FarmPeriod.RABI
        Season.SUMMER -> FarmPeriod.SUMMER
        else -> null
    }

    fun toCycleSeason(period: FarmPeriod): String = when (period) {
        FarmPeriod.KHARIF -> Season.KHARIF
        FarmPeriod.RABI -> Season.RABI
        FarmPeriod.SUMMER -> Season.SUMMER
        FarmPeriod.FULL_YEAR -> Season.KHARIF
    }

    /** "2026-27" */
    fun cropYearLabel(cropYear: Int): String = "$cropYear-" + ((cropYear + 1) % 100).toString().padStart(2, '0')
}
