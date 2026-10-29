package com.vglstudio.camera.pose.line.guide.core.ads

/** A break gate for tests: gives a ticket when [allow], records every break and every ticket shown. */
internal class RecordingBreakGate(var allow: Boolean) : InterstitialBreakGate {
    val breaks = mutableListOf<AdPlacement>()
    val shown = mutableListOf<BreakTicket>()

    override suspend fun onBreak(placement: AdPlacement): BreakTicket? {
        breaks += placement
        return if (allow) BreakTicket(placement, breaks.count { it == placement }) else null
    }

    override fun onShown(ticket: BreakTicket) {
        shown += ticket
    }
}
