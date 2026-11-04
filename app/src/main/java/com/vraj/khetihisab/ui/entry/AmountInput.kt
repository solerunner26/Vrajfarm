package com.vraj.khetihisab.ui.entry

/**
 * Keypad text rules (doc 06 "Keypad input rules"):
 * max [maxInt] integer digits and [maxDec] decimals, leading zeros collapsed, a second "." (or any "."
 * when decimals are not allowed) is ignored, ⌫ removes one char.
 */
data class AmountRules(val maxInt: Int = 7, val maxDec: Int = 2) {
    companion object {
        val AMOUNT = AmountRules(7, 2)
        val MANN = AmountRules(4, 1) // 0–9 999, one decimal
        val KG = AmountRules(3, 2) // normalised to < 20 after each key
        val RATE = AmountRules(5, 2) // 0–99 999
    }
}

object AmountInput {
    fun append(text: String, ch: Char, rules: AmountRules = AmountRules.AMOUNT): String {
        if (ch == '.') {
            if (rules.maxDec <= 0 || text.contains('.')) return text
            return if (text.isEmpty()) "0." else "$text."
        }
        if (!ch.isDigit()) return text
        val dot = text.indexOf('.')
        if (dot >= 0) {
            if (text.length - dot - 1 >= rules.maxDec) return text
            return text + ch
        }
        if (text == "0") return ch.toString() // collapse leading zero
        if (text.length >= rules.maxInt) return text
        return text + ch
    }

    fun backspace(text: String): String = text.dropLast(1)

    fun toDouble(text: String): Double = text.toDoubleOrNull() ?: 0.0

    /** 30.0 → "30", 30.5 → "30.5" (field display after normalisation). */
    fun format(v: Double): String {
        if (v == 0.0) return ""
        val r = Math.round(v * 100.0) / 100.0
        return if (r == Math.floor(r)) r.toLong().toString() else r.toString().trimEnd('0').trimEnd('.')
    }
}
