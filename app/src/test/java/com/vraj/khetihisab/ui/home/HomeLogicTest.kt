package com.vraj.khetihisab.ui.home

import com.vraj.khetihisab.data.entity.AccountSection
import com.vraj.khetihisab.data.entity.AssetEntity
import com.vraj.khetihisab.data.entity.AssetLoanEntity
import com.vraj.khetihisab.data.entity.FarmCropCycleEntity
import com.vraj.khetihisab.data.entity.PayStatus
import com.vraj.khetihisab.data.entity.Priority
import com.vraj.khetihisab.data.entity.ReminderEntity
import com.vraj.khetihisab.data.entity.Season
import com.vraj.khetihisab.data.entity.TransactionEntity
import com.vraj.khetihisab.data.entity.TxnType
import com.vraj.khetihisab.data.model.LoanWithAsset
import com.vraj.khetihisab.data.model.SectionTotals
import com.vraj.khetihisab.data.model.Totals
import com.vraj.khetihisab.data.model.UdhaarSummary
import com.vraj.khetihisab.data.repo.DateRange
import com.vraj.khetihisab.data.repo.LedgerMath
import com.vraj.khetihisab.panchang.GujaratiPanchangHelper
import com.vraj.khetihisab.panchang.PanchangAlertType
import com.vraj.khetihisab.panchang.SpecialDay
import com.vraj.khetihisab.ui.components.AmountKind
import com.vraj.khetihisab.ui.farms.FarmPeriod
import com.vraj.khetihisab.ui.farms.PeriodChoice
import com.vraj.khetihisab.ui.theme.VkIcon
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

/** doc 05 HomeViewModelTest rules, on the pure [HomeLogic]. */
class HomeLogicTest {

    private val today = LocalDate.of(2026, 11, 4)

    private fun txn(
        section: AccountSection,
        type: TxnType,
        rupees: Long,
        id: Long = 0,
        pay: PayStatus = PayStatus.PAID,
        farmId: Long? = null,
        familyId: Long? = null,
        categoryId: Long? = null,
        counterparty: String? = null,
    ) = TransactionEntity(
        id = id, date = LocalDate.of(2026, 11, 2), tithiLabel = "આસો વદ આઠમ", section = section, type = type,
        amountPaise = rupees * 100, payStatus = pay, farmId = farmId, familyId = familyId, categoryId = categoryId,
        counterparty = counterparty, createdAt = 0, updatedAt = 0,
    )

    // ---------------- business P&L card

    @Test fun businessCardValuesExcludePersonalAndMatchTheTotals() {
        val entries = listOf(
            txn(AccountSection.FARM, TxnType.INCOME, 48_800),
            txn(AccountSection.FARM, TxnType.EXPENSE, 7_350),
            txn(AccountSection.ANIMAL, TxnType.INCOME, 12_000),
            txn(AccountSection.ANIMAL, TxnType.EXPENSE, 4_500),
            txn(AccountSection.FAMILY, TxnType.ADVANCE, 5_000),
            txn(AccountSection.GENERAL, TxnType.EXPENSE, 3_420),
            txn(AccountSection.PERSONAL, TxnType.EXPENSE, 9_999),
        )
        // What businessBySection would return (SQL groups; a defensive PERSONAL row is dropped).
        val rows = listOf(
            SectionTotals(AccountSection.GENERAL, 0, 3_420_00),
            SectionTotals(AccountSection.FARM, 48_800_00, 7_350_00),
            SectionTotals(AccountSection.FAMILY, 0, 5_000_00),
            SectionTotals(AccountSection.ANIMAL, 12_000_00, 4_500_00),
            SectionTotals(AccountSection.PERSONAL, 0, 9_999_00),
        )
        val sheet = HomeLogic.businessBreakdown(rows)
        assertEquals(listOf(AccountSection.FARM, AccountSection.ANIMAL, AccountSection.GENERAL, AccountSection.FAMILY), sheet.map { it.section })
        val sum = HomeLogic.sumOf(sheet)
        assertEquals(LedgerMath.businessTotals(entries), sum)
        assertEquals(Totals(60_800_00, 20_270_00), sum)
        assertEquals(40_530_00L, sum.net)
    }

    @Test fun breakdownFillsMissingSectionsWithZero() {
        val sheet = HomeLogic.businessBreakdown(listOf(SectionTotals(AccountSection.ANIMAL, 100, 50)))
        assertEquals(4, sheet.size)
        assertEquals(SectionTotals(AccountSection.FARM, 0, 0), sheet[0])
        assertEquals(50L, sheet[1].netPaise)
    }

    @Test fun businessRangeIsTheFinancialYear() {
        assertEquals(DateRange(LocalDate.of(2026, 4, 1), LocalDate.of(2027, 3, 31)), HomeLogic.businessRange(today))
        assertEquals(DateRange(LocalDate.of(2025, 4, 1), LocalDate.of(2026, 3, 31)), HomeLogic.businessRange(LocalDate.of(2026, 3, 31)))
    }

    @Test fun negativeNetIsSignedWithMinus() {
        assertEquals("−₹12,500", ScreensFormat.signedRupees(-12_500_00))
        assertEquals("₹2,93,500", ScreensFormat.signedRupees(2_93_500_00))
    }

    // ---------------- alerts

    @Test fun udhaarHiddenWhenZero() {
        assertFalse(HomeLogic.showUdhaar(UdhaarSummary.EMPTY))
        assertFalse(HomeLogic.showUdhaar(UdhaarSummary(0, 0, 0)))
        assertTrue(HomeLogic.showUdhaar(UdhaarSummary(18_400_00, 0, 1)))
        assertTrue(HomeLogic.showUdhaar(UdhaarSummary(0, 62_000_00, 4)))
    }

    private fun loan(id: Long, due: LocalDate) = LoanWithAsset(
        AssetLoanEntity(
            id = id, assetId = 1, borrowerName = "રમેશભાઈ", borrowerPhone = null, borrowerPhotoPath = null,
            handedOverDate = due.minusDays(10), handedOverTithi = "", expectedReturnDate = due, createdAt = 0, updatedAt = 0,
        ),
        AssetEntity(id = 1, name = "રોટાવેટર", iconKey = "asset", purchaseDate = null, pricePaise = null, photoPath = null, note = null, createdAt = 0, updatedAt = 0),
    )

    @Test fun overdueListIsCappedAtTwoOldestFirst() {
        val (shown, more) = HomeLogic.capOverdue(
            listOf(loan(1, today.minusDays(1)), loan(2, today.minusDays(4)), loan(3, today.minusDays(2))),
            today,
        )
        assertEquals(listOf(2L, 3L), shown.map { it.loan.loan.id })
        assertEquals(4, shown.first().daysLate)
        assertEquals(1, more)
    }

    @Test fun urgentKeepsHighWithin48hAtMostTwo() {
        val now = 1_000_000_000_000L
        val h = 60L * 60 * 1000
        fun r(id: Long, p: Priority, dueIn: Long, done: Boolean = false) = ReminderEntity(
            id = id, title = "લાઇટ બિલ $id", note = null, imagePath = null, priority = p, dueAt = now + dueIn, isDone = done,
            createdAt = 0, updatedAt = 0,
        )
        val out = HomeLogic.urgent(
            listOf(r(1, Priority.HIGH, 50 * h), r(2, Priority.NORMAL, h), r(3, Priority.HIGH, 30 * h), r(4, Priority.HIGH, -h), r(5, Priority.HIGH, 2 * h), r(6, Priority.HIGH, h, done = true)),
            now,
        )
        assertEquals(listOf(4L, 5L), out.map { it.reminder.id })
    }

    // ---------------- farm card period

    private fun cycle(season: String, year: Int, start: LocalDate, active: Boolean = true) =
        FarmCropCycleEntity(farmId = 1, cropId = 1, season = season, year = year, startDate = start, endDate = null, isActive = active)

    @Test fun farmPeriodUsesLatestActiveCycleElseCropYear() {
        assertEquals(
            PeriodChoice(FarmPeriod.RABI, 2026),
            HomeLogic.farmPeriod(listOf(cycle(Season.KHARIF, 2026, LocalDate.of(2026, 6, 10)), cycle(Season.RABI, 2026, LocalDate.of(2026, 11, 1))), today),
        )
        assertEquals(PeriodChoice(FarmPeriod.FULL_YEAR, 2026), HomeLogic.farmPeriod(emptyList(), today))
        assertEquals(
            PeriodChoice(FarmPeriod.FULL_YEAR, 2026),
            HomeLogic.farmPeriod(listOf(cycle(Season.RABI, 2026, LocalDate.of(2026, 11, 1), active = false)), today),
        )
        assertEquals(2027, HomeLogic.farmPeriod(listOf(cycle(Season.SUMMER, 2027, LocalDate.of(2027, 3, 5))), today).seasonYear)
    }

    // ---------------- upcoming + rows

    @Test fun upcomingTakesAtMostThreeAfterToday() {
        val day = GujaratiPanchangHelper.day(today)
        val hits = setOf(today, today.plusDays(1), today.plusDays(3), today.plusDays(5), today.plusDays(9))
        val out = HomeLogic.upcoming(today) { d -> if (d in hits) SpecialDay(d, day, setOf(PanchangAlertType.BIJ), emptyList(), null) else null }
        assertEquals(listOf(today.plusDays(1), today.plusDays(3), today.plusDays(5)), out.map { it.date })
    }

    @Test fun recentRowTitleMetaAndKind() {
        val names = LedgerNames(farms = mapOf(3L to "ખેતર 3"), families = mapOf(1L to "પરિવાર A"), categories = mapOf(9L to "ડુંગળી વેચાણ"))
        val label: (AccountSection) -> String = { it.name }
        val sale = HomeLogic.row(txn(AccountSection.FARM, TxnType.INCOME, 48_800, farmId = 3, categoryId = 9), names, label)
        assertEquals("ડુંગળી વેચાણ · ખેતર 3", sale.title)
        assertEquals("02-11 · આસો વદ આઠમ", sale.meta)
        assertEquals(AmountKind.INCOME, sale.kind)
        assertEquals(VkIcon.INCOME, sale.icon)

        val adv = HomeLogic.row(txn(AccountSection.FAMILY, TxnType.ADVANCE, 5_000, familyId = 1), names, label)
        assertEquals("FAMILY · પરિવાર A", adv.title)
        assertEquals(VkIcon.FAMILY, adv.icon)

        val pending = HomeLogic.row(txn(AccountSection.GENERAL, TxnType.EXPENSE, 100, pay = PayStatus.PENDING_PAYABLE, counterparty = "PGVCL"), names, label)
        assertEquals("GENERAL · PGVCL", pending.title)
        assertEquals(AmountKind.PENDING, pending.kind)
        assertTrue(pending.pending)
    }

    @Test fun entryRouteCarriesPresets() {
        assertEquals("entry?kind=income", HomeRoutes.entry(HomeRoutes.KIND_INCOME))
        assertEquals("entry?kind=expense&section=PERSONAL", HomeRoutes.entry(HomeRoutes.KIND_EXPENSE, AccountSection.PERSONAL))
        assertEquals("entry?kind=expense&section=FAMILY&family=2", HomeRoutes.entry("expense", AccountSection.FAMILY, familyId = 2))
        assertEquals("farm/5?period=RABI&year=2026", HomeRoutes.farmDetail(5, FarmPeriod.RABI, 2026))
    }
}
