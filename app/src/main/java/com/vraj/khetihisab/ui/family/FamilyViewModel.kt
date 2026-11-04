package com.vraj.khetihisab.ui.family

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vraj.khetihisab.data.entity.FamilyEntity
import com.vraj.khetihisab.data.entity.PersonProfileEntity
import com.vraj.khetihisab.data.entity.TransactionEntity
import com.vraj.khetihisab.data.repo.DataRepositories
import com.vraj.khetihisab.data.repo.DateRange
import com.vraj.khetihisab.share.PhoneNormalizer
import com.vraj.khetihisab.share.WhatsAppTemplateStore
import com.vraj.khetihisab.share.WhatsAppTemplates
import com.vraj.khetihisab.ui.home.ScreensFormat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.YearMonth

/** Data for the Family advance ledger (doc 08). */
interface FamilySource {
    fun families(): Flow<List<FamilyEntity>>
    fun monthTotal(familyId: Long, ym: YearMonth): Flow<Long>
    fun yearTotal(familyId: Long, year: Int): Flow<Long>
    fun entries(familyId: Long, ym: YearMonth): Flow<List<TransactionEntity>>
    fun categoryNames(): Flow<Map<Long, String>>
    fun owner(): Flow<PersonProfileEntity?>
    fun template(): String
    suspend fun savePhone(familyId: Long, phone: String)
    suspend fun delete(id: Long)
}

class RepoFamilySource(private val repos: DataRepositories, private val templates: WhatsAppTemplateStore) : FamilySource {
    override fun families() = repos.master.families()
    override fun monthTotal(familyId: Long, ym: YearMonth) = repos.ledger.familyAdvanceForMonth(familyId, ym)
    override fun yearTotal(familyId: Long, year: Int) = repos.ledger.familyAdvanceForYear(familyId, year)
    override fun entries(familyId: Long, ym: YearMonth) = repos.ledger.familyEntries(familyId, DateRange.month(ym))
    override fun categoryNames(): Flow<Map<Long, String>> = repos.master.allCategories().map { l -> l.associate { it.id to it.name } }
    override fun owner() = repos.profiles.owner()
    override fun template() = templates.familyTemplate()
    override suspend fun savePhone(familyId: Long, phone: String) {
        val code = repos.master.familyById(familyId)?.code ?: return
        repos.profiles.updateBhagiyaPhone(code, phone)
    }
    override suspend fun delete(id: Long) = repos.ledger.softDelete(id)
}

data class FamilyRowUi(val id: Long, val title: String, val meta: String, val amountPaise: Long)

data class FamilyUiState(
    val code: String = FamilyLogic.CODE_A,
    val month: YearMonth,
    val family: FamilyEntity? = null,
    val monthTotal: Long = 0,
    val yearTotal: Long = 0,
    val rows: List<FamilyRowUi> = emptyList(),
    /** Exactly the text handed to WhatsApp (doc 08 acceptance: byte for byte). */
    val receipt: String = "",
    /** Tab labels: family code → ભાગિયા name (mirrored from the profile). */
    val names: Map<String, String> = emptyMap(),
) {
    val phone: String? get() = family?.whatsappPhone?.takeIf { it.isNotBlank() }
    val displayName: String get() = family?.displayName.orEmpty()
}

/** Pure helpers for the Family screen (unit-tested). */
object FamilyLogic {
    const val CODE_A = "A"
    const val CODE_B = "B"

    fun pick(families: List<FamilyEntity>, code: String): FamilyEntity? =
        families.firstOrNull { it.code.equals(code, ignoreCase = true) }

    /** Statement lines oldest first; the on-screen list stays newest first. */
    fun statementLines(entries: List<TransactionEntity>, categories: Map<Long, String>): List<WhatsAppTemplates.FamilyEntryLine> =
        entries.sortedWith(compareBy<TransactionEntity> { it.date }.thenBy { it.createdAt })
            .map { WhatsAppTemplates.FamilyEntryLine(it.date, it.amountPaise, noteOf(it, categories)) }

    fun noteOf(t: TransactionEntity, categories: Map<Long, String>): String? =
        t.note?.trim()?.takeIf { it.isNotEmpty() } ?: t.categoryId?.let(categories::get)

    fun receipt(
        family: FamilyEntity?,
        month: YearMonth,
        monthTotal: Long,
        yearTotal: Long,
        entries: List<TransactionEntity>,
        categories: Map<Long, String>,
        owner: PersonProfileEntity?,
        template: String,
    ): String = WhatsAppTemplates.familyStatement(
        displayName = family?.displayName.orEmpty(),
        month = month,
        monthTotalPaise = monthTotal,
        entries = statementLines(entries, categories),
        yearTotalPaise = yearTotal,
        ownerName = owner?.name.orEmpty(),
        village = WhatsAppTemplates.villageFromAddress(owner?.address),
        template = template,
    )

    fun rows(entries: List<TransactionEntity>, categories: Map<Long, String>, fallback: String): List<FamilyRowUi> = entries.map {
        FamilyRowUi(
            id = it.id,
            title = noteOf(it, categories) ?: fallback,
            meta = if (it.tithiLabel.isBlank()) ScreensFormat.dmy(it.date) else "${ScreensFormat.dmy(it.date)} · ${it.tithiLabel}",
            amountPaise = it.amountPaise,
        )
    }

    /** A usable WhatsApp number: 10 Indian digits after normalising +91 / spaces. */
    fun isValidPhone(raw: String): Boolean = PhoneNormalizer.toE164NoPlus(raw) != null
}

private data class Selection(val code: String, val month: YearMonth)

@OptIn(ExperimentalCoroutinesApi::class)
class FamilyViewModel(
    private val source: FamilySource,
    private val handle: SavedStateHandle,
    today: LocalDate = ScreensFormat.today(),
    private val fallbackTitle: String = "",
) : ViewModel() {

    private val selection = MutableStateFlow(
        Selection(
            handle.get<String>(KEY_CODE) ?: FamilyLogic.CODE_A,
            handle.get<String>(KEY_MONTH)?.let { runCatching { YearMonth.parse(it) }.getOrNull() } ?: YearMonth.from(today),
        ),
    )

    val state: StateFlow<FamilyUiState> = combine(selection, source.families(), ::Pair)
        .flatMapLatest { (sel, families) ->
            val fam = FamilyLogic.pick(families, sel.code)
            val names = families.associate { it.code to it.displayName }
            if (fam == null) {
                flowOf(FamilyUiState(sel.code, sel.month, names = names))
            } else {
                combine(
                    source.monthTotal(fam.id, sel.month),
                    source.yearTotal(fam.id, sel.month.year),
                    source.entries(fam.id, sel.month),
                    source.categoryNames(),
                    source.owner(),
                ) { m, y, entries, cats, owner ->
                    FamilyUiState(
                        code = sel.code,
                        month = sel.month,
                        family = fam,
                        monthTotal = m,
                        yearTotal = y,
                        rows = FamilyLogic.rows(entries, cats, fallbackTitle),
                        receipt = FamilyLogic.receipt(fam, sel.month, m, y, entries, cats, owner, source.template()),
                        names = names,
                    )
                }
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), FamilyUiState(selection.value.code, selection.value.month))

    fun selectFamily(code: String) = update(selection.value.copy(code = code))

    fun selectMonth(ym: YearMonth) = update(selection.value.copy(month = ym))

    fun savePhone(phone: String) {
        val id = state.value.family?.id ?: return
        viewModelScope.launch { source.savePhone(id, phone.trim()) }
    }

    fun delete(id: Long) {
        viewModelScope.launch { source.delete(id) }
    }

    private fun update(s: Selection) {
        selection.value = s
        handle[KEY_CODE] = s.code
        handle[KEY_MONTH] = s.month.toString()
    }

    companion object {
        const val KEY_CODE = "family_code"
        const val KEY_MONTH = "family_month"
    }
}
