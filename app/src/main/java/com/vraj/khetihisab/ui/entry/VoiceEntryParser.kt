package com.vraj.khetihisab.ui.entry

import com.vraj.khetihisab.data.entity.AccountSection
import com.vraj.khetihisab.data.entity.TxnType

/** A type the speech can name ("ખાતર", "લાઇટ બિલ", "દૂધ વેચાણ"). */
data class VoiceCategory(val id: Long, val name: String, val section: AccountSection, val type: TxnType)

/** What one spoken sentence asks for. Null = not said (the form keeps what it had). */
data class VoiceEntry(
    val kind: EntryKind? = null,
    val section: AccountSection? = null,
    val farmId: Long? = null,
    val familyId: Long? = null,
    val categoryId: Long? = null,
    val categoryIsCropSale: Boolean = false,
    val rupees: Long? = null,
    val pending: Boolean = false,
    val counterparty: String? = null,
    val text: String = "",
)

/**
 * Whole-entry speech (owner request 2026-11-05): "ખાતર બે હજાર ખેતર ત્રણ ઉધાર રમેશભાઈ" →
 * ખર્ચ · ખેતર 3 · ખાતર · ₹2,000 · ઉધાર · રમેશભાઈ. The user only checks and presses સાચવો —
 * nothing is saved by speech alone.
 */
object VoiceEntryParser {
    private val INCOME_WORDS = listOf("આવક", "વેચાણ", "વેચ્યો", "વેચ્યા", "વેચી", "મળ્યા", "મળ્યો", "જમા")
    private val EXPENSE_WORDS = listOf("ખર્ચ", "ખરીદી", "ખરીદ્યું", "આપ્યા", "ચૂકવ્યા", "ઉપાડ")
    private val UDHAAR_WORDS = listOf("ઉધાર", "બાકી")
    private val ANIMAL_WORDS = listOf("પશુ", "દૂધ", "ગાય", "ભેંસ", "ઘાસ", "ખાણ")
    private val PERSONAL_WORDS = listOf("ઘર", "અંગત", "ઘરનું", "ઘરખર્ચ")
    private val FARM_WORDS = listOf("ખેતર", "ખેતરમાં", "ખેતરે")
    private val BHAGIYA_WORDS = listOf("ભાગિયા", "ભાગીયા", "ભાગિયો")
    private val FILLER = setOf("રૂપિયા", "રૂ", "રૂ.", "નો", "ની", "નું", "ના", "માં", "ને", "થી", "નો.", "રકમ")

    private val NUMBER_WORDS: Map<String, Int> = mapOf(
        "એક" to 1, "બે" to 2, "ત્રણ" to 3, "ચાર" to 4, "પાંચ" to 5, "છ" to 6, "સાત" to 7, "આઠ" to 8,
        "નવ" to 9, "દસ" to 10, "દશ" to 10, "અગિયાર" to 11, "બાર" to 12,
    )

    private fun ascii(s: String) = buildString { s.forEach { c -> append(if (c in '૦'..'૯') '0' + (c - '૦') else c) } }

    private fun tokens(text: String): List<String> =
        ascii(text).replace(",", " ").split(Regex("[\\s\\u00A0]+")).map { it.trim('.', '₹', '-', '–', '!', '?') }.filter { it.isNotEmpty() }

    private fun smallNumber(tok: String?): Int? = tok?.toIntOrNull() ?: NUMBER_WORDS[tok]

    private fun squash(s: String) = s.replace(" ", "").replace("‍", "").replace("‌", "")

    fun parse(
        text: String,
        farms: List<Pair<Long, String>>,
        families: List<Pair<Long, String>>,
        categories: List<VoiceCategory>,
    ): VoiceEntry {
        val toks = tokens(text)
        val used = BooleanArray(toks.size)
        val joined = squash(ascii(text))

        // ---- ઉધાર <name…> ----
        var pending = false
        var counterparty: String? = null
        val u = toks.indexOfFirst { t -> UDHAAR_WORDS.any { t.startsWith(it) } }
        if (u >= 0) {
            pending = true
            used[u] = true
            val name = mutableListOf<String>()
            var k = u + 1
            while (k < toks.size && isNameToken(toks[k])) { name += toks[k]; used[k] = true; k++ }
            counterparty = name.joinToString(" ").ifBlank { null }
        }

        // ---- ખેતર N ----
        var farmId: Long? = null
        val fi = toks.indexOfFirst { t -> FARM_WORDS.any { t == it } }
        if (fi >= 0) {
            used[fi] = true
            val n = smallNumber(toks.getOrNull(fi + 1))
            if (n != null) {
                used[fi + 1] = true
                farmId = farms.firstOrNull { (_, label) -> Regex("\\d+").find(ascii(label))?.value?.toIntOrNull() == n }?.first
                    ?: farms.getOrNull(n - 1)?.first
            }
        }
        if (farmId == null) farmId = farms.firstOrNull { (_, label) -> label.isNotBlank() && joined.contains(squash(label)) }?.first

        // ---- ભાગિયા (by number or by name) ----
        var familyId: Long? = null
        val bi = toks.indexOfFirst { t -> BHAGIYA_WORDS.any { t.startsWith(it) } }
        if (bi >= 0) {
            used[bi] = true
            val n = smallNumber(toks.getOrNull(bi + 1))
            if (n != null) { used[bi + 1] = true; familyId = families.getOrNull(n - 1)?.first }
        }
        if (familyId == null && counterparty == null) {
            families.firstOrNull { (_, name) -> name.isNotBlank() && joined.contains(squash(name)) }?.let { (id, name) ->
                familyId = id
                markUsed(toks, used, name)
            }
        }

        // ---- type (longest name that appears) ----
        val cat = categories.filter { it.name.isNotBlank() && joined.contains(squash(it.name)) }.maxByOrNull { it.name.length }
        cat?.let { markUsed(toks, used, it.name) }

        // ---- આવક / ખર્ચ ----
        val saidIncome = toks.any { t -> INCOME_WORDS.any { t.startsWith(it) } }
        val saidExpense = toks.any { t -> EXPENSE_WORDS.any { t.startsWith(it) } }
        val kind = when {
            cat != null && !saidIncome && !saidExpense -> if (cat.type == TxnType.INCOME) EntryKind.INCOME else EntryKind.EXPENSE
            saidIncome && !saidExpense -> EntryKind.INCOME
            saidExpense -> EntryKind.EXPENSE
            else -> null
        }

        // ---- section ----
        val section = when {
            familyId != null -> AccountSection.FAMILY
            farmId != null -> AccountSection.FARM
            cat != null -> cat.section
            toks.any { t -> ANIMAL_WORDS.any { t.startsWith(it) } } -> AccountSection.ANIMAL
            toks.any { t -> PERSONAL_WORDS.any { t == it } } -> AccountSection.PERSONAL
            else -> null
        }
        val catFits = cat != null && cat.section == section &&
            (kind == null || (kind == EntryKind.INCOME) == (cat.type == TxnType.INCOME))

        // ---- amount from what is left ----
        val rest = toks.filterIndexed { i, t -> !used[i] && t !in FILLER }.joinToString(" ")
        val rupees = VoiceAmountParser.parseRupees(rest)

        return VoiceEntry(
            kind = kind,
            section = section,
            farmId = farmId,
            familyId = familyId,
            categoryId = if (catFits) cat!!.id else null,
            categoryIsCropSale = catFits && cat!!.name == EntryViewModel.CROP_SALE,
            rupees = rupees,
            pending = pending,
            counterparty = counterparty,
            text = text.trim(),
        )
    }

    /** A word that belongs to a person's name (not a number, keyword or filler). */
    private fun isNameToken(t: String): Boolean {
        if (t in FILLER) return false
        if (t.toLongOrNull() != null || t in NUMBER_WORDS || t in setOf("સો", "હજાર", "લાખ")) return false
        val keywords = INCOME_WORDS + EXPENSE_WORDS + FARM_WORDS + BHAGIYA_WORDS + UDHAAR_WORDS
        return keywords.none { t.startsWith(it) }
    }

    private fun markUsed(toks: List<String>, used: BooleanArray, phrase: String) {
        val parts = tokens(phrase)
        for (start in toks.indices) {
            if (start + parts.size <= toks.size && parts.indices.all { toks[start + it] == parts[it] }) {
                parts.indices.forEach { used[start + it] = true }
                return
            }
        }
        // Joined speech ("લાઇટબિલ"): mark the single token that contains it.
        toks.indexOfFirst { squash(it).contains(squash(phrase)) }.takeIf { it >= 0 }?.let { used[it] = true }
    }

    /** Puts a parsed sentence into the form; fields that were not said stay as they were. */
    fun apply(form: EntryForm, v: VoiceEntry): EntryForm {
        var f = form
        v.kind?.let { f = EntryReducer.reduce(f, EntryEvent.SetKind(it)) }
        v.section?.let { if (it != f.section) f = EntryReducer.reduce(f, EntryEvent.SetSection(it)) }
        v.farmId?.let { if (f.section == AccountSection.FARM) f = f.copy(farmId = it) }
        v.familyId?.let { if (f.section == AccountSection.FAMILY) f = f.copy(familyId = it) }
        v.categoryId?.let { f = f.copy(categoryId = it, categoryIsCropSale = v.categoryIsCropSale) }
        v.rupees?.let { if (!f.apmcMode) f = f.copy(amountText = it.coerceIn(0, 9_999_999).toString()) }
        if (v.pending) f = f.copy(pending = true, counterparty = v.counterparty ?: f.counterparty)
        if (v.text.isNotBlank()) f = f.copy(voiceNote = v.text.take(200))
        return f
    }
}
