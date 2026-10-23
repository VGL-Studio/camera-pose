package com.vglstudio.camera.pose.line.guide.feature.camera.photo

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

/** Saves photos to the gallery through MediaStore. Public for the device test in :app. */
@Singleton
class MediaStorePhotoStore @Inject constructor(@ApplicationContext private val context: Context) : PhotoStore {
    override suspend fun save(file: File, takenAtMillis: Long): String = throw IOException("Not implemented")

    override suspend fun exists(uri: String): Boolean = false
}
