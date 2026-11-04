package com.vraj.khetihisab.ui.assets

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.vraj.khetihisab.data.dao.BorrowerSuggestion
import com.vraj.khetihisab.data.entity.AssetEntity
import com.vraj.khetihisab.data.entity.AssetLoanEntity
import com.vraj.khetihisab.panchang.GujaratiPanchangHelper
import com.vraj.khetihisab.share.PhoneNormalizer
import com.vraj.khetihisab.share.WhatsAppTemplates
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate

enum class LendError { NAME, ASSET, RETURN_BEFORE_HANDOVER }

enum class LendDialog { NO_PHOTO, ALREADY_LENT, NEW_ASSET }

/** Editable part of the Lend form. */
data class LendForm(
    val photoPath: String? = null,
    val photoAt: java.time.LocalDateTime? = null,
    val name: String = "",
    val phone: String = "",
    val assetId: Long? = null,
    val handover: LocalDate,
    /** 1 / 3 / 7 / 15, or null when [customReturn] is used. */
    val returnDays: Int? = 3,
    val customReturn: LocalDate? = null,
    val notify: Boolean = true,
) {
    val expectedReturn: LocalDate get() = customReturn ?: handover.plusDays((returnDays ?: 0).toLong())
}

data class LendUiState(
    val form: LendForm,
    val assets: List<AssetEntity> = emptyList(),
    val suggestions: List<BorrowerSuggestion> = emptyList(),
    val handoverTithi: String = "",
    val preview: String = "",
    val errors: Set<LendError> = emptySet(),
    val dialog: LendDialog? = null,
    val saving: Boolean = false,
) {
    val selectedAsset: AssetEntity? get() = assets.firstOrNull { it.id == form.assetId }
}

sealed interface LendEvent {
    /** Saved. When [sendText] is non-null the screen opens WhatsApp, then calls [LendViewModel.onReminderOpened]. */
    data class Saved(val loanId: Long, val phone: String?, val sendText: String?) : LendEvent
}

/** Lend Asset form (doc 11, artboard 10): photo → borrower → asset → dates → WhatsApp preview. */
class LendViewModel(
    private val store: AssetsStore,
    preselectedAssetId: Long? = null,
    private val today: () -> LocalDate = LocalDate::now,
    private val tithiOf: (LocalDate) -> String = GujaratiPanchangHelper::shortLabel,
    private val loanTemplate: () -> String = { WhatsAppTemplates.DEFAULT_LOAN_REMINDER },
) : ViewModel() {

    private val form = MutableStateFlow(LendForm(assetId = preselectedAssetId, handover = today()))
    private val meta = MutableStateFlow(Meta())

    private data class Meta(
        val errors: Set<LendError> = emptySet(),
        val dialog: LendDialog? = null,
        val saving: Boolean = false,
        val photoConfirmed: Boolean = false,
        val lentConfirmed: Boolean = false,
        val sendAfterSave: Boolean = false,
        /** A save is in progress and waiting on a dialog answer. */
        val pendingSave: Boolean = false,
    )

    private val events = Channel<LendEvent>(Channel.BUFFERED)
    val eventFlow = events.receiveAsFlow()

    private val owner = store.owner().map {
        OwnerSignature(it?.name.orEmpty(), WhatsAppTemplates.villageFromAddress(it?.address))
    }

    /** Assets not currently lent, plus the preselected one (so the "already lent" warning can apply). */
    private val assets = combine(store.inventory(), form) { inv, f ->
        inv.filter { !it.isLent || it.asset.id == f.assetId }.map { it.asset }
    }

    private val suggestions = store.previousBorrowers().map(::dedupeBorrowers)

    val state: StateFlow<LendUiState> = combine(form, assets, suggestions, owner, meta) { f, a, s, o, m ->
        val assetName = a.firstOrNull { it.id == f.assetId }?.name.orEmpty()
        LendUiState(
            form = f,
            assets = a,
            suggestions = s,
            handoverTithi = runCatching { tithiOf(f.handover) }.getOrDefault(""),
            preview = previewText(f, assetName, o),
            errors = m.errors,
            dialog = m.dialog,
            saving = m.saving,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), LendUiState(form = form.value))

    init {
        if (preselectedAssetId != null) {
            viewModelScope.launch {
                if (store.isCurrentlyLent(preselectedAssetId)) meta.update { it.copy(dialog = LendDialog.ALREADY_LENT) }
            }
        }
    }

    // ---------- inputs ----------
    fun onPhoto(path: String, at: java.time.LocalDateTime = java.time.LocalDateTime.now()) = form.update { it.copy(photoPath = path, photoAt = at) }
    fun onName(v: String) {
        form.update { it.copy(name = v) }
        clearError(LendError.NAME)
    }
    fun onPhone(v: String) = form.update { it.copy(phone = v.filter { c -> c.isDigit() || c == '+' || c == ' ' }) }

    fun onContactPicked(name: String?, phone: String?) = form.update {
        it.copy(name = if (it.name.isBlank() && !name.isNullOrBlank()) name else it.name, phone = phone ?: it.phone)
    }.also { if (!name.isNullOrBlank()) clearError(LendError.NAME) }

    fun onSuggestion(s: BorrowerSuggestion) {
        form.update { it.copy(name = s.borrowerName, phone = s.borrowerPhone.orEmpty()) }
        clearError(LendError.NAME)
    }

    fun onAsset(id: Long) {
        form.update { it.copy(assetId = id) }
        meta.update { it.copy(lentConfirmed = false) }
        clearError(LendError.ASSET)
    }

    fun onHandover(d: LocalDate) {
        form.update { it.copy(handover = d) }
        clearError(LendError.RETURN_BEFORE_HANDOVER)
    }

    fun onReturnDays(days: Int) {
        form.update { it.copy(returnDays = days, customReturn = null) }
        clearError(LendError.RETURN_BEFORE_HANDOVER)
    }

    fun onCustomReturn(d: LocalDate) {
        form.update { it.copy(returnDays = null, customReturn = d) }
        clearError(LendError.RETURN_BEFORE_HANDOVER)
    }

    fun onNotify(on: Boolean) = form.update { it.copy(notify = on) }

    fun showNewAsset() = meta.update { it.copy(dialog = LendDialog.NEW_ASSET) }

    fun addAsset(name: String) {
        val n = name.trim()
        dismissDialog()
        if (n.isEmpty()) return
        viewModelScope.launch {
            val id = store.addAsset(n)
            onAsset(id)
        }
    }

    fun dismissDialog() = meta.update { it.copy(dialog = null, saving = false, pendingSave = false) }

    // ---------- save ----------
    /** "સાચવો" ([send] = false) or "સાચવો + મોકલો" ([send] = true). */
    fun save(send: Boolean) {
        if (meta.value.saving) return
        meta.update { it.copy(sendAfterSave = send, pendingSave = true) }
        proceed()
    }

    /** Confirms the open dialog ("ફોટો વગર સાચવવું છે?" or "પહેલેથી ઉછીનું આપેલ છે") and continues saving. */
    fun confirmDialog() {
        when (meta.value.dialog) {
            LendDialog.NO_PHOTO -> meta.update { it.copy(dialog = null, photoConfirmed = true) }.also { proceed() }
            LendDialog.ALREADY_LENT -> {
                meta.update { it.copy(dialog = null, lentConfirmed = true) }
                // Shown on entry (route lend/{assetId}) there is no save to continue.
                if (meta.value.pendingSave) proceed()
            }
            else -> dismissDialog()
        }
    }

    private fun proceed() {
        val f = form.value
        val errs = validate(f)
        if (errs.isNotEmpty()) {
            meta.update { it.copy(errors = errs, pendingSave = false) }
            return
        }
        if (f.photoPath == null && !meta.value.photoConfirmed) {
            meta.update { it.copy(dialog = LendDialog.NO_PHOTO) }
            return
        }
        meta.update { it.copy(saving = true) }
        viewModelScope.launch {
            val assetId = f.assetId!!
            if (!meta.value.lentConfirmed && store.isCurrentlyLent(assetId)) {
                meta.update { it.copy(dialog = LendDialog.ALREADY_LENT, saving = false) }
                return@launch
            }
            val asset = state.value.assets.firstOrNull { it.id == assetId }
            val sig = owner.first()
            val phone = f.phone.trim().ifEmpty { null }
            val loanId = withContext(NonCancellable) {
                store.lend(
                    AssetLoanEntity(
                        assetId = assetId,
                        borrowerName = f.name.trim(),
                        borrowerPhone = phone,
                        borrowerPhotoPath = f.photoPath,
                        handedOverDate = f.handover,
                        handedOverTithi = runCatching { tithiOf(f.handover) }.getOrDefault(""),
                        expectedReturnDate = f.expectedReturn,
                        notifyOnDue = f.notify,
                        createdAt = 0,
                        updatedAt = 0,
                    ),
                )
            }
            val text = if (meta.value.sendAfterSave) previewText(f, asset?.name.orEmpty(), sig) else null
            meta.update { it.copy(saving = false, pendingSave = false) }
            events.send(LendEvent.Saved(loanId, phone, text))
        }
    }

    /** Records that the reminder was opened right after saving. Survives leaving the screen. */
    fun onReminderOpened(loanId: Long) {
        viewModelScope.launch { withContext(NonCancellable) { store.markReminded(loanId) } }
    }

    private fun clearError(e: LendError) = meta.update { it.copy(errors = it.errors - e) }

    private fun previewText(f: LendForm, assetName: String, o: OwnerSignature): String = WhatsAppTemplates.loanReminder(
        borrower = f.name,
        asset = assetName,
        handedOver = f.handover,
        expectedReturn = f.expectedReturn,
        ownerName = o.name,
        village = o.village,
        today = f.handover,
        template = loanTemplate(),
    )

    companion object {
        val RETURN_DAY_OPTIONS = listOf(1, 3, 7, 15)

        fun validate(f: LendForm): Set<LendError> = buildSet {
            if (f.name.isBlank()) add(LendError.NAME)
            if (f.assetId == null) add(LendError.ASSET)
            if (f.expectedReturn.isBefore(f.handover)) add(LendError.RETURN_BEFORE_HANDOVER)
        }

        /** One chip per person: the same number in different formats counts once (doc 11 edge case). */
        fun dedupeBorrowers(list: List<BorrowerSuggestion>): List<BorrowerSuggestion> {
            val seenPhones = HashSet<String>()
            val seenNames = HashSet<String>()
            return list.filter { s ->
                val p = PhoneNormalizer.toE164NoPlus(s.borrowerPhone)
                val n = s.borrowerName.trim()
                val fresh = (p == null || seenPhones.add(p)) && seenNames.add(n)
                fresh
            }
        }

        fun factory(store: AssetsStore, preselectedAssetId: Long?, loanTemplate: () -> String): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T =
                    LendViewModel(store, preselectedAssetId, loanTemplate = loanTemplate) as T
            }
    }
}
