package com.vglstudio.camera.pose.line.guide.core.ads

/**
 * The build switch of the consent shortcut (CACHED_EARLY, ERROR_ALLOWED), bound in :app per build flavor (S-12). The
 * shortcut works only when this and the remote key is_enable_consent_shortcut are both on.
 */
data class ConsentShortcutConfig(val enabledInBuild: Boolean) {
    companion object {
        val OFF = ConsentShortcutConfig(enabledInBuild = false)
    }
}
