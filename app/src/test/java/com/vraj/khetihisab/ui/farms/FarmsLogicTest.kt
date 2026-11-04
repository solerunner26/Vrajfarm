package com.vraj.khetihisab.ui.farms

import com.vraj.khetihisab.data.model.FarmPnlRow
import com.vraj.khetihisab.data.repo.DateRange
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate

/** doc 07 tests: SeasonRangeTest + FarmScoreTest + scorecard display. */
class FarmsLogicTest {

    private fun d(y: Int, m: Int, day: Int) = LocalDate.of(y, m, day)

    // ---------------- SeasonRange

    @Test fun rabiCrossesTheYearBoundary() {
        assertEquals(DateRange(d(2026, 11, 1), d(2027, 3, 31)), SeasonRange.of(FarmPeriod.RABI, 2026))
    }

    @Test fun kharifSummerAndFullYearRanges() {
        assertEquals(DateRange(d(2026, 6, 1), d(2026, 10, 31)), SeasonRange.of(FarmPeriod.KHARIF, 2026))
        assertEquals(DateRange(d(2027, 3, 1), d(2027, 6, 30)), SeasonRange.of(FarmPeriod.SUMMER, 2027))
        assertEquals(DateRange(d(2026, 4, 1), d(2027, 3, 31)), SeasonRange.of(FarmPeriod.FULL_YEAR, 2026))
    }

    @Test fun summerOfCropYear2026IsLabelled2027() {
        val c = PeriodChoice(FarmPeriod.SUMMER, 2026)
        assertEquals(2027, c.seasonYear)
        assertEquals(DateRange(d(2027, 3, 1), d(2027, 6, 30)), c.range)
        assertEquals(c, PeriodChoice.ofSeason(FarmPeriod.SUMMER, 2027))
    }

    @Test fun defaultSeasonContainsToday() {
        assertEquals(PeriodChoice(FarmPeriod.RABI, 2026), SeasonRange.current(d(2026, 11, 4)))
        assertEquals(PeriodChoice(FarmPeriod.RABI, 2025), SeasonRange.current(d(2026, 2, 10)))
        assertEquals(PeriodChoice(FarmPeriod.KHARIF, 2026), SeasonRange.current(d(2026, 7, 1)))
        val summer = SeasonRange.current(d(2026, 5, 2))
        assertEquals(FarmPeriod.SUMMER, summer.period)
        assertEquals(2026, summer.seasonYear)
        listOf(d(2026, 11, 4), d(2026, 2, 10), d(2026, 7, 1), d(2026, 5, 2)).forEach { today ->
            val r = SeasonRange.current(today).range
            assert(!today.isBefore(r.from) && !today.isAfter(r.to)) { "$today not in $r" }
        }
    }

    @Test fun cropYearLabel() {
        assertEquals("2026-27", SeasonRange.cropYearLabel(2026))
        assertEquals("2099-00", SeasonRange.cropYearLabel(2099))
    }

    // ---------------- FarmScore

    private fun row(id: Long, name: String, vigha: Double, inc: Long, exp: Long) = FarmPnlRow(id, name, vigha, inc * 100, exp * 100)

    /** The canvas sample (artboard 04). */
    private val sample = listOf(
        row(1, "ખેતર 1", 12.0, 132_000, 48_000),
        row(2, "ખેતર 2", 8.0, 72_000, 26_000),
        row(3, "ખેતર 3", 10.0, 118_000, 39_500),
        row(4, "ખેતર 4", 6.0, 42_000, 18_000),
        row(5, "ખેતર 5", 5.0, 22_000, 11_000),
        row(6, "ખેતર 6", 7.0, 64_000, 25_000),
        row(7, "ખેતર 7", 4.0, 36_000, 14_000),
        row(8, "ખેતર 8", 0.0, 0, 11_000),
    )

    @Test fun perVighaDivisionAndRounding() {
        val s = FarmScoring.score(listOf(row(1, "a", 3.0, 100, 0)), emptyMap()).single()
        assertEquals(10_000L, s.profitPaise)
        assertEquals(3_333L, s.perVighaPaise) // 3333.33 paise rounds to 3333
        val s2 = FarmScoring.score(listOf(FarmPnlRow(1, "a", 3.0, 2, 0)), emptyMap()).single()
        assertEquals(1L, s2.perVighaPaise) // 0.67 rounds up
    }

    @Test fun vighaZeroIsZeroAndRanksLastWithDash() {
        val ui = FarmScoring.display(FarmScoring.score(sample, emptyMap()), byVigha = true)
        val last = ui.last()
        assertEquals("ખેતર 8", last.score.name)
        assertEquals(0L, last.score.perVighaPaise)
        assertNull(last.valuePaise) // shown as "—"
        assertEquals(0f, last.barFraction)
        assertEquals(8, last.rank)
    }

    @Test fun perVighaRankingMatchesCanvas() {
        val ui = FarmScoring.display(FarmScoring.score(sample, mapOf(1L to "કપાસ")), byVigha = true)
        // ખેતર 3: 78,500 / 10 = 7,850 per vigha is best; ખેતર 1: 84,000 / 12 = 7,000.
        assertEquals(listOf("ખેતર 3", "ખેતર 1"), ui.take(2).map { it.score.name })
        assertEquals(785_000L, ui.first().valuePaise)
        assertEquals(1f, ui.first().barFraction)
        assertEquals("કપાસ", ui[1].score.crop)
    }

    @Test fun totalProfitModeRanksByProfitAndKeepsLosses() {
        val ui = FarmScoring.display(FarmScoring.score(sample, emptyMap()), byVigha = false)
        assertEquals("ખેતર 1", ui.first().score.name)
        val loss = ui.last()
        assertEquals("ખેતર 8", loss.score.name)
        assertEquals(-1_100_000L, loss.valuePaise)
        assertEquals(0f, loss.barFraction)
    }

    @Test fun tiesBrokenByName() {
        val ui = FarmScoring.rank(
            FarmScoring.score(listOf(row(1, "ખ", 1.0, 100, 0), row(2, "ક", 1.0, 100, 0)), emptyMap()),
            byVigha = true,
        )
        assertEquals(listOf("ક", "ખ"), ui.map { it.name })
    }

    @Test fun allLossesGiveZeroBars() {
        val ui = FarmScoring.display(FarmScoring.score(listOf(row(1, "a", 2.0, 0, 10), row(2, "b", 2.0, 0, 20)), emptyMap()), true)
        ui.forEach { assertEquals(0f, it.barFraction) }
    }

    @Test fun totalVighaAndText() {
        val scores = FarmScoring.score(sample, emptyMap())
        assertEquals(52.0, FarmScoring.totalVigha(scores), 0.0)
        assertEquals("52", FarmScoring.vighaText(52.0))
        assertEquals("7.5", FarmScoring.vighaText(7.5))
    }
}
