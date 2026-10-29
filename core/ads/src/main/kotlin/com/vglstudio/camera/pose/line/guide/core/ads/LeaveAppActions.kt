package com.vglstudio.camera.pose.line.guide.core.ads

import android.content.ActivityNotFoundException
import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContract
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.platform.LocalContext
import androidx.core.app.ActivityOptionsCompat

/*
 * How features leave the app on purpose (plan T-074, pass 2; G-03, G-05), each call site in one line: a system
 * screen through [rememberAdHoldingLauncher], a page of another app through [rememberStartOutsideApp], a system flow
 * that outlives the screen through [rememberStartAdHold]. Only in this app; proposed to the template.
 */

/**
 * rememberLauncherForActivityResult that holds the ads that show by themselves while the system screen it opens is
 * up (G-03, way 1): hold right before launch, release on the first line of the result, also when launch throws.
 * Without an ads controller (tests, previews) it only launches.
 */
@Composable
fun <I, O> rememberAdHoldingLauncher(
    contract: ActivityResultContract<I, O>,
    onResult: (O) -> Unit,
): ActivityResultLauncher<I> {
    val suppression = rememberAdSuppression()
    val currentOnResult by rememberUpdatedState(onResult)
    val launcher = rememberLauncherForActivityResult(contract) { result ->
        suppression.release()
        currentOnResult(result)
    }
    return remember(launcher, suppression) { AdHoldingLauncher(launcher, suppression) }
}

/** Holds [suppression] around each launch of [launcher]; the result callback releases it. */
private class AdHoldingLauncher<I>(
    private val launcher: ActivityResultLauncher<I>,
    private val suppression: AdSuppression,
) : ActivityResultLauncher<I>() {
    override val contract: ActivityResultContract<I, *> get() = launcher.contract

    override fun launch(input: I, options: ActivityOptionsCompat?) {
        suppression.hold()
        try {
            launcher.launch(input, options)
        } catch (e: RuntimeException) {
            // No screen opened, so no result will come to release the hold.
            suppression.release()
            throw e
        }
    }

    override fun unregister() = launcher.unregister()
}

/**
 * Starts [Intent] outside the app and, when it started, marks the leave so the next return shows no resume ad
 * (G-03, way 3; G-05). Returns false when no activity handles it (ActivityNotFoundException), with no mark.
 * Without an ads controller (tests, previews) it only starts.
 */
@Composable
fun rememberStartOutsideApp(): (Intent) -> Boolean {
    val context = LocalContext.current
    val markLeavingApp = rememberMarkLeavingApp()
    return remember(context, markLeavingApp) {
        { intent ->
            try {
                context.startActivity(intent)
                markLeavingApp()
                true
            } catch (e: ActivityNotFoundException) {
                false
            }
        }
    }
}

/**
 * Holds the ads that show by themselves for a system flow that goes on after the calling screen has left, such as
 * the store review asked on the way back to the camera (G-03, way 1). Each call of the returned action starts a hold
 * and returns its release, to call once on the main thread when the flow ends; a second call of a release does
 * nothing. Unlike [rememberAdSuppression], leaving the composition does not release it. Holds nothing without an ads
 * controller (tests, previews).
 */
@Composable
fun rememberStartAdHold(): () -> (() -> Unit) {
    val target = LocalResumeSuppression.current ?: LocalAdsController.current?.resumeSuppression
    return remember(target) {
        {
            val hold = target?.hold(ResumeSuppression.SYSTEM_SCREEN)
            var released = false
            val release: () -> Unit = {
                if (!released) {
                    released = true
                    hold?.let { target?.release(it) }
                }
            }
            release
        }
    }
}
