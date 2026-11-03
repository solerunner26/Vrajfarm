<?php
/**
 * Vraj Kheti Hisab — backup receiver configuration TEMPLATE.
 *
 * 1. Copy this file to a place OUTSIDE public_html, for example:
 *        /home/<cpanel-user>/vraj_config.php
 *    (backup_receiver.php looks there by default when it is installed at
 *     /home/<cpanel-user>/public_html/vraj/backup_receiver.php)
 * 2. Replace every placeholder below.
 * 3. In cPanel File Manager set its permissions to 0600 (or 0640).
 *
 * NEVER commit the real file. Only this example (with placeholders) belongs in git.
 * The script refuses to run while api_key still contains "REPLACE" or is shorter
 * than 32 characters, and refuses a config file that sits inside the web root.
 */

return [
    // Shared secret. Generate one with: openssl rand -hex 32
    // (or cPanel Terminal: php -r 'echo bin2hex(random_bytes(32)), PHP_EOL;')
    // Enter the SAME value in the app: Settings -> રાત્રિ બેકઅપ -> ગુપ્ત કી.
    'api_key' => 'REPLACE_WITH_64_HEX_CHARACTERS',

    // Folder where the ZIPs are kept. Must be OUTSIDE public_html.
    // It is created automatically (permissions 0700) on the first upload.
    'store_dir' => '/home/REPLACE_CPANEL_USER/vraj_backups',

    // Notification email (one address). The ZIP is never attached.
    'mail_enabled' => true,
    'mail_to'      => 'owner@example.com',
    // Use a mailbox on YOUR hosted domain (create it in cPanel -> Email Accounts);
    // GoDaddy often drops mail whose From is a gmail.com/yahoo.com address.
    'mail_from'    => 'backup@example.com',

    // Limits.
    'max_mb'              => 200,  // keep in sync with .user.ini upload_max_filesize / post_max_size
    'retention_days'      => 365,  // ZIPs older than this (by file time) are deleted after each upload
    'rate_limit_per_hour' => 10,   // requests per IP per hour (pings and failed attempts count)

    // Reject plain http:// (the key would travel unencrypted). Leave true in production.
    'require_https' => true,

    'timezone' => 'Asia/Kolkata',

    // Optional: send through SMTP with PHPMailer instead of PHP mail().
    // Install PHPMailer OUTSIDE public_html (cPanel Terminal):
    //     cd ~ && composer require phpmailer/phpmailer
    // then point 'autoload' at /home/<cpanel-user>/vendor/autoload.php.
    // If SMTP fails the script falls back to mail().
    'smtp' => [
        'enabled'  => false,
        'autoload' => '/home/REPLACE_CPANEL_USER/vendor/autoload.php',
        'host'     => 'mail.example.com',  // GoDaddy cPanel: usually mail.<your-domain>
        'port'     => 465,
        'secure'   => 'ssl',               // 'ssl' for 465, 'tls' for 587
        'user'     => 'backup@example.com',
        'pass'     => 'REPLACE_WITH_MAILBOX_PASSWORD',
    ],
];
