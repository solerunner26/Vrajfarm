package com.vraj.khetihisab.ui.shopping

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.vraj.khetihisab.data.entity.FarmEntity
import com.vraj.khetihisab.data.entity.ShopTab
import com.vraj.khetihisab.data.entity.ShoppingItemEntity
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate

/** One row of the list. [struck] = ticked and waiting the 1.5 s before it leaves the list. */
data class ShopRow(
    val id: Long,
    val name: String,
    val farmName: String?,
    val isTool: Boolean,
    val pricePaise: Long?,
    val struck: Boolean,
    val item: ShoppingItemEntity,
)

data class ShopGroup(val date: LocalDate, val isToday: Boolean, val rows: List<ShopRow>)

data class ShopDraft(
    val name: String = "",
    val date: LocalDate,
    val farmId: Long? = null,
    val priceText: String = "",
    val isTool: Boolean = false,
    /** Once the user touches the checkbox, keyword auto-ticking stops. */
    val toolTouched: Boolean = false,
    val expanded: Boolean = false,
)

data class ConvertSheet(val itemId: Long, val name: String, val priceText: String, val purchaseDate: LocalDate)

data class ShoppingUiState(
    val tab: ShopTab = ShopTab.FARM,
    val groups: List<ShopGroup> = emptyList(),
    val farms: List<FarmEntity> = emptyList(),
    val draft: ShopDraft,
    val convert: ConvertSheet? = null,
    val loaded: Boolean = false,
)

sealed interface ShoppingEvent {
    /** Show the undo snackbar for a bought item. */
    data class Bought(val id: Long, val name: String) : ShoppingEvent
    data class AssetCreated(val assetId: Long) : ShoppingEvent
}

/** Shopping list (doc 11, artboard 11). */
@OptIn(ExperimentalCoroutinesApi::class)
class ShoppingViewModel(
    private val store: ShoppingStore,
    private val today: () -> LocalDate = LocalDate::now,
    private val removeDelayMs: Long = REMOVE_DELAY_MS,
) : ViewModel() {

    private val tab = MutableStateFlow(ShopTab.FARM)
    private val struck = MutableStateFlow<Set<Long>>(emptySet())
    private val draft = MutableStateFlow(ShopDraft(date = today()))
    private val convert = MutableStateFlow<ConvertSheet?>(null)
    private val pending = mutableMapOf<Long, Job>()

    private val events = Channel<ShoppingEvent>(Channel.BUFFERED)
    val eventFlow = events.receiveAsFlow()

    private val items = tab.flatMapLatest { store.active(it) }
    private val farms = store.farms()

    private val listState = combine(tab, items, farms, struck) { t, list, fs, s ->
        val names = fs.associate { it.id to it.name }
        Triple(t, group(list, names, s, today()), fs)
    }

    val state: StateFlow<ShoppingUiState> = combine(listState, draft, convert) { (t, groups, fs), d, c ->
        ShoppingUiState(tab = t, groups = groups, farms = fs, draft = d, convert = c, loaded = true)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ShoppingUiState(draft = draft.value))

    fun selectTab(t: ShopTab) {
        tab.value = t
    }

    // ---------- add row ----------
    fun onDraftName(name: String) = draft.update {
        it.copy(name = name, isTool = if (it.toolTouched) it.isTool else ToolKeywords.matches(name))
    }

    fun onDraftTool(on: Boolean) = draft.update { it.copy(isTool = on, toolTouched = true) }
    fun onDraftPrice(text: String) = draft.update { it.copy(priceText = text.filter { c -> c.isDigit() || c == '.' }) }
    fun onDraftFarm(farmId: Long?) = draft.update { it.copy(farmId = farmId) }
    fun onDraftDate(date: LocalDate) = draft.update { it.copy(date = date) }
    fun toggleMore() = draft.update { it.copy(expanded = !it.expanded) }

    /** Voice result (doc 06 recogniser) — fills the name; the user still taps add. */
    fun onVoiceText(text: String) = onDraftName(text.trim())

    fun add() {
        val d = draft.value
        val name = d.name.trim()
        if (name.isEmpty()) return
        val t = tab.value
        viewModelScope.launch {
            store.add(
                tab = t,
                name = name,
                forDate = d.date,
                farmId = if (t == ShopTab.FARM) d.farmId else null,
                estPricePaise = MoneyInput.toPaise(d.priceText),
                isPhysicalTool = d.isTool,
            )
        }
        // Keep date/farm/expanded for quick repeated entry.
        draft.value = d.copy(name = "", priceText = "", isTool = false, toolTouched = false)
    }

    // ---------- tick / undo ----------
    /**
     * Ticking strikes the row through, then after [removeDelayMs] marks it bought (it leaves the active
     * list) and offers undo. Ticking again inside that window cancels. Physical tools open the
     * convert sheet straight away (canvas 11).
     */
    fun toggle(row: ShopRow) {
        val id = row.id
        if (id in struck.value) {
            pending.remove(id)?.cancel()
            struck.update { it - id }
            if (convert.value?.itemId == id) convert.value = null
            return
        }
        struck.update { it + id }
        if (row.isTool && row.item.convertedAssetId == null) {
            convert.value = ConvertSheet(
                itemId = id,
                name = row.name,
                priceText = row.pricePaise?.let(MoneyInput::fromPaise).orEmpty(),
                purchaseDate = today(),
            )
        }
        pending[id] = viewModelScope.launch {
            delay(removeDelayMs)
            store.setBought(id, true)
            pending.remove(id)
            struck.update { it - id }
            events.send(ShoppingEvent.Bought(id, row.name))
        }
    }

    fun undo(id: Long) {
        viewModelScope.launch { store.setBought(id, false) }
    }

    // ---------- convert sheet ----------
    fun onConvertName(v: String) = convert.update { it?.copy(name = v) }
    fun onConvertPrice(v: String) = convert.update { it?.copy(priceText = v.filter { c -> c.isDigit() || c == '.' }) }
    fun onConvertDate(v: LocalDate) = convert.update { it?.copy(purchaseDate = v) }
    fun dismissConvert() {
        convert.value = null
    }

    /** "હા, ઉમેરો" → insertAsset + markConverted (one transaction in the repository). */
    fun confirmConvert() {
        val c = convert.value ?: return
        if (c.name.isBlank()) return
        convert.value = null
        viewModelScope.launch {
            val assetId = store.convertToAsset(c.itemId, c.name.trim(), MoneyInput.toPaise(c.priceText), c.purchaseDate)
            events.send(ShoppingEvent.AssetCreated(assetId))
        }
    }

    @OptIn(kotlinx.coroutines.DelicateCoroutinesApi::class)
    override fun onCleared() {
        // Rows ticked just before leaving the screen are still bought (the user saw them struck).
        pending.values.forEach { it.cancel() }
        val ids = struck.value
        if (ids.isNotEmpty()) {
            kotlinx.coroutines.GlobalScope.launch {
                ids.forEach { runCatching { store.setBought(it, true) } }
            }
        }
        super.onCleared()
    }

    companion object {
        const val REMOVE_DELAY_MS = 1_500L

        /** Groups by `forDate`, newest first; items keep the DAO order (newest first). */
        fun group(items: List<ShoppingItemEntity>, farmNames: Map<Long, String>, struck: Set<Long>, today: LocalDate): List<ShopGroup> =
            items.groupBy { it.forDate }
                .toSortedMap(compareByDescending { it })
                .map { (date, list) ->
                    ShopGroup(
                        date = date,
                        isToday = date == today,
                        rows = list.map {
                            ShopRow(
                                id = it.id,
                                name = it.name,
                                farmName = it.farmId?.let(farmNames::get),
                                isTool = it.isPhysicalTool,
                                pricePaise = it.estPricePaise,
                                struck = it.id in struck,
                                item = it,
                            )
                        },
                    )
                }

        fun factory(store: ShoppingStore): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T = ShoppingViewModel(store) as T
        }
    }
}

/** ₹ text ⇄ paise for the price fields. */
object MoneyInput {
    /** "3200" → 320000, "45.5" → 4550, "" / junk → null. */
    fun toPaise(text: String): Long? {
        val t = text.replace(",", "").replace("₹", "").trim()
        if (t.isEmpty()) return null
        val bd = t.toBigDecimalOrNull() ?: return null
        if (bd.signum() < 0) return null
        return bd.movePointRight(2).setScale(0, java.math.RoundingMode.HALF_UP).toLong()
    }

    /** 320000 → "3200", 4550 → "45.50". */
    fun fromPaise(p: Long): String = if (p % 100 == 0L) (p / 100).toString() else "%d.%02d".format(p / 100, p % 100)
}
