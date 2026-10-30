package com.vglstudio.camera.pose.line.guide.core.network

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.NetworkCapabilities.NET_CAPABILITY_INTERNET
import android.net.NetworkCapabilities.NET_CAPABILITY_VALIDATED
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.cash.turbine.test
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Shadows.shadowOf
import org.robolectric.shadows.ShadowNetworkCapabilities

/** "Internet" means the default network has INTERNET and VALIDATED (spec network KB-1, ADR-0024; plan T-076). */
@RunWith(AndroidJUnit4::class)
class AndroidConnectivitySourceTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val manager = context.getSystemService(ConnectivityManager::class.java)
    private val network = checkNotNull(manager.activeNetwork) { "Robolectric has no active network" }

    private fun capabilities(vararg capability: Int): NetworkCapabilities {
        val capabilities = ShadowNetworkCapabilities.newInstance()
        shadowOf(capabilities).addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
        capability.forEach { shadowOf(capabilities).addCapability(it) }
        return capabilities
    }

    private fun setActive(capabilities: NetworkCapabilities) {
        shadowOf(manager).setNetworkCapabilities(network, capabilities)
    }

    @Test
    fun hasUsableInternet_noCapabilities_isFalse() {
        assertFalse((null as NetworkCapabilities?).hasUsableInternet())
    }

    @Test
    fun isConnected_needsInternetAndValidated() {
        val source = AndroidConnectivitySource(context)

        setActive(capabilities(NET_CAPABILITY_INTERNET))
        assertFalse(source.isConnected())

        setActive(capabilities(NET_CAPABILITY_VALIDATED))
        assertFalse(source.isConnected())

        setActive(capabilities(NET_CAPABILITY_INTERNET, NET_CAPABILITY_VALIDATED))
        assertTrue(source.isConnected())
    }

    @Test
    fun connectivity_waitsForValidation_thenReportsLoss_andUnregisters() = runTest {
        val internet = capabilities(NET_CAPABILITY_INTERNET)
        val validated = capabilities(NET_CAPABILITY_INTERNET, NET_CAPABILITY_VALIDATED)
        setActive(internet)

        AndroidConnectivitySource(context).connectivity().test {
            assertFalse(awaitItem())
            val callback = shadowOf(manager).networkCallbacks.single()

            callback.onAvailable(network)
            callback.onCapabilitiesChanged(network, internet)
            expectNoEvents()

            callback.onCapabilitiesChanged(network, validated)
            assertTrue(awaitItem())
            callback.onCapabilitiesChanged(network, internet)
            assertFalse(awaitItem())
            callback.onCapabilitiesChanged(network, validated)
            assertTrue(awaitItem())
            callback.onLost(network)
            assertFalse(awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
        assertTrue(shadowOf(manager).networkCallbacks.isEmpty())
    }

    @Test
    fun internetConnectivity_followsValidation() = runTest {
        val connectivity = DefaultInternetConnectivity(AndroidConnectivitySource(context))

        setActive(capabilities(NET_CAPABILITY_INTERNET))
        assertFalse(connectivity.connected.first())

        setActive(capabilities(NET_CAPABILITY_INTERNET, NET_CAPABILITY_VALIDATED))
        assertTrue(connectivity.connected.first())
    }
}
