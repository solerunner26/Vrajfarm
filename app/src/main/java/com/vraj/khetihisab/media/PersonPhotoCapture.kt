package com.vraj.khetihisab.media

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Typeface
import android.media.ExifInterface
import android.net.Uri
import android.os.Build
import androidx.core.content.FileProvider
import androidx.core.content.res.ResourcesCompat
import com.vraj.khetihisab.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale
import java.util.UUID
import kotlin.math.max
import kotlin.math.roundToInt

/**
 * Person / borrower photo pipeline (doc 11 "Borrower photo"):
 * `TakePicture` into a FileProvider tmp file → decode (EXIF-rotated) → longest side 1024 →
 * timestamp strip "03-11-2026 · 10:42 AM" (white on 50% black, Noto Sans Gujarati) →
 * WebP q75, stepped down until ≤ 120 KB → `filesDir/<folder>/<uuid>.webp`.
 *
 * The composable wrapper with camera permission handling is [rememberPersonPhotoCapture].
 */
object PersonPhotoCapture {

    const val FOLDER_BORROWERS = "borrower_photos"
    const val FOLDER_PEOPLE = "people_photos"
    private const val TMP = "tmp"

    const val MAX_SIDE = 1024
    const val MAX_BYTES = 120 * 1024
    const val START_QUALITY = 75
    private const val MIN_QUALITY = 30

    private val STAMP: DateTimeFormatter = DateTimeFormatter.ofPattern("dd-MM-yyyy · hh:mm a", Locale.ENGLISH)

    /** "03-11-2026 · 10:42 AM" */
    fun timestampText(at: LocalDateTime): String = at.format(STAMP)

    /** FileProvider authority, declared in the manifest as `${applicationId}.files`. */
    fun authority(context: Context): String = "${context.packageName}.files"

    /** A fresh tmp file + its content Uri for `ActivityResultContracts.TakePicture`. */
    fun newCaptureTarget(context: Context): Pair<File, Uri> {
        val dir = File(context.filesDir, TMP).apply { mkdirs() }
        val f = File(dir, "cap_${UUID.randomUUID()}.jpg")
        return f to FileProvider.getUriForFile(context, authority(context), f)
    }

    /** Target size (w, h) so that the longest side is at most [maxSide]; never upscales. */
    fun scaledSize(width: Int, height: Int, maxSide: Int = MAX_SIDE): Pair<Int, Int> {
        val longest = max(width, height)
        if (longest <= maxSide || longest <= 0) return width to height
        val s = maxSide.toDouble() / longest
        return max(1, (width * s).roundToInt()) to max(1, (height * s).roundToInt())
    }

    /** Power-of-two decode sample size that keeps the longest side ≥ [maxSide]. */
    fun sampleSize(width: Int, height: Int, maxSide: Int = MAX_SIDE): Int {
        var s = 1
        while (max(width, height) / (s * 2) >= maxSide) s *= 2
        return s
    }

    /**
     * Processes [source] (the camera tmp file) into `filesDir/[folder]/<uuid>.webp` and deletes the
     * source. Returns the absolute path, or null when the image could not be decoded (cancelled shot).
     */
    suspend fun process(
        context: Context,
        source: File,
        folder: String = FOLDER_BORROWERS,
        takenAt: LocalDateTime = LocalDateTime.now(),
    ): String? = withContext(Dispatchers.IO) {
        try {
            if (!source.exists() || source.length() == 0L) return@withContext null
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeFile(source.path, bounds)
            if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return@withContext null
            val opts = BitmapFactory.Options().apply { inSampleSize = sampleSize(bounds.outWidth, bounds.outHeight) }
            val decoded = BitmapFactory.decodeFile(source.path, opts) ?: return@withContext null
            val rotated = rotate(decoded, exifDegrees(source))
            val (w, h) = scaledSize(rotated.width, rotated.height)
            val scaled = if (w == rotated.width && h == rotated.height) rotated else Bitmap.createScaledBitmap(rotated, w, h, true)
            val stamped = scaled.copy(Bitmap.Config.ARGB_8888, true)
            drawTimestamp(context, stamped, timestampText(takenAt))

            val bytes = compressWebp(stamped)
            val dir = File(context.filesDir, folder).apply { mkdirs() }
            val out = File(dir, "${UUID.randomUUID()}.webp")
            out.writeBytes(bytes)
            listOf(decoded, rotated, scaled, stamped).distinct().forEach { if (!it.isRecycled) it.recycle() }
            out.absolutePath
        } finally {
            source.delete()
        }
    }

    /** Deletes a stored photo (retake / discarded form). Only touches files under filesDir. */
    fun delete(context: Context, path: String?) {
        if (path == null) return
        val f = File(path)
        if (f.canonicalPath.startsWith(context.filesDir.canonicalPath)) f.delete()
    }

    @Suppress("DEPRECATION")
    private fun compressWebp(bmp: Bitmap): ByteArray {
        val format = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) Bitmap.CompressFormat.WEBP_LOSSY else Bitmap.CompressFormat.WEBP
        var q = START_QUALITY
        var bytes: ByteArray
        do {
            val bos = ByteArrayOutputStream()
            bmp.compress(format, q, bos)
            bytes = bos.toByteArray()
            q -= 10
        } while (bytes.size > MAX_BYTES && q >= MIN_QUALITY)
        return bytes
    }

    private fun drawTimestamp(context: Context, bmp: Bitmap, text: String) {
        val c = Canvas(bmp)
        val textSize = (bmp.width.coerceAtMost(bmp.height) * 0.05f).coerceIn(18f, 44f)
        val strip = textSize * 1.8f
        val bg = Paint().apply { color = Color.argb(128, 0, 0, 0) }
        c.drawRect(0f, bmp.height - strip, bmp.width.toFloat(), bmp.height.toFloat(), bg)
        val tf = runCatching { ResourcesCompat.getFont(context, R.font.noto_sans_gujarati) }.getOrNull() ?: Typeface.DEFAULT
        val p = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            this.textSize = textSize
            typeface = Typeface.create(tf, Typeface.BOLD)
        }
        c.drawText(text, textSize * 0.6f, bmp.height - strip / 2f + textSize / 3f, p)
    }

    private fun exifDegrees(f: File): Int = runCatching {
        when (ExifInterface(f.path).getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)) {
            ExifInterface.ORIENTATION_ROTATE_90 -> 90
            ExifInterface.ORIENTATION_ROTATE_180 -> 180
            ExifInterface.ORIENTATION_ROTATE_270 -> 270
            else -> 0
        }
    }.getOrDefault(0)

    private fun rotate(b: Bitmap, degrees: Int): Bitmap {
        if (degrees == 0) return b
        val m = Matrix().apply { postRotate(degrees.toFloat()) }
        return Bitmap.createBitmap(b, 0, 0, b.width, b.height, m, true)
    }
}
