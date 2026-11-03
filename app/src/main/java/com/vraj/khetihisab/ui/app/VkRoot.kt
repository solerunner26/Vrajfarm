package com.vraj.khetihisab.ui.app

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.vraj.khetihisab.di.AppContainer
import com.vraj.khetihisab.security.BiometricHelper
import com.vraj.khetihisab.security.LockScreen
import com.vraj.khetihisab.settings.ThemePrefs
import kotlinx.coroutines.launch

object VkRoutes {
    const val HOME = "home"
}

/**
 * App root. While [com.vraj.khetihisab.security.LockManager.locked] is true only the LockScreen is
 * composed — the NavHost is not in the tree at all, so there is no flash of content (doc 04 §2).
 * Feature agents register their destinations in the NavHost below.
 */
@Composable
fun VkRoot(container: AppContainer, activity: FragmentActivity, themePrefs: ThemePrefs) {
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
    NavHost(navController = nav, startDestination = VkRoutes.HOME) {
        composable(VkRoutes.HOME) {
            HomePlaceholderScreen(
                themeId = themePrefs.themeId,
                onThemeChange = { id -> scope.launch { container.themeRepository.setTheme(id) } },
            )
        }
    }
}
