package com.example.mediq.core.designsystem.component

import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.navigation.compose.rememberNavController
import com.example.mediq.core.designsystem.theme.MediQTheme
import org.junit.Rule
import org.junit.Test

/** Verifies the rendered navigation semantics, not just the BottomNavItem data. */
class MediQBottomBarSemanticsTest {
    @get:Rule val composeRule = createComposeRule()

    @Test
    fun bookingsTabHasVisibleTextAndAnAccessibleNameContainingIt() {
        composeRule.setContent {
            MediQTheme {
                MediQBottomBar(rememberNavController())
            }
        }

        composeRule.onNodeWithText("Bookings", useUnmergedTree = true).assertExists()
        composeRule.onNode(
            hasContentDescription("Bookings, appointments") and hasClickAction()
        ).assertExists()
    }
}
