package com.vglstudio.camera.pose.line.guide.feature.camera.pose

import androidx.compose.ui.graphics.ImageBitmap
import java.io.File
import javax.inject.Inject

/** Turns a pose image file into a bitmap no smaller than [targetPx] on its short side; null when it cannot be read. */
fun interface PoseImageDecoder {
    suspend fun decode(file: File, targetPx: Int): ImageBitmap?
}

internal class BitmapPoseImageDecoder @Inject constructor() : PoseImageDecoder {
    override suspend fun decode(file: File, targetPx: Int): ImageBitmap? = null
}
