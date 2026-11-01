package com.vglstudio.camera.pose.line.guide.core.ads

import com.vglstudio.camera.pose.line.guide.core.config.RemoteKey

/**
 * Remote Config keys of the ads module, named as in AdsKit (docs/ops/ads-business-rules.md, section 10; ADR-0026),
 * with their values before any remote value arrives. The defaults are business decisions (ADR-0014). Public so
 * :app can list every key of the app; a key declared here must also be returned by [all] (checked by a unit test).
 * The module reads them only through [AdsFlags].
 *
 * Keys of a placement are derived from its declaration in [AdPlacement] (MC-03, MC-04): `is_show_<code>`,
 * `ads_interval_<group>`, `<parameter>_<code>`. Number keys keep the AdsKit names and state their unit in the
 * description, not at the end of the name (ADR-0026, decision 9).
 */
object AdsSettings {
    /**
     * Names of the official AdsKit key table (DG:171-181). A key of section 10.1 whose name is not here ends its
     * description with a note that the template named it.
     */
    internal val ADSKIT_KEY_NAMES: Set<String> = setOf(
        "show_open_ads",
        "show_open_ads_first_open",
        "open_ad_type",
        "show_resum_ads",
        "resume_ad_type",
        "ads_interval",
        "is_show_native_after_inter",
        "is_show_native_bottom",
        "time_reload_collap_ad",
        "is_show_rw_get_item",
        "is_show_native_onboarding",
    )

    private fun booleanKey(name: String, default: Boolean, description: String) =
        RemoteKey.boolean(name, default, nameNote(name, description))

    private fun longKey(name: String, default: Long, description: String) =
        RemoteKey.long(name, default, nameNote(name, description))

    private fun stringKey(name: String, default: String, description: String) =
        RemoteKey.string(name, default, nameNote(name, description))

    /** Names taken from JBase, not from AdsKit (ADR-0038): their description says so instead of [TEMPLATE_NAME]. */
    internal val JBASE_KEY_NAMES: Set<String> = setOf("time_close_native")

    private fun nameNote(name: String, description: String) = when (name) {
        in ADSKIT_KEY_NAMES -> description
        in JBASE_KEY_NAMES -> "$description $JBASE_NAME"
        else -> "$description $TEMPLATE_NAME"
    }

    val IS_SHOW_ADS = booleanKey(
        name = "is_show_ads",
        default = true,
        description = "Master switch for every ad. When false, the app loads and shows no ad at all.",
    )

    val SHOW_OPEN_ADS = booleanKey(
        name = "show_open_ads",
        default = false,
        description = "Shows the ad when the app opens, from the second open on.",
    )

    val SHOW_OPEN_ADS_FIRST_OPEN = booleanKey(
        name = "show_open_ads_first_open",
        default = false,
        description = "Shows the ad when the app opens for the first time after install.",
    )

    val OPEN_AD_TYPE = stringKey(
        name = "open_ad_type",
        default = "aoa",
        description = "Ad shown when the app opens: inter for an interstitial, any other value for an app open ad.",
    )

    /** Keeps the AdsKit typo on purpose: the key already exists on Firebase under this name. */
    val SHOW_RESUM_ADS = booleanKey(
        name = "show_resum_ads",
        default = false,
        description = "Shows an ad when the user comes back to the app. Name kept with the AdsKit typo.",
    )

    val RESUME_AD_TYPE = stringKey(
        name = "resume_ad_type",
        default = "aoa",
        description = "Ad shown when the user comes back: only inter gives an interstitial, " +
            "any other value an app open ad.",
    )

    val ADS_INTERVAL = longKey(
        name = "ads_interval",
        default = 45L,
        description = "Minimum time between two full-screen ads of the default interval group, in seconds.",
    )

    val IS_SHOW_NATIVE_AFTER_INTER = booleanKey(
        name = "is_show_native_after_inter",
        default = false,
        description = "Shows a full-screen native ad right after an interstitial (combo).",
    )

    val TIME_CLOSE_NATIVE = longKey(
        name = "time_close_native",
        default = 5L,
        description = "Countdown before the full-screen native ad can be closed, in seconds; 1 to 30, " +
            "any other value uses 5.",
    )

    val IS_SHOW_NATIVE_BOTTOM = booleanKey(
        name = "is_show_native_bottom",
        default = false,
        description = "Bottom slot of every bottom ad bar: a small native ad when true, a banner when false.",
    )

    val IS_SHOW_NATIVE_COLLAP = booleanKey(
        name = "is_show_native_collap",
        default = true,
        description = "Collapsible native layer on the bottom ad bar, only when is_show_native_bottom is also true.",
    )

    val TIME_EXPAND_COLLAP_AD = longKey(
        name = "time_expand_collap_ad",
        default = 10L,
        description = "Time after the user collapses the collapsible native ad before it expands again, in seconds; " +
            "0 means never.",
    )

    val IS_ENABLE_CONSENT_SHORTCUT = booleanKey(
        name = "is_enable_consent_shortcut",
        default = false,
        description = "Remote switch of ERROR_ALLOWED, the temporary right when UMP fails on the network (S-12); " +
            "also needs the build setting of the app. CACHED_EARLY is always on (ADR-0038). False takes " +
            "ERROR_ALLOWED back at once.",
    )

    val IS_LOG_AD_FULLSCREEN_EVENTS = booleanKey(
        name = "is_log_ad_fullscreen_events",
        default = false,
        description = "Logs the Firebase lifecycle events of full-screen ads.",
    )

    val IS_LOG_AD_SLOT_EVENTS = booleanKey(
        name = "is_log_ad_slot_events",
        default = false,
        description = "Logs the Firebase lifecycle events of ad slots (banner, native).",
    )

    /** The order of AdMob and AppLovin MAX (ADR-0037, plan T-068); read by [MediationOrder.of]. */
    val ADS_MEDIATION_ORDER = stringKey(
        name = "ads_mediation_order",
        default = MediationOrder.DEFAULT.code,
        description = "Order of the two ad platforms for banner, MREC, interstitial, rewarded and resume app open " +
            "ads: admob, max, admob_max or max_admob; any other value uses max_admob. Without a MAX SDK key in " +
            "the build, admob.",
    )

    /** The switch `is_show_<code>` of [placement], off by default (ADR-0014, MC-03). */
    fun placementEnabled(placement: AdPlacement): RemoteKey<Boolean> = placementEnabled(placement.declaration)

    internal fun placementEnabled(declaration: AdPlacementDeclaration): RemoteKey<Boolean> = RemoteKey.boolean(
        name = "is_show_${declaration.code}",
        default = false,
        description = "Shows ads in this placement. Off by default." +
            (if (declaration.stagingOnly) " Staging ad gallery only." else "") +
            " ${derivedNote(declaration)}",
    )

    /** `ads_interval` for [AdIntervalGroup.DEFAULT], else `ads_interval_<name>` with the group's default (MC-04). */
    fun interval(group: AdIntervalGroup): RemoteKey<Long> {
        val name = group.name ?: return ADS_INTERVAL
        return RemoteKey.long(
            name = "ads_interval_$name",
            default = requireNotNull(group.defaultSeconds),
            description = "Minimum time between two interstitials of the interval group $name, in seconds. " +
                "Derived key of interval group $name.",
        )
    }

    /** `native_every_<code>` of [placement], or `null` when the placement declares no [AdPlacement.nativeEvery]. */
    fun nativeEvery(placement: AdPlacement): RemoteKey<Long>? = nativeEvery(placement.declaration)

    internal fun nativeEvery(declaration: AdPlacementDeclaration): RemoteKey<Long>? {
        val every = declaration.nativeEvery ?: return null
        return placementParamKey("native_every", declaration) { name ->
            RemoteKey.long(
                name = name,
                default = every.toLong(),
                description = "Number of items between two native ads of this placement, in items. " +
                    derivedNote(declaration),
            )
        }
    }

    /**
     * A parameter of one placement, named `<param>_<code>` (question 28 of the ads spec). Every parameter key of a
     * placement is made here, so later ones (`rw_no_ad_policy_<code>`, T-031) follow the same pattern.
     */
    internal fun <T : Any> placementParamKey(
        param: String,
        declaration: AdPlacementDeclaration,
        key: (name: String) -> RemoteKey<T>,
    ): RemoteKey<T> = key("${param}_${declaration.code}")

    /**
     * Every key for [placements]: the keys of section 10.1 above in their order, then for each placement its switch
     * and its parameters, then one `ads_interval_<name>` per interval group of its own. An app-wide placement has no
     * switch of its own: the keys of section 10.1 turn it on (MC-09, plan T-024). Two keys with the same name (for
     * example one group declared with two defaults) throw [IllegalArgumentException].
     */
    internal fun keysFor(placements: List<AdPlacementDeclaration>): List<RemoteKey<*>> {
        val keys = GLOBAL_KEYS +
            placements.flatMap { listOfNotNull(if (it.appWide) null else placementEnabled(it), nativeEvery(it)) } +
            placements.map { it.intervalGroup }.filter { it != AdIntervalGroup.DEFAULT }.distinct().map(::interval)
        val duplicates = keys.groupBy { it.name }.filterValues { it.size > 1 }.keys
        require(duplicates.isEmpty()) { "Remote Config keys declared twice: $duplicates" }
        return keys
    }

    /** Every key of the module, derived from the placements of [AdPlacement]. */
    fun all(): List<RemoteKey<*>> = keysFor(AdPlacement.entries.map { it.declaration })

    /** The keys of section 10.1 owned by ads, in the order of the table. */
    private val GLOBAL_KEYS: List<RemoteKey<*>> = listOf(
        IS_SHOW_ADS,
        SHOW_OPEN_ADS,
        SHOW_OPEN_ADS_FIRST_OPEN,
        OPEN_AD_TYPE,
        SHOW_RESUM_ADS,
        RESUME_AD_TYPE,
        ADS_INTERVAL,
        IS_SHOW_NATIVE_AFTER_INTER,
        TIME_CLOSE_NATIVE,
        IS_SHOW_NATIVE_BOTTOM,
        IS_SHOW_NATIVE_COLLAP,
        TIME_EXPAND_COLLAP_AD,
        IS_ENABLE_CONSENT_SHORTCUT,
        IS_LOG_AD_FULLSCREEN_EVENTS,
        IS_LOG_AD_SLOT_EVENTS,
        ADS_MEDIATION_ORDER,
    )

    private fun derivedNote(declaration: AdPlacementDeclaration) =
        "Derived key of placement ${declaration.code} (${declaration.format.code})."
}

/** End of the description of a key whose name is not in the official AdsKit key table. */
private const val TEMPLATE_NAME = "Name set by the template, not an AdsKit key."

/** End of the description of a key whose name comes from JBase (ADR-0038). */
private const val JBASE_NAME = "Name from JBase, not an AdsKit key."
