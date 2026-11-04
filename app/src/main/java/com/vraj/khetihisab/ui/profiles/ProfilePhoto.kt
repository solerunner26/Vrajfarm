package com.vraj.khetihisab.ui.profiles

import android.content.Context
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import com.vraj.khetihisab.data.entity.ProfileRole
import com.vraj.khetihisab.media.PersonPhotoCapture
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import kotlin.math.max
import kotlin.math.min

/**
 * Profile photo processing (doc 14 "Photo processing"): decode with ImageDecoder (EXIF rotation is
 * applied by the decoder), centre-crop to a square, scale to 512×512, WebP quality 80 stepping down
 * until ≤ 60 KB, stored under `filesDir/people_photos/` (included in the nightly backup).
 *
 * Deviation from doc 14: the file name is `<role>_<timestamp>.webp` instead of overwriting `<role>.webp`,
 * because VkPhotoSlot loads `File(path)` without the `path@updatedAt` cache key; a new name makes Coil
 * show the new photo at once. The previous file is deleted by the caller after the DB update.
 */
object ProfilePhoto {
    const val SIZE = 512
    const val MAX_BYTES = 60 * 1024
    const val START_QUALITY = 80
    private const val MIN_QUALITY = 30
    const val FOLDER = PersonPhotoCapture.FOLDER_PEOPLE

    /** Centre square of a [w]×[h] image: (left, top, side). */
    fun centreSquare(w: Int, h: Int): Triple<Int, Int, Int> {
        val side = min(w, h)
        return Triple((w - side) / 2, (h - side) / 2, side)
    }

    /** Decoder sample size so the shorter side stays ≥ [SIZE] (memory on large camera shots). */
    fun sampleFor(w: Int, h: Int): Int {
        var s = 1
        while (min(w, h) / (s * 2) >= SIZE) s *= 2
        return s
    }

    fun fileName(role: ProfileRole, now: Long): String = "${role.name.lowercase()}_$now.webp"

    suspend fun fromUri(context: Context, uri: Uri, role: ProfileRole): String? =
        decodeAndStore(context, role) { ImageDecoder.createSource(context.contentResolver, uri) }

    /** Camera tmp file; the tmp file is deleted afterwards. */
    suspend fun fromFile(context: Context, file: File, role: ProfileRole): String? = try {
        decodeAndStore(context, role) { ImageDecoder.createSource(file) }
    } finally {
        withContext(Dispatchers.IO) { file.delete() }
    }

    private suspend fun decodeAndStore(context: Context, role: ProfileRole, source: () -> ImageDecoder.Source): String? =
        withContext(Dispatchers.IO) {
            runCatching {
                val decoded = ImageDecoder.decodeBitmap(source()) { decoder, info, _ ->
                    decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
                    decoder.setTargetSampleSize(sampleFor(info.size.width, info.size.height))
                }
                val (l, t, side) = centreSquare(decoded.width, decoded.height)
                val square = Bitmap.createBitmap(decoded, l, t, max(1, side), max(1, side))
                val scaled = Bitmap.createScaledBitmap(square, SIZE, SIZE, true)
                val bytes = compress(scaled)
                listOf(decoded, square, scaled).distinct().forEach { if (!it.isRecycled) it.recycle() }
                val dir = File(context.filesDir, FOLDER).apply { mkdirs() }
                val out = File(dir, fileName(role, System.currentTimeMillis()))
                out.writeBytes(bytes)
                out.absolutePath
            }.getOrNull()
        }

    @Suppress("DEPRECATION")
    private fun compress(bmp: Bitmap): ByteArray {
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

    /** Deletes an old profile photo (only inside filesDir). */
    fun delete(context: Context, path: String?) = PersonPhotoCapture.delete(context, path)
}
