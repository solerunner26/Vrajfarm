package com.vraj.khetihisab.update

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageInstaller
import android.net.Uri
import android.os.Build
import android.provider.Settings
import com.vraj.khetihisab.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.io.File
import java.io.RandomAccessFile
import java.security.MessageDigest
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicLong

/**
 * Self-update straight from GitHub Releases (owner request 2026-11-04: "one button, full speed").
 *
 * Every release carries `update.json` next to the APK (tools/release.ps1 writes it). The app reads
 * `releases/latest/download/update.json` — a plain CDN file, no GitHub API rate limit — and when its
 * `versionCode` is higher than ours shows "હમણાં અપડેટ કરો". The APK is fetched in [PARTS] parallel
 * byte ranges (much faster on Indian mobile networks than one stream), checked against `sha256`, and
 * handed to the system PackageInstaller. Android itself asks the user once to allow installs from this
 * app, and shows its own "Update?" confirm; that cannot be skipped by any normal app.
 */
data class UpdateInfo(
    val versionCode: Int,
    val versionName: String,
    val apkUrl: String,
    val sha256: String?,
    val size: Long,
    val notes: String,
)

object AppUpdater {
    const val REPO = "solerunner26/Vrajfarm"
    const val LATEST_JSON = "https://github.com/$REPO/releases/latest/download/update.json"
    private const val PARTS = 4
    private const val MIN_PART_BYTES = 512L * 1024

    private val http by lazy {
        OkHttpClient.Builder()
            .connectTimeout(20, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .followRedirects(true)
            .followSslRedirects(true)
            .build()
    }

    fun parse(json: String): UpdateInfo? = runCatching {
        val o = JSONObject(json)
        UpdateInfo(
            versionCode = o.getInt("versionCode"),
            versionName = o.optString("versionName"),
            apkUrl = o.getString("apk"),
            sha256 = o.optString("sha256").takeIf { it.length == 64 },
            size = o.optLong("size", -1L),
            notes = o.optString("notes"),
        )
    }.getOrNull()

    /** A newer release, or null (offline, no release, or already up to date). */
    suspend fun check(currentCode: Int = BuildConfig.VERSION_CODE): UpdateInfo? = withContext(Dispatchers.IO) {
        runCatching {
            val req = Request.Builder().url(LATEST_JSON).header("Cache-Control", "no-cache").build()
            http.newCall(req).execute().use { r -> if (!r.isSuccessful) null else r.body?.string()?.let(::parse) }
        }.getOrNull()?.takeIf { it.versionCode > currentCode && it.apkUrl.startsWith("https://github.com/$REPO/") }
    }

    /** Downloads the APK into cache/update/. [progress] gets 0f..1f. Returns null on failure or bad checksum. */
    suspend fun download(context: Context, info: UpdateInfo, progress: (Float) -> Unit): File? = withContext(Dispatchers.IO) {
        val dir = File(context.cacheDir, "update").apply { deleteRecursively(); mkdirs() }
        val out = File(dir, "VrajKhetiHisab-${info.versionCode}.apk")
        val ok = runCatching {
            val size = if (info.size > 0) info.size else headSize(info.apkUrl)
            if (size > PARTS * MIN_PART_BYTES) parallel(info.apkUrl, size, out, progress) else single(info.apkUrl, out, size, progress)
        }.getOrDefault(false)
        if (!ok) return@withContext null
        val sha = info.sha256
        if (sha != null && !sha.equals(sha256(out), ignoreCase = true)) {
            out.delete()
            return@withContext null
        }
        progress(1f)
        out
    }

    private fun headSize(url: String): Long = runCatching {
        http.newCall(Request.Builder().url(url).head().build()).execute().use { it.header("Content-Length")?.toLongOrNull() ?: -1L }
    }.getOrDefault(-1L)

    private fun single(url: String, out: File, size: Long, progress: (Float) -> Unit): Boolean {
        http.newCall(Request.Builder().url(url).build()).execute().use { r ->
            if (!r.isSuccessful) return false
            val body = r.body ?: return false
            val total = if (size > 0) size else body.contentLength()
            var done = 0L
            out.outputStream().use { o ->
                body.byteStream().use { i ->
                    val buf = ByteArray(128 * 1024)
                    while (true) {
                        val n = i.read(buf)
                        if (n < 0) break
                        o.write(buf, 0, n)
                        done += n
                        if (total > 0) progress(done.toFloat() / total)
                    }
                }
            }
            return total <= 0 || done == total
        }
    }

    /** [PARTS] Range requests written into one pre-sized file; falls back to one stream if ranges are refused. */
    private suspend fun parallel(url: String, size: Long, out: File, progress: (Float) -> Unit): Boolean = coroutineScope {
        RandomAccessFile(out, "rw").use { it.setLength(size) }
        val done = AtomicLong(0)
        val part = size / PARTS
        val jobs = (0 until PARTS).map { i ->
            val from = i * part
            val to = if (i == PARTS - 1) size - 1 else from + part - 1
            async(Dispatchers.IO) {
                val req = Request.Builder().url(url).header("Range", "bytes=$from-$to").build()
                http.newCall(req).execute().use { r ->
                    if (r.code != 206) return@async false
                    val body = r.body ?: return@async false
                    RandomAccessFile(out, "rw").use { raf ->
                        raf.seek(from)
                        body.byteStream().use { input ->
                            val buf = ByteArray(128 * 1024)
                            var pos = from
                            while (true) {
                                val n = input.read(buf)
                                if (n < 0) break
                                raf.write(buf, 0, n)
                                pos += n
                                progress(done.addAndGet(n.toLong()).toFloat() / size)
                            }
                            pos == to + 1
                        }
                    }
                }
            }
        }
        val all = jobs.awaitAll().all { it }
        if (all) true else single(url, out, size, progress)
    }

    fun sha256(f: File): String {
        val md = MessageDigest.getInstance("SHA-256")
        f.inputStream().use { i ->
            val buf = ByteArray(256 * 1024)
            while (true) {
                val n = i.read(buf)
                if (n < 0) break
                md.update(buf, 0, n)
            }
        }
        return md.digest().joinToString("") { "%02x".format(it) }
    }

    /** Android's one-time "allow installs from this app" switch. */
    fun canInstall(context: Context): Boolean = context.packageManager.canRequestPackageInstalls()

    fun allowInstallsIntent(context: Context): Intent =
        Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, Uri.parse("package:${context.packageName}"))
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

    /** Streams the APK into a PackageInstaller session; the system then shows its confirm screen. */
    fun install(context: Context, apk: File) {
        val pi = context.packageManager.packageInstaller
        val params = PackageInstaller.SessionParams(PackageInstaller.SessionParams.MODE_FULL_INSTALL).apply {
            setAppPackageName(context.packageName)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                // Lets Android skip the confirm when this app installed itself last time.
                setRequireUserAction(PackageInstaller.SessionParams.USER_ACTION_NOT_REQUIRED)
            }
        }
        val id = pi.createSession(params)
        pi.openSession(id).use { s ->
            s.openWrite("base.apk", 0, apk.length()).use { o -> apk.inputStream().use { it.copyTo(o) }; s.fsync(o) }
            val cb = PendingIntent.getBroadcast(
                context,
                id,
                Intent(context, UpdateInstallReceiver::class.java),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE,
            )
            s.commit(cb.intentSender)
        }
    }
}

/** Shows the system confirm screen when the installer asks for it. */
class UpdateInstallReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        when (intent.getIntExtra(PackageInstaller.EXTRA_STATUS, PackageInstaller.STATUS_FAILURE)) {
            PackageInstaller.STATUS_PENDING_USER_ACTION -> {
                @Suppress("DEPRECATION")
                val confirm = if (Build.VERSION.SDK_INT >= 33) {
                    intent.getParcelableExtra(Intent.EXTRA_INTENT, Intent::class.java)
                } else {
                    intent.getParcelableExtra(Intent.EXTRA_INTENT)
                }
                // Intent-redirection guard (android/skills security/android-intent-security): only follow a
                // confirm screen that belongs to a system app, and never forward URI grants.
                val target = confirm ?: return
                target.removeFlags(
                    Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION or
                        Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION or Intent.FLAG_GRANT_PREFIX_URI_PERMISSION,
                )
                val info = target.resolveActivityInfo(context.packageManager, 0) ?: return
                val system = (info.applicationInfo.flags and android.content.pm.ApplicationInfo.FLAG_SYSTEM) != 0
                if (!system) return
                context.startActivity(target.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            }
            else -> Unit
        }
    }
}
