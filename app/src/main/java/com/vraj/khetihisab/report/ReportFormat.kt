package com.vraj.khetihisab.report

import com.vraj.khetihisab.data.entity.AccountSection
import com.vraj.khetihisab.data.entity.PayStatus
import com.vraj.khetihisab.data.entity.TxnType
import com.vraj.khetihisab.data.repo.DateRange
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.abs

/** Pure formatting for PDF reports. Money is Long paise (CLAUDE.md). */
object ReportFormat {
    private val DDMMYYYY: DateTimeFormatter = DateTimeFormatter.ofPattern("dd-MM-yyyy", Locale.ROOT)

    /** Indian grouping: 1234567 -> "12,34,567". */
    fun groupIndian(n: Long): String {
        val s = abs(n).toString()
        if (s.length <= 3) return s
        val last3 = s.takeLast(3)
        val head = s.dropLast(3).reversed().chunked(2).joinToString(",").reversed()
        return "$head,$last3"
    }

    /** 123450 -> "₹1,234.50", 120000 -> "₹1,200", -5000 -> "-₹50". */
    fun rupees(paise: Long): String {
        val a = abs(paise)
        val r = a / 100
        val p = a % 100
        val body = if (p == 0L) "₹${groupIndian(r)}" else "₹${groupIndian(r)}.${p.toString().padStart(2, '0')}"
        return if (paise < 0) "-$body" else body
    }

    fun date(d: LocalDate): String = d.format(DDMMYYYY)

    fun range(r: DateRange): String =
        if (r.from == r.to) date(r.from) else "${date(r.from)} થી ${date(r.to)}"

    /** 2.5 -> "2.5", 3.0 -> "3". */
    fun vigha(v: Double): String =
        if (v == Math.floor(v) && !v.isInfinite()) v.toLong().toString()
        else String.format(Locale.ROOT, "%.2f", v).trimEnd('0').trimEnd('.')

    fun mann(v: Double): String = vigha(v)

    fun percent(part: Long, whole: Long): String =
        if (whole <= 0L) "0%" else String.format(Locale.ROOT, "%.1f%%", part * 100.0 / whole)

    fun section(s: AccountSection): String = when (s) {
        AccountSection.FARM -> "ખેતી"
        AccountSection.ANIMAL -> "પશુપાલન"
        AccountSection.FAMILY -> "પરિવાર"
        AccountSection.GENERAL -> "સામાન્ય"
        AccountSection.PERSONAL -> "અંગત"
    }

    fun type(t: TxnType): String = when (t) {
        TxnType.INCOME -> "આવક"
        TxnType.EXPENSE -> "ખર્ચ"
        TxnType.ADVANCE -> "ઉપાડ"
    }

    fun payStatus(p: PayStatus): String = when (p) {
        PayStatus.PAID -> "ચૂકવેલ"
        PayStatus.PENDING_PAYABLE -> "દેવાના બાકી"
        PayStatus.PENDING_RECEIVABLE -> "લેવાના બાકી"
    }

    private val MONTHS_GU = listOf(
        "જાન્યુઆરી", "ફેબ્રુઆરી", "માર્ચ", "એપ્રિલ", "મે", "જૂન",
        "જુલાઈ", "ઑગસ્ટ", "સપ્ટેમ્બર", "ઑક્ટોબર", "નવેમ્બર", "ડિસેમ્બર",
    )

    fun month(ym: YearMonth): String = "${MONTHS_GU[ym.monthValue - 1]} ${ym.year}"
}

/** Gujarati labels used inside PDFs (titles live in strings_backup.xml as rep_*). */
object ReportLabels {
    const val APP_NAME = "વ્રજ ખેતી હિસાબ"
    const val FOOTER_OWNER = "ગિરવાનસિંહ વાળા · થોરાળા"
    const val GENERATED = "તૈયાર: %s"
    const val PAGE = "પાનું %d / %d"
    const val NO_ENTRIES = "કોઈ નોંધ નથી"
    const val TOTAL = "કુલ"
    const val GRAND_TOTAL = "કુલ સરવાળો"
    const val DATE = "તારીખ"
    const val DETAIL = "વિગત"
    const val TYPE = "પ્રકાર"
    const val STATUS = "સ્થિતિ"
    const val AMOUNT = "રકમ"
    const val INCOME = "આવક"
    const val EXPENSE = "ખર્ચ"
    const val NET = "ચોખ્ખું"
    const val FARM = "ખેતર"
    const val VIGHA = "વીઘા"
    const val PER_VIGHA = "વીઘા દીઠ નફો"
    const val RANK = "ક્રમ"
    const val VIGHA_NOT_SET = "વીઘા સેટ કરો"
    const val DELETED_FARM = "ખેતર કાઢી નાખેલ"
    const val MONTH = "મહિનો"
    const val CATEGORY = "પ્રકાર"
    const val SHARE = "હિસ્સો"
    const val PERSON = "વ્યક્તિ"
    const val SECTION = "વિભાગ"
    const val AGE_DAYS = "દિવસ"
    const val RECEIVABLE = "લેવાના બાકી"
    const val PAYABLE = "દેવાના બાકી"
    const val PHOTO = "ફોટો"
    const val ITEM = "વસ્તુ"
    const val PHONE = "ફોન"
    const val HANDED_OVER = "આપ્યા"
    const val DUE = "પરત તારીખ"
    const val LATE_DAYS = "મોડું (દિવસ)"
    const val INVENTORY = "મિલકત યાદી"
    const val ACTIVE_LOANS = "ઉછીનું આપેલ"
    const val LENT = "ઉછીનું આપેલ"
    const val AVAILABLE = "હાજર"
    const val PRICE = "કિંમત"
    const val PURCHASED = "ખરીદી"
    const val OVERDUE = "મુદત વીતી"
    const val DAY_SUMMARY = "વિભાગવાર સરવાળો"
    const val GENERAL_NOTE = "સામાન્ય ખર્ચ (ખેતરમાં વહેંચાયેલ નથી): %s"
    const val PERSONAL_NOTE = "અંગત હિસાબ ખેતીના નફા-નુકસાનમાં ગણાતો નથી."
    const val SUMMARY = "સારાંશ"
    const val ENTRIES = "નોંધો"
    const val INCOME_BY_CATEGORY = "આવક પ્રકાર પ્રમાણે"
    const val EXPENSE_BY_CATEGORY = "ખર્ચ પ્રકાર પ્રમાણે"
    const val YEAR_TOTAL = "વર્ષ %d કુલ"
    const val FAMILY_FALLBACK = "પરિવાર %s"
    const val APMC = "%s મણ × %s"
    const val ADVANCE = "ઉપાડ"
}
