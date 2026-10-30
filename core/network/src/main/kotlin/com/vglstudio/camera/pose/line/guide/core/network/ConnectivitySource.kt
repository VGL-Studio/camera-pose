package com.vglstudio.camera.pose.line.guide.core.network

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged

/** Whether the device has usable Internet, read from the system's network state only (spec network, KB-1). */
internal interface ConnectivitySource {
    /** Usable Internet right now: the default network has INTERNET and VALIDATED (ADR-0024). */
    fun isConnected(): Boolean

    /** Current value at once, then every change. The system callback is registered only while collected. */
    fun connectivity(): Flow<Boolean>
}

/**
 * A network that has not been validated, such as Wi-Fi behind a sign-in page or Wi-Fi without a way out, is no
 * Internet (spec network KB-1, ADR-0024 point 1; plan T-076). The kind of connection does not matter.
 */
internal fun isUsableInternet(hasInternet: Boolean, isValidated: Boolean): Boolean = hasInternet && isValidated

internal fun NetworkCapabilities?.hasUsableInternet(): Boolean = this != null &&
    isUsableInternet(
        hasInternet = hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET),
        isValidated = hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED),
    )

/**
 * Watches the default network through [ConnectivityManager]. Reads only its capabilities: no Wi-Fi name, IP address
 * or network identifier. onAvailable is not used: the system has not validated the network yet at that point, and
 * onCapabilitiesChanged reports it once it has.
 */
internal class AndroidConnectivitySource(context: Context) : ConnectivitySource {
    private val manager = context.getSystemService(ConnectivityManager::class.java)

    override fun isConnected(): Boolean = manager.getNetworkCapabilities(manager.activeNetwork).hasUsableInternet()

    override fun connectivity(): Flow<Boolean> = callbackFlow {
        val callback = object : ConnectivityManager.NetworkCallback() {
            override fun onCapabilitiesChanged(network: Network, capabilities: NetworkCapabilities) {
                trySend(capabilities.hasUsableInternet())
            }

            override fun onLost(network: Network) {
                trySend(false)
            }
        }
        trySend(isConnected())
        manager.registerDefaultNetworkCallback(callback)
        awaitClose { manager.unregisterNetworkCallback(callback) }
    }.distinctUntilChanged()
}
