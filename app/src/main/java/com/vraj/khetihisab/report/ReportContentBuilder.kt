package com.vraj.khetihisab.report

import com.vraj.khetihisab.data.entity.AccountSection
import com.vraj.khetihisab.data.entity.PayStatus
import com.vraj.khetihisab.data.entity.TransactionEntity
import com.vraj.khetihisab.data.entity.TxnType
import com.vraj.khetihisab.data.model.AssetWithStatus
import com.vraj.khetihisab.data.model.FarmPnlRow
import com.vraj.khetihisab.data.model.LoanWithAsset
import com.vraj.khetihisab.data.model.MonthTotal
import com.vraj.khetihisab.data.model.Totals
import com.vraj.khetihisab.data.repo.DateRange
import com.vraj.khetihisab.data.repo.FarmPnlStat
import java.time.LocalDate
import java.time.YearMonth
import java.time.temporal.ChronoUnit
import com.vraj.khetihisab.report.ReportFormat as F
import com.vraj.khetihisab.report.ReportLabels as L

/** Id -> display name lookups for ledger rows. */
data class LedgerLookups(
    val farms: Map<Long, String> = emptyMap(),
    val families: Map<Long, String> = emptyMap(),
    val categories: Map<Long, String> = emptyMap(),
)

/** A family column in report #4. */
data class FamilyColumn(val familyId: Long, val label: String)

/**
 * Pure builders: plain data in, [ReportDoc] out. No Android, no I/O — unit-tested on the JVM.
 * PERSONAL rows only appear in the daily ledger (own section), the personal report and the udhaar report;
 * the farm reports take FARM-only DAO results.
 */
object ReportContentBuilder {

    private val SECTION_ORDER = listOf(
        AccountSection.FARM, AccountSection.ANIMAL, AccountSection.FAMILY, AccountSection.GENERAL, AccountSection.PERSONAL,
    )

    /** Label of the business P&L total row (every section except PERSONAL). */
    internal const val BUSINESS_TOTAL_LABEL = "કુલ (અંગત સિવાય)"

    // ---------------------------------------------------------------- #1 daily ledger
    fun dailyLedger(range: DateRange, entries: List<TransactionEntity>, lookups: LedgerLookups): ReportDoc {
        val multiDay = range.from != range.to
        val blocks = mutableListOf<ReportBlock>()
        val bySection = entries.groupBy { it.section }

        for (section in SECTION_ORDER) {
            val rows = bySection[section].orEmpty()
            if (rows.isEmpty()) continue
            blocks += ReportBlock.Heading("${F.section(section)} (${rows.size})")
            val columns = buildList {
                if (multiDay) add(Column(L.DATE, 1.3f))
                add(Column(L.DETAIL, 3.2f))
                add(Column(refHeader(section), 1.4f))
                add(Column(L.TYPE, 0.9f))
                add(Column(L.STATUS, 1.3f))
                add(Column(L.AMOUNT, 1.5f, Align.END))
            }
            val body = rows.map { t ->
                Row(
                    buildList {
                        if (multiDay) add("${F.date(t.date)}\n${t.tithiLabel}")
                        add(detail(t, lookups))
                        add(reference(t, lookups))
                        add(F.type(t.type))
                        add(F.payStatus(t.payStatus))
                        add(F.rupees(t.amountPaise))
                    },
                    if (t.payStatus != PayStatus.PAID) RowStyle.HIGHLIGHT else RowStyle.NORMAL,
                )
            }
            val totals = TxnType.entries.mapNotNull { type ->
                val sum = rows.filter { it.type == type }.sumOf { it.amountPaise }
                if (sum == 0L) null else totalRow(columns.size, "${L.TOTAL} ${F.type(type)}", F.rupees(sum))
            }
            blocks += ReportBlock.Table(columns, body, totals)
            blocks += ReportBlock.Spacer()
        }

        if (entries.isEmpty()) {
            blocks += ReportBlock.Paragraph(L.NO_ENTRIES, muted = true)
        } else {
            blocks += ReportBlock.Heading(L.DAY_SUMMARY)
            val cols = listOf(
                Column(L.SECTION, 1.6f),
                Column(L.INCOME, 1.4f, Align.END),
                Column(L.EXPENSE, 1.4f, Align.END),
                Column(L.ADVANCE, 1.4f, Align.END),
                Column(L.NET, 1.4f, Align.END),
            )
            val summary = SECTION_ORDER.mapNotNull { s ->
                val rows = bySection[s].orEmpty()
                if (rows.isEmpty()) null else sectionSummaryRow(F.section(s), rows)
            }.toMutableList()
            // Business P&L: FARM + ANIMAL + FAMILY + GENERAL. PERSONAL never counts.
            val business = entries.filter { com.vraj.khetihisab.data.repo.LedgerMath.countsInBusinessPnl(it.section) }
            if (business.isNotEmpty()) summary += sectionSummaryRow(BUSINESS_TOTAL_LABEL, business)
            blocks += ReportBlock.Table(cols, summary)
            if (bySection.containsKey(AccountSection.PERSONAL)) {
                blocks += ReportBlock.Paragraph(L.PERSONAL_NOTE, muted = true)
            }
        }
        return ReportDoc(ReportType.DAILY_LEDGER, ReportType.DAILY_LEDGER.titleGu, F.range(range), blocks)
    }

    private fun sectionSummaryRow(label: String, rows: List<TransactionEntity>): Row {
        fun sum(t: TxnType) = rows.filter { it.type == t }.sumOf { it.amountPaise }
        val inc = sum(TxnType.INCOME)
        val exp = sum(TxnType.EXPENSE)
        val adv = sum(TxnType.ADVANCE)
        return Row(listOf(label, F.rupees(inc), F.rupees(exp), F.rupees(adv), F.rupees(inc - exp - adv)))
    }

    private fun refHeader(s: AccountSection) = when (s) {
        AccountSection.FARM -> L.FARM
        AccountSection.FAMILY -> "ભાગિયા"
        else -> L.PERSON
    }

    private fun reference(t: TransactionEntity, lk: LedgerLookups): String = when (t.section) {
        AccountSection.FARM -> t.farmId?.let { lk.farms[it] } ?: L.DELETED_FARM.takeIf { t.farmId == null } ?: ""
        AccountSection.FAMILY -> t.familyId?.let { lk.families[it] } ?: ""
        else -> t.counterparty.orEmpty()
    }

    /** Category · counterparty · APMC maths · note — joined, wrapped by StaticLayout. */
    internal fun detail(t: TransactionEntity, lk: LedgerLookups): String = buildList {
        t.categoryId?.let { lk.categories[it] }?.let(::add)
        if (t.section == AccountSection.FARM || t.section == AccountSection.FAMILY) {
            t.counterparty?.takeIf { it.isNotBlank() }?.let(::add)
        }
        if (t.mann != null && t.ratePerMannPaise != null) {
            val totalMann = t.mann + (t.extraKg ?: 0.0) / 20.0
            add(L.APMC.format(F.mann(totalMann), F.rupees(t.ratePerMannPaise)))
        }
        t.note?.takeIf { it.isNotBlank() }?.let(::add)
    }.joinToString(" · ").ifEmpty { F.type(t.type) }

    private fun totalRow(n: Int, label: String, amount: String): Row =
        Row(List(n) { i -> if (i == 0) label else if (i == n - 1) amount else "" }, RowStyle.TOTAL)

    // ---------------------------------------------------------------- #2 farm P&L
    fun farmPnl(range: DateRange, rows: List<FarmPnlRow>, unassigned: Totals, grand: Totals, generalExpensePaise: Long): ReportDoc {
        val cols = listOf(
            Column(L.FARM, 2.2f),
            Column(L.VIGHA, 0.9f, Align.END),
            Column(L.INCOME, 1.6f, Align.END),
            Column(L.EXPENSE, 1.6f, Align.END),
            Column(L.NET, 1.6f, Align.END),
        )
        val body = rows.map { r ->
            Row(listOf(r.name, F.vigha(r.areaInVigha), F.rupees(r.incomePaise), F.rupees(r.expensePaise), F.rupees(r.netPaise)))
        }.toMutableList()
        if (unassigned.income != 0L || unassigned.expense != 0L) {
            body += Row(listOf(L.DELETED_FARM, "", F.rupees(unassigned.income), F.rupees(unassigned.expense), F.rupees(unassigned.net)))
        }
        val totalVigha = rows.sumOf { it.areaInVigha }
        val totals = listOf(
            Row(listOf(L.GRAND_TOTAL, F.vigha(totalVigha), F.rupees(grand.income), F.rupees(grand.expense), F.rupees(grand.net)), RowStyle.TOTAL),
        )
        val blocks = mutableListOf<ReportBlock>(ReportBlock.Table(cols, body, totals))
        if (generalExpensePaise != 0L) {
            blocks += ReportBlock.Spacer()
            blocks += ReportBlock.Paragraph(L.GENERAL_NOTE.format(F.rupees(generalExpensePaise)), muted = true)
        }
        return ReportDoc(ReportType.FARM_PNL, ReportType.FARM_PNL.titleGu, F.range(range), blocks)
    }

    // ---------------------------------------------------------------- #3 vigha scorecard
    fun vighaScorecard(range: DateRange, ranked: List<FarmPnlStat>): ReportDoc {
        val cols = listOf(
            Column(L.RANK, 0.6f, Align.CENTER),
            Column(L.FARM, 2.2f),
            Column(L.VIGHA, 0.9f, Align.END),
            Column(L.NET, 1.6f, Align.END),
            Column(L.PER_VIGHA, 1.8f, Align.END),
        )
        var rank = 0
        val body = ranked.map { s ->
            val hasArea = s.row.areaInVigha > 0.0
            Row(
                listOf(
                    if (hasArea) (++rank).toString() else "-",
                    s.row.name,
                    if (hasArea) F.vigha(s.row.areaInVigha) else "0",
                    F.rupees(s.row.netPaise),
                    if (hasArea) F.rupees(s.profitPerVighaPaise) else L.VIGHA_NOT_SET,
                ),
                if (!hasArea) RowStyle.HIGHLIGHT else RowStyle.NORMAL,
            )
        }
        return ReportDoc(ReportType.VIGHA_SCORECARD, ReportType.VIGHA_SCORECARD.titleGu, F.range(range), listOf(ReportBlock.Table(cols, body)))
    }

    // ---------------------------------------------------------------- #4 family A/B advances
    /** Months of [range] are listed (max [MAX_MONTHS]); a year-total row follows each calendar year. */
    fun familyAdvances(range: DateRange, families: List<FamilyColumn>, buckets: Map<Long, List<MonthTotal>>): ReportDoc {
        val cols = listOf(Column(L.MONTH, 1.8f)) +
            families.map { Column(it.label, 1.4f, Align.END) } +
            Column(L.TOTAL, 1.4f, Align.END)
        val byFamily: Map<Long, Map<YearMonth, Long>> = families.associate { f ->
            f.familyId to buckets[f.familyId].orEmpty().associate { YearMonth.parse(it.yearMonth) to it.totalPaise }
        }
        val months = monthsOf(range)
        val body = mutableListOf<Row>()
        months.groupBy { it.year }.forEach { (year, yms) ->
            yms.forEach { ym ->
                val values = families.map { byFamily[it.familyId]?.get(ym) ?: 0L }
                body += Row(listOf(F.month(ym)) + values.map(F::rupees) + F.rupees(values.sum()))
            }
            val yearValues = families.map { f -> yms.sumOf { byFamily[f.familyId]?.get(it) ?: 0L } }
            body += Row(listOf(L.YEAR_TOTAL.format(year)) + yearValues.map(F::rupees) + F.rupees(yearValues.sum()), RowStyle.TOTAL)
        }
        val totals = if (months.map { it.year }.distinct().size > 1) {
            val all = families.map { f -> months.sumOf { byFamily[f.familyId]?.get(it) ?: 0L } }
            listOf(Row(listOf(L.GRAND_TOTAL) + all.map(F::rupees) + F.rupees(all.sum()), RowStyle.TOTAL))
        } else emptyList()
        return ReportDoc(ReportType.FAMILY_ADVANCES, ReportType.FAMILY_ADVANCES.titleGu, F.range(range), listOf(ReportBlock.Table(cols, body, totals)))
    }

    const val MAX_MONTHS = 120

    internal fun monthsOf(range: DateRange): List<YearMonth> {
        val out = mutableListOf<YearMonth>()
        var ym = YearMonth.from(range.from)
        val end = YearMonth.from(range.to)
        while (!ym.isAfter(end) && out.size < MAX_MONTHS) {
            out += ym
            ym = ym.plusMonths(1)
        }
        return out
    }

    // ---------------------------------------------------------------- #5 personal
    fun personal(range: DateRange, totals: Totals, entries: List<TransactionEntity>, lookups: LedgerLookups): ReportDoc {
        val personal = entries.filter { it.section == AccountSection.PERSONAL }
        val blocks = mutableListOf<ReportBlock>()
        blocks += ReportBlock.Heading(L.SUMMARY)
        blocks += ReportBlock.Table(
            listOf(Column(L.DETAIL, 2f), Column(L.AMOUNT, 1.5f, Align.END)),
            listOf(
                Row(listOf(L.INCOME, F.rupees(totals.income))),
                Row(listOf(L.EXPENSE, F.rupees(totals.expense))),
            ),
            listOf(Row(listOf(L.NET, F.rupees(totals.net)), RowStyle.TOTAL)),
        )
        blocks += ReportBlock.Spacer()
        for ((type, heading) in listOf(TxnType.INCOME to L.INCOME_BY_CATEGORY, TxnType.EXPENSE to L.EXPENSE_BY_CATEGORY)) {
            val ofType = personal.filter { it.type == type }
            if (ofType.isEmpty()) continue
            val whole = ofType.sumOf { it.amountPaise }
            val grouped = ofType.groupBy { it.categoryId?.let(lookups.categories::get) ?: F.type(type) }
                .mapValues { (_, v) -> v.sumOf { it.amountPaise } }
                .entries.sortedByDescending { it.value }
            blocks += ReportBlock.Heading(heading)
            blocks += ReportBlock.Table(
                listOf(Column(L.CATEGORY, 2.4f), Column(L.SHARE, 1f, Align.END), Column(L.AMOUNT, 1.5f, Align.END)),
                grouped.map { Row(listOf(it.key, F.percent(it.value, whole), F.rupees(it.value))) },
                listOf(Row(listOf(L.TOTAL, "100%", F.rupees(whole)), RowStyle.TOTAL)),
            )
            blocks += ReportBlock.Spacer()
        }
        blocks += ReportBlock.Heading(L.ENTRIES)
        blocks += ReportBlock.Table(
            listOf(
                Column(L.DATE, 1.3f),
                Column(L.DETAIL, 3f),
                Column(L.TYPE, 0.9f),
                Column(L.STATUS, 1.3f),
                Column(L.AMOUNT, 1.5f, Align.END),
            ),
            personal.map { t ->
                Row(
                    listOf(F.date(t.date), detailPersonal(t, lookups), F.type(t.type), F.payStatus(t.payStatus), F.rupees(t.amountPaise)),
                    if (t.payStatus != PayStatus.PAID) RowStyle.HIGHLIGHT else RowStyle.NORMAL,
                )
            },
        )
        blocks += ReportBlock.Paragraph(L.PERSONAL_NOTE, muted = true)
        return ReportDoc(ReportType.PERSONAL, ReportType.PERSONAL.titleGu, F.range(range), blocks)
    }

    private fun detailPersonal(t: TransactionEntity, lk: LedgerLookups): String =
        listOfNotNull(
            t.categoryId?.let(lk.categories::get),
            t.counterparty?.takeIf { it.isNotBlank() },
            t.note?.takeIf { it.isNotBlank() },
        ).joinToString(" · ").ifEmpty { F.type(t.type) }

    // ---------------------------------------------------------------- #6 pending udhaar
    fun pendingUdhaar(today: LocalDate, pending: List<TransactionEntity>, lookups: LedgerLookups): ReportDoc {
        val blocks = mutableListOf<ReportBlock>()
        for ((status, heading) in listOf(PayStatus.PENDING_RECEIVABLE to L.RECEIVABLE, PayStatus.PENDING_PAYABLE to L.PAYABLE)) {
            val rows = pending.filter { it.payStatus == status }.sortedBy { it.date }
            val sum = rows.sumOf { it.amountPaise }
            blocks += ReportBlock.Heading("$heading · ${F.rupees(sum)}")
            val cols = listOf(
                Column(L.DATE, 1.3f),
                Column(L.PERSON, 2.8f),
                Column(L.SECTION, 1.1f),
                Column(L.AGE_DAYS, 0.8f, Align.END),
                Column(L.AMOUNT, 1.5f, Align.END),
            )
            blocks += ReportBlock.Table(
                cols,
                rows.map { t ->
                    val age = ageDays(t.date, today)
                    Row(
                        listOf(F.date(t.date), udhaarWho(t, lookups), F.section(t.section), age.toString(), F.rupees(t.amountPaise)),
                        if (age > OLD_UDHAAR_DAYS) RowStyle.HIGHLIGHT else RowStyle.NORMAL,
                    )
                },
                if (rows.isEmpty()) emptyList() else listOf(totalRow(cols.size, L.TOTAL, F.rupees(sum))),
            )
            blocks += ReportBlock.Spacer()
        }
        return ReportDoc(ReportType.PENDING_UDHAAR, ReportType.PENDING_UDHAAR.titleGu, F.date(today), blocks)
    }

    /** Rows older than this are highlighted in report #6. */
    const val OLD_UDHAAR_DAYS = 90

    fun ageDays(from: LocalDate, today: LocalDate): Long = ChronoUnit.DAYS.between(from, today).coerceAtLeast(0)

    private fun udhaarWho(t: TransactionEntity, lk: LedgerLookups): String =
        listOfNotNull(
            t.counterparty?.takeIf { it.isNotBlank() },
            t.familyId?.let(lk.families::get),
            t.farmId?.let(lk.farms::get),
            t.categoryId?.let(lk.categories::get),
            t.note?.takeIf { it.isNotBlank() },
        ).distinct().joinToString(" · ").ifEmpty { F.type(t.type) }

    // ---------------------------------------------------------------- #7 assets & loans
    fun assetsLoans(today: LocalDate, inventory: List<AssetWithStatus>, activeLoans: List<LoanWithAsset>): ReportDoc {
        val blocks = mutableListOf<ReportBlock>()
        blocks += ReportBlock.Heading("${L.INVENTORY} (${inventory.size})")
        blocks += ReportBlock.Table(
            listOf(
                Column(L.ITEM, 2.6f),
                Column(L.PURCHASED, 1.3f),
                Column(L.PRICE, 1.4f, Align.END),
                Column(L.STATUS, 1.3f),
            ),
            inventory.map { a ->
                Row(
                    listOf(
                        a.asset.name,
                        a.asset.purchaseDate?.let(F::date) ?: "-",
                        a.asset.pricePaise?.let(F::rupees) ?: "-",
                        if (a.isLent) L.LENT else L.AVAILABLE,
                    ),
                )
            },
            inventory.mapNotNull { it.asset.pricePaise }.sum().takeIf { it > 0 }
                ?.let { listOf(Row(listOf(L.TOTAL, "", F.rupees(it), ""), RowStyle.TOTAL)) }
                .orEmpty(),
        )
        blocks += ReportBlock.Spacer()

        val overdueCount = activeLoans.count { it.loan.expectedReturnDate.isBefore(today) }
        blocks += ReportBlock.Heading(
            "${L.ACTIVE_LOANS} (${activeLoans.size})" + if (overdueCount > 0) " · ${L.OVERDUE}: $overdueCount" else "",
        )
        blocks += ReportBlock.Table(
            listOf(
                Column(L.PHOTO, 0.9f, Align.CENTER, isImage = true),
                Column(L.PERSON, 1.9f),
                Column(L.ITEM, 1.7f),
                Column(L.HANDED_OVER, 1.4f),
                Column(L.DUE, 1.3f),
                Column(L.LATE_DAYS, 0.9f, Align.END),
            ),
            activeLoans.sortedBy { it.loan.expectedReturnDate }.map { lw ->
                val l = lw.loan
                val late = ChronoUnit.DAYS.between(l.expectedReturnDate, today)
                Row(
                    listOf(
                        l.borrowerPhotoPath.orEmpty(),
                        listOfNotNull(l.borrowerName, l.borrowerPhone?.takeIf { it.isNotBlank() }).joinToString("\n"),
                        lw.asset.name,
                        "${F.date(l.handedOverDate)}\n${l.handedOverTithi}",
                        F.date(l.expectedReturnDate),
                        if (late > 0) late.toString() else "-",
                    ),
                    if (late > 0) RowStyle.HIGHLIGHT else RowStyle.NORMAL,
                )
            },
        )
        return ReportDoc(ReportType.ASSETS_LOANS, ReportType.ASSETS_LOANS.titleGu, F.date(today), blocks)
    }
}
