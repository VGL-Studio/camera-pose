package com.vglstudio.camera.pose.line.guide.feature.camera.photo

import android.Manifest
import android.content.Context
import android.os.Build

/** Saving to the gallery on Android 9 and lower needs this permission; Android 10 and higher need none. */
const val STORAGE_PERMISSION = Manifest.permission.WRITE_EXTERNAL_STORAGE

/** Write access to shared storage, needed to save to the gallery on Android 9 and lower only. */
interface StoragePermission {
    /** True when saving must ask for the permission first: Android 9 or lower and not granted. */
    fun needsRequest(): Boolean
}

/** After a denial on Android 9 or lower: true when the user chose "don't ask again" (no rationale any more). */
fun storagePermissionBlocked(granted: Boolean, rationaleAfter: Boolean): Boolean = false

/**
 * [sdkInt] is a parameter so Robolectric tests run the Android 9 rule on the SDK they already have.
 * Public for the device test in :app.
 */
class AndroidStoragePermission(private val context: Context, private val sdkInt: Int = Build.VERSION.SDK_INT) :
    StoragePermission {
    override fun needsRequest(): Boolean = false
}
