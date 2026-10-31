---
ticket: T-085
title: Dữ liệu tư thế premium và danh sách đã mở khóa (`:core:poses`, catalog)
status: draft                # draft | approved | stopped | done
version: 1                   # tăng khi plan được sửa lại sau phản hồi hoặc sau khi dừng
risk_score:                  # tổng điểm theo risk-scoring.md, để trống nếu chạm điểm chặn
risk_level: high             # low | medium | high
token_estimate: 9.0M         # cỡ M theo số gợi ý của scripts/token-usage.py estimate (2026-10-31, 19 task); không nâng tay
blocking_points: [core, r2]
run_order:                   # AI điền sau họp đánh giá rủi ro
reviewer:                    # người duyệt
cross_reviewer:              # bắt buộc khi risk_level là high
approved_at:
lane:                        # để trống: phiên tự chạy; quick: đường làm nhanh có người theo dõi (ADR-0034)
---

# PLAN T-085: DỮ LIỆU TƯ THẾ PREMIUM VÀ DANH SÁCH ĐÃ MỞ KHÓA

## 1. Tóm tắt

`:core:poses` đọc trường `premium` của mỗi tư thế trong catalog (thiếu trường thì miễn phí), giữ danh sách tư thế đã mở khóa trong DataStore (khóa `poses_unlocked`, cùng khuôn với danh sách đã lưu), và có repository công khai `PremiumPoseRepository` với `access: Flow<PremiumAccess>`, `isUsable(pose): Flow<Boolean>` và `unlock(poseId)`. Cờ `FeatureFlag.PREMIUM_POSES` (`feature_premium_poses_enabled`, mặc định tắt, bật ở staging) quyết có khóa hay không; cờ tắt thì mọi tư thế dùng được và `unlock` không ghi gì. `scripts/setup-check-app.sh` báo lỗi khi tư thế mặc định hay tư thế khởi đầu là premium; app cũng tự giữ luật này (bỏ `premium` của tư thế khởi đầu, tư thế mặc định là tư thế miễn phí đầu tiên). Kiểm chứng bằng unit test JVM của `:core:poses` và test của script; phần chạy thử ngày 2026-10-31 đã chạy đúng code của mục 3.1 (mục 4.1).

Chưa có giao diện: người dùng chưa thấy gì khác. Dấu premium, hộp thoại, quảng cáo có thưởng, sự kiện `premium_pose_unlocked` thuộc T-086; trang giới thiệu theo cờ thuộc T-082. Hai plan đó dẫn theo các chữ ký ở mục 3.1, phần "API công khai".

## 2. Hiểu yêu cầu

Ticket xử lý phần dữ liệu của KB-8 trong `features/poses/spec.md` (câu 6, 9, 10, 12):

| Kịch bản | Tiêu chí chấp nhận | Cách hiểu của AI |
| --- | --- | --- |
| KB-8 (câu 6) | Tư thế premium đánh dấu trong catalog, ở một phiên bản catalog mới; catalog không có trường thì mọi tư thế miễn phí | Trường `premium` (boolean, tùy chọn, mặc định `false`) trên mỗi tư thế. Không cần nâng `APP_SCHEMA` hay `minAppSchema`: parser bỏ qua khóa lạ (`ignoreUnknownKeys`), nên bản app cũ đọc được catalog mới và coi mọi tư thế miễn phí. "Phiên bản mới" là đường dẫn mới (`poses/catalog/v4/catalog-<sha8>.json`) do người làm nội dung đẩy sau khi merge, rồi đổi khóa `poses_catalog_path` (mục 8, câu 5) |
| KB-8, tiêu chí 7 (câu 9) | Tư thế đã mở khóa thì lần mở app sau, kể cả sau khi cập nhật app hay catalog mới, nó vẫn mở; chỉ mất khi xóa dữ liệu app hay cài lại | Danh sách mã tư thế trong DataStore dùng chung, khóa mới `poses_unlocked`, lưu theo mã, không theo catalog: tư thế bỏ khỏi catalog rồi quay lại vẫn mở. Cấu hình sao lưu của app ghi ở mục 3.3 (dòng Dữ liệu) và mục 8, câu 3 |
| KB-8, tiêu chí 8 (câu 10) | Cờ `feature_premium_poses_enabled` tắt thì không có dấu premium, mọi tư thế dùng được, không ghi gì vào danh sách đã mở khóa | `PremiumAccess.enabled = false` thì `isLocked` luôn `false`; `unlock` đọc cờ một lần (`first()`) và trả về ngay khi cờ tắt. Cờ theo dõi bằng `Flow` (ADR-0003): bật tắt giữa chừng thì `access` phát giá trị mới. Danh sách đã có giữ nguyên khi cờ tắt |
| KB-8, tiêu chí 11 (câu 6) | Khi mở camera lần đầu thì tư thế mặc định và các tư thế của bộ khởi đầu không bao giờ là premium | Hai lớp: script kiểm catalog báo lỗi (ticket), và app tự giữ: parser đặt `premium = false` cho tư thế `starter`, `PoseLibrary.defaultPose` là tư thế miễn phí đầu tiên. Lớp thứ hai là đề xuất của AI (mục 8, câu 1) vì script chỉ chạy trên staging |
| Câu 10, cờ ở staging | Bản staging bật cờ sẵn | `StagingFeatureFlagsModule` thêm `PREMIUM_POSES`, như ticket và `CLAUDE.md` bước 9. Cờ bật cục bộ thì Remote Config staging không tắt được (mục 8, câu 4) |
| Câu 12, phần dữ liệu | Ticket dữ liệu và catalog trong `:core:poses`; khóa ở giao diện là ticket khác | Plan không chạm `:feature:*`, `:core:ads`, `:core:analytics` |

Các tiêu chí còn lại của KB-8 (dấu premium, hộp thoại, quảng cáo có thưởng, sự kiện) và hai tiêu chí premium ở KB-1 của spec camera thuộc T-086.

Thiết kế tham chiếu: Không có thiết kế riêng (ticket không có giao diện; docs/ops/design-workflow.md).

ADR liên quan: ADR-0001 (core chỉ phụ thuộc core: `:core:poses` đã phụ thuộc `:core:config`), ADR-0003 (cờ là `Flow`, mặc định tắt), ADR-0015 (catalog đổi nội dung thì đổi đường dẫn), ADR-0019 (log không có dữ liệu cá nhân), ADR-0023 (khóa Remote Config, bảng khóa sinh tự động), ADR-0029, ADR-0031.

## 3. Giải pháp

### 3.1. Cách làm

Làm theo mẫu có sẵn của `:core:poses`: danh sách đã lưu (`DataStorePoseHistoryRepository`, plan T-009), parser bỏ từng tư thế hỏng (T-009), cờ đọc một lần cho quyết định tại một thời điểm (`DefaultInAppReview` của `:core:review`). Đoạn code dưới đây là code đã chạy thử (mục 4.1), trừ phần tài liệu.

**API công khai (T-082, T-086 dẫn theo).** Tên và chữ ký dưới đây là ổn định; plan khác dùng nguyên văn:

```kotlin
// :core:config, FeatureFlag.kt (enum có sẵn, thêm một dòng)
PREMIUM_POSES(
    "feature_premium_poses_enabled",
    "Locks the premium poses of the catalog until they are unlocked; off means every pose can be used.",
),

// :core:poses, model/Pose.kt (thêm một trường, mặc định false, đặt ngay sau starter)
val premium: Boolean = false,

// :core:poses, model/PremiumAccess.kt (tạo)
package com.vglstudio.camera.pose.line.guide.core.poses.model

/** Which poses are locked at one moment (spec poses, KB-8). */
data class PremiumAccess(
    /** FeatureFlag.PREMIUM_POSES. Off: no pose is locked. */
    val enabled: Boolean,
    /** Poses unlocked on this device, also the ones no longer in the catalog (spec poses, question 9). */
    val unlockedIds: Set<String>,
) {
    /** True when premium poses are on, [pose] is premium and it was not unlocked. */
    fun isLocked(pose: Pose): Boolean = enabled && pose.premium && pose.id !in unlockedIds

    fun isUsable(pose: Pose): Boolean = !isLocked(pose)
}

// :core:poses, PremiumPoseRepository.kt (tạo)
package com.vglstudio.camera.pose.line.guide.core.poses

import com.vglstudio.camera.pose.line.guide.core.poses.model.Pose
import com.vglstudio.camera.pose.line.guide.core.poses.model.PremiumAccess
import kotlinx.coroutines.flow.Flow

/** Premium poses and the ones unlocked on this device, kept until the app data is cleared (spec poses, KB-8). */
interface PremiumPoseRepository {
    /** Which poses are locked now; a new value when FeatureFlag.PREMIUM_POSES or the unlocked list changes. */
    val access: Flow<PremiumAccess>

    /** Whether [pose] can be used now: the flag is off, the pose is not premium, or it was unlocked. */
    fun isUsable(pose: Pose): Flow<Boolean>

    /**
     * Unlocks [poseId] for good on this device. Does nothing when FeatureFlag.PREMIUM_POSES is off (spec poses,
     * question 10); a second call keeps the first unlock time. Throws [java.io.IOException] when the list cannot be
     * written; the caller tells the user.
     */
    suspend fun unlock(poseId: String)
}
```

Cách dùng dự kiến, để hai plan sau không phải đoán:

- T-082 (trang premium của luồng giới thiệu) chỉ cần `featureFlags.enabled(FeatureFlag.PREMIUM_POSES)`; không cần `:core:poses`.
- T-086 (thư viện, chi tiết, camera) tiêm `PremiumPoseRepository` (Hilt, `@Singleton`) vào ViewModel. Lưới và dải tư thế gộp `library` với `access` (`combine`) rồi hỏi `access.isLocked(pose)` cho từng thẻ: một `Flow` cho cả danh sách, không một `Flow` cho mỗi thẻ. Màn chi tiết có thể dùng `isUsable(pose)`. Sau khi nhận thưởng thì gọi `unlock(pose.id)`, bắt `IOException`. Repository không biết catalog: `unlock` ghi bất kỳ mã nào khi cờ bật, nên chỉ gọi cho tư thế đang khóa.
- Feature test giả lập bằng một lớp tự viết cài `PremiumPoseRepository` (ví dụ `MutableStateFlow<PremiumAccess>`); test dựng tư thế premium bằng `Pose(..., premium = true)` hay `pose.copy(premium = true)`.
- Trước khi Remote Config tải xong, cờ là giá trị mặc định (tắt, trừ staging): `access` phát `enabled = false` trước, rồi `true`. Màn hình có thể thấy tư thế mở rồi khóa lại trong lúc đó; T-086 quyết cách hiện.

**Catalog.**

- `internal/CatalogJson.kt`, `PoseJson`: thêm `val premium: Boolean = false` ngay sau `starter`, kèm chú thích `// Catalogs made before premium poses (spec poses, question 6) have no such field: every pose is free.`
- `internal/CatalogParser.kt`: `Pose(... premium = pose.premium ...)`. Tư thế `starter` mà `premium` thì giữ tư thế, đặt `premium = false`, đếm và ghi một dòng cảnh báo không nêu mã tư thế:

```kotlin
        var premiumStarters = 0
        val poses = root.poses.mapNotNull { element ->
            // ... decode như cũ ...
            when (val converted = convert(pose, categories.map { it.id }.toSet(), keptIds)) {
                is Converted.Kept -> {
                    keptIds += converted.pose.id
                    // A starter pose is never premium (spec poses, question 6); scripts/setup-check-app.sh reports it.
                    if (converted.pose.starter && converted.pose.premium) {
                        premiumStarters++
                        converted.pose.copy(premium = false)
                    } else {
                        converted.pose
                    }
                }
                is Converted.Dropped -> { /* như cũ */ }
            }
        }
        if (premiumStarters > 0) {
            logger.warn(LOG_TAG, "Ignored premium on $premiumStarters starter poses of the catalog")
        }
```

- `premium` sai kiểu (ví dụ `"yes"`) làm tư thế đó bị bỏ như mọi trường sai kiểu hiện nay (`malformed`, T-009). Không đổi luật này (mục 8, câu 6).
- `internal/PoseOrdering.kt`, `buildPoseLibrary`: `defaultPose = catalog.poses.firstOrNull { !it.premium } ?: catalog.poses.firstOrNull()`, kèm chú thích `// Never a premium pose (spec poses, question 6), so the camera never opens on a locked pose.` Không xét cờ: catalog đúng thì tư thế đầu tiên miễn phí, kết quả như cũ.
- `model/PoseLibrary.kt`: KDoc của `defaultPose` đổi thành "The first pose of the catalog that is not premium (the first pose when every pose is premium): what the camera uses the very first time."
- `model/Pose.kt`: KDoc thêm "A [premium] pose is locked until it is unlocked on the device, while FeatureFlag.PREMIUM_POSES is on ([com.vglstudio.camera.pose.line.guide.core.poses.PremiumPoseRepository]). A starter pose is never premium."
- Không đổi `APP_SCHEMA`, `DEFAULT_CATALOG_PATH`, `docs/ops/required-assets.txt`: catalog mới chưa có trên R2 lúc merge, và catalog v3 hiện tại vẫn hợp lệ (mọi tư thế miễn phí). Đường dẫn bắt buộc chỉ đổi khi app phải có file mới (`docs/CLAUDE.md`, mục Khi viết plan); việc đổi catalog mặc định sang v4 là ticket sau (mục 8, câu 5).

**Danh sách đã mở khóa.** Phần đọc ghi danh sách mã tư thế của `DataStorePoseHistoryRepository` tách ra lớp `internal` dùng chung `PoseListStore`, để danh sách mới "cùng cách với danh sách đã lưu" mà không chép code. Khuôn lưu không đổi (một chuỗi JSON mỗi khóa, `[{"id":"w01","at":1729500000000}]`), nên không cần chuyển đổi dữ liệu cũ.

```kotlin
// internal/PoseListStore.kt (tạo)
package com.vglstudio.camera.pose.line.guide.core.poses.internal

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import com.vglstudio.camera.pose.line.guide.core.logging.AppLogger
import java.io.IOException
import kotlin.math.min
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.retryWhen
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json

@Serializable
internal data class StoredPose(val id: String, val at: Long)

/**
 * Lists of pose ids in the shared preferences, each one JSON string: `[{"id":"w01","at":1729500000000}]`.
 * A string that cannot be read counts as an empty list.
 */
internal class PoseListStore(private val dataStore: DataStore<Preferences>, private val logger: AppLogger) {
    private val json = Json { ignoreUnknownKeys = true }

    // A read error must never crash the screen or freeze it: show empty data, wait, then read again.
    fun read(key: Preferences.Key<String>): Flow<List<StoredPose>> = dataStore.data
        .retryWhen { cause, attempt ->
            if (cause !is IOException) return@retryWhen false
            logger.warn(LOG_TAG, "Could not read a stored pose list, trying again")
            emit(emptyPreferences())
            delay(retryDelayMillis(attempt))
            true
        }
        .map { preferences -> decode(preferences[key]) }
        .distinctUntilChanged()

    /** Writes [transform] of the list at [key]. Throws [IOException] when it cannot be written. */
    suspend fun update(key: Preferences.Key<String>, transform: (List<StoredPose>) -> List<StoredPose>) {
        dataStore.edit { preferences -> preferences[key] = encode(transform(decode(preferences[key]))) }
    }

    private fun decode(value: String?): List<StoredPose> {
        if (value == null) return emptyList()
        return try {
            json.decodeFromString(ListSerializer(StoredPose.serializer()), value)
        } catch (e: IllegalArgumentException) {
            logger.warn(LOG_TAG, "A stored pose list is corrupted, ignoring it")
            emptyList()
        }
    }

    private fun encode(list: List<StoredPose>): String =
        json.encodeToString(ListSerializer(StoredPose.serializer()), list)

    private companion object {
        const val MAX_RETRY_DELAY_MILLIS = 30_000L

        fun retryDelayMillis(attempt: Long): Long =
            min(1_000L shl attempt.coerceAtMost(5).toInt(), MAX_RETRY_DELAY_MILLIS)
    }
}
```

`internal/DataStorePoseHistoryRepository.kt` giữ hàm dựng `(dataStore, clock, logger)` (test có sẵn và `PosesModule` không đổi), dùng `private val store = PoseListStore(dataStore, logger)`; `saved`, `recent` đọc `store.read(...)`; `setSaved`, `markUsed` gọi `store.update(KEY) { list -> ... }` với đúng phép tính cũ (thêm ở đầu nếu chưa có, giữ thời điểm lưu đầu; `markUsed` đưa lên đầu, giữ 20). Lớp `StoredPose` `private` cũ, `decode`, `encode`, `stored`, `retryDelayMillis` chuyển sang `PoseListStore`. KDoc của lớp: "Saved and recent poses, each one list of [PoseListStore]."

```kotlin
// internal/DataStorePremiumPoseRepository.kt (tạo)
package com.vglstudio.camera.pose.line.guide.core.poses.internal

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.stringPreferencesKey
import com.vglstudio.camera.pose.line.guide.core.config.FeatureFlag
import com.vglstudio.camera.pose.line.guide.core.config.FeatureFlags
import com.vglstudio.camera.pose.line.guide.core.logging.AppLogger
import com.vglstudio.camera.pose.line.guide.core.poses.PremiumPoseRepository
import com.vglstudio.camera.pose.line.guide.core.poses.model.Pose
import com.vglstudio.camera.pose.line.guide.core.poses.model.PremiumAccess
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

/** The unlocked poses are one list of [PoseListStore], newest first, with the unlock time (spec poses, section 6). */
internal class DataStorePremiumPoseRepository(
    dataStore: DataStore<Preferences>,
    private val flags: FeatureFlags,
    private val clock: PosesClock,
    private val logger: AppLogger,
) : PremiumPoseRepository {
    private val store = PoseListStore(dataStore, logger)

    override val access: Flow<PremiumAccess> = combine(
        flags.enabled(FeatureFlag.PREMIUM_POSES),
        store.read(UNLOCKED_KEY),
    ) { enabled, unlocked -> PremiumAccess(enabled, unlocked.mapTo(mutableSetOf()) { it.id }) }
        .distinctUntilChanged()

    override fun isUsable(pose: Pose): Flow<Boolean> = access.map { it.isUsable(pose) }.distinctUntilChanged()

    override suspend fun unlock(poseId: String) {
        // A decision at one moment (ADR-0003): with the flag off nothing is written (spec poses, question 10).
        if (!flags.enabled(FeatureFlag.PREMIUM_POSES).first()) {
            logger.info(LOG_TAG, "Unlock skipped: flag_off")
            return
        }
        store.update(UNLOCKED_KEY) { list ->
            if (list.any { it.id == poseId }) list else listOf(StoredPose(poseId, clock.nowMillis())) + list
        }
        logger.info(LOG_TAG, "A premium pose was unlocked")
    }

    private companion object {
        val UNLOCKED_KEY = stringPreferencesKey("poses_unlocked")
    }
}
```

- Khóa `poses_unlocked` chưa có trong app (`grep` mọi `PreferencesKey("...")` ngày 2026-10-31: 22 khóa, không trùng).
- Log theo ADR-0019, thẻ `Poses`: "A premium pose was unlocked", "Unlock skipped: flag_off"; không ghi mã tư thế (cùng cách với "A pose was saved").
- Lỗi đọc: như danh sách đã lưu, phát danh sách rỗng (mọi tư thế premium khóa) rồi đọc lại. Lỗi ghi: `IOException` ném ra cho ViewModel của T-086.

**Hilt.** `di/PosesModule.kt` thêm:

```kotlin
    @Provides
    @Singleton
    fun premiumRepository(
        dataStore: DataStore<Preferences>,
        flags: FeatureFlags,
        clock: PosesClock,
        logger: AppLogger,
    ): PremiumPoseRepository = DataStorePremiumPoseRepository(dataStore, flags, clock, logger)
```

`FeatureFlags` do `ConfigModule` của `:core:config` cung cấp; `:core:poses` đã có `implementation(project(":core:config"))`, không đổi `build.gradle.kts`.

**Cờ.** `FeatureFlag.PREMIUM_POSES` như phần API. `app/src/staging/.../StagingFeatureFlagsModule.kt` thêm, sau `poseMatching()`:

```kotlin
    @Provides
    @IntoSet
    @LocallyEnabledFlags
    fun premiumPoses(): FeatureFlag = FeatureFlag.PREMIUM_POSES
```

Sinh lại bảng khóa: `./gradlew :app:testStagingDebugUnitTest -PremoteConfigKeys.update=true`. Chạy thử cho đúng một dòng mới ở mỗi file: `docs/remote-config-keys.md` dòng `| \`feature_premium_poses_enabled\` | boolean | \`false\` | \`:core:config\` | Locks the premium poses ... |`, `docs/remote-config-template.json` khối `"feature_premium_poses_enabled"` (5 dòng).

**Script kiểm catalog.** `scripts/setup-check-app.sh`, hàm `check_catalog`, sau phần `minAppSchema` và trước vòng kiểm file khởi đầu (diff đã chạy thử):

```diff
-  local path="$1" where="$2" strict="$3" catalog="$work/catalog.json" file count=0 bad=0
+  local path="$1" where="$2" strict="$3" catalog="$work/catalog.json" file count=0 bad=0 premium_bad
@@
+  # Premium poses (spec poses, question 6): the default pose (the first pose of the catalog) and the starter poses are
+  # never premium. The app copes (it skips premium poses for the default one and ignores premium on starter poses),
+  # but the catalog is then not what the content team meant.
+  premium_bad="$(jq -r '[.poses[] | objects] as $p
+    | ([$p[0] | select(. != null and .premium == true) | "mặc định \(.id)"]
+      + [$p[] | select(.starter == true and .premium == true) | "khởi đầu \(.id)"]) | join(", ")' \
+    "$catalog" 2> /dev/null || true)"
+  if [ -n "$premium_bad" ]; then
+    problem "$where $path có tư thế premium ở chỗ không được ($premium_bad): tư thế mặc định (tư thế đầu tiên của catalog) và tư thế khởi đầu không bao giờ là premium (spec poses, câu 6). Sửa catalog, đẩy bản mới lên R2 với đường dẫn mới."
+  fi
   while IFS= read -r file; do
@@
-  elif [ "$bad" -eq 0 ]; then
+  elif [ "$bad" -eq 0 ] && [ -z "$premium_bad" ]; then
     ok "$where $path và $count file khởi đầu trả 200"
```

- Lỗi áp cho mọi catalog mà script kiểm: catalog mặc định của app và mọi giá trị của `poses_catalog_path` trên Remote Config staging, kể cả giá trị theo điều kiện (catalog đó vẫn tới một số máy). Vẫn kiểm file khởi đầu như cũ; chỉ bỏ dòng `ok` của catalog đó.
- "Tư thế mặc định" của script là phần tử object đầu tiên của `poses`, đúng ý người làm nội dung; app thì chọn tư thế miễn phí đầu tiên trong các tư thế hợp lệ. Hai cách khác nhau chỉ khi tư thế đầu tiên hỏng hay premium, mà cả hai đều là lỗi của catalog.
- Script chỉ chạy với R2 staging (`env-canary.yml` mỗi sáng, `start-run.yml` trước phiên). Catalog production do người kiểm trước khi đổi khóa production (`docs/ops/pose-catalog.md`, mục mới bên dưới).

**Tài liệu nội dung.** Repo chưa có tài liệu khuôn catalog cho người làm nội dung: khuôn chỉ nằm ở bản đề xuất (`docs/research/proposal.md`, mục 11, tài liệu khảo sát) và plan T-009. Plan tạo `docs/ops/pose-catalog.md` (mục 8, câu 2), tiếng Việt, gồm:

1. Đường dẫn và phiên bản: `poses/catalog/v<n>/catalog-<sha8>.json`, nội dung đổi thì đường dẫn đổi (ADR-0015); thứ tự làm như `docs/ops/staging-and-runs.md` mục 2.4: đẩy catalog và mọi file lên R2 trước, rồi mới đổi khóa `poses_catalog_path`, rồi chạy lại `env-canary` với `setup_only`.
2. Gốc: `schema`, `minAppSchema` (app hiện đọc `1`), `datasetVersion`, `categories` (`id`, `name` theo ngôn ngữ, bắt buộc `en`), `poses`.
3. Mỗi tư thế, đúng tên trường của `PoseJson`: `id`, `category`, `subjectType` (`single`, `couple`, `group`), `gender` (`female`, `male`, `mixed`, `any`), `framing` (`full`, `half`, `close`), `tags`, `difficulty`, `starter`, `premium`, `name`, `steps`, `mistakes`, `preview` (`small`, `large`), `outline`, `keypoints` (33 điểm `[x, y, visibility]`, tùy chọn). Tư thế thiếu hay sai trường bắt buộc thì app bỏ riêng tư thế đó.
4. `premium` (T-085): `true` hay `false`, không đặt thì `false`; giá trị khác kiểu (chuỗi, số) làm app bỏ tư thế đó. Khoảng 20% tư thế, rải đều các nhóm (spec poses câu 6). Tư thế đầu tiên của catalog (tư thế mặc định của camera) và mọi tư thế `starter` không được là premium: `scripts/setup-check.sh` báo lỗi; app vẫn chạy (bỏ `premium` của tư thế khởi đầu, chọn tư thế miễn phí đầu tiên làm mặc định).
5. Tư thế đã mở khóa giữ theo `id` trên máy: đổi `id` của một tư thế premium thì người đã mở phải mở lại. Bỏ `premium` của một tư thế thì nó miễn phí cho mọi người; đặt `premium` cho tư thế đang miễn phí thì người chưa mở thấy nó bị khóa.
6. Bật trên production: `feature_premium_poses_enabled` và `is_show_rw_premium_pose` bật cùng lúc, sau khi catalog có `premium` đã lên R2 production và đã được kiểm như mục 4 (spec poses câu 10).

`docs/ops/staging-and-runs.md`, đoạn "Catalog tư thế (camera-pose, plan T-065)" (dòng 142): thêm một câu "Từ T-085, tư thế đầu tiên và tư thế `starter` của mỗi catalog không được có `premium: true`; có thì là lỗi (`docs/ops/pose-catalog.md`)."

### 3.2. Danh sách file

| Thao tác | Đường dẫn | Nội dung thay đổi |
| --- | --- | --- |
| Sửa | `core/config/src/main/kotlin/com/vglstudio/camera/pose/line/guide/core/config/FeatureFlag.kt` | `PREMIUM_POSES` |
| Sửa | `app/src/staging/kotlin/com/vglstudio/camera/pose/line/guide/di/StagingFeatureFlagsModule.kt` | `premiumPoses()` |
| Sửa | `core/poses/src/main/kotlin/com/vglstudio/camera/pose/line/guide/core/poses/model/Pose.kt` | `premium: Boolean = false`, KDoc |
| Sửa | `core/poses/src/main/kotlin/com/vglstudio/camera/pose/line/guide/core/poses/model/PoseLibrary.kt` | KDoc của `defaultPose` |
| Tạo | `core/poses/src/main/kotlin/com/vglstudio/camera/pose/line/guide/core/poses/model/PremiumAccess.kt` | `PremiumAccess` |
| Tạo | `core/poses/src/main/kotlin/com/vglstudio/camera/pose/line/guide/core/poses/PremiumPoseRepository.kt` | Interface công khai |
| Sửa | `core/poses/src/main/kotlin/com/vglstudio/camera/pose/line/guide/core/poses/internal/CatalogJson.kt` | `PoseJson.premium` |
| Sửa | `core/poses/src/main/kotlin/com/vglstudio/camera/pose/line/guide/core/poses/internal/CatalogParser.kt` | Đọc `premium`; bỏ `premium` của tư thế khởi đầu, ghi cảnh báo |
| Sửa | `core/poses/src/main/kotlin/com/vglstudio/camera/pose/line/guide/core/poses/internal/PoseOrdering.kt` | `defaultPose` bỏ qua tư thế premium |
| Tạo | `core/poses/src/main/kotlin/com/vglstudio/camera/pose/line/guide/core/poses/internal/PoseListStore.kt` | `StoredPose`, đọc ghi danh sách tách từ repository lịch sử |
| Sửa | `core/poses/src/main/kotlin/com/vglstudio/camera/pose/line/guide/core/poses/internal/DataStorePoseHistoryRepository.kt` | Dùng `PoseListStore`, hành vi giữ nguyên |
| Tạo | `core/poses/src/main/kotlin/com/vglstudio/camera/pose/line/guide/core/poses/internal/DataStorePremiumPoseRepository.kt` | Cài đặt `PremiumPoseRepository`, khóa `poses_unlocked` |
| Sửa | `core/poses/src/main/kotlin/com/vglstudio/camera/pose/line/guide/core/poses/di/PosesModule.kt` | `premiumRepository(...)` |
| Tạo | `core/poses/src/test/kotlin/com/vglstudio/camera/pose/line/guide/core/poses/FakeFeatureFlags.kt` | Cờ giả, như `core/review` |
| Tạo | `core/poses/src/test/kotlin/com/vglstudio/camera/pose/line/guide/core/poses/DataStorePremiumPoseRepositoryTest.kt` | Test mục 4 |
| Tạo | `core/poses/src/test/kotlin/com/vglstudio/camera/pose/line/guide/core/poses/PremiumAccessTest.kt` | Test mục 4 |
| Sửa | `core/poses/src/test/kotlin/com/vglstudio/camera/pose/line/guide/core/poses/CatalogParserTest.kt` | Test mới của mục 4 |
| Sửa | `core/poses/src/test/kotlin/com/vglstudio/camera/pose/line/guide/core/poses/RealCatalogV3Test.kt` | Test mới: catalog v3 không có `premium` |
| Sửa | `core/poses/src/test/kotlin/com/vglstudio/camera/pose/line/guide/core/poses/PoseOrderingTest.kt` | Test mới của `defaultPose` |
| Sửa | `core/poses/src/test/kotlin/com/vglstudio/camera/pose/line/guide/core/poses/Fixtures.kt` | `pose(..., premium: Boolean = false)` |
| Sửa | `scripts/setup-check-app.sh` | Lỗi khi tư thế mặc định hay khởi đầu là premium |
| Sửa | `scripts/tests/app.sh` | Năm test của khối `setup-check app` (mục 4) |
| Sửa | `docs/remote-config-keys.md` | Sinh lại bằng `-PremoteConfigKeys.update=true` |
| Sửa | `docs/remote-config-template.json` | Sinh lại cùng lệnh |
| Tạo | `docs/ops/pose-catalog.md` | Khuôn catalog cho người làm nội dung (mục 3.1) |
| Sửa | `docs/ops/staging-and-runs.md` | Một câu ở đoạn "Catalog tư thế (camera-pose, plan T-065)" |

Nơi khác đọc các file bị đổi (L-020 của mẫu):

- `scripts/setup-check-app.sh` được `scripts/setup-check.sh` đọc vào (`source`) và được `scripts/tests/app.sh` kiểm; `.github/workflows/env-canary.yml` và `.github/workflows/start-run.yml` nhắc tên file trong chú thích và chạy `setup-check.sh`. Không đổi tham số, biến môi trường hay cách gọi; chỉ thêm một lỗi mới, nên hai workflow không sửa (và `.github/` không được sửa ngoài plan cho phép). Lỗi mới chặn phiên như mọi lỗi catalog khác chỉ khi catalog thật có tư thế premium ở chỗ không được. `docs/template-sync.txt` liệt kê file này là của app, không đồng bộ từ mẫu: không sửa.
- `docs/ops/staging-and-runs.md` được nhắc trong chú thích hay thông báo của `.github/workflows/device-session.yml`, `.github/workflows/env-canary.yml`, `.github/workflows/resume-run.yml`, `.github/workflows/staging.yml`, `.github/workflows/start-run.yml`, `scripts/cloud-env-check.sh`, `scripts/env-canary.sh`, `scripts/run-watchdog.py`, theo tên file và số mục. Plan chỉ thêm một câu vào đoạn có sẵn, không đổi tiêu đề hay số mục, nên không file nào trong đó bị ảnh hưởng.
- `app/src/test/kotlin/com/vglstudio/camera/pose/line/guide/config/RemoteConfigKeysTest.kt` so `docs/remote-config-keys.md` và `docs/remote-config-template.json` với khóa của code; hai file sinh lại trong cùng commit có dòng enum, nên test qua mà không sửa (chạy thử, mục 4.1).
- `scripts/tests/app.sh` được `scripts/tests/run.sh` đọc vào, chạy trong `scripts/check.sh`. Test mới dùng `expect`, `sca_reset`, `sca_check` có sẵn; không đổi chúng.
- `docs/remote-config-keys.md`, `docs/remote-config-template.json` được `scripts/ads-plan.py` đọc; `ads-plan.py` đọc mọi dòng của bảng, không đếm số khóa cố định, nên không sửa. `app/build.gradle.kts` chỉ nêu đường dẫn của hai file: không đổi.
- `FeatureFlag.kt`: `DefaultFeatureFlagsTest` lặp qua `FeatureFlag.entries` (mặc định tắt, khóa khớp tên), `AppRemoteKeys` lấy `FeatureFlag.entries`: dòng mới thỏa, không sửa.
- `Pose.kt`: mọi chỗ dựng `Pose(...)` dùng tham số có tên (`Fixtures.kt`, `feature/poses/.../PosesFakes.kt`, `feature/camera/.../PoseFakes.kt`, `CatalogParser.kt`); trường mới có mặc định, nên không sửa feature test. `equals` của `Pose` thêm một trường, mọi tư thế hiện tại có `premium = false`, nên so sánh cũ không đổi.
- `DataStorePoseHistoryRepository.kt`: chỉ `PosesModule` và `DataStorePoseHistoryRepositoryTest` dựng nó; hàm dựng giữ nguyên.
- `docs/ops/staging-and-runs.md`, `docs/ops/pose-catalog.md`: tài liệu, không script nào đọc.

### 3.3. Cờ tính năng, quảng cáo, dữ liệu, quyền, thư viện

- Cờ tính năng: `feature_premium_poses_enabled` (`FeatureFlag.PREMIUM_POSES`), mặc định tắt (ADR-0003), bật cục bộ ở staging (`StagingFeatureFlagsModule`, spec poses câu 10). Theo dõi bằng `Flow` trong `access`; `unlock` đọc một lần cho quyết định tại thời điểm ghi.
- Quảng cáo: Không chạm. `rw_premium_pose` thuộc T-086.
- Sự kiện phân tích: Không chạm. `premium_pose_unlocked` thuộc T-086.
- Dữ liệu trên máy: khóa DataStore mới `poses_unlocked` (spec poses mục 6, "Danh sách tư thế premium đã mở khóa, kèm thời điểm mở"). Không dữ liệu nào ra khỏi máy. Không chuyển đổi dữ liệu cũ: khuôn của `poses_saved`, `poses_recent` giữ nguyên. Cấu hình sao lưu (câu 9), đã đọc ngày 2026-10-31:
  - `app/src/main/AndroidManifest.xml` dòng 30: `android:allowBackup="false"`, có từ commit đầu của app (`7762576`, từ mẫu); không có `android:dataExtractionRules` hay `android:fullBackupContent`.
  - Manifest gộp `stagingDebug` (`./gradlew :app:processStagingDebugMainManifest`, file trong `app/build/intermediates/merged_manifest/`): chỉ có `android:allowBackup="false"` (dòng 153), thư viện không thêm thuộc tính sao lưu nào.
  - `res/xml` của mọi module chỉ có `locales_config.xml` (app) và `camera_capture_paths.xml` (camera), không có luật sao lưu.
  - Nghĩa là: Auto Backup lên Google Drive tắt, nên cài lại app thì danh sách mất, đúng câu 9 ("mất khi xóa dữ liệu app hay cài lại"). Theo tài liệu Android, với app có `targetSdk` từ 31 (app này 36), `allowBackup="false"` không chặn chuyển dữ liệu trực tiếp giữa hai máy (device-to-device) trên Android 12 trở lên: khi người dùng chuyển sang máy mới bằng cáp hay Wi-Fi, danh sách đã mở (cùng mọi dữ liệu DataStore khác của app) có thể đi theo. Chưa thử trên máy thật. Plan không đổi cấu hình sao lưu (mục 8, câu 3).
- File R2: Không thêm file bắt buộc, không đổi đường dẫn. Khuôn catalog thêm trường tùy chọn `premium`; catalog mới do người làm nội dung đẩy sau khi merge (ticket, mục Nội dung).
- **Quyền truy cập:** Không thêm.
- **Thư viện và SDK:** Không thêm. `DataStore`, `stringPreferencesKey`, `kotlinx-serialization-json`, `kotlinx-coroutines` đã có trên classpath biên dịch của `:core:poses` (dùng ở `DataStorePoseHistoryRepository`); `FeatureFlags` của `:core:config` đã là phụ thuộc `implementation` của `:core:poses`. Chạy thử biên dịch và test qua (mục 4.1). Script dùng `jq`, đã dùng trong cùng file.

### 3.4. Phương án dự phòng

Không có. Mọi phần là biến thể của mẫu có sẵn và đã chạy thử với đúng code của mục 3.1 (mục 4.1).

### 3.5. Hành vi đã có bị đổi

| Hành vi cũ | Hành vi mới (câu của mục 3.1) | Test có sẵn khẳng định hành vi cũ | Giữ, hay câu khẳng định mới |
| --- | --- | --- | --- |
| `defaultPose` là tư thế đầu tiên của catalog | "`defaultPose = catalog.poses.firstOrNull { !it.premium } ?: catalog.poses.firstOrNull()`" | `PoseOrderingTest.defaultPose_isFirstPoseInCatalogOrder_evenWhenOthersSavedOrRecent`, `PoseLibraryFromTest.from_defaultsToEmptyHistory` (`"p1"`), `PoseLibraryFromTest.from_matchesLibraryOrder` | Giữ: catalog của các test không có tư thế premium, tư thế đầu vẫn là kết quả. Thêm test mới ở mục 4 cho trường hợp premium |
| Đọc ghi `poses_saved`, `poses_recent` trong `DataStorePoseHistoryRepository` | Chuyển vào `PoseListStore`, cùng phép tính, cùng khuôn lưu | Cả 9 test của `DataStorePoseHistoryRepositoryTest` (lưu qua hai lần tạo repository, giữ thời điểm lưu đầu, 20 tư thế gần đây, lỗi ghi ném `IOException`, lỗi đọc phát rỗng rồi giá trị thật, chuỗi hỏng là rỗng và có cảnh báo, log không có lỗi hay mã tư thế) | Giữ nguyên, không sửa test: đây là đối chứng của phần tách. Chạy thử: 9/9 qua sau khi tách |
| Dòng cảnh báo khi đọc lỗi: "Could not read the saved poses, trying again" | "Could not read a stored pose list, trying again" (dùng chung cho cả danh sách đã mở) | Không test nào so chữ của dòng này (`readFailsOnce_emitsEmptyThenStoredValues` chỉ so giá trị) | Giữ |
| Parser bỏ qua khóa lạ, kể cả `premium` | Đọc `premium`; tư thế `starter` thì `premium = false` và thêm một dòng cảnh báo | `CatalogParserTest.unknownFields_areIgnored`, `validCatalog_readsEveryPoseField_includingKeypointsAndStarter`, `partlyInvalid_...` (đếm cảnh báo `isNotEmpty`, không nêu mã) | Giữ: các file mẫu không có `premium`; dòng cảnh báo mới không nêu mã tư thế, nên câu "never names them" vẫn đúng |
| `Pose` có 15 trường | Thêm `premium` (mặc định `false`) | Test của `:feature:poses`, `:feature:camera` dựng `Pose(...)` bằng tham số có tên và so `Pose` bằng `equals` | Giữ: mọi tư thế của test có `premium = false` |
| `FeatureFlag.entries` có 9 cờ | 10 cờ | `DefaultFeatureFlagsTest.everyFlag_isOffByDefault`, `withoutLocalOrRemoteValues_everyFlagIsOff`, `everyFlag_remoteKeyMatchesKeyAndDefault`; `RemoteConfigKeysTest.keysFile_matchesCode`, `templateFile_matchesCode`, `everyFeatureFlagAndPlacement_isListed` | Giữ: test lặp qua mọi cờ, cờ mới thỏa. Hai test bảng khóa đỏ khi dòng enum vào rồi qua khi sinh lại trong cùng commit (như T-079, T-080) |
| `setup-check-app.sh` in dòng `ok` cho catalog có file khởi đầu đủ | Không in `ok` khi catalog có tư thế premium ở chỗ không được | Khối `setup-check app` của `scripts/tests/app.sh` (các file mẫu không có `premium`) | Giữ: chạy thử với code mới, toàn bộ khối qua (mục 4.1) |

`scripts/existing-tests.py` chạy ngày 2026-10-31 (không có kết quả nào từ `.claude/worktrees/`): 588 test dùng tên của các file, 10 "Cần xem trước", 80 "Nên xem". Đã đọc nhóm "Cần xem trước" và các test của `:core:poses`, `:core:config`, `RemoteConfigKeysTest`, `PoseViewModelTest` trong nhóm "Nên xem":

- 10 test "Cần xem trước" (`PoseComponentsTest.poseThumb_is64dp`, các test bố cục của `CameraScreen*Test`, `LoadingScreenTest.landscape_retryReachable_w960h520`, `PosesScreenTest.realBottomBar_withoutAds_takesNoSpace`, `wide_hasThreeColumns`) khớp chỉ vì chữ `Pose` hay `poses` trong tên lớp, đo bố cục của màn hình. Plan không đổi giao diện: giữ.
- `PoseViewModelTest.firstOpen_selectsDefaultPose_withoutEventOrUse` và các test của camera dựa vào `defaultPose`: tư thế giả của camera (`PoseFakes.kt`) không có `premium`, nên `defaultPose` vẫn là tư thế đầu: giữ.
- `DefaultFeatureFlagsTest.*`, `NetworkDialogControllerTest.*`, `InAppReviewTest.flagOff_neverAsks`, `MainScreenTest.chooser_*`: dùng cờ khác, không đọc `FeatureFlag.entries` theo số lượng: giữ.
- `DataStorePoseHistoryRepositoryTest.*`, `DefaultPoseLibraryRepositoryTest.library_updatesWhenSavedOrRecentChange`: dòng thứ hai của bảng trên: giữ, là đối chứng của phần tách.
- `CatalogParserTest.*`, `RealCatalogV3Test.*`, `StagingSampleCatalogTest.*`, `DefaultPoseCatalogRepositoryTest.*`: file mẫu không có `premium`, các câu khẳng định không nhắc trường này: giữ.
- `RemoteConfigKeysTest.appModuleKeys_areListed`: khóa của `:core:poses` (`PosesRemoteKeys`) không đổi: giữ.

## 4. Kiểm thử

Test JVM của `:core:poses` dùng `InMemoryDataStore` có sẵn (`failWrites`, `readFailures`), `RecordingAppLogger`, `PosesClock { now }`, và `FakeFeatureFlags` mới (`MutableStateFlow<Set<FeatureFlag>>`, cùng dạng với `core/review/src/test/.../Fakes.kt`). Repository được dựng lại trên cùng `InMemoryDataStore` để giả lập lần mở app sau (như `setSavedTrueThenFalse_persistsAcrossRepositoryInstances`).

| Tiêu chí chấp nhận | Loại test | Tên test hoặc cách kiểm tra |
| --- | --- | --- |
| Câu 6: catalog có `premium` đọc đúng | Unit test | `CatalogParserTest.premium_readsTrue_missingAndFalseAreFree`: catalog ba tư thế không `starter` (`premium: true`, `premium: false`, không có trường): `poses.map { it.premium } == [true, false, false]`, `droppedPoses == 0` |
| Câu 6: catalog cũ không có trường thì mọi tư thế miễn phí | Unit test | `RealCatalogV3Test.realCatalog_hasNoPremiumField_everyPoseFree`: catalog v3 thật (350 tư thế) không có tư thế nào `premium`; đối chứng ở bước 3: bản tạm của file mẫu thêm `"premium": true` vào một tư thế không `starter` thì đỏ |
| KB-8 tiêu chí 11: tư thế khởi đầu không bao giờ premium (app) | Unit test | `CatalogParserTest.premiumOnStarter_isIgnored_andLogsCountWithoutIds`: tư thế `starter` có `premium: true` vẫn được giữ, `premium == false`; có một cảnh báo thẻ `Poses` chứa `"premium on 1 starter"`, không chứa mã tư thế. Đối chứng đã chạy (mục 4.1): bỏ luật thì `[true, false, true]` |
| KB-8 tiêu chí 11: tư thế mặc định không bao giờ premium (app) | Unit test | `PoseOrderingTest.defaultPose_isFirstFreePose_whenFirstIsPremium`: catalog `[p1 premium, p2, p3]`, có `saved` và `recent` như test cũ: `defaultPose.id == "p2"`. `PoseOrderingTest.defaultPose_everyPosePremium_isFirstPose`: mọi tư thế premium: `"p1"`. Đối chứng đã chạy: bỏ luật thì `expected f1 but was p1` |
| KB-8 tiêu chí 11: script báo lỗi | Test của script | `scripts/tests/app.sh`, ba test: "a premium default pose stops, naming it" (`1 1 0`: mã thoát 1, một dòng `LỖI - catalog mặc định của app <v3> có tư thế premium ở chỗ không được (mặc định a)`, không có dòng `ok`); "a premium starter pose stops, naming it, and its files are still checked" (`1 1 1`: dòng `(khởi đầu b)`, file nét viền của `b` vẫn được hỏi một lần); "a premium starter pose in the Remote Config catalog stops" (`--staging`, `1 1`: dòng `khóa poses_catalog_path trỏ tới catalog <v9> ... (khởi đầu b)`). Cả ba đỏ trên script hiện tại (mục 4.1) |
| Script không báo nhầm | Test của script | Hai test giữ hành vi: "a catalog without the premium field has no premium line" (`0 0`), "a premium pose that is neither the default nor a starter passes" (`0 0 1`: mã thoát 0, không dòng nào chứa `premium`, có dòng `ok ... và 1 file khởi đầu`). Qua trên cả script cũ và mới (mục 4.1); phép đột biến làm chúng đỏ: bỏ `select(.starter == true ...)` để mọi tư thế premium bị báo thì test thứ hai đỏ |
| KB-8 tiêu chí 8: cờ bật thì tư thế premium khóa, tư thế thường dùng được | Unit test | `DataStorePremiumPoseRepositoryTest.flagOn_premiumLocked_freeUsable`: `access.first()`: `isLocked(premium) == true`, `isUsable(free) == true`, `enabled == true` |
| KB-8 tiêu chí 8: cờ tắt thì mọi tư thế dùng được, không ghi gì | Unit test | `DataStorePremiumPoseRepositoryTest.flagOff_everyPoseUsable_andUnlockWritesNothing`: cờ tắt: `isUsable(premium).first() == true`; `unlock("p1")` rồi `dataStore.data.first()[stringPreferencesKey("poses_unlocked")] == null`; đối chứng trong cùng test: bật cờ, `isUsable(premium).first() == false` (không có gì được ghi). Đã chạy (mục 4.1): bỏ phần đọc cờ thì đỏ `expected null, but was [{"id":"p1","at":1000}]` |
| Cờ đổi giữa chừng (L-061 của mẫu) | Unit test | `DataStorePremiumPoseRepositoryTest.access_followsFlagOffAndOnAgain`: Turbine trên `isUsable(premium)` khi cờ bật: `false`; tắt cờ: `true`; bật lại: `false`. Danh sách không bị xóa: mở khóa `p2` trước khi tắt, sau khi bật lại `p2` vẫn mở |
| KB-8 tiêu chí 7: mở khóa giữ qua lần mở app sau | Unit test | `DataStorePremiumPoseRepositoryTest.unlock_thenNewRepository_staysUnlocked`: `repository().unlock("p1")`, rồi một repository mới trên cùng bộ lưu: `isUsable(premium).first() == true`. Đối chứng cùng test: `p3` premium chưa mở vẫn khóa |
| Danh sách kèm thời điểm mở, mở lại giữ thời điểm đầu | Unit test | `DataStorePremiumPoseRepositoryTest.unlock_storesTimeNewestFirst_secondUnlockKeepsFirstTime`: `now = 1000` mở `a`, `now = 2000` mở `b`, `now = 5000` mở lại `a`: chuỗi lưu là `[{"id":"b","at":2000},{"id":"a","at":1000}]` |
| Câu 9: tư thế bỏ khỏi catalog rồi quay lại vẫn mở | Unit test | `DataStorePremiumPoseRepositoryTest.unlockedId_keptWhateverTheCatalog`: mở `gone`; `access.first().unlockedIds == setOf("gone")`; một `Pose` mã `gone` premium dựng sau đó `isLocked == false` |
| `isUsable` phát lại khi mở khóa | Unit test | `DataStorePremiumPoseRepositoryTest.isUsable_turnsTrueAfterUnlock`: Turbine: `false`, `unlock("p1")`, `true`, không phát thêm (`expectNoEvents`) |
| Lỗi ghi | Unit test | `DataStorePremiumPoseRepositoryTest.unlock_writeFails_throwsIOException`: `failWrites = true`: `unlock` ném lỗi, `assertTrue(error is IOException)` (CLAUDE.md bước 10) |
| Lỗi đọc không làm dừng màn hình | Unit test | `DataStorePremiumPoseRepositoryTest.readFailsOnce_lockedThenStoredValue`: mở `p1`, `readFailures = 1`: `access.take(2).toList()` cho `unlockedIds` là `[]` rồi `["p1"]` |
| Chuỗi lưu hỏng | Unit test | `DataStorePremiumPoseRepositoryTest.corruptedUnlockedList_isEmpty_andLogsWarningWithoutIds`: đặt `poses_unlocked = "[{\"id\":\"p1\""`: `unlockedIds` rỗng, có cảnh báo thẻ `Poses`, không dòng log nào chứa `p1` |
| Log không có mã tư thế (ADR-0019) | Unit test | `DataStorePremiumPoseRepositoryTest.unlock_logsWithoutPoseId`: cờ bật, `unlock("p1")`: có dòng `INFO` thẻ `Poses` "A premium pose was unlocked"; cờ tắt: "Unlock skipped: flag_off"; không dòng nào chứa `p1` |
| Luật khóa | Unit test | `PremiumAccessTest.isLocked_onlyWhenEnabledPremiumAndNotUnlocked`: bảng 8 trường hợp (`enabled`, `premium`, đã mở): chỉ `(true, true, false)` cho `isLocked == true`; `isUsable` là phủ định |
| Hành vi danh sách đã lưu giữ nguyên sau khi tách `PoseListStore` | Unit test có sẵn | 9 test của `DataStorePoseHistoryRepositoryTest`, không sửa (mục 3.5) |
| Cờ mặc định tắt, bảng khóa khớp code | Unit test có sẵn | `DefaultFeatureFlagsTest.everyFlag_isOffByDefault`; `RemoteConfigKeysTest.keysFile_matchesCode`, `templateFile_matchesCode` qua sau khi sinh lại |
| Bản staging bật cờ | Kiểm tra thủ công (người: đọc diff một dòng của `StagingFeatureFlagsModule`) | Người duyệt PR xem có `premiumPoses()` với `@IntoSet @LocallyEnabledFlags`. Chưa có giao diện nên không thấy được trên máy; T-086 kiểm trên bản staging |
| Tài liệu khuôn catalog | Kiểm tra thủ công (người: đọc tài liệu) | Người làm nội dung đọc `docs/ops/pose-catalog.md` trước khi soạn catalog v4 |

Commit test (bước 3) có khung rỗng để test biên dịch được (`docs/auto-run.md` bước 3):

- `FeatureFlag.PREMIUM_POSES`, `Pose.premium`, `PremiumAccess` (với `isLocked` trả `false`), `PremiumPoseRepository`, `DataStorePremiumPoseRepository` (với `access = flowOf(PremiumAccess(false, emptySet()))`, `isUsable` trả `flowOf(true)`, `unlock` không làm gì) có mặt; `PoseJson` chưa có `premium`, parser chưa đọc nó; `PoseListStore` chưa có (repository lịch sử chưa đổi). Dòng enum đi cùng commit test, nên hai tệp bảng khóa sinh lại trong commit đó để `RemoteConfigKeysTest` không đỏ vì lý do khác.
- Test đỏ ở câu khẳng định với khung rỗng: `premium_readsTrue_missingAndFalseAreFree`, `premiumOnStarter_isIgnored_andLogsCountWithoutIds` (câu về dòng cảnh báo), `defaultPose_isFirstFreePose_whenFirstIsPremium`, `flagOn_premiumLocked_freeUsable`, `flagOff_everyPoseUsable_andUnlockWritesNothing` (phần đối chứng bật cờ), `access_followsFlagOffAndOnAgain`, `unlock_thenNewRepository_staysUnlocked` (phần đối chứng `p3`), `unlock_storesTimeNewestFirst_secondUnlockKeepsFirstTime`, `unlockedId_keptWhateverTheCatalog`, `isUsable_turnsTrueAfterUnlock`, `unlock_writeFails_throwsIOException`, `readFailsOnce_lockedThenStoredValue`, `corruptedUnlockedList_isEmpty_andLogsWarningWithoutIds` (câu về cảnh báo), `unlock_logsWithoutPoseId`, `PremiumAccessTest.isLocked_onlyWhenEnabledPremiumAndNotUnlocked`, và ba test script (đã thấy đỏ, mục 4.1).
- Test xanh ngay với khung rỗng, mỗi test ghi phép đột biến làm nó đỏ (L-108), chạy một lần ở bước 4 và ghi vào nhật ký:
  - `realCatalog_hasNoPremiumField_everyPoseFree`: đột biến parser đặt `premium = true` cho mọi tư thế không `starter`.
  - `defaultPose_everyPosePremium_isFirstPose`: đột biến bỏ phần `?: catalog.poses.firstOrNull()`.
  - Hai test script giữ hành vi: đột biến như dòng "Script không báo nhầm".
- Thứ tự: `Fixtures.pose(..., premium)` và `FakeFeatureFlags` vào commit test; `PoseListStore` và phần tách của repository lịch sử vào commit code, cùng lúc với cài đặt thật.

**Ngoại lệ viết test trước:**

- `StagingFeatureFlagsModule.premiumPoses()`: một dòng nối của Hilt, cùng dạng với 9 dòng có sẵn không có test riêng; không có giao diện để kiểm. Kiểm bằng mắt ở PR (dòng thủ công trên) và qua bản staging ở T-086.
- `di/PosesModule.premiumRepository(...)`: phần nối của Hilt; đồ thị phụ thuộc được kiểm khi `:app` biên dịch (`scripts/check.sh` build staging), đã biên dịch qua lúc chạy thử.
- `docs/ops/pose-catalog.md`, câu thêm ở `docs/ops/staging-and-runs.md`, bảng khóa sinh lại: tài liệu, không có test riêng ngoài `RemoteConfigKeysTest`.

### 4.1. Chạy thử

Tiêu chí E là 1, nên không bắt buộc chạy `scripts/check.sh`. Vẫn chạy thử để đoạn code ở mục 3.1 là code đã chạy, và vì test của script trên tệp mẫu phải chạy thật lúc viết plan (L-075).

- **Đã chạy thử:** 2026-10-31, 17:25 tới 17:45 UTC, trên `origin/main` (`4a5ee7e`), trong worktree của nhánh plan nhưng không commit; mọi thay đổi đã hoàn tác (`git status` sạch trước commit plan). Lời giao đề nghị một worktree tạm riêng; công cụ của phiên chỉ cho ghi trong worktree của agent, nên worktree tạm `../plan-T-085-try` đã tạo rồi gỡ ngay, không dùng.
  - Code: đúng phần Kotlin và script của mục 3.1 (`FeatureFlag.PREMIUM_POSES`, `StagingFeatureFlagsModule`, `Pose.premium`, `PoseJson.premium`, luật của parser, `defaultPose`, `PremiumAccess`, `PremiumPoseRepository`, `PoseListStore`, repository lịch sử đã tách, `DataStorePremiumPoseRepository`, `PosesModule`, `setup-check-app.sh`).
  - Test tạm `PremiumTryTest` (8 test, một lớp, rút gọn từ mục 4): cờ bật khóa tư thế premium; mở khóa qua hai lần tạo repository và giữ thời điểm đầu (`[{"id":"p1","at":1000}]`); cờ tắt thì dùng được và không ghi, bật lại thì vẫn khóa; `isUsable` (Turbine) `false` rồi `true` sau khi mở; lỗi ghi ném `IOException`; parser đọc `[true, false, false]` và có cảnh báo `premium on 1 starter`; `defaultPose` bỏ tư thế premium; catalog v3 thật không có tư thế premium.
  - Năm test mới của `scripts/tests/app.sh` (mục 4), chạy bằng một khung tạm chỉ đọc vào `app.sh` với hàm `expect` của `run.sh` (không chạy cả bộ `run.sh`).
- **Lệnh:**
  - `./gradlew -q :core:poses:testDebugUnitTest :core:config:testDebugUnitTest`
  - `./gradlew -q :core:poses:testDebugUnitTest --tests '*PremiumTryTest*'` (đối chứng)
  - `./gradlew -q :app:testStagingDebugUnitTest --tests '*RemoteConfigKeysTest*' -PremoteConfigKeys.update=true :app:processStagingDebugMainManifest :core:poses:ktlintCheck :core:config:ktlintCheck :app:ktlintCheck`
  - `bash <khung tạm> <worktree>`: đọc vào `scripts/tests/app.sh`, in `ok`/`FAIL` của từng test và số lỗi.
- **Kết quả:**
  - `:core:poses`: 81 test, 0 lỗi (`PremiumTryTest` 8/8; 9 test của `DataStorePoseHistoryRepositoryTest` qua sau khi tách `PoseListStore`, không sửa test). `:core:config`: 50 test, 0 lỗi.
  - Đối chứng (ba đột biến cùng lúc: bỏ phần đọc cờ của `unlock`, `defaultPose = catalog.poses.firstOrNull()`, bỏ luật tư thế khởi đầu của parser): 3/8 đỏ đúng lý do: `flagOff_everyPoseUsable_andUnlockWritesNothing` (`expected null, but was:<[{"id":"p1","at":1000}]>`), `defaultPose_skipsPremium` (`expected:<[f]1> but was:<[p]1>`), `parser_readsPremium_missingIsFree_starterNeverPremium` (`expected:<[true, false, false]> but was:<[true, false, true]>`). 5 test còn lại xanh.
  - `:app`: biên dịch qua (đồ thị Hilt với `PremiumPoseRepository`), `RemoteConfigKeysTest` qua sau khi sinh lại; bảng khóa đổi đúng một dòng ở `docs/remote-config-keys.md` và một khối 5 dòng ở `docs/remote-config-template.json`. ktlint của ba module qua.
  - Manifest gộp `stagingDebug`: chỉ `android:allowBackup="false"` (mục 3.3).
  - Test của script, trên script mới: 5/5 mới qua, toàn khối `app.sh` 0 lỗi. Trên script hiện tại (đã hoàn tác phần sửa): 3 đỏ đúng lý do (`a premium default pose stops, naming it: expected '1 1 0', got '0 0 1'`; `a premium starter pose stops, ...: expected '1 1 1', got '0 0 1'`; `a premium starter pose in the Remote Config catalog stops: expected '1 1', got '0 0'`), 2 test giữ hành vi qua (đối chứng).
  - Không chạy `scripts/check.sh` (E là 1, mục 5) và không chạy test của `:feature:*`: `Pose.premium` có mặc định và mọi chỗ dựng dùng tham số có tên.

## 5. Chấm rủi ro

**Điểm chặn bị chạm:** module core dùng chung (`:core:poses`, `:core:config`; điểm 7); khuôn catalog trên R2 đổi (trường mới; ticket xếp vào điểm 8, AI giữ cách xếp này dù đường dẫn và cách đặt tên không đổi). Không chạm điểm 9: khóa DataStore mới, không chuyển đổi dữ liệu cũ. Rủi ro Cao, cần duyệt chéo (Tuấn và Nhật). Điểm dưới đây chỉ để tham khảo.

| Tiêu chí | Điểm (0 đến 3) | Lý do |
| --- | --- | --- |
| A. Phạm vi thay đổi | 3 | `:core:poses`, `:core:config`, `:app` (staging), script và test của script, tài liệu; thêm interface công khai và trường công khai của `Pose` |
| B. Độ rõ của yêu cầu | 1 | Spec đã chốt (câu 6, 9, 10); AI tự quyết vài chi tiết, nêu ở mục 8 (luật phòng thủ của app, chỗ đặt tài liệu, catalog mặc định) |
| C. Khả năng kiểm chứng tự động | 0 | Mọi tiêu chí của ticket có unit test JVM hay test của script; không có giao diện |
| D. Ảnh hưởng chéo | 2 | Bảng khóa sinh lại mà T-080 cũng sửa; T-082, T-086 dựa vào API mới; `Pose` dùng ở mọi feature |
| E. Độ mới của kỹ thuật | 1 | Biến thể nhỏ của danh sách đã lưu và của kiểm catalog bằng `jq` |
| **Tổng** | 7 (tham khảo) | **Mức: Cao** (điểm chặn) |

## 6. Ảnh hưởng chéo

- File hoặc thành phần dùng chung bị chạm: `FeatureFlag.kt` (`:core:config`), `Pose.kt` và repository của `:core:poses` (mọi feature đọc tư thế), `StagingFeatureFlagsModule.kt`, `docs/remote-config-keys.md`, `docs/remote-config-template.json`, `scripts/setup-check-app.sh` (kiểm trước mọi phiên).
- Task khác trong sprint có thể xung đột:
  - T-080 (plan approved, chạy ở RUN-20261031-02 trước): sinh lại cùng hai tệp bảng khóa (`docs/remote-config-keys.md`, `docs/remote-config-template.json`). Không sửa file Kotlin chung với plan này.
  - T-082 (luồng giới thiệu, `depends_on: [T-085]`): đọc `FeatureFlag.PREMIUM_POSES`. Cờ bật cục bộ ở staging làm Remote Config staging không tắt được (mục 8, câu 4).
  - T-086 (`depends_on: [T-085, T-080]`): dùng `PremiumPoseRepository`, `PremiumAccess`, `Pose.premium`; có thể sửa `PosesModule.kt` hay test `Fixtures.kt` nếu cần, nên chạy sau.
  - Plan T-082, T-086 đang soạn song song: nếu một trong hai cũng ghi Tạo cho file của mục 3.2 (ví dụ `FakeFeatureFlags.kt` của `:core:poses`), thống nhất một plan Tạo, plan kia Sửa (L-033).
- Đề xuất thứ tự chạy: sau T-080, trước T-082 và T-086. Một task, một PR.
- Đề xuất về dự án mẫu (`docs/ops/from-app.md`): không có; mọi phần là của app (`:core:poses`, catalog, script riêng của app). Ghi điều này trong mô tả PR.

## 7. Điều kiện dừng riêng

- Bảng khóa sinh lại có thay đổi ngoài khóa `feature_premium_poses_enabled`: dừng, vì nghĩa là cờ hay khóa khác đã đổi ngoài plan.
- Một test của `DataStorePoseHistoryRepositoryTest` đỏ sau khi tách `PoseListStore`: sửa code của phần tách, không sửa test; vẫn đỏ sau 3 lần sửa thì dừng (hành vi của danh sách đã lưu phải giữ nguyên).
- Có ảnh chuẩn screenshot nào đổi (`git status --short -- '*/src/test/screenshots/'`): dừng; plan không đổi giao diện.

## 8. Câu hỏi cho người duyệt

1. **App tự giữ luật "tư thế mặc định và khởi đầu không premium".** Ticket chỉ yêu cầu script báo lỗi, nhưng script chỉ kiểm catalog của staging, và người làm nội dung có thể đẩy catalog production mà không qua nó. Đề xuất của AI: thêm hai luật nhỏ trong app (parser bỏ `premium` của tư thế `starter` và ghi cảnh báo; `defaultPose` là tư thế miễn phí đầu tiên), để camera lần đầu không bao giờ mở trên tư thế khóa. Cách khác: chỉ script, app đọc nguyên giá trị. Cách này ít code hơn, nhưng một catalog sai trên production sẽ khóa tư thế khởi đầu ngay ở lần mở đầu.
2. **Chỗ đặt tài liệu khuôn catalog.** Repo chưa có tài liệu nội dung riêng; khuôn chỉ có ở `docs/research/proposal.md` mục 11 (tài liệu khảo sát) và plan T-009. Đề xuất của AI: tạo `docs/ops/pose-catalog.md` (mục 3.1), không sửa bản đề xuất. Nếu nhóm đã có tài liệu nội dung ngoài repo, AI ghi tài liệu này ở đó thay vì tạo file mới.
3. **Sao lưu (câu 9).** Kết quả thật: `android:allowBackup="false"`, không có `dataExtractionRules` hay `fullBackupContent` (mục 3.3). Cài lại thì danh sách mất, đúng câu 9. Theo tài liệu Android, trên Android 12 trở lên `allowBackup="false"` không chặn chuyển dữ liệu trực tiếp sang máy mới, nên danh sách (và mọi dữ liệu khác của app) có thể đi theo khi người dùng chuyển máy bằng cáp hay Wi-Fi. Đề xuất của AI: giữ nguyên cấu hình, chấp nhận trường hợp này (người dùng không mất gì; spec mục 2 chỉ loại sao lưu "giữa các máy" như một tính năng, không đòi chặn). Muốn chặn thì cần thêm `dataExtractionRules` với `<device-transfer>` loại trừ dữ liệu: đổi cấu hình của cả app, không đưa vào plan nếu không được yêu cầu.
4. **Cờ bật cục bộ ở staging thì Remote Config staging không tắt được.** `StagingFeatureFlagsModule` đưa cờ vào `@LocallyEnabledFlags`, và `DefaultFeatureFlags` trả `flowOf(true)` cho cờ đó, bỏ qua giá trị Firebase (`DefaultFeatureFlagsTest.locallyEnabledFlag_staysOnWhenRemoteSaysOff`). Ticket T-082 có bước thử "tắt cờ premium trên Firebase staging thì còn 3 trang", bước này không làm được trên bản staging. Đề xuất của AI: giữ `StagingFeatureFlagsModule` như ticket và `CLAUDE.md` bước 9; plan T-082 kiểm trường hợp 3 trang bằng test JVM với cờ giả, bỏ bước tắt cờ trên Firebase. Cách khác: một cơ chế "giá trị cục bộ mà Firebase thắng" cho cờ (như `LocalRemoteValues` của khóa thường), là thay đổi của `:core:config` và của dự án mẫu, cần ticket riêng.
5. **Catalog mặc định của app.** Plan không đổi `DEFAULT_CATALOG_PATH` (vẫn v3, mọi tư thế miễn phí) và không thêm file bắt buộc, vì catalog có `premium` chỉ lên R2 sau khi merge. Khi người làm nội dung đẩy catalog v4 và đổi `poses_catalog_path` trên staging, `setup-check` tự kiểm v4 (giá trị của khóa). Bản build không đọc được Remote Config thì vẫn dùng v3, nên không có tư thế premium. Đề xuất của AI: mở một ticket nhỏ sau khi v4 có trên R2 (theo cách của T-071) để đổi `DEFAULT_CATALOG_PATH`, `docs/ops/required-assets.txt` và file mẫu của test. Đúng không?
6. **`premium` sai kiểu.** Giá trị không phải `true` hay `false` (ví dụ `"yes"`) làm app bỏ cả tư thế, như mọi trường sai kiểu hiện nay; script không báo riêng. Đề xuất của AI: giữ luật chung của parser, ghi rõ trong `docs/ops/pose-catalog.md`. Cách khác: script báo lỗi thêm cho trường hợp này (vài dòng `jq` và một test).

## 9. Quyết định duyệt

- **Quyết định:** Duyệt / Dừng / Duyệt có điều kiện
- **Người duyệt:** <tên>, **duyệt chéo:** <tên, nếu rủi ro cao>
- **Ghi chú:** <điều kiện, yêu cầu sửa, trả lời câu hỏi ở mục 8>

## 10. Nhật ký thực hiện
