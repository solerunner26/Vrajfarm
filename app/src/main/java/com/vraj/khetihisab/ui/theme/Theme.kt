package com.vraj.khetihisab.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember

/**
 * Root theme (doc 01 §7). Dynamic colour (Material You) is OFF by design: the five themes are
 * the brand, and Samsung wallpaper colours would break the single-hue look.
 */
@Composable
fun VkTheme(id: VkThemeId = VkThemeId.DEFAULT, content: @Composable () -> Unit) {
    val (scheme, extra) = remember(id) { VkThemes.of(id) }
    CompositionLocalProvider(
        LocalVkExtra provides extra,
        LocalVkIcons provides VkIcons.of(id),
        LocalVkThemeId provides id,
    ) {
        MaterialTheme(colorScheme = scheme, typography = VkTypography, shapes = VkShapes, content = content)
    }
}

/** Resolves "follow system dark mode": NIGHT when the system is dark, otherwise the chosen theme. */
@Composable
fun resolveThemeId(chosen: VkThemeId, followSystemDark: Boolean): VkThemeId =
    if (followSystemDark && isSystemInDarkTheme()) VkThemeId.NIGHT else chosen

/** doc 02 name kept for compatibility: green theme, or night when [darkTheme]. */
@Composable
fun VrajTheme(darkTheme: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) =
    VkTheme(if (darkTheme) VkThemeId.NIGHT else VkThemeId.DEFAULT, content)

val LocalVkThemeId = androidx.compose.runtime.staticCompositionLocalOf { VkThemeId.DEFAULT }
