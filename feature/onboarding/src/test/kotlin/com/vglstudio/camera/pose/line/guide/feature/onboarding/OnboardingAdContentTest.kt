package com.vglstudio.camera.pose.line.guide.feature.onboarding

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.Snapshot
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.vglstudio.camera.pose.line.guide.core.designsystem.theme.AppTheme
import com.vglstudio.camera.pose.line.guide.feature.onboarding.OnboardingPage.CAMERA
import com.vglstudio.camera.pose.line.guide.feature.onboarding.OnboardingPage.INTRO
import com.vglstudio.camera.pose.line.guide.feature.onboarding.OnboardingPage.POSES
import com.vglstudio.camera.pose.line.guide.feature.onboarding.OnboardingPage.PREMIUM
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

/**
 * The wiring of [OnboardingAdContent] (plan T-083, section 4): the gate of the card, the report of the card and the
 * Next button of the real screen, with a fake card and the clock stopped.
 */
@RunWith(AndroidJUnit4::class)
@Config(qualifiers = "vi-w411dp-h891dp-xxhdpi")
class OnboardingAdContentTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    private val events = mutableListOf<String>()
    private var report: ((Boolean) -> Unit)? = null
    private val slotPages = mutableListOf<OnboardingPage>()

    private fun setContent(uiState: () -> OnboardingUiState, adsAllowed: Boolean) {
        composeRule.mainClock.autoAdvance = false
        composeRule.setContent {
            AppTheme {
                OnboardingAdContent(
                    uiState = uiState(),
                    adsAllowed = adsAllowed,
                    onNext = {},
                    onBack = {},
                    onEnableCamera = {},
                    onIntroAdWaitStarted = { events += "start" },
                    onIntroAdWaitEnded = { events += it.logName },
                    adSlot = { page, onAdAvailabilityChange ->
                        slotPages += page
                        report = onAdAvailabilityChange
                        Box(Modifier.fillMaxWidth().height(250.dp))
                    },
                )
            }
        }
    }

    private fun frame() {
        Snapshot.sendApplyNotifications()
        composeRule.mainClock.advanceTimeByFrame()
    }

    private fun advance(millis: Long) {
        Snapshot.sendApplyNotifications()
        composeRule.mainClock.advanceTimeBy(millis)
    }

    private fun next() = composeRule.onNodeWithTag(OnboardingTestTags.NEXT)

    @Test
    fun pageOne_allowed_nextHeld_releasedWhenCardHasAd() {
        setContent({ OnboardingUiState.Browsing(PAGES_4, 0) }, adsAllowed = true)
        frame()
        next().assertIsNotEnabled()
        requireNotNull(report)(true)
        frame()
        next().assertIsEnabled()
        assertEquals(listOf("start", "ad"), events)
    }

    @Test
    fun pageTwo_notHeld() {
        setContent({ OnboardingUiState.Browsing(PAGES_4, 1) }, adsAllowed = true)
        frame()
        next().assertIsEnabled()
        advance(6_000)
        next().assertIsEnabled()
        assertEquals(emptyList<String>(), events)
    }

    @Test
    fun adSlot_followsPage() {
        var state by mutableStateOf<OnboardingUiState>(OnboardingUiState.Browsing(PAGES_4, 0))
        setContent({ state }, adsAllowed = true)
        frame()
        state = OnboardingUiState.Browsing(PAGES_4, 1)
        frame()
        val changes = slotPages.fold(emptyList<OnboardingPage>()) { seen, page ->
            if (seen.lastOrNull() == page) seen else seen + page
        }
        assertEquals(listOf(INTRO, POSES), changes)
    }

    @Test
    fun notAllowed_notHeld() {
        setContent({ OnboardingUiState.Browsing(PAGES_4, 0) }, adsAllowed = false)
        frame()
        next().assertIsEnabled()
        advance(6_000)
        next().assertIsEnabled()
        assertEquals(emptyList<String>(), events)
    }

    @Test
    fun pageOne_cardReportsNoAd_heldFiveSeconds() {
        setContent({ OnboardingUiState.Browsing(PAGES_4, 0) }, adsAllowed = true)
        frame()
        // The pool is exhausted (spec onboarding, question 5; an emulator has no native ad): the hold runs its time.
        requireNotNull(report)(false)
        advance(4_900)
        next().assertIsNotEnabled()
        advance(200)
        frame()
        next().assertIsEnabled()
        assertEquals(listOf("start", "timeout"), events)
    }

    private companion object {
        val PAGES_4 = listOf(INTRO, POSES, PREMIUM, CAMERA)
    }
}
