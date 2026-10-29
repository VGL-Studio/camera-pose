package com.vglstudio.camera.pose.line.guide.core.ads

import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first

/**
 * The one send gate of every native small request (L-072): waits until the app is in [foreground] (O-05, L-A19),
 * then asks [mayRequestNow] (S-15), read when foreground comes, not when called. False means: send nothing, count
 * no failure, plan no retry. Every path calls it right before it sends: the first load of a slot, the next load
 * after a claim, the one retry of a claimer that lost, the 5 s retry with no ad, the 60 s refresh tick and the retry
 * after a failed refresh.
 */
internal suspend fun awaitSendAllowed(foreground: StateFlow<Boolean>, mayRequestNow: () -> Boolean): Boolean {
    foreground.first { it }
    return mayRequestNow()
}
