package com.vraj.khetihisab.data.repo

import com.vraj.khetihisab.data.dao.TxnDao
import com.vraj.khetihisab.data.entity.TransactionEntity
import com.vraj.khetihisab.data.model.CategoryTotal
import com.vraj.khetihisab.data.model.FarmPnlRow
import com.vraj.khetihisab.data.model.MonthTotal
import com.vraj.khetihisab.data.model.Totals
import com.vraj.khetihisab.data.model.UdhaarSummary
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.LocalDate
import java.time.YearMonth

/** Called after every write so widgets can refresh (doc 13 WidgetRefresher). */
fun interface DataChangeListener {
    fun onDataChanged()

    companion object {
        val NONE = DataChangeListener { }
    }
}

/**
 * Ledger access for ViewModels (Home, Entry, Farms, Family, Personal, Reports).
 * Farm P&L comes only from FARM-section queries; PERSONAL never reaches them.
 */
class LedgerRepository(
    private val dao: TxnDao,
    private val clock: () -> Long = System::currentTimeMillis,
    private val listener: DataChangeListener = DataChangeListener.NONE,
) {
    // ---- writes ----
    suspend fun insert(t: TransactionEntity): Long = dao.insert(t).also { listener.onDataChanged() }

    suspend fun update(t: TransactionEntity) {
        dao.update(t.copy(updatedAt = clock()))
        listener.onDataChanged()
    }

    suspend fun softDelete(id: Long) {
        dao.softDelete(id, clock())
        listener.onDataChanged()
    }

    suspend fun restore(id: Long) {
        dao.restore(id, clock())
        listener.onDataChanged()
    }

    suspend fun settle(id: Long) {
        dao.settle(id, clock())
        listener.onDataChanged()
    }

    suspend fun byId(id: Long): TransactionEntity? = dao.byId(id)

    // ---- lists ----
    fun recent(limit: Int = 20): Flow<List<TransactionEntity>> = dao.recent(limit)
    fun entries(range: DateRange): Flow<List<TransactionEntity>> = dao.entriesInRange(range.from, range.to)
    suspend fun entriesNow(range: DateRange): List<TransactionEntity> = dao.entriesInRangeNow(range.from, range.to)

    // ---- farm P&L ----
    fun farmPnl(range: DateRange): Flow<List<FarmPnlRow>> = dao.farmPnl(range.from, range.to)

    /** Farm rows with profit-per-vigha attached (0 when the area is 0), in farm sortOrder. */
    fun farmPnlStats(range: DateRange): Flow<List<FarmPnlStat>> =
        farmPnl(range).map { rows -> rows.map { FarmPnlStat(it, LedgerMath.profitPerVigha(it)) } }

    /** Vigha scorecard: best profit-per-vigha first, farms without area last. */
    fun vighaRanking(range: DateRange): Flow<List<FarmPnlStat>> =
        farmPnl(range).map(LedgerMath::rankByProfitPerVigha)

    fun profitPerVigha(row: FarmPnlRow): Long = LedgerMath.profitPerVigha(row)

    fun farmTotals(range: DateRange): Flow<Totals> = dao.farmTotals(range.from, range.to)
    fun unassignedFarmTotals(range: DateRange): Flow<Totals> = dao.unassignedFarmTotals(range.from, range.to)

    // ---- animal husbandry (પશુપાલન) ----
    fun animalTotals(range: DateRange): Flow<Totals> = dao.animalTotals(range.from, range.to)
    fun animalByCategory(range: DateRange): Flow<List<CategoryTotal>> = dao.animalByCategory(range.from, range.to)

    // ---- business P&L: everything except PERSONAL ----
    /** Overall profit/loss: farm + animal husbandry + general + farm managers' advances. */
    fun businessTotals(range: DateRange): Flow<Totals> = dao.businessTotals(range.from, range.to)
    fun businessBySection(range: DateRange): Flow<List<com.vraj.khetihisab.data.model.SectionTotals>> =
        dao.businessBySection(range.from, range.to)

    // ---- general ----
    fun generalExpense(range: DateRange): Flow<Long> = dao.generalExpense(range.from, range.to)

    // ---- personal (doc 09) ----
    fun personalTotals(range: DateRange): Flow<Totals> = dao.personalTotals(range.from, range.to)
    fun personalByCategory(range: DateRange): Flow<List<CategoryTotal>> = dao.personalByCategory(range.from, range.to)
    fun personalRecent(limit: Int = 20): Flow<List<TransactionEntity>> = dao.personalRecent(limit)
    fun personalUdhaar(): Flow<UdhaarSummary> = dao.personalUdhaar()
    fun personalPendingList(): Flow<List<TransactionEntity>> = dao.personalPendingList()

    // ---- family A / B ----
    fun familyAdvanceTotal(familyId: Long, range: DateRange): Flow<Long> =
        dao.familyAdvanceTotal(familyId, range.from, range.to)

    fun familyAdvanceForMonth(familyId: Long, ym: YearMonth): Flow<Long> =
        familyAdvanceTotal(familyId, DateRange.month(ym))

    fun familyAdvanceForYear(familyId: Long, year: Int): Flow<Long> =
        familyAdvanceTotal(familyId, DateRange.year(year))

    /** All 12 months of [year] (0 for months without advances) — report #4. */
    fun familyAdvanceByMonth(familyId: Long, year: Int): Flow<Map<YearMonth, Long>> {
        val r = DateRange.year(year)
        return dao.familyAdvanceByMonth(familyId, r.from, r.to).map { fillYearMonths(year, it) }
    }

    fun familyAdvanceBuckets(familyId: Long, range: DateRange): Flow<List<MonthTotal>> =
        dao.familyAdvanceByMonth(familyId, range.from, range.to)

    fun familyEntries(familyId: Long, range: DateRange): Flow<List<TransactionEntity>> =
        dao.familyEntries(familyId, range.from, range.to)

    // ---- udhaar ----
    /** Home card: FARM + FAMILY + GENERAL only. */
    fun pendingUdhaar(): Flow<UdhaarSummary> = dao.pendingUdhaar()

    /** Home sheet rows: excludes PERSONAL. */
    fun pendingList(): Flow<List<TransactionEntity>> = dao.pendingList()

    /** Report #6 only: includes PERSONAL. */
    fun pendingListAll(): Flow<List<TransactionEntity>> = dao.pendingListAll()

    companion object {
        fun today(): LocalDate = LocalDate.now(LedgerMath.IST)
    }
}
