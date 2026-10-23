package com.vglstudio.camera.pose.line.guide.feature.camera.photo

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat

/** Saving to the gallery on Android 9 and lower needs this permission; Android 10 and higher need none. */
const val STORAGE_PERMISSION = Manifest.permission.WRITE_EXTERNAL_STORAGE

/** Write access to shared storage, needed to save to the gallery on Android 9 and lower only. */
interface StoragePermission {
    /** True when saving must ask for the permission first: Android 9 or lower and not granted. */
    fun needsRequest(): Boolean
}

/**
 * After a denial on Android 9 or lower: true when the user chose "don't ask again". On these versions a plain denial
 * always makes the system suggest a rationale, so no rationale after asking means the dialog will not show again.
 */
fun storagePermissionBlocked(granted: Boolean, rationaleAfter: Boolean): Boolean = !granted && !rationaleAfter

/**
 * [sdkInt] is a parameter so Robolectric tests run the Android 9 rule on the SDK they already have.
 * Public for the device test in :app.
 */
class AndroidStoragePermission(private val context: Context, private val sdkInt: Int = Build.VERSION.SDK_INT) :
    StoragePermission {
    override fun needsRequest(): Boolean = sdkInt <= LAST_SDK_NEEDING_PERMISSION &&
        ContextCompat.checkSelfPermission(context, STORAGE_PERMISSION) != PackageManager.PERMISSION_GRANTED

    private companion object {
        /** Android 9 (API 28): from Android 10, MediaStore saves without any permission. */
        const val LAST_SDK_NEEDING_PERMISSION = Build.VERSION_CODES.P
    }
}
