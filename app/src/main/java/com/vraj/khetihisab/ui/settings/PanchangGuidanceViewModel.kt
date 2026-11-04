package com.vraj.khetihisab.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vraj.khetihisab.data.entity.CustomFestivalEntity
import com.vraj.khetihisab.data.entity.PanchangAlertType
import com.vraj.khetihisab.data.repo.PanchangPrefRepository
import com.vraj.khetihisab.data.seed.PanchangDefaults
import com.vraj.khetihisab.notify.PanchangNotifier
import com.vraj.khetihisab.panchang.FestivalRules
import com.vraj.khetihisab.ui.home.ScreensDeps
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Editor state for one alert type (doc 10 §E). Texts are newline-separated lines. */
data class GuidanceDraft(
    val type: PanchangAlertType = PanchangAlertType.AMAS,
    val doText: String = "",
    val avoidText: String = "",
    val loaded: Boolean = false,
    val saved: Boolean = false,
) {
    val isDefault: Boolean
        get() = PanchangDefaults.lines(doText) == PanchangDefaults.lines(PanchangDefaults.doText(type)) &&
            PanchangDefaults.lines(avoidText) == PanchangDefaults.lines(PanchangDefaults.avoidText(type))
}

/** "મારો તહેવાર ઉમેરો" input (doc 10 PanchangAlertSettingsScreen). */
data class FestivalInput(
    val name: String = "",
    val byTithi: Boolean = true,
    val monthIndex: Int = 7,
    val vad: Boolean = false,
    val tithi: Int = 1,
    val fixedDate: String = "",
)

enum class FestivalError { NAME, DATE }

/**
 * Guidance editor + custom festivals. [reschedule] re-arms the evening alarm (custom festivals can add alerts).
 */
class PanchangGuidanceViewModel(
    private val repo: PanchangPrefRepository,
    private val reschedule: suspend () -> Unit = {},
) : ViewModel() {

    private val _draft = MutableStateFlow(GuidanceDraft())
    val draft: StateFlow<GuidanceDraft> = _draft.asStateFlow()

    val festivals: StateFlow<List<CustomFestivalEntity>> =
        repo.allCustomFestivals().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    init {
        select(PanchangAlertType.AMAS)
    }

    /** Loads the stored texts of [type] (defaults if the row is missing). */
    fun select(type: PanchangAlertType) {
        _draft.value = GuidanceDraft(type = type)
        viewModelScope.launch {
            val p = repo.prefsMap().first()[type] ?: PanchangDefaults.defaultPref(type, 0L)
            if (_draft.value.type == type) {
                _draft.value = GuidanceDraft(type, p.doText, p.avoidText, loaded = true)
            }
        }
    }

    fun onDoText(t: String) = _draft.update { it.copy(doText = t, saved = false) }
    fun onAvoidText(t: String) = _draft.update { it.copy(avoidText = t, saved = false) }

    fun save() {
        val d = _draft.value
        viewModelScope.launch {
            repo.updateGuidance(d.type, cleanLines(d.doText), cleanLines(d.avoidText))
            _draft.update { if (it.type == d.type) it.copy(doText = cleanLines(d.doText), avoidText = cleanLines(d.avoidText), saved = true) else it }
        }
    }

    /** "મૂળ સલાહ પાછી લાવો": restores the doc 10 §E defaults in Room and in the editor. */
    fun resetToDefault() {
        val type = _draft.value.type
        viewModelScope.launch {
            repo.resetGuidance(type)
            _draft.value = GuidanceDraft(
                type = type,
                doText = PanchangDefaults.doText(type),
                avoidText = PanchangDefaults.avoidText(type),
                loaded = true,
                saved = true,
            )
        }
    }

    /** Validates and inserts a custom festival; returns the problem, or null when saved. */
    fun addFestival(input: FestivalInput): FestivalError? {
        val error = validate(input)
        if (error != null) return error
        val entity = toEntity(input)
        viewModelScope.launch {
            repo.upsertFestival(entity)
            runCatching { reschedule() }
        }
        return null
    }

    fun deleteFestival(id: Long) = viewModelScope.launch {
        repo.deleteFestival(id)
        runCatching { reschedule() }
    }

    companion object {
        /** Trims each line, drops blanks and duplicates, keeps the order. */
        fun cleanLines(text: String): String = PanchangDefaults.lines(text).joinToString("\n")

        fun validate(input: FestivalInput): FestivalError? {
            if (input.name.isBlank()) return FestivalError.NAME
            if (!input.byTithi && FestivalRules.parseMonthDay(input.fixedDate) == null) return FestivalError.DATE
            if (input.byTithi && (input.monthIndex !in 0..11 || input.tithi !in 1..15)) return FestivalError.DATE
            return null
        }

        fun toEntity(input: FestivalInput): CustomFestivalEntity = if (input.byTithi) {
            CustomFestivalEntity(
                nameGu = input.name.trim(),
                monthIndex = input.monthIndex,
                paksha = if (input.vad) "VAD" else "SUD",
                tithiInPaksha = input.tithi,
                fixedMonthDay = null,
                note = null,
            )
        } else {
            val md = FestivalRules.parseMonthDay(input.fixedDate)!!
            CustomFestivalEntity(
                nameGu = input.name.trim(),
                monthIndex = null,
                paksha = null,
                tithiInPaksha = null,
                fixedMonthDay = "%02d-%02d".format(md.monthValue, md.dayOfMonth),
                note = null,
            )
        }

        fun create(deps: ScreensDeps) = PanchangGuidanceViewModel(
            repo = deps.repos.panchang,
            reschedule = { PanchangNotifier.reschedule(deps.appContext) },
        )
    }
}
