package com.vglstudio.camera.pose.line.guide.feature.poses.detail

import androidx.compose.runtime.Composable
import com.vglstudio.camera.pose.line.guide.core.ads.AdPlacement

// Skeleton for the tests written first (plan T-079, pass 2); not built yet.
internal class ShootGate {
    fun tryBegin(pick: () -> String?): String? = null

    fun finish(): String? = null
}

@Suppress("UNUSED_PARAMETER")
@Composable
internal fun rememberShootWithAd(placement: AdPlacement, onOpen: (poseId: String) -> Unit): (pick: () -> String?) -> Unit =
    { _ -> }
