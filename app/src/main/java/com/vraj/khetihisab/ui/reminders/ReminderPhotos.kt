package com.vraj.khetihisab.ui.reminders

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import com.vraj.khetihisab.media.PersonPhotoCapture
import com.vraj.khetihisab.media.suppressLockForExternal
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import java.util.UUID

/**
 * Reminder photo pipeline (doc 13): camera or gallery → EXIF-rotated, longest side 1024 →
 * WebP stepped down until ≤ 100 KB → `filesDir/reminder_photos/<uuid>.webp` (in the nightly backup).
 */
object ReminderPhotos {
    const val FOLDER = "reminder_photos"
    const val MAX_BYTES = 100 * 1024
    private const val START_QUALITY = 75
    private const val MIN_QUALITY = 25

    /** Quality steps tried in order until the output fits [MAX_BYTES]. */
    fun qualitySteps(): List<Int> = (START_QUALITY downTo MIN_QUALITY step 10).toList()

    /** Camera tmp file → stored WebP path (the tmp file is always deleted). Null if undecodable. */
    suspend fun fromFile(context: Context, source: File): String? = withContext(Dispatchers.IO) {
        try {
            encode(context, source.path, exifDegrees(source.path))
        } finally {
            source.delete()
        }
    }

    /** Gallery Uri → copied to a tmp file, then processed like a camera shot. */
    suspend fun fromUri(context: Context, uri: Uri): String? = withContext(Dispatchers.IO) {
        val tmpDir = File(context.filesDir, "tmp").apply { mkdirs() }
        val tmp = File(tmpDir, "pick_${UUID.randomUUID()}")
        val copied = runCatching {
            context.contentResolver.openInputStream(uri)?.use { input -> tmp.outputStream().use { input.copyTo(it) } } != null
        }.getOrDefault(false)
        if (!copied) {
            tmp.delete()
            return@withContext null
        }
        fromFile(context, tmp)
    }

    private fun encode(context: Context, path: String, degrees: Int): String? {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(path, bounds)
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null
        val opts = BitmapFactory.Options().apply { inSampleSize = PersonPhotoCapture.sampleSize(bounds.outWidth, bounds.outHeight) }
        val decoded = BitmapFactory.decodeFile(path, opts) ?: return null
        val rotated = if (degrees == 0) decoded else Bitmap.createBitmap(decoded, 0, 0, decoded.width, decoded.height, Matrix().apply { postRotate(degrees.toFloat()) }, true)
        val (w, h) = PersonPhotoCapture.scaledSize(rotated.width, rotated.height)
        val scaled = if (w == rotated.width && h == rotated.height) rotated else Bitmap.createScaledBitmap(rotated, w, h, true)
        val bytes = compress(scaled)
        listOf(decoded, rotated, scaled).distinct().forEach { if (!it.isRecycled) it.recycle() }
        val dir = File(context.filesDir, FOLDER).apply { mkdirs() }
        val out = File(dir, "${UUID.randomUUID()}.webp")
        out.writeBytes(bytes)
        return out.absolutePath
    }

    @Suppress("DEPRECATION")
    private fun compress(bmp: Bitmap): ByteArray {
        val format = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) Bitmap.CompressFormat.WEBP_LOSSY else Bitmap.CompressFormat.WEBP
        var bytes = ByteArray(0)
        for (q in qualitySteps()) {
            val bos = ByteArrayOutputStream()
            bmp.compress(format, q, bos)
            bytes = bos.toByteArray()
            if (bytes.size <= MAX_BYTES) break
        }
        return bytes
    }

    private fun exifDegrees(path: String): Int = runCatching {
        when (ExifInterface(path).getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)) {
            ExifInterface.ORIENTATION_ROTATE_90 -> 90
            ExifInterface.ORIENTATION_ROTATE_180 -> 180
            ExifInterface.ORIENTATION_ROTATE_270 -> 270
            else -> 0
        }
    }.getOrDefault(0)
}

@Stable
class ReminderPhotoPicker internal constructor(
    private val cameraImpl: () -> Unit,
    private val galleryImpl: () -> Unit,
    private val deniedState: () -> Boolean,
    private val busyState: () -> Boolean,
) {
    fun camera() = cameraImpl()
    fun gallery() = galleryImpl()
    val permissionDenied: Boolean get() = deniedState()
    val busy: Boolean get() = busyState()
}

/** Camera (with runtime CAMERA permission) + system photo picker. [onPicked] gets the WebP path. */
@Composable
fun rememberReminderPhotoPicker(onPicked: (String) -> Unit): ReminderPhotoPicker {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var pendingPath by rememberSaveable { mutableStateOf<String?>(null) }
    var denied by rememberSaveable { mutableStateOf(false) }
    var busy by remember { mutableStateOf(false) }

    fun process(block: suspend () -> String?) {
        busy = true
        scope.launch {
            val path = block()
            busy = false
            if (path != null) onPicked(path)
        }
    }

    val camera = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { ok ->
        val tmp = pendingPath?.let(::File)
        pendingPath = null
        if (tmp == null) return@rememberLauncherForActivityResult
        if (!ok) {
            tmp.delete()
            return@rememberLauncherForActivityResult
        }
        process { ReminderPhotos.fromFile(context, tmp) }
    }

    val gallery = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) process { ReminderPhotos.fromUri(context, uri) }
    }

    fun openCamera() {
        val (file, uri) = PersonPhotoCapture.newCaptureTarget(context)
        pendingPath = file.absolutePath
        suppressLockForExternal(context)
        runCatching { camera.launch(uri) }.onFailure {
            pendingPath = null
            file.delete()
        }
    }

    val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        denied = !granted
        if (granted) openCamera()
    }

    return remember(camera, gallery, permission) {
        ReminderPhotoPicker(
            cameraImpl = {
                if (ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
                    denied = false
                    openCamera()
                } else {
                    suppressLockForExternal(context)
                    permission.launch(Manifest.permission.CAMERA)
                }
            },
            galleryImpl = {
                suppressLockForExternal(context)
                runCatching { gallery.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) }
            },
            deniedState = { denied },
            busyState = { busy },
        )
    }
}
