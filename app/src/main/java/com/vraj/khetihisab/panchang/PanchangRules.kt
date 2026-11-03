package com.vraj.khetihisab.panchang

import java.time.Instant
import java.time.LocalDate
import java.time.Month
import kotlin.math.floor

/** Calendar rules (doc 02 §5.3). Pure functions; unit-testable without astronomy. */
object PanchangRules {

    /** tithiIndex 1..30 → (paksha, tithiInPaksha 1..15). */
    fun pakshaOf(tithiIndex: Int): Pair<Paksha, Int> {
        require(tithiIndex in 1..30) { "tithiIndex out of range: $tithiIndex" }
        return if (tithiIndex <= 15) Paksha.SUD to tithiIndex else Paksha.VAD to (tithiIndex - 15)
    }

    /** Sidereal rashi 0..11 (0 = Mesha) of a sidereal longitude. */
    fun rashiOf(siderealLongitude: Double): Int = (floor(Astro.norm360(siderealLongitude) / 30.0).toInt()) % 12

    data class MonthResult(val monthIndex: Int, val isAdhik: Boolean, val isKshayaWarning: Boolean)

    /**
     * Amanta month from the sun's rashi at the opening (r0) and closing (r1) new moons of a lunation.
     * - r1 == r0: no sankranti in this lunation → Adhik, named after the next month.
     * - otherwise the month is (r0 + 1) % 12 (sun entering Mesha → ચૈત્ર, Tula → આસો, Vrischika → કારતક).
     * - r1 == r0 + 2 (kshaya maas, very rare): still (r0 + 1) with a warning flag.
     */
    fun monthFromRashis(r0: Int, r1: Int): MonthResult {
        val idx = (r0 + 1) % 12
        val gap = ((r1 - r0) % 12 + 12) % 12
        return MonthResult(monthIndex = idx, isAdhik = gap == 0, isKshayaWarning = gap >= 2)
    }

    /**
     * Vikram Samvat (Gujarati, Kartik-based). The new year starts on the first civil day of nija
     * કારતક (કારતક સુદ 1, or sud 2 if sud 1 is kshaya). Before that: year + 56, from then: year + 57.
     *
     * A Kartik/Magshar/Posh lunation that falls in Oct–Dec belongs to the new samvat.
     * January dates in Posh/Magshar still belong to the samvat that started the previous October.
     */
    fun vikramSamvat(date: LocalDate, monthIndex: Int, isAdhik: Boolean): Int {
        val newYearStarted = date.month >= Month.OCTOBER &&
            monthIndex in 7..11 &&
            !(monthIndex == 7 && isAdhik)
        return date.year + if (newYearStarted) 57 else 56
    }

    /** Lunation that contains [instant]: (previous new moon, next new moon). */
    fun lunationAround(instant: Instant): Pair<Instant, Instant> {
        val e = Astro.elongation(instant)
        val guess = instant.minusMillis((e / Astro.SYNODIC_RATE * Astro.MS_PER_DAY).toLong())
        var nm0 = Astro.solveElongation(0.0, guess)
        if (nm0.isAfter(instant)) {
            // Converged on the following new moon (e was ~360); step back one lunation.
            nm0 = Astro.solveElongation(0.0, nm0.minusMillis((Astro.SYNODIC_MONTH * Astro.MS_PER_DAY).toLong()))
        }
        val nm1 = Astro.solveElongation(0.0, nm0.plusMillis((Astro.SYNODIC_MONTH * Astro.MS_PER_DAY).toLong()))
        return nm0 to nm1
    }
}
