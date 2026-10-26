package com.vglstudio.camera.pose.line.guide.feature.loading.data

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Shadows.shadowOf
import org.robolectric.shadows.ShadowNetworkCapabilities

/** How "the system confirmed the Internet" is read from the network state (plan T-057). */
@RunWith(AndroidJUnit4::class)
class InternetCheckTest {
    private fun capabilities(vararg capability: Int): NetworkCapabilities {
        val capabilities = ShadowNetworkCapabilities.newInstance()
        capability.forEach { shadowOf(capabilities).addCapability(it) }
        return capabilities
    }

    @Test
    fun noNetwork_isNotValidated() {
        assertFalse((null as NetworkCapabilities?).isValidatedInternet())
    }

    @Test
    fun internetWithoutValidation_isNotValidated() {
        assertFalse(capabilities(NetworkCapabilities.NET_CAPABILITY_INTERNET).isValidatedInternet())
    }

    @Test
    fun validatedWithoutInternet_isNotValidated() {
        assertFalse(capabilities(NetworkCapabilities.NET_CAPABILITY_VALIDATED).isValidatedInternet())
    }

    @Test
    fun validatedInternet_isValidated() {
        val capabilities = capabilities(
            NetworkCapabilities.NET_CAPABILITY_INTERNET,
            NetworkCapabilities.NET_CAPABILITY_VALIDATED,
        )
        assertTrue(capabilities.isValidatedInternet())
    }

    @Test
    fun androidCheck_readsTheActiveNetwork() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val manager = context.getSystemService(ConnectivityManager::class.java)
        val network = checkNotNull(manager.activeNetwork) { "Robolectric has no active network" }
        val check = AndroidInternetCheck(context)

        shadowOf(manager).setNetworkCapabilities(
            network,
            capabilities(NetworkCapabilities.NET_CAPABILITY_INTERNET, NetworkCapabilities.NET_CAPABILITY_VALIDATED),
        )
        assertTrue(check.isInternetValidated())

        shadowOf(manager).setNetworkCapabilities(network, capabilities(NetworkCapabilities.NET_CAPABILITY_INTERNET))
        assertFalse(check.isInternetValidated())
    }
}
