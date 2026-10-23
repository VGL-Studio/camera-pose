package com.vglstudio.camera.pose.line.guide.core.config

import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

/** The privacy policy address read from Remote Config (spec settings, KB-5; plan T-025, section 4). */
class DefaultAppLinksTest {
    private val remote = FakeRemoteFlagSource()
    private val links = DefaultAppLinks(DefaultRemoteSettings(remote))

    @Test
    fun withoutRemoteValue_usesDefault() = runTest {
        assertEquals("https://studio.vgl.io.vn/privacy-policy", links.privacyPolicyUrl.first())
    }

    @Test
    fun remoteValue_replacesDefault() = runTest {
        remote.strings.value = mapOf("privacy_policy_url" to "https://example.com/p")
        assertEquals("https://example.com/p", links.privacyPolicyUrl.first())
    }

    @Test
    fun invalidRemoteValue_usesDefault() = runTest {
        remote.strings.value = mapOf("privacy_policy_url" to "")
        assertEquals("https://studio.vgl.io.vn/privacy-policy", links.privacyPolicyUrl.first())
        remote.strings.value = mapOf("privacy_policy_url" to "http://x")
        assertEquals("https://studio.vgl.io.vn/privacy-policy", links.privacyPolicyUrl.first())
    }
}
