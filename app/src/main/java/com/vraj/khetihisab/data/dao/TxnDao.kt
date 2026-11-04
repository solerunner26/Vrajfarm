package com.vraj.khetihisab.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.vraj.khetihisab.data.entity.TransactionEntity
import com.vraj.khetihisab.data.entity.AccountSection
import com.vraj.khetihisab.data.model.CategoryTotal
import com.vraj.khetihisab.data.model.DayTotal
import com.vraj.khetihisab.data.model.FarmPnlRow
import com.vraj.khetihisab.data.model.MonthTotal
import com.vraj.khetihisab.data.model.SectionTotals
import com.vraj.khetihisab.data.model.Totals
import com.vraj.khetihisab.data.model.UdhaarSummary
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

/**
 * Ledger DAO. Every query filters isDeleted = 0.
 *
 * ISOLATION: farm queries ([farmPnl], [farmTotals], [unassignedFarmTotals]) filter section='FARM' only.
 * The Home udhaar card ([pendingUdhaar], [pendingList]) excludes PERSONAL.
 * Personal-only reads are prefixed `personal`.
 */
@Dao
interface TxnDao {
    @Insert suspend fun insert(t: TransactionEntity): Long

    @Update suspend fun update(t: TransactionEntity)

    @Query("UPDATE txn SET isDeleted=1, updatedAt=:now WHERE id=:id")
    suspend fun softDelete(id: Long, now: Long)

    /** Undo for [softDelete] (the "પાછું લો" snackbar). */
    @Query("UPDATE txn SET isDeleted=0, updatedAt=:now WHERE id=:id")
    suspend fun restore(id: Long, now: Long)

    @Query("SELECT * FROM txn WHERE id=:id AND isDeleted=0")
    suspend fun byId(id: Long): TransactionEntity?

    @Query("SELECT * FROM txn WHERE isDeleted=0 ORDER BY date DESC, createdAt DESC LIMIT :limit")
    fun recent(limit: Int = 20): Flow<List<TransactionEntity>>

    /** All non-deleted rows in a range, any section (daily ledger report #1). */
    @Query("SELECT * FROM txn WHERE isDeleted=0 AND date BETWEEN :from AND :to ORDER BY date, section, createdAt")
    fun entriesInRange(from: LocalDate, to: LocalDate): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM txn WHERE isDeleted=0 AND date BETWEEN :from AND :to ORDER BY date, section, createdAt")
    suspend fun entriesInRangeNow(from: LocalDate, to: LocalDate): List<TransactionEntity>

    // ---------------- FARM P&L — the only P&L source. PERSONAL is excluded by design. ----------------

    @Query(
        """SELECT f.id farmId, f.name, f.areaInVigha,
      COALESCE(SUM(CASE WHEN t.type='INCOME'  THEN t.amountPaise END),0) incomePaise,
      COALESCE(SUM(CASE WHEN t.type='EXPENSE' THEN t.amountPaise END),0) expensePaise
    FROM farm f LEFT JOIN txn t ON t.farmId=f.id AND t.isDeleted=0 AND t.section='FARM'
      AND t.date BETWEEN :from AND :to
    GROUP BY f.id ORDER BY f.sortOrder""",
    )
    fun farmPnl(from: LocalDate, to: LocalDate): Flow<List<FarmPnlRow>>

    @Query(
        """SELECT COALESCE(SUM(CASE WHEN type='INCOME' THEN amountPaise END),0) income,
      COALESCE(SUM(CASE WHEN type='EXPENSE' THEN amountPaise END),0) expense
    FROM txn WHERE isDeleted=0 AND section='FARM' AND date BETWEEN :from AND :to""",
    )
    fun farmTotals(from: LocalDate, to: LocalDate): Flow<Totals>

    /** FARM rows whose farm was deleted (farmId set to NULL) — reports show them as "ખેતર કાઢી નાખેલ". */
    @Query(
        """SELECT COALESCE(SUM(CASE WHEN type='INCOME' THEN amountPaise END),0) income,
      COALESCE(SUM(CASE WHEN type='EXPENSE' THEN amountPaise END),0) expense
    FROM txn WHERE isDeleted=0 AND section='FARM' AND farmId IS NULL AND date BETWEEN :from AND :to""",
    )
    fun unassignedFarmTotals(from: LocalDate, to: LocalDate): Flow<Totals>

    // ---------------- ANIMAL HUSBANDRY (પશુપાલન) — own card, counted in business P&L ----------------

    @Query(
        """SELECT COALESCE(SUM(CASE WHEN type='INCOME' THEN amountPaise END),0) income,
      COALESCE(SUM(CASE WHEN type='EXPENSE' THEN amountPaise END),0) expense
    FROM txn WHERE isDeleted=0 AND section='ANIMAL' AND date BETWEEN :from AND :to""",
    )
    fun animalTotals(from: LocalDate, to: LocalDate): Flow<Totals>

    @Query(
        """SELECT c.name, SUM(t.amountPaise) total FROM txn t JOIN category_master c ON c.id=t.categoryId
    WHERE t.isDeleted=0 AND t.section='ANIMAL' AND t.type='EXPENSE' AND t.date BETWEEN :from AND :to
    GROUP BY c.id ORDER BY total DESC""",
    )
    fun animalByCategory(from: LocalDate, to: LocalDate): Flow<List<CategoryTotal>>

    // ---------------- BUSINESS P&L — every section except PERSONAL ----------------
    // income  = INCOME in FARM + ANIMAL (+ any other non-personal section)
    // expense = EXPENSE in FARM + ANIMAL + GENERAL, plus FAMILY ADVANCE (farm managers' salary/ઉપાડ)

    @Query(
        """SELECT COALESCE(SUM(CASE WHEN type='INCOME' THEN amountPaise END),0) income,
      COALESCE(SUM(CASE WHEN type IN ('EXPENSE','ADVANCE') THEN amountPaise END),0) expense
    FROM txn WHERE isDeleted=0 AND section!='PERSONAL' AND date BETWEEN :from AND :to""",
    )
    fun businessTotals(from: LocalDate, to: LocalDate): Flow<Totals>

    @Query(
        """SELECT section,
      COALESCE(SUM(CASE WHEN type='INCOME' THEN amountPaise END),0) incomePaise,
      COALESCE(SUM(CASE WHEN type IN ('EXPENSE','ADVANCE') THEN amountPaise END),0) expensePaise
    FROM txn WHERE isDeleted=0 AND section!='PERSONAL' AND date BETWEEN :from AND :to
    GROUP BY section""",
    )
    fun businessBySection(from: LocalDate, to: LocalDate): Flow<List<SectionTotals>>

    // ---------------- GENERAL (own card, never allocated to farms) ----------------

    @Query(
        """SELECT COALESCE(SUM(amountPaise),0) FROM txn WHERE isDeleted=0 AND section='GENERAL'
      AND type='EXPENSE' AND date BETWEEN :from AND :to""",
    )
    fun generalExpense(from: LocalDate, to: LocalDate): Flow<Long>

    // ---------------- any one section (Samanya / Personal screens, calendar) ----------------

    /** Totals of one section. For GENERAL this is never a farm P&L; PERSONAL stays isolated by the caller. */
    @Query(
        """SELECT COALESCE(SUM(CASE WHEN type='INCOME' THEN amountPaise END),0) income,
      COALESCE(SUM(CASE WHEN type IN ('EXPENSE','ADVANCE') THEN amountPaise END),0) expense
    FROM txn WHERE isDeleted=0 AND section=:section AND date BETWEEN :from AND :to""",
    )
    fun sectionTotals(section: AccountSection, from: LocalDate, to: LocalDate): Flow<Totals>

    @Query(
        """SELECT c.name, SUM(t.amountPaise) total FROM txn t JOIN category_master c ON c.id=t.categoryId
    WHERE t.isDeleted=0 AND t.section=:section AND t.type IN ('EXPENSE','ADVANCE') AND t.date BETWEEN :from AND :to
    GROUP BY c.id ORDER BY total DESC""",
    )
    fun sectionByCategory(section: AccountSection, from: LocalDate, to: LocalDate): Flow<List<CategoryTotal>>

    @Query(
        """SELECT COALESCE(SUM(CASE WHEN payStatus='PENDING_PAYABLE' THEN amountPaise END),0) payable,
      COALESCE(SUM(CASE WHEN payStatus='PENDING_RECEIVABLE' THEN amountPaise END),0) receivable,
      COUNT(CASE WHEN payStatus!='PAID' THEN 1 END) count
    FROM txn WHERE isDeleted=0 AND section=:section""",
    )
    fun sectionUdhaar(section: AccountSection): Flow<UdhaarSummary>

    /** Per-day income / outgoing (expense + ઉપાડ) for the Panchang calendar colours. All sections. */
    @Query(
        """SELECT date,
      COALESCE(SUM(CASE WHEN type='INCOME' THEN amountPaise END),0) income,
      COALESCE(SUM(CASE WHEN type IN ('EXPENSE','ADVANCE') THEN amountPaise END),0) expense
    FROM txn WHERE isDeleted=0 AND date BETWEEN :from AND :to GROUP BY date""",
    )
    fun dailyTotals(from: LocalDate, to: LocalDate): Flow<List<DayTotal>>

    // ---------------- PERSONAL (doc 09) ----------------

    @Query(
        """SELECT COALESCE(SUM(CASE WHEN type='INCOME' THEN amountPaise END),0) income,
      COALESCE(SUM(CASE WHEN type='EXPENSE' THEN amountPaise END),0) expense
    FROM txn WHERE isDeleted=0 AND section='PERSONAL' AND date BETWEEN :from AND :to""",
    )
    fun personalTotals(from: LocalDate, to: LocalDate): Flow<Totals>

    @Query(
        """SELECT c.name, SUM(t.amountPaise) total FROM txn t JOIN category_master c ON c.id=t.categoryId
    WHERE t.isDeleted=0 AND t.section='PERSONAL' AND t.type='EXPENSE' AND t.date BETWEEN :from AND :to
    GROUP BY c.id ORDER BY total DESC""",
    )
    fun personalByCategory(from: LocalDate, to: LocalDate): Flow<List<CategoryTotal>>

    @Query("SELECT * FROM txn WHERE isDeleted=0 AND section='PERSONAL' ORDER BY date DESC, createdAt DESC LIMIT :limit")
    fun personalRecent(limit: Int = 20): Flow<List<TransactionEntity>>

    /** Personal udhaar card inside doc 09 only. */
    @Query(
        """SELECT COALESCE(SUM(CASE WHEN payStatus='PENDING_PAYABLE' THEN amountPaise END),0) payable,
      COALESCE(SUM(CASE WHEN payStatus='PENDING_RECEIVABLE' THEN amountPaise END),0) receivable,
      COUNT(CASE WHEN payStatus!='PAID' THEN 1 END) count
    FROM txn WHERE isDeleted=0 AND section='PERSONAL'""",
    )
    fun personalUdhaar(): Flow<UdhaarSummary>

    @Query("SELECT * FROM txn WHERE isDeleted=0 AND section='PERSONAL' AND payStatus!='PAID' ORDER BY date")
    fun personalPendingList(): Flow<List<TransactionEntity>>

    // ---------------- FAMILY A / B (filtered by familyId) ----------------

    @Query(
        """SELECT COALESCE(SUM(amountPaise),0) FROM txn WHERE isDeleted=0 AND section='FAMILY' AND type='ADVANCE'
    AND familyId=:familyId AND date BETWEEN :from AND :to""",
    )
    fun familyAdvanceTotal(familyId: Long, from: LocalDate, to: LocalDate): Flow<Long>

    /** Month-wise advances (report #4). date is epochDay, so *86400 gives unix seconds. */
    @Query(
        """SELECT strftime('%Y-%m', date * 86400, 'unixepoch') yearMonth, SUM(amountPaise) totalPaise
    FROM txn WHERE isDeleted=0 AND section='FAMILY' AND type='ADVANCE'
      AND familyId=:familyId AND date BETWEEN :from AND :to
    GROUP BY yearMonth ORDER BY yearMonth""",
    )
    fun familyAdvanceByMonth(familyId: Long, from: LocalDate, to: LocalDate): Flow<List<MonthTotal>>

    @Query(
        """SELECT * FROM txn WHERE isDeleted=0 AND section='FAMILY' AND familyId=:familyId
      AND date BETWEEN :from AND :to ORDER BY date DESC""",
    )
    fun familyEntries(familyId: Long, from: LocalDate, to: LocalDate): Flow<List<TransactionEntity>>

    // ---------------- UDHAAR (Home card: FARM + FAMILY + GENERAL, never PERSONAL) ----------------

    @Query(
        """SELECT COALESCE(SUM(CASE WHEN payStatus='PENDING_PAYABLE' THEN amountPaise END),0) payable,
      COALESCE(SUM(CASE WHEN payStatus='PENDING_RECEIVABLE' THEN amountPaise END),0) receivable,
      COUNT(CASE WHEN payStatus!='PAID' THEN 1 END) count
    FROM txn WHERE isDeleted=0 AND section!='PERSONAL'""",
    )
    fun pendingUdhaar(): Flow<UdhaarSummary>

    /**
     * Pending rows for the Home udhaar sheet. Deviation from doc 03: PERSONAL is excluded here so the
     * Home sheet matches the Home card; personal pending rows come from [personalPendingList].
     */
    @Query("SELECT * FROM txn WHERE isDeleted=0 AND payStatus!='PAID' AND section!='PERSONAL' ORDER BY date")
    fun pendingList(): Flow<List<TransactionEntity>>

    /** Every pending row incl. PERSONAL — only for the "ઉધાર બાકી" PDF report (#6). */
    @Query("SELECT * FROM txn WHERE isDeleted=0 AND payStatus!='PAID' ORDER BY date")
    fun pendingListAll(): Flow<List<TransactionEntity>>

    @Query("UPDATE txn SET payStatus='PAID', updatedAt=:now WHERE id=:id")
    suspend fun settle(id: Long, now: Long)
}
