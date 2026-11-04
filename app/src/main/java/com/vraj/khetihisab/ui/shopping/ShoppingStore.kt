package com.vraj.khetihisab.ui.shopping

import com.vraj.khetihisab.data.entity.FarmEntity
import com.vraj.khetihisab.data.entity.ShopTab
import com.vraj.khetihisab.data.entity.ShoppingItemEntity
import com.vraj.khetihisab.data.repo.MasterRepository
import com.vraj.khetihisab.data.repo.ShoppingRepository
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

/** What [ShoppingViewModel] needs from data/. A thin seam so the ViewModel is JVM-testable. */
interface ShoppingStore {
    fun active(tab: ShopTab): Flow<List<ShoppingItemEntity>>
    fun farms(): Flow<List<FarmEntity>>
    suspend fun add(tab: ShopTab, name: String, forDate: LocalDate, farmId: Long?, estPricePaise: Long?, isPhysicalTool: Boolean): Long
    suspend fun setBought(id: Long, bought: Boolean)
    suspend fun convertToAsset(itemId: Long, name: String, pricePaise: Long?, purchaseDate: LocalDate?): Long
}

class RepoShoppingStore(
    private val shopping: ShoppingRepository,
    private val master: MasterRepository,
) : ShoppingStore {
    override fun active(tab: ShopTab) = shopping.active(tab)
    override fun farms() = master.farms()
    override suspend fun add(tab: ShopTab, name: String, forDate: LocalDate, farmId: Long?, estPricePaise: Long?, isPhysicalTool: Boolean) =
        shopping.add(tab, name, forDate, farmId, estPricePaise, isPhysicalTool)
    override suspend fun setBought(id: Long, bought: Boolean) = shopping.setBought(id, bought)
    override suspend fun convertToAsset(itemId: Long, name: String, pricePaise: Long?, purchaseDate: LocalDate?) =
        shopping.convertToAsset(itemId, name, pricePaise, purchaseDate)
}
