package com.vraj.khetihisab.media

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
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
import com.vraj.khetihisab.VrajApp
import kotlinx.coroutines.launch
import java.io.File

/** Handle returned by [rememberPersonPhotoCapture]. */
@Stable
class PersonPhotoLauncher internal constructor(
    private val launchImpl: () -> Unit,
    private val permissionDeniedState: () -> Boolean,
    private val busyState: () -> Boolean,
) {
    /** Asks for the camera permission if needed, then opens the camera. */
    fun launch() = launchImpl()

    /** True after the user denied the camera permission: show "ફોટો માટે કેમેરા પરવાનગી આપો". */
    val permissionDenied: Boolean get() = permissionDeniedState()

    /** True while the shot is being compressed. */
    val busy: Boolean get() = busyState()
}

/**
 * Camera capture for person photos. Because the manifest declares CAMERA, `ACTION_IMAGE_CAPTURE`
 * requires the runtime permission — so we request it first. Returning from the camera does not
 * re-prompt the PIN (suppressNextLock). [onCaptured] gets the stored WebP path.
 */
@Composable
fun rememberPersonPhotoCapture(
    folder: String = PersonPhotoCapture.FOLDER_BORROWERS,
    onCaptured: (String) -> Unit,
): PersonPhotoLauncher {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var pendingPath by rememberSaveable { mutableStateOf<String?>(null) }
    var denied by rememberSaveable { mutableStateOf(false) }
    var busy by remember { mutableStateOf(false) }

    val camera = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { ok ->
        val tmp = pendingPath?.let(::File)
        pendingPath = null
        if (tmp == null) return@rememberLauncherForActivityResult
        if (!ok) {
            tmp.delete()
            return@rememberLauncherForActivityResult
        }
        busy = true
        scope.launch {
            val path = PersonPhotoCapture.process(context, tmp, folder)
            busy = false
            if (path != null) onCaptured(path)
        }
    }

    fun openCamera() {
        val (file, uri) = PersonPhotoCapture.newCaptureTarget(context)
        pendingPath = file.absolutePath
        suppressLockForExternal(context)
        runCatching { camera.launch(uri) }.onFailure { pendingPath = null; file.delete() }
    }

    val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        denied = !granted
        if (granted) openCamera()
    }

    return remember(camera, permission) {
        PersonPhotoLauncher(
            launchImpl = {
                if (ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
                    denied = false
                    openCamera()
                } else {
                    suppressLockForExternal(context)
                    permission.launch(Manifest.permission.CAMERA)
                }
            },
            permissionDeniedState = { denied },
            busyState = { busy },
        )
    }
}

/** Opens this app's system settings page (camera permission permanently denied). */
fun openAppSettings(context: Context) {
    suppressLockForExternal(context)
    val i = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", context.packageName, null))
        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    runCatching { context.startActivity(i) }
}

/** Returning from camera, voice or settings must not re-prompt the PIN (doc 04). */
fun suppressLockForExternal(context: Context) {
    runCatching { (context.applicationContext as? VrajApp)?.container?.lockManager?.suppressNextLock = true }
}
