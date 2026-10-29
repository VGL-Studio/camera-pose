package com.vglstudio.camera.pose.line.guide.core.ads

import android.app.Activity
import android.content.Context

/**
 * The context a full-screen ad is loaded with (question 4 of plan T-034): the resumed activity of the app, so
 * networks that need one (Unity, ironSource) bid too, unless it is finishing or destroyed; else [application].
 * [resumed] comes from [AdsActivityTracker], which never reports an SDK activity. Not kept after the call.
 */
internal fun fullScreenLoadContext(resumed: Activity?, application: Context): Context =
    resumed?.takeUnless { it.isFinishing || it.isDestroyed } ?: application
