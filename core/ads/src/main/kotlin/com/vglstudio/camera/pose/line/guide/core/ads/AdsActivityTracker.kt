package com.vglstudio.camera.pose.line.guide.core.ads

import android.app.Activity
import android.app.Application
import android.os.Bundle
import java.lang.ref.WeakReference

/** Which activities belong to the app. SDK activities (AdActivity, mediation adapters) are ignored by ads. */
fun interface AppActivityFilter {
    fun isAppActivity(activity: Activity): Boolean

    companion object {
        /**
         * Class name starts with the package of [anchor] plus a dot (MainActivity::class.java), so the staging suffix
         * of the application id does not matter.
         */
        fun inPackageOf(anchor: Class<*>): AppActivityFilter {
            val packageName = anchor.name.substringBeforeLast('.', missingDelimiterValue = "")
            return AppActivityFilter { isClassInPackage(it.javaClass.name, packageName) }
        }
    }
}

/** Whether [className] is in [packageName] or one of its subpackages. */
internal fun isClassInPackage(className: String, packageName: String): Boolean =
    packageName.isNotEmpty() && className.startsWith("$packageName.")

/**
 * Follows the activities of the app through the callbacks of [Application], registered once by [AdsController.start]
 * (HD-01). Activities that [filter] rejects (the SDK's AdActivity, mediation adapters) are ignored entirely: not
 * counted for foreground, never the resumed activity, never reported. The one exception is the foreground of the whole
 * process ([Listener.onProcessForegroundChanged], plan T-025), which counts every activity of the process, so a
 * full-screen ad of the SDK covering the app is not a trip to background. Main thread only.
 */
internal class AdsActivityTracker(private val filter: AppActivityFilter, private val listener: Listener) :
    Application.ActivityLifecycleCallbacks {
    /** What the tracker reports, for app activities only, but [onProcessForegroundChanged]. */
    interface Listener {
        fun onForegroundChanged(foreground: Boolean) {}

        /**
         * The process came to foreground or went to background: every activity counts, the SDK's included; a
         * configuration change (rotation) is neither ([ProcessForegroundCounter], A-10).
         */
        fun onProcessForegroundChanged(foreground: Boolean) {}

        fun onActivityResumed(activity: Activity) {}

        fun onActivityPaused(activity: Activity) {}

        fun onActivityDestroyed(activity: Activity) {}
    }

    private val started = StartedActivityCounter()
    private val process = ProcessForegroundCounter()
    private var isForeground = false
    private var resumed: WeakReference<Activity>? = null

    /** True while at least one app activity is started. */
    val foreground: Boolean get() = isForeground

    /** The app activity resumed now, or null. */
    val resumedActivity: Activity? get() = resumed?.get()

    override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {}

    override fun onActivityStarted(activity: Activity) {
        // Every activity of the process, before the filter (A-10).
        if (process.onStart()) listener.onProcessForegroundChanged(true)
        if (!filter.isAppActivity(activity)) return
        if (started.onStart()) {
            isForeground = true
            listener.onForegroundChanged(true)
        }
    }

    override fun onActivityResumed(activity: Activity) {
        if (!filter.isAppActivity(activity)) return
        resumed = WeakReference(activity)
        listener.onActivityResumed(activity)
    }

    override fun onActivityPaused(activity: Activity) {
        if (!filter.isAppActivity(activity)) return
        if (resumed?.get() === activity) resumed = null
        listener.onActivityPaused(activity)
    }

    override fun onActivityStopped(activity: Activity) {
        if (process.onStop(activity.isChangingConfigurations)) listener.onProcessForegroundChanged(false)
        if (!filter.isAppActivity(activity)) return
        if (started.onStop()) {
            isForeground = false
            listener.onForegroundChanged(false)
        }
    }

    override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {}

    override fun onActivityDestroyed(activity: Activity) {
        if (!filter.isAppActivity(activity)) return
        if (resumed?.get() === activity) resumed = null
        listener.onActivityDestroyed(activity)
    }
}
