package com.vraj.khetihisab.ui.entry

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.vraj.khetihisab.R
import com.vraj.khetihisab.ui.components.VkPrimaryButton
import com.vraj.khetihisab.ui.components.VkTextButton
import com.vraj.khetihisab.ui.home.rememberScreensDeps
import java.io.File

/** Handle returned by [rememberPhotoCapture]. */
class PhotoCaptureHandle(val takePhoto: () -> Unit, val pickFromGallery: () -> Unit)

/**
 * Camera (TakePicture → FileProvider temp file) + gallery (PickVisualMedia ImageOnly). The CAMERA
 * permission is requested at first use; when permanently denied, a dialog links to app Settings.
 * [onImage] receives the source Uri plus the temp file to delete after processing (null for gallery).
 */
@Composable
fun rememberPhotoCapture(prefix: String, onImage: (Uri, File?) -> Unit): PhotoCaptureHandle {
    val context = LocalContext.current
    val deps = rememberScreensDeps()
    val cb by rememberUpdatedState(onImage)
    var pendingPath by rememberSaveable { mutableStateOf<String?>(null) }
    var pendingUri by rememberSaveable { mutableStateOf<String?>(null) }
    var showDenied by remember { mutableStateOf(false) }

    val camera = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { ok ->
        val f = pendingPath?.let(::File)
        val u = pendingUri?.let(Uri::parse)
        pendingPath = null
        pendingUri = null
        if (ok && f != null && u != null && f.length() > 0) cb(u, f) else f?.delete()
    }
    val gallery = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) cb(uri, null)
    }

    fun launchCamera() {
        val (file, uri) = BillPhotoHelper.newCaptureTarget(context, prefix)
        pendingPath = file.absolutePath
        pendingUri = uri.toString()
        deps.lockManager.suppressNextLock = true
        runCatching { camera.launch(uri) }.onFailure { deps.lockManager.suppressNextLock = false }
    }

    val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) launchCamera() else showDenied = true
    }

    if (showDenied) {
        CameraDeniedDialog(context, onDismiss = { showDenied = false })
    }

    return remember(context) {
        PhotoCaptureHandle(
            takePhoto = {
                if (ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
                    launchCamera()
                } else {
                    deps.lockManager.suppressNextLock = true
                    permission.launch(Manifest.permission.CAMERA)
                }
            },
            pickFromGallery = {
                deps.lockManager.suppressNextLock = true
                runCatching {
                    gallery.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                }.onFailure { deps.lockManager.suppressNextLock = false }
            },
        )
    }
}

@Composable
private fun CameraDeniedDialog(context: Context, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(28.dp),
        containerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
        title = { Text(stringResource(R.string.scr_camera_denied_title)) },
        text = { Text(stringResource(R.string.scr_camera_denied_body), style = MaterialTheme.typography.bodyLarge) },
        confirmButton = {
            Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                VkPrimaryButton(stringResource(R.string.scr_open_settings), {
                    onDismiss()
                    val i = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", context.packageName, null))
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    runCatching { context.startActivity(i) }
                }, Modifier.fillMaxWidth())
                VkTextButton(stringResource(R.string.scr_cancel), onDismiss, Modifier.fillMaxWidth())
            }
        },
    )
}
