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
    /**
     * DB v2 (2026-11-05): big farms are run by BOTH ભાગિયા. When true the farm belongs to MANAGER_1
     * and MANAGER_2 ([managerRole] is then MANAGER_1). Use [managedBy], never compare managerRole alone.
     */
    @androidx.room.ColumnInfo(defaultValue = "0") val jointManagers: Boolean = false,
) {
    fun managedBy(role: ProfileRole): Boolean = jointManagers && role != ProfileRole.OWNER || managerRole == role

    /** Who runs this farm: empty, one ભાગિયા, or both. */
    val managers: Set<ProfileRole>
        get() = when {
            jointManagers -> setOf(ProfileRole.MANAGER_1, ProfileRole.MANAGER_2)
            managerRole != null -> setOf(managerRole)
            else -> emptySet()
        }

    companion object {
        /** (managerRole, jointManagers) for a set of managers. */
        fun encode(managers: Set<ProfileRole>): Pair<ProfileRole?, Boolean> {
            val m = managers - ProfileRole.OWNER
            return when {
                m.size >= 2 -> ProfileRole.MANAGER_1 to true
                m.size == 1 -> m.first() to false
                else -> null to false
            }
        }
    }
}

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
