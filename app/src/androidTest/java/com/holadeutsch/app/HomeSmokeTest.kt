package com.holadeutsch.app

import android.Manifest
import android.os.Build
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.isDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.rule.GrantPermissionRule
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Happy-path smoke test: first-run setup completes and the quiz entry point is visible. */
@RunWith(AndroidJUnit4::class)
class HomeSmokeTest {

    @get:Rule(order = 0)
    val notificationPermissionRule: GrantPermissionRule =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            GrantPermissionRule.grant(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            GrantPermissionRule.grant()
        }

    @get:Rule(order = 1)
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun onboardingLeadsToHomeWithQuizButton() {
        // Wait for the start destination to resolve (DataStore read).
        composeRule.waitUntil(5_000) {
            composeRule.onNodeWithText("¡Hola! 👋").isDisplayed() ||
                composeRule.onNodeWithText("Empezar quiz").isDisplayed()
        }

        // On a fresh install the setup screen appears first: complete it.
        if (composeRule.onNodeWithText("¡Hola! 👋").isDisplayed()) {
            composeRule.onNodeWithText("Tu nombre").performTextInput("Ana")
            composeRule.onNodeWithText("25 palabras al día").performClick()
            composeRule.onNodeWithText("¡Empezar!").performClick()
        }

        composeRule.onNodeWithText("Empezar quiz").assertIsDisplayed()
    }
}
