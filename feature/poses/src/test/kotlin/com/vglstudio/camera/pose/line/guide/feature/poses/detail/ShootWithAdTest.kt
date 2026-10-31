package com.vglstudio.camera.pose.line.guide.feature.poses.detail

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.vglstudio.camera.pose.line.guide.core.ads.AdPlacement
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * "Shoot with this pose" with the interstitial before the camera (spec poses, KB-4; plan T-079, pass 2): without an
 * ad the camera opens at once with the pose, and a tap while an earlier one waits for its ad is ignored.
 */
@RunWith(AndroidJUnit4::class)
class ShootWithAdTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun noAdsController_opensAtOnce() {
        val opened = mutableListOf<String>()
        var shoot: (pick: () -> String?) -> Unit = {}
        composeRule.setContent {
            shoot = rememberShootWithAd(AdPlacement.INTER_POSE_DETAIL) { opened += it }
        }
        composeRule.waitForIdle()
        assertTrue(opened.isEmpty())

        composeRule.runOnUiThread { shoot { "pose_1" } }
        composeRule.waitForIdle()
        assertEquals(listOf("pose_1"), opened)

        // No wait is left behind, so the next tap opens the camera again.
        composeRule.runOnUiThread { shoot { "pose_1" } }
        composeRule.waitForIdle()
        assertEquals(listOf("pose_1", "pose_1"), opened)
    }

    @Test
    fun gate_secondTapWhileWaiting_pickNotCalled() {
        val gate = ShootGate()
        var picks = 0

        assertEquals(
            "a",
            gate.tryBegin {
                picks++
                "a"
            },
        )
        assertEquals(1, picks)

        // A tap while "a" waits for its ad: ignored, and the choice is not logged again.
        assertNull(
            gate.tryBegin {
                picks++
                "b"
            },
        )
        assertEquals(1, picks)

        assertEquals("a", gate.finish())
        assertNull(gate.finish())

        // Control: once the wait is over, the next tap is taken.
        assertEquals(
            "c",
            gate.tryBegin {
                picks++
                "c"
            },
        )
        assertEquals(2, picks)
    }

    @Test
    fun pickReturnsNull_noWait() {
        val gate = ShootGate()
        var picks = 0

        // No pose (the detail is not ready): nothing waits.
        assertNull(
            gate.tryBegin {
                picks++
                null
            },
        )
        assertEquals(1, picks)
        assertNull(gate.finish())

        // The next tap calls pick as usual.
        assertEquals(
            "d",
            gate.tryBegin {
                picks++
                "d"
            },
        )
        assertEquals(2, picks)
    }
}
