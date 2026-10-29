package com.vglstudio.camera.pose.line.guide.core.ads

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf

/** The block of the ads that show by themselves, provided by [AdsHost]; null in tests and previews. */
internal val LocalResumeSuppression = staticCompositionLocalOf<ResumeSuppression?> { null }

/** The block for the composables of this file: from [AdsHost], else from [LocalAdsController], else none. */
@Composable
private fun currentResumeSuppression(): ResumeSuppression? =
    LocalResumeSuppression.current ?: LocalAdsController.current?.resumeSuppression

/**
 * A hold on the ads that show by themselves (resume ad), for a screen of the system that the app opens: photo
 * picker, camera, permission dialog (G-03, way 1). Made by [rememberAdSuppression]. Main thread only.
 */
class AdSuppression internal constructor(private val target: ResumeSuppression?) {
    private var hold: ForegroundAdSuppressor.Hold? = null

    /** Call right before opening the screen of the system. A second call while held does nothing. */
    fun hold() {
        if (hold != null) return
        hold = target?.hold(ResumeSuppression.SYSTEM_SCREEN)
    }

    /** Call on the first line of the result callback, also when opening failed. A second call does nothing. */
    fun release() {
        val current = hold ?: return
        hold = null
        target?.release(current)
    }
}

/**
 * A hold for a screen of the system (G-03, way 1): [AdSuppression.hold] right before opening it,
 * [AdSuppression.release] on the first line of its callback. Released by itself when the caller leaves the
 * composition. Does nothing in tests and previews (no [LocalAdsController]).
 */
@Composable
fun rememberAdSuppression(): AdSuppression {
    val target = currentResumeSuppression()
    val suppression = remember(target) { AdSuppression(target) }
    DisposableEffect(suppression) { onDispose { suppression.release() } }
    return suppression
}

/**
 * Blocks the ads that show by themselves while [active] and while in the composition (G-03, way 2): a screen of the
 * app that covers the whole screen, such as the native page after an interstitial.
 */
@Composable
fun SuppressResumeAds(active: Boolean) {
    val target = currentResumeSuppression() ?: return
    DisposableEffect(target, active) {
        val hold = if (active) target.hold(ResumeSuppression.FULL_SCREEN_PAGE) else null
        onDispose { hold?.let(target::release) }
    }
}

/**
 * Returns the action to call right after leaving the app on purpose succeeded (share, link, Play): the next return
 * to an activity of the app shows no resume ad, and the mark is then used up (G-03, way 3; G-05).
 */
@Composable
fun rememberMarkLeavingApp(): () -> Unit {
    val target = currentResumeSuppression()
    return remember(target) { { target?.markLeavingApp() } }
}

/**
 * The root of the app's UI (G-04, HD-05): provides [LocalAdsController] and reports the hold of the ads that show by
 * themselves, read in composition, so the block is lowered one recomposition after the last hold is released (G-02).
 * :app places it once, where it provided [LocalAdsController] before; without it the block is never lowered.
 */
@Composable
fun AdsHost(ads: AdsController?, content: @Composable () -> Unit) {
    CompositionLocalProvider(LocalAdsController provides ads) {
        if (ads == null) {
            content()
        } else {
            AdsHostContent(ads.resumeSuppression, ads.foregroundAdSuppressor, content)
        }
    }
}

/** [AdsHost] without the controller, for tests. */
@Composable
internal fun AdsHostContent(
    suppression: ResumeSuppression,
    suppressor: ForegroundAdSuppressor,
    content: @Composable () -> Unit,
) {
    ShellSuppressionReport(suppression, suppressor)
    CompositionLocalProvider(LocalResumeSuppression provides suppression) { content() }
}

/** Reads the hold in composition and reports it after every recomposition (G-04); draws nothing. */
@Composable
private fun ShellSuppressionReport(suppression: ResumeSuppression, suppressor: ForegroundAdSuppressor) {
    val suppressed = suppressor.suppressed.collectAsState().value
    // Read so that a hold raised and released between two frames still brings a recomposition and a report.
    val awaitingReport = suppression.awaitingReport.collectAsState().value
    SideEffect {
        // With neither one, the block is already low: nothing to report.
        if (awaitingReport || suppressed) suppression.onShellReported(suppressed)
    }
}
