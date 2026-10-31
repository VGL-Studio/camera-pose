package com.vglstudio.camera.pose.line.guide.core.ads

/**
 * How an ad is shown: the closed list of formats of :core:ads (docs/ops/ads-business-rules.md, MC-02). A placement
 * has exactly one format. [code] is the name of the format in that list. An app that needs another format proposes
 * it to the template project.
 */
enum class AdFormat(val code: String) {
    /**
     * Fixed banner shown inside a screen with [AdSlot]. The SDK refreshes it at the rate set on its ad unit in AdMob
     * (auto refresh on); the app only retries after a failure (B-02).
     */
    BANNER("banner"),

    /** Covers the whole screen, shown at a natural break with [rememberShowFullScreenAd]. */
    INTERSTITIAL("interstitial"),

    /**
     * Shown inside a screen with [NativeAdSlot], no media: icon, headline, "Ad" label, call to action. Also the format
     * of a bottom bar placement ([BottomAdBarScaffold]): its bottom slot is native small (plan T-030).
     */
    NATIVE_SMALL("native_small"),

    /**
     * Shown inside a screen with [NativeAdSlot], in a list row or with [NativeAdCell], with media (image or video),
     * from the pool (T-028); replaces MREC.
     */
    NATIVE_MEDIUM("native_medium"),

    /**
     * The 250 dp layer of a bottom bar ([BottomAdBarScaffold]) floating over its slot: the user collapses it, and it
     * opens again `time_expand_collap_ad` seconds later (CL-01 to CL-04). Not drawn on its own (D-01).
     */
    NATIVE_COLLAPSIBLE("native_collap"),

    /**
     * Covers the whole screen at a natural break, shown with [rememberShowFullScreenAd], or right after an
     * interstitial when the combo is on (`native_combo`, through [NativeFullScreenTurns], plan T-026), or as the page
     * of a flow step with [NativeFullScreenStep], no controls (MC-06, plan T-032).
     */
    NATIVE_FULL_SCREEN("native_fullscreen"),

    /**
     * App open ad. Shown by the loading screen through [StartupAds] (T-024) and when the app comes back to foreground
     * (`resume`, T-025).
     */
    APP_OPEN("app_open"),

    /**
     * Rewarded ad before an action the user chose (MC-05): the feature holds the action with [rememberRewardedAction]
     * and asks with [RewardedAdDialog] (plan T-031). It has a full-screen slot ([hasFullScreenSlot]) but is not
     * [isFullScreen]: it never reads nor moves `ads_interval` (R-05, I-02).
     */
    REWARDED("rewarded"),

    /**
     * Banner that the user collapses, shown at the bottom of a screen with [AdSlot]. :core:ads loads it again 30 s after
     * each load, each time as a new expanded banner (B-03); its ad unit has auto refresh off.
     */
    BANNER_COLLAPSIBLE("banner_collapsible"),

    /**
     * 250 dp native card at one step of a flow (onboarding, result), shown with [NativeStepAd] outside the pager, one
     * pooled ad per step (MC-06, T-032).
     */
    NATIVE_STEP("native_step"),

    /**
     * Native card in a grid, a list or a pager page, shown with [NativeAdCell] or as an in-list row, from the pool
     * (T-028).
     */
    NATIVE_IN_COLLECTION("native_in_collection"),
}

/**
 * Formats shown with [rememberShowFullScreenAd]: interstitial and native full screen. They share the guardrails of
 * full-screen ads (interval, one at a time).
 */
internal val AdFormat.isFullScreen: Boolean
    get() = this == AdFormat.INTERSTITIAL || this == AdFormat.NATIVE_FULL_SCREEN

/**
 * Formats with a preloaded full-screen slot in [AdsController]: the formats of [isFullScreen], app open and rewarded.
 * App open and rewarded are not [isFullScreen] (not shown with [rememberShowFullScreenAd]), so they move no
 * `ads_interval` mark (I-02, R-05).
 */
internal val AdFormat.hasFullScreenSlot: Boolean
    get() = isFullScreen || this == AdFormat.APP_OPEN || this == AdFormat.REWARDED

/**
 * The interval group of an interstitial (MC-04). [DEFAULT] uses the key `ads_interval`; a group made with [named]
 * uses `ads_interval_<name>`, with the default seconds of the app's spec. Two groups are equal when they have the
 * same name and the same default.
 */
class AdIntervalGroup private constructor(
    /** Name of the group, `null` for [DEFAULT]. */
    val name: String?,
    /** Default of `ads_interval_<name>` in seconds; `null` for [DEFAULT], which uses the default of `ads_interval`. */
    val defaultSeconds: Long?,
) {
    override fun equals(other: Any?): Boolean =
        other is AdIntervalGroup && name == other.name && defaultSeconds == other.defaultSeconds

    override fun hashCode(): Int = 31 * name.hashCode() + defaultSeconds.hashCode()

    override fun toString(): String = name ?: "default"

    companion object {
        /** The group of every interstitial that declares none, and of app open, resume and combo. */
        val DEFAULT = AdIntervalGroup(null, null)

        /** A group of its own, read from `ads_interval_<name>`, [defaultSeconds] by default. */
        fun named(name: String, defaultSeconds: Long): AdIntervalGroup {
            require(CODE_PATTERN.matches(name)) { "Interval group '$name' must be lower snake case" }
            require(defaultSeconds >= 0) { "Interval group '$name' needs a default of 0 seconds or more" }
            return AdIntervalGroup(name, defaultSeconds)
        }
    }
}

/**
 * Every ad placement of the app, declared in one place (ADR-0004, MC-01).
 * A placement is a business decision: it must come from an approved spec.
 * Adding, removing or moving a placement is a high-risk change (risk-scoring.md, blocking point 1).
 * [key] is the placement code: it names the Remote Config keys of the placement and is the value of `ad_placement`.
 * Each placement shows ads only once the Remote Config key `is_show_<key>` is switched on (off by default), except an
 * [appWide] placement, which the shared keys of section 10.1 switch on; [AdsSettings] derives every key of a placement
 * from this declaration.
 * Production builds need a real ad unit for every placement except the staging-only ones (see [ConfiguredAdUnitIds]).
 *
 * A placement that is not staging only stays on one line, `NAME("code", AdFormat.X),` or with its attributes after
 * the format: scripts/release.sh reads only one-line declarations (AdPlacementSourceTest, docs/ops/ads-guide.md,
 * section 5).
 */
enum class AdPlacement(
    val key: String,
    val format: AdFormat,
    /** Interval group, interstitials only (MC-04). */
    val intervalGroup: AdIntervalGroup = AdIntervalGroup.DEFAULT,
    /** Default N of `native_every_<key>` (one ad after every N items), list formats only (MC-07). */
    val nativeEvery: Int? = null,
    /** True when this placement shows without reading the `ads_interval` gate (MC-04, I-04); full screen only. */
    val ignoreInterval: Boolean = false,
    /** False when closing this placement moves no `ads_interval` mark (MC-04, I-04); full screen only. */
    val recordInterval: Boolean = true,
    /**
     * Switched on by the shared keys of section 10.1, with no `is_show_<code>` of its own (MC-09): the ads of the
     * loading screen (T-024), the native ad of the combo (T-026) and the resume ad (T-025). Features never show it.
     */
    val appWide: Boolean = false,
    /** The large call to action of 5.7 (56 dp, 17 sp, card 422 dp high); native medium only (`cta_large`). */
    val ctaLarge: Boolean = false,
    /** A cell keeps a static placeholder instead of hiding while its pool is exhausted (P-06); pooled formats only. */
    val keepSpaceWhenPoolEmpty: Boolean = false,
    /**
     * What the action of a rewarded placement does when no ad can be shown (MC-05): [RewardedNoAdPolicy.Block] when
     * null; rewarded only. The action itself is the `onGranted` of [rememberRewardedAction], named in the comment of
     * the line and in the app's spec.
     */
    val noAdPolicy: RewardedNoAdPolicy? = null,
) {
    /** The bottom bar of the sample screen ([BottomAdBarScaffold], plan T-030, question 1). */
    SAMPLE_NATIVE_SMALL("sample_native_small", AdFormat.NATIVE_SMALL),
    OPEN_APP("open_app", AdFormat.APP_OPEN, appWide = true),
    OPEN_APP_INTER("open_app_inter", AdFormat.INTERSTITIAL, ignoreInterval = true, appWide = true),
    NATIVE_AFTER_INTER("native_combo", AdFormat.NATIVE_FULL_SCREEN, appWide = true),
    RESUME("resume", AdFormat.APP_OPEN, appWide = true),
    RESUME_INTER("resume_inter", AdFormat.INTERSTITIAL, ignoreInterval = true, appWide = true),

    // The placements of this app (spec ads, questions 1, 2 and 8): one line each, as scripts/release.sh reads them.

    /** Bottom bar of the pose library (spec poses, section 5; spec ads, question 8). */
    POSES_BOTTOM("poses_bottom", AdFormat.NATIVE_SMALL),

    /** Bottom bar of the pose detail, below the main button (spec poses, section 5; spec ads, question 8). */
    POSE_DETAIL_BOTTOM("pose_detail_bottom", AdFormat.NATIVE_SMALL),

    // Skeleton for the tests written first (plan T-079, pass 2): the format is set in the code commit.
    INTER_POSE_DETAIL("inter_pose_detail", AdFormat.NATIVE_SMALL),

    /** Leaving the review screen after a saved photo (spec camera, section 5; breaks: InterstitialBreaks). */
    REVIEW_INTERSTITIAL("review_interstitial", AdFormat.INTERSTITIAL),

    // Staging ad gallery only (plans T-014, T-036): placements of every format, see [stagingOnly]. The collapsible
    // layer has none of its own: it loads through the placement of its bottom bar (GALLERY_NATIVE_SMALL, T-030).
    GALLERY_NATIVE_SMALL("gallery_native_small", AdFormat.NATIVE_SMALL),
    GALLERY_NATIVE_MEDIUM("gallery_native_medium", AdFormat.NATIVE_MEDIUM),
    GALLERY_INLIST_SMALL("gallery_inlist_small", AdFormat.NATIVE_SMALL, nativeEvery = 8),
    GALLERY_INLIST_MEDIUM("gallery_inlist_medium", AdFormat.NATIVE_MEDIUM, nativeEvery = 8),
    GALLERY_INTERSTITIAL("gallery_interstitial", AdFormat.INTERSTITIAL),
    GALLERY_NATIVE_FULL_SCREEN("gallery_native_full_screen", AdFormat.NATIVE_FULL_SCREEN),

    // Holds the sample action of the gallery, policy block by default (plan T-031, question 13 of the ads spec).
    GALLERY_RW_ACTION("gallery_rw_action", AdFormat.REWARDED),
    GALLERY_APP_OPEN("gallery_app_open", AdFormat.APP_OPEN),
    GALLERY_BANNER("gallery_banner", AdFormat.BANNER),
    GALLERY_BANNER_COLLAPSIBLE("gallery_banner_collapsible", AdFormat.BANNER_COLLAPSIBLE),
    GALLERY_IN_COLLECTION("gallery_in_collection", AdFormat.NATIVE_IN_COLLECTION, nativeEvery = 4),
    GALLERY_NATIVE_STEP("gallery_native_step", AdFormat.NATIVE_STEP),
    GALLERY_NATIVE_STEP_FULL("gallery_native_step_full", AdFormat.NATIVE_FULL_SCREEN),
    ;

    /**
     * Exists only for the staging ad gallery: production has no ad unit for it and never shows it.
     * scripts/release.sh skips the same prefix.
     */
    val stagingOnly: Boolean get() = key.startsWith(STAGING_ONLY_KEY_PREFIX)

    /** The declaration of this placement, checked when the class is loaded. */
    internal val declaration: AdPlacementDeclaration =
        AdPlacementDeclaration(
            key,
            format,
            intervalGroup,
            nativeEvery,
            stagingOnly,
            ignoreInterval,
            recordInterval,
            appWide,
            ctaLarge,
            keepSpaceWhenPoolEmpty,
            noAdPolicy,
        )
}

/**
 * What a placement declares (MC-01): code, format and the attributes of its format. Kept apart from [AdPlacement]
 * so that tests can declare placements of their own. A wrong declaration throws [IllegalArgumentException] when it
 * is made, so a wrong line of [AdPlacement] fails every test that touches placements.
 */
internal data class AdPlacementDeclaration(
    val code: String,
    val format: AdFormat,
    val intervalGroup: AdIntervalGroup = AdIntervalGroup.DEFAULT,
    val nativeEvery: Int? = null,
    val stagingOnly: Boolean = false,
    /** The gate of `ads_interval` is not read (MC-04, I-04). */
    val ignoreInterval: Boolean = false,
    /** Closing it moves the mark of its group (MC-04, I-04). */
    val recordInterval: Boolean = true,
    /** Switched on by the shared keys of section 10.1, no `is_show_<code>` (MC-09). */
    val appWide: Boolean = false,
    /** The large call to action (`cta_large`, 5.7); native medium only. */
    val ctaLarge: Boolean = false,
    /** A cell keeps a static placeholder while its pool is exhausted (P-06); pooled formats only. */
    val keepSpaceWhenPoolEmpty: Boolean = false,
    /** What the action does when no ad can be shown (MC-05); rewarded only. */
    val noAdPolicy: RewardedNoAdPolicy? = null,
) {
    /** The policy of this placement: [noAdPolicy], or [RewardedNoAdPolicy.Block] (MC-05). */
    val effectiveNoAdPolicy: RewardedNoAdPolicy get() = noAdPolicy ?: RewardedNoAdPolicy.Block

    init {
        require(CODE_PATTERN.matches(code)) { "Placement code '$code' must be lower snake case" }
        require(intervalGroup == AdIntervalGroup.DEFAULT || format == AdFormat.INTERSTITIAL) {
            "Placement $code: only an interstitial has an interval group of its own"
        }
        require((!ignoreInterval && recordInterval) || format.isFullScreen) {
            "Placement $code: ignoreInterval and recordInterval are for full-screen formats only"
        }
        require(!appWide || format in APP_WIDE_FORMATS) {
            "Placement $code: appWide is for app open, interstitial and native full-screen placements only"
        }
        require(!ctaLarge || format == AdFormat.NATIVE_MEDIUM) {
            "Placement $code: ctaLarge is for native medium only"
        }
        require(!keepSpaceWhenPoolEmpty || format.usesPool) {
            "Placement $code: keepSpaceWhenPoolEmpty is for pooled formats only"
        }
        require(noAdPolicy == null || format == AdFormat.REWARDED) {
            "Placement $code: noAdPolicy is for rewarded only"
        }
        if (nativeEvery != null) {
            require(format in NATIVE_EVERY_FORMATS) { "Placement $code: nativeEvery is for list formats only" }
            require(nativeEvery >= 1) { "Placement $code: nativeEvery must be 1 or more" }
        }
    }

    private companion object {
        val NATIVE_EVERY_FORMATS = setOf(AdFormat.NATIVE_SMALL, AdFormat.NATIVE_MEDIUM, AdFormat.NATIVE_IN_COLLECTION)

        /**
         * App open and interstitial when the app opens (T-024) or comes back (T-025), native full screen for the
         * combo (T-026).
         */
        val APP_WIDE_FORMATS = setOf(AdFormat.APP_OPEN, AdFormat.INTERSTITIAL, AdFormat.NATIVE_FULL_SCREEN)
    }
}

/** Prefix of the staging-only placements; scripts/release.sh skips the same one. */
private const val STAGING_ONLY_KEY_PREFIX = "gallery_"

/** Placement codes and group names are lower snake case, like the Remote Config keys they name. */
private val CODE_PATTERN = Regex("^[a-z][a-z0-9]*(_[a-z0-9]+)*$")
