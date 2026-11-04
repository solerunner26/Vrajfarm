package com.vraj.khetihisab.ui.home

import com.vraj.khetihisab.data.entity.PayStatus
import com.vraj.khetihisab.data.entity.TransactionEntity
import com.vraj.khetihisab.data.entity.TxnType
import com.vraj.khetihisab.panchang.PanchangNames
import com.vraj.khetihisab.ui.components.AmountKind
import com.vraj.khetihisab.ui.components.VkFormat
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/** Pure formatting helpers shared by the compose-screens package (JVM-testable). */
object ScreensFormat {
    val IST: ZoneId = ZoneId.of("Asia/Kolkata")
    private val DMY: DateTimeFormatter = DateTimeFormatter.ofPattern("dd-MM-yyyy")
    private val DM: DateTimeFormatter = DateTimeFormatter.ofPattern("dd-MM")

    fun today(): LocalDate = LocalDate.now(IST)

    fun dmy(d: LocalDate): String = d.format(DMY)
    fun dm(d: LocalDate): String = d.format(DM)

    /** "નવેમ્બર" */
    fun monthGu(ym: YearMonth): String = PanchangNames.gregorianMonths[ym.monthValue - 1]

    /** "નવેમ્બર 2026" */
    fun monthYearGu(ym: YearMonth): String = "${monthGu(ym)} ${ym.year}"

    /** "03 નવેમ્બર 2026" */
    fun longDate(d: LocalDate): String =
        "${d.dayOfMonth.toString().padStart(2, '0')} ${PanchangNames.gregorianMonths[d.monthValue - 1]} ${d.year}"

    /** "₹12,500" or "−₹12,500" (negative net profit, doc 05 edge cases). */
    fun signedRupees(paise: Long): String = if (paise < 0) "−${VkFormat.rupees(paise)}" else VkFormat.rupees(paise)

    /** Amount colour/sign for a ledger row: pending → error, income → +primary, else −onSurface. */
    fun amountKind(t: TransactionEntity): AmountKind = when {
        t.payStatus != PayStatus.PAID -> AmountKind.PENDING
        t.type == TxnType.INCOME -> AmountKind.INCOME
        else -> AmountKind.EXPENSE
    }

    /**
     * First grapheme of a Gujarati / Latin name for the avatar ("ગિરવાનસિંહ" → "ગિ").
     * Keeps following combining marks (matras, anusvara, virama).
     */
    fun initialOf(name: String?): String {
        val s = name?.trim().orEmpty()
        if (s.isEmpty()) return ""
        val sb = StringBuilder()
        var i = 0
        val first = s.codePointAt(0)
        sb.appendCodePoint(first)
        i += Character.charCount(first)
        while (i < s.length) {
            val cp = s.codePointAt(i)
            val type = Character.getType(cp)
            if (type == Character.NON_SPACING_MARK.toInt() || type == Character.COMBINING_SPACING_MARK.toInt() ||
                type == Character.ENCLOSING_MARK.toInt()
            ) {
                sb.appendCodePoint(cp)
                i += Character.charCount(cp)
            } else {
                break
            }
        }
        return sb.toString()
    }

    /** Greeting name: "ગિરવાનસિંહ વાળા" → "ગિરવાનસિંહભાઈ". */
    fun greetingName(ownerName: String?): String {
        val first = ownerName?.trim()?.split(' ')?.firstOrNull().orEmpty()
        return if (first.isEmpty()) "" else first + "ભાઈ"
    }

    /**
     * Parses a rupee text typed on the keypad ("5400", "1600.5") to paise. Blank or invalid → 0.
     * Uses string arithmetic so 0.1 + 0.2 style float errors never reach the ledger.
     */
    fun rupeesTextToPaise(text: String): Long {
        val t = text.trim()
        if (t.isEmpty() || t == ".") return 0L
        val parts = t.split('.')
        val whole = parts[0].ifEmpty { "0" }.toLongOrNull() ?: return 0L
        val frac = parts.getOrNull(1).orEmpty().take(2).padEnd(2, '0')
        val fracVal = frac.toLongOrNull() ?: 0L
        return whole * 100 + fracVal
    }
}
