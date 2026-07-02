package com.holadeutsch.app

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Happy-path smoke test: the app launches and the quiz entry point is visible. */
@RunWith(AndroidJUnit4::class)
class HomeSmokeTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun homeShowsQuizButton() {
        composeRule.onNodeWithText("Empezar quiz").assertIsDisplayed()
    }
}
