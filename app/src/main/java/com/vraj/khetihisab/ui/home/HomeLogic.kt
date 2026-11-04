package com.vraj.khetihisab.ui.home

import com.vraj.khetihisab.data.entity.AccountSection
import com.vraj.khetihisab.data.entity.FarmCropCycleEntity
import com.vraj.khetihisab.data.entity.PayStatus
import com.vraj.khetihisab.data.entity.Priority
import com.vraj.khetihisab.data.entity.ReminderEntity
import com.vraj.khetihisab.data.entity.TransactionEntity
import com.vraj.khetihisab.data.entity.TxnType
import com.vraj.khetihisab.data.model.LoanWithAsset
import com.vraj.khetihisab.data.model.SectionTotals
import com.vraj.khetihisab.data.model.Totals
import com.vraj.khetihisab.data.model.UdhaarSummary
import com.vraj.khetihisab.data.repo.DateRange
import com.vraj.khetihisab.panchang.SpecialDay
import com.vraj.khetihisab.share.WhatsAppTemplates
import com.vraj.khetihisab.ui.components.AmountKind
import com.vraj.khetihisab.ui.farms.FarmPeriod
import com.vraj.khetihisab.ui.farms.PeriodChoice
import com.vraj.khetihisab.ui.farms.SeasonRange
import com.vraj.khetihisab.ui.theme.VkIcon
import java.time.LocalDate

/** One ledger row as shown on Home (recent list, udhaar sheet). */
data class LedgerRowUi(
    val id: Long,
    val title: String,
    val meta: String,
    val icon: VkIcon,
    val amountPaise: Long,
    val kind: AmountKind,
    val billPath: String?,
    val pending: Boolean,
    val payStatus: PayStatus,
)

data class LoanCardUi(val loan: LoanWithAsset, val daysLate: Int)

data class UrgentUi(val reminder: ReminderEntity, val dueDate: LocalDate)

/** Business P&L card: overall totals + one line per section, PERSONAL never included. */
data class BusinessUi(val totals: Totals, val sections: List<SectionTotals>, val cropYear: Int)

/** Lookup names for building ledger rows. */
data class LedgerNames(
    val farms: Map<Long, String> = emptyMap(),
    val families: Map<Long, String> = emptyMap(),
    val categories: Map<Long, String> = emptyMap(),
)

/** Pure Home rules (doc 05), unit-tested on the JVM. */
object HomeLogic {
    const val MAX_OVERDUE = 2
    const val MAX_URGENT = 2
    const val MAX_RECENT = 6
    const val UPCOMING_DAYS = 14L
    const val MAX_UPCOMING = 3
    private const val URGENT_WINDOW_MS = 48L * 60 * 60 * 1000

    /** Order of the breakdown sheet: crops, animals, general, managers' advances. */
    val BUSINESS_ORDER = listOf(AccountSection.FARM, AccountSection.ANIMAL, AccountSection.GENERAL, AccountSection.FAMILY)

    /** Udhaar card is hidden completely when nothing is pending (doc 05 edge cases). */
    fun showUdhaar(u: UdhaarSummary): Boolean = u.payable > 0L || u.receivable > 0L

    /** At most [MAX_OVERDUE] loan cards; the rest become "વધુ N". */
    fun capOverdue(loans: List<LoanWithAsset>, today: LocalDate): Pair<List<LoanCardUi>, Int> {
        val sorted = loans.sortedBy { it.loan.expectedReturnDate }
        val shown = sorted.take(MAX_OVERDUE).map {
            LoanCardUi(it, WhatsAppTemplates.overdueDays(it.loan.expectedReturnDate, today).toInt())
        }
        return shown to (sorted.size - shown.size)
    }

    /** HIGH reminders due within the next 48 h (already overdue ones too), at most [MAX_URGENT]. */
    fun urgent(list: List<ReminderEntity>, nowMillis: Long): List<UrgentUi> = list
        .filter { it.priority == Priority.HIGH && !it.isDone && !it.isDeleted && it.dueAt <= nowMillis + URGENT_WINDOW_MS }
        .sortedBy { it.dueAt }
        .take(MAX_URGENT)
        .map { UrgentUi(it, java.time.Instant.ofEpochMilli(it.dueAt).atZone(ScreensFormat.IST).toLocalDate()) }

    /** Fixed-order breakdown with zero rows for missing sections; PERSONAL is dropped defensively. */
    fun businessBreakdown(rows: List<SectionTotals>): List<SectionTotals> {
        val by = rows.filter { it.section != AccountSection.PERSONAL }.groupBy { it.section }
        return BUSINESS_ORDER.map { s ->
            val list = by[s].orEmpty()
            SectionTotals(s, list.sumOf { it.incomePaise }, list.sumOf { it.expensePaise })
        }
    }

    /** Sum of the breakdown; equals `businessTotals` for the same range. */
    fun sumOf(sections: List<SectionTotals>): Totals =
        Totals(sections.sumOf { it.incomePaise }, sections.sumOf { it.expensePaise })

    /** Range of the business P&L card: the crop (financial) year Apr–Mar containing [today]. */
    fun businessRange(today: LocalDate): DateRange = DateRange.financialYear(today)

    /**
     * Home farm card period (doc 05 "Data"): the season of the most recently started active cycle;
     * with no active cycle, the full crop year containing [today].
     */
    fun farmPeriod(activeCycles: List<FarmCropCycleEntity>, today: LocalDate): PeriodChoice {
        val latest = activeCycles.filter { it.isActive }.maxByOrNull { it.startDate }
        val p = latest?.let { SeasonRange.fromCycleSeason(it.season) }
        return if (latest != null && p != null) {
            // Cycle "year" is the calendar year the season starts in; for Rabi that is the Nov year.
            PeriodChoice.ofSeason(p, latest.year)
        } else {
            PeriodChoice(FarmPeriod.FULL_YEAR, SeasonRange.cropYearOf(today))
        }
    }

    /** Next special days after [today] (up to [MAX_UPCOMING] within [UPCOMING_DAYS]). */
    fun upcoming(today: LocalDate, special: (LocalDate) -> SpecialDay?): List<SpecialDay> {
        val out = ArrayList<SpecialDay>()
        var d = today.plusDays(1)
        val end = today.plusDays(UPCOMING_DAYS)
        while (!d.isAfter(end) && out.size < MAX_UPCOMING) {
            runCatching { special(d) }.getOrNull()?.let(out::add)
            d = d.plusDays(1)
        }
        return out
    }

    fun iconFor(t: TransactionEntity): VkIcon = when {
        t.type == TxnType.INCOME -> VkIcon.INCOME
        t.section == AccountSection.FAMILY -> VkIcon.FAMILY
        t.section == AccountSection.ANIMAL -> VkIcon.ANIMAL
        t.section == AccountSection.GENERAL -> VkIcon.UTILITY
        t.section == AccountSection.PERSONAL -> VkIcon.PERSONAL
        else -> VkIcon.EXPENSE
    }

    /**
     * Row title "{category} · {farm}" (doc 05 §4). Family rows use the family name, pending rows add
     * the counterparty; [sectionLabel] is the fallback when there is no category.
     */
    fun title(t: TransactionEntity, names: LedgerNames, sectionLabel: (AccountSection) -> String): String {
        val category = t.categoryId?.let(names.categories::get)
        val place = when (t.section) {
            AccountSection.FARM -> t.farmId?.let(names.farms::get)
            AccountSection.FAMILY -> t.familyId?.let(names.families::get)
            else -> null
        }
        val head = category ?: t.note?.trim()?.takeIf { it.isNotEmpty() } ?: sectionLabel(t.section)
        val who = t.counterparty?.trim()?.takeIf { it.isNotEmpty() && t.payStatus != PayStatus.PAID }
        return listOfNotNull(head, place, who).distinct().joinToString(" · ")
    }

    /** "02-11 · આસો વદ આઠમ" */
    fun meta(t: TransactionEntity): String =
        if (t.tithiLabel.isBlank()) ScreensFormat.dm(t.date) else "${ScreensFormat.dm(t.date)} · ${t.tithiLabel}"

    fun row(t: TransactionEntity, names: LedgerNames, sectionLabel: (AccountSection) -> String): LedgerRowUi = LedgerRowUi(
        id = t.id,
        title = title(t, names, sectionLabel),
        meta = meta(t),
        icon = iconFor(t),
        amountPaise = t.amountPaise,
        kind = ScreensFormat.amountKind(t),
        billPath = t.billPhotoPath,
        pending = t.payStatus != PayStatus.PAID,
        payStatus = t.payStatus,
    )
}
