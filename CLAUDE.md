# Vraj Kheti Hisab (વ્રજ ખેતી હિસાબ)

Offline-first Gujarati farm and personal accounting app for Android, built for the Samsung Galaxy S22.
- Owner: Girwansinh Vala, Thorala, Ta. Mahuva, Dist. Bhavnagar.
- Design and development: Jaldip Vala. This credit appears on the Help screen only.

## Source of truth (read before working)
1. **`handover/`**: start with `handover/00_README.md`. It has one implementation doc per feature (02–16) and defines the build order and agent ownership.
2. **Visuals**: two-tone Vraj Green Material 3, specified in `handover/01_Design_System.md`. This overrides any "dark glass / multi-colour" wording in older files.
3. Design canvas "Vraj Kheti Hisab — UI/UX": https://claude.ai/artifact/PVTaG7qVWVFJ7LSDoFvgnR
4. Root specs (background): `Architecture.md`, `Modules_Specification.md`, `Dashboard_Specification.md`, `Development_Roadmap.md`, `Testing_Suite.md`, `New_Features_Specification.md`.
5. Icon and logo sources: `design/icons/*.svg`, `design/logo/logo-mark.svg`.

## Tech stack
- Kotlin 2.x, Jetpack Compose Material 3 (dynamic colour **off**)
- Room (KSP), WorkManager + AlarmManager, Glance widgets
- SecurityCrypto, Biometric, OkHttp, Coil
- minSdk 29, targetSdk 35. Package: `com.vraj.khetihisab`.

There is no Firebase or Supabase. All data lives in local Room, plus a nightly upload to the owner's GoDaddy PHP endpoint. The database has **13 entities** (see handover/03).

## Non-negotiable rules
- There are 5 account sections: FARM, ANIMAL (પશુપાલન, animal husbandry: cows, buffaloes), FAMILY (farm managers' salary/ઉપાડ), GENERAL, PERSONAL.
  - The **business P&L** (`businessTotals`) counts every section except PERSONAL. Expenses include FAMILY advances.
  - The per-farm P&L and the Vigha scorecard use FARM only.
  - Personal (`PERSONAL`) money never enters any P&L. Add a DAO test for every new P&L query.
- Profit-per-Vigha returns 0 when Vigha is 0. Money is stored as `Long` paise.
- APMC: `TotalMann = Mann + ExtraKg / 20.0`, and `TotalAmount = TotalMann * RatePerMann`.
- There are 5 user-selectable themes (green, mati, akash, kesar, night). Only colour tokens differ between them (handover/01 §7). Buttons, chips and tabs are always single-line (handover/01 §8, doc 17).
- Each theme uses one hue in two tones:
  - Brand gradient `#2E7D5B → #174A37` on neutral surfaces.
  - Gold `#9A6B12` only for Panchang and festival content.
  - Red only for the M3 `error` role.
  - Screens use only the `Vk*` components (handover/01 §6). Never hard-code colours in screens.
- Sizes: body ≥ 18sp, headers 22sp, currency ≥ 26sp bold, touch targets ≥ 60dp, cards 16dp, dialogs 28dp.
- All user-facing copy is in Gujarati (`values/strings.xml`). Font: Noto Sans Gujarati. The wordmark alone uses Baloo Bhai 2.
- Icons: the `ic_vk_*` two-tone green vectors, drawn with `Image()` and never tinted.
- WhatsApp: only open it with the message pre-filled (`WhatsAppLauncher`, handover/12). Never send automatically.
- Panchang guidance text is framed "માન્યતા પ્રમાણે" (as per belief) and is user-editable (handover/10).
- Photos (bills, people, borrowers, reminders): compress to WebP and store under `filesDir/<folder>/`. Include them in the nightly backup.

## Module ownership (subagents in `.claude/agents/`)
| Agent | Handover docs |
|---|---|
| android-foundation | 02 foundation, theme, Panchang engine · 04 security & settings · 10 festival rules engine |
| room-database | 03 all 13 entities, DAOs, seeding |
| compose-screens | 05 Home · 06 Entry/APMC · 07 Farms · 08 Family · 09 Personal · 10 dialog/calendar/settings UI · 14 Profiles/Help |
| assets-shopping | 11 shopping, assets, lending · 12 WhatsApp |
| reminders-widgets | 13 reminders, notifications, widgets · 10 alert scheduling |
| backup-reports | 15 PDF reports and nightly backup |
| qa-tester | 16 test matrix and device checks via mobile-mcp |

Build order: 02 → 03 first. Then the Wave 2 docs in parallel, starting with 12 (WhatsAppLauncher). The QA run (16) comes last.

## Commands
- Build: `./gradlew assembleDebug`
- Unit tests: `./gradlew testDebugUnitTest`
- Instrumented tests: `./gradlew connectedDebugAndroidTest`
- Install on the device: `adb install -r "%LOCALAPPDATA%\VrajKhetiBuild\app\outputs\apk\debug\app-debug.apk"`
