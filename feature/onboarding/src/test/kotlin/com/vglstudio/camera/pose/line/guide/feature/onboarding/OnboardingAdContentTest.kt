package com.vglstudio.camera.pose.line.guide.feature.onboarding

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
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
import com.vglstudio.camera.pose.line.guide.core.ads.nextStepAdReady
import com.vglstudio.camera.pose.line.guide.core.designsystem.theme.AppTheme
import com.vglstudio.camera.pose.line.guide.feature.onboarding.OnboardingPage.CAMERA
import com.vglstudio.camera.pose.line.guide.feature.onboarding.OnboardingPage.GUIDE
import com.vglstudio.camera.pose.line.guide.feature.onboarding.OnboardingPage.INTRO
import com.vglstudio.camera.pose.line.guide.feature.onboarding.OnboardingPage.POSES
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

/**
 * The wiring of [OnboardingAdContent] (plan T-083, section 4): the gate of the card, the report of the card and the
 * Next button of the real screen, with a fake card and the clock stopped. Page three (plan T-092, section 4): the
 * signal of the pool is the rule [nextStepAdReady] on a fake count and the report of the card of the page on screen.
 */
@RunWith(AndroidJUnit4::class)
@Config(qualifiers = "vi-w411dp-h891dp-xxhdpi")
class OnboardingAdContentTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    private val events = mutableListOf<String>()
    private var report: ((Boolean) -> Unit)? = null
    private val slotPages = mutableListOf<OnboardingPage>()

    /** Free ads of the pool, as [nextStepAdReady] counts them (plan T-092). */
    private var claimable by mutableStateOf(0)

    private var adsAllowed by mutableStateOf(true)

    /** Bumped to build the screen again with fresh state: the control of the tests where Next is never held. */
    private var run by mutableStateOf(0)

    private fun setContent(uiState: () -> OnboardingUiState, adsAllowed: Boolean) {
        this.adsAllowed = adsAllowed
        composeRule.mainClock.autoAdvance = false
        composeRule.setContent {
            AppTheme {
                key(run) { Content(uiState()) }
            }
        }
    }

    @Composable
    private fun Content(uiState: OnboardingUiState) {
        OnboardingAdContent(
            uiState = uiState,
            adsAllowed = adsAllowed,
            onNext = {},
            onBack = {},
            onEnableCamera = {},
            onIntroAdWaitStarted = { events += "start" },
            onIntroAdWaitEnded = { events += it.logName },
            nextAdReady = { _, currentStepHasAd -> nextStepAdReady(claimable, currentStepHasAd) },
            onGuideAdWaitStarted = { events += "guide start" },
            onGuideAdWaitEnded = { events += "guide ${it.logName}" },
            adSlot = { page, onAdAvailabilityChange ->
                slotPages += page
                report = onAdAvailabilityChange
                Box(Modifier.fillMaxWidth().height(250.dp))
            },
        )
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

    // ---- Page three waits for the ad of page four (spec onboarding, KB-6, question 12; plan T-092) ----

    /** Starts on page two (page one never held: its hold is used up by leaving it), then goes to [page]. */
    private fun fromPageTwo(adsAllowed: Boolean = true): (OnboardingPage) -> Unit {
        var state by mutableStateOf<OnboardingUiState>(OnboardingUiState.Browsing(PAGES_4, 1))
        setContent({ state }, adsAllowed = adsAllowed)
        frame()
        next().assertIsEnabled()
        return { page -> state = OnboardingUiState.Browsing(PAGES_4, PAGES_4.indexOf(page)) }
    }

    @Test
    fun guide_allowedNoNextAd_nextHeld_threeSeconds() {
        val goTo = fromPageTwo()
        goTo(GUIDE)
        frame()
        next().assertIsNotEnabled()
        advance(2_900)
        next().assertIsNotEnabled()
        advance(200)
        frame()
        next().assertIsEnabled()
        assertEquals(listOf("guide start", "guide timeout"), events)
    }

    @Test
    fun guide_nextAdReady_notHeld() {
        claimable = 2
        val goTo = fromPageTwo()
        goTo(GUIDE)
        frame()
        next().assertIsEnabled()
        advance(4_000)
        next().assertIsEnabled()
        assertEquals(emptyList<String>(), events)

        // Control (L-055): the same page built again with no ad for page four is held.
        claimable = 0
        run++
        frame()
        next().assertIsNotEnabled()
        assertEquals(listOf("guide start"), events)
    }

    @Test
    fun guide_notAllowed_notHeld() {
        val goTo = fromPageTwo(adsAllowed = false)
        goTo(GUIDE)
        frame()
        next().assertIsEnabled()
        advance(4_000)
        next().assertIsEnabled()
        assertEquals(emptyList<String>(), events)

        // Control (L-055): the same page built again with ads allowed is held.
        adsAllowed = true
        run++
        frame()
        next().assertIsNotEnabled()
        assertEquals(listOf("guide start"), events)
    }

    @Test
    fun camera_neverHeld() {
        val goTo = fromPageTwo()
        goTo(GUIDE)
        frame()
        next().assertIsNotEnabled()
        goTo(CAMERA)
        frame()
        composeRule.onNodeWithTag(OnboardingTestTags.ENABLE_CAMERA).assertIsEnabled()
        advance(4_000)
        composeRule.onNodeWithTag(OnboardingTestTags.ENABLE_CAMERA).assertIsEnabled()
        assertEquals(listOf("guide start", "guide left page"), events)
    }

    @Test
    fun guide_oneFreeAd_heldFromFirstFrame() {
        // One free ad is for the card of page three itself: the first frame of the page is already held.
        claimable = 1
        val goTo = fromPageTwo()
        goTo(GUIDE)
        frame()
        next().assertIsNotEnabled()
        assertEquals(listOf("guide start"), events)
        advance(2_900)
        next().assertIsNotEnabled()
        advance(200)
        frame()
        next().assertIsEnabled()
        assertEquals(listOf("guide start", "guide timeout"), events)
    }

    @Test
    fun guide_cardWaiting_oneAdArrives_stillHeld() {
        val goTo = fromPageTwo()
        goTo(GUIDE)
        frame()
        next().assertIsNotEnabled()

        // One ad comes while the card of page three has none: it is for that card, Next stays held.
        claimable = 1
        frame()
        advance(1_000)
        next().assertIsNotEnabled()

        // Control: a second one is for page four.
        claimable = 2
        frame()
        next().assertIsEnabled()
        assertEquals(listOf("guide start", "guide ad"), events)
    }

    @Test
    fun guide_cardGotAd_nextAdArrives_released() {
        claimable = 1
        val goTo = fromPageTwo()
        goTo(GUIDE)
        frame()
        next().assertIsNotEnabled()

        // The card of page three claims the ad (the count falls), then reports it: nothing free for page four.
        claimable = 0
        frame()
        requireNotNull(report)(true)
        frame()
        next().assertIsNotEnabled()

        // The pool refills: one free ad while the card has its own is for page four.
        claimable = 1
        frame()
        next().assertIsEnabled()
        assertEquals(listOf("guide start", "guide ad"), events)
    }

    @Test
    fun guide_cardHasAdOnPageTwo_notCountedForPageThree() {
        val goTo = fromPageTwo()
        // The card of page two has its ad; the pool holds one more.
        requireNotNull(report)(true)
        claimable = 1
        frame()
        goTo(GUIDE)
        frame()
        // The card of page three has no ad yet (its slot reported nothing): the one free ad is for that card.
        next().assertIsNotEnabled()
        advance(1_000)
        next().assertIsNotEnabled()

        // Control: once the card of page three reports its ad, the same one free ad is for page four.
        requireNotNull(report)(true)
        frame()
        next().assertIsEnabled()
        assertEquals(listOf("guide start", "guide ad"), events)
    }

    private companion object {
        val PAGES_4 = listOf(INTRO, POSES, GUIDE, CAMERA)
    }
}
