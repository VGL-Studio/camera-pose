package com.vglstudio.camera.pose.line.guide.feature.camera.photo

import android.Manifest
import android.app.Application
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Shadows.shadowOf

/** The storage permission rule (plan T-016, version 2). Runs on SDK 35 with sdkInt passed in. */
@RunWith(AndroidJUnit4::class)
class StoragePermissionTest {
    private val application: Application = ApplicationProvider.getApplicationContext()

    @Test
    fun api28_notGranted_needsRequest() {
        assertTrue(AndroidStoragePermission(application, sdkInt = 28).needsRequest())
    }

    @Test
    fun api28_granted_doesNotNeedRequest() {
        shadowOf(application).grantPermissions(Manifest.permission.WRITE_EXTERNAL_STORAGE)
        assertFalse(AndroidStoragePermission(application, sdkInt = 28).needsRequest())
    }

    @Test
    fun api29_neverNeedsRequest() {
        assertFalse(AndroidStoragePermission(application, sdkInt = 29).needsRequest())
    }

    @Test
    fun api35_neverNeedsRequest() {
        assertFalse(AndroidStoragePermission(application, sdkInt = 35).needsRequest())
    }

    @Test
    fun blocked_deniedWithoutRationale() {
        assertTrue(storagePermissionBlocked(granted = false, rationaleAfter = false))
    }

    @Test
    fun notBlocked_deniedWithRationale() {
        assertFalse(storagePermissionBlocked(granted = false, rationaleAfter = true))
    }

    @Test
    fun notBlocked_whenGranted() {
        assertFalse(storagePermissionBlocked(granted = true, rationaleAfter = false))
    }
}
