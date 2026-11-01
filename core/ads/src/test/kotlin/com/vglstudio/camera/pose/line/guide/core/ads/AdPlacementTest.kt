package com.vglstudio.camera.pose.line.guide.core.ads

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

/** Placements of the staging ad gallery: one per format, staging only, same rule as scripts/release.sh (plan T-014). */
class AdPlacementTest {
    private val galleryPlacements = AdPlacement.entries.filter { it.key.startsWith("gallery_") }

    @Test
    fun galleryPlacements_coverEveryFormatOnce() {
        assertEquals(14, galleryPlacements.size)
        // Every format but the collapsible layer, which loads through the placement of its bottom bar (plan T-036).
        assertEquals(
            AdFormat.entries.toSet() - AdFormat.NATIVE_COLLAPSIBLE,
            galleryPlacements.map { it.format }.toSet(),
        )
        val keys = AdPlacement.entries.map { it.key }
        assertEquals(keys.size, keys.toSet().size)
    }

    @Test
    fun galleryBannerMrec_isStagingOnlyMrec() {
        val mrec = AdPlacement.GALLERY_BANNER_MREC
        assertEquals("gallery_banner_mrec", mrec.key)
        assertEquals(AdFormat.BANNER_MREC, mrec.format)
        assertTrue(mrec.stagingOnly)
        assertFalse(mrec.appWide)
        assertEquals("is_show_gallery_banner_mrec", AdsSettings.placementEnabled(mrec).name)
    }

    @Test
    fun galleryAppOpen_isStagingOnly_notAppWide() {
        // Not app-wide: it has a switch of its own, `is_show_gallery_app_open`, which staging switches on.
        assertTrue(AdPlacement.GALLERY_APP_OPEN.stagingOnly)
        assertFalse(AdPlacement.GALLERY_APP_OPEN.appWide)
        assertEquals(AdFormat.APP_OPEN, AdPlacement.GALLERY_APP_OPEN.format)
        assertEquals("is_show_gallery_app_open", AdsSettings.placementEnabled(AdPlacement.GALLERY_APP_OPEN).name)
        // Control: the app open of the loading screen is app-wide.
        assertTrue(AdPlacement.OPEN_APP.appWide)
    }

    @Test
    fun stagingOnly_isExactlyGalleryPrefix() {
        // Control: both kinds exist, so the check below compares true and false values.
        assertTrue(AdPlacement.entries.any { it.key.startsWith("gallery_") })
        assertTrue(AdPlacement.entries.any { !it.key.startsWith("gallery_") })
        AdPlacement.entries.forEach { placement ->
            // Same prefix as the one scripts/release.sh skips (PR #81), written out on purpose.
            assertEquals(placement.key, placement.key.startsWith("gallery_"), placement.stagingOnly)
        }
    }

    @Test
    fun otherPlacements_areNotStagingOnly() {
        assertFalse(AdPlacement.SAMPLE_NATIVE_SMALL.stagingOnly)
        AdPlacement.entries.filterNot { it.key.startsWith("gallery_") }.forEach { placement ->
            assertFalse(placement.key, placement.stagingOnly)
        }
        // Control: a gallery placement is staging only, so the property does not always return false.
        assertTrue(AdPlacement.GALLERY_NATIVE_SMALL.stagingOnly)
    }

    @Test
    fun intervalGroupOnNonInterstitial_isRejected() {
        // Control: the same group on an interstitial is accepted.
        AdPlacementDeclaration("inter_sounds", AdFormat.INTERSTITIAL, AdIntervalGroup.named("sounds", 30))
        listOf(AdFormat.NATIVE_SMALL, AdFormat.NATIVE_FULL_SCREEN, AdFormat.APP_OPEN, AdFormat.REWARDED).forEach {
            assertThrows("$it", IllegalArgumentException::class.java) {
                AdPlacementDeclaration("x", it, AdIntervalGroup.named("sounds", 30))
            }
        }
    }

    @Test
    fun nativeEveryOnNonListFormat_isRejected() {
        // Control: the list formats accept it.
        listOf(AdFormat.NATIVE_SMALL, AdFormat.NATIVE_MEDIUM, AdFormat.NATIVE_IN_COLLECTION).forEach {
            AdPlacementDeclaration("list_x", it, nativeEvery = 6)
        }
        listOf(AdFormat.BANNER, AdFormat.INTERSTITIAL, AdFormat.NATIVE_COLLAPSIBLE, AdFormat.NATIVE_STEP).forEach {
            assertThrows("$it", IllegalArgumentException::class.java) {
                AdPlacementDeclaration("x", it, nativeEvery = 6)
            }
        }
    }

    @Test
    fun ctaLarge_onlyNativeMedium() {
        // Control: medium accepts it.
        AdPlacementDeclaration("medium_x", AdFormat.NATIVE_MEDIUM, ctaLarge = true)
        AdFormat.entries.filterNot { it == AdFormat.NATIVE_MEDIUM }.forEach {
            assertThrows("$it", IllegalArgumentException::class.java) {
                AdPlacementDeclaration("x", it, ctaLarge = true)
            }
        }
    }

    @Test
    fun keepSpaceWhenPoolEmpty_onlyPooledFormats() {
        val pooled = listOf(AdFormat.NATIVE_MEDIUM, AdFormat.NATIVE_IN_COLLECTION, AdFormat.NATIVE_STEP)
        // Control: the formats of a pool accept it.
        pooled.forEach { AdPlacementDeclaration("pool_x", it, keepSpaceWhenPoolEmpty = true) }
        AdFormat.entries.filterNot { it in pooled }.forEach {
            assertThrows("$it", IllegalArgumentException::class.java) {
                AdPlacementDeclaration("x", it, keepSpaceWhenPoolEmpty = true)
            }
        }
    }

    @Test
    fun nativeEveryBelowOne_isRejected() {
        // Control: 1 is accepted.
        AdPlacementDeclaration("list_x", AdFormat.NATIVE_IN_COLLECTION, nativeEvery = 1)
        listOf(0, -1).forEach {
            assertThrows("$it", IllegalArgumentException::class.java) {
                AdPlacementDeclaration("list_x", AdFormat.NATIVE_IN_COLLECTION, nativeEvery = it)
            }
        }
    }

    @Test
    fun inListGalleryPlacements_declareNativeEvery8() {
        assertEquals(8, AdPlacement.GALLERY_INLIST_SMALL.nativeEvery)
        assertEquals(8, AdPlacement.GALLERY_INLIST_MEDIUM.nativeEvery)
        // The cards of the grid and pager pages (plan T-036).
        assertEquals(4, AdPlacement.GALLERY_IN_COLLECTION.nativeEvery)
        val inList = setOf(
            AdPlacement.GALLERY_INLIST_SMALL,
            AdPlacement.GALLERY_INLIST_MEDIUM,
            AdPlacement.GALLERY_IN_COLLECTION,
        )
        AdPlacement.entries.filterNot { it in inList }
            .forEach { assertNull(it.key, it.nativeEvery) }
    }

    @Test
    fun everyPlacement_isInDefaultGroup() {
        // Control: groups compare by name and default, so the check below is not an identity check.
        assertEquals(AdIntervalGroup.named("sounds", 30), AdIntervalGroup.named("sounds", 30))
        assertFalse(AdIntervalGroup.named("sounds", 30) == AdIntervalGroup.named("sounds", 40))
        assertFalse(AdIntervalGroup.named("sounds", 30) == AdIntervalGroup.DEFAULT)
        AdPlacement.entries.forEach { assertEquals(it.key, AdIntervalGroup.DEFAULT, it.intervalGroup) }
    }

    @Test
    fun intervalAttributes_defaultAndValidated() {
        // OPEN_APP_INTER ignores the interval gate on purpose (question 3 of plan T-024); checked on its own.
        assertTrue(AdPlacement.OPEN_APP_INTER.ignoreInterval)
        assertTrue(AdPlacement.OPEN_APP_INTER.recordInterval)
        // RESUME_INTER too (AS-01, plan T-025): no `ads_interval` gate, the mark still moves when it closes.
        assertTrue(AdPlacement.RESUME_INTER.ignoreInterval)
        assertTrue(AdPlacement.RESUME_INTER.recordInterval)
        val skipInterval = setOf(AdPlacement.OPEN_APP_INTER, AdPlacement.RESUME_INTER)
        AdPlacement.entries.filterNot { it in skipInterval }.forEach {
            assertFalse(it.key, it.ignoreInterval)
            assertTrue(it.key, it.recordInterval)
            assertFalse(it.key, it.declaration.ignoreInterval)
            assertTrue(it.key, it.declaration.recordInterval)
        }
        assertThrows(IllegalArgumentException::class.java) {
            AdPlacementDeclaration("x", AdFormat.NATIVE_SMALL, ignoreInterval = true)
        }
        assertThrows(IllegalArgumentException::class.java) {
            AdPlacementDeclaration("x", AdFormat.APP_OPEN, recordInterval = false)
        }
        // Control: the full-screen formats accept both attributes.
        AdPlacementDeclaration("x", AdFormat.INTERSTITIAL, recordInterval = false)
        AdPlacementDeclaration("x", AdFormat.NATIVE_FULL_SCREEN, ignoreInterval = true)
    }

    @Test
    fun openAppInter_ignoresIntervalAndPushesMark() {
        val declaration = AdPlacement.OPEN_APP_INTER.declaration
        assertFalse(declaration.readsInterval())
        assertTrue(declaration.pushesIntervalMark())
        assertEquals(AdFormat.INTERSTITIAL, AdPlacement.OPEN_APP_INTER.format)
        assertTrue(AdPlacement.OPEN_APP_INTER.appWide)
        assertTrue(declaration.appWide)
    }

    @Test
    fun openApp_neverPushesMark() {
        // I-02: closing an app open ad moves no `ads_interval` mark.
        assertFalse(AdPlacement.OPEN_APP.declaration.pushesIntervalMark())
        assertEquals(AdFormat.APP_OPEN, AdPlacement.OPEN_APP.format)
        assertTrue(AdPlacement.OPEN_APP.appWide)
        assertTrue(AdPlacement.OPEN_APP.declaration.appWide)
    }

    @Test
    fun appWide_onlyForAppOpenInterstitialAndNativeFullScreen() {
        // Control: app open, interstitial and native full screen (the combo of plan T-026) accept it.
        AdPlacementDeclaration("x", AdFormat.APP_OPEN, appWide = true)
        AdPlacementDeclaration("x", AdFormat.INTERSTITIAL, appWide = true)
        AdPlacementDeclaration("x", AdFormat.NATIVE_FULL_SCREEN, appWide = true)
        listOf(AdFormat.NATIVE_SMALL, AdFormat.BANNER, AdFormat.REWARDED).forEach {
            assertThrows("$it", IllegalArgumentException::class.java) {
                AdPlacementDeclaration("x", it, appWide = true)
            }
        }
    }

    @Test
    fun appWidePlacements_areOpenAppAndCombo() {
        val appWide = AdPlacement.entries.filter { it.appWide }
        assertEquals(
            listOf(
                AdPlacement.OPEN_APP,
                AdPlacement.OPEN_APP_INTER,
                AdPlacement.NATIVE_AFTER_INTER,
                AdPlacement.RESUME,
                AdPlacement.RESUME_INTER,
            ),
            appWide,
        )
        appWide.forEach { assertFalse(it.key, it.stagingOnly) }
    }

    @Test
    fun startupTurn_acceptsAppWideOnly() {
        assertThrows(IllegalArgumentException::class.java) { StartupAdTurn(AdPlacement.SAMPLE_NATIVE_SMALL) }
        // Control: an app-wide placement is accepted.
        assertEquals(AdPlacement.OPEN_APP, StartupAdTurn(AdPlacement.OPEN_APP).placement)
    }

    @Test
    fun nativeCombo_isAppWideNativeFullScreen() {
        val combo = AdPlacement.NATIVE_AFTER_INTER
        assertEquals("native_combo", combo.key)
        assertEquals(AdFormat.NATIVE_FULL_SCREEN, combo.format)
        assertTrue(combo.appWide)
        assertTrue(combo.declaration.appWide)
        assertFalse(combo.stagingOnly)
        val names = AdsSettings.all().map { it.name }
        assertFalse(names.toString(), "is_show_native_combo" in names)
        // Control: the shared key that switches the combo on is there.
        assertTrue(names.toString(), "is_show_native_after_inter" in names)
    }

    @Test
    fun startupTurn_rejectsNativeCombo() {
        assertThrows(IllegalArgumentException::class.java) { StartupAdTurn(AdPlacement.NATIVE_AFTER_INTER) }
        // Control: the app-wide interstitial is still a startup placement.
        assertEquals(AdPlacement.OPEN_APP_INTER, StartupAdTurn(AdPlacement.OPEN_APP_INTER).placement)
    }

    @Test
    fun noAdPolicyOnNonRewarded_isRejected() {
        listOf(AdFormat.INTERSTITIAL, AdFormat.NATIVE_SMALL, AdFormat.APP_OPEN).forEach {
            assertThrows(IllegalArgumentException::class.java) {
                AdPlacementDeclaration("x", it, noAdPolicy = RewardedNoAdPolicy.Allow)
            }
        }
        // Control: a rewarded placement takes it.
        AdPlacementDeclaration("x", AdFormat.REWARDED, noAdPolicy = RewardedNoAdPolicy.Allow)
    }

    @Test
    fun rewardedWithoutPolicy_isBlock() {
        assertEquals(RewardedNoAdPolicy.Block, AdPlacementDeclaration("x", AdFormat.REWARDED).effectiveNoAdPolicy)
        // Control (L-055): a declared policy is kept.
        assertEquals(
            RewardedNoAdPolicy.Allow,
            AdPlacementDeclaration("x", AdFormat.REWARDED, noAdPolicy = RewardedNoAdPolicy.Allow).effectiveNoAdPolicy,
        )
        assertEquals(RewardedNoAdPolicy.Block, AdPlacement.GALLERY_RW_ACTION.declaration.effectiveNoAdPolicy)
    }

    @Test
    fun allowAfterBelowOne_isRejected() {
        assertThrows(IllegalArgumentException::class.java) { RewardedNoAdPolicy.AllowAfter(0) }
        assertEquals(1, RewardedNoAdPolicy.AllowAfter(1).tries)
    }

    @Test
    fun rewardedDeclarations_rwGetItemAndRwExport_areAccepted() {
        val getItem = AdPlacementDeclaration("rw_get_item", AdFormat.REWARDED)
        val export = AdPlacementDeclaration(
            "rw_export",
            AdFormat.REWARDED,
            noAdPolicy = RewardedNoAdPolicy.AllowAfter(2),
        )

        assertEquals(RewardedNoAdPolicy.Block, getItem.effectiveNoAdPolicy)
        assertEquals(RewardedNoAdPolicy.AllowAfter(2), export.effectiveNoAdPolicy)
    }

    @Test
    fun rewarded_cannotDeclareIntervalOptions() {
        // Guard (R-05): rewarded never reads nor moves the `ads_interval` mark.
        assertThrows(IllegalArgumentException::class.java) {
            AdPlacementDeclaration("x", AdFormat.REWARDED, ignoreInterval = true)
        }
        assertThrows(IllegalArgumentException::class.java) {
            AdPlacementDeclaration("x", AdFormat.REWARDED, recordInterval = false)
        }
        // Control: an interstitial takes both.
        AdPlacementDeclaration("x", AdFormat.INTERSTITIAL, ignoreInterval = true, recordInterval = false)
    }

    @Test
    fun resumePlacements_areAppWide() {
        val resume = AdPlacement.RESUME
        assertEquals("resume", resume.key)
        assertEquals(AdFormat.APP_OPEN, resume.format)
        assertTrue(resume.appWide)
        assertFalse(resume.stagingOnly)
        // App open moves no `ads_interval` mark (I-02).
        assertFalse(resume.declaration.pushesIntervalMark())
        val inter = AdPlacement.RESUME_INTER
        assertEquals("resume_inter", inter.key)
        assertEquals(AdFormat.INTERSTITIAL, inter.format)
        assertTrue(inter.ignoreInterval)
        assertTrue(inter.recordInterval)
        assertTrue(inter.appWide)
        assertFalse(inter.stagingOnly)
        assertFalse(inter.declaration.readsInterval())
        assertTrue(inter.declaration.pushesIntervalMark())
        assertEquals(AdIntervalGroup.DEFAULT, inter.intervalGroup)
        val names = AdsSettings.all().map { it.name }
        assertFalse(names.toString(), "is_show_resume" in names)
        assertFalse(names.toString(), "is_show_resume_inter" in names)
        // Control: the shared keys that switch the resume ad are there.
        assertTrue(names.toString(), "show_resum_ads" in names)
        assertTrue(names.toString(), "resume_ad_type" in names)
    }

    @Test
    fun startupTurn_rejectsResumePlacements() {
        assertThrows(IllegalArgumentException::class.java) { StartupAdTurn(AdPlacement.RESUME) }
        assertThrows(IllegalArgumentException::class.java) { StartupAdTurn(AdPlacement.RESUME_INTER) }
        // Control: the two placements of the loading screen are accepted.
        assertEquals(AdPlacement.OPEN_APP, StartupAdTurn(AdPlacement.OPEN_APP).placement)
        assertEquals(AdPlacement.OPEN_APP_INTER, StartupAdTurn(AdPlacement.OPEN_APP_INTER).placement)
    }
}
