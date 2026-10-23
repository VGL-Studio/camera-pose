package com.vglstudio.camera.pose.line.guide.core.config

import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/** Web pages the app opens, set remotely without a new release (spec settings, KB-5). */
interface AppLinks {
    /** The privacy policy page; always an https address. */
    val privacyPolicyUrl: Flow<String>
}

/**
 * Reads the addresses from Remote Config (RemoteSettings: keys are declared by the core module that uses them).
 * A remote value that is not an https address (empty, mistyped) is replaced by the default.
 */
internal class DefaultAppLinks @Inject constructor(private val remoteSettings: RemoteSettings) : AppLinks {
    override val privacyPolicyUrl: Flow<String> = remoteSettings
        .string(PRIVACY_POLICY_URL_KEY, DEFAULT_PRIVACY_POLICY_URL)
        .map { url -> url.takeIf { it.startsWith(HTTPS) } ?: DEFAULT_PRIVACY_POLICY_URL }

    private companion object {
        const val PRIVACY_POLICY_URL_KEY = "privacy_policy_url"
        const val DEFAULT_PRIVACY_POLICY_URL = "https://studio.vgl.io.vn/privacy-policy"
        const val HTTPS = "https://"
    }
}
