package com.vraj.khetihisab.ui.entry

import android.content.Context
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.FileProvider
import coil.compose.AsyncImage
import com.vraj.khetihisab.R
import com.vraj.khetihisab.ui.components.VkTonalButton
import com.vraj.khetihisab.ui.theme.LocalVkExtra
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import java.util.UUID
import kotlin.math.max
import kotlin.math.roundToInt

/**
 * Bill photos (doc 06 §Bill photo): camera → FileProvider temp JPEG in filesDir/tmp → decoded with
 * ImageDecoder (EXIF rotation applied by the platform) at ≤ 1600 px longest side → WebP < 85 KB in
 * filesDir/bill_photos/<uuid>.webp. The temp file is deleted afterwards.
 */
object BillPhotoHelper {
    const val MAX_BYTES = 85 * 1024
    const val MAX_SIDE = 1600
    const val FOLDER = "bill_photos"
    private const val TMP = "tmp"

    fun authority(context: Context) = "${context.packageName}.files"

    /** New temp file + content:// Uri for ActivityResultContracts.TakePicture. */
    fun newCaptureTarget(context: Context, prefix: String = "bill"): Pair<File, Uri> {
        val dir = File(context.filesDir, TMP).apply { mkdirs() }
        val f = File(dir, "${prefix}_${System.currentTimeMillis()}.jpg")
        return f to FileProvider.getUriForFile(context, authority(context), f)
    }

    /** Processes [source] into a new bill WebP. Returns its absolute path, or null if decoding failed. */
    suspend fun process(context: Context, source: Uri, tempToDelete: File? = null): String? = withContext(Dispatchers.IO) {
        try {
            val bmp = decodeScaled(context, source, MAX_SIDE) ?: return@withContext null
            val bytes = compressToLimit(bmp, MAX_BYTES)
            bmp.recycle()
            val dir = File(context.filesDir, FOLDER).apply { mkdirs() }
            val out = File(dir, "${UUID.randomUUID()}.webp")
            out.writeBytes(bytes)
            out.absolutePath
        } catch (_: Exception) {
            null
        } finally {
            tempToDelete?.delete()
        }
    }

    /** Deletes a bill created in this session that was never saved (remove ✕ / back). */
    fun discard(path: String?) {
        if (path != null) runCatching { File(path).delete() }
    }

    /** Longest side ≤ [maxSide]; software bitmap so it can be compressed. */
    fun decodeScaled(context: Context, uri: Uri, maxSide: Int): Bitmap? {
        val src = ImageDecoder.createSource(context.contentResolver, uri)
        return ImageDecoder.decodeBitmap(src) { decoder, info, _ ->
            decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
            decoder.isMutableRequired = false
            val w = info.size.width
            val h = info.size.height
            val longest = max(w, h)
            if (longest > maxSide) {
                val scale = maxSide.toFloat() / longest
                decoder.setTargetSize((w * scale).roundToInt().coerceAtLeast(1), (h * scale).roundToInt().coerceAtLeast(1))
            }
        }
    }

    @Suppress("DEPRECATION")
    private val webpFormat: Bitmap.CompressFormat
        get() = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) Bitmap.CompressFormat.WEBP_LOSSY else Bitmap.CompressFormat.WEBP

    /**
     * Quality 80 → 40 in steps of 10 while > [maxBytes]; still too big → scale ×0.85 and repeat.
     */
    fun compressToLimit(source: Bitmap, maxBytes: Int, startQuality: Int = 80, minQuality: Int = 40): ByteArray {
        var bmp = source
        while (true) {
            var q = startQuality
            while (true) {
                val bytes = ByteArrayOutputStream().use { bos ->
                    bmp.compress(webpFormat, q, bos)
                    bos.toByteArray()
                }
                if (bytes.size <= maxBytes) {
                    if (bmp !== source) bmp.recycle()
                    return bytes
                }
                if (q <= minQuality) break
                q -= 10
            }
            val nw = (bmp.width * 0.85f).roundToInt()
            val nh = (bmp.height * 0.85f).roundToInt()
            if (nw < 64 || nh < 64) {
                // Pathological input: return the smallest attempt.
                return ByteArrayOutputStream().use { bos -> bmp.compress(webpFormat, minQuality, bos); bos.toByteArray() }
            }
            val scaled = Bitmap.createScaledBitmap(bmp, nw, nh, true)
            if (bmp !== source) bmp.recycle()
            bmp = scaled
        }
    }
}

/**
 * Full-screen bill viewer: pinch-to-zoom 1–5× (Modifier.transformable) and double-tap toggles 2.5×.
 */
@Composable
fun BillViewer(path: String, onDismiss: () -> Unit) {
    var scale by remember { mutableFloatStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }
    val state = rememberTransformableState { zoom, pan, _ ->
        scale = (scale * zoom).coerceIn(1f, 5f)
        offset = if (scale == 1f) Offset.Zero else offset + pan
    }
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Box(Modifier.fillMaxSize().background(LocalVkExtra.current.scrim)) {
            AsyncImage(
                model = File(path),
                contentDescription = stringResource(R.string.scr_view_bill),
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(Unit) {
                        detectTapGestures(onDoubleTap = {
                            if (scale > 1f) { scale = 1f; offset = Offset.Zero } else scale = 2.5f
                        })
                    }
                    .transformable(state)
                    .graphicsLayer {
                        scaleX = scale
                        scaleY = scale
                        translationX = offset.x
                        translationY = offset.y
                    },
            )
            VkTonalButton(
                stringResource(R.string.scr_close),
                onDismiss,
                Modifier.align(Alignment.TopEnd).statusBarsPadding().padding(16.dp),
            )
        }
    }
}
