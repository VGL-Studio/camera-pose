package com.vglstudio.camera.pose.line.guide.feature.poses.data

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import java.io.File
import javax.inject.Inject

/** Turns a thumbnail file into a bitmap at least [targetWidthPx] wide when the file is; null when it cannot be read. */
fun interface PoseImageDecoder {
    fun decode(file: File, targetWidthPx: Int): ImageBitmap?
}

/**
 * Decodes with BitmapFactory (WebP from API 26): halves the image while it stays at least [targetWidthPx] wide, in
 * RGB_565 since thumbnails need no transparency, so only small bitmaps are kept for the cards on screen (plan T-021).
 */
internal class BitmapPoseImageDecoder @Inject constructor() : PoseImageDecoder {
    override fun decode(file: File, targetWidthPx: Int): ImageBitmap? {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(file.path, bounds)
        val width = bounds.outWidth
        if (width <= 0 || bounds.outHeight <= 0) return null
        var sampleSize = 1
        while (width / (sampleSize * 2) >= targetWidthPx) sampleSize *= 2
        val options = BitmapFactory.Options().apply {
            inSampleSize = sampleSize
            inPreferredConfig = Bitmap.Config.RGB_565
        }
        return BitmapFactory.decodeFile(file.path, options)?.asImageBitmap()
    }
}
