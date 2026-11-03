package com.vraj.khetihisab.ui.components

import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Backspace
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vraj.khetihisab.R
import com.vraj.khetihisab.ui.theme.LocalVkExtra

/**
 * Segmented button (css .vk-seg): outlined 28dp pill track, equal segments, 56dp tall.
 * The ✓ comes from the selected style, never from the label (doc 17 #2). Labels clip with ellipsis.
 */
@Composable
fun <T> VkSegmented(
    options: List<Pair<T, String>>,
    selected: T,
    onSelect: (T) -> Unit,
    modifier: Modifier = Modifier,
    errorSelected: Boolean = false,
) {
    val cs = MaterialTheme.colorScheme
    val shape = RoundedCornerShape(28.dp)
    Row(
        modifier
            .fillMaxWidth()
            .height(56.dp)
            .clip(shape)
            .border(1.dp, cs.outline, shape)
            .selectableGroup(),
    ) {
        options.forEachIndexed { i, (value, label) ->
            val on = value == selected
            if (i > 0) Box(Modifier.width(1.dp).fillMaxHeight().background(cs.outline))
            val bg = when {
                !on -> androidx.compose.ui.graphics.Color.Transparent
                errorSelected -> cs.errorContainer
                else -> cs.primaryContainer
            }
            val fg = when {
                !on -> cs.onSurfaceVariant
                errorSelected -> cs.onErrorContainer
                else -> cs.onPrimaryContainer
            }
            Row(
                Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .background(bg)
                    .selectable(selected = on, role = Role.RadioButton, onClick = { onSelect(value) })
                    .padding(horizontal = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
            ) {
                if (on) {
                    Icon(Icons.Rounded.Check, contentDescription = null, tint = fg, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(4.dp))
                }
                Text(
                    label,
                    color = fg,
                    style = MaterialTheme.typography.labelLarge.copy(fontSize = 15.sp),
                    maxLines = 1,
                    softWrap = false,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

/** Filter chip, 48dp, 12dp radius, selected = primaryContainer + check (doc 01 §2). */
@Composable
fun VkChip(text: String, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val cs = MaterialTheme.colorScheme
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = {
            Text(text, style = MaterialTheme.typography.labelLarge.copy(fontSize = 15.sp), maxLines = 1, softWrap = false, overflow = TextOverflow.Ellipsis)
        },
        leadingIcon = if (selected) {
            { Icon(Icons.Rounded.Check, contentDescription = null, modifier = Modifier.size(18.dp)) }
        } else {
            null
        },
        modifier = modifier.heightIn(min = 48.dp),
        shape = RoundedCornerShape(12.dp),
        colors = FilterChipDefaults.filterChipColors(
            containerColor = cs.surfaceContainerLowest,
            labelColor = cs.onSurface,
            selectedContainerColor = cs.primaryContainer,
            selectedLabelColor = cs.onPrimaryContainer,
            selectedLeadingIconColor = cs.onPrimaryContainer,
        ),
        border = FilterChipDefaults.filterChipBorder(
            enabled = true,
            selected = selected,
            borderColor = cs.outline,
            selectedBorderColor = cs.primaryContainer,
        ),
    )
}

/**
 * 3×4 numeric keypad. Default keys are 64dp rounded squares (css .vk-key); the lock screen uses
 * 76dp round keys via [keySize] + [keyShape]. Left-bottom key is "." when [allowDecimal].
 */
@Composable
fun VkNumericKeypad(
    onKey: (Char) -> Unit,
    onBackspace: () -> Unit,
    modifier: Modifier = Modifier,
    allowDecimal: Boolean = true,
    keySize: Dp = 64.dp,
    keyShape: Shape = RoundedCornerShape(16.dp),
    fillWidth: Boolean = true,
) {
    val rows = listOf(listOf('1', '2', '3'), listOf('4', '5', '6'), listOf('7', '8', '9'), listOf(if (allowDecimal) '.' else ' ', '0', '<'))
    val cs = MaterialTheme.colorScheme
    val x = LocalVkExtra.current
    val backLabel = stringResource(R.string.keypad_backspace)
    Column(modifier, verticalArrangement = Arrangement.spacedBy(12.dp)) {
        rows.forEach { row ->
            Row(
                if (fillWidth) Modifier.fillMaxWidth() else Modifier,
                horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterHorizontally),
            ) {
                row.forEach { k ->
                    // fillWidth: keys share the row width (entry keypad); otherwise fixed square/round keys (lock).
                    val keyMod = if (fillWidth) Modifier.weight(1f).height(keySize) else Modifier.size(keySize)
                    if (k == ' ') {
                        Spacer(keyMod)
                    } else {
                        Box(
                            keyMod
                                .clip(keyShape)
                                .background(x.key)
                                .border(1.dp, cs.outlineVariant, keyShape)
                                .clickable(role = Role.Button) { if (k == '<') onBackspace() else onKey(k) }
                                .then(if (k == '<') Modifier.semantics { contentDescription = backLabel } else Modifier),
                            contentAlignment = Alignment.Center,
                        ) {
                            if (k == '<') {
                                Icon(Icons.AutoMirrored.Outlined.Backspace, contentDescription = null, tint = cs.onSurface, modifier = Modifier.size(28.dp))
                            } else {
                                Text(
                                    k.toString(),
                                    color = cs.onSurface,
                                    style = TextStyle(fontSize = 26.sp, fontWeight = FontWeight.Bold, fontFamily = MaterialTheme.typography.bodyLarge.fontFamily),
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
