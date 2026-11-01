package com.vglstudio.camera.pose.line.guide.core.ads

/** One of the two ad platforms of banner, MREC, interstitial, rewarded and resume app open ads (ADR-0037). */
enum class AdPlatform { ADMOB, MAX }

/**
 * `ads_mediation_order` (ADR-0037, KB-31): which platforms load and in which order. Read with [of]; the order that
 * applies is [effective], since a build without an AppLovin MAX SDK key has AdMob only (question 43 of the ads spec).
 */
enum class MediationOrder(val code: String, val usesAdmob: Boolean, val usesMax: Boolean) {
    ADMOB("admob", usesAdmob = true, usesMax = false),
    MAX("max", usesAdmob = false, usesMax = true),
    ADMOB_MAX("admob_max", usesAdmob = true, usesMax = true),
    MAX_ADMOB("max_admob", usesAdmob = true, usesMax = true),
    ;

    /** The platforms in the order they are asked, the first one first. */
    val platforms: List<AdPlatform>
        get() = when (this) {
            ADMOB -> listOf(AdPlatform.ADMOB)
            MAX -> listOf(AdPlatform.MAX)
            ADMOB_MAX -> listOf(AdPlatform.ADMOB, AdPlatform.MAX)
            MAX_ADMOB -> listOf(AdPlatform.MAX, AdPlatform.ADMOB)
        }

    companion object {
        val DEFAULT = MAX_ADMOB

        /** Only the exact codes count (case, spaces): any other value, empty too, is [DEFAULT], like A-12. */
        fun of(value: String): MediationOrder = entries.firstOrNull { it.code == value } ?: DEFAULT

        /** Without MAX in the build ([maxAvailable] false), every order is [ADMOB], even `max`. */
        fun effective(value: MediationOrder, maxAvailable: Boolean): MediationOrder = if (maxAvailable) value else ADMOB
    }
}
