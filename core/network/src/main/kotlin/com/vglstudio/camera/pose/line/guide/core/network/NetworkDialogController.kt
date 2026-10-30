package com.vglstudio.camera.pose.line.guide.core.network

import com.vglstudio.camera.pose.line.guide.core.config.FeatureFlag
import com.vglstudio.camera.pose.line.guide.core.config.FeatureFlags
import com.vglstudio.camera.pose.line.guide.core.config.RemoteSettings
import com.vglstudio.camera.pose.line.guide.core.logging.AppLogger
import com.vglstudio.camera.pose.line.guide.core.network.di.NetworkScope
import java.util.concurrent.atomic.AtomicReference
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
 *
 * It also shows when the app comes back from the background without network, after a check of at most
 * [NetworkSettings.FOREGROUND_GRACE_MILLIS] ([onAppStarted]); the first start and a configuration change are not a
 * return.
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

    // True while a full-screen ad is on screen ([setFullScreenAdShowing]); the dialog then waits for it.
    private val adShowing = MutableStateFlow(false)

    // The reason of a show that waits for the full-screen ad to close; null when none. Any thread.
    private val pendingShow = AtomicReference<String?>(null)

    private val _state = MutableStateFlow<NetworkDialogState>(NetworkDialogState.Hidden)

    /** What :app draws over the current screen. */
    val state: StateFlow<NetworkDialogState> = _state.asStateFlow()

    override val dialogShown: StateFlow<Boolean> =
        _state.map { it is NetworkDialogState.Shown }.stateIn(scope, SharingStarted.Eagerly, false)

    private val _recovered = MutableSharedFlow<Unit>(extraBufferCapacity = 1)

    override val recovered: Flow<Unit> = _recovered.asSharedFlow()

    @Volatile
    private var retryJob: Job? = null

    // True once the app stopped other than for a configuration change, since the last start (plan T-077).
    @Volatile
    private var leftApp = false

    @Volatile
    private var foregroundJob: Job? = null

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
     * Called by :app while a full-screen ad is on screen (plan T-074, pass 2; T-017 of the template): the dialog waits
     * for it and shows when it closes. An ad that starts while the dialog shows hides it and stops a running check;
     * the dialog shows again when the ad closes. Main thread.
     */
    fun setFullScreenAdShowing(showing: Boolean) {
        if (adShowing.value == showing) return
        adShowing.value = showing
        if (showing) {
            var hidden = false
            _state.update { current ->
                hidden = current is NetworkDialogState.Shown
                if (hidden) NetworkDialogState.Hidden else current
            }
            if (hidden) {
                retryJob?.cancel()
                pendingShow.set(AFTER_AD)
                logger.info(TAG, "Dialog hidden: full_screen_ad")
            }
        } else {
            pendingShow.getAndSet(null)?.let(::showNow)
        }
    }

    /**
     * Checks the network at once and every 250 ms for at most 2 seconds. Connected: the dialog closes and [recovered]
     * emits. Still offline: the dialog stays, with a line saying so. Ignored while a check runs.
     */
    fun retry() {
        val current = _state.value
        if (current !is NetworkDialogState.Shown || current.checking) return
        // Compare-and-set: disarm() runs on another thread and may hide the dialog meanwhile; never reopen it.
        if (!_state.compareAndSet(current, CHECKING)) return
        retryJob = scope.launch {
            var waited = 0L
            while (!connectivity.isConnected()) {
                if (waited >= NetworkSettings.RETRY_MAX_MILLIS) {
                    if (_state.compareAndSet(CHECKING, STILL_OFFLINE)) {
                        logger.info(TAG, "Retry: still_offline")
                    }
                    return@launch
                }
                delay(NetworkSettings.RETRY_POLL_MILLIS)
                waited += NetworkSettings.RETRY_POLL_MILLIS
            }
            if (_state.compareAndSet(CHECKING, NetworkDialogState.Hidden)) {
                logger.info(TAG, "Retry: online")
                _recovered.tryEmit(Unit)
            }
        }
    }

    /**
     * Called by :app on each start of the activity. Main thread. Back from the background past the loading screen
     * with the flag on: the network is checked at once and every 250 ms for at most
     * [NetworkSettings.FOREGROUND_GRACE_MILLIS]; still offline, the dialog shows (spec network, KB-3).
     */
    fun onAppStarted() {
        if (!leftApp) return
        leftApp = false
        if (!armed.value) return
        foregroundJob?.cancel()
        foregroundJob = scope.launch { checkOnForeground() }
    }

    /** Skeleton for the tests of plan T-078. */
    fun onAppRestored() {}

    /**
     * Called by :app on each stop of the activity. Main thread. [changingConfigurations] is true when the activity
     * stops only to be recreated (a rotation): the next start is then not a return.
     */
    fun onAppStopped(changingConfigurations: Boolean) {
        if (!changingConfigurations) leftApp = true
    }

    /** May be called from any thread. Shows the dialog at once, unless it is not armed or already shown. */
    override fun reportNetworkFailure(source: String) {
        if (armed.value) show("load_failed source=$source")
    }

    private suspend fun checkOnForeground() {
        var waited = 0L
        while (!connectivity.isConnected()) {
            if (waited >= NetworkSettings.FOREGROUND_GRACE_MILLIS) {
                if (armed.value) show(FOREGROUND_REASON)
                return
            }
            delay(NetworkSettings.RETRY_POLL_MILLIS)
            waited += NetworkSettings.RETRY_POLL_MILLIS
        }
    }

    private suspend fun showAfterDelay() {
        val seconds = settings.value(NetworkSettings.LOST_DELAY).first()
        delay(seconds * MILLIS_PER_SECOND)
        show("device_offline delay=${seconds}s")
    }

    /** Shows the dialog, or keeps [reason] for later while a full-screen ad is on screen. Any thread. */
    private fun show(reason: String) {
        if (!adShowing.value) {
            showNow(reason)
            return
        }
        if (pendingShow.getAndSet(reason) == null) logger.info(TAG, "Dialog waits: full_screen_ad")
        // The ad may have closed meanwhile, after it looked for a waiting show: show it now, once.
        if (!adShowing.value) pendingShow.getAndSet(null)?.let(::showNow)
    }

    private fun showNow(reason: String) {
        var shown = false
        _state.update { current ->
            shown = current == NetworkDialogState.Hidden
            if (shown) NetworkDialogState.Shown(checking = false, stillOffline = false) else current
        }
        if (shown) logger.info(TAG, "Dialog shown: $reason")
    }

    private fun disarm(flagOff: Boolean) {
        retryJob?.cancel()
        foregroundJob?.cancel()
        // A show waiting for the ad is dropped like any failure on the loading screen.
        pendingShow.set(null)
        if (_state.value != NetworkDialogState.Hidden) {
            logger.info(TAG, "Dialog hidden: ${if (flagOff) "flag_off" else "inactive"}")
        }
        _state.value = NetworkDialogState.Hidden
    }

    private companion object {
        const val TAG = "Network"
        const val MILLIS_PER_SECOND = 1_000L
        const val AFTER_AD = "after_full_screen_ad"
        const val FOREGROUND_REASON = "offline_on_foreground"
        val CHECKING = NetworkDialogState.Shown(checking = true, stillOffline = false)
        val STILL_OFFLINE = NetworkDialogState.Shown(checking = false, stillOffline = true)
    }
}
