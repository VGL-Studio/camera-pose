package com.vglstudio.camera.pose.line.guide.core.ads

/**
 * Debug settings of the consent form (User Messaging Platform), bound in :app per build flavor.
 * Staging uses [EEA_TEST_DEVICE] so anyone can test the European consent form; production uses [NONE].
 * [allowConsentReset] lets the staging build clear the stored consent to test the form again (S-18).
 */
data class ConsentDebugOptions(val forceEea: Boolean, val allowConsentReset: Boolean = false) {
    companion object {
        val NONE = ConsentDebugOptions(forceEea = false)

        /** Treats every device as a test device located in the European Economic Area; consent can be reset. */
        val EEA_TEST_DEVICE = ConsentDebugOptions(forceEea = true, allowConsentReset = true)
    }
}
