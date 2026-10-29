package com.vglstudio.camera.pose.line.guide.core.ads

import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Ad Inspector of the staging ad gallery: SDK result mapping, and no SDK call without an ads controller. */
@RunWith(AndroidJUnit4::class)
class AdInspectorTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun nullError_isClosed() {
        assertEquals(AdInspectorResult.Closed, adInspectorResult(null))
    }

    @Test
    fun errorCode_isFailedWithCode() {
        assertEquals(AdInspectorResult.Failed(3), adInspectorResult(3))
        // Control: the code is kept, not replaced by a fixed value.
        assertEquals(AdInspectorResult.Failed(0), adInspectorResult(0))
    }

    @Test
    fun withoutController_reportsNotReady() {
        val results = mutableListOf<AdInspectorResult>()
        composeRule.setContent {
            val open = rememberOpenAdInspector { results += it }
            Button(onClick = open) { Text("open") }
        }

        composeRule.onNodeWithText("open").performClick()
        composeRule.waitForIdle()

        assertEquals(listOf<AdInspectorResult>(AdInspectorResult.NotReady), results)
    }
}
