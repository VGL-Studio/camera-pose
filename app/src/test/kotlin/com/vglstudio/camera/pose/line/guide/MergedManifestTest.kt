package com.vglstudio.camera.pose.line.guide

import java.io.File
import javax.xml.parsers.DocumentBuilderFactory
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.w3c.dom.Document
import org.w3c.dom.Element

/**
 * The mediation SDKs bring start-up providers, receivers and permissions into the app.
 * core/ads/src/main/AndroidManifest.xml removes the AppLovin provider and two AppLovin permissions and keeps VIBRATE
 * (plan T-015), and removes the Meta provider and the exported Mintegral receiver (plan T-034): no network code runs
 * before consent (ADR-0004, ADR-0026). The permission list is fixed: a new permission from an SDK upgrade fails the
 * test, so a plan decides it.
 * This reads the main merged manifest of stagingDebug: the unit test manifest (`android_merged_manifest`) does not
 * apply `tools:node="remove"`. The working directory of a unit test is the module directory.
 * This app has no AppsFlyer (spec ads, question 3; plan T-074): nothing of it is in the manifest, and the only
 * permission the mediation SDKs add to the app is VIBRATE (spec ads, section 6).
 */
class MergedManifestTest {
    private val manifest: Document by lazy {
        val file = File(MERGED_MANIFEST)
        assertTrue("Missing ${file.absolutePath}; run :app:processStagingDebugMainManifest", file.isFile)
        DocumentBuilderFactory.newInstance()
            .apply { isNamespaceAware = true }
            .newDocumentBuilder()
            .parse(file)
    }

    private fun names(tag: String): List<String> {
        val nodes = manifest.getElementsByTagName(tag)
        return (0 until nodes.length).map { (nodes.item(it) as Element).getAttributeNS(ANDROID_NS, "name") }
    }

    @Test
    fun mergedManifest_hasNoAdGalleryActivity() {
        val activities = names("activity")
        // Control: the activities of the merged manifest are read.
        assertTrue(activities.toString(), "com.vglstudio.camera.pose.line.guide.MainActivity" in activities)
        assertEquals(emptyList<String>(), activities.filter { it.endsWith(".AdGalleryActivity") })
    }

    @Test
    fun writeStorage_hasMaxSdk28() {
        val permissions = manifest.getElementsByTagName("uses-permission")
        val writeStorage = (0 until permissions.length)
            .map { permissions.item(it) as Element }
            .single { it.getAttributeNS(ANDROID_NS, "name") == "android.permission.WRITE_EXTERNAL_STORAGE" }
        assertEquals("28", writeStorage.getAttributeNS(ANDROID_NS, "maxSdkVersion"))
    }

    @Test
    fun mergedManifest_containsAppLovinActivity() {
        assertTrue(
            "No com.applovin activity: the AppLovin SDK is not in the build, or this is the wrong file",
            names("activity").any { it.startsWith("com.applovin.") },
        )
    }

    @Test
    fun mergedManifest_hasNoAppLovinInitProvider() {
        assertFalse(
            "AppLovinInitProvider runs before consent",
            "com.applovin.sdk.AppLovinInitProvider" in names("provider"),
        )
    }

    @Test
    fun mergedManifest_hasNoCheckLicensePermission() {
        assertFalse("com.android.vending.CHECK_LICENSE" in names("uses-permission"))
    }

    @Test
    fun mergedManifest_hasNoWifiStatePermission() {
        assertFalse("android.permission.ACCESS_WIFI_STATE" in names("uses-permission"))
    }

    @Test
    fun mergedManifest_keepsVibratePermission() {
        // The AppLovin SDK vibrates without checking the permission or catching the error.
        assertTrue("android.permission.VIBRATE" in names("uses-permission"))
    }

    @Test
    fun mergedManifest_containsActivityOfEveryNetwork() {
        val activities = names("activity")
        val prefixes = listOf(
            "com.facebook.ads.",
            "com.fyber.inneractive.",
            "com.inmobi.ads.",
            "com.ironsource.",
            "com.mbridge.msdk.",
            "com.moloco.sdk.",
            "com.bytedance.sdk.openadsdk.",
            "com.unity3d.ads.",
        )
        for (prefix in prefixes) {
            assertTrue(
                "No $prefix activity: that network's SDK is not in the build",
                activities.any { it.startsWith(prefix) },
            )
        }
    }

    @Test
    fun mergedManifest_permissionsAreExactlyTheAllowedSet() {
        val expected = listOf(
            "android.permission.ACCESS_ADSERVICES_AD_ID",
            "android.permission.ACCESS_ADSERVICES_ATTRIBUTION",
            "android.permission.ACCESS_ADSERVICES_TOPICS",
            "android.permission.ACCESS_NETWORK_STATE",
            "android.permission.CAMERA",
            "android.permission.FOREGROUND_SERVICE",
            "android.permission.INTERNET",
            "android.permission.VIBRATE",
            "android.permission.WAKE_LOCK",
            "android.permission.WRITE_EXTERNAL_STORAGE",
            "com.google.android.finsky.permission.BIND_GET_INSTALL_REFERRER_SERVICE",
            "com.google.android.gms.permission.AD_ID",
            "com.vglstudio.camera.pose.line.guide.staging.DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION",
        )
        assertEquals(expected, names("uses-permission").sorted())
    }

    @Test
    fun mergedManifest_hasNoMetaAudienceNetworkProvider() {
        assertFalse(
            "AudienceNetworkContentProvider starts the Meta SDK before consent",
            "com.facebook.ads.AudienceNetworkContentProvider" in names("provider"),
        )
    }

    @Test
    fun mergedManifest_hasNoMintegralNetworkReceiver() {
        assertFalse(
            "NetWorkChangeReceiver is exported and never gets the system broadcast it filters",
            "com.mbridge.msdk.foundation.same.broadcast.NetWorkChangeReceiver" in names("receiver"),
        )
    }

    @Test
    fun mergedManifest_hasNoAppLovinSdkKey() {
        // The adapter reads the SDK key from the AdMob mediation mapping only; the app carries no key (plan T-034).
        assertFalse(
            "applovin.sdk.key must not be in the app",
            "applovin.sdk.key" in names("meta-data"),
        )
    }

    @Test
    fun mergedManifest_hasNoAppsFlyerStorePermissions() {
        // AppsFlyer reads the referrer of the preinstalled Samsung and Huawei stores; this app has no AppsFlyer, and
        // core/analytics/src/main/AndroidManifest.xml still removes them (plan T-074).
        val permissions = names("uses-permission")
        assertFalse("com.samsung.android.mapsagent.permission.READ_APP_INFO" in permissions)
        assertFalse("com.huawei.appmarket.service.commondata.permission.GET_COMMON_DATA" in permissions)
    }

    @Test
    fun mergedManifest_hasNoAppsFlyerStartupComponent() {
        // Control: the merged manifest has providers, so an empty match means something.
        assertTrue(names("provider").isNotEmpty())
        val components = names("provider") + names("receiver") + names("service")
        assertEquals(emptyList<String>(), components.filter { it.startsWith("com.appsflyer.") })
    }

    @Test
    fun mergedManifest_queriesAreExactlyTheAllowedSet() {
        val queries = manifest.getElementsByTagName("queries")
        val packages = mutableSetOf<String>()
        val actions = mutableSetOf<String>()
        for (i in 0 until queries.length) {
            val query = queries.item(i) as Element
            val packageNodes = query.getElementsByTagName("package")
            for (j in 0 until packageNodes.length) {
                packages += (packageNodes.item(j) as Element).getAttributeNS(ANDROID_NS, "name")
            }
            val actionNodes = query.getElementsByTagName("action")
            for (j in 0 until actionNodes.length) {
                actions += (actionNodes.item(j) as Element).getAttributeNS(ANDROID_NS, "name")
            }
        }
        val expectedPackages = setOf(
            "com.facebook.katana",
            "com.android.vending",
            "com.google.market",
            "com.amazon.venezia",
            "com.sec.android.app.samsungapps",
            "com.xiaomi.market",
            "com.huawei.appmarket",
            "com.oppo.market",
            "com.bbk.appstore",
            "com.topjohnwu.magisk",
            "eu.chainfire.supersu",
            "com.noshufou.android.su",
            "com.koushikdutta.superuser",
            "com.thirdparty.superuser",
        )
        // No com.instagram.android nor com.facebook.lite: those come with AppsFlyer, not in this app (plan T-074).
        assertEquals(14, expectedPackages.size)
        assertEquals(expectedPackages, packages)
        // Measured in the template before AppsFlyer (plan T-033, 2026-10-26); no AppsFlyer referrer provider here.
        val expectedActions = setOf(
            "android.intent.action.VIEW",
            "android.intent.action.INSERT",
            "android.intent.action.DIAL",
            "android.intent.action.MAIN",
            "android.support.customtabs.action.CustomTabsService",
            "androidx.browser.customtabs.CustomTabsService",
            "com.attribution.REFERRAL_PROVIDER",
            "com.digitalturbine.ignite.cl.IgniteRemoteService",
        )
        assertEquals(expectedActions, actions)
    }

    @Test
    fun mergedManifest_keepsBackupOff() {
        val application = manifest.getElementsByTagName("application").item(0) as Element
        assertEquals("false", application.getAttributeNS(ANDROID_NS, "allowBackup"))
    }

    private companion object {
        const val ANDROID_NS = "http://schemas.android.com/apk/res/android"
        const val MERGED_MANIFEST =
            "build/intermediates/merged_manifest/stagingDebug/processStagingDebugMainManifest/AndroidManifest.xml"
    }
}
