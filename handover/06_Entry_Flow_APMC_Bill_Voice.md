# 06 · 3-Tap Entry, APMC Mann Calculator, Bill Photo, Gujarati Voice

**Owning agent:** compose-screens · **Canvas:** 03 3-Tap Entry + APMC Mann · **Depends on:** 02, 03

## Purpose
Record any money movement in 3 taps with big buttons that work outdoors:
1. Choose the **section** (ખેતર / પરિવાર / સામાન્ય / અંગત).
2. Choose the **target** (the farm, the family or the category).
3. Enter the **amount** and payment status, then save.

Crop sales use the APMC Mann calculator. A bill can be photographed, and a note can be spoken in Gujarati.

## User stories
- I sold 30 મણ 10 kg of cotton at ₹1,600 per મણ and want the total calculated (₹48,800).
- I bought fertiliser on credit, so I mark it ઉધાર બાકી and it shows on Home.
- I photograph the bill so I can show it later.
- I speak "ડીએપી ખાતર ચાર થેલી" instead of typing.

## Screen (EntryScreen) — artboard 03
1. `VkTopHeader("નવી એન્ટ્રી", onBack)`, with a trailing mic `FilledTonalIconButton` (56dp).
2. `VkSegmented` with **+ આવક / વેચાણ** and **− ખર્ચ / ઉપાડ**. Initial value from the nav argument.
3. "ટેપ 1 · વિભાગ": 4 `VkChip` tiles (icon 28dp + label, 64dp tall, 4-column grid): ખેતર, પરિવાર, સામાન્ય, અંગત.
4. "ટેપ 2". Contents depend on the section:
   - FARM: 8 farm tiles (4×2) showing "ખેતર N" and the active crop, then a horizontal row of category chips for the selected type.
   - FAMILY: A / B tiles. The type is forced to `ADVANCE` and the label reads "ઉપાડ".
   - GENERAL: chips for લાઇટ બિલ, પાણી વ્યવસ્થા, CCTV, રિપેરિંગ.
   - PERSONAL: personal category chips.
5. The amount area has two modes:
   - **Simple** (default): a large amount display (displaySmall) + `VkNumericKeypad`.
   - **APMC Mann** (shown when FARM + INCOME + category પાક વેચાણ, or when the "મણ ગણક" switch is on):
     - `VkCard` with 3 field tiles: મણ, વધારાના કિલો, મણનો ભાવ ₹. The active tile has a primary border.
     - The shared keypad types into the active tile.
     - Live result row: "કુલ મણ 30.50" and "કુલ રકમ ₹48,800" (30sp, primary).
     - A caption "1 મણ = 20 કિલો".
6. "ટેપ 3 · ચુકવણી": `VkSegmented` with **રોકડ / ચૂકવેલ** and **ઉધાર બાકી**. For INCOME, pending means `PENDING_RECEIVABLE`. For EXPENSE, it means `PENDING_PAYABLE`. When pending, an optional "કોની પાસે / કોને?" text field appears (counterparty).
7. Two `VkOutlinedButton`s: **પાવતી ફોટો** (camera) and **બોલીને નોંધ** (voice). Captured items show as thumbnail or text chips with remove ✕.
8. Sticky `VkPrimaryButton("સાચવો · ₹48,800")`. It is disabled until the amount is > 0 and steps 1–2 are complete.

## Business logic
### APMC Mann (`domain/ApmcCalculator.kt`, pure, unit-tested)
```kotlin
object ApmcCalculator {
  const val KG_PER_MANN = 20.0
  fun totalMann(mann: Double, extraKg: Double): Double = mann + extraKg / KG_PER_MANN
  fun totalAmountPaise(mann: Double, extraKg: Double, ratePerMannPaise: Long): Long =
      (totalMann(mann, extraKg) * ratePerMannPaise).roundToLong()
}
// 30 મણ + 10 kg × ₹1,600 → 30.5 × 160000 paise = 4,880,000 paise = ₹48,800
```
- Input limits: મણ 0–9 999 (one decimal allowed), kg 0–19.99 (if ≥ 20, normalise: mann += kg/20 and kg %= 20, and show a snackbar "20 કિલો = 1 મણ ઉમેર્યું"), rate 0–99 999.
- Store `mann`, `extraKg` and `ratePerMannPaise` on the transaction for reports.

### Keypad input rules
- Max 7 integer digits and 2 decimals. Leading zeros are collapsed.
- "." is ignored when there is already one, or when decimals are not allowed.
- ⌫ removes one char. A long press clears.

### Save
```kotlin
val t = TransactionEntity(date = selectedDate /*default today, editable via date chip*/,
  tithiLabel = GujaratiPanchangHelper.shortLabel(selectedDate), section, type, amountPaise, payStatus,
  counterparty, farmId, cycleId = activeCycleFor(farmId)?.id, familyId, categoryId, note, voiceNote, billPhotoPath,
  mann, extraKg, ratePerMannPaise, createdAt = now, updatedAt = now)
txnDao.insert(t); navigate back to Home; Snackbar "એન્ટ્રી સચવાઈ ગઈ" with "પાછું લો" (undo = softDelete).
```
A date chip "આજે · 03-11-2026" in the header opens `DatePickerDialog` (M3), for back-dating entries.

### Bill photo (`media/BillPhotoHelper`)
1. `rememberLauncherForActivityResult(ActivityResultContracts.TakePicture())` with a `FileProvider` URI to `cacheDir/tmp/bill_<ts>.jpg`. Gallery alternative: `PickVisualMedia(ImageOnly)`.
2. Decode with `ImageDecoder` (API 28+), with `setTargetSize` to a longest side of **1600 px** and EXIF rotation respected.
3. Compress to WebP (`Bitmap.CompressFormat.WEBP_LOSSY`, API 30+; `WEBP` on 29). Start at quality 80 and step down by 10 while the size is > **85 KB**, to a minimum of 40. If it is still too big, scale ×0.85 and repeat.
4. Save to `filesDir/bill_photos/<txnUuid>.webp` and delete the temp file.
5. `BillViewer`: full-screen with pinch-to-zoom (`Modifier.transformable`, scale 1–5×) and double-tap to toggle 2.5×.

### Gujarati voice (`media/VoiceInput`)
```kotlin
val launcher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { r ->
  r.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)?.firstOrNull()?.let(onText) }
Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
  putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
  putExtra(RecognizerIntent.EXTRA_LANGUAGE, "gu-IN"); putExtra(RecognizerIntent.EXTRA_PROMPT, "બોલો…") }
```
- If no recognizer handles the intent (`resolveActivity == null`), show "ગુજરાતી અવાજ ઉપલબ્ધ નથી". Offline needs the Google gu-IN offline pack. A one-time tip explains how to download it.
- Optional parsing helper `VoiceAmountParser`: extracts digits or Gujarati number words (એક…સો, હજાર, લાખ) to pre-fill the amount, but **only as a suggestion chip** "₹4,000 વાપરો?". It never auto-fills silently.
- Set `LockManager.suppressNextLock = true` before launching the camera or voice intent.

## Edge cases
- Switching section after choosing a farm resets step 2.
- APMC mode: an amount typed in simple mode is replaced by the computed value, after a confirm dialog if it was non-zero.
- The camera permission is requested at first use. If permanently denied, show a dialog with a Settings link.
- Rotation and process death: keep the form in `SavedStateHandle`.

## Gujarati strings
| Key | Text |
|---|---|
| `entry_title` | નવી એન્ટ્રી |
| `tap1` | ટેપ 1 · વિભાગ |
| `tap2_farm` | ટેપ 2 · ખેતર અને પાક |
| `tap3` | ટેપ 3 · ચુકવણી |
| `apmc_title` | APMC મણ ગણક |
| `mann` | મણ |
| `extra_kg` | વધારાના કિલો |
| `rate_mann` | મણનો ભાવ ₹ |
| `total_mann` | કુલ મણ |
| `total_amount` | કુલ રકમ |
| `kg_rule` | 1 મણ = 20 કિલો |
| `paid` | રોકડ / ચૂકવેલ |
| `pending` | ઉધાર બાકી |
| `bill_photo` | પાવતી ફોટો |
| `voice_note` | બોલીને નોંધ |
| `save_amount` | સાચવો · %s |
| `saved` | એન્ટ્રી સચવાઈ ગઈ |
| `undo` | પાછું લો |
| `voice_unavailable` | ગુજરાતી અવાજ ઉપલબ્ધ નથી |

## Acceptance criteria
- [ ] A typical expense takes 3 taps + amount + save. An income takes the same.
- [ ] The APMC result matches the formula. Unit test: `30.5 × ₹1,600 = ₹48,800`.
- [ ] Bill WebP files are ≤ 85 KB and readable when zoomed.
- [ ] The voice prompt appears in Gujarati, and the recognised text lands in the note.
- [ ] Undo works within the snackbar window.

## Tests
- `FarmAccountingMathTest`: APMC cases (30+10kg@1600 = 48 800; 0 kg; kg = 20 normalisation; rate 0).
- `BillPhotoHelperTest` (instrumented, a 4000×3000 test image): output ≤ 85 KB and longest side ≤ 1600.
- `KhetHisabUiTest.threeTapExpense`: ખર્ચ → ખેતર → ખેતર 1 → ખાતર → 5400 → સાચવો → shows in Recent on Home.
