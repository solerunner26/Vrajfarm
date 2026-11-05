package com.vraj.khetihisab.backup

import android.content.Context
import android.content.SharedPreferences
import android.util.Base64
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader
import java.io.OutputStream
import java.net.InetSocketAddress
import javax.net.ssl.HttpsURLConnection
import javax.net.ssl.SSLSocket
import javax.net.ssl.SSLSocketFactory

/**
 * One-tap backup e-mail (owner request 2026-11-05: "send automatically on a single click, not a draft").
 *
 * Gmail does not accept a plain password from apps; the owner creates a 16-letter **App Password** once
 * (Google Account › Security › 2-Step Verification › App passwords). The address and that password are
 * kept in EncryptedSharedPreferences (Android Keystore AES-256) and are never logged, shown or backed up.
 * Mail goes straight to smtp.gmail.com:465 over TLS with the hostname checked; nothing passes through
 * any other server.
 */
class MailSettings(private val prefs: SharedPreferences) {
    val gmail: String get() = prefs.getString(K_USER, "").orEmpty()
    val hasPassword: Boolean get() = !prefs.getString(K_PASS, null).isNullOrBlank()
    internal fun password(): String? = prefs.getString(K_PASS, null)?.takeIf { it.isNotBlank() }
    var to: String
        get() = prefs.getString(K_TO, null)?.takeIf { it.isNotBlank() } ?: gmail
        set(v) { prefs.edit().putString(K_TO, v.trim()).apply() }
    var nightly: Boolean
        get() = prefs.getBoolean(K_NIGHTLY, false)
        set(v) { prefs.edit().putBoolean(K_NIGHTLY, v).apply() }

    val configured: Boolean get() = isGmail(gmail) && hasPassword

    fun save(gmail: String, appPassword: String?) {
        prefs.edit().putString(K_USER, gmail.trim().lowercase()).apply {
            if (appPassword != null) putString(K_PASS, appPassword.replace(" ", ""))
        }.apply()
    }

    fun clear() = prefs.edit().clear().apply()

    companion object {
        private const val NAME = "vk_mail_secure"
        private const val K_USER = "gmail"
        private const val K_PASS = "app_password"
        private const val K_TO = "to"
        private const val K_NIGHTLY = "nightly"

        fun isGmail(s: String): Boolean = Regex("^[A-Za-z0-9._%+-]+@(gmail|googlemail)\\.com$").matches(s.trim())

        /** 16 letters (Google shows them in 4 groups of 4; spaces are ignored). */
        fun isAppPassword(s: String): Boolean = s.replace(" ", "").let { it.length == 16 && it.all(Char::isLetter) }

        fun create(context: Context): MailSettings {
            val app = context.applicationContext
            val key = MasterKey.Builder(app).setKeyScheme(MasterKey.KeyScheme.AES256_GCM).build()
            val prefs = EncryptedSharedPreferences.create(
                app, NAME, key,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
            )
            return MailSettings(prefs)
        }
    }
}

object GmailSender {
    sealed interface Result {
        data object Sent : Result
        /** Wrong address / App Password (535), or 2-Step Verification not on. */
        data object AuthFailed : Result
        data object NoNetwork : Result
        data class Failed(val code: String) : Result
    }

    private const val HOST = "smtp.gmail.com"
    private const val PORT = 465

    fun send(user: String, appPassword: String, to: String, subject: String, body: String, attachment: File?): Result {
        val socket = try {
            (SSLSocketFactory.getDefault().createSocket() as SSLSocket).apply {
                connect(InetSocketAddress(HOST, PORT), 20_000)
                soTimeout = 60_000
                startHandshake()
            }
        } catch (e: java.io.IOException) {
            return Result.NoNetwork
        }
        socket.use { s ->
            // SSLSocket does not check the hostname on its own.
            if (!HttpsURLConnection.getDefaultHostnameVerifier().verify(HOST, s.session)) return Result.Failed("host")
            val input = BufferedReader(InputStreamReader(s.inputStream, Charsets.US_ASCII))
            val out = s.outputStream
            fun reply(): Pair<Int, String> {
                var line: String
                val all = StringBuilder()
                do {
                    line = input.readLine() ?: return -1 to all.toString()
                    all.append(line).append('\n')
                } while (line.length >= 4 && line[3] == '-')
                return (line.take(3).toIntOrNull() ?: -1) to all.toString()
            }
            fun cmd(c: String): Int { out.writeLine(c); return reply().first }

            try {
                if (reply().first != 220) return Result.Failed("greeting")
                if (cmd("EHLO vraj-kheti") != 250) return Result.Failed("ehlo")
                if (cmd("AUTH LOGIN") != 334) return Result.Failed("auth")
                if (cmd(b64(user)) != 334) return Result.AuthFailed
                if (cmd(b64(appPassword)) != 235) return Result.AuthFailed
                if (cmd("MAIL FROM:<$user>") != 250) return Result.Failed("from")
                if (cmd("RCPT TO:<${to.trim()}>") !in setOf(250, 251)) return Result.Failed("rcpt")
                if (cmd("DATA") != 354) return Result.Failed("data")
                val buf = java.io.BufferedOutputStream(out, 64 * 1024)
                writeMessage(buf, user, to.trim(), subject, body, attachment)
                buf.flush()
                out.writeLine(".")
                if (reply().first != 250) return Result.Failed("sent")
                runCatching { cmd("QUIT") }
                return Result.Sent
            } catch (e: java.io.IOException) {
                return Result.NoNetwork
            }
        }
    }

    private fun writeMessage(out: OutputStream, from: String, to: String, subject: String, body: String, file: File?) {
        val boundary = "vk-" + System.currentTimeMillis()
        out.writeLine("From: =?UTF-8?B?${b64("વ્રજ ખેતી હિસાબ")}?= <$from>")
        out.writeLine("To: <$to>")
        out.writeLine("Subject: =?UTF-8?B?${b64(subject)}?=")
        out.writeLine("MIME-Version: 1.0")
        out.writeLine("Content-Type: multipart/mixed; boundary=\"$boundary\"")
        out.writeLine("")
        out.writeLine("--$boundary")
        out.writeLine("Content-Type: text/plain; charset=UTF-8")
        out.writeLine("Content-Transfer-Encoding: base64")
        out.writeLine("")
        wrapped(Base64.encodeToString(body.toByteArray(Charsets.UTF_8), Base64.NO_WRAP)).forEach { out.writeLine(it) }
        if (file != null) {
            out.writeLine("--$boundary")
            out.writeLine("Content-Type: application/json; name=\"${file.name}\"")
            out.writeLine("Content-Transfer-Encoding: base64")
            out.writeLine("Content-Disposition: attachment; filename=\"${file.name}\"")
            out.writeLine("")
            wrapped(Base64.encodeToString(file.readBytes(), Base64.NO_WRAP)).forEach { out.writeLine(it) }
        }
        out.writeLine("--$boundary--")
    }

    /** RFC 2045: base64 lines of at most 76 characters (none can start with '.'). */
    private fun wrapped(b: String): List<String> = b.chunked(76)

    private fun b64(s: String) = Base64.encodeToString(s.toByteArray(Charsets.UTF_8), Base64.NO_WRAP)

    private fun OutputStream.writeLine(s: String) {
        write((s + "\r\n").toByteArray(Charsets.UTF_8))
        flush()
    }
}
