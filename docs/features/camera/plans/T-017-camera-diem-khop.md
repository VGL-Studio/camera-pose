---
ticket: T-017
title: Nhận dạng dáng và điểm khớp
status: approved             # draft | approved | stopped | done
version: 1                   # tăng khi plan được sửa lại sau phản hồi hoặc sau khi dừng
risk_score:                  # tổng điểm theo risk-scoring.md, để trống nếu chạm điểm chặn
risk_level: high             # low | medium | high
token_estimate: 45M          # tổng token dự kiến, ví dụ 20M; số gợi ý: scripts/token-usage.py estimate (docs/ops/token-usage.md)
blocking_points: [sdk, analytics, core]
run_order:                   # AI điền sau họp đánh giá rủi ro
reviewer: Nhật               # người duyệt
cross_reviewer: Tuấn         # bắt buộc khi risk_level là high
approved_at: 2026-10-23
---

# PLAN T-017: NHẬN DẠNG DÁNG VÀ ĐIỂM KHỚP

## 1. Tóm tắt

Thêm nhận dạng dáng người bằng ML Kit Pose Detection chạy trên máy (luồng `ImageAnalysis` thứ ba của CameraX), so dáng với 33 điểm khớp của tư thế (`Pose.keypoints`) theo góc của tám đoạn tay chân, ra điểm 0 đến 100 và lời nhắc theo khớp lệch nhiều nhất; đạt ngưỡng (80, đặt từ xa) thì báo "Đúng dáng rồi, bấm chụp!", nét viền và viền nút chụp đổi sang màu đạt. Không có người (độ tin cậy dưới ngưỡng) thì không hiện điểm, chỉ nhắc "Lùi lại để cả người vào khung". Sau cờ `feature_pose_matching_enabled` (mặc định tắt, bản staging bật). Phần chấm điểm là hàm thuần có unit test với dáng tổng hợp; phần ML Kit và CameraX là lớp mỏng kiểm trên máy thật. `MatchHint` là thành phần mới của `:core:designsystem`. Screenshot test cùng tên ba ảnh thiết kế, ảnh so sánh `scripts/design-compare.sh camera`; số khung hình mỗi giây và dung lượng APK đo và ghi vào báo cáo phiên.

`token_estimate` 45M thay cho số gợi ý 10.9M của `scripts/token-usage.py estimate` (trung vị cỡ L): các task cỡ L của camera tốn khoảng 40M (T-014 40.7M, T-016 39.7M, `docs/metrics/token-usage.csv`); task này thêm thư viện mới, một use case CameraX, hàm chấm điểm và nhiều test tổng hợp.

## 2. Hiểu yêu cầu

Ticket xử lý KB-5 trong `features/camera/spec.md` và tham số `matched` của `photo_captured` (spec mục 6):

| Kịch bản | Tiêu chí chấp nhận | Cách hiểu của AI |
| --- | --- | --- |
| KB-5 | Khi không có người trong khung (độ tin cậy dưới ngưỡng) thì không hiện điểm; hiện "Lùi lại để cả người vào khung" | "Có người" khi mọi điểm cần cho khung hình của tư thế có `inFrameLikelihood` từ ngưỡng trở lên (mặc định 0.5, đặt từ xa): toàn thân (`Framing.FULL`): hai vai, hai hông, hai gối, hai cổ chân; nửa người (`HALF`): hai vai, hai hông; cận (`CLOSE`): mũi, hai vai. ML Kit không thấy ai cũng tính là không có người. Trạng thái `NoPerson`: `MatchHint` có biểu tượng `person_search`, không có số |
| KB-5 | Khi có người thì hiện điểm 0 đến 100 và lời nhắc ngắn (ví dụ "Nâng tay trái cao hơn") | Trạng thái `Scoring(score, hint)`. Điểm và lời nhắc cách tính ở mục 3.1. Lời nhắc chỉ đổi khi khớp lệch nhất giữ nguyên qua 3 khung liên tiếp (tránh nhảy chữ) |
| KB-5 | Khi điểm đạt ngưỡng (mặc định 80, trong `RemoteSettings`) thì lời nhắc đổi thành "Đúng dáng rồi, bấm chụp!", nét viền và viền nút chụp đổi sang màu báo đạt | Trạng thái `Matched(score)`. Vào khi điểm đã làm mượt từ ngưỡng trở lên; ra khi dưới ngưỡng trừ 5 (tránh nhấp nháy quanh ngưỡng). Nét viền `appColors.success`, viền nút chụp `appColors.successContainer` (README mục 3) |
| KB-5 | Khi một người thật đứng đúng tư thế thì điểm đạt ít nhất 80 (thủ công, báo cáo phiên thử) | Kiểm tra thủ công; cần catalog có điểm khớp thật (mục 8, câu 3) |
| KB-5 | Khi đo trên máy tầm thấp thì camera vẫn chạy ít nhất 15 khung hình mỗi giây khi bật nhận dạng | Khung phân tích nhỏ (640 x 480), bỏ khung cũ khi đang bận (`STRATEGY_KEEP_ONLY_LATEST`), ML Kit chế độ luồng (`STREAM_MODE`). Bộ phân tích ghi log `debug` "Pose analysis: <n> fps" mỗi 5 giây để đo. Đo thủ công trên máy tầm thấp (spec câu 5) |
| KB-5 | Khi tư thế không có điểm khớp, hoặc nét viền đang ẩn, thì không chạy nhận dạng, không hiện điểm | `keypoints == null` hoặc `hidden` (T-015) hoặc cờ tắt: bộ phân tích đóng khung ngay, không gọi ML Kit; trạng thái `Off` |
| KB-5, đặc biệt | Khi khung chỉ có trần nhà, tường hay đồ vật thì không hiện điểm | Như tiêu chí đầu: đòi đủ điểm theo khung hình với độ tin cậy từ ngưỡng. Kiểm bằng unit test với dáng tổng hợp độ tin cậy thấp, và thủ công |
| Spec mục 6 | `photo_captured` có `matched`: có hay không đạt ngưỡng lúc chụp | `"matched" to "true"` khi trạng thái lúc chụp là `Matched`, `"false"` trong mọi trường hợp khác, kể cả khi nhận dạng tắt (mục 8, câu 5) |
| Spec mục 8 | Cờ `feature_pose_matching_enabled`, mặc định tắt | `FeatureFlag.POSE_MATCHING`; bản staging bật trong `StagingFeatureFlagsModule` (CLAUDE.md, bước 9). Theo dõi cờ bằng `Flow` (ADR-0003): tắt thì ngừng ngay; bật lúc camera đang mở thì có hiệu lực ở lần mở camera kế tiếp (mục 8, câu 6) |

Thiết kế tham chiếu: `features/camera/design/README.md` (mục 3: "Điểm khớp", "Nét viền" đạt điểm, "Nút chụp" đạt điểm); số liệu trong `design/context/CameraScreen_matching_dark.md`, `CameraScreen_matched_dark.md`, `CameraScreen_no-person_dark.md`:

| Phần | Số liệu |
| --- | --- |
| `MatchHint` | Viên thuốc cao 56, viền `Stroke.sticker` `camera/onControl`, bo `full`, lề trái `Spacing.sm`, phải `Spacing.md` (thiết kế 6 và 16, 6 làm tròn lên 8 theo lưới 4 dp), vòng 40 viền `Stroke.sticker` cách chữ `Spacing.sm`, chữ `labelLarge` `camera/onControl`. Giữa ngang khung, đáy cách đỉnh zoom `Spacing.md` (thiết kế: đỉnh 504, cao 56; zoom 576) |
| Đang chấm (`matching`) | Nền `camera/control`, vòng `secondaryContainer`, số điểm `labelLarge` |
| Đạt (`matched`) | Nền `successContainer`, vòng `camera/control`, số điểm; nét viền `success`; viền nút chụp `successContainer` |
| Không thấy người (`no-person`) | Nền `camera/control`, vòng `camera/control`, biểu tượng `person_search` 22 |

ADR liên quan: ADR-0003 (cờ mới mặc định tắt, đọc bằng `Flow`; tham số kinh doanh qua `RemoteSettings` của core), ADR-0008 (thành phần mới trong `:core:designsystem`), ADR-0012 (sự kiện qua `:core:analytics`), ADR-0019 (log qua `AppLogger`), ADR-0020 (screenshot cùng tên ảnh thiết kế).

## 3. Giải pháp

### 3.1. Cách làm

**Điều kiện đầu vào.** T-014 và T-015 đã merge: `ActivePoseHolder`, `PoseViewModel`, `PoseOutline(color)`, `PreviewHint`, trạng thái `hidden`, `mirrored` của nét viền và tham số `hintBottomPadding` của `PoseOverlay` (plan T-015). `:core:poses` có `Pose.keypoints` 33 điểm theo thứ tự chuẩn MediaPipe (plan T-009; `docs/research/apk-analysis.md`), cùng thứ tự với chỉ số `PoseLandmark` của ML Kit. Thiếu thì dừng (mục 7).

**Thư viện.** `feature/camera/build.gradle.kts` thêm `implementation(libs.mlkit.pose.detection)` (bí danh đã có trong `gradle/libs.versions.toml`, dev thêm theo spec câu 9). Bản base, mô hình đóng gói trong APK, không tải từ Google Play Services.

**Các lớp mới trong `:feature:camera` (gói `match`).**

```kotlin
/** One landmark in frame space: x, y from 0 to 1 as the user sees the preview (mirrored for the front camera). */
data class BodyLandmark(val x: Float, val y: Float, val inFrame: Float)

/** Landmarks by MediaPipe index (0 nose to 32 right foot index); missing when ML Kit did not return it. */
data class BodyPose(val landmarks: Map<Int, BodyLandmark>, val mirrored: Boolean)

/** Frames analysed by ML Kit; null while the camera is closed or analysis is off. */
interface PoseFrameSource {
    val frames: StateFlow<BodyPose?>
    /** Runs detection only while active; otherwise each frame is closed at once. */
    fun setActive(active: Boolean)
}

sealed interface MatchUiState {
    data object Off : MatchUiState
    data object NoPerson : MatchUiState
    data class Scoring(val score: Int, val hint: MatchHintText) : MatchUiState
    data class Matched(val score: Int) : MatchUiState
}

enum class MatchHintText { RAISE_LEFT_ARM, LOWER_LEFT_ARM, RAISE_RIGHT_ARM, LOWER_RIGHT_ARM, BEND_LEFT_ELBOW, STRAIGHTEN_LEFT_ARM, BEND_RIGHT_ELBOW, STRAIGHTEN_RIGHT_ARM, WIDEN_LEFT_LEG, CLOSE_LEFT_LEG, WIDEN_RIGHT_LEG, CLOSE_RIGHT_LEG, BEND_LEFT_KNEE, STRAIGHTEN_LEFT_LEG, BEND_RIGHT_KNEE, STRAIGHTEN_RIGHT_LEG }
```

**Chấm điểm (`match/PoseScorer.kt`, thuần, không phụ thuộc Android).**

- `isPersonPresent(pose: BodyPose?, framing: Framing, minInFrame: Float): Boolean`: đủ điểm theo khung hình (mục 2) với `inFrame >= minInFrame`.
- `score(template: List<Keypoint>, templateMirrored: Boolean, detected: BodyPose, minInFrame: Float): ScoreResult(score: Int, worst: MatchHintText?)`:
  - Hai dáng đưa về "không gian màn hình" (như người dùng thấy): mẫu lật ngang khi nét viền đang lật (T-015); dáng nhận được đã lật sẵn với camera trước. Ghép đoạn theo bên trên màn hình: khi đúng một trong hai dáng bị lật, đổi trái phải của chỉ số mẫu (người đứng soi gương khớp tay trái với tay bên trái màn hình của nét viền).
  - Tám đoạn: cánh tay trên (vai, khuỷu), cẳng tay (khuỷu, cổ tay), đùi (hông, gối), cẳng chân (gối, cổ chân), mỗi bên. Một đoạn được tính khi cả hai điểm của mẫu có `visibility` từ 0.5 và cả hai điểm nhận được có `inFrame` từ ngưỡng.
  - Lệch của một đoạn: góc giữa hai hướng (0 đến 180 độ). Điểm của đoạn: `max(0, 1 - lệch / 60)`. Điểm tổng: trung bình các đoạn được tính, nhân 100, làm tròn. Không đoạn nào được tính: `score = 0`, `worst = null`. Chỉ dùng góc nên không phụ thuộc vị trí và cỡ nét viền (người dùng kéo, chụm ở T-015).
  - Lời nhắc theo đoạn lệch nhiều nhất, gọi tên theo bên thật của người dùng (nhãn của ML Kit): cánh tay trên chỉ xuống thấp hơn mẫu thì `RAISE_*_ARM`, cao hơn thì `LOWER_*_ARM`; cẳng tay: góc khuỷu lớn hơn mẫu thì `BEND_*_ELBOW`, nhỏ hơn thì `STRAIGHTEN_*_ARM`; đùi: xa trục người hơn mẫu thì `CLOSE_*_LEG`, gần hơn thì `WIDEN_*_LEG`; cẳng chân: góc gối lớn hơn mẫu thì `BEND_*_KNEE`, nhỏ hơn thì `STRAIGHTEN_*_LEG`.

**`match/PoseMatchViewModel.kt` (tạo, `@HiltViewModel`).** Nhận `PoseFrameSource`, `ActivePoseHolder`, `FeatureFlags`, `PoseMatchingSettings`, `PoseMatchHolder`, `AppLogger`. `uiState: StateFlow<MatchUiState>`.

- `combine(featureFlags.enabled(POSE_MATCHING), activePose.current, activePose.outline, settings.scoreThreshold, settings.minInFramePercent)`: `active = cờ bật && pose?.keypoints != null && outline.visible`. `frameSource.setActive(active)`; không `active` thì `Off`. Log `info` "Pose matching on" hoặc "Pose matching off: <lý do>" (cờ, không có điểm khớp, nét viền ẩn) mỗi lần đổi.
- Thu `frameSource.frames` khi `active`: `null` thì `Off` (camera đóng). Không có người (3 khung liên tiếp, tránh nhấp nháy) thì `NoPerson`. Có người: `score(...)`, làm mượt `smoothed = 0.5 * score + 0.5 * smoothed` (khung có người đầu tiên lấy thẳng); `smoothed >= threshold` thì `Matched`, đang `Matched` mà `smoothed < threshold - 5` thì ra; còn lại `Scoring(smoothed, hint)` với `hint` đổi khi `worst` mới giữ 3 khung liên tiếp. Đổi tư thế thì đặt lại phần làm mượt.
- `PoseMatchHolder.matched` (`@Singleton`, `StateFlow<Boolean>`) bằng `uiState is Matched`, để `CameraViewModel` đọc lúc chụp. Log `info` "Pose matched" khi vào trạng thái đạt (một lần mỗi lần vào), không log mỗi khung.

**Bộ phân tích (`match/MlKitPoseAnalyzer.kt`, tạo, `@Singleton`, cài `PoseFrameSource` và `ImageAnalysis.Analyzer`).** Lớp mỏng trên ML Kit, kiểm trên máy (như `CameraXController`, plan T-013 mục 4):

- `PoseDetection.getClient(PoseDetectorOptions.Builder().setDetectorMode(STREAM_MODE).build())` tạo lần đầu khi `setActive(true)`; `close()` khi camera đóng hẳn (`release`).
- `analyze(image)`: không `active` thì `image.close()` ngay. Ngược lại `InputImage.fromMediaImage(image.image, rotationDegrees)`, `process`, khi xong: đưa mỗi `PoseLandmark` về 0 đến 1 theo cỡ ảnh đã xoay (đổi rộng cao khi xoay 90 hoặc 270), lật `x` khi camera trước, phát `BodyPose`; lỗi ML Kit: log `warn` "Pose detection failed" một lần mỗi 10 giây, phát `null`; luôn `image.close()`.
- Đếm khung đã xử lý; mỗi 5 giây log `debug` "Pose analysis: <n> fps" (để đo trên máy thật).
- `lens` do `CameraXController` đặt khi mở camera; `reset()` khi đóng camera: `frames` về `null`.

**`camera/CameraXController.kt` (sửa).** Constructor thêm `MlKitPoseAnalyzer`, `FeatureFlags`. Trong `open()`: `val analysis = featureFlags.enabled(FeatureFlag.POSE_MATCHING).first()`; bật thì tạo `ImageAnalysis` (`ResolutionSelector` tỷ lệ 4:3, `ResolutionStrategy(Size(640, 480), FALLBACK_RULE_CLOSEST_LOWER_THEN_HIGHER)`, `STRATEGY_KEEP_ONLY_LATEST`, `setAnalyzer(executor một luồng, analyzer)`), gắn cùng `preview`, `capture`; tắt thì như cũ. `analyzer.lens = lens`. `bindToLifecycle` ba use case lỗi (máy không hỗ trợ tổ hợp) thì log `warn` "Pose analysis not supported on this camera", gắn lại chỉ hai use case như cũ (camera vẫn chụp được, nhận dạng `Off`). `close()` gọi `analyzer.reset()`; `release()` đóng bộ nhận dạng. `CameraController` (interface) không đổi.

**`ActivePoseHolder` (sửa).** Thêm `outline: StateFlow<OutlineView>` với `data class OutlineView(val visible: Boolean = true, val mirrored: Boolean = false)` và `setOutline(view)`. `PoseViewModel.publish()` (T-015 có `hidden`, `mirrored`) gọi `activePose.setOutline(OutlineView(!hidden, mirrored))`.

**`CameraViewModel` (sửa nhỏ).** Constructor thêm `PoseMatchHolder`. `photo_captured` thêm `"matched" to matchHolder.matched.value.toString()`, đọc lúc chụp xong như `pose_id`.

**`:core:config` (sửa).**

- `FeatureFlag.POSE_MATCHING("feature_pose_matching_enabled")`.
- `PoseMatchingSettings.kt` (tạo), theo mẫu `AppLinks`: `interface PoseMatchingSettings { val scoreThreshold: Flow<Int>; val minInFramePercent: Flow<Int> }`, `internal class DefaultPoseMatchingSettings` đọc `RemoteSettings.long("pose_match_threshold", 80)` (chặn 1 đến 100) và `long("pose_match_min_in_frame_percent", 50)` (chặn 1 đến 99). Giá trị ngoài khoảng (gõ nhầm trên Remote Config) thì dùng mặc định.
- `di/ConfigModule.kt`: `@Binds` cho `PoseMatchingSettings`.

**`app/src/staging/.../StagingFeatureFlagsModule.kt` (sửa).** Thêm `POSE_MATCHING`, để bản staging có nhận dạng.

**Màn hình.**

- `CameraRoute`: thêm `matchViewModel: PoseMatchViewModel = hiltViewModel()`, thu `uiState`, truyền `matchState`.
- `CameraScreen`: tham số mới có mặc định ở cuối `matchState: MatchUiState = MatchUiState.Off`; truyền cho `PoseOverlay` và `CaptureRow`.
- `PoseOverlay` (T-015): `PoseOutline(color = if (matchState is Matched) appColors.success else appColors.cameraOverlayStroke)`; `MatchHint` giữa ngang, `padding(bottom = hintBottomPadding)` (cùng chỗ với lời nhắc khóa của T-015). Lời nhắc khóa đang hiện thì không vẽ `MatchHint` (hai thứ cùng chỗ; lời nhắc khóa chỉ hiện 2 giây, plan T-015 câu 2). Bảng chỉnh đang mở thì không vẽ `MatchHint` (bảng che chỗ đó). Thẻ test `MATCH_HINT`.
- `CaptureRow` (`CameraControls.kt`): `matched: Boolean = false`; `ShutterButton` nhận `ringColor`, `successContainer` khi đạt. Thẻ test không đổi.

**Thành phần mới trong `:core:designsystem` (`component/MatchHint.kt`).** `MatchHint(text: String, modifier, score: Int? = null, matched: Boolean = false, icon: Painter? = null)`: như số liệu ở mục 2; `score` có thì vòng ghi số, không thì vẽ `icon`. Ngữ nghĩa: một node gộp, đọc "<điểm> điểm, <lời nhắc>" hoặc lời nhắc; `liveRegion = Polite` để TalkBack đọc khi lời nhắc đổi (mục 8, câu 7). Thêm vào cuối `CameraComponentCatalog` ba dạng (đang chấm 68, đạt 92, không thấy người); ghi lại `DesignSystem_camera_components_dark.png`. Biểu tượng `ic_person_search.xml` (core, Material Symbols).

**Chữ mới (`strings.xml` của feature).** Theo bảng chữ của thiết kế: `camera_match_matched` "Đúng dáng rồi, bấm chụp!", `camera_match_no_person` "Lùi lại để cả người vào khung", `camera_match_raise_left_arm` "Nâng tay trái cao hơn". AI đề xuất (mục 8, câu 4): `camera_match_lower_left_arm` "Hạ tay trái thấp hơn", `camera_match_raise_right_arm` "Nâng tay phải cao hơn", `camera_match_lower_right_arm` "Hạ tay phải thấp hơn", `camera_match_bend_left_elbow` "Gập khuỷu tay trái thêm", `camera_match_straighten_left_arm` "Duỗi thẳng tay trái", `camera_match_bend_right_elbow` "Gập khuỷu tay phải thêm", `camera_match_straighten_right_arm` "Duỗi thẳng tay phải", `camera_match_widen_left_leg` "Đưa chân trái ra xa hơn", `camera_match_close_left_leg` "Khép chân trái vào", `camera_match_widen_right_leg` "Đưa chân phải ra xa hơn", `camera_match_close_right_leg` "Khép chân phải vào", `camera_match_bend_left_knee` "Gập gối trái thêm", `camera_match_straighten_left_leg` "Duỗi thẳng chân trái", `camera_match_bend_right_knee` "Gập gối phải thêm", `camera_match_straighten_right_leg` "Duỗi thẳng chân phải", `camera_cd_match_score` "%1$d điểm, %2$s".

**Thứ tự làm.** Bước 3: khung rỗng (mục 4), mọi test, chữ đủ; chạy để thấy thất bại đúng lý do; commit. Bước 4: `:core:config` (cờ, cài đặt, module); `MatchHint`, biểu tượng, catalog; `PoseScorer`; `PoseMatchViewModel`, `PoseMatchHolder`; `ActivePoseHolder`, `PoseViewModel`; `MlKitPoseAnalyzer`, `CameraXController`, `build.gradle.kts`; `CameraViewModel`; `PoseOverlay`, `CameraControls`, `CameraScreen`, `CameraRoute`; `StagingFeatureFlagsModule`; `scripts/check.sh` ghi ảnh chuẩn; `scripts/design-compare.sh camera`; đo APK (`:app:assembleStagingRelease` trên nhánh chính và trên nhánh task, `unzip -l` phần `lib/arm64-v8a/` và `assets/`), so manifest gộp trước và sau (`:app:processStagingDebugMainManifest`).

### 3.2. Danh sách file

| Thao tác | Đường dẫn | Nội dung thay đổi |
| --- | --- | --- |
| Sửa | `feature/camera/build.gradle.kts` | `implementation(libs.mlkit.pose.detection)` |
| Tạo | `feature/camera/src/main/kotlin/com/vglstudio/camera/pose/line/guide/feature/camera/match/BodyPose.kt` | `BodyLandmark`, `BodyPose`, `PoseFrameSource` |
| Tạo | `feature/camera/src/main/kotlin/com/vglstudio/camera/pose/line/guide/feature/camera/match/MatchUiState.kt` | `MatchUiState`, `MatchHintText` |
| Tạo | `feature/camera/src/main/kotlin/com/vglstudio/camera/pose/line/guide/feature/camera/match/PoseScorer.kt` | `isPersonPresent`, `score`, `ScoreResult` |
| Tạo | `feature/camera/src/main/kotlin/com/vglstudio/camera/pose/line/guide/feature/camera/match/PoseMatchViewModel.kt` | ViewModel theo mục 3.1 |
| Tạo | `feature/camera/src/main/kotlin/com/vglstudio/camera/pose/line/guide/feature/camera/match/PoseMatchHolder.kt` | `matched` lúc chụp |
| Tạo | `feature/camera/src/main/kotlin/com/vglstudio/camera/pose/line/guide/feature/camera/match/MlKitPoseAnalyzer.kt` | Lớp mỏng trên ML Kit |
| Tạo | `feature/camera/src/main/kotlin/com/vglstudio/camera/pose/line/guide/feature/camera/match/MatchHints.kt` | `MatchHintText` sang chuỗi; `MatchHintOverlay` vẽ `MatchHint` theo `MatchUiState` |
| Tạo | `feature/camera/src/main/kotlin/com/vglstudio/camera/pose/line/guide/feature/camera/di/MatchModule.kt` | Gắn `PoseFrameSource` vào `MlKitPoseAnalyzer` (file riêng, không chạm `CameraModule.kt`, `PoseModule.kt`) |
| Sửa | `feature/camera/src/main/kotlin/com/vglstudio/camera/pose/line/guide/feature/camera/camera/CameraXController.kt` | Use case `ImageAnalysis` khi cờ bật, đóng mở bộ phân tích |
| Sửa | `feature/camera/src/main/kotlin/com/vglstudio/camera/pose/line/guide/feature/camera/pose/ActivePoseHolder.kt` | `OutlineView`, `outline`, `setOutline` |
| Sửa | `feature/camera/src/main/kotlin/com/vglstudio/camera/pose/line/guide/feature/camera/pose/PoseViewModel.kt` | `publish()` đặt `OutlineView` |
| Sửa | `feature/camera/src/main/kotlin/com/vglstudio/camera/pose/line/guide/feature/camera/pose/PoseOverlay.kt` | Màu nét viền khi đạt, `MatchHint` |
| Sửa | `feature/camera/src/main/kotlin/com/vglstudio/camera/pose/line/guide/feature/camera/CameraViewModel.kt` | Nhận `PoseMatchHolder`; `matched` của `photo_captured` |
| Sửa | `feature/camera/src/main/kotlin/com/vglstudio/camera/pose/line/guide/feature/camera/CameraScreen.kt` | `CameraRoute` thêm `PoseMatchViewModel`; `matchState`; thẻ `MATCH_HINT` |
| Sửa | `feature/camera/src/main/kotlin/com/vglstudio/camera/pose/line/guide/feature/camera/CameraControls.kt` | `CaptureRow(matched)`, `ShutterButton(ringColor)` |
| Sửa | `feature/camera/src/main/res/values/strings.xml` | Chữ mới ở mục 3.1 |
| Tạo | `feature/camera/src/test/kotlin/com/vglstudio/camera/pose/line/guide/feature/camera/match/PoseScorerTest.kt` | Unit test chấm điểm với dáng tổng hợp |
| Tạo | `feature/camera/src/test/kotlin/com/vglstudio/camera/pose/line/guide/feature/camera/match/TestBodyPoses.kt` | Dáng tổng hợp: đứng dang tay, tay trái hạ, chân dang, độ tin cậy thấp, chỉ có đồ vật |
| Tạo | `feature/camera/src/test/kotlin/com/vglstudio/camera/pose/line/guide/feature/camera/match/PoseMatchViewModelTest.kt` | Unit test ViewModel với nguồn khung giả |
| Tạo | `feature/camera/src/test/kotlin/com/vglstudio/camera/pose/line/guide/feature/camera/match/MatchFakes.kt` | `FakePoseFrameSource`, `FakeFeatureFlags`, `FakePoseMatchingSettings` |
| Tạo | `feature/camera/src/test/kotlin/com/vglstudio/camera/pose/line/guide/feature/camera/CameraScreenMatchTest.kt` | Test giao diện phần điểm khớp (file riêng) |
| Sửa | `feature/camera/src/test/kotlin/com/vglstudio/camera/pose/line/guide/feature/camera/CameraViewModelTest.kt` | Hàm dựng truyền `PoseMatchHolder()`; hai test mới |
| Sửa | `feature/camera/src/test/kotlin/com/vglstudio/camera/pose/line/guide/feature/camera/pose/PoseViewModelTest.kt` | Một test mới cho `OutlineView` |
| Sửa | `feature/camera/src/test/kotlin/com/vglstudio/camera/pose/line/guide/feature/camera/CameraScreenScreenshotTest.kt` | Hàm `Screen` nhận `matchState`; bốn test mới |
| Tạo | `feature/camera/src/test/screenshots/CameraScreen_matching_dark.png` | Ảnh chuẩn mới, cùng tên ảnh thiết kế |
| Tạo | `feature/camera/src/test/screenshots/CameraScreen_matched_dark.png` | Ảnh chuẩn mới, cùng tên ảnh thiết kế |
| Tạo | `feature/camera/src/test/screenshots/CameraScreen_no-person_dark.png` | Ảnh chuẩn mới, cùng tên ảnh thiết kế |
| Tạo | `feature/camera/src/test/screenshots/CameraScreen_matching_small.png` | Khung 360 x 640, nhiều nội dung nhất (L-028) |
| Ghi lại | `docs/features/camera/design/review/` | Sinh bởi `scripts/design-compare.sh camera`, không sửa tay |
| Sửa | `core/config/src/main/kotlin/com/vglstudio/camera/pose/line/guide/core/config/FeatureFlag.kt` | `POSE_MATCHING` |
| Tạo | `core/config/src/main/kotlin/com/vglstudio/camera/pose/line/guide/core/config/PoseMatchingSettings.kt` | Interface và `DefaultPoseMatchingSettings` |
| Sửa | `core/config/src/main/kotlin/com/vglstudio/camera/pose/line/guide/core/config/di/ConfigModule.kt` | `@Binds` cho `PoseMatchingSettings` |
| Tạo | `core/config/src/test/kotlin/com/vglstudio/camera/pose/line/guide/core/config/DefaultPoseMatchingSettingsTest.kt` | Mặc định, giá trị từ xa, giá trị ngoài khoảng |
| Tạo | `core/designsystem/src/main/kotlin/com/vglstudio/camera/pose/line/guide/core/designsystem/component/MatchHint.kt` | Thành phần mới |
| Tạo | `core/designsystem/src/main/res/drawable/ic_person_search.xml` | Biểu tượng Material Symbols |
| Sửa | `core/designsystem/src/main/kotlin/com/vglstudio/camera/pose/line/guide/core/designsystem/catalog/DesignSystemCatalog.kt` | Ba dạng `MatchHint` cuối `CameraComponentCatalog` |
| Tạo | `core/designsystem/src/test/kotlin/com/vglstudio/camera/pose/line/guide/core/designsystem/MatchHintTest.kt` | Test giao diện thành phần |
| Ghi lại | `core/designsystem/src/test/screenshots/DesignSystem_camera_components_dark.png` | Thêm `MatchHint` |
| Sửa | `app/src/staging/kotlin/com/vglstudio/camera/pose/line/guide/di/StagingFeatureFlagsModule.kt` | Bật `POSE_MATCHING` ở staging |

Không file nào khác. Đặc biệt không sửa: `gradle/libs.versions.toml` (bí danh đã có), `build-logic/`, `app/src/main/AndroidManifest.xml` (không thêm quyền), `CameraController.kt` (interface giữ nguyên), `CameraFakes.kt`, `CameraScreenTest.kt`, `CameraScreenPoseTest.kt`, `CameraScreenOverlayTest.kt` (T-015), mọi file của màn xem lại; `:core:poses`; `docs/design-system/tokens.json` (lề 6 của thiết kế làm tròn về `Spacing.sm`, mục 2).

**Nơi khác đọc các file trên** (L-020), không cần sửa:

- `feature/camera/build.gradle.kts`: `checkModuleDependencies` chỉ xét phụ thuộc module (`project(...)`); thư viện ngoài không ảnh hưởng.
- `FeatureFlag.kt`: `DefaultFeatureFlagsTest.kt` kiểm mọi cờ mặc định tắt (cờ mới mặc định tắt nên qua) và đặt tên `feature_<tên>_enabled` (đúng tên spec).
- `ConfigModule.kt`: Hilt; chỉ thêm một `@Binds`.
- `ActivePoseHolder.kt`: `CameraViewModelTest.kt`, `PoseViewModelTest.kt` dựng `ActivePoseHolder()`; constructor không đổi.
- `CameraViewModel.kt`: chỉ `CameraViewModelTest.kt` (trong danh sách) dựng ViewModel; Hilt cung cấp `PoseMatchHolder`. Test cũ so `photo_captured` bằng cả bộ tham số: phải thêm `"matched" to "false"` vào các map mong đợi trong cùng file (commit test của bước 3, L-025).
- `CameraXController.kt`: không có test JVM (plan T-013 mục 4); Hilt cung cấp `MlKitPoseAnalyzer`, `FeatureFlags`.
- `CameraScreen.kt`, `CameraControls.kt`, `PoseOverlay.kt`: test của T-013, T-014, T-015 gọi không có `matchState`, `matched` (mặc định `Off`, `false`), nên giao diện như cũ; `AppLaunchTest.kt` chỉ chờ `camera_screen`.
- `StagingFeatureFlagsModule.kt`: chỉ Hilt đọc.
- `DesignSystemCatalog.kt`: chỉ `DesignSystemCatalogScreenshotTest.kt` gọi; chỉ trang camera đổi.

### 3.3. Cờ tính năng, quảng cáo, dữ liệu, quyền, thư viện

- Cờ tính năng: `feature_pose_matching_enabled` (`FeatureFlag.POSE_MATCHING`, mặc định tắt, spec mục 8), bật ở staging. Tham số từ xa (không phải cờ, ADR-0003): `pose_match_threshold` (mặc định 80, spec câu 4), `pose_match_min_in_frame_percent` (mặc định 50).
- Quảng cáo: Không chạm.
- Sự kiện phân tích (điểm chặn 4, đã có trong spec mục 6): `photo_captured` thêm `matched` là `true` hoặc `false`. Không sự kiện mới.
- Dữ liệu: khung hình phân tích hoàn toàn trên máy, không lưu, không gửi đi (spec mục 6). Thư viện ML Kit có thể gửi số liệu dùng thư viện cho Google (mục 8, câu 2); không có dữ liệu ảnh hay điểm khớp nào ra khỏi máy.
- File R2: Không chạm (điểm khớp nằm trong catalog sẵn có).
- **Quyền truy cập:** Không thêm. Phân tích khung dùng quyền `CAMERA` đã có (KB-7). Thư viện ML Kit không kéo quyền mới vào manifest gộp (so `:app:processStagingDebugMainManifest` trước và sau, mục 4.1); nó thêm `MlKitInitProvider` (khởi tạo ML Kit lúc mở app), `MlKitComponentDiscoveryService`, `MlKitRemoteWorkerService` và `androidx.work.multiprocess.RemoteWorkManagerService`. Không thêm: quyền nào khác ngoài `CAMERA` đã có.
- **Thư viện và SDK:** `com.google.mlkit:pose-detection:18.0.0-beta5` (bí danh `libs.mlkit.pose.detection`, đã có trong `gradle/libs.versions.toml`), thêm vào `:feature:camera`; giấy phép: điều khoản ML Kit của Google (Google APIs Terms of Service, như Firebase đang dùng). Các lớp `androidx.camera.core.ImageAnalysis`, `ImageProxy` có trong `androidx-camera-core` đã có trên classpath của `:feature:camera`. Thư viện này kéo theo `androidx.appcompat` (tài nguyên `abc_*`) và WorkManager đa tiến trình. Dung lượng thêm vào APK: mục 4.1.

### 3.4. Phương án dự phòng

- **Điểm chưa chắc:** gắn ba use case (`Preview`, `ImageCapture`, `ImageAnalysis`) cùng lúc trên máy cấp phần cứng `LEGACY`, và chạy ML Kit trên khung 640 x 480 đủ nhanh trên máy tầm thấp.
- **Điều kiện chuyển:** trong phiên không có máy thật, nên phương án này dành cho lần sửa sau phản hồi: dev đo dưới 15 khung hình mỗi giây, hoặc log "Pose analysis not supported on this camera" xuất hiện trên máy tầm thấp. Trong phiên, test ở mục 4 không phụ thuộc phần này.
- **Cách làm thay:** trong `MlKitPoseAnalyzer.kt`, chỉ phân tích một khung mỗi 100 ms (bỏ các khung giữa, như AI Posing, `docs/research/proposal.md`), và trong `CameraXController.kt` hạ khung phân tích xuống 480 x 360. Tiêu chí và test giữ nguyên.

## 4. Kiểm thử

Mọi test dưới đây vào commit test của bước 3, cùng khung rỗng để biên dịch được: `PoseScorer` có đủ hàm, `isPersonPresent` trả `false`, `score` trả `ScoreResult(0, null)`; `PoseMatchViewModel` có `uiState` luôn `Off`; `PoseMatchHolder.matched` luôn `false`; `MlKitPoseAnalyzer` có đủ hàm, không làm gì; `ActivePoseHolder.outline` luôn mặc định; `CameraViewModel` đã nhận `PoseMatchHolder` nhưng chưa ghi `matched`; `PoseMatchingSettings` trả mặc định; `MatchHint` vẽ `Box(modifier)` rỗng; `CameraScreen`, `CaptureRow` đã có tham số mới; chữ đủ. Phần sửa trong `CameraViewModelTest.kt` (hàm dựng, thêm `"matched" to "false"` vào map mong đợi của test cũ, hai test mới) và `CameraScreenScreenshotTest.kt` cùng vào commit test của bước 3 (L-025). Lý do thất bại ở cột cuối là điều AI dự kiến; bước 3 ghi lý do thật vào nhật ký.

Dáng tổng hợp (`TestBodyPoses.kt`): toạ độ 0 đến 1 tự dựng cho 33 điểm theo thứ tự MediaPipe: `standingArmsOut` (đứng thẳng, hai tay dang ngang), `leftArmDown` (như trên, cánh tay trái chỉ xuống), `legsApart`, `lowConfidence` (đủ điểm nhưng `inFrame` 0.2), `upperBodyOnly` (chân thiếu), `empty`. Mẫu là cùng các dáng ở dạng `Keypoint` (`visibility` 0.9). Không cần ảnh, không cần ML Kit.

| Tiêu chí chấp nhận | Loại test | Tên test hoặc cách kiểm tra |
| --- | --- | --- |
| KB-5, không có người | Unit test | `PoseScorerTest.noLandmarks_isNotPresent`, `lowConfidence_isNotPresent` (ngưỡng 0.5), `fullBodyPose_withoutAnkles_isNotPresent`, `halfBodyPose_withoutLegs_isPresent`. Đối chứng cạnh nhau: `standingArmsOut` có người (L-035). Thất bại: khung rỗng trả `false` cho cả dáng có người |
| KB-5, đặc biệt: đồ vật | Unit test | `objectsOnly_isNotPresent`: chỉ có vài điểm rời rạc độ tin cậy 0.3 đến 0.45 (cách ML Kit thường trả khi khung chỉ có tường, trần) |
| KB-5, điểm 0 đến 100 | Unit test | `samePose_scoresAtLeast95`; `samePose_movedAndScaled_scoresTheSame` (dời 0.2, nhân cỡ 0.6: chênh không quá 1); `leftArmDown_scoresLower_andHintsRaiseLeftArm`: điểm từ 60 đến 90, `worst == RAISE_LEFT_ARM`; `oppositeArms_scoresBelow50`; `score_isWithin0And100` với mọi cặp dáng tổng hợp |
| KB-5, lời nhắc đúng bên | Unit test | `frontCamera_mirroredDetection_matchesAsInAMirror`: `leftArmDown` lật ngang với `mirrored = true` so với mẫu không lật: điểm như camera sau, lời nhắc vẫn `RAISE_LEFT_ARM` (tay trái thật của người dùng). `flippedOutline_swapsSides`: mẫu lật (T-015) và dáng nhận được không lật: so tay trái người dùng với tay bên trái màn hình của nét viền |
| KB-5, nửa người | Unit test | `upperBodyOnly_scoresArmsOnly`: dáng nửa người đúng: điểm từ 95; lời nhắc không bao giờ là của chân |
| KB-5, cờ, điểm khớp, ẩn | Unit test | `PoseMatchViewModelTest.flagOff_isOff_andSourceInactive`; `poseWithoutKeypoints_isOff`; `outlineHidden_isOff_andSourceInactive`; `allOn_activatesSource`. Đổi từng điều kiện khi đang chạy: `source.active` đổi theo, trạng thái về `Off`. Thất bại: khung rỗng không gọi `setActive(true)` |
| KB-5, không có người trên màn hình | Unit test | `threeFramesWithoutPerson_isNoPerson`: một khung không người sau khung có người vẫn `Scoring`; ba khung liên tiếp: `NoPerson` |
| KB-5, chấm điểm và lời nhắc | Unit test | `personPresent_isScoring_withSmoothedScore`: khung đầu điểm 70 rồi 90: `Scoring(70)` rồi `Scoring(80)`; `hint_changesOnlyAfterThreeFrames` |
| KB-5, đạt ngưỡng | Unit test | `reachingThreshold_isMatched_andHolderTrue`: điểm làm mượt từ 80: `Matched`, `matchHolder.matched.value`; `matched_leavesOnlyBelowThresholdMinus5` (78 vẫn `Matched`, 74 ra). `remoteThreshold_isUsed`: ngưỡng 60 thì 65 là `Matched` |
| Camera đóng | Unit test | `framesNull_isOff`; `poseChange_resetsSmoothing` |
| Tham số từ xa | Unit test | `DefaultPoseMatchingSettingsTest.defaults_are80And50`; `remoteValues_areUsed`; `outOfRange_fallsBackToDefault` (0, 101, -5) |
| `photo_captured` có `matched` | Unit test | `CameraViewModelTest.shutter_whenMatched_logsMatchedTrue`: `matchHolder.set(true)`; sự kiện có `"matched" to "true"`. `shutter_notMatched_logsMatchedFalse`. Các test cũ của `photo_captured` mong đợi thêm `"matched" to "false"`. Thất bại: khung rỗng chưa ghi `matched` |
| `OutlineView` từ T-015 | Unit test | `PoseViewModelTest.hiddenAndMirrored_areSharedWithMatching`: `onToggleHidden()`: `activePose.outline.value.visible == false`; `onToggleMirror()`: `mirrored == true` |
| KB-5, đang chấm trên màn hình | Compose UI test | `CameraScreenMatchTest.scoring_showsScoreAndHint`: `Scoring(68, RAISE_LEFT_ARM)`: có "68", "Nâng tay trái cao hơn"; node `MATCH_HINT` đọc "68 điểm, Nâng tay trái cao hơn"; nét viền màu trắng (điểm ảnh giữa nét của ảnh tạm là #FFFFFF) |
| KB-5, đạt trên màn hình | Compose UI test, `@GraphicsMode(NATIVE)` | `matched_showsMessage_greenOutline_andShutterRing`: `Matched(92)`: có "92", "Đúng dáng rồi, bấm chụp!"; điểm ảnh trên nét viền là #3DDC97; điểm ảnh trên viền nút chụp (2 dp vào trong mép `SHUTTER`) là #3DDC97; `Scoring` thì #FFFFFF (đối chứng) |
| KB-5, không thấy người trên màn hình | Compose UI test | `noPerson_showsHintWithoutScore`: có "Lùi lại để cả người vào khung", không có node chữ số |
| KB-5, tắt | Compose UI test | `off_noMatchHint`: `Off`: không có `MATCH_HINT` |
| Chỗ của lời nhắc | Compose UI test | `matchHint_isAboveZoom`: đáy `MATCH_HINT` cách đỉnh `ZOOM` 16 dp (sai số 2 dp), giữa ngang `PREVIEW`. `lockHintVisible_hidesMatchHint`; `panelOpen_hidesMatchHint` |
| Chữ đúng bảng chữ của thiết kế | Compose UI test | Các test trên so đúng chuỗi của README mục 2 ("Nâng tay trái cao hơn", "Đúng dáng rồi, bấm chụp!", "Lùi lại để cả người vào khung"); chữ AI đề xuất so theo mục 3.1 |
| Màn hình thấp (L-028) | Compose UI test, `@Config(qualifiers = "w360dp-h640dp-xxhdpi")` | `small_matchHintInsidePreview_clearOfStepCardAndZoom`: `Scoring` với lời nhắc dài nhất ("Đưa chân phải ra xa hơn"): `MATCH_HINT` nằm trọn trong `PREVIEW`, không chồng `STEP_CARD`, `ZOOM`; chữ xuống dòng không bị cắt |
| Thành phần `MatchHint` | Compose UI test, `@GraphicsMode(NATIVE)` | `MatchHintTest.height56_circle40`; `scoring_isWhiteWithYellowCircle` (điểm giữa vòng #FFC83D, điểm trên nền viên thuốc #FFFFFF); `matched_isGreenWithWhiteCircle` (#3DDC97, vòng #FFFFFF); `icon_insteadOfScore`; `semantics_readsScoreAndText`, `liveRegion_isPolite` |
| Giao diện catalog | Screenshot test, ghi lại | `DesignSystemCatalogScreenshotTest.cameraComponents_dark` (`src/test/screenshots/DesignSystem_camera_components_dark.png`), thêm ba `MatchHint` |
| Đúng thiết kế, ba ảnh mới | Screenshot test, `@GraphicsMode(NATIVE)`, `@Config(qualifiers = "w411dp-h891dp-xxhdpi")` | `CameraScreenScreenshotTest.matching_dark` (`src/test/screenshots/CameraScreen_matching_dark.png`): như `default_dark`, `Scoring(68, RAISE_LEFT_ARM)`. `matched_dark` (`src/test/screenshots/CameraScreen_matched_dark.png`): `Matched(92)`. `noPerson_dark` (`src/test/screenshots/CameraScreen_no-person_dark.png`): `NoPerson` |
| Màn hình thấp (L-028) | Screenshot test khung `w360dp-h640dp-xxhdpi` | `matching_small` (`src/test/screenshots/CameraScreen_matching_small.png`): thẻ bước hai hàng, cột công cụ ở lề, `MatchHint` với lời nhắc dài nhất |
| So với thiết kế | Ảnh so sánh và rà soát độc lập (bước 6) | Chạy `scripts/design-compare.sh camera` sau khi ghi ảnh chuẩn; commit `docs/features/camera/design/review/`, dán `review/summary.md` vào mô tả PR. Rà soát xem ảnh ghép của ba ảnh mới theo mục 7 của README thiết kế. Khác biệt đã biết: lề trái 8 thay 6 của `MatchHint`; ảnh tạm |
| Ảnh thiết kế của ticket khác | | Ảnh thiết kế của ticket khác (T-013, T-014, T-015, T-016, T-020): `CameraScreen_starting_dark`, `CameraScreen_permission-denied_dark`, `CameraScreen_error_dark`, `CameraScreen_countdown_dark`, `CameraScreen_default_dark`, `CameraScreen_no-pose_dark`, `CameraScreen_steps-hidden_dark`, `CameraScreen_adjust_dark`, `CameraScreen_locked_dark`, `ReviewScreen_default_dark`, `ReviewScreen_saving_dark`, `ReviewScreen_save-error_dark`, `ReviewScreen_discard_dark`, `CameraScreen_default_dark_unfolded`, `ReviewScreen_default_dark_unfolded` |
| Dung lượng APK (ticket, không phải tiêu chí đạt) | Đo trong phiên, ghi vào báo cáo | Bước 4: `./gradlew :app:assembleStagingRelease` trên nhánh chính và trên nhánh task; ghi cỡ hai APK, chênh lệch, và cỡ phần `lib/arm64-v8a/` cộng `assets/` mà ML Kit thêm (một máy chỉ tải một kiến trúc khi phát hành bằng AAB). Ghi vào nhật ký và báo cáo phiên; spec không còn ngưỡng (spec mục 9, câu 12), nên không dừng vì dung lượng |
| Quyền của manifest gộp | Đo trong phiên | `./gradlew :app:processStagingDebugMainManifest` trước và sau; danh sách `uses-permission` không có quyền mới so với mục 4.1. Có quyền mới: dừng (mục 7) |
| Người thật đúng tư thế đạt 80 | Kiểm tra thủ công | Bản staging, catalog có điểm khớp thật (mục 8, câu 3), máy thật: đứng đúng một tư thế toàn thân: điểm từ 80 và "Đúng dáng rồi, bấm chụp!", nét viền và nút chụp xanh; hạ tay trái: điểm giảm, lời nhắc "Nâng tay trái cao hơn"; dùng camera trước: lời nhắc vẫn gọi đúng tay. Ghi kết quả vào báo cáo phiên thử (`scripts/device-session.sh`) |
| Khung hình mỗi giây trên máy tầm thấp | Kiểm tra thủ công | Máy tầm thấp (spec câu 5), bật nhận dạng, đứng trong khung 30 giây: log `VGL.Camera` "Pose analysis: <n> fps" (`adb logcat -v threadtime \| grep ' VGL\.'`); khung xem trước mượt, đo bằng `adb shell dumpsys gfxinfo <gói> framestats` không dưới 15 khung mỗi giây. Ghi vào báo cáo phiên thử |
| Không báo nhầm | Kiểm tra thủ công | Hướng camera lên trần nhà, vào tường trống, vào bàn ghế 30 giây mỗi chỗ: chỉ có "Lùi lại để cả người vào khung", không có số |
| Tắt từ xa | Kiểm tra thủ công | Bản staging với Remote Config đặt `pose_match_threshold` 60: đạt sớm hơn; (bản production) cờ tắt: không có `MatchHint`, log "Pose matching off: flag" |

**Ngoại lệ viết test trước:**

- `MlKitPoseAnalyzer` và phần `ImageAnalysis` của `CameraXController`: lớp mỏng trên phần cứng và thư viện gốc, không chạy được trên JVM (mục 4.1); kiểm bằng `AppLaunchTest` có sẵn trên emulator (camera vẫn tới `camera_screen` khi staging bật cờ) và kiểm tra thủ công. Như `CameraXController` của T-013.
- Biểu tượng `ic_person_search.xml`: tài nguyên.
- `StagingFeatureFlagsModule`: cấu hình Hilt của bản staging.

### 4.1. Chạy thử

- **Đã chạy thử:** 2026-10-23, trên nhánh chính (commit fa65d75) cộng thay đổi tạm không commit: `implementation(libs.mlkit.pose.detection)` trong `feature/camera/build.gradle.kts`; một lớp tạm trong `main` gọi `PoseDetection.getClient(PoseDetectorOptions.Builder().setDetectorMode(STREAM_MODE).build())`, `InputImage.fromMediaImage`, `ImageAnalysis.Builder().setBackpressureStrategy(STRATEGY_KEEP_ONLY_LATEST)` và `PoseLandmark`; một test Robolectric tạm gọi `PoseDetection.getClient(...)` và `process(InputImage.fromBitmap(...))`.
- **Lệnh:** `./gradlew :app:assembleStagingDebug` trên nhánh chính và trên nhánh có thay đổi tạm; `./gradlew :feature:camera:testDebugUnitTest --tests '*SpikeTest*'`; `scripts/check.sh`.
- **Kết quả:**
  - Biên dịch: mọi lớp trên có trên classpath biên dịch của `:feature:camera` khi thêm bí danh; `ImageAnalysis`, `ImageProxy` có sẵn trong `androidx-camera-core`. `minSdk` 26 qua.
  - Manifest gộp: không có `uses-permission` mới. Thành phần mới: `MlKitInitProvider`, `MlKitComponentDiscoveryService`, `MlKitRemoteWorkerService`, `RemoteWorkManagerService`.
  - Robolectric: `PoseDetection.getClient` ném `IllegalStateException: MlKitContext has not been initialized`. Vì vậy test JVM không bao giờ dựng `MlKitPoseAnalyzer` (mục 4, ngoại lệ; mục 7, điều 5); bộ nhận dạng chỉ tạo khi `setActive(true)` lần đầu, nên dựng lớp này cũng không ném.
  - Dung lượng (APK `stagingDebug`, đủ bốn kiến trúc): 20 979 148 lên 108 221 134 byte. Phần thêm: `lib/<kiến trúc>/libxeno_native.so` (`arm64-v8a` 21 597 896, `armeabi-v7a` 16 008 520, `x86` 19 902 212, `x86_64` 19 852 240 byte, lưu không nén); `assets/mlkit_pose/` hai mô hình `.tflite` 5 776 256 byte và dữ liệu thử 0.1 MB; mã dex thêm khoảng 1.1 MB (nén, bản debug chưa thu gọn); tài nguyên `abc_*` của AppCompat. Một máy `arm64-v8a` (AAB tách theo kiến trúc): cài thêm khoảng 28.5 MB, tải về thêm khoảng 12 MB (thư viện gốc nén còn 5.7 MB, hai mô hình nén còn 4.9 MB, dex). Spec đã bỏ tiêu chí dung lượng và ghi các số đo này (spec mục 9, câu 12; mục 8, câu 1).
  - `scripts/check.sh` (cùng lần với thay đổi tạm của T-015, T-017, có `libs.mlkit.pose.detection` trong `:feature:camera`): "Cổng chất lượng: QUA", 752 test JVM, 0 lỗi, 65 ảnh screenshot, `lintStagingDebug` qua. Hai lần chạy trước thất bại vì lý do không thuộc kỹ thuật: máy chạy thiếu `gh` trong `PATH` (test của `release.sh`), và code tạm chưa đúng ktlint (sửa bằng `./gradlew :feature:camera:ktlintFormat`).

## 5. Chấm rủi ro

**Điểm chặn bị chạm:** điểm 2 (thư viện ML Kit lần đầu vào app, dù bí danh đã có trong `gradle/libs.versions.toml`), điểm 4 (tham số `matched` của `photo_captured`; số liệu dùng thư viện ML Kit gửi cho Google, mục 8, câu 2), điểm 7 (cờ và cài đặt mới trong `:core:config`, thành phần mới trong `:core:designsystem`). Không chạm điểm 3 (không thêm quyền, mục 4.1).

| Tiêu chí | Điểm (0 đến 3) | Lý do |
| --- | --- | --- |
| A. Phạm vi thay đổi | 3 | Bốn module (`:feature:camera`, `:core:config`, `:core:designsystem`, `:app` bản staging), khoảng 40 file |
| B. Độ rõ của yêu cầu | 1 | Spec xác nhận lại 2026-10-23 (bỏ tiêu chí dung lượng APK), thiết kế duyệt, các câu hỏi ở mục 8 đã trả lời; AI vẫn tự quyết chi tiết nhỏ khi làm (ngưỡng 60 độ, chữ của 16 lời nhắc, cách làm mượt điểm) |
| C. Khả năng kiểm chứng tự động | 3 | Chấm điểm có test tổng hợp, nhưng ba tiêu chí chính (người thật đạt 80, 15 khung hình mỗi giây trên máy tầm thấp, không báo nhầm với cảnh thật) chỉ kiểm được trên máy thật; ML Kit không chạy trên JVM |
| D. Ảnh hưởng chéo | 2 | Sửa `CameraXController.kt`, `CameraViewModel.kt`, `PoseViewModel.kt`, `PoseOverlay.kt`, `CameraScreen.kt`, `CameraControls.kt`, ảnh chuẩn camera mà T-015, T-020 cũng sửa; sửa `ConfigModule.kt`, `FeatureFlag.kt` mà T-019 cũng có thể sửa |
| E. Độ mới của kỹ thuật | 3 | ML Kit Pose Detection bản beta, use case `ImageAnalysis`, chấm điểm dáng tự viết: lần đầu trong dự án, tài liệu ít về độ chính xác |
| **Tổng** | 12 | **Mức: Cao** (chạm điểm chặn, và tổng điểm cũng từ 9 trở lên) |

## 6. Ảnh hưởng chéo

- File hoặc thành phần dùng chung bị chạm: `:core:config` (`FeatureFlag.kt`, `ConfigModule.kt`, `PoseMatchingSettings.kt` mới); `:core:designsystem` (`MatchHint.kt` mới, `DesignSystemCatalog.kt`, một biểu tượng, một ảnh catalog); `:app` (`StagingFeatureFlagsModule.kt`); trong `:feature:camera`: `build.gradle.kts`, `CameraXController.kt`, `CameraViewModel.kt`, `ActivePoseHolder.kt`, `PoseViewModel.kt`, `PoseOverlay.kt`, `CameraScreen.kt`, `CameraControls.kt`, `strings.xml`, `CameraViewModelTest.kt`, `PoseViewModelTest.kt`, `CameraScreenScreenshotTest.kt`, `design/review/`.
- Task khác có thể xung đột:
  - **T-015** (chỉnh nét viền): ticket T-017 phụ thuộc T-015 (trạng thái `hidden`, `mirrored`, tham số `hintBottomPadding`, lời nhắc khóa cùng chỗ). Cùng sửa `PoseViewModel.kt`, `PoseOverlay.kt`, `CameraScreen.kt`, `CameraControls.kt`, `strings.xml`, `DesignSystemCatalog.kt`, `DesignSystem_camera_components_dark.png`, `CameraScreenScreenshotTest.kt`.
  - **T-020** (máy gập): cùng sửa `CameraScreen.kt`, `CameraControls.kt` (T-020 tách `ShutterButton`, zoom ra cột phải). `MatchHint` đặt theo `hintBottomPadding` nên đúng chỗ ở máy gập dù T-020 chạy trước hay sau.
  - **T-019** (hỏi đánh giá): có thể thêm cờ `feature_in_app_review_enabled` vào `FeatureFlag.kt`, cài đặt từ xa và `@Binds` vào `ConfigModule.kt`, cờ vào `StagingFeatureFlagsModule.kt`. Chỉ xung đột dòng; thứ tự hằng trong `FeatureFlag` không quan trọng.
  - **T-018** (quảng cáo màn xem lại): không chung file.
- Đề xuất thứ tự chạy: sau T-015; trước T-020 trong nhóm camera (T-015, T-017, T-020), để T-020 ghi ảnh `_unfolded` một lần với đủ phần. T-020 không phụ thuộc T-017, nên nếu T-017 dừng, T-020 vẫn chạy.

## 7. Điều kiện dừng riêng

1. Lúc bắt đầu task, thiếu phần của T-014, T-015 mà plan dựa vào: `ActivePoseHolder`, trạng thái `hidden`, `mirrored` của nét viền, `PoseOutline(color)`, tham số `hintBottomPadding`, `lockHintVisible`. Dừng.
2. Manifest gộp sau khi thêm ML Kit có quyền mà mục 4.1 không ghi: dừng (điểm chặn 3).
3. Thêm ML Kit làm `scripts/check.sh` thất bại vì quy tắc của build (ví dụ `minSdk`, xung đột phiên bản thư viện) mà sửa cần đụng `gradle/libs.versions.toml` hay `build-logic/`: dừng.
4. `ImageAnalysis` của CameraX cần thư viện ngoài `androidx-camera-core` đang có: dừng.
5. Test JVM nào (kể cả của T-013 đến T-016) dựng `MlKitPoseAnalyzer` thật và hỏng vì ML Kit: dừng, không đổi test cũ.

## 8. Câu hỏi cho người duyệt

1. **Tiêu chí "APK tăng không quá 10 MB" không đạt được.** Số đo ở mục 4.1: với `pose-detection` 18.0.0-beta5 (bản spec chọn, câu 9), một máy `arm64-v8a` tải về thêm khoảng 12 MB và cài thêm khoảng 28.5 MB; phần lớn là thư viện gốc `libxeno_native.so` (21.6 MB) và hai mô hình (5.8 MB), không thu gọn được bằng R8. Theo tài liệu AI biết, ML Kit chưa có bản pose tải mô hình qua Google Play Services như nhận dạng chữ hay mã vạch; dev nên kiểm lại trước khi chọn. Bản `pose-detection-accurate` còn lớn hơn. Phương án:
   - A. Sửa spec: tiêu chí thành "tải về tăng không quá 15 MB trên một kiến trúc", đo bằng bản staging release (AAB tách kiến trúc); giữ plan như hiện tại.
   - B. Tải phần nhận dạng theo nhu cầu (Play Feature Delivery, module tính năng động): APK gốc không tăng, người bật nhận dạng tải thêm khoảng 12 MB. Cần thư viện Play Feature Delivery, đổi cấu hình build và module `:app` (điểm chặn 2, 7), ngoài phạm vi ticket: tách thành ticket mới.
   - C. Bỏ tiêu chí dung lượng, ghi số đo vào spec.
   Đề xuất của AI: A.
- Đã trả lời: phương án C (Tuấn, 2026-10-23). Spec bỏ tiêu chí khỏi KB-5, ghi số đo ở mục 9, câu 12, xác nhận lại 2026-10-23 (PR #64). Plan vẫn đo dung lượng APK và khung hình mỗi giây, ghi vào báo cáo phiên như ticket yêu cầu.
2. **ML Kit gửi số liệu dùng thư viện cho Google, và tự khởi tạo lúc mở app** (người phụ trách sản phẩm cần biết, điểm chặn 4). Manifest gộp có `MlKitInitProvider`, chạy khi app mở, trước `Application.onCreate`; không đổi thứ tự các SDK khác (Firebase, AdMob). Theo tài liệu của Google, ML Kit có thể gửi thông tin chẩn đoán và cách dùng thư viện (không gửi ảnh hay kết quả nhận dạng). Spec ghi "phân tích dáng chạy hoàn toàn trên máy", vẫn đúng với ảnh và điểm khớp. Đề xuất của AI: giữ, và dev cập nhật mục An toàn dữ liệu (Data safety) trên Play Console trước khi phát hành, như việc của quyền lưu tệp (spec câu 10).
3. **Catalog chưa có điểm khớp thật.** Catalog staging hiện có điểm khớp giả cho `w01`, `m01` (plan T-009: "số liệu giả"); các tư thế khác không có điểm khớp nên không chạy nhận dạng. Tiêu chí "người thật đúng tư thế đạt 80" cần điểm khớp thật. Đề xuất của AI: dev hoặc người làm nội dung thêm điểm khớp thật cho vài tư thế toàn thân trước khi thử (ví dụ chạy ML Kit trên ảnh mẫu của tư thế để lấy 33 điểm); việc này ngoài phiên tự chạy và không chặn task.
4. **Cách chấm điểm và bộ lời nhắc.** Thiết kế chỉ có ví dụ "Nâng tay trái cao hơn". Đề xuất của AI: chấm theo góc tám đoạn tay chân (không phụ thuộc vị trí và cỡ nét viền), lệch 60 độ thì đoạn đó 0 điểm; 16 lời nhắc ở mục 3.1. Người làm nội dung có thể sửa chữ; ngưỡng 60 độ chỉnh sau khi thử với người thật (có thể đưa vào `RemoteSettings` ở ticket sau).
5. **`matched` khi nhận dạng tắt.** Đề xuất của AI: `false` (không đạt ngưỡng lúc chụp), kể cả khi cờ tắt hay tư thế không có điểm khớp. Muốn phân biệt "không chấm" với "chấm mà chưa đạt" thì cần thêm giá trị vào spec mục 6.
6. **Cờ bật trong lúc camera đang mở.** Đề xuất của AI: tắt cờ thì ngừng ngay (không gọi ML Kit nữa); bật cờ thì có hiệu lực ở lần mở camera kế tiếp (ra nền, đổi camera, mở lại app), vì phải gắn lại use case CameraX. Thực tế cờ lấy từ Remote Config lúc khởi động, trước khi mở camera.
7. **TalkBack đọc lời nhắc mỗi khi đổi.** Đề xuất của AI: `MatchHint` là vùng đọc lịch sự (`liveRegion = Polite`): TalkBack đọc lời nhắc mới khi người dùng không thao tác; không đọc số điểm mỗi khung (điểm đổi liên tục).
8. **Lời nhắc khóa và `MatchHint` cùng chỗ.** Đề xuất của AI: lời nhắc khóa (2 giây, plan T-015 câu 2) che `MatchHint` khi hiện; bảng chỉnh mở thì không có `MatchHint`.

## 9. Quyết định duyệt

<!-- Người duyệt điền. AI không điền mục này. -->

- **Quyết định:** Duyệt
- **Người duyệt:** Nhật, **duyệt chéo:** Tuấn
- **Ghi chú:** Ngày 2026-10-23, Tuấn trả lời câu hỏi cùng AI: chấp nhận mọi đề xuất của AI ở mục 8. Câu 1: phương án C, bỏ tiêu chí dung lượng APK (spec đã sửa ở PR #64); phiên vẫn đo và ghi vào báo cáo.

## 10. Nhật ký thực hiện

<!-- AI ghi khi task đã có kết quả cuối (đã merge hoặc đã dừng), trên nhánh báo cáo của phiên (auto-run.md, bước 10). Mục mới thêm vào cuối, không sửa mục cũ. -->
