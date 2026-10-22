package com.vglstudio.camera.pose.line.guide.core.network

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

/** What other modules may know about the network dialog (spec network). */
interface NetworkStatus {
    /** True while the network dialog covers the screen: a camera countdown stops, no full-screen ad shows. */
    val dialogShown: StateFlow<Boolean>

    /** Emits once each time "Retry" closes the dialog; a screen with a failed load loads it again. */
    val recovered: Flow<Unit>
}
