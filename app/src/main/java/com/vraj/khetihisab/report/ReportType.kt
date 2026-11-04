package com.vraj.khetihisab.report

import androidx.annotation.StringRes
import com.vraj.khetihisab.R
import com.vraj.khetihisab.data.repo.DateRange
import java.time.LocalDate
import java.time.YearMonth

/**
 * The 7 A4 Gujarati reports (doc 15 Part A). [slug] is the file-name prefix:
 * `filesDir/reports/<slug>_<yyyyMMdd_HHmm>.pdf`.
 *
 * [titleGu] duplicates the `rep_*` string so the pure content builder and JVM tests do not need
 * Android resources; the Settings grid should use [titleRes].
 */
enum class ReportType(
    val slug: String,
    @StringRes val titleRes: Int,
    val titleGu: String,
    /** false = the report ignores the date range (pending udhaar, assets). */
    val usesRange: Boolean,
) {
    DAILY_LEDGER("daily", R.string.rep_daily, "દૈનિક હિસાબ", true),
    FARM_PNL("farm_pnl", R.string.rep_farm_pnl, "ખેતરવાર નફો-નુકસાન", true),
    VIGHA_SCORECARD("vigha", R.string.rep_vigha, "વીઘા સ્કોરકાર્ડ", true),
    FAMILY_ADVANCES("family", R.string.rep_family, "પરિવાર A/B ઉપાડ", true),
    PERSONAL("personal", R.string.rep_personal, "અંગત હિસાબ", true),
    PENDING_UDHAAR("udhaar", R.string.rep_udhaar, "ઉધાર બાકી", false),
    ASSETS_LOANS("assets", R.string.rep_assets, "મિલકત અને ઉછીનું", false),
    ;

    /** Default filter per doc 15: today / financial year / calendar year / this month. */
    fun defaultRange(today: LocalDate): DateRange = when (this) {
        DAILY_LEDGER -> DateRange.day(today)
        FARM_PNL, VIGHA_SCORECARD -> DateRange.financialYear(today)
        FAMILY_ADVANCES -> DateRange.year(today.year)
        PERSONAL -> DateRange.month(YearMonth.from(today))
        PENDING_UDHAAR, ASSETS_LOANS -> DateRange.day(today)
    }
}
