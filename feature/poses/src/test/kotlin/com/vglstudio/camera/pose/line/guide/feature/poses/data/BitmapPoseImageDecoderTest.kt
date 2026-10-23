package com.vglstudio.camera.pose.line.guide.feature.poses.data

import android.graphics.Bitmap
import androidx.test.ext.junit.runners.AndroidJUnit4
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.annotation.GraphicsMode

/** Real decoding with BitmapFactory (native graphics of Robolectric). */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class BitmapPoseImageDecoderTest {
    @get:Rule
    val folder = TemporaryFolder()

    @Test
    fun largeImage_isSampledDownButAtLeastTargetWidth() {
        val file = png(width = 1944, height = 2520)
        val image = BitmapPoseImageDecoder().decode(file, targetWidthPx = 486)
        assertNotNull(image)
        assertEquals(486, image!!.width)
        assertEquals(630, image.height)
        // A small image keeps its size.
        val small = BitmapPoseImageDecoder().decode(png(width = 540, height = 700), targetWidthPx = 486)
        assertEquals(540, small!!.width)
    }

    @Test
    fun corruptFile_returnsNull() {
        val file = folder.newFile("broken.webp").apply { writeText("not an image") }
        assertNull(BitmapPoseImageDecoder().decode(file, targetWidthPx = 486))
    }

    private fun png(width: Int, height: Int): File {
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val file = folder.newFile("image-${width}x$height.png")
        file.outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
        return file
    }
}
