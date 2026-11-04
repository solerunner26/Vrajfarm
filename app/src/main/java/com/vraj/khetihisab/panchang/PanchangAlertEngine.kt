package com.vraj.khetihisab.panchang

import com.vraj.khetihisab.data.entity.CustomFestivalEntity
import com.vraj.khetihisab.data.entity.PanchangAlertPrefEntity
import com.vraj.khetihisab.data.seed.PanchangDefaults
import java.time.LocalDate
import com.vraj.khetihisab.data.entity.PanchangAlertType as PrefType

/** Which per-type switch of [PanchangAlertPrefEntity] gates the alert, besides `enabled`. */
enum class AlertPurpose {
    /** Only `enabled` (calendar sheet, generic use). */
    ANY,

    /** `enabled && notifyEveningBefore` (7 PM notification, doc 10 scheduling §2). */
    EVENING_NOTIFICATION,

    /** `enabled && showDialogOnDay` (Home dialog, doc 10 scheduling §5). */
    DAY_DIALOG,
}

/** Room's [PrefType] ↔ the engine's [PanchangAlertType] (same names, different packages). */
fun PanchangAlertType.toPrefType(): PrefType = PrefType.valueOf(name)
fun PrefType.toAlertType(): PanchangAlertType = PanchangAlertType.valueOf(name)

/**
 * doc 10 §D: combines the tithi types and festivals of a day into a [SpecialDay], and the user's
 * prefs into a [PanchangAlert]. Pure Kotlin; call it off the main thread for long ranges
 * (each day costs a few astronomy evaluations, cached by GujaratiPanchangHelper).
 */
object PanchangAlertEngine {

    /** Guidance merge order (doc 10 §D): festival first, then AGIYARAS, AMAS, POONAM, BIJ. */
    val GUIDANCE_ORDER: List<PanchangAlertType> = listOf(
        PanchangAlertType.FESTIVAL,
        PanchangAlertType.AGIYARAS,
        PanchangAlertType.AMAS,
        PanchangAlertType.POONAM,
        PanchangAlertType.BIJ,
    )

    /** The special day at [date], or null if nothing matches. */
    fun special(
        date: LocalDate,
        custom: List<CustomFestivalEntity> = emptyList(),
        includeVadBij: Boolean = false,
        source: PanchangSource = PanchangSource.Default,
    ): SpecialDay? {
        val observed = FestivalRules.observedTithis(date, source)
        val types = LinkedHashSet<PanchangAlertType>()
        val festivals = FestivalRules.festivalsOn(date, observed, custom, source)
        if (festivals.isNotEmpty()) types += PanchangAlertType.FESTIVAL
        val tithiTypes = FestivalRules.typesOf(observed, includeVadBij)
        types += tithiTypes
        if (types.isEmpty()) return null
        val ekadashi = if (PanchangAlertType.AGIYARAS in tithiTypes) FestivalRules.ekadashiNameOf(observed) else null
        return SpecialDay(
            date = date,
            day = source.day(date),
            types = GUIDANCE_ORDER.filter { it in types }.toSet(),
            festivals = festivals,
            ekadashiName = ekadashi,
        )
    }

    /** Every special day in [from]..[to] inclusive (calendar list, widgets). */
    fun specialDaysInRange(
        from: LocalDate,
        to: LocalDate,
        custom: List<CustomFestivalEntity> = emptyList(),
        includeVadBij: Boolean = false,
        source: PanchangSource = PanchangSource.Default,
    ): List<SpecialDay> {
        if (to.isBefore(from)) return emptyList()
        val out = ArrayList<SpecialDay>()
        var d = from
        while (!d.isAfter(to)) {
            special(d, custom, includeVadBij, source)?.let(out::add)
            d = d.plusDays(1)
        }
        return out
    }

    /** First special day on or after [from], within [maxDays] (Today widget "next special day"). */
    fun nextSpecialDay(
        from: LocalDate,
        custom: List<CustomFestivalEntity> = emptyList(),
        includeVadBij: Boolean = false,
        maxDays: Int = 45,
        source: PanchangSource = PanchangSource.Default,
    ): SpecialDay? {
        var d = from
        repeat(maxDays) {
            special(d, custom, includeVadBij, source)?.let { return it }
            d = d.plusDays(1)
        }
        return null
    }

    /**
     * The alert for [date], or null when every matched type is disabled (FESTIVAL counts as a type).
     * [prefs] is `PanchangPrefRepository.prefsMapNow()`; missing rows fall back to the defaults.
     */
    fun alertFor(
        date: LocalDate,
        prefs: Map<PrefType, PanchangAlertPrefEntity>,
        custom: List<CustomFestivalEntity>,
        showGuidance: Boolean,
        includeVadBij: Boolean = false,
        purpose: AlertPurpose = AlertPurpose.ANY,
        source: PanchangSource = PanchangSource.Default,
    ): PanchangAlert? {
        val special = special(date, custom, includeVadBij, source) ?: return null
        return alertFrom(special, prefs, showGuidance, purpose)
    }

    /** Pure part of [alertFor]: filters [special] by prefs and merges the guidance. */
    fun alertFrom(
        special: SpecialDay,
        prefs: Map<PrefType, PanchangAlertPrefEntity>,
        showGuidance: Boolean,
        purpose: AlertPurpose = AlertPurpose.ANY,
    ): PanchangAlert? {
        fun pref(t: PanchangAlertType) = prefs[t.toPrefType()] ?: PanchangDefaults.defaultPref(t.toPrefType(), 0L)
        fun allowed(t: PanchangAlertType): Boolean {
            val p = pref(t)
            return p.enabled && when (purpose) {
                AlertPurpose.ANY -> true
                AlertPurpose.EVENING_NOTIFICATION -> p.notifyEveningBefore
                AlertPurpose.DAY_DIALOG -> p.showDialogOnDay
            }
        }

        val types = GUIDANCE_ORDER.filter { it in special.types && allowed(it) }
        if (types.isEmpty()) return null

        val filtered = special.copy(
            types = types.toSet(),
            festivals = if (PanchangAlertType.FESTIVAL in types) special.festivals else emptyList(),
            ekadashiName = if (PanchangAlertType.AGIYARAS in types) special.ekadashiName else null,
        )

        val doLines = LinkedHashSet<String>()
        val avoidLines = LinkedHashSet<String>()
        for (t in types) {
            if (t == PanchangAlertType.FESTIVAL) {
                filtered.festivals.forEach { doLines += FestivalRules.doOverride(it) }
            }
            val p = pref(t)
            doLines += PanchangDefaults.lines(p.doText)
            avoidLines += PanchangDefaults.lines(p.avoidText)
        }
        return PanchangAlert(
            special = filtered,
            doLines = doLines.toList(),
            avoidLines = avoidLines.toList(),
            showGuidance = showGuidance,
        )
    }

    /** Chip labels in display order ("તહેવાર", "અમાસ", …). */
    fun typeLabelGu(type: PanchangAlertType): String = when (type) {
        PanchangAlertType.FESTIVAL -> "તહેવાર"
        PanchangAlertType.AGIYARAS -> "અગિયારસ"
        PanchangAlertType.AMAS -> "અમાસ"
        PanchangAlertType.POONAM -> "પૂનમ"
        PanchangAlertType.BIJ -> "બીજ"
    }
}
