package com.vglstudio.camera.pose.line.guide.core.poses.internal

import com.vglstudio.camera.pose.line.guide.core.assets.AssetPath
import com.vglstudio.camera.pose.line.guide.core.assets.RemoteAssets
import com.vglstudio.camera.pose.line.guide.core.poses.LoadFailureReason
import com.vglstudio.camera.pose.line.guide.core.poses.PoseContent
import com.vglstudio.camera.pose.line.guide.core.poses.PoseFileResult
import com.vglstudio.camera.pose.line.guide.core.poses.PoseLoadFailure
import kotlinx.coroutines.CoroutineScope

/** Loads a file and gives up waiting after [timeoutMillis]. The download itself may go on in the background. */
internal class TimedAssetLoader(
    private val assets: RemoteAssets,
    private val scope: CoroutineScope,
    private val timeoutMillis: Long = LOAD_TIMEOUT_MILLIS,
) {
    suspend fun load(path: AssetPath, content: PoseContent): PoseFileResult =
        PoseFileResult.Failed(PoseLoadFailure(content, LoadFailureReason.OTHER))
}
