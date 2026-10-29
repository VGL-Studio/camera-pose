package com.vglstudio.camera.pose.line.guide.di

import com.vglstudio.camera.pose.line.guide.core.ads.AdPlacement
import com.vglstudio.camera.pose.line.guide.core.ads.AdsSettings
import com.vglstudio.camera.pose.line.guide.core.config.LocalRemoteValue
import com.vglstudio.camera.pose.line.guide.core.config.LocalRemoteValues
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.ElementsIntoSet

/**
 * Remote Config values of staging builds only, used when Firebase has no value for the key (ADR-0023).
 * A value set on the staging Firebase project still wins, so the team can switch a placement off remotely.
 * Production builds have no such module: keys there read Firebase, then their default.
 */
@Module
@InstallIn(SingletonComponent::class)
object StagingRemoteValuesModule {
    /**
     * The placements of this app show Google test ads on staging (plan T-074, section 8, question 2). Named one by
     * one, not every placement: the gallery and sample placements of the framework stay off (question 1).
     */
    @Provides
    @ElementsIntoSet
    @LocalRemoteValues
    fun appPlacementsShown(): Set<LocalRemoteValue> = listOf(
        AdPlacement.POSES_BANNER,
        AdPlacement.POSES_DETAIL_BANNER,
        AdPlacement.REVIEW_INTERSTITIAL,
    ).map { LocalRemoteValue.of(AdsSettings.placementEnabled(it), true) }.toSet()

    /** The ad lifecycle logs are on for testers (spec ads, question 6). */
    @Provides
    @ElementsIntoSet
    @LocalRemoteValues
    fun adLogsOn(): Set<LocalRemoteValue> = listOf(
        AdsSettings.IS_LOG_AD_FULLSCREEN_EVENTS,
        AdsSettings.IS_LOG_AD_SLOT_EVENTS,
    ).map { LocalRemoteValue.of(it, true) }.toSet()
}
