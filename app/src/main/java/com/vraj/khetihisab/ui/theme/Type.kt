package com.vraj.khetihisab.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.vraj.khetihisab.R

/*
 * Fonts (OFL, licences in assets/licenses/):
 *  - res/font/noto_sans_gujarati.ttf  — Noto Sans Gujarati variable font (wdth, wght 100–900),
 *    from github.com/google/fonts. One file serves all five weights via FontVariation (API 26+;
 *    minSdk is 29). Static per-weight TTFs were not consistently published, so the variable
 *    master is bundled instead of noto_sans_gujarati_regular/_medium/… named in doc 02.
 *  - res/font/baloo_bhai2.ttf — Baloo Bhai 2 variable (wght 400–800). Wordmark only.
 */

@OptIn(ExperimentalTextApi::class)
private fun noto(weight: Int) = Font(
    R.font.noto_sans_gujarati,
    weight = FontWeight(weight),
    variationSettings = FontVariation.Settings(FontVariation.weight(weight)),
)

@OptIn(ExperimentalTextApi::class)
val NotoSansGujarati = FontFamily(noto(400), noto(500), noto(600), noto(700), noto(800))

@OptIn(ExperimentalTextApi::class)
val VkWordmark = FontFamily(
    Font(
        R.font.baloo_bhai2,
        weight = FontWeight.ExtraBold,
        variationSettings = FontVariation.Settings(FontVariation.weight(800)),
    ),
)

private fun style(size: Int, weight: Int, lineHeightEm: Double = 1.4) = TextStyle(
    fontFamily = NotoSansGujarati,
    fontSize = size.sp,
    fontWeight = FontWeight(weight),
    // Gujarati matras need more vertical room than Latin (doc 17 #19).
    lineHeight = lineHeightEm.em,
)

/** doc 01 §3 mapping. Body ≥ 18sp, headers 22sp, labels for buttons 16sp/700. */
val VkTypography = Typography(
    displayLarge = style(40, 800, 1.25),
    displayMedium = style(34, 800, 1.25),
    displaySmall = style(30, 800, 1.25), // hero money
    headlineLarge = style(28, 800, 1.3),
    headlineMedium = style(24, 800, 1.3),
    headlineSmall = style(22, 800, 1.3), // card headers, screen titles
    titleLarge = style(20, 800, 1.35),
    titleMedium = style(18, 700), // list titles, body emphasis
    titleSmall = style(16, 700),
    bodyLarge = style(18, 400, 1.5), // body
    bodyMedium = style(16, 400, 1.5),
    bodySmall = style(14, 500), // meta, dates
    labelLarge = style(16, 700, 1.25), // buttons
    labelMedium = style(14, 700, 1.25),
    labelSmall = style(12, 600, 1.25),
)

/** Currency is at least 26sp bold (CLAUDE.md). */
val VkCurrencyStyle = style(26, 800, 1.25)

val VkWordmarkStyle = TextStyle(fontFamily = VkWordmark, fontWeight = FontWeight.ExtraBold, fontSize = 30.sp, lineHeight = 1.3.em)
