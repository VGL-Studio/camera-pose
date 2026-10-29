package com.vglstudio.camera.pose.line.guide

import android.app.Application
import com.vglstudio.camera.pose.line.guide.core.ads.AdsController
import com.vglstudio.camera.pose.line.guide.core.ads.AppActivityFilter
import com.vglstudio.camera.pose.line.guide.core.analytics.AppsFlyerStartup
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class TemplateApplication : Application() {
    @Inject
    lateinit var ads: AdsController

    @Inject
    lateinit var appsFlyer: AppsFlyerStartup

    override fun onCreate() {
        super.onCreate()
        // AppsFlyer is not in this app (spec ads, question 3): with no key it never starts and sends nothing.
        appsFlyer.prepare()
        // :core:ads starts once, before any screen (HD-01). Only activities in the package of MainActivity count as the
        // app's; the SDK's own activities are ignored.
        ads.start(this, AppActivityFilter.inPackageOf(MainActivity::class.java))
    }
}
