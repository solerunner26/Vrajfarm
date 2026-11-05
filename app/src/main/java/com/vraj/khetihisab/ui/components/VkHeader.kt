package com.vraj.khetihisab.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.ArrowDropDown
import androidx.compose.ui.platform.testTag
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.vraj.khetihisab.R
import com.vraj.khetihisab.ui.decor.VkFieldMotif
import com.vraj.khetihisab.ui.decor.VkToranEdge
import com.vraj.khetihisab.ui.theme.LocalVkExtra
import com.vraj.khetihisab.ui.theme.VkDimens
import com.vraj.khetihisab.ui.theme.VkIcon
import com.vraj.khetihisab.ui.theme.VkIconImage

/**
 * Brand-gradient header band with the furrow/sun line-art and a toran-scallop bottom edge
 * (doc 01 §4.1). `hero = true` → at least 170dp (Home). Draws under the status bar (edge-to-edge).
 */
@Composable
fun VkTopHeader(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    onBack: (() -> Unit)? = null,
    actions: @Composable RowScope.() -> Unit = {},
    hero: Boolean = false,
    content: @Composable ColumnScope.() -> Unit = {},
) {
    val x = LocalVkExtra.current
    Box(
        modifier
            .fillMaxWidth()
            .background(x.brandBrush)
            .then(if (hero) Modifier.heightIn(min = VkDimens.heroHeader) else Modifier),
    ) {
        VkFieldMotif(Modifier.matchParentSize())
        Column(
            Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(start = 16.dp, end = 16.dp, top = 6.dp, bottom = 30.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(
                Modifier.fillMaxWidth().heightIn(min = 56.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                if (onBack != null) {
                    VkHeaderButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Rounded.ArrowBack,
                            contentDescription = stringResource(R.string.cd_back),
                            tint = x.onGradient,
                            modifier = Modifier.size(24.dp),
                        )
                    }
                }
                Column(Modifier.weight(1f)) {
                    Text(
                        title,
                        style = MaterialTheme.typography.headlineSmall,
                        color = x.onGradient,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.semantics { heading() },
                    )
                    if (subtitle != null) {
                        Text(
                            subtitle,
                            style = MaterialTheme.typography.bodySmall,
                            color = x.onGradient.copy(alpha = 0.88f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
                actions()
            }
            content()
        }
        VkToranEdge(Modifier.align(Alignment.BottomCenter))
    }
}

/**
 * Round header action on the gradient (css .vk-hbtn): a 60dp touch box (doc 01 §2) around a 48dp
 * visible disc, with a springy press.
 */
@Composable
fun VkHeaderButton(onClick: () -> Unit, modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    val interaction = androidx.compose.runtime.remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
    Box(
        modifier
            .size(VkDimens.minTouch)
            .vkPressScale(interaction)
            .clip(CircleShape)
            .clickable(interactionSource = interaction, indication = androidx.compose.material3.ripple(), role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            Modifier.size(48.dp).clip(CircleShape).background(LocalVkExtra.current.headerButton),
            contentAlignment = Alignment.Center,
        ) { content() }
    }
}

/** Header action showing an ic_vk icon (30dp). */
@Composable
fun VkHeaderIconButton(icon: VkIcon, label: String, onClick: () -> Unit) {
    VkHeaderButton(onClick = onClick, modifier = Modifier.semantics { contentDescription = label }) {
        VkIconImage(icon, size = 30.dp)
    }
}

/**
 * Month picker as a header chip (audit #32): "નવેમ્બર 2026" + a drop-down arrow icon, on a 60dp
 * touch box. Put it in [VkTopHeader] actions on every month-based screen.
 */
@Composable
fun VkMonthChip(label: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val x = LocalVkExtra.current
    Box(
        modifier
            .heightIn(min = VkDimens.minTouch)
            .clip(CircleShape)
            .clickable(role = Role.Button, onClick = onClick)
            .testTag("month_chip"),
        contentAlignment = Alignment.Center,
    ) {
        Row(
            Modifier.clip(CircleShape).background(x.headerButton).padding(start = 12.dp, end = 6.dp, top = 6.dp, bottom = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(label, color = x.onGradient, style = MaterialTheme.typography.labelLarge, maxLines = 1, softWrap = false)
            Icon(Icons.Rounded.ArrowDropDown, contentDescription = null, tint = x.onGradient, modifier = Modifier.size(24.dp))
        }
    }
}

/** Small pill on the header ("વિ.સં. 2082"). */
@Composable
fun VkHeaderChip(text: String, modifier: Modifier = Modifier) {
    val x = LocalVkExtra.current
    Box(
        modifier
            .clip(CircleShape)
            .background(x.headerButton)
            .padding(horizontal = 12.dp, vertical = 4.dp),
    ) {
        Text(text, color = x.onGradient, style = MaterialTheme.typography.labelMedium, maxLines = 1, softWrap = false)
    }
}
