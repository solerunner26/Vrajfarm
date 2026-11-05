package com.vraj.khetihisab.ui.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.vraj.khetihisab.ui.theme.VkDimens
import com.vraj.khetihisab.ui.theme.VkIcon

/**
 * The app's text field (audit #10): outlined, 16dp radius, card fill, ≥ 60dp, bodyLarge text.
 * Every field sends the Gujarati keyboard hint ([gujarati], owner request 2026-11-05) except
 * e-mail fields, which keep a Latin keyboard. Pass [keyboardType] for numbers, or a full
 * [keyboard] for IME actions.
 */
@Composable
fun VkTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    singleLine: Boolean = true,
    minLines: Int = 1,
    maxLines: Int = if (singleLine) 1 else Int.MAX_VALUE,
    keyboardType: KeyboardType = KeyboardType.Text,
    keyboard: KeyboardOptions = KeyboardOptions(keyboardType = keyboardType),
    isError: Boolean = false,
    supporting: String? = null,
    placeholder: String? = null,
    password: Boolean = false,
    enabled: Boolean = true,
    trailing: (@Composable () -> Unit)? = null,
    leading: (@Composable () -> Unit)? = null,
) {
    val cs = MaterialTheme.colorScheme
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label, maxLines = 1, overflow = TextOverflow.Ellipsis) },
        placeholder = placeholder?.let { { Text(it, maxLines = 1, overflow = TextOverflow.Ellipsis) } },
        singleLine = singleLine,
        minLines = if (singleLine) 1 else minLines,
        maxLines = if (singleLine) 1 else maxLines.coerceAtLeast(minLines),
        isError = isError,
        enabled = enabled,
        keyboardOptions = if (keyboard.keyboardType == KeyboardType.Email) keyboard else keyboard.gujarati(),
        visualTransformation = if (password) PasswordVisualTransformation() else VisualTransformation.None,
        supportingText = supporting?.let { { Text(it) } },
        trailingIcon = trailing,
        leadingIcon = leading,
        shape = RoundedCornerShape(16.dp),
        textStyle = MaterialTheme.typography.bodyLarge,
        colors = OutlinedTextFieldDefaults.colors(
            focusedContainerColor = cs.surfaceContainerLowest,
            unfocusedContainerColor = cs.surfaceContainerLowest,
            unfocusedBorderColor = cs.outline,
        ),
        modifier = modifier.fillMaxWidth().heightIn(min = VkDimens.minTouch),
    )
}

/** A [VkListRow] with an M3 Switch; the whole row (≥ 64dp) toggles it (audit #46). */
@Composable
fun VkSwitchRow(
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    onLabel: String,
    offLabel: String,
    modifier: Modifier = Modifier,
    meta: String? = null,
    icon: VkIcon? = null,
    enabled: Boolean = true,
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
