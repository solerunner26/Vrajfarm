package com.vraj.khetihisab

import com.vraj.khetihisab.data.entity.AccountSection
import com.vraj.khetihisab.data.entity.FarmEntity
import com.vraj.khetihisab.data.entity.PayStatus
import com.vraj.khetihisab.data.entity.ProfileRole
import com.vraj.khetihisab.data.entity.TxnType
import com.vraj.khetihisab.data.repo.AssetRepository
import com.vraj.khetihisab.share.WhatsAppTemplates
import com.vraj.khetihisab.ui.entry.EntryForm
import com.vraj.khetihisab.ui.entry.EntryKind
import com.vraj.khetihisab.ui.entry.VoiceCategory
import com.vraj.khetihisab.ui.entry.VoiceEntryParser
import com.vraj.khetihisab.ui.milk.MilkLogic
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.YearMonth

/** Rules added in the 2026-11-05 owner round. */
class RoundThreeLogicTest {

    // ---------------- WhatsApp formats (owner's exact words) ----------------

    @Test fun bhagiyaMessageIsTheOwnersFormat() {
        val text = WhatsAppTemplates.familyStatement(
            displayName = "રમેશભાઈ",
            month = YearMonth.of(2026, 11),
            monthTotalPaise = 5_000_00,
            entries = listOf(WhatsAppTemplates.FamilyEntryLine(LocalDate.of(2026, 11, 5), 5_000_00, null)),
            yearTotalPaise = 42_000_00,
            ownerName = "ગિરવાનસિંહ વાળા",
            village = "થોરાળા",
        )
        assertEquals(
            "રમેશભાઈ, તારીખ 05-11-2026 ના રોજ ₹5,000 ઉપાડ રૂપે આપેલ છે. વર્ષ 2026 માં કુલ ઉપાડ ₹42,000, " +
                "તમારી જાણ સારું. આ મેસેજ નોંધ માટે મોકલવામાં આવે છે. - ગિરવાનસિંહ વાળા .",
            text,
        )
    }

    @Test fun borrowMessageIsTheOwnersFormat() {
        val text = WhatsAppTemplates.loanReminder(
            borrower = "મનુભાઈ",
            asset = "પાવડો",
            handedOver = LocalDate.of(2026, 10, 20),
            expectedReturn = LocalDate.of(2026, 10, 30),
            ownerName = "ગિરવાનસિંહ વાળા",
            village = "થોરાળા",
            today = LocalDate.of(2026, 11, 5),
        )
        assertEquals(
            "નમસ્તે મનુભાઈ, તમે 20-10-2026 ના રોજ અમારું પાવડો લઈ ગયેલ છો. જે આજે આજ 05-11-2026 સુધી પાછું આપેલ નથી. " +
                "કામ પૂરું થઈ ગયેલ હોય તો કૃપા કરીને સમયસર પરત કરશો. - ગિરવાનસિંહ વાળા .",
            text,
        )
    }

    // ---------------- one or both ભાગિયા on a farm ----------------

    @Test fun farmCanBelongToBothBhagiya() {
        val f = FarmEntity(name = "ખેતર 1", areaInVigha = 20.0, managerRole = ProfileRole.MANAGER_1, sortOrder = 1, createdAt = 0, updatedAt = 0, jointManagers = true)
        assertTrue(f.managedBy(ProfileRole.MANAGER_1))
        assertTrue(f.managedBy(ProfileRole.MANAGER_2))
        assertFalse(f.managedBy(ProfileRole.OWNER))
        assertEquals(ProfileRole.MANAGER_1 to true, FarmEntity.encode(setOf(ProfileRole.MANAGER_1, ProfileRole.MANAGER_2)))
        assertEquals(ProfileRole.MANAGER_2 to false, FarmEntity.encode(setOf(ProfileRole.MANAGER_2)))
        assertEquals(null to false, FarmEntity.encode(emptySet()))
        // Removing one ભાગિયા from a joint farm leaves the other.
        assertEquals(ProfileRole.MANAGER_2 to false, FarmEntity.encode(f.managers - ProfileRole.MANAGER_1))
    }

    // ---------------- loan reminder after a week ----------------

    @Test fun firstLoanReminderIsAWeekOrTheReturnDate() {
        val d = LocalDate.of(2026, 11, 1)
        assertEquals(LocalDate.of(2026, 11, 8), AssetRepository.firstLoanReminder(d, d.plusDays(10)))
        assertEquals(LocalDate.of(2026, 11, 4), AssetRepository.firstLoanReminder(d, d.plusDays(3)))
    }

    // ---------------- milk register ----------------

    @Test fun milkMaths() {
        assertEquals(6.5, MilkLogic.number("6,5")!!, 0.0)
        assertNull(MilkLogic.number("abc"))
        assertEquals(5_550L, MilkLogic.ratePaise("55.50"))
        assertEquals(-1_000L, MilkLogic.difference(calculated = 10_000, paid = 9_000))
    }

    // ---------------- whole-entry speech ----------------

    private val farms = (1..8).map { it.toLong() to "ખેતર $it" }
    private val families = listOf(101L to "રમેશભાઈ", 102L to "ભાગિયા 2")
    private val cats = listOf(
        VoiceCategory(1, "ખાતર", AccountSection.FARM, TxnType.EXPENSE),
        VoiceCategory(2, "પાક વેચાણ", AccountSection.FARM, TxnType.INCOME),
        VoiceCategory(3, "લાઇટ બિલ", AccountSection.GENERAL, TxnType.EXPENSE),
        VoiceCategory(4, "દૂધ વેચાણ", AccountSection.ANIMAL, TxnType.INCOME),
    )

    @Test fun ownersExampleSentenceFillsEverything() {
        val v = VoiceEntryParser.parse("ખાતર બે હજાર ખેતર ત્રણ ઉધાર મહેશભાઈ", farms, families, cats)
        assertEquals(EntryKind.EXPENSE, v.kind)
        assertEquals(AccountSection.FARM, v.section)
        assertEquals(3L, v.farmId)
        assertEquals(1L, v.categoryId)
        assertEquals(2_000L, v.rupees)
        assertTrue(v.pending)
        assertEquals("મહેશભાઈ", v.counterparty)

        val f = VoiceEntryParser.apply(EntryForm(), v)
        assertEquals(AccountSection.FARM, f.section)
        assertEquals(3L, f.farmId)
        assertEquals(200_000L, f.amountPaise)
        assertEquals(PayStatus.PENDING_PAYABLE, f.payStatus)
        assertTrue("checked by the user, then saved", f.canSave)
    }

    @Test fun lightBillIsGeneral() {
        val v = VoiceEntryParser.parse("લાઇટ બિલ 1850", farms, families, cats)
        assertEquals(AccountSection.GENERAL, v.section)
        assertEquals(3L, v.categoryId)
        assertEquals(1_850L, v.rupees)
        assertFalse(v.pending)
    }

    @Test fun milkSaleIsAnimalIncome() {
        val v = VoiceEntryParser.parse("દૂધ વેચાણ બાર હજાર", farms, families, cats)
        assertEquals(EntryKind.INCOME, v.kind)
        assertEquals(AccountSection.ANIMAL, v.section)
        assertEquals(4L, v.categoryId)
        assertEquals(12_000L, v.rupees)
    }

    @Test fun advanceToBhagiyaByName() {
        val v = VoiceEntryParser.parse("રમેશભાઈ ઉપાડ પાંચ હજાર", farms, families, cats)
        assertEquals(AccountSection.FAMILY, v.section)
        assertEquals(101L, v.familyId)
        assertEquals(5_000L, v.rupees)
    }

    @Test fun farmNumberIsNotTheAmount() {
        val v = VoiceEntryParser.parse("ખેતર 5 પાક વેચાણ 40000", farms, families, cats)
        assertEquals(5L, v.farmId)
        assertEquals(40_000L, v.rupees)
        assertEquals(EntryKind.INCOME, v.kind)
        assertEquals(2L, v.categoryId)
    }
}
