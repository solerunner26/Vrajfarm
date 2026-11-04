package com.vraj.khetihisab.ui.farms

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vraj.khetihisab.data.model.FarmPnlRow
import com.vraj.khetihisab.data.model.Totals
import com.vraj.khetihisab.data.repo.DataRepositories
import com.vraj.khetihisab.data.repo.DateRange
import com.vraj.khetihisab.ui.home.ScreensFormat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import java.time.LocalDate

/** Data the Farms screen needs. FARM-section queries only (PERSONAL never reaches them). */
interface FarmsSource {
    fun farmPnl(range: DateRange): Flow<List<FarmPnlRow>>
    fun farmTotals(range: DateRange): Flow<Totals>

    /** farmId → active crop name. */
    fun activeCrops(): Flow<Map<Long, String>>
}

class RepoFarmsSource(private val repos: DataRepositories) : FarmsSource {
    override fun farmPnl(range: DateRange) = repos.ledger.farmPnl(range)
    override fun farmTotals(range: DateRange) = repos.ledger.farmTotals(range)
    override fun activeCrops(): Flow<Map<Long, String>> =
        repos.master.activeCycles().map { list -> list.associate { it.cycle.farmId to it.crop.name } }
}

data class FarmsUiState(
    val choice: PeriodChoice,
    val byVigha: Boolean = true,
    val totals: Totals = Totals.ZERO,
    val totalVigha: Double = 0.0,
    val rows: List<ScoreRowUi> = emptyList(),
    val loaded: Boolean = false,
) {
    /** The four chips of the selected crop year: ખરીફ, રવી, ઉનાળુ, આખું વર્ષ (doc 07 §2). */
    val chips: List<PeriodChoice> get() = FarmPeriod.entries.map { PeriodChoice(it, choice.cropYear) }
}

@OptIn(ExperimentalCoroutinesApi::class)
class FarmsViewModel(
    private val source: FarmsSource,
    private val handle: SavedStateHandle,
    today: LocalDate = ScreensFormat.today(),
) : ViewModel() {

    private val initial = SeasonRange.current(today)
    private val choice = MutableStateFlow(
        PeriodChoice(
            handle.get<String>(KEY_PERIOD)?.let { n -> FarmPeriod.entries.firstOrNull { it.name == n } } ?: initial.period,
            handle.get<Int>(KEY_YEAR) ?: initial.cropYear,
        ),
    )
    private val byVigha = MutableStateFlow(handle.get<Boolean>(KEY_BY_VIGHA) ?: true)

    val state: StateFlow<FarmsUiState> = combine(choice, byVigha, ::Pair)
        .flatMapLatest { (c, v) ->
            combine(source.farmPnl(c.range), source.farmTotals(c.range), source.activeCrops()) { rows, totals, crops ->
                val scores = FarmScoring.score(rows, crops)
                FarmsUiState(c, v, totals, FarmScoring.totalVigha(scores), FarmScoring.display(scores, v), loaded = true)
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), FarmsUiState(choice.value, byVigha.value))

    fun selectPeriod(p: FarmPeriod) = setChoice(choice.value.copy(period = p))

    fun shiftYear(delta: Int) = setChoice(choice.value.copy(cropYear = choice.value.cropYear + delta))

    fun setByVigha(on: Boolean) {
        byVigha.value = on
        handle[KEY_BY_VIGHA] = on
    }

    private fun setChoice(c: PeriodChoice) {
        choice.value = c
        handle[KEY_PERIOD] = c.period.name
        handle[KEY_YEAR] = c.cropYear
    }

    companion object {
        const val KEY_PERIOD = "farms_period"
        const val KEY_YEAR = "farms_year"
        const val KEY_BY_VIGHA = "farms_by_vigha"
    }
}
