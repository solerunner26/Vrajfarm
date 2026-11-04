package com.vraj.khetihisab.share

/**
 * WhatsApp number normalisation (doc 12 §1). Returns the E.164 number without the "+"
 * (e.g. "919876543210") or null when the input is not a usable Indian mobile number.
 *
 * 1. strip everything except digits
 * 2. drop a leading "00" (international prefix)
 * 3. 10 digits starting 6–9 → prepend the country code
 * 4. 11 digits starting "0" → drop the 0, prepend the country code
 * 5. 12 digits starting with the country code → keep
 * 6. anything else → null
 */
object PhoneNormalizer {

    fun toE164NoPlus(raw: String?, defaultCc: String = "91"): String? {
        if (raw == null) return null
        var d = raw.filter { it in '0'..'9' }
        if (d.startsWith("00")) d = d.substring(2)
        return when {
            d.length == 10 && d[0] in '6'..'9' -> defaultCc + d
            d.length == 11 && d[0] == '0' && d[1] in '6'..'9' -> defaultCc + d.substring(1)
            d.length == 10 + defaultCc.length && d.startsWith(defaultCc) && d[defaultCc.length] in '6'..'9' -> d
            else -> null
        }
    }

    /** True when [a] and [b] are the same WhatsApp number in any format (borrower de-duplication, doc 11). */
    fun sameNumber(a: String?, b: String?): Boolean {
        val na = toE164NoPlus(a) ?: return false
        return na == toE164NoPlus(b)
    }

    /** Display form "+91 98765 43210" for a normalised number; the raw input otherwise. */
    fun display(raw: String?): String {
        val n = toE164NoPlus(raw) ?: return raw.orEmpty()
        val local = n.takeLast(10)
        return "+${n.dropLast(10)} ${local.take(5)} ${local.drop(5)}"
    }
}
