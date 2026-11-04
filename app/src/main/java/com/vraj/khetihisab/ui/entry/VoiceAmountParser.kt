package com.vraj.khetihisab.ui.entry

/**
 * Pulls a rupee amount out of recognised Gujarati speech, e.g. "ચાર હજાર" → 4000, "4000" → 4000,
 * "૧૫૦૦" → 1500, "બે લાખ પચાસ હજાર" → 250000. Used only for the "₹4,000 વાપરો?" suggestion chip;
 * it never fills the amount silently (doc 06).
 */
object VoiceAmountParser {
    private val words: Map<String, Long> = mapOf(
        "એક" to 1, "બે" to 2, "ત્રણ" to 3, "ચાર" to 4, "પાંચ" to 5, "છ" to 6, "સાત" to 7, "આઠ" to 8,
        "નવ" to 9, "દસ" to 10, "દશ" to 10, "અગિયાર" to 11, "બાર" to 12, "તેર" to 13, "ચૌદ" to 14,
        "પંદર" to 15, "સોળ" to 16, "સત્તર" to 17, "અઢાર" to 18, "ઓગણીસ" to 19, "વીસ" to 20,
        "પચ્ચીસ" to 25, "ત્રીસ" to 30, "ચાલીસ" to 40, "પચાસ" to 50, "સાઠ" to 60, "સિત્તેર" to 70,
        "એંસી" to 80, "નેવું" to 90, "અડધો" to 0, "દોઢ" to 0, "અઢી" to 0,
    ).mapValues { it.value.toLong() }

    private val multipliers: Map<String, Long> = mapOf("સો" to 100L, "હજાર" to 1_000L, "લાખ" to 100_000L)

    private fun gujaratiDigitsToAscii(s: String): String = buildString {
        s.forEach { c -> append(if (c in '૦'..'૯') '0' + (c - '૦') else c) }
    }

    /** Rupees, or null when no number was spoken. */
    fun parseRupees(text: String): Long? {
        val tokens = gujaratiDigitsToAscii(text)
            .replace(",", "")
            .split(Regex("[\\s\\u00A0]+"))
            .map { it.trim('.', '₹', '-', '–') }
            .filter { it.isNotEmpty() }
        var total = 0L
        var current = 0L
        var found = false
        for (raw in tokens) {
            val tok = raw.removePrefix("રૂપિયા").removePrefix("રૂ.").removePrefix("રૂ")
            val digits = tok.toLongOrNull()
            when {
                digits != null -> { current += digits; found = true }
                tok in multipliers -> {
                    val m = multipliers.getValue(tok)
                    if (m == 100L) {
                        current = (if (current == 0L) 1 else current) * 100
                    } else {
                        total += (if (current == 0L) 1 else current) * m
                        current = 0
                    }
                    found = true
                }
                words[tok]?.let { it > 0 } == true -> { current += words.getValue(tok); found = true }
            }
        }
        val value = total + current
        return if (found && value > 0) value else null
    }
}
