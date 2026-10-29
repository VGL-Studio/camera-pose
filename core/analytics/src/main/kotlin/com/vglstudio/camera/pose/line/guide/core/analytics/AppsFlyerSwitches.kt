package com.vglstudio.camera.pose.line.guide.core.analytics

import com.vglstudio.camera.pose.line.guide.core.config.RemoteKey
import com.vglstudio.camera.pose.line.guide.core.config.RemoteSettings
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

/**
 * The latest value of every `is_log_af_*` switch ([AppsFlyerSettings]), followed as a flow (ADR-0003) and read again
 * at every event, so a change during the session applies from the next event (section 8.3). Before Remote Config has
 * a value, each switch is its default: on.
 */
@Singleton
internal class AppsFlyerSwitches(settings: RemoteSettings, scope: CoroutineScope) {
    /** A scope of its own, as AdsController has. */
    @Inject
    constructor(settings: RemoteSettings) : this(settings, CoroutineScope(SupervisorJob() + Dispatchers.Default))

    private val values: Map<String, StateFlow<Boolean>> = AppsFlyerSettings.switches().associate { key ->
        key.name to settings.value(key).stateIn(scope, SharingStarted.Eagerly, key.default)
    }

    fun isOn(key: RemoteKey<Boolean>): Boolean = values[key.name]?.value ?: key.default
}
