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

    /**
     * The app open ad from the second open and the resume ad on every screen, and the ad lifecycle logs for testers
     * (spec ads, question 6; plan T-074, pass 2). Not set, so off as their default: show_open_ads_first_open,
     * is_show_native_after_inter, is_show_native_bottom.
     */
    @Provides
    @ElementsIntoSet
    @LocalRemoteValues
    fun globalAdSwitchesOn(): Set<LocalRemoteValue> = listOf(
        AdsSettings.SHOW_OPEN_ADS,
        AdsSettings.SHOW_RESUM_ADS,
        AdsSettings.IS_LOG_AD_FULLSCREEN_EVENTS,
        AdsSettings.IS_LOG_AD_SLOT_EVENTS,
    ).map { LocalRemoteValue.of(it, true) }.toSet()

    // Skeleton of plan T-079 for the tests written first.
    @Provides
    @ElementsIntoSet
    @LocalRemoteValues
    fun globalAdSwitchesOff(): Set<LocalRemoteValue> = emptySet()
}
