package com.vraj.khetihisab.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.LocalContext
import androidx.glance.action.ActionParameters
import androidx.glance.action.actionParametersOf
import androidx.glance.appwidget.CheckBox
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.provideContent
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.material3.ColorProviders
import androidx.glance.text.TextStyle
import com.vraj.khetihisab.R
import com.vraj.khetihisab.notify.VkDeepLinks
import com.vraj.khetihisab.ui.theme.VkThemes
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** doc 13: 4×3 "ખરીદી બાકી" + count; up to 5 pending items (Farm first) with a tick box. No prices. */
class ShoppingListGlanceWidget : GlanceAppWidget() {
    override val sizeMode: SizeMode = SizeMode.Single

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val colors = WidgetTheme.colors(context)
        val state = withContext(Dispatchers.IO) {
            runCatching { WidgetLogic.shoppingState(WidgetGraph.source(context).pendingShopping()) }
                .getOrDefault(ShoppingWidgetState(0, emptyList()))
        }
        provideContent {
            GlanceTheme(colors = ColorProviders(VkThemes.colorScheme(VkThemes.palette(colors.id)))) {
                Content(colors, state)
            }
        }
    }

    @Composable
    private fun Content(c: WidgetColors, s: ShoppingWidgetState) {
        val ctx = LocalContext.current
        WidgetFrame(c) {
            WidgetHeader(
                c,
                R.drawable.ic_vk_shopping,
                ctx.getString(R.string.w_shop),
                badge = s.pendingCount.toString(),
                onClick = actionStartActivity(VkDeepLinks.intent(ctx, VkDeepLinks.SHOPPING)),
            )
            Spacer(GlanceModifier.height(6.dp))
            if (s.rows.isEmpty()) {
                Line(ctx.getString(R.string.w_shop_empty), c.onSurfaceVariant, 16.sp)
            }
            s.rows.forEach { row ->
                CheckBox(
                    checked = false,
                    onCheckedChange = actionRunCallback<ToggleShoppingAction>(actionParametersOf(ToggleShoppingAction.ITEM_ID to row.id)),
                    text = row.name,
                    style = TextStyle(color = c.onSurface, fontSize = 16.sp),
                    maxLines = 1,
                    modifier = GlanceModifier.fillMaxWidth().height(36.dp),
                )
            }
        }
    }
}

/** Tick → `setBought(true)` → redraw (doc 13). Works while the app is locked: it only writes a boolean. */
class ToggleShoppingAction : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        val id = parameters[ITEM_ID] ?: return
        WidgetActions.toggleShopping(WidgetGraph.source(context), id) {
            ShoppingListGlanceWidget().update(context, glanceId)
        }
    }

    companion object {
        val ITEM_ID = ActionParameters.Key<Long>("shopping_item_id")
    }
}

class ShoppingListWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = ShoppingListGlanceWidget()
}
