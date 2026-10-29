package com.vglstudio.camera.pose.line.guide.core.ads

import androidx.activity.compose.LocalActivity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember

/**
 * Returns an action that clears the stored consent and the settled result, so the consent form can be tested again
 * (S-18). It works on staging only ([ConsentDebugOptions.allowConsentReset]); production has no reset. Without an ads
 * controller or an activity (tests, previews) it does nothing. For the staging ad gallery (T-036), never a feature.
 */
@Composable
fun rememberResetConsent(): () -> Unit {
    val ads = LocalAdsController.current
    val activity = LocalActivity.current
    return remember(ads, activity) {
        {
            if (ads != null && activity != null) ads.resetConsent()
        }
    }
}
