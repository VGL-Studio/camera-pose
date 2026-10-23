package com.vglstudio.camera.pose.line.guide.feature.camera.review

import androidx.compose.ui.graphics.ImageBitmap
import com.vglstudio.camera.pose.line.guide.core.analytics.Analytics
import com.vglstudio.camera.pose.line.guide.core.analytics.AnalyticsEvent
import com.vglstudio.camera.pose.line.guide.feature.camera.data.SavedPhotosRepository
import com.vglstudio.camera.pose.line.guide.feature.camera.photo.PhotoDecoder
import com.vglstudio.camera.pose.line.guide.feature.camera.photo.PhotoStore
import com.vglstudio.camera.pose.line.guide.feature.camera.photo.StoragePermission
import java.io.File
import java.io.IOException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

internal class FakePhotoStore : PhotoStore {
    var savedUri = "content://media/external/images/media/7"
    var saveError: IOException? = null
    val existing = mutableSetOf<String>()
    val saveCalls = mutableListOf<File>()

    /** When set, save waits for it, so a test sees SAVING and taps during a save. */
    var pendingSave: CompletableDeferred<Unit>? = null

    override suspend fun save(file: File, takenAtMillis: Long): String {
        saveCalls += file
        pendingSave?.await()
        saveError?.let { throw it }
        return savedUri
    }

    override suspend fun exists(uri: String): Boolean = uri in existing
}

internal class FakeSavedPhotosRepository : SavedPhotosRepository {
    val last = MutableStateFlow<String?>(null)
    val count = MutableStateFlow(0)
    val recorded = mutableListOf<String>()
    var failWrites = false

    /** When set, recordSaved waits for it, so a test can leave the screen during the write. */
    var pendingWrite: CompletableDeferred<Unit>? = null

    override val lastSavedPhoto: Flow<String?> = last
    override val savedCount: Flow<Int> = count

    override suspend fun recordSaved(uri: String) {
        pendingWrite?.await()
        if (failWrites) throw IOException("disk full")
        recorded += uri
        last.value = uri
        count.value += 1
    }
}

internal class FakeReviewAnalytics : Analytics {
    val events = mutableListOf<AnalyticsEvent>()

    override fun log(event: AnalyticsEvent) {
        events += event
    }
}

/** Android 10 and higher by default: saving never asks. */
internal class FakeStoragePermission : StoragePermission {
    var needsRequest = false

    override fun needsRequest(): Boolean = needsRequest
}

internal class FakePhotoDecoder : PhotoDecoder {
    override suspend fun decode(location: String, maxSidePx: Int): ImageBitmap? = null
}
