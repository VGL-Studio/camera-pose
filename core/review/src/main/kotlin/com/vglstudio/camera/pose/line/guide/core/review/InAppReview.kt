package com.vglstudio.camera.pose.line.guide.core.review

import android.app.Activity
import com.vglstudio.camera.pose.line.guide.core.config.FeatureFlag
import com.vglstudio.camera.pose.line.guide.core.config.FeatureFlags
import com.vglstudio.camera.pose.line.guide.core.config.RemoteSettings
import com.vglstudio.camera.pose.line.guide.core.logging.AppLogger
import com.vglstudio.camera.pose.line.guide.core.review.di.ReviewScope
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * Asks for a store review when the rules allow it (spec camera, KB-10). Features reach it through
 * [rememberRequestInAppReview], never directly.
 */
interface InAppReview {
    /** Returns at once; the request goes on in the scope of the app, so it outlives the screen that asked. */
    fun requestIfDue(activity: Activity, savedPhotos: Int)
}

@Singleton
internal class DefaultInAppReview @Inject constructor(
    private val prompt: ReviewPrompt,
    private val flags: FeatureFlags,
    private val settings: RemoteSettings,
    private val history: ReviewHistory,
    private val clock: ReviewClock,
    private val logger: AppLogger,
    @param:ReviewScope private val scope: CoroutineScope,
) : InAppReview {
    override fun requestIfDue(activity: Activity, savedPhotos: Int) {
        scope.launch { ask(activity, savedPhotos) }
    }

    /** True when the review flow ran to its end without an error. Never throws, so the app never stops for it. */
    suspend fun ask(activity: Activity, savedPhotos: Int): Boolean {
        try {
            // A decision at one moment, not a screen that follows the flag (ADR-0003).
            if (!flags.enabled(FeatureFlag.IN_APP_REVIEW).first()) {
                logger.info(LOG_TAG, "Review skipped: flag_off")
                return false
            }
            val minSaved = settings.value(ReviewSettings.MIN_SAVED_PHOTOS_KEY).first()
            val minDays = settings.value(ReviewSettings.MIN_DAYS_BETWEEN_KEY).first()
            if (!isReviewDue(savedPhotos, minSaved, history.lastAskedAt(), clock.nowMillis(), minDays)) {
                logger.info(LOG_TAG, "Review skipped: not_due saved=$savedPhotos")
                return false
            }
            prompt.show(activity)
            history.recordAsked(clock.nowMillis())
            logger.info(LOG_TAG, "Review asked")
            return true
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            // For example a device without Google Play. Not recorded, so the next break tries again.
            logger.warn(LOG_TAG, "Review failed", e)
            return false
        }
    }

    private companion object {
        const val LOG_TAG = "InAppReview"
    }
}
