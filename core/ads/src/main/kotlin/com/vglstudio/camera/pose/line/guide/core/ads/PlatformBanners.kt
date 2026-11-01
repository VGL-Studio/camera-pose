package com.vglstudio.camera.pose.line.guide.core.ads

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.vglstudio.camera.pose.line.guide.core.logging.AppLogger
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch

private const val TAG = "Ads"

/**
 * The platforms a banner slot of [format] asks, in order (plan T-071, KB-31, ADR-0037): a fixed banner and an MREC
 * follow [order] with the platforms that have a unit ([admobUnit], [maxUnit]; blank is none); a collapsible banner
 * is AdMob only. Empty: the slot has no frame (B-04). [AdsController.maxReady] is not read here: a list that changed
 * when MAX became ready would rebuild the frame and drop the ad of AdMob it shows.
 */
internal fun bannerPlatforms(
    format: AdFormat,
    order: MediationOrder,
    admobUnit: String?,
    maxUnit: String?,
): List<AdPlatform> {
    val hasAdmob = !admobUnit.isNullOrBlank()
    if (format == AdFormat.BANNER_COLLAPSIBLE) return if (hasAdmob) listOf(AdPlatform.ADMOB) else emptyList()
    return order.platforms.filter { platform ->
        when (platform) {
            AdPlatform.ADMOB -> hasAdmob
            AdPlatform.MAX -> !maxUnit.isNullOrBlank()
        }
    }
}

/**
 * The height of a frame of [platforms]: the tallest of theirs, so the frame does not jump when the platform shown
 * changes; the ad is drawn at the top of the frame. With no platform (no frame) the height of AdMob, as before T-071.
 */
internal fun bannerFrameHeightDp(platforms: List<AdPlatform>, admobHeightDp: Int, maxHeightDp: Int): Int =
    platforms.maxOfOrNull { if (it == AdPlatform.ADMOB) admobHeightDp else maxHeightDp } ?: admobHeightDp

/**
 * The key of a frame for [BannerSlotHost] (its `unitId`): null without platforms (no frame), else one string of the
 * platforms and their units, so a new order or unit builds a new frame. Kept in memory only, never logged.
 */
internal fun bannerSlotUnitKey(platforms: List<AdPlatform>, admobUnit: String?, maxUnit: String?): String? {
    if (platforms.isEmpty()) return null
    return platforms.joinToString(";") { platform ->
        when (platform) {
            AdPlatform.ADMOB -> "admob=${admobUnit.orEmpty()}"
            AdPlatform.MAX -> "max=${maxUnit.orEmpty()}"
        }
    }
}

/** The code of the turn of MAX skipped while MAX is not ready: no code of `MaxErrorCode` 13.6.4, never in metrics. */
internal const val MAX_NOT_READY = -9000

/**
 * The banner of one frame with its platforms in order (plan T-071, KB-31): [children] are the banner of each platform,
 * the first one asked first. One child (order `admob` or `max`, or one platform without a unit) goes through this too,
 * so every send has one path (L-072).
 *
 * - [send] asks the platform of this turn: the first one, or the next one after [BannerLoadResult.TryNext]. MAX not
 *   [maxReady] is not asked: its turn counts as no ad (`MAX_NOT_READY`), no request is sent and no `MaxAdView` built.
 *   A child paused is resumed before it sends.
 * - The result of the child asked: Loaded shows it and pauses the others; Failed of the last platform is Failed (one
 *   failure of B-02, even after a pause), of an earlier one TryNext (the frame asks the next platform at once through
 *   [runBannerLoads]; after a pause of the frame the next turn starts again from the first platform). A MAX child that
 *   failed is paused, so MAX never refreshes or sends by itself outside the send step; an AdMob child is not, as
 *   before T-071.
 * - The refreshes of the child shown pass through (Failed is retried from the first platform); other children are
 *   ignored. [pause] pauses every child; [resume] resumes the child shown and the one asked.
 * - [results] has one inbox per collection, and every child is subscribed before the collection first suspends, so a
 *   result given inside [send] (the turn of MAX skipped, an SDK that fails at once) is never lost. The children's
 *   results must be flows subscribed at once (a `SharedFlow`), not built on a channel. Main thread only.
 * - [heightsDp] is the height of each platform's banner: a child is drawn at its own height, full width, at the top
 *   of the frame (question 4 of the plan, B-09); a platform without a height takes the size of the frame.
 */
internal class OrderedBannerAdView(
    private val children: List<Pair<AdPlatform, BannerAdView>>,
    private val maxReady: () -> Boolean,
    private val logger: AppLogger,
    private val placementKey: String,
    private val heightsDp: Map<AdPlatform, Int> = emptyMap(),
) : BannerAdView {
    /** One result of child [child]; [sendNo] is set on the result the frame gives for a skipped turn of MAX. */
    private class Tagged(val child: Int, val result: BannerLoadResult, val sendNo: Int?)

    /** The child asked by the next [send]. */
    private var next = 0

    /** The child sent to that has not answered yet. */
    private var pending: Int? = null

    /** The child whose ad shows. */
    private var showing by mutableStateOf<Int?>(null)
    private var running = true
    private var pausedSinceSend = false
    private val paused = BooleanArray(children.size)
    private var sendCount = 0

    /** The inbox of the collection of [results] running now. */
    private var active: Channel<Tagged>? = null

    override val results: Flow<BannerLoadResult> = flow {
        coroutineScope {
            val inbox = Channel<Tagged>(Channel.UNLIMITED)
            active = inbox
            try {
                children.forEachIndexed { index, (_, child) ->
                    launch(start = CoroutineStart.UNDISPATCHED) {
                        child.results.collect { inbox.send(Tagged(index, it, null)) }
                    }
                }
                for (tagged in inbox) {
                    handle(tagged)?.let { emit(it) }
                }
            } finally {
                if (active === inbox) active = null
            }
        }
    }

    override fun send() {
        if (children.isEmpty()) return
        sendCount++
        val index = next.coerceIn(children.indices)
        pending = index
        pausedSinceSend = false
        if (children[index].first == AdPlatform.MAX && !maxReady()) {
            logger.info(TAG, "Banner $placementKey MAX not ready")
            active?.trySend(Tagged(index, BannerLoadResult.Failed(MAX_NOT_READY), sendCount))
            return
        }
        resumeChild(index)
        children[index].second.send()
    }

    override fun pause() {
        running = false
        next = 0
        pausedSinceSend = true
        children.indices.forEach(::pauseChild)
    }

    override fun resume() {
        running = true
        showing?.let(::resumeChild)
        pending?.let(::resumeChild)
    }

    override fun destroy() {
        next = 0
        pending = null
        children.forEach { (_, child) -> child.destroy() }
    }

    @Composable
    override fun Content(showAd: Boolean, modifier: Modifier) {
        val shown = showing
        Box(modifier = modifier, contentAlignment = Alignment.TopCenter) {
            children.forEachIndexed { index, (platform, child) ->
                // A platform shorter than the frame takes its own height at the top of the frame (B-09): an AdView
                // of AdMob given the height of the frame would centre its ad in it.
                val own = heightsDp[platform]
                val childModifier = if (own != null) Modifier.fillMaxWidth().height(own.dp) else modifier
                child.Content(showAd = showAd && index == shown, modifier = childModifier)
            }
        }
    }

    private fun handle(tagged: Tagged): BannerLoadResult? {
        // The result given for a skipped turn of MAX belongs to that send only.
        if (tagged.sendNo != null && tagged.sendNo != sendCount) return null
        val index = tagged.child
        return when (index) {
            pending -> answer(index, tagged.result)
            showing -> tagged.result.takeUnless { it == BannerLoadResult.TryNext }
            else -> null
        }
    }

    /** The answer of the child asked. */
    private fun answer(index: Int, result: BannerLoadResult): BannerLoadResult? {
        pending = null
        val platform = children[index].first
        return when (result) {
            BannerLoadResult.Loaded -> {
                showing = index
                next = 0
                children.indices.filter { it != index }.forEach(::pauseChild)
                if (running) {
                    resumeChild(index)
                } else if (platform == AdPlatform.MAX) {
                    pauseChild(index)
                }
                BannerLoadResult.Loaded
            }

            is BannerLoadResult.Failed -> {
                if (platform == AdPlatform.MAX) pauseChild(index)
                when {
                    index >= children.lastIndex -> {
                        next = 0
                        result
                    }

                    pausedSinceSend -> {
                        next = 0
                        BannerLoadResult.TryNext
                    }

                    else -> {
                        next = index + 1
                        logger.info(
                            TAG,
                            "Banner $placementKey $platform no ad, trying ${children[next].first}",
                        )
                        BannerLoadResult.TryNext
                    }
                }
            }

            BannerLoadResult.TryNext -> null
        }
    }

    private fun pauseChild(index: Int) {
        if (paused[index]) return
        paused[index] = true
        children[index].second.pause()
    }

    private fun resumeChild(index: Int) {
        if (!paused[index]) return
        paused[index] = false
        children[index].second.resume()
    }
}
