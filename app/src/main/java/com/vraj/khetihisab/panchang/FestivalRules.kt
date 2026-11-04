package com.vraj.khetihisab.panchang

import com.vraj.khetihisab.data.entity.CustomFestivalEntity
import com.vraj.khetihisab.data.seed.PanchangDefaults
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.MonthDay

/*
 * Special-day rules (doc 10 §A–§C). Pure Kotlin on top of GujaratiPanchangHelper + Astro.
 *
 * Observance resolution (doc 10 §C notes):
 * - SUNRISE: the civil day whose sunrise tithi matches.
 *   - Kshaya tithi (no sunrise of its own) → the civil day D with sunrise(D) < start and end < sunrise(D+1).
 *   - Vriddhi tithi (two sunrises) → the FIRST of the two days.
 * - PRADOSH (sunset + 72 min) / MIDNIGHT (00:00 IST at the end of civil day D, i.e. the night of D):
 *   the civil day D on which the tithi is in force at that moment (first such day if two qualify);
 *   if no day qualifies, fall back to the sunrise rule.
 * - Festivals never fire in an Adhik month. Agiyaras still does, with the Adhik names.
 */

/** When a festival's tithi is checked. */
enum class Observance { SUNRISE, PRADOSH, MIDNIGHT }

/** A tithi of a specific lunar month: tithiIndex 1..30 (1–15 sud, 16–30 vad; 30 = અમાસ). */
data class TithiKey(val monthIndex: Int, val isAdhik: Boolean, val tithiIndex: Int) {
    val paksha: Paksha get() = if (tithiIndex <= 15) Paksha.SUD else Paksha.VAD
    val tithiInPaksha: Int get() = if (tithiIndex <= 15) tithiIndex else tithiIndex - 15
}

/** Festival by amanta month + paksha + tithi (doc 10 §C). */
data class LunarFestival(
    val nameGu: String,
    val monthIndex: Int,
    val paksha: Paksha,
    val tithiInPaksha: Int,
    val observance: Observance = Observance.SUNRISE,
    /** Festival-specific "કરવું" lines shown before the generic FESTIVAL guidance (doc 10 §E). */
    val doOverride: List<String> = emptyList(),
) {
    init {
        require(monthIndex in 0..11) { "monthIndex out of range: $monthIndex" }
        require(tithiInPaksha in 1..15) { "tithiInPaksha out of range: $tithiInPaksha" }
    }

    val tithiIndex: Int get() = if (paksha == Paksha.SUD) tithiInPaksha else tithiInPaksha + 15

    fun matches(key: TithiKey): Boolean = !key.isAdhik && key.monthIndex == monthIndex && key.tithiIndex == tithiIndex
}

/** Festival on a fixed civil date (solar), e.g. ઉત્તરાયણ 14 Jan. */
data class SolarFestival(val nameGu: String, val monthDay: MonthDay, val doOverride: List<String> = emptyList())

/**
 * Supplies panchang days and the tithi at any instant. [Default] uses the real engine; tests pass
 * synthetic days to exercise kshaya / vriddhi / pradosh rules without astronomy.
 */
interface PanchangSource {
    fun day(date: LocalDate): PanchangDay

    /** Tithi index 1..30 in force at [instant]. */
    fun tithiAt(instant: Instant): Int

    companion object {
        val Default: PanchangSource = object : PanchangSource {
            override fun day(date: LocalDate): PanchangDay = GujaratiPanchangHelper.day(date)
            override fun tithiAt(instant: Instant): Int = Astro.tithiAt(instant)
        }
    }
}

object FestivalRules {

    /** Pradosh = sunset + 72 minutes (doc 10 §C). */
    const val PRADOSH_MINUTES = 72L

    // ---- §B Ekadashi names ----

    private val sudEkadashi = listOf(
        "કામદા", "મોહિની", "નિર્જળા", "દેવપોઢી", "પવિત્રા", "જળઝીલણી",
        "પાશાંકુશા", "દેવઊઠી (પ્રબોધિની)", "મોક્ષદા", "પુત્રદા", "જયા", "આમલકી",
    )
    private val vadEkadashi = listOf(
        "વરૂથિની", "અપરા", "યોગિની", "કામિકા", "અજા", "ઇન્દિરા",
        "રમા", "ઉત્પત્તિ", "સફલા", "ષટ્તિલા", "વિજયા", "પાપમોચની",
    )
    const val ADHIK_SUD_EKADASHI = "પદ્મિની"
    const val ADHIK_VAD_EKADASHI = "પરમા"

    /** "રમા" for આસો વદ 11; Adhik months use પદ્મિની / પરમા. */
    fun ekadashiName(monthIndex: Int, isAdhik: Boolean, paksha: Paksha): String {
        require(monthIndex in 0..11) { "monthIndex out of range: $monthIndex" }
        return when {
            isAdhik && paksha == Paksha.SUD -> ADHIK_SUD_EKADASHI
            isAdhik -> ADHIK_VAD_EKADASHI
            paksha == Paksha.SUD -> sudEkadashi[monthIndex]
            else -> vadEkadashi[monthIndex]
        }
    }

    // ---- §C Festivals ----

    private const val CHAITRA = 0
    private const val VAISHAKH = 1
    private const val ASHADH = 3
    private const val SHRAVAN = 4
    private const val BHADARVO = 5
    private const val ASO = 6
    private const val KARTAK = 7
    private const val MAHA = 10
    private const val FAGAN = 11

    private fun sud(name: String, month: Int, t: Int, o: Observance = Observance.SUNRISE, extra: List<String> = emptyList()) =
        LunarFestival(name, month, Paksha.SUD, t, o, extra)

    private fun vad(name: String, month: Int, t: Int, o: Observance = Observance.SUNRISE) =
        LunarFestival(name, month, Paksha.VAD, t, o)

    const val DIWALI = "દિવાળી"
    const val BESTU_VARAS = "બેસતું વર્ષ (નૂતન વર્ષ)"
    const val BHAI_BIJ = "ભાઈબીજ"
    const val DEV_UTHI = "દેવઊઠી અગિયારસ / તુલસી વિવાહ"
    const val DEV_DIWALI = "દેવ દિવાળી"
    const val DHANTERAS = "ધનતેરસ"
    const val KALI_CHAUDAS = "કાળી ચૌદશ"
    const val JANMASHTAMI = "જન્માષ્ટમી"
    const val UTTARAYAN = "ઉત્તરાયણ (મકરસંક્રાંતિ)"

    val lunarFestivals: List<LunarFestival> = listOf(
        sud("ચૈત્રી નવરાત્રિ પ્રારંભ", CHAITRA, 1),
        sud("રામ નવમી", CHAITRA, 9),
        sud("હનુમાન જયંતી", CHAITRA, 15),
        sud(PanchangDefaults.AKHATRIJ_NAME, VAISHAKH, 3, extra = PanchangDefaults.AKHATRIJ_DO_LINES),
        sud("અષાઢી બીજ", ASHADH, 2),
        sud("ગુરુ પૂર્ણિમા", ASHADH, 15),
        sud("રક્ષાબંધન / બળેવ", SHRAVAN, 15),
        vad("નાગ પાંચમ", SHRAVAN, 5),
        vad("રાંધણ છઠ", SHRAVAN, 6),
        vad("શીતળા સાતમ", SHRAVAN, 7),
        vad(JANMASHTAMI, SHRAVAN, 8, Observance.MIDNIGHT),
        sud("ગણેશ ચતુર્થી", BHADARVO, 4),
        vad("સર્વપિતૃ અમાસ", BHADARVO, 15),
        sud("નવરાત્રિ પ્રારંભ", ASO, 1),
        sud("દશેરા", ASO, 10),
        sud("શરદ પૂનમ", ASO, 15),
        vad("વાઘ બારસ", ASO, 12),
        vad(DHANTERAS, ASO, 13, Observance.PRADOSH),
        vad(KALI_CHAUDAS, ASO, 14),
        vad(DIWALI, ASO, 15, Observance.PRADOSH),
        sud(BESTU_VARAS, KARTAK, 1),
        sud(BHAI_BIJ, KARTAK, 2),
        sud("લાભ પાંચમ", KARTAK, 5),
        sud(DEV_UTHI, KARTAK, 11),
        sud(DEV_DIWALI, KARTAK, 15),
        sud("વસંત પંચમી", MAHA, 5),
        vad("મહાશિવરાત્રિ", MAHA, 14, Observance.MIDNIGHT),
        sud("હોળી (હોળિકા દહન)", FAGAN, 15, Observance.PRADOSH),
        vad("ધુળેટી", FAGAN, 1),
    )

    val solarFestivals: List<SolarFestival> = listOf(
        SolarFestival(UTTARAYAN, MonthDay.of(1, 14)),
        SolarFestival("વાસી ઉત્તરાયણ", MonthDay.of(1, 15)),
    )

    /** Festival-specific do-lines (doc 10 §E), e.g. અખાત્રીજ. */
    fun doOverride(festivalName: String): List<String> =
        lunarFestivals.firstOrNull { it.nameGu == festivalName }?.doOverride
            ?: solarFestivals.firstOrNull { it.nameGu == festivalName }?.doOverride
            ?: emptyList()

    // ---- §A Tithi observance ----

    private fun PanchangDay.key() = TithiKey(monthIndex, isAdhik, tithiIndex)

    /**
     * Tithis observed on civil [date] by the sunrise rule: the sunrise tithi (unless it is the second
     * day of a vriddhi) plus any kshaya tithi that starts and ends before the next sunrise.
     */
    fun observedTithis(date: LocalDate, source: PanchangSource = PanchangSource.Default): List<TithiKey> {
        val d = source.day(date)
        val prev = source.day(date.minusDays(1))
        val next = source.day(date.plusDays(1))
        val out = ArrayList<TithiKey>(2)
        if (prev.key() != d.key()) out += d.key() // vriddhi → only the first day observes it
        val gap = ((next.tithiIndex - d.tithiIndex) % 30 + 30) % 30
        for (i in 1 until gap) { // gap ≥ 2 → kshaya tithi(s) between the two sunrises
            val raw = d.tithiIndex + i
            val wrapped = raw > 30
            val t = if (wrapped) raw - 30 else raw
            out += if (wrapped) TithiKey(next.monthIndex, next.isAdhik, t) else TithiKey(d.monthIndex, d.isAdhik, t)
        }
        return out
    }

    /**
     * Tithi (with its month) in force at [instant], an instant after sunrise of civil [date] and
     * before the next sunrise. A smaller tithi index than at sunrise means a new moon passed, so
     * the month is the next day's.
     */
    fun keyAt(date: LocalDate, instant: Instant, source: PanchangSource = PanchangSource.Default): TithiKey {
        val d = source.day(date)
        val t = source.tithiAt(instant)
        return if (t < d.tithiIndex) {
            val n = source.day(date.plusDays(1))
            TithiKey(n.monthIndex, n.isAdhik, t)
        } else {
            TithiKey(d.monthIndex, d.isAdhik, t)
        }
    }

    /** The moment a [Observance.PRADOSH] / [Observance.MIDNIGHT] festival is checked on civil [date]. */
    fun momentOf(observance: Observance, date: LocalDate, source: PanchangSource = PanchangSource.Default): Instant? =
        when (observance) {
            Observance.SUNRISE -> null
            Observance.PRADOSH -> source.day(date).sunset.toInstant().plusSeconds(PRADOSH_MINUTES * 60)
            Observance.MIDNIGHT -> date.plusDays(1).atTime(LocalTime.MIDNIGHT).atZone(GujaratiPanchangHelper.ZONE).toInstant()
        }

    /** Tithi types (BIJ, AGIYARAS, POONAM, AMAS) observed on [date]. Vad Bij only if [includeVadBij]. */
    fun tithiTypes(
        date: LocalDate,
        includeVadBij: Boolean = false,
        source: PanchangSource = PanchangSource.Default,
    ): Set<PanchangAlertType> = typesOf(observedTithis(date, source), includeVadBij)

    fun typesOf(keys: List<TithiKey>, includeVadBij: Boolean): Set<PanchangAlertType> {
        val out = LinkedHashSet<PanchangAlertType>()
        for (k in keys) {
            when (k.tithiIndex) {
                2 -> out += PanchangAlertType.BIJ
                17 -> if (includeVadBij) out += PanchangAlertType.BIJ
                11, 26 -> out += PanchangAlertType.AGIYARAS
                15 -> out += PanchangAlertType.POONAM
                30 -> out += PanchangAlertType.AMAS
            }
        }
        return out
    }

    /** "રમા" if an Agiyaras is observed on [date], else null. */
    fun ekadashiNameOf(keys: List<TithiKey>): String? =
        keys.firstOrNull { it.tithiInPaksha == 11 }?.let { ekadashiName(it.monthIndex, it.isAdhik, it.paksha) }

    /**
     * Festival names on civil [date], in display order: solar, then pradosh/midnight festivals
     * (the evening's main event, e.g. દિવાળી before કાળી ચૌદશ), then sunrise festivals, then custom.
     */
    fun festivalsOn(
        date: LocalDate,
        custom: List<CustomFestivalEntity> = emptyList(),
        source: PanchangSource = PanchangSource.Default,
    ): List<String> = festivalsOn(date, observedTithis(date, source), custom, source)

    internal fun festivalsOn(
        date: LocalDate,
        observed: List<TithiKey>,
        custom: List<CustomFestivalEntity>,
        source: PanchangSource,
    ): List<String> {
        val out = ArrayList<String>()
        val md = MonthDay.from(date)
        solarFestivals.filter { it.monthDay == md }.forEach { out += it.nameGu }

        val moments = HashMap<Pair<Observance, LocalDate>, TithiKey>()
        fun at(o: Observance, d: LocalDate): TithiKey =
            moments.getOrPut(o to d) { keyAt(d, momentOf(o, d, source)!!, source) }

        val timed = ArrayList<String>()
        val sunrise = ArrayList<String>()
        for (f in lunarFestivals) {
            if (f.observance == Observance.SUNRISE) {
                if (observed.any(f::matches)) sunrise += f.nameGu
            } else {
                if (timedMatch(f, date, observed, ::at)) timed += f.nameGu
            }
        }
        out += timed
        out += sunrise

        for (c in custom) {
            if (!c.enabled) continue
            if (customMatches(c, md, observed)) out += c.nameGu.trim()
        }
        return out.filter { it.isNotEmpty() }.distinct()
    }

    private fun timedMatch(
        f: LunarFestival,
        date: LocalDate,
        observed: List<TithiKey>,
        at: (Observance, LocalDate) -> TithiKey,
    ): Boolean {
        val today = f.matches(at(f.observance, date))
        val yesterday = f.matches(at(f.observance, date.minusDays(1)))
        if (today) return !yesterday // two qualifying days → first one
        // Fallback: tithi never in force at the moment → sunrise rule, unless yesterday qualified.
        return !yesterday && observed.any(f::matches)
    }

    private fun customMatches(c: CustomFestivalEntity, md: MonthDay, observed: List<TithiKey>): Boolean {
        val fixed = c.fixedMonthDay?.let(::parseMonthDay)
        if (fixed != null) return fixed == md
        val month = c.monthIndex ?: return false
        val t = c.tithiInPaksha ?: return false
        val paksha = when (c.paksha?.uppercase()) {
            "SUD" -> Paksha.SUD
            "VAD" -> Paksha.VAD
            else -> return false
        }
        if (month !in 0..11 || t !in 1..15) return false
        val idx = if (paksha == Paksha.SUD) t else t + 15
        return observed.any { !it.isAdhik && it.monthIndex == month && it.tithiIndex == idx }
    }

    /**
     * "MM-dd" → MonthDay; null if malformed. 29-02 is rejected (doc 10 edge case: it does not exist
     * every year), as are dates such as 30-02 or 31-04.
     */
    fun parseMonthDay(s: String): MonthDay? {
        val m = Regex("""^\s*(\d{1,2})-(\d{1,2})\s*$""").matchEntire(s) ?: return null
        val month = m.groupValues[1].toInt()
        val day = m.groupValues[2].toInt()
        if (month !in 1..12 || day < 1) return null
        if (month == 2 && day == 29) return null
        return runCatching { MonthDay.of(month, day) }.getOrNull()?.takeIf { it.isValidYear(2027) }
    }
}
