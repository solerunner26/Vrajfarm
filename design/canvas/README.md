# Offline copy of the design canvas

These are the source files of the Design canvas "Vraj Kheti Hisab — UI/UX" (https://claude.ai/artifact/PVTaG7qVWVFJ7LSDoFvgnR). Each `*.dc.html` is one artboard (screen):
- the full markup
- the Gujarati copy
- the interaction logic, in the `<script type="text/x-dc">` block

`canvas.json` holds the board layout and titles.

**How to use them without the canvas**
- Use them as the exact reference for layout, wording and states when implementing Compose screens. Map the CSS classes (`vk-card`, `vk-btn filled`, `vk-seg`, `vk-chip`, …) to the `Vk*` Compose components listed in `handover/01_Design_System.md` §6.
- `/_blob/...` URLs (icons, logo, `vk-theme.css`, `vk-icons.js`) only resolve inside the canvas. Local equivalents:
  - `design/vk-theme.css`
  - `design/vk-icons.js` (the theme → blob id map)
  - `design/themes/<theme>/<icon>.svg`
- `{{...}}` holes are filled by the `renderVals()` method in each file's script. That method also holds the sample data and the formulas (for example the APMC Mann calculation in `Entry.dc.html`).

| File | Screen |
|---|---|
| Lock | 01 PIN lock |
| Main | 02 Home dashboard |
| Entry | 03 3-tap entry + APMC Mann |
| Farms | 04 8 farms P&L + Vigha scorecard |
| Family | 05 Family A/B advances |
| Personal | 06 Personal account |
| PanchangAlert | 07 Panchang alert dialog |
| PanchangCalendar | 08 Panchang calendar |
| Assets | 09 Assets & lending |
| Lend | 10 Lend asset (borrower photo + WhatsApp) |
| Shopping | 11 Shopping list |
| Reminders | 12 Reminders |
| Widgets | 13 Home-screen widgets |
| Profiles | 14 Owner & managers |
| Settings | 15 Settings (theme picker, Panchang, backup) |
| Help | 16 Help & About |
| Brand / Themes | Brand board / Home in all 5 themes |
