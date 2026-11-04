package com.vraj.khetihisab.ui.entry

import kotlin.math.floor
import kotlin.math.roundToLong

/**
 * APMC Mann calculator (doc 06, CLAUDE.md): TotalMann = Mann + ExtraKg / 20, Total = TotalMann × Rate.
 * 30 મણ + 10 kg × ₹1,600 → 30.5 × 160000 paise = 4,880,000 paise = ₹48,800.
 */
object ApmcCalculator {
    const val KG_PER_MANN = 20.0

    fun totalMann(mann: Double, extraKg: Double): Double = mann + extraKg / KG_PER_MANN

    fun totalAmountPaise(mann: Double, extraKg: Double, ratePerMannPaise: Long): Long =
        (totalMann(mann, extraKg) * ratePerMannPaise).roundToLong()

    /** kg ≥ 20 → whole manns move over: (mann + ⌊kg/20⌋, kg mod 20). */
    fun normalize(mann: Double, extraKg: Double): Pair<Double, Double> {
        if (extraKg < KG_PER_MANN) return mann to extraKg
        val whole = floor(extraKg / KG_PER_MANN)
        val rest = extraKg - whole * KG_PER_MANN
        return (mann + whole) to (Math.round(rest * 100.0) / 100.0)
    }
}
