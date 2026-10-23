package com.vglstudio.camera.pose.line.guide.feature.camera.photo

import android.content.Context
import androidx.compose.ui.graphics.ImageBitmap
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

/** Reads a photo to show on screen, scaled down. */
interface PhotoDecoder {
    /** [location] is a file path or a `content://` uri. Null when the photo cannot be read. */
    suspend fun decode(location: String, maxSidePx: Int): ImageBitmap?
}

internal class AndroidPhotoDecoder @Inject constructor(@ApplicationContext private val context: Context) :
    PhotoDecoder {
    override suspend fun decode(location: String, maxSidePx: Int): ImageBitmap? = null
}
