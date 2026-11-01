package com.vglstudio.camera.pose.line.guide.core.ads

/**
 * The build switch of ERROR_ALLOWED, bound in :app per build flavor (S-12). ERROR_ALLOWED works only when this and the
 * remote key is_enable_consent_shortcut are both on; CACHED_EARLY needs neither (ADR-0038).
 */
data class ConsentShortcutConfig(val enabledInBuild: Boolean) {
    companion object {
        val OFF = ConsentShortcutConfig(enabledInBuild = false)
    }
}
