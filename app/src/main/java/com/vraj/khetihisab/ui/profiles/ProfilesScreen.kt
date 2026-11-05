package com.vraj.khetihisab.ui.profiles

import android.Manifest
import android.content.pm.PackageManager
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.vraj.khetihisab.R
import com.vraj.khetihisab.data.entity.FarmEntity
import com.vraj.khetihisab.data.entity.ProfileRole
import com.vraj.khetihisab.media.PersonPhotoCapture
import com.vraj.khetihisab.media.openAppSettings
import com.vraj.khetihisab.media.suppressLockForExternal
import com.vraj.khetihisab.share.PhoneNormalizer
import com.vraj.khetihisab.ui.components.VkBadge
import com.vraj.khetihisab.ui.components.VkCard
import com.vraj.khetihisab.ui.components.VkChip
import com.vraj.khetihisab.ui.components.VkListRow
import com.vraj.khetihisab.ui.components.VkOutlinedButton
import com.vraj.khetihisab.ui.components.VkPhotoSlot
import com.vraj.khetihisab.ui.components.VkPrimaryButton
import com.vraj.khetihisab.ui.components.VkSectionTitle
import com.vraj.khetihisab.ui.components.VkTextButton
import com.vraj.khetihisab.ui.components.VkTonalButton
import com.vraj.khetihisab.ui.components.VkTopHeader
import com.vraj.khetihisab.ui.home.ScreensFormat
import com.vraj.khetihisab.ui.home.ScreensShare
import com.vraj.khetihisab.ui.home.rememberScreensDeps
import com.vraj.khetihisab.ui.home.screenViewModel
import com.vraj.khetihisab.ui.settings.StBodyColumn
import com.vraj.khetihisab.ui.settings.StDialog
import com.vraj.khetihisab.ui.settings.StError
import com.vraj.khetihisab.ui.settings.StNote
import com.vraj.khetihisab.ui.settings.StScrollBody
import com.vraj.khetihisab.ui.settings.StTextField
import com.vraj.khetihisab.ui.theme.VkIcon
import com.vraj.khetihisab.ui.theme.VkIconImage
import kotlinx.coroutines.launch
import java.io.File

@Composable
fun ProfilesRoute(onBack: () -> Unit) {
    val vm: ProfilesViewModel = screenViewModel { deps, _ ->
        ProfilesViewModel(
            RepoProfilesStore(deps.repos.profiles, deps.repos.master),
            deletePhotoFile = { ProfilePhoto.delete(deps.appContext, it) },
        )
    }
    ProfilesScreen(vm, onBack)
}

@Composable
private fun roleLabel(role: ProfileRole): String = when (role) {
    ProfileRole.OWNER -> stringResource(R.string.st_role_owner)
    ProfileRole.MANAGER_1 -> stringResource(R.string.st_role_manager, 1)
    ProfileRole.MANAGER_2 -> stringResource(R.string.st_role_manager, 2)
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ProfilesScreen(vm: ProfilesViewModel, onBack: () -> Unit) {
    val u by vm.ui.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val deps = rememberScreensDeps()
    val scope = rememberCoroutineScope()
    var sheetRole by rememberSaveable { mutableStateOf<ProfileRole?>(null) }
    var editRole by rememberSaveable { mutableStateOf<ProfileRole?>(null) }
    var pendingCamera by rememberSaveable { mutableStateOf<String?>(null) }
    var cameraRole by rememberSaveable { mutableStateOf<ProfileRole?>(null) }
    var cameraDenied by rememberSaveable { mutableStateOf(false) }
    var busy by remember { mutableStateOf(false) }

    val photoFailed = stringResource(R.string.st_photo_failed)
    val saved = stringResource(R.string.st_saved)
    val snackbar = com.vraj.khetihisab.ui.components.rememberVkSnackbar()
    LaunchedEffect(vm) {
        vm.events.collect { e ->
            if (e == ProfileEvent.PhotoFailed) snackbar.show(photoFailed) else snackbar.show(saved, success = true)
        }
    }

    val camera = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { ok ->
        val tmp = pendingCamera?.let(::File)
        val role = cameraRole
        pendingCamera = null
        if (tmp == null || role == null) return@rememberLauncherForActivityResult
        if (!ok) {
            tmp.delete()
            return@rememberLauncherForActivityResult
        }
        busy = true
        scope.launch {
            vm.onPhoto(role, ProfilePhoto.fromFile(context, tmp, role))
            busy = false
        }
    }

    fun openCamera(role: ProfileRole) {
        val (file, uri) = PersonPhotoCapture.newCaptureTarget(context)
        pendingCamera = file.absolutePath
        cameraRole = role
        suppressLockForExternal(context)
        runCatching { camera.launch(uri) }.onFailure { pendingCamera = null; file.delete() }
    }

    val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        cameraDenied = !granted
        val role = cameraRole
        if (granted && role != null) openCamera(role)
    }

    val gallery = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        val role = cameraRole
        if (uri == null || role == null) return@rememberLauncherForActivityResult
        busy = true
        scope.launch {
            vm.onPhoto(role, ProfilePhoto.fromUri(context, uri, role))
            busy = false
        }
    }

    StScrollBody {
        VkTopHeader(title = stringResource(R.string.st_profile_title), onBack = onBack)
        StBodyColumn {
            u.owner?.let { owner ->
                OwnerCard(owner, onPhoto = { sheetRole = ProfileRole.OWNER }, onEdit = { editRole = ProfileRole.OWNER })
            }
            if (busy) StNote(stringResource(R.string.st_photo_processing))
            if (cameraDenied) {
                StError(stringResource(R.string.st_camera_denied))
                VkTextButton(stringResource(R.string.st_open_settings), { openAppSettings(context) }, Modifier.fillMaxWidth())
            }
            VkSectionTitle(stringResource(R.string.st_managers))
            u.managers.forEach { m ->
                ManagerCard(
                    m,
                    onPhoto = { sheetRole = m.role },
                    onEdit = { editRole = m.role },
                    onWhatsApp = {
                        ScreensShare.whatsApp(context, deps, m.phone, context.getString(R.string.st_wa_manager_greeting, m.name.ifBlank { "" }).trim())
                    },
                    onCall = { m.phone?.let { ScreensShare.dial(context, deps, it) } },
                )
            }
        }
    }

    sheetRole?.let { role ->
        val card = if (role == ProfileRole.OWNER) u.owner else u.managers.firstOrNull { it.role == role }
        com.vraj.khetihisab.ui.components.VkBottomSheet(stringResource(R.string.st_photo_sheet_title, roleLabel(role)), { sheetRole = null }) {
            Column(
                Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                VkCard(Modifier.fillMaxWidth(), contentPadding = 0.dp) {
                    VkListRow(
                        title = stringResource(R.string.st_photo_camera),
                        leadingIcon = VkIcon.CAMERA,
                        onClick = {
                            sheetRole = null
                            cameraRole = role
                            if (ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
                                cameraDenied = false
                                openCamera(role)
                            } else {
                                suppressLockForExternal(context)
                                permission.launch(Manifest.permission.CAMERA)
                            }
                        },
                    )
                }
                VkCard(Modifier.fillMaxWidth(), contentPadding = 0.dp) {
                    VkListRow(
                        title = stringResource(R.string.st_photo_gallery),
                        leadingIcon = VkIcon.PROFILE,
                        onClick = {
                            sheetRole = null
                            cameraRole = role
                            suppressLockForExternal(context)
                            gallery.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                        },
                    )
                }
                if (card?.photoPath != null) {
                    VkCard(Modifier.fillMaxWidth(), contentPadding = 0.dp) {
                        VkListRow(
                            title = stringResource(R.string.st_photo_remove),
                            onClick = {
                                vm.removePhoto(role)
                                sheetRole = null
                            },
                        )
                    }
                }
                VkTextButton(stringResource(R.string.st_cancel), { sheetRole = null }, Modifier.fillMaxWidth())
            }
        }
    }

    editRole?.let { role ->
        val card = if (role == ProfileRole.OWNER) u.owner else u.managers.firstOrNull { it.role == role }
        if (card != null) {
            ProfileEditDialog(card, u.allFarms, onDismiss = { editRole = null }) { e ->
                vm.save(e).also { if (it == null) editRole = null }
            }
        }
    }
}

/** Photo, or the initial on a primaryContainer circle, plus a camera badge (canvas 14). */
@Composable
private fun ProfileAvatar(card: PersonCard, size: Dp, onClick: () -> Unit) {
    val cs = MaterialTheme.colorScheme
    val desc = stringResource(R.string.st_change_photo, roleLabel(card.role))
    Box(Modifier.semantics { contentDescription = desc }) {
        // Same avatar as the Home header: the photo, or the 3D profile icon (audit #49).
        com.vraj.khetihisab.ui.home.PersonAvatar(
            photoPath = card.photoPath,
            initial = "",
            size = size,
            onClick = onClick,
            contentDescription = desc,
        )
        val badge = if (size >= 100.dp) 42.dp else 32.dp
        Box(
            Modifier
                .align(Alignment.BottomEnd)
                .offset(x = 2.dp, y = 2.dp)
                .size(badge)
                .clip(CircleShape)
                .background(cs.primaryContainer)
                .border(3.dp, cs.surfaceContainerLowest, CircleShape)
                .clickable(role = Role.Button, onClick = onClick),
            contentAlignment = Alignment.Center,
        ) {
            VkIconImage(VkIcon.CAMERA, size = badge * 0.58f)
        }
    }
}

@Composable
private fun OwnerCard(owner: PersonCard, onPhoto: () -> Unit, onEdit: () -> Unit) {
    val cs = MaterialTheme.colorScheme
    VkCard(Modifier.fillMaxWidth()) {
        Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
            ProfileAvatar(owner, 124.dp, onPhoto)
            VkBadge(stringResource(R.string.st_role_owner), cs.primaryContainer, cs.onPrimaryContainer)
            Text(
                owner.name.ifBlank { stringResource(R.string.st_add_name) },
                style = MaterialTheme.typography.headlineMedium,
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            owner.address?.let {
                Text(it, style = MaterialTheme.typography.bodyMedium, color = cs.onSurfaceVariant, textAlign = TextAlign.Center)
            }
            owner.phone?.let { Text(PhoneNormalizer.display(it), style = MaterialTheme.typography.titleSmall) }
            VkOutlinedButton(stringResource(R.string.st_edit_profile), onEdit)
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ManagerCard(m: PersonCard, onPhoto: () -> Unit, onEdit: () -> Unit, onWhatsApp: () -> Unit, onCall: () -> Unit) {
    val cs = MaterialTheme.colorScheme
    VkCard(Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            ProfileAvatar(m, 84.dp, onPhoto)
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(roleLabel(m.role), style = MaterialTheme.typography.labelMedium, color = cs.primary, maxLines = 1)
                Text(
                    m.name.ifBlank { stringResource(R.string.st_add_name) },
                    style = MaterialTheme.typography.titleLarge,
                    color = if (m.name.isBlank()) cs.onSurfaceVariant else cs.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    m.phone?.let(PhoneNormalizer::display) ?: stringResource(R.string.st_need_number),
                    style = MaterialTheme.typography.bodySmall,
                    color = cs.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp), itemVerticalAlignment = Alignment.CenterVertically) {
            Text(stringResource(R.string.st_responsibility), style = MaterialTheme.typography.bodySmall, color = cs.onSurfaceVariant)
            if (m.farms.isEmpty()) {
                Text(stringResource(R.string.st_no_farms), style = MaterialTheme.typography.bodySmall, color = cs.onSurfaceVariant)
            } else {
                m.farms.forEach { f -> VkBadge(f.name, cs.primaryContainer, cs.onPrimaryContainer) }
            }
        }
        // Two half-width buttons (≤ 12 chars each, doc 01 §8.2) + a full-width edit: never wraps at 360dp.
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            VkPrimaryButton(stringResource(R.string.st_whatsapp), onWhatsApp, Modifier.weight(1f), enabled = m.hasPhone && m.name.isNotBlank())
            VkTonalButton(stringResource(R.string.st_call), onCall, Modifier.weight(1f), enabled = m.hasPhone && m.name.isNotBlank())
        }
        if (!m.hasPhone || m.name.isBlank()) StNote(stringResource(R.string.st_need_number))
        VkOutlinedButton(stringResource(R.string.st_edit), onEdit, Modifier.fillMaxWidth())
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ProfileEditDialog(card: PersonCard, farms: List<FarmEntity>, onDismiss: () -> Unit, onSave: (ProfileEdit) -> ProfileError?) {
    var name by rememberSaveable(card.role) { mutableStateOf(card.name) }
    var phone by rememberSaveable(card.role) { mutableStateOf(card.phone.orEmpty()) }
    var address by rememberSaveable(card.role) { mutableStateOf(card.address.orEmpty()) }
    var selected by rememberSaveable(card.role) { mutableStateOf(card.farms.map { it.id }.toLongArray()) }
    var error by remember { mutableStateOf<ProfileError?>(null) }
    val isOwner = card.role == ProfileRole.OWNER

    StDialog(
        title = roleLabel(card.role),
        confirmLabel = stringResource(R.string.st_save),
        onConfirm = { error = onSave(ProfileEdit(card.role, name, phone, address, selected.toSet())) },
        dismissLabel = stringResource(R.string.st_cancel),
        onDismiss = onDismiss,
    ) {
        StTextField(name, { name = it; error = null }, stringResource(R.string.st_name), isError = error == ProfileError.NAME)
        StTextField(
            phone, { phone = it; error = null }, stringResource(R.string.st_phone),
            keyboard = KeyboardOptions(keyboardType = KeyboardType.Phone),
            isError = error == ProfileError.PHONE,
            placeholder = "+91",
        )
        if (isOwner) {
            StTextField(address, { address = it }, stringResource(R.string.st_address), singleLine = false, minLines = 2)
        } else {
            Text(stringResource(R.string.st_assign_farms), style = MaterialTheme.typography.labelLarge)
            com.vraj.khetihisab.ui.components.VkChoiceGrid(
                options = farms.map { it.id to it.name },
                selectedSet = selected.toSet(),
                onToggle = { id -> selected = if (id in selected) selected.filter { it != id }.toLongArray() else selected + id },
            )
            StNote(stringResource(R.string.st_assign_note))
        }
        when (error) {
            ProfileError.NAME -> StError(stringResource(R.string.st_err_name))
            ProfileError.PHONE -> StError(stringResource(R.string.st_err_phone))
            null -> Unit
        }
    }
}
