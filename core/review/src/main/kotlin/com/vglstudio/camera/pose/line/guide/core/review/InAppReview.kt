package com.vglstudio.camera.pose.line.guide.core.review

import android.app.Activity
import com.vglstudio.camera.pose.line.guide.core.config.FeatureFlags
import com.vglstudio.camera.pose.line.guide.core.config.RemoteSettings
import com.vglstudio.camera.pose.line.guide.core.logging.AppLogger
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/** Asks for a store review when the rules allow it. Returns at once; the work goes on in the app scope. */
interface InAppReview {
    fun requestIfDue(activity: Activity, savedPhotos: Int)
}

/** Placeholder until the code step (plan T-019). */
@Singleton
@Suppress("UnusedPrivateProperty")
internal class DefaultInAppReview @Inject constructor(
    private val prompt: ReviewPrompt,
    private val flags: FeatureFlags,
    private val settings: RemoteSettings,
    private val history: ReviewHistory,
    private val clock: ReviewClock,
    private val logger: AppLogger,
    private val scope: CoroutineScope,
) : InAppReview {
    override fun requestIfDue(activity: Activity, savedPhotos: Int) {
        scope.launch { ask(activity, savedPhotos) }
    }

    @Suppress("UNUSED_PARAMETER")
    suspend fun ask(activity: Activity, savedPhotos: Int): Boolean = false
}
