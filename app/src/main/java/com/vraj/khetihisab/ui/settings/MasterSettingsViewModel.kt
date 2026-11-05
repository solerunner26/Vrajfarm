package com.vraj.khetihisab.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vraj.khetihisab.data.entity.AccountSection
import com.vraj.khetihisab.data.entity.CategoryMasterEntity
import com.vraj.khetihisab.data.entity.CropMasterEntity
import com.vraj.khetihisab.data.entity.FarmCropCycleEntity
import com.vraj.khetihisab.data.entity.FarmEntity
import com.vraj.khetihisab.data.entity.ProfileRole
import com.vraj.khetihisab.data.entity.TxnType
import com.vraj.khetihisab.data.model.CycleWithCrop
import com.vraj.khetihisab.data.repo.MasterRepository
import com.vraj.khetihisab.share.PhoneNormalizer
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate

/** Edit-farm dialog result (doc 04 FarmsSettingsScreen). */
data class FarmEdit(
    val farmId: Long?,
    val name: String,
    val vighaText: String,
    val manager: ProfileRole?,
    /** બંને ભાગિયા (joint farm, DB v2). */
    val joint: Boolean = false,
    /** New active crop (null = keep the current cycle). */
    val cropId: Long?,
    val season: String,
    val year: Int,
)

enum class MasterError { NAME, VIGHA, PHONE, DUPLICATE, IN_USE }

/** One-shot UI messages. */
sealed interface MasterEvent {
    data object Saved : MasterEvent
    data class Error(val error: MasterError) : MasterEvent
}

/**
 * Master Settings (doc 04): farms (name, વીઘા, manager, active crop cycle), family WhatsApp numbers,
 * crops and categories (per section, incl. ANIMAL / પશુપાલન). Categories are never hard-deleted:
 * they are deactivated (`isActive = false`), so old entries keep their category.
 * Every write goes through [MasterRepository], so all screens update live through Flow.
 */
class MasterSettingsViewModel(
    private val master: MasterRepository,
    private val today: () -> LocalDate = { LocalDate.now(SettingsFormat.IST) },
) : ViewModel() {

    private fun <T> Flow<T>.state(initial: T): StateFlow<T> = stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), initial)

    val farms: StateFlow<List<FarmEntity>> = master.farms().state(emptyList())
    val activeCycles: StateFlow<List<CycleWithCrop>> = master.activeCycles().state(emptyList())
    val crops: StateFlow<List<CropMasterEntity>> = master.allCrops().state(emptyList())
    val categories: StateFlow<List<CategoryMasterEntity>> = master.allCategories().state(emptyList())

    private val _events = MutableSharedFlow<MasterEvent>(extraBufferCapacity = 4)
    val events: SharedFlow<MasterEvent> = _events.asSharedFlow()

    fun currentYear(): Int = today().year

    // ---- farms ----

    /** Validates and saves; returns the problem (shown inline) or null. */
    fun saveFarm(e: FarmEdit): MasterError? {
        val name = e.name.trim()
        if (name.isEmpty()) return MasterError.NAME
        val vigha = SettingsFormat.parseVigha(e.vighaText) ?: 0.0
        if (!SettingsFormat.vighaValid(vigha)) return MasterError.VIGHA
        viewModelScope.launch {
            val id = if (e.farmId == null) {
                master.addFarm(name, vigha, (farms.value.maxOfOrNull { it.sortOrder } ?: 0) + 1)
            } else {
                val old = farms.value.firstOrNull { it.id == e.farmId }
                if (old?.name != name) master.renameFarm(e.farmId, name)
                if (old?.areaInVigha != vigha) master.updateFarmArea(e.farmId, vigha)
                e.farmId
            }
            val old = farms.value.firstOrNull { it.id == id }
            val wanted = when {
                e.joint -> setOf(ProfileRole.MANAGER_1, ProfileRole.MANAGER_2)
                e.manager != null -> setOf(e.manager)
                else -> emptySet()
            }
            if (e.farmId == null || old?.managers != wanted) master.updateFarmManagers(id, wanted)
            if (e.cropId != null) {
                val current = activeCycles.value.firstOrNull { it.cycle.farmId == id }?.cycle
                val changed = current == null || current.cropId != e.cropId || current.season != e.season || current.year != e.year
                if (changed) {
                    master.startCycle(
                        FarmCropCycleEntity(
                            farmId = id, cropId = e.cropId, season = e.season, year = e.year,
                            startDate = today(), endDate = null, isActive = true,
                        ),
                    )
                }
            }
            _events.emit(MasterEvent.Saved)
        }
        return null
    }

    // ---- crops ----

    fun addCrop(name: String): MasterError? {
        val n = name.trim()
        if (n.isEmpty()) return MasterError.NAME
        if (crops.value.any { it.name.equals(n, ignoreCase = true) }) return MasterError.DUPLICATE
        viewModelScope.launch {
            runCatching { master.addCrop(n) }
                .onSuccess { _events.emit(MasterEvent.Saved) }
                .onFailure { _events.emit(MasterEvent.Error(MasterError.DUPLICATE)) }
        }
        return null
    }

    fun renameCrop(c: CropMasterEntity, name: String): MasterError? {
        val n = name.trim()
        if (n.isEmpty()) return MasterError.NAME
        if (crops.value.any { it.id != c.id && it.name.equals(n, ignoreCase = true) }) return MasterError.DUPLICATE
        viewModelScope.launch {
            runCatching { master.updateCrop(c.copy(name = n)) }
                .onFailure { _events.emit(MasterEvent.Error(MasterError.DUPLICATE)) }
        }
        return null
    }

    fun setCropActive(c: CropMasterEntity, on: Boolean) = viewModelScope.launch { master.updateCrop(c.copy(isActive = on)) }

    /** How many farm seasons use this crop (> 0 → cannot delete, only switch off). */
    suspend fun cropUsage(c: CropMasterEntity): Int = master.cycleCountForCrop(c.id)

    fun deleteCrop(c: CropMasterEntity) = viewModelScope.launch {
        runCatching { master.deleteCrop(c) }
            .onSuccess { _events.emit(MasterEvent.Saved) }
            .onFailure { _events.emit(MasterEvent.Error(MasterError.IN_USE)) }
    }

    suspend fun categoryUsage(c: CategoryMasterEntity): Int = master.txnCountForCategory(c.id)

    fun deleteCategory(c: CategoryMasterEntity) = viewModelScope.launch {
        runCatching { master.deleteCategory(c) }
            .onSuccess { _events.emit(MasterEvent.Saved) }
            .onFailure { _events.emit(MasterEvent.Error(MasterError.IN_USE)) }
    }

    suspend fun farmUsage(f: com.vraj.khetihisab.data.entity.FarmEntity): Int = master.txnCountForFarm(f.id)

    fun deleteFarm(f: com.vraj.khetihisab.data.entity.FarmEntity) = viewModelScope.launch {
        runCatching { master.deleteFarm(f) }
            .onSuccess { _events.emit(MasterEvent.Saved) }
            .onFailure { _events.emit(MasterEvent.Error(MasterError.IN_USE)) }
    }

    // ---- categories ----

    fun categoriesOf(all: List<CategoryMasterEntity>, section: AccountSection): Map<TxnType, List<CategoryMasterEntity>> =
        groupCategories(all, section)

    fun addCategory(section: AccountSection, type: TxnType, name: String): MasterError? {
        val n = name.trim()
        if (n.isEmpty()) return MasterError.NAME
        if (categories.value.any { it.section == section && it.type == type && it.name.equals(n, ignoreCase = true) }) return MasterError.DUPLICATE
        viewModelScope.launch {
            master.addCategory(CategoryMasterEntity(name = n, section = section, type = type))
            _events.emit(MasterEvent.Saved)
        }
        return null
    }

    fun renameCategory(c: CategoryMasterEntity, name: String): MasterError? {
        val n = name.trim()
        if (n.isEmpty()) return MasterError.NAME
        viewModelScope.launch { master.updateCategory(c.copy(name = n)) }
        return null
    }

    /** Deactivate instead of delete (doc 04: never hard-delete a category that is in use). */
    fun setCategoryActive(c: CategoryMasterEntity, on: Boolean) = viewModelScope.launch { master.updateCategory(c.copy(isActive = on)) }

    companion object {
        /** Settings order of sections; ANIMAL (પશુપાલન) sits right after FARM. */
        val SECTIONS = listOf(AccountSection.FARM, AccountSection.ANIMAL, AccountSection.FAMILY, AccountSection.GENERAL, AccountSection.PERSONAL)

        /** Types offered per section: Family has ઉપાડ (ADVANCE) besides expense; others income + expense. */
        fun typesFor(section: AccountSection): List<TxnType> = when (section) {
            AccountSection.FAMILY -> listOf(TxnType.ADVANCE, TxnType.EXPENSE, TxnType.INCOME)
            else -> listOf(TxnType.EXPENSE, TxnType.INCOME)
        }

        /** Categories of [section] grouped by type (in [typesFor] order), active first then by name. */
        fun groupCategories(all: List<CategoryMasterEntity>, section: AccountSection): Map<TxnType, List<CategoryMasterEntity>> {
            val mine = all.filter { it.section == section }
            val offered = typesFor(section)
            // Offered types always appear (so "add" has a home); other types only if rows exist.
            val types = offered + TxnType.entries.filter { t -> t !in offered && mine.any { it.type == t } }
            return types.associateWith { t ->
                mine.filter { it.type == t }.sortedWith(compareByDescending<CategoryMasterEntity> { it.isActive }.thenBy { it.id })
            }
        }
    }
}
