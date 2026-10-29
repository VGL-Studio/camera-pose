package com.vglstudio.camera.pose.line.guide.di

import com.vglstudio.camera.pose.line.guide.core.ads.AdUnitIds
import com.vglstudio.camera.pose.line.guide.core.ads.ConsentDebugOptions
import com.vglstudio.camera.pose.line.guide.core.ads.ConsentShortcutConfig
import com.vglstudio.camera.pose.line.guide.core.ads.GoogleTestAdUnitIds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

/**
 * Staging shows Google's test ads only (ADR-0007) and treats every device as being in the EEA,
 * so the team can test the consent form from anywhere (ADR-0013).
 */
@Module
@InstallIn(SingletonComponent::class)
object StagingAdsModule {
    @Provides
    fun adUnitIds(): AdUnitIds = GoogleTestAdUnitIds

    @Provides
    fun consentDebugOptions(): ConsentDebugOptions = ConsentDebugOptions.EEA_TEST_DEVICE

    /**
     * The build half of the consent shortcut is on, so testers can try CACHED_EARLY and ERROR_ALLOWED by switching
     * is_enable_consent_shortcut on in the staging Firebase project; the remote key stays off by default (S-12).
     */
    @Provides
    fun consentShortcutConfig(): ConsentShortcutConfig = ConsentShortcutConfig(enabledInBuild = true)
}
