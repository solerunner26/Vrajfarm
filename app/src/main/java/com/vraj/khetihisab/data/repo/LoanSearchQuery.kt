package com.vraj.khetihisab.data.repo

import java.time.DateTimeException
import java.time.LocalDate

/**
 * Loan-history search input (doc 11). The query is trimmed; if it looks like a date
 * (`^\d{1,2}[-/.]\d{1,2}[-/.]\d{4}$`, read as dd-MM-yyyy with lenient separators) its epochDay is
 * passed to the DAO, otherwise epochDay is null and only name matching applies.
 */
data class LoanSearchQuery(val text: String, val epochDay: Long?) {
    val isBlank: Boolean get() = text.isEmpty()

    companion object {
        private val DATE = Regex("""^(\d{1,2})[-/.](\d{1,2})[-/.](\d{4})$""")

        fun parse(raw: String): LoanSearchQuery {
            val q = raw.trim()
            val m = DATE.matchEntire(q) ?: return LoanSearchQuery(q, null)
            val (d, mo, y) = m.destructured
            val epochDay = try {
                LocalDate.of(y.toInt(), mo.toInt(), d.toInt()).toEpochDay()
            } catch (_: DateTimeException) {
                null
            }
            return LoanSearchQuery(q, epochDay)
        }
    }
}
