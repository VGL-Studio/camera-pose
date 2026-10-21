package com.vglstudio.camera.pose.line.guide.core.network

import com.vglstudio.camera.pose.line.guide.core.config.FeatureFlag
import com.vglstudio.camera.pose.line.guide.core.config.FeatureFlags
import com.vglstudio.camera.pose.line.guide.core.config.RemoteSettings
import com.vglstudio.camera.pose.line.guide.core.logging.AppLogger
import com.vglstudio.camera.pose.line.guide.core.network.di.NetworkScope
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Decides when the network dialog shows (spec network). :app sets [setActive] once past the loading screen, draws
 * [state] over every screen and calls [retry] from the dialog. Other modules use [NetworkStatus] and
 * [NetworkFailureReporter], never this class.
 *
 * The dialog shows when the device reports no network for longer than the delay of [RemoteSettings], or at once
 * when a download fails because of the network. It closes only through "Retry", when the flag is turned off, or when
 * the app goes back to the loading screen.
 */
@Singleton
class NetworkDialogController @Inject internal constructor(
    private val connectivity: ConnectivitySource,
    featureFlags: FeatureFlags,
    private val settings: RemoteSettings,
    private val logger: AppLogger,
    @param:NetworkScope private val scope: CoroutineScope,
) : NetworkStatus,
    NetworkFailureReporter {
    private val active = MutableStateFlow(false)

    // Past loading and flag on; failures reported while false are dropped, not kept for later.
    private val armed = MutableStateFlow(false)

    private val _state = MutableStateFlow<NetworkDialogState>(NetworkDialogState.Hidden)

    /** What :app draws over the current screen. */
    val state: StateFlow<NetworkDialogState> = _state.asStateFlow()

    override val dialogShown: StateFlow<Boolean> =
        _state.map { it is NetworkDialogState.Shown }.stateIn(scope, SharingStarted.Eagerly, false)

    private val _recovered = MutableSharedFlow<Unit>(extraBufferCapacity = 1)

    override val recovered: Flow<Unit> = _recovered.asSharedFlow()

    private var retryJob: Job? = null

    init {
        scope.launch {
            combine(active, featureFlags.enabled(FeatureFlag.NETWORK_DIALOG)) { active, enabled -> active to enabled }
                .distinctUntilChanged()
                .collectLatest { (active, enabled) ->
                    armed.value = active && enabled
                    if (!armed.value) {
                        disarm(flagOff = active && !enabled)
                    } else {
                        // Collected only while armed, so the system callback is registered only past loading.
                        connectivity.connectivity().collectLatest { connected -> if (!connected) showAfterDelay() }
                    }
                }
        }
    }

    /** Called by :app: true on every screen after the loading screen, false on the loading screen. */
    fun setActive(active: Boolean) {
        if (this.active.value != active) logger.info(TAG, "Active: $active")
        this.active.value = active
    }

    /**
     * Checks the network at once and every 250 ms for at most 2 seconds. Connected: the dialog closes and [recovered]
     * emits. Still offline: the dialog stays, with a line saying so. Ignored while a check runs.
     */
    fun retry() {
        val current = _state.value
        if (current !is NetworkDialogState.Shown || current.checking) return
        _state.value = NetworkDialogState.Shown(checking = true, stillOffline = false)
        retryJob = scope.launch {
            var waited = 0L
            while (!connectivity.isConnected()) {
                if (waited >= NetworkSettings.RETRY_MAX_MILLIS) {
                    _state.value = NetworkDialogState.Shown(checking = false, stillOffline = true)
                    logger.info(TAG, "Retry: still_offline")
                    return@launch
                }
                delay(NetworkSettings.RETRY_POLL_MILLIS)
                waited += NetworkSettings.RETRY_POLL_MILLIS
            }
            _state.value = NetworkDialogState.Hidden
            logger.info(TAG, "Retry: online")
            _recovered.tryEmit(Unit)
        }
    }

    /** May be called from any thread. Shows the dialog at once, unless it is not armed or already shown. */
    override fun reportNetworkFailure(source: String) {
        if (armed.value) show("load_failed source=$source")
    }

    private suspend fun showAfterDelay() {
        val seconds = settings.long(NetworkSettings.LOST_DELAY_SECONDS, NetworkSettings.LOST_DELAY_SECONDS_DEFAULT)
            .first()
        delay(seconds * MILLIS_PER_SECOND)
        show("device_offline delay=${seconds}s")
    }

    private fun show(reason: String) {
        var shown = false
        _state.update { current ->
            shown = current == NetworkDialogState.Hidden
            if (shown) NetworkDialogState.Shown(checking = false, stillOffline = false) else current
        }
        if (shown) logger.info(TAG, "Dialog shown: $reason")
    }

    private fun disarm(flagOff: Boolean) {
        retryJob?.cancel()
        if (_state.value != NetworkDialogState.Hidden) {
            logger.info(TAG, "Dialog hidden: ${if (flagOff) "flag_off" else "inactive"}")
        }
        _state.value = NetworkDialogState.Hidden
    }

    private companion object {
        const val TAG = "Network"
        const val MILLIS_PER_SECOND = 1_000L
    }
}
