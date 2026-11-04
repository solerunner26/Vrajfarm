package com.vraj.khetihisab.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vraj.khetihisab.R
import com.vraj.khetihisab.data.entity.AccountSection
import com.vraj.khetihisab.data.entity.PersonProfileEntity
import com.vraj.khetihisab.data.model.CategoryTotal
import com.vraj.khetihisab.data.model.Totals
import com.vraj.khetihisab.data.model.UdhaarSummary
import com.vraj.khetihisab.data.repo.DateRange
import com.vraj.khetihisab.panchang.PanchangDay
import com.vraj.khetihisab.panchang.SpecialDay
import com.vraj.khetihisab.ui.farms.PeriodChoice
import com.vraj.khetihisab.ui.farms.SeasonRange
import com.vraj.khetihisab.ui.panchang.PanchangSource
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.YearMonth

/** Everything Home renders (doc 05 "Data"). Money in paise. */
data class HomeUiState(
    val today: LocalDate,
    val panchang: PanchangDay? = null,
    val upcoming: List<SpecialDay> = emptyList(),
    val ownerName: String = "",
    val ownerAddress: String? = null,
    val ownerPhoto: String? = null,
    val backup: BackupUiStatus = BackupUiStatus.PENDING,
    val udhaar: UdhaarSummary = UdhaarSummary.EMPTY,
    val pending: List<LedgerRowUi> = emptyList(),
    val overdue: List<LoanCardUi> = emptyList(),
    val overdueMore: Int = 0,
    val urgent: List<UrgentUi> = emptyList(),
    val business: BusinessUi = BusinessUi(Totals.ZERO, HomeLogic.businessBreakdown(emptyList()), SeasonRange.cropYearOf(today)),
    val farm: Totals = Totals.ZERO,
    val farmPeriod: PeriodChoice = SeasonRange.current(today),
    val animal: Totals = Totals.ZERO,
    val animalCategories: List<CategoryTotal> = emptyList(),
    val familyMonth: Long = 0,
    val generalMonth: Long = 0,
    val personalExpenseMonth: Long = 0,
    val personalNetMonth: Long = 0,
    /** Settings › સામાન્ય › "હોમ પર રકમ હંમેશાં બતાવો". Off → Hisab amounts start as XXXXX.XX. */
    val showNumbers: Boolean = false,
    val shoppingPending: Int = 0,
    val assetsLent: Int = 0,
    val recent: List<LedgerRowUi> = emptyList(),
) {
    val showUdhaar: Boolean get() = HomeLogic.showUdhaar(udhaar)
    val hasAlerts: Boolean get() = showUdhaar || overdue.isNotEmpty() || urgent.isNotEmpty()
}

private data class AlertsPart(
    val udhaar: UdhaarSummary,
    val pending: List<LedgerRowUi>,
    val overdue: Pair<List<LoanCardUi>, Int>,
    val urgent: List<UrgentUi>,
)

private data class MoneyPart(
    val business: BusinessUi,
    val farm: Pair<PeriodChoice, Totals>,
    val animal: Totals,
    val animalCategories: List<CategoryTotal>,
)

private data class MonthPart(val family: Long, val general: Long, val personal: Long, val personalNet: Long, val shop: Int, val lent: Int)

private data class HeaderPart(
    val owner: PersonProfileEntity?,
    val backup: BackupUiStatus,
    val upcoming: List<SpecialDay>,
    val showNumbers: Boolean = false,
)

@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModel(private val deps: ScreensDeps) : ViewModel() {
    private val repos = deps.repos
    private val ledger = repos.ledger
    val today: LocalDate = ScreensFormat.today()
    private val month = DateRange.month(YearMonth.from(today))
    private val panchangDay: PanchangDay? = runCatching { PanchangSource.day(today) }.getOrNull()

    private val sectionLabel: (AccountSection) -> String = { s ->
        deps.appContext.getString(
            when (s) {
                AccountSection.FARM -> R.string.scr_sec_farm_short
                AccountSection.ANIMAL -> R.string.scr_sec_animal_short
                AccountSection.FAMILY -> R.string.scr_sec_family_short
                AccountSection.GENERAL -> R.string.scr_sec_general_short
                AccountSection.PERSONAL -> R.string.scr_sec_personal_short
            },
        )
    }

    private val names: Flow<LedgerNames> = combine(
        repos.master.farms(),
        repos.master.families(),
        repos.master.allCategories(),
    ) { farms, families, cats ->
        LedgerNames(farms.associate { it.id to it.name }, families.associate { it.id to it.displayName }, cats.associate { it.id to it.name })
    }

    private val alerts: Flow<AlertsPart> = combine(
        ledger.pendingUdhaar(),
        combine(ledger.pendingList(), names) { list, n -> list.map { HomeLogic.row(it, n, sectionLabel) } },
        repos.assets.overdueLoans(today),
        repos.reminders.urgent(10),
    ) { u, pending, loans, rem ->
        AlertsPart(u, pending, HomeLogic.capOverdue(loans, today), HomeLogic.urgent(rem, System.currentTimeMillis()))
    }

    private val money: Flow<MoneyPart> = run {
        val fy = HomeLogic.businessRange(today)
        val business = combine(ledger.businessTotals(fy), ledger.businessBySection(fy)) { t, rows ->
            BusinessUi(t, HomeLogic.businessBreakdown(rows), SeasonRange.cropYearOf(today))
        }
        val farm = repos.master.activeCycles()
            .map { cycles -> HomeLogic.farmPeriod(cycles.map { it.cycle }, today) }
            .flatMapLatest { p -> ledger.farmTotals(p.range).map { p to it } }
        combine(business, farm, ledger.animalTotals(month), ledger.animalByCategory(month)) { b, f, a, ac ->
            MoneyPart(b, f, a, ac)
        }
    }

    private val monthPart: Flow<MonthPart> = combine(
        ledger.businessBySection(month).map { rows -> rows.filter { it.section == AccountSection.FAMILY }.sumOf { it.expensePaise } },
        ledger.generalExpense(month),
        ledger.personalTotals(month),
        repos.shopping.activeCount(),
        repos.assets.activeLoanCount(),
    ) { fam, gen, per, shop, lent -> MonthPart(fam, gen, per.expense, per.net, shop, lent) }

    private val header: Flow<HeaderPart> = combine(
        repos.profiles.owner(),
        deps.backupInfo.map { it.status },
        repos.panchang.allCustomFestivals().map { custom ->
            HomeLogic.upcoming(today) { d -> PanchangSource.special(d, custom) }
        },
        deps.themeRepository.showHomeNumbers,
    ) { owner, backup, upcoming, show -> HeaderPart(owner, backup, upcoming, show) }

    private val recent: Flow<List<LedgerRowUi>> =
        combine(ledger.recent(HomeLogic.MAX_RECENT), names) { list, n -> list.map { HomeLogic.row(it, n, sectionLabel) } }

    val state: StateFlow<HomeUiState> = combine(header, alerts, money, monthPart, recent) { h, a, m, mp, r ->
        HomeUiState(
            today = today,
            panchang = panchangDay,
            upcoming = h.upcoming,
            ownerName = h.owner?.name.orEmpty(),
            ownerAddress = h.owner?.address,
            ownerPhoto = h.owner?.photoPath,
            backup = h.backup,
            udhaar = a.udhaar,
            pending = a.pending,
            overdue = a.overdue.first,
            overdueMore = a.overdue.second,
            urgent = a.urgent,
            business = m.business,
            farm = m.farm.second,
            farmPeriod = m.farm.first,
            animal = m.animal,
            animalCategories = m.animalCategories,
            familyMonth = mp.family,
            generalMonth = mp.general,
            personalExpenseMonth = mp.personal,
            personalNetMonth = mp.personalNet,
            showNumbers = h.showNumbers,
            shoppingPending = mp.shop,
            assetsLent = mp.lent,
            recent = r,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HomeUiState(today, panchang = panchangDay))

    /** "ચૂકતે" on a PendingUdhaar row (doc 05 logic 2). The card updates live through the Flows. */
    fun settle(id: Long) {
        viewModelScope.launch { ledger.settle(id) }
    }
}
