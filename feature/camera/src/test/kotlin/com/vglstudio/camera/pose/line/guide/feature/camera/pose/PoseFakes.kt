package com.vglstudio.camera.pose.line.guide.feature.camera.pose

import androidx.compose.ui.graphics.ImageBitmap
import com.vglstudio.camera.pose.line.guide.core.assets.AssetPath
import com.vglstudio.camera.pose.line.guide.core.network.NetworkStatus
import com.vglstudio.camera.pose.line.guide.core.poses.CatalogState
import com.vglstudio.camera.pose.line.guide.core.poses.PoseCatalogRepository
import com.vglstudio.camera.pose.line.guide.core.poses.PoseFileResult
import com.vglstudio.camera.pose.line.guide.core.poses.PoseHistoryRepository
import com.vglstudio.camera.pose.line.guide.core.poses.PoseLibraryRepository
import com.vglstudio.camera.pose.line.guide.core.poses.PoseLoadFailure
import com.vglstudio.camera.pose.line.guide.core.poses.StarterSetResult
import com.vglstudio.camera.pose.line.guide.core.poses.model.Framing
import com.vglstudio.camera.pose.line.guide.core.poses.model.LibraryState
import com.vglstudio.camera.pose.line.guide.core.poses.model.LocalizedList
import com.vglstudio.camera.pose.line.guide.core.poses.model.LocalizedText
import com.vglstudio.camera.pose.line.guide.core.poses.model.Pose
import com.vglstudio.camera.pose.line.guide.core.poses.model.PoseCatalog
import com.vglstudio.camera.pose.line.guide.core.poses.model.PoseCategory
import com.vglstudio.camera.pose.line.guide.core.poses.model.PoseFile
import com.vglstudio.camera.pose.line.guide.core.poses.model.PoseGender
import com.vglstudio.camera.pose.line.guide.core.poses.model.PoseLibrary
import com.vglstudio.camera.pose.line.guide.core.poses.model.SavedPose
import com.vglstudio.camera.pose.line.guide.core.poses.model.SubjectType
import com.vglstudio.camera.pose.line.guide.core.poses.model.UsedPose
import com.vglstudio.camera.pose.line.guide.feature.camera.data.PoseOverlaySettingsRepository
import java.io.File
import java.io.IOException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * Catalog of the pose tests: categories women ("Nữ"), men ("Nam") and kids (no pose); poses in catalog order
 * w01, m01, w02, m02, w03. Women poses have four steps, men poses three.
 */
internal object PoseFixtures {
    val CATEGORIES = listOf(
        PoseCategory("women", LocalizedText(mapOf("en" to "Nữ"))),
        PoseCategory("men", LocalizedText(mapOf("en" to "Nam"))),
        PoseCategory("kids", LocalizedText(mapOf("en" to "Trẻ em"))),
    )

    fun pose(id: String): Pose {
        val women = id.startsWith("w")
        return pose(
            id = id,
            categoryId = if (women) "women" else "men",
            steps = (1..(if (women) 4 else 3)).map { "Step $it" },
        )
    }

    fun pose(id: String, categoryId: String, steps: List<String>): Pose = Pose(
        id = id,
        categoryId = categoryId,
        subjectType = SubjectType.SINGLE,
        gender = PoseGender.ANY,
        framing = Framing.FULL,
        tags = emptyList(),
        difficulty = 1,
        starter = false,
        name = LocalizedText(mapOf("en" to "Pose $id")),
        steps = LocalizedList(mapOf("en" to steps)),
        mistakes = LocalizedList(emptyMap()),
        keypoints = null,
        thumbnailPath = AssetPath("poses/img/$id/preview_540-1a2b3c4d.webp"),
        imagePath = AssetPath("poses/img/$id/preview_1200-1a2b3c4d.webp"),
        outlinePath = AssetPath("poses/img/$id/outline-1a2b3c4d.webp"),
    )

    fun catalog(poses: List<Pose> = listOf("w01", "m01", "w02", "m02", "w03").map { pose(it) }): PoseCatalog =
        PoseCatalog(datasetVersion = "test", categories = CATEGORIES, poses = poses)

    fun library(
        saved: List<SavedPose> = emptyList(),
        recent: List<UsedPose> = emptyList(),
        catalog: PoseCatalog = catalog(),
    ): PoseLibrary = PoseLibrary.from(catalog, saved, recent)
}

internal class FakePoseLibraryRepository : PoseLibraryRepository {
    val state = MutableStateFlow<LibraryState>(LibraryState.Loading)
    var retryCount = 0

    override val library: Flow<LibraryState> = state

    override suspend fun retry() {
        retryCount++
    }

    fun ready(library: PoseLibrary = PoseFixtures.library()) {
        state.value = LibraryState.Ready(library)
    }
}

/** Records every file load. A result is `Ready(File("<id>-<file>"))` unless one is set; a load waits while pending. */
internal class FakePoseCatalogRepository : PoseCatalogRepository {
    val loads = mutableListOf<Pair<String, PoseFile>>()
    val results = mutableMapOf<Pair<String, PoseFile>, PoseFileResult>()
    val pending = mutableMapOf<Pair<String, PoseFile>, CompletableDeferred<Unit>>()

    override val catalog: StateFlow<CatalogState> = MutableStateFlow(CatalogState.NotLoaded)

    override suspend fun loadCatalog(): CatalogState = catalog.value

    override suspend fun loadFile(pose: Pose, file: PoseFile): PoseFileResult {
        val key = pose.id to file
        loads += key
        pending[key]?.await()
        return results[key] ?: PoseFileResult.Ready(fileOf(pose.id, file))
    }

    override suspend fun loadStarterSet(onProgress: (done: Int, total: Int) -> Unit): StarterSetResult =
        StarterSetResult.Complete

    override val failures: SharedFlow<PoseLoadFailure> = MutableSharedFlow()

    companion object {
        fun fileOf(id: String, file: PoseFile) = File("$id-$file")
    }
}

internal class FakePoseHistoryRepository : PoseHistoryRepository {
    val used = mutableListOf<String>()
    var failWrites = false

    override val saved: Flow<List<SavedPose>> = MutableStateFlow(emptyList())
    override val recent: Flow<List<UsedPose>> = MutableStateFlow(emptyList())

    override suspend fun setSaved(poseId: String, saved: Boolean) {
        if (failWrites) throw IOException("disk full")
    }

    override suspend fun markUsed(poseId: String) {
        if (failWrites) throw IOException("disk full")
        used += poseId
    }
}

internal class FakePoseOverlaySettingsRepository : PoseOverlaySettingsRepository {
    val stored = MutableStateFlow(false)
    val saves = mutableListOf<Boolean>()
    var failWrites = false

    override val stepsHidden: Flow<Boolean> = stored

    override suspend fun setStepsHidden(hidden: Boolean) {
        if (failWrites) throw IOException("disk full")
        saves += hidden
        stored.value = hidden
    }
}

/** One 1 x 1 bitmap per file, so a test can tell which file an image came from; files in [undecodable] give null. */
internal class FakePoseImageDecoder : PoseImageDecoder {
    val undecodable = mutableSetOf<File>()
    private val bitmaps = mutableMapOf<File, ImageBitmap>()

    fun bitmapOf(file: File): ImageBitmap = bitmaps.getOrPut(file) { ImageBitmap(1, 1) }

    override suspend fun decode(file: File, targetPx: Int): ImageBitmap? =
        if (file in undecodable) null else bitmapOf(file)
}

internal class FakeNetworkStatus : NetworkStatus {
    override val dialogShown = MutableStateFlow(false)
    override val recovered = MutableSharedFlow<Unit>()
}
