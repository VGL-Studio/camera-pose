package com.vglstudio.camera.pose.line.guide.core.ads

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Text
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.vglstudio.camera.pose.line.guide.core.analytics.AdFunnelEvent
import com.vglstudio.camera.pose.line.guide.core.analytics.AdFunnelKind
import com.vglstudio.camera.pose.line.guide.core.analytics.RecordingAdFunnel
import kotlinx.coroutines.flow.flowOf
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

/**
 * `af_native_ad_eligible` (section 8.3; plan T-033): once each time a native slot that passed the app's gate comes into
 * the composition; a recomposition does not count, leaving and coming back does.
 */
@RunWith(AndroidJUnit4::class)
class NativeEligibleReportTest {
    @get:Rule
    val composeRule = createComposeRule()

    private var reports = 0

    @Test
    fun enterOnce_recomposeDoesNotReport() {
        var label by mutableIntStateOf(0)
        composeRule.setContent {
            NativeEligibleEffect(allowed = true) { reports++ }
            Text("label $label")
        }
        composeRule.waitForIdle()
        assertEquals(1, reports)

        label = 1
        composeRule.waitForIdle()
        // Control: the screen did recompose.
        composeRule.onNodeWithText("label 1").assertExists()
        assertEquals(1, reports)
    }

    @Test
    fun leaveAndEnter_reportsAgain() {
        var present by mutableStateOf(true)
        composeRule.setContent {
            if (present) NativeEligibleEffect(allowed = true) { reports++ }
        }
        composeRule.waitForIdle()
        assertEquals(1, reports)

        present = false
        composeRule.waitForIdle()
        assertEquals(1, reports)
        present = true
        composeRule.waitForIdle()
        assertEquals(2, reports)
    }

    @Test
    fun notAllowed_reportsNothing_thenAllowed_reportsOnce() {
        var allowed by mutableStateOf(false)
        composeRule.setContent { NativeEligibleEffect(allowed = allowed) { reports++ } }
        composeRule.waitForIdle()
        assertEquals(0, reports)

        // Control (L-055): allowed, the same slot reports once.
        allowed = true
        composeRule.waitForIdle()
        assertEquals(1, reports)
    }

    @Test
    fun slotGate_withController_recordsNativeEligible() {
        val funnel = RecordingAdFunnel()
        val controller = testAdsController(funnel = funnel)
        var allowed by mutableStateOf(false)
        composeRule.setContent {
            CompositionLocalProvider(LocalAdsController provides controller) {
                NativeAdSlotGate(
                    allowed = allowed,
                    states = flowOf(NativeAdState.Loading),
                    size = NativeAdSize.SMALL,
                    keepSpaceWhenEmpty = false,
                    onAdAvailabilityChange = {},
                    modifier = Modifier,
                    adContent = {},
                )
            }
        }
        composeRule.waitForIdle()
        assertTrue(funnel.events.isEmpty())

        allowed = true
        composeRule.waitForIdle()
        assertEquals(listOf<AdFunnelEvent>(AdFunnelEvent.Eligible(AdFunnelKind.NATIVE)), funnel.events)
    }

    @Test
    fun slotGate_offScreen_recordsNothing_thenOnScreen_recordsOnce() {
        val funnel = RecordingAdFunnel()
        val controller = testAdsController(funnel = funnel)
        var onScreen by mutableStateOf(false)
        composeRule.setContent {
            CompositionLocalProvider(LocalAdsController provides controller) {
                NativeAdSlotGate(
                    allowed = true,
                    states = flowOf(NativeAdState.Loading),
                    size = NativeAdSize.SMALL,
                    keepSpaceWhenEmpty = false,
                    onAdAvailabilityChange = {},
                    modifier = Modifier,
                    onScreen = onScreen,
                    adContent = {},
                )
            }
        }
        composeRule.waitForIdle()
        // A page of a pager kept in composition off screen has not come into view.
        assertTrue(funnel.events.isEmpty())

        onScreen = true
        composeRule.waitForIdle()
        assertEquals(listOf<AdFunnelEvent>(AdFunnelEvent.Eligible(AdFunnelKind.NATIVE)), funnel.events)
    }

    @Test
    fun fullPageStepGate_withController_recordsOnlyWhenShown() {
        val funnel = RecordingAdFunnel()
        val controller = testAdsController(funnel = funnel)
        var shown by mutableStateOf(false)
        composeRule.setContent {
            CompositionLocalProvider(LocalAdsController provides controller) {
                NativeFullScreenStepGate(
                    allowed = true,
                    states = flowOf(NativeAdState.Loading),
                    shown = shown,
                    adContent = {},
                )
            }
        }
        composeRule.waitForIdle()
        // A page of the flow kept but not on screen has not come into view.
        assertTrue(funnel.events.isEmpty())

        shown = true
        composeRule.waitForIdle()
        assertEquals(listOf<AdFunnelEvent>(AdFunnelEvent.Eligible(AdFunnelKind.NATIVE)), funnel.events)
    }

    @Test
    fun fullScreenStep_belowMinHeight_noEligible() {
        val funnel = RecordingAdFunnel()
        val controller = testAdsController(funnel = funnel)
        var height by mutableStateOf(151.dp)
        composeRule.setContent {
            CompositionLocalProvider(LocalAdsController provides controller) {
                Box(Modifier.fillMaxWidth().height(height)) {
                    NativeFullScreenStepGate(
                        allowed = true,
                        states = flowOf(NativeAdState.Loading),
                        shown = true,
                        adContent = {},
                    )
                }
            }
        }
        composeRule.waitForIdle()
        // A page too short to take an ad is not a chance to show one (plan T-041, question 6).
        assertTrue(funnel.events.isEmpty())

        // Control (L-055): at the least height, once.
        height = 152.dp
        composeRule.waitForIdle()
        assertEquals(listOf<AdFunnelEvent>(AdFunnelEvent.Eligible(AdFunnelKind.NATIVE)), funnel.events)
    }

    /** KB-20 (plan T-049): the page area of a phone in landscape (186.5 dp) is a chance to show the ad. */
    @Test
    @Config(qualifiers = "w873dp-h393dp-440dpi")
    fun fullScreenStep_phoneLandscape_eligible() {
        val funnel = RecordingAdFunnel()
        val controller = testAdsController(funnel = funnel)
        composeRule.setContent {
            CompositionLocalProvider(LocalAdsController provides controller) {
                Box(Modifier.fillMaxWidth().height(186.5.dp)) {
                    NativeFullScreenStepGate(
                        allowed = true,
                        states = flowOf(NativeAdState.Loading),
                        shown = true,
                        adContent = {},
                    )
                }
            }
        }
        composeRule.waitForIdle()
        assertEquals(listOf<AdFunnelEvent>(AdFunnelEvent.Eligible(AdFunnelKind.NATIVE)), funnel.events)
    }
}
