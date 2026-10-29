package com.vglstudio.camera.pose.line.guide.core.ads.di

import com.vglstudio.camera.pose.line.guide.core.ads.DefaultInterstitialBreakGate
import com.vglstudio.camera.pose.line.guide.core.ads.InterstitialBreakGate
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

/** One break gate for the app: the counts of the session live in it (spec ads, question 1). */
@Module
@InstallIn(SingletonComponent::class)
internal abstract class InterstitialBreakModule {
    @Binds
    abstract fun interstitialBreakGate(impl: DefaultInterstitialBreakGate): InterstitialBreakGate
}
