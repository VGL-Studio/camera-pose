package com.vglstudio.camera.pose.line.guide.core.ads

/** A full-screen placement in the preload plan: loads only while switched on; settled unless it is loading (S-22). */
internal class FullScreenPreloadSlot(
    private val placement: AdPlacement,
    private val enabled: () -> Boolean,
    private val state: () -> FullScreenLoadState,
    private val load: (AdPlacement) -> Unit,
) : PreloadSlot {
    override val format: AdFormat = placement.format

    override fun loadIfNeeded() {
        if (enabled()) load(placement)
    }

    override fun settled(): Boolean = !enabled() || state() != FullScreenLoadState.Loading
}
