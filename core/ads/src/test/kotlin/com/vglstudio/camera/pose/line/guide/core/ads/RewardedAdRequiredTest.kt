package com.vglstudio.camera.pose.line.guide.core.ads

import androidx.activity.ComponentActivity
import androidx.activity.compose.LocalActivity
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.vglstudio.camera.pose.line.guide.core.analytics.AdMetricEvent
import com.vglstudio.camera.pose.line.guide.core.analytics.RecordingAdMetrics
import com.vglstudio.camera.pose.line.guide.core.logging.RecordingAppLogger
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * [rememberRewardedAdRequired]: whether a tap on a rewarded action would open its dialog now (R-08). It reads the
 * very gate [RewardedAction.start] reads, so a mark on the screen and the dialog never disagree.
 */
@RunWith(AndroidJUnit4::class)
class RewardedAdRequiredTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    private val settings = FakeRemoteSettings().apply {
        set(AdsSettings.placementEnabled(AdPlacement.GALLERY_RW_ACTION), true)
        // On, a dialog or an opportunity is recorded if there is one.
        set(AdsSettings.IS_LOG_AD_FULLSCREEN_EVENTS, true)
    }
    private val adFree = MutableStateFlow(false)
    private val metrics = RecordingAdMetrics()
    private var required: Boolean? = null
    private var observed = mutableListOf<Boolean?>()
    private var action: RewardedAction? = null
    private var granted = 0

    private fun controller(): AdsController {
        val status = object : AdFreeStatus {
            override val adFree = this@RewardedAdRequiredTest.adFree
        }
        return testAdsController(
            unitIds = AdUnitIds { null },
            flags = DefaultAdsFlags(settings, status),
            metrics = metrics,
            logger = RecordingAppLogger(),
        ).also { controller ->
            composeRule.runOnUiThread {
                controller.start(composeRule.activity.application, AppActivityFilter { true })
            }
            composeRule.waitForIdle()
        }
    }

    private fun setRequired(controller: AdsController, withAction: Boolean = false) {
        composeRule.setContent {
            AdsHost(controller) {
                AdsTestTheme {
                    required = rememberRewardedAdRequired(AdPlacement.GALLERY_RW_ACTION)
                    if (withAction) {
                        val current = rememberRewardedAction(AdPlacement.GALLERY_RW_ACTION, onGranted = { granted++ })
                        action = current
                        RewardedAdDialog(current, title = "Title")
                    }
                }
            }
        }
        composeRule.waitForIdle()
    }

    private fun change(block: () -> Unit): Boolean? {
        composeRule.runOnIdle(block)
        composeRule.waitForIdle()
        return required
    }

    private fun dialogs(): Int = composeRule.onAllNodesWithTag(RewardedAdDialogTestTags.CARD).fetchSemanticsNodes().size

    @Test
    fun noController_false() {
        composeRule.setContent {
            AdsTestTheme { required = rememberRewardedAdRequired(AdPlacement.GALLERY_RW_ACTION) }
        }
        composeRule.waitForIdle()

        assertEquals(false, required)
    }

    @Test
    fun beforeFirstValue_null_thenFollowsTheGate() {
        val gate = MutableSharedFlow<Boolean>(replay = 1)
        composeRule.setContent {
            val value = rememberRewardedAdRequiredOf(gate)
            observed += value
        }
        composeRule.waitForIdle()
        // No value yet: never guessed (as start() waits for the first value).
        assertEquals(listOf<Boolean?>(null), observed.distinct())

        composeRule.runOnIdle { gate.tryEmit(true) }
        composeRule.waitForIdle()
        assertEquals(true, observed.last())

        composeRule.runOnIdle { gate.tryEmit(false) }
        composeRule.waitForIdle()
        assertEquals(false, observed.last())
    }

    @Test
    fun gateChangesWhileShown_neverAnswersWithTheOldGate() {
        // A screen that switches placements while shown: the answer of the old gate is never shown for the new one.
        val first = MutableSharedFlow<Boolean>(replay = 1).apply { tryEmit(true) }
        val second = MutableSharedFlow<Boolean>(replay = 1)
        var gate: Flow<Boolean> by mutableStateOf(first)
        composeRule.setContent { observed += rememberRewardedAdRequiredOf(gate) }
        composeRule.waitForIdle()
        // Control: the first gate is known.
        assertEquals(true, observed.last())

        observed.clear()
        composeRule.runOnIdle { gate = second }
        composeRule.waitForIdle()
        assertEquals(listOf<Boolean?>(null), observed.distinct())

        composeRule.runOnIdle { second.tryEmit(false) }
        composeRule.waitForIdle()
        assertEquals(false, observed.last())
        assertTrue(observed.toString(), true !in observed)
    }

    @Test
    fun noActivity_false() {
        // As rememberRewardedAction, which runs the action at once without an activity.
        val controller = controller()
        composeRule.runOnUiThread { controller.openConsentGateForTest() }
        composeRule.setContent {
            AdsHost(controller) {
                CompositionLocalProvider(LocalActivity provides null) {
                    required = rememberRewardedAdRequired(AdPlacement.GALLERY_RW_ACTION)
                }
            }
        }
        composeRule.waitForIdle()

        assertEquals(false, required)
    }

    @Test
    fun realController_followsEveryPartOfTheGate() {
        val controller = controller()
        composeRule.runOnUiThread { controller.openConsentGateForTest() }
        setRequired(controller)
        // Control: every part open, the dialog would open.
        assertEquals(true, required)

        val key = AdsSettings.placementEnabled(AdPlacement.GALLERY_RW_ACTION)
        assertEquals("is_show_<code> off", false, change { settings.set(key, false) })
        assertEquals("is_show_<code> back on", true, change { settings.set(key, true) })
        assertEquals("is_show_ads off", false, change { settings.set(AdsSettings.IS_SHOW_ADS, false) })
        assertEquals("is_show_ads back on", true, change { settings.set(AdsSettings.IS_SHOW_ADS, true) })
        assertEquals("ad-free", false, change { adFree.value = true })
        assertEquals("ad-free ended", true, change { adFree.value = false })
        assertEquals("consent gate closed", false, change { controller.closeConsentGateForTest() })
        assertEquals("consent gate open again", true, change { controller.reopenConsentGateForTest() })
    }

    @Test
    fun realController_sameAnswerAsStart_forEveryPartOfTheGate() {
        val key = AdsSettings.placementEnabled(AdPlacement.GALLERY_RW_ACTION)
        val cases = listOf<Pair<String, (AdsController) -> Unit>>(
            "all open" to {},
            "is_show_<code> off" to { settings.set(key, false) },
            "is_show_ads off" to { settings.set(AdsSettings.IS_SHOW_ADS, false) },
            "ad-free" to { adFree.value = true },
            "consent gate closed" to { it.closeConsentGateForTest() },
        )
        val controller = controller()
        composeRule.runOnUiThread { controller.openConsentGateForTest() }
        setRequired(controller, withAction = true)
        val answers = mutableListOf<String>()
        cases.forEach { (name, close) ->
            // Back to all open, then close one part.
            composeRule.runOnIdle {
                settings.set(key, true)
                settings.set(AdsSettings.IS_SHOW_ADS, true)
                adFree.value = false
                controller.reopenConsentGateForTest()
            }
            composeRule.waitForIdle()
            val grantedBefore = granted
            val answer = change { close(controller) }
            composeRule.runOnIdle { action!!.start() }
            composeRule.waitForIdle()
            val dialogOpened = dialogs() == 1
            assertEquals(name, answer, dialogOpened)
            assertEquals(name, !dialogOpened, granted == grantedBefore + 1)
            answers += "$name=$answer"
            if (dialogOpened) composeRule.runOnIdle { action!!.dismiss() }
            composeRule.waitForIdle()
        }
        // Control: both answers were seen, so the comparison above checked both ways.
        assertEquals(answers.toString(), listOf("all open=true"), answers.filter { it.endsWith("=true") })
    }

    @Test
    fun realController_sameAnswerAsStart_gateOpen_dialogOpens() {
        val controller = controller()
        composeRule.runOnUiThread { controller.openConsentGateForTest() }
        setRequired(controller, withAction = true)
        assertEquals(true, required)

        composeRule.runOnIdle { action!!.start() }
        composeRule.waitForIdle()

        assertEquals(1, dialogs())
        assertEquals(0, granted)
        // Control of realController_readingRecordsNothing: in this setup a dialog is recorded.
        assertTrue(metrics.events.toString(), metrics.events.any { it is AdMetricEvent.RewardDialog })
    }

    @Test
    fun realController_sameAnswerAsStart_gateClosed_actionRunsAtOnce() {
        val controller = controller()
        // Consent gate never opened: the gate counts as off.
        setRequired(controller, withAction = true)
        assertEquals(false, required)

        composeRule.runOnIdle { action!!.start() }
        composeRule.waitForIdle()

        assertEquals(0, dialogs())
        assertEquals(1, granted)
    }

    @Test
    fun realController_readingRecordsNothing() {
        val controller = controller()
        composeRule.runOnUiThread { controller.openConsentGateForTest() }
        setRequired(controller)
        assertEquals(true, required)
        change { adFree.value = true }
        change { adFree.value = false }

        assertTrue(
            metrics.events.toString(),
            metrics.events.none { it is AdMetricEvent.RewardDialog || it is AdMetricEvent.Opportunity },
        )
    }

    @Test
    fun nonRewardedPlacement_isRejected() {
        val error = runCatching {
            composeRule.setContent { rememberRewardedAdRequired(AdPlacement.SAMPLE_NATIVE_SMALL) }
            composeRule.waitForIdle()
        }.exceptionOrNull()

        assertTrue("expected IllegalArgumentException, got $error", error is IllegalArgumentException)
    }
}
