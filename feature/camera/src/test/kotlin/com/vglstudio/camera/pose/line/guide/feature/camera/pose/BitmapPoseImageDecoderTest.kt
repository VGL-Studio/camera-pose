package com.vglstudio.camera.pose.line.guide.feature.camera.pose

import android.graphics.Bitmap
import android.graphics.Color
import androidx.test.ext.junit.runners.AndroidJUnit4
import java.io.File
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.annotation.GraphicsMode

/** Decoding pose images with BitmapFactory, scaled down by powers of two (plan T-014). */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class BitmapPoseImageDecoderTest {
    @get:Rule
    val folder = TemporaryFolder()

    private val decoder = BitmapPoseImageDecoder()

    private fun png(width: Int, height: Int): File {
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888).apply { eraseColor(Color.WHITE) }
        val file = folder.newFile("image_${width}x$height.png")
        file.outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
        return file
    }

    @Test
    fun decode_scalesDownToTarget() = runTest {
        val image = decoder.decode(png(1024, 2048), targetPx = 256)
        assertNotNull(image)
        // Halved while the short side stays at least 256: 1024 -> 512 -> 256.
        assertTrue("width ${image!!.width}", image.width == 256 || image.width == 512)
        assertEquals(image.width * 2, image.height)
    }

    @Test
    fun decode_smallImage_keepsSize() = runTest {
        val image = decoder.decode(png(100, 150), targetPx = 256)
        assertEquals(100, image?.width)
        assertEquals(150, image?.height)
    }

    @Test
    fun decode_notAnImage_returnsNull() = runTest {
        val text = folder.newFile("outline.webp").apply { writeText("not an image") }
        assertNull(decoder.decode(text, targetPx = 256))
        // The same decoder reads a real image, so null comes from the content.
        assertNotNull(decoder.decode(png(10, 10), targetPx = 256))
    }
}
