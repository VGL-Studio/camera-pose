package com.vglstudio.camera.pose.line.guide.core.ads

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

/** The two entries of :core:ads for staging-only placements (plan T-036, step 2). */
class StagingOnlyEntriesTest {
    @Test
    fun acceptsStagingAppOpen_onlyGalleryAppOpen() {
        // An app open placement of a feature: no `gallery_` prefix, not app-wide.
        assertFalse(AdPlacementDeclaration("home_open", AdFormat.APP_OPEN).acceptsStagingAppOpen())
        // The app open of the loading screen and of the resume ad.
        assertFalse(AdPlacement.OPEN_APP.declaration.acceptsStagingAppOpen())
        assertFalse(AdPlacement.RESUME.declaration.acceptsStagingAppOpen())
        // Staging only, but not app open.
        assertFalse(AdPlacement.GALLERY_INTERSTITIAL.declaration.acceptsStagingAppOpen())
        // Control: the app open placement of the gallery.
        assertTrue(AdPlacement.GALLERY_APP_OPEN.declaration.acceptsStagingAppOpen())
    }

    @Test
    fun rewardedPolicyOf_override_onlyForStagingOnly() {
        val notStaging = AdPlacementDeclaration("rw_export", AdFormat.REWARDED, stagingOnly = false)
        assertThrows(IllegalArgumentException::class.java) {
            rewardedPolicyOf(notStaging, RewardedNoAdPolicy.Allow)
        }

        // Control: the gallery placement takes the policy given.
        val gallery = AdPlacement.GALLERY_RW_ACTION.declaration
        assertEquals(RewardedNoAdPolicy.Allow, rewardedPolicyOf(gallery, RewardedNoAdPolicy.Allow))
        assertEquals(RewardedNoAdPolicy.AllowAfter(2), rewardedPolicyOf(gallery, RewardedNoAdPolicy.AllowAfter(2)))
        assertEquals(RewardedNoAdPolicy.Block, rewardedPolicyOf(gallery, RewardedNoAdPolicy.Block))
    }

    @Test
    fun rewardedPolicyOf_noOverride_isPolicyOfTheLine() {
        assertEquals(RewardedNoAdPolicy.Block, rewardedPolicyOf(AdPlacement.GALLERY_RW_ACTION.declaration, null))
        // Control (L-055): a line with a policy of its own gives that policy, so null does not always mean Block;
        // no override is needed for a placement that is not staging only.
        val export = AdPlacementDeclaration(
            "rw_export",
            AdFormat.REWARDED,
            noAdPolicy = RewardedNoAdPolicy.AllowAfter(2),
        )
        assertEquals(RewardedNoAdPolicy.AllowAfter(2), rewardedPolicyOf(export, null))
    }
}
