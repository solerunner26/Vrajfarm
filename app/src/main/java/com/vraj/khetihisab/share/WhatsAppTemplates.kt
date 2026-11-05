package com.vraj.khetihisab.share

import com.vraj.khetihisab.panchang.PanchangNames
import com.vraj.khetihisab.ui.components.VkFormat
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

/**
 * Gujarati WhatsApp message builders (doc 12 "Templates", doc 08 receipt).
 *
 * The template text is user-editable (Settings › "WhatsApp મેસેજ નમૂના", stored by
 * [WhatsAppTemplateStore]); the placeholders are fixed. The output is exactly the text the UI previews
 * and the text handed to WhatsApp — byte for byte.
 */
object WhatsAppTemplates {

    private val DMY: DateTimeFormatter = DateTimeFormatter.ofPattern("dd-MM-yyyy")
    private val DM: DateTimeFormatter = DateTimeFormatter.ofPattern("dd-MM")

    const val P_BORROWER = "{borrower}"
    const val P_HANDED_OVER = "{handedOver}"
    const val P_ASSET = "{asset}"
    const val P_EXPECTED_RETURN = "{expectedReturn}"
    const val P_LATE = "{late}"
    const val P_OWNER = "{ownerName}"
    const val P_VILLAGE = "{village}"

    const val P_DISPLAY_NAME = "{displayName}"
    const val P_MONTH = "{monthGu}"
    const val P_YEAR = "{yyyy}"
    const val P_MONTH_TOTAL = "{monthTotal}"
    const val P_ENTRIES = "{entries}"
    const val P_YEAR_TOTAL = "{yearTotal}"
    /** Today's date (loan reminder) — owner format 2026-11-05. */
    const val P_TODAY = "{today}"
    /** Date and amount of ONE ઉપાડ (ભાગિયા message) — owner format 2026-11-05. */
    const val P_DATE = "{date}"
    const val P_AMOUNT = "{amount}"

    /** Default loan reminder — the owner's exact wording (2026-11-05). */
    val DEFAULT_LOAN_REMINDER: String =
        "નમસ્તે $P_BORROWER, તમે $P_HANDED_OVER ના રોજ અમારું $P_ASSET લઈ ગયેલ છો. " +
            "જે આજે આજ $P_TODAY સુધી પાછું આપેલ નથી. કામ પૂરું થઈ ગયેલ હોય તો કૃપા કરીને સમયસર પરત કરશો. - $P_OWNER ."

    /** The pre-2026-11-05 multi-line loan template (kept for templates users saved earlier). */
    val OLD_LOAN_REMINDER: String = listOf(
        "નમસ્તે $P_BORROWER,",
        "તમે તા. $P_HANDED_OVER ના રોજ અમારું $P_ASSET લઈ ગયા છો.",
        "પરત આપવાની તારીખ: $P_EXPECTED_RETURN",
        "કૃપા કરી સમયસર પરત કરશો.",
        P_LATE,
        "— $P_OWNER, $P_VILLAGE",
    ).joinToString("\n")

    /**
     * Default ભાગિયા ઉપાડ message — the owner's exact wording (2026-11-05): one advance, its date and
     * amount, and the year's total. ({date}/{amount} = the newest advance in the list.)
     */
    val DEFAULT_FAMILY_STATEMENT: String =
        "$P_DISPLAY_NAME, તારીખ $P_DATE ના રોજ $P_AMOUNT ઉપાડ રૂપે આપેલ છે. વર્ષ $P_YEAR માં કુલ ઉપાડ $P_YEAR_TOTAL, " +
            "તમારી જાણ સારું. આ મેસેજ નોંધ માટે મોકલવામાં આવે છે. - $P_OWNER ."

    /** The pre-2026-11-05 monthly statement template. */
    val OLD_FAMILY_STATEMENT: String = listOf(
        "નમસ્તે $P_DISPLAY_NAME,",
        "$P_MONTH $P_YEAR નો ઉપાડ: $P_MONTH_TOTAL",
        P_ENTRIES,
        "વર્ષ $P_YEAR કુલ: $P_YEAR_TOTAL",
        "— $P_OWNER, વ્રજ ખેતી હિસાબ, $P_VILLAGE",
    ).joinToString("\n")

    const val FAMILY_MAX_ENTRIES = 10
    const val NO_ADVANCE_LINE = "આ મહિને કોઈ ઉપાડ નથી"

    /** "(N દિવસ મોડું થયું છે)" — same text as R.string.as_wa_late_line. */
    fun lateLine(days: Long): String = "($days દિવસ મોડું થયું છે)"

    /** Days past [expectedReturn] on [today]; 0 when not overdue. */
    fun overdueDays(expectedReturn: LocalDate, today: LocalDate): Long =
        ChronoUnit.DAYS.between(expectedReturn, today).coerceAtLeast(0)

    /**
     * Loan reminder. If [today] is after [expectedReturn] the line "({n} દિવસ મોડું થયું છે)" is added
     * just before the signature (at the `{late}` placeholder, or before the "—" line when a custom
     * template has no `{late}`).
     */
    fun loanReminder(
        borrower: String,
        asset: String,
        handedOver: LocalDate,
        expectedReturn: LocalDate,
        ownerName: String,
        village: String,
        today: LocalDate = LocalDate.now(),
        template: String = DEFAULT_LOAN_REMINDER,
    ): String {
        val late = overdueDays(expectedReturn, today).let { if (it > 0) lateLine(it) else "" }
        var t = template
        t = when {
            late.isEmpty() -> dropPlaceholderLine(t, P_LATE)
            // Old multi-line templates get the late line before "— signature"; the owner's one-line
            // format already says "આજ {today} સુધી પાછું આપેલ નથી", so nothing is added there.
            !t.contains(P_LATE) && t.contains('\n') -> insertBeforeSignature(t, P_LATE)
            else -> t
        }
        val filled = t
            .replace(P_BORROWER, borrower.trim())
            .replace(P_HANDED_OVER, handedOver.format(DMY))
            .replace(P_ASSET, asset.trim())
            .replace(P_EXPECTED_RETURN, expectedReturn.format(DMY))
            .replace(P_TODAY, today.format(DMY))
            .replace(P_OWNER, ownerName.trim())
            .replace(P_VILLAGE, village.trim())
            .replace(P_LATE, late)
        return cleanLines(filled)
    }

    /** One line of the family statement. */
    data class FamilyEntryLine(val date: LocalDate, val amountPaise: Long, val note: String?)

    /**
     * Family advance statement (doc 08): entries "• dd-MM — ₹x (note)", max [FAMILY_MAX_ENTRIES],
     * then "…અને N વધુ". A month without entries says [NO_ADVANCE_LINE].
     */
    fun familyStatement(
        displayName: String,
        month: YearMonth,
        monthTotalPaise: Long,
        entries: List<FamilyEntryLine>,
        yearTotalPaise: Long,
        ownerName: String,
        village: String,
        yearLabel: String = month.year.toString(),
        template: String = DEFAULT_FAMILY_STATEMENT,
    ): String {
        val lines = if (entries.isEmpty()) {
            NO_ADVANCE_LINE
        } else {
            val shown = entries.take(FAMILY_MAX_ENTRIES).map { e ->
                val note = e.note?.trim().orEmpty()
                "• ${e.date.format(DM)} — ${VkFormat.rupees(e.amountPaise)}" + if (note.isNotEmpty()) " ($note)" else ""
            }
            val more = entries.size - FAMILY_MAX_ENTRIES
            (if (more > 0) shown + "…અને $more વધુ" else shown).joinToString("\n")
        }
        val newest = entries.lastOrNull()
        // The one-advance format makes no sense without an advance.
        if (newest == null && template.contains(P_DATE)) return NO_ADVANCE_LINE
        val filled = template
            .replace(P_DATE, newest?.date?.format(DMY).orEmpty())
            .replace(P_AMOUNT, newest?.let { VkFormat.rupees(it.amountPaise) }.orEmpty())
            .replace(P_DISPLAY_NAME, displayName.trim())
            .replace(P_MONTH, PanchangNames.gregorianMonths[month.monthValue - 1])
            .replace(P_YEAR, yearLabel)
            .replace(P_MONTH_TOTAL, VkFormat.rupees(monthTotalPaise))
            .replace(P_YEAR_TOTAL, VkFormat.rupees(yearTotalPaise))
            .replace(P_OWNER, ownerName.trim())
            .replace(P_VILLAGE, village.trim())
            .replace(P_ENTRIES, lines)
        return cleanLines(filled)
    }

    /**
     * Village from the OWNER address: first comma part, with the "મુ. પો." / "મુ." prefix removed.
     * "મુ. પો. થોરાળા, તા. મહુવા, જિ. ભાવનગર" → "થોરાળા".
     */
    fun villageFromAddress(address: String?): String {
        val first = address?.split(',', '،')?.firstOrNull()?.trim().orEmpty()
        return first
            .replace(Regex("""^(મુ\.?\s*પો\.?|મુ\.?|પો\.?)\s*"""), "")
            .trim()
    }

    private fun insertBeforeSignature(t: String, line: String): String {
        val lines = t.split('\n').toMutableList()
        val sig = lines.indexOfLast { it.trimStart().startsWith("—") }
        if (sig >= 0) lines.add(sig, line) else lines.add(line)
        return lines.joinToString("\n")
    }

    /** Removes a line that holds only [placeholder] (used when its value is empty). */
    private fun dropPlaceholderLine(t: String, placeholder: String): String =
        t.split('\n').filterNot { it.trim() == placeholder }.joinToString("\n")

    /**
     * Tidies a signature whose owner or village is missing ("— ગિરવાનસિંહ, " → "— ગિરવાનસિંહ"; a bare
     * "—" line is removed) and trims trailing whitespace. Blank lines typed in a template are kept.
     */
    private fun cleanLines(s: String): String = s.split('\n')
        .mapNotNull { line ->
            val trimmed = line.trim()
            if (trimmed.startsWith("—")) {
                val parts = trimmed.removePrefix("—").split(',').map { it.trim() }.filter { it.isNotEmpty() }
                if (parts.isEmpty()) null else "— " + parts.joinToString(", ")
            } else {
                line.trimEnd()
            }
        }
        .joinToString("\n")
        .trimEnd()
}
