package com.vraj.khetihisab.ui.farms

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vraj.khetihisab.data.entity.AccountSection
import com.vraj.khetihisab.data.entity.CropMasterEntity
import com.vraj.khetihisab.data.entity.FarmCropCycleEntity
import com.vraj.khetihisab.data.entity.TransactionEntity
import com.vraj.khetihisab.data.model.CycleWithCrop
import com.vraj.khetihisab.data.model.Totals
import com.vraj.khetihisab.data.repo.DataRepositories
import com.vraj.khetihisab.ui.home.ScreensFormat
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class FarmDetailUiState(
    val farmId: Long,
    val choice: PeriodChoice,
    val name: String = "",
    val vigha: Double = 0.0,
    val totals: Totals = Totals.ZERO,
    val cycle: CycleWithCrop? = null,
    val entries: List<TransactionEntity> = emptyList(),
    val categories: Map<Long, String> = emptyMap(),
    val crops: List<CropMasterEntity> = emptyList(),
)

/** FarmDetailScreen (doc 07): this farm's stats, active crop and entries for the chosen period. */
class FarmDetailViewModel(
    private val repos: DataRepositories,
    private val farmId: Long,
    choice: PeriodChoice,
) : ViewModel() {

    private val range = choice.range

    val state: StateFlow<FarmDetailUiState> = combine(
        repos.ledger.farmPnl(range).map { rows -> rows.firstOrNull { it.farmId == farmId } },
        repos.master.activeCycleFor(farmId),
        repos.ledger.entries(range).map { list -> list.filter { it.section == AccountSection.FARM && it.farmId == farmId } },
        repos.master.allCategories().map { list -> list.associate { it.id to it.name } },
        repos.master.activeCrops(),
    ) { row, cycle, entries, cats, crops ->
        FarmDetailUiState(
            farmId = farmId,
            choice = choice,
            name = row?.name.orEmpty(),
            vigha = row?.areaInVigha ?: 0.0,
            totals = Totals(row?.incomePaise ?: 0L, row?.expensePaise ?: 0L),
            cycle = cycle,
            entries = entries,
            categories = cats,
            crops = crops,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), FarmDetailUiState(farmId, choice))

    /** "પાક બદલો": `startCycle` closes the current cycle and opens a new one for the season of today. */
    fun changeCrop(cropId: Long) {
        viewModelScope.launch {
            val today = ScreensFormat.today()
            val season = SeasonRange.current(today)
            repos.master.startCycle(
                FarmCropCycleEntity(
                    farmId = farmId,
                    cropId = cropId,
                    season = SeasonRange.toCycleSeason(season.period),
                    year = season.seasonYear,
                    startDate = today,
                    endDate = null,
                    isActive = true,
                ),
            )
        }
    }
}
