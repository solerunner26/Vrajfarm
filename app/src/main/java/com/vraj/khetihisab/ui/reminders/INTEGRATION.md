# Reminders screen — wiring for the lead integrator (doc 13, Wave 3C)

## Route
- `RemindersRoutes.REMINDERS = "reminders"`, deep link `vraj://reminders` (`VkDeepLinks.REMINDERS`).
- Register it inside the NavHost in `ui/app/VkRoot.kt`:
  ```kotlin
  import com.vraj.khetihisab.ui.reminders.remindersGraph
  NavHost(...) {
      ...
      remindersGraph(nav)
  }
  ```
- Navigate from Home tiles / Panchang dialog with `nav.navigate(RemindersRoutes.REMINDERS)`.
  `HomePlaceholderScreen`'s `onOpen(route)` can simply pass `"reminders"`.

## Deep link
- Producers already emit `vraj://reminders`: the reminder notification content intent
  (`ReminderNotifier`) and the `ReminderGlanceWidget` header tap.
- The manifest already has the `vraj` VIEW intent filter and `singleTop`; VkRoot already calls
  `nav.handleDeepLink(intent)` for warm intents (`pendingDeepLink`). With the `navDeepLink` inside
  `remindersGraph`, both cold start (NavHost reads the Activity intent) and warm intents land here.
- Still open from HANDOVER: `MainActivity.onNewIntent` must push the new intent into
  `pendingDeepLink` (lead-owned) for warm opens to work.

## Dependencies
- None to inject. The graph builds `RepoReminderStore(applicationContext)`, which reads
  `NotifyGraph.repos(context)` — already installed in `VrajApp.onCreate` with AppContainer's
  `DataRepositories` (shared WidgetRefresher listener).
- Uses existing manifest permissions: CAMERA, POST_NOTIFICATIONS, SCHEDULE_EXACT_ALARM, and the
  FileProvider `${applicationId}.files` (`tmp/` for camera shots). Photos are stored as WebP
  ≤ 100 KB in `filesDir/reminder_photos/` (already in `BackupZipBuilder.PHOTO_DIRS` and
  `res/xml/file_paths.xml`).

## One code path for every DONE
`ReminderActions` (`notify/ReminderReceivers.kt`) is shared by the notification action
("પૂરું થયું"), the `ReminderGlanceWidget` DONE pill (`RepoWidgetDataSource.markReminderDone`) and
this screen (`RepoReminderStore`):
- `markDone(ctx, id)`: repo.markDone + cancel all alarm slots + cancel notification + refresh widgets
- `reopen(ctx, id)`: undo snackbar / "ફરી ચાલુ કરો" → repo.markUndone + reschedule
- `rearm(ctx, r)`: after add/edit → cancel all slots (incl. snooze) + notification, then schedule
- `delete(ctx, id)`: soft delete + cancel alarms + notification

## Behaviour notes
- POST_NOTIFICATIONS is asked contextually after the first saved reminder (API 33+).
- If exact alarms are not allowed, a banner on the active tab opens
  `ACTION_REQUEST_SCHEDULE_EXACT_ALARM`; alarms fall back to a 15-min `setWindow`.
- A reminder saved with a past time gets no alarm and shows "મુદત વીતી" (sheet warns first).
