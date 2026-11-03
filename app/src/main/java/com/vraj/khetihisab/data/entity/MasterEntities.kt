package com.vraj.khetihisab.data.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.time.LocalDate

@Entity(tableName = "farm")
data class FarmEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    /** e.g. "ખેતર 1" */
    val name: String,
    val areaInVigha: Double,
    /** Farm -> manager assignment (replaces assignedFarmIds CSV from New_Features F1). */
    val managerRole: ProfileRole? = null,
    val sortOrder: Int,
    val createdAt: Long,
    val updatedAt: Long,
)

@Entity(tableName = "family")
data class FamilyEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    /** "A" | "B" */
    val code: String,
    val displayName: String,
    val whatsappPhone: String?,
    val createdAt: Long,
    val updatedAt: Long,
)

@Entity(tableName = "crop_master", indices = [Index(value = ["name"], unique = true)])
data class CropMasterEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val isActive: Boolean = true,
)

@Entity(tableName = "category_master")
data class CategoryMasterEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val section: AccountSection,
    val type: TxnType,
    val isPhysicalTool: Boolean = false,
    val isActive: Boolean = true,
)

@Entity(
    tableName = "farm_crop_cycle",
    foreignKeys = [
        ForeignKey(
            entity = FarmEntity::class,
            parentColumns = ["id"],
            childColumns = ["farmId"],
            onDelete = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = CropMasterEntity::class,
            parentColumns = ["id"],
            childColumns = ["cropId"],
            onDelete = ForeignKey.RESTRICT,
        ),
    ],
    indices = [Index("farmId"), Index("cropId")],
)
data class FarmCropCycleEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val farmId: Long,
    val cropId: Long,
    /** [Season.KHARIF] | [Season.RABI] | [Season.SUMMER] */
    val season: String,
    val year: Int,
    val startDate: LocalDate,
    val endDate: LocalDate?,
    val isActive: Boolean,
)
