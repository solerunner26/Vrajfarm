package com.vraj.khetihisab.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.vraj.khetihisab.data.entity.AccountSection
import com.vraj.khetihisab.data.entity.CategoryMasterEntity
import com.vraj.khetihisab.data.entity.CropMasterEntity
import com.vraj.khetihisab.data.entity.FamilyEntity
import com.vraj.khetihisab.data.entity.FarmCropCycleEntity
import com.vraj.khetihisab.data.entity.FarmEntity
import com.vraj.khetihisab.data.entity.ProfileRole
import com.vraj.khetihisab.data.entity.TxnType
import com.vraj.khetihisab.data.model.CycleWithCrop
import kotlinx.coroutines.flow.Flow

/** Master data: farms, families, crops, categories, crop cycles (Master Settings, doc 04). */
@Dao
abstract class MasterDao {
    // ---------------- farm ----------------
    @Query("SELECT * FROM farm ORDER BY sortOrder")
    abstract fun farms(): Flow<List<FarmEntity>>

    @Query("SELECT * FROM farm ORDER BY sortOrder")
    abstract suspend fun farmsNow(): List<FarmEntity>

    @Query("SELECT * FROM farm WHERE id=:id")
    abstract suspend fun farmById(id: Long): FarmEntity?

    /** A ભાગિયા's farms, including farms run jointly by both (DB v2). */
    @Query("SELECT * FROM farm WHERE managerRole=:role OR (jointManagers=1 AND :role!='OWNER') ORDER BY sortOrder")
    abstract fun farmsForManager(role: ProfileRole): Flow<List<FarmEntity>>

    @Insert abstract suspend fun insertFarm(f: FarmEntity): Long

    @Update abstract suspend fun updateFarm(f: FarmEntity)

    /** Cascades cycles; its transactions keep farmId = NULL. UI must confirm first. */
    @Delete abstract suspend fun deleteFarm(f: FarmEntity)

    @Query("UPDATE farm SET areaInVigha=:area, updatedAt=:now WHERE id=:farmId")
    abstract suspend fun updateFarmArea(farmId: Long, area: Double, now: Long)

    @Query("UPDATE farm SET name=:name, updatedAt=:now WHERE id=:farmId")
    abstract suspend fun renameFarm(farmId: Long, name: String, now: Long)

    /** One column per farm, so assignment is exclusive between the two managers by construction. */
    @Query("UPDATE farm SET managerRole=:role, jointManagers=0, updatedAt=:now WHERE id=:farmId")
    abstract suspend fun updateFarmManager(farmId: Long, role: ProfileRole?, now: Long)

    @Query("UPDATE farm SET managerRole=:role, jointManagers=:joint, updatedAt=:now WHERE id=:farmId")
    abstract suspend fun updateFarmManagers(farmId: Long, role: ProfileRole?, joint: Boolean, now: Long)

    /** Replace a manager's whole assignment in one go (profile edit dialog). */
    @Transaction
    open suspend fun setManagerFarms(role: ProfileRole, farmIds: Set<Long>, now: Long) {
        // Not exclusive any more (2026-11-05): ticking a farm for ભાગિયા 2 that ભાગિયા 1 already runs
        // makes it a joint farm; unticking removes only this ભાગિયા.
        farmsNow().forEach { f ->
            val now0 = f.managers
            val next = if (f.id in farmIds) now0 + role else now0 - role
            if (next != now0) {
                val (r, joint) = FarmEntity.encode(next)
                updateFarmManagers(f.id, r, joint, now)
            }
        }
    }

    // ---------------- family ----------------
    @Query("SELECT * FROM family ORDER BY code")
    abstract fun families(): Flow<List<FamilyEntity>>

    @Query("SELECT * FROM family WHERE code=:code LIMIT 1")
    abstract suspend fun familyByCode(code: String): FamilyEntity?

    @Query("SELECT * FROM family WHERE id=:id")
    abstract suspend fun familyById(id: Long): FamilyEntity?

    @Insert abstract suspend fun insertFamily(f: FamilyEntity): Long

    @Update abstract suspend fun updateFamily(f: FamilyEntity)

    @Query("UPDATE family SET whatsappPhone=:phone, updatedAt=:now WHERE id=:familyId")
    abstract suspend fun updateFamilyPhone(familyId: Long, phone: String?, now: Long)

    // ---------------- crop master ----------------
    @Query("SELECT * FROM crop_master WHERE isActive=1 ORDER BY id")
    abstract fun activeCrops(): Flow<List<CropMasterEntity>>

    @Query("SELECT * FROM crop_master ORDER BY id")
    abstract fun allCrops(): Flow<List<CropMasterEntity>>

    @Insert abstract suspend fun insertCrop(c: CropMasterEntity): Long

    @Update abstract suspend fun updateCrop(c: CropMasterEntity)

    // ---------------- category master ----------------
    @Query("SELECT * FROM category_master WHERE isActive=1 AND section=:section AND type=:type ORDER BY id")
    abstract fun categories(section: AccountSection, type: TxnType): Flow<List<CategoryMasterEntity>>

    @Query("SELECT * FROM category_master WHERE isActive=1 AND section=:section ORDER BY type, id")
    abstract fun categoriesForSection(section: AccountSection): Flow<List<CategoryMasterEntity>>

    @Query("SELECT * FROM category_master ORDER BY section, type, id")
    abstract fun allCategories(): Flow<List<CategoryMasterEntity>>

    @Insert abstract suspend fun insertCategory(c: CategoryMasterEntity): Long

    @Update abstract suspend fun updateCategory(c: CategoryMasterEntity)

    /** Old entries keep their amount; their type shows "—" (FK SET_NULL). */
    @Delete abstract suspend fun deleteCategory(c: CategoryMasterEntity)

    @Query("SELECT COUNT(*) FROM txn WHERE categoryId=:id AND isDeleted=0")
    abstract suspend fun txnCountForCategory(id: Long): Int

    @Delete abstract suspend fun deleteCrop(c: CropMasterEntity)

    /** Crop cycles reference crops with RESTRICT: a crop used on a farm cannot be deleted. */
    @Query("SELECT COUNT(*) FROM farm_crop_cycle WHERE cropId=:id")
    abstract suspend fun cycleCountForCrop(id: Long): Int

    @Query("SELECT COUNT(*) FROM txn WHERE farmId=:id AND isDeleted=0")
    abstract suspend fun txnCountForFarm(id: Long): Int

    // ---------------- farm crop cycle ----------------
    @Insert abstract suspend fun insertCycle(c: FarmCropCycleEntity): Long

    @Update abstract suspend fun updateCycle(c: FarmCropCycleEntity)

    @Query("SELECT * FROM farm_crop_cycle WHERE farmId=:farmId ORDER BY year DESC, startDate DESC")
    abstract fun cyclesFor(farmId: Long): Flow<List<FarmCropCycleEntity>>

    /** Crop label on the farm card (doc 07). */
    @Transaction
    @Query("SELECT * FROM farm_crop_cycle WHERE farmId=:farmId AND isActive=1 ORDER BY startDate DESC LIMIT 1")
    abstract fun activeCycleFor(farmId: Long): Flow<CycleWithCrop?>

    @Transaction
    @Query("SELECT * FROM farm_crop_cycle WHERE isActive=1 ORDER BY farmId, startDate DESC")
    abstract fun activeCycles(): Flow<List<CycleWithCrop>>

    /** Closes any active cycle on the farm, then inserts the new one. */
    @Transaction
    open suspend fun startCycle(c: FarmCropCycleEntity): Long {
        closeActiveCycles(c.farmId)
        return insertCycle(c.copy(isActive = true))
    }

    @Query("UPDATE farm_crop_cycle SET isActive=0 WHERE farmId=:farmId AND isActive=1")
    abstract suspend fun closeActiveCycles(farmId: Long)
}
