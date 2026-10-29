package com.vglstudio.camera.pose.line.guide.core.ads

import com.vglstudio.camera.pose.line.guide.core.config.RemoteSettings
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The frequency rules of T-009 (grace period of the first session, limit per session, interval kept on the device)
 * are gone (plan T-019, question 10 of the ads spec), and :core:ads reads Remote Config only through [AdsFlags].
 * Checked by class name, not by scanning source text (L-062).
 */
class RemovedPacingRulesTest {
    private val pkg = "com.vglstudio.camera.pose.line.guide.core.ads"

    @Test
    fun t009PacingClasses_areGone() {
        // Control: a class of the same package that stays is found, so the names below are looked up correctly.
        Class.forName("$pkg.FullScreenAdGuard")
        listOf(
            "FullScreenAdPacer",
            "FullScreenAdSession",
            "FullScreenAdHistoryStore",
            "DataStoreFullScreenAdHistoryStore",
            "FullScreenAdLimits",
            "FullScreenAdPolicyKt",
            "AdsRemoteRulesKt",
            "AdClock",
        ).forEach { name ->
            assertThrows("$name must be removed", ClassNotFoundException::class.java) { Class.forName("$pkg.$name") }
        }
    }

    @Test
    fun adsController_readsRemoteConfigOnlyThroughAdsFlags() {
        val constructors = AdsController::class.java.declaredConstructors
        assertTrue("No constructor found", constructors.isNotEmpty())
        constructors.forEach { constructor ->
            val types = constructor.parameterTypes.toList()
            assertTrue("$constructor takes RemoteSettings", types.none { it == RemoteSettings::class.java })
            assertTrue("$constructor takes a history store", types.none { it.simpleName.contains("HistoryStore") })
            assertTrue("$constructor does not take AdsFlags", types.any { it == AdsFlags::class.java })
        }
    }
}
