package com.vraj.khetihisab.ui.shopping

import java.text.Normalizer
import java.util.Locale

/**
 * Auto-ticks "આ એક સાધન છે" (doc 11 business logic). Matching is `contains`, tolerant of case,
 * Unicode composition (NFC/NFD), zero-width joiners and the optional nukta / chandrabindu marks.
 */
object ToolKeywords {

    val GUJARATI: List<String> = listOf(
        "પંપ", "મોટર", "ટ્રેક્ટર", "રોટાવેટર", "થ્રેશર", "ટ્રોલી", "કોદાળી", "પાવડો", "હળ",
        "સ્પ્રેયર", "ડ્રીપ", "પાઇપ", "કટર", "ઓજાર", "સાધન",
    )

    /** Same tools typed in English / Gujlish. */
    val LATIN: List<String> = listOf(
        "pump", "motor", "tractor", "rotavator", "thresher", "trolley", "sprayer", "drip", "pipe", "cutter",
    )

    private val all: List<String> by lazy { (GUJARATI + LATIN).map(::fold) }

    fun matches(name: String): Boolean {
        val n = fold(name)
        if (n.isBlank()) return false
        return all.any { n.contains(it) }
    }

    /** NFC, lower-case, without ZWJ/ZWNJ, nukta (U+0ABC) and chandrabindu (U+0A81). */
    internal fun fold(s: String): String = Normalizer.normalize(s, Normalizer.Form.NFC)
        .lowercase(Locale.ROOT)
        .filterNot { it == '‌' || it == '‍' || it == '઼' || it == 'ઁ' }
}
