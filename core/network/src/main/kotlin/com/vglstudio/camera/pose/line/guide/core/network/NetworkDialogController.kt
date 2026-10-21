package com.vglstudio.camera.pose.line.guide.core.network

import com.vglstudio.camera.pose.line.guide.core.config.FeatureFlags
import com.vglstudio.camera.pose.line.guide.core.config.RemoteSettings
import com.vglstudio.camera.pose.line.guide.core.logging.AppLogger
import com.vglstudio.camera.pose.line.guide.core.network.di.NetworkScope
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.emptyFlow

/** Decides when the network dialog shows (spec network). */
@Singleton
class NetworkDialogController @Inject internal constructor(
    private val connectivity: ConnectivitySource,
    private val featureFlags: FeatureFlags,
    private val settings: RemoteSettings,
    private val logger: AppLogger,
    @param:NetworkScope private val scope: CoroutineScope,
) : NetworkStatus,
    NetworkFailureReporter {
    private val _state = MutableStateFlow<NetworkDialogState>(NetworkDialogState.Hidden)

    val state: StateFlow<NetworkDialogState> = _state.asStateFlow()

    override val dialogShown: StateFlow<Boolean> = MutableStateFlow(false)

    override val recovered: Flow<Unit> = emptyFlow()

    fun setActive(active: Boolean) = Unit

    fun retry() = Unit

    override fun reportNetworkFailure(source: String) = Unit
}
