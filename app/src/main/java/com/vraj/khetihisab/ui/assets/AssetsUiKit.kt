package com.vraj.khetihisab.ui.assets

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.vraj.khetihisab.R
import com.vraj.khetihisab.ui.components.VkTextButton
import com.vraj.khetihisab.ui.theme.LocalVkExtra
import com.vraj.khetihisab.ui.theme.VkIcon
import com.vraj.khetihisab.ui.theme.VkIconImage
import java.io.File
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

/** Small, module-local building blocks. Colours come only from MaterialTheme / LocalVkExtra. */
internal object AssetsFormat {
    val IST: ZoneId = ZoneId.of("Asia/Kolkata")
    private val DMY: DateTimeFormatter = DateTimeFormatter.ofPattern("dd-MM-yyyy")
    fun dmy(d: LocalDate): String = d.format(DMY)
    fun dmy(epochMillis: Long): String = dmy(Instant.ofEpochMilli(epochMillis).atZone(IST).toLocalDate())

    /** The asset icon by iconKey (asset | lend | utility). */
    fun icon(iconKey: String?): VkIcon = when (iconKey) {
        "utility" -> VkIcon.UTILITY
        "lend" -> VkIcon.LEND
        else -> VkIcon.ASSET
    }
}

/** Section label above a group (css .vk-label). */
@Composable
internal fun AsLabel(text: String, modifier: Modifier = Modifier) {
    Text(
        text,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = modifier.padding(start = 4.dp),
    )
}

@Composable
internal fun AsTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    keyboard: KeyboardOptions = KeyboardOptions.Default,
    isError: Boolean = false,
    supporting: String? = null,
    leading: (@Composable () -> Unit)? = null,
) {
    val cs = MaterialTheme.colorScheme
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label, maxLines = 1, overflow = TextOverflow.Ellipsis) },
        singleLine = true,
        isError = isError,
        keyboardOptions = keyboard,
        leadingIcon = leading,
        supportingText = supporting?.let { { Text(it, maxLines = 1, overflow = TextOverflow.Ellipsis) } },
        shape = RoundedCornerShape(16.dp),
        textStyle = MaterialTheme.typography.bodyLarge,
        colors = OutlinedTextFieldDefaults.colors(
            focusedContainerColor = cs.surfaceContainerLowest,
            unfocusedContainerColor = cs.surfaceContainerLowest,
            unfocusedBorderColor = cs.outline,
        ),
        modifier = modifier.fillMaxWidth().heightIn(min = 60.dp),
    )
}

/** Header search field (Assets: "કોણ લઈ ગયું? નામ, તારીખ…"). */
@Composable
internal fun AsSearchField(value: String, onValueChange: (String) -> Unit, placeholder: String, modifier: Modifier = Modifier) {
    val cs = MaterialTheme.colorScheme
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        placeholder = { Text(placeholder, maxLines = 1, overflow = TextOverflow.Ellipsis) },
        leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
        singleLine = true,
        shape = RoundedCornerShape(28.dp),
        textStyle = MaterialTheme.typography.bodyLarge,
        colors = OutlinedTextFieldDefaults.colors(
            focusedContainerColor = cs.surfaceContainerLowest,
            unfocusedContainerColor = cs.surfaceContainerLowest,
            focusedTextColor = cs.onSurface,
            unfocusedTextColor = cs.onSurface,
            unfocusedBorderColor = cs.surfaceContainerLowest,
            focusedBorderColor = cs.primary,
        ),
        modifier = modifier.fillMaxWidth().heightIn(min = 56.dp),
    )
}

/** Rounded photo (stored WebP) or a silhouette placeholder; tap → [onClick]. */
@Composable
internal fun AsPhoto(
    path: String?,
    width: Dp,
    height: Dp,
    radius: Dp,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    overlay: @Composable BoxScope.() -> Unit = {},
) {
    val x = LocalVkExtra.current
    val shape = RoundedCornerShape(radius)
    Box(
        modifier
            .size(width, height)
            .clip(shape)
            .background(x.silhouetteBg)
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, shape)
            .then(if (onClick != null) Modifier.clickable(role = Role.Image, onClick = onClick) else Modifier),
        contentAlignment = Alignment.Center,
    ) {
        if (path != null && File(path).exists()) {
            AsyncImage(model = File(path), contentDescription = contentDescription, contentScale = ContentScale.Crop, modifier = Modifier.size(width, height))
        } else {
            VkIconImage(VkIcon.PROFILE, size = (minOf(width, height).value * 0.5f).dp)
        }
        overlay()
    }
}

/** M3 date picker in a dialog; [initial] and the result are local dates (UTC-midnight millis inside). */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun AsDatePickerDialog(initial: LocalDate, onDismiss: () -> Unit, onPicked: (LocalDate) -> Unit) {
    val st = rememberDatePickerState(initialSelectedDateMillis = initial.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli())
    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            VkTextButton(stringResource(R.string.as_ok), onClick = {
                st.selectedDateMillis?.let { onPicked(Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate()) }
                onDismiss()
            })
        },
        dismissButton = { VkTextButton(stringResource(R.string.as_cancel), onClick = onDismiss) },
    ) { DatePicker(state = st) }
}
