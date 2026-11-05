package com.vraj.khetihisab.ui.components

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.KeyboardArrowRight
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.background
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.vraj.khetihisab.ui.theme.VkIcon
import com.vraj.khetihisab.ui.theme.VkIconImage

/*
 * Selection controls (owner review 2026-11-05: uneven wrapping chip rows looked unprofessional).
 * Rule for the whole app:
 *  - a fixed set of choices                → VkDropdownField (one 60dp field, list opens below)
 *  - type a name OR pick an earlier one     → VkComboField
 *  - a date / time                          → VkPickerField
 *  - the main pick of a flow (Entry tap 2)  → VkChoiceGrid (equal-width tiles, 2 or 3 columns)
 * Chips (VkChip) remain only for multi-value filters and display badges.
 */

private val FieldShape = RoundedCornerShape(16.dp)

@Composable
private fun fieldColors() = OutlinedTextFieldDefaults.colors(
    focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
)

/** Pick one of a fixed list. The selected label shows in the field; the list opens under it. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun <T> VkDropdownField(
    label: String,
    options: List<Pair<T, String>>,
    selected: T?,
    onSelect: (T) -> Unit,
    modifier: Modifier = Modifier,
    isError: Boolean = false,
    placeholder: String? = null,
    testTag: String? = null,
) {
    var open by remember { mutableStateOf(false) }
    val text = options.firstOrNull { it.first == selected }?.second.orEmpty()
    ExposedDropdownMenuBox(expanded = open, onExpandedChange = { open = it && options.isNotEmpty() }, modifier = modifier) {
        OutlinedTextField(
            value = text,
            onValueChange = {},
            readOnly = true,
            singleLine = true,
            isError = isError,
            label = { Text(label, maxLines = 1, overflow = TextOverflow.Ellipsis) },
            placeholder = placeholder?.let { p -> { Text(p, maxLines = 1, overflow = TextOverflow.Ellipsis) } },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = open) },
            textStyle = MaterialTheme.typography.bodyLarge,
            shape = FieldShape,
            colors = fieldColors(),
            modifier = Modifier
                .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                .fillMaxWidth()
                .heightIn(min = 60.dp)
                .then(if (testTag != null) Modifier.testTag(testTag) else Modifier),
        )
        ExposedDropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            options.forEach { (value, name) ->
                DropdownMenuItem(
                    text = { Text(name, style = MaterialTheme.typography.bodyLarge, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                    trailingIcon = if (value == selected) {
                        { Icon(Icons.Rounded.Check, contentDescription = null, tint = MaterialTheme.colorScheme.primary) }
                    } else {
                        null
                    },
                    onClick = { onSelect(value); open = false },
                    modifier = Modifier.heightIn(min = 56.dp),
                )
            }
        }
    }
}

/** Type a name, or pick one used before (borrowers, animals). */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VkComboField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    suggestions: List<String>,
    modifier: Modifier = Modifier,
    isError: Boolean = false,
    supporting: String? = null,
    onPick: (String) -> Unit = onValueChange,
    testTag: String? = null,
) {
    var open by remember { mutableStateOf(false) }
    val matches = suggestions.filter { value.isBlank() || it.contains(value.trim(), ignoreCase = true) }.take(8)
    ExposedDropdownMenuBox(expanded = open && matches.isNotEmpty(), onExpandedChange = { open = it }, modifier = modifier) {
        OutlinedTextField(
            value = value,
            onValueChange = { onValueChange(it); open = true },
            singleLine = true,
            isError = isError,
            label = { Text(label, maxLines = 1, overflow = TextOverflow.Ellipsis) },
            supportingText = supporting?.let { s -> { Text(s) } },
            trailingIcon = if (suggestions.isNotEmpty()) {
                { ExposedDropdownMenuDefaults.TrailingIcon(expanded = open && matches.isNotEmpty()) }
            } else {
                null
            },
            keyboardOptions = VkGujaratiKeyboard,
            textStyle = MaterialTheme.typography.bodyLarge,
            shape = FieldShape,
            colors = fieldColors(),
            modifier = Modifier
                .menuAnchor(MenuAnchorType.PrimaryEditable)
                .fillMaxWidth()
                .heightIn(min = 60.dp)
                .then(if (testTag != null) Modifier.testTag(testTag) else Modifier),
        )
        ExposedDropdownMenu(expanded = open && matches.isNotEmpty(), onDismissRequest = { open = false }) {
            matches.forEach { s ->
                DropdownMenuItem(
                    text = { Text(s, style = MaterialTheme.typography.bodyLarge, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                    onClick = { onPick(s); open = false },
                    modifier = Modifier.heightIn(min = 56.dp),
                )
            }
        }
    }
}

/** A date / time shown as a field: label above, value, icon, chevron. Tap to change. */
@Composable
fun VkPickerField(
    label: String,
    value: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: VkIcon = VkIcon.PANCHANG,
    testTag: String? = null,
) {
    val cs = MaterialTheme.colorScheme
    Row(
        modifier
            .fillMaxWidth()
            .heightIn(min = 64.dp)
            .clip(FieldShape)
            .background(cs.surfaceContainerLowest)
            .border(1.dp, cs.outline, FieldShape)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 8.dp)
            .then(if (testTag != null) Modifier.testTag(testTag) else Modifier),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        VkIconImage(icon, size = 32.dp)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(label, style = MaterialTheme.typography.bodySmall, color = cs.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
            // Up to 2 lines: "05-11-2026 · આસો વદ અગિયારસ" must never be cut.
            Text(value, style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold), color = cs.onSurface, maxLines = 2, overflow = TextOverflow.Ellipsis)
        }
        Icon(Icons.Rounded.KeyboardArrowRight, contentDescription = null, tint = cs.onSurfaceVariant)
    }
}

/**
 * Equal-width option tiles in a fixed grid (no ragged wrapping). Single select ([selected]) or
 * multi select ([selectedSet]). Labels are one line and ellipsize instead of growing the tile.
 */
@Composable
fun <T> VkChoiceGrid(
    options: List<Pair<T, String>>,
    onToggle: (T) -> Unit,
    modifier: Modifier = Modifier,
    columns: Int = 2,
    selected: T? = null,
    selectedSet: Set<T>? = null,
    /** Optional last tile, same shape as the others (e.g. "+ નવો પ્રકાર"). */
    addLabel: String? = null,
    onAdd: (() -> Unit)? = null,
) {
    val cs = MaterialTheme.colorScheme
    // Same tile as Entry's farm / section tiles: 16dp radius, 64dp min, centred one-line label.
    val shape = RoundedCornerShape(16.dp)
    val items: List<Any?> = options + if (addLabel != null && onAdd != null) listOf(TrailingSlot) else emptyList()
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        items.chunked(columns).forEach { row ->
            Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                row.forEach { item ->
                    if (item === TrailingSlot) {
                        Box(
                            Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .heightIn(min = 64.dp)
                                .clip(shape)
                                .background(cs.surfaceContainerLowest)
                                .border(1.5.dp, cs.primary, shape)
                                .clickable(role = Role.Button) { onAdd?.invoke() }
                                .padding(horizontal = 12.dp, vertical = 10.dp)
                                .testTag("choice_add"),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(addLabel.orEmpty(), style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold), color = cs.primary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                    } else {
                        @Suppress("UNCHECKED_CAST")
                        val pair = item as Pair<T, String>
                        val value = pair.first
                        val label = pair.second
                        val on = if (selectedSet != null) value in selectedSet else value == selected
                        Box(
                            Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .heightIn(min = 64.dp)
                                .clip(shape)
                                .background(if (on) cs.primaryContainer else cs.surfaceContainerLowest)
                                .border(if (on) 2.dp else 1.dp, if (on) cs.primary else cs.outlineVariant, shape)
                                .clickable(role = Role.Button) { onToggle(value) }
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                if (on) {
                                    Icon(Icons.Rounded.Check, contentDescription = null, tint = cs.primary, modifier = Modifier.size(20.dp))
                                    Spacer(Modifier.width(6.dp))
                                }
                                Text(
                                    label,
                                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = if (on) FontWeight.Bold else FontWeight.Medium),
                                    color = if (on) cs.onPrimaryContainer else cs.onSurface,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                        }
                    }
                }
                repeat(columns - row.size) { Spacer(Modifier.weight(1f)) }
            }
        }
    }
}

private object TrailingSlot
