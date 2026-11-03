# 01 · Design System — "Vraj Green" (two-tone Material 3)

This file replaces the earlier multi-colour, dark-glass direction. **Every screen uses one hue family (green) in two tones**, applied as a gradient. Neutral surfaces carry everything else. A single muted gold is used **only** for Panchang and festival ornaments. Red is used **only** as the Material 3 `error` role (overdue items, unpaid udhaar).

Visual reference: Design canvas https://claude.ai/artifact/PVTaG7qVWVFJ7LSDoFvgnR

## 1. Colour roles (`ui/theme/Color.kt`)

| M3 role | Hex | Notes |
|---|---|---|
| primary | `#1F5C45` | deep Vraj green |
| onPrimary | `#FFFFFF` | |
| primaryContainer | `#D3EADB` | tonal buttons, selected chips |
| onPrimaryContainer | `#0B2E21` | |
| secondary | `#4E8C6C` | the second (lighter) green tone |
| secondaryContainer | `#E4F2E9` | |
| onSecondaryContainer | `#123D2D` | |
| tertiary (Panchang gold) | `#9A6B12` | Panchang/festival only |
| tertiaryContainer | `#F5E9CF` | Panchang card, festival dialog band |
| onTertiaryContainer | `#3D2A05` | |
| error | `#B3261E` | overdue, udhaar |
| errorContainer / onErrorContainer | `#F9DEDC` / `#410E0B` | |
| surface | `#F5F7F4` | app background |
| surfaceContainerLowest | `#FFFFFF` | cards |
| surfaceContainerLow | `#EEF3EF` | inputs, segmented tracks |
| surfaceContainer | `#E7EEE9` | list dividers' band, bottom dock |
| onSurface | `#17201B` | primary text (≈15:1 on surface) |
| onSurfaceVariant | `#46534C` | secondary text (≈7:1) |
| outline / outlineVariant | `#74827A` / `#C4D0C8` | |

**Brand gradient** (`Brush.linearGradient`, 135°): `#2E7D5B → #174A37`. Use it for the top header band, the primary CTA, the selected segment and the logo tile. Nothing else is a gradient, except the icon glyphs and a 4–6% tint of `secondaryContainer` behind hero cards.

**Money colours.** Income `+₹` uses `primary`. Expenses `−₹` use `onSurface`. Pending or overdue amounts use `error`. Nothing is orange, blue or purple.

## 2. Shape & spacing

- Cards: 16dp radius, 16dp padding, 1dp `outlineVariant` border, no elevation (M3 filled/outlined card).
- Buttons: full pill (M3). Primary = filled with the brand gradient, 60dp tall. Secondary = tonal (`primaryContainer`). Tertiary = outlined.
- Dialogs: 28dp radius (M3 standard).
- Chips / segmented buttons: 12dp radius, 48–56dp tall, selected = `primaryContainer` + check icon.
- Touch targets ≥ 60dp for list rows and main actions. Keypad keys are 64dp.
- Screen gutter 16dp. Vertical rhythm 12/16/24dp.

## 3. Typography (Noto Sans Gujarati; the wordmark only uses Baloo Bhai 2)

| Style | Size / weight | Use |
|---|---|---|
| displaySmall | 30sp / 800 | hero money |
| headlineSmall | 22sp / 800 | card headers, screen titles |
| titleMedium | 18sp / 700 | list titles, body emphasis |
| bodyLarge | 18sp / 400 | body |
| labelLarge | 16sp / 700 | buttons |
| bodySmall | 14sp / 500 | meta, dates (onSurfaceVariant) |

Currency is at least 26sp bold.

## 4. Aesthetic elements (subtle, two-tone only)

1. **Header band**: a brand-gradient top area (~170dp on Home) with a line-art motif drawn at 10–14% white: gentle field furrows and a rising-sun arc. Its bottom edge is a soft **toran-scallop** curve.
2. **Leaf-sprig divider**: a small two-leaf glyph between hairlines, used before section titles.
3. **Moon-phase glyph** on the Panchang card, drawn from the tithi (0–30).
4. **Corner rangoli dots**: a faint 3-dot ornament in gold at 25% on the Panchang and festival dialogs only.
All ornaments are `ImageVector`s in `ui/decor/`. They are decorative (`contentDescription = null`).

## 5. Icons

There are 24 `ic_vk_*` vectors (the original 22 plus `moon` for tithi alerts and `diya` for festivals), all in the **same two-tone green**:
- tile `#E6F3EB → #CBE6D6`
- main glyph `#2E7D5B → #174A37`
- accent glyph `#9ED3B3 → #5FA580`

Draw them with `Image()` and never tint them. Sizes: 40dp on cards, 28–32dp in buttons, 20dp in list badges. Source SVGs: `design/icons/`.

## 6. Components (build once in `ui/components/`, reuse everywhere)

`VkTopHeader`, `VkCard`, `VkSectionTitle` (with leaf divider), `VkPrimaryButton`, `VkTonalButton`, `VkSegmented`, `VkChip`, `VkListRow` (60dp+), `VkAmount` (sign + colour by type), `VkStatusBadge` (ok / due / overdue), `VkNumericKeypad`, `VkPhotoSlot`, `VkPanchangCard`, `VkFestivalDialog`, `VkBottomDock`.
Screens may only compose these. **Do not hand-style a button or card inside a screen.**

## 7. Five user-selectable themes (supersedes the single green palette above)

The user picks a theme in **Settings › થીમ**. All five themes use the same components, sizes, shapes and ornaments. **Only colour tokens change.** Section 1 above is the *green* theme. The canonical token source is `design/vk-theme.css`; mirror it exactly in `ui/theme/VkThemes.kt`.

| Token (M3 role) | green · વ્રજ લીલો | mati · માટી | akash · આકાશ | kesar · કેસર | night · રાત્રિ (dark) |
|---|---|---|---|---|---|
| gradient g1 → g2 | #2E7D5B → #174A37 | #B5562E → #7A3218 | #3F51B5 → #1A237E | #9A6408 → #5E3B00 | #245E47 → #0E2A20 |
| onGradient | #FFFFFF | #FFFFFF | #FFFFFF | #FFFFFF | #E8F3EC |
| surface (bg) | #F5F7F4 | #F8F5F3 | #F5F6FA | #F9F6EF | #0F1512 |
| card (surfaceContainerLowest) | #FFFFFF | #FFFFFF | #FFFFFF | #FFFFFF | #18201B |
| surfaceContainerLow | #EEF3EF | #F3ECE8 | #EEF0F6 | #F4EEE2 | #1E2722 |
| surfaceContainer | #E7EEE9 | #ECE3DE | #E6E8F1 | #EDE5D5 | #222C26 |
| outlineVariant | #C4D0C8 | #E0D0C8 | #CDD1E4 | #E2D6BF | #2C3830 |
| outline | #74827A | #85736B | #767A8F | #827562 | #8A9890 |
| onSurface | #17201B | #221A16 | #191B24 | #211B10 | #E3EAE5 |
| onSurfaceVariant | #46534C | #55463F | #464A5C | #51473A | #B9C6BE |
| primary | #1F5C45 | #8F3E1E | #2F3E9E | #7A5000 | #8FD3AE |
| onPrimary | #FFFFFF | #FFFFFF | #FFFFFF | #FFFFFF | #003824 |
| primaryContainer | #D3EADB | #F6DCCF | #DDE1FF | #FCE3B0 | #1F5C45 |
| onPrimaryContainer | #0B2E21 | #3A1404 | #0A1660 | #2B1A00 | #C8EED8 |
| secondaryContainer | #E4F2E9 | #FBEAE2 | #EEF0FF | #FFF1D6 | #1B2B23 |
| onSecondaryContainer | #123D2D | #4A1D0B | #1A237E | #3D2600 | #C4E3D1 |
| tertiary (Panchang text) | #7A520A | #7A520A | #7A520A | #8A3B12 | #E8C27A |
| tertiaryContainer | #F5E9CF | #F5E9CF | #F5E9CF | #F8E1D4 | #3A2E14 |
| onTertiaryContainer | #3D2A05 | #3D2A05 | #3D2A05 | #3A1405 | #F3DFB4 |
| error / errorContainer / onErrorContainer | #B3261E / #F9DEDC / #410E0B | same | same | same | #FFB4AB / #5C1A15 / #FFDAD6 |

The Saffron theme's Panchang accent is a maroon-brown so that it doesn't blend into the saffron primary. In every theme, gold is for Panchang only and red is for errors only.

Contrast checked: body text ≥ 4.5:1 on its surface in every theme, and white header text ≥ 4.5:1 on g1. Kesar's g1 was darkened from #A86F0A to #9A6408 to pass.

### Implementation (Compose)

```kotlin
enum class VkThemeId { GREEN, MATI, AKASH, KESAR, NIGHT }

@Immutable data class VkExtraColors(
    val gradientStart: Color, val gradientEnd: Color, val onGradient: Color,
    val headerButton: Color, val ornament: Color, val silhouette: Color, val silhouetteBg: Color,
)
val LocalVkExtra = staticCompositionLocalOf<VkExtraColors> { error("VkTheme missing") }

@Composable fun VkTheme(id: VkThemeId, content: @Composable () -> Unit) {
    val (scheme, extra) = VkThemes.of(id)          // lightColorScheme(...) / darkColorScheme(...) from the table
    CompositionLocalProvider(LocalVkExtra provides extra, LocalVkIcons provides VkIcons.of(id)) {
        MaterialTheme(colorScheme = scheme, typography = VkTypography, shapes = VkShapes, content = content)
    }
}
```

- **Persistence:** `ThemeRepository` stores the chosen id in DataStore Preferences (`vk_theme`). Default is `GREEN`. `MainActivity` collects it and wraps the nav host in `VkTheme(id)`. Changing the theme recomposes live, with no restart.
- **System bars:** call `enableEdgeToEdge()`. Status-bar icons are light over the gradient header in every theme. The navigation bar uses `surfaceContainer`.
- **Icons:** draw each `ic_vk_*` as an `ImageVector` whose three brushes come from `LocalVkIcons.current` (tile, glyph, accent per theme; values in `design/gen-themes.js`). Don't ship 5×24 drawables. The canvas used recoloured SVGs only for preview.
- **Glance widgets:** read the same DataStore key and use `ColorProvider(day, night)` with that theme's card and primary colours.
- **Dynamic colour (Material You) stays OFF.** The five themes are the brand.
- **"Follow system dark mode"** is an optional toggle under the picker. When it's on, the app uses `NIGHT` whenever the system is dark and the chosen light theme otherwise.

## 8. Text-fitting rules (from the design QA audit, doc 17)

1. **Buttons, chips, tabs and segmented buttons are always one line:** `maxLines = 1` and `softWrap = false`. If a label doesn't fit, shorten the label. Don't let it wrap.
2. **Label length budgets at 360dp width:**
   - half-width button: ≤ 12 Gujarati characters at 16sp
   - 3-column button: ≤ 8 characters at 14sp
   - 4-column chip: 1–3 characters (put the unit in the section label instead)
3. **List titles and meta use `overflow = TextOverflow.Ellipsis`.** Amounts never shrink (`Modifier.wrapContentWidth()`).
4. **Never place an icon and a long label together in a button narrower than 160dp.** Drop the icon.
5. **Dialog actions:** if two actions don't fit side by side at 16sp, stack them full-width (filled → tonal → text).
6. **Reserve space for content that appears later** (for example the "હોમ ખોલો" button after the PIN), so the layout doesn't jump.
