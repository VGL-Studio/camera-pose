package com.vglstudio.camera.pose.line.guide.core.assets

import java.io.IOException

/** Why a file could not be loaded, so callers can react differently (for example show a network dialog). */
enum class AssetFailureReason {
    /** The device could not reach the server. */
    NETWORK,

    /** The server answered that the file does not exist (HTTP 404). */
    NOT_FOUND,

    /** Any other failure. */
    OTHER,
}

internal fun IOException.toFailureReason(): AssetFailureReason = AssetFailureReason.OTHER
