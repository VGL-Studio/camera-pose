package com.vglstudio.camera.pose.line.guide.core.assets

import com.vglstudio.camera.pose.line.guide.core.logging.AppLogger
import java.io.IOException

private const val LOG_TAG = "Assets"

/** Enough to tell the cause apart, short enough for one logcat line. */
private const val MAX_CLASSES = 5

/**
 * "Could not download <path> (reason=NETWORK, error=IOException <- UnknownHostException)", plus ", status=404".
 * Class names are in the line itself because Android drops the stack trace of an UnknownHostException.
 * The error messages are left out: an HttpStatusException message holds the full address.
 */
internal fun downloadFailureMessage(path: AssetPath, error: IOException): String {
    val chain = generateSequence<Throwable>(error) { it.cause }.take(MAX_CLASSES).toList()
    val classes = chain.joinToString(" <- ") { it.javaClass.simpleName.ifEmpty { it.javaClass.name } }
    val status = chain.firstNotNullOfOrNull { it as? HttpStatusException }?.status
    val statusPart = if (status != null) ", status=$status" else ""
    return "Could not download ${path.value} (reason=${error.toFailureReason()}, error=$classes$statusPart)"
}

/** Logs a failed download as a warning under the tag "Assets" (ADR-0019). */
internal fun downloadFailureLogger(logger: AppLogger): (AssetPath, IOException) -> Unit =
    { path, error -> logger.warn(LOG_TAG, downloadFailureMessage(path, error), error) }
