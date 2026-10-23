package com.vglstudio.camera.pose.line.guide.feature.camera.photo

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.os.StatFs
import android.provider.MediaStore
import androidx.annotation.RequiresApi
import com.vglstudio.camera.pose.line.guide.feature.camera.R
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.util.TimeZone
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Saves photos to `Pictures/Camera Pose/` of the gallery (spec camera, KB-6; plan T-016). The bytes are copied as the
 * camera wrote them, EXIF orientation included. Android 10 and higher: MediaStore with RELATIVE_PATH and IS_PENDING,
 * no permission. Android 9 and lower: the file is written by path, which needs WRITE_EXTERNAL_STORAGE (asked by the
 * review screen first), then added to MediaStore. Public for the device test in :app.
 */
@Singleton
class MediaStorePhotoStore @Inject constructor(@ApplicationContext private val context: Context) : PhotoStore {
    private val albumName: String get() = context.getString(R.string.review_album_name)

    override suspend fun save(file: File, takenAtMillis: Long): String = withContext(Dispatchers.IO) {
        try {
            val displayName = PhotoFileNames.displayName(takenAtMillis, TimeZone.getDefault())
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                saveToMediaStore(file, takenAtMillis, displayName)
            } else {
                saveByPath(file, takenAtMillis, displayName)
            }
        } catch (e: StorageFullException) {
            throw e
        } catch (e: IOException) {
            throw if (isStorageFull(e)) StorageFullException(e) else e
        } catch (e: SecurityException) {
            // Normally impossible: the review screen asks for the permission before saving on Android 9 and lower.
            throw IOException("No permission to save", e)
        }
    }

    override suspend fun exists(uri: String): Boolean = withContext(Dispatchers.IO) {
        try {
            context.contentResolver.query(Uri.parse(uri), arrayOf(MediaStore.MediaColumns._ID), null, null, null)
                ?.use { it.moveToFirst() } ?: false
        } catch (e: SecurityException) {
            false
        } catch (e: IllegalArgumentException) {
            false
        }
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    private fun saveToMediaStore(file: File, takenAtMillis: Long, displayName: String): String {
        // Deprecated from API 29 but still the primary shared storage volume that MediaStore writes to.
        @Suppress("DEPRECATION")
        val volume = Environment.getExternalStorageDirectory()
        checkFreeSpace(volume, file)
        val resolver = context.contentResolver
        val values = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, displayName)
            put(MediaStore.Images.Media.MIME_TYPE, MIME_JPEG)
            put(MediaStore.Images.Media.RELATIVE_PATH, "${Environment.DIRECTORY_PICTURES}/$albumName")
            put(MediaStore.Images.Media.DATE_TAKEN, takenAtMillis)
            put(MediaStore.Images.Media.IS_PENDING, 1)
        }
        val collection = MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
        val uri = resolver.insert(collection, values) ?: throw IOException("MediaStore insert returned null")
        try {
            val output = resolver.openOutputStream(uri) ?: throw IOException("No output stream")
            output.use { out -> file.inputStream().use { it.copyTo(out) } }
            val done = ContentValues().apply { put(MediaStore.Images.Media.IS_PENDING, 0) }
            resolver.update(uri, done, null, null)
        } catch (e: Exception) {
            resolver.delete(uri, null, null)
            throw e
        }
        return uri.toString()
    }

    private fun saveByPath(file: File, takenAtMillis: Long, displayName: String): String {
        // Deprecated from API 29; correct on Android 8 and 9, the only versions that take this branch.
        @Suppress("DEPRECATION")
        val pictures = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
        val dir = File(pictures, albumName)
        if (!dir.isDirectory && !dir.mkdirs()) throw IOException("Cannot create the album folder")
        checkFreeSpace(dir, file)
        val target = PhotoFileNames.uniqueFile(dir, displayName)
        try {
            FileOutputStream(target).use { out ->
                file.inputStream().use { it.copyTo(out) }
                out.fd.sync()
            }
            val values = ContentValues().apply {
                // The file path is how MediaStore finds a file on Android 9 and lower (deprecated from API 29).
                @Suppress("DEPRECATION")
                put(MediaStore.Images.Media.DATA, target.absolutePath)
                put(MediaStore.Images.Media.DISPLAY_NAME, target.name)
                put(MediaStore.Images.Media.TITLE, target.nameWithoutExtension)
                put(MediaStore.Images.Media.MIME_TYPE, MIME_JPEG)
                put(MediaStore.Images.Media.DATE_TAKEN, takenAtMillis)
            }
            val uri = context.contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
                ?: throw IOException("MediaStore insert returned null")
            return uri.toString()
        } catch (e: Exception) {
            target.delete()
            throw e
        }
    }

    private fun checkFreeSpace(dir: File, file: File) {
        val available = try {
            StatFs(dir.path).availableBytes
        } catch (e: IllegalArgumentException) {
            // The volume cannot be read: let the write itself report the problem.
            return
        }
        if (available < file.length() + FREE_SPACE_MARGIN_BYTES) throw StorageFullException()
    }

    private companion object {
        const val MIME_JPEG = "image/jpeg"

        /** Below this much free space after the photo, saving counts as "storage full" (plan T-016, question 3). */
        const val FREE_SPACE_MARGIN_BYTES = 5L * 1024 * 1024
    }
}
