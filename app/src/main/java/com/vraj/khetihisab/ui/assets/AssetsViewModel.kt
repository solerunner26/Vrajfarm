package com.vraj.khetihisab.ui.assets

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.vraj.khetihisab.data.entity.AssetEntity
import com.vraj.khetihisab.data.model.AssetWithStatus
import com.vraj.khetihisab.data.model.LoanWithAsset
import com.vraj.khetihisab.share.WhatsAppTemplates
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate

enum class AssetsTab { LENT, ALL, HISTORY }

data class LoanCard(val loan: LoanWithAsset, val status: LoanStatus)

data class AssetTile(val asset: AssetEntity, val isLent: Boolean, val borrowerName: String?)

data class AssetDetail(val asset: AssetEntity, val isLent: Boolean, val loans: List<LoanWithAsset>)

data class AssetsUiState(
    val tab: AssetsTab = AssetsTab.LENT,
    val query: String = "",
    val lent: List<LoanCard> = emptyList(),
    val inventory: List<AssetTile> = emptyList(),
    val history: List<LoanWithAsset> = emptyList(),
    val confirmReturn: LoanWithAsset? = null,
    val detail: AssetDetail? = null,
    val fullPhoto: String? = null,
    val loaded: Boolean = false,
) {
    val lentCount: Int get() = lent.size
    val allCount: Int get() = inventory.size
}

sealed interface AssetsEvent {
    /** Screen calls `WhatsAppLauncher.open(context, phone, text)` then [AssetsViewModel.onReminderOpened]. */
    data class OpenWhatsApp(val loanId: Long, val phone: String?, val text: String) : AssetsEvent
    data class Dial(val phone: String) : AssetsEvent
    data object Returned : AssetsEvent
    data object DeleteBlocked : AssetsEvent
}

/** Owner signature for the WhatsApp templates. */
data class OwnerSignature(val name: String, val village: String)

/** Assets & lending (doc 11, artboard 09). */
@OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)
class AssetsViewModel(
    private val store: AssetsStore,
    private val today: () -> LocalDate = LocalDate::now,
    private val loanTemplate: () -> String = { WhatsAppTemplates.DEFAULT_LOAN_REMINDER },
    remindLoanId: Long? = null,
) : ViewModel() {

    private val tab = MutableStateFlow(AssetsTab.LENT)
    private val query = MutableStateFlow("")
    private val confirmReturn = MutableStateFlow<LoanWithAsset?>(null)
    private val detailId = MutableStateFlow<Long?>(null)
    private val fullPhoto = MutableStateFlow<String?>(null)

    private val events = Channel<AssetsEvent>(Channel.BUFFERED)
    val eventFlow = events.receiveAsFlow()

    private val owner = store.owner().map {
        OwnerSignature(it?.name.orEmpty(), WhatsAppTemplates.villageFromAddress(it?.address))
    }

    private val active = store.activeLoans()
    private val history = query.debounce(SEARCH_DEBOUNCE_MS).map { it.trim() }.distinctUntilChanged()
        .flatMapLatest { store.searchHistory(it) }

    private val detail = detailId.flatMapLatest { id ->
        if (id == null) flowOf(null) else combine(store.inventory(), store.loansForAsset(id)) { inv, loans ->
            inv.firstOrNull { it.asset.id == id }?.let { AssetDetail(it.asset, it.isLent, loans) }
        }
    }

    private val lists = combine(active, store.inventory(), history) { act, inv, hist ->
        val d = today()
        val cards = LoanStatus.sortActive(act, d).map { LoanCard(it, LoanStatus.of(it.loan.expectedReturnDate, d)) }
        Triple(cards, tiles(inv, act), hist)
    }

    private val overlays = combine(confirmReturn, detail, fullPhoto) { c, d, p -> Triple(c, d, p) }

    val state: StateFlow<AssetsUiState> = combine(tab, query, lists, overlays) { t, q, (cards, tiles, hist), (c, d, p) ->
        AssetsUiState(
            tab = t, query = q, lent = cards, inventory = tiles, history = hist,
            confirmReturn = c, detail = d, fullPhoto = p, loaded = true,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AssetsUiState())

    init {
        // Notification action "યાદ અપાવો" (OverdueLoanWorker) → open WhatsApp for that loan.
        if (remindLoanId != null) {
            viewModelScope.launch { store.loanById(remindLoanId)?.let { remind(it) } }
        }
    }

    fun selectTab(t: AssetsTab) {
        tab.value = t
    }

    /** Typing a query jumps to the history tab ("કોણ લઈ ગયું?"). */
    fun onQuery(q: String) {
        query.value = q
        if (q.isNotBlank()) tab.value = AssetsTab.HISTORY
    }

    fun remind(loan: LoanWithAsset) {
        viewModelScope.launch {
            val sig = owner.first()
            val text = WhatsAppTemplates.loanReminder(
                borrower = loan.loan.borrowerName,
                asset = loan.asset.name,
                handedOver = loan.loan.handedOverDate,
                expectedReturn = loan.loan.expectedReturnDate,
                ownerName = sig.name,
                village = sig.village,
                today = today(),
                template = loanTemplate(),
            )
            events.send(AssetsEvent.OpenWhatsApp(loan.loan.id, loan.loan.borrowerPhone, text))
        }
    }

    /** Records `lastWhatsAppReminderAt` (opened, not delivered). Survives the screen being left. */
    fun onReminderOpened(loanId: Long) {
        viewModelScope.launch { withContext(NonCancellable) { store.markReminded(loanId) } }
    }

    fun call(loan: LoanWithAsset) {
        val p = loan.loan.borrowerPhone?.takeIf { it.isNotBlank() } ?: return
        viewModelScope.launch { events.send(AssetsEvent.Dial(p)) }
    }

    fun askReturn(loan: LoanWithAsset) {
        confirmReturn.value = loan
    }

    fun cancelReturn() {
        confirmReturn.value = null
    }

    /** "પરત મળ્યું ✓" confirmed → markReturned (also closes the linked reminder). */
    fun confirmReturned() {
        val l = confirmReturn.value ?: return
        confirmReturn.value = null
        viewModelScope.launch {
            withContext(NonCancellable) { store.markReturned(l.loan.id) }
            events.send(AssetsEvent.Returned)
        }
    }

    fun openDetail(assetId: Long) {
        detailId.value = assetId
    }

    fun closeDetail() {
        detailId.value = null
    }

    fun editAsset(assetId: Long, name: String, priceText: String) {
        if (name.isBlank()) return
        viewModelScope.launch { store.updateAsset(assetId, name, com.vraj.khetihisab.ui.shopping.MoneyInput.toPaise(priceText)) }
    }

    fun deleteAsset(assetId: Long) {
        viewModelScope.launch {
            if (store.deleteAsset(assetId)) detailId.value = null else events.send(AssetsEvent.DeleteBlocked)
        }
    }

    fun showPhoto(path: String?) {
        fullPhoto.value = path
    }

    companion object {
        const val SEARCH_DEBOUNCE_MS = 250L

        fun tiles(inv: List<AssetWithStatus>, active: List<LoanWithAsset>): List<AssetTile> {
            val borrower = active.associate { it.loan.assetId to it.loan.borrowerName }
            return inv.map { AssetTile(it.asset, it.isLent, borrower[it.asset.id]) }
        }

        fun factory(store: AssetsStore, loanTemplate: () -> String, remindLoanId: Long?): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T =
                    AssetsViewModel(store, loanTemplate = loanTemplate, remindLoanId = remindLoanId) as T
            }
    }
}
