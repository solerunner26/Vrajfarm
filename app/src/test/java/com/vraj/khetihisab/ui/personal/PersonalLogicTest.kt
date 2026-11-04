package com.vraj.khetihisab.ui.personal

import com.vraj.khetihisab.data.entity.AccountSection
import com.vraj.khetihisab.data.entity.TransactionEntity
import com.vraj.khetihisab.data.entity.TxnType
import com.vraj.khetihisab.data.model.CategoryTotal
import com.vraj.khetihisab.data.model.Totals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.YearMonth

/** doc 09 PersonalViewModelTest: the "અન્ય" bucket and net computation. */
class PersonalLogicTest {

    @Test fun topSixThenOthers() {
        val cats = listOf(
            CategoryTotal("કરિયાણું", 8_200_00), CategoryTotal("શિક્ષણ", 5_500_00), CategoryTotal("તહેવાર", 4_600_00),
            CategoryTotal("દવા", 3_000_00), CategoryTotal("કપડાં", 2_000_00), CategoryTotal("મુસાફરી", 1_500_00),
            CategoryTotal("ભેટ", 700_00), CategoryTotal("મોબાઇલ", 300_00),
        )
        val bars = PersonalLogic.bars(cats, "અન્ય")
        assertEquals(7, bars.size)
        assertEquals("અન્ય", bars.last().name)
        assertTrue(bars.last().isOthers)
        assertEquals(1_000_00L, bars.last().totalPaise)
        assertEquals(1f, bars.first().fraction)
        assertEquals(5_500.0 / 8_200.0, bars[1].fraction.toDouble(), 1e-4)
        // The breakdown sums to the expense total.
        assertEquals(cats.sumOf { it.total }, bars.sumOf { it.totalPaise })
    }

    @Test fun noOthersWhenSixOrFewerAndZeroRowsDropped() {
        val bars = PersonalLogic.bars(listOf(CategoryTotal("દવા", 300_00), CategoryTotal("ખાલી", 0)), "અન્ય")
        assertEquals(1, bars.size)
        assertFalse(bars.single().isOthers)
        assertTrue(PersonalLogic.bars(emptyList(), "અન્ય").isEmpty())
    }

    @Test fun netAndSavingsSign() {
        val s = PersonalUiState(YearMonth.of(2026, 11), totals = Totals(35_000_00, 21_300_00))
        assertEquals(13_700_00L, s.savings)
        assertEquals(-1_00L, PersonalUiState(YearMonth.of(2026, 11), totals = Totals(0, 1_00)).savings)
        assertTrue(PersonalUiState(YearMonth.of(2026, 11)).isEmpty)
    }

    @Test fun onlyPersonalRowsAreKept() {
        fun t(s: AccountSection) = TransactionEntity(
            date = LocalDate.of(2026, 11, 1), tithiLabel = "", section = s, type = TxnType.EXPENSE, amountPaise = 100, createdAt = 0, updatedAt = 0,
        )
        val kept = PersonalLogic.personalOnly(AccountSection.entries.map(::t))
        assertEquals(listOf(AccountSection.PERSONAL), kept.map { it.section })
    }
}
