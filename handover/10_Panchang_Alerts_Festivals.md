# 10 · Panchang Alerts — Bij, Agiyaras, Poonam, Amas & Festivals (NEW)

**Owning agents:**
- android-foundation: `PanchangAlertEngine` + `FestivalRules`, pure Kotlin in `panchang/`
- reminders-widgets: scheduling, notifications, boot receiver
- compose-screens: `VkFestivalDialog`, `PanchangCalendarScreen`, `PanchangAlertSettingsScreen`

**Canvas:** 07 Panchang Alert Dialog · 08 Panchang Calendar · 15 Settings (Panchang alerts) · **Depends on:** 02 (engine), 03 (prefs tables), 13 (reminders)

## Purpose
Many farmers plan work by the Hindu calendar. They avoid starting some work on **અમાસ**, prefer to start sowing or purchases on **પૂનમ**, fast on **અગિયારસ**, and treat **બીજ** (the first moon sighting) as auspicious. The app warns the owner **the evening before** and greets them **on the day** with a Material 3 dialog. The dialog shows the day's name and the work guidance, and the owner can edit that guidance. Major Gujarati festivals are included.

## User stories
- At 7 PM I get a notification: "આવતીકાલે અમાસ — દિવાળી".
- The first time I open the app on Agiyaras, a dialog shows "આજે રમા અગિયારસ" with the guidance, once.
- From the dialog I can create a reminder or open the calendar.
- In a month calendar I can see every Bij, Agiyaras, Poonam, Amas and festival, and tap a day for details.
- I can switch each alert type on or off and rewrite the guidance in my own words.

## Rules (`panchang/FestivalRules.kt`)

### A. Tithi types (from `PanchangDay`, doc 02)
| Type | Condition | Display |
|---|---|---|
| BIJ | `paksha==SUD && tithiInPaksha==2` | "બીજ" (with "ચંદ્ર દર્શન") |
| AGIYARAS | `tithiInPaksha==11` (SUD or VAD) | "{ekadashiName} અગિયારસ" |
| POONAM | `tithiIndex==15` | "{monthGu} પૂનમ" |
| AMAS | `tithiIndex==30` | "{monthGu} અમાસ" |

Vad Bij is **not** alerted by default. Only Sud Bij triggers, because that is the moon-darshan Bij farmers observe. The user can enable "વદ બીજ પણ" in Settings.

### B. Ekadashi names (Amanta month index 0=ચૈત્ર … 11=ફાગણ)
| Month | સુદ 11 | વદ 11 |
|---|---|---|
| 0 ચૈત્ર | કામદા | વરૂથિની |
| 1 વૈશાખ | મોહિની | અપરા |
| 2 જેઠ | નિર્જળા | યોગિની |
| 3 અષાઢ | દેવપોઢી | કામિકા |
| 4 શ્રાવણ | પવિત્રા | અજા |
| 5 ભાદરવો | જળઝીલણી | ઇન્દિરા |
| 6 આસો | પાશાંકુશા | રમા |
| 7 કારતક | દેવઊઠી (પ્રબોધિની) | ઉત્પત્તિ |
| 8 માગશર | મોક્ષદા | સફલા |
| 9 પોષ | પુત્રદા | ષટ્તિલા |
| 10 મહા | જયા | વિજયા |
| 11 ફાગણ | આમલકી | પાપમોચની |
| Adhik maas | પદ્મિની | પરમા |

### C. Festivals (rule = month, paksha, tithi; amanta)
| Gujarati name | Rule | Observance |
|---|---|---|
| ચૈત્રી નવરાત્રિ પ્રારંભ | ચૈત્ર સુદ 1 | sunrise |
| રામ નવમી | ચૈત્ર સુદ 9 | sunrise |
| હનુમાન જયંતી | ચૈત્ર સુદ 15 | sunrise |
| અખાત્રીજ | વૈશાખ સુદ 3 | sunrise · farm guidance override (below) |
| અષાઢી બીજ | અષાઢ સુદ 2 | sunrise |
| ગુરુ પૂર્ણિમા | અષાઢ સુદ 15 | sunrise |
| રક્ષાબંધન / બળેવ | શ્રાવણ સુદ 15 | sunrise |
| નાગ પાંચમ | શ્રાવણ વદ 5 | sunrise |
| રાંધણ છઠ | શ્રાવણ વદ 6 | sunrise |
| શીતળા સાતમ | શ્રાવણ વદ 7 | sunrise |
| જન્માષ્ટમી | શ્રાવણ વદ 8 | **midnight** (tithi at 00:00 IST) |
| ગણેશ ચતુર્થી | ભાદરવો સુદ 4 | sunrise |
| સર્વપિતૃ અમાસ | ભાદરવો વદ 15 | sunrise |
| નવરાત્રિ પ્રારંભ | આસો સુદ 1 | sunrise |
| દશેરા | આસો સુદ 10 | sunrise |
| શરદ પૂનમ | આસો સુદ 15 | sunrise |
| વાઘ બારસ | આસો વદ 12 | sunrise |
| ધનતેરસ | આસો વદ 13 | **pradosh** (sunset + 72 min) |
| કાળી ચૌદશ | આસો વદ 14 | sunrise |
| દિવાળી | આસો વદ 15 (અમાસ) | **pradosh** |
| બેસતું વર્ષ (નૂતન વર્ષ) | કારતક સુદ 1 | sunrise |
| ભાઈબીજ | કારતક સુદ 2 | sunrise |
| લાભ પાંચમ | કારતક સુદ 5 | sunrise |
| દેવઊઠી અગિયારસ / તુલસી વિવાહ | કારતક સુદ 11 | sunrise |
| દેવ દિવાળી | કારતક સુદ 15 | sunrise |
| ઉત્તરાયણ (મકરસંક્રાંતિ) | **solar, fixed 14 Jan** | civil date |
| વાસી ઉત્તરાયણ | **fixed 15 Jan** | civil date |
| વસંત પંચમી | મહા સુદ 5 | sunrise |
| મહાશિવરાત્રિ | મહા વદ 14 | **midnight** |
| હોળી (હોળિકા દહન) | ફાગણ સુદ 15 | **pradosh** |
| ધુળેટી | ફાગણ વદ 1 | sunrise |

Notes on the rules:
- `CustomFestivalEntity` rows (user-added) are evaluated the same way: either a month+paksha+tithi rule or a fixed `MM-dd`.
- Festivals never fire in an **Adhik** month. Agiyaras still fires there, using the Adhik names.
- **Observance resolution:**
  - `sunrise`: the day whose sunrise tithi matches.
  - `pradosh` / `midnight`: the civil day D on which the tithi is in force at that moment. If none qualifies, fall back to the sunrise rule.
  - **Kshaya tithi** (no sunrise of its own): assign it to the civil day during which it starts and ends, i.e. sunrise(D) < start and end < sunrise(D+1).
  - **Vriddhi** (two sunrises): use the **first** day.

### D. Combination
One day can match several rules. For example, દિવાળી + AMAS, દેવ દિવાળી + POONAM, ભાઈબીજ + BIJ, દેવઊઠી + AGIYARAS.
```kotlin
data class SpecialDay(val date: LocalDate, val day: PanchangDay, val types: Set<PanchangAlertType>,
    val festivals: List<String>, val ekadashiName: String?) {
  val titleGu: String get() = festivals.firstOrNull()
      ?: ekadashiName?.let { "$it અગિયારસ" }
      ?: when { POONAM in types -> "${day.monthGu} પૂનમ"; AMAS in types -> "${day.monthGu} અમાસ"; else -> "બીજ · ચંદ્ર દર્શન" }
}
object PanchangAlertEngine {
  fun special(date: LocalDate, custom: List<CustomFestivalEntity>): SpecialDay?
  fun specialDaysInRange(from: LocalDate, to: LocalDate, custom: List<CustomFestivalEntity>): List<SpecialDay>
  fun alertFor(date: LocalDate, prefs: Map<PanchangAlertType, PanchangAlertPrefEntity>, custom: List<CustomFestivalEntity>, showGuidance: Boolean): PanchangAlert?
}
data class PanchangAlert(val special: SpecialDay, val doLines: List<String>, val avoidLines: List<String>, val showGuidance: Boolean)
```
`alertFor` returns null when every matched type is disabled. FESTIVAL counts as a type. Guidance merges the enabled types' `doText` and `avoidText` (split on newlines, de-duplicated), plus any festival-specific override. The order is festival first, then AGIYARAS, AMAS, POONAM, BIJ.

### E. Default guidance — **"માન્યતા પ્રમાણે"**, seeded into `PanchangAlertPrefEntity` and editable
| Type | doText (કરવું) | avoidText (ટાળવું) |
|---|---|---|
| AMAS | પિતૃ તર્પણ અને દાન · ખેતર અને સાધનોની સફાઈ-દેખરેખ | નવી વાવણી શરૂ કરવી · નવું સાધન કે જમીન ખરીદવી · નવું કામ શરૂ કરવું |
| POONAM | વાવણી કે નવું કામ શરૂ કરવું · ખરીદી · પૂજા-પાઠ | — |
| AGIYARAS | ઉપવાસ · ભજન-કીર્તન · દાન | કેટલાક લોકો કાપણી / લણણી ટાળે છે · અનાજ (ચોખા) ન ખાવું |
| BIJ | ચંદ્ર દર્શન · નવા કામની શુભ શરૂઆત | — |
| FESTIVAL | પરિવાર સાથે ઉજવણી · પૂજા | — |

Festival override: અખાત્રીજ → do: "ખેતીના નવા વર્ષની શરૂઆત · હળ પૂજન · નવું કામ શુભ".

The dialog footer always says: "માન્યતા પ્રમાણે · સેટિંગ્સમાં બદલી શકો". The app presents these as beliefs, never as facts.

## UI

### VkFestivalDialog (artboard 07)
An M3 `BasicAlertDialog` with a 28dp radius and a `surfaceContainerLowest` background. From top to bottom:
1. **Band**: a 120dp `tertiaryContainer` header with `VkRangoliDots` in the corners, plus the moon-phase glyph (56dp) or a diya/festival glyph. Over the band:
   - an overline "આજે" or "આવતીકાલે" (labelLarge, onTertiaryContainer)
   - the title `titleGu` (headlineSmall/800)
   - the tithi label "આસો વદ અગિયારસ · વિ.સં. 2082"
2. **Type chips**: one `AssistChip` per matched type, e.g. "અગિયારસ" and "તહેવાર". All chips are neutral and outlined.
3. **Guidance** (only if `showGuidance`):
   - "કરવા જેવું": up to 4 bullets with a leaf glyph
   - "ટાળવા જેવું": up to 4 bullets with an outlined-circle glyph; this section is hidden if empty
   - the footer note in bodySmall
4. **Actions**, stacked full width:
   - `VkPrimaryButton("સમજાયું")`
   - `VkTonalButton("રિમાઇન્ડર બનાવો")`
   - `TextButton("કેલેન્ડર જુઓ")`

### PanchangCalendarScreen (artboard 08)
- `VkTopHeader("પંચાંગ કેલેન્ડર")` with month navigation `‹ નવેમ્બર 2026 ›` and the subtitle "આસો / કારતક · વિ.સં. 2082–83". The subtitle shows both lunar months that overlap the Gregorian month.
- Weekday header: ર સો મં બુ ગુ શુ શ (week starts Sunday).
- 7-column grid, cells ≥ 48×64dp. Each cell has:
  - the date (18sp/700)
  - the short tithi below (11sp, e.g. "નોમ" or "અમાસ")
  - a marker glyph, top right, **by shape rather than colour**:
    - Poonam = filled circle
    - Amas = hollow circle
    - Agiyaras = "૧૧" mini badge
    - Bij = crescent
    - Festival = 4-point star in tertiary
  - Today: a 2dp primary outline. Special days: a `secondaryContainer` cell fill.
- A legend row below the grid with the 5 markers.
- "આ મહિનાના ખાસ દિવસો": a list of `VkListRow`, e.g. "05 ગુરુ · રમા અગિયારસ" and "08 રવિ · દિવાળી (અમાસ)".
- Tapping a day opens a `ModalBottomSheet` with:
  - the full `tithiGu`
  - "તિથિ સમાપ્તિ: 06-11 સવારે 04:12"
  - sunrise and sunset
  - matched names and guidance
  - `VkTonalButton("રિમાઇન્ડર બનાવો")`

### PanchangAlertSettingsScreen (in Settings, artboard 15)
- 5 switches, one per type: બીજ, અગિયારસ, પૂનમ, અમાસ, તહેવાર. A sub-switch "વદ બીજ પણ" sits under Bij.
- Switch "કામની સલાહ બતાવો" (show guidance).
- Time picker "આગલી સાંજે સૂચના: 7:00 PM" (`TimePicker`, M3).
- Per type, a "સલાહ બદલો" row opens an editor with two multi-line `OutlinedTextField`s (કરવું / ટાળવું) and "મૂળ સલાહ પાછી લાવો" (reset to default).
- "મારો તહેવાર ઉમેરો": add a `CustomFestivalEntity` by month+paksha+tithi or by a fixed date.

## Scheduling (reminders-widgets, `notify/PanchangAlarmScheduler`)
1. Schedule a daily alarm at `panchang_evening_hour:00` IST (default 19:00):
   - `AlarmManager.setExactAndAllowWhileIdle(RTC_WAKEUP, …)` if `canScheduleExactAlarms()`
   - otherwise `setWindow(RTC_WAKEUP, t, 30 min, pi)`
2. `PanchangAlarmReceiver.onReceive` runs inside `goAsync()` + a coroutine:
   1. compute `tomorrow`
   2. `alert = PanchangAlertEngine.alertFor(tomorrow, prefs, custom, showGuidance)`
   3. if the alert is not null, post a notification on channel **`panchang`** ("પંચાંગ ચેતવણી", IMPORTANCE_DEFAULT):
      - title "આવતીકાલે {titleGu}"
      - text "{tithiGu} · {first doLine or avoidLine}"
      - `BigTextStyle` with all the lines
      - action "રિમાઇન્ડર બનાવો" → `CreatePanchangReminderReceiver`
      - content intent: deep link `vraj://panchang/{epochDay}`, which opens the calendar sheet
   4. schedule the next day
3. Reschedule on `BOOT_COMPLETED`, `TIME_SET`, `TIMEZONE_CHANGED` and `MY_PACKAGE_REPLACED`, and when the settings change.
4. Request `POST_NOTIFICATIONS` (API 33+) when the user first enables any type. Request exact-alarm access via `ACTION_REQUEST_SCHEDULE_EXACT_ALARM` only after explaining why in a dialog.
5. **The dialog on the day** (Home, doc 05):
   - Show it if `alertFor(today) != null`, the preference `showDialogOnDay` is on, and the stored `panchang_dialog_dismissed_<epochDay>` is absent.
   - Write that key on any action.
   - Clean up keys older than 60 days.
6. **"રિમાઇન્ડર બનાવો"** inserts `ReminderEntity(title = titleGu, note = tithiGu, priority = NORMAL, dueAt = date 07:00 IST, sourceType = "PANCHANG", sourceId = epochDay)`. If one exists for the same source, it shows "રિમાઇન્ડર પહેલેથી છે" instead of duplicating.

## Edge cases
- Two special days in a row (e.g., ધનતેરસ → કાળી ચૌદશ → દિવાળી): one notification per evening, each about its own next day.
- Several matches on one day: one dialog and one notification, with combined chips.
- The phone is off at 19:00: the alarm fires on boot only if it is before midnight. Otherwise skip it; the day dialog still covers it.
- The user disables all types: cancel the alarm.
- Custom festival on 29/30 Feb or an invalid fixed date: reject it at input.

## Gujarati strings
| Key | Text |
|---|---|
| `pa_today` | આજે |
| `pa_tomorrow` | આવતીકાલે |
| `pa_do` | કરવા જેવું |
| `pa_avoid` | ટાળવા જેવું |
| `pa_belief_note` | માન્યતા પ્રમાણે · સેટિંગ્સમાં બદલી શકો |
| `pa_ok` | સમજાયું |
| `pa_make_reminder` | રિમાઇન્ડર બનાવો |
| `pa_open_calendar` | કેલેન્ડર જુઓ |
| `pa_channel` | પંચાંગ ચેતવણી |
| `pa_calendar` | પંચાંગ કેલેન્ડર |
| `pa_special_this_month` | આ મહિનાના ખાસ દિવસો |
| `pa_tithi_ends` | તિથિ સમાપ્તિ: %s |
| `pa_settings` | પંચાંગ ચેતવણી |
| `pa_show_guidance` | કામની સલાહ બતાવો |
| `pa_evening_time` | આગલી સાંજે સૂચના: %s |
| `pa_edit_guidance` | સલાહ બદલો |
| `pa_reset_default` | મૂળ સલાહ પાછી લાવો |
| `pa_add_festival` | મારો તહેવાર ઉમેરો |
| `pa_vad_bij` | વદ બીજ પણ |
| `pa_exists` | રિમાઇન્ડર પહેલેથી છે |
| `type_bij` | બીજ |
| `type_agiyaras` | અગિયારસ |
| `type_poonam` | પૂનમ |
| `type_amas` | અમાસ |
| `type_festival` | તહેવાર |

## Expected upcoming days
These are approximate, around 03-11-2026. The engine computes them, and they must be verified against a printed panchang.
- 05-11 રમા અગિયારસ
- 06-11 ધનતેરસ (pradosh check; may fall on 06 or 07)
- 08-11 દિવાળી (અમાસ)
- 09-11 બેસતું વર્ષ
- 10-11 ભાઈબીજ (Bij)
- ~20-11 દેવઊઠી અગિયારસ
- ~24-11 દેવ દિવાળી (પૂનમ)

## Acceptance criteria
- [ ] Every Sud Bij, Agiyaras (both pakshas), Poonam and Amas in 2026–2027 is detected, with zero misses against the engine's own tithi table.
- [ ] The festival dates for VS 2082–2083 match a printed Gujarati panchang. QA attaches a comparison table (doc 16).
- [ ] The notification arrives within ±1 min of the set time when exact alarms are allowed, and within the 30-min window otherwise.
- [ ] The dialog shows once per day and never again after dismissal.
- [ ] The guidance edits persist, and "reset" restores the defaults.
- [ ] Turning a type off stops both its dialog and its notification.
- [ ] The calendar distinguishes the 5 markers by shape, so it does not rely on colour.

## Tests
- `FestivalRulesTest`:
  - each rule on synthetic `PanchangDay`s
  - kshaya assignment
  - vriddhi chooses the first day
  - pradosh rule for Diwali
  - Adhik month suppresses festivals but names Agiyaras પદ્મિની/પરમા
- `PanchangAlertEngineTest`: merge order and de-duplication; disabled types return null; custom fixed-date festival.
- `PanchangAlarmSchedulerTest` (Robolectric `ShadowAlarmManager`): exact vs window path; reschedule after boot.
- Compose test: the dialog shows → tap સમજાયું → recompose Home → no dialog.
