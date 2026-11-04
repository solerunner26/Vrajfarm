package com.vraj.khetihisab.ui.reminders

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.navDeepLink
import com.vraj.khetihisab.notify.VkDeepLinks

object RemindersRoutes {
    /** Route of the Reminders screen. Deep link: [VkDeepLinks.REMINDERS] = `vraj://reminders`. */
    const val REMINDERS = "reminders"
}

/**
 * Registers the Reminders screen. The lead adds `remindersGraph(nav)` inside VkRoot's NavHost;
 * notification taps and the ReminderGlanceWidget header open it through `vraj://reminders`.
 */
fun NavGraphBuilder.remindersGraph(navController: NavController) {
    composable(
        RemindersRoutes.REMINDERS,
        deepLinks = listOf(navDeepLink { uriPattern = VkDeepLinks.REMINDERS }),
    ) {
        val context = androidx.compose.ui.platform.LocalContext.current.applicationContext
        val vm: RemindersViewModel = viewModel(
            factory = RemindersViewModel.factory(RepoReminderStore(context)) { needsNotificationPermission(context) },
        )
        RemindersScreen(vm, onBack = {
            if (!navController.popBackStack()) navController.navigate("home")
        })
    }
}

private fun needsNotificationPermission(context: Context): Boolean =
    Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
        ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
