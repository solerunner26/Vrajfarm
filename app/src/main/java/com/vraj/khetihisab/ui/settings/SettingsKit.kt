package com.vraj.khetihisab.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.vraj.khetihisab.ui.components.VkDialog
import com.vraj.khetihisab.ui.components.VkSwitchRow
import com.vraj.khetihisab.ui.components.VkTextField
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

/** Settings-family alias of [com.vraj.khetihisab.ui.components.VkTextField] (kept for call sites). */
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
) = VkTextField(
    value = value,
    onValueChange = onValueChange,
    label = label,
    modifier = modifier,
    singleLine = singleLine,
    minLines = minLines,
    keyboard = keyboard,
    isError = isError,
    supporting = supporting,
    placeholder = placeholder,
    password = password,
)

/** Alias of [VkSwitchRow]. */
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
) = VkSwitchRow(title, checked, onCheckedChange, onLabel, offLabel, modifier, meta, icon, enabled)

/** Alias of [VkDialog] with a confirm and a dismiss action. */
@Composable
internal fun StDialog(
    title: String,
    confirmLabel: String,
    onConfirm: () -> Unit,
    dismissLabel: String,
    onDismiss: () -> Unit,
    confirmEnabled: Boolean = true,
    danger: Boolean = false,
    deleteLabel: String? = null,
    onDelete: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) = VkDialog(
    deleteLabel = deleteLabel,
    onDelete = onDelete,
    title = title,
    onDismiss = onDismiss,
    confirmLabel = confirmLabel,
    onConfirm = onConfirm,
    dismissLabel = dismissLabel,
    confirmEnabled = confirmEnabled,
    danger = danger,
    content = content,
)

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
