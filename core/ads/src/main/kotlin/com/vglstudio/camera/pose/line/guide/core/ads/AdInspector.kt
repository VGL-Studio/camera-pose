package com.vglstudio.camera.pose.line.guide.core.ads

import androidx.activity.compose.LocalActivity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState

/** Result of opening AdMob's Ad Inspector. */
sealed interface AdInspectorResult {
    /** The inspector was shown and the user closed it. */
    data object Closed : AdInspectorResult

    /** The SDK refused, for example on a device that is not a test device. [code] is the SDK error code. */
    data class Failed(val code: Int) : AdInspectorResult

    /** Ads are not started yet (no consent, or no ads controller in tests and previews). */
    data object NotReady : AdInspectorResult
}

/**
 * Returns an action that opens AdMob's Ad Inspector, for the staging ad gallery only. Never call it from a feature.
 * [onResult] gets the outcome once per call; without an ads controller or an activity it gets [AdInspectorResult.NotReady]
 * and no SDK is called.
 */
@Composable
fun rememberOpenAdInspector(onResult: (AdInspectorResult) -> Unit): () -> Unit {
    val ads = LocalAdsController.current
    val activity = LocalActivity.current
    val currentOnResult = rememberUpdatedState(onResult)
    return remember(ads, activity) {
        {
            if (ads == null || activity == null) {
                currentOnResult.value(AdInspectorResult.NotReady)
            } else {
                ads.openAdInspector(activity) { currentOnResult.value(it) }
            }
        }
    }
}

/** Maps the SDK's answer: no error means the inspector was shown and closed. */
internal fun adInspectorResult(errorCode: Int?): AdInspectorResult =
    if (errorCode == null) AdInspectorResult.Closed else AdInspectorResult.Failed(errorCode)
