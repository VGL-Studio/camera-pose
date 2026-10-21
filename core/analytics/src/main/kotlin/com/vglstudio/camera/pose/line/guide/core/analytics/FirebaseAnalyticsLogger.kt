package com.vglstudio.camera.pose.line.guide.core.analytics

import android.content.Context
import android.os.Bundle
import com.google.firebase.analytics.FirebaseAnalytics
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

/** Sends events to Firebase Analytics (ADR-0012). Tests use a fake [Analytics] instead. */
internal class FirebaseAnalyticsLogger @Inject constructor(@param:ApplicationContext private val context: Context) :
    Analytics {
    private val firebase by lazy { FirebaseAnalytics.getInstance(context) }

    override fun log(event: AnalyticsEvent) {
        val params = Bundle().apply { event.params.forEach { (key, value) -> putString(key, value) } }
        firebase.logEvent(event.name, params)
    }
}
