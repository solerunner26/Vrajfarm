# Developer guide

Everything needed to build, change, test and release Vraj Kheti Hisab. Project rules are in [CLAUDE.md](../CLAUDE.md); the live status board is [HANDOVER.md](../HANDOVER.md); per-feature specs are in [handover/](../handover/00_README.md).

## Stack

| | |
|---|---|
| Language / UI | Kotlin 2.2, Jetpack Compose (BOM 2025.08), Material 3, dynamic colour off |
| Data | Room (KSP), schema **v2**, 14 entities; DataStore; EncryptedSharedPreferences |
| Background | WorkManager + AlarmManager; Glance widgets |
| Other | OkHttp (update download), Coil, Biometric |
| Android | minSdk 29, targetSdk 35, package `com.vraj.khetihisab` |
| Build | AGP 8.13, Gradle 9.3; output in `%LOCALAPPDATA%\VrajKhetiBuild\` (outside OneDrive) |

No Firebase/Supabase and no server: all data is local.

## Build and test (Windows)

```powershell
$env:JAVA_HOME = "C:\Program Files\Android\Android Studio\jbr"
.\gradlew.bat assembleDebug                 # debug APK
.\gradlew.bat testDebugUnitTest             # JVM + Robolectric tests
.\gradlew.bat connectedDebugAndroidTest     # instrumented (emulator / device)
.\gradlew.bat assembleRelease               # signed, R8-shrunk (needs the release key)
```

`local.properties` (git-ignored) needs `sdk.dir=C:\\Users\\<you>\\AppData\\Local\\Android\\Sdk`.

Emulator: `emulator -avd Medium_Phone_API_36.1 -no-snapshot -gpu host` (`swiftshader_indirect` crashes on the dev PC). The PIN pad ignores `adb input text`; tap the keys. `connectedDebugAndroidTest` uninstalls the app afterwards, and fails with `INSTALL_FAILED_UPDATE_INCOMPATIBLE` if a release-signed build is installed — uninstall it first.

## Project layout (`app/src/main/java/com/vraj/khetihisab/`)

| Package | What |
|---|---|
| `data/` | Room entities, DAOs, repositories, seed data, `AppDatabase` (+ migrations) |
| `ui/components/` | The shared `Vk*` components — screens use only these |
| `ui/theme/` | 5 themes (tokens only), typography, `VkIconImage` (3D embossed icons), `VkIconSet` |
| `ui/home`, `entry`, `farms`, `family`, `personal`, `milk`, `assets`, `shopping`, `reminders`, `panchang`, `reports`, `settings`, `profiles`, `help` | Screens + view models |
| `ui/decor/` | Ornaments, `VkMoon3D` |
| `panchang/` | Tithi / festival engine |
| `backup/` | JSON backup, Drive/WhatsApp/Gmail, nightly worker, reminders |
| `update/` | GitHub self-update |
| `report/` | Gujarati PDF generator (letterhead) |
| `share/` | WhatsApp templates and launcher (pre-filled only) |
| `notify/`, `widget/`, `security/` | Notifications, Glance widgets, PIN/biometric lock |

## Money rules (do not break)

- Money is `Long` paise. APMC: `TotalMann = Mann + ExtraKg / 20.0`, `TotalAmount = TotalMann * RatePerMann`.
- Sections: FARM, ANIMAL, FAMILY (ભાગિયા advances), GENERAL, PERSONAL. The business P&L counts all but PERSONAL; expenses include FAMILY advances. Farm P&L and the Vigha scorecard use FARM only. Profit per Vigha is 0 when Vigha is 0.
- Every new P&L query gets a DAO test.

## Database

- Schema JSON is exported to `app/schemas/` and committed.
- Every schema change bumps `AppDatabase.VERSION`, adds an explicit `Migration(n, n+1)` and a `MigrationTestHelper` test (`androidTest/.../MigrationTest.kt`). `fallbackToDestructiveMigration()` is forbidden.
- v1 → v2: `farm.jointManagers` (farm run by both ભાગિયા; use `FarmEntity.managedBy / managers / encode`) and the `milk_entry` table.
- ભાગિયા = farm manager = the `family` rows A/B. Edit people only in `person_profile` (MANAGER_1/2); `ProfileRepository.syncBhagiya()` mirrors name and phone into `family`.

## UI rules

- Two-tone, one hue per theme; colours only from `MaterialTheme.colorScheme` / `LocalVkExtra`. Red = M3 `error` only (and calendar expense days, by owner request). Gold only for Panchang.
- Body ≥ 18sp, currency ≥ 26sp bold (`VkFitMoney` shrinks to fit, never below 18sp), touch targets ≥ 60dp, cards 16dp, dialogs 28dp. Buttons, chips, tabs: one line.
- All copy Gujarati in `res/values/strings*.xml`; every text field sends the Gujarati keyboard hint (`VkKeyboard.kt`).
- **Selection controls** (`VkSelect.kt`): fixed choices → `VkDropdownField`; type-or-pick → `VkComboField`; dates/times → `VkPickerField`; the main pick of a flow → `VkChoiceGrid` (equal tiles). No wrapping chip rows for choices.
- Dialogs: `VkDialog` (stacked actions; delete goes in `deleteLabel/onDelete`, never inside scrolling content). Sheets: `VkBottomSheet`. Bottom bars: `VkActionBar` (below the list, never over it). Feedback: app snackbar `LocalVkSnackbar` with undo, `VkSuccessBurst`.
- Before a release, screenshot every changed screen on the emulator and look at it.

## Backup format

`backup/JsonBackup.kt` writes one streamed JSON:

```json
{ "app": "vraj_kheti_hisab", "format": 2, "dbVersion": 2, "appVersion": "0.2.25",
  "createdAt": 1793807723394, "photoCount": 3,
  "counts": { "txn": 120, "farm": 8 },
  "tables": { "txn": [ { "id": 1, "date": 20396, "amountPaise": 500000 } ] },
  "files":  [ { "path": "bill_photos/a.webp", "data": "<base64>" } ] }
```

Restore checks `app`, refuses newer `dbVersion`, parses fully, then replaces all tables in one transaction and swaps the photo folders. Destinations: Drive via a persisted SAF document URI (overwritten nightly), WhatsApp share, Gmail SMTP (`GmailSender`, App Password in EncryptedSharedPreferences). The PIN and theme are not in the backup.

## Self-update

- Each GitHub release carries the APK and `update.json` (`versionCode`, `versionName`, `apk`, `sha256`, `size`, `notes`).
- The app reads `releases/latest/download/update.json` every time it comes to the front (10-minute gap) and twice a day in the background (`UpdateCheckWorker`, one notification per version).
- Download: 4 parallel byte ranges, SHA-256 checked, installed with `PackageInstaller`. The install receiver only relaunches system confirm screens and strips URI grants.

## Releasing

```powershell
# from a clean, committed main
powershell -ExecutionPolicy Bypass -File tools\release.ps1 -Notes "નવું: ..."
```

The script builds the release APK, sets `versionCode` = git commit count and `versionName` = `0.2.<code>`, writes `update.json` and publishes GitHub release `v<versionName>` as latest. Installed apps then offer the update.

**Release key:** `%USERPROFILE%\.vrajkheti\vrajkheti-release.jks` + `keystore.properties`. Never commit it and never change it: phones can only update in place when every APK has the same signature. Keep a backup copy offline.

## Git workflow

Direct pushes to `main` are rejected. Work on a branch, open a PR, merge, then release from `main`. Commits are authored as Jaldip Vala. Update `HANDOVER.md` at every milestone.
