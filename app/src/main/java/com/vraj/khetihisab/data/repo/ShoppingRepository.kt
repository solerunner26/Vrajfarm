package com.vraj.khetihisab.data.repo

import androidx.room.withTransaction
import com.vraj.khetihisab.data.AppDatabase
import com.vraj.khetihisab.data.entity.AssetEntity
import com.vraj.khetihisab.data.entity.ShopTab
import com.vraj.khetihisab.data.entity.ShoppingItemEntity
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

/** Shopping list (doc 11). PERSONAL-tab items never count in farm reports. */
class ShoppingRepository(
    private val db: AppDatabase,
    private val clock: () -> Long = System::currentTimeMillis,
    private val listener: DataChangeListener = DataChangeListener.NONE,
) {
    private val dao = db.shoppingDao()
    private val assetDao = db.assetDao()

    fun active(tab: ShopTab): Flow<List<ShoppingItemEntity>> = dao.active(tab)
    fun activeCount(): Flow<Int> = dao.activeCount()
    suspend fun activeNow(): List<ShoppingItemEntity> = dao.activeNow()
    fun bought(tab: ShopTab): Flow<List<ShoppingItemEntity>> = dao.bought(tab)

    suspend fun add(
        tab: ShopTab,
        name: String,
        forDate: LocalDate,
        farmId: Long? = null,
        estPricePaise: Long? = null,
        isPhysicalTool: Boolean = false,
    ): Long {
        val now = clock()
        return dao.insert(
            ShoppingItemEntity(
                tab = tab,
                name = name.trim(),
                forDate = forDate,
                // Personal items are never tied to a farm.
                farmId = if (tab == ShopTab.FARM) farmId else null,
                estPricePaise = estPricePaise,
                isPhysicalTool = isPhysicalTool,
                createdAt = now,
                updatedAt = now,
            ),
        ).also { listener.onDataChanged() }
    }

    suspend fun setBought(id: Long, bought: Boolean) {
        dao.setBought(id, bought, if (bought) clock() else null)
        listener.onDataChanged()
    }

    suspend fun update(i: ShoppingItemEntity) {
        dao.update(i.copy(updatedAt = clock()))
        listener.onDataChanged()
    }

    suspend fun softDelete(id: Long) {
        dao.softDelete(id, clock())
        listener.onDataChanged()
    }

    /** "હા, ઉમેરો": creates the asset and links it to the shopping item in one transaction. */
    suspend fun convertToAsset(
        itemId: Long,
        name: String,
        pricePaise: Long?,
        purchaseDate: LocalDate?,
        iconKey: String = "asset",
    ): Long {
        val now = clock()
        val assetId = db.withTransaction {
            val id = assetDao.insertAsset(
                AssetEntity(
                    name = name.trim(),
                    iconKey = iconKey,
                    purchaseDate = purchaseDate,
                    pricePaise = pricePaise,
                    photoPath = null,
                    note = null,
                    createdAt = now,
                    updatedAt = now,
                ),
            )
            dao.markConverted(itemId, id)
            id
        }
        listener.onDataChanged()
        return assetId
    }
}
