package com.vraj.khetihisab.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.vraj.khetihisab.ui.components.VkListRow
import com.vraj.khetihisab.ui.components.VkPrimaryButton
import com.vraj.khetihisab.ui.components.VkTextButton
import com.vraj.khetihisab.ui.theme.VkIcon

/*
 * Small building blocks shared by ui/settings, ui/reports, ui/profiles, ui/help and ui/panchang.
 * They only compose Vk* components and M3 primitives that take colours from MaterialTheme
 * (no hard-coded colours, doc 01 §6). Labels on buttons stay one line (doc 01 §8).
 */

/** Bottom padding for every scrollable screen: last item clears the nav bar (doc 17). */
internal val ScreenBottomPadding = 32.dp

/** Scrollable screen body under a VkTopHeader: 16dp gutter, 16dp rhythm, bottom padding. */
@Composable
internal fun StScrollBody(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
    ) {
        content()
    }
}

/** Body column used inside [StScrollBody] after the header. */
@Composable
internal fun StBodyColumn(content: @Composable ColumnScope.() -> Unit) {
    Column(
        Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = ScreenBottomPadding)),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        content = content,
    )
}

/** Outlined text field in the app's input style (16dp radius, card fill, 60dp min height). */
@Composable
internal fun StTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    singleLine: Boolean = true,
    minLines: Int = 1,
    keyboard: KeyboardOptions = KeyboardOptions.Default,
    isError: Boolean = false,
    supporting: String? = null,
    placeholder: String? = null,
    password: Boolean = false,
) {
    val cs = MaterialTheme.colorScheme
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label, maxLines = 1, overflow = TextOverflow.Ellipsis) },
        placeholder = placeholder?.let { { Text(it, maxLines = 1, overflow = TextOverflow.Ellipsis) } },
        singleLine = singleLine,
        minLines = if (singleLine) 1 else minLines,
        isError = isError,
        keyboardOptions = keyboard,
        visualTransformation = if (password) PasswordVisualTransformation() else VisualTransformation.None,
        supportingText = supporting?.let { { Text(it) } },
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

/** A VkListRow with an M3 Switch (touch target = whole row, ≥ 64dp). */
@Composable
internal fun StSwitchRow(
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    meta: String? = null,
    icon: VkIcon? = null,
    enabled: Boolean = true,
    onLabel: String,
    offLabel: String,
) {
    VkListRow(
        title = title,
        meta = meta,
        leadingIcon = icon,
        modifier = modifier.semantics {
            role = Role.Switch
            stateDescription = if (checked) onLabel else offLabel
        },
        onClick = if (enabled) ({ onCheckedChange(!checked) }) else null,
        trailing = { Switch(checked = checked, onCheckedChange = null, enabled = enabled) },
    )
}

/**
 * M3 dialog (28dp radius by default) with a scrollable body and stacked full-width actions
 * (filled → text), so two actions never squeeze side by side (doc 01 §8.5).
 */
@Composable
internal fun StDialog(
    title: String,
    confirmLabel: String,
    onConfirm: () -> Unit,
    dismissLabel: String,
    onDismiss: () -> Unit,
    confirmEnabled: Boolean = true,
    content: @Composable ColumnScope.() -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title, style = MaterialTheme.typography.headlineSmall) },
        text = {
            Column(
                Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                content = content,
            )
        },
        confirmButton = {
            Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                VkPrimaryButton(confirmLabel, onConfirm, Modifier.fillMaxWidth(), enabled = confirmEnabled)
                VkTextButton(dismissLabel, onDismiss, Modifier.fillMaxWidth())
            }
        },
    )
}

/** Small meta text (bodySmall, onSurfaceVariant), wraps. */
@Composable
internal fun StNote(text: String, modifier: Modifier = Modifier) {
    Text(text, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = modifier)
}

/** Error text in the M3 error role. */
@Composable
internal fun StError(text: String, modifier: Modifier = Modifier) {
    Text(text, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error, modifier = modifier)
}
