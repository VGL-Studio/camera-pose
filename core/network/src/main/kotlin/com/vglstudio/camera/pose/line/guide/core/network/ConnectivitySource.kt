package com.vglstudio.camera.pose.line.guide.core.network

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow

/** Whether the device has a network that can reach the Internet. */
internal interface ConnectivitySource {
    fun isConnected(): Boolean

    /** Current value at once, then every change. The system callback is registered only while collected. */
    fun connectivity(): Flow<Boolean>
}

internal class AndroidConnectivitySource : ConnectivitySource {
    override fun isConnected(): Boolean = false

    override fun connectivity(): Flow<Boolean> = emptyFlow()
}
