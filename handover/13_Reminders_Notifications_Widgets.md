# 13 · Reminders, Push Notifications & Home-screen Widgets (Glance)

**Owning agent:** reminders-widgets · **Canvas:** 12 Reminders, 13 Home-screen Widgets · **Depends on:** 02, 03. Also owns the scheduling part of doc 10.

## Purpose
- Text and image reminders with HIGH/NORMAL priority and a deadline, delivered as Gujarati push notifications. DONE moves a reminder to the archive.
- Three Glance widgets on the Galaxy S22 home screen: pending shopping, urgent reminders, and today + tithi.
- One notification framework that every module uses.

## User stories
- I add "લાઇટ બિલ ભરવાનું" for tomorrow with HIGH priority and get notified in Gujarati.
- I attach a photo to a reminder (for example a broken part).
- I tick DONE and the reminder moves to the archive.
- From the home screen I can tick shopping items and mark reminders DONE without opening the app.

## Notification framework (`notify/`)
`NotificationChannels.createAll(context)` runs in `Application.onCreate`:

| Channel id | Name (Gujarati) | Importance |
|---|---|---|
| `reminders` | રિમાઇન્ડર | HIGH |
| `assets` | મિલકત ચેતવણી | DEFAULT |
| `shopping` | ખરીદી યાદી | LOW |
| `panchang` | પંચાંગ ચેતવણી | DEFAULT |
| `backup` | બેકઅપ | LOW |

- Small icon: a monochrome `ic_stat_vk` (24dp, white).
- Accent `color = primary`. Every `PendingIntent` uses `FLAG_IMMUTABLE`.
- `POST_NOTIFICATIONS` (API 33+) is requested contextually, the first time the user creates a reminder or enables Panchang alerts.

## Reminders

### Screen (RemindersScreen, artboard 12)
1. `VkTopHeader("રિમાઇન્ડર")`.
2. `VkSegmented` with **ચાલુ (n)** and **આર્કાઇવ (n)**.
3. A `VkCard` per reminder:
   - the priority tag: HIGH = `primaryContainer` chip "જરૂરી", NORMAL = outlined chip "સામાન્ય"; no amber
   - the deadline text: "આવતીકાલે · 04-11-2026", "મુદત વીતી · 30-10-2026" in error, or "આજે 5:00 PM"
   - the title (18sp/800), the note, and an image thumbnail (64dp) if there is one
   - `VkPrimaryButton("DONE · પૂરું થયું")`

   Archived cards show the title struck through, the done date, and a "ફરી ચાલુ કરો" text button.
4. Bottom `VkPrimaryButton("+ નવું રિમાઇન્ડર")` opens the `ReminderEditorSheet`:
   - title (with a voice mic)
   - note
   - priority segmented
   - date and time pickers (M3)
   - an optional photo (camera/gallery → WebP ≤ 100 KB in `filesDir/reminder_photos/`)
   - save

   Source-linked reminders (LOAN, PANCHANG) show a small origin chip, e.g. "મિલકત" or "પંચાંગ".

### Scheduling (`notify/ReminderScheduler`)
```kotlin
fun schedule(r: ReminderEntity) {
  val am = context.getSystemService<AlarmManager>()!!
  val at = if (r.priority == HIGH) r.dueAt - 60 * 60_000 else r.dueAt   // HIGH: 1 h early + at due
  val pi = PendingIntent.getBroadcast(context, r.id.toInt(), Intent(context, ReminderReceiver::class.java).putExtra("id", r.id), FLAG_IMMUTABLE or FLAG_UPDATE_CURRENT)
  if (Build.VERSION.SDK_INT < 31 || am.canScheduleExactAlarms()) am.setExactAndAllowWhileIdle(RTC_WAKEUP, at, pi)
  else am.setWindow(RTC_WAKEUP, at, 15 * 60_000, pi)
}
```
- `ReminderReceiver` loads the row. If it is not done, it posts a notification:
  - title = the reminder title
  - text "{deadline label} · {note}"
  - actions **"પૂરું થયું"** (→ `ReminderActionReceiver`, which calls `markDone`, cancels the notification and refreshes the widgets) and **"1 કલાક પછી"** (snooze: reschedule at now + 1 h)
  - for HIGH, `setCategory(CATEGORY_REMINDER)`
- Reschedule all future reminders on `BOOT_COMPLETED`, `TIME_SET`, `TIMEZONE_CHANGED` and `MY_PACKAGE_REPLACED` (`BootReceiver`), and when exact-alarm permission changes (`ACTION_SCHEDULE_EXACT_ALARM_PERMISSION_STATE_CHANGED`).
- Editing or deleting a reminder cancels or reschedules its alarm.

## Widgets (`widget/`, Jetpack Glance 1.1)
Common rules:
- Use `GlanceTheme` with colours from doc 01. Light: surface `#FFFFFF`, text `#17201B`, accents primary. Dark: the derived roles.
- 24dp corner radius (`cornerRadius` on API 31+, a rounded background drawable before that).
- Glance cannot render vector gradients reliably on all launchers, so ship **PNG exports** of the needed `ic_vk_*` icons at 96px as `drawable-nodpi/ic_vk_*_png.png`.
- Data comes from a `GlanceAppWidget.provideGlance` that reads repositories through `AppContainer` (`withContext(IO)`).
- Refresh with `updateAll(context)` after any relevant DAO write (`WidgetRefresher.refresh()`, called by the repositories), plus a 30-min `updatePeriodMillis` fallback.

| Widget | Size | Content | Actions |
|---|---|---|---|
| `ShoppingListGlanceWidget` | 4×3 (min 250×180dp) | header icon + "ખરીદી બાકી" + count badge; up to 5 items with a check box, Farm tab first | tapping the box runs `ToggleShoppingAction : ActionCallback` (setBought true, refresh); tapping the header opens the Shopping screen |
| `ReminderGlanceWidget` | 4×2 | "જરૂરી કામ"; up to 3 HIGH, then NORMAL, reminders, each with a deadline and a "DONE" pill | `MarkReminderDoneAction` |
| `TodayGlanceWidget` | 2×2 | the panchang glyph, the date number (30sp), "નવેમ્બર · મંગળ", the tithi "આસો વદ નોમ"; if today or tomorrow is a special day (doc 10), a line "આવતીકાલે દિવાળી" | tap opens the Panchang calendar |

`res/xml/*_info.xml` provides `previewLayout`, `targetCellWidth`/`Height` (API 31+), `resizeMode="horizontal|vertical"` and `widgetCategory="home_screen"`. The Today widget updates at midnight IST via an alarm, using `updateAll` in the receiver.

## Edge cases
- A reminder with a due date in the past is saved without an alarm. It shows "મુદત વીતી" immediately.
- If exact-alarm permission is revoked, fall back to windows and show a one-time banner in Settings.
- Doze: `setExactAndAllowWhileIdle` is fine for alarms; widgets are not time-critical.
- App locked (doc 04): widget actions still work, because they only write booleans, but they never display amounts.

## Gujarati strings
| Key | Text |
|---|---|
| `rem_title` | રિમાઇન્ડર |
| `rem_active` | ચાલુ (%d) |
| `rem_archive` | આર્કાઇવ (%d) |
| `rem_high` | જરૂરી |
| `rem_normal` | સામાન્ય |
| `rem_done` | DONE · પૂરું થયું |
| `rem_new` | + નવું રિમાઇન્ડર |
| `rem_overdue` | મુદત વીતી · %s |
| `rem_tomorrow` | આવતીકાલે · %s |
| `rem_snooze` | 1 કલાક પછી |
| `rem_reopen` | ફરી ચાલુ કરો |
| `w_shop` | ખરીદી બાકી |
| `w_rem` | જરૂરી કામ |
| `ch_reminders` | રિમાઇન્ડર |
| `ch_assets` | મિલકત ચેતવણી |
| `ch_shopping` | ખરીદી યાદી |
| `ch_panchang` | પંચાંગ ચેતવણી |
| `ch_backup` | બેકઅપ |

## Acceptance criteria
- [ ] Reminders fire on time with Gujarati text, and the DONE action from the notification archives them.
- [ ] Alarms survive a reboot and a time change.
- [ ] The 3 widgets render on the One UI launcher, and their actions update the DB and the widget within 2 s.
- [ ] Widgets show no money amounts.

## Tests
- `ReminderSchedulerTest` (Robolectric `ShadowAlarmManager`): HIGH schedules 1 h early; the window fallback is used when exact alarms are not allowed.
- `ReminderDaoTest`: active → done → archive ordering.
- Glance: `ToggleShoppingAction` unit test with a fake repo. Manual QA on device (doc 16).
