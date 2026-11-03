# AGENTS.md — start here (Codex, Claude Code, Cursor, Gemini, Copilot or any AI coding agent)

This repo is **Vraj Kheti Hisab (વ્રજ ખેતી હિસાબ)**: an offline-first Gujarati farm and personal accounting Android app (Kotlin + Jetpack Compose). Several AI tools may work on it one after another. To keep work continuous, every agent follows the same protocol.

## 1. Read in this order (about 15 minutes)
1. **`HANDOVER.md`**: the live status board. It says what is done, what is in progress, and the exact next task. **Always read it first.**
2. `CLAUDE.md`: project rules (tool-neutral despite the name).
3. `handover/00_README.md`: index of the 17 implementation docs, build order, and module ownership.
4. The handover doc for the module you are about to build (`handover/02` … `16`).
5. `handover/01_Design_System.md` + `handover/17_Design_QA_Audit.md` before touching any UI.

## 2. Non-negotiable rules (short form; the full list is in CLAUDE.md)
- **Money**
  - Store money as `Long` paise. PERSONAL entries never appear in farm P&L.
  - Profit-per-Vigha is 0 when Vigha is 0.
  - APMC: `TotalMann = Mann + ExtraKg/20.0`.
- **UI**
  - 5 themes: green, mati, akash, kesar, night. Only colour tokens differ between them.
  - Screens use only the `Vk*` components from `ui/components`. Never hard-code colours.
  - Buttons, chips and tabs are always one line. No fixed screen heights: every screen scrolls.
  - All user-facing text is Gujarati (Noto Sans Gujarati).
- **WhatsApp** only opens with a pre-filled message. Never send automatically.
- **Panchang guidance text** always says "માન્યતા પ્રમાણે" (as per belief) and is user-editable.
- **Never commit secrets:** keystores, the backup API key, `local.properties`.

## 3. Build environment (Windows)
- **JDK:** `JAVA_HOME="C:\Program Files\Android\Android Studio\jbr"` (there is no java on PATH).
- **SDK:** `C:\Users\<you>\AppData\Local\Android\Sdk` goes in `local.properties` (git-ignored; create it locally with `sdk.dir=...`).
- **Commands:** `./gradlew assembleDebug`, `./gradlew testDebugUnitTest`, `./gradlew connectedDebugAndroidTest`.
- **Build output** goes to `%LOCALAPPDATA%\VrajKhetiBuild\` (outside OneDrive) via the root `build.gradle.kts`.

## 4. Design sources
- The live canvas (claude.ai, owner-only) is https://claude.ai/artifact/PVTaG7qVWVFJ7LSDoFvgnR.
- If you can't open it, use the offline copies:
  - `design/canvas/*.dc.html`: one file per screen. Read the markup and Gujarati copy; images point to `/_blob/…` ids that only resolve on the canvas.
  - `design/vk-theme.css`: the canonical tokens and component styles for all 5 themes.
  - `design/icons/*.svg` (green) and `design/themes/<theme>/*.svg`: icons and logo.
  - `design/gen-icons.js` + `design/gen-themes.js`: icon generators, the palette source.

## 5. Handover protocol (mandatory for every AI session)
1. **Before you start:** pull, read `HANDOVER.md`, and claim a task by setting its status to `IN PROGRESS (<tool name>, <date>)` in the board.
2. **While you work:** keep each change scoped to the files your module owns (see the module table in `handover/00_README.md`).
3. **When you finish a task, or before your session or usage limit ends** (do this early, around 90–95% of your budget, not at 100%):
   - Make sure the build still compiles, or write down exactly what's broken.
   - Update `HANDOVER.md`: status, what changed, test results, the next concrete step, and open questions.
   - Commit with a clear message and push.
4. **Never leave uncommitted half-work.** If you must stop mid-task, commit it on a branch named `wip/<module>-<tool>` and say so in `HANDOVER.md`.
