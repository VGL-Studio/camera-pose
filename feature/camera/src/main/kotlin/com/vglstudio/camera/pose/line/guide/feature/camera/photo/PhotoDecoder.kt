package com.vglstudio.camera.pose.line.guide.feature.camera.photo

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.io.FileInputStream
import java.io.IOException
import java.io.InputStream
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Reads a photo to show on screen, scaled down. */
interface PhotoDecoder {
    /** [location] is a file path or a `content://` uri. Null when the photo cannot be read. */
    suspend fun decode(location: String, maxSidePx: Int): ImageBitmap?
}

/**
 * Decodes with BitmapFactory at a power-of-two sample size so the long side is at most maxSidePx, then turns the
 * bitmap upright by its EXIF orientation (android.media.ExifInterface, part of the system since API 24).
 */
internal class AndroidPhotoDecoder @Inject constructor(@ApplicationContext private val context: Context) :
    PhotoDecoder {
    override suspend fun decode(location: String, maxSidePx: Int): ImageBitmap? = withContext(Dispatchers.IO) {
        try {
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            open(location).use { BitmapFactory.decodeStream(it, null, bounds) }
            if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return@withContext null
            val options = BitmapFactory.Options().apply {
                inSampleSize = sampleSize(maxOf(bounds.outWidth, bounds.outHeight), maxSidePx)
            }
            val bitmap = open(location).use { BitmapFactory.decodeStream(it, null, options) }
                ?: return@withContext null
            val orientation = open(location).use {
                ExifInterface(it).getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)
            }
            upright(bitmap, orientation).asImageBitmap()
        } catch (e: IOException) {
            null
        } catch (e: SecurityException) {
            null
        } catch (e: IllegalArgumentException) {
            null
        }
    }

    private fun open(location: String): InputStream = if (location.startsWith(CONTENT_SCHEME)) {
        context.contentResolver.openInputStream(Uri.parse(location)) ?: throw IOException("No stream")
    } else {
        FileInputStream(File(location))
    }

    private fun sampleSize(longSide: Int, maxSide: Int): Int {
        var size = 1
        while (longSide / size > maxSide) size *= 2
        return size
    }

    private fun upright(bitmap: Bitmap, orientation: Int): Bitmap {
        val matrix = Matrix()
        when (orientation) {
            ExifInterface.ORIENTATION_ROTATE_90 -> matrix.postRotate(90f)
            ExifInterface.ORIENTATION_ROTATE_180 -> matrix.postRotate(180f)
            ExifInterface.ORIENTATION_ROTATE_270 -> matrix.postRotate(270f)
            ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> matrix.postScale(-1f, 1f)
            ExifInterface.ORIENTATION_FLIP_VERTICAL -> matrix.postScale(1f, -1f)
            ExifInterface.ORIENTATION_TRANSPOSE -> {
                matrix.postRotate(90f)
                matrix.postScale(-1f, 1f)
            }
            ExifInterface.ORIENTATION_TRANSVERSE -> {
                matrix.postRotate(270f)
                matrix.postScale(-1f, 1f)
            }
            else -> return bitmap
        }
        val turned = Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
        if (turned !== bitmap) bitmap.recycle()
        return turned
    }

    private companion object {
        const val CONTENT_SCHEME = "content://"
    }
}
