package com.vglstudio.camera.pose.line.guide.core.ads

import com.google.android.gms.ads.mediation.Adapter
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The mediation adapters of the nine networks ship in :core:ads, so AdMob can serve their ads without code (KB-9,
 * plans T-015 and T-034).
 */
class MediationAdapterClasspathTest {
    @Test
    fun appLovinAdapter_isOnClasspath() {
        // initialize = false: the static initializer is not run, so no Android API is called on the JVM.
        val adapter = Class.forName(
            "com.google.ads.mediation.applovin.AppLovinMediationAdapter",
            false,
            javaClass.classLoader,
        )
        assertTrue(
            "${adapter.name} is not a Google Mobile Ads mediation adapter",
            Adapter::class.java.isAssignableFrom(adapter),
        )
    }

    @Test
    fun networkAdapters_areOnClasspath() {
        val names = listOf(
            "com.google.ads.mediation.facebook.FacebookMediationAdapter",
            "com.google.ads.mediation.fyber.FyberMediationAdapter",
            "com.google.ads.mediation.inmobi.InMobiMediationAdapter",
            "com.google.ads.mediation.ironsource.IronSourceMediationAdapter",
            "com.google.ads.mediation.mintegral.MintegralMediationAdapter",
            "com.google.ads.mediation.moloco.MolocoMediationAdapter",
            "com.google.ads.mediation.pangle.PangleMediationAdapter",
            "com.google.ads.mediation.unity.UnityMediationAdapter",
        )
        for (name in names) {
            // The ClassNotFoundException message names the missing class.
            val adapter = Class.forName(name, false, javaClass.classLoader)
            assertTrue(
                "$name is not a Google Mobile Ads mediation adapter",
                Adapter::class.java.isAssignableFrom(adapter),
            )
        }
    }

    @Test
    fun maxSdkAndGoogleAdapter_areOnClasspath() {
        // AppLovin MAX and its Google adapter, declared by core/ads on their own lines (plan T-068, ADR-0037).
        Class.forName("com.applovin.sdk.AppLovinSdk", false, javaClass.classLoader)
        val adapter = Class.forName(
            "com.applovin.mediation.adapters.GoogleMediationAdapter",
            false,
            javaClass.classLoader,
        )
        val maxAdapter = Class.forName("com.applovin.mediation.adapter.MaxAdapter", false, javaClass.classLoader)
        assertTrue("${adapter.name} is not a MAX adapter", maxAdapter.isAssignableFrom(adapter))
    }

    @Test
    fun unityAdsSdk_isOnClasspath() {
        // The Unity adapter's POM does not bring the Unity Ads SDK; core/ads declares it on its own line.
        Class.forName("com.unity3d.ads.UnityAds", false, javaClass.classLoader)
    }
}
