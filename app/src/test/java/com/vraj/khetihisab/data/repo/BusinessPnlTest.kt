package com.vraj.khetihisab.data.repo

import com.vraj.khetihisab.data.entity.AccountSection
import com.vraj.khetihisab.data.entity.TransactionEntity
import com.vraj.khetihisab.data.entity.TxnType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

/** Business P&L = FARM + ANIMAL (પશુપાલન) + FAMILY advances + GENERAL. PERSONAL never counts. */
class BusinessPnlTest {

    private fun txn(section: AccountSection, type: TxnType, rupees: Long, deleted: Boolean = false) = TransactionEntity(
        date = LocalDate.of(2026, 11, 4), tithiLabel = "", section = section, type = type,
        amountPaise = rupees * 100, isDeleted = deleted, createdAt = 0, updatedAt = 0,
    )

    @Test fun onlyPersonalIsExcluded() {
        AccountSection.entries.forEach { s ->
            assertEquals(s != AccountSection.PERSONAL, LedgerMath.countsInBusinessPnl(s))
        }
        assertTrue(LedgerMath.countsInBusinessPnl(AccountSection.ANIMAL))
        assertFalse(LedgerMath.countsInBusinessPnl(AccountSection.PERSONAL))
    }

    @Test fun animalHusbandryCountsAndPersonalDoesNot() {
        val t = LedgerMath.businessTotals(
            listOf(
                txn(AccountSection.FARM, TxnType.INCOME, 48_800),
                txn(AccountSection.FARM, TxnType.EXPENSE, 7_350),
                txn(AccountSection.ANIMAL, TxnType.INCOME, 12_000), // દૂધ વેચાણ
                txn(AccountSection.ANIMAL, TxnType.EXPENSE, 4_500), // ઘાસચારો
                txn(AccountSection.FAMILY, TxnType.ADVANCE, 5_000), // manager advance
                txn(AccountSection.GENERAL, TxnType.EXPENSE, 3_420), // light bill
                txn(AccountSection.PERSONAL, TxnType.EXPENSE, 9_999), // must be ignored
                txn(AccountSection.PERSONAL, TxnType.INCOME, 35_000), // must be ignored
                txn(AccountSection.ANIMAL, TxnType.EXPENSE, 1_000, deleted = true), // soft-deleted
            ),
        )
        assertEquals((48_800L + 12_000) * 100, t.income)
        assertEquals((7_350L + 4_500 + 5_000 + 3_420) * 100, t.expense)
        assertEquals(40_530L * 100, t.net)
    }
}
