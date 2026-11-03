package com.vraj.khetihisab.data.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.time.LocalDate

@Entity(tableName = "shopping_item", indices = [Index("tab"), Index("isBought")])
data class ShoppingItemEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val tab: ShopTab,
    val name: String,
    val forDate: LocalDate,
    val farmId: Long?,
    val estPricePaise: Long?,
    val isPhysicalTool: Boolean = false,
    val isBought: Boolean = false,
    val boughtAt: Long? = null,
    val convertedAssetId: Long? = null,
    val isDeleted: Boolean = false,
    val createdAt: Long,
    val updatedAt: Long,
)

@Entity(tableName = "asset")
data class AssetEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    /** asset | lend | utility */
    val iconKey: String,
    val purchaseDate: LocalDate?,
    val pricePaise: Long?,
    val photoPath: String?,
    val note: String?,
    val isDeleted: Boolean = false,
    val createdAt: Long,
    val updatedAt: Long,
)

@Entity(
    tableName = "asset_loan",
    foreignKeys = [
        ForeignKey(
            entity = AssetEntity::class,
            parentColumns = ["id"],
            childColumns = ["assetId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("assetId"), Index("returnedAt"), Index("expectedReturnDate")],
)
data class AssetLoanEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val assetId: Long,
    val borrowerName: String,
    val borrowerPhone: String?,
    val borrowerPhotoPath: String?,
    val handedOverDate: LocalDate,
    val handedOverTithi: String,
    val expectedReturnDate: LocalDate,
    val returnedAt: Long? = null,
    val notifyOnDue: Boolean = true,
    val lastWhatsAppReminderAt: Long? = null,
    val createdAt: Long,
    val updatedAt: Long,
)

@Entity(tableName = "reminder", indices = [Index("isDone"), Index("dueAt")])
data class ReminderEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val note: String?,
    val imagePath: String?,
    val priority: Priority,
    val dueAt: Long,
    val isDone: Boolean = false,
    val doneAt: Long? = null,
    /** [ReminderSource.LOAN] | [ReminderSource.PANCHANG] | [ReminderSource.MANUAL] */
    val sourceType: String? = null,
    val sourceId: Long? = null,
    val isDeleted: Boolean = false,
    val createdAt: Long,
    val updatedAt: Long,
)
