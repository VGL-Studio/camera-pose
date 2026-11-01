package com.vglstudio.camera.pose.line.guide.core.config

/**
 * Every feature flag of the app, declared in one place (ADR-0003).
 * Naming rule for remote keys: `feature_<name>_enabled`.
 * [defaultValue] is what the app uses until remote values are fetched. It is always `false`
 * (checked by a unit test): a feature stays hidden in production until it is approved.
 * [description] is one sentence in English, shown on Firebase and in docs/remote-config-keys.md (ADR-0023).
 * To see a feature while it is being built, enable it for the staging build only (see [LocallyEnabledFlags]).
 */
enum class FeatureFlag(val key: String, val description: String, val defaultValue: Boolean = false) {
    LOADING("feature_loading_enabled", "Shows the loading screen that prepares the app before the start screen."),
    NETWORK_DIALOG(
        "feature_network_dialog_enabled",
        "Shows the dialog that blocks the app while the device has no usable Internet.",
    ),
    ONBOARDING(
        "feature_onboarding_enabled",
        "Shows the welcome screens and the camera permission request on first open.",
    ),
    POSES("feature_poses_enabled", "Shows the pose library button on the camera and the library screens."),
    SETTINGS("feature_settings_enabled", "Shows the settings button and the settings screen."),
    IN_APP_REVIEW(
        "feature_in_app_review_enabled",
        "Asks for a Play review after enough saved photos (core/review rules).",
    ),
    POSE_MATCHING("feature_pose_matching_enabled", "Shows the live pose match score on the camera."),
    PREMIUM_POSES(
        "feature_premium_poses_enabled",
        "Locks the premium poses of the catalog until they are unlocked; off means every pose can be used.",
    ),
    ;

    /** The Remote Config key of this flag, listed with every other key of the app (ADR-0023). */
    val remoteKey: RemoteKey<Boolean> = RemoteKey.boolean(key, defaultValue, description)
}
