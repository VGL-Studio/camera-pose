package com.vglstudio.camera.pose.line.guide.core.ads

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow

/** Test tag of the ad drawn by [FakeBannerAdView] while it shows its ad. */
internal const val FAKE_BANNER_TAG = "fake_banner"

/**
 * A banner of the SDK for tests: records sends (at [clock]), pauses, resumes and destroys, emits results on
 * command, and draws a box tagged [tag] while it shows its ad (two fakes of one frame take two tags, plan T-071). Like
 * the SDK, it replays no result to a late collector.
 */
internal class FakeBannerAdView(private val clock: () -> Long = { 0L }, private val tag: String = FAKE_BANNER_TAG) :
    BannerAdView {
    private val emitted = MutableSharedFlow<BannerLoadResult>(extraBufferCapacity = 64)

    /** Called at the end of every [send], to answer it (for example fail at once). */
    var onSend: () -> Unit = {}

    val sendTimes = mutableListOf<Long>()
    val sendCount: Int get() = sendTimes.size
    var pauseCount = 0
        private set
    var resumeCount = 0
        private set
    var destroyCount = 0
        private set

    /** False after a [pause] until the next [resume]: whether the last of the two calls was a resume. */
    var isRunning = true
        private set

    override val results: Flow<BannerLoadResult> = emitted

    fun emitLoaded() {
        check(emitted.tryEmit(BannerLoadResult.Loaded))
    }

    fun emitFailed(code: Int) {
        check(emitted.tryEmit(BannerLoadResult.Failed(code)))
    }

    /** The result of a frame of two platforms whose first platform had no ad (plan T-071). */
    fun emitTryNext() {
        check(emitted.tryEmit(BannerLoadResult.TryNext))
    }

    override fun send() {
        sendTimes += clock()
        onSend()
    }

    override fun pause() {
        pauseCount++
        isRunning = false
    }

    override fun resume() {
        resumeCount++
        isRunning = true
    }

    override fun destroy() {
        destroyCount++
    }

    @Composable
    override fun Content(showAd: Boolean, modifier: Modifier) {
        Box(modifier = modifier) {
            if (showAd) Box(modifier = Modifier.fillMaxSize().testTag(tag))
        }
    }
}

/** Makes the fake banners of a slot and keeps every one it made. */
internal class FakeBannerAdViews(private val clock: () -> Long = { 0L }) {
    val created = mutableListOf<FakeBannerAdView>()
    val createdCount: Int get() = created.size
    val last: FakeBannerAdView get() = created.last()

    fun newView(): BannerAdView = FakeBannerAdView(clock).also { created += it }
}
