package com.vraj.khetihisab.ui.app

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import androidx.navigation.navDeepLink
import com.vraj.khetihisab.di.AppContainer
import com.vraj.khetihisab.notify.VkDeepLinks
import com.vraj.khetihisab.security.BiometricHelper
import com.vraj.khetihisab.security.LockScreen
import com.vraj.khetihisab.settings.ThemePrefs
import com.vraj.khetihisab.ui.assets.AssetsModule
import com.vraj.khetihisab.ui.assets.AssetsScreen
import com.vraj.khetihisab.ui.assets.AssetsViewModel
import com.vraj.khetihisab.ui.assets.LendScreen
import com.vraj.khetihisab.ui.assets.LendViewModel
import com.vraj.khetihisab.ui.assets.RepoAssetsStore
import com.vraj.khetihisab.ui.entry.EntryScreen
import com.vraj.khetihisab.ui.home.homeGraph
import com.vraj.khetihisab.ui.reminders.remindersGraph
import com.vraj.khetihisab.ui.shopping.RepoShoppingStore
import com.vraj.khetihisab.ui.shopping.ShoppingScreen
import com.vraj.khetihisab.ui.shopping.ShoppingViewModel
import kotlinx.coroutines.launch

object VkRoutes {
    const val HOME = "home"
    const val ENTRY = "entry?kind={kind}&section={section}&farm={farm}&family={family}"
    const val SHOPPING = "shopping"
    const val ASSETS = "assets?remind={remind}"
    const val LEND = "lend?assetId={assetId}"

    fun entry(kind: String) = "entry?kind=$kind"
    fun assets(remindLoanId: Long? = null) = if (remindLoanId == null) "assets" else "assets?remind=$remindLoanId"
    fun lend(assetId: Long? = null) = if (assetId == null) "lend" else "lend?assetId=$assetId"
}

/**
 * App root. While [com.vraj.khetihisab.security.LockManager.locked] is true only the LockScreen is
 * composed — the NavHost is not in the tree at all, so there is no flash of content (doc 04 §2).
 * Only the lead integrator edits this file (HANDOVER.md, Wave 2 contract).
 */
@Composable
fun VkRoot(
    container: AppContainer,
    activity: FragmentActivity,
    themePrefs: ThemePrefs,
    pendingDeepLink: kotlinx.coroutines.flow.StateFlow<android.content.Intent?> = kotlinx.coroutines.flow.MutableStateFlow(null),
    onDeepLinkHandled: () -> Unit = {},
) {
    val lockManager = container.lockManager
    val locked by lockManager.locked.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()

    if (locked) {
        val bioAvailable = remember { BiometricHelper.isAvailable(activity) }
        LockScreen(
            pinStore = container.pinStore,
            onUnlocked = lockManager::unlock,
            biometricAvailable = bioAvailable,
            onBiometric = { BiometricHelper.prompt(activity, onSuccess = lockManager::unlock) },
        )
        return
    }

    val nav = rememberNavController()
    // Warm deep links (notification / widget taps while the app is open). Waits for unlock,
    // because this code only runs once the lock screen has returned above.
    val deepLink by pendingDeepLink.collectAsStateWithLifecycle()
    androidx.compose.runtime.LaunchedEffect(deepLink) {
        deepLink?.let { intent ->
            runCatching { nav.handleDeepLink(intent) }
            onDeepLinkHandled()
        }
    }
    NavHost(navController = nav, startDestination = VkRoutes.HOME) {
        // Home (start), farms, farm/{id}, family, personal — doc 05/07/08/09.
        homeGraph(nav)
        // Reminders list + editor (doc 13); deep link vraj://reminders.
        remindersGraph(nav)

        composable(
            VkRoutes.ENTRY,
            arguments = listOf(
                navArgument("kind") { type = NavType.StringType; defaultValue = "expense" },
                navArgument("section") { type = NavType.StringType; nullable = true; defaultValue = null },
                navArgument("farm") { type = NavType.LongType; defaultValue = -1L },
                navArgument("family") { type = NavType.LongType; defaultValue = -1L },
            ),
        ) {
            EntryScreen(onBack = { nav.popBackStack() }, onSaved = { nav.popBackStack() })
        }

        composable(VkRoutes.SHOPPING, deepLinks = listOf(navDeepLink { uriPattern = VkDeepLinks.SHOPPING })) {
            val deps = AssetsModule.get(LocalContext.current)
            val vm: ShoppingViewModel = viewModel(factory = ShoppingViewModel.factory(RepoShoppingStore(deps.shopping, deps.master)))
            ShoppingScreen(vm, onBack = { nav.popBackStack() }, onOpenAssets = { nav.navigate(VkRoutes.assets()) })
        }

        composable(
            VkRoutes.ASSETS,
            arguments = listOf(navArgument("remind") { type = NavType.LongType; defaultValue = -1L }),
        ) { entry ->
            val deps = AssetsModule.get(LocalContext.current)
            val remind = entry.arguments?.getLong("remind")?.takeIf { it > 0 }
            val vm: AssetsViewModel = viewModel(
                factory = AssetsViewModel.factory(RepoAssetsStore(deps.assets, deps.profiles), container.whatsAppTemplates::loanTemplate, remind),
            )
            AssetsScreen(vm, onBack = { nav.popBackStack() }, onLend = { id -> nav.navigate(VkRoutes.lend(id)) })
        }

        composable(
            VkRoutes.LEND,
            arguments = listOf(navArgument("assetId") { type = NavType.LongType; defaultValue = -1L }),
        ) { entry ->
            val deps = AssetsModule.get(LocalContext.current)
            val assetId = entry.arguments?.getLong("assetId")?.takeIf { it > 0 }
            val vm: LendViewModel = viewModel(
                factory = LendViewModel.factory(RepoAssetsStore(deps.assets, deps.profiles), assetId, container.whatsAppTemplates::loanTemplate),
            )
            LendScreen(vm, onBack = { nav.popBackStack() }, onDone = { nav.popBackStack() })
        }
    }
}
