package com.vglstudio.camera.pose.line.guide.feature.poses.data

import androidx.compose.ui.graphics.ImageBitmap
import java.io.File
import javax.inject.Inject

/** Turns a thumbnail file into a bitmap at least [targetWidthPx] wide when the file is; null when it cannot be read. */
fun interface PoseImageDecoder {
    fun decode(file: File, targetWidthPx: Int): ImageBitmap?
}

// Step 3 skeleton (plan T-021).
internal class BitmapPoseImageDecoder @Inject constructor() : PoseImageDecoder {
    override fun decode(file: File, targetWidthPx: Int): ImageBitmap? = null
}
