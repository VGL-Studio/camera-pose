package com.vglstudio.camera.pose.line.guide.core.ads

import app.cash.turbine.test
import com.vglstudio.camera.pose.line.guide.core.config.RemoteKey
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** The placement shown when the app opens (A-01, A-02) and the switch of every placement (L-072), plan T-024. */
@OptIn(ExperimentalCoroutinesApi::class)
class StartupAdChoiceTest {
    private val settings = FakeRemoteSettings()
    private val flags = DefaultAdsFlags(settings)

    private fun FakeRemoteSettings.set(name: String, value: Boolean) =
        set(RemoteKey.boolean(name, false, "Test key."), value)

    private fun FakeRemoteSettings.set(name: String, value: String) =
        set(RemoteKey.string(name, "", "Test key."), value)

    private suspend fun globalFlags(): AdsGlobalFlags = flags.global.first()

    private fun adFreeStatus(flow: StateFlow<Boolean>): AdFreeStatus = object : AdFreeStatus {
        override val adFree = flow
    }

    /** A first-open mark that is never read. */
    private val neverRead: Flow<Boolean> = flow { awaitCancellation() }

    @Test
    fun firstOpen_readsFirstOpenKeyOnly() = runTest {
        val global = globalFlags().copy(showOpenAdsFirstOpen = true, showOpenAds = false)
        assertEquals(AdPlacement.OPEN_APP, startupAdPlacement(global, firstOpen = true))
        // Control (L-055): the same keys on a later open show nothing.
        assertNull(startupAdPlacement(global, firstOpen = false))
    }

    @Test
    fun laterOpen_readsShowOpenAdsOnly() = runTest {
        val global = globalFlags().copy(showOpenAds = true, showOpenAdsFirstOpen = false)
        assertEquals(AdPlacement.OPEN_APP, startupAdPlacement(global, firstOpen = false))
        assertNull(startupAdPlacement(global, firstOpen = true))
    }

    @Test
    fun masterSwitchOff_isNull() = runTest {
        settings.set("show_open_ads", true)
        settings.set("show_open_ads_first_open", true)
        // Control: with the master switch on, both opens show an ad.
        assertEquals(AdPlacement.OPEN_APP, startupAdPlacement(globalFlags(), firstOpen = true))
        assertEquals(AdPlacement.OPEN_APP, startupAdPlacement(globalFlags(), firstOpen = false))
        settings.set("is_show_ads", false)
        assertNull(startupAdPlacement(globalFlags(), firstOpen = true))
        assertNull(startupAdPlacement(globalFlags(), firstOpen = false))
    }

    @Test
    fun interType_choosesOpenAppInter() = runTest {
        val on = globalFlags().copy(showOpenAds = true, openAdType = OpenAdType.INTERSTITIAL)
        assertEquals(AdPlacement.OPEN_APP_INTER, startupAdPlacement(on, firstOpen = false))
        assertEquals(
            AdPlacement.OPEN_APP,
            startupAdPlacement(on.copy(openAdType = OpenAdType.APP_OPEN), firstOpen = false),
        )
        assertNull(startupAdPlacement(on.copy(showOpenAds = false), firstOpen = false))
    }

    @Test
    fun placementSwitch_followsKeysAndFirstOpen() = runTest {
        settings.set("show_open_ads_first_open", true)
        assertEquals(true, placementSwitch(AdPlacement.OPEN_APP, flags, flowOf(true)).first())
        assertEquals(false, placementSwitch(AdPlacement.OPEN_APP_INTER, flags, flowOf(true)).first())
        settings.set("open_ad_type", "inter")
        assertEquals(false, placementSwitch(AdPlacement.OPEN_APP, flags, flowOf(true)).first())
        assertEquals(true, placementSwitch(AdPlacement.OPEN_APP_INTER, flags, flowOf(true)).first())
    }

    @Test
    fun placementSwitch_keyTurnedOff_emitsFalse() = runTest {
        settings.set("show_open_ads_first_open", true)
        placementSwitch(AdPlacement.OPEN_APP, flags, flowOf(true)).test {
            assertEquals(true, awaitItem())
            settings.set("show_open_ads_first_open", false)
            assertEquals(false, awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun placementSwitch_waitsForFirstOpen() = runTest {
        settings.set("show_open_ads_first_open", true)
        settings.set("show_open_ads", true)
        placementSwitch(AdPlacement.OPEN_APP, flags, neverRead).test {
            advanceTimeBy(1_000)
            expectNoEvents()
            cancelAndIgnoreRemainingEvents()
        }
        // Control: once the mark is read, the switch has a value.
        assertEquals(true, placementSwitch(AdPlacement.OPEN_APP, flags, flowOf(false)).first())
    }

    @Test
    fun placementSwitch_otherPlacements_unchanged() = runTest {
        val placement = AdPlacement.SAMPLE_NATIVE_SMALL
        // Other placements do not wait for the first-open mark, and follow is_show_<code> as before.
        assertEquals(false, placementSwitch(placement, flags, neverRead).first())
        settings.set("is_show_sample_native_small", true)
        assertEquals(true, placementSwitch(placement, flags, neverRead).first())
        settings.set("is_show_ads", false)
        assertEquals(false, placementSwitch(placement, flags, emptyFlow()).first())
        settings.set("is_show_ads", true)
        // Control (L-055): an app-wide placement does not follow a switch of its own name.
        settings.set("is_show_open_app", true)
        assertEquals(false, placementSwitch(AdPlacement.OPEN_APP, flags, flowOf(false)).first())
    }

    @Test
    fun placementSwitch_nativeCombo_followsNativeAfterInterKey() = runTest {
        val combo = AdPlacement.NATIVE_AFTER_INTER
        // Waits for nothing: the combo does not depend on the first-open mark.
        assertEquals(false, placementSwitch(combo, flags, neverRead).first())
        settings.set("is_show_native_after_inter", true)
        assertEquals(true, placementSwitch(combo, flags, neverRead).first())
        settings.set("is_show_ads", false)
        assertEquals(false, placementSwitch(combo, flags, neverRead).first())
        settings.set("is_show_ads", true)
        // Control: OPEN_APP still follows show_open_ads, not the combo key.
        assertEquals(false, placementSwitch(AdPlacement.OPEN_APP, flags, flowOf(false)).first())
        settings.set("show_open_ads", true)
        assertEquals(true, placementSwitch(AdPlacement.OPEN_APP, flags, flowOf(false)).first())
        assertEquals(true, placementSwitch(combo, flags, neverRead).first())
    }

    @Test
    fun resumeAdPlacement_followsResumeKeys() = runTest {
        // Control: off by default.
        assertNull(resumeAdPlacement(globalFlags()))
        settings.set("show_resum_ads", true)
        assertEquals(AdPlacement.RESUME, resumeAdPlacement(globalFlags()))
        settings.set("resume_ad_type", "inter")
        assertEquals(AdPlacement.RESUME_INTER, resumeAdPlacement(globalFlags()))
        // Only the exact value `inter` is an interstitial (A-12).
        settings.set("resume_ad_type", "INTER")
        assertEquals(AdPlacement.RESUME, resumeAdPlacement(globalFlags()))
        settings.set("resume_ad_type", "")
        assertEquals(AdPlacement.RESUME, resumeAdPlacement(globalFlags()))
        settings.set("resume_ad_type", "inter")
        settings.set("is_show_ads", false)
        assertNull(resumeAdPlacement(globalFlags()))
        settings.set("is_show_ads", true)
        settings.set("show_resum_ads", false)
        assertNull(resumeAdPlacement(globalFlags()))
    }

    @Test
    fun placementSwitch_resume_followsResumeKeys() = runTest {
        settings.set("show_resum_ads", true)
        // No wait for the first-open mark.
        assertEquals(true, placementSwitch(AdPlacement.RESUME, flags, neverRead).first())
        assertEquals(false, placementSwitch(AdPlacement.RESUME_INTER, flags, neverRead).first())
        settings.set("resume_ad_type", "inter")
        assertEquals(false, placementSwitch(AdPlacement.RESUME, flags, neverRead).first())
        assertEquals(true, placementSwitch(AdPlacement.RESUME_INTER, flags, neverRead).first())
        // Switched off midway (L-061): the switch emits false.
        placementSwitch(AdPlacement.RESUME_INTER, flags, neverRead).test {
            assertEquals(true, awaitItem())
            settings.set("show_resum_ads", false)
            assertEquals(false, awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
        assertEquals(false, placementSwitch(AdPlacement.RESUME, flags, neverRead).first())
        // Control: OPEN_APP still follows show_open_ads, whatever show_resum_ads is.
        assertEquals(false, placementSwitch(AdPlacement.OPEN_APP, flags, flowOf(false)).first())
        settings.set("show_open_ads", true)
        assertEquals(true, placementSwitch(AdPlacement.OPEN_APP, flags, flowOf(false)).first())
        settings.set("show_resum_ads", true)
        assertEquals(true, placementSwitch(AdPlacement.OPEN_APP, flags, flowOf(false)).first())
    }

    @Test
    fun startupAdPlacement_neverResume() = runTest {
        val base = globalFlags()
        val chosen = mutableListOf<AdPlacement?>()
        val bools = listOf(true, false)
        for (open in bools) {
            for (openFirst in bools) {
                for (resume in bools) {
                    for (firstOpen in bools) {
                        for (openType in OpenAdType.entries) {
                            for (resumeType in OpenAdType.entries) {
                                val flags = base.copy(
                                    showOpenAds = open,
                                    showOpenAdsFirstOpen = openFirst,
                                    showResumeAds = resume,
                                    openAdType = openType,
                                    resumeAdType = resumeType,
                                )
                                chosen += startupAdPlacement(flags, firstOpen)
                            }
                        }
                    }
                }
            }
        }
        assertTrue(chosen.none { it == AdPlacement.RESUME || it == AdPlacement.RESUME_INTER })
        // Control (L-055): the loop did reach the startup placements.
        assertTrue(AdPlacement.OPEN_APP in chosen)
        assertTrue(AdPlacement.OPEN_APP_INTER in chosen)
    }

    @Test
    fun placementSwitch_galleryAppOpen_followsOwnKey() = runTest {
        val gallery = AdPlacement.GALLERY_APP_OPEN
        // Its own key on, the keys of the loading screen off: on, with no wait for the first-open mark.
        settings.set("is_show_gallery_app_open", true)
        assertEquals(true, placementSwitch(gallery, flags, neverRead).first())
        // Its own key off, the keys of the loading screen on: off.
        settings.set("is_show_gallery_app_open", false)
        settings.set("show_open_ads", true)
        settings.set("show_open_ads_first_open", true)
        assertEquals(false, placementSwitch(gallery, flags, neverRead).first())
        // Control (L-055): the same keys switch on the app open of the loading screen.
        assertEquals(true, placementSwitch(AdPlacement.OPEN_APP, flags, flowOf(false)).first())

        // The loading screen never chooses the gallery placement, whatever the keys.
        val base = globalFlags()
        val chosen = mutableListOf<AdPlacement?>()
        val bools = listOf(true, false)
        for (open in bools) {
            for (openFirst in bools) {
                for (firstOpen in bools) {
                    for (openType in OpenAdType.entries) {
                        val global = base.copy(
                            showOpenAds = open,
                            showOpenAdsFirstOpen = openFirst,
                            openAdType = openType,
                        )
                        chosen += startupAdPlacement(global, firstOpen)
                    }
                }
            }
        }
        assertTrue(chosen.none { it == gallery })
        assertTrue(AdPlacement.OPEN_APP in chosen)
    }

    @Test
    fun adFree_isNull() = runTest {
        // KB-30 and KB-13, criterion 5: no app open ad for a user who bought ad removal.
        settings.set("show_open_ads", true)
        settings.set("show_open_ads_first_open", true)
        val adFree = DefaultAdsFlags(settings, adFreeStatus(MutableStateFlow(true))).global.first()
        assertNull(startupAdPlacement(adFree, firstOpen = true))
        assertNull(startupAdPlacement(adFree, firstOpen = false))
        // Control (L-055): the same keys without the signal show the app open ad.
        val notAdFree = DefaultAdsFlags(settings, adFreeStatus(MutableStateFlow(false))).global.first()
        assertEquals(AdPlacement.OPEN_APP, startupAdPlacement(notAdFree, firstOpen = true))
        assertEquals(AdPlacement.OPEN_APP, startupAdPlacement(notAdFree, firstOpen = false))
    }

    @Test
    fun placementSwitch_adFreeMidSession_emitsFalse() = runTest {
        // KB-30, criterion 3, for the app-wide placements (L-061): off at once, back on when the signal goes back.
        settings.set("show_open_ads", true)
        settings.set("show_resum_ads", true)
        settings.set("is_show_native_after_inter", true)
        val adFree = MutableStateFlow(false)
        val adFreeFlags = DefaultAdsFlags(settings, adFreeStatus(adFree))
        listOf(AdPlacement.OPEN_APP, AdPlacement.RESUME, AdPlacement.NATIVE_AFTER_INTER).forEach { placement ->
            placementSwitch(placement, adFreeFlags, flowOf(false)).test {
                assertEquals(placement.key, true, awaitItem())
                adFree.value = true
                assertEquals(placement.key, false, awaitItem())
                adFree.value = false
                assertEquals(placement.key, true, awaitItem())
                cancelAndIgnoreRemainingEvents()
            }
        }
    }
}
