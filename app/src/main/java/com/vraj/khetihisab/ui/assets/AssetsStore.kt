package com.vraj.khetihisab.ui.assets

import com.vraj.khetihisab.data.dao.BorrowerSuggestion
import com.vraj.khetihisab.data.entity.AssetEntity
import com.vraj.khetihisab.data.entity.AssetLoanEntity
import com.vraj.khetihisab.data.entity.PersonProfileEntity
import com.vraj.khetihisab.data.model.AssetWithStatus
import com.vraj.khetihisab.data.model.LoanWithAsset
import com.vraj.khetihisab.data.repo.AssetRepository
import com.vraj.khetihisab.data.repo.ProfileRepository
import kotlinx.coroutines.flow.Flow

/** What the Assets and Lend ViewModels need from data/. A thin seam so they are JVM-testable. */
interface AssetsStore {
    fun activeLoans(): Flow<List<LoanWithAsset>>
    fun inventory(): Flow<List<AssetWithStatus>>
    fun searchHistory(query: String): Flow<List<LoanWithAsset>>
    fun loansForAsset(assetId: Long): Flow<List<LoanWithAsset>>
    fun previousBorrowers(): Flow<List<BorrowerSuggestion>>
    fun owner(): Flow<PersonProfileEntity?>
    suspend fun loanById(id: Long): LoanWithAsset?
    suspend fun isCurrentlyLent(assetId: Long): Boolean
    suspend fun addAsset(name: String): Long
    suspend fun lend(loan: AssetLoanEntity): Long
    suspend fun markReturned(loanId: Long)
    suspend fun markReminded(loanId: Long)
    /** false = blocked by an active loan ("પહેલાં પરત મળ્યું કરો"). */
    suspend fun deleteAsset(assetId: Long): Boolean
}

class RepoAssetsStore(
    private val assets: AssetRepository,
    private val profiles: ProfileRepository,
    private val clock: () -> Long = System::currentTimeMillis,
) : AssetsStore {
    override fun activeLoans() = assets.activeLoans()
    override fun inventory() = assets.inventory()
    override fun searchHistory(query: String) = assets.searchLoanHistory(query)
    override fun loansForAsset(assetId: Long) = assets.loansForAsset(assetId)
    override fun previousBorrowers() = assets.previousBorrowers(20)
    override fun owner() = profiles.owner()
    override suspend fun loanById(id: Long) = assets.loanById(id)
    override suspend fun isCurrentlyLent(assetId: Long) = assets.isCurrentlyLent(assetId)
    override suspend fun addAsset(name: String): Long {
        val now = clock()
        return assets.addAsset(
            AssetEntity(name = name.trim(), iconKey = "asset", purchaseDate = null, pricePaise = null, photoPath = null, note = null, createdAt = now, updatedAt = now),
        )
    }
    override suspend fun lend(loan: AssetLoanEntity) = assets.lend(loan)
    override suspend fun markReturned(loanId: Long) = assets.markReturned(loanId)
    override suspend fun markReminded(loanId: Long) = assets.markReminded(loanId)
    override suspend fun deleteAsset(assetId: Long) = assets.deleteAsset(assetId) == AssetRepository.DeleteResult.Deleted
}
