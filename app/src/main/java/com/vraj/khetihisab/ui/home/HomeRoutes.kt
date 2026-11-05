package com.vraj.khetihisab.ui.home

import com.vraj.khetihisab.data.entity.AccountSection
import com.vraj.khetihisab.ui.farms.FarmPeriod

/**
 * Routes registered by [homeGraph] plus the routes it *calls* in other modules. Calls go through
 * `NavController.navigateSafely`, so a route that is not registered yet is a no-op, never a crash.
 */
object HomeRoutes {
    // ---- registered by homeGraph ----
    const val HOME = "home"
    const val FARMS = "farms"
    const val FAMILY = "family"
    const val PERSONAL = "personal"
    const val FARM_DETAIL = "farm/{farmId}?period={period}&year={year}"

    fun farmDetail(farmId: Long, period: FarmPeriod, cropYear: Int) = "farm/$farmId?period=${period.name}&year=$cropYear"

    // ---- owned by other modules (see INTEGRATION.md) ----
    const val SHOPPING = "shopping"
    const val ASSETS = "assets"
    const val SETTINGS = "settings"
    const val BACKUP_SETTINGS = "settings/backup"
    const val PROFILES = "profiles"
    const val REMINDERS = "reminders"
    const val PANCHANG = "panchang/calendar"

    /** Reports screen pre-selected on a report ("farm_pnl", "vigha", "family", "personal" …, doc 15). */
    fun reports(slug: String) = "reports?type=$slug"

    /**
     * Entry (doc 06). Today VkRoot declares only `kind`; the extra query arguments are read by
     * EntryViewModel (ARG_SECTION / ARG_FARM / ARG_FAMILY) once VkRoot declares them (INTEGRATION.md §2).
     * Until then they are ignored and Entry opens with only the kind set.
     */
    fun entry(kind: String, section: AccountSection? = null, farmId: Long? = null, familyId: Long? = null, date: java.time.LocalDate? = null): String {
        val sb = StringBuilder("entry?kind=").append(kind)
        if (date != null) sb.append("&date=").append(date.toEpochDay())
        if (section != null) sb.append("&section=").append(section.name)
        if (farmId != null) sb.append("&farm=").append(farmId)
        if (familyId != null) sb.append("&family=").append(familyId)
        return sb.toString()
    }

    /** Opens Entry pre-filled with an existing row; saving updates that row (no duplicate). */
    fun editEntry(id: Long) = "entry?edit=$id"

    /** સામાન્ય ખર્ચ screen (same one-section screen as Personal). */
    const val GENERAL = "general"

    /** દૂધ રજીસ્ટર (milk register, DB v2). */
    const val MILK = "milk"

    const val KIND_EXPENSE = "expense"
    const val KIND_INCOME = "income"
}
