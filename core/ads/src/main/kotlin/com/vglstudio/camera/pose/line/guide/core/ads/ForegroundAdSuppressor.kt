package com.vglstudio.camera.pose.line.guide.core.ads

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Blocks the ads that show by themselves (resume ad, T-025) while any source holds it (G-01): a counter, not a flag,
 * so one source releasing never clears the hold of another one, and the count never goes below zero. Main thread only.
 */
internal class ForegroundAdSuppressor {
    private var count = 0
    private val _suppressed = MutableStateFlow(false)

    /** True while at least one [Hold] is not released. */
    val suppressed: StateFlow<Boolean> = _suppressed.asStateFlow()

    /** Starts a hold for [source] (a name for the log, such as `consent_form`). */
    fun acquire(source: String): Hold {
        count++
        _suppressed.value = true
        return Hold(source)
    }

    /** One hold of [source]; only its first [release] counts. */
    inner class Hold internal constructor(val source: String) {
        private var released = false

        fun release() {
            if (released) return
            released = true
            count = (count - 1).coerceAtLeast(0)
            _suppressed.value = count > 0
        }
    }
}
