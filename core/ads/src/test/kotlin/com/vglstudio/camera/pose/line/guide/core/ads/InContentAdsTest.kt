package com.vglstudio.camera.pose.line.guide.core.ads

import android.app.Application
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * The pure functions of the in-content ads of a screen (MC-13, KB-35 criterion 4, plan T-092), the check of the
 * placements [InContentAds] takes, and [inContentAdsOn] on a real [AdsController].
 */
@RunWith(AndroidJUnit4::class)
class InContentAdsTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun isInContentPlacement_table() {
        val inContent = listOf(
            AdPlacement.GALLERY_NATIVE_SMALL,
            AdPlacement.GALLERY_NATIVE_MEDIUM,
            AdPlacement.GALLERY_BANNER_MREC,
            AdPlacement.GALLERY_IN_COLLECTION,
            AdPlacement.GALLERY_NATIVE_STEP,
            AdPlacement.GALLERY_NATIVE_STEP_FULL,
            // By format only (question 6 of the plan): the native small of a bar and a full-screen native too.
            AdPlacement.SAMPLE_NATIVE_SMALL,
            AdPlacement.GALLERY_NATIVE_FULL_SCREEN,
        )
        val notInContent = listOf(
            AdPlacement.GALLERY_BANNER,
            AdPlacement.GALLERY_BANNER_COLLAPSIBLE,
            AdPlacement.GALLERY_INTERSTITIAL,
            AdPlacement.GALLERY_APP_OPEN,
            AdPlacement.GALLERY_RW_ACTION,
            AdPlacement.NATIVE_AFTER_INTER,
        )
        inContent.forEach { assertTrue("${it.key} is in the content", isInContentPlacement(it)) }
        notInContent.forEach { assertFalse("${it.key} is not in the content", isInContentPlacement(it)) }
    }

    @Test
    fun inContentBlock_table() {
        assertEquals(null, inContentBlock(anyOn = null, blockedThisEntry = false))
        assertEquals(false, inContentBlock(anyOn = false, blockedThisEntry = false))
        assertEquals(true, inContentBlock(anyOn = true, blockedThisEntry = false))
        assertEquals(true, inContentBlock(anyOn = false, blockedThisEntry = true))
        assertEquals(true, inContentBlock(anyOn = null, blockedThisEntry = true))
    }

    @Test
    fun mergeInContentBlocks_table() {
        assertEquals(false, mergeInContentBlocks(outer = false, own = false))
        assertEquals(true, mergeInContentBlocks(outer = true, own = null))
        assertEquals(null, mergeInContentBlocks(outer = false, own = null))
        assertEquals(null, mergeInContentBlocks(outer = null, own = false))
    }

    @Test
    fun isFloatingBanner_table() {
        assertTrue(isFloatingBanner(AdFormat.BANNER_COLLAPSIBLE, inBottomBar = false))
        assertFalse(isFloatingBanner(AdFormat.BANNER_COLLAPSIBLE, inBottomBar = true))
        assertFalse(isFloatingBanner(AdFormat.BANNER, inBottomBar = false))
        assertFalse(isFloatingBanner(AdFormat.BANNER_MREC, inBottomBar = false))
    }

    @Test
    fun anchoredPlacement_throws() {
        var placements by mutableStateOf(listOf(AdPlacement.GALLERY_NATIVE_MEDIUM))
        composeRule.setContent {
            InContentAds(placements) { Text("content", Modifier.testTag(CONTENT)) }
        }
        // Control: an in-content placement does not throw, and the content is drawn (no ads controller).
        assertEquals(1, composeRule.onAllNodesWithTag(CONTENT).fetchSemanticsNodes().size)

        val error = runCatching {
            placements = listOf(AdPlacement.GALLERY_BANNER_COLLAPSIBLE)
            composeRule.waitForIdle()
        }.exceptionOrNull()
        assertTrue("expected IllegalArgumentException, got $error", error is IllegalArgumentException)
    }

    @Test
    fun inContentAdsOn_anyOfTheList() {
        val settings = FakeRemoteSettings().apply {
            set(AdsSettings.placementEnabled(AdPlacement.GALLERY_NATIVE_MEDIUM), false)
            set(AdsSettings.placementEnabled(AdPlacement.GALLERY_BANNER_MREC), true)
        }
        val controller = testAdsController(
            context = ApplicationProvider.getApplicationContext<Application>(),
            flags = DefaultAdsFlags(settings),
        )
        val both = listOf(AdPlacement.GALLERY_NATIVE_MEDIUM, AdPlacement.GALLERY_BANNER_MREC)

        assertEquals(true, runBlocking { controller.inContentAdsOn(both).first() })
        settings.set(AdsSettings.placementEnabled(AdPlacement.GALLERY_BANNER_MREC), false)
        assertEquals(false, runBlocking { controller.inContentAdsOn(both).first() })
        assertEquals(false, runBlocking { controller.inContentAdsOn(emptyList()).first() })
    }

    private companion object {
        const val CONTENT = "content"
    }
}
