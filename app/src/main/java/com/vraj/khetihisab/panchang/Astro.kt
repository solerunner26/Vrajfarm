package com.vraj.khetihisab.panchang

import java.time.Instant
import java.time.LocalDate
import kotlin.math.abs
import kotlin.math.acos
import kotlin.math.asin
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.sin

/**
 * Low-cost positional astronomy for the Panchang (doc 02 §5.2). Pure Kotlin `Double`, no I/O.
 *
 * Accuracy notes:
 * - Sun: Meeus ch. 25 low-precision apparent longitude (≈0.01°).
 * - Moon: Meeus ch. 47 with the **full** 59-term longitude series of table 47.A plus the
 *   A1/A2/L′−F additive terms (≈10″). Doc 02 asks for "at least the 24 largest terms"; the full
 *   series is used because it tightens tithi boundaries to well under a minute.
 * - Both series are evaluated in Terrestrial Time (UT + ΔT). Using UT directly would shift every
 *   tithi boundary by ~1 minute in 2026.
 */
object Astro {

    private const val DEG = Math.PI / 180.0
    const val MS_PER_DAY = 86_400_000.0
    const val J2000 = 2451545.0
    const val SYNODIC_RATE = 12.1907 // mean elongation rate, °/day
    const val SYNODIC_MONTH = 29.530588 // days

    fun norm360(x: Double): Double {
        val r = x % 360.0
        return if (r < 0) r + 360.0 else r
    }

    fun wrap180(x: Double): Double {
        val r = norm360(x)
        return if (r > 180.0) r - 360.0 else r
    }

    private fun sinD(x: Double) = sin(x * DEG)
    private fun cosD(x: Double) = cos(x * DEG)

    /** Julian Day (UT) of an instant. */
    fun jd(instant: Instant): Double = instant.toEpochMilli() / MS_PER_DAY + 2440587.5

    fun instantOfJd(jd: Double): Instant = Instant.ofEpochMilli(((jd - 2440587.5) * MS_PER_DAY).toLong())

    /** ΔT = TT − UT in seconds (Espenak & Meeus polynomials, valid 1986–2150). */
    fun deltaTSeconds(year: Double): Double = when {
        year < 2005.0 -> {
            val t = year - 2000.0
            63.86 + 0.3345 * t - 0.060374 * t * t + 0.0017275 * t * t * t +
                0.000651814 * t * t * t * t + 0.00002373599 * t * t * t * t * t
        }
        year < 2050.0 -> {
            val t = year - 2000.0
            62.92 + 0.32217 * t + 0.005589 * t * t
        }
        else -> {
            val u = (year - 1820.0) / 100.0
            -20.0 + 32.0 * u * u - 0.5628 * (2150.0 - year)
        }
    }

    private fun yearFraction(jdUt: Double): Double = 2000.0 + (jdUt - J2000) / 365.25

    /** Julian centuries (TT) since J2000 for a UT Julian Day. */
    fun centuriesTT(jdUt: Double): Double {
        val jde = jdUt + deltaTSeconds(yearFraction(jdUt)) / 86400.0
        return (jde - J2000) / 36525.0
    }

    /** Apparent geocentric (tropical) longitude of the Sun, degrees. */
    fun sunLongitude(t: Double): Double {
        val l0 = 280.46646 + 36000.76983 * t + 0.0003032 * t * t
        val m = 357.52911 + 35999.05029 * t - 0.0001537 * t * t
        val c = (1.914602 - 0.004817 * t - 0.000014 * t * t) * sinD(m) +
            (0.019993 - 0.000101 * t) * sinD(2 * m) +
            0.000289 * sinD(3 * m)
        val omega = 125.04 - 1934.136 * t
        return norm360(l0 + c - 0.00569 - 0.00478 * sinD(omega))
    }

    // Meeus table 47.A: multiples of D, M, M′, F and the Σl coefficient (1e-6 °).
    private val MOON_TERMS: Array<IntArray> = arrayOf(
        intArrayOf(0, 0, 1, 0, 6288774), intArrayOf(2, 0, -1, 0, 1274027),
        intArrayOf(2, 0, 0, 0, 658314), intArrayOf(0, 0, 2, 0, 213618),
        intArrayOf(0, 1, 0, 0, -185116), intArrayOf(0, 0, 0, 2, -114332),
        intArrayOf(2, 0, -2, 0, 58793), intArrayOf(2, -1, -1, 0, 57066),
        intArrayOf(2, 0, 1, 0, 53322), intArrayOf(2, -1, 0, 0, 45758),
        intArrayOf(0, 1, -1, 0, -40923), intArrayOf(1, 0, 0, 0, -34720),
        intArrayOf(0, 1, 1, 0, -30383), intArrayOf(2, 0, 0, -2, 15327),
        intArrayOf(0, 0, 1, 2, -12528), intArrayOf(0, 0, 1, -2, 10980),
        intArrayOf(4, 0, -1, 0, 10675), intArrayOf(0, 0, 3, 0, 10034),
        intArrayOf(4, 0, -2, 0, 8548), intArrayOf(2, 1, -1, 0, -7888),
        intArrayOf(2, 1, 0, 0, -6766), intArrayOf(1, 0, -1, 0, -5163),
        intArrayOf(1, 1, 0, 0, 4987), intArrayOf(2, -1, 1, 0, 4036),
        intArrayOf(2, 0, 2, 0, 3994), intArrayOf(4, 0, 0, 0, 3861),
        intArrayOf(2, 0, -3, 0, 3665), intArrayOf(0, 1, -2, 0, -2689),
        intArrayOf(2, 0, -1, 2, -2602), intArrayOf(2, -1, -2, 0, 2390),
        intArrayOf(1, 0, 1, 0, -2348), intArrayOf(2, -2, 0, 0, 2236),
        intArrayOf(0, 1, 2, 0, -2120), intArrayOf(0, 2, 0, 0, -2069),
        intArrayOf(2, -2, -1, 0, 2048), intArrayOf(2, 0, 1, -2, -1773),
        intArrayOf(2, 0, 0, 2, -1595), intArrayOf(4, -1, -1, 0, 1215),
        intArrayOf(0, 0, 2, 2, -1110), intArrayOf(3, 0, -1, 0, -892),
        intArrayOf(2, 1, 1, 0, -810), intArrayOf(4, -1, -2, 0, 759),
        intArrayOf(0, 2, -1, 0, -713), intArrayOf(2, 2, -1, 0, -700),
        intArrayOf(2, 1, -2, 0, 691), intArrayOf(2, -1, 0, -2, 596),
        intArrayOf(4, 0, 1, 0, 549), intArrayOf(0, 0, 4, 0, 537),
        intArrayOf(4, -1, 0, 0, 520), intArrayOf(1, 0, -2, 0, -487),
        intArrayOf(2, 1, 0, -2, -399), intArrayOf(0, 0, 2, -2, -381),
        intArrayOf(1, 1, 1, 0, 351), intArrayOf(3, 0, -2, 0, -340),
        intArrayOf(4, 0, -3, 0, 330), intArrayOf(2, -1, 2, 0, 327),
        intArrayOf(0, 2, 1, 0, -323), intArrayOf(1, 1, -1, 0, 299),
        intArrayOf(2, 0, 3, 0, 294),
    )

    /** Apparent geocentric (tropical) longitude of the Moon, degrees (nutation in longitude approximated). */
    fun moonLongitude(t: Double): Double {
        val t2 = t * t
        val t3 = t2 * t
        val t4 = t3 * t
        val lp = 218.3164477 + 481267.88123421 * t - 0.0015786 * t2 + t3 / 538841.0 - t4 / 65194000.0
        val d = 297.8501921 + 445267.1114034 * t - 0.0018819 * t2 + t3 / 545868.0 - t4 / 113065000.0
        val m = 357.5291092 + 35999.0502909 * t - 0.0001536 * t2 + t3 / 24490000.0
        val mp = 134.9633964 + 477198.8675055 * t + 0.0087414 * t2 + t3 / 69699.0 - t4 / 14712000.0
        val f = 93.2720950 + 483202.0175233 * t - 0.0036539 * t2 - t3 / 3526000.0 + t4 / 863310000.0
        val e = 1.0 - 0.002516 * t - 0.0000074 * t2
        val a1 = 119.75 + 131.849 * t
        val a2 = 53.09 + 479264.290 * t

        var sum = 0.0
        for (row in MOON_TERMS) {
            val arg = row[0] * d + row[1] * m + row[2] * mp + row[3] * f
            val eFactor = when (abs(row[1])) { 1 -> e; 2 -> e * e; else -> 1.0 }
            sum += row[4] * eFactor * sinD(arg)
        }
        sum += 3958 * sinD(a1) + 1962 * sinD(lp - f) + 318 * sinD(a2)

        val omega = 125.04452 - 1934.136261 * t
        return norm360(lp + sum / 1e6 - 0.00478 * sinD(omega))
    }

    /** Moon − Sun elongation in [0, 360). Ayanamsa cancels, so no sidereal correction is needed. */
    fun elongation(instant: Instant): Double {
        val t = centuriesTT(jd(instant))
        return norm360(moonLongitude(t) - sunLongitude(t))
    }

    /** Tithi in force at an instant, 1..30. */
    fun tithiAt(instant: Instant): Int = (floor(elongation(instant) / 12.0).toInt() + 1).coerceIn(1, 30)

    /**
     * Instant closest to [guess] when the elongation equals [target] (mod 360).
     * Newton/secant iteration seeded with the mean synodic rate.
     */
    fun solveElongation(target: Double, guess: Instant): Instant {
        var t = guess
        repeat(30) {
            val d = wrap180(target - elongation(t))
            if (abs(d) < 1e-6) return t
            // Local rate from a ±1 h finite difference (varies 10–15 °/day over a month).
            val h = 3_600_000L
            val rate = wrap180(elongation(t.plusMillis(h)) - elongation(t.minusMillis(h))) / (2.0 / 24.0)
            val r = if (rate > 5.0) rate else SYNODIC_RATE
            t = t.plusMillis((d / r * MS_PER_DAY).toLong())
        }
        return t
    }

    /** Lahiri (Chitrapaksha) ayanamsa, degrees: 23.853° at 2000.0 plus 50.29″/yr. */
    fun lahiriAyanamsa(instant: Instant): Double = 23.853 + 0.013969 * (yearFraction(jd(instant)) - 2000.0)

    /** Sidereal (nirayana) longitude of the Sun. */
    fun siderealSunLongitude(instant: Instant): Double {
        val t = centuriesTT(jd(instant))
        return norm360(sunLongitude(t) - lahiriAyanamsa(instant))
    }

    /** Illuminated fraction of the Moon from the elongation: k = (1 − cos e) / 2. */
    fun moonIllumination(elongationDeg: Double): Double = (1.0 - cosD(elongationDeg)) / 2.0

    data class SunTimes(val sunrise: Instant, val sunset: Instant)

    /**
     * Sunrise / sunset by the sunrise equation (h0 = −0.833°) for a civil date at the given place.
     * [lonEastDeg] is positive east. Accuracy ≈ ±1–2 minutes.
     */
    fun sunTimes(date: LocalDate, latDeg: Double, lonEastDeg: Double): SunTimes {
        // n = whole days from J2000.0 (2000-01-01 12:00) to 12:00 UTC of the civil date.
        // For India (UTC+5:30, 72–88°E) the local solar transit falls on the same UTC date.
        val n = (date.toEpochDay() - LocalDate.of(2000, 1, 1).toEpochDay()).toDouble()
        // Mean solar transit: J* = n − λE/360 (+0.0008 d ≈ ΔT).
        val jStar = n + 0.0008 - lonEastDeg / 360.0
        val m = norm360(357.5291 + 0.98560028 * jStar)
        val c = 1.9148 * sinD(m) + 0.02 * sinD(2 * m) + 0.0003 * sinD(3 * m)
        val lambda = norm360(m + c + 180.0 + 102.9372)
        val jTransit = J2000 + jStar + 0.0053 * sinD(m) - 0.0069 * sinD(2 * lambda)
        val sinDec = sinD(lambda) * sinD(23.4397)
        val dec = asin(sinDec) / DEG
        val cosW0 = (sinD(-0.833) - sinD(latDeg) * sinDec) / (cosD(latDeg) * cosD(dec))
        val w0 = acos(cosW0.coerceIn(-1.0, 1.0)) / DEG
        return SunTimes(
            sunrise = instantOfJd(jTransit - w0 / 360.0),
            sunset = instantOfJd(jTransit + w0 / 360.0),
        )
    }
}
