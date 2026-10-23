package com.vglstudio.camera.pose.line.guide.feature.camera.photo

import android.content.ContentProvider
import android.content.ContentValues
import android.database.Cursor
import android.net.Uri
import android.os.Environment
import android.provider.MediaStore
import android.system.ErrnoException
import android.system.OsConstants
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import java.io.IOException
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.shadows.ShadowStatFs

/** Robolectric to build ErrnoException, an Android class, and to stand in for MediaStore. */
@RunWith(AndroidJUnit4::class)
class PhotoStoreTest {
    @get:Rule
    val folder = TemporaryFolder()

    @Test
    fun storageFullException_isStorageFull() {
        assertTrue(isStorageFull(StorageFullException()))
    }

    @Test
    fun enospcMessage_isStorageFull() {
        assertTrue(isStorageFull(IOException("write failed: ENOSPC (No space left on device)")))
    }

    @Test
    fun errnoEnospc_isStorageFull_alsoWrappedTwice() {
        assertTrue(isStorageFull(IOException(ErrnoException("write", OsConstants.ENOSPC))))
        assertTrue(isStorageFull(IOException(IOException(ErrnoException("write", OsConstants.ENOSPC)))))
    }

    @Test
    fun otherError_isNotStorageFull() {
        assertFalse(isStorageFull(IOException("EACCES")))
        // Control: the same error wrapped in a storage-full error is recognised, so false comes from the error.
        assertTrue(isStorageFull(StorageFullException(IOException("EACCES"))))
    }

    // MediaStore may throw unchecked errors from insert or update; save reports them as IOException, the only error
    // the review screen catches. SDK 35, so the Android 10 and higher branch.

    @Test
    fun save_insertThrowsIllegalState_isIOException() {
        assertSaveFailsWithIOException(IllegalStateException("volume not mounted"))
    }

    @Test
    fun save_insertThrowsIllegalArgument_isIOException() {
        assertSaveFailsWithIOException(IllegalArgumentException("unknown uri"))
    }

    private fun assertSaveFailsWithIOException(error: RuntimeException) {
        ThrowingMediaProvider.error = error
        Robolectric.setupContentProvider(ThrowingMediaProvider::class.java, MediaStore.AUTHORITY)
        @Suppress("DEPRECATION")
        val volume = Environment.getExternalStorageDirectory()
        ShadowStatFs.registerStats(volume, BLOCKS, BLOCKS, BLOCKS)
        val file = folder.newFile("capture_1.jpg").apply { writeBytes(ByteArray(16)) }
        val store = MediaStorePhotoStore(ApplicationProvider.getApplicationContext())

        val thrown = runCatching { runBlocking { store.save(file, 0L) } }.exceptionOrNull()

        assertTrue("thrown: $thrown", thrown is IOException)
        assertFalse("not storage full: $thrown", thrown is StorageFullException)
    }

    private companion object {
        const val BLOCKS = 100_000
    }
}

/** A MediaStore whose writes fail with [error]. */
class ThrowingMediaProvider : ContentProvider() {
    override fun onCreate(): Boolean = true

    override fun insert(uri: Uri, values: ContentValues?): Uri = throw error

    override fun update(uri: Uri, values: ContentValues?, selection: String?, selectionArgs: Array<out String>?): Int =
        throw error

    override fun query(
        uri: Uri,
        projection: Array<out String>?,
        selection: String?,
        selectionArgs: Array<out String>?,
        sortOrder: String?,
    ): Cursor? = null

    override fun delete(uri: Uri, selection: String?, selectionArgs: Array<out String>?): Int = 0

    override fun getType(uri: Uri): String? = null

    companion object {
        var error: RuntimeException = IllegalStateException()
    }
}
