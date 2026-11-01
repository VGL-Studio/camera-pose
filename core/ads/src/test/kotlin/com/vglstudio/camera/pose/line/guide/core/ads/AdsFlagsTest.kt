package com.vglstudio.camera.pose.line.guide.core.ads

import app.cash.turbine.test
import com.vglstudio.camera.pose.line.guide.core.config.RemoteKey
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The ad flags read from Remote Config (plan T-019, KB-7, KB-10, KB-25). Keys are written by name here, as the
 * team writes them on Firebase; [FakeRemoteSettings] matches values by name.
 */
class AdsFlagsTest {
    private val settings = FakeRemoteSettings()
    private val flags = DefaultAdsFlags(settings)

    private val rw = AdPlacementDeclaration(code = "rw_get_item", format = AdFormat.REWARDED)
    private val sounds = AdPlacementDeclaration(
        code = "inter_sounds",
        format = AdFormat.INTERSTITIAL,
        intervalGroup = AdIntervalGroup.named("sounds", 30),
    )
    private val list = AdPlacementDeclaration(code = "list_x", format = AdFormat.NATIVE_IN_COLLECTION, nativeEvery = 6)

    private fun bool(name: String) = RemoteKey.boolean(name, false, "Test key.")

    private fun long(name: String) = RemoteKey.long(name, 0L, "Test key.")

    private fun string(name: String) = RemoteKey.string(name, "", "Test key.")

    private fun FakeRemoteSettings.set(name: String, value: Boolean) = set(bool(name), value)

    private fun FakeRemoteSettings.set(name: String, value: Long) = set(long(name), value)

    private fun FakeRemoteSettings.set(name: String, value: String) = set(string(name), value)

    private fun adFreeStatus(flow: StateFlow<Boolean>): AdFreeStatus = object : AdFreeStatus {
        override val adFree = flow
    }

    @Test
    fun masterSwitchOff_turnsEveryPlacementOff() = runTest {
        // App-wide placements have no is_show_<code> (MC-09, question 3 of plan T-024); their switch is checked by
        // StartupAdChoiceTest. Control: a placement that is not app-wide is still in the loop.
        val placements = AdPlacement.entries.filterNot { it.appWide }
        assertTrue(AdPlacement.entries.any { it.appWide })
        assertTrue(AdPlacement.SAMPLE_NATIVE_SMALL in placements)
        placements.forEach { placement ->
            settings.set("is_show_ads", true)
            settings.set("is_show_${placement.key}", true)
            flags.placementEnabled(placement).test {
                assertEquals(placement.key, true, awaitItem())
                settings.set("is_show_ads", false)
                assertEquals(placement.key, false, awaitItem())
                cancelAndIgnoreRemainingEvents()
            }
        }
    }

    @Test
    fun masterSwitchOff_turnsAppOpenResumeAndComboOff() = runTest {
        listOf("show_open_ads", "show_open_ads_first_open", "show_resum_ads", "is_show_native_after_inter")
            .forEach { settings.set(it, true) }
        flags.global.test {
            val on = awaitItem()
            assertTrue(on.adsEnabled)
            assertTrue(on.showOpenAds)
            assertTrue(on.showOpenAdsFirstOpen)
            assertTrue(on.showResumeAds)
            assertTrue(on.showNativeAfterInter)
            settings.set("is_show_ads", false)
            val off = awaitItem()
            assertFalse(off.adsEnabled)
            assertFalse(off.showOpenAds)
            assertFalse(off.showOpenAdsFirstOpen)
            assertFalse(off.showResumeAds)
            assertFalse(off.showNativeAfterInter)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun masterSwitchBackOn_restoresEnabledPlacements() = runTest {
        val placement = AdPlacement.SAMPLE_NATIVE_SMALL
        settings.set("is_show_${placement.key}", true)
        settings.set("is_show_ads", false)
        flags.placementEnabled(placement).test {
            assertEquals(false, awaitItem())
            settings.set("is_show_ads", true)
            assertEquals(true, awaitItem())
            cancelAndIgnoreRemainingEvents()
        }

        // A placement that is switched off stays off when the master switch comes back on.
        val placementOff = FakeRemoteSettings().apply { set("is_show_ads", false) }
        val offFlags = DefaultAdsFlags(placementOff)
        offFlags.placementEnabled(placement).test {
            assertEquals(false, awaitItem())
            placementOff.set("is_show_ads", true)
            expectNoEvents()
            cancelAndIgnoreRemainingEvents()
        }
        assertEquals(false, offFlags.placementEnabled(placement).first())
    }

    @Test
    fun placementOffByDefault_evenWithMasterSwitchOn() = runTest {
        val placement = AdPlacement.SAMPLE_NATIVE_SMALL
        // Control (L-055): the same placement switched on is on.
        val other = FakeRemoteSettings().apply { set("is_show_${placement.key}", true) }
        assertEquals(true, DefaultAdsFlags(other).placementEnabled(placement).first())
        assertEquals(false, flags.placementEnabled(placement).first())
    }

    @Test
    fun openAdType_isReadFromStringKey() = runTest {
        mapOf(
            "inter" to OpenAdType.INTERSTITIAL,
            "aoa" to OpenAdType.APP_OPEN,
            "INTER" to OpenAdType.APP_OPEN,
            "x" to OpenAdType.APP_OPEN,
            "" to OpenAdType.APP_OPEN,
        ).forEach { (value, expected) ->
            settings.set("open_ad_type", value)
            assertEquals("open_ad_type=$value", expected, flags.global.first().openAdType)
        }
        assertTrue(settings.readNames.toString(), "open_ad_type" in settings.readNames)
    }

    @Test
    fun resumeAdType_onlyExactInterIsInterstitial() = runTest {
        mapOf(
            "inter" to OpenAdType.INTERSTITIAL,
            "aoa" to OpenAdType.APP_OPEN,
            "INTER" to OpenAdType.APP_OPEN,
            "x" to OpenAdType.APP_OPEN,
            "" to OpenAdType.APP_OPEN,
        ).forEach { (value, expected) ->
            settings.set("resume_ad_type", value)
            assertEquals("resume_ad_type=$value", expected, flags.global.first().resumeAdType)
        }
        assertTrue(settings.readNames.toString(), "resume_ad_type" in settings.readNames)
    }

    @Test
    fun global_usesTableDefaultsWhenNothingSet() = runTest {
        assertEquals(
            AdsGlobalFlags(
                adsEnabled = true,
                showOpenAds = false,
                showOpenAdsFirstOpen = false,
                openAdType = OpenAdType.APP_OPEN,
                showResumeAds = false,
                resumeAdType = OpenAdType.APP_OPEN,
                adsIntervalSeconds = 45,
                showNativeAfterInter = false,
                showNativeBottom = false,
                // Off because is_show_native_bottom is off by default (D-01).
                showNativeCollap = false,
                expandCollapAfterSeconds = 10,
                consentShortcutEnabled = false,
                logFullScreenEvents = false,
                logSlotEvents = false,
                closeNativeCountdownSeconds = 5,
            ),
            flags.global.first(),
        )
    }

    @Test
    fun global_followsEveryRemoteValue() = runTest {
        // Every key but the master switch set away from its default (control of the defaults tests, L-055).
        settings.set("show_open_ads", true)
        settings.set("show_open_ads_first_open", true)
        settings.set("open_ad_type", "inter")
        settings.set("show_resum_ads", true)
        settings.set("resume_ad_type", "inter")
        settings.set("ads_interval", 60L)
        settings.set("is_show_native_after_inter", true)
        settings.set("is_show_native_bottom", true)
        settings.set("is_show_native_collap", true)
        settings.set("time_expand_collap_ad", 12L)
        settings.set("is_enable_consent_shortcut", true)
        settings.set("is_log_ad_fullscreen_events", true)
        settings.set("is_log_ad_slot_events", true)
        settings.set("time_close_native", 12L)
        settings.set("ads_mediation_order", "admob_max")
        assertEquals(
            AdsGlobalFlags(
                adsEnabled = true,
                showOpenAds = true,
                showOpenAdsFirstOpen = true,
                openAdType = OpenAdType.INTERSTITIAL,
                showResumeAds = true,
                resumeAdType = OpenAdType.INTERSTITIAL,
                adsIntervalSeconds = 60,
                showNativeAfterInter = true,
                showNativeBottom = true,
                showNativeCollap = true,
                expandCollapAfterSeconds = 12,
                consentShortcutEnabled = true,
                logFullScreenEvents = true,
                logSlotEvents = true,
                closeNativeCountdownSeconds = 12,
                mediationOrder = MediationOrder.ADMOB_MAX,
            ),
            flags.global.first(),
        )
        // is_show_native_collap off by itself turns the collapsible layer off.
        settings.set("is_show_native_collap", false)
        assertFalse(flags.global.first().showNativeCollap)
    }

    @Test
    fun globalMediationOrder_defaultIsMaxAdmob() = runTest {
        assertEquals(MediationOrder.MAX_ADMOB, flags.global.first().mediationOrder)
        // Control (L-055): the key is really read, so the default above is not a constant.
        settings.set("ads_mediation_order", "admob")
        assertEquals(MediationOrder.ADMOB, flags.global.first().mediationOrder)
    }

    @Test
    fun globalMediationOrder_invalidIsDefault() = runTest {
        // Control (L-055): a valid value is read first, so the default below comes from the invalid value.
        settings.set("ads_mediation_order", "max")
        assertEquals(MediationOrder.MAX, flags.global.first().mediationOrder)
        settings.set("ads_mediation_order", "x")
        assertEquals(MediationOrder.MAX_ADMOB, flags.global.first().mediationOrder)
    }

    @Test
    fun globalMediationOrder_followsChange() = runTest {
        flags.global.test {
            assertEquals(MediationOrder.MAX_ADMOB, awaitItem().mediationOrder)
            settings.set("ads_mediation_order", "admob")
            assertEquals(MediationOrder.ADMOB, awaitItem().mediationOrder)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun global_emitsAgainWhenAValueChanges() = runTest {
        flags.global.test {
            assertEquals(45L, awaitItem().adsIntervalSeconds)
            settings.set("ads_interval", 60L)
            assertEquals(60L, awaitItem().adsIntervalSeconds)
            settings.set("ads_interval", 60L)
            expectNoEvents()
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun nativeCollap_requiresNativeBottom() = runTest {
        settings.set("is_show_native_bottom", false)
        settings.set("is_show_native_collap", true)
        assertFalse(flags.global.first().showNativeCollap)
        settings.set("is_show_native_bottom", true)
        settings.set("is_show_native_collap", true)
        assertTrue(flags.global.first().showNativeCollap)
        settings.set("is_show_native_bottom", true)
        settings.set("is_show_native_collap", false)
        assertFalse(flags.global.first().showNativeCollap)
    }

    @Test
    fun timeCloseNative_outsideOneToThirty_readsFive() = runTest {
        assertEquals(5L, flags.global.first().closeNativeCountdownSeconds)
        // Control: a value inside the range is written and read back (L-055).
        settings.set("time_close_native", 12L)
        assertEquals(12L, flags.global.first().closeNativeCountdownSeconds)
        mapOf(1L to 1L, 30L to 30L, 0L to 5L, 31L to 5L, -3L to 5L).forEach { (remote, expected) ->
            settings.set("time_close_native", remote)
            assertEquals("time_close_native=$remote", expected, flags.global.first().closeNativeCountdownSeconds)
        }
        assertTrue(settings.readNames.toString(), "time_close_native" in settings.readNames)
    }

    @Test
    fun negativeSeconds_areReadAsZero() = runTest {
        // Control: the defaults before any value is set.
        assertEquals(45L, flags.global.first().adsIntervalSeconds)
        assertEquals(10L, flags.global.first().expandCollapAfterSeconds)
        assertEquals(45L, flags.intervalSeconds(AdIntervalGroup.DEFAULT).first())
        settings.set("ads_interval", -5L)
        settings.set("time_expand_collap_ad", -1L)
        assertEquals(0L, flags.global.first().adsIntervalSeconds)
        assertEquals(0L, flags.global.first().expandCollapAfterSeconds)
        assertEquals(0L, flags.intervalSeconds(AdIntervalGroup.DEFAULT).first())
    }

    @Test
    fun readsOnlyDeclaredKeys() = runTest {
        flags.global.first()
        // App-wide placements have no is_show_<code> (MC-09, question 3 of plan T-024).
        AdPlacement.entries.filterNot { it.appWide }.forEach { flags.placementEnabled(it).first() }
        flags.intervalSeconds(AdIntervalGroup.DEFAULT).first()
        AdPlacement.entries.forEach { flags.nativeEvery(it).first() }

        val read = settings.readNames
        // Control: the flags read something, so the checks below look at real names; the switch of a placement that
        // is not app-wide is still read.
        assertTrue("Nothing read", read.isNotEmpty())
        assertTrue(read.toString(), "is_show_sample_native_small" in read)
        val declared = AdsSettings.all().map { it.name }.toSet()
        assertTrue("Read undeclared keys: ${read - declared}", declared.containsAll(read))
        val wave2 = setOf(
            "ads_enabled",
            "ads_personalized_regulated_regions_enabled",
            "ads_interstitial_min_interval_seconds",
            "ads_first_session_grace_seconds",
            "ads_interstitial_max_per_session_count",
            "ads_inlist_every_count",
            "ads_native_load_timeout_seconds",
            "ads_sample_native_small_enabled",
            "ads_gallery_native_small_enabled",
            "ads_gallery_native_medium_enabled",
            "ads_gallery_inlist_small_enabled",
            "ads_gallery_inlist_medium_enabled",
            "ads_gallery_native_collapsible_enabled",
            "ads_gallery_interstitial_enabled",
            "ads_gallery_native_full_screen_enabled",
        )
        assertEquals(emptySet<String>(), read intersect wave2)
    }

    @Test
    fun fakePlacement_followsDerivedKeys() = runTest {
        assertEquals(false, flags.placementEnabled(rw).first())
        settings.set("is_show_rw_get_item", true)
        assertEquals(true, flags.placementEnabled(rw).first())

        assertEquals(6L, flags.nativeEvery(list).first())
        settings.set("native_every_list_x", 3L)
        assertEquals(3L, flags.nativeEvery(list).first())
        // A placement that declares no nativeEvery has none.
        assertEquals(null, flags.nativeEvery(rw).first())
    }

    @Test
    fun customGroup_readsItsOwnKey() = runTest {
        assertEquals(30L, flags.intervalSeconds(sounds.intervalGroup).first())
        flags.intervalSeconds(AdIntervalGroup.DEFAULT).test {
            assertEquals(45L, awaitItem())
            settings.set("ads_interval_sounds", 90L)
            assertEquals(90L, flags.intervalSeconds(sounds.intervalGroup).first())
            expectNoEvents()
            cancelAndIgnoreRemainingEvents()
        }
        settings.set("ads_interval", 60L)
        assertEquals(90L, flags.intervalSeconds(sounds.intervalGroup).first())
        assertEquals(60L, flags.intervalSeconds(AdIntervalGroup.DEFAULT).first())
    }

    @Test
    fun adFree_turnsEveryPlacementOff() = runTest {
        // KB-30: the ad-free signal works like is_show_ads off, mid session too (L-061). Control: back off, back on.
        val adFree = MutableStateFlow(false)
        val adFreeFlags = DefaultAdsFlags(settings, adFreeStatus(adFree))
        val placements = AdPlacement.entries.filterNot { it.appWide }
        assertTrue(AdPlacement.SAMPLE_NATIVE_SMALL in placements)
        settings.set("is_show_ads", true)
        placements.forEach { placement ->
            settings.set("is_show_${placement.key}", true)
            adFreeFlags.placementEnabled(placement).test {
                assertEquals(placement.key, true, awaitItem())
                adFree.value = true
                assertEquals(placement.key, false, awaitItem())
                adFree.value = false
                assertEquals(placement.key, true, awaitItem())
                cancelAndIgnoreRemainingEvents()
            }
        }
    }

    @Test
    fun adFree_turnsAppOpenResumeAndComboOff() = runTest {
        val adFree = MutableStateFlow(false)
        val adFreeFlags = DefaultAdsFlags(settings, adFreeStatus(adFree))
        listOf("show_open_ads", "show_open_ads_first_open", "show_resum_ads", "is_show_native_after_inter")
            .forEach { settings.set(it, true) }
        settings.set("is_show_native_bottom", true)
        settings.set("is_enable_consent_shortcut", true)
        adFreeFlags.global.test {
            val on = awaitItem()
            assertTrue(on.adsEnabled)
            assertTrue(on.showOpenAds)
            assertTrue(on.showOpenAdsFirstOpen)
            assertTrue(on.showResumeAds)
            assertTrue(on.showNativeAfterInter)
            adFree.value = true
            val off = awaitItem()
            assertFalse(off.adsEnabled)
            assertFalse(off.showOpenAds)
            assertFalse(off.showOpenAdsFirstOpen)
            assertFalse(off.showResumeAds)
            assertFalse(off.showNativeAfterInter)
            // Keys that turn no ad on by themselves keep their remote value (plan T-063, section 3.1 B).
            assertTrue(off.showNativeBottom)
            assertTrue(off.consentShortcutEnabled)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun adFree_andMasterSwitch_bothMustAllow() = runTest {
        val placement = AdPlacement.SAMPLE_NATIVE_SMALL
        settings.set("is_show_${placement.key}", true)
        mapOf(
            (true to false) to true,
            (true to true) to false,
            (false to false) to false,
            (false to true) to false,
        ).forEach { (inputs, expected) ->
            val (showAds, adFree) = inputs
            settings.set("is_show_ads", showAds)
            val adFreeFlags = DefaultAdsFlags(settings, adFreeStatus(MutableStateFlow(adFree)))
            val label = "is_show_ads=$showAds adFree=$adFree"
            assertEquals(label, expected, adFreeFlags.placementEnabled(placement).first())
            assertEquals(label, expected, adFreeFlags.global.first().adsEnabled)
        }
    }

    @Test
    fun adFreeNone_sameFlagsAsBefore() = runTest {
        // KB-30, criterion 4: an app that wires no signal gets the flags of before.
        assertFalse(AdFreeStatus.NONE.adFree.value)
        val placements = AdPlacement.entries.filterNot { it.appWide }
        listOf("show_open_ads", "show_open_ads_first_open", "show_resum_ads", "is_show_native_after_inter")
            .forEach { settings.set(it, true) }
        placements.forEach { settings.set("is_show_${it.key}", true) }
        listOf(true, false).forEach { showAds ->
            settings.set("is_show_ads", showAds)
            val before = DefaultAdsFlags(settings)
            val none = DefaultAdsFlags(settings, AdFreeStatus.NONE)
            assertEquals("is_show_ads=$showAds", before.global.first(), none.global.first())
            placements.forEach { placement ->
                assertEquals(
                    "${placement.key} is_show_ads=$showAds",
                    before.placementEnabled(placement).first(),
                    none.placementEnabled(placement).first(),
                )
            }
        }
        // Control (L-055): the same keys with the signal on differ from NONE, so the checks above compare real flags.
        settings.set("is_show_ads", true)
        val adFreeOn = DefaultAdsFlags(settings, adFreeStatus(MutableStateFlow(true)))
        assertFalse(adFreeOn.global.first().adsEnabled)
        assertTrue(DefaultAdsFlags(settings, AdFreeStatus.NONE).global.first().adsEnabled)
    }
}
