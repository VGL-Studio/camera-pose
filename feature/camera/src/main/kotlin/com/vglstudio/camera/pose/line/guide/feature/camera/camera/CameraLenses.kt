package com.vglstudio.camera.pose.line.guide.feature.camera.camera

import com.vglstudio.camera.pose.line.guide.feature.camera.model.Lens
import kotlin.coroutines.cancellation.CancellationException

/**
 * The lenses [hasCamera] reports; empty when it throws, so the open decides as before (spec camera, KB-4, T-064).
 * The error goes to [onError], for the caller to log (ADR-0019, plan T-072).
 */
internal fun lensesOf(onError: (Exception) -> Unit = {}, hasCamera: (Lens) -> Boolean): Set<Lens> = try {
    Lens.entries.filterTo(mutableSetOf(), hasCamera)
} catch (e: CancellationException) {
    throw e
} catch (e: Exception) {
    onError(e)
    emptySet()
}
