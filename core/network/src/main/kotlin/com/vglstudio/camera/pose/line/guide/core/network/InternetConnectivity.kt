package com.vglstudio.camera.pose.line.guide.core.network

import javax.inject.Inject
import kotlinx.coroutines.flow.Flow

/**
 * Whether the device has Internet, for other core modules (:core:ads, S-09), read from the same source as the network
 * dialog ([ConnectivitySource]): no second watcher of the system network state.
 */
interface InternetConnectivity {
    /** The current value at once, then every change. */
    val connected: Flow<Boolean>
}

internal class DefaultInternetConnectivity @Inject constructor(source: ConnectivitySource) : InternetConnectivity {
    override val connected: Flow<Boolean> = source.connectivity()
}
