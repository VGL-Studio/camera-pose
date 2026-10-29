package com.vglstudio.camera.pose.line.guide.feature.loading

import com.vglstudio.camera.pose.line.guide.core.ads.StartupAdTurn
import com.vglstudio.camera.pose.line.guide.core.ads.StartupAds
import kotlinx.coroutines.CompletableDeferred

/**
 * The startup ad of :core:ads for the tests of this module (plan T-074, pass 2): returns [result] when it completes
 * (null at once by default) and counts the calls; throws [error] when it is set.
 */
internal class FakeStartupAds : StartupAds {
    var calls = 0

    // Named argument: CompletableDeferred(null) is the overload with a null parent job, which never completes.
    var result: CompletableDeferred<StartupAdTurn?> = CompletableDeferred(value = null)
    var error: Throwable? = null

    override suspend fun awaitStartupAd(): StartupAdTurn? {
        calls++
        error?.let { throw it }
        return result.await()
    }
}
