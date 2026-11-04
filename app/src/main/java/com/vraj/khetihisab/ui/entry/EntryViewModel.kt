package com.vraj.khetihisab.ui.entry

import android.os.Bundle
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vraj.khetihisab.data.entity.AccountSection
import com.vraj.khetihisab.data.entity.CategoryMasterEntity
import com.vraj.khetihisab.data.entity.TransactionEntity
import com.vraj.khetihisab.data.entity.TxnType
import com.vraj.khetihisab.panchang.GujaratiPanchangHelper
import com.vraj.khetihisab.ui.home.ScreensDeps
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate

/** A tile / chip choice in tap 2. */
data class EntryOption(val id: Long, val label: String, val sub: String? = null, val isCropSale: Boolean = false)

data class EntryLists(
    val farms: List<EntryOption> = emptyList(),
    val families: List<EntryOption> = emptyList(),
    val categories: List<EntryOption> = emptyList(),
)

class EntryViewModel(private val deps: ScreensDeps, private val handle: SavedStateHandle) : ViewModel() {
    private val ledger = deps.repos.ledger
    private val master = deps.repos.master

    private val _form = MutableStateFlow(restoreOrInitial())
    val form: StateFlow<EntryForm> = _form.asStateFlow()

    /** An event waiting for the "રકમ બદલાશે" confirm (simple amount → APMC). */
    private val _confirm = MutableStateFlow<EntryEvent?>(null)
    val pendingConfirm: StateFlow<EntryEvent?> = _confirm.asStateFlow()

    private val _savedId = MutableStateFlow<Long?>(null)
    val savedId: StateFlow<Long?> = _savedId.asStateFlow()

    private var saved = false

    /** The bill already stored on the row being edited; never deleted unless the edit is saved. */
    private var originalBill: String? = null

    init {
        val edit = _form.value.editId
        if (edit != null && handle.get<Bundle>(KEY_FORM) == null) {
            viewModelScope.launch {
                val t = ledger.byId(edit) ?: return@launch
                val cats = master.allCategories().first()
                originalBill = t.billPhotoPath
                set(formOf(t, cats.firstOrNull { it.id == t.categoryId }?.name == CROP_SALE))
            }
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    val lists: StateFlow<EntryLists> = combine(
        master.farms(),
        master.activeCycles(),
        master.families(),
        _form.map { it.section to it.txnType },
    ) { farms, cycles, families, sel -> Triple(farms to cycles, families, sel) }
        .flatMapLatest { (fc, families, sel) ->
            val (farms, cycles) = fc
            val cropByFarm = cycles.associate { it.cycle.farmId to it.crop.name }
            val farmOpts = farms.map { EntryOption(it.id, it.name, cropByFarm[it.id]) }
            val famOpts = families.map { EntryOption(it.id, it.displayName) }
            val (section, type) = sel
            val cats = if (section == null) flowOf(emptyList()) else master.categories(section, type)
            cats.map { list -> EntryLists(farmOpts, famOpts, list.map(::option)) }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), EntryLists())

    private fun option(c: CategoryMasterEntity) = EntryOption(c.id, c.name, isCropSale = c.name == CROP_SALE)

    fun onEvent(e: EntryEvent) {
        val cur = _form.value
        val next = EntryReducer.reduce(cur, e)
        if (!cur.apmcMode && next.apmcMode && cur.amountText.isNotEmpty()) {
            _confirm.value = e
            return
        }
        if (e is EntryEvent.SetBill && cur.billPath != null && cur.billPath != e.path && cur.billPath != originalBill) BillPhotoHelper.discard(cur.billPath)
        set(next)
    }

    fun confirmApmc(accept: Boolean) {
        val e = _confirm.value ?: return
        _confirm.value = null
        if (accept) set(EntryReducer.reduce(_form.value.copy(amountText = ""), e))
    }

    private fun set(f: EntryForm) {
        _form.value = f
        handle[KEY_FORM] = f.toBundle()
    }

    fun save() {
        val f = _form.value
        if (!f.canSave || saved) return
        saved = true
        viewModelScope.launch {
            val cycleId = if (f.section == AccountSection.FARM && f.farmId != null) {
                master.activeCycleFor(f.farmId).first()?.cycle?.id
            } else {
                null
            }
            val now = System.currentTimeMillis()
            val apmc = f.apmcMode
            val tithi = runCatching { GujaratiPanchangHelper.shortLabel(f.date) }.getOrDefault("")
            val t = TransactionEntity(
                date = f.date,
                tithiLabel = tithi,
                section = f.section!!,
                type = f.txnType,
                amountPaise = f.amountPaise,
                payStatus = f.payStatus,
                counterparty = f.counterparty.trim().ifEmpty { null }.takeIf { f.pending },
                farmId = f.farmId.takeIf { f.section == AccountSection.FARM },
                cycleId = cycleId,
                familyId = f.familyId.takeIf { f.section == AccountSection.FAMILY },
                categoryId = f.categoryId ?: familyDefaultCategory(f),
                note = f.voiceNote,
                voiceNote = f.voiceNote,
                billPhotoPath = f.billPath,
                mann = if (apmc) f.mann else null,
                extraKg = if (apmc) f.extraKg else null,
                ratePerMannPaise = if (apmc) f.ratePerMannPaise else null,
                createdAt = now,
                updatedAt = now,
            )
            val editId = f.editId
            if (editId != null) {
                val old = ledger.byId(editId)
                if (old != null) {
                    if (old.billPhotoPath != null && old.billPhotoPath != f.billPath) BillPhotoHelper.discard(old.billPhotoPath)
                    ledger.update(t.copy(id = old.id, createdAt = old.createdAt, cycleId = cycleId ?: old.cycleId))
                    _savedId.value = old.id
                    return@launch
                }
            }
            _savedId.value = ledger.insert(t)
        }
    }

    /** FAMILY rows carry the seeded "રોકડ ઉપાડ" category so reports can label them. */
    private suspend fun familyDefaultCategory(f: EntryForm): Long? =
        if (f.section != AccountSection.FAMILY) null
        else master.categories(AccountSection.FAMILY, TxnType.ADVANCE).first().firstOrNull()?.id

    override fun onCleared() {
        // A bill captured for an entry that was never saved is not kept.
        val bill = _form.value.billPath
        if (!saved && bill != originalBill) BillPhotoHelper.discard(bill)
    }

    private fun restoreOrInitial(): EntryForm {
        handle.get<Bundle>(KEY_FORM)?.let { return fromBundle(it) }
        val kind = if (handle.get<String>(ARG_KIND)?.lowercase() == "income") EntryKind.INCOME else EntryKind.EXPENSE
        val section = handle.get<String>(ARG_SECTION)?.let { s -> AccountSection.entries.firstOrNull { it.name == s.uppercase() } }
        val farm = handle.get<Long>(ARG_FARM)?.takeIf { it > 0 }
        val family = handle.get<Long>(ARG_FAMILY)?.takeIf { it > 0 }
        val date = handle.get<Long>(ARG_DATE)?.takeIf { it != Long.MIN_VALUE }?.let { runCatching { LocalDate.ofEpochDay(it) }.getOrNull() }
        val edit = handle.get<Long>(ARG_EDIT)?.takeIf { it > 0 }
        return EntryForm(
            editId = edit,
            date = date ?: com.vraj.khetihisab.ui.home.ScreensFormat.today(),
            kind = kind,
            section = section ?: when {
                farm != null -> AccountSection.FARM
                family != null -> AccountSection.FAMILY
                else -> null
            },
            farmId = farm,
            familyId = family,
        )
    }

    companion object {
        const val ARG_KIND = "kind"
        const val ARG_SECTION = "section"
        const val ARG_FARM = "farm"
        const val ARG_FAMILY = "family"
        const val ARG_DATE = "date"
        const val ARG_EDIT = "edit"

        /** An existing row back into the 3-tap form (Entry edit route). */
        fun formOf(t: TransactionEntity, cropSale: Boolean): EntryForm {
            val apmc = t.mann != null && t.ratePerMannPaise != null
            return EntryForm(
                kind = if (t.type == TxnType.INCOME) EntryKind.INCOME else EntryKind.EXPENSE,
                section = t.section,
                farmId = t.farmId,
                familyId = t.familyId,
                categoryId = t.categoryId,
                categoryIsCropSale = cropSale,
                amountText = if (apmc) "" else paiseText(t.amountPaise),
                apmcSwitch = apmc && !cropSale,
                mannText = if (apmc) AmountInput.format(t.mann ?: 0.0) else "",
                kgText = if (apmc) AmountInput.format(t.extraKg ?: 0.0) else "",
                rateText = if (apmc) paiseText(t.ratePerMannPaise ?: 0L) else "",
                pending = t.payStatus != com.vraj.khetihisab.data.entity.PayStatus.PAID,
                counterparty = t.counterparty.orEmpty(),
                voiceNote = t.voiceNote ?: t.note,
                billPath = t.billPhotoPath,
                date = t.date,
                editId = t.id,
            )
        }

        fun paiseText(p: Long): String = if (p % 100 == 0L) (p / 100).toString() else "%d.%02d".format(p / 100, p % 100)
        private const val KEY_FORM = "entry_form"
        const val CROP_SALE = "પાક વેચાણ"

        fun EntryForm.toBundle(): Bundle = Bundle().apply {
            putString("kind", kind.name)
            putString("section", section?.name)
            putLong("farm", farmId ?: -1)
            putLong("family", familyId ?: -1)
            putLong("cat", categoryId ?: -1)
            putBoolean("crop", categoryIsCropSale)
            putString("amount", amountText)
            putBoolean("apmc", apmcSwitch)
            putString("field", activeField.name)
            putString("mann", mannText)
            putString("kg", kgText)
            putString("rate", rateText)
            putBoolean("pending", pending)
            putString("cp", counterparty)
            putString("voice", voiceNote)
            putString("bill", billPath)
            putLong("date", date.toEpochDay())
            putLong("edit", editId ?: -1)
        }

        fun fromBundle(b: Bundle): EntryForm = EntryForm(
            kind = EntryKind.valueOf(b.getString("kind") ?: EntryKind.EXPENSE.name),
            section = b.getString("section")?.let(AccountSection::valueOf),
            farmId = b.getLong("farm", -1).takeIf { it > 0 },
            familyId = b.getLong("family", -1).takeIf { it > 0 },
            categoryId = b.getLong("cat", -1).takeIf { it > 0 },
            categoryIsCropSale = b.getBoolean("crop"),
            amountText = b.getString("amount").orEmpty(),
            apmcSwitch = b.getBoolean("apmc"),
            activeField = ApmcField.valueOf(b.getString("field") ?: ApmcField.MANN.name),
            mannText = b.getString("mann").orEmpty(),
            kgText = b.getString("kg").orEmpty(),
            rateText = b.getString("rate").orEmpty(),
            pending = b.getBoolean("pending"),
            counterparty = b.getString("cp").orEmpty(),
            voiceNote = b.getString("voice"),
            billPath = b.getString("bill"),
            date = LocalDate.ofEpochDay(b.getLong("date", LocalDate.now().toEpochDay())),
            editId = b.getLong("edit", -1).takeIf { it > 0 },
        )
    }
}
