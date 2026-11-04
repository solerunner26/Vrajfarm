package com.vraj.khetihisab.ui.home

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.DeviceConfigurationOverride
import androidx.compose.ui.test.FontScale
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.hasTestTag
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.vraj.khetihisab.data.entity.AccountSection
import com.vraj.khetihisab.data.model.SectionTotals
import com.vraj.khetihisab.data.model.Totals
import com.vraj.khetihisab.data.model.UdhaarSummary
import com.vraj.khetihisab.ui.theme.VkTheme
import com.vraj.khetihisab.ui.theme.VkThemeId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.time.LocalDate

/** doc 05 "Compose UI test": the dock opens Entry with the right kind; cards navigate; alerts hide. */
@RunWith(AndroidJUnit4::class)
class HomeScreenUiTest {

    @get:Rule val rule = createComposeRule()

    private val today = LocalDate.of(2026, 11, 4)

    private fun state(udhaar: UdhaarSummary = UdhaarSummary.EMPTY) = HomeUiState(
        today = today,
        udhaar = udhaar,
        business = BusinessUi(
            Totals(60_800_00, 20_270_00),
            HomeLogic.businessBreakdown(listOf(SectionTotals(AccountSection.FARM, 48_800_00, 7_350_00))),
            2026,
        ),
    )

    private fun show(s: HomeUiState, routes: MutableList<String>) = rule.setContent {
        VkTheme(VkThemeId.GREEN) { HomeScreen(s, onOpen = { routes += it }, onSettle = {}, onLoanWhatsApp = {}) }
    }

    @Test fun dockIncomeOpensEntryWithIncome() {
        val routes = mutableListOf<String>()
        show(state(), routes)
        rule.onNodeWithText("આવક / વેચાણ", substring = true).performClick()
        assertEquals("entry?kind=income", routes.last())
    }

    @Test fun dockExpenseOpensEntryWithExpense() {
        val routes = mutableListOf<String>()
        show(state(), routes)
        rule.onNodeWithText("ખર્ચ / ઉપાડ", substring = true).performClick()
        assertEquals("entry?kind=expense", routes.last())
    }

    @Test fun udhaarCardHiddenWhenZero() {
        show(state(), mutableListOf())
        rule.onNodeWithTag("home_udhaar").assertDoesNotExist()
    }

    @Test fun udhaarCardShownWhenPending() {
        val routes = mutableListOf<String>()
        show(state(UdhaarSummary(18_400_00, 62_000_00, 5)), routes)
        rule.onNodeWithTag("home_udhaar").assertIsDisplayed()
        rule.onNodeWithText("₹18,400").assertIsDisplayed()
    }

    @Test fun farmCardOpensFarms() {
        val routes = mutableListOf<String>()
        show(state(), routes)
        rule.onNodeWithTag("home_list").performScrollToNode(hasTestTag("home_farms"))
        rule.onNodeWithTag("home_farms").performClick()
        assertTrue(routes.contains(HomeRoutes.FARMS))
    }

    // ---- Kul Nafo-Nukshan hidden on Home (owner request 2026-11-04) ----

    @Test fun totalsHiddenByDefault() {
        show(state(), mutableListOf())
        rule.onNodeWithTag("home_list").performScrollToNode(hasTestTag("home_business"))
        rule.onAllNodesWithText(com.vraj.khetihisab.ui.components.VK_MASK).assertCountEquals(4)
        rule.onAllNodesWithText("₹40,530").assertCountEquals(0)
    }

    @Test fun eyeRevealsAndHidesTotals() {
        show(state(), mutableListOf())
        rule.onNodeWithTag("home_list").performScrollToNode(hasTestTag("home_business"))
        rule.onNodeWithTag("eye_toggle").performClick()
        rule.waitForIdle()
        rule.onNodeWithText("₹40,530").assertIsDisplayed()
        rule.onNodeWithTag("eye_toggle").performClick()
        rule.waitForIdle()
        rule.onAllNodesWithText("₹40,530").assertCountEquals(0)
    }

    @Test fun showNumbersSettingRevealsWithoutTap() {
        show(state().copy(showNumbers = true), mutableListOf())
        rule.onNodeWithTag("home_list").performScrollToNode(hasTestTag("home_business"))
        rule.waitForIdle()
        rule.onNodeWithText("₹40,530").assertIsDisplayed()
    }

    // ---- every Home tile opens its own screen (Samanya once opened Settings) ----

    private fun tileOpens(tag: String, route: String) {
        val routes = mutableListOf<String>()
        show(state(), routes)
        rule.onNodeWithTag("home_list").performScrollToNode(hasTestTag(tag))
        rule.onNodeWithTag(tag).performClick()
        assertEquals(route, routes.lastOrNull())
    }

    @Test fun samanyaTileOpensGeneralNotSettings() = tileOpens("home_tile_general", HomeRoutes.GENERAL)
    @Test fun bhagiyaTileOpensAdvances() = tileOpens("home_tile_family", HomeRoutes.FAMILY)
    @Test fun angatTileOpensPersonal() = tileOpens("home_tile_personal", HomeRoutes.PERSONAL)
    @Test fun kharidiTileOpensShopping() = tileOpens("home_tile_shop", HomeRoutes.SHOPPING)
    @Test fun angatHisabSubTileOpensPersonal() = tileOpens("home_personal_hisab", HomeRoutes.PERSONAL)

    @Test fun noTileOpensSettingsExceptTheGear() {
        val routes = mutableListOf<String>()
        show(state(), routes)
        listOf("home_tile_family", "home_tile_personal", "home_tile_general", "home_tile_shop", "home_farms", "home_personal_hisab").forEach { tag ->
            rule.onNodeWithTag("home_list").performScrollToNode(hasTestTag(tag))
            rule.onNodeWithTag(tag).performClick()
        }
        assertTrue("tiles must not open Settings: $routes", routes.none { it == HomeRoutes.SETTINGS })
    }

    // ---- android/skills testing-setup step 11: large font must still lay out ----

    @Test fun tilesSurviveFontScale150() {
        rule.setContent {
            DeviceConfigurationOverride(DeviceConfigurationOverride.FontScale(1.5f)) {
                VkTheme(VkThemeId.MATI) { HomeScreen(state(), onOpen = {}, onSettle = {}, onLoanWhatsApp = {}) }
            }
        }
        rule.onNodeWithTag("home_list").performScrollToNode(hasTestTag("home_tile_general"))
        rule.onNodeWithTag("home_tile_general").assertIsDisplayed()
    }
}
