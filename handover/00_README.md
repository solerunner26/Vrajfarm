# Vraj Kheti Hisab — Claude Code Handover Pack

This pack has everything a Claude Code agent needs to build **વ્રજ ખેતી હિસાબ** (Vraj Kheti Hisab). It is an offline-first Gujarati farm and personal accounting app for Android, targeting the Samsung Galaxy S22.

- **Owner:** Girwansinh Vala, At Post Thorala, Ta. Mahuva, Dist. Bhavnagar
- **Design & development:** Jaldip Vala. This credit appears on the Help screen only.

## Sources of truth, in priority order
1. `handover/01_Design_System.md` — visuals. Two-tone "Vraj Green" Material 3. **This overrides** the dark-glass and multi-colour wording in the older root specs.
2. `handover/02…16` — what each feature does and how to build it.
3. Root specs: `Architecture.md`, `Modules_Specification.md`, `Dashboard_Specification.md`, `Development_Roadmap.md`, `Testing_Suite.md`, `New_Features_Specification.md`.
4. Design canvas: https://claude.ai/artifact/PVTaG7qVWVFJ7LSDoFvgnR (one artboard per screen).
5. Icons and logo: `design/icons/*.svg`, `design/logo/logo-mark.svg`.

## Index

| # | Doc | Owning agent | Canvas artboard |
|---|---|---|---|
| 01 | Design System | all (read-only) | Brand |
| 02 | Foundation, Theme, Panchang Engine | android-foundation | Brand |
| 03 | Database (Room) — 13 entities | room-database | — |
| 04 | Security, Lock, Master Settings | android-foundation | 01 PIN Lock, 15 Settings |
| 05 | Home Dashboard | compose-screens | 02 Home Dashboard |
| 06 | Entry Flow, APMC, Bill photo, Voice | compose-screens | 03 3-Tap Entry |
| 07 | Farms P&L, Profit-per-Vigha | compose-screens | 04 8 Farms P&L |
| 08 | Family A/B Advances + WhatsApp receipt | compose-screens | 05 Family A/B |
| 09 | Personal Account | compose-screens | 06 Personal Account |
| 10 | Panchang Alerts & Festivals (NEW) | android-foundation (rules) + reminders-widgets (scheduling) + compose-screens (dialog, calendar, settings UI) | 07 Panchang Alert Dialog, 08 Panchang Calendar, 15 Settings |
| 11 | Shopping, Assets, Lending | assets-shopping | 09 Assets, 10 Lend Asset, 11 Shopping |
| 12 | WhatsApp Integration | assets-shopping | 05 Family, 10 Lend Asset, 14 Profiles |
| 13 | Reminders, Notifications, Widgets | reminders-widgets | 12 Reminders, 13 Widgets |
| 14 | Profiles, Help & About | compose-screens | 14 Profiles, 16 Help & About |
| 15 | PDF Reports & Nightly Backup | backup-reports | 15 Settings |
| 16 | Testing & QA | qa-tester | all |
| 17 | Design QA audit: findings, fixes, text-fitting checks | compose-screens + qa-tester | all, plus "Home in all 5 themes" |

**Themes:** there are 5 user-selectable themes: green, mati, akash, kesar, and night (dark). Tokens are in `01_Design_System.md §7`, with the canonical CSS in `design/vk-theme.css`. android-foundation builds `VkTheme` + `ThemeRepository` (DataStore) in Wave 1. The theme is stored as a preference, not in a Room entity.

## Build order

1. **Wave 1, in sequence:** 02, then 03.
   - 02: Gradle, theme, the `Vk*` components, icons, `GujaratiPanchangHelper`.
   - 03: all entities, DAOs, seeding and repositories.

   Everything else depends on these two.
2. **Wave 2, in parallel.** Each agent works on its own packages:
   - 04 + 10-engine: android-foundation
   - 05, 06, 07, 08, 09, 14, 10-calendar UI: compose-screens
   - 11 + 12: assets-shopping. Build `WhatsAppLauncher` first, because 08 and 14 call it.
   - 13 + 10-alerts: reminders-widgets
   - 15: backup-reports
3. **Wave 3:** 16 qa-tester runs the full matrix and checks screenshots against the canvas.

## Package layout (`com.vraj.khetihisab`)

```
app/src/main/java/com/vraj/khetihisab/
  di/            AppContainer (manual DI; no Hilt needed)
  data/          entity/, dao/, AppDatabase.kt, repo/
  panchang/      GujaratiPanchangHelper, Astro, FestivalRules, PanchangNames
  ui/theme/      Color, Type, Shape, Theme
  ui/components/ Vk* components (doc 01 §6)
  ui/decor/      header motif, toran scallop, leaf divider, moon phase
  ui/<feature>/  Screen + ViewModel per feature
  media/         BillPhotoHelper, PersonPhotoCapture
  share/         WhatsAppLauncher
  notify/        channels, schedulers, receivers
  work/          WorkManager workers
  widget/        Glance widgets
  report/        GujaratiPdfGenerator
  backup/        NightlyBackupWorker, BackupScheduler
  security/      LockManager, PinStore
server/backup_receiver.php
```

## How to run an agent

From the project root in Claude Code:
```
Use the room-database agent to implement handover/03_Database_Room.md
```
Each agent reads `CLAUDE.md`, `handover/01_Design_System.md` and its own doc, then:
1. builds the feature,
2. runs `./gradlew assembleDebug testDebugUnitTest`,
3. reports which files it changed and the test output.

## Definition of done (every doc)
- [ ] Every acceptance-criteria box in the doc is satisfied.
- [ ] The listed tests exist and pass.
- [ ] Screens use only `Vk*` components and theme roles. There are no hard-coded colours in screens.
- [ ] All user-facing strings are in `res/values/strings.xml`, in Gujarati.
- [ ] Touch targets are ≥ 60dp and body text is ≥ 18sp.
- [ ] Personal money never appears in farm P&L.
- [ ] Nothing is ever auto-sent (WhatsApp, email) without a user tap, except the nightly backup the owner has configured.
- [ ] `./gradlew assembleDebug` succeeds and lint shows no new errors.
