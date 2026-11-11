package com.vglstudio.camera.pose.line.guide.core.ads

import com.vglstudio.camera.pose.line.guide.core.logging.AppLogger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow

/** The illustration texts of the design, used by every native ad test. */
internal val sampleNativeAdAssets = NativeAdAssets(
    headline = "Headline of the ad",
    advertiser = "Advertiser or short body",
    body = "Body text of the ad, two lines at most so the call to action stays in view.",
    callToAction = "Install",
    icon = null,
    hasMedia = true,
    starRating = 4.0,
)

/** Texts longer than any layout holds: the full native ad keeps its height with them (plan T-027). */
internal val longFullNativeAdAssets = sampleNativeAdAssets.copy(
    headline = "Headline of the ad that is far too long to fit on a single line of the card",
    advertiser = "Advertiser with a name that is also much too long for its line",
    body = "Body text of the ad that goes on and on. ".repeat(8),
    callToAction = "Install now and get the full version",
)

/** A loaded ad that counts how many times it was destroyed; the test sets [impressionRecorded] (P-01a). */
internal class FakeLoadedNativeAd(override val assets: NativeAdAssets = sampleNativeAdAssets) : LoadedNativeAd {
    var destroyCount = 0
        private set

    override var impressionRecorded = false

    override fun destroy() {
        destroyCount++
    }
}

/** A loader whose requests stay open until the test answers them with [deliver] or [fail]. */
internal class FakeNativeAdLoader : NativeAdLoader {
    class Request(
        val unitId: String,
        val size: NativeAdSize,
        val onLoaded: (LoadedNativeAd) -> Unit,
        val onFailed: (Int, String?) -> Unit,
    )

    val requests = mutableListOf<Request>()

    /** Requests not answered yet, and the most there ever were at once (plan T-040, one load in flight). */
    var open = 0
        private set
    var maxOpen = 0
        private set

    override fun load(
        unitId: String,
        size: NativeAdSize,
        onLoaded: (LoadedNativeAd) -> Unit,
        onFailed: (code: Int, logMessage: String?) -> Unit,
    ) {
        requests += Request(unitId, size, onLoaded, onFailed)
        open++
        maxOpen = maxOf(maxOpen, open)
    }

    /** Answers request [index] (in the order they were made) with [ad]; for loads of two stocks at once. */
    fun deliverAt(index: Int, ad: LoadedNativeAd) {
        open--
        requests[index].onLoaded(ad)
    }

    /** Answers the last request with [ad]. */
    fun deliver(ad: LoadedNativeAd) {
        open--
        requests.last().onLoaded(ad)
    }

    /** Answers the last request with an SDK error [code] and the [logMessage] the loader hands over (plan T-093). */
    fun fail(code: Int, logMessage: String? = null) {
        open--
        requests.last().onFailed(code, logMessage)
    }
}

/**
 * Native loads by [NativeLoadUse] through [loader] ([loadOneNativeAd]), recording the use, the send time and the
 * placement of the waiting slot (null for a load of the stock itself) of each (plans T-040, T-086), and the
 * [PlacementCell] each load hands over in [holders], holding that placement until a slot claims the ad. [throwNext]
 * makes the next load throw before anything is sent, like a request that cannot be built (O-09).
 */
internal class FakeNativeLoads(
    private val loader: FakeNativeAdLoader,
    private val now: () -> Long,
    private val logger: AppLogger,
    private val timeoutMillis: Long = 30_000L,
) {
    class Sent(val use: NativeLoadUse, val atMillis: Long, val placement: AdPlacement? = null)

    val sent = mutableListOf<Sent>()
    val holders = mutableListOf<PlacementCell>()
    var throwNext: Exception? = null

    val uses: List<NativeLoadUse> get() = sent.map { it.use }
    val times: List<Long> get() = sent.map { it.atMillis }
    val placements: List<AdPlacement?> get() = sent.map { it.placement }

    suspend fun load(use: NativeLoadUse, placement: AdPlacement? = null): NativeSourceLoad {
        throwNext?.let {
            throwNext = null
            throw it
        }
        sent += Sent(use, now(), placement)
        val holder = PlacementCell().apply { value = placement }
        holders += holder
        return NativeSourceLoad(
            loadOneNativeAd(loader, FAKE_UNIT_ID, NativeAdSize.SMALL, timeoutMillis, logger, FAKE_KEY),
            holder,
        )
    }

    companion object {
        const val FAKE_UNIT_ID = "test-native-unit"
        const val FAKE_KEY = "poses_bottom"
    }
}

/**
 * The loads of a native pool (plan T-028) through [loader] ([loadOneNativeAd]): records the send time of each in
 * [times] and the holder cell of each ad in [holders]. A Loaded result becomes a [PoolLoad], any other one null (a
 * failed load). [throwNext] makes the next load throw before anything is sent (O-09).
 */
internal class FakePoolLoads(
    private val loader: FakeNativeAdLoader,
    private val now: () -> Long,
    private val logger: AppLogger,
    private val timeoutMillis: Long = 30_000L,
) {
    val times = mutableListOf<Long>()
    val holders = mutableListOf<PlacementCell>()
    var throwNext: Exception? = null

    suspend fun load(): PoolLoad? {
        throwNext?.let {
            throwNext = null
            throw it
        }
        times += now()
        val state =
            loadOneNativeAd(loader, FAKE_POOL_UNIT_ID, NativeAdSize.MEDIUM, timeoutMillis, logger, FAKE_POOL_KEY)
        val ad = (state as? NativeAdState.Loaded)?.ad ?: return null
        return PoolLoad(ad, PlacementCell().also { holders += it })
    }

    companion object {
        const val FAKE_POOL_UNIT_ID = "test-pool-unit"
        const val FAKE_POOL_KEY = "native_medium#1"
    }
}

/**
 * A pool for Compose tests: every load answers at once from [ads] (null when none is left), sent through an
 * unconfined scope, so [NativeAdPool.refill] fills it before it returns.
 */
internal fun immediatePool(ads: List<FakeLoadedNativeAd>, capacity: Int = 3, logger: AppLogger): NativeAdPool {
    val left = ArrayDeque(ads)
    return NativeAdPool(
        key = NativePoolKey(AdFormat.NATIVE_MEDIUM, FakePoolLoads.FAKE_POOL_UNIT_ID),
        capacity = capacity,
        load = { left.removeFirstOrNull()?.let { PoolLoad(it, PlacementCell()) } },
        sendGate = { true },
        canLoadNow = { true },
        canShowNow = { true },
        retryResets = MutableStateFlow(0),
        clock = { 0L },
        scope = CoroutineScope(Dispatchers.Unconfined),
        logger = logger,
        logKey = FakePoolLoads.FAKE_POOL_KEY,
    )
}

/** Fake ads with the headlines "Ad 1" to "Ad [count]". */
internal fun numberedAds(count: Int): List<FakeLoadedNativeAd> =
    (1..count).map { FakeLoadedNativeAd(sampleNativeAdAssets.copy(headline = "Ad $it")) }
