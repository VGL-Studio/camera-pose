package com.vglstudio.camera.pose.line.guide.feature.poses.detail

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import com.vglstudio.camera.pose.line.guide.core.ads.AdPlacement
import com.vglstudio.camera.pose.line.guide.core.ads.rememberShowInterstitial

/**
 * The rule "a tap while an earlier one waits for its ad is ignored" of "Shoot with this pose" (spec poses, KB-4; plan
 * T-079, section 8, question 4). Free of Android, so it runs in unit tests. Main thread only.
 */
internal class ShootGate {
    private var waiting: String? = null

    /**
     * Returns null without calling [pick] while a tap waits. Else calls [pick] (the ViewModel logs the choice and
     * returns the pose); a pose becomes the waiting one and is returned, no pose leaves nothing waiting.
     */
    fun tryBegin(pick: () -> String?): String? {
        if (waiting != null) return null
        val pose = pick() ?: return null
        waiting = pose
        return pose
    }

    /** Returns the pose of the waiting tap, if any, and ends the wait. */
    fun finish(): String? {
        val pose = waiting
        waiting = null
        return pose
    }
}

/**
 * Returns the action of "Shoot with this pose" (KB-4). The action asks [ShootGate.tryBegin] for the pose (the
 * ViewModel logs the choice), asks the interstitial of [placement] at this natural break, then opens the camera with
 * the pose in onFinished, shown or not. A tap while an earlier one waits for its ad is ignored, so the camera opens
 * once and the choice is logged once.
 */
@Composable
internal fun rememberShootWithAd(
    placement: AdPlacement,
    onOpen: (poseId: String) -> Unit,
): (pick: () -> String?) -> Unit {
    val gate = remember { ShootGate() }
    val currentOnOpen by rememberUpdatedState(onOpen)
    // onFinished runs once per call, on the main thread (Dispatchers.Main.immediate, L-030).
    val showAd = rememberShowInterstitial(placement) { _ -> gate.finish()?.let(currentOnOpen) }
    return remember(gate, showAd) {
        { pick -> if (gate.tryBegin(pick) != null) showAd() }
    }
}
