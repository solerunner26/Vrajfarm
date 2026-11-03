package com.vraj.khetihisab.ui.components

import kotlin.math.abs

/** Money / number formatting shared by every screen. Money is stored as Long paise (CLAUDE.md). */
object VkFormat {

    /** Indian digit grouping: 1234567 → "12,34,567". */
    fun groupIndian(n: Long): String {
        val s = abs(n).toString()
        if (s.length <= 3) return s
        val last3 = s.takeLast(3)
        val rest = s.dropLast(3)
        val sb = StringBuilder()
        rest.reversed().chunked(2).forEachIndexed { i, chunk ->
            if (i > 0) sb.append(',')
            sb.append(chunk)
        }
        return sb.reverse().toString() + "," + last3
    }

    /** 123450 paise → "₹1,234.50"; whole rupees drop the decimals: 120000 → "₹1,200". No sign. */
    fun rupees(paise: Long): String {
        val a = abs(paise)
        val r = a / 100
        val p = a % 100
        return if (p == 0L) "₹${groupIndian(r)}" else "₹${groupIndian(r)}.${p.toString().padStart(2, '0')}"
    }
}
