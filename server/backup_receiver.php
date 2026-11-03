<?php
/**
 * Vraj Kheti Hisab (વ્રજ ખેતી હિસાબ) — nightly backup receiver.
 *
 * Target: PHP 8.0+ on GoDaddy cPanel shared hosting (Apache or LiteSpeed).
 * Contract: handover/15_PDF_Reports_Nightly_Backup.md, "Server contract".
 *
 * Requests
 *   POST multipart/form-data
 *     headers: X-Vraj-Key: <api key>, X-Vraj-Device: <install id, optional>
 *     parts:   file   = the backup ZIP (application/zip)
 *              date   = yyyy-MM-dd   (the backup day; becomes vraj_yyyyMMdd.zip)
 *              sha256 = lowercase hex SHA-256 of the ZIP
 *   GET ?ping=1 with X-Vraj-Key  -> {"ok":true}  (connection test from Settings)
 *
 * Responses (always JSON)
 *   200 {"ok":true,"stored":"vraj_20261103.zip","bytes":N,"sha256":"…",…}
 *   400 {"ok":false,"error":"date|sha256|file|upload|type|hash"}
 *   401 {"ok":false,"error":"auth"}
 *   403 {"ok":false,"error":"https"}
 *   405 {"ok":false,"error":"method"}
 *   413 {"ok":false,"error":"size"}
 *   429 {"ok":false,"error":"rate"}   (+ Retry-After header)
 *   500 {"ok":false,"error":"config|store|server"}
 *   507 {"ok":false,"error":"disk"}
 *
 * Secrets: NONE in this file. The API key and mail settings are read from a
 * PHP config file that lives OUTSIDE the web root. Default location:
 *   /home/<cpanel-user>/vraj_config.php
 * (this script normally sits at /home/<cpanel-user>/public_html/vraj/backup_receiver.php,
 * so the default is two directories above this file). Override with the
 * VRAJ_CONFIG environment variable (e.g. "SetEnv VRAJ_CONFIG /home/u/x.php" in .htaccess).
 * Template: backup_config.example.php.
 */

declare(strict_types=1);

ini_set('display_errors', '0');
ini_set('log_errors', '1');
error_reporting(E_ALL);

const VK_VERSION = '1.0.0';
const VK_RATE_WINDOW_SECONDS = 3600;
const VK_ZIP_NAME_REGEX = '/^vraj_\d{8}\.zip$/D';
const VK_DENY_HTACCESS = <<<'HTACCESS'
# Vraj Kheti Hisab backup store: never serve anything from here.
<IfModule mod_authz_core.c>
    Require all denied
</IfModule>
<IfModule !mod_authz_core.c>
    Order deny,allow
    Deny from all
</IfModule>
Options -Indexes

HTACCESS;

/** Per-request context used for logging (never holds the API key). */
final class VkCtx
{
    public static ?string $logFile = null;
    public static string $ip = '-';
    public static string $device = '-';
}

// ---------------------------------------------------------------------------
// Response helpers
// ---------------------------------------------------------------------------

function vk_send(int $status, array $body, array $headers = []): void
{
    if (!headers_sent()) {
        http_response_code($status);
        header('Content-Type: application/json; charset=utf-8');
        header('Cache-Control: no-store');
        header('X-Content-Type-Options: nosniff');
        header('X-Robots-Tag: noindex, nofollow');
        foreach ($headers as $name => $value) {
            header($name . ': ' . $value);
        }
    }
    echo json_encode($body, JSON_UNESCAPED_SLASHES | JSON_UNESCAPED_UNICODE), "\n";
    exit;
}

function vk_fail(int $status, string $error, string $message = '', array $headers = [], array $extra = []): void
{
    $body = ['ok' => false, 'error' => $error];
    if ($message !== '') {
        $body['message'] = $message;
    }
    $body += $extra;
    vk_log($status . ' ' . $error . ($message !== '' ? ' (' . $message . ')' : ''));
    vk_send($status, $body, $headers);
}

function vk_log(string $line): void
{
    $file = VkCtx::$logFile;
    if ($file === null) {
        return;
    }
    // Simple size-based rotation: keep receiver.log + receiver.log.1 (each ≤ 1 MB).
    if (is_file($file) && (int) @filesize($file) > 1048576) {
        @rename($file, $file . '.1');
    }
    $entry = sprintf("%s ip=%s device=%s %s\n", date('c'), VkCtx::$ip, VkCtx::$device, $line);
    @file_put_contents($file, $entry, FILE_APPEND | LOCK_EX);
}

// ---------------------------------------------------------------------------
// Small utilities
// ---------------------------------------------------------------------------

/** Reads a request header in a way that works on mod_php, CGI/FPM and LiteSpeed. */
function vk_header(string $name): string
{
    $key = 'HTTP_' . strtoupper(str_replace('-', '_', $name));
    if (isset($_SERVER[$key]) && is_string($_SERVER[$key])) {
        return $_SERVER[$key];
    }
    if (isset($_SERVER['REDIRECT_' . $key]) && is_string($_SERVER['REDIRECT_' . $key])) {
        return $_SERVER['REDIRECT_' . $key];
    }
    if (function_exists('getallheaders')) {
        foreach ((getallheaders() ?: []) as $k => $v) {
            if (is_string($k) && strcasecmp($k, $name) === 0) {
                return (string) $v;
            }
        }
    }
    return '';
}

function vk_path_inside(string $child, string $parent): bool
{
    $c = rtrim(str_replace('\\', '/', $child), '/') . '/';
    $p = rtrim(str_replace('\\', '/', $parent), '/') . '/';
    if (DIRECTORY_SEPARATOR === '\\') {
        $c = strtolower($c);
        $p = strtolower($p);
    }
    return str_starts_with($c, $p);
}

/** Converts php.ini shorthand ("200M", "1G") to bytes. */
function vk_ini_bytes(string $value): int
{
    $value = trim($value);
    if ($value === '') {
        return 0;
    }
    $unit = strtolower(substr($value, -1));
    $num = (int) $value;
    return match ($unit) {
        'g' => $num * 1073741824,
        'm' => $num * 1048576,
        'k' => $num * 1024,
        default => $num,
    };
}

function vk_is_https(): bool
{
    if (!empty($_SERVER['HTTPS']) && strtolower((string) $_SERVER['HTTPS']) !== 'off') {
        return true;
    }
    if ((int) ($_SERVER['SERVER_PORT'] ?? 0) === 443) {
        return true;
    }
    return strtolower((string) ($_SERVER['HTTP_X_FORWARDED_PROTO'] ?? '')) === 'https';
}

/** RFC 2047 encoded-words, split so each word stays under 75 characters. */
function vk_mime_header(string $text): string
{
    $chars = preg_split('//u', $text, -1, PREG_SPLIT_NO_EMPTY);
    if ($chars === false) {
        return '=?UTF-8?B?' . base64_encode($text) . '?=';
    }
    $words = [];
    foreach (array_chunk($chars, 12) as $chunk) {
        $words[] = '=?UTF-8?B?' . base64_encode(implode('', $chunk)) . '?=';
    }
    return implode(' ', $words);
}

// ---------------------------------------------------------------------------
// Configuration (outside web root)
// ---------------------------------------------------------------------------

function vk_config_path(): string
{
    foreach (['VRAJ_CONFIG', 'REDIRECT_VRAJ_CONFIG'] as $name) {
        $env = getenv($name);
        if (is_string($env) && $env !== '') {
            return $env;
        }
        if (isset($_SERVER[$name]) && is_string($_SERVER[$name]) && $_SERVER[$name] !== '') {
            return $_SERVER[$name];
        }
    }
    // /home/<user>/public_html/vraj/backup_receiver.php  ->  /home/<user>/vraj_config.php
    return dirname(__DIR__, 2) . DIRECTORY_SEPARATOR . 'vraj_config.php';
}

function vk_doc_root(): ?string
{
    $root = isset($_SERVER['DOCUMENT_ROOT']) ? (string) $_SERVER['DOCUMENT_ROOT'] : '';
    if ($root === '') {
        return null;
    }
    $real = realpath($root);
    return $real === false ? null : $real;
}

function vk_load_config(): array
{
    $path = vk_config_path();
    if (!is_file($path) || !is_readable($path)) {
        error_log('vraj backup_receiver: config file not found or not readable: ' . $path);
        vk_fail(500, 'config');
    }
    $real = realpath($path);
    $docRoot = vk_doc_root();
    if ($real !== false && $docRoot !== null && vk_path_inside($real, $docRoot)) {
        error_log('vraj backup_receiver: refusing config inside the web root: ' . $real);
        vk_fail(500, 'config');
    }

    $cfg = require $path;
    if (!is_array($cfg)) {
        error_log('vraj backup_receiver: config file must "return [ ... ];"');
        vk_fail(500, 'config');
    }

    $key = $cfg['api_key'] ?? '';
    if (!is_string($key) || strlen($key) < 32 || stripos($key, 'REPLACE') !== false) {
        error_log('vraj backup_receiver: api_key missing, too short (<32) or still the placeholder');
        vk_fail(500, 'config');
    }
    $store = $cfg['store_dir'] ?? '';
    if (!is_string($store) || trim($store) === '') {
        error_log('vraj backup_receiver: store_dir missing');
        vk_fail(500, 'config');
    }
    $smtp = $cfg['smtp'] ?? [];

    return [
        'api_key'        => $key,
        'store_dir'      => rtrim(trim($store), '/\\'),
        'mail_enabled'   => (bool) ($cfg['mail_enabled'] ?? true),
        'mail_to'        => trim((string) ($cfg['mail_to'] ?? '')),
        'mail_from'      => trim((string) ($cfg['mail_from'] ?? '')),
        'max_bytes'      => max(1, (int) ($cfg['max_mb'] ?? 200)) * 1048576,
        'retention_days' => max(1, (int) ($cfg['retention_days'] ?? 365)),
        'rate_limit'     => max(1, (int) ($cfg['rate_limit_per_hour'] ?? 10)),
        'require_https'  => (bool) ($cfg['require_https'] ?? true),
        'timezone'       => (string) ($cfg['timezone'] ?? 'Asia/Kolkata'),
        'smtp'           => is_array($smtp) ? $smtp : [],
    ];
}

function vk_prepare_store(array $cfg): string
{
    $dir = $cfg['store_dir'];
    if (!is_dir($dir) && !@mkdir($dir, 0700, true) && !is_dir($dir)) {
        error_log('vraj backup_receiver: cannot create store_dir: ' . $dir);
        vk_fail(500, 'store');
    }
    $real = realpath($dir);
    if ($real === false || !is_writable($real)) {
        error_log('vraj backup_receiver: store_dir not writable: ' . $dir);
        vk_fail(500, 'store');
    }
    $docRoot = vk_doc_root();
    if ($docRoot !== null && vk_path_inside($real, $docRoot)) {
        error_log('vraj backup_receiver: refusing store_dir inside the web root: ' . $real);
        vk_fail(500, 'config');
    }
    // Defence in depth: even though the folder is outside public_html, deny web access.
    $ht = $real . DIRECTORY_SEPARATOR . '.htaccess';
    if (!is_file($ht)) {
        @file_put_contents($ht, VK_DENY_HTACCESS);
        @chmod($ht, 0644);
    }
    $idx = $real . DIRECTORY_SEPARATOR . 'index.html';
    if (!is_file($idx)) {
        @file_put_contents($idx, '');
    }
    return $real;
}

// ---------------------------------------------------------------------------
// Rate limiting (file based, per IP, sliding one-hour window)
// ---------------------------------------------------------------------------

/** @return int|null seconds until the client may retry, or null when allowed */
function vk_rate_limit(string $storeDir, string $ip, int $limit): ?int
{
    $dir = $storeDir . DIRECTORY_SEPARATOR . '.ratelimit';
    if (!is_dir($dir) && !@mkdir($dir, 0700, true) && !is_dir($dir)) {
        error_log('vraj backup_receiver: rate-limit dir unavailable, allowing request');
        return null; // fail open: the API key still protects the endpoint
    }
    $now = time();

    // Occasional housekeeping of stale counters.
    if (random_int(1, 50) === 1) {
        foreach (glob($dir . DIRECTORY_SEPARATOR . '*.json') ?: [] as $old) {
            $mt = @filemtime($old);
            if ($mt !== false && $mt < $now - 2 * VK_RATE_WINDOW_SECONDS) {
                @unlink($old);
            }
        }
    }

    $file = $dir . DIRECTORY_SEPARATOR . hash('sha256', $ip) . '.json';
    $fh = @fopen($file, 'c+');
    if ($fh === false) {
        return null;
    }
    try {
        if (!flock($fh, LOCK_EX)) {
            return null;
        }
        $raw = stream_get_contents($fh);
        $stamps = json_decode(is_string($raw) && $raw !== '' ? $raw : '[]', true);
        if (!is_array($stamps)) {
            $stamps = [];
        }
        $stamps = array_values(array_filter(
            $stamps,
            static fn($t): bool => is_int($t) && $t > $now - VK_RATE_WINDOW_SECONDS
        ));
        if (count($stamps) >= $limit) {
            return max(1, $stamps[0] + VK_RATE_WINDOW_SECONDS - $now);
        }
        $stamps[] = $now;
        ftruncate($fh, 0);
        rewind($fh);
        fwrite($fh, (string) json_encode($stamps));
        fflush($fh);
        return null;
    } finally {
        flock($fh, LOCK_UN);
        fclose($fh);
    }
}

// ---------------------------------------------------------------------------
// Retention
// ---------------------------------------------------------------------------

/** Deletes vraj_YYYYMMDD.zip files whose mtime is older than $days. Returns deleted names. */
function vk_prune(string $storeDir, int $days, string $keepName): array
{
    $now = time();
    $cutoff = $now - $days * 86400;
    $deleted = [];
    foreach (glob($storeDir . DIRECTORY_SEPARATOR . 'vraj_*.zip') ?: [] as $path) {
        $name = basename($path);
        if ($name === $keepName || !preg_match(VK_ZIP_NAME_REGEX, $name)) {
            continue;
        }
        $mt = @filemtime($path);
        if ($mt !== false && $mt < $cutoff && @unlink($path)) {
            $deleted[] = $name;
        }
    }
    // Leftovers from interrupted uploads.
    foreach (glob($storeDir . DIRECTORY_SEPARATOR . '.incoming_*.part') ?: [] as $part) {
        $mt = @filemtime($part);
        if ($mt !== false && $mt < $now - 86400) {
            @unlink($part);
        }
    }
    sort($deleted);
    return $deleted;
}

// ---------------------------------------------------------------------------
// ZIP inspection
// ---------------------------------------------------------------------------

/** Returns the decoded manifest.json (or null). Calls vk_fail(400) if the ZIP is corrupt. */
function vk_inspect_zip(string $path): ?array
{
    if (!class_exists('ZipArchive')) {
        return null; // zip extension not installed: signature + hash checks still apply
    }
    $zip = new ZipArchive();
    $rc = $zip->open($path, ZipArchive::CHECKCONS);
    if ($rc !== true) {
        vk_fail(400, 'type', 'zip archive is corrupt (code ' . (int) $rc . ')');
    }
    $manifest = null;
    $raw = $zip->getFromName('manifest.json', 65536);
    if (is_string($raw)) {
        $decoded = json_decode($raw, true);
        if (is_array($decoded)) {
            $manifest = $decoded;
        }
    }
    $zip->close();
    return $manifest;
}

/** Extracts a safe name => int list of record counts from the manifest. */
function vk_manifest_counts(?array $manifest): array
{
    $out = [];
    if ($manifest === null || !isset($manifest['counts']) || !is_array($manifest['counts'])) {
        return $out;
    }
    foreach ($manifest['counts'] as $k => $v) {
        if (count($out) >= 50) {
            break;
        }
        $name = preg_replace('/[^A-Za-z0-9_]/', '', (string) $k);
        if ($name !== '' && is_numeric($v)) {
            $out[$name] = (int) $v;
        }
    }
    return $out;
}

// ---------------------------------------------------------------------------
// Email notification (no attachment, the ZIP stays on the server)
// ---------------------------------------------------------------------------

function vk_mail_body(array $info): string
{
    $mb = number_format($info['bytes'] / 1048576, 2);
    $lines = [
        'વ્રજ ખેતી હિસાબ — રાત્રિ બેકઅપ મળ્યો',
        'Vraj Kheti Hisab — nightly backup received',
        '',
        'તારીખ / Date        : ' . $info['date'],
        'ફાઈલ / File         : ' . $info['stored'],
        'કદ / Size           : ' . $mb . ' MB (' . $info['bytes'] . ' bytes)',
        'SHA-256            : ' . $info['sha256'],
        'મળ્યો / Received    : ' . $info['received'],
        'ઉપકરણ / Device      : ' . $info['device'],
    ];
    if ($info['counts'] !== []) {
        $lines[] = '';
        $lines[] = 'હિસાબ સંખ્યા / Record counts:';
        foreach ($info['counts'] as $name => $n) {
            $lines[] = '  ' . $name . ': ' . $n;
        }
    }
    $lines[] = '';
    $lines[] = 'જૂના બેકઅપ દૂર કર્યા / Old backups pruned (> ' . $info['retention_days'] . ' days): '
        . count($info['pruned']);
    if ($info['disk_free'] !== null) {
        $lines[] = 'સર્વર પર ખાલી જગ્યા / Free disk: ' . number_format($info['disk_free'] / 1073741824, 2) . ' GB';
    }
    $lines[] = '';
    $lines[] = 'ઝિપ ફાઈલ સર્વર પર સુરક્ષિત છે, આ ઈમેલમાં જોડેલી નથી.';
    $lines[] = '(The ZIP stays on the server; it is not attached to this email.)';
    return implode("\r\n", $lines) . "\r\n";
}

function vk_send_smtp(array $cfg, string $to, string $from, string $subject, string $body): bool
{
    $s = $cfg['smtp'];
    $autoload = (string) ($s['autoload'] ?? '');
    if ($autoload === '' || !is_file($autoload)) {
        error_log('vraj backup_receiver: smtp.enabled but PHPMailer autoload not found');
        return false;
    }
    require_once $autoload;
    $cls = 'PHPMailer\\PHPMailer\\PHPMailer';
    if (!class_exists($cls)) {
        error_log('vraj backup_receiver: PHPMailer class not found after autoload');
        return false;
    }
    try {
        $m = new $cls(true);
        $m->isSMTP();
        $m->Host = (string) ($s['host'] ?? '');
        $m->Port = (int) ($s['port'] ?? 465);
        $m->SMTPAuth = true;
        $m->Username = (string) ($s['user'] ?? '');
        $m->Password = (string) ($s['pass'] ?? '');
        $m->SMTPSecure = (string) ($s['secure'] ?? 'ssl');
        $m->Timeout = 20;
        $m->CharSet = 'UTF-8';
        $m->setFrom($from !== '' ? $from : (string) ($s['user'] ?? ''), 'Vraj Kheti Hisab');
        $m->addAddress($to);
        $m->isHTML(false);
        $m->Subject = $subject;
        $m->Body = $body;
        return (bool) $m->send();
    } catch (Throwable $e) {
        error_log('vraj backup_receiver: SMTP send failed: ' . $e->getMessage());
        return false;
    }
}

function vk_notify(array $cfg, array $info): bool
{
    if (!$cfg['mail_enabled'] || $cfg['mail_to'] === '') {
        return false;
    }
    $to = $cfg['mail_to'];
    if (filter_var($to, FILTER_VALIDATE_EMAIL) === false) {
        error_log('vraj backup_receiver: mail_to is not a valid single address');
        return false;
    }
    $from = $cfg['mail_from'];
    if ($from !== '' && filter_var($from, FILTER_VALIDATE_EMAIL) === false) {
        error_log('vraj backup_receiver: mail_from is not a valid address, ignoring it');
        $from = '';
    }
    $subject = 'વ્રજ ખેતી હિસાબ બેકઅપ — ' . $info['date'];
    $body = vk_mail_body($info);

    if (!empty($cfg['smtp']['enabled']) && vk_send_smtp($cfg, $to, $from, $subject, $body)) {
        return true;
    }
    if (!function_exists('mail')) {
        return false;
    }
    $headers = [
        'MIME-Version: 1.0',
        'Content-Type: text/plain; charset=UTF-8',
        'Content-Transfer-Encoding: base64',
        'X-Mailer: VrajKhetiHisab-Backup/' . VK_VERSION,
    ];
    $params = '';
    if ($from !== '') {
        array_unshift($headers, 'From: Vraj Kheti Hisab <' . $from . '>');
        $params = '-f' . $from; // envelope sender; GoDaddy delivers more reliably with it
    }
    return @mail($to, vk_mime_header($subject), chunk_split(base64_encode($body)), implode("\r\n", $headers), $params);
}

// ---------------------------------------------------------------------------
// Main
// ---------------------------------------------------------------------------

function vk_require_auth(array $cfg): void
{
    $given = trim(vk_header('X-Vraj-Key'));
    // Compare fixed-length digests so neither the value nor its length leaks via timing.
    if ($given === '' || !hash_equals(hash('sha256', $cfg['api_key']), hash('sha256', $given))) {
        usleep(300000); // slow down guessing; the rate limit caps attempts per hour
        vk_fail(401, 'auth');
    }
}

function vk_main(): void
{
    $cfg = vk_load_config();
    if (!@date_default_timezone_set($cfg['timezone'])) {
        date_default_timezone_set('Asia/Kolkata');
    }

    VkCtx::$ip = (string) ($_SERVER['REMOTE_ADDR'] ?? '-');
    $device = preg_replace('/[^A-Za-z0-9._-]/', '', substr(vk_header('X-Vraj-Device'), 0, 64));
    VkCtx::$device = ($device === null || $device === '') ? '-' : $device;

    $store = vk_prepare_store($cfg);
    VkCtx::$logFile = $store . DIRECTORY_SEPARATOR . 'receiver.log';

    // 1. Rate limit (counts every request, including failed auth attempts).
    $retryAfter = vk_rate_limit($store, VkCtx::$ip, $cfg['rate_limit']);
    if ($retryAfter !== null) {
        vk_fail(429, 'rate', 'too many requests', ['Retry-After' => (string) $retryAfter]);
    }

    // 2. Method.
    $method = strtoupper((string) ($_SERVER['REQUEST_METHOD'] ?? 'GET'));
    $isPing = $method === 'GET' && (string) ($_GET['ping'] ?? '') === '1';
    if ($method !== 'POST' && !$isPing) {
        vk_fail(405, 'method', 'use POST (or GET ?ping=1)', ['Allow' => 'POST, GET']);
    }

    // 3. Transport.
    if ($cfg['require_https'] && !vk_is_https()) {
        vk_fail(403, 'https', 'use https://');
    }

    // 4. Auth.
    vk_require_auth($cfg);

    if ($isPing) {
        vk_log('200 ping');
        vk_send(200, ['ok' => true, 'ping' => true, 'version' => VK_VERSION, 'time' => date('c')]);
    }

    @set_time_limit(300);
    ignore_user_abort(true);

    // 5. Size before touching the body.
    $contentLength = (int) ($_SERVER['CONTENT_LENGTH'] ?? 0);
    $maxMb = (int) ($cfg['max_bytes'] / 1048576);
    if ($contentLength > $cfg['max_bytes'] + 1048576) {
        vk_fail(413, 'size', 'backup larger than ' . $maxMb . ' MB');
    }
    $postMax = vk_ini_bytes((string) ini_get('post_max_size'));
    if ($postMax > 0 && $contentLength > $postMax) {
        vk_fail(413, 'size', 'request larger than PHP post_max_size');
    }

    // 6. Fields.
    $dateRaw = $_POST['date'] ?? null;
    $date = is_string($dateRaw) ? trim($dateRaw) : '';
    if (!preg_match('/^(\d{4})-(\d{2})-(\d{2})$/D', $date, $m)
        || !checkdate((int) $m[2], (int) $m[3], (int) $m[1])) {
        vk_fail(400, 'date', 'date must be yyyy-MM-dd');
    }
    $storedName = 'vraj_' . $m[1] . $m[2] . $m[3] . '.zip';

    $shaRaw = $_POST['sha256'] ?? null;
    $claimed = is_string($shaRaw) ? strtolower(trim($shaRaw)) : '';
    if (!preg_match('/^[0-9a-f]{64}$/D', $claimed)) {
        vk_fail(400, 'sha256', 'sha256 must be 64 hex characters');
    }

    // 7. Upload status.
    $f = $_FILES['file'] ?? null;
    if (!is_array($f) || !isset($f['error']) || is_array($f['error'])) {
        vk_fail(400, 'file', 'missing multipart part "file"');
    }
    $err = (int) $f['error'];
    if ($err === UPLOAD_ERR_INI_SIZE || $err === UPLOAD_ERR_FORM_SIZE) {
        vk_fail(413, 'size', 'file larger than PHP upload_max_filesize');
    }
    if ($err === UPLOAD_ERR_NO_FILE) {
        vk_fail(400, 'file', 'no file uploaded');
    }
    if ($err === UPLOAD_ERR_NO_TMP_DIR || $err === UPLOAD_ERR_CANT_WRITE || $err === UPLOAD_ERR_EXTENSION) {
        error_log('vraj backup_receiver: PHP upload error code ' . $err);
        vk_fail(500, 'store', 'server could not save the upload');
    }
    if ($err !== UPLOAD_ERR_OK) {
        vk_fail(400, 'upload', 'upload incomplete (code ' . $err . ')');
    }
    $tmp = (string) ($f['tmp_name'] ?? '');
    if ($tmp === '' || !is_uploaded_file($tmp)) {
        vk_fail(400, 'file', 'not an uploaded file');
    }
    clearstatcache(true, $tmp);
    $size = @filesize($tmp);
    if ($size === false || $size <= 0) {
        vk_fail(400, 'file', 'empty file');
    }
    if ($size > $cfg['max_bytes']) {
        vk_fail(413, 'size', 'backup larger than ' . $maxMb . ' MB');
    }

    // 8. Type: ZIP local-file-header signature + finfo MIME.
    $fh = @fopen($tmp, 'rb');
    $sig = $fh !== false ? (string) fread($fh, 4) : '';
    if ($fh !== false) {
        fclose($fh);
    }
    if ($sig !== "PK\x03\x04") {
        vk_fail(400, 'type', 'not a zip file');
    }
    if (class_exists('finfo')) {
        $mime = (new finfo(FILEINFO_MIME_TYPE))->file($tmp);
        $allowed = ['application/zip', 'application/x-zip-compressed', 'application/x-zip'];
        if (!is_string($mime) || !in_array($mime, $allowed, true)) {
            vk_fail(400, 'type', 'unexpected MIME type ' . (is_string($mime) ? $mime : '?'));
        }
    }

    // 9. Integrity.
    $actual = (string) hash_file('sha256', $tmp);
    if (!hash_equals($actual, $claimed)) {
        vk_fail(400, 'hash', 'sha256 mismatch', [], ['received_sha256' => $actual]);
    }

    // 10. Structure + manifest (when the zip extension exists).
    $manifest = vk_inspect_zip($tmp);

    // 11. Disk space (needs room for the temp copy + the final file).
    $free = @disk_free_space($store);
    if (is_float($free) && $free < $size * 2 + 52428800) {
        vk_fail(507, 'disk', 'server disk is full');
    }

    // 12. Store atomically: move to a temp name, then rename over the same-date file.
    $final = $store . DIRECTORY_SEPARATOR . $storedName;
    $part = $store . DIRECTORY_SEPARATOR . '.incoming_' . bin2hex(random_bytes(8)) . '.part';
    if (!@move_uploaded_file($tmp, $part)) {
        error_log('vraj backup_receiver: move_uploaded_file failed into ' . $store);
        vk_fail(500, 'store', 'could not store the backup');
    }
    @chmod($part, 0600);
    $lock = @fopen($store . DIRECTORY_SEPARATOR . '.write.lock', 'c');
    if ($lock !== false) {
        flock($lock, LOCK_EX);
    }
    $renamed = @rename($part, $final);
    if ($lock !== false) {
        flock($lock, LOCK_UN);
        fclose($lock);
    }
    if (!$renamed) {
        @unlink($part);
        error_log('vraj backup_receiver: rename to final name failed: ' . $final);
        vk_fail(500, 'store', 'could not store the backup');
    }
    @touch($final);
    clearstatcache(true, $final);
    $bytes = (int) filesize($final);

    // 13. Retention.
    $pruned = vk_prune($store, $cfg['retention_days'], $storedName);

    // 14. Email (failure never fails the upload).
    $diskFree = @disk_free_space($store);
    $mailed = vk_notify($cfg, [
        'date'           => $date,
        'stored'         => $storedName,
        'bytes'          => $bytes,
        'sha256'         => $actual,
        'received'       => date('d-m-Y H:i:s T'),
        'device'         => VkCtx::$device,
        'counts'         => vk_manifest_counts($manifest),
        'pruned'         => $pruned,
        'retention_days' => $cfg['retention_days'],
        'disk_free'      => is_float($diskFree) ? $diskFree : null,
    ]);

    vk_log(sprintf('200 stored=%s bytes=%d pruned=%d mailed=%s', $storedName, $bytes, count($pruned), $mailed ? 'yes' : 'no'));

    vk_send(200, [
        'ok'       => true,
        'stored'   => $storedName,
        'bytes'    => $bytes,
        'sha256'   => $actual,
        'date'     => $date,
        'manifest' => $manifest !== null,
        'pruned'   => count($pruned),
        'mailed'   => $mailed,
    ]);
}

set_exception_handler(static function (Throwable $e): void {
    error_log('vraj backup_receiver: ' . get_class($e) . ': ' . $e->getMessage()
        . ' at ' . $e->getFile() . ':' . $e->getLine());
    vk_log('500 server (' . get_class($e) . ')');
    vk_send(500, ['ok' => false, 'error' => 'server']);
});

vk_main();
