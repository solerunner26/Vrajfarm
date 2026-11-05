package com.vraj.khetihisab.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

/** doc 01 §2: chips 12dp, cards 16dp, dialogs/sheets 28dp. Buttons use CircleShape (pill). */
val VkShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(28.dp),
    extraLarge = RoundedCornerShape(28.dp),
)

object VkDimens {
    val gutter = 16.dp
    val cardPadding = 16.dp
    /** Space between top-level cards/sections in a screen list (audit #27). */
    val listGap = 16.dp
    /** Sub-tile / inner tile radius (audit #29). */
    val subTileRadius = 16.dp
    val minTouch = 60.dp
    val buttonHeight = 60.dp
    val keypadKey = 64.dp
    val heroHeader = 170.dp
}
