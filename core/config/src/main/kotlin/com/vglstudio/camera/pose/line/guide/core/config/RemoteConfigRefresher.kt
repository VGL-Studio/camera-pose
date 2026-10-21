package com.vglstudio.camera.pose.line.guide.core.config

import com.google.android.gms.tasks.Task
import com.google.firebase.FirebaseApp
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.google.firebase.remoteconfig.FirebaseRemoteConfigFetchThrottledException
import com.vglstudio.camera.pose.line.guide.core.logging.AppLogger
import java.io.IOException
import javax.inject.Inject
import kotlin.coroutines.cancellation.CancellationException
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull

/**
 * Fetches the latest Remote Config values from the server and activates them (ADR-0018).
 * Only the loading screen may use this to block the UI; every other screen reads what is already there (ADR-0012).
 */
interface RemoteConfigRefresher {
    /** Returns true when fresh values are active. Never throws, except for cancellation. */
    suspend fun refresh(): Boolean
}

/** The decisions of a refresh, apart from Firebase, so they can be tested on the JVM. */
internal class RemoteConfigRefreshPolicy(
    private val isFakeConfig: () -> Boolean,
    private val fetchAndActivate: suspend () -> Unit,
    private val logger: AppLogger,
    private val timeoutMillis: Long = REFRESH_TIMEOUT_MILLIS,
) {
    suspend fun refresh(): Boolean {
        return try {
            if (isFakeConfig()) {
                logger.info(LOG_TAG, "Placeholder Firebase config, refresh counted as done")
                return true
            }
            val done = withTimeoutOrNull(timeoutMillis) {
                fetchAndActivate()
                true
            }
            if (done == null) logger.warn(LOG_TAG, "Refresh timed out after $timeoutMillis ms")
            done ?: false
        } catch (e: CancellationException) {
            throw e
        } catch (e: FirebaseRemoteConfigFetchThrottledException) {
            // The server answered a moment ago, so the active values are recent (ADR-0018).
            logger.info(LOG_TAG, "Refresh throttled by Firebase, keeping the recent values")
            true
        } catch (e: Exception) {
            logger.warn(LOG_TAG, "Refresh failed", e)
            false
        }
    }

    internal companion object {
        const val REFRESH_TIMEOUT_MILLIS = 10_000L
        const val LOG_TAG = "RemoteConfig"
    }
}

/** Build variants that use the placeholder Firebase file have this project id (ADR-0012). */
private const val FAKE_PROJECT_ID = "fake-staging-project"

internal class FirebaseRemoteConfigRefresher @Inject constructor(logger: AppLogger) : RemoteConfigRefresher {
    private val policy = RemoteConfigRefreshPolicy(
        isFakeConfig = { FirebaseApp.getInstance().options.projectId == FAKE_PROJECT_ID },
        fetchAndActivate = {
            val remoteConfig = FirebaseRemoteConfig.getInstance()
            // Minimum interval 0: always ask the server, never the cache (ADR-0018).
            remoteConfig.fetch(0).await()
            remoteConfig.activate().await()
        },
        logger = logger,
    )

    override suspend fun refresh(): Boolean = policy.refresh()
}

private suspend fun <T> Task<T>.await(): T = suspendCancellableCoroutine { continuation ->
    addOnSuccessListener { continuation.resume(it) }
    addOnFailureListener { continuation.resumeWithException(it) }
    // A cancelled Task is a failed fetch, not a cancelled caller: report it as an error.
    addOnCanceledListener { continuation.resumeWithException(IOException("Remote Config task was cancelled")) }
}
