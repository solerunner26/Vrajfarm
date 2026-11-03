# 08 · Family A & B Advance Ledger (ઉપાડ ખાતું) + WhatsApp Receipt

**Owning agent:** compose-screens (UI) · uses `WhatsAppLauncher` from assets-shopping (doc 12) · **Canvas:** 05 Family A/B Advances · **Depends on:** 03, 12

## Purpose
Track the cash advances (ઉપાડ) given to the two working families, Family A and Family B, by month and by year. Each family gets a Gujarati statement on WhatsApp with one tap.

## User stories
- I give ₹5,000 to Family A and record it as an advance in 3 taps (doc 06, section પરિવાર).
- I can see this month's and this year's totals for A and B separately.
- I tap "WhatsApp પાવતી મોકલો" and WhatsApp opens with the statement already typed to that family's number.

## Screen (FamilyScreen) — artboard 05
1. `VkTopHeader("ઉપાડ ખાતું", onBack)`.
2. `VkSegmented` with **પરિવાર A** and **પરિવાર B**. The selection is remembered in `SavedStateHandle`.
3. Hero `VkCard(tone = Primary)` containing:
   - an avatar circle (the initial, or a photo if one is set later)
   - the display name and phone, or "નંબર ઉમેરો" if there is none (opens FamilySettings)
   - two stat tiles: "{month} ઉપાડ" and "વર્ષ {yyyy} કુલ" (26sp)
   - a month picker chip "નવેમ્બર 2026 ▾" that opens a month-year selector (12 chips plus year arrows)
4. `VkSectionTitle("આ મહિનાનો ઉપાડ")`, then a `VkCard` list of entries. Each row shows the note or category, the date with tithi, and the amount in onSurface. A swipe or long-press gives edit/delete, with a confirm.
5. "WhatsApp મેસેજ પૂર્વાવલોકન": a `VkCard` holding a chat-bubble preview (secondaryContainer bubble, onSecondaryContainer text, 16/16/4/16 radius). The text is exactly what will be sent.
6. `VkPrimaryButton("WhatsApp પાવતી મોકલો", icon = ic_vk_message)`.
7. A `VkTonalButton("+ ઉપાડ ઉમેરો")` opens Entry pre-set to FAMILY + this family.

## Data
- `TxnDao.familyAdvanceTotal(familyId, monthStart, monthEnd)`.
- The year total uses the calendar year Jan–Dec. This is configurable later; also show a VS-year toggle "વિ.સં. વર્ષ" that uses Kartik sud 1 → day before the next Kartik sud 1, from doc 02.
- `TxnDao.familyEntries(familyId, from, to)`.
- `FamilyEntity.whatsappPhone`.

## Business logic
Receipt template (`share/Templates.familyStatement`):
```
નમસ્તે {displayName},
{monthGu} {yyyy} નો ઉપાડ: {monthTotal}
{entries: "• dd-MM — ₹x (note)" one per line, max 10, then "…અને N વધુ"}
વર્ષ {yyyy} કુલ: {yearTotal}
— {ownerName}, વ્રજ ખેતી હિસાબ, થોરાળા
```
- `ownerName` comes from the OWNER profile.
- Amounts use Indian grouping.
- Send via `WhatsAppLauncher.open(phone = family.whatsappPhone, text)` (doc 12).
  - If the phone is missing, show a dialog "WhatsApp નંબર ઉમેરો" with an input that saves to `FamilyEntity`, then continue.
  - If WhatsApp is not installed, the launcher falls back to the share sheet.

## Edge cases
- A month with no advances: the bubble says "આ મહિને કોઈ ઉપાડ નથી". The button is still enabled, because sending a ₹0 statement is valid.
- More than 10 entries: truncate with a count.
- The phone stored with spaces or +91 is normalised by the launcher.

## Gujarati strings
| Key | Text |
|---|---|
| `family_title` | ઉપાડ ખાતું |
| `family_a` | પરિવાર A |
| `family_b` | પરિવાર B |
| `month_advance` | %s ઉપાડ |
| `year_total` | વર્ષ %d કુલ |
| `this_month_list` | આ મહિનાનો ઉપાડ |
| `wa_preview` | WhatsApp મેસેજ પૂર્વાવલોકન |
| `wa_send_receipt` | WhatsApp પાવતી મોકલો |
| `add_advance` | + ઉપાડ ઉમેરો |
| `add_number` | WhatsApp નંબર ઉમેરો |
| `no_advance_month` | આ મહિને કોઈ ઉપાડ નથી |

## Acceptance criteria
- [ ] The A and B totals are independent (the isolation test from doc 03 passes).
- [ ] The preview text equals the text sent to WhatsApp, byte for byte.
- [ ] WhatsApp opens on the correct chat with the text pre-filled. Nothing is sent without the user tapping Send in WhatsApp.

## Tests
- `FamilyStatementTemplateTest`: formatting, truncation at 10, ₹0 month.
- `FamilyViewModelTest`: switching the A/B tab switches the totals.
