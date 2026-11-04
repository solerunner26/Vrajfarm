package com.vraj.khetihisab.ui.personal

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vraj.khetihisab.data.entity.AccountSection
import com.vraj.khetihisab.data.entity.TransactionEntity
import com.vraj.khetihisab.data.model.CategoryTotal
import com.vraj.khetihisab.data.model.Totals
import com.vraj.khetihisab.data.model.UdhaarSummary
import com.vraj.khetihisab.data.repo.DataRepositories
import com.vraj.khetihisab.data.repo.DateRange
import com.vraj.khetihisab.ui.home.HomeLogic
import com.vraj.khetihisab.ui.home.LedgerNames
import com.vraj.khetihisab.ui.home.LedgerRowUi
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
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.YearMonth

/** One bar of "ખર્ચ ક્યાં થયો?". [isOthers] = the "અન્ય" bucket. */
data class CategoryBar(val name: String, val totalPaise: Long, val fraction: Float, val isOthers: Boolean = false)

/** Pure Personal rules (doc 09), unit-tested. */
object PersonalLogic {
    const val TOP = 6

    /** Top 6 categories, then the rest summed into "અન્ય"; widths relative to the largest bar. */
    fun bars(list: List<CategoryTotal>, othersLabel: String): List<CategoryBar> {
        val sorted = list.filter { it.total > 0 }.sortedByDescending { it.total }
        val top = sorted.take(TOP).map { it.name to it.total }
        val rest = sorted.drop(TOP).sumOf { it.total }
        val all = if (rest > 0) top + (othersLabel to rest) else top
        val max = all.maxOfOrNull { it.second }?.coerceAtLeast(1L) ?: 1L
        return all.mapIndexed { i, (n, t) ->
            CategoryBar(n, t, (t.toDouble() / max).toFloat().coerceIn(0f, 1f), isOthers = rest > 0 && i == all.lastIndex)
        }
    }

    /** Only PERSONAL rows ever reach this screen. */
    fun personalOnly(list: List<TransactionEntity>): List<TransactionEntity> = list.filter { it.section == AccountSection.PERSONAL }
}

data class PersonalUiState(
    val month: YearMonth,
    val totals: Totals = Totals.ZERO,
    val bars: List<CategoryBar> = emptyList(),
    val rows: List<LedgerRowUi> = emptyList(),
    val udhaar: UdhaarSummary = UdhaarSummary.EMPTY,
) {
    val savings: Long get() = totals.net
    val isEmpty: Boolean get() = rows.isEmpty() && totals.income == 0L && totals.expense == 0L
    val showUdhaar: Boolean get() = udhaar.payable > 0L || udhaar.receivable > 0L
}

@OptIn(ExperimentalCoroutinesApi::class)
class PersonalViewModel(
    private val repos: DataRepositories,
    private val handle: SavedStateHandle,
    private val othersLabel: String,
    private val sectionLabel: String,
    today: LocalDate = ScreensFormat.today(),
    /** PERSONAL (અંગત / ઘર) or GENERAL (સામાન્ય ખર્ચ): the same one-section screen. */
    val section: AccountSection = AccountSection.PERSONAL,
) : ViewModel() {

    private val month = MutableStateFlow(
        handle.get<String>(KEY_MONTH)?.let { runCatching { YearMonth.parse(it) }.getOrNull() } ?: YearMonth.from(today),
    )

    private val categories: Flow<Map<Long, String>> =
        repos.master.allCategories().map { l -> l.associate { it.id to it.name } }

    val state: StateFlow<PersonalUiState> = month.flatMapLatest { ym ->
        val r = DateRange.month(ym)
        combine(
            repos.ledger.sectionTotals(section, r),
            repos.ledger.sectionByCategory(section, r),
            repos.ledger.entries(r),
            categories,
            repos.ledger.sectionUdhaar(section),
        ) { totals, byCat, entries, cats, udhaar ->
            val names = LedgerNames(categories = cats)
            PersonalUiState(
                month = ym,
                totals = totals,
                bars = PersonalLogic.bars(byCat, othersLabel),
                rows = entries.filter { it.section == section }
                    .sortedWith(compareByDescending<TransactionEntity> { it.date }.thenByDescending { it.createdAt })
                    .map { HomeLogic.row(it, names) { sectionLabel } },
                udhaar = udhaar,
            )
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), PersonalUiState(month.value))

    fun selectMonth(ym: YearMonth) {
        month.value = ym
        handle[KEY_MONTH] = ym.toString()
    }

    fun delete(id: Long) {
        viewModelScope.launch { repos.ledger.softDelete(id) }
    }

    companion object {
        const val KEY_MONTH = "personal_month"
    }
}
