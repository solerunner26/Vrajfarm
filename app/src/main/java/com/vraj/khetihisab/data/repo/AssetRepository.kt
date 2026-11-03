package com.vraj.khetihisab.data.repo

import androidx.room.withTransaction
import com.vraj.khetihisab.data.AppDatabase
import com.vraj.khetihisab.data.dao.BorrowerSuggestion
import com.vraj.khetihisab.data.entity.AssetEntity
import com.vraj.khetihisab.data.entity.AssetLoanEntity
import com.vraj.khetihisab.data.entity.Priority
import com.vraj.khetihisab.data.entity.ReminderEntity
import com.vraj.khetihisab.data.entity.ReminderSource
import com.vraj.khetihisab.data.model.AssetWithStatus
import com.vraj.khetihisab.data.model.LoanWithAsset
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate
import java.time.LocalTime

/** Asset inventory + lending (doc 11). */
class AssetRepository(
    private val db: AppDatabase,
    private val clock: () -> Long = System::currentTimeMillis,
    private val listener: DataChangeListener = DataChangeListener.NONE,
) {
    private val dao = db.assetDao()
    private val reminderDao = db.reminderDao()

    sealed interface DeleteResult {
        data object Deleted : DeleteResult
        /** "પહેલાં પરત મળ્યું કરો" */
        data object BlockedByActiveLoan : DeleteResult
    }

    // ---- assets ----
    fun inventory(): Flow<List<AssetWithStatus>> = dao.inventory()
    suspend fun assetById(id: Long): AssetEntity? = dao.assetById(id)

    suspend fun addAsset(a: AssetEntity): Long = dao.insertAsset(a).also { listener.onDataChanged() }

    suspend fun updateAsset(a: AssetEntity) {
        dao.updateAsset(a.copy(updatedAt = clock()))
        listener.onDataChanged()
    }

    /** Soft delete; blocked while a loan is active. History keeps the asset's loans. */
    suspend fun deleteAsset(id: Long): DeleteResult {
        if (dao.activeLoanCountFor(id) > 0) return DeleteResult.BlockedByActiveLoan
        dao.softDeleteAsset(id, clock())
        listener.onDataChanged()
        return DeleteResult.Deleted
    }

    // ---- loans ----
    fun activeLoans(): Flow<List<LoanWithAsset>> = dao.activeLoans()
    fun activeLoanCount(): Flow<Int> = dao.activeLoanCount()
    fun overdueLoans(today: LocalDate): Flow<List<LoanWithAsset>> = dao.overdueLoans(today)
    suspend fun overdueLoansNow(today: LocalDate): List<LoanWithAsset> = dao.overdueLoansNow(today)
    fun loansForAsset(assetId: Long): Flow<List<LoanWithAsset>> = dao.loansForAsset(assetId)
    fun previousBorrowers(limit: Int = 10): Flow<List<BorrowerSuggestion>> = dao.previousBorrowers(limit)
    suspend fun loanById(id: Long): LoanWithAsset? = dao.loanById(id)

    /** For the "પહેલેથી ઉછીનું આપેલ છે" confirm dialog. */
    suspend fun isCurrentlyLent(assetId: Long): Boolean = dao.activeLoanCountFor(assetId) > 0

    /**
     * Loan history search by borrower name, asset name or a dd-MM-yyyy date.
     * A blank query returns the full history.
     */
    fun searchLoanHistory(query: String): Flow<List<LoanWithAsset>> {
        val q = LoanSearchQuery.parse(query)
        return if (q.isBlank) dao.allLoans() else dao.searchHistory(q.text, q.epochDay)
    }

    /**
     * Saves a loan. If [AssetLoanEntity.notifyOnDue], also inserts the HIGH reminder
     * "{asset} પાછું લાવવું" due on the return date at 09:00 IST (sourceType LOAN).
     * Returns the new loan id.
     */
    suspend fun lend(loan: AssetLoanEntity): Long {
        require(!loan.expectedReturnDate.isBefore(loan.handedOverDate)) { "expected return before handover" }
        val now = clock()
        val id = db.withTransaction {
            val loanId = dao.insertLoan(loan.copy(createdAt = now, updatedAt = now))
            if (loan.notifyOnDue) {
                val assetName = dao.assetById(loan.assetId)?.name.orEmpty()
                reminderDao.insert(
                    ReminderEntity(
                        title = "$assetName પાછું લાવવું",
                        note = loan.borrowerName,
                        imagePath = null,
                        priority = Priority.HIGH,
                        dueAt = LedgerMath.istMillis(loan.expectedReturnDate, LocalTime.of(9, 0)),
                        sourceType = ReminderSource.LOAN,
                        sourceId = loanId,
                        createdAt = now,
                        updatedAt = now,
                    ),
                )
            }
            loanId
        }
        listener.onDataChanged()
        return id
    }

    /** "પરત મળ્યું ✓": closes the loan and marks its linked reminder done. */
    suspend fun markReturned(loanId: Long) {
        val now = clock()
        db.withTransaction {
            dao.markReturned(loanId, now)
            reminderDao.markDoneBySource(ReminderSource.LOAN, loanId, now)
        }
        listener.onDataChanged()
    }

    suspend fun markReminded(loanId: Long) {
        dao.markReminded(loanId, clock())
        listener.onDataChanged()
    }
}
