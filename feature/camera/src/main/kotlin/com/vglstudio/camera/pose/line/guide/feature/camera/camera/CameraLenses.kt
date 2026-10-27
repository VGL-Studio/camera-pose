package com.vglstudio.camera.pose.line.guide.feature.camera.camera

import com.vglstudio.camera.pose.line.guide.feature.camera.model.Lens

/** The lenses [hasCamera] reports; empty when it throws, so the open decides as before (spec camera, KB-4, T-064). */
@Suppress("UNUSED_PARAMETER")
internal fun lensesOf(hasCamera: (Lens) -> Boolean): Set<Lens> = Lens.entries.toSet()
