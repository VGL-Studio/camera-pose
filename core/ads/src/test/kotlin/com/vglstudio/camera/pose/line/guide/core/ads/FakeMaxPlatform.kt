package com.vglstudio.camera.pose.line.guide.core.ads

/**
 * [MaxPlatform] for unit tests (plan T-068): records every [initialize] with its key, and calls the callbacks only
 * when the test calls [finishInitialization], like the SDK that answers later.
 */
internal class FakeMaxPlatform(override var isInitialized: Boolean = false) : MaxPlatform {
    private val keys = mutableListOf<String>()
    private val callbacks = mutableListOf<() -> Unit>()

    /** The keys passed to [initialize], one per call. */
    val initializedKeys: List<String> get() = keys.toList()

    val calls: Int get() = keys.size

    override fun initialize(sdkKey: String, onInitialized: () -> Unit) {
        keys += sdkKey
        callbacks += onInitialized
    }

    fun finishInitialization() {
        callbacks.forEach { it() }
        callbacks.clear()
    }
}
