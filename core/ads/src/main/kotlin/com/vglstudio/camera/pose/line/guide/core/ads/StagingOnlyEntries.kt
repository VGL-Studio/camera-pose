package com.vglstudio.camera.pose.line.guide.core.ads

/**
 * Whether [rememberShowAppOpenAd] may show this placement: an app open placement of the staging ad gallery only
 * (plan T-036). The app open ads of the app are shown by the loading screen ([StartupAds], T-024) and when the app
 * comes back (T-025), never by a button; `open_app` and `resume` are app-wide, not staging only.
 */
internal fun AdPlacementDeclaration.acceptsStagingAppOpen(): Boolean = format == AdFormat.APP_OPEN && stagingOnly

/**
 * The no-ad policy of one rewarded turn (MC-05): the policy of the placement line
 * ([AdPlacementDeclaration.effectiveNoAdPolicy]) when [override] is null; else [override], which only a staging-only
 * placement may take (the policy picker of the staging ad gallery, plan T-036, question 13 of the ads spec). A feature
 * declares its policy on the placement line; [override] on any other placement is an [IllegalArgumentException].
 */
internal fun rewardedPolicyOf(declaration: AdPlacementDeclaration, override: RewardedNoAdPolicy?): RewardedNoAdPolicy {
    if (override == null) return declaration.effectiveNoAdPolicy
    require(declaration.stagingOnly) {
        "${declaration.code}: the no-ad policy is set at run time on the staging ad gallery only; " +
            "declare it on the placement line"
    }
    return override
}
