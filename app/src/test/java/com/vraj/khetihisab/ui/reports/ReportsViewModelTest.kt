package com.vraj.khetihisab.ui.reports

import com.vraj.khetihisab.data.repo.DateRange
import com.vraj.khetihisab.report.ReportType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.File
import java.time.LocalDate

@OptIn(ExperimentalCoroutinesApi::class)
class ReportsViewModelTest {

    private val today = LocalDate.of(2026, 11, 4)
    private val calls = mutableListOf<Pair<ReportType, DateRange>>()

    @Before
    fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @After
    fun tearDown() = Dispatchers.resetMain()

    private fun vm(fail: Boolean = false, initial: ReportType? = null) = ReportsViewModel(
        generator = { t, r ->
            calls += t to r
            if (fail) error("boom")
            File("reports/${t.slug}_20261104_1200.pdf")
        },
        today = { today },
        initialType = initial,
    )

    @Test
    fun selectUsesDefaultRangeAndGenerates() = runTest {
        val v = vm(initial = ReportType.FARM_PNL)
        assertEquals(DateRange(LocalDate.of(2026, 4, 1), LocalDate.of(2027, 3, 31)), v.ui.value.range)
        v.generate()
        assertEquals(ReportType.FARM_PNL to DateRange.financialYear(today), calls.single())
        assertEquals(ReportType.FARM_PNL, v.ui.value.lastType)
        assertTrue(v.ui.value.lastFile!!.endsWith("farm_pnl_20261104_1200.pdf"))
        assertNull(v.ui.value.selected)
        assertFalse(v.ui.value.generating)
    }

    @Test
    fun rangeEditsStayOrdered() {
        val v = vm()
        v.select(ReportType.PERSONAL)
        v.setFrom(LocalDate.of(2026, 12, 10))
        assertEquals(DateRange(LocalDate.of(2026, 12, 10), LocalDate.of(2026, 12, 10)), v.ui.value.range)
        v.setTo(LocalDate.of(2026, 12, 1))
        assertEquals(DateRange(LocalDate.of(2026, 12, 1), LocalDate.of(2026, 12, 1)), v.ui.value.range)
        v.setPreset(RangePreset.LAST_MONTH)
        assertEquals(DateRange(LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 31)), v.ui.value.range)
        v.select(ReportType.DAILY_LEDGER)
        v.setDay(LocalDate.of(2026, 11, 2))
        assertEquals(DateRange.day(LocalDate.of(2026, 11, 2)), v.ui.value.range)
    }

    @Test
    fun failureKeepsSheetOpen() = runTest {
        val v = vm(fail = true, initial = ReportType.PENDING_UDHAAR)
        v.generate()
        assertTrue(v.ui.value.failed)
        assertEquals(ReportType.PENDING_UDHAAR, v.ui.value.selected)
        assertNull(v.ui.value.lastFile)
    }

    @Test
    fun presetsPerType() {
        assertTrue(ReportsViewModel.presetsFor(ReportType.ASSETS_LOANS).isEmpty())
        assertEquals(listOf(RangePreset.TODAY, RangePreset.YESTERDAY), ReportsViewModel.presetsFor(ReportType.DAILY_LEDGER))
        assertEquals(DateRange.year(2025), ReportsViewModel.presetRange(RangePreset.LAST_YEAR, today))
        assertEquals("02-11-2026 – 04-11-2026", rangeLabel(DateRange(LocalDate.of(2026, 11, 2), today)))
        assertEquals("04-11-2026", rangeLabel(DateRange.day(today)))
        assertEquals(7, ReportType.entries.size)
    }
}
