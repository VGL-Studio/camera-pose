package com.vglstudio.camera.pose.line.guide.feature.camera.pose

import android.graphics.BitmapFactory
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import java.io.File
import javax.inject.Inject
import kotlin.math.min
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Turns a pose image file into a bitmap no smaller than [targetPx] on its short side; null when it cannot be read. */
fun interface PoseImageDecoder {
    suspend fun decode(file: File, targetPx: Int): ImageBitmap?
}

/**
 * Decodes with BitmapFactory (WebP from API 26), halving the image while its short side stays at least [targetPx],
 * so a 1080 px outline or a 256 px thumbnail does not keep a full size bitmap in memory (plan T-014).
 */
internal class BitmapPoseImageDecoder @Inject constructor() : PoseImageDecoder {
    override suspend fun decode(file: File, targetPx: Int): ImageBitmap? = withContext(Dispatchers.IO) {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(file.path, bounds)
        val shortSide = min(bounds.outWidth, bounds.outHeight)
        if (shortSide <= 0) return@withContext null
        var sampleSize = 1
        while (shortSide / (sampleSize * 2) >= targetPx) sampleSize *= 2
        val options = BitmapFactory.Options().apply { inSampleSize = sampleSize }
        BitmapFactory.decodeFile(file.path, options)?.asImageBitmap()
    }
}
