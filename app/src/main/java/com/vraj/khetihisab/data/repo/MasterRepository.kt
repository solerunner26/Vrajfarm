package com.vraj.khetihisab.data.repo

import com.vraj.khetihisab.data.dao.MasterDao
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

/** Farms, families, crops, categories and crop cycles (Master Settings + pickers). */
class MasterRepository(
    private val dao: MasterDao,
    private val clock: () -> Long = System::currentTimeMillis,
    private val listener: DataChangeListener = DataChangeListener.NONE,
) {
    // ---- farms ----
    fun farms(): Flow<List<FarmEntity>> = dao.farms()
    suspend fun farmsNow(): List<FarmEntity> = dao.farmsNow()
    fun farmsForManager(role: ProfileRole): Flow<List<FarmEntity>> = dao.farmsForManager(role)

    suspend fun addFarm(name: String, areaInVigha: Double, sortOrder: Int): Long {
        val now = clock()
        return dao.insertFarm(
            FarmEntity(name = name, areaInVigha = areaInVigha, sortOrder = sortOrder, createdAt = now, updatedAt = now),
        ).also { listener.onDataChanged() }
    }

    suspend fun updateFarmArea(farmId: Long, areaInVigha: Double) {
        require(!areaInVigha.isNaN() && areaInVigha >= 0.0) { "area must be >= 0" }
        dao.updateFarmArea(farmId, areaInVigha, clock())
        listener.onDataChanged()
    }

    suspend fun renameFarm(farmId: Long, name: String) {
        dao.renameFarm(farmId, name, clock())
        listener.onDataChanged()
    }

    suspend fun updateFarmManager(farmId: Long, role: ProfileRole?) {
        dao.updateFarmManager(farmId, role, clock())
        listener.onDataChanged()
    }

    suspend fun setManagerFarms(role: ProfileRole, farmIds: Set<Long>) {
        dao.setManagerFarms(role, farmIds, clock())
        listener.onDataChanged()
    }

    /** UI must confirm first. Cycles cascade; transactions keep farmId = NULL ("ખેતર કાઢી નાખેલ"). */
    suspend fun deleteFarm(farm: FarmEntity) {
        dao.deleteFarm(farm)
        listener.onDataChanged()
    }

    // ---- families ----
    fun families(): Flow<List<FamilyEntity>> = dao.families()
    suspend fun familyByCode(code: String): FamilyEntity? = dao.familyByCode(code)
    suspend fun familyById(id: Long): FamilyEntity? = dao.familyById(id)

    suspend fun updateFamilyPhone(familyId: Long, phone: String?) {
        dao.updateFamilyPhone(familyId, phone?.trim()?.ifEmpty { null }, clock())
        listener.onDataChanged()
    }

    suspend fun updateFamily(f: FamilyEntity) {
        dao.updateFamily(f.copy(updatedAt = clock()))
        listener.onDataChanged()
    }

    // ---- crops ----
    fun activeCrops(): Flow<List<CropMasterEntity>> = dao.activeCrops()
    fun allCrops(): Flow<List<CropMasterEntity>> = dao.allCrops()
    suspend fun addCrop(name: String): Long = dao.insertCrop(CropMasterEntity(name = name.trim()))
    suspend fun updateCrop(c: CropMasterEntity) = dao.updateCrop(c)

    // ---- categories ----
    fun categories(section: AccountSection, type: TxnType): Flow<List<CategoryMasterEntity>> =
        dao.categories(section, type)

    fun categoriesForSection(section: AccountSection): Flow<List<CategoryMasterEntity>> =
        dao.categoriesForSection(section)

    fun allCategories(): Flow<List<CategoryMasterEntity>> = dao.allCategories()
    suspend fun addCategory(c: CategoryMasterEntity): Long = dao.insertCategory(c)
    suspend fun updateCategory(c: CategoryMasterEntity) = dao.updateCategory(c)

    // ---- crop cycles ----
    fun cyclesFor(farmId: Long): Flow<List<FarmCropCycleEntity>> = dao.cyclesFor(farmId)
    fun activeCycleFor(farmId: Long): Flow<CycleWithCrop?> = dao.activeCycleFor(farmId)
    fun activeCycles(): Flow<List<CycleWithCrop>> = dao.activeCycles()

    suspend fun startCycle(c: FarmCropCycleEntity): Long = dao.startCycle(c).also { listener.onDataChanged() }

    suspend fun updateCycle(c: FarmCropCycleEntity) {
        dao.updateCycle(c)
        listener.onDataChanged()
    }
}
