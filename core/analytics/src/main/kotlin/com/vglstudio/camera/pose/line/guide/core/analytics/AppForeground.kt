package com.vglstudio.camera.pose.line.guide.core.analytics

import android.app.Activity
import android.app.Application
import android.content.Context
import android.os.Bundle
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.concurrent.CopyOnWriteArrayList
import javax.inject.Inject
import javax.inject.Singleton

/** Calls a listener each time the whole process comes back to foreground, not the first time (plan T-099, step 6). */
internal interface AppForeground {
    fun onEachReturn(listener: () -> Unit)
}

/**
 * Counts the started activities of the process ([ForegroundCounter]). Registers in its constructor, made in
 * `Application.onCreate` (through TemplateApplication, the start-up of the attribution SDKs and TenjinTracker), before
 * the first activity, so an activity on screen when Tenjin starts is already counted. One instance, so one set of
 * callbacks.
 */
@Singleton
internal class ActivityCountForeground @Inject constructor(@param:ApplicationContext context: Context) :
    AppForeground,
    Application.ActivityLifecycleCallbacks {
    private val counter = ForegroundCounter()
    private val listeners = CopyOnWriteArrayList<() -> Unit>()

    init {
        (context.applicationContext as Application).registerActivityLifecycleCallbacks(this)
    }

    override fun onEachReturn(listener: () -> Unit) {
        listeners += listener
    }

    override fun onActivityStarted(activity: Activity) {
        if (counter.onActivityStarted()) listeners.forEach { it() }
    }

    override fun onActivityStopped(activity: Activity) = counter.onActivityStopped()

    override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) = Unit

    override fun onActivityResumed(activity: Activity) = Unit

    override fun onActivityPaused(activity: Activity) = Unit

    override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) = Unit

    override fun onActivityDestroyed(activity: Activity) = Unit
}
