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
                "native_combo=unit/5;resume=unit/6;resume_inter=unit/7;banner=unit/8;native_collap=unit/9;" +
                // The placements of this app (plan T-074, ADR-0029).
                "poses_bottom=unit/10;pose_detail_bottom=unit/11;review_interstitial=unit/12;" +
                "inter_pose_detail=unit/13;poses_in_collection=unit/14",
        )
        assertEquals("unit/1", ids.of(AdPlacement.SAMPLE_NATIVE_SMALL))
        assertTrue(ids.missing.isEmpty())
        // Control: without the two slot keys, the layers of the bottom bar are missing (plan T-056, section 3.5).
        val withoutSlots = ConfiguredAdUnitIds(
            " sample_native_small = unit/1 ; other_key=unit/2;open_app=unit/3;open_app_inter=unit/4;" +
                "native_combo=unit/5;resume=unit/6;resume_inter=unit/7;" +
                "poses_bottom=unit/10;pose_detail_bottom=unit/11;review_interstitial=unit/12;" +
                "inter_pose_detail=unit/13;poses_in_collection=unit/14",
        )
        assertEquals(listOf("banner", "native_collap"), withoutSlots.missing)
        // Control: app-wide placements still need a unit on production (plan T-024, section 3.3).
        val withoutAppWide = ConfiguredAdUnitIds(
            " sample_native_small = unit/1 ; other_key=unit/2;" +
                "poses_bottom=unit/10;pose_detail_bottom=unit/11;review_interstitial=unit/12;" +
                "inter_pose_detail=unit/13;poses_in_collection=unit/14",
        )
        assertEquals(
            listOf("open_app", "open_app_inter", "native_combo", "resume", "resume_inter", "banner", "native_collap"),
            withoutAppWide.missing,
        )
    }

    @Test
    fun placementWithoutUnit_isReportedMissingAndShowsNoAd() {
        val ids = ConfiguredAdUnitIds("")
        // Staging-only placements (gallery_*) never ship, so a release needs no unit for them.
        assertEquals(
            AdPlacement.entries.filterNot { it.stagingOnly }.map { it.key } + listOf("banner", "native_collap"),
            ids.missing,
        )
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
        assertEquals(14, gallery.size)
        gallery.forEach { placement ->
            assertTrue(placement.key, GoogleTestAdUnitIds.of(placement).startsWith("ca-app-pub-3940256099942544/"))
        }
    }

    @Test
    fun googleTestUnit_bannerMrec_isFixedSizeBannerUnit() {
        // Google's fixed-size banner unit, used for every fixed size, 300 x 250 too (plan T-062, question 1).
        val fixedSize = "ca-app-pub-3940256099942544/6300978111"
        // Control: the adaptive banner has its own unit.
        assertEquals("ca-app-pub-3940256099942544/9214589741", GoogleTestAdUnitIds.unitFor(AdFormat.BANNER))
        assertEquals(fixedSize, GoogleTestAdUnitIds.unitFor(AdFormat.BANNER_MREC))
        assertEquals(fixedSize, GoogleTestAdUnitIds.of(AdPlacement.GALLERY_BANNER_MREC))
        // Staging only: production never gets a unit for it, nor reports it missing.
        val configured = ConfiguredAdUnitIds("gallery_banner_mrec=x")
        assertNull(configured.of(AdPlacement.GALLERY_BANNER_MREC))
        assertTrue("gallery_banner_mrec" !in configured.missing)
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
        // The banner and collapsible layers have no unit on production without a slot key (plan T-056).
        assertNull(ids.of(bar, AdFormat.BANNER))
        assertNull(ids.of(bar, AdFormat.NATIVE_COLLAPSIBLE))
    }

    // Units per slot for the layers of a bottom bar (plan T-056, KB-17).

    @Test
    fun production_layerUnits_readFromSlotKeys() {
        val ids = ConfiguredAdUnitIds("sample_native_small=unit/1;banner=unit/b;native_collap=unit/c")
        val bar = AdPlacement.SAMPLE_NATIVE_SMALL
        assertEquals("unit/b", ids.of(bar, AdFormat.BANNER))
        assertEquals("unit/c", ids.of(bar, AdFormat.NATIVE_COLLAPSIBLE))
        assertEquals("unit/1", ids.of(bar, AdFormat.NATIVE_SMALL))
    }

    @Test
    fun production_ownFormat_ignoresSlotKey() {
        // A slot key never stands in for the placement's own format (plan T-056, section 3.1, point 2).
        val slotsOnly = ConfiguredAdUnitIds("native_small=unit/s;interstitial=unit/i")
        assertNull(slotsOnly.of(AdPlacement.SAMPLE_NATIVE_SMALL))
        assertNull(slotsOnly.of(AdPlacement.SAMPLE_NATIVE_SMALL, AdFormat.NATIVE_SMALL))
        assertNull(slotsOnly.of(AdPlacement.OPEN_APP_INTER))
        // Control: with the placement keys, the same configuration gives their units.
        val withPlacements = ConfiguredAdUnitIds(
            "native_small=unit/s;interstitial=unit/i;sample_native_small=unit/1;open_app_inter=unit/4",
        )
        assertEquals("unit/1", withPlacements.of(AdPlacement.SAMPLE_NATIVE_SMALL))
        assertEquals("unit/1", withPlacements.of(AdPlacement.SAMPLE_NATIVE_SMALL, AdFormat.NATIVE_SMALL))
        assertEquals("unit/4", withPlacements.of(AdPlacement.OPEN_APP_INTER))
    }

    @Test
    fun production_stagingOnlyLayer_null() {
        val ids = ConfiguredAdUnitIds("banner=unit/b;native_collap=unit/c")
        assertNull(ids.of(AdPlacement.GALLERY_NATIVE_SMALL, AdFormat.BANNER))
        assertNull(ids.of(AdPlacement.GALLERY_NATIVE_SMALL, AdFormat.NATIVE_COLLAPSIBLE))
        // Control: a production placement gets the unit of the same slot.
        assertEquals("unit/b", ids.of(AdPlacement.SAMPLE_NATIVE_SMALL, AdFormat.BANNER))
    }

    @Test
    fun missingUnitKeys_listsLayerSlotsOnce() {
        val placements = listOf(
            AdPlacementDeclaration("bar_a", AdFormat.NATIVE_SMALL),
            AdPlacementDeclaration("bar_b", AdFormat.NATIVE_SMALL),
            AdPlacementDeclaration("inter_a", AdFormat.INTERSTITIAL),
        )
        // Each slot key once, though two bars need it.
        assertEquals(
            listOf("banner", "native_collap"),
            missingUnitKeys(placements, setOf("bar_a", "bar_b", "inter_a")),
        )
        assertEquals(
            listOf("native_collap"),
            missingUnitKeys(placements, setOf("bar_a", "bar_b", "inter_a", "banner")),
        )
        assertEquals(
            listOf("bar_a", "bar_b", "inter_a", "banner", "native_collap"),
            missingUnitKeys(placements, emptySet()),
        )
    }

    @Test
    fun missingUnitKeys_noBar_noSlotKeys() {
        val inter = AdPlacementDeclaration("inter_a", AdFormat.INTERSTITIAL)
        val gallery = AdPlacementDeclaration("gallery_x", AdFormat.NATIVE_SMALL, stagingOnly = true)
        assertEquals(listOf("inter_a"), missingUnitKeys(listOf(inter, gallery), emptySet()))
        // Control: the same native small placement shipped on production needs its unit and the two slot keys.
        assertEquals(
            listOf("inter_a", "gallery_x", "banner", "native_collap"),
            missingUnitKeys(listOf(inter, gallery.copy(stagingOnly = false)), emptySet()),
        )
    }

    @Test
    fun missingUnitsWarning_listsMissingKeys() {
        val placementUnits = "sample_native_small=unit/1;open_app=unit/3;open_app_inter=unit/4;" +
            // The placements of this app (ADR-0029).
            "poses_bottom=unit/10;pose_detail_bottom=unit/11;review_interstitial=unit/12;inter_pose_detail=unit/13;" +
            "poses_in_collection=unit/14;" +
            "native_combo=unit/5;resume=unit/6;resume_inter=unit/7"
        val warning = missingUnitsWarning(ConfiguredAdUnitIds(placementUnits))
        assertTrue("warning: $warning", warning != null && "banner, native_collap" in warning)
        // Control: with the two slot keys nothing is missing, so no warning.
        assertNull(missingUnitsWarning(ConfiguredAdUnitIds("$placementUnits;banner=unit/8;native_collap=unit/9")))
        // Staging and other unit sources never warn.
        assertNull(missingUnitsWarning(GoogleTestAdUnitIds))
        assertNull(missingUnitsWarning(AdUnitIds { null }))
    }

    @Test
    fun placementKeys_neverSlotNames() {
        val formatCodes = AdFormat.entries.map { it.code }.toSet()
        // Control: the slot names are among the format codes, so the check below is not empty.
        assertTrue(formatCodes.containsAll(listOf("banner", "native_collap")))
        assertEquals(emptySet<String>(), AdPlacement.entries.map { it.key }.toSet() intersect formatCodes)
    }
}
