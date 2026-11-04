package com.vraj.khetihisab.backup

import java.io.File
import java.io.FileOutputStream
import java.security.DigestOutputStream
import java.security.MessageDigest
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.zip.Deflater
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

/**
 * Builds the nightly snapshot ZIP (doc 15 step 3). Pure java.io — JVM-tested.
 *
 * Layout:
 * ```
 * db/vraj_kheti.db (+ -wal / -shm when present)
 * bill_photos/…  people_photos/…  borrower_photos/…  reminder_photos/…
 * reports/daily_<yyyy-MM-dd>.pdf
 * files.sha256      "<sha256>  <entry>" per line, every entry above (restore verification)
 * manifest.json     last; small (the server reads at most 64 KB of it for the email counts)
 * ```
 */
object BackupZipBuilder {

    const val MANIFEST = "manifest.json"
    const val CHECKSUMS = "files.sha256"
    const val FORMAT_VERSION = "1.0"
    const val APP_ID = "vraj_kheti_hisab"

    /** Photo folders under filesDir that every backup carries (CLAUDE.md). */
    val PHOTO_DIRS = listOf("bill_photos", "people_photos", "borrower_photos", "reminder_photos")

    data class Input(
        /** Main DB file first, then -wal / -shm if they exist. Missing files are skipped. */
        val dbFiles: List<File>,
        /** zip folder name -> local directory (missing directories are skipped). */
        val photoDirs: Map<String, File>,
        val dailyPdf: File?,
        /** e.g. "reports/daily_2026-11-03.pdf" */
        val dailyPdfEntry: String?,
        val dbVersion: Int,
        val appVersion: String,
        /** table -> row count, shown in the server email. */
        val counts: Map<String, Long>,
        val createdAt: ZonedDateTime,
    )

    data class Output(
        val zip: File,
        /** SHA-256 of the whole ZIP (the `sha256` form part). */
        val sha256: String,
        val bytes: Long,
        /** entry name -> SHA-256 of its uncompressed content. */
        val entryHashes: Map<String, String>,
        val manifestJson: String,
    )

    fun build(input: Input, out: File): Output {
        out.parentFile?.mkdirs()
        val tmp = File(out.parentFile, out.name + ".part")
        val hashes = linkedMapOf<String, String>()
        val zipDigest = MessageDigest.getInstance("SHA-256")
        val manifest: String
        DigestOutputStream(FileOutputStream(tmp), zipDigest).use { dos ->
            ZipOutputStream(dos.buffered()).use { zip ->
                zip.setLevel(Deflater.DEFAULT_COMPRESSION)
                zip.setMethod(ZipOutputStream.DEFLATED)

                input.dbFiles.filter { it.isFile }.forEach { f -> hashes["db/${f.name}"] = putFile(zip, "db/${f.name}", f) }

                for ((folder, dir) in input.photoDirs) {
                    if (!dir.isDirectory) continue
                    dir.walkTopDown()
                        .filter { it.isFile && !it.name.endsWith(".part") }
                        .sortedBy { it.relativeTo(dir).invariantSeparatorsPath }
                        .forEach { f ->
                            val name = "$folder/${f.relativeTo(dir).invariantSeparatorsPath}"
                            hashes[name] = putFile(zip, name, f)
                        }
                }

                if (input.dailyPdf != null && input.dailyPdf.isFile && input.dailyPdfEntry != null) {
                    hashes[input.dailyPdfEntry] = putFile(zip, input.dailyPdfEntry, input.dailyPdf)
                }

                val checksums = hashes.entries.joinToString("") { "${it.value}  ${it.key}\n" }
                putBytes(zip, CHECKSUMS, checksums.toByteArray(Charsets.UTF_8))

                manifest = manifestJson(input, hashes)
                putBytes(zip, MANIFEST, manifest.toByteArray(Charsets.UTF_8))
            }
        }
        if (out.exists()) out.delete()
        if (!tmp.renameTo(out)) {
            tmp.copyTo(out, overwrite = true)
            tmp.delete()
        }
        return Output(out, Sha256.hex(zipDigest.digest()), out.length(), hashes, manifest)
    }

    private fun putFile(zip: ZipOutputStream, name: String, f: File): String {
        val e = ZipEntry(name).apply { time = f.lastModified() }
        zip.putNextEntry(e)
        val md = MessageDigest.getInstance("SHA-256")
        f.inputStream().use { input ->
            val buf = ByteArray(64 * 1024)
            while (true) {
                val n = input.read(buf)
                if (n < 0) break
                md.update(buf, 0, n)
                zip.write(buf, 0, n)
            }
        }
        zip.closeEntry()
        return Sha256.hex(md.digest())
    }

    private fun putBytes(zip: ZipOutputStream, name: String, bytes: ByteArray) {
        zip.putNextEntry(ZipEntry(name))
        zip.write(bytes)
        zip.closeEntry()
    }

    /** Manifest: hashes only for db/ and reports/ entries (photo hashes live in [CHECKSUMS]). */
    internal fun manifestJson(input: Input, hashes: Map<String, String>): String {
        val small = hashes.filterKeys { it.startsWith("db/") || it.startsWith("reports/") }
        val photoCount = hashes.keys.count { k -> PHOTO_DIRS.any { k.startsWith("$it/") } }
        return buildString {
            append('{')
            append("\"app\":").append(Json.str(APP_ID)).append(',')
            append("\"version\":").append(Json.str(FORMAT_VERSION)).append(',')
            append("\"appVersion\":").append(Json.str(input.appVersion)).append(',')
            append("\"dbVersion\":").append(input.dbVersion).append(',')
            append("\"createdAt\":").append(Json.str(input.createdAt.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME))).append(',')
            append("\"counts\":").append(Json.obj(input.counts.mapValues { it.value.toString() }, raw = true)).append(',')
            append("\"photos\":").append(photoCount).append(',')
            append("\"files\":").append(hashes.size).append(',')
            append("\"sha256\":").append(Json.obj(small))
            append('}')
        }
    }
}

/** Minimal JSON writer/reader helpers (org.json is a stub in JVM unit tests). */
internal object Json {
    fun str(s: String): String = buildString {
        append('"')
        for (c in s) {
            when (c) {
                '"' -> append("\\\"")
                '\\' -> append("\\\\")
                '\n' -> append("\\n")
                '\r' -> append("\\r")
                '\t' -> append("\\t")
                else -> if (c < ' ') append(String.format("\\u%04x", c.code)) else append(c)
            }
        }
        append('"')
    }

    fun obj(map: Map<String, String>, raw: Boolean = false): String =
        map.entries.joinToString(",", "{", "}") { (k, v) -> "${str(k)}:${if (raw) v else str(v)}" }

    /** Top-level string field, e.g. `"error":"auth"`. Good enough for the server's flat responses. */
    fun stringField(json: String, name: String): String? =
        Regex("\"${Regex.escape(name)}\"\\s*:\\s*\"((?:[^\"\\\\]|\\\\.)*)\"").find(json)?.groupValues?.get(1)

    fun longField(json: String, name: String): Long? =
        Regex("\"${Regex.escape(name)}\"\\s*:\\s*(-?\\d+)").find(json)?.groupValues?.get(1)?.toLongOrNull()

    fun boolField(json: String, name: String): Boolean? =
        Regex("\"${Regex.escape(name)}\"\\s*:\\s*(true|false)").find(json)?.groupValues?.get(1)?.toBooleanStrictOrNull()
}
