package com.vglstudio.camera.pose.line.guide.core.config

/**
 * Every feature flag of the app, declared in one place (ADR-0003).
 * Naming rule for remote keys: `feature_<name>_enabled`.
 * [defaultValue] is what the app uses until remote values are fetched. It is always `false`
 * (checked by a unit test): a feature stays hidden in production until it is approved.
 * To see a feature while it is being built, enable it for the staging build only (see [LocallyEnabledFlags]).
 */
enum class FeatureFlag(val key: String, val defaultValue: Boolean = false) {
    SAMPLE("feature_sample_enabled"),
    LOADING("feature_loading_enabled"),
    NETWORK_DIALOG("feature_network_dialog_enabled"),
    CAMERA("feature_camera_enabled"),
    ONBOARDING("feature_onboarding_enabled"),
    POSES("feature_poses_enabled"),
    SETTINGS("feature_settings_enabled"),
}
