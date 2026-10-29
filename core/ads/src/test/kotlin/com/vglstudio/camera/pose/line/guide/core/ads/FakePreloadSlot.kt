package com.vglstudio.camera.pose.line.guide.core.ads

/**
 * A slot of the preload plan for tests: records the time of each [loadIfNeeded] in [loadCalls] (and its format in
 * the shared [order], if any) and reads as settled from [settleAt] on; null never settles.
 */
internal class FakePreloadSlot(
    override val format: AdFormat,
    private val now: () -> Long,
    var settleAt: Long? = null,
    private val order: MutableList<AdFormat>? = null,
) : PreloadSlot {
    val loadCalls = mutableListOf<Long>()

    override fun loadIfNeeded() {
        loadCalls += now()
        order?.add(format)
    }

    override fun settled(): Boolean = settleAt?.let { now() >= it } ?: false
}
