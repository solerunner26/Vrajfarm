# 11 · Shopping List, Asset Inventory & Lending (with Borrower Photo)

**Owning agent:** assets-shopping · **Canvas:** 09 Assets & Lending, 10 Lend Asset, 11 Shopping List · **Depends on:** 03, 12

## Purpose
1. A dated shopping list in two tabs, ખેતી માટે (Farm) and અંગત / ઘર (Personal).
2. When a bought item is a physical tool, the app offers to add it to the asset inventory.
3. Track who has borrowed which asset. A **photo of the borrower** is taken at handover. The app flags overdue returns, and a searchable history answers "who took the tractor on 23-11-2025?".

## User stories
- I add "DAP ખાતર 4 થેલી" to the farm list by typing or speaking, and tick it off when bought.
- When I tick "દવા છાંટવાનો પંપ", the app asks "મિલકતમાં ઉમેરવું છે?". If I say yes, it becomes an asset.
- When Rameshbhai takes the rotavator, I take his photo, enter his number and pick the return date (3 days). The app reminds me when it is overdue, and I can send him a WhatsApp reminder.
- A year later I search "ટ્રેક્ટર" or "23-11-2025" and see who took it, with their photo.

## Screens

### ShoppingScreen (artboard 11)
1. `VkTopHeader("ખરીદી યાદી")`, with a trailing assets `IconButton`.
2. `VkSegmented` with **ખેતી માટે** and **અંગત / ઘર**.
3. Add row: `OutlinedTextField("નવી વસ્તુ ઉમેરો…")` + mic `FilledTonalIconButton` (voice from doc 06) + an add button.
   - Optional fields in an expandable "વધુ" section: date (default today), farm (Farm tab only), estimated price, and the checkbox "આ એક સાધન છે" (isPhysicalTool). The checkbox is auto-ticked if the name matches the tool keyword list.
4. Items grouped by date ("આજે · 03-11-2026", "01-11-2026"), each group in a `VkCard`.
   - Each row is a ≥ 64dp `VkListRow` with an M3 `Checkbox` (36dp touch area), the name, meta "ખેતર 1 · કપાસ" and the price.
   - Checking an item strikes it through, then removes it from the active list after 1.5 s, with an undo snackbar.
5. Footer info row: "દર રવિવારે સવારે 8 વાગ્યે બાકી ખરીદીની યાદી" (weekly reminder, see below).
6. **Convert sheet** (`ModalBottomSheet`). It appears when the bought item `isPhysicalTool`:
   - text "મિલકતમાં ઉમેરવું છે?" and "{name} એક સાધન છે"
   - fields: name, price (pre-filled), purchase date
   - buttons "ના" (outlined) and "હા, ઉમેરો" (primary)

   Yes → `AssetDao.insertAsset` and then `ShoppingDao.markConverted`.

### AssetsScreen (artboard 09)
1. `VkTopHeader("મિલકત અને ઉછીનું")`.
2. Search field (`SearchBar`, M3): "કોણ લઈ ગયું? નામ, તારીખ, મિલકત".
3. `VkSegmented` tabs: **ઉછીની (n)**, **બધી (n)** and **ઇતિહાસ**.
4. The ઉછીની tab shows a `VkCard` per active loan, sorted overdue first. The border is `error` if the loan is overdue, otherwise `outlineVariant`. Each card has:
   - borrower photo (76×92dp, 14dp radius; a "ફોટો નથી" warning chip if it is missing)
   - asset icon + name (18sp/800)
   - borrower name
   - "લીધું {date} · {tithi}"
   - "પરત {date}"
   - `VkStatusBadge`: "N દિવસ મોડું" (error), "N દિવસ બાકી", or "આજે પરત"
   - "છેલ્લે યાદ અપાવ્યું: {date}" if the borrower has been reminded
   - actions: `VkPrimaryButton("યાદ અપાવો", ic_vk_message)` → WhatsAppLauncher (doc 12), and `VkOutlinedButton("પરત મળ્યું ✓")`, which asks for confirmation, then `markReturned` and moves the loan to history
   - a call `IconButton` (`ACTION_DIAL`)
5. The બધી tab is a 2-column grid of `VkCard`s. Each shows the icon, the name and a status badge ("ઘરે છે", "ઉછીનું આપેલ · {borrower}"). Tapping opens AssetDetail: photo, price, purchase date, and all loans of this asset.
6. The ઇતિહાસ tab lists returned and active loans, newest first, as rows with a 56×64dp photo, "{asset} · {borrower}", "લીધું …" and "પરત મળ્યું …". Tapping a photo opens it full screen.
7. Fixed bottom `VkPrimaryButton("+ મિલકત ઉછીની આપો", ic_vk_lend)` → LendAssetScreen.

### LendAssetScreen (artboard 10): one scrolling form, 4 numbered steps
1. **"લઈ જનારનો ફોટો"**: a large `VkPhotoSlot` (full width, 220dp, 20dp radius, dashed outline). Tapping it opens the camera. Once captured, the slot shows the photo with a timestamp overlay "03-11-2026 · 10:42 AM" and a "ફરી પાડો" chip.
2. **"કોણ લઈ જાય છે?"**:
   - name `OutlinedTextField`
   - WhatsApp number `OutlinedTextField` (keyboard Phone) + a contact-picker button (`ActivityResultContracts.PickContact`, then read the number via `ContactsContract.CommonDataKinds.Phone`)
   - suggestions from previous borrowers, as autocomplete chips
3. **"કઈ મિલકત?"**: `VkChip`s of assets that are not currently lent, plus "+ નવી મિલકત".
4. **"તારીખ"**:
   - a read-only handover row (today + tithi; the date is changeable via the date picker)
   - return chips: 1 / 3 / 7 / 15 દિવસ / "તારીખ પસંદ કરો"
   - a checkbox "પરત તારીખે મને નોટિફિકેશન આપો" (default on)
5. WhatsApp preview bubble (the template from doc 12).
6. Buttons: `VkOutlinedButton("સાચવો")` and `VkPrimaryButton("સાચવો + મોકલો")`. The second saves, then opens WhatsApp. Both return to Assets.

## Business logic
- **Tool keyword list**, for auto-ticking isPhysicalTool: પંપ, મોટર, ટ્રેક્ટર, રોટાવેટર, થ્રેશર, ટ્રોલી, કોદાળી, પાવડો, હળ, સ્પ્રેયર, ડ્રીપ, પાઇપ, કટર, ઓજાર, સાધન. Matching is case and diacritic tolerant (`contains`).
- **Borrower photo** (`media/PersonPhotoCapture`):
  - `TakePicture` → decode → scale the longest side to 1024 → draw the timestamp text (white on a 50% black strip, Noto Sans Gujarati) → WebP quality 75, ≤ 120 KB → `filesDir/borrower_photos/<uuid>.webp`.
  - The photo is **recommended, not mandatory**. If the user saves without one, show a confirm "ફોટો વગર સાચવવું છે?".
- **Save loan**: `AssetLoanEntity(handedOverDate, handedOverTithi = shortLabel(date), expectedReturnDate = date + n, notifyOnDue)`. If `notifyOnDue`, also insert a `ReminderEntity(title = "{asset} પાછું લાવવું", note = borrower, priority = HIGH, dueAt = expectedReturnDate 09:00, sourceType = "LOAN", sourceId = loanId)`.
- **Mark returned**: `markReturned` and mark the linked reminder done.
- **History search**:
  - The query is trimmed.
  - If it matches `^\d{1,2}[-/.]\d{1,2}[-/.]\d{4}$`, parse it with `dd-MM-yyyy` (lenient separators) and pass `epochDay`. Otherwise pass `epochDay = null`.
  - The DAO searches the borrower name, the asset name and the date (doc 03).
  - Debounce 250 ms.
- **Overdue** (`work/OverdueLoanWorker`, a `PeriodicWorkRequest` every 24 h, initial delay to the next 09:00 IST):
  - `overdueLoansNow(today)` → for each loan, one notification on channel `assets` ("મિલકત ચેતવણી").
  - title "{asset} પરત નથી", text "{borrower} · {n} દિવસ મોડું"
  - action "યાદ અપાવો" (opens the app, then WhatsAppLauncher; it never sends silently) and action "પરત મળ્યું"
- **Weekly shopping** (`work/WeeklyShoppingWorker`, periodic 7 days, aligned to Sunday 08:00 IST):
  - If the active count is > 0, notify "{n} વસ્તુઓ ખરીદવાની બાકી" with the first 5 names in `InboxStyle`.

## Edge cases
- Lending an asset that is already lent triggers a warning dialog.
- Deleting an asset with an active loan is blocked: "પહેલાં પરત મળ્યું કરો".
- Camera permission denied: the slot shows "ફોટો માટે કેમેરા પરવાનગી આપો", with a Settings link.
- The same borrower with different number formats is deduplicated by the normalised phone (doc 12).
- Expected return before the handover date is a validation error.

## Gujarati strings
| Key | Text |
|---|---|
| `shop_title` | ખરીદી યાદી |
| `tab_farm` | ખેતી માટે |
| `tab_personal` | અંગત / ઘર |
| `add_item_hint` | નવી વસ્તુ ઉમેરો… |
| `is_tool` | આ એક સાધન છે |
| `weekly_note` | દર રવિવારે સવારે 8 વાગ્યે બાકી ખરીદીની યાદી |
| `convert_q` | મિલકતમાં ઉમેરવું છે? |
| `convert_sub` | %s એક સાધન છે |
| `yes_add` | હા, ઉમેરો |
| `no` | ના |
| `assets_title` | મિલકત અને ઉછીનું |
| `search_who` | કોણ લઈ ગયું? નામ, તારીખ, મિલકત |
| `tab_lent` | ઉછીની (%d) |
| `tab_all` | બધી (%d) |
| `tab_history` | ઇતિહાસ |
| `taken_on` | લીધું %s |
| `return_on` | પરત %s |
| `returned_on` | પરત મળ્યું %s |
| `remind` | યાદ અપાવો |
| `mark_returned` | પરત મળ્યું ✓ |
| `at_home` | ઘરે છે |
| `lent_to` | ઉછીનું આપેલ · %s |
| `lend_title` | મિલકત ઉછીની આપો |
| `step_photo` | 1 · લઈ જનારનો ફોટો |
| `take_photo` | ફોટો પાડો |
| `retake` | ફરી પાડો |
| `step_who` | 2 · કોણ લઈ જાય છે? |
| `step_asset` | 3 · કઈ મિલકત? |
| `step_dates` | 4 · તારીખ |
| `notify_on_due` | પરત તારીખે મને નોટિફિકેશન આપો |
| `save` | સાચવો |
| `save_send` | સાચવો + મોકલો |
| `no_photo_confirm` | ફોટો વગર સાચવવું છે? |
| `already_lent` | પહેલેથી ઉછીનું આપેલ છે |
| `days_left` | %d દિવસ બાકી |
| `due_today` | આજે પરત |
| `last_reminded` | છેલ્લે યાદ અપાવ્યું: %s |
| `shop_pending_notif` | %d વસ્તુઓ ખરીદવાની બાકી |

## Acceptance criteria
- [ ] Ticking an item removes it from the active list. Undo restores it.
- [ ] Tool items prompt the conversion, and "yes" creates the asset.
- [ ] A loan can be created with a photo in ≤ 6 taps, plus typing the name.
- [ ] Overdue loans appear on Home (doc 05), in the Assets tab, and as a daily notification.
- [ ] Searching "23-11-2025" or "ટ્રેક્ટર" finds the historical loan with its photo.
- [ ] Borrower photos are ≤ 120 KB and carry the timestamp.

## Tests
- `ToolKeywordTest`
- `LoanSearchQueryParserTest`: date formats
- `AssetLoanDaoTest` (doc 03)
- `OverdueLoanWorkerTest` (`work-testing` `TestListenableWorkerBuilder`): posts N notifications
- Compose test: lend flow with a fake camera result → the card appears in the ઉછીની tab
