<?php
/**
 * Local test suite for server/backup_receiver.php.
 *
 *   php server/tests/run_tests.php
 *
 * 1. `php -l` on every PHP file in server/.
 * 2. Starts `php -S 127.0.0.1:<free port>` with throw-away configs (in the
 *    system temp dir, i.e. outside the docroot) and exercises the contract:
 *    405 / 401 / 400 (date, sha256, file, type, hash) / 413 / 200 / overwrite /
 *    365-day pruning / 403 https / 429 rate limit / 500 missing config.
 * Uses only core PHP (no curl or zip extension needed). Exit code 0 = all pass.
 */

declare(strict_types=1);

if (PHP_SAPI !== 'cli') {
    http_response_code(404);
    exit;
}

final class T
{
    public static int $pass = 0;
    public static int $fail = 0;

    public static function check(bool $cond, string $label, string $detail = ''): void
    {
        if ($cond) {
            self::$pass++;
            echo "PASS  {$label}\n";
        } else {
            self::$fail++;
            echo "FAIL  {$label}" . ($detail !== '' ? "  -> {$detail}" : '') . "\n";
        }
    }
}

$serverDir = dirname(__DIR__);
$php = PHP_BINARY;

// ---------------------------------------------------------------------------
// 1. Lint
// ---------------------------------------------------------------------------
echo "== php -l ==\n";
foreach (['backup_receiver.php', 'backup_config.example.php', 'tests/run_tests.php'] as $rel) {
    $out = [];
    $code = 1;
    exec(escapeshellarg($php) . ' -l ' . escapeshellarg($serverDir . DIRECTORY_SEPARATOR . $rel) . ' 2>&1', $out, $code);
    T::check($code === 0, "php -l {$rel}", implode(' ', $out));
}

// ---------------------------------------------------------------------------
// Helpers
// ---------------------------------------------------------------------------

/** Minimal ZIP writer (STORED entries) so the test does not need ext-zip. */
function make_zip(array $entries): string
{
    $local = '';
    $central = '';
    $count = 0;
    foreach ($entries as $name => $data) {
        $crc = crc32($data);
        $len = strlen($data);
        $nlen = strlen($name);
        $offset = strlen($local);
        $local .= pack('VvvvvvVVVvv', 0x04034b50, 20, 0, 0, 0, 0x21, $crc, $len, $len, $nlen, 0) . $name . $data;
        $central .= pack('VvvvvvvVVVvvvvvVV', 0x02014b50, 20, 20, 0, 0, 0, 0x21, $crc, $len, $len, $nlen, 0, 0, 0, 0, 0, $offset) . $name;
        $count++;
    }
    return $local . $central . pack('VvvvvVVv', 0x06054b50, 0, 0, $count, $count, strlen($central), strlen($local), 0);
}

function multipart(array $fields, ?array $file): array
{
    $b = '----vkTest' . bin2hex(random_bytes(8));
    $body = '';
    foreach ($fields as $k => $v) {
        $body .= "--{$b}\r\nContent-Disposition: form-data; name=\"{$k}\"\r\n\r\n{$v}\r\n";
    }
    if ($file !== null) {
        [$filename, $data, $type] = $file;
        $body .= "--{$b}\r\nContent-Disposition: form-data; name=\"file\"; filename=\"{$filename}\"\r\n"
            . "Content-Type: {$type}\r\n\r\n{$data}\r\n";
    }
    $body .= "--{$b}--\r\n";
    return [$body, "multipart/form-data; boundary={$b}"];
}

function http(string $method, string $url, array $headers = [], ?string $body = null): array
{
    $h = [];
    foreach ($headers as $k => $v) {
        $h[] = "{$k}: {$v}";
    }
    if ($body !== null) {
        $h[] = 'Content-Length: ' . strlen($body);
    }
    $opts = ['http' => [
        'method' => $method,
        'header' => implode("\r\n", $h),
        'ignore_errors' => true,
        'timeout' => 30,
        'protocol_version' => 1.0,
    ]];
    if ($body !== null) {
        $opts['http']['content'] = $body;
    }
    $raw = @file_get_contents($url, false, stream_context_create($opts));
    $respHeaders = function_exists('http_get_last_response_headers')
        ? (http_get_last_response_headers() ?? [])
        : ($http_response_header ?? []);
    $status = 0;
    if (isset($respHeaders[0]) && preg_match('#^HTTP/\S+\s+(\d{3})#', $respHeaders[0], $m)) {
        $status = (int) $m[1];
    }
    $json = is_string($raw) ? json_decode($raw, true) : null;
    return ['status' => $status, 'json' => is_array($json) ? $json : [], 'headers' => $respHeaders, 'raw' => (string) $raw];
}

function header_value(array $headers, string $name): ?string
{
    foreach ($headers as $line) {
        if (stripos($line, $name . ':') === 0) {
            return trim(substr($line, strlen($name) + 1));
        }
    }
    return null;
}

function write_config(string $path, array $cfg): void
{
    file_put_contents($path, "<?php\nreturn " . var_export($cfg, true) . ";\n");
}

/** @return array{0:resource,1:string} [process, receiver URL] */
function start_server(string $php, string $docroot, string $configPath)
{
    $sock = stream_socket_server('tcp://127.0.0.1:0', $errno, $errstr);
    if ($sock === false) {
        fwrite(STDERR, "cannot allocate a port: {$errstr}\n");
        exit(2);
    }
    $name = (string) stream_socket_get_name($sock, false);
    fclose($sock);
    $port = (int) substr($name, strrpos($name, ':') + 1);

    $cmd = [$php, '-d', 'upload_max_filesize=8M', '-d', 'post_max_size=10M', '-d', 'display_errors=0',
        '-S', "127.0.0.1:{$port}", '-t', $docroot];
    $env = getenv();
    $env['VRAJ_CONFIG'] = $configPath;
    $null = DIRECTORY_SEPARATOR === '\\' ? 'NUL' : '/dev/null';
    $proc = proc_open($cmd, [0 => ['pipe', 'r'], 1 => ['file', $null, 'w'], 2 => ['file', $null, 'w']], $pipes, $docroot, $env);
    if (!is_resource($proc)) {
        fwrite(STDERR, "cannot start php -S\n");
        exit(2);
    }
    for ($i = 0; $i < 100; $i++) {
        $c = @fsockopen('127.0.0.1', $port, $e, $s, 0.2);
        if ($c !== false) {
            fclose($c);
            break;
        }
        usleep(100000);
    }
    return [$proc, "http://127.0.0.1:{$port}/backup_receiver.php"];
}

function stop_server($proc): void
{
    if (is_resource($proc)) {
        proc_terminate($proc);
        proc_close($proc);
    }
}

function rrmdir(string $dir): void
{
    if (!is_dir($dir)) {
        return;
    }
    $it = new RecursiveIteratorIterator(
        new RecursiveDirectoryIterator($dir, FilesystemIterator::SKIP_DOTS),
        RecursiveIteratorIterator::CHILD_FIRST
    );
    foreach ($it as $f) {
        $f->isDir() ? @rmdir($f->getPathname()) : @unlink($f->getPathname());
    }
    @rmdir($dir);
}

// ---------------------------------------------------------------------------
// 2. Integration tests against php -S
// ---------------------------------------------------------------------------

$base = rtrim(sys_get_temp_dir(), '/\\') . DIRECTORY_SEPARATOR . 'vraj_rcv_test_' . bin2hex(random_bytes(4));
$store = $base . DIRECTORY_SEPARATOR . 'store';
mkdir($store, 0700, true);
$key = bin2hex(random_bytes(32)); // throw-away key, never written to the repo
$baseCfg = [
    'api_key' => $key,
    'store_dir' => $store,
    'mail_enabled' => false,
    'mail_to' => '',
    'mail_from' => '',
    'max_mb' => 1,
    'retention_days' => 365,
    'rate_limit_per_hour' => 1000,
    'require_https' => false,
    'timezone' => 'Asia/Kolkata',
];
$cfgA = $base . DIRECTORY_SEPARATOR . 'cfg_a.php';
write_config($cfgA, $baseCfg);

$procs = [];
register_shutdown_function(static function () use (&$procs, $base): void {
    foreach ($procs as $p) {
        stop_server($p);
    }
    rrmdir($base);
});

echo "\n== integration (php -S) ==\n";
[$procA, $url] = start_server($php, $serverDir, $cfgA);
$procs[] = $procA;
$auth = ['X-Vraj-Key' => $key, 'X-Vraj-Device' => 'test-device-1'];

$manifest = json_encode(['app' => 'vraj_kheti_hisab', 'version' => '1.0', 'dbVersion' => 1,
    'createdAt' => '2026-11-03T23:45:00+05:30', 'counts' => ['txn' => 42, 'farm' => 3]]);
$zip = make_zip(['manifest.json' => (string) $manifest, 'db/vraj_kheti.db' => str_repeat("SQLite format 3\0", 64)]);
$zipSha = hash('sha256', $zip);

$post = static function (array $fields, ?array $file, array $headers) use ($url): array {
    [$body, $ct] = multipart($fields, $file);
    return http('POST', $url, $headers + ['Content-Type' => $ct], $body);
};

// 405
$r = http('GET', $url);
T::check($r['status'] === 405 && ($r['json']['error'] ?? '') === 'method', 'GET without ping -> 405', $r['raw']);
$r = http('PUT', $url, $auth, 'x');
T::check($r['status'] === 405, 'PUT -> 405', $r['raw']);

// ping
$r = http('GET', $url . '?ping=1');
T::check($r['status'] === 401 && ($r['json']['error'] ?? '') === 'auth', 'ping without key -> 401', $r['raw']);
$r = http('GET', $url . '?ping=1', $auth);
T::check($r['status'] === 200 && ($r['json']['ok'] ?? false) === true, 'ping with key -> 200 ok', $r['raw']);

// 401
$r = $post(['date' => '2026-11-03', 'sha256' => $zipSha], ['vraj.zip', $zip, 'application/zip'], ['X-Vraj-Key' => str_repeat('0', 64)]);
T::check($r['status'] === 401 && $r['json'] === ['ok' => false, 'error' => 'auth'], 'POST wrong key -> 401 {"ok":false,"error":"auth"}', $r['raw']);
$r = $post(['date' => '2026-11-03', 'sha256' => $zipSha], ['vraj.zip', $zip, 'application/zip'], []);
T::check($r['status'] === 401, 'POST no key -> 401', $r['raw']);

// 400s
$r = $post(['date' => '2026-11-03', 'sha256' => str_repeat('a', 64)], ['vraj.zip', $zip, 'application/zip'], $auth);
T::check($r['status'] === 400 && ($r['json']['error'] ?? '') === 'hash', 'bad sha256 value -> 400 hash', $r['raw']);
T::check(($r['json']['received_sha256'] ?? '') === $zipSha, 'hash error echoes server-side sha256');
$r = $post(['date' => '2026-02-30', 'sha256' => $zipSha], ['vraj.zip', $zip, 'application/zip'], $auth);
T::check($r['status'] === 400 && ($r['json']['error'] ?? '') === 'date', 'impossible date -> 400 date', $r['raw']);
$r = $post(['date' => '../../etc/passwd', 'sha256' => $zipSha], ['vraj.zip', $zip, 'application/zip'], $auth);
T::check($r['status'] === 400 && ($r['json']['error'] ?? '') === 'date', 'path traversal date -> 400 date', $r['raw']);
$r = $post(['date' => '2026-11-03', 'sha256' => 'xyz'], ['vraj.zip', $zip, 'application/zip'], $auth);
T::check($r['status'] === 400 && ($r['json']['error'] ?? '') === 'sha256', 'malformed sha256 -> 400 sha256', $r['raw']);
$r = $post(['date' => '2026-11-03', 'sha256' => $zipSha], null, $auth);
T::check($r['status'] === 400 && ($r['json']['error'] ?? '') === 'file', 'missing file part -> 400 file', $r['raw']);
$txt = "<?php echo 'not a zip';";
$r = $post(['date' => '2026-11-03', 'sha256' => hash('sha256', $txt)], ['vraj.zip', $txt, 'application/zip'], $auth);
T::check($r['status'] === 400 && ($r['json']['error'] ?? '') === 'type', 'non-zip content -> 400 type', $r['raw']);

// 413 (max_mb = 1 in this config)
$big = make_zip(['photos/big.bin' => random_bytes(1536 * 1024)]);
$r = $post(['date' => '2026-11-03', 'sha256' => hash('sha256', $big)], ['vraj.zip', $big, 'application/zip'], $auth);
T::check($r['status'] === 413 && ($r['json']['error'] ?? '') === 'size', '1.5 MB zip over 1 MB limit -> 413 size', $r['raw']);
$huge = make_zip(['photos/huge.bin' => random_bytes(3 * 1024 * 1024)]);
$r = $post(['date' => '2026-11-03', 'sha256' => hash('sha256', $huge)], ['vraj.zip', $huge, 'application/zip'], $auth);
T::check($r['status'] === 413 && ($r['json']['error'] ?? '') === 'size', '3 MB request rejected by Content-Length precheck -> 413', $r['raw']);
T::check((glob($store . DIRECTORY_SEPARATOR . 'vraj_*.zip') ?: []) === [], 'nothing stored after rejected uploads');

// Retention fixtures
$old = $store . DIRECTORY_SEPARATOR . 'vraj_20240101.zip';
$recent = $store . DIRECTORY_SEPARATOR . 'vraj_20261020.zip';
$other = $store . DIRECTORY_SEPARATOR . 'keep_me.txt';
foreach ([$old, $recent, $other] as $p) {
    file_put_contents($p, 'x');
}
touch($old, time() - 400 * 86400);
touch($recent, time() - 10 * 86400);
touch($other, time() - 400 * 86400);

// 200
$r = $post(['date' => '2026-11-03', 'sha256' => $zipSha], ['upload.zip', $zip, 'application/zip'], $auth);
$j = $r['json'];
T::check($r['status'] === 200 && ($j['ok'] ?? false) === true, 'valid upload -> 200 ok', $r['raw']);
T::check(($j['stored'] ?? '') === 'vraj_20261103.zip', 'stored name is vraj_20261103.zip (from date, not filename)', $r['raw']);
T::check(($j['bytes'] ?? -1) === strlen($zip), 'bytes echoes the stored size');
T::check(($j['sha256'] ?? '') === $zipSha, 'sha256 integrity echo');
T::check(($j['manifest'] ?? null) === class_exists('ZipArchive'), 'manifest read when ext-zip is present');
$final = $store . DIRECTORY_SEPARATOR . 'vraj_20261103.zip';
T::check(is_file($final) && hash_file('sha256', $final) === $zipSha, 'stored file is byte-identical');
T::check(($j['pruned'] ?? -1) === 1 && !is_file($old), 'zip older than 365 days pruned');
T::check(is_file($recent), '10-day-old zip kept');
T::check(is_file($other), 'non-backup file never touched by pruning');
$ht = $store . DIRECTORY_SEPARATOR . '.htaccess';
T::check(is_file($ht) && str_contains((string) file_get_contents($ht), 'Require all denied'), 'store dir .htaccess written (Require all denied)');
T::check(($j['mailed'] ?? null) === false, 'mail disabled in test config -> mailed=false');

// Overwrite same date
$zip2 = make_zip(['manifest.json' => (string) $manifest, 'db/vraj_kheti.db' => 'second run']);
$r = $post(['date' => '2026-11-03', 'sha256' => hash('sha256', $zip2)], ['upload.zip', $zip2, 'application/zip'], $auth);
T::check($r['status'] === 200 && hash_file('sha256', $final) === hash('sha256', $zip2), 'same date re-upload overwrites', $r['raw']);
T::check((glob($store . DIRECTORY_SEPARATOR . '.incoming_*.part') ?: []) === [], 'no temp .part files left');

$log = (string) @file_get_contents($store . DIRECTORY_SEPARATOR . 'receiver.log');
T::check($log !== '' && !str_contains($log, $key), 'receiver.log written and never contains the API key');
stop_server($procA);

// https + rate limit (limit 3, https required)
$cfgB = $base . DIRECTORY_SEPARATOR . 'cfg_b.php';
$storeB = $base . DIRECTORY_SEPARATOR . 'store_b';
write_config($cfgB, ['store_dir' => $storeB, 'rate_limit_per_hour' => 3, 'require_https' => true] + $baseCfg);
[$procB, $urlB] = start_server($php, $serverDir, $cfgB);
$procs[] = $procB;
$codes = [];
for ($i = 0; $i < 3; $i++) {
    $codes[] = http('GET', $urlB . '?ping=1', $auth)['status'];
}
T::check($codes === [403, 403, 403], 'plain http with require_https -> 403', implode(',', $codes));
$r = http('GET', $urlB . '?ping=1', $auth);
T::check($r['status'] === 429 && ($r['json']['error'] ?? '') === 'rate', '4th request in an hour -> 429 rate', $r['raw']);
$ra = header_value($r['headers'], 'Retry-After');
T::check($ra !== null && (int) $ra > 0 && (int) $ra <= 3600, 'Retry-After header present', (string) $ra);
stop_server($procB);

// Missing config -> 500 config
[$procC, $urlC] = start_server($php, $serverDir, $base . DIRECTORY_SEPARATOR . 'does_not_exist.php');
$procs[] = $procC;
$r = http('GET', $urlC . '?ping=1', $auth);
T::check($r['status'] === 500 && ($r['json']['error'] ?? '') === 'config', 'missing config -> 500 config', $r['raw']);
stop_server($procC);

// Placeholder key -> 500 config
$cfgD = $base . DIRECTORY_SEPARATOR . 'cfg_d.php';
write_config($cfgD, ['api_key' => 'REPLACE_WITH_64_HEX_CHARACTERS_REPLACE_WITH_64_HEX'] + $baseCfg);
[$procD, $urlD] = start_server($php, $serverDir, $cfgD);
$procs[] = $procD;
$r = http('GET', $urlD . '?ping=1', ['X-Vraj-Key' => 'REPLACE_WITH_64_HEX_CHARACTERS_REPLACE_WITH_64_HEX']);
T::check($r['status'] === 500 && ($r['json']['error'] ?? '') === 'config', 'placeholder api_key refused -> 500 config', $r['raw']);
stop_server($procD);

echo "\n" . T::$pass . ' passed, ' . T::$fail . " failed\n";
exit(T::$fail === 0 ? 0 : 1);
