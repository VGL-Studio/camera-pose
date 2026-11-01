package com.vglstudio.camera.pose.line.guide.core.ads

import android.app.Application
import android.os.Looper
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.vglstudio.camera.pose.line.guide.core.logging.LogLevel
import com.vglstudio.camera.pose.line.guide.core.logging.RecordingAppLogger
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Shadows.shadowOf

/**
 * The warning of missing production ad units when :core:ads starts (plan T-056, section 3.1, point 5): one line per
 * process, startup goes on. Only the lines of this warning are counted, since consent may log other warnings.
 */
@RunWith(AndroidJUnit4::class)
class AdsControllerUnitIdsTest {
    private val application = ApplicationProvider.getApplicationContext<Application>()
    private val logger = RecordingAppLogger()

    private fun startTwice(unitIds: AdUnitIds) {
        val controller = testAdsController(context = application, unitIds = unitIds, logger = logger)
        controller.start(application, AppActivityFilter { true })
        controller.start(application, AppActivityFilter { true })
        shadowOf(Looper.getMainLooper()).idle()
    }

    private fun missingWarnings() = logger.entries.filter { MISSING in it.message }

    @Test
    fun start_withMissingUnits_warnsOnce() {
        startTwice(ConfiguredAdUnitIds(PLACEMENT_UNITS))

        val warnings = missingWarnings()
        assertEquals(warnings.toString(), 1, warnings.size)
        val warning = warnings.single()
        assertEquals(LogLevel.WARN, warning.level)
        assertEquals("Ads", warning.tag)
        assertTrue(warning.message, "banner, native_collap" in warning.message)
        // Startup goes on.
        assertTrue(logger.entries.any { it.message == "Ads started" })
    }

    @Test
    fun start_withAllUnits_noWarning() {
        startTwice(ConfiguredAdUnitIds("$PLACEMENT_UNITS;banner=unit/8;native_collap=unit/9"))

        assertEquals(emptyList<Any>(), missingWarnings().filter { it.level == LogLevel.WARN })
        assertTrue(logger.entries.any { it.message == "Ads started" })
    }

    @Test
    fun start_staging_noWarning() {
        startTwice(GoogleTestAdUnitIds)

        assertEquals(emptyList<Any>(), missingWarnings().filter { it.level == LogLevel.WARN })
        assertTrue(logger.entries.any { it.message == "Ads started" })
    }

    private companion object {
        const val MISSING = "Production ad units missing"

        /** Every production placement key, no slot key. */
        const val PLACEMENT_UNITS = "open_app=unit/3;open_app_inter=unit/4;" +
            // The placements of this app (ADR-0029).
            "poses_bottom=unit/10;pose_detail_bottom=unit/11;review_interstitial=unit/12;inter_pose_detail=unit/13;" +
            "poses_in_collection=unit/14;" +
            "native_combo=unit/5;resume=unit/6;resume_inter=unit/7"
    }
}
