package com.vraj.khetihisab.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.vraj.khetihisab.R
import com.vraj.khetihisab.data.entity.Priority
import com.vraj.khetihisab.ui.theme.LocalVkExtra
import com.vraj.khetihisab.ui.theme.VkDimens

/*
 * Dialogs, sheets and bars shared by every screen (design audit A). One look for all of them:
 * 28dp dialogs on the card surface, stacked full-width actions (doc 01 §8.5), single-line labels.
 */

/**
 * The app dialog. Title in headlineSmall, scrollable [content], actions stacked full width:
 * confirm (filled, or [VkDangerButton] when [danger]) above the dismiss text button.
 * [dismissLabel] = null hides the dismiss button (back / outside tap still close it).
 */
@Composable
fun VkDialog(
    title: String,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    confirmLabel: String? = null,
    onConfirm: (() -> Unit)? = null,
    dismissLabel: String? = stringResource(R.string.scr_cancel),
    confirmEnabled: Boolean = true,
    danger: Boolean = false,
    dismissible: Boolean = true,
    /** Delete lives with the actions (never inside scrolling content, where it can be cut off). */
    deleteLabel: String? = null,
    onDelete: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit = {},
) {
    val scroll = rememberScrollState()
    AlertDialog(
        onDismissRequest = { if (dismissible) onDismiss() },
        modifier = modifier,
        shape = RoundedCornerShape(28.dp),
        containerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
        titleContentColor = MaterialTheme.colorScheme.onSurface,
        textContentColor = MaterialTheme.colorScheme.onSurface,
        title = { Text(title, style = MaterialTheme.typography.headlineSmall, modifier = Modifier.semantics { heading() }) },
        text = {
            Column {
                Column(
                    Modifier.weight(1f, fill = false).verticalScroll(scroll),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    content = content,
                )
                // A hairline tells the user there is more above/below, instead of a half-cut control.
                if (scroll.maxValue > 0) {
                    androidx.compose.material3.HorizontalDivider(
                        Modifier.padding(top = 8.dp),
                        color = MaterialTheme.colorScheme.outlineVariant,
                    )
                }
            }
        },
        confirmButton = {
            Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (confirmLabel != null && onConfirm != null) {
                    if (danger) {
                        VkDangerButton(confirmLabel, onConfirm, Modifier.fillMaxWidth(), enabled = confirmEnabled)
                    } else {
                        VkPrimaryButton(confirmLabel, onConfirm, Modifier.fillMaxWidth(), enabled = confirmEnabled)
                    }
                }
                if (deleteLabel != null && onDelete != null) VkDangerButton(deleteLabel, onDelete, Modifier.fillMaxWidth())
                if (dismissLabel != null) VkTextButton(dismissLabel, onDismiss, Modifier.fillMaxWidth())
            }
        },
    )
}

/**
 * The one delete pattern (audit #7): "કાઢી નાખવું છે?" + what is deleted + a red outlined
 * "કાઢી નાખો". [undoable] tells the user they can bring it back from the snackbar.
 */
@Composable
fun VkConfirmDeleteDialog(
    what: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    undoable: Boolean = true,
    title: String = stringResource(R.string.v5_delete_title),
) {
    VkDialog(
        title = title,
        onDismiss = onDismiss,
        confirmLabel = stringResource(R.string.scr_delete),
        onConfirm = onConfirm,
        danger = true,
    ) {
        if (what.isNotBlank()) Text(what, style = MaterialTheme.typography.bodyLarge)
        Text(
            stringResource(if (undoable) R.string.v5_delete_body_undo else R.string.v5_delete_body_final),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/**
 * Bottom sheet for every add/edit/list panel (audit #9, #30): card surface, title, scrollable body
 * that rises above the keyboard (imePadding), status-bar icons matched to the sheet.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VkBottomSheet(
    title: String,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    scrollable: Boolean = true,
    content: @Composable ColumnScope.() -> Unit,
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        modifier = modifier,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
    ) {
        VkSheetSystemBars()
        Column(
            Modifier
                .fillMaxWidth()
                .imePadding()
                .then(if (scrollable) Modifier.verticalScroll(rememberScrollState()) else Modifier)
                .navigationBarsPadding()
                .padding(bottom = 16.dp),
        ) {
            Text(
                title,
                style = MaterialTheme.typography.titleLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp).semantics { heading() },
            )
            content()
        }
    }
}

/**
 * Sticky bottom bar for a screen's main action(s) (audit #23): dock surface, hairline on top,
 * clears the navigation bar. [content] is a Row — give buttons `Modifier.weight(1f)`.
 */
@Composable
fun VkActionBar(modifier: Modifier = Modifier, content: @Composable RowScope.() -> Unit) {
    val cs = MaterialTheme.colorScheme
    Column(modifier.fillMaxWidth().background(LocalVkExtra.current.dock)) {
        Box(Modifier.fillMaxWidth().height(1.dp).background(cs.outlineVariant))
        Row(
            Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 16.dp)),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            content = content,
        )
    }
}

/** Material icon action with a 60dp touch target (edit / delete on list rows, audit #13, #14). */
@Composable
fun VkIconAction(icon: ImageVector, label: String, onClick: () -> Unit, modifier: Modifier = Modifier, danger: Boolean = false) {
    val cs = MaterialTheme.colorScheme
    IconButton(onClick = onClick, modifier = modifier.size(VkDimens.minTouch)) {
        Box(Modifier.size(40.dp).background(if (danger) cs.errorContainer else cs.surfaceContainer, CircleShape), contentAlignment = Alignment.Center) {
            Icon(icon, contentDescription = label, tint = if (danger) cs.onErrorContainer else cs.onSurfaceVariant, modifier = Modifier.size(22.dp))
        }
    }
}

/** Reminder priority badge (audit #24): જરૂરી on primaryContainer, સામાન્ય on surfaceContainer. */
@Composable
fun VkPriorityBadge(priority: Priority, modifier: Modifier = Modifier) {
    val cs = MaterialTheme.colorScheme
    when (priority) {
        Priority.HIGH -> VkBadge(stringResource(R.string.rem_high), cs.primaryContainer, cs.onPrimaryContainer, modifier)
        Priority.NORMAL -> VkBadge(stringResource(R.string.rem_normal), cs.surfaceContainer, cs.onSurfaceVariant, modifier)
    }
}

/** Spacer-height helper for content that must clear a [VkActionBar] / dock. */
val VkActionBarClearance = 112.dp

/**
 * M3 date picker in the app's dialog style. Dates after [maxDate] are ignored (no future entries).
 * Dates are converted at UTC midnight, as the M3 picker expects.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VkDatePickerDialog(
    date: java.time.LocalDate,
    onPick: (java.time.LocalDate) -> Unit,
    onDismiss: () -> Unit,
    maxDate: java.time.LocalDate? = null,
) {
    val state = androidx.compose.material3.rememberDatePickerState(
        initialSelectedDateMillis = date.atStartOfDay().toInstant(java.time.ZoneOffset.UTC).toEpochMilli(),
    )
    androidx.compose.material3.DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            VkTextButton(stringResource(R.string.scr_ok), {
                state.selectedDateMillis?.let { ms ->
                    val d = java.time.Instant.ofEpochMilli(ms).atZone(java.time.ZoneOffset.UTC).toLocalDate()
                    if (maxDate == null || !d.isAfter(maxDate)) onPick(d)
                }
                onDismiss()
            })
        },
        dismissButton = { VkTextButton(stringResource(R.string.scr_cancel), onDismiss) },
    ) { androidx.compose.material3.DatePicker(state = state) }
}

/** Tonal date chip used inside sheets ("તારીખ: 05/11/2026"), 56dp, opens [VkDatePickerDialog]. */
@Composable
fun VkDateChip(label: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val cs = MaterialTheme.colorScheme
    androidx.compose.material3.AssistChip(
        onClick = onClick,
        label = { Text(label, style = MaterialTheme.typography.labelLarge, maxLines = 1, softWrap = false) },
        leadingIcon = {
            Icon(androidx.compose.material.icons.Icons.Rounded.CalendarMonth, contentDescription = null, tint = cs.primary, modifier = Modifier.size(20.dp))
        },
        shape = RoundedCornerShape(12.dp),
        modifier = modifier.height(56.dp).testTag("date_chip"),
    )
}
