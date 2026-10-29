package com.vglstudio.camera.pose.line.guide.core.ads

import android.app.Application
import android.content.ActivityNotFoundException
import android.content.Intent
import androidx.activity.ComponentActivity
import androidx.activity.compose.LocalActivityResultRegistryOwner
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.ActivityResultRegistry
import androidx.activity.result.ActivityResultRegistryOwner
import androidx.activity.result.contract.ActivityResultContract
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.core.app.ActivityOptionsCompat
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.vglstudio.camera.pose.line.guide.core.logging.RecordingAppLogger
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Shadows.shadowOf

/**
 * The two helpers that features use to leave the app on purpose (plan T-074, pass 2; G-03, G-05), inside the app's
 * root [AdsHostContent] with the real [ResumeSuppression] and [ForegroundAdSuppressor]: a system screen opened through
 * [rememberAdHoldingLauncher] holds the resume ad while it is up, a page opened with [rememberStartOutsideApp] marks
 * the leave, and [rememberStartAdHold] holds until its release, also after the caller has left.
 */
@RunWith(AndroidJUnit4::class)
class LeaveAppActionsTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    private val suppressor = ForegroundAdSuppressor()
    private val suppression = ResumeSuppression(suppressor, RecordingAppLogger())
    private val registry = RecordingRegistry()
    private val registryOwner = object : ActivityResultRegistryOwner {
        override val activityResultRegistry: ActivityResultRegistry = registry
    }

    @Before
    fun setUp() {
        // An intent that no activity handles fails as on a device, instead of being recorded as started.
        shadowOf(ApplicationProvider.getApplicationContext<Application>()).checkActivities(true)
    }

    /** The registry of a test: records the hold seen by the system at each launch, or throws [launchError]. */
    private inner class RecordingRegistry : ActivityResultRegistry() {
        val heldAtLaunch = mutableListOf<Boolean>()
        var lastRequestCode: Int? = null
        var launchError: RuntimeException? = null

        override fun <I, O> onLaunch(
            requestCode: Int,
            contract: ActivityResultContract<I, O>,
            input: I,
            options: ActivityOptionsCompat?,
        ) {
            heldAtLaunch += suppressor.suppressed.value
            launchError?.let { throw it }
            lastRequestCode = requestCode
        }
    }

    private fun setContentInHost(content: @Composable () -> Unit) {
        composeRule.setContent {
            AdsHostContent(suppression, suppressor) {
                CompositionLocalProvider(LocalActivityResultRegistryOwner provides registryOwner) { content() }
            }
        }
        composeRule.waitForIdle()
    }

    @Test
    fun launcher_holdsBeforeLaunch_releasesOnFirstLineOfResult() {
        val heldInCallback = mutableListOf<Boolean>()
        var launcher: ActivityResultLauncher<String>? = null
        setContentInHost {
            launcher = rememberAdHoldingLauncher(ActivityResultContracts.RequestPermission()) { granted ->
                heldInCallback += suppressor.suppressed.value
                assertTrue(granted)
            }
        }
        // Control: nothing holds before the launch.
        assertFalse(suppressor.suppressed.value)

        composeRule.runOnUiThread { requireNotNull(launcher).launch(PERMISSION) }
        assertEquals(listOf(true), registry.heldAtLaunch)
        // Still held while the system screen is up.
        assertTrue(suppressor.suppressed.value)

        composeRule.runOnUiThread { registry.dispatchResult(requireNotNull(registry.lastRequestCode), true) }
        // Released on the first line of the result, before the caller's code runs.
        assertEquals(listOf(false), heldInCallback)
        assertFalse(suppressor.suppressed.value)
    }

    @Test
    fun launcher_launchThrows_stillReleases() {
        var launcher: ActivityResultLauncher<String>? = null
        setContentInHost { launcher = rememberAdHoldingLauncher(ActivityResultContracts.RequestPermission()) {} }
        registry.launchError = ActivityNotFoundException("no permission screen")

        composeRule.runOnUiThread {
            assertThrows(ActivityNotFoundException::class.java) { requireNotNull(launcher).launch(PERMISSION) }
        }

        // Control: the hold was on when the system was asked.
        assertEquals(listOf(true), registry.heldAtLaunch)
        assertFalse(suppressor.suppressed.value)
    }

    @Test
    fun startOutsideApp_marksLeave_onlyWhenStarted() {
        var start: (Intent) -> Boolean = { error("not composed") }
        setContentInHost { start = rememberStartOutsideApp() }
        val context = composeRule.activity

        val missing = composeRule.runOnUiThread { start(Intent(NO_SUCH_ACTION)) }
        composeRule.waitForIdle()
        assertFalse(missing)
        assertFalse("no mark when nothing opened", suppression.resumeAdsSuppressed.value)

        val opened = composeRule.runOnUiThread { start(Intent(context, ComponentActivity::class.java)) }
        composeRule.waitForIdle()
        assertTrue(opened)
        assertTrue("the leave is marked", suppression.resumeAdsSuppressed.value)
        // Control: it is the leave mark, used up by the next resume of an app activity (G-05).
        composeRule.runOnUiThread { suppression.onActivityResumed() }
        assertFalse(suppression.resumeAdsSuppressed.value)
    }

    @Test
    fun startAdHold_holdsUntilReleased_evenAfterTheCallerLeaves() {
        var shown by mutableStateOf(true)
        var startHold: () -> (() -> Unit) = { error("not composed") }
        setContentInHost { if (shown) startHold = rememberStartAdHold() }

        val release = composeRule.runOnUiThread { startHold() }
        assertTrue(suppressor.suppressed.value)
        shown = false
        composeRule.waitForIdle()
        // Not released by leaving the composition: the flow it holds for goes on.
        assertTrue(suppressor.suppressed.value)

        composeRule.runOnUiThread { release() }
        assertFalse(suppressor.suppressed.value)
        // A second release does nothing: another source's hold still counts.
        val other = composeRule.runOnUiThread { suppressor.acquire("other") }
        composeRule.runOnUiThread { release() }
        assertTrue(suppressor.suppressed.value)
        composeRule.runOnUiThread { other.release() }
    }

    @Test
    fun noController_justOpens() {
        var launcher: ActivityResultLauncher<String>? = null
        var start: (Intent) -> Boolean = { error("not composed") }
        var startHold: () -> (() -> Unit) = { error("not composed") }
        var result: Boolean? = null
        composeRule.setContent {
            CompositionLocalProvider(LocalActivityResultRegistryOwner provides registryOwner) {
                launcher = rememberAdHoldingLauncher(ActivityResultContracts.RequestPermission()) { result = it }
                start = rememberStartOutsideApp()
                startHold = rememberStartAdHold()
            }
        }
        composeRule.waitForIdle()
        val context = composeRule.activity

        composeRule.runOnUiThread { requireNotNull(launcher).launch(PERMISSION) }
        assertNull(result)
        composeRule.runOnUiThread { registry.dispatchResult(requireNotNull(registry.lastRequestCode), true) }
        assertEquals(true, result)
        assertTrue(composeRule.runOnUiThread { start(Intent(context, ComponentActivity::class.java)) })
        assertFalse(composeRule.runOnUiThread { start(Intent(NO_SUCH_ACTION)) })
        composeRule.runOnUiThread { startHold()() }
        // Control: no root, so nothing was held anywhere.
        assertFalse(suppressor.suppressed.value)
    }

    private companion object {
        const val PERMISSION = "android.permission.CAMERA"
        const val NO_SUCH_ACTION = "com.vglstudio.test.NO_SUCH_ACTION"
    }
}
