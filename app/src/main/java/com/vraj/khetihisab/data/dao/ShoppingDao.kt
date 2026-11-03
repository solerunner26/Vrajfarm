package com.vraj.khetihisab.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.vraj.khetihisab.data.entity.ShopTab
import com.vraj.khetihisab.data.entity.ShoppingItemEntity
import kotlinx.coroutines.flow.Flow

/** Shopping list. PERSONAL-tab items are never part of any farm report. */
@Dao
interface ShoppingDao {
    @Query("SELECT * FROM shopping_item WHERE isDeleted=0 AND isBought=0 AND tab=:tab ORDER BY forDate DESC, id DESC")
    fun active(tab: ShopTab): Flow<List<ShoppingItemEntity>>

    @Query("SELECT COUNT(*) FROM shopping_item WHERE isDeleted=0 AND isBought=0")
    fun activeCount(): Flow<Int>

    /** For the weekly worker / widget (first N names). */
    @Query("SELECT * FROM shopping_item WHERE isDeleted=0 AND isBought=0 ORDER BY forDate DESC, id DESC")
    suspend fun activeNow(): List<ShoppingItemEntity>

    @Query("SELECT * FROM shopping_item WHERE isDeleted=0 AND isBought=1 AND tab=:tab ORDER BY boughtAt DESC")
    fun bought(tab: ShopTab): Flow<List<ShoppingItemEntity>>

    @Query("SELECT * FROM shopping_item WHERE id=:id")
    suspend fun byId(id: Long): ShoppingItemEntity?

    /** bought=false + at=null is the undo path. */
    @Query("UPDATE shopping_item SET isBought=:bought, boughtAt=:at, updatedAt=COALESCE(:at, updatedAt) WHERE id=:id")
    suspend fun setBought(id: Long, bought: Boolean, at: Long?)

    @Insert suspend fun insert(i: ShoppingItemEntity): Long

    @Update suspend fun update(i: ShoppingItemEntity)

    @Query("UPDATE shopping_item SET isDeleted=1, updatedAt=:now WHERE id=:id")
    suspend fun softDelete(id: Long, now: Long)

    @Query("UPDATE shopping_item SET convertedAssetId=:assetId WHERE id=:id")
    suspend fun markConverted(id: Long, assetId: Long)
}
