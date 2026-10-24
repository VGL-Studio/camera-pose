package com.vglstudio.camera.pose.line.guide.core.review.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import com.google.android.play.core.review.ReviewManager
import com.google.android.play.core.review.ReviewManagerFactory
import com.vglstudio.camera.pose.line.guide.core.logging.AppLogger
import com.vglstudio.camera.pose.line.guide.core.review.DataStoreReviewHistory
import com.vglstudio.camera.pose.line.guide.core.review.DefaultInAppReview
import com.vglstudio.camera.pose.line.guide.core.review.InAppReview
import com.vglstudio.camera.pose.line.guide.core.review.PlayReviewPrompt
import com.vglstudio.camera.pose.line.guide.core.review.ReviewClock
import com.vglstudio.camera.pose.line.guide.core.review.ReviewHistory
import com.vglstudio.camera.pose.line.guide.core.review.ReviewPrompt
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Qualifier
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

/** Scope of the review requests, which go on after the screen that asked is closed. */
@Qualifier
@Retention(AnnotationRetention.BINARY)
internal annotation class ReviewScope

@Module
@InstallIn(SingletonComponent::class)
internal object ReviewModule {
    @Provides
    @Singleton
    fun reviewManager(@ApplicationContext context: Context): ReviewManager = ReviewManagerFactory.create(context)

    @Provides
    fun prompt(manager: ReviewManager): ReviewPrompt = PlayReviewPrompt(manager)

    @Provides
    @Singleton
    fun history(dataStore: DataStore<Preferences>, logger: AppLogger): ReviewHistory =
        DataStoreReviewHistory(dataStore, logger)

    // Wall-clock time: the 30 days between two requests must survive a restart of the device.
    @Provides
    fun clock(): ReviewClock = ReviewClock { System.currentTimeMillis() }

    // The review flow needs the main thread.
    @Provides
    @Singleton
    @ReviewScope
    fun scope(): CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    @Provides
    @Singleton
    fun inAppReview(impl: DefaultInAppReview): InAppReview = impl
}
