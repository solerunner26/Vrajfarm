package com.vraj.khetihisab.data.repo

import com.vraj.khetihisab.data.model.FarmPnlRow
import java.time.LocalDate
import java.time.LocalTime
import java.time.Month
import java.time.YearMonth
import java.time.ZoneId
import kotlin.math.roundToLong

/** Pure money / date helpers for the data layer. Unit-tested on the JVM. */
object LedgerMath {
    val IST: ZoneId = ZoneId.of("Asia/Kolkata")

    /** Profit (paise) per vigha. Returns 0 when the area is 0 (or negative / NaN) — never divides by zero. */
    fun profitPerVigha(row: FarmPnlRow): Long = profitPerVigha(row.incomePaise, row.expensePaise, row.areaInVigha)

    fun profitPerVigha(incomePaise: Long, expensePaise: Long, areaInVigha: Double): Long =
        if (areaInVigha.isNaN() || areaInVigha <= 0.0) 0L
        else ((incomePaise - expensePaise) / areaInVigha).roundToLong()

    /**
     * Vigha scorecard order (doc 07 / report #3): highest profit-per-vigha first. Farms with no area set
     * cannot be ranked fairly, so they go last (keeping sortOrder among themselves).
     */
    fun rankByProfitPerVigha(rows: List<FarmPnlRow>): List<FarmPnlStat> =
        rows.map { FarmPnlStat(it, profitPerVigha(it)) }
            .sortedWith(compareBy<FarmPnlStat> { it.row.areaInVigha <= 0.0 }.thenByDescending { it.profitPerVighaPaise })

    /** epochMillis at [time] IST on [date] (reminder due times: loans 09:00, Panchang 07:00). */
    fun istMillis(date: LocalDate, time: LocalTime): Long = date.atTime(time).atZone(IST).toInstant().toEpochMilli()
}

data class FarmPnlStat(
    val row: FarmPnlRow,
    val profitPerVighaPaise: Long,
)

/** Inclusive LocalDate ranges for the BETWEEN queries. */
data class DateRange(val from: LocalDate, val to: LocalDate) {
    init {
        require(!to.isBefore(from)) { "to ($to) is before from ($from)" }
    }

    companion object {
        fun day(d: LocalDate) = DateRange(d, d)

        fun month(ym: YearMonth) = DateRange(ym.atDay(1), ym.atEndOfMonth())

        fun year(year: Int) = DateRange(LocalDate.of(year, 1, 1), LocalDate.of(year, 12, 31))

        /** Indian financial year Apr–Mar containing [d] (Home farm-totals fallback, doc 05). */
        fun financialYear(d: LocalDate): DateRange {
            val startYear = if (d.monthValue >= Month.APRIL.value) d.year else d.year - 1
            return DateRange(LocalDate.of(startYear, 4, 1), LocalDate.of(startYear + 1, 3, 31))
        }
    }
}

/** Fills every month of [year] (missing months = 0) from "yyyy-MM" SQL buckets. */
fun fillYearMonths(year: Int, buckets: List<com.vraj.khetihisab.data.model.MonthTotal>): Map<YearMonth, Long> {
    val byMonth = buckets.associate { YearMonth.parse(it.yearMonth) to it.totalPaise }
    return (1..12).associate { m -> YearMonth.of(year, m).let { it to (byMonth[it] ?: 0L) } }
}
