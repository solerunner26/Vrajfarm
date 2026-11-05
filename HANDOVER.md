# HANDOVER — live status board

> Every AI or human working on this repo updates this file **before stopping** (see `AGENTS.md` §5).
> Last updated: **2026-11-05 by Agent Mode: expanded the multilingual showcase with customization, security/report USPs, guided workflows and a playable launch story**

## Snapshot
- **Phase:** Wave 1 done. Wave 2 code compiles, is integrated, and `assembleDebug` + `testDebugUnitTest` pass (2026-11-04). Several Wave 2 screens were never written (see task board).
- **Design:**
  - 16 screens + brand + 5-theme board, offline copies in `design/canvas/`
  - 17 handover docs in `handover/`
  - Visual QA done on Home, Entry, Settings (green + night), Lend and the Panchang dialog. See doc 17.
- **Code:** Android builds green. **Never launched on a device yet** (no phone/emulator attached on 2026-11-04).
- **Public showcase:** `docs/website/` contains the static, responsive Gujarati/Hindi/English presentation with 33 fictional-data screen previews, six guided workflows, a local-only feature selector, and a 30-second launch story that auto-starts and loops until paused or closed. `docs/Farm-Ledger-Website.zip` is the ready-to-upload archive. Security claims describe implemented controls only; built-in reports are the documented seven PDF types. The site has no API, data collection, runtime framework or GoDaddy backup server.

## Verify (Windows)
- Double-click `tools/build-check.bat` → `tools/build-log.txt` (assembleDebug + unit tests). Last run 2026-11-04: **BUILD SUCCESSFUL**.
- Double-click `tools/device-check.bat` → `tools/device-log.txt` (adb install + launch). Last run: no device attached.

## Original Wave 1 setup notes
1. Create `local.properties` with `sdk.dir=C\:\\Users\\<you>\\AppData\\Local\\Android\\Sdk`.
2. Run, with `JAVA_HOME` set to Android Studio's `jbr`:
   - `./gradlew assembleDebug`
   - `./gradlew testDebugUnitTest`
3. If it fails, fix it within the owning module (table below) and record the result here.

## Task board

| # | Task (handover doc) | Owner agent / files | Status | Notes / next step |
|---|---|---|---|---|
| 1 | Foundation: Gradle, 5 themes, Vk components, icons as ImageVectors, Panchang engine, PIN/biometric lock (02, 04) | android-foundation: build files, `ui/theme`, `ui/components`, `ui/decor`, `panchang`, `security`, `settings/ThemeRepository` | **DONE (Claude, 2026-11-03)**, verified by the lead | `assembleDebug` OK. Unit tests: 65 run, 0 failed, 3 skipped on purpose. **Not yet launched on a device:** the emulator stayed offline. See "Foundation notes" below. |
| 2 | Database: 13 Room entities, DAOs, seeding, repositories (03) | room-database: `app/src/**/data/**` only | **DONE (Claude, 2026-11-03)** | Schema v1 exported to `app/schemas/`. Data unit tests pass (22/22). 27 instrumented DAO tests compile but **have not been run yet**: they need a device or emulator (`connectedDebugAndroidTest`). Deviations from doc 03 are listed below under "Database notes". |
| 3 | WhatsAppLauncher (12) | assets-shopping | **DONE (compiles)** | `share/`: launcher, templates, editable template store, phone normaliser. |
| 4 | Home dashboard (05) | compose-screens | **TODO — not written** | Only helpers exist (`ui/home/Screens*.kt`). `HomePlaceholderScreen` is still the start screen; its Shopping/Assets tiles and the +/− dock now navigate. Canvas: `Main.dc.html` |
| 5 | 3-tap entry, APMC, bill photo, voice (06) | compose-screens | **DONE (compiles, wired)** | Route `entry?kind=expense|income`. Not device-tested. |
| 6 | Farms P&L + Vigha scorecard (07) | compose-screens | **TODO — not written** | Repo helpers ready: `LedgerRepository.farmPnlStats`, `vighaRanking`. `Farms.dc.html` |
| 7 | Family advances + WhatsApp receipt (08) | compose-screens | **TODO — not written** | `familyAdvanceByMonth` + `WhatsAppTemplates` family statement ready. `Family.dc.html` |
| 8 | Personal account (09) | compose-screens | **TODO — not written** | Use `personalPendingList()`. `Personal.dc.html` |
| 9 | Panchang alerts, calendar, festival rules (10) | foundation (rules) + compose-screens (UI) + reminders-widgets (scheduling) | **PARTIAL** | Engine (`FestivalRules`, `PanchangAlertEngine`) + evening alarm/notifications done and wired. **Calendar screen and Panchang settings UI not written.** `ui/panchang/PanchangSource` still uses its fallback (TODO to switch to the engine). |
| 10 | Shopping, assets, lending, borrower photo, history (11) | assets-shopping | **DONE (compiles, wired)** | Routes `shopping`, `assets?remind=`, `lend?assetId=`. `LendScreen.kt` written by the integrator on 2026-11-04 (agent stopped before it); no contact picker yet. |
| 11 | Reminders, notifications, Glance widgets (13) | reminders-widgets | **PARTIAL** | Receivers, alarms, boot re-arm, 3 Glance widgets: done and in the manifest. **Reminders screen + editor sheet not written** (strings exist in `strings_reminders.xml`). |
| 12 | Profiles, Help & About (14) | compose-screens | **TODO — not written** | The credit "Design and Developed by Jaldip Vala" appears ONLY in Help |
| 13a | Server: `server/backup_receiver.php` + cPanel guide (15) | backup-reports: `server/**` | **DONE, NOT EXECUTED (Claude, 2026-11-03)** | Code reviewed only, because there's no PHP on the dev PC. **First run `php server/tests/run_tests.php` (about 35 checks) on any PHP 8 machine.** Contract changes for the Android worker: too large → **413** `{"error":"size"}` (don't retry); HTTPS is required; rate limit 10/h per IP. The real config is `vraj_config.php` outside the web root (git-ignored). |
| 13b | Android: PDF reports + 11:45 PM `NightlyBackupWorker` (15) | backup-reports | **DONE (compiles, wired)** | Fixed 2026-11-04: nested-comment bug in `BackupZipBuilder`, missing `strings_backup.xml`, PDF page list. Scheduled at app start. **Reports screen and backup settings UI (URL/key entry) not written**; needs the GoDaddy domain. |
| 14 | QA matrix, device checks (16, 17) | qa-tester | TODO | Last. Use the mobile-mcp server from `.mcp.json` if available |
| 15 | Generic public product showcase | Agent Mode: `docs/website/**` | **DONE (2026-11-05)** | Static responsive site under `docs/website/`; three languages, 33 fictional-data screen previews, six step-by-step workflows, local feature wishlist, and one 30-second captioned ad that auto-starts and loops. Ready-to-upload archive at `docs/Farm-Ledger-Website.zip`. Includes factual security safeguards and seven current built-in shareable PDF reports; no absolute-security or unlimited-report claim. `node --check` and jsdom interaction/language smoke tests pass. No app backend or GoDaddy backup service is included. |

## Foundation notes
- **Versions:**
  - Gradle 9.3.1, AGP 8.13.2, Kotlin 2.2.21, KSP 2.2.21-2.0.5
  - compile/target 35, min 29
  - Compose BOM 2025.08.01, Room 2.7.2, WorkManager 2.10.3, Glance 1.1.1
  - Kept on purpose: newer artifacts need Kotlin 2.3+.
- **APK** is at `%LOCALAPPDATA%\VrajKhetiBuild\app\outputs\apk\debug\app-debug.apk`, because the build directory is moved outside OneDrive.
- **Icons:** components take `icon: VkIcon?` (theme-aware ImageVector), not drawable ints. Use `VkIconImage(VkIcon.X)`. `node tools/svg2vk.js .` regenerates `VkIconSet.kt`, the green drawables and the launcher icon from `design/icons`.
- **Fonts:** Noto Sans Gujarati and Baloo Bhai 2 are bundled as variable TTFs (OFL licences in `assets/licenses`).
- **Panchang:**
  - Test vectors 08, 09 and 10-11-2026 in doc 02 look **one day early**. The engine (new moon 09-11-2026 12:32 IST, by the sunrise rule) says:
    - 08-11 is chaudas (Diwali that evening, by the pradosh rule)
    - 09-11 is amas
    - 10-11 is Bestu Varas
    - 11-11 is Bhai Bij
  - Those vector tests are `@Ignore`d pending a check against a printed panchang. Also check 19-03-2026: it is 6 minutes from sunrise, and Chaitra sud 1 comes out as kshaya.
- **Not built yet:**
  - doc 04 Master Settings screens and the biometric toggle row
  - doc 10 `FestivalRules` / `PanchangAlertEngine`
  - the `KhetHisabUiTest.pinLock` instrumented test
- **Startup:** `PinStore.create` and the first theme read run on the main thread. Profile them on the S22.

## Database notes (deviations from doc 03, all intentional)
- **Upserts:** `ProfileDao` is an abstract class with a role-resolving `upsert`. A plain `@Upsert` with unique(role) would silently drop edits.
- **Personal entries:** `pendingList()` excludes PERSONAL. Use `personalPendingList()` for doc 09 and `pendingListAll()` for report #6.
- **Farm managers:** `MasterDao.updateFarmManager(…, now)`, plus `setManagerFarms` (exclusive assignment).
- **Indices and columns:**
  - txn has extra indices on cycleId and categoryId.
  - `isPending` is an extension property, not a column.
- **Not stored in Room:** the "વદ બીજ પણ" toggle (Bij on Vad too). It needs a DataStore key, owned by doc 10.
- **Repository helpers available to screens:**
  - `LedgerRepository`: farmPnlStats, vighaRanking, familyAdvanceByMonth
  - `AssetRepository.lend()`: creates the overdue reminder automatically. `markReturned` closes it.
  - `ReminderRepository.createPanchangReminder` (deduplicated)
  - `DataRepositories` is the factory for AppContainer, with a `DataChangeListener` hook for widgets.

## Open questions for the owner (Jaldip)
1. Is the festival list in doc 10 §C complete? Are any local fairs missing (Mahuva/Thorala, Swaminarayan days)?
2. Are the default Panchang guidance texts OK (Agiyaras: avoid harvesting and rice; Amas: no new sowing or purchases)?
3. Should Bij alerts be Sud Bij only, or Vad Bij too?
4. Which printed jantri is the reference for checking dates?
5. Should the family-advance year be Jan–Dec or the Vikram Samvat year?
6. ~~What is the GoDaddy backup URL / domain?~~ Obsolete: GoDaddy upload removed 2026-11-04 (JSON backup to Drive / WhatsApp / e-mail instead).

## Wave 3 status (2026-11-04): this section overrides older rows in the task board
| Area | Status | Notes |
|---|---|---|
| Home (05), including the business P&L and પશુપાલન cards | **DONE, wired (start screen)** | `ui/home/HomeNav.kt` `homeGraph`. The placeholder Home was deleted. |
| Farms + farm detail (07) | **DONE, wired** | routes `farms`, `farm/{farmId}?period=&year=` |
| Family A/B (08) | **DONE, wired** | Year = Jan–Dec (open question 5). Editing an entry needs an Entry edit route (TODO). |
| Personal (09) | **DONE, wired** | route `personal` |
| Reminders (13) | **DONE, wired** | route `reminders`, deep link `vraj://reminders`. Notification, widget and screen all use `ReminderActions.markDone`. |
| Entry presets | **DONE** | `entry?kind=&section=&farm=&family=` |
| Warm deep links | **DONE** | `MainActivity.onNewIntent`, then `VkRoot` `handleDeepLink`, after unlock |
| Settings + 8 sub-screens, Reports, Profiles, Help, Panchang calendar (04, 10, 14, 15) | **DONE, wired** | `settingsGraph`. Routes: `settings`, `settings/{panchang,backup,farms,family,crops,categories,security,templates}`, `reports?type=`, `profiles`, `help`, `panchang/calendar?epochDay=` (deep link `vraj://panchang/{epochDay}`). |
| Daily Panchang dialog | **DONE, wired** | `PanchangAlertHost` is a Home overlay. Shows once a day; "રિમાઇન્ડર બનાવો" creates a reminder. `PanchangSource` now uses `PanchangAlertEngine`. |
| Unused strings | **CLEANED** | Removed 176 unused string resources (old duplicates in `strings_screens.xml`). Widget labels used by the manifest were kept. |

**All screens are built and wired.** `assembleDebug` OK. Unit tests green. **Instrumented tests: 34 run, 0 failed** on the emulator (Android 16, API 36). Device QA done 2026-11-04; see the change log.

**Known gaps:**
- Entry has no edit route yet (Family long-press can only delete).
- There is no backup *restore* (no API in `backup/`).
- The Family year is fixed to Jan–Dec (open question 5).
- The GoDaddy URL is still a placeholder (open question 6).

## Change log
- **2026-11-05 (Claude, selection-controls fix after owner review):** the owner rejected the lend screen's wrapping day chips. Root cause: the UI audit had been told to skip ui/assets/*. Every chip group in the app was re-checked and replaced:
  - New `ui/components/VkSelect.kt` with the rule for the whole app: fixed choices → `VkDropdownField`; type-or-pick → `VkComboField`; dates/times → `VkPickerField`; main pick of a flow → `VkChoiceGrid` (equal tiles, optional same-shape "+ add" tile).
  - Lend: borrower combo, asset dropdown, handover date field, return-period dropdown (+ custom date); preview hidden until name + asset are set.
  - Entry tap 2 types, farm-detail crop picker, Profiles farm assignment (multi), month picker (2 columns) → `VkChoiceGrid`. Farm settings manager/crop, shopping farm, report period, Farms period, Panchang guidance type/month/tithi, expense-type section → `VkDropdownField`. Reminder date/time, milk date, shopping and report dates → `VkPickerField`. Milk animal → `VkComboField`.
  - `VkDialog` got a `deleteLabel/onDelete` action slot and a scroll hairline; in-content delete buttons moved there.
  - Shopping icon redrawn as a woven basket (it read as a padlock) in design/icons/shopping.svg, VkIconSet and the drawable.
  - Every changed screen was screenshot-checked on the emulator. 179 unit + 56 instrumented tests green.
- **2026-11-05 (Claude + 2 agents, FINAL release):** owner said this is the last update for now.
  - **Update not shown on the Pixel:** the app checked GitHub only once per process; Android kept the process alive, so it never re-checked. Now `UpdateState.refresh()` runs every time the app comes to the front (10-min gap) and `UpdateCheckWorker` checks twice a day and posts "નવું અપડેટ" (once per version). A phone still on the debug-signed `v0.1.0-beta` has no updater and cannot take a release-signed update: uninstall once and install the latest APK.
  - **Asset management:** Settings/Home open `assets?tab=all`; summary tiles (total / lent / late), "+ નવી મિલકત ઉમેરો", assets as `VkTile`s, shared `AssetFormDialog` (name, price, note) for add/edit, delete confirm, lend bar below the list (no overlap), 60dp call button. Lend: `AssetDropdown` (M3 exposed menu) of existing assets + "+ નવી"; return chips show "N દિવસ".
  - **UI/UX audit + fix (agents):** a read-only audit (57 findings + 8 animation ideas) and a fixer pass. New shared components: `VkDialog`, `VkConfirmDeleteDialog`, `VkBottomSheet`, `VkActionBar`, `VkTextField`, `VkSwitchRow`, `VkFitMoney` (autosize + count-up), app-level snackbar (`LocalVkSnackbar`) with undo, `VkSuccessBurst`, `VkCheckMark`, `VkProgressBar`, `VkLoading`, `VkEmptyState`, `LocalVkMotion` (respects animator scale 0). 60dp header/outlined/text buttons, 56dp chips, sliding segmented pill, PIN shake, FAQ fade, growing bars, backup-done pulse. Owner decisions kept over the audit: calendar green income / red expense, Home privacy, Bhagiya naming. Not done: audit #40-#43, calendar DaySheet still a ModalBottomSheet, reminder delete has no undo (no repository restore).
  - Tests: 179 unit + 56 instrumented green.
- **2026-11-05 (Claude, owner feedback round 3):** 15 points; 175 unit + 56 instrumented tests green; v0.2.14 → new build upgrade tested on the emulator (data kept, 0 crashes).
  - **DB v2** (`AppDatabase.MIGRATION_1_2`, `MigrationTest`): `farm.jointManagers` (a farm run by BOTH ભાગિયા; use `FarmEntity.managedBy/managers/encode`) + new `milk_entry` table (14 entities). Profiles farm ticks are no longer exclusive; Settings farm dialog: કોઈ નહીં / ભાગિયા 1 / ભાગિયા 2 / બંને.
  - **Milk register** (`ui/milk/MilkScreen.kt`, `MilkDao`): AM/PM litres, fat %, rate per litre per animal; month shows litres, what the dairy should pay, what it paid (= ANIMAL INCOME in categories containing "દૂધ"), and the difference. Money stays in the ledger, so nothing double-counts.
  - **Gmail one-tap** (`backup/GmailSender.kt`): direct SMTP to smtp.gmail.com:465 (TLS + hostname check) with a Google App Password kept in EncryptedSharedPreferences (`MailSettings`). Optional nightly e-mail. The old "open Gmail" draft stays as a fallback.
  - **Backup nag:** red pill + pulsing "!" flag on Home and the Settings tile when no backup in 24 h (`BackupStatus.takenRecently`); 21:30 daily notification (`BackupScheduler.scheduleReminder`, `BackupAlarmReceiver.ACTION_REMIND`).
  - **Home privacy:** the one eye hides/reveals every Home amount (Hisab, section tiles, Udhaar card, recent rows). Date taps open the calendar. Avatar = photo or 3D profile icon (no letter).
  - **WhatsApp:** owner's exact formats as default templates (store keys bumped to `*_v2`): ભાગિયા = one advance `{date}`/`{amount}` + year total (per-row send in the ઉપાડ screen); borrowed asset = `{today}` "સુધી પાછું આપેલ નથી".
  - **Entry:** typed નોંધ field; "+ નવો પ્રકાર" chip creates and selects a type; whole-sentence voice (`VoiceEntryParser`: section, farm N, ભાગિયા name, type, amount, ઉધાર <name>).
  - **Gujarati keyboard:** every text field sends `hintLocales = gu-IN` (`VkGujaratiKeyboard`); Help explains adding Gujarati to Gboard once.
  - **Assets:** "મિલકત વ્યવસ્થા" tile in Settings + Home; edit name/price; loan reminder first after a week (or the return date), then weekly, with notification buttons પરત આવી ગયું / 1 અઠવાડિયા પછી / 1 મહિના પછી.
  - **Reports:** letterhead on every page (logo square, name/owner/address/phone centred, rule, date right, bold centred title).
- **2026-11-04 (Claude, owner feedback round 2):** all 12 owner points + 3D request done; 170 unit + 51 instrumented tests green; release APK 7.5 MB (was 74 MB debug).
  - **ભાગિયા (Bhagiya) = farm manager = old પરિવાર A/B — one person.** Edited only in Profiles (MANAGER_1/2). `ProfileRepository.syncBhagiya()` mirrors name + phone into the `family` rows (A↔MANAGER_1, B↔MANAGER_2), runs at app start and on every profile save. The separate "પરિવાર WhatsApp નંબર" settings screen was deleted. All copy says ભાગિયા.
  - **Default theme = માટી (Mati)** (`VkThemeId.DEFAULT`).
  - **Tiles everywhere** (`ui/components/VkTiles.kt`): `VkTile`, `VkHeroTile`, `VkSubTile`, staggered rise-in (`vkAppear`), springy press (`vkPressScale`), breathing icon disc, one-hue corner ornaments. Home = હિસાબ hero tile [કુલ નફો-નુકસાન, ખેતર, પશુપાલન, અંગત] + [ભાગિયા ઉપાડ][અંગત/ઘર][સામાન્ય ખર્ચ][ખરીદી, મિલકત]. Settings = 8 tiles (થીમ, સુરક્ષા, બેકઅપ, પંચાંગ સૂચના, સામાન્ય સેટિંગ, રિપોર્ટ, મદદ, એપ અપડેટ); પંચાંગ સૂચના, થીમ and સામાન્ય are their own pages (`SettingsPages.kt`).
  - **Hidden totals on Home only:** the four હિસાબ amounts show `₹ XXXXX.XX` until the eye (`VkEyeToggle`) is tapped; revealed amounts count up. Settings › સામાન્ય › "હોમ પર રકમ હંમેશાં બતાવો" (`ThemeRepository.showHomeNumbers`).
  - **Bug fixed: સામાન્ય ખર્ચ tile opened Settings.** New `general` route reuses the one-section screen (`PersonalRoute(section = GENERAL)`). New UI tests check every Home tile's route (`HomeScreenUiTest`).
  - **Bug fixed: bottom dock covered the last tiles/rows** (taps hit "+ ખર્ચ"). Home, Personal/Samanya and Farm detail now put the dock below the list, not over it.
  - **Calendar money:** day number green (`LocalVkExtra.income`) for income days, red (M3 error) for outgoing; small bars when both; day sheet lists that day's entries with totals and "+ આવક / + ખર્ચ" for that date (`TxnDao.dailyTotals`).
  - **Edit + delete everywhere:** tap any ledger row → `EntryActionsSheet` (સુધારો / કાઢી નાખો). Entry edit route `entry?edit={id}` updates the same row (bill photo kept safe). Delete added for expense types, crops (blocked when used in a season → offer "બંધ કરો"), farms, shopping items (+ edit).
  - **Simpler ઉધાર:** tap 3 now says આપી દીધા / હજી આપવાના (expense) or મળી ગયા / હજી લેવાના (income); a name is required for udhaar; a plain sentence shows what will be saved. Home card: મારે આપવાના / મારે લેવાના. Help has 4 new FAQs (ઉધાર, edit/delete, update, ભાગિયા).
  - **Backup rewritten, GoDaddy removed** (`server/`, uploader, ZIP builder, settings deleted). `backup/JsonBackup.kt` = one streamed .json of all 13 tables + photos (base64). Destinations (`BackupDestinations.kt`): Google Drive via the system "Save to" picker, persisted URI, overwritten nightly at 23:45 (= sync, no Google Cloud setup); WhatsApp share; Gmail with saved address. Restore: pick the file, or "Open with" from WhatsApp/Gmail (`RestoreInbox`, content:// only) → confirm dialog with date/counts → transaction replace → app restarts. Round-trip test in `RoundTwoTest`.
  - **Self-update from GitHub** (`update/AppUpdater.kt`, `UpdateUi.kt`): reads `releases/latest/download/update.json` once per start; one-button dialog; 4-part parallel Range download; SHA-256 check; PackageInstaller (Android asks once to allow this source, then shows its own confirm). Receiver only relaunches system confirm screens and strips URI grants (android/skills intent-security).
  - **Automatic versions + signing:** versionCode = git commit count, versionName = `0.2.<code>`. Release key at `%USERPROFILE%\.vrajkheti\vrajkheti-release.jks` + `keystore.properties` (outside the repo and OneDrive). `tools/release.ps1` builds, writes update.json and publishes the GitHub release.
  - **3D look:** every `VkIconImage` is embossed (drop shadow, bevel highlight, glossy sheen) in all themes; moon phases are drawn as a realistic sphere (`ui/decor/VkMoon3D.kt`: maria, craters, earthshine, limb darkening, halo), also as tiny calendar markers.
  - Release lint found a real bug: WorkManager's expedited backup needs `SystemForegroundService` with `foregroundServiceType="dataSync"` — added to the manifest.
  - Testing per android/skills `testing/testing-setup`: navigation tests (step 10), behaviour tests (9), font scale 1.5 (11), DAO tests for every new query (7), end-to-end on the emulator (12): entry → udhaar → edit → calendar → backup to file → delete → restore → data back, 0 crashes.
- **2026-11-04 (Claude, device QA + fixes):**
  - **Tested on the emulator:** `Medium_Phone_API_36.1` with `-gpu host`, 1080×2400. Every screen was clicked through in the Night theme, then Home in mati/akash/kesar.
  - **Flows verified:**
    - PIN.
    - Entry: APMC 30 મણ + 10 kg × ₹1,600 = ₹48,800; ANIMAL ₹4,500 counted; PERSONAL ₹9,999 excluded → business net ₹44,300.
    - Shopping: add an item.
    - Lend: borrower, new asset, return date, photo nudge. WhatsApp opens pre-filled; with no WhatsApp installed it falls back to the share sheet and never sends.
    - The loan creates a return reminder.
    - Reminders: add.
    - Settings, Profiles, Help (credit only there).
    - Daily PDF, whose business-total row is correct.
    - Panchang calendar + day sheet (Diwali 08-11).
    - Farms, Family, Personal.
  - **Fixed:**
    1. Backup "not set up" now shows the neutral "બેકઅપ બાકી", not red "નિષ્ફળ", and no longer shows the message twice. Implemented in `BackupStatus.stateAt` (NOT_CONFIGURED ≠ failure).
    2. Disabled `VkPrimaryButton`/`VkTonalButton` now use the M3 disabled style (12 % / 38 % onSurface, no gradient).
    3. A brand-coloured status-bar scrim plus the theme background are set once in `VkRoot`:
       - Scrolled content no longer runs under the status bar.
       - Screens without their own background (Reminders) no longer stay light in Night.
    4. The Entry amount uses Indian grouping (₹4,500).
    5. The Settings backup subtitle can wrap to 2 lines.
    6. The Lend "સાચવો + મોકલો" button had its icon dropped so the label is not cut (doc 01 §8.4).
    7. `VkSheetSystemBars()` is called in every ModalBottomSheet. M3 took the icon colour from the phone's dark mode, so the clock was dark-on-dark in Night.
    8. The Farms crop-year switcher uses chevron buttons, so "પાક વર્ષ 2026-27" is no longer cut off.
    9. The Personal header uses "અંગત / ઘર" (the title was cut by the month chip).
    10. `ProfileDao.all()` orders OWNER, MANAGER_1, MANAGER_2. It had sorted alphabetically.
    11. Test libraries were bumped (espresso 3.7.0, runner/core 1.7.0, ext-junit 1.3.0), because Espresso 3.6 crashes on API 36 (`InputManager.getInstance`).
  - **Minor, not fixed:**
    - The shopping icon reads a little like a lock.
    - The Home header has no logo mark (doc 05).
    - The new-asset dialog has extra bottom space.
- **2026-11-04 (Claude, cleanup + animal husbandry):**
  - **Removed:**
    - `ui/home/WhatsAppBridge.kt`, a temporary share-sheet stand-in that bypassed `WhatsAppLauncher` (breaking the CLAUDE.md rule). `ScreensShare` now calls `WhatsAppLauncher.open`, and its unused `loanReminder` was dropped.
    - `ui/panchang/PanchangUiPrefs.kt`, a duplicate SharedPreferences copy of `notify/PanchangAlertSettingsStore` (DataStore) that was never used.
    - `test/.../panchang/alerts/ProbeTest.kt`, a debug probe that only wrote a temp file.
  - **Added the ANIMAL section (પશુપાલન):**
    - enum value, 7 seeded categories, a cow icon `ic_vk_animal` in all 5 themes, and a 5th entry tile (3 + 2 layout)
    - queries `animalTotals`, `businessTotals`, `businessBySection`
    - a "કુલ (અંગત સિવાય)" row in the daily PDF
  - **Business P&L** = FARM + ANIMAL + GENERAL + FAMILY advances. PERSONAL is excluded.
  - Docs 03/05/06/15 and CLAUDE.md updated.
  - **Results:** build green. Unit tests 67 run, 0 failed. New DAO test compiles; it needs a device to run.
  - The Home card for the business P&L is specified in doc 05; it will be built with the Home screen.
- **2026-11-04 (Claude, integration):** Wave 2 compiled for the first time (51 errors fixed, all in backup/report). Wrote `LendScreen`, integrated all modules, added widget/receiver manifest entries and `tools/*-check.bat`. Build + unit tests green.
- **2026-11-03 (Claude):**
  - Design: v1 multi-colour, then two-tone green, then 5 themes + QA fixes.
  - Wrote the handover docs 00–17.
  - Wave 1 agents started, then paused by a usage limit and resumed.
  - Repo initialised and pushed. Added `AGENTS.md` and this board.

## Wave 2 integration (done 2026-11-04)
- The agents never wrote their INTEGRATION.md files; the integrator wired them from the code:
  - `di/AppContainer`: one `DataRepositories` (widget refresher listener), WhatsApp template store, backup scheduler/status.
  - `VrajApp.onCreate`: installs it into `NotifyGraph`, `AssetsModule`, `ScreensDepsProvider` (with real backup status + "backup now"); creates notification channels; re-arms alarms and the 23:45 backup.
  - `ui/app/VkRoot`: routes `home`, `entry`, `shopping` (deep link `vraj://shopping`), `assets`, `lend`.
  - Manifest: all receivers, boot re-arm, 3 widget providers (`res/xml/widget_*.xml`), `vraj://` VIEW filter, `singleTop`.
- Known gaps: `MainActivity` does not forward `onNewIntent` deep links to the NavController yet; `vraj://reminders` and `vraj://panchang/…` have no destination until those screens exist.

## Next steps (in order)
1. Run on the S22 or an emulator (`tools/device-check.bat`), click through Entry, Shopping, Assets, Lend.
2. Build the missing screens: Home (05, including the new "કુલ નફો-નુકસાન" and પશુપાલન cards), Reminders (13), Farms (07), Family (08), Personal (09), Panchang calendar + settings (10), Settings/Master + backup settings + Reports (04, 15), Profiles/Help (14).
3. Hook `onNewIntent` deep links; switch `PanchangSource` to `PanchangAlertEngine`.
4. QA (16).

## Wave 2 integration contract (original)
Each Wave 2 agent owns only its own packages. It exposes a `NavGraphBuilder.xxxGraph(navController)` and writes its wiring needs to an `INTEGRATION.md` inside its package:
- `ui/assets/INTEGRATION.md`
- `ui/home/INTEGRATION.md`
- `notify/INTEGRATION.md`
- `backup/INTEGRATION.md`

The **lead integrator** is the only one who edits `ui/app/VkRoot.kt` (NavHost), `di/AppContainer.kt` and `AndroidManifest.xml`. If you take over mid-way, finish the integration from those INTEGRATION.md files.

## NEXT TASK FOR ANY AI (updated 2026-11-04 after owner feedback round 2)
1. **Ship updates with one command:** commit, then `powershell -ExecutionPolicy Bypass -File tools\release.ps1 -Notes "..."`. Never change the release key; phones can only update in place when every APK is signed by `%USERPROFILE%\.vrajkheti\vrajkheti-release.jks`. **Back that folder up** (USB / password manager). If it is lost, every phone must uninstall and restore from a JSON backup.
2. **First install of the release build:** the earlier `v0.1.0-beta` was debug-signed, so the owner must uninstall it once before installing v0.2.x (no JSON backup existed in that beta).
3. **Real-phone checks (S22 / Pixel 10 Pro):** Drive picker with a signed-in Google account (does nightly overwrite keep working after reboot?), WhatsApp share of the .json, Gmail attachment, "Open with" restore from WhatsApp, the update dialog end to end (allow-source screen, system confirm), camera, biometric, widgets, exact alarms.
4. **Nice to have:** CI release via GitHub Actions (needs the keystore as a repo secret — owner decision), JaCoCo coverage, screenshot tests (android/skills testing-setup steps 8 and 13), and the shopping icon still reads a little like a lock.

> Emulator note: `swiftshader_indirect` crashes on this PC (exit 139). Use `emulator -avd Medium_Phone_API_36.1 -gpu host`. The PIN pad ignores `adb input text`, so tap its keys instead. `connectedDebugAndroidTest` uninstalls the app afterwards, so set up the PIN again.
