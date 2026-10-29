package com.vglstudio.camera.pose.line.guide.core.config

import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/** Web pages the app opens, set remotely without a new release (spec settings, KB-5). */
interface AppLinks {
    /** The privacy policy page; always an https address. */
    val privacyPolicyUrl: Flow<String>

    /** The Remote Config keys of the links, declared once here (ADR-0023). */
    companion object {
        val PRIVACY_POLICY_URL: RemoteKey<String> = RemoteKey.string(
            "privacy_policy_url",
            "https://studio.vgl.io.vn/privacy-policy",
            "Address of the privacy policy page opened from settings.",
        )

        /** Every key of this file, for the list of the app's keys. */
        val KEYS: List<RemoteKey<*>> = listOf(PRIVACY_POLICY_URL)
    }
}

/**
 * Reads the addresses from Remote Config (RemoteSettings: keys are declared by the core module that uses them).
 * A remote value that is not an https address (empty, mistyped) is replaced by the default.
 */
internal class DefaultAppLinks @Inject constructor(private val remoteSettings: RemoteSettings) : AppLinks {
    override val privacyPolicyUrl: Flow<String> = remoteSettings
        .value(AppLinks.PRIVACY_POLICY_URL)
        .map { url -> url.takeIf { it.startsWith(HTTPS) } ?: AppLinks.PRIVACY_POLICY_URL.default }

    private companion object {
        const val HTTPS = "https://"
    }
}
