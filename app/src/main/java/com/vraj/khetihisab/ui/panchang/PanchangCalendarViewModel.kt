package com.vraj.khetihisab.ui.panchang

import com.vraj.khetihisab.data.entity.AccountSection
import com.vraj.khetihisab.data.model.DayTotal
import com.vraj.khetihisab.data.repo.DateRange
import com.vraj.khetihisab.ui.home.HomeLogic
import com.vraj.khetihisab.ui.home.LedgerNames
import com.vraj.khetihisab.ui.home.LedgerRowUi
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vraj.khetihisab.data.entity.CustomFestivalEntity
import com.vraj.khetihisab.data.entity.PanchangAlertPrefEntity
import com.vraj.khetihisab.data.repo.ReminderRepository
import com.vraj.khetihisab.notify.NotifyGraph
import com.vraj.khetihisab.notify.PanchangAlertSettings
import com.vraj.khetihisab.panchang.PanchangAlert
import com.vraj.khetihisab.panchang.SpecialDay
import com.vraj.khetihisab.ui.home.ScreensDeps
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.YearMonth
import com.vraj.khetihisab.data.entity.PanchangAlertType as PrefType

/** "રિમાઇન્ડર બનાવો" outcome for the snackbar. */
enum class ReminderOutcome { CREATED, EXISTS, FAILED }

/**
 * Panchang calendar (doc 10, artboard 08): month grid + markers, "આ મહિનાના ખાસ દિવસો", day sheet
 * with guidance and "રિમાઇન્ડર બનાવો". The month is computed off the main thread.
 */
class PanchangCalendarViewModel(
    customFestivals: Flow<List<CustomFestivalEntity>>,
    prefs: Flow<Map<PrefType, PanchangAlertPrefEntity>>,
    alertSettings: Flow<PanchangAlertSettings>,
    private val createReminder: suspend (LocalDate, String, String) -> ReminderOutcome,
    private val today: () -> LocalDate,
    initialDay: LocalDate? = null,
    computeDispatcher: CoroutineDispatcher = Dispatchers.Default,
    /** Per-day આવક / ખર્ચ of a month (all sections) for the green / red day numbers. */
    private val dayTotals: (YearMonth) -> Flow<List<DayTotal>> = { flowOf(emptyList()) },
    /** The entries of one day, shown in the day sheet. */
    private val dayEntries: (LocalDate) -> Flow<List<LedgerRowUi>> = { flowOf(emptyList()) },
) : ViewModel() {

    private val _month = MutableStateFlow(YearMonth.from(initialDay ?: today()))
    val month: StateFlow<YearMonth> = _month.asStateFlow()

    private val _selected = MutableStateFlow(initialDay)
    val selected: StateFlow<LocalDate?> = _selected.asStateFlow()

    val prefs: StateFlow<Map<PrefType, PanchangAlertPrefEntity>> =
        prefs.stateIn(viewModelScope, SharingStarted.Eagerly, emptyMap())
    val settings: StateFlow<PanchangAlertSettings> =
        alertSettings.stateIn(viewModelScope, SharingStarted.Eagerly, PanchangAlertSettings())

    /** null while the first month is being computed. */
    val calendar: StateFlow<CalendarMonth?> = combine(
        _month,
        customFestivals,
        alertSettings.map { it.includeVadBij }.distinctUntilChanged(),
    ) { ym, custom, vadBij ->
        PanchangCalendarModel.build(ym, today(), custom, vadBij)
    }.flowOn(computeDispatcher).stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    @OptIn(ExperimentalCoroutinesApi::class)
    val money: StateFlow<Map<LocalDate, DayTotal>> = _month
        .flatMapLatest { ym -> dayTotals(ym).map { l -> l.associateBy { it.date } } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyMap())

    @OptIn(ExperimentalCoroutinesApi::class)
    val selectedEntries: StateFlow<List<LedgerRowUi>> = _selected
        .flatMapLatest { d -> if (d == null) flowOf(emptyList()) else dayEntries(d) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _events = MutableSharedFlow<ReminderOutcome>(extraBufferCapacity = 4)
    val events: SharedFlow<ReminderOutcome> = _events.asSharedFlow()

    fun previousMonth() = _month.value.minusMonths(1).let(::goTo)
    fun nextMonth() = _month.value.plusMonths(1).let(::goTo)

    private fun goTo(ym: YearMonth) {
        // The engine supports 1990–2100.
        if (ym.year in 1990..2100) _month.value = ym
    }

    fun select(date: LocalDate?) {
        _selected.value = date
    }

    /** Guidance for a special day, filtered by the user's on/off prefs (null when every type is off). */
    fun alertFor(special: SpecialDay): PanchangAlert? =
        PanchangSource.alertFrom(special, prefs.value, settings.value.showGuidance)

    fun makeReminder(special: SpecialDay) {
        viewModelScope.launch {
            _events.emit(runCatching { createReminder(special.date, special.titleGu, special.day.tithiGu) }.getOrDefault(ReminderOutcome.FAILED))
        }
    }

    companion object {
        fun create(deps: ScreensDeps, initialEpochDay: Long?): PanchangCalendarViewModel {
            val repo = deps.repos.panchang
            val reminders = deps.repos.reminders
            return PanchangCalendarViewModel(
                customFestivals = repo.allCustomFestivals().map { list -> list.filter { it.enabled } },
                prefs = repo.prefsMap(),
                alertSettings = NotifyGraph.settings(deps.appContext).settings,
                createReminder = { d, t, n -> reminders.createPanchangReminder(d, t, n).toOutcome() },
                today = { LocalDate.now(PanchangUi.IST) },
                initialDay = initialEpochDay?.let { runCatching { LocalDate.ofEpochDay(it) }.getOrNull() },
                dayTotals = { ym -> deps.repos.ledger.dailyTotals(DateRange.month(ym)) },
                dayEntries = { d ->
                    combine(deps.repos.ledger.entries(DateRange(d, d)), deps.repos.master.allCategories(), deps.repos.master.farms(), deps.repos.master.families()) { list, cats, farms, fams ->
                        val names = LedgerNames(
                            farms = farms.associate { it.id to it.name },
                            families = fams.associate { it.id to it.displayName },
                            categories = cats.associate { it.id to it.name },
                        )
                        list.sortedByDescending { it.createdAt }.map { t -> HomeLogic.row(t, names) { s -> DaySectionLabels.of(s) } }
                    }
                },
            )
        }

        fun ReminderRepository.CreateResult.toOutcome(): ReminderOutcome = when (this) {
            is ReminderRepository.CreateResult.Created -> ReminderOutcome.CREATED
            is ReminderRepository.CreateResult.AlreadyExists -> ReminderOutcome.EXISTS
        }
    }
}

/** Section names for calendar rows (same words as the Entry chips). */
internal object DaySectionLabels {
    fun of(s: AccountSection): String = when (s) {
        AccountSection.FARM -> "ખેતર"
        AccountSection.ANIMAL -> "પશુપાલન"
        AccountSection.FAMILY -> "ભાગિયા"
        AccountSection.GENERAL -> "સામાન્ય"
        AccountSection.PERSONAL -> "અંગત"
    }
}
