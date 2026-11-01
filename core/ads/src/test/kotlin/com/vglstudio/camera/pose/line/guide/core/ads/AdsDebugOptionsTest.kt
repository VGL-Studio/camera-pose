package com.vglstudio.camera.pose.line.guide.core.ads

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.android.gms.ads.RequestConfiguration
import com.vglstudio.camera.pose.line.guide.core.logging.LogEntry
import com.vglstudio.camera.pose.line.guide.core.logging.LogLevel
import com.vglstudio.camera.pose.line.guide.core.logging.RecordingAppLogger
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/**
 * The developer's test devices of the staging build (plan T-058, KB-28): the ids read from local.properties, set on
 * the request configuration of the SDK once, logged by count only, never set without ids.
 */
@RunWith(AndroidJUnit4::class)
class AdsDebugOptionsTest {
    @Test
    fun testDeviceIdsOf_splitsTrimsDropsBlankAndDuplicates() {
        // Control (L-055): an empty value gives no id, a single id is read back as it was written.
        assertEquals(emptyList<String>(), testDeviceIdsOf(""))
        assertEquals(listOf("A"), testDeviceIdsOf("A"))

        val ids = testDeviceIdsOf(" $DEVICE , ,$OTHER_DEVICE,$DEVICE,")

        assertEquals(listOf(DEVICE, OTHER_DEVICE), ids)
    }

    @Test
    fun staging_keepsIdsAndLogsNetworks() {
        val staging = AdsDebugOptions.staging("A,B")
        assertEquals(listOf("A", "B"), staging.testDeviceIds)
        assertTrue(staging.logNetworkResponses)

        val noDevice = AdsDebugOptions.staging("")
        assertEquals(emptyList<String>(), noDevice.testDeviceIds)
        assertTrue(noDevice.logNetworkResponses)

        // Control: production has no device and logs no network.
        assertEquals(emptyList<String>(), AdsDebugOptions.NONE.testDeviceIds)
        assertFalse(AdsDebugOptions.NONE.logNetworkResponses)
    }

    @Test
    fun requestConfiguration_hasTestDevices_keepsOtherFields() {
        val base = RequestConfiguration.Builder()
            .setMaxAdContentRating(RequestConfiguration.MAX_AD_CONTENT_RATING_G)
            .build()

        val configuration = requestConfigurationWithTestDevices(base, listOf("A", "B"))

        assertEquals(listOf("A", "B"), configuration?.testDeviceIds)
        assertEquals(RequestConfiguration.MAX_AD_CONTENT_RATING_G, configuration?.maxAdContentRating)
        assertNull(requestConfigurationWithTestDevices(base, emptyList()))
    }

    @Test
    fun applyTestDevices_setsOnce_logsCountOnly() {
        val logger = RecordingAppLogger()
        val set = mutableListOf<RequestConfiguration>()

        applyTestDevices(
            options = AdsDebugOptions.staging(DEVICE),
            current = { RequestConfiguration.Builder().build() },
            set = { set += it },
            logger = logger,
        )

        assertEquals(listOf(DEVICE), set.single().testDeviceIds)
        assertEquals(listOf(LogEntry(LogLevel.INFO, "Ads", "Test devices set: 1")), logger.entries)
        assertTrue(logger.entries.toString(), logger.entries.none { "33BE2250" in it.message })
    }

    @Test
    fun applyTestDevices_none_callsNothing() {
        listOf(AdsDebugOptions.NONE, AdsDebugOptions.staging("")).forEach { options ->
            val logger = RecordingAppLogger()
            var reads = 0
            var sets = 0

            applyTestDevices(
                options = options,
                current = {
                    reads++
                    RequestConfiguration.Builder().build()
                },
                set = { sets++ },
                logger = logger,
            )

            assertEquals("$options", 0, reads)
            assertEquals("$options", 0, sets)
            assertEquals("$options", emptyList<LogEntry>(), logger.entries)
        }

        // Control (L-055): the same lambdas are called once when there is a device.
        var reads = 0
        var sets = 0
        applyTestDevices(
            options = AdsDebugOptions.staging("A"),
            current = {
                reads++
                RequestConfiguration.Builder().build()
            },
            set = { sets++ },
            logger = RecordingAppLogger(),
        )
        assertEquals(1, reads)
        assertEquals(1, sets)
    }

    private companion object {
        /** The example id of Google's documentation, not a real device. */
        const val DEVICE = "33BE2250B43518CCDA7DE426D04EE231"
        const val OTHER_DEVICE = "ABCDEF0123456789ABCDEF0123456789"
    }
}
