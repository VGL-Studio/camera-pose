package com.vglstudio.camera.pose.line.guide.config

import com.vglstudio.camera.pose.line.guide.core.ads.AdsSettings
import com.vglstudio.camera.pose.line.guide.core.ads.InterstitialBreakSettings
import com.vglstudio.camera.pose.line.guide.core.analytics.AppsFlyerSettings
import com.vglstudio.camera.pose.line.guide.core.config.AppLinks
import com.vglstudio.camera.pose.line.guide.core.config.FeatureFlag
import com.vglstudio.camera.pose.line.guide.core.config.PoseMatchingSettings
import com.vglstudio.camera.pose.line.guide.core.config.RemoteKey
import com.vglstudio.camera.pose.line.guide.core.network.NetworkSettings
import com.vglstudio.camera.pose.line.guide.core.poses.PosesRemoteKeys
import com.vglstudio.camera.pose.line.guide.core.review.ReviewSettings

/**
 * Every Remote Config key of the app, by owning module (ADR-0023). A new core module with keys is added here.
 * :app is the only module that sees every core module (ADR-0001). docs/remote-config-keys.md and
 * docs/remote-config-template.json are generated from this list by RemoteConfigKeysTest.
 */
object AppRemoteKeys {
    val byModule: Map<String, List<RemoteKey<*>>> = linkedMapOf(
        ":core:config" to FeatureFlag.entries.map { it.remoteKey } + AppLinks.KEYS + PoseMatchingSettings.KEYS,
        ":core:ads" to AdsSettings.all() + InterstitialBreakSettings.all(),
        ":core:analytics" to AppsFlyerSettings.all(),
        ":core:network" to NetworkSettings.all(),
        ":core:poses" to PosesRemoteKeys.all(),
        ":core:review" to ReviewSettings.all(),
    )
}
