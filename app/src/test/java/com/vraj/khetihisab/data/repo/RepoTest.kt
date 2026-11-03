package com.vraj.khetihisab.data.repo

import com.vraj.khetihisab.data.model.FarmPnlRow
import com.vraj.khetihisab.data.model.MonthTotal
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test
import java.time.LocalDate
import java.time.LocalTime
import java.time.YearMonth

class RepoTest {
    private fun row(id: Long, area: Double, income: Long, expense: Long) =
        FarmPnlRow(farmId = id, name = "ખેતર $id", areaInVigha = area, incomePaise = income, expensePaise = expense)

    @Test fun profitPerVighaZero() {
        assertEquals(0L, LedgerMath.profitPerVigha(row(1, 0.0, 100_000_00, 10_000_00)))
    }

    @Test fun profitPerVighaNegativeOrNanAreaIsZero() {
        assertEquals(0L, LedgerMath.profitPerVigha(5_000, 0, -2.0))
        assertEquals(0L, LedgerMath.profitPerVigha(5_000, 0, Double.NaN))
    }

    @Test fun profitPerVighaDividesNetByArea() {
        // ₹1,00,000 income − ₹40,000 expense over 4 vigha = ₹15,000 / vigha
        assertEquals(15_000_00L, LedgerMath.profitPerVigha(row(1, 4.0, 100_000_00, 40_000_00)))
    }

    @Test fun profitPerVighaRoundsHalfUpAndHandlesLoss() {
        assertEquals(33L, LedgerMath.profitPerVigha(100, 0, 3.0)) // 33.33
        assertEquals(67L, LedgerMath.profitPerVigha(200, 0, 3.0)) // 66.67
        assertEquals(-2_500_00L, LedgerMath.profitPerVigha(0, 5_000_00, 2.0))
    }

    @Test fun rankingPutsZeroAreaLast() {
        val ranked = LedgerMath.rankByProfitPerVigha(
            listOf(
                row(1, 0.0, 999_999_00, 0), // no area: last despite high profit
                row(2, 2.0, 10_000_00, 0), // 5,000 / vigha
                row(3, 1.0, 8_000_00, 0), // 8,000 / vigha
            ),
        )
        assertEquals(listOf(3L, 2L, 1L), ranked.map { it.row.farmId })
        assertEquals(0L, ranked.last().profitPerVighaPaise)
    }

    @Test fun dateRanges() {
        val m = DateRange.month(YearMonth.of(2024, 2))
        assertEquals(LocalDate.of(2024, 2, 1), m.from)
        assertEquals(LocalDate.of(2024, 2, 29), m.to)

        val y = DateRange.year(2026)
        assertEquals(LocalDate.of(2026, 1, 1), y.from)
        assertEquals(LocalDate.of(2026, 12, 31), y.to)

        assertEquals(
            DateRange(LocalDate.of(2026, 4, 1), LocalDate.of(2027, 3, 31)),
            DateRange.financialYear(LocalDate.of(2026, 11, 3)),
        )
        assertEquals(
            DateRange(LocalDate.of(2025, 4, 1), LocalDate.of(2026, 3, 31)),
            DateRange.financialYear(LocalDate.of(2026, 3, 31)),
        )
        assertThrows(IllegalArgumentException::class.java) {
            DateRange(LocalDate.of(2026, 1, 2), LocalDate.of(2026, 1, 1))
        }
    }

    @Test fun fillYearMonthsFillsMissingWithZero() {
        val map = fillYearMonths(2026, listOf(MonthTotal("2026-03", 5_000_00), MonthTotal("2026-11", 700_00)))
        assertEquals(12, map.size)
        assertEquals(5_000_00L, map[YearMonth.of(2026, 3)])
        assertEquals(700_00L, map[YearMonth.of(2026, 11)])
        assertEquals(0L, map[YearMonth.of(2026, 1)])
        assertEquals(YearMonth.of(2026, 1), map.keys.first())
    }

    @Test fun istMillis() {
        // 2025-11-26 09:00 IST == 2025-11-26 03:30 UTC
        val expected = java.time.Instant.parse("2025-11-26T03:30:00Z").toEpochMilli()
        assertEquals(expected, LedgerMath.istMillis(LocalDate.of(2025, 11, 26), LocalTime.of(9, 0)))
    }
}
