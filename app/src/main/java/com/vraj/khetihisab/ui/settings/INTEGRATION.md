# Wave 3B — Settings / Reports / Profiles / Help / Panchang calendar: wiring

Owner packages: `ui/settings/**`, `ui/reports/**`, `ui/profiles/**`, `ui/help/**`, `ui/panchang/**`,
strings in `res/values/strings_settings.xml` (prefix `st_`). Nothing outside these was edited.

## 1. NavHost (ui/app/VkRoot.kt — lead only)

```kotlin
import com.vraj.khetihisab.ui.settings.settingsGraph
import com.vraj.khetihisab.ui.settings.SettingsRoutes

NavHost(navController = nav, startDestination = VkRoutes.HOME) {
    // … existing routes …
    settingsGraph(nav, onOpenReminders = { nav.navigate("reminders") }) // pass null until Reminders exists
}
```

Routes registered by `settingsGraph`:

| Route | Screen |
|---|---|
| `settings` | Main Settings (theme, backup card, Panchang switches, general list, PDF chips) |
| `settings/panchang` | Guidance editor (કરવું / ટાળવું, reset to default) + "મારો તહેવાર ઉમેરો" |
| `settings/backup` | Endpoint URL + API key entry, "કનેક્શન તપાસો" (ping) |
| `settings/farms` | Farms: name, વીઘા (0–500), manager, active crop cycle; add farm |
| `settings/family` | Family A/B display name + WhatsApp number |
| `settings/crops` | Crops: add / rename / activate |
| `settings/categories` | Categories per section incl. **ANIMAL (પશુપાલન)**: add / rename / deactivate (never delete) |
| `settings/security` | Biometric toggle + PIN change (current → new → confirm) |
| `settings/templates` | WhatsApp template editor (loan reminder, family statement) |
| `reports?type={slug}` | 7 PDF reports; `SettingsRoutes.reports(ReportType.X)` preselects one |
| `profiles` | Owner + 2 managers, photos, farm assignment, WhatsApp / call |
| `help` | Help & About (the ONLY place with the developer credit) |
| `panchang/calendar?epochDay={epochDay}` | Month grid + markers, special-days list, day sheet |

Deep links on the calendar destination: `vraj://panchang/{epochDay}` (matches `VkDeepLinks.panchangDay`)
and `vraj://panchang` (today). The manifest `vraj://` VIEW filter already exists; `MainActivity` must forward
`onNewIntent` (the pending-deep-link StateFlow already in VkRoot handles it once wired).

Navigate from Home: `nav.navigate(SettingsRoutes.SETTINGS)`, `SettingsRoutes.calendar()`,
`SettingsRoutes.PROFILES`, `SettingsRoutes.HELP`.

## 2. Panchang day dialog on Home (doc 10 scheduling §5)

Home (ui/home owner) should compose, once, above its content:

```kotlin
com.vraj.khetihisab.ui.panchang.PanchangAlertHost(
    onOpenCalendar = { epochDay -> nav.navigate(SettingsRoutes.calendar(epochDay)) },
)
```

It shows `VkFestivalDialog` when `PanchangAlertEngine.alertFor(today, DAY_DIALOG)` is not null and
`panchang_dialog_dismissed_<epochDay>` is absent. Every action calls
`PanchangAlertSettingsStore.markDialogDismissed`; "રિમાઇન્ડર બનાવો" uses
`ReminderRepository.createPanchangReminder` (deduplicated, toast "રિમાઇન્ડર પહેલેથી છે").

## 3. Dependencies used (no new DI needed)

- `ScreensDepsProvider` / `screenViewModel` (ui/home, read-only): repos, ThemeRepository, PinStore, LockManager, `onBackupNow`.
- `NotifyGraph.settings(ctx)` = `PanchangAlertSettingsStore` (the only store for evening time, show-guidance, વદ બીજ, dismissals).
- `PanchangNotifier.reschedule(ctx)` after any Panchang switch / time / custom-festival change.
- `BackupGraph.deps(ctx)`: `status`, `settings` (EncryptedSharedPreferences), `uploader.ping`.
- `GujaratiPdfGenerator(ctx, AppDatabase.get(ctx))` + `ReportShare.open/share` (lock suppressed first).
- `WhatsAppTemplateStore(ctx)`; WhatsApp/call through `ScreensShare` (→ `WhatsAppLauncher.open`, pre-filled "રામ રામ {name},").

## 4. ui/panchang/PanchangSource

Now a thin facade over `PanchangAlertEngine` / `FestivalRules` (no fallback logic left). Same public API
(`day`, `ekadashiName`, `special`, `specialDaysInRange`, `alertFor`) plus `alertFrom`.

## 5. Requests for other owners

1. **ui/home owner — duplicate strings.** `strings_screens.xml` contains an older copy of the Settings /
   Profiles / Help / calendar copy (`scr_settings_*`, `scr_pa_*`, `scr_profile_*`, `scr_faq_*`,
   `scr_credit_*`, `scr_weekdays_short`, …). These screens use `st_*` only. Please delete the unused
   `scr_*` keys — especially `scr_credit_en/name/gu`, so the credit text exists in one resource only
   (doc 14 "credit only in Help"; QA greps for it).
2. **media owner (optional).** Doc 14 asks for `PersonPhotoCapture.processProfile`; it does not exist and
   `process()` stamps a timestamp (borrower style). Profiles therefore use `ui/profiles/ProfilePhoto`
   (ImageDecoder → centre-square 512×512 → WebP q80 stepping down to ≤ 60 KB → `filesDir/people_photos/`).
   File names are `<role>_<millis>.webp` (old file deleted after the DB update) instead of overwriting
   `<role>.webp`, because `VkPhotoSlot` loads `File(path)` without the `path@updatedAt` Coil cache key.
3. **Home avatar** should read `PersonProfileEntity.photoPath` live (it changes name on every new photo).
4. **Backup restore** ("બેકઅપમાંથી પાછું લાવો", doc 15) has no API in `backup/` yet, so there is no restore row.

## 6. Deviations (intentional)

- Manager card buttons: WhatsApp + ફોન કરો side by side, "સુધારો" full width below (3 × 16sp labels wrap/ellipsize at 360dp; doc 01 §8).
- FAQ uses the canvas copy (7 items incl. the PIN question from doc 14); each item is a `VkCard` with `animateContentSize` + stateDescription.
- Calendar: tapping a day opens the doc 10 `ModalBottomSheet` (the canvas' inline selected-day card is folded into the sheet).
- Vad Bij switch sits directly under the Bij row on the main Settings screen.

## 7. Tests

`app/src/test/java/com/vraj/khetihisab/ui/{settings,reports,profiles,help,panchang}/` — 33 JVM tests:
guidance save/reset/custom festivals, PIN change flow + lockout, category grouping (ANIMAL), Nov 2026
calendar grid/markers against the real engine, Panchang dialog once-per-day, profile save + exclusive farm
assignment + photo replace/fail, reports ranges/generate, CreditOnlyInHelpTest (file scan).
