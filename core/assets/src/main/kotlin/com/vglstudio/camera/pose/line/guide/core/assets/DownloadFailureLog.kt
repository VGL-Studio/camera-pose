package com.vglstudio.camera.pose.line.guide.core.assets

import com.vglstudio.camera.pose.line.guide.core.logging.AppLogger
import java.io.IOException

/** "Could not download <path> (reason=NETWORK, error=IOException <- UnknownHostException)", plus ", status=404". */
@Suppress("UNUSED_PARAMETER")
internal fun downloadFailureMessage(path: AssetPath, error: IOException): String = ""

/** Logs a failed download as a warning under the tag "Assets" (ADR-0019). */
@Suppress("UNUSED_PARAMETER")
internal fun downloadFailureLogger(logger: AppLogger): (AssetPath, IOException) -> Unit = { _, _ -> }
