package com.vraj.khetihisab.ui.home

import androidx.compose.runtime.Composable
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.vraj.khetihisab.data.entity.AccountSection
import com.vraj.khetihisab.ui.family.FamilyRoute
import com.vraj.khetihisab.ui.farms.FarmDetailRoute
import com.vraj.khetihisab.ui.farms.FarmPeriod
import com.vraj.khetihisab.ui.farms.FarmsRoute
import com.vraj.khetihisab.ui.farms.PeriodChoice
import com.vraj.khetihisab.ui.farms.SeasonRange
import com.vraj.khetihisab.ui.personal.PersonalRoute

/**
 * Registers `home`, `farms`, `farm/{farmId}`, `family` and `personal` (docs 05, 07, 08, 09).
 * Wiring: see `ui/home/INTEGRATION.md`. Screens never touch [navController]; they get lambdas.
 *
 * @param homeOverlay drawn on top of Home, e.g. the once-a-day Panchang dialog owned by the
 *   Panchang module (doc 10). It receives the same safe `onOpen(route)` navigator.
 */
fun NavGraphBuilder.homeGraph(
    navController: NavController,
    homeOverlay: @Composable (onOpen: (String) -> Unit) -> Unit = {},
) {
    val open: (String) -> Unit = { route -> navController.navigateSafely(route) }
    val back: () -> Unit = { navController.popBackStack() }

    composable(HomeRoutes.HOME) {
        HomeRoute(onOpen = open)
        homeOverlay(open)
    }

    composable(HomeRoutes.FARMS) {
        FarmsRoute(
            onBack = back,
            onOpenFarm = { id, choice -> open(HomeRoutes.farmDetail(id, choice.period, choice.cropYear)) },
            onReport = { byVigha -> open(HomeRoutes.reports(if (byVigha) "vigha" else "farm_pnl")) },
        )
    }

    composable(
        HomeRoutes.FARM_DETAIL,
        arguments = listOf(
            navArgument("farmId") { type = NavType.LongType },
            navArgument("period") { type = NavType.StringType; nullable = true; defaultValue = null },
            navArgument("year") { type = NavType.IntType; defaultValue = -1 },
        ),
    ) { entry ->
        val args = entry.arguments
        val farmId = args?.getLong("farmId") ?: -1L
        val current = SeasonRange.current(ScreensFormat.today())
        val period = args?.getString("period")?.let { n -> FarmPeriod.entries.firstOrNull { it.name == n } } ?: current.period
        val year = args?.getInt("year")?.takeIf { it > 0 } ?: current.cropYear
        FarmDetailRoute(
            farmId = farmId,
            choice = PeriodChoice(period, year),
            onBack = back,
            onAddEntry = { id -> open(HomeRoutes.entry(HomeRoutes.KIND_EXPENSE, AccountSection.FARM, farmId = id)) },
            onEditEntry = { id -> open(HomeRoutes.editEntry(id)) },
        )
    }

    composable(HomeRoutes.MILK) {
        com.vraj.khetihisab.ui.milk.MilkRoute(
            onBack = back,
            onDairyPayment = { open(HomeRoutes.entry(HomeRoutes.KIND_INCOME, AccountSection.ANIMAL)) },
        )
    }

    composable(HomeRoutes.FAMILY) {
        FamilyRoute(
            onBack = back,
            onEditEntry = { id -> open(HomeRoutes.editEntry(id)) },
            onAddAdvance = { familyId -> open(HomeRoutes.entry(HomeRoutes.KIND_EXPENSE, AccountSection.FAMILY, familyId = familyId)) },
        )
    }

    composable(HomeRoutes.PERSONAL) {
        PersonalRoute(
            onBack = back,
            onExpense = { open(HomeRoutes.entry(HomeRoutes.KIND_EXPENSE, AccountSection.PERSONAL)) },
            onIncome = { open(HomeRoutes.entry(HomeRoutes.KIND_INCOME, AccountSection.PERSONAL)) },
            onEditEntry = { id -> open(HomeRoutes.editEntry(id)) },
        )
    }

    // સામાન્ય ખર્ચ: its own screen (the Home tile used to open Settings by mistake).
    composable(HomeRoutes.GENERAL) {
        PersonalRoute(
            onBack = back,
            onExpense = { open(HomeRoutes.entry(HomeRoutes.KIND_EXPENSE, AccountSection.GENERAL)) },
            onIncome = { open(HomeRoutes.entry(HomeRoutes.KIND_INCOME, AccountSection.GENERAL)) },
            onEditEntry = { id -> open(HomeRoutes.editEntry(id)) },
            section = AccountSection.GENERAL,
        )
    }
}
