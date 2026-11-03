# 17 · Design QA Audit — findings and fixes (2026-11-03)

This is a senior design-review pass over all 16 screens on the canvas. Each issue below has been fixed on the canvas. The rules that came out of the review are in `01_Design_System.md §8`; Claude Code must follow them in the Compose build.

**Method.** I reviewed the code of every artboard. For each one I checked text width against the available width at 360dp, contrast ratios for every theme, touch-target sizes, layout jumps, and consistency of controls. The canvas could not be screenshotted in this session, so **a visual pass on the real device (doc 16) is still required.**

## Findings

| # | Screen | Problem | Severity | Fix |
|---|---|---|---|---|
| 1 | Home dock | "− ખર્ચ / ઉપાડ લખો" and "+ આવક / વેચાણ લખો" wrapped to two lines in a 158dp button | High | Labels shortened to "ખર્ચ / ઉપાડ" and "આવક / વેચાણ" with a +/− sign. The button is now one line, enforced with nowrap. |
| 2 | Entry, all segmented buttons | "✓ + આવક / વેચાણ" was too long and could wrap | High | The ✓ now comes from the selected style, not the label, and labels were shortened ("રોકડ ચૂકવેલ", "ઉધાર બાકી"). Segments clip with ellipsis instead of wrapping. |
| 3 | Home section cards | Meta lines like "લાઇટ · પાણી · CCTV ₹6,840" wrapped to 2–3 lines in 140dp cards | Medium | Shortened ("આ મહિને ₹6,840"). Titles are single-line with ellipsis. |
| 4 | Recent-entry rows | Long titles next to the amount wrapped and pushed the amount out of line | Medium | Title and meta are single-line with ellipsis. The amount never shrinks. |
| 5 | Lend "return in" chips | "✓ 15 દિવસ" overflowed a 74dp chip | High | Chips show only the number. The unit moved to the label ("કેટલા દિવસ પછી પરત?"). |
| 6 | Assets loan card | "યાદ અપાવો" with an icon overflowed a 140dp button | Medium | Icon removed from the half-width button. |
| 7 | Panchang dialog | Two side-by-side actions ("રિમાઇન્ડર બનાવો" / "કેલેન્ડર જુઓ") had about 4dp to spare | Medium | All actions are stacked full-width: filled → tonal → text. |
| 8 | Entry photo/voice buttons | Icon + label + 20dp padding overflowed 160dp | Medium | Padding reduced to 12dp. |
| 9 | Settings theme picker | "વ્રજ લીલો" overflowed a 54dp swatch tile | Medium | The tile shows the short name ("લીલો"). The full name appears in the note below. |
| 10 | PIN lock | "હોમ ખોલો" appeared and pushed the keypad down (layout jump) | Medium | A 56dp slot is reserved for the button at all times. |
| 11 | Gold text on gold container | #9A6B12 on #F5E9CF measured about 3.9:1 (fails AA) | High | Panchang text token changed to #7A520A (≥ 5:1). In night it is #E8C27A on #3A2E14. |
| 12 | Struck-through shopping items | #74827A on white measured about 4.1:1 | Medium | Done items use onSurfaceVariant (≥ 7:1) plus strike-through. |
| 13 | Kesar theme header | White on #A86F0A measured about 4.2:1 | High | g1 darkened to #9A6408 (≥ 5:1). |
| 14 | Links styled as cards | The global link colour turned whole card text green | Medium | Card, row, hero and header-button links keep their own text colour. |
| 15 | Back buttons | Text "‹" glyph was inconsistent across fonts | Low | Replaced with a 24dp stroke arrow icon everywhere. |
| 16 | Farms rank badges | Ranks 4–8 inherited the gradient avatar style | Low | Only the top 3 use the gradient. The others use a neutral container. |
| 17 | Icons in the night theme | Light icon tiles glared on dark cards | High | A dedicated night icon set with dark tiles and mint glyphs. |
| 18 | Controls drawn ad hoc per screen | Buttons, chips and cards had slightly different paddings and radii on different screens | High | Every screen now uses one shared component set (`design/vk-theme.css` maps 1:1 to the `Vk*` Compose components). |

## Visual pass in the browser (Play mode, 1:1 scale) — 2026-11-03

**Checked:**
- Home and Entry in Green.
- Settings in Green and Night, including a live theme switch.
- Lend in Green.
- Panchang dialog in Green.

**Not inspected visually:** the other screens, and the Mati, Akash and Kesar themes. Those rely on the same shared components, and their contrast was checked numerically.

**Confirmed working:**
- Every button, segmented button and chip renders on one line.
- Noto Sans Gujarati renders, conjuncts included.
- Night recolours every control and icon.

| # | Screen | Problem seen on screen | Fix |
|---|---|---|---|
| 19 | Lend | The bottom "સાચવો / સાચવો + મોકલો" buttons were cut off at the screen edge, because Gujarati lines render taller than estimated | Increased the height of every screen by 8–14%. Verified: both buttons fully visible. |
| 20 | Home | The last recent entry was hidden behind the bottom dock | Taller screen. Verified: the last row sits above the dock. |
| 21 | Home | The Family card meta was truncated to "A ₹12,000 · B ₹8,5…" | Changed to "આ મહિને ₹20,500". The A/B split stays on the Family screen. |
| 22 | Home | The settings icon read as a sun or brightness | Replaced with a standard gear outline. Verified. |
| 23 | All | The farm icon (sun + hill) read as a person avatar at 40dp | Redrawn as a two-leaf sprout over a furrowed field in all 5 themes. Verified. |
| 24 | Settings | The Night swatch looked like a second green | The swatch is now charcoal (#2B3631 → #07090A). |

**Compose implication.** Never use fixed screen heights. Every screen is a `LazyColumn` or a `verticalScroll` with bottom content padding equal to the dock or FAB height plus 16dp.

## Acceptance checks for the Compose build (add to doc 16)

- [ ] Screenshot test at 360×780dp, for every screen in every theme (16 × 5): no `Text` in a Button, Chip, Tab or SegmentedButton reports `lineCount > 1`. Use `onTextLayout` in debug builds and fail the test if it does.
- [ ] Font scale 1.3 (Samsung "large" text): labels may ellipsize but must never wrap or overlap. A second screenshot set is required at this scale.
- [ ] Contrast test (`AccessibilityChecks.enable()` in Espresso) passes in all 5 themes.
- [ ] Switching theme in Settings recolours every visible surface, icon and widget within one frame, with no restart.
- [ ] Night theme: no pure-white surfaces anywhere, the status bar is dark, and photos are not inverted.
