package com.vglstudio.camera.pose.line.guide.core.network

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged

/** Whether the device has a network that can reach the Internet. */
internal interface ConnectivitySource {
    fun isConnected(): Boolean

    /** Current value at once, then every change. The system callback is registered only while collected. */
    fun connectivity(): Flow<Boolean>
}

/**
 * Connected means the default network has [NetworkCapabilities.NET_CAPABILITY_INTERNET]; validation is not required,
 * because some devices validate slowly or never (plan T-011, question 1). No decision is made here.
 */
internal class AndroidConnectivitySource(context: Context) : ConnectivitySource {
    private val manager = context.getSystemService(ConnectivityManager::class.java)

    override fun isConnected(): Boolean = manager.getNetworkCapabilities(manager.activeNetwork).hasInternet()

    override fun connectivity(): Flow<Boolean> = callbackFlow {
        val callback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                trySend(true)
            }

            override fun onCapabilitiesChanged(network: Network, capabilities: NetworkCapabilities) {
                trySend(capabilities.hasInternet())
            }

            override fun onLost(network: Network) {
                trySend(false)
            }
        }
        trySend(isConnected())
        manager.registerDefaultNetworkCallback(callback)
        awaitClose { manager.unregisterNetworkCallback(callback) }
    }.distinctUntilChanged()

    private fun NetworkCapabilities?.hasInternet(): Boolean =
        this?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) == true
}
