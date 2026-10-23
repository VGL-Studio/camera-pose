package com.vglstudio.camera.pose.line.guide.feature.camera.photo

import android.system.ErrnoException
import android.system.OsConstants
import androidx.test.ext.junit.runners.AndroidJUnit4
import java.io.IOException
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/** Robolectric only to build ErrnoException, an Android class. */
@RunWith(AndroidJUnit4::class)
class PhotoStoreTest {
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
}
