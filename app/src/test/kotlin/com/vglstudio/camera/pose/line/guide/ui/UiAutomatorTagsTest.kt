package com.vglstudio.camera.pose.line.guide.ui

import android.app.Application
import androidx.compose.foundation.layout.Box
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.SemanticsPropertiesAndroid
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

/**
 * `uiautomator dump` shows a Compose `testTag` as `resource-id` only when a parent sets `testTagsAsResourceId`
 * (scripts/device-scenario.py selects elements by it, plan T-046). A plain Application, so neither Hilt nor Firebase
 * starts.
 */
@OptIn(ExperimentalComposeUiApi::class)
@RunWith(AndroidJUnit4::class)
@Config(application = Application::class)
class UiAutomatorTagsTest {
    @get:Rule
    val composeRule = createComposeRule()

    private val exposesTags = SemanticsMatcher("TestTagsAsResourceId is true") {
        it.config.getOrNull(SemanticsPropertiesAndroid.TestTagsAsResourceId) == true
    }

    @Test
    fun exposeTestTags_setsTestTagsAsResourceId() {
        composeRule.setContent {
            Box(Modifier.exposeTestTagsForUiAutomator()) {
                Box(Modifier.testTag("tagged"))
            }
        }

        composeRule.onAllNodes(exposesTags, useUnmergedTree = true).assertCountEquals(1)
    }

    @Test
    fun withoutModifier_hasNoTestTagsAsResourceId() {
        composeRule.setContent {
            Box {
                Box(Modifier.testTag("tagged"))
            }
        }

        composeRule.onAllNodes(exposesTags, useUnmergedTree = true).assertCountEquals(0)
    }
}
