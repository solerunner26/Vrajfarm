package com.vraj.khetihisab.ui.entry

import android.graphics.Bitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.isDialog
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.getBoundsInRoot
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.vraj.khetihisab.data.entity.AccountSection
import com.vraj.khetihisab.data.entity.TxnType
import com.vraj.khetihisab.ui.theme.VkTheme
import com.vraj.khetihisab.ui.theme.VkThemeId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

/**
 * Owner request 2026-11-07: speech is confirmed before it fills the form, and the note sits above
 * the amount and keypad (never under the save bar). Screenshots go to the app's external files dir.
 */
@RunWith(AndroidJUnit4::class)
class EntryVoiceUiTest {

    @get:Rule val rule = createComposeRule()

    private val lists = EntryLists(
        farms = (1L..8L).map { EntryOption(it, "ખેતર $it") },
        families = listOf(EntryOption(1, "ભાગિયા 1"), EntryOption(2, "ભાગિયા 2")),
        categories = listOf(EntryOption(11, "ખાતર"), EntryOption(12, "બિયારણ")),
        allCategories = listOf(
            VoiceCategory(11, "ખાતર", AccountSection.FARM, TxnType.EXPENSE),
            VoiceCategory(12, "બિયારણ", AccountSection.FARM, TxnType.EXPENSE),
        ),
    )

    private fun shot(name: String, node: SemanticsNodeInteraction = rule.onRoot()) {
        rule.waitForIdle()
        val bmp = node.captureToImage().asAndroidBitmap()
        val dir = InstrumentationRegistry.getInstrumentation().targetContext.getExternalFilesDir("ui-shots") ?: return
        File(dir, "$name.png").outputStream().use { bmp.compress(Bitmap.CompressFormat.PNG, 100, it) }
    }

    @Test fun voiceDialogShowsSentenceAndChoices() {
        val text = "ખાતર બે હજાર ખેતર ત્રણ ઉધાર એગ્રો સેન્ટર"
        val parsed = VoiceEntryParser.parse(text, lists.farms.map { it.id to it.label }, lists.families.map { it.id to it.label }, lists.allCategories)
        val calls = mutableListOf<String>()
        rule.setContent {
            VkTheme(VkThemeId.MATI) {
                VoiceConfirmDialog(text, parsed, lists, onYes = { calls += "yes" }, onAgain = { calls += "again" }, onCancel = { calls += "cancel" })
            }
        }
        rule.onNodeWithText("તમે આ બોલ્યા – બરાબર છે?").assertIsDisplayed()
        rule.onNodeWithText(text, substring = true).assertIsDisplayed()
        rule.onNodeWithText("₹2,000", substring = true).assertIsDisplayed()
        rule.onNodeWithText("ફરી બોલો").assertIsDisplayed()
        shot("voice_dialog", rule.onNode(isDialog()))
        rule.onNodeWithText("ફરી બોલો").performClick()
        rule.onNodeWithText("હા, બરાબર છે").performClick()
        assertEquals(listOf("again", "yes"), calls)
    }

    @Test fun noteSitsAboveAmountAndKeypad() {
        val form = EntryForm(
            section = AccountSection.FARM,
            farmId = 3,
            categoryId = 11,
            voiceNote = "ખાતર બે હજાર ખેતર ત્રણ ઉધાર એગ્રો સેન્ટર, રમેશભાઈ પાસેથી લીધું – આવતા અઠવાડિયે ચૂકવવાનું",
        )
        rule.setContent {
            VkTheme(VkThemeId.MATI) {
                EntryContent(form, lists, onEvent = {}, onBack = {}, onSave = {}, onTakePhoto = {}, onVoice = {}, onViewBill = {})
            }
        }
        rule.onNode(hasScrollAction()).performScrollToNode(hasTestTag(EntryTestTags.NOTE))
        val note = rule.onNodeWithTag(EntryTestTags.NOTE).assertIsDisplayed().getBoundsInRoot()
        val save = rule.onNodeWithTag(EntryTestTags.SAVE).getBoundsInRoot()
        assertTrue("note clear of the save bar", note.bottom < save.top)
        shot("entry_note_above_keypad")
        rule.onNode(hasScrollAction()).performScrollToNode(hasTestTag(EntryTestTags.AMOUNT))
        val amount = rule.onNodeWithTag(EntryTestTags.AMOUNT).getBoundsInRoot()
        val noteAfter = rule.onAllNodes(hasTestTag(EntryTestTags.NOTE)).fetchSemanticsNodes().firstOrNull()?.boundsInRoot
        val amountTopPx = amount.top.value * rule.density.density
        // The note scrolled off the top (above the amount), or it is still visible and ends above it.
        assertTrue("note above amount", noteAfter == null || noteAfter.bottom <= amountTopPx)
    }
}
