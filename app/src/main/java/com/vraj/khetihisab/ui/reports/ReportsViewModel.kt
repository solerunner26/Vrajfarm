package com.vraj.khetihisab.ui.reports

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vraj.khetihisab.data.AppDatabase
import com.vraj.khetihisab.data.repo.DateRange
import com.vraj.khetihisab.report.GujaratiPdfGenerator
import com.vraj.khetihisab.report.ReportType
import com.vraj.khetihisab.ui.home.ScreensDeps
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId

/** Quick range choices shown as chips above the custom from/to pickers. */
enum class RangePreset { TODAY, YESTERDAY, THIS_MONTH, LAST_MONTH, FINANCIAL_YEAR, THIS_YEAR, LAST_YEAR }

data class ReportsUi(
    val selected: ReportType? = null,
    val range: DateRange? = null,
    val generating: Boolean = false,
    val lastFile: String? = null,
    val lastType: ReportType? = null,
    val failed: Boolean = false,
)

/**
 * doc 15 Part A: pick one of the 7 reports, a date/range, generate off the main thread
 * (GujaratiPdfGenerator runs on Dispatchers.Default), then open/share via ReportShare.
 */
class ReportsViewModel(
    private val generator: suspend (ReportType, DateRange) -> File,
    private val today: () -> LocalDate = { LocalDate.now(IST) },
    initialType: ReportType? = null,
) : ViewModel() {

    private val _ui = MutableStateFlow(ReportsUi())
    val ui: StateFlow<ReportsUi> = _ui.asStateFlow()

    init {
        initialType?.let(::select)
    }

    fun select(type: ReportType) = _ui.update {
        it.copy(selected = type, range = type.defaultRange(today()), failed = false)
    }

    fun close() = _ui.update { it.copy(selected = null, failed = false) }

    fun setPreset(p: RangePreset) = _ui.update { it.copy(range = presetRange(p, today()), failed = false) }

    /** Daily report: one day. */
    fun setDay(d: LocalDate) = _ui.update { it.copy(range = DateRange.day(d), failed = false) }

    fun setFrom(d: LocalDate) = _ui.update { u ->
        val to = u.range?.to?.takeIf { !it.isBefore(d) } ?: d
        u.copy(range = DateRange(d, to), failed = false)
    }

    fun setTo(d: LocalDate) = _ui.update { u ->
        val from = u.range?.from?.takeIf { !it.isAfter(d) } ?: d
        u.copy(range = DateRange(from, d), failed = false)
    }

    fun generate() {
        val u = _ui.value
        val type = u.selected ?: return
        if (u.generating) return
        val range = u.range ?: type.defaultRange(today())
        _ui.update { it.copy(generating = true, failed = false) }
        viewModelScope.launch {
            val r = runCatching { generator(type, range) }
            _ui.update {
                it.copy(
                    generating = false,
                    lastFile = r.getOrNull()?.absolutePath ?: it.lastFile,
                    lastType = if (r.isSuccess) type else it.lastType,
                    failed = r.isFailure,
                    selected = if (r.isSuccess) null else it.selected,
                )
            }
        }
    }

    companion object {
        val IST: ZoneId = ZoneId.of("Asia/Kolkata")

        /** Which presets make sense for [type] (doc 15 "Filters"). */
        fun presetsFor(type: ReportType): List<RangePreset> = when (type) {
            ReportType.DAILY_LEDGER -> listOf(RangePreset.TODAY, RangePreset.YESTERDAY)
            ReportType.FARM_PNL, ReportType.VIGHA_SCORECARD -> listOf(RangePreset.FINANCIAL_YEAR, RangePreset.THIS_YEAR, RangePreset.THIS_MONTH)
            ReportType.FAMILY_ADVANCES -> listOf(RangePreset.THIS_YEAR, RangePreset.LAST_YEAR)
            ReportType.PERSONAL -> listOf(RangePreset.THIS_MONTH, RangePreset.LAST_MONTH, RangePreset.THIS_YEAR)
            ReportType.PENDING_UDHAAR, ReportType.ASSETS_LOANS -> emptyList()
        }

        fun presetRange(p: RangePreset, today: LocalDate): DateRange = when (p) {
            RangePreset.TODAY -> DateRange.day(today)
            RangePreset.YESTERDAY -> DateRange.day(today.minusDays(1))
            RangePreset.THIS_MONTH -> DateRange.month(YearMonth.from(today))
            RangePreset.LAST_MONTH -> DateRange.month(YearMonth.from(today).minusMonths(1))
            RangePreset.FINANCIAL_YEAR -> DateRange.financialYear(today)
            RangePreset.THIS_YEAR -> DateRange.year(today.year)
            RangePreset.LAST_YEAR -> DateRange.year(today.year - 1)
        }

        fun create(deps: ScreensDeps, initialType: ReportType?): ReportsViewModel {
            val pdf by lazy { GujaratiPdfGenerator(deps.appContext, AppDatabase.get(deps.appContext)) }
            return ReportsViewModel(generator = { t, r -> pdf.generate(t, r) }, initialType = initialType)
        }
    }
}
