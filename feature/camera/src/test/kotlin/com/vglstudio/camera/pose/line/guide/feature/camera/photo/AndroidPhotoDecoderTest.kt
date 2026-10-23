package com.vglstudio.camera.pose.line.guide.feature.camera.photo

import android.graphics.Bitmap
import android.graphics.Color
import android.media.ExifInterface
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import java.io.File
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.annotation.GraphicsMode

/** Reading the photo to show: scaled down, turned upright by its EXIF orientation (plan T-016). */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class AndroidPhotoDecoderTest {
    @get:Rule
    val folder = TemporaryFolder()

    private val decoder = AndroidPhotoDecoder(ApplicationProvider.getApplicationContext())

    private fun jpeg(name: String, width: Int = 1000, height: Int = 800): File {
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888).apply { eraseColor(Color.GRAY) }
        val file = folder.newFile(name)
        file.outputStream().use { bitmap.compress(Bitmap.CompressFormat.JPEG, 90, it) }
        return file
    }

    @Test
    fun decode_scalesDown() = runTest {
        val image = decoder.decode(jpeg("wide.jpg").path, maxSidePx = 300)
        assertNotNull(image)
        assertTrue("size ${image!!.width} x ${image.height}", maxOf(image.width, image.height) <= 300)
        assertTrue("wide stays wide", image.width > image.height)
    }

    @Test
    fun decode_appliesExifRotation() = runTest {
        val file = jpeg("rotated.jpg")
        ExifInterface(file.path).apply {
            setAttribute(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_ROTATE_90.toString())
            saveAttributes()
        }

        val image = decoder.decode(file.path, maxSidePx = 2000)

        assertNotNull(image)
        assertTrue("size ${image!!.width} x ${image.height}", image.height > image.width)
    }

    @Test
    fun decode_missingFile_returnsNull() = runTest {
        assertNull(decoder.decode(File(folder.root, "missing.jpg").path, maxSidePx = 300))
        // The same decoder reads a real photo, so null comes from the missing file.
        assertNotNull(decoder.decode(jpeg("present.jpg").path, maxSidePx = 300))
    }
}
