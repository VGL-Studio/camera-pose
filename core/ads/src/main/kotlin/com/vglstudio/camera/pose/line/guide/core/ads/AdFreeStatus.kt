package com.vglstudio.camera.pose.line.guide.core.ads

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Whether the user bought ad removal (KB-30, question 39 of the ads spec), given by the app through Hilt. The app
 * owns the purchase and where it is kept; :core:ads only reads [adFree] and never stores it. True works like the
 * master switch `is_show_ads` off: no new load, slots on screen drop their ads at once, no startup or resume ad,
 * held actions run at once, no opportunity. A StateFlow, so :core:ads never waits for a first value (startup).
 */
interface AdFreeStatus {
    val adFree: StateFlow<Boolean>

    companion object {
        /** The template has no purchase: never ad-free. */
        val NONE: AdFreeStatus = object : AdFreeStatus {
            override val adFree: StateFlow<Boolean> = MutableStateFlow(false).asStateFlow()
        }
    }
}
