package com.vraj.khetihisab

import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.toArgb
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.vraj.khetihisab.ui.app.VkRoot
import com.vraj.khetihisab.ui.theme.VkTheme
import com.vraj.khetihisab.ui.theme.VkThemeId
import com.vraj.khetihisab.ui.theme.VkThemes
import com.vraj.khetihisab.ui.theme.resolveThemeId
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking

/** FragmentActivity because BiometricPrompt needs one (doc 04 §4). */
class MainActivity : FragmentActivity() {

    /**
     * Deep links that arrive while the activity is already running (launchMode singleTop:
     * notification / widget taps such as vraj://reminders). VkRoot hands them to the NavController
     * once the app is unlocked. Cold-start links are read by NavHost from the launch intent itself.
     */
    private val pendingDeepLink = kotlinx.coroutines.flow.MutableStateFlow<android.content.Intent?>(null)

    override fun onNewIntent(intent: android.content.Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        if (intent.data != null) pendingDeepLink.value = intent
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // doc 04 §1: release builds block screenshots and the recents preview.
        if (!BuildConfig.ALLOW_SCREENSHOTS) {
            window.setFlags(WindowManager.LayoutParams.FLAG_SECURE, WindowManager.LayoutParams.FLAG_SECURE)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) setRecentsScreenshotEnabled(false)
        }

        val container = (application as VrajApp).container
        // Read the stored theme once before the first frame so the wrong theme never flashes.
        val initialPrefs = runBlocking { container.themeRepository.prefs.first() }

        setContent {
            val prefs by container.themeRepository.prefs.collectAsStateWithLifecycle(initialPrefs)
            val locked by container.lockManager.locked.collectAsStateWithLifecycle()
            val themeId = resolveThemeId(prefs.themeId, prefs.followSystemDark)

            LaunchedEffect(themeId, locked) { applySystemBars(themeId, locked) }

            VkTheme(themeId) {
                VkRoot(
                    container = container,
                    activity = this,
                    themePrefs = prefs,
                    pendingDeepLink = pendingDeepLink,
                    onDeepLinkHandled = { pendingDeepLink.value = null },
                )
            }
        }
    }

    /**
     * Status bar: light icons over the gradient header in every theme (doc 01 §7). The lock screen
     * has no header, so light themes use dark icons there. Navigation bar uses surfaceContainer.
     */
    private fun applySystemBars(id: VkThemeId, locked: Boolean) {
        val p = VkThemes.palette(id)
        val nav = p.cont.toArgb()
        val status = if (p.isDark || !locked) {
            SystemBarStyle.dark(Color.TRANSPARENT)
        } else {
            SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT)
        }
        val navStyle = if (p.isDark) SystemBarStyle.dark(nav) else SystemBarStyle.light(nav, nav)
        enableEdgeToEdge(statusBarStyle = status, navigationBarStyle = navStyle)
    }
}
