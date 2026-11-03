# HANDOVER — live status board

> Every AI or human working on this repo updates this file **before stopping** (see `AGENTS.md` §5).
> Last updated: **2026-11-03 by Claude Code (Opus)**

## Snapshot
- **Phase:** design complete. Android build is in Wave 1 (foundation + database), **in progress, not yet verified**.
- **Design:**
  - 16 screens + brand + 5-theme board, offline copies in `design/canvas/`
  - 17 handover docs in `handover/`
  - Visual QA done on Home, Entry, Settings (green + night), Lend and the Panchang dialog. See doc 17.
- **Code:** a Gradle project and `app/` exist and are partially written by two Claude sub-agents. **The build has not been run successfully end-to-end yet.** Treat the code as a draft until the checks in the next section pass.

## Before continuing: verify Wave 1
1. Create `local.properties` with `sdk.dir=C\:\\Users\\<you>\\AppData\\Local\\Android\\Sdk`.
2. Run, with `JAVA_HOME` set to Android Studio's `jbr`:
   - `./gradlew assembleDebug`
   - `./gradlew testDebugUnitTest`
3. If it fails, fix it within the owning module (table below) and record the result here.

## Task board

| # | Task (handover doc) | Owner agent / files | Status | Notes / next step |
|---|---|---|---|---|
| 1 | Foundation: Gradle, 5 themes, Vk components, icons as ImageVectors, Panchang engine, PIN/biometric lock (02, 04) | android-foundation: build files, `ui/theme`, `ui/components`, `ui/decor`, `panchang`, `security` | **IN PROGRESS (Claude, 2026-11-03)**: partial | Was writing the SVG→ImageVector converter, `ic_vk_*` drawables and the adaptive launcher icon. Next: finish, then `assembleDebug` + Panchang unit tests against the doc 02 vectors. |
| 2 | Database: 13 Room entities, DAOs, seeding, repositories (03) | room-database: `app/src/**/data/**` only | **IN PROGRESS (Claude, 2026-11-03)**: partial (about 38 files written) | Next: compile against the real project, `testDebugUnitTest`, and make sure the instrumented DAO tests compile. |
| 3 | WhatsAppLauncher (12) | assets-shopping | TODO | Do first in Wave 2. Other modules depend on it. |
| 4 | Home dashboard (05) | compose-screens | TODO | Canvas: `design/canvas/Main.dc.html` |
| 5 | 3-tap entry, APMC, bill photo, voice (06) | compose-screens | TODO | `Entry.dc.html` |
| 6 | Farms P&L + Vigha scorecard (07) | compose-screens | TODO | `Farms.dc.html` |
| 7 | Family advances + WhatsApp receipt (08) | compose-screens | TODO | `Family.dc.html` |
| 8 | Personal account (09) | compose-screens | TODO | `Personal.dc.html` |
| 9 | Panchang alerts, calendar, festival rules (10) | foundation (rules) + compose-screens (UI) + reminders-widgets (scheduling) | TODO | `PanchangAlert` / `PanchangCalendar.dc.html` |
| 10 | Shopping, assets, lending, borrower photo, history (11) | assets-shopping | TODO | `Shopping`, `Assets`, `Lend.dc.html` |
| 11 | Reminders, notifications, Glance widgets (13) | reminders-widgets | TODO | `Reminders`, `Widgets.dc.html` |
| 12 | Profiles, Help & About (14) | compose-screens | TODO | The credit "Design and Developed by Jaldip Vala" appears ONLY in Help |
| 13 | PDF reports + 11:45 PM backup + `backup_receiver.php` (15) | backup-reports | TODO | Needs the owner's GoDaddy backup URL (still open) |
| 14 | QA matrix, device checks (16, 17) | qa-tester | TODO | Last. Use the mobile-mcp server from `.mcp.json` if available |

## Open questions for the owner (Jaldip)
1. Is the festival list in doc 10 §C complete? Are any local fairs missing (Mahuva/Thorala, Swaminarayan days)?
2. Are the default Panchang guidance texts OK (Agiyaras: avoid harvesting and rice; Amas: no new sowing or purchases)?
3. Should Bij alerts be Sud Bij only, or Vad Bij too?
4. Which printed jantri is the reference for checking dates?
5. Should the family-advance year be Jan–Dec or the Vikram Samvat year?
6. What is the GoDaddy backup URL / domain?

## Change log
- **2026-11-03 (Claude):**
  - Design: v1 multi-colour, then two-tone green, then 5 themes + QA fixes.
  - Wrote the handover docs 00–17.
  - Wave 1 agents started, then paused by a usage limit and resumed.
  - Repo initialised and pushed. Added `AGENTS.md` and this board.
