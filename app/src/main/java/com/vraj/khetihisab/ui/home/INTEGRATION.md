# ui/home — integration notes (Wave 3A: Home, Farms, Family, Personal)

Owner: compose-screens. Only the lead integrator edits `ui/app/VkRoot.kt`, `di/**`, `VrajApp` and the manifest.
Everything below is what the integrator must do there. Nothing else needs to change.

## 1. Register the graph and remove the placeholder (required)

In `ui/app/VkRoot.kt`, inside `NavHost(navController = nav, startDestination = VkRoutes.HOME)`:

```kotlin
import com.vraj.khetihisab.ui.home.homeGraph

// delete:  composable(VkRoutes.HOME) { HomePlaceholderScreen(...) }
homeGraph(nav)
```

- `homeGraph` registers `home` (= `VkRoutes.HOME`, still the start destination), `farms`,
  `farm/{farmId}?period={period}&year={year}`, `family` and `personal`.
- After that, `ui/app/HomePlaceholderScreen.kt` is unused and can be deleted, together with the
  `home_*` / `home_wave1_note` strings in `values/strings.xml` that only it uses
  (`home_greeting`, `home_sections`, `home_section_*`, `home_this_month`, `home_recent`, `home_all`, `home_wave1_note`).
  The theme picker it showed belongs on the Settings screen.
- Optional overlay: `homeGraph(nav) { onOpen -> PanchangDayDialog(onOpen) }` draws a composable on top of
  Home. Use it for the once-a-day Panchang / festival dialog (doc 05 logic 1, doc 10), which is owned by the
  Panchang module. Home itself only shows the "આગામી તિથિ અને પર્વ" card (from `ui/panchang/PanchangSource`).

## 2. Entry presets (required for "+ ઉપાડ ઉમેરો", farm "+ આ ખેતર માટે એન્ટ્રી" and the Personal dock)

These screens navigate to `HomeRoutes.entry(kind, section, farmId, familyId)`, e.g.
`entry?kind=expense&section=FAMILY&family=2`, `entry?kind=income&section=PERSONAL`,
`entry?kind=expense&section=FARM&farm=5`. `EntryViewModel` already reads `section`, `farm` and `family`
from its `SavedStateHandle` (`ARG_SECTION`, `ARG_FARM`, `ARG_FAMILY`), but VkRoot only declares `kind`, so
today the extra values are ignored (Entry still opens, with only the kind set). Change the route to:

```kotlin
const val ENTRY = "entry?kind={kind}&section={section}&farm={farm}&family={family}"

composable(
    VkRoutes.ENTRY,
    arguments = listOf(
        navArgument("kind") { type = NavType.StringType; defaultValue = "expense" },
        navArgument("section") { type = NavType.StringType; nullable = true; defaultValue = null },
        navArgument("farm") { type = NavType.LongType; defaultValue = -1L },
        navArgument("family") { type = NavType.LongType; defaultValue = -1L },
    ),
) { EntryScreen(onBack = { nav.popBackStack() }, onSaved = { nav.popBackStack() }) }
```

`VkRoutes.entry(kind)` keeps working unchanged.

## 3. Routes Home calls in other modules

All calls go through `NavController.navigateSafely(route)` (in `ScreensUi.kt`): a route that is not
registered yet does nothing instead of crashing. Register these names (or tell compose-screens to rename):

| Constant (`HomeRoutes`) | Route | From | Registered by |
|---|---|---|---|
| `SETTINGS` | `settings` | header gear, "સામાન્ય ખર્ચ" card | `settingsGraph` (`SettingsRoutes.SETTINGS`) |
| `BACKUP_SETTINGS` | `settings/backup` | backup pill | `settingsGraph` (`SettingsRoutes.BACKUP`) |
| `PROFILES` | `profiles` | owner avatar | `settingsGraph` (`SettingsRoutes.PROFILES`) |
| `PANCHANG` | `panchang/calendar` | "આગામી તિથિ અને પર્વ" card | `settingsGraph` (`SettingsRoutes.CALENDAR`) |
| `reports(slug)` | `reports?type=vigha` / `farm_pnl` | Farms header PDF button | `settingsGraph` (`SettingsRoutes.REPORTS`) |
| `REMINDERS` | `reminders` | urgent reminder rows | reminders graph (`RemindersRoutes.REMINDERS`) |
| `SHOPPING` | `shopping` | "ખરીદી, મિલકત" card | VkRoot (exists) |
| `ASSETS` | `assets` | overdue loan card, "વધુ N" | VkRoot (exists) |
| `entry(...)` | `entry?kind=…` | dock, family, farm detail, personal dock | VkRoot (exists, see §2) |

If one of those graphs is not wired yet, the tap is simply a no-op.

## 4. Dependencies

No new DI. ViewModels are created with `screenViewModel { deps, handle -> … }` from `ScreensDepsProvider`,
which `VrajApp` already installs (shared `DataRepositories`, `backupInfo` for the Home pill).
WhatsApp goes through `ScreensShare` → `share/WhatsAppLauncher` (loan reminder via
`WhatsAppLauncher.loanReminder`, family statement via `WhatsAppTemplates.familyStatement` with the
user-edited template from `WhatsAppTemplateStore`).

## 5. Behaviour notes / deviations

- **Business P&L card** ("કુલ નફો-નુકસાન") uses the financial year Apr–Mar containing today
  (`HomeLogic.businessRange`), shown as the chip "વર્ષ 2026-27 ›". Tapping opens the breakdown sheet
  (`businessBySection`, fixed order ખેતી, પશુપાલન, સામાન્ય ખર્ચ, ભાગિયા ઉપાડ; PERSONAL never listed).
- **Farm card / Farms summary** use `farmTotals` (FARM section incl. entries whose farm was deleted).
  The Home farm card period = season of the newest active crop cycle, else the full crop year.
- **Season chips**: crop year 2026 = ખરીફ 2026 (Jun–Oct), રવી 2026 (01-11-2026 → 31-03-2027),
  ઉનાળુ 2027 (Mar–Jun 2027), આખું વર્ષ (Apr 2026 → Mar 2027), with ‹ / › year buttons.
- **પશુપાલન card**: this month's `animalTotals` ("દૂધ ₹… · ખર્ચ ₹…" + month net); tapping opens a sheet
  with `animalByCategory`.
- **Family**: year total is the calendar year Jan–Dec (open question 5 — the વિ.સં. year toggle is not built).
  The bubble text is exactly the text handed to WhatsApp. Missing number → dialog saves it to
  `FamilyEntity.whatsappPhone`, then opens WhatsApp. Long-press an entry → delete (soft) with confirm.
  Editing an entry needs an Entry edit route (not available yet).
- **Personal**: month entries come from `entries(range)` filtered to PERSONAL; personal udhaar card only on
  this screen. The dock opens Entry pre-set to PERSONAL (needs §2).
- **Farms PDF button** (doc 07 §1) opens the Reports screen pre-selected on report #3 (vigha) or #2
  (farm_pnl) depending on the scorecard sort; generation itself is owned by Reports.
- **"બધી" (see all)** on Home recent entries opens `farms` (as on the canvas) until an all-entries screen exists.
