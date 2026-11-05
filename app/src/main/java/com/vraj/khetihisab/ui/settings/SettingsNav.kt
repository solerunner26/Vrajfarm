package com.vraj.khetihisab.ui.settings

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import androidx.navigation.navDeepLink
import com.vraj.khetihisab.report.ReportType
import com.vraj.khetihisab.ui.help.HelpRoute
import com.vraj.khetihisab.ui.panchang.PanchangCalendarRoute
import com.vraj.khetihisab.ui.profiles.ProfilesRoute
import com.vraj.khetihisab.ui.reports.ReportsRoute

/** Routes owned by the settings group (see INTEGRATION.md). */
object SettingsRoutes {
    const val SETTINGS = "settings"
    const val THEME = "settings/theme"
    const val PANCHANG_ALERTS = "settings/panchang-alerts"
    const val GENERAL = "settings/general"
    const val PANCHANG = "settings/panchang"
    const val BACKUP = "settings/backup"
    const val FARMS = "settings/farms"
    const val CROPS = "settings/crops"
    const val CATEGORIES = "settings/categories"
    const val SECURITY = "settings/security"
    const val TEMPLATES = "settings/templates"
    /** Routes owned by other graphs, opened from Settings tiles (audit #38). */
    const val ASSETS_ALL = "assets?tab=all"
    const val MILK = "milk"

    const val REPORTS = "reports?type={type}"
    const val PROFILES = "profiles"
    const val HELP = "help"
    const val CALENDAR = "panchang/calendar?epochDay={epochDay}"

    /** Notification deep links (notify/VkDeepLinks): `vraj://panchang/{epochDay}` and `vraj://panchang`. */
    const val DEEP_LINK_PANCHANG_DAY = "vraj://panchang/{epochDay}"
    const val DEEP_LINK_PANCHANG = "vraj://panchang"

    const val ARG_TYPE = "type"
    const val ARG_EPOCH_DAY = "epochDay"
    const val NO_DAY = Long.MIN_VALUE

    fun reports(type: ReportType? = null) = if (type == null) "reports" else "reports?type=${type.slug}"
    fun calendar(epochDay: Long? = null) = if (epochDay == null) "panchang/calendar" else "panchang/calendar?epochDay=$epochDay"
}

/**
 * Settings, Master Settings sub-screens, Reports, Profiles, Help and the Panchang calendar.
 * Call inside the app NavHost: `settingsGraph(nav)`.
 *
 * @param onOpenReminders optional: shown as a snackbar action after "રિમાઇન્ડર બનાવો" (doc 13 screen, if present).
 */
fun NavGraphBuilder.settingsGraph(
    nav: NavController,
    onOpenReminders: (() -> Unit)? = null,
) {
    val back: () -> Unit = { nav.popBackStack() }
    val open: (String) -> Unit = { route -> nav.navigate(route) { launchSingleTop = true } }

    composable(SettingsRoutes.SETTINGS) {
        SettingsRoute(SettingsActions(onBack = back, onOpen = open))
    }
    composable(SettingsRoutes.PANCHANG) { PanchangGuidanceRoute(onBack = back) }
    composable(SettingsRoutes.THEME) { ThemeSettingsRoute(onBack = back) }
    composable(SettingsRoutes.PANCHANG_ALERTS) { PanchangAlertsRoute(onBack = back, onOpen = open) }
    composable(SettingsRoutes.GENERAL) { GeneralSettingsRoute(onBack = back, onOpen = open) }
    composable(SettingsRoutes.BACKUP, deepLinks = listOf(navDeepLink { uriPattern = com.vraj.khetihisab.backup.BackupNotifier.BACKUP_DEEP_LINK })) {
        BackupSettingsRoute(onBack = back)
    }
    composable(SettingsRoutes.FARMS) { FarmsSettingsRoute(onBack = back) }
    composable(SettingsRoutes.CROPS) { CropsSettingsRoute(onBack = back) }
    composable(SettingsRoutes.CATEGORIES) { CategoriesSettingsRoute(onBack = back) }
    composable(SettingsRoutes.SECURITY) { SecuritySettingsRoute(onBack = back) }
    composable(SettingsRoutes.TEMPLATES) { TemplatesSettingsRoute(onBack = back) }

    composable(
        SettingsRoutes.REPORTS,
        arguments = listOf(navArgument(SettingsRoutes.ARG_TYPE) { type = NavType.StringType; nullable = true; defaultValue = null }),
    ) { entry ->
        val slug = entry.arguments?.getString(SettingsRoutes.ARG_TYPE)
        ReportsRoute(initialType = ReportType.entries.firstOrNull { it.slug == slug }, onBack = back)
    }
    composable(SettingsRoutes.PROFILES) { ProfilesRoute(onBack = back) }
    composable(SettingsRoutes.HELP) { HelpRoute(onBack = back) }
    composable(
        SettingsRoutes.CALENDAR,
        arguments = listOf(navArgument(SettingsRoutes.ARG_EPOCH_DAY) { type = NavType.LongType; defaultValue = SettingsRoutes.NO_DAY }),
        deepLinks = listOf(
            navDeepLink { uriPattern = SettingsRoutes.DEEP_LINK_PANCHANG_DAY },
            navDeepLink { uriPattern = SettingsRoutes.DEEP_LINK_PANCHANG },
        ),
    ) { entry ->
        val day = entry.arguments?.getLong(SettingsRoutes.ARG_EPOCH_DAY)?.takeIf { it != SettingsRoutes.NO_DAY }
        PanchangCalendarRoute(initialEpochDay = day, onBack = back, onOpenReminders = onOpenReminders, onOpenEntry = open)
    }
}
