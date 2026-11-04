# 15 · Gujarati PDF Reports (7) & 11:45 PM Nightly Backup

**Owning agent:** backup-reports · **Canvas:** 15 Settings (backup card, PDF report grid) · **Depends on:** 02, 03, 14

## Purpose
- Produce 7 printable A4 Gujarati reports in which conjuncts (જોડાક્ષર) render correctly.
- Every night at 23:45 IST, package the database, all photos and today's report, and upload them to the owner's own GoDaddy server. That server keeps a 365-day archive and emails the owner. No third-party cloud is involved.

## User stories
- I tap "ખેતરવાર નફો-નુકસાન", get a PDF, and can share or print it.
- Every night my data is backed up automatically, and Home shows "બેકઅપ OK".
- If the backup fails, I see "બેકઅપ નિષ્ફળ" and can retry with "હમણાં બેકઅપ લો".

## Part A — PDF reports (`report/GujaratiPdfGenerator`)

### Reports
| # | Gujarati title | Content | Filters |
|---|---|---|---|
| 1 | દૈનિક હિસાબ | all entries of a day, grouped by section (ખેતી, પશુપાલન, પરિવાર, સામાન્ય, અંગત), with a "કુલ (અંગત સિવાય)" business-P&L row | date (default today) |
| 2 | ખેતરવાર નફો-નુકસાન | per farm: income, expense, net; then a grand total | season / range |
| 3 | વીઘા સ્કોરકાર્ડ | the ranking table (doc 07) | season |
| 4 | પરિવાર A/B ઉપાડ | month-wise advances for A and B, with year totals | year |
| 5 | અંગત હિસાબ | personal income, expense and categories | month / range |
| 6 | ઉધાર બાકી | pending payable and receivable lists, with age in days | none |
| 7 | મિલકત અને ઉછીનું | inventory, active loans (with 48px borrower thumbnails), overdue highlighted | none |

### Rendering
- `android.graphics.pdf.PdfDocument`, page **595×842 pt** (A4 at 72 dpi), margins 36 pt.
- Text uses **`StaticLayout.Builder.obtain(text, 0, len, TextPaint, width)`** with:
  - `TextPaint.typeface = ResourcesCompat.getFont(ctx, R.font.noto_sans_gujarati_regular)`
  - `setBreakStrategy(BREAK_STRATEGY_HIGH_QUALITY)` and `setHyphenationFrequency(NONE)`

  **Never** use `Canvas.drawText` for Gujarati runs longer than a single word. `StaticLayout` does the HarfBuzz shaping that conjuncts need.
- **Header** on every page:
  - a 28 pt brand-green band (flat `#1F5C45`; PDFs avoid gradients for printing)
  - the logo, rasterised once at 64 px
  - "વ્રજ ખેતી હિસાબ"
  - the report title
  - the range
  - "તૈયાર: dd-MM-yyyy HH:mm · {tithi}"
- **Footer**: "ગિરવાનસિંહ વાળા · થોરાળા" on the left and "પાનું N / M" on the right. Do not include the developer credit (doc 14 rule).
- **Tables**: a `TableRenderer` that measures each row with `StaticLayout` height, paginates when `y + rowH > pageBottom`, and repeats the header row on new pages. Amount columns are right-aligned, with zebra rows `#F5F7F4`.
- Output: `filesDir/reports/<slug>_<yyyyMMdd_HHmm>.pdf`. Open it with `FileProvider` via `ACTION_VIEW application/pdf`, with a share button (`ACTION_SEND`).
- Generate off the main thread (`Dispatchers.Default`) and show a progress dialog.

## Part B — Nightly backup

### Schedule (`backup/BackupScheduler`)
- Use `AlarmManager` exact at **23:45 IST** daily: `setExactAndAllowWhileIdle` if `canScheduleExactAlarms()`, otherwise `setWindow(23:30, 30 min)`.
- `BackupAlarmReceiver` enqueues a `OneTimeWorkRequest<NightlyBackupWorker>` with constraints `NetworkType.CONNECTED` and `setExpedited(RUN_AS_NON_EXPEDITED_WORK_REQUEST)`, using unique work name "nightly_backup" with `REPLACE`.
- Backoff: exponential, 10 min, up to 6 attempts. If there is still no network by morning, the next run includes everything anyway, because the backup is a full snapshot.
- Reschedule on boot and time change (shared `BootReceiver`).
- "હમણાં બેકઅપ લો" enqueues the same worker immediately.

### Worker steps (`NightlyBackupWorker.doWork`)
1. `db.openHelper.writableDatabase.query("PRAGMA wal_checkpoint(FULL)").close()`.
2. Generate report #1 for today, the daily ledger PDF.
3. Create `cacheDir/backup/vraj_<yyyyMMdd>.zip` (`ZipOutputStream`, DEFLATE) containing:
   - `db/vraj_kheti.db` (+ `-wal`/`-shm` if present after the checkpoint)
   - `bill_photos/**`, `people_photos/**`, `borrower_photos/**`, `reminder_photos/**`
   - `reports/daily_<date>.pdf`
   - `manifest.json`:
     ```json
     {"app":"vraj_kheti_hisab","version":"1.0","dbVersion":1,"createdAt":"…","counts":{"txn":N,…},"sha256":{"db/vraj_kheti.db":"…"}}
     ```
4. Upload via OkHttp multipart `POST {endpoint}`:
   - headers: `X-Vraj-Key: <apiKey>` and `X-Vraj-Device: <installId>`
   - parts: `file` (the zip, `application/zip`), `date` (yyyy-MM-dd), `sha256` (of the zip)
   - timeouts: connect 30 s, write 5 min
5. On HTTP 200 with JSON `{"ok":true,"stored":"vraj_20261103.zip","bytes":…}`: save `BackupStatus(lastSuccessAt = now, lastBytes)`, delete the local zip and post a silent notification on the `backup` channel only if the previous status was failed. Otherwise call `Result.retry()`, and after the last attempt store `lastFailureAt` and `reason`.
6. Keep the last 3 zips locally only if an upload failed, so storage stays bounded.

### Settings (Backup card on artboard 15)
- Endpoint URL (default `https://<owner-domain>/vraj/backup_receiver.php`, entered by the owner).
- API key, stored in `EncryptedSharedPreferences`. Never hard-code a key and never log it.
- "છેલ્લો બેકઅપ" and "આગલો" times.
- "હમણાં બેકઅપ લો".
- "બેકઅપમાંથી પાછું લાવો" (restore): pick a zip with `OpenDocument` → verify the manifest and sha256 → close the DB → replace the files → restart the app. Confirm twice before restoring.

### Server contract (`server/backup_receiver.php`, PHP 8, GoDaddy cPanel)
- Config file **outside web root**: `/home/<user>/vraj_config.php` returning `['api_key'=>..., 'store_dir'=>'/home/<user>/vraj_backups', 'mail_to'=>..., 'max_mb'=>200]`.
- Behaviour:
  1. Accept `POST` only, else return 405.
  2. `hash_equals($cfg['api_key'], $_SERVER['HTTP_X_VRAJ_KEY'] ?? '')`, else return 401 `{"ok":false,"error":"auth"}`.
  3. Validate the upload: `UPLOAD_ERR_OK`, size ≤ max, `finfo` MIME `application/zip`, name sanitised to `vraj_YYYYMMDD.zip` from the `date` field (regex `^\d{4}-\d{2}-\d{2}$`), and `hash_file('sha256')` equal to the posted `sha256`. Otherwise return 400.
  4. `move_uploaded_file` into `store_dir`, overwriting the same date.
  5. Retention: delete zips with `filemtime` older than 365 days.
  6. Email via `mail()` (or PHPMailer over SMTP if configured): subject "વ્રજ ખેતી હિસાબ બેકઅપ — {date}", body with the size and the manifest counts. **Do not attach the zip.** It stays on the server, and the email only notifies.
  7. Respond 200 `{"ok":true,"stored":"…","bytes":N}`.
- Add an `.htaccess` in the store dir with `Require all denied`, and rate-limit to 10 requests/hour per IP via a small file-based counter.

## Edge cases
- The DB is locked during the checkpoint: retry once after 2 s.
- A zip larger than 200 MB (many photos): fail with reason "size" and show "ફોટા ઘણા છે — સર્વર મર્યાદા વધારો". A future version will upload photos incrementally.
- The device is in Doze at 23:45: the window alarm and WorkManager handle it, and the status shows the real time it ran.
- Changing the API key re-tests the connection via `GET ?ping=1`, which returns `{"ok":true}` after the auth check.

## Gujarati strings
| Key | Text |
|---|---|
| `rep_daily` | દૈનિક હિસાબ |
| `rep_farm_pnl` | ખેતરવાર નફો-નુકસાન |
| `rep_vigha` | વીઘા સ્કોરકાર્ડ |
| `rep_family` | પરિવાર A/B ઉપાડ |
| `rep_personal` | અંગત હિસાબ |
| `rep_udhaar` | ઉધાર બાકી |
| `rep_assets` | મિલકત અને ઉછીનું |
| `rep_generated` | તૈયાર: %s |
| `rep_page` | પાનું %d / %d |
| `bk_title` | રાત્રિ બેકઅપ |
| `bk_last` | છેલ્લો બેકઅપ |
| `bk_next` | આગલો |
| `bk_now` | હમણાં બેકઅપ લો |
| `bk_restore` | બેકઅપમાંથી પાછું લાવો |
| `bk_endpoint` | સર્વર સરનામું |
| `bk_key` | ગુપ્ત કી |
| `bk_too_big` | ફોટા ઘણા છે — સર્વર મર્યાદા વધારો |

## Acceptance criteria
- [ ] All 7 PDFs render Gujarati conjuncts correctly (QA visual check of ક્ષ, જ્ઞ, શ્ર, દ્ધ, ત્ર) and paginate long tables.
- [ ] The backup runs nightly without the app open, survives a reboot, and updates the Home pill.
- [ ] The server rejects a wrong key (401), a bad hash (400) and non-POST requests (405), and prunes files older than 365 days.
- [ ] Restoring a zip reproduces identical DAO totals.
- [ ] No secret is in source control.

## Tests
- `GujaratiPdfGeneratorTest` (instrumented): generate report #2 with 300 rows → the page count is > 1, and the file is a valid PDF (open it with `PdfRenderer`).
- `NightlyBackupWorkerTest` (work-testing + OkHttp `MockWebServer`): success path and retry on 500.
- `ZipManifestTest`: the sha256 matches.
- PHP: a `tests/backup_receiver_test.sh` curl script covering the 401, 400, 405 and 200 cases against a local `php -S`.
