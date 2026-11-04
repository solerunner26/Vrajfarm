package com.vraj.khetihisab.ui.components

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.window.DialogWindowProvider
import androidx.core.view.WindowCompat

/**
 * Call first inside every ModalBottomSheet. M3 picks the sheet window's status-bar icon colour
 * from the PHONE's dark mode, not the app theme, so in the Night theme on a light phone the clock
 * was dark on a dark sheet. This matches the icons to the sheet surface instead.
 */
@Composable
fun VkSheetSystemBars() {
    val view = LocalView.current
    val lightSurface = MaterialTheme.colorScheme.surfaceContainerLowest.luminance() > 0.5f
    SideEffect {
        var p = view.parent
        while (p != null && p !is DialogWindowProvider) p = p.parent
        val window = (p as? DialogWindowProvider)?.window ?: return@SideEffect
        WindowCompat.getInsetsController(window, window.decorView).apply {
            isAppearanceLightStatusBars = lightSurface
            isAppearanceLightNavigationBars = lightSurface
        }
    }
}
