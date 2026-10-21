---
ticket: T-009
title: Module dữ liệu tư thế dùng chung `:core:poses`
status: approved                # draft | approved | stopped | done
version: 1                   # tăng khi plan được sửa lại sau phản hồi hoặc sau khi dừng
risk_score:                  # tổng điểm theo risk-scoring.md, để trống nếu chạm điểm chặn
risk_level: high             # low | medium | high
token_estimate: 40M          # cỡ L, số gợi ý của scripts/token-usage.py estimate
blocking_points: [core, r2]  # các điểm chặn bị chạm, ví dụ [ads, sdk]
run_order:                   # AI điền sau họp đánh giá rủi ro
reviewer: Nhật                    # người duyệt
cross_reviewer: Tuấn             # bắt buộc khi risk_level là high
approved_at: 2026-10-21
---

# PLAN T-009: MODULE DỮ LIỆU TƯ THẾ DÙNG CHUNG `:CORE:POSES`

## 1. Tóm tắt

Tạo module core mới `:core:poses` cho camera, thư viện và màn loading dùng chung: mô hình tư thế, đọc catalog, tải catalog và ảnh qua `RemoteAssets` với mức chờ 15 giây, tải bộ khởi đầu có báo tiến trình, danh sách đã lưu và đã dùng gần đây trong DataStore, thứ tự hiển thị (đã lưu, gần đây, còn lại) và ẩn nhóm rỗng. `:core:config` có thêm giá trị chuỗi trong `RemoteSettings` để đọc đường dẫn catalog. `:core:assets` cho biết lý do tải lỗi (`NETWORK`, `NOT_FOUND`, `OTHER`) trong `AssetResult.Unavailable`, để `:core:poses` và T-011 dùng chung một cách phân loại. Module chưa có màn hình nào dùng; mọi logic được kiểm bằng unit test trên JVM với file mẫu trong repo, không gọi mạng. Catalog mẫu nhỏ cho R2 staging nằm trong repo, dev đưa lên R2 sau khi merge.

## 2. Hiểu yêu cầu

Ticket không có kịch bản riêng (`scenarios: []`). Nó cung cấp phần dữ liệu cho các tiêu chí sau trong `features/poses/spec.md`, `features/camera/spec.md` và `features/loading/spec.md`; phần giao diện của các tiêu chí này thuộc T-010, T-014, T-021, T-022.

| Kịch bản | Tiêu chí chấp nhận | Cách hiểu của AI |
| --- | --- | --- |
| poses KB-1 | Tư thế xếp theo thứ tự: đã lưu, rồi đã dùng gần đây (mới dùng nhất trước), rồi còn lại theo thứ tự catalog; áp dụng cho "Tất cả" và từng nhóm | Một hàm thuần `orderPoses` trong `:core:poses`. Nhóm đã lưu xếp mới lưu nhất trước (như chip "Đã lưu", KB-3). Tư thế vừa đã lưu vừa đã dùng chỉ hiện một lần, ở phần đã lưu. Lọc theo nhóm trước, rồi xếp |
| poses KB-1 | Dòng tổng số: số tư thế và số nhóm (câu hỏi 3 của spec) | `PoseLibrary.totalPoses` đếm mọi tư thế hợp lệ trong catalog; `categories.size` chỉ đếm nhóm có tư thế |
| poses KB-1 | Chỉ nhóm có ít nhất một tư thế mới có chip | `PoseLibrary.categories` bỏ nhóm rỗng, giữ thứ tự catalog |
| poses KB-1 | Tên tư thế đọc được, không có chuỗi kỹ thuật | Tên lấy theo ngôn ngữ máy, thiếu thì dùng tiếng Anh (câu hỏi 4). Tư thế thiếu tên tiếng Anh bị bỏ. Kiểm tên dạng `snake_case` là việc của script kiểm catalog (bản đề xuất, bài học 9), ngoài phạm vi |
| poses KB-3 | Lưu, bỏ lưu; chip "Đã lưu" mới lưu nhất ở đầu; mở lại app vẫn còn | `PoseHistoryRepository.setSaved` ghi vào DataStore, kèm thời điểm lưu |
| poses KB-3 | Tư thế đã lưu không còn trong catalog thì không hiện | `orderPoses` bỏ mã không có trong catalog, dữ liệu đã lưu giữ nguyên |
| poses KB-3 | Ghi danh sách đã lưu lỗi thì báo cho người dùng | `setSaved` ném `IOException`; ViewModel của T-021, T-022 bắt và báo |
| poses KB-5 | Tải không có phản hồi sau 15 giây thì coi là lỗi | Mỗi lần tải (catalog, ảnh nhỏ, ảnh lớn, nét viền) có mức chờ 15 giây trong `:core:poses` (ADR-1001), trả lỗi `TIMEOUT` |
| poses KB-5 | Lỗi do mất mạng thì hiện hộp thoại mạng | `:core:assets` phân loại lỗi tải: lỗi kết nối là `NETWORK`, HTTP 404 là `NOT_FOUND`, còn lại `OTHER`. `:core:poses` chuyển sang `LoadFailureReason` và phát mọi lần tải lỗi qua `failures`. T-011 dùng lại lý do `NETWORK` này để hiện hộp thoại, không phân loại lại (mục 8, ghi chú 1) |
| poses KB-5 | Thẻ của bộ khởi đầu hiện ảnh ngay | Ảnh đã có trên máy thì `loadFile` trả ngay (hành vi của `RemoteAssets`) |
| camera KB-1 | Lần đầu là tư thế mặc định: tư thế đầu tiên của "Tất cả" theo thứ tự catalog (câu hỏi 1 của spec camera) | `PoseLibrary.defaultPose` là tư thế đầu tiên trong catalog, không theo thứ tự đã lưu, gần đây |
| camera KB-1 | Mở lại app vẫn là tư thế dùng lần trước; tư thế đó không còn thì dùng mặc định | `PoseLibrary.lastUsedPose` là tư thế mới dùng nhất còn trong catalog. Camera ghi "đã dùng" mỗi lần chọn tư thế (`markUsed`), nên không cần khóa "tư thế đang dùng" riêng (câu hỏi 5) |
| camera mục 6 | Lưu tư thế đã dùng gần đây kèm thời điểm dùng | `markUsed` ghi mã và thời điểm, giữ tối đa 20 tư thế (câu hỏi 3) |
| loading KB-1 | Bộ khởi đầu tăng dần theo từng file tải xong, không lùi | `loadStarterSet(onProgress)` gọi lại sau mỗi file xong với `(done, total)`; `done` chỉ tăng |
| loading KB-2 | Thử lại chỉ tải phần còn thiếu, tiến trình tiếp tục | Repository nhớ các file đã xong trong lần chạy của app; lần sau chỉ gọi `load` cho file chưa xong, `done` bắt đầu từ số đã xong |
| loading KB-2 | Catalog, từng file bộ khởi đầu chờ tối đa 15 giây | Như poses KB-5 (ADR-1001) |
| loading KB-3 | Catalog và bộ khởi đầu đã có trên máy thì xong ngay, không cần mạng | `RemoteAssets` trả file đã giữ; catalog đọc từ file trên máy |
| Ticket | Catalog mẫu nhỏ trên R2 staging | File mẫu trong repo, tên file chứa mã băm của nội dung; dev đưa lên R2 staging sau khi merge (mục 3.3) |

Thiết kế tham chiếu: không có thiết kế riêng, module không có giao diện.

ADR liên quan: ADR-0001 (core chỉ phụ thuộc core), ADR-0003 và ADR-0012 (`RemoteSettings`), ADR-0006 và ADR-0015 (file R2 chỉ thêm mới, đường dẫn có phiên bản, `Unavailable` đưa vào trạng thái; plan thêm lý do vào `Unavailable`, không đổi quy tắc nào của ADR), ADR-0019 (log), ADR-1001 (15 giây).

## 3. Giải pháp

### 3.1. Cách làm

**Vị trí và phụ thuộc.** `:core:poses` là module core của app (spec camera, câu hỏi 8). Plugin `template.android.library`, `template.hilt` và `kotlin.serialization` (đã khai báo ở `build.gradle.kts` gốc). Phụ thuộc: `:core:assets`, `:core:config`, `:core:data`, `:core:logging` và `libs.kotlinx.serialization.json` (đã có trong `gradle/libs.versions.toml`, không sửa file này). Không phụ thuộc `:core:network`: module đó chưa có (T-011); lý do lỗi đến từ `:core:assets`. Feature dùng module bằng một dòng `implementation(project(":core:poses"))` trong `build.gradle.kts` của feature, vì convention plugin chỉ gắn các core của dự án mẫu (câu hỏi 7).

**Khuôn catalog** (bản đề xuất, mục 11; lược đồ phiên bản 1):

```json
{
  "schema": 1,
  "minAppSchema": 1,
  "datasetVersion": "2026.10.1",
  "categories": [{ "id": "women", "name": { "en": "Women", "vi": "Nữ" } }],
  "poses": [{
    "id": "w01", "category": "women", "subjectType": "single", "gender": "female",
    "framing": "full", "tags": ["standing"], "difficulty": 1, "starter": true,
    "name": { "en": "...", "vi": "..." },
    "steps": { "en": ["...", "..."], "vi": ["...", "..."] },
    "mistakes": { "en": ["..."], "vi": ["..."] },
    "preview": { "small": "<đường dẫn R2>", "large": "<đường dẫn R2>" },
    "outline": "<đường dẫn R2>",
    "keypoints": [[0.51, 0.12, 0.99]]
  }]
}
```

- `subjectType`: `single`, `couple`, `group`. `gender`: `female`, `male`, `mixed`, `any`. `framing`: `full`, `half`, `close`. Feature tự hiện chữ theo `strings.xml` của mình.
- `starter: true` đánh dấu tư thế thuộc bộ khởi đầu (60 tư thế khi có nội dung thật). Thiếu thì là `false`.
- Đường dẫn ảnh nằm trong catalog, app không tự ghép. Đường dẫn phải hợp lệ với `AssetPath` (ADR-0015). Mẫu `poses/img/<poseId>/<sha8>/preview_540.webp` của bản đề xuất **không** hợp lệ, vì `AssetPath` chỉ nhận thư mục `v<n>` hoặc mã băm trong tên file. Plan dùng mã băm trong tên file: `poses/img/<poseId>/preview_540-<sha8>.webp`, `preview_1200-<sha8>.webp`, `outline-<sha8>.webp`, mỗi file mã băm của chính nó (câu hỏi 2).
- `keypoints` tùy chọn: đúng 33 điểm `[x, y, visibility]`, `x`, `y` từ 0 đến 1 theo ảnh nét viền. Sai số điểm thì bỏ `keypoints`, giữ tư thế.
- Đọc bằng `kotlinx.serialization` với `ignoreUnknownKeys = true`, để catalog sau thêm trường mà app cũ vẫn đọc được.
- `minAppSchema` lớn hơn `APP_SCHEMA = 1` thì catalog bị từ chối (lý do `OTHER`, log riêng), vì app này không hiểu được (câu hỏi 6).
- Tư thế bị bỏ (log `warn` với số lượng và lý do, không ghi mã): mã trùng (giữ cái đầu), nhóm không có trong `categories`, giá trị `subjectType`, `gender`, `framing` lạ, thiếu tên hoặc bước tiếng Anh, đường dẫn ảnh không hợp lệ.

**API công khai** (gói `com.vglstudio.camera.pose.line.guide.core.poses`):

- Mô hình (`model/`): `Pose`, `PoseCategory`, `LocalizedText` (`resolve(locale: Locale = Locale.getDefault()): String`), `LocalizedList`, `SubjectType`, `PoseGender`, `Framing`, `Keypoint`, `PoseCatalog(datasetVersion, categories, poses)`, `PoseFile` (`THUMBNAIL`, `IMAGE`, `OUTLINE`).
- Ngôn ngữ: khớp đủ thẻ (`pt-BR`), rồi khớp ngôn ngữ (`es-MX` dùng `es-419`, `pt` dùng `pt-BR`), rồi `en`.
- Lỗi (`PoseLoadFailure.kt`):
  - `enum class PoseContent(val analyticsValue: String) { CATALOG("catalog"), THUMBNAIL("thumbnail"), IMAGE("image"), OUTLINE("outline") }`.
  - `enum class LoadFailureReason(val analyticsValue: String) { NETWORK("network"), NOT_FOUND("not_found"), TIMEOUT("timeout"), OTHER("other") }`.
  - `data class PoseLoadFailure(val content: PoseContent, val reason: LoadFailureReason)`.
  - Cách quy đổi (một hàm `internal` trong `TimedAssetLoader`): quá 15 giây là `TIMEOUT`; `AssetResult.Unavailable(reason)` chuyển thẳng `NETWORK`, `NOT_FOUND`, `OTHER` sang cùng tên; catalog đọc lỗi hay lược đồ quá mới là `OTHER`.
  - Feature tự ghi `content_load_failed`; `:core:poses` không ghi sự kiện phân tích.
- `PoseFileResult`: `Ready(file: File)`, `Failed(failure: PoseLoadFailure)`.
- `CatalogState`: `NotLoaded`, `Loading`, `Ready(catalog)`, `Failed(failure)`.
- `StarterSetResult`: `Complete`, `Incomplete(failedFiles: Int, failure: PoseLoadFailure)`.
- `interface PoseCatalogRepository`:
  - `val catalog: StateFlow<CatalogState>`.
  - `suspend fun loadCatalog(): CatalogState`: đọc đường dẫn hiện tại từ `RemoteSettings`; đã `Ready` với cùng đường dẫn thì trả ngay; ngược lại tải (15 giây), đọc, cập nhật `catalog`. Nhiều nơi gọi cùng lúc chỉ tải một lần (`Mutex`). Dùng cho loading (T-010) và nút "Thử lại".
  - `suspend fun loadFile(pose: Pose, file: PoseFile): PoseFileResult`, 15 giây.
  - `suspend fun loadStarterSet(onProgress: (done: Int, total: Int) -> Unit): StarterSetResult`: chờ catalog (`loadCatalog()`; catalog lỗi thì trả `Incomplete` với lỗi catalog); tải ảnh nhỏ và nét viền của mọi tư thế `starter`, tối đa 4 file cùng lúc, mỗi file 15 giây; gọi `onProgress` một lần lúc bắt đầu và sau mỗi file xong.
  - `val failures: SharedFlow<PoseLoadFailure>`: mọi lần tải lỗi, cho vỏ app ở T-011 (bộ đệm 16, bỏ cái cũ nhất).
- `interface PoseHistoryRepository`:
  - `val saved: Flow<List<SavedPose>>` (mới lưu nhất trước), `val recent: Flow<List<UsedPose>>` (mới dùng nhất trước). Đọc lỗi `IOException` thì phát rỗng, chờ rồi đọc lại, như `DataStoreCounterRepository`.
  - `suspend fun setSaved(poseId: String, saved: Boolean)`: lưu khi đã lưu thì giữ thời điểm cũ. Ném `IOException` khi ghi lỗi.
  - `suspend fun markUsed(poseId: String)`: đưa lên đầu với thời điểm hiện tại, giữ tối đa 20. Ném `IOException`.
- `interface PoseLibraryRepository`:
  - `val library: Flow<LibraryState>` với `LibraryState`: `Loading`, `Ready(PoseLibrary)`, `Failed(PoseLoadFailure)`. Ghép `catalog`, `saved`, `recent`; khi bắt đầu thu mà catalog đang `NotLoaded` thì gọi `loadCatalog()` (trường hợp cờ loading tắt, spec loading câu hỏi 9).
  - `suspend fun retry()`: gọi lại `loadCatalog()`.
- `PoseLibrary`: `categories` (chỉ nhóm có tư thế), `totalPoses`, `savedIds: Set<String>`, `posesFor(filter: PoseFilter): List<Pose>` với `PoseFilter`: `All`, `Saved`, `Category(id)`, `defaultPose: Pose?`, `lastUsedPose: Pose?`. Danh sách đã tính sẵn khi tạo, không tính lại mỗi lần gọi.

**Phần `internal`:** DTO JSON (`CatalogJson.kt`), `CatalogParser`, `orderPoses` và cách dựng `PoseLibrary` (`PoseOrdering.kt`), `TimedAssetLoader`, ba lớp cài đặt, `PosesSettings` (khóa và đường dẫn mặc định), `PosesClock`, Hilt module.

**Lý do lỗi trong `:core:assets`** (đã quyết, mục 8, ghi chú 1):

- `AssetFailureReason.kt` (mới, công khai): `enum class AssetFailureReason { NETWORK, NOT_FOUND, OTHER }` và `internal fun IOException.toFailureReason()`: lỗi hoặc một `cause` trong chuỗi là `UnknownHostException`, `SocketException` (gồm `ConnectException`, `NoRouteToHostException`) hay `SocketTimeoutException` thì `NETWORK`; `HttpStatusException` với mã 404 thì `NOT_FOUND`; còn lại `OTHER`. Cùng danh sách lỗi mạng với plan T-011.
- `AssetDownloader.kt`: thêm `internal class HttpStatusException(val status: Int, url: String) : IOException("HTTP $status for $url")`; `HttpAssetDownloader` ném lớp này thay cho `IOException` trơn khi mã trả về khác 200. Chữ của lỗi không đổi.
- `RemoteAssets.kt`: `data object Unavailable` thành `data class Unavailable(val reason: AssetFailureReason = AssetFailureReason.OTHER)`. Có giá trị mặc định để ít chỗ phải sửa. Chỗ dùng `is AssetResult.Unavailable` không đổi; chỗ dùng `AssetResult.Unavailable` như một giá trị phải thêm `()`. Đã tìm bằng `grep -rn "Unavailable"` trong `app`, `core`, `feature`: chỉ có `DefaultRemoteAssets.kt`, `DefaultRemoteAssetsTest.kt` (hai dòng) và `DefaultPreparationRepositoryTest.kt` (một dòng). `DefaultPreparationRepository.kt` chỉ dùng `is AssetResult.Ready`, không phải sửa. Chú thích KDoc của `Unavailable` ghi thêm ý nghĩa của `reason`.
- `DefaultRemoteAssets.kt`: trong `catch (e: IOException)` trả `AssetResult.Unavailable(e.toFailureReason())`. Tham số của constructor không đổi, nên lời gọi theo vị trí của test hiện có và phần T-011 thêm sau (`networkFailures` ở cuối) không vướng nhau.
- Sửa ba dòng test hiện có chỉ vì đổi kiểu, giữ nguyên điều chúng kiểm tra: hai dòng `assertEquals(AssetResult.Unavailable, ...)` của `DefaultRemoteAssetsTest` thành `assertEquals(AssetResult.Unavailable(AssetFailureReason.OTHER), ...)` (bộ tải giả ném `IOException` trơn, đúng là `OTHER`); `assets.result = AssetResult.Unavailable` của `DefaultPreparationRepositoryTest` thành `AssetResult.Unavailable()`.
- `:core:assets` có `origin: template` (ADR-0015). Người phụ trách sản phẩm đã quyết thay đổi của Camera Pose không đưa về dự án mẫu, nên PR chỉ ghi đây là thay đổi ở module của dự án mẫu, không mở đề xuất `from-app`. ADR-0015 không sửa: plan chỉ thêm thông tin vào kết quả, không đổi quy tắc.

**Chờ 15 giây.** `HttpAssetDownloader` chặn luồng, không hủy được giữa chừng; `withTimeout` quanh `load` chỉ trả về khi lần tải kết thúc. `TimedAssetLoader` chạy `assets.load(path)` bằng `async` trong `CoroutineScope` riêng của module (`SupervisorJob` và `Dispatchers.IO`) rồi `withTimeoutOrNull(15_000) { deferred.await() }`. Hết giờ thì người gọi nhận `TIMEOUT` đúng lúc 15 giây; lần tải vẫn chạy nền và giữ file khi xong, lần sau lấy ngay. Hằng số `LOAD_TIMEOUT_MILLIS = 15_000` (ADR-1001).

**DataStore.** Dùng `DataStore<Preferences>` chung của `:core:data`. Hai khóa mới, không chuyển đổi dữ liệu cũ (không chạm điểm chặn 9): `poses_saved` và `poses_recent`, mỗi khóa một chuỗi JSON `[{"id":"w01","at":1729500000000}]`. Chuỗi hỏng thì coi là rỗng và log `warn`. Thời điểm lấy từ `PosesClock` (`fun interface`, `System.currentTimeMillis()`), để test cố định được.

**Đường dẫn catalog.** `RemoteSettings` có thêm `fun string(key: String, default: String): Flow<String>`; `RemoteFlagSource` thêm `string(key)`, `FirebaseRemoteFlagSource` đọc bằng `asString()`. Khóa `poses_catalog_path`, mặc định `DEFAULT_CATALOG_PATH` trong `PosesSettings`, là đường dẫn catalog mẫu ở mục 3.3. Bản staging dùng file Firebase giả nên luôn dùng giá trị mặc định; không cần sửa `app/src/staging`. Giá trị từ xa không hợp lệ với `AssetPath` thì dùng mặc định và log `warn`. Phần sửa `:core:config` có ích cho mọi app: ghi đề xuất về dự án mẫu trong mô tả PR (`docs/ops/from-app.md`).

**Log** (ADR-0019), thẻ `Poses`: `info` khi catalog sẵn sàng (số nhóm, số tư thế, số tư thế bị bỏ), khi bộ khởi đầu xong (số file); `warn` khi tải lỗi (nội dung, lý do), khi đọc catalog lỗi, khi tư thế bị bỏ, khi chuỗi DataStore hỏng; `info` khi thử lại. Không ghi đường dẫn, mã tư thế hay nội dung file.

**Hilt** (`di/PosesModule.kt`, `internal`): gắn ba interface vào lớp cài đặt `@Singleton`; cung cấp `PosesClock`, scope và dispatcher của module bằng qualifier `internal`. Mọi lớp nhận dispatcher và scope qua constructor để test dùng `StandardTestDispatcher` và `backgroundScope`. `:app` chưa phụ thuộc module này; đồ thị Hilt được kiểm khi T-010 dùng nó (mục 5, D).

**Thứ tự commit (L-025).** Bước 3: mọi file test, file mẫu, các fake, và khung rỗng của API công khai cùng lớp `internal` (hàm trả giá trị mặc định: danh sách rỗng, `CatalogState.NotLoaded`, `flowOf(default)` cho `string` của `DefaultRemoteSettings`, `flowOf(null)` cho `FirebaseRemoteFlagSource.string`), cùng `settings.gradle.kts` và `core/poses/build.gradle.kts`, để test biên dịch được và thất bại ở phần kiểm tra. `FakeRemoteFlagSource` thêm `strings` và `string(key)` ngay ở bước 3. Ở `:core:assets`, bước 3 gồm `AssetFailureReason` với `toFailureReason()` khung rỗng (luôn `OTHER`), `HttpStatusException`, kiểu mới `Unavailable(reason)` với `DefaultRemoteAssets` vẫn trả `Unavailable()`, và ba dòng test sửa vì đổi kiểu. Test cũ vẫn qua với khung này; test mới về `NETWORK`, `NOT_FOUND` thất bại ở phần khẳng định. `HttpAssetDownloader` ném `HttpStatusException` thuộc bước 4. `staging-sample.json` thuộc bước 3, `DEFAULT_CATALOG_PATH` thật thuộc bước 4 (khung rỗng để chuỗi `poses/catalog/v1/catalog-00000000.json`). Bước 4: phần cài đặt thật.

### 3.2. Danh sách file

<!-- Đây là ranh giới trong phiên tự chạy. AI chỉ được chạm vào các file trong bảng này. -->

Đường dẫn gói viết tắt: `.../poses` là `com/vglstudio/camera/pose/line/guide/core/poses`.

| Thao tác | Đường dẫn | Nội dung thay đổi |
| --- | --- | --- |
| Sửa | `settings.gradle.kts` | `include(":core:poses")` sau `:core:logging` |
| Tạo | `core/poses/build.gradle.kts` | Plugin library, hilt, serialization; `namespace`; bốn core và `kotlinx-serialization-json` |
| Tạo | `core/poses/consumer-rules.pro` | Rỗng, như các core khác |
| Tạo | `core/poses/src/main/kotlin/.../poses/model/Pose.kt` | `Pose`, `PoseCategory`, `SubjectType`, `PoseGender`, `Framing`, `Keypoint`, `PoseFile` |
| Tạo | `core/poses/src/main/kotlin/.../poses/model/LocalizedText.kt` | `LocalizedText`, `LocalizedList`, cách chọn ngôn ngữ |
| Tạo | `core/poses/src/main/kotlin/.../poses/model/PoseCatalog.kt` | `PoseCatalog` |
| Tạo | `core/poses/src/main/kotlin/.../poses/model/PoseLibrary.kt` | `PoseLibrary`, `PoseFilter`, `LibraryState`, `SavedPose`, `UsedPose` |
| Tạo | `core/poses/src/main/kotlin/.../poses/PoseLoadFailure.kt` | `PoseContent`, `LoadFailureReason`, `PoseLoadFailure`, `PoseFileResult`, `CatalogState`, `StarterSetResult` |
| Tạo | `core/poses/src/main/kotlin/.../poses/PoseCatalogRepository.kt` | Interface |
| Tạo | `core/poses/src/main/kotlin/.../poses/PoseHistoryRepository.kt` | Interface |
| Tạo | `core/poses/src/main/kotlin/.../poses/PoseLibraryRepository.kt` | Interface |
| Tạo | `core/poses/src/main/kotlin/.../poses/internal/CatalogJson.kt` | DTO `@Serializable` |
| Tạo | `core/poses/src/main/kotlin/.../poses/internal/CatalogParser.kt` | Đọc, kiểm, bỏ tư thế lỗi, `APP_SCHEMA` |
| Tạo | `core/poses/src/main/kotlin/.../poses/internal/PoseOrdering.kt` | `orderPoses`, dựng `PoseLibrary` |
| Tạo | `core/poses/src/main/kotlin/.../poses/internal/TimedAssetLoader.kt` | Chờ 15 giây, quy đổi lý do lỗi |
| Tạo | `core/poses/src/main/kotlin/.../poses/internal/DefaultPoseCatalogRepository.kt` | Cài đặt catalog, ảnh, bộ khởi đầu, `failures` |
| Tạo | `core/poses/src/main/kotlin/.../poses/internal/DataStorePoseHistoryRepository.kt` | Cài đặt đã lưu, gần đây |
| Tạo | `core/poses/src/main/kotlin/.../poses/internal/DefaultPoseLibraryRepository.kt` | Ghép catalog, đã lưu, gần đây |
| Tạo | `core/poses/src/main/kotlin/.../poses/internal/PosesSettings.kt` | Khóa `poses_catalog_path`, `DEFAULT_CATALOG_PATH`, `LOAD_TIMEOUT_MILLIS`, `RECENT_LIMIT = 20`, `STARTER_PARALLELISM = 4` |
| Tạo | `core/poses/src/main/kotlin/.../poses/internal/PosesClock.kt` | `fun interface PosesClock` |
| Tạo | `core/poses/src/main/kotlin/.../poses/di/PosesModule.kt` | Hilt module |
| Tạo | `core/poses/src/test/kotlin/.../poses/FakeRemoteAssets.kt` | Kết quả theo đường dẫn, độ trễ ảo, đếm số lần gọi mỗi đường dẫn |
| Tạo | `core/poses/src/test/kotlin/.../poses/FakeRemoteSettings.kt` | Giá trị trong bộ nhớ |
| Tạo | `core/poses/src/test/kotlin/.../poses/InMemoryDataStore.kt` | DataStore trong bộ nhớ, có chế độ ghi lỗi và đọc lỗi một lần |
| Tạo | `core/poses/src/test/kotlin/.../poses/Fixtures.kt` | Đọc file mẫu từ `src/test/resources`, dựng `Pose` cho test |
| Tạo | `core/poses/src/test/kotlin/.../poses/CatalogParserTest.kt` | Mục 4 |
| Tạo | `core/poses/src/test/kotlin/.../poses/LocalizedTextTest.kt` | Mục 4 |
| Tạo | `core/poses/src/test/kotlin/.../poses/PoseOrderingTest.kt` | Mục 4 |
| Tạo | `core/poses/src/test/kotlin/.../poses/TimedAssetLoaderTest.kt` | Mục 4 |
| Tạo | `core/poses/src/test/kotlin/.../poses/DefaultPoseCatalogRepositoryTest.kt` | Mục 4 |
| Tạo | `core/poses/src/test/kotlin/.../poses/DataStorePoseHistoryRepositoryTest.kt` | Mục 4 |
| Tạo | `core/poses/src/test/kotlin/.../poses/DefaultPoseLibraryRepositoryTest.kt` | Mục 4 |
| Tạo | `core/poses/src/test/kotlin/.../poses/StagingSampleCatalogTest.kt` | Mục 4 |
| Tạo | `core/poses/src/test/resources/catalog/valid.json` | Ba nhóm (một nhóm rỗng), sáu tư thế, đủ trường, hai tư thế có `keypoints`, ba tư thế `starter` |
| Tạo | `core/poses/src/test/resources/catalog/partly-invalid.json` | Mỗi loại tư thế lỗi một mục, cùng hai tư thế đúng |
| Tạo | `core/poses/src/test/resources/catalog/malformed.json` | JSON cắt dở |
| Tạo | `core/poses/src/test/resources/catalog/newer-schema.json` | `minAppSchema: 2` |
| Tạo | `core/poses/src/test/resources/catalog/staging-sample.json` | Catalog mẫu cho R2 staging (mục 3.3) |
| Tạo | `core/assets/src/main/kotlin/com/vglstudio/camera/pose/line/guide/core/assets/AssetFailureReason.kt` | `AssetFailureReason`, `toFailureReason()` |
| Sửa | `core/assets/src/main/kotlin/com/vglstudio/camera/pose/line/guide/core/assets/AssetDownloader.kt` | `HttpStatusException`; `HttpAssetDownloader` ném lớp này khi mã khác 200 |
| Sửa | `core/assets/src/main/kotlin/com/vglstudio/camera/pose/line/guide/core/assets/RemoteAssets.kt` | `Unavailable(reason)` có mặc định `OTHER`, KDoc |
| Sửa | `core/assets/src/main/kotlin/com/vglstudio/camera/pose/line/guide/core/assets/DefaultRemoteAssets.kt` | Trả `Unavailable(e.toFailureReason())` |
| Sửa | `core/assets/src/test/kotlin/com/vglstudio/camera/pose/line/guide/core/assets/DefaultRemoteAssetsTest.kt` | Hai dòng `Unavailable` thành `Unavailable(AssetFailureReason.OTHER)`, không đổi điều test kiểm tra |
| Tạo | `core/assets/src/test/kotlin/com/vglstudio/camera/pose/line/guide/core/assets/AssetFailureReasonTest.kt` | Mục 4 |
| Tạo | `core/assets/src/test/kotlin/com/vglstudio/camera/pose/line/guide/core/assets/DefaultRemoteAssetsFailureReasonTest.kt` | Mục 4 |
| Sửa | `feature/loading/src/test/kotlin/com/vglstudio/camera/pose/line/guide/feature/loading/data/DefaultPreparationRepositoryTest.kt` | Một dòng `AssetResult.Unavailable` thành `AssetResult.Unavailable()` |
| Sửa | `core/config/src/main/kotlin/com/vglstudio/camera/pose/line/guide/core/config/RemoteSettings.kt` | Thêm `string(key, default)` |
| Sửa | `core/config/src/main/kotlin/com/vglstudio/camera/pose/line/guide/core/config/RemoteFlagSource.kt` | Thêm `string(key)` |
| Sửa | `core/config/src/main/kotlin/com/vglstudio/camera/pose/line/guide/core/config/FirebaseRemoteFlagSource.kt` | `string(key)` đọc bằng `asString()` |
| Sửa | `core/config/src/test/kotlin/com/vglstudio/camera/pose/line/guide/core/config/FakeRemoteFlagSource.kt` | Thêm `strings` và `string(key)` |
| Sửa | `core/config/src/test/kotlin/com/vglstudio/camera/pose/line/guide/core/config/DefaultRemoteSettingsTest.kt` | Thêm hai test mới, không đổi test cũ |
| Sửa | `CLAUDE.md` | Thêm dòng `:core:poses` vào bảng cấu trúc module |

Không sửa `:app`, `gradle/libs.versions.toml`, `build-logic/`. `scripts/`, `.github/` và `build-logic/` không nhắc tới `settings.gradle.kts` hay các file của `:core:config`, `:core:assets`, `:feature:loading` ở trên (đã tìm bằng `grep -rn`), nên không bị ảnh hưởng. `app/src/androidTest/kotlin/com/vglstudio/camera/pose/line/guide/AppLaunchTest.kt`, `scripts/device-session.sh` và `scripts/status.py` có nhắc `CLAUDE.md` nhưng chỉ trong chú thích hay chữ in ra để dẫn tới một mục; plan chỉ thêm một dòng vào bảng module, không đổi tên hay xóa mục nào, nên ba file này không bị ảnh hưởng. Nhật ký plan, trạng thái plan và ticket, báo cáo phiên đi qua nhánh báo cáo của phiên (auto-run.md, mục 4).

### 3.3. Cờ tính năng, quảng cáo, dữ liệu

- Cờ tính năng: không có. Module chưa có màn hình; cờ `feature_poses_enabled` thuộc T-021.
- Quảng cáo: không chạm.
- Sự kiện phân tích: không chạm. Module chỉ cung cấp `analyticsValue` để feature ghi `content_load_failed` đã có trong spec.
- Tham số `RemoteSettings` mới: `poses_catalog_path` (chuỗi), mặc định là đường dẫn catalog mẫu bên dưới.
- File R2 (điểm chặn 8):
  - Catalog: `poses/catalog/v1/catalog-<sha8>.json`, với `<sha8>` là 8 ký tự đầu của SHA-256 nội dung file, đúng bản đề xuất mục 11.
  - Ảnh nội dung thật: `poses/img/<poseId>/preview_540-<sha8>.webp`, `preview_1200-<sha8>.webp`, `outline-<sha8>.webp` (khác bản đề xuất, câu hỏi 2).
  - Ảnh mượn của catalog mẫu (chỉ staging, bản đề xuất mục 11): `borrowed/poses/img/v1/<poseId>/preview_540.webp`, `preview_1200.webp`, `outline.webp`. Dùng thư mục `v1` vì đường dẫn phải có sẵn trước khi có ảnh; hợp lệ với `AssetPath`.
  - Catalog mẫu `staging-sample.json`: AI viết trong bước 3. Sáu tư thế một người lấy theo 6 tư thế SimplePose (bản đề xuất mục 11): mã `w01`, `w02`, `w03` (nhóm `women`), `m01`, `m02`, `m03` (nhóm `men`); thêm nhóm `couples` rỗng để thử ẩn nhóm. `w01`, `w02`, `m01`, `m02` là `starter`. Chữ do AI viết (không chép của app mẫu), tiếng Anh và tiếng Việt; các ngôn ngữ khác dùng tiếng Anh (câu hỏi 4). `w01`, `m01` có `keypoints` 33 điểm làm mẫu khuôn, số liệu giả, ghi rõ trong `datasetVersion` là `sample`.
  - AI không đưa file nào lên R2, không có khóa R2 và không được ghi đè, xóa file R2 (ADR-0015). Sau khi merge, dev đưa lên R2 staging: `staging-sample.json` thành `poses/catalog/v1/catalog-<sha8>.json` (tên trong `DEFAULT_CATALOG_PATH`), và 18 file ảnh mượn theo đường dẫn trên; ghi ảnh mượn vào `docs/borrowed-assets.md` kèm SHA-256 (bản đề xuất mục 11). Test không gọi mạng; T-010 là ticket đầu tiên tải thật.

## 4. Kiểm thử

Mọi test là unit test trên JVM, không Robolectric, không mạng. `FakeRemoteAssets` trả file tạm trong `TemporaryFolder`. Thời gian dùng `runTest` với thời gian ảo.

| Tiêu chí chấp nhận | Loại test | Tên test hoặc cách kiểm tra |
| --- | --- | --- |
| Đọc catalog đúng khuôn, giữ thứ tự catalog | Unit test | `CatalogParserTest.validCatalog_parsesCategoriesAndPosesInCatalogOrder` |
| Đọc đủ trường của tư thế | Unit test | `CatalogParserTest.validCatalog_readsEveryPoseField_includingKeypointsAndStarter` |
| `keypoints` tùy chọn | Unit test | `CatalogParserTest.poseWithoutKeypoints_hasNullKeypoints`, `keypointsNot33Points_dropsKeypointsKeepsPose` |
| `starter` thiếu là `false` | Unit test | `CatalogParserTest.starterMissing_isFalse` |
| Trường lạ không làm hỏng catalog | Unit test | `CatalogParserTest.unknownFields_areIgnored` |
| Catalog hỏng | Unit test | `CatalogParserTest.malformedJson_returnsInvalid` |
| Lược đồ quá mới | Unit test | `CatalogParserTest.minAppSchemaNewerThanApp_returnsUnsupportedSchema` |
| Bỏ tư thế lỗi, giữ phần còn lại | Unit test | `CatalogParserTest.partlyInvalid_dropsDuplicateUnknownCategoryUnknownEnumMissingEnglishAndInvalidPath_keepsValidPoses` |
| Tên không có chuỗi kỹ thuật (poses KB-1) | Unit test | `LocalizedTextTest.exactTag_isUsed`, `languageOnly_matchesRegionalVariant` (`es-MX` ra `es-419`, `pt` ra `pt-BR`), `missingLanguage_fallsBackToEnglish` |
| poses KB-1, thứ tự trong "Tất cả" | Unit test | `PoseOrderingTest.all_savedNewestFirst_thenRecentNewestFirst_thenRestInCatalogOrder` |
| poses KB-1, tư thế vừa lưu vừa dùng | Unit test | `PoseOrderingTest.poseSavedAndRecent_appearsOnceInSavedPart` |
| poses KB-1, thứ tự trong từng nhóm | Unit test | `PoseOrderingTest.category_appliesSameOrderWithinGroupOnly` |
| poses KB-1, ẩn nhóm rỗng | Unit test | `PoseOrderingTest.categories_hideEmptyGroups_keepCatalogOrder` |
| poses KB-1, dòng tổng số | Unit test | `PoseOrderingTest.totals_countEveryPoseAndOnlyVisibleCategories` |
| poses KB-3, "Đã lưu" mới lưu nhất trước | Unit test | `PoseOrderingTest.savedFilter_newestSavedFirst` |
| poses KB-3, tư thế đã lưu không còn trong catalog | Unit test | `PoseOrderingTest.savedPoseMissingFromCatalog_isNotShown`, `recentPoseMissingFromCatalog_isIgnored` |
| camera KB-1, tư thế mặc định | Unit test | `PoseOrderingTest.defaultPose_isFirstPoseInCatalogOrder_evenWhenOthersSavedOrRecent` |
| camera KB-1, tư thế dùng lần trước | Unit test | `PoseOrderingTest.lastUsedPose_isNewestRecentStillInCatalog`, `lastUsedPose_noneInCatalog_isNull` |
| poses KB-3, lưu và bỏ lưu, mở lại app vẫn còn | Unit test | `DataStorePoseHistoryRepositoryTest.setSavedTrueThenFalse_persistsAcrossRepositoryInstances` (hai đối tượng repository trên cùng `InMemoryDataStore`) |
| Lưu lại tư thế đã lưu | Unit test | `DataStorePoseHistoryRepositoryTest.setSavedTwice_keepsFirstSavedTime` |
| Chưa có dữ liệu | Unit test | `DataStorePoseHistoryRepositoryTest.nothingStored_savedAndRecentAreEmpty` |
| camera mục 6, đã dùng gần đây | Unit test | `DataStorePoseHistoryRepositoryTest.markUsed_movesPoseToFront_withClockTime` |
| Giới hạn danh sách gần đây | Unit test | `DataStorePoseHistoryRepositoryTest.markUsed_keepsAtMost20_dropsOldest` |
| poses KB-3, ghi lỗi | Unit test | `DataStorePoseHistoryRepositoryTest.writeFails_throwsIOException` (`assertTrue(error is IOException)`, CLAUDE.md bước 10) |
| Đọc lỗi thì thử lại | Unit test | `DataStorePoseHistoryRepositoryTest.readFailsOnce_emitsEmptyThenStoredValues` |
| Chuỗi lưu bị hỏng | Unit test | `DataStorePoseHistoryRepositoryTest.corruptedStoredValue_isEmpty_andLogsWarning` (`RecordingAppLogger`) |
| Tải được | Unit test | `TimedAssetLoaderTest.ready_returnsFile` |
| Phân loại lỗi tải | Unit test | `AssetFailureReasonTest.unknownHost_isNetwork`, `connectException_isNetwork`, `socketTimeout_isNetwork`, `networkErrorAsCause_isNetwork` (`IOException` bọc `UnknownHostException`), `http404_isNotFound`, `http500_isOther`, `plainIOException_isOther` |
| `:core:assets` trả lý do | Unit test | `DefaultRemoteAssetsFailureReasonTest.downloaderThrowsUnknownHost_returnsUnavailableNetwork`, `downloaderThrowsHttp404_returnsUnavailableNotFound`, `downloaderThrowsPlainIOException_returnsUnavailableOther`, `keptFile_returnsReadyWithoutReason` (bộ tải giả, `TemporaryFolder`, như `DefaultRemoteAssetsTest`) |
| Test cũ vẫn đúng sau khi đổi kiểu | Test sẵn có | `DefaultRemoteAssetsTest`, `DefaultPreparationRepositoryTest.unavailableFile_returnsFalse` qua, với ba dòng sửa ở mục 3.1 |
| `Unavailable` thành lỗi, đúng lý do | Unit test | `TimedAssetLoaderTest.unavailableNetwork_returnsFailedWithReasonNetwork`, `unavailableNotFound_returnsFailedWithReasonNotFound`, `unavailableOther_returnsFailedWithReasonOther` |
| poses KB-5, loading KB-2: 15 giây | Unit test | `TimedAssetLoaderTest.noAnswerWithin15s_returnsTimeoutAtExactly15s` (fake trễ 60 giây, `currentTime == 15_000` khi trả về), `answerAt14999ms_returnsReady` |
| Hết giờ mà tải vẫn tiếp | Unit test | `TimedAssetLoaderTest.afterTimeout_downloadContinues_andNextLoadIsReadyWithoutNewDownload` |
| Đường dẫn catalog từ `RemoteSettings` | Unit test | `DefaultPoseCatalogRepositoryTest.loadCatalog_usesPathFromRemoteSettings` |
| Đường dẫn từ xa không hợp lệ | Unit test | `DefaultPoseCatalogRepositoryTest.invalidRemotePath_usesDefaultPath_andLogsWarning` |
| Catalog sẵn sàng | Unit test | `DefaultPoseCatalogRepositoryTest.loadCatalog_ready_updatesStateToReady` |
| Catalog lỗi, cả ba kiểu | Unit test | `DefaultPoseCatalogRepositoryTest.loadCatalog_unavailableNetwork_failsWithCatalogNetwork_andEmitsFailure`, `loadCatalog_timeout_failsWithTimeout`, `loadCatalog_malformed_failsWithOther` |
| Không tải lại khi đã có | Unit test | `DefaultPoseCatalogRepositoryTest.loadCatalog_whenReady_doesNotLoadAgain`, `loadCatalog_concurrentCalls_loadOnce` |
| Thử lại sau lỗi | Unit test | `DefaultPoseCatalogRepositoryTest.loadCatalog_afterFailure_loadsAgain` |
| Ảnh nhỏ, ảnh lớn, nét viền | Unit test | `DefaultPoseCatalogRepositoryTest.loadFile_usesCatalogPathOfEachFileKind`, `loadFile_failure_emitsContentAndReason` |
| loading KB-1, bộ khởi đầu | Unit test | `DefaultPoseCatalogRepositoryTest.starterSet_loadsThumbnailAndOutlineOfStarterPosesOnly` |
| loading KB-1, tiến trình theo từng file, không lùi | Unit test | `DefaultPoseCatalogRepositoryTest.starterSet_reportsProgressAfterEachFile_neverDecreasing` |
| loading KB-2, thử lại chỉ tải phần thiếu | Unit test | `DefaultPoseCatalogRepositoryTest.starterSet_retryAfterFailure_loadsOnlyMissingFiles_andProgressStartsFromDone` |
| loading KB-2, 15 giây mỗi file | Unit test | `DefaultPoseCatalogRepositoryTest.starterSet_fileWithoutAnswer_failsAfter15s_othersComplete` |
| Bộ khởi đầu khi catalog lỗi | Unit test | `DefaultPoseCatalogRepositoryTest.starterSet_catalogFails_returnsIncompleteWithCatalogFailure` |
| Log không chứa đường dẫn, mã tư thế | Unit test | `DefaultPoseCatalogRepositoryTest.logs_haveTagPoses_andNoPathsOrPoseIds` |
| Thư viện tự tải catalog khi chưa có | Unit test | `DefaultPoseLibraryRepositoryTest.library_catalogNotLoaded_startsLoading_thenReady` |
| Thư viện đổi theo đã lưu, gần đây | Unit test | `DefaultPoseLibraryRepositoryTest.library_updatesWhenSavedOrRecentChange` (Turbine) |
| Thư viện khi catalog lỗi và thử lại | Unit test | `DefaultPoseLibraryRepositoryTest.library_catalogFailed_emitsFailed_thenRetryEmitsReady` |
| Catalog mẫu đọc được | Unit test | `StagingSampleCatalogTest.stagingSample_parsesSixPoses_fourStarter_andHidesEmptyCouples` |
| Tên file catalog mẫu khớp nội dung | Unit test | `StagingSampleCatalogTest.defaultCatalogPath_isV1Folder_andEndsWithFirst8HexOfSha256OfStagingSample` |
| Đường dẫn ảnh của catalog mẫu | Unit test | `StagingSampleCatalogTest.stagingSample_everyImagePathIsAValidBorrowedV1AssetPath` |
| `RemoteSettings.string` | Unit test | `DefaultRemoteSettingsTest.stringWithoutRemoteValue_usesDefault`, `stringRemoteValue_replacesDefault` |
| Quy tắc phụ thuộc module (ADR-0001) | Kiểm tra tự động | `checkModuleDependencies` trong `scripts/check.sh` |
| Catalog mẫu có trên R2 staging | Kiểm tra thủ công | Sau khi dev đưa file lên: `curl -I` với địa chỉ staging cộng `DEFAULT_CATALOG_PATH` và một ảnh `borrowed/poses/img/v1/w01/preview_540.webp`, đều trả `200` |

**Ngoại lệ viết test trước:** `HttpAssetDownloader` ném `HttpStatusException` chỉ kiểm được bằng mạng thật, như các phần khác của lớp này (chưa có test); phần phân loại của nó có test qua `AssetFailureReasonTest.http404_isNotFound`. `build.gradle.kts`, `consumer-rules.pro`, `settings.gradle.kts`, `PosesModule.kt`, `FirebaseRemoteFlagSource.string` (cần Firebase thật, giống các hàm cùng lớp chưa có test) và dòng mới trong `CLAUDE.md` không có test riêng; chúng được kiểm qua build và các test trên. Đồ thị Hilt chỉ được kiểm khi T-010 dùng module.

## 5. Chấm rủi ro

**Điểm chặn bị chạm:** điểm 7 (module core mới `:core:poses`, sửa `:core:config` và `:core:assets` dùng chung) và điểm 8 (cách đặt tên, đường dẫn catalog và ảnh trên R2). Rủi ro cao, bắt buộc duyệt chéo. Không chạm điểm 9: hai khóa DataStore mới, không có dữ liệu cũ phải chuyển.

Bảng điểm dưới đây chỉ để tham khảo:

| Tiêu chí | Điểm (0 đến 3) | Lý do |
| --- | --- | --- |
| A. Phạm vi thay đổi | 3 | Bốn module (`:core:poses` mới, `:core:config`, `:core:assets`, test của `:feature:loading`), `settings.gradle.kts`, đổi giao diện công khai `RemoteSettings` và `AssetResult` (đã ở mức cao nhất) |
| B. Độ rõ của yêu cầu | 2 | Ba spec đã xác nhận, nhưng còn câu hỏi ảnh hưởng cách làm: đường dẫn ảnh của bản đề xuất không hợp với `AssetPath` (câu 2) |
| C. Khả năng kiểm chứng tự động | 1 | Mọi logic có unit test với thời gian ảo; file trên R2 staging kiểm tra thủ công bằng `curl` |
| D. Ảnh hưởng chéo | 3 | T-010, T-011, T-014, T-021, T-022 dựa vào API này và vào lý do lỗi của `:core:assets`; mọi màn tải file đi qua `AssetResult` đã đổi kiểu. Đồ thị Hilt chưa được kiểm tới khi T-010 dùng |
| E. Độ mới của kỹ thuật | 2 | `kotlinx.serialization` đọc file lần đầu trong dự án; chờ có giới hạn quanh lần tải không hủy được |
| **Tổng** | 11 | **Mức: Cao** |

## 6. Ảnh hưởng chéo

- File hoặc thành phần dùng chung bị chạm:
  - `:core:config`: thêm `string` vào `RemoteSettings` và `RemoteFlagSource`. Chỉ thêm, `:core:ads` (`AdsController`) và các fake hiện có không phải đổi, trừ `FakeRemoteFlagSource` trong danh sách. Đề xuất về dự án mẫu ghi trong mô tả PR.
  - `settings.gradle.kts`: thêm một dòng `include`.
  - `:core:assets`: `AssetResult.Unavailable` đổi từ `data object` sang `data class` có `reason` mặc định; `DefaultRemoteAssets` trả lý do; `HttpAssetDownloader` ném `HttpStatusException`. Mọi chỗ dùng đã nằm trong mục 3.2. Thay đổi ở module có `origin: template`, không đưa về dự án mẫu theo quyết định của người phụ trách sản phẩm.
  - DataStore chung `app_preferences`: thêm hai khóa `poses_saved`, `poses_recent`, không trùng khóa hiện có (`sample_count`).
- Task khác trong sprint có thể xung đột:
  - T-010 (loading tải catalog và bộ khởi đầu), T-014 (camera chọn tư thế), T-021 (thư viện), T-022 (chi tiết) phụ thuộc trực tiếp vào API ở mục 3.1. T-010 thay `DefaultPreparationRepository` bằng `loadCatalog()` và `loadStarterSet()`.
  - T-011 (`:core:network`) phụ thuộc lý do `NETWORK` mà plan này thêm vào `:core:assets`: nó báo hộp thoại mạng khi `Unavailable` có lý do `NETWORK`, không phân loại lại. T-011 cũng sửa `DefaultRemoteAssets.kt` và `AssetsModule.kt` (thêm `networkFailures` ở cuối constructor), và có thể sửa `RemoteSettings` cho thời gian chờ 3 giây; hai task cùng sửa `:core:assets`, `:core:config` nên chạy lần lượt.
  - T-010 cũng sửa `RemoteConfigRefresher` của `:core:config`, khác file với plan này.
- Đề xuất thứ tự chạy: chạy T-009 trước T-011, T-010, T-014, T-021. T-011 bắt buộc sau T-009 vì dùng lý do lỗi của `:core:assets`.

## 7. Điều kiện dừng riêng

1. Cần sửa `:core:assets` ngoài phần nêu ở mục 3.1 (ví dụ `AssetPath`, `AssetsModule`, tham số constructor của `DefaultRemoteAssets`), hoặc thấy thêm chỗ dùng `AssetResult.Unavailable` như giá trị ngoài ba dòng đã liệt kê.
2. Đổi kiểu `Unavailable` làm một test sẵn có đổi điều nó kiểm tra, không chỉ thêm `()` hay lý do `OTHER`.
3. Cần sửa `gradle/libs.versions.toml` hay `build-logic/`, ví dụ plugin `kotlin.serialization` không áp dụng được cho module core bằng `alias`.
4. Cần sửa `:app` hay `app/src/staging` để build hoặc test qua.
5. Test sẵn có của `:core:config`, `:core:ads`, `:feature:loading` hay `:feature:sample` thất bại sau khi thêm `string` vào `RemoteSettings`.
6. Câu hỏi 2 được trả lời theo hướng giữ mẫu đường dẫn ảnh của bản đề xuất mà không sửa `AssetPath`: hai điều này không cùng làm được.
7. `TimedAssetLoaderTest.noAnswerWithin15s_returnsTimeoutAtExactly15s` không qua được mà không đổi cách chờ đã nêu ở mục 3.1.

## 8. Câu hỏi cho người duyệt

1. **Lý do lỗi `network` và `not_found`: đã quyết, làm trong T-009.** Phiên chính đối chiếu với plan T-011 và chọn làm ngay ở đây: `:core:assets` thêm lý do vào `AssetResult.Unavailable` (mục 3.1), `:core:poses` chuyển sang `LoadFailureReason`, T-011 dùng lại lý do này. Không cần trả lời.
- Trả lời: OK.
2. **Đường dẫn ảnh của bản đề xuất mâu thuẫn với ADR-0015.** `poses/img/<poseId>/<sha8>/preview_540.webp` bị `AssetPath` từ chối: thư mục mã băm không được coi là phiên bản, chỉ thư mục `v<n>` hoặc mã băm trong tên file.
   - Đề xuất của AI: đặt mã băm vào tên file, `poses/img/<poseId>/preview_540-<sha8>.webp` (cũng vậy cho `preview_1200`, `outline`), và catalog ghi sẵn đường dẫn đầy đủ của từng ảnh để app không tự ghép. Người làm nội dung theo mẫu này. Không sửa ADR-0015. Ảnh mượn của catalog mẫu dùng `borrowed/poses/img/v1/<poseId>/...`.
   - Trả lời: OK.
3. **Danh sách đã dùng gần đây giữ bao nhiêu tư thế?** Spec không nêu.
   - Đề xuất của AI: 20 tư thế, đủ cho hàng đầu dải camera và thư viện mà không đẩy hết phần còn lại xuống. Danh sách đã lưu không giới hạn.
   - Trả lời: OK.
4. **Ngôn ngữ thiếu bản dịch.** Catalog mẫu chỉ có tiếng Anh và tiếng Việt; nội dung thật có thể thiếu vài ngôn ngữ lúc đầu.
   - Đề xuất của AI: khớp đủ thẻ ngôn ngữ, rồi khớp ngôn ngữ, rồi dùng tiếng Anh. Tiếng Anh là bắt buộc; tư thế thiếu tên hoặc bước tiếng Anh bị bỏ. Kiểm đủ 7 ngôn ngữ để cho script kiểm catalog của người làm nội dung.
   - Trả lời: OK.
5. **"Tư thế đang dùng" của camera (spec camera, mục 6).**
   - Đề xuất của AI: không lưu khóa riêng. Tư thế đang dùng là tư thế mới nhất trong danh sách đã dùng gần đây (`lastUsedPose`), vì camera ghi "đã dùng" mỗi lần chọn. Nếu muốn lần mở chi tiết rồi bấm "Chụp với tư thế này" không tính là "đã dùng" thì cần khóa riêng ở T-014.
   - Trả lời: OK.
6. **Catalog có lược đồ mới hơn app (`minAppSchema` lớn hơn 1).**
   - Đề xuất của AI: từ chối catalog đó, báo lỗi như catalog hỏng, log rõ lý do. Người làm nội dung chỉ tăng `minAppSchema` khi bản app mới đã phát hành, và để đường dẫn cũ trong Remote Config cho app cũ (ADR-0015: file cũ không bị xóa). Cần một điều kiện Remote Config theo phiên bản app khi việc này xảy ra.
   - Trả lời: OK.
7. **Feature thêm một dòng phụ thuộc vào `build.gradle.kts` của mình.** `CLAUDE.md` (bước 1) ghi file đó chỉ áp dụng plugin và `namespace`; convention plugin chỉ gắn core của dự án mẫu.
   - Đề xuất của AI: T-010, T-014, T-021 thêm `implementation(project(":core:poses"))` vào `build.gradle.kts` của feature. Không sửa `build-logic/` vì `:core:poses` chỉ có ở app này. Dòng mới trong bảng module của `CLAUDE.md` ghi điều này.
   - Trả lời: OK.
8. **Đường dẫn catalog mặc định trong bản production.** `DEFAULT_CATALOG_PATH` chỉ tới catalog mẫu của staging, nên bản production không lấy được Remote Config sẽ không có catalog.
   - Đề xuất của AI: trước khi phát hành, đặt `poses_catalog_path` trên Remote Config production và đổi `DEFAULT_CATALOG_PATH` sang catalog thật đã có trên cả hai bucket. Ghi thành việc trong spec hay ticket phát hành, không làm trong T-009.
   - Trả lời: OK.
9. **Ai đưa catalog mẫu và 18 ảnh mượn lên R2 staging?**
   - Đề xuất của AI: dev, sau khi PR được merge, theo mục 3.3, rồi kiểm bằng `curl`. AI không có khóa R2 và không được ghi lên R2. Nếu cần catalog mẫu trên R2 trước T-010, dev làm việc này trước phiên có T-010.
   - Trả lời: OK.

## 9. Quyết định duyệt

<!-- Người duyệt điền. AI không điền mục này. -->

- **Quyết định:** Duyệt
- **Người duyệt:** Nhật, **duyệt chéo:**Tuấn
- **Ghi chú:** 

## 10. Nhật ký thực hiện

<!-- AI ghi khi task đã có kết quả cuối (đã merge hoặc đã dừng), trên nhánh báo cáo của phiên (auto-run.md, bước 10). Mục mới thêm vào cuối, không sửa mục cũ. -->

### RUN-YYYYMMDD-NN, phiên bản plan 1

- **Kết quả:** đã merge / dừng / CI thất bại
- **Nhánh, PR:** <...>
- **Đã làm:** <...>
- **Test viết trước:** <số test, xác nhận đã thất bại đúng lý do trước khi viết code>
- **Rà soát độc lập:** <số điểm phát hiện, đã xử lý những gì, điểm nào còn để người xem>
- **Kết quả CI:** <build, lint, unit test, UI test, screenshot test>
- **Lệch so với plan:** <chỗ nào và vì sao, hoặc "Không có">
- **Việc còn lại:** <...>
- **Cần người xem:** <điểm dev nên kiểm tra sáng nay>
