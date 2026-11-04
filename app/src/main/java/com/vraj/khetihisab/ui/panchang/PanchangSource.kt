package com.vraj.khetihisab.ui.panchang

import com.vraj.khetihisab.data.entity.CustomFestivalEntity
import com.vraj.khetihisab.data.entity.PanchangAlertPrefEntity
import com.vraj.khetihisab.panchang.GujaratiPanchangHelper
import com.vraj.khetihisab.panchang.Paksha
import com.vraj.khetihisab.panchang.PanchangAlert
import com.vraj.khetihisab.panchang.PanchangAlertType
import com.vraj.khetihisab.panchang.PanchangDay
import com.vraj.khetihisab.panchang.SpecialDay
import java.time.LocalDate
import com.vraj.khetihisab.data.entity.PanchangAlertType as PrefType

/**
 * The only place in the compose-screens package that knows how special days are computed.
 *
 * TODO(doc 10): when `panchang/PanchangAlertEngine` + `FestivalRules` land (reminders-widgets /
 * foundation), replace [special] and [alertFor] bodies with calls to
 * `PanchangAlertEngine.special(date, custom)` / `PanchangAlertEngine.alertFor(date, prefs, custom, showGuidance)`.
 * Until then this fallback derives BIJ / AGIYARAS / POONAM / AMAS from [GujaratiPanchangHelper] and
 * custom festivals (fixed MM-dd or month+paksha+tithi); built-in festivals are not detected.
 */
object PanchangSource {

    /** Ekadashi names (doc 10 §B), month index 0 = ચૈત્ર; pair = (સુદ 11, વદ 11). */
    private val ekadashi: List<Pair<String, String>> = listOf(
        "કામદા" to "વરૂથિની", "મોહિની" to "અપરા", "નિર્જળા" to "યોગિની", "દેવપોઢી" to "કામિકા",
        "પવિત્રા" to "અજા", "જળઝીલણી" to "ઇન્દિરા", "પાશાંકુશા" to "રમા", "દેવઊઠી" to "ઉત્પત્તિ",
        "મોક્ષદા" to "સફલા", "પુત્રદા" to "ષટ્તિલા", "જયા" to "વિજયા", "આમલકી" to "પાપમોચની",
    )
    private val ekadashiAdhik = "પદ્મિની" to "પરમા"

    fun day(date: LocalDate): PanchangDay = GujaratiPanchangHelper.day(date)

    fun ekadashiName(d: PanchangDay): String? {
        if (d.tithiInPaksha != 11) return null
        val pair = if (d.isAdhik) ekadashiAdhik else ekadashi.getOrNull(d.monthIndex) ?: return null
        return if (d.paksha == Paksha.SUD) pair.first else pair.second
    }

    private fun customMatches(c: CustomFestivalEntity, d: PanchangDay): Boolean {
        if (!c.enabled) return false
        c.fixedMonthDay?.let { md ->
            val parts = md.split('-')
            val m = parts.getOrNull(0)?.toIntOrNull()
            val dd = parts.getOrNull(1)?.toIntOrNull()
            return m == d.date.monthValue && dd == d.date.dayOfMonth
        }
        if (d.isAdhik) return false // festivals never fire in an Adhik month (doc 10 §C)
        return c.monthIndex == d.monthIndex && c.paksha == d.paksha.name && c.tithiInPaksha == d.tithiInPaksha
    }

    /** Special day on [date], or null for an ordinary day. */
    fun special(date: LocalDate, custom: List<CustomFestivalEntity> = emptyList(), vadBij: Boolean = false): SpecialDay? {
        val d = runCatching { day(date) }.getOrNull() ?: return null
        val types = linkedSetOf<PanchangAlertType>()
        val festivals = custom.filter { customMatches(it, d) }.map { it.nameGu }
        if (festivals.isNotEmpty()) types += PanchangAlertType.FESTIVAL
        val ek = ekadashiName(d)
        if (ek != null) types += PanchangAlertType.AGIYARAS
        if (d.tithiIndex == 30) types += PanchangAlertType.AMAS
        if (d.tithiIndex == 15) types += PanchangAlertType.POONAM
        if (d.tithiInPaksha == 2 && (d.paksha == Paksha.SUD || vadBij)) types += PanchangAlertType.BIJ
        if (types.isEmpty()) return null
        return SpecialDay(date, d, types, festivals, ek)
    }

    fun specialDaysInRange(
        from: LocalDate,
        to: LocalDate,
        custom: List<CustomFestivalEntity> = emptyList(),
        vadBij: Boolean = false,
    ): List<SpecialDay> {
        val out = ArrayList<SpecialDay>()
        var d = from
        while (!d.isAfter(to)) {
            special(d, custom, vadBij)?.let(out::add)
            d = d.plusDays(1)
        }
        return out
    }

    /** Merge order: festival, agiyaras, amas, poonam, bij (doc 10 §D). */
    private val mergeOrder = listOf(
        PanchangAlertType.FESTIVAL, PanchangAlertType.AGIYARAS, PanchangAlertType.AMAS,
        PanchangAlertType.POONAM, PanchangAlertType.BIJ,
    )

    private fun prefType(t: PanchangAlertType): PrefType = PrefType.valueOf(t.name)

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
    ): PanchangAlert? {
        val sp = special(date, custom, vadBij) ?: return null
        val enabled = sp.types.filter { t ->
            val p = prefs[prefType(t)]
            p == null || (p.enabled && (!requireDialog || p.showDialogOnDay))
        }.toSet()
        if (enabled.isEmpty()) return null
        val doLines = LinkedHashSet<String>()
        val avoidLines = LinkedHashSet<String>()
        mergeOrder.filter { it in enabled }.forEach { t ->
            val p = prefs[prefType(t)] ?: return@forEach
            p.doText.lines().map { it.trim() }.filter { it.isNotEmpty() }.forEach(doLines::add)
            p.avoidText.lines().map { it.trim() }.filter { it.isNotEmpty() }.forEach(avoidLines::add)
        }
        return PanchangAlert(sp.copy(types = enabled), doLines.toList(), avoidLines.toList(), showGuidance)
    }
}
