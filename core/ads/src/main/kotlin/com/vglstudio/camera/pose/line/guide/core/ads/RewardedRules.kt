package com.vglstudio.camera.pose.line.guide.core.ads

/**
 * What a rewarded placement does when no ad can be shown (MC-05): [Block] (the default, the user may try again),
 * [Allow] (the action runs), or [AllowAfter] (the action runs on the [AllowAfter.tries]-th try in a row with no ad,
 * question 3 of plan T-031). Closing the ad before the reward always blocks, whatever the policy (R-02, R-04).
 * A placement declares it as [AdPlacement.noAdPolicy]; [parse] reads the code of a remote value, for an app whose spec
 * adds the key `rw_no_ad_policy_<code>` (question 28 of the ads spec).
 */
sealed interface RewardedNoAdPolicy {
    /** The code of the policy: `block`, `allow`, `allow_after_<n>`. */
    val code: String

    /** The action is blocked; the user may try again. */
    data object Block : RewardedNoAdPolicy {
        override val code: String = "block"
    }

    /** The action runs with no ad. */
    data object Allow : RewardedNoAdPolicy {
        override val code: String = "allow"
    }

    /** The action runs on the [tries]-th try in a row with no ad (1 or more; `allow_after_1` is [Allow]). */
    data class AllowAfter(val tries: Int) : RewardedNoAdPolicy {
        init {
            require(tries >= 1) { "allow_after needs 1 try or more, was $tries" }
        }

        override val code: String get() = "$ALLOW_AFTER_PREFIX$tries"
    }

    companion object {
        private const val ALLOW_AFTER_PREFIX = "allow_after_"
        private val ALLOW_AFTER = Regex("^$ALLOW_AFTER_PREFIX([1-9][0-9]{0,8})$")

        /** The policy of [code], or null when it is not one: codes are lower case, `n` from 1, no leading zero. */
        fun parse(code: String): RewardedNoAdPolicy? = when (code) {
            Block.code -> Block
            Allow.code -> Allow
            else -> ALLOW_AFTER.matchEntire(code)?.groupValues?.get(1)?.toIntOrNull()?.let { AllowAfter(it) }
        }
    }
}

/** What one load round of a rewarded placement does (L-072). */
internal enum class RewardedLoadStep { NOT_NEEDED, SWITCHED_OFF, GATE_CLOSED, SEND }

/**
 * The send decision of every rewarded load round (preload plan, F-05, F-06, F-07), in this order: a load is needed
 * (F-04), the placement is switched on, the consent gate lets a request go now (S-15).
 */
internal fun rewardedLoadStep(needsLoad: Boolean, enabled: Boolean, mayRequest: Boolean): RewardedLoadStep = when {
    !needsLoad -> RewardedLoadStep.NOT_NEEDED
    !enabled -> RewardedLoadStep.SWITCHED_OFF
    !mayRequest -> RewardedLoadStep.GATE_CLOSED
    else -> RewardedLoadStep.SEND
}

/** Why no rewarded ad can be shown now (R-03, R-04). */
internal enum class RewardedNoAdReason { CONSENT_CLOSED, NETWORK, NOT_READY }

/**
 * Why no rewarded ad can be shown, or null when one can: the consent gate first, then a ready ad (an ad loaded before
 * the device went offline is shown), then the network (R-03: offline now, or the last load failed on the network).
 */
internal fun noAdReason(
    consentOpen: Boolean,
    ready: Boolean,
    offline: Boolean,
    lastLoadNetworkError: Boolean,
): RewardedNoAdReason? = when {
    !consentOpen -> RewardedNoAdReason.CONSENT_CLOSED
    ready -> null
    offline || lastLoadNetworkError -> RewardedNoAdReason.NETWORK
    else -> RewardedNoAdReason.NOT_READY
}

/** R-03: a load error is a network error with code 2 (`ERROR_CODE_NETWORK_ERROR`), or any code while offline. */
internal fun isNetworkLoadError(code: Int, offline: Boolean): Boolean = code == NETWORK_ERROR_CODE || offline

/** R-03 for MAX: NETWORK_ERROR (-1000), NETWORK_TIMEOUT (-1001), NO_NETWORK (-1009), or any code while offline. */
internal fun isMaxNetworkLoadError(code: Int, offline: Boolean): Boolean = code in MAX_NETWORK_ERROR_CODES || offline

/**
 * R-03 with two platforms (plan T-106): among the platforms of [platforms] with a failed load since their last send
 * (flag not null), at least one, and every one failed on the network. A platform with no result yet does not count.
 */
internal fun rewardedNetworkError(platforms: List<AdPlatform>, admob: Boolean?, max: Boolean?): Boolean {
    val known = platforms.mapNotNull { if (it == AdPlatform.ADMOB) admob else max }
    return known.isNotEmpty() && known.all { it }
}

/** Whether [policy] lets the action run on a try with no ad, the [noAdInARow]-th one in a row (question 3). */
internal fun grantsWithoutAd(policy: RewardedNoAdPolicy, noAdInARow: Int): Boolean = when (policy) {
    RewardedNoAdPolicy.Block -> false
    RewardedNoAdPolicy.Allow -> true
    is RewardedNoAdPolicy.AllowAfter -> noAdInARow >= policy.tries
}

/** The loader of a full-screen slot in [AdsController]. */
internal enum class FullScreenLoader { INTERSTITIAL, NATIVE_FULL_SCREEN, APP_OPEN, REWARDED }

/**
 * The loader of [format], so a rewarded placement never falls into the interstitial loader. Exhaustive on purpose,
 * with no `else`: a format with no full-screen slot ([hasFullScreenSlot]) is an [IllegalArgumentException].
 */
internal fun fullScreenLoaderOf(format: AdFormat): FullScreenLoader = when (format) {
    AdFormat.INTERSTITIAL -> FullScreenLoader.INTERSTITIAL
    AdFormat.NATIVE_FULL_SCREEN -> FullScreenLoader.NATIVE_FULL_SCREEN
    AdFormat.APP_OPEN -> FullScreenLoader.APP_OPEN
    AdFormat.REWARDED -> FullScreenLoader.REWARDED
    AdFormat.BANNER,
    AdFormat.NATIVE_SMALL,
    AdFormat.NATIVE_MEDIUM,
    AdFormat.NATIVE_COLLAPSIBLE,
    AdFormat.BANNER_COLLAPSIBLE,
    AdFormat.NATIVE_STEP,
    AdFormat.NATIVE_IN_COLLECTION,
    AdFormat.BANNER_MREC,
    -> throw IllegalArgumentException("$format has no full-screen slot")
}

/** `LoadAdError.ERROR_CODE_NETWORK_ERROR`. */
private const val NETWORK_ERROR_CODE = 2

/** `MaxErrorCode.NETWORK_ERROR`, `NETWORK_TIMEOUT`, `NO_NETWORK` of AppLovin MAX. */
private val MAX_NETWORK_ERROR_CODES = setOf(-1000, -1001, -1009)
