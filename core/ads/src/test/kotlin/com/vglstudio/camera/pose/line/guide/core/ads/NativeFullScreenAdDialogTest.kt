package com.vglstudio.camera.pose.line.guide.core.ads

import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedDispatcher
import androidx.activity.compose.LocalOnBackPressedDispatcherOwner
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * The dialog that holds the native full-screen ad (KB-5, criterion 2): the back button closes the ad, a touch outside
 * does not. The dialog is its own window with its own back dispatcher (plan T-013, section 4.1).
 */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w411dp-h891dp-xxhdpi")
class NativeFullScreenAdDialogTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun dialogProperties_closeOnBackOnly() {
        val properties = nativeFullScreenDialogProperties()
        assertTrue(properties.dismissOnBackPress)
        assertFalse(properties.dismissOnClickOutside)
        assertFalse(properties.usePlatformDefaultWidth)
    }

    @Test
    fun backPress_callsOnClose() {
        var closes = 0
        var dispatcher: OnBackPressedDispatcher? = null
        composeRule.setContent {
            NativeFullScreenAdDialog(onClose = { closes++ }) {
                dispatcher = LocalOnBackPressedDispatcherOwner.current!!.onBackPressedDispatcher
                Box(modifier = Modifier.fillMaxSize().testTag("fakeAd"))
            }
        }
        composeRule.waitForIdle()
        composeRule.runOnUiThread { dispatcher!!.onBackPressed() }
        composeRule.waitForIdle()
        assertEquals(1, closes)
        assertFalse(composeRule.activity.isFinishing)
    }
}
