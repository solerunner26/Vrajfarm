# Vraj Kheti Hisab: nightly backup receiver (GoDaddy cPanel)

Every night at 23:45 IST the app uploads one ZIP (database, photos and the day's PDF) to
`backup_receiver.php` on the owner's own GoDaddy hosting. The script:

- checks the secret key
- checks the file (size, ZIP type, SHA-256)
- stores it as `vraj_YYYYMMDD.zip` in a private folder
- deletes backups older than 365 days
- emails the owner a short notice (the ZIP is **not** attached)

The contract is defined in `handover/15_PDF_Reports_Nightly_Backup.md` ("Server contract").

## Files in this folder

| File | Goes to (on the server) | Purpose |
|---|---|---|
| `backup_receiver.php` | `public_html/vraj/backup_receiver.php` | The endpoint |
| `.htaccess` | `public_html/vraj/.htaccess` | Only the endpoint is reachable; no directory listing |
| `.user.ini` | `public_html/vraj/.user.ini` | PHP upload limits (200 MB) and timeouts |
| `backup_config.example.php` | copy it to `/home/<user>/vraj_config.php` (**outside** public_html) | Key, folder, email settings |
| `store_dir.htaccess` | `/home/<user>/vraj_backups/.htaccess` (optional, created automatically) | Denies web access to the backups |
| `tests/` | **do not upload** | Local and live tests |
| `README.md` | **do not upload** | This guide |

**Never put the real key in git.** `vraj_config.php` and `backup_config.php` are git-ignored.

---

## Step-by-step deployment (about 20 minutes)

Below, `<user>` is your cPanel username (shown at the top right of cPanel, and in the File Manager path
`/home/<user>`). `example.com` is a placeholder for the owner's real domain.

### 1. Make sure HTTPS works
1. Open cPanel → **SSL/TLS Status**.
2. The domain should show a green padlock (AutoSSL). If it does not, click **Run AutoSSL** and wait.
3. Then open `https://example.com` in a browser. It must load without a warning.

The script refuses plain `http://` because the key would travel unencrypted.

### 2. Choose PHP 8
1. Open cPanel → **MultiPHP Manager** (or **Select PHP Version**).
2. Set the domain to **PHP 8.1 or newer** (8.0 is the minimum).
3. In **Select PHP Version → Extensions**, make sure `fileinfo` and `zip` are ticked. Both are usually on already. They are recommended but not required.

### 3. Create the secret key
1. Use any of these:
   - cPanel → **Terminal**: `php -r 'echo bin2hex(random_bytes(32)), PHP_EOL;'`
   - On your own PC with Git Bash or WSL: `openssl rand -hex 32`
2. You get 64 characters like `3f9a…`.
3. Save it in your password manager. You will type it into the phone once.

Do not email or WhatsApp the key.

### 4. Create the config file (outside public_html)
1. Open cPanel → **File Manager** and go to `/home/<user>`. This is the folder that *contains* `public_html`, not inside it.
2. Click **+ File** and name it `vraj_config.php`.
3. Right-click it → **Edit**. Paste the contents of `backup_config.example.php`, then change:
   - `api_key`: the 64-character key from step 3.
   - `store_dir`: `/home/<user>/vraj_backups`.
   - `mail_to`: the owner's email address.
   - `mail_from`: an address on **your** domain, for example `backup@example.com`. Create it first in cPanel → **Email Accounts**. GoDaddy often silently drops mail "from" gmail.com.
4. Save the file.
5. Right-click → **Change Permissions** → set **0600** (owner read and write only).

The script refuses to run (it returns `500 {"error":"config"}`) if the key still says `REPLACE…`, if the key is shorter than 32 characters, or if the config file is inside `public_html`.

### 5. Upload the endpoint
1. In File Manager, open `public_html` and create a folder `vraj`.
2. Upload `backup_receiver.php`, `.htaccess` and `.user.ini` into `public_html/vraj/`.
   - Files starting with a dot are hidden. In File Manager, click **Settings → Show Hidden Files (dotfiles)**.
3. Set the permissions to `backup_receiver.php` **0644** and `.htaccess` **0644**.

The script finds `/home/<user>/vraj_config.php` by itself. That location is two folders above the script.

If you put the config somewhere else, add this line to `public_html/vraj/.htaccess`:
```
SetEnv VRAJ_CONFIG /home/<user>/somewhere/vraj_config.php
```

### 6. Test from your PC
1. Run a ping (replace the URL and the key):
   ```bash
   curl -s -H "X-Vraj-Key: YOUR_64_CHAR_KEY" "https://example.com/vraj/backup_receiver.php?ping=1"
   # -> {"ok":true,"ping":true,"version":"1.0.0","time":"…"}
   ```
2. Upload a test file. The date 2000-01-01 is used so the upload can never replace a real backup:
   ```bash
   SHA=$(sha256sum test.zip | cut -d' ' -f1)
   curl -s -H "X-Vraj-Key: YOUR_64_CHAR_KEY" -H "X-Vraj-Device: my-pc" \
        -F "file=@test.zip;type=application/zip" -F "date=2000-01-01" -F "sha256=$SHA" \
        https://example.com/vraj/backup_receiver.php
   # -> {"ok":true,"stored":"vraj_20000101.zip","bytes":…,"sha256":"…",…}
   ```
3. Or run the full contract check:
   ```bash
   VRAJ_URL=https://example.com/vraj/backup_receiver.php VRAJ_KEY=YOUR_64_CHAR_KEY \
     bash server/tests/backup_receiver_test.sh
   ```
4. Check the results:
   - `/home/<user>/vraj_backups/` now has `vraj_20000101.zip`, `receiver.log` and `.htaccess`.
   - The owner received the email "વ્રજ ખેતી હિસાબ બેકઅપ — 2000-01-01". If not, check the spam folder, then see "Email" below.
5. **Delete `vraj_20000101.zip` afterwards.**

The server allows **10 requests per hour per IP**, and failed attempts count. If you see `429`, wait an hour or raise `rate_limit_per_hour` while testing.

### 7. Set up the phone
1. Open the app and go to **Settings → રાત્રિ બેકઅપ**.
2. Enter **સર્વર સરનામું** = `https://example.com/vraj/backup_receiver.php`.
3. Enter **ગુપ્ત કી** = the key. The app stores it in EncryptedSharedPreferences.
4. The app pings the server to confirm the details. Then tap **હમણાં બેકઅપ લો** once and check that Home shows **બેકઅપ OK**.

---

## API (for developers)

**`POST`** `multipart/form-data`
- Headers: `X-Vraj-Key` (required) and `X-Vraj-Device` (optional, only used in the log and email).
- Parts:
  - `file`: the ZIP
  - `date`: `yyyy-MM-dd`
  - `sha256`: hex SHA-256 of the ZIP

**`GET ?ping=1`** with `X-Vraj-Key` → `200 {"ok":true,…}`

| HTTP | `error` | Meaning |
|---|---|---|
| 200 | none | `{"ok":true,"stored":"vraj_20261103.zip","bytes":N,"sha256":"…","date":"…","manifest":bool,"pruned":N,"mailed":bool}` |
| 400 | `date` | `date` is missing, not `yyyy-MM-dd`, or not a real day |
| 400 | `sha256` | `sha256` is missing or not 64 hex characters |
| 400 | `file` / `upload` | No file part, an empty file, or an interrupted upload |
| 400 | `type` | Not a ZIP (bad signature, MIME type or structure) |
| 400 | `hash` | The SHA-256 does not match. The response includes `received_sha256` |
| 401 | `auth` | Missing or wrong key |
| 403 | `https` | The request came over plain `http://` |
| 405 | `method` | Anything other than POST or `GET ?ping=1` |
| 413 | `size` | Larger than `max_mb`, or larger than PHP's `upload_max_filesize`/`post_max_size`. The app shows "ફોટા ઘણા છે — સર્વર મર્યાદા વધારો" |
| 429 | `rate` | Too many requests. See the `Retry-After` header (seconds) |
| 500 | `config` / `store` / `server` | A config problem, an unwritable folder, or an unexpected error. Details are in the cPanel **Errors** log. They are never sent to the client |
| 507 | `disk` | The hosting disk is almost full |

Notes:
- The size error uses **413**, not 400, so that the app can tell "too big" apart from "corrupt". The JSON `error` field is always the reliable signal.
- A second upload with the same `date` replaces that day's file atomically (temporary name, then rename).
- Retention deletes only files matching `vraj_YYYYMMDD.zip` whose file time is older than `retention_days`. Nothing else in the folder is touched.

## Email

- **Default:** PHP `mail()`. It works on most GoDaddy cPanel plans when `mail_from` is a mailbox on the same domain.
- **If mail does not arrive:**
  1. Check the spam folder.
  2. Check that `mail_from` exists in cPanel → Email Accounts.
  3. Check cPanel → **Email Deliverability** (SPF and DKIM should be "Valid").
- **SMTP (more reliable):**
  1. In cPanel → Terminal, run `cd ~ && composer require phpmailer/phpmailer`. This installs PHPMailer into `/home/<user>/vendor`, outside public_html.
  2. In `vraj_config.php`, set `smtp.enabled => true` and fill in `host` (usually `mail.example.com`), `port` (465), `secure` ('ssl'), `user` and `pass` (the mailbox password).
  3. If SMTP fails, the script falls back to `mail()`.
- A failed email never fails the upload. The response simply says `"mailed":false`.

## Restoring a backup

1. In File Manager, open `/home/<user>/vraj_backups/`.
2. Right-click the day you want → **Download**.
3. Copy the ZIP to the phone and use **Settings → બેકઅપમાંથી પાછું લાવો**.

## Local test (developer PC with PHP 8 installed)

```bash
php server/tests/run_tests.php
```

What it does:
1. Lints all PHP files.
2. Starts `php -S` on a free port with temporary configs in the system temp folder.
3. Checks every status code in the table above, plus pruning, overwrite, the store `.htaccess` and that the key never appears in `receiver.log`.
4. Cleans up afterwards.

It needs no PHP extensions beyond the core.

## Security checklist

- [ ] HTTPS works and `require_https` is `true`.
- [ ] `vraj_config.php` is in `/home/<user>/`, not in `public_html`, with permissions 0600.
- [ ] `vraj_backups/` is in `/home/<user>/`, not in `public_html`.
- [ ] `https://example.com/vraj/` shows **403 Forbidden** (no file list).
- [ ] `https://example.com/vraj/.user.ini` shows **403**.
- [ ] The key is 64 hex characters, stored only in the config file, the phone and a password manager.
- [ ] `README.md` and `tests/` are **not** uploaded to the server.
