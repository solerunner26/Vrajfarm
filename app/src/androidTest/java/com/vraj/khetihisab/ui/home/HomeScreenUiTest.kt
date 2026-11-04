package com.vraj.khetihisab.ui.home

import androidx.compose.ui.test.assertIsDisplayed
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

    @Test fun businessCardShowsTotals() {
        show(state(), mutableListOf())
        rule.onNodeWithTag("home_list").performScrollToNode(hasTestTag("home_business"))
        rule.onNodeWithText("₹40,530").assertIsDisplayed()
    }
}
