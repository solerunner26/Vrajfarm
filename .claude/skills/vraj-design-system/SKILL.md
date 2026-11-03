---
name: vraj-design-system
description: Vraj Kheti Hisab visual rules for Compose — two-tone Vraj Green Material 3 colour roles, Vk* components, type scale, ic_vk icon usage, aesthetic ornaments, Gujarati copy conventions and the Design canvas reference. Load before building or reviewing any screen or widget.
---

# Vraj Kheti Hisab design system — two-tone Vraj Green Material 3

Full spec: `handover/01_Design_System.md`. It is the source of truth; this skill is a summary.
Canvas: https://claude.ai/artifact/PVTaG7qVWVFJ7LSDoFvgnR · Icons: `design/icons/` · Logo: `design/logo/logo-mark.svg`

**One hue, two tones.** No orange, blue, purple or teal section colours. Sections differ by icon and label, never by colour.

## Colour roles (`ui/theme/Color.kt`, `dynamicColor = false`)
| Role | Hex | Use |
|---|---|---|
| primary / onPrimary | #1F5C45 / #FFFFFF | main actions, income amounts |
| primaryContainer / onPrimaryContainer | #D3EADB / #0B2E21 | tonal buttons, selected chips |
| secondary | #4E8C6C | the second (lighter) green tone |
| secondaryContainer / onSecondaryContainer | #E4F2E9 / #123D2D | hero card tint, chat bubbles |
| tertiary | #9A6B12 | **Panchang/festival only** |
| tertiaryContainer / onTertiaryContainer | #F5E9CF / #3D2A05 | Panchang card, festival dialog band |
| error / errorContainer / onErrorContainer | #B3261E / #F9DEDC / #410E0B | overdue, udhaar only |
| surface | #F5F7F4 | app background |
| surfaceContainerLowest | #FFFFFF | cards |
| surfaceContainerLow / surfaceContainer | #EEF3EF / #E7EEE9 | inputs, tracks, bottom dock |
| onSurface / onSurfaceVariant | #17201B / #46534C | text |
| outline / outlineVariant | #74827A / #C4D0C8 | borders |

- Brand gradient (135°): `#2E7D5B → #174A37`. Use it for the header band, the primary CTA, the selected segment and the logo tile.
- Money: income `+₹` in primary, expense `−₹` in onSurface, pending or overdue in error.
- Dark theme: surface #0F1512, cards #18201B, primary #8FD3AE, primaryContainer #1F5C45. Light is the default for sunlight.

## Shape, type, size
- Cards 16dp radius with a 1dp outlineVariant border and no elevation. Buttons are pills, 60dp tall. Dialogs 28dp. Chips 12dp, 48–56dp tall.
- Noto Sans Gujarati:
  - display 30sp/800
  - headline 22sp/800
  - title 18sp/700
  - body 18sp
  - label 16sp/700
  - meta 14sp
- Baloo Bhai 2 is for the wordmark only.
- Touch targets ≥ 60dp. Keypad keys 64dp. Gutter 16dp.

## Components (`ui/components/`; screens use only these)
VkTopHeader (hero band + field motif + toran edge) · VkCard(tone) · VkSectionTitle (leaf divider) · VkPrimaryButton · VkTonalButton · VkOutlinedButton · VkSegmented · VkChip · VkListRow · VkAmount · VkStatusBadge · VkNumericKeypad · VkPhotoSlot · VkPanchangCard · VkFestivalDialog · VkBottomDock

## Ornaments (`ui/decor/`, decorative, `contentDescription = null`)
- field furrows + sun arc at 10–14% white in the header
- toran scallop edge
- leaf-sprig divider
- moon-phase glyph
- rangoli dots (gold 25%, Panchang/festival only)

## Icons
`Image(painterResource(R.drawable.ic_vk_<name>), null)`. Never tint.
- All icons share one palette: tile #E6F3EB→#CBE6D6, glyph #2E7D5B→#174A37, accent #9ED3B3→#5FA580.
- Sizes: 40dp on cards, 28–32dp in buttons, 20dp in badges.
- Names: farm, family, utility, personal, shopping, asset, lend, reminder, income, expense, message, camera, bill, calculator, backup, settings, help, profile, panchang, moon (tithi), diya (festival), lock, mic, report.

## Copy
- Gujarati first. Dates use `dd-MM-yyyy` + tithi (`02-11-2026 · આસો વદ આઠમ`). Money uses `₹48,800` (Indian grouping).
- Panchang guidance always carries "માન્યતા પ્રમાણે".
- Key strings: ઉધાર બાકી, દેવાના બાકી, લેવાના બાકી, ચૂકતે કરો, મિલકત ઉછીની આપો, ફોટો પાડો, યાદ અપાવો, પરત મળ્યું, દિવસ મોડું, DONE · પૂરું થયું, WhatsApp પાવતી મોકલો, સમજાયું, રિમાઇન્ડર બનાવો, કેલેન્ડર જુઓ.
