package com.vglstudio.camera.pose.line.guide.feature.poses

import androidx.compose.ui.graphics.ImageBitmap
import com.vglstudio.camera.pose.line.guide.core.analytics.Analytics
import com.vglstudio.camera.pose.line.guide.core.analytics.AnalyticsEvent
import com.vglstudio.camera.pose.line.guide.core.assets.AssetPath
import com.vglstudio.camera.pose.line.guide.core.config.FeatureFlag
import com.vglstudio.camera.pose.line.guide.core.config.FeatureFlags
import com.vglstudio.camera.pose.line.guide.core.network.NetworkStatus
import com.vglstudio.camera.pose.line.guide.core.poses.LoadFailureReason
import com.vglstudio.camera.pose.line.guide.core.poses.PoseContent
import com.vglstudio.camera.pose.line.guide.core.poses.PoseLoadFailure
import com.vglstudio.camera.pose.line.guide.core.poses.model.Framing
import com.vglstudio.camera.pose.line.guide.core.poses.model.LocalizedList
import com.vglstudio.camera.pose.line.guide.core.poses.model.LocalizedText
import com.vglstudio.camera.pose.line.guide.core.poses.model.Pose
import com.vglstudio.camera.pose.line.guide.core.poses.model.PoseCategory
import com.vglstudio.camera.pose.line.guide.core.poses.model.PoseFilter
import com.vglstudio.camera.pose.line.guide.core.poses.model.PoseGender
import com.vglstudio.camera.pose.line.guide.core.poses.model.SubjectType
import com.vglstudio.camera.pose.line.guide.feature.poses.data.LibraryContent
import com.vglstudio.camera.pose.line.guide.feature.poses.data.LibraryResult
import com.vglstudio.camera.pose.line.guide.feature.poses.data.PoseImageRepository
import com.vglstudio.camera.pose.line.guide.feature.poses.data.PoseImageResult
import com.vglstudio.camera.pose.line.guide.feature.poses.data.PoseThumbnailRepository
import com.vglstudio.camera.pose.line.guide.feature.poses.data.PosesLibrarySource
import com.vglstudio.camera.pose.line.guide.feature.poses.data.ThumbnailResult
import java.io.IOException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.merge

/** A pose built with the public constructor of :core:poses; names in Vietnamese and English. */
internal fun testPose(
    id: String,
    categoryId: String = "women",
    subjectType: SubjectType = SubjectType.SINGLE,
    stepCount: Int = 4,
    name: String = "Pose $id",
    englishName: String = "Pose $id (en)",
    framing: Framing = Framing.FULL,
    mistakes: List<String> = emptyList(),
): Pose = Pose(
    id = id,
    categoryId = categoryId,
    subjectType = subjectType,
    gender = PoseGender.ANY,
    framing = framing,
    tags = emptyList(),
    difficulty = 1,
    starter = false,
    name = LocalizedText(mapOf("vi" to name, "en" to englishName)),
    steps = LocalizedList(mapOf("en" to (1..stepCount).map { "Step $it" })),
    mistakes = if (mistakes.isEmpty()) LocalizedList(emptyMap()) else LocalizedList(mapOf("en" to mistakes)),
    keypoints = null,
    thumbnailPath = AssetPath("poses/img/$id/preview_540-0123abcd.webp"),
    imagePath = AssetPath("poses/img/$id/preview_1200-0123abcd.webp"),
    outlinePath = AssetPath("poses/img/$id/outline-0123abcd.webp"),
)

internal object PosesFixtures {
    val WOMEN = PoseCategory("women", LocalizedText(mapOf("vi" to "Nữ", "en" to "Women")))
    val MEN = PoseCategory("men", LocalizedText(mapOf("vi" to "Nam", "en" to "Men")))
    val COUPLES = PoseCategory("couples", LocalizedText(mapOf("vi" to "Cặp đôi", "en" to "Couples")))

    /** Catalog order: w01, w02, w03, m01, m02, c01, c02. */
    val POSES = listOf(
        testPose("w01", "women", SubjectType.SINGLE, 4, name = "Tựa lan can", englishName = "Lean on the rail"),
        testPose("w02", "women", SubjectType.SINGLE, 3, name = "Nhìn qua vai", englishName = "Look over the shoulder"),
        testPose("w03", "women", SubjectType.SINGLE, 4, name = "Tay chống hông"),
        testPose("m01", "men", SubjectType.SINGLE, 3, name = "Đứng dựa tường"),
        testPose("m02", "men", SubjectType.GROUP, 3, name = "Ngồi bậc thềm"),
        testPose("c01", "couples", SubjectType.COUPLE, 5, name = "Bước đi tự nhiên"),
        testPose("c02", "couples", SubjectType.COUPLE, 4, name = "Tựa vai nhau"),
    )
}

/**
 * [LibraryContent] over [poses] (default the fixture catalog) in 3 categories, with [savedIds] saved: saved poses
 * first (in the order of [savedIds]), then the others in catalog order, for All and for each category, as :core:poses
 * orders them. Saved holds the saved poses in the order of [savedIds].
 */
internal fun testContent(
    savedIds: List<String> = listOf("w02"),
    poses: List<Pose> = PosesFixtures.POSES,
    categories: List<PoseCategory> = listOf(PosesFixtures.WOMEN, PosesFixtures.MEN, PosesFixtures.COUPLES),
    totalPoses: Int = poses.size,
): LibraryContent {
    val byId = poses.associateBy { it.id }
    val saved = savedIds.mapNotNull { byId[it] }
    fun ordered(list: List<Pose>) = list.filter { it.id in savedIds }.sortedBy { savedIds.indexOf(it.id) } +
        list.filter { it.id !in savedIds }
    val lists = buildMap<PoseFilter, List<Pose>> {
        put(PoseFilter.All, ordered(poses))
        put(PoseFilter.Saved, saved)
        categories.forEach { category ->
            put(PoseFilter.Category(category.id), ordered(poses.filter { it.categoryId == category.id }))
        }
    }
    return LibraryContent(
        categories = categories,
        totalPoses = totalPoses,
        savedIds = saved.map { it.id }.toSet(),
        lists = lists,
    )
}

/**
 * The library as a state flow; [emitAgain] sends the current value a second time (a state flow drops equal values).
 * A write records the call, fails when [failWrites], waits for [writeGate] when set, then publishes the content with
 * the pose saved first or removed, as :core:poses would. With [publishLater] the new content waits in [pending] until
 * [publishPending]: the write is done before the library has the new value.
 */
internal class FakePosesLibrarySource : PosesLibrarySource {
    val state = MutableStateFlow<LibraryResult>(LibraryResult.Loading)
    private val repeats = MutableSharedFlow<LibraryResult>(extraBufferCapacity = 8)
    var retryCount = 0
    val writes = mutableListOf<Pair<String, Boolean>>()
    var failWrites = false
    var writeGate: CompletableDeferred<Unit>? = null
    var publishLater = false
    var pending: LibraryContent? = null

    override val library: Flow<LibraryResult> = merge(state, repeats)

    fun ready(content: LibraryContent = testContent()) {
        state.value = LibraryResult.Ready(content)
    }

    fun fail(reason: LoadFailureReason) {
        state.value = LibraryResult.Failed(PoseLoadFailure(PoseContent.CATALOG, reason))
    }

    fun emitAgain() {
        check(repeats.tryEmit(state.value))
    }

    override suspend fun retry() {
        retryCount++
    }

    override suspend fun setSaved(poseId: String, saved: Boolean) {
        writes += poseId to saved
        writeGate?.await()
        if (failWrites) throw IOException("disk full")
        val content = (state.value as? LibraryResult.Ready)?.content ?: return
        val savedIds = content.posesFor(PoseFilter.Saved).map { it.id }
        val newSaved = if (saved) listOf(poseId) + (savedIds - poseId) else savedIds - poseId
        val next = testContent(savedIds = newSaved)
        if (publishLater) pending = next else ready(next)
    }

    fun publishPending() {
        ready(checkNotNull(pending))
        pending = null
    }
}

/**
 * Thumbnails by pose id: [results] (default a Ready image from [image]), after [delays] of virtual time when set.
 * Counts loads by id and the loads running now.
 */
internal class FakePoseThumbnailRepository(private val image: () -> ImageBitmap) : PoseThumbnailRepository {
    val results = mutableMapOf<String, ThumbnailResult>()
    val delays = mutableMapOf<String, Long>()
    val gates = mutableMapOf<String, CompletableDeferred<Unit>>()
    val loads = mutableMapOf<String, Int>()
    var running = 0
    var maxRunning = 0
    val images = mutableMapOf<String, ImageBitmap>()

    fun imageOf(id: String) = images.getOrPut(id) { image() }

    fun fail(id: String, reason: LoadFailureReason) {
        results[id] = ThumbnailResult.Failed(PoseLoadFailure(PoseContent.THUMBNAIL, reason))
    }

    override suspend fun load(pose: Pose): ThumbnailResult {
        loads[pose.id] = (loads[pose.id] ?: 0) + 1
        running++
        maxRunning = maxOf(maxRunning, running)
        try {
            delays[pose.id]?.let { delay(it) }
            gates[pose.id]?.await()
            return results[pose.id] ?: ThumbnailResult.Ready(imageOf(pose.id))
        } finally {
            running--
        }
    }
}

/**
 * Large images by pose id: [results] (default a Ready image from [image]), after [delays] of virtual time when set.
 * Counts loads by id.
 */
internal class FakePoseImageRepository(private val image: () -> ImageBitmap) : PoseImageRepository {
    val results = mutableMapOf<String, PoseImageResult>()
    val delays = mutableMapOf<String, Long>()
    val gates = mutableMapOf<String, CompletableDeferred<Unit>>()
    val loads = mutableMapOf<String, Int>()
    val images = mutableMapOf<String, ImageBitmap>()

    fun imageOf(id: String) = images.getOrPut(id) { image() }

    fun fail(id: String, reason: LoadFailureReason) {
        results[id] = PoseImageResult.Failed(PoseLoadFailure(PoseContent.IMAGE, reason))
    }

    override suspend fun load(pose: Pose): PoseImageResult {
        loads[pose.id] = (loads[pose.id] ?: 0) + 1
        delays[pose.id]?.let { delay(it) }
        gates[pose.id]?.await()
        return results[pose.id] ?: PoseImageResult.Ready(imageOf(pose.id))
    }
}

internal class FakeNetworkStatus : NetworkStatus {
    override val dialogShown = MutableStateFlow(false)
    override val recovered = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
}

internal class FakeAnalytics : Analytics {
    val events = mutableListOf<AnalyticsEvent>()

    override fun log(event: AnalyticsEvent) {
        events += event
    }
}

/** POSES follows [value]; other flags are off. */
internal class FakeFeatureFlags(initial: Boolean = true) : FeatureFlags {
    val value = MutableStateFlow(initial)

    override fun enabled(flag: FeatureFlag): Flow<Boolean> =
        if (flag == FeatureFlag.POSES) value else MutableStateFlow(false)
}
