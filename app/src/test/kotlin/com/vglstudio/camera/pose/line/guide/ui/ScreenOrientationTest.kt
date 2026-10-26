package com.vglstudio.camera.pose.line.guide.ui

import android.app.Application
import android.content.ComponentName
import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.content.res.Resources
import androidx.activity.ComponentActivity
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.vglstudio.camera.pose.line.guide.MainActivity
import com.vglstudio.camera.pose.line.guide.core.logging.LogEntry
import com.vglstudio.camera.pose.line.guide.core.logging.LogLevel
import com.vglstudio.camera.pose.line.guide.core.logging.RecordingAppLogger
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.annotation.Config

/**
 * Portrait on a phone, the device's orientation on a large screen (spec camera, KB-11; plan T-056). A plain
 * Application, so neither Hilt nor Firebase starts; the activity tests use a plain ComponentActivity. Robolectric
 * reports a stable density of 160 whatever the qualifiers, so the activity tests pass the density of `xxhdpi`.
 */
@RunWith(AndroidJUnit4::class)
@Config(application = Application::class)
class ScreenOrientationTest {
    private val portrait = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
    private val unspecified = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
    private val undefined = Configuration.SMALLEST_SCREEN_WIDTH_DP_UNDEFINED

    private fun activity(): ComponentActivity = Robolectric.buildActivity(ComponentActivity::class.java).setup().get()

    @Test
    fun phone_isPortrait() {
        assertEquals(portrait, screenOrientationFor(listOf(411, 411)))
    }

    @Test
    fun width599_isPortrait_width600_followsTheDevice() {
        assertEquals(portrait, screenOrientationFor(listOf(599)))
        assertEquals(unspecified, screenOrientationFor(listOf(600)))
    }

    @Test
    fun letterboxedWindowOnALargeDisplay_followsTheDevice() {
        assertEquals(unspecified, screenOrientationFor(listOf(486, 673)))
    }

    @Test
    fun undefinedWidth_isIgnored() {
        assertEquals(portrait, screenOrientationFor(listOf(undefined, 411)))
        assertEquals(unspecified, screenOrientationFor(listOf(undefined, 673)))
    }

    @Test
    fun physicalPanel_inDp() {
        assertEquals(700, physicalSmallestWidthDp(1840, 2208, 420))
        assertEquals(411, physicalSmallestWidthDp(1080, 2400, 420))
        assertEquals(0, physicalSmallestWidthDp(0, 2208, 420))
        assertEquals(0, physicalSmallestWidthDp(1840, 2208, 0))
    }

    @Test
    @Config(qualifiers = "w673dp-h841dp-xxhdpi")
    fun unfolded_unlocksThePortraitOfTheManifest() {
        val activity = activity()
        activity.requestedOrientation = portrait

        activity.applyScreenOrientation(RecordingAppLogger(), stableDensityDpi = XXHDPI)

        assertEquals(unspecified, activity.requestedOrientation)
    }

    @Test
    @Config(qualifiers = "w841dp-h673dp-land-xxhdpi")
    fun unfoldedLandscape_unlocks() {
        val activity = activity()
        activity.requestedOrientation = portrait

        activity.applyScreenOrientation(RecordingAppLogger(), stableDensityDpi = XXHDPI)

        assertEquals(unspecified, activity.requestedOrientation)
    }

    @Test
    @Config(qualifiers = "w411dp-h891dp-xxhdpi")
    fun foldedAgain_locksToPortrait() {
        val activity = activity()
        // As after unfolding: the request is kept across the relaunch of folding.
        activity.requestedOrientation = unspecified

        activity.applyScreenOrientation(RecordingAppLogger(), stableDensityDpi = XXHDPI)

        assertEquals(portrait, activity.requestedOrientation)
    }

    @Test
    @Config(qualifiers = "w486dp-h673dp-xxhdpi")
    fun letterboxedActivity_readsTheSystemConfiguration() {
        val activity = activity()
        activity.requestedOrientation = portrait
        // Control: every source sees the letterboxed 486 dp, so the app stays portrait.
        activity.applyScreenOrientation(RecordingAppLogger(), stableDensityDpi = XXHDPI)
        assertEquals(portrait, activity.requestedOrientation)

        val system = Resources.getSystem()
        val original = Configuration(system.configuration)
        try {
            val large = Configuration(system.configuration).apply { smallestScreenWidthDp = 673 }
            @Suppress("DEPRECATION")
            system.updateConfiguration(large, system.displayMetrics)

            assertEquals(673, activity.screenSmallestWidthsDp(XXHDPI).system)
            activity.applyScreenOrientation(RecordingAppLogger(), stableDensityDpi = XXHDPI)
            assertEquals(unspecified, activity.requestedOrientation)
        } finally {
            @Suppress("DEPRECATION")
            system.updateConfiguration(original, system.displayMetrics)
        }
    }

    @Test
    @Config(qualifiers = "w486dp-h673dp-xxhdpi")
    fun physicalPanel_isOneOfTheSources() {
        val activity = activity()
        // 1458 x 2019 px at 480 dpi.
        assertEquals(486, activity.screenSmallestWidthsDp(XXHDPI).panel)

        activity.requestedOrientation = portrait
        // The same panel read at 160 dpi is 1458 dp: large.
        activity.applyScreenOrientation(RecordingAppLogger(), stableDensityDpi = 160)

        assertEquals(unspecified, activity.requestedOrientation)
    }

    @Test
    @Config(qualifiers = "w673dp-h841dp-xxhdpi")
    fun apply_logsTheSourcesAndTheChoice() {
        val activity = activity()
        activity.requestedOrientation = portrait
        val logger = RecordingAppLogger()

        activity.applyScreenOrientation(logger, stableDensityDpi = XXHDPI)
        activity.applyScreenOrientation(logger, stableDensityDpi = XXHDPI)

        val widths = "smallest width dp: activity 673, system 673, window 0, panel 673"
        assertEquals(
            listOf(
                LogEntry(LogLevel.INFO, "Orientation", "Orientation unspecified, changed true ($widths)"),
                LogEntry(LogLevel.INFO, "Orientation", "Orientation unspecified, changed false ($widths)"),
            ),
            logger.entries,
        )
    }

    @Test
    fun manifest_keepsPortrait_soPhonesStartWithoutARelaunch() {
        val activity = activity()

        val info = activity.packageManager.getActivityInfo(ComponentName(activity, MainActivity::class.java), 0)

        assertEquals(portrait, info.screenOrientation)
    }

    private companion object {
        const val XXHDPI = 480
    }
}
