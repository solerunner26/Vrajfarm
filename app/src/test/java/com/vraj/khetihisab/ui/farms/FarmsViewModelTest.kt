package com.vraj.khetihisab.ui.farms

import androidx.lifecycle.SavedStateHandle
import com.vraj.khetihisab.data.model.FarmPnlRow
import com.vraj.khetihisab.data.model.Totals
import com.vraj.khetihisab.data.repo.DateRange
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import java.time.LocalDate

@OptIn(ExperimentalCoroutinesApi::class)
class FarmsViewModelTest {

    @Before fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())
    @After fun tearDown() = Dispatchers.resetMain()

    /** Kharif rows differ from Rabi rows so a period switch is visible. */
    private class FakeSource : FarmsSource {
        val requested = mutableListOf<DateRange>()
        val crops = MutableStateFlow(mapOf(1L to "કપાસ"))
        override fun farmPnl(range: DateRange): Flow<List<FarmPnlRow>> {
            requested += range
            return flowOf(
                if (range.from.monthValue == 6) {
                    listOf(FarmPnlRow(1, "ખેતર 1", 10.0, 1_00_000_00, 40_000_00), FarmPnlRow(2, "ખેતર 2", 0.0, 10_000_00, 0))
                } else {
                    listOf(FarmPnlRow(1, "ખેતર 1", 10.0, 0, 5_000_00), FarmPnlRow(2, "ખેતર 2", 0.0, 0, 0))
                },
            )
        }
        override fun farmTotals(range: DateRange): Flow<Totals> = farmPnl(range).map { rows ->
            Totals(rows.sumOf { it.incomePaise }, rows.sumOf { it.expensePaise })
        }
        override fun activeCrops(): Flow<Map<Long, String>> = crops
    }

    @Test fun defaultsToTodaysSeasonAndRanksPerVigha() = runTest {
        val src = FakeSource()
        val vm = FarmsViewModel(src, SavedStateHandle(), today = LocalDate.of(2026, 7, 15))
        val job = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.state.collect {} }
        val s = vm.state.value
        assertEquals(PeriodChoice(FarmPeriod.KHARIF, 2026), s.choice)
        assertEquals(Totals(1_10_000_00, 40_000_00), s.totals)
        assertEquals(10.0, s.totalVigha, 0.0)
        assertEquals("ખેતર 1", s.rows.first().score.name)
        assertEquals(6_000_00L, s.rows.first().valuePaise) // ₹60,000 / 10 vigha
        assertEquals("કપાસ", s.rows.first().score.crop)
        assertNull(s.rows.last().valuePaise) // no vigha → "—", last
        job.cancel()
    }

    @Test fun switchingPeriodAndSortRequeries() = runTest {
        val src = FakeSource()
        val handle = SavedStateHandle()
        val vm = FarmsViewModel(src, handle, today = LocalDate.of(2026, 7, 15))
        val job = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.state.collect {} }

        vm.selectPeriod(FarmPeriod.RABI)
        assertEquals(DateRange(LocalDate.of(2026, 11, 1), LocalDate.of(2027, 3, 31)), src.requested.last())
        assertEquals(-5_000_00L, vm.state.value.totals.net)

        vm.setByVigha(false)
        val rows = vm.state.value.rows
        assertEquals("ખેતર 2", rows.first().score.name) // 0 profit beats a loss in total mode
        assertEquals(-5_000_00L, rows.last().valuePaise)

        vm.shiftYear(-1)
        assertEquals(PeriodChoice(FarmPeriod.RABI, 2025), vm.state.value.choice)
        assertEquals("RABI", handle.get<String>(FarmsViewModel.KEY_PERIOD))
        assertEquals(2025, handle.get<Int>(FarmsViewModel.KEY_YEAR))
        job.cancel()
    }

    @Test fun restoresFromSavedState() = runTest {
        val handle = SavedStateHandle(mapOf(FarmsViewModel.KEY_PERIOD to "FULL_YEAR", FarmsViewModel.KEY_YEAR to 2024, FarmsViewModel.KEY_BY_VIGHA to false))
        val vm = FarmsViewModel(FakeSource(), handle, today = LocalDate.of(2026, 7, 15))
        assertEquals(PeriodChoice(FarmPeriod.FULL_YEAR, 2024), vm.state.value.choice)
        assertEquals(false, vm.state.value.byVigha)
    }
}
