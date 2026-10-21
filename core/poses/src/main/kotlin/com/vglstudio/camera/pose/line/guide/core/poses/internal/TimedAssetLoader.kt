package com.vglstudio.camera.pose.line.guide.core.poses.internal

import com.vglstudio.camera.pose.line.guide.core.assets.AssetFailureReason
import com.vglstudio.camera.pose.line.guide.core.assets.AssetPath
import com.vglstudio.camera.pose.line.guide.core.assets.AssetResult
import com.vglstudio.camera.pose.line.guide.core.assets.RemoteAssets
import com.vglstudio.camera.pose.line.guide.core.poses.LoadFailureReason
import com.vglstudio.camera.pose.line.guide.core.poses.PoseContent
import com.vglstudio.camera.pose.line.guide.core.poses.PoseFileResult
import com.vglstudio.camera.pose.line.guide.core.poses.PoseLoadFailure
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.async
import kotlinx.coroutines.withTimeoutOrNull

/**
 * Loads a file and gives up waiting after [timeoutMillis] (ADR-1001). A download cannot be cancelled half way, so it
 * runs in [scope], not in the caller: after the timeout it goes on in the background and keeps the file when it is
 * done, and the next load finds the file at once.
 */
internal class TimedAssetLoader(
    private val assets: RemoteAssets,
    private val scope: CoroutineScope,
    private val timeoutMillis: Long = LOAD_TIMEOUT_MILLIS,
) {
    suspend fun load(path: AssetPath, content: PoseContent): PoseFileResult {
        val download = scope.async { assets.load(path) }
        val result = withTimeoutOrNull(timeoutMillis) { download.await() }
            ?: return PoseFileResult.Failed(PoseLoadFailure(content, LoadFailureReason.TIMEOUT))
        return when (result) {
            is AssetResult.Ready -> PoseFileResult.Ready(result.file)
            is AssetResult.Unavailable -> PoseFileResult.Failed(PoseLoadFailure(content, result.reason.toLoadReason()))
        }
    }

    private fun AssetFailureReason.toLoadReason(): LoadFailureReason = when (this) {
        AssetFailureReason.NETWORK -> LoadFailureReason.NETWORK
        AssetFailureReason.NOT_FOUND -> LoadFailureReason.NOT_FOUND
        AssetFailureReason.OTHER -> LoadFailureReason.OTHER
    }
}
