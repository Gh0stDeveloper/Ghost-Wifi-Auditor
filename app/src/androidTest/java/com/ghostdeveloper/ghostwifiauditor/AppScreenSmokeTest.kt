package com.ghostdeveloper.ghostwifiauditor

import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Device/emulator smoke test. Compiled in CI, executed during M9 physical QA.
 */
@RunWith(AndroidJUnit4::class)
class AppScreenSmokeTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()

    @Test fun launchesAndOpensSettings() {
        compose.onNodeWithText("Ghost WiFi Auditor", useUnmergedTree = true).assertExists()
        compose.onNodeWithContentDescription("Ajustes", useUnmergedTree = true).performClick()
        // A fresh test device defaults to Spanish, but users can change languages.
        compose.onNodeWithText("AMOLED negro", useUnmergedTree = true).assertExists()
    }
}
