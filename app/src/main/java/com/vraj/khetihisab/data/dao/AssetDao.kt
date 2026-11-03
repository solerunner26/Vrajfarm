package com.vraj.khetihisab.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.vraj.khetihisab.data.entity.AssetEntity
import com.vraj.khetihisab.data.entity.AssetLoanEntity
import com.vraj.khetihisab.data.model.AssetWithStatus
import com.vraj.khetihisab.data.model.LoanWithAsset
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

/** Asset inventory + lending (doc 11). Loans of soft-deleted assets stay visible in history. */
@Dao
interface AssetDao {
    // ---------------- assets ----------------
    @Insert suspend fun insertAsset(a: AssetEntity): Long

    @Update suspend fun updateAsset(a: AssetEntity)

    @Query("UPDATE asset SET isDeleted=1, updatedAt=:now WHERE id=:id")
    suspend fun softDeleteAsset(id: Long, now: Long)

    @Query("SELECT * FROM asset WHERE id=:id")
    suspend fun assetById(id: Long): AssetEntity?

    @Query(
        """SELECT a.*, (SELECT COUNT(*) FROM asset_loan l WHERE l.assetId=a.id AND l.returnedAt IS NULL) > 0 AS isLent
    FROM asset a WHERE a.isDeleted=0 ORDER BY a.name""",
    )
    fun inventory(): Flow<List<AssetWithStatus>>

    // ---------------- loans ----------------
    @Insert suspend fun insertLoan(l: AssetLoanEntity): Long

    @Update suspend fun updateLoan(l: AssetLoanEntity)

    @Transaction
    @Query("SELECT * FROM asset_loan WHERE id=:id")
    suspend fun loanById(id: Long): LoanWithAsset?

    @Transaction
    @Query("SELECT * FROM asset_loan WHERE returnedAt IS NULL ORDER BY expectedReturnDate")
    fun activeLoans(): Flow<List<LoanWithAsset>>

    @Query("SELECT COUNT(*) FROM asset_loan WHERE returnedAt IS NULL")
    fun activeLoanCount(): Flow<Int>

    /** For the "પહેલેથી ઉછીનું આપેલ છે" warning and the delete guard ("પહેલાં પરત મળ્યું કરો"). */
    @Query("SELECT COUNT(*) FROM asset_loan WHERE assetId=:assetId AND returnedAt IS NULL")
    suspend fun activeLoanCountFor(assetId: Long): Int

    @Transaction
    @Query("SELECT * FROM asset_loan WHERE returnedAt IS NULL AND expectedReturnDate < :today ORDER BY expectedReturnDate")
    fun overdueLoans(today: LocalDate): Flow<List<LoanWithAsset>>

    @Transaction
    @Query("SELECT * FROM asset_loan WHERE returnedAt IS NULL AND expectedReturnDate < :today")
    suspend fun overdueLoansNow(today: LocalDate): List<LoanWithAsset>

    @Transaction
    @Query("SELECT * FROM asset_loan WHERE assetId=:assetId ORDER BY handedOverDate DESC, id DESC")
    fun loansForAsset(assetId: Long): Flow<List<LoanWithAsset>>

    @Query("UPDATE asset_loan SET returnedAt=:at, updatedAt=:at WHERE id=:id")
    suspend fun markReturned(id: Long, at: Long)

    @Query("UPDATE asset_loan SET lastWhatsAppReminderAt=:at WHERE id=:id")
    suspend fun markReminded(id: Long, at: Long)

    /**
     * History search: borrower name, asset name, or exact handover date.
     * The repository converts a dd-MM-yyyy query into [epochDay] (null otherwise).
     */
    @Transaction
    @Query(
        """SELECT l.* FROM asset_loan l JOIN asset a ON a.id=l.assetId
    WHERE l.borrowerName LIKE '%'||:q||'%' OR a.name LIKE '%'||:q||'%' OR l.handedOverDate=:epochDay
    ORDER BY l.handedOverDate DESC, l.id DESC""",
    )
    fun searchHistory(q: String, epochDay: Long?): Flow<List<LoanWithAsset>>

    /** Full history (blank search), newest first. */
    @Transaction
    @Query("SELECT * FROM asset_loan ORDER BY handedOverDate DESC, id DESC")
    fun allLoans(): Flow<List<LoanWithAsset>>

    /** Autocomplete chips: previous borrowers, most recent first, one row per name. */
    @Query(
        """SELECT borrowerName, borrowerPhone, MAX(handedOverDate) lastDate FROM asset_loan
    GROUP BY borrowerName ORDER BY lastDate DESC LIMIT :limit""",
    )
    fun previousBorrowers(limit: Int = 10): Flow<List<BorrowerSuggestion>>
}

data class BorrowerSuggestion(
    val borrowerName: String,
    val borrowerPhone: String?,
    val lastDate: Long,
)
