package com.vraj.khetihisab.ui.panchang

import com.vraj.khetihisab.data.entity.CustomFestivalEntity
import com.vraj.khetihisab.data.entity.PanchangAlertPrefEntity
import com.vraj.khetihisab.panchang.AlertPurpose
import com.vraj.khetihisab.panchang.FestivalRules
import com.vraj.khetihisab.panchang.GujaratiPanchangHelper
import com.vraj.khetihisab.panchang.PanchangAlert
import com.vraj.khetihisab.panchang.PanchangAlertEngine
import com.vraj.khetihisab.panchang.PanchangDay
import com.vraj.khetihisab.panchang.SpecialDay
import java.time.LocalDate
import com.vraj.khetihisab.data.entity.PanchangAlertType as PrefType

/**
 * UI-side facade over the doc 10 engine. Every special-day / alert decision is delegated to
 * [PanchangAlertEngine] + [FestivalRules] (built-in festivals with sunrise / pradosh / midnight
 * observance, kshaya/vriddhi, Adhik rules, custom festivals). Dates outside the engine's range
 * (1990–2100) return null instead of throwing, so a UI never crashes on them.
 */
object PanchangSource {

    fun day(date: LocalDate): PanchangDay = GujaratiPanchangHelper.day(date)

    /** "રમા" for an Agiyaras day, else null. */
    fun ekadashiName(d: PanchangDay): String? =
        if (d.tithiInPaksha == 11) FestivalRules.ekadashiName(d.monthIndex, d.isAdhik, d.paksha) else null

    /** Special day on [date], or null for an ordinary day. */
    fun special(date: LocalDate, custom: List<CustomFestivalEntity> = emptyList(), vadBij: Boolean = false): SpecialDay? =
        runCatching { PanchangAlertEngine.special(date, custom, vadBij) }.getOrNull()

    fun specialDaysInRange(
        from: LocalDate,
        to: LocalDate,
        custom: List<CustomFestivalEntity> = emptyList(),
        vadBij: Boolean = false,
    ): List<SpecialDay> = runCatching { PanchangAlertEngine.specialDaysInRange(from, to, custom, vadBij) }.getOrDefault(emptyList())

    /**
     * Day alert; null when every matched type is disabled. [requireDialog] also honours each
     * type's `showDialogOnDay` switch (Home dialog).
     */
    fun alertFor(
        date: LocalDate,
        prefs: Map<PrefType, PanchangAlertPrefEntity>,
        custom: List<CustomFestivalEntity>,
        showGuidance: Boolean,
        vadBij: Boolean = false,
        requireDialog: Boolean = false,
    ): PanchangAlert? = runCatching {
        PanchangAlertEngine.alertFor(
            date = date,
            prefs = prefs,
            custom = custom,
            showGuidance = showGuidance,
            includeVadBij = vadBij,
            purpose = if (requireDialog) AlertPurpose.DAY_DIALOG else AlertPurpose.ANY,
        )
    }.getOrNull()

    /** Alert of an already computed [special] day (calendar sheet). */
    fun alertFrom(special: SpecialDay, prefs: Map<PrefType, PanchangAlertPrefEntity>, showGuidance: Boolean): PanchangAlert? =
        PanchangAlertEngine.alertFrom(special, prefs, showGuidance, AlertPurpose.ANY)
}
