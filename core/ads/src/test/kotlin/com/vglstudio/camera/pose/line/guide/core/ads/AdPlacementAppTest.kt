package com.vglstudio.camera.pose.line.guide.core.ads

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The placements of this app in the AdsKit framework (spec ads, questions 2, 4 and 8; spec camera and poses,
 * section 5): declared with their spec format, switched by `is_show_<code>`, no sample banner, the two bottom
 * banners replaced by native bottom bars, and the interstitial before the camera in the pose detail (plan T-079).
 */
class AdPlacementAppTest {
    @Test
    fun appPlacements_declaredWithSpecFormats() {
        val expected = mapOf(
            "poses_bottom" to AdFormat.NATIVE_SMALL,
            "pose_detail_bottom" to AdFormat.NATIVE_SMALL,
            "review_interstitial" to AdFormat.INTERSTITIAL,
            "inter_pose_detail" to AdFormat.INTERSTITIAL,
            "poses_in_collection" to AdFormat.NATIVE_IN_COLLECTION,
            "rw_premium_pose" to AdFormat.REWARDED,
        )
        val byKey = AdPlacement.entries.associateBy { it.key }
        expected.forEach { (key, format) ->
            val placement = byKey[key]
            assertTrue("$key is not declared", placement != null)
            assertEquals(key, format, placement!!.format)
            assertFalse("$key must not be app-wide", placement.appWide)
            assertFalse("$key must not be staging only", placement.stagingOnly)
            assertEquals("is_show_$key", AdsSettings.placementEnabled(placement).name)
            assertEquals(false, AdsSettings.placementEnabled(placement).default)
        }
        assertEquals(AdPlacement.POSES_BOTTOM, byKey["poses_bottom"])
        assertEquals(AdPlacement.POSE_DETAIL_BOTTOM, byKey["pose_detail_bottom"])
        assertEquals(AdPlacement.REVIEW_INTERSTITIAL, byKey["review_interstitial"])
        assertEquals(AdPlacement.INTER_POSE_DETAIL, byKey["inter_pose_detail"])
        assertEquals(AdPlacement.POSES_IN_COLLECTION, byKey["poses_in_collection"])
        assertEquals(AdPlacement.RW_PREMIUM_POSE, byKey["rw_premium_pose"])
    }

    @Test
    fun rwPremiumPose_blocksWithoutAd() {
        // Spec ads, question 12; spec poses, KB-8 criterion 6: the unlock is blocked when no ad can be shown (plan T-086).
        val placement = AdPlacement.RW_PREMIUM_POSE
        assertEquals("rw_premium_pose", placement.key)
        assertEquals(AdFormat.REWARDED, placement.format)
        assertNull(placement.noAdPolicy)
        assertEquals(RewardedNoAdPolicy.Block, placement.declaration.effectiveNoAdPolicy)
        assertFalse(placement.stagingOnly)
        assertFalse(placement.appWide)
        assertEquals("is_show_rw_premium_pose", AdsSettings.placementEnabled(placement).name)
        assertEquals(false, AdsSettings.placementEnabled(placement).default)
    }

    @Test
    fun posesInCollection_cardEvery8_hiddenWhenPoolEmpty_offByDefault() {
        // Spec ads, question 9; spec poses, KB-6 criterion 5 (plan T-080).
        val placement = AdPlacement.POSES_IN_COLLECTION
        assertEquals(AdFormat.NATIVE_IN_COLLECTION, placement.format)
        assertEquals(8, placement.nativeEvery)
        assertEquals(8L, AdsSettings.nativeEvery(placement)!!.default)
        assertFalse(placement.keepSpaceWhenPoolEmpty)
        assertEquals(false, AdsSettings.placementEnabled(placement).default)
        // Control: the gallery card of the framework keeps its own N, so the values above are the app's.
        assertEquals(4, AdPlacement.GALLERY_IN_COLLECTION.nativeEvery)
    }

    @Test
    fun interPoseDetail_sharesDefaultIntervalWithReview() {
        // Spec ads, question 9; spec poses, KB-4: the shared `ads_interval`, read and moved like review_interstitial.
        val placement = AdPlacement.INTER_POSE_DETAIL
        assertEquals(AdFormat.INTERSTITIAL, placement.format)
        assertEquals(AdIntervalGroup.DEFAULT, placement.intervalGroup)
        assertEquals(AdPlacement.REVIEW_INTERSTITIAL.intervalGroup, placement.intervalGroup)
        assertFalse(placement.appWide)
        assertFalse(placement.ignoreInterval)
        assertTrue(placement.recordInterval)
    }

    @Test
    fun bannersRemoved() {
        // Control: the interstitial of the app is still declared, so the list read is the real one.
        assertTrue(AdPlacement.entries.any { it.key == "review_interstitial" })
        listOf("poses_banner", "poses_detail_banner").forEach { key ->
            assertTrue("$key must be removed (spec ads, question 8)", AdPlacement.entries.none { it.key == key })
        }
    }

    @Test
    fun noSamplePlacement() {
        // Control: the library bottom bar of the app is there, so the list read is the real one.
        assertTrue(AdPlacement.entries.any { it.key == "poses_bottom" })
        // The app has no sample screen, so no placement of it (spec ads, question 4; plan T-081, ADR-1008).
        listOf("sample_bottom_banner", "sample_native_small").forEach { key ->
            assertTrue("$key must be removed", AdPlacement.entries.none { it.key == key })
        }
    }
}
