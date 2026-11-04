package com.vraj.khetihisab

import com.vraj.khetihisab.data.entity.AccountSection
import com.vraj.khetihisab.data.entity.PayStatus
import com.vraj.khetihisab.data.entity.TransactionEntity
import com.vraj.khetihisab.data.entity.TxnType
import com.vraj.khetihisab.ui.entry.EntryEvent
import com.vraj.khetihisab.ui.entry.EntryForm
import com.vraj.khetihisab.ui.entry.EntryKind
import com.vraj.khetihisab.ui.entry.EntryReducer
import com.vraj.khetihisab.ui.entry.EntryViewModel
import com.vraj.khetihisab.ui.theme.VkThemeId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

/** Pure rules added in the 2026-11-04 round. */
class RoundTwoLogicTest {

    @Test fun defaultThemeIsMati() {
        assertEquals(VkThemeId.MATI, VkThemeId.fromKey(null))
        assertEquals(VkThemeId.GREEN, VkThemeId.fromKey("green"))
    }

    @Test fun udhaarNeedsAName() {
        val f = EntryForm(kind = EntryKind.EXPENSE, section = AccountSection.GENERAL, categoryId = 1, amountText = "500")
        assertTrue(f.canSave)
        val pending = EntryReducer.reduce(f, EntryEvent.SetPending(true))
        assertTrue(pending.needsName)
        assertFalse("udhaar without a name cannot be saved", pending.canSave)
        val named = EntryReducer.reduce(pending, EntryEvent.SetCounterparty("ખાતર ભંડાર"))
        assertTrue(named.canSave)
        assertEquals(PayStatus.PENDING_PAYABLE, named.payStatus)
    }

    @Test fun incomeUdhaarIsReceivable() {
        val f = EntryForm(kind = EntryKind.INCOME, section = AccountSection.FARM, farmId = 3, amountText = "1000", pending = true, counterparty = "વેપારી")
        assertEquals(PayStatus.PENDING_RECEIVABLE, f.payStatus)
    }

    @Test fun editFormRoundTripsAnExistingRow() {
        val t = TransactionEntity(
            id = 42, date = LocalDate.of(2026, 11, 3), tithiLabel = "", section = AccountSection.FARM, type = TxnType.INCOME,
            amountPaise = 1_234_50, payStatus = PayStatus.PENDING_RECEIVABLE, counterparty = "રમેશ", farmId = 3,
            createdAt = 1, updatedAt = 1,
        )
        val f = EntryViewModel.formOf(t, cropSale = false)
        assertEquals(42L, f.editId)
        assertEquals("1234.50", f.amountText)
        assertEquals(1_234_50L, f.amountPaise)
        assertTrue(f.pending)
        assertEquals("રમેશ", f.counterparty)
        assertEquals(EntryKind.INCOME, f.kind)
        assertTrue(f.canSave)
    }
}

/** org.json is a stub on the plain JVM; Robolectric provides the real one. */
@org.junit.runner.RunWith(org.robolectric.RobolectricTestRunner::class)
@org.robolectric.annotation.Config(sdk = [34])
class UpdateJsonTest {
    @Test fun updateJsonParses() {
        val u = com.vraj.khetihisab.update.AppUpdater.parse(
            """{"versionCode":57,"versionName":"0.2.57","apk":"https://github.com/solerunner26/Vrajfarm/releases/download/v0.2.57/a.apk","sha256":"${"a".repeat(64)}","size":1000,"notes":"નવું"}""",
        )!!
        assertEquals(57, u.versionCode)
        assertEquals(1000L, u.size)
        assertEquals("નવું", u.notes)
    }
}
