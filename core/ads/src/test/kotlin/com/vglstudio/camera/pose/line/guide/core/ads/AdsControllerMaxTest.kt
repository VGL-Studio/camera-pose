package com.vglstudio.camera.pose.line.guide.core.ads

import android.app.Application
import android.os.Looper
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.vglstudio.camera.pose.line.guide.core.logging.RecordingAppLogger
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Shadows.shadowOf

/**
 * AppLovin MAX tied to the real [AdsController] (plan T-068, KB-31, S-19): the order with the SDK key applied, and no
 * start of MAX before the consent gate opens. No SDK is called: [FakeMaxPlatform] stands for MAX.
 */
@RunWith(AndroidJUnit4::class)
class AdsControllerMaxTest {
    private val application = ApplicationProvider.getApplicationContext<Application>()
    private val settings = FakeRemoteSettings()
    private val platform = FakeMaxPlatform()

    private val logger = RecordingAppLogger()

    private fun controller(config: MaxConfig) = testAdsController(
        context = application,
        flags = DefaultAdsFlags(settings),
        maxConfig = config,
        maxPlatform = platform,
        logger = logger,
    )

    private fun idle() = shadowOf(Looper.getMainLooper()).idle()

    private fun setOrder(value: String) {
        settings.set(AdsSettings.ADS_MEDIATION_ORDER, value)
        idle()
    }

    @Test
    fun mediationOrder_isAdmobWithoutKey() {
        assertEquals(MediationOrder.ADMOB, controller(MaxConfig.NONE).mediationOrder.value)
        // Control: with a key, the default of the remote key applies.
        assertEquals(MediationOrder.MAX_ADMOB, controller(MaxConfig(FAKE_KEY)).mediationOrder.value)
    }

    @Test
    fun mediationOrder_followsRemoteKey() {
        val controller = controller(MaxConfig(FAKE_KEY))
        controller.start(application, AppActivityFilter { true })
        idle()
        setOrder("admob")
        assertEquals(MediationOrder.ADMOB, controller.mediationOrder.value)
    }

    @Test
    fun max_notStartedWithoutConsentGate() {
        val controller = controller(MaxConfig(FAKE_KEY))
        controller.start(application, AppActivityFilter { true })
        idle()
        listOf("admob", "max_admob", "admob_max", "max").forEach(::setOrder)
        assertEquals(0, platform.calls)
        // Control (L-055): every change reached the controller, so MAX was asked and held back by the gate.
        assertEquals(MediationOrder.MAX, controller.mediationOrder.value)
    }

    @Test
    fun gateReopened_startsMaxIfNotStarted() {
        // Plan T-068 versions 3 and 4, S-14: consent given again starts MAX if it has not started.
        val controller = controller(MaxConfig(FAKE_KEY))
        controller.start(application, AppActivityFilter { true })
        idle()
        controller.openConsentGateForTest()
        setOrder("admob")
        controller.admobReadyForTest()
        idle()
        assertEquals(0, platform.calls)
        controller.closeConsentGateForTest()
        idle()
        setOrder("max_admob")
        assertEquals(MediationOrder.MAX_ADMOB, controller.mediationOrder.value)
        assertEquals(0, platform.calls)
        controller.reopenConsentGateForTest()
        idle()
        assertEquals(1, platform.calls)
    }

    @Test
    fun gateClosedWhileWaitingForAdmob_maxNotStarted() {
        // Plan T-068 version 4, KB-31: the gate closed while startSdk waited for AdMob to be ready (S-21).
        val controller = controller(MaxConfig(FAKE_KEY))
        controller.start(application, AppActivityFilter { true })
        idle()
        controller.openConsentGateForTest()
        setOrder("max_admob")
        controller.closeConsentGateForTest()
        idle()
        controller.admobReadyForTest()
        idle()
        assertEquals(0, platform.calls)
        // Control (L-055): MAX was held back by the closed gate only; the gate open again starts it.
        controller.reopenConsentGateForTest()
        idle()
        assertEquals(1, platform.calls)
    }

    @Test
    fun maxUnitId_readsConfig() {
        // Plan T-071: the MAX ad unit of each banner slot comes from the build (MaxConfig.adUnits).
        val controller = controller(MaxConfig(FAKE_KEY, "banner=u1;banner_mrec=m2"))
        assertEquals("u1", controller.maxUnitId(AdFormat.BANNER))
        assertEquals("m2", controller.maxUnitId(AdFormat.BANNER_MREC))
        assertNull(controller.maxUnitId(AdFormat.INTERSTITIAL))
        // Control: without an SDK key, no MAX unit.
        assertNull(controller(MaxConfig("", "banner=u1")).maxUnitId(AdFormat.BANNER))
    }

    @Test
    fun start_keyWithoutBannerUnits_logsMissingOnce_noUnitInLog() {
        val controller = controller(MaxConfig(FAKE_KEY, "banner=unitsecret1"))
        controller.start(application, AppActivityFilter { true })
        controller.start(application, AppActivityFilter { true })
        idle()
        val missing = logger.entries.filter { it.message.startsWith("MAX ad units missing") }
        assertEquals(
            listOf("MAX ad units missing for banner_mrec; those slots run without MAX"),
            missing.map {
                it.message
            },
        )
        assertTrue(logger.entries.none { "unitsecret1" in it.message || FAKE_KEY in it.message })

        // Both missing: both slots named.
        val noUnits = RecordingAppLogger()
        testAdsController(context = application, maxConfig = MaxConfig(FAKE_KEY), logger = noUnits)
            .start(application, AppActivityFilter { true })
        idle()
        assertTrue(
            noUnits.entries.map { it.message }.toString(),
            noUnits.entries.any {
                it.message ==
                    "MAX ad units missing for banner, banner_mrec; those slots run without MAX"
            },
        )

        // Control: without an SDK key, no such line.
        val noKey = RecordingAppLogger()
        testAdsController(context = application, maxConfig = MaxConfig.NONE, logger = noKey)
            .start(application, AppActivityFilter { true })
        idle()
        assertTrue(noKey.entries.isNotEmpty())
        assertTrue(noKey.entries.none { it.message.startsWith("MAX ad units missing") })
    }

    private companion object {
        val FAKE_KEY = "FAKE" + "k".repeat(82)
    }
}
