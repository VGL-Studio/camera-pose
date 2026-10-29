package com.vglstudio.camera.pose.line.guide.core.ads

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AdUnitIdsTest {
    @Test
    fun configuredUnits_areParsedPerPlacementKey() {
        val ids = ConfiguredAdUnitIds(
            " sample_native_small = unit/1 ; other_key=unit/2;open_app=unit/3;open_app_inter=unit/4;" +
                "native_combo=unit/5;resume=unit/6;resume_inter=unit/7",
        )
        assertEquals("unit/1", ids.of(AdPlacement.SAMPLE_NATIVE_SMALL))
        assertTrue(ids.missing.isEmpty())
        // Control: app-wide placements still need a unit on production (plan T-024, section 3.3).
        val withoutAppWide = ConfiguredAdUnitIds(" sample_native_small = unit/1 ; other_key=unit/2;")
        assertEquals(
            listOf(
                AdPlacement.OPEN_APP,
                AdPlacement.OPEN_APP_INTER,
                AdPlacement.NATIVE_AFTER_INTER,
                AdPlacement.RESUME,
                AdPlacement.RESUME_INTER,
            ),
            withoutAppWide.missing,
        )
    }

    @Test
    fun placementWithoutUnit_isReportedMissingAndShowsNoAd() {
        val ids = ConfiguredAdUnitIds("")
        // Staging-only placements (gallery_*) never ship, so a release needs no unit for them.
        assertEquals(AdPlacement.entries.filterNot { it.stagingOnly }, ids.missing)
        assertNull(ids.of(AdPlacement.SAMPLE_NATIVE_SMALL))
    }

    @Test
    fun stagingOnlyPlacement_hasNoUnit_evenWhenConfigured() {
        val ids = ConfiguredAdUnitIds("gallery_native_small=x;sample_native_small=y")
        // Control: the other placement of the same configuration gets its unit.
        assertEquals("y", ids.of(AdPlacement.SAMPLE_NATIVE_SMALL))
        assertNull(ids.of(AdPlacement.GALLERY_NATIVE_SMALL))
    }

    @Test
    fun googleTestIds_coverGalleryPlacements() {
        val gallery = AdPlacement.entries.filter { it.stagingOnly }
        assertEquals(13, gallery.size)
        gallery.forEach { placement ->
            assertTrue(placement.key, GoogleTestAdUnitIds.of(placement).startsWith("ca-app-pub-3940256099942544/"))
        }
    }

    @Test(expected = IllegalArgumentException::class)
    fun malformedEntry_isRejected() {
        ConfiguredAdUnitIds("sample_native_small")
    }

    @Test
    fun googleTestUnits_existForEveryPlacement() {
        AdPlacement.entries.forEach { placement ->
            assertTrue(GoogleTestAdUnitIds.of(placement).startsWith("ca-app-pub-3940256099942544/"))
        }
    }

    @Test
    fun googleTestUnits_nativeFormats() {
        assertEquals("ca-app-pub-3940256099942544/2247696110", GoogleTestAdUnitIds.of(AdPlacement.SAMPLE_NATIVE_SMALL))
        // NATIVE_MEDIUM is also checked through the format table, independent of the placements.
        assertEquals("ca-app-pub-3940256099942544/1044960115", GoogleTestAdUnitIds.unitFor(AdFormat.NATIVE_MEDIUM))
        assertEquals("ca-app-pub-3940256099942544/2247696110", GoogleTestAdUnitIds.unitFor(AdFormat.NATIVE_SMALL))
    }

    @Test
    fun googleTestUnit_existsForEveryFormat() {
        AdFormat.entries.forEach { format ->
            assertTrue(
                "$format has no test unit",
                GoogleTestAdUnitIds.unitFor(format).startsWith("ca-app-pub-3940256099942544/"),
            )
        }
        assertEquals("ca-app-pub-3940256099942544/1044960115", GoogleTestAdUnitIds.unitFor(AdFormat.NATIVE_FULL_SCREEN))
    }

    @Test
    fun everyPlacementKey_isUniqueAndUsableInRemoteConfig() {
        val keys = AdPlacement.entries.map { it.key }
        assertEquals(keys.size, keys.toSet().size)
        keys.forEach { assertTrue("Key '$it' must be lower_snake_case", Regex("^[a-z0-9_]+$").matches(it)) }
    }

    @Test
    fun googleTestUnits_newFormats() {
        assertEquals("ca-app-pub-3940256099942544/9257395921", GoogleTestAdUnitIds.unitFor(AdFormat.APP_OPEN))
        assertEquals("ca-app-pub-3940256099942544/5224354917", GoogleTestAdUnitIds.unitFor(AdFormat.REWARDED))
        assertEquals("ca-app-pub-3940256099942544/9214589741", GoogleTestAdUnitIds.unitFor(AdFormat.BANNER_COLLAPSIBLE))
        assertEquals("ca-app-pub-3940256099942544/1044960115", GoogleTestAdUnitIds.unitFor(AdFormat.NATIVE_STEP))
        assertEquals(
            "ca-app-pub-3940256099942544/1044960115",
            GoogleTestAdUnitIds.unitFor(AdFormat.NATIVE_IN_COLLECTION),
        )
    }

    @Test
    fun staging_unitOfEachLayerFormat() {
        // Staging shows every layer of a bottom bar with the test unit of the layer's format (question 2, plan T-030).
        val bar = AdPlacement.SAMPLE_NATIVE_SMALL
        assertEquals(GoogleTestAdUnitIds.unitFor(AdFormat.BANNER), GoogleTestAdUnitIds.of(bar, AdFormat.BANNER))
        assertEquals(
            GoogleTestAdUnitIds.unitFor(AdFormat.NATIVE_COLLAPSIBLE),
            GoogleTestAdUnitIds.of(bar, AdFormat.NATIVE_COLLAPSIBLE),
        )
        assertEquals(GoogleTestAdUnitIds.of(bar), GoogleTestAdUnitIds.of(bar, AdFormat.NATIVE_SMALL))
    }

    @Test
    fun production_otherFormat_null_ownFormat_configured() {
        val ids = ConfiguredAdUnitIds("sample_native_small=unit/1")
        val bar = AdPlacement.SAMPLE_NATIVE_SMALL
        // Control: the placement's own format gets its configured unit.
        assertEquals("unit/1", ids.of(bar, AdFormat.NATIVE_SMALL))
        // The banner and collapsible layers have no unit on production until units per slot exist (spec, Q14).
        assertNull(ids.of(bar, AdFormat.BANNER))
        assertNull(ids.of(bar, AdFormat.NATIVE_COLLAPSIBLE))
    }
}
