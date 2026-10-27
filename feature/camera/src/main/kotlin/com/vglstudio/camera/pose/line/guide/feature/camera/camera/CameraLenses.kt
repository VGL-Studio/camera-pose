package com.vglstudio.camera.pose.line.guide.feature.camera.camera

import com.vglstudio.camera.pose.line.guide.feature.camera.model.Lens
import kotlin.coroutines.cancellation.CancellationException

/** The lenses [hasCamera] reports; empty when it throws, so the open decides as before (spec camera, KB-4, T-064). */
internal fun lensesOf(hasCamera: (Lens) -> Boolean): Set<Lens> = try {
    Lens.entries.filterTo(mutableSetOf(), hasCamera)
} catch (e: CancellationException) {
    throw e
} catch (e: Exception) {
    emptySet()
}
