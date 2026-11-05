package com.vraj.khetihisab.ui.family

import androidx.lifecycle.SavedStateHandle
import com.vraj.khetihisab.data.entity.AccountSection
import com.vraj.khetihisab.data.entity.FamilyEntity
import com.vraj.khetihisab.data.entity.PersonProfileEntity
import com.vraj.khetihisab.data.entity.ProfileRole
import com.vraj.khetihisab.data.entity.TransactionEntity
import com.vraj.khetihisab.data.entity.TxnType
import com.vraj.khetihisab.share.WhatsAppTemplates
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.LocalDate
import java.time.YearMonth

/** doc 08 FamilyViewModelTest: switching A/B switches the totals; month totals; preview == sent text. */
@OptIn(ExperimentalCoroutinesApi::class)
class FamilyViewModelTest {

    @Before fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())
    @After fun tearDown() = Dispatchers.resetMain()

    private val nov = YearMonth.of(2026, 11)

    private fun adv(id: Long, familyId: Long, date: LocalDate, rupees: Long, note: String?) = TransactionEntity(
        id = id, date = date, tithiLabel = "આસો વદ છઠ", section = AccountSection.FAMILY, type = TxnType.ADVANCE,
        amountPaise = rupees * 100, familyId = familyId, note = note, createdAt = id, updatedAt = id,
    )

    private class FakeSource(private val all: List<TransactionEntity>) : FamilySource {
        val families = MutableStateFlow(
            listOf(
                FamilyEntity(1, "A", "પરિવાર A", "9876543210", 0, 0),
                FamilyEntity(2, "B", "પરિવાર B", null, 0, 0),
            ),
        )
        val savedPhones = mutableListOf<Pair<Long, String>>()
        val deleted = mutableListOf<Long>()
        private fun of(fid: Long, from: LocalDate, to: LocalDate) =
            all.filter { it.familyId == fid && !it.date.isBefore(from) && !it.date.isAfter(to) }

        override fun families(): Flow<List<FamilyEntity>> = families
        override fun monthTotal(familyId: Long, ym: YearMonth) = flowOf(of(familyId, ym.atDay(1), ym.atEndOfMonth()).sumOf { it.amountPaise })
        override fun yearTotal(familyId: Long, year: Int) =
            flowOf(of(familyId, LocalDate.of(year, 1, 1), LocalDate.of(year, 12, 31)).sumOf { it.amountPaise })
        override fun entries(familyId: Long, ym: YearMonth) =
            flowOf(of(familyId, ym.atDay(1), ym.atEndOfMonth()).sortedByDescending { it.date })
        override fun categoryNames(): Flow<Map<Long, String>> = flowOf(emptyMap())
        override fun owner(): Flow<PersonProfileEntity?> =
            flowOf(PersonProfileEntity(1, ProfileRole.OWNER, "ગિરવાનસિંહ વાળા", null, "મુ. પો. થોરાળા, તા. મહુવા", null, 0))
        override fun template() = WhatsAppTemplates.DEFAULT_FAMILY_STATEMENT
        override suspend fun savePhone(familyId: Long, phone: String) {
            savedPhones += familyId to phone
            families.value = families.value.map { if (it.id == familyId) it.copy(whatsappPhone = phone) else it }
        }
        override suspend fun delete(id: Long) { deleted += id }
    }

    private val data = listOf(
        adv(1, 1, LocalDate.of(2026, 11, 2), 5_000, "રોકડ ઉપાડ"),
        adv(2, 1, LocalDate.of(2026, 11, 3), 7_000, null),
        adv(3, 1, LocalDate.of(2026, 10, 24), 4_000, "દવાખાના માટે"),
        adv(4, 2, LocalDate.of(2026, 11, 1), 8_500, null),
        adv(5, 2, LocalDate.of(2026, 3, 1), 62_700, null),
        adv(6, 1, LocalDate.of(2025, 12, 31), 99_000, null), // previous year: never in 2026 totals
    )

    @Test fun switchingTabSwitchesTotals() = runTest {
        val src = FakeSource(data)
        val vm = FamilyViewModel(src, SavedStateHandle(), today = LocalDate.of(2026, 11, 4), fallbackTitle = "ઉપાડ")
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.state.collect {} }

        val a = vm.state.value
        assertEquals("A", a.code)
        assertEquals(12_000_00L, a.monthTotal)
        assertEquals(16_000_00L, a.yearTotal)
        assertEquals(listOf(2L, 1L), a.rows.map { it.id }) // newest first on screen
        assertEquals("ઉપાડ", a.rows.first().title) // no note → fallback
        assertEquals("02-11-2026 · આસો વદ છઠ", a.rows.last().meta)

        vm.selectFamily("B")
        val b = vm.state.value
        assertEquals("B", b.code)
        assertEquals(8_500_00L, b.monthTotal)
        assertEquals(71_200_00L, b.yearTotal)
        assertEquals(null, b.phone)
    }

    @Test fun monthPickerChangesMonthTotalsAndKeepsYear() = runTest {
        val handle = SavedStateHandle()
        val vm = FamilyViewModel(FakeSource(data), handle, today = LocalDate.of(2026, 11, 4))
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.state.collect {} }
        vm.selectMonth(YearMonth.of(2026, 10))
        assertEquals(4_000_00L, vm.state.value.monthTotal)
        assertEquals(16_000_00L, vm.state.value.yearTotal)
        assertEquals("2026-10", handle.get<String>(FamilyViewModel.KEY_MONTH))

        vm.selectMonth(YearMonth.of(2026, 9))
        assertEquals(0L, vm.state.value.monthTotal)
        assertTrue(vm.state.value.receipt.contains(WhatsAppTemplates.NO_ADVANCE_LINE))
    }

    @Test fun receiptIsTheOwnersOneAdvanceFormat() = runTest {
        val vm = FamilyViewModel(FakeSource(data), SavedStateHandle(), today = LocalDate.of(2026, 11, 4))
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.state.collect {} }
        // Owner format 2026-11-05: the newest advance, its date and amount, and the year's total.
        val expected = "પરિવાર A, તારીખ 03-11-2026 ના રોજ ₹7,000 ઉપાડ રૂપે આપેલ છે. વર્ષ 2026 માં કુલ ઉપાડ ₹16,000, " +
            "તમારી જાણ સારું. આ મેસેજ નોંધ માટે મોકલવામાં આવે છે. - ગિરવાનસિંહ વાળા ."
        assertEquals(expected, vm.state.value.receipt)
        assertEquals("9876543210", vm.state.value.phone)
    }

    @Test fun savePhoneAndDeleteGoToTheSource() = runTest {
        val src = FakeSource(data)
        val vm = FamilyViewModel(src, SavedStateHandle(mapOf(FamilyViewModel.KEY_CODE to "B")), today = LocalDate.of(2026, 11, 4))
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.state.collect {} }
        vm.savePhone(" 98250 12345 ")
        assertEquals(listOf(2L to "98250 12345"), src.savedPhones)
        assertEquals("98250 12345", vm.state.value.phone)
        vm.delete(4)
        assertEquals(listOf(4L), src.deleted)
    }

    @Test fun phoneValidation() {
        assertTrue(FamilyLogic.isValidPhone("+91 98765 43210"))
        assertTrue(FamilyLogic.isValidPhone("09876543210"))
        assertEquals(false, FamilyLogic.isValidPhone("12345"))
    }
}
