package com.vglstudio.camera.pose.line.guide.feature.onboarding

import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.Snapshot
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.lifecycle.Lifecycle
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * The hold of Next on page one while the step card waits for an ad (spec onboarding, KB-6; plan T-083, section 4),
 * on a small screen with one button, the clock stopped. State changed by a test reaches the next frame only after
 * [Snapshot.sendApplyNotifications] (plan T-083, section 4.1). The activity is built again with one content lambda,
 * so rememberSaveable finds its state (plan T-083, section 4).
 */
@RunWith(AndroidJUnit4::class)
class IntroAdWaitTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    private var onIntro by mutableStateOf(true)
    private var allowed by mutableStateOf(true)
    private var hasAd by mutableStateOf(false)
    private val events = mutableListOf<String>()

    /** One lambda for every setContent of a test: the same group keys, so rememberSaveable restores. */
    private val content: @Composable () -> Unit = {
        val held = rememberIntroAdWait(
            onIntro = onIntro,
            allowed = allowed,
            hasAd = hasAd,
            onStarted = { events += START },
            onEnded = { events += it.logName },
        )
        Button(onClick = {}, enabled = !held, modifier = Modifier.testTag(BUTTON)) { Text("Next") }
    }

    private fun start() {
        composeRule.mainClock.autoAdvance = false
        composeRule.setContent(content)
    }

    private fun frame() {
        Snapshot.sendApplyNotifications()
        composeRule.mainClock.advanceTimeByFrame()
    }

    private fun advance(millis: Long) {
        Snapshot.sendApplyNotifications()
        composeRule.mainClock.advanceTimeBy(millis)
    }

    private fun assertHeld() = composeRule.onNodeWithTag(BUTTON).assertIsNotEnabled()

    private fun assertReady() = composeRule.onNodeWithTag(BUTTON).assertIsEnabled()

    @Test
    fun allowedNoAd_heldFromFirstFrame_releasedAfterFiveSeconds() {
        start()
        // Held from the first frame: the button never shows enabled first.
        assertHeld()
        advance(4_900)
        assertHeld()
        advance(200)
        frame()
        assertReady()
        assertEquals(listOf(START, "timeout"), events)
    }

    @Test
    fun adComes_releasedAtOnce_neverHeldAgain() {
        start()
        advance(1_000)
        assertHeld()
        hasAd = true
        frame()
        assertReady()
        // The ad goes away: the hold is used up.
        hasAd = false
        frame()
        assertReady()
        advance(6_000)
        assertReady()
        assertEquals(listOf(START, "ad"), events)
    }

    @Test
    fun notAllowed_neverHeld() {
        allowed = false
        start()
        assertReady()
        frame()
        assertReady()
        advance(6_000)
        assertReady()
        assertEquals(emptyList<String>(), events)
    }

    @Test
    fun switchOffMidway_releasedAtOnce_notHeldWhenBackOn() {
        start()
        advance(1_000)
        assertHeld()
        allowed = false
        frame()
        assertReady()
        allowed = true
        frame()
        assertReady()
        advance(6_000)
        assertReady()
        assertEquals(listOf(START, "not allowed"), events)
    }

    @Test
    fun backgroundMidway_releasedOnReturn_notHeldAgain() {
        start()
        advance(1_000)
        assertHeld()
        composeRule.activityRule.scenario.moveToState(Lifecycle.State.CREATED)
        assertEquals(listOf(START, "background"), events)
        composeRule.activityRule.scenario.moveToState(Lifecycle.State.RESUMED)
        frame()
        assertReady()
        advance(6_000)
        assertReady()
        assertEquals(listOf(START, "background"), events)
    }

    @Test
    fun rebuiltMidway_notHeldAgain() {
        val tester = StateRestorationTester(composeRule)
        composeRule.mainClock.autoAdvance = false
        tester.setContent(content)
        advance(1_000)
        assertHeld()
        // With the clock stopped the restore draws nothing (plan T-083, section 4.1).
        composeRule.mainClock.autoAdvance = true
        tester.emulateSavedInstanceStateRestore()
        composeRule.mainClock.autoAdvance = false
        assertReady()
        advance(6_000)
        assertReady()
        assertEquals(listOf(START, "disposed"), events)
    }

    @Test
    fun recreatedActivity_midway_notHeldAgain() {
        start()
        advance(1_000)
        assertHeld()
        composeRule.mainClock.autoAdvance = true
        composeRule.activityRule.scenario.recreate()
        composeRule.activityRule.scenario.onActivity { it.setContent(content = content) }
        composeRule.mainClock.autoAdvance = false
        // ON_STOP comes before the activity is destroyed (plan T-083, question 9): no second start.
        assertEquals(listOf(START, "background"), events)
        assertReady()
        advance(6_000)
        assertReady()
        assertEquals(listOf(START, "background"), events)
    }

    @Test
    fun allowedLate_onPageOne_heldFiveSecondsFromThen() {
        allowed = false
        start()
        assertReady()
        advance(2_000)
        assertReady()
        allowed = true
        frame()
        assertHeld()
        assertEquals(listOf(START), events)
        advance(4_900)
        assertHeld()
        advance(200)
        frame()
        assertReady()
        assertEquals(listOf(START, "timeout"), events)
    }

    @Test
    fun allowedAfterLeavingPageOne_noWait() {
        allowed = false
        start()
        onIntro = false
        frame()
        allowed = true
        frame()
        // Back on page one: leaving it used the hold up.
        onIntro = true
        frame()
        assertReady()
        advance(6_000)
        assertReady()
        assertEquals(emptyList<String>(), events)
    }

    @Test
    fun leftPage_released_notHeldWhenBack() {
        start()
        advance(1_000)
        assertHeld()
        onIntro = false
        frame()
        assertReady()
        onIntro = true
        frame()
        assertReady()
        advance(6_000)
        assertReady()
        assertEquals(listOf(START, "left page"), events)
    }

    @Test
    fun allowedInBackground_notHeldUntilForeground() {
        allowed = false
        start()
        assertReady()
        composeRule.activityRule.scenario.moveToState(Lifecycle.State.CREATED)
        allowed = true
        advance(1_000)
        assertEquals(emptyList<String>(), events)
        composeRule.activityRule.scenario.moveToState(Lifecycle.State.RESUMED)
        frame()
        assertHeld()
        assertEquals(listOf(START), events)
    }

    private companion object {
        const val BUTTON = "intro_ad_wait_button"
        const val START = "start"
    }
}
