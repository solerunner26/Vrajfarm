# 05 · Home Dashboard (default screen after unlock)

**Owning agent:** compose-screens · **Canvas:** 02 Home Dashboard · **Depends on:** 02, 03, 10 (Panchang day + dialog), 11 (loans), 13 (reminders)

## Purpose
Home is the owner's command centre after unlock. It shows today's date and tithi, what needs attention (udhaar, overdue tools, urgent reminders, today's Panchang alert), and the 5 account areas with their running totals. The two thumb-sized buttons add money in or out.

## User stories
- I can see today's English date, weekday and Gujarati tithi without opening anything.
- I can see how much I owe shops and how much the market owes me, and settle with 1 tap.
- If someone has kept my rotavator past its return date, Home tells me, and I can remind them on WhatsApp from there.
- On Bij, Agiyaras, Poonam, Amas or a festival, a dialog greets me once with the day's guidance.
- From anywhere on Home, I can add an expense or an income in 3 taps.

## Layout (top → bottom). Components from doc 01 §6.
1. **`VkTopHeader(hero = true)`**. Brand-gradient band with the field motif and a toran edge. Contents:
   - Row: logo 40dp, wordmark "વ્રજ ખેતી હિસાબ" (Baloo Bhai 2, 22sp, onPrimary) and greeting "નમસ્તે, ગિરવાનસિંહભાઈ" (14sp, onPrimary 85%).
   - On the right: owner avatar (`VkPhotoSlot` 44dp, opens Profiles) and a settings `IconButton` (48dp, white `ic_vk_settings` inside a 20% white circle).
   - Below: `VkPanchangCard` overlapping the band edge by 28dp. It contains:
     - moon glyph
     - `03 નવેમ્બર 2026 · મંગળવાર` (18sp/700)
     - `આસો વદ નોમ · વિ.સં. 2082` (16sp, tertiary)
     - a backup pill `VkStatusBadge` ("બેકઅપ OK" / "બેકઅપ બાકી" / "બેકઅપ નિષ્ફળ") on the right
     - if an upcoming special day falls within the next 3 days, a trailing chip "પરમ દિવસે અગિયારસ"
2. **Alerts**. Show only the ones that apply, in this priority order:
   1. `VkCard(tone = Error)` "ઉધાર બાકી". Two stat tiles, દેવાના બાકી and લેવાના બાકી, a count chip, and `VkTonalButton("ચૂકતે કરો")` that opens the `PendingUdhaarSheet` (a list with a Settle button per row).
   2. `VkCard(tone = Neutral, border = error)` for each overdue loan, at most 2 (then "વધુ N"). Shows the borrower photo (56dp, rounded 12), "{asset} પરત નથી", "{borrower} · પરત {date}", `VkStatusBadge(Overdue(days))` and a WhatsApp `FilledTonalIconButton` (60dp) that calls `WhatsAppLauncher.loanReminder(loan)` (doc 12).
   3. Urgent reminders: `VkListRow` for each HIGH reminder due within 48 h, at most 2.
3. **`VkSectionTitle("વિભાગો")`** and the section cards. All are `VkCard`. Each has a 40dp icon, a 22sp title and a summary. They differ only by icon, never by colour.
   - Full-width **કુલ નફો-નુકસાન** (business P&L, added 2026-11-04): `LedgerRepository.businessTotals`. It has three stat columns, આવક / ખર્ચ / ચોખ્ખો નફો, and the caption "ખેતી + પશુપાલન + સામાન્ય + મેનેજર ઉપાડ · અંગત સિવાય". Tapping it opens a breakdown sheet from `businessBySection`.
   - Full-width **૮ ખેતર હિસાબ**: three stat columns, આવક / ખર્ચ / ચોખ્ખો નફો (net in primary, or error if negative), and a season chip.
   - Full-width **પશુપાલન** (`ic_vk_animal`): "દૂધ ₹… · ખર્ચ ₹…" plus the month net, from `animalTotals`.
   - 2×2 grid:
     - **પરિવાર A/B ઉપાડ**: "A ₹12,000 · B ₹8,500" for the current month
     - **સામાન્ય ખેતી ખર્ચ**: "લાઇટ · પાણી · CCTV" + month total
     - **અંગત / ઘર હિસાબ**: month expense
     - **ખરીદી તથા મિલકત**: "N બાકી · M ઉછીની"
4. **`VkSectionTitle("તાજેતરની એન્ટ્રી", action = "બધી જુઓ")`**. A `VkCard` holding up to 6 `VkListRow`s, each with:
   - category icon
   - title "{category} · {farm}"
   - meta "dd-MM-yyyy · tithi"
   - trailing `VkAmount`
   - if a bill photo exists, an `AssistChip("પાવતી જુઓ", leading = ic_vk_camera 18dp)` that opens `BillViewer`
5. **`VkBottomDock`**, fixed at the bottom with the `surfaceContainer` background and a top hairline:
   - `VkTonalButton("− ખર્ચ / ઉપાડ લખો")` → `Entry(kind = EXPENSE)`
   - `VkPrimaryButton("+ આવક / વેચાણ લખો")` → `Entry(kind = INCOME)`

   Both are 64dp tall and equal width. The content scrolls with 96dp bottom padding.

## Data (HomeViewModel)
```kotlin
data class HomeUiState(
  val panchang: PanchangDay, val upcoming: SpecialDay?, val backup: BackupStatus,
  val udhaar: UdhaarSummary, val overdue: List<LoanWithAsset>, val urgent: List<ReminderEntity>,
  val farm: Totals, val familyA: Long, val familyB: Long, val general: Long, val personalExpense: Long,
  val shoppingPending: Int, val assetsLent: Int, val recent: List<TxnRow>, val todayAlert: PanchangAlert?)
```
- Combine the `Flow`s from doc 03 with `combine(...)`. `stateIn(viewModelScope, WhileSubscribed(5_000), initial)`.
- Current month = `YearMonth.now(IST)`. Farm totals use the **current season**, which defaults to the active cycle's season and year. If there is none, use the financial year Apr–Mar.
- `todayAlert = PanchangAlertEngine.alertFor(today)` (doc 10), shown only if `!dismissed(today)`.

## Business logic
1. On the first composition of a day, if `todayAlert != null` and it has not been dismissed, show `VkFestivalDialog` (doc 10). Store the dismissal for `epochDay` when it is closed.
2. "ચૂકતે કરો" opens the sheet, then `TxnDao.settle(id)`. The card updates live, and disappears once the totals are 0.
3. The backup pill comes from `BackupStatusStore` (doc 15).
   - OK = the last success is within 26 h.
   - બાકી = no backup has run yet.
   - નિષ્ફળ = the last attempt failed. Tapping it opens Backup settings.
4. All amounts are formatted with `NumberFormat.getCurrencyInstance(Locale("en","IN"))`, with no decimals unless paise ≠ 0.

## Edge cases
- When nothing needs attention, hide the alert section completely. Do not show an empty card.
- Negative net profit is shown as `−₹12,500` in `error`.
- With very long farm names (> 14 chars), ellipsize to 1 line.
- Font scale 1.3: cards grow vertically, nothing clips, and the dock buttons wrap to 2 lines.

## Gujarati strings
| Key | Text |
|---|---|
| `home_greeting` | નમસ્તે, %s |
| `udhaar_title` | ઉધાર બાકી |
| `udhaar_payable` | દેવાના બાકી |
| `udhaar_receivable` | લેવાના બાકી |
| `udhaar_settle` | ચૂકતે કરો |
| `loan_not_returned` | %s પરત નથી |
| `days_late` | %d દિવસ મોડું |
| `sec_farms` | ૮ ખેતર હિસાબ |
| `sec_family` | પરિવાર A/B ઉપાડ |
| `sec_general` | સામાન્ય ખેતી ખર્ચ |
| `sec_personal` | અંગત / ઘર હિસાબ |
| `sec_shop` | ખરીદી તથા મિલકત |
| `recent` | તાજેતરની એન્ટ્રી |
| `see_all` | બધી જુઓ |
| `view_bill` | પાવતી જુઓ |
| `dock_expense` | − ખર્ચ / ઉપાડ લખો |
| `dock_income` | + આવક / વેચાણ લખો |
| `backup_ok` | બેકઅપ OK |
| `backup_pending` | બેકઅપ બાકી |
| `backup_failed` | બેકઅપ નિષ્ફળ |

## Acceptance criteria
- [ ] Home is the start destination after unlock, and the first frame renders in < 500 ms on an S22 with 5 000 transactions.
- [ ] Every number updates live after an entry is saved, with no manual refresh.
- [ ] Every card navigates to its screen. The dock is always visible.
- [ ] The Panchang dialog shows at most once per day.
- [ ] The visual output matches artboard 02: single hue, M3 controls.

## Tests
- `HomeViewModelTest` (fake repos): udhaar is hidden when 0; the overdue list is capped at 2; personal expense is not in the farm totals.
- Compose UI test: tap the dock income button → Entry opens with Income selected.
