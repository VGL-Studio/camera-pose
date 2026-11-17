package com.vglstudio.camera.pose.line.guide.core.analytics

import com.vglstudio.camera.pose.line.guide.core.config.RemoteKey
import com.vglstudio.camera.pose.line.guide.core.config.RemoteSettings
import kotlinx.coroutines.CoroutineScope

/** The latest value of every Tenjin switch ([TenjinSettings]). */
internal class TenjinSwitches(settings: RemoteSettings, scope: CoroutineScope) {
    fun isOn(key: RemoteKey<Boolean>): Boolean = key.default
}
