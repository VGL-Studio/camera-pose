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
     * one, not every placement: the gallery placements of the framework stay off (question 1). The onboarding card
     * is on as at release (spec onboarding, section 8; plan T-083); `show_open_ads_first_open` stays unset, so the
     * first open still has no full-screen ad (KB-1).
     */
    @Provides
    @ElementsIntoSet
    @LocalRemoteValues
    fun appPlacementsShown(): Set<LocalRemoteValue> = listOf(
        AdPlacement.POSES_BOTTOM,
        AdPlacement.POSE_DETAIL_BOTTOM,
        AdPlacement.INTER_POSE_DETAIL,
        AdPlacement.REVIEW_INTERSTITIAL,
        // The card of the pose library grid (spec ads, question 11; plan T-080).
        AdPlacement.POSES_IN_COLLECTION,
        // The unlock of a premium pose (spec poses, section 8; plan T-086).
        AdPlacement.RW_PREMIUM_POSE,
        // The step card of the onboarding flow, first open only (spec onboarding, section 8; plan T-083).
        AdPlacement.NATIVE_ONBOARDING,
    ).map { LocalRemoteValue.of(AdsSettings.placementEnabled(it), true) }.toSet()

    /**
     * The app open ad from the second open and the resume ad on every screen, the ad lifecycle logs for testers (spec
     * ads, question 6; plan T-074, pass 2), and the native slot of the bottom bars (spec ads, question 11; plan T-079).
     * Not set, so off as their default: show_open_ads_first_open, is_show_native_after_inter.
     */
    @Provides
    @ElementsIntoSet
    @LocalRemoteValues
    fun globalAdSwitchesOn(): Set<LocalRemoteValue> = listOf(
        AdsSettings.SHOW_OPEN_ADS,
        AdsSettings.SHOW_RESUM_ADS,
        AdsSettings.IS_SHOW_NATIVE_BOTTOM,
        AdsSettings.IS_LOG_AD_FULLSCREEN_EVENTS,
        AdsSettings.IS_LOG_AD_SLOT_EVENTS,
    ).map { LocalRemoteValue.of(it, true) }.toSet()

    /**
     * The collapsible layer over the bottom bars stays off, as on release (spec ads, questions 10 and 11; plan T-079):
     * its default in code is on.
     */
    @Provides
    @ElementsIntoSet
    @LocalRemoteValues
    fun globalAdSwitchesOff(): Set<LocalRemoteValue> =
        setOf(LocalRemoteValue.of(AdsSettings.IS_SHOW_NATIVE_COLLAP, false))
}
