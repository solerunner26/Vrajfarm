package com.vraj.khetihisab.ui.reminders

import com.vraj.khetihisab.ui.components.gujarati
import android.app.Activity
import android.content.Intent
import android.speech.RecognizerIntent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.vraj.khetihisab.R
import com.vraj.khetihisab.data.entity.Priority
import com.vraj.khetihisab.media.suppressLockForExternal
import com.vraj.khetihisab.notify.ReminderTiming
import com.vraj.khetihisab.ui.components.VkChip
import com.vraj.khetihisab.ui.components.VkOutlinedButton
import com.vraj.khetihisab.ui.components.VkPrimaryButton
import com.vraj.khetihisab.ui.components.VkSegmented
import com.vraj.khetihisab.ui.components.VkTextButton
import com.vraj.khetihisab.ui.components.VkTonalButton
import com.vraj.khetihisab.ui.theme.LocalVkExtra
import com.vraj.khetihisab.ui.theme.VkIcon
import com.vraj.khetihisab.ui.theme.VkIconImage
import java.io.File
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneOffset

/** Add / edit sheet (doc 13 ReminderEditorSheet): title + mic, note, priority, date, time, photo. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ReminderEditorSheet(d: ReminderDraft, vm: RemindersViewModel) {
    val context = LocalContext.current
    val cs = MaterialTheme.colorScheme
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var pickDate by rememberSaveable { mutableStateOf(false) }
    var pickTime by rememberSaveable { mutableStateOf(false) }
    var confirmDelete by rememberSaveable { mutableStateOf(false) }

    val photo = rememberReminderPhotoPicker(onPicked = vm::onPhoto)

    val voice = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { res ->
        if (res.resultCode == Activity.RESULT_OK) {
            res.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)?.firstOrNull()?.let(vm::onVoiceText)
        }
    }
    val voicePrompt = stringResource(R.string.rem_voice_prompt)
    fun startVoice() {
        val i = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
            .putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            .putExtra(RecognizerIntent.EXTRA_LANGUAGE, "gu-IN")
            .putExtra(RecognizerIntent.EXTRA_PROMPT, voicePrompt)
        suppressLockForExternal(context)
        runCatching { voice.launch(i) }
    }

    ModalBottomSheet(
        onDismissRequest = vm::dismissEditor,
        sheetState = sheetState,
        containerColor = cs.surfaceContainerLowest,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
    ) {
        com.vraj.khetihisab.ui.components.VkSheetSystemBars()
        Column(
            Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .imePadding()
                .navigationBarsPadding()
                .padding(horizontal = 20.dp)
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                stringResource(if (d.isNew) R.string.rem_sheet_new else R.string.rem_sheet_edit),
                style = MaterialTheme.typography.headlineSmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )

            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                RemTextField(
                    value = d.title,
                    onValueChange = vm::onTitle,
                    label = stringResource(R.string.rem_field_title),
                    isError = d.titleError,
                    supporting = if (d.titleError) stringResource(R.string.rem_title_required) else null,
                    modifier = Modifier.weight(1f),
                )
                val voiceLabel = stringResource(R.string.rem_voice)
                FilledTonalIconButton(
                    onClick = ::startVoice,
                    modifier = Modifier.size(60.dp).semantics { contentDescription = voiceLabel },
                    colors = IconButtonDefaults.filledTonalIconButtonColors(containerColor = cs.primaryContainer),
                ) { VkIconImage(VkIcon.MIC, size = 32.dp) }
            }

            RemTextField(value = d.note, onValueChange = vm::onNote, label = stringResource(R.string.rem_field_note), singleLine = false)

            RemLabel(stringResource(R.string.rem_priority))
            VkSegmented(
                options = listOf(
                    Priority.HIGH to stringResource(R.string.rem_high),
                    Priority.NORMAL to stringResource(R.string.rem_normal),
                ),
                selected = d.priority,
                onSelect = vm::onPriority,
            )

            // Stacked full-width: "તારીખ: 05-11-2026" exceeds the half-width label budget (doc 01 §8).
            VkChip(
                stringResource(R.string.rem_date, d.date.format(ReminderTiming.DATE)),
                selected = true,
                onClick = { pickDate = true },
                modifier = Modifier.fillMaxWidth(),
            )
            VkChip(
                stringResource(R.string.rem_time, d.time.format(ReminderTiming.TIME)),
                selected = true,
                onClick = { pickTime = true },
                modifier = Modifier.fillMaxWidth(),
            )
            if (vm.isPast(d)) {
                Text(stringResource(R.string.rem_past_warning), style = MaterialTheme.typography.bodyMedium, color = cs.error)
            }

            RemLabel(stringResource(R.string.rem_photo))
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                ReminderThumb(d.photoPath, size = 96.dp)
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    VkTonalButton(stringResource(R.string.rem_photo_camera), photo::camera, Modifier.fillMaxWidth(), enabled = !photo.busy)
                    VkOutlinedButton(stringResource(R.string.rem_photo_gallery), photo::gallery, Modifier.fillMaxWidth(), enabled = !photo.busy)
                    if (d.photoPath != null) {
                        VkTextButton(stringResource(R.string.rem_photo_remove), vm::removePhoto, Modifier.fillMaxWidth())
                    }
                }
            }
            when {
                photo.busy -> Text(stringResource(R.string.rem_photo_busy), style = MaterialTheme.typography.bodyMedium, color = cs.onSurfaceVariant)
                photo.permissionDenied -> Text(stringResource(R.string.rem_camera_denied), style = MaterialTheme.typography.bodyMedium, color = cs.error)
            }

            VkPrimaryButton(stringResource(R.string.rem_save), vm::save, Modifier.fillMaxWidth(), enabled = !photo.busy)
            // One dismiss: swipe down / back closes the sheet (audit #19). Delete asks first (#7).
            if (!d.isNew) com.vraj.khetihisab.ui.components.VkDangerButton(stringResource(R.string.rem_delete), { confirmDelete = true }, Modifier.fillMaxWidth())
        }
    }

    if (confirmDelete) {
        com.vraj.khetihisab.ui.components.VkConfirmDeleteDialog(
            what = d.title,
            onConfirm = { confirmDelete = false; vm.delete() },
            onDismiss = { confirmDelete = false },
            undoable = false,
        )
    }
    if (pickDate) RemDatePickerDialog(d.date, onDismiss = { pickDate = false }, onPicked = vm::onDate)
    if (pickTime) RemTimePickerDialog(d.time, onDismiss = { pickTime = false }, onPicked = vm::onTime)
}

/** 64dp card thumb / 96dp sheet slot: stored WebP, or the camera icon on the silhouette tile. */
@Composable
internal fun ReminderThumb(path: String?, size: Dp) {
    val x = LocalVkExtra.current
    val shape = RoundedCornerShape(12.dp)
    Box(
        Modifier
            .size(size)
            .clip(shape)
            .background(x.silhouetteBg)
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, shape),
        contentAlignment = Alignment.Center,
    ) {
        if (path != null && File(path).exists()) {
            AsyncImage(
                model = File(path),
                contentDescription = stringResource(R.string.rem_photo_cd),
                contentScale = ContentScale.Crop,
                modifier = Modifier.size(size),
            )
        } else {
            VkIconImage(VkIcon.CAMERA, size = (size.value * 0.48f).dp)
        }
    }
}

@Composable
private fun RemLabel(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = Modifier.padding(start = 4.dp),
    )
}

@Composable
private fun RemTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    singleLine: Boolean = true,
    isError: Boolean = false,
    supporting: String? = null,
) {
    com.vraj.khetihisab.ui.components.VkTextField(
        value = value,
        onValueChange = onValueChange,
        label = label,
        modifier = modifier,
        singleLine = singleLine,
        maxLines = if (singleLine) 1 else 4,
        keyboard = KeyboardOptions(imeAction = if (singleLine) ImeAction.Next else ImeAction.Default),
        isError = isError,
        supporting = supporting,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RemDatePickerDialog(initial: LocalDate, onDismiss: () -> Unit, onPicked: (LocalDate) -> Unit) {
    val st = rememberDatePickerState(initialSelectedDateMillis = initial.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli())
    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            VkTextButton(stringResource(R.string.rem_ok), onClick = {
                st.selectedDateMillis?.let { onPicked(Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate()) }
                onDismiss()
            })
        },
        dismissButton = { VkTextButton(stringResource(R.string.rem_cancel), onClick = onDismiss) },
    ) { DatePicker(state = st) }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RemTimePickerDialog(initial: LocalTime, onDismiss: () -> Unit, onPicked: (LocalTime) -> Unit) {
    val st = rememberTimePickerState(initialHour = initial.hour, initialMinute = initial.minute, is24Hour = false)
    com.vraj.khetihisab.ui.components.VkDialog(
        title = stringResource(R.string.rem_pick_time),
        onDismiss = onDismiss,
        confirmLabel = stringResource(R.string.rem_ok),
        onConfirm = {
            onPicked(LocalTime.of(st.hour, st.minute))
            onDismiss()
        },
        dismissLabel = stringResource(R.string.rem_cancel),
    ) { TimePicker(state = st) }
}
