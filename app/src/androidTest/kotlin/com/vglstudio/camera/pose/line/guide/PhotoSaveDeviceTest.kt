package com.vglstudio.camera.pose.line.guide

import android.content.ContentUris
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.net.Uri
import android.provider.MediaStore
import androidx.core.content.FileProvider
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SdkSuppress
import com.vglstudio.camera.pose.line.guide.feature.camera.photo.MediaStorePhotoStore
import java.io.File
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Saving to the real gallery through MediaStore, and sharing the cache file through the FileProvider of
 * :feature:camera (spec camera, KB-6; plan T-016). Runs on the CI emulator (API 34) only. No camera, no screen.
 */
@RunWith(AndroidJUnit4::class)
@SdkSuppress(minSdkVersion = 29)
class PhotoSaveDeviceTest {
    private val context: Context = ApplicationProvider.getApplicationContext()
    private val saved = mutableListOf<Uri>()

    @After
    fun tearDown() {
        saved.forEach { context.contentResolver.delete(it, null, null) }
    }

    private fun captureFile(): File {
        val dir = File(context.cacheDir, "captures").apply { mkdirs() }
        val file = File(dir, "capture_device_test.jpg")
        val bitmap = Bitmap.createBitmap(40, 30, Bitmap.Config.ARGB_8888).apply { eraseColor(Color.GRAY) }
        file.outputStream().use { bitmap.compress(Bitmap.CompressFormat.JPEG, 90, it) }
        return file
    }

    @Test
    fun save_insertsIntoAppAlbum() = runBlocking {
        val file = captureFile()
        val store = MediaStorePhotoStore(context)

        val uri = Uri.parse(store.save(file, 0L))
        saved += uri

        val projection = arrayOf(
            MediaStore.Images.Media.RELATIVE_PATH,
            MediaStore.Images.Media.MIME_TYPE,
            MediaStore.Images.Media.IS_PENDING,
        )
        context.contentResolver.query(uri, projection, null, null, null).use { cursor ->
            assertNotNull(cursor)
            assertTrue(cursor!!.moveToFirst())
            assertEquals("Pictures/Camera Pose/", cursor.getString(0))
            assertEquals("image/jpeg", cursor.getString(1))
            assertEquals(0, cursor.getInt(2))
        }
        val bytes = context.contentResolver.openInputStream(uri)!!.use { it.readBytes() }
        assertEquals(file.length().toInt(), bytes.size)
        assertTrue(ContentUris.parseId(uri) > 0)
        assertTrue(store.exists(uri.toString()))

        context.contentResolver.delete(uri, null, null)
        saved.clear()
        assertFalse(store.exists(uri.toString()))
    }

    @Test
    fun cacheCapture_readableThroughFileProvider() {
        val file = captureFile()
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.captures", file)

        val bytes = context.contentResolver.openInputStream(uri)!!.use { it.readBytes() }

        assertEquals(file.length().toInt(), bytes.size)
    }
}
