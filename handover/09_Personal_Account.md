# 09 · Isolated Personal / Home Account (અંગત / ઘર હિસાબ)

**Owning agent:** compose-screens · **Canvas:** 06 Personal Account · **Depends on:** 03

## Purpose
A completely separate ledger for the owner's personal income and household spending: groceries, education, festivals, medicine and rental income. It must **never** affect farm profit and loss.

## User stories
- I want to see this month's personal income vs expense.
- I want to see where my home money went, by category.
- I want to be sure that nothing here changes my farm profit.

## Screen (PersonalScreen) — artboard 06
1. `VkTopHeader("અંગત / ઘર હિસાબ", onBack)` with a month chip.
2. An info `VkCard(tone = Primary)` with `ic_vk_lock` (28dp) and the text "આ ખાતું ખેતીના નફા-નુકસાનથી સંપૂર્ણ અલગ છે."
3. Two stat cards side by side:
   - આવક · {month} (primary)
   - ખર્ચ · {month} (onSurface)
   - Under them, a net line "બચત: ₹13,700", in primary or error.
4. `VkCard` "ખર્ચ ક્યાં થયો?" with category rows. Each row has the name, the amount and a 10dp bar (primary on surfaceContainer, width relative to the largest category). The top 6 are shown, then "અન્ય".
5. `VkSectionTitle("તાજેતરની એન્ટ્રી")` and the entry list (`VkListRow`, with bill chip and pending badge).
6. Personal udhaar: if there are pending personal rows, show a small `VkCard(tone = Error)` with personal દેવાના and લેવાના totals. This is personal only; Home excludes it.
7. `VkBottomDock` scoped to PERSONAL: Entry opens with section = PERSONAL already chosen.

## Data
- `TxnDao.personalTotals(from, to)`.
- `personalByCategory(from, to)`.
- `recent` filtered `section = 'PERSONAL'`. Add a DAO query `personalRecent(limit)`.
- `pendingList()` filtered PERSONAL.

## Business logic
- Month range: the 1st to the last day of the selected month, IST.
- Net = income − expense.
- Category bars: `width = total / maxTotal`. Categories after the 6th are summed into "અન્ય".

## Edge cases
- An empty month shows an empty-state illustration (leaf divider + "આ મહિને કોઈ એન્ટ્રી નથી") and the dock.
- A transaction moved from PERSONAL to FARM by editing disappears here and appears in the farm screens live.

## Gujarati strings
| Key | Text |
|---|---|
| `personal_title` | અંગત / ઘર હિસાબ |
| `personal_isolated` | આ ખાતું ખેતીના નફા-નુકસાનથી સંપૂર્ણ અલગ છે. |
| `income_month` | આવક · %s |
| `expense_month` | ખર્ચ · %s |
| `savings` | બચત: %s |
| `where_spent` | ખર્ચ ક્યાં થયો? |
| `others` | અન્ય |
| `empty_month` | આ મહિને કોઈ એન્ટ્રી નથી |

## Acceptance criteria
- [ ] Every number here comes only from `section = PERSONAL`.
- [ ] Adding a personal expense leaves the farm P&L and the Home farm card unchanged. This is covered by a test.
- [ ] The category breakdown sums to the expense total.

## Tests
- `PersonalViewModelTest`: the "અન્ય" bucket and net computation.
- DAO isolation test (doc 03 `personalExcludedFromFarmPnl`).
