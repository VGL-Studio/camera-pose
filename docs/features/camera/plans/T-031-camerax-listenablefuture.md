---
ticket: T-031
title: Gọi thẳng CameraControl.setZoomRatio, bỏ reflection
status: approved                # draft | approved | stopped | done
version: 1                   # tăng khi plan được sửa lại sau phản hồi hoặc sau khi dừng
risk_score:                  # tổng điểm theo risk-scoring.md, để trống nếu chạm điểm chặn
risk_level: high             # low | medium | high
token_estimate: 5.0M         # tổng token dự kiến, ví dụ 20M; số gợi ý: scripts/token-usage.py estimate (docs/ops/token-usage.md)
blocking_points: [sdk]       # các điểm chặn bị chạm, ví dụ [ads, sdk]
run_order:                   # AI điền sau họp đánh giá rủi ro
reviewer: Nhật                   # người duyệt
cross_reviewer: Tuấn             # bắt buộc khi risk_level là high
approved_at: 2026-10-23
---

# PLAN T-031: GỌI THẲNG CAMERACONTROL.SETZOOMRATIO, BỎ REFLECTION

## 1. Tóm tắt

Đưa Guava `31.1-android`, đúng bản đã có trong APK qua Firebase Analytics, lên classpath biên dịch của `:feature:camera`. Camera gọi thẳng `CameraControl.setZoomRatio` trong một hàm nhỏ `applyZoom` có unit test (MockK), rồi bỏ reflection và luật keep của T-013. Kiểm chứng bằng unit test, `scripts/check.sh` và bước thử zoom trên bản staging.

## 2. Hiểu yêu cầu

Ticket xử lý các kịch bản sau trong `features/camera/spec.md`:

| Kịch bản | Tiêu chí chấp nhận | Cách hiểu của AI |
| --- | --- | --- |
| KB-4 | Khi máy có ống kính góc rộng thì nút zoom có 0.5x, 1x, 2x; không có thì chỉ có 1x và 2x. Mức đang chọn có nền màu nhấn | Giữ nguyên hành vi của T-013. Ticket chỉ đổi cách gọi CameraX, nên mức zoom chọn trên màn hình phải tới đúng `setZoomRatio` như trước |

Bài học L-007: hàm CameraX trả `ListenableFuture` không gọi được vì Guava không có trên classpath biên dịch; reflection kèm luật keep dễ vỡ ở bản minify.

Thiết kế tham chiếu: không đổi giao diện.

ADR liên quan: ADR-0019 (log qua `AppLogger`).

## 3. Giải pháp

### 3.1. Cách làm

**Vì sao thiếu lớp khi biên dịch** (đo ngày 2026-10-23 bằng `./gradlew :feature:camera:dependencies`):

- `camera-core` 1.6.2 khai báo `com.google.guava:listenablefuture:1.0` ở API.
- Ở runtime, Firebase Analytics (qua `play-services-measurement`) kéo `com.google.guava:guava:31.1-android`. Guava đã chứa `ListenableFuture`, nên Gradle thay `listenablefuture` bằng bản rỗng `9999.0-empty-to-avoid-conflict-with-guava`.
- Classpath biên dịch được ép khớp phiên bản với runtime, nên nhận bản rỗng đó, nhưng không có Guava. Lớp `ListenableFuture` vì thế không có lúc biên dịch.

`androidx.concurrent:concurrent-futures-ktx` không giải quyết được: nó cũng chỉ khai báo `listenablefuture` ở API, nên lại thành bản rỗng. Vì vậy plan thêm chính Guava, đúng bản `31.1-android` mà runtime đã có. APK không thêm thư viện nào.

Các bước:

- `gradle/libs.versions.toml`: `guava = "31.1-android"` ở `[versions]` (cạnh `camerax`, chú thích "the version Firebase already brings at runtime; compile classpath only gains it, plan T-031") và `guava = { group = "com.google.guava", name = "guava", version.ref = "guava" }` ở `[libraries]`.
- `feature/camera/build.gradle.kts`: `implementation(libs.guava)`, chú thích dẫn plan T-031 và L-007.
- File mới `camera/CameraZoom.kt` có hàm `internal fun applyZoom(control: CameraControl, ratio: Float, logger: AppLogger)`:
  - gọi `control.setZoomRatio(ratio)` trong `runCatching`;
  - lỗi gọi đồng bộ thì `logger.warn("Camera", "Zoom could not be set", it)`, giống câu log hiện có.
  - Không chờ future: kết quả không dùng tới, như T-013.
- `CameraXController.setZoom` gọi `applyZoom(control, ratio, logger)`. Bỏ `import java.lang.reflect.Method`, bỏ `setZoomRatio` trong `companion object` và chú thích về reflection.
- `feature/camera/consumer-rules.pro`: bỏ luật keep của `setZoomRatio` cùng dòng chú thích. Gọi thẳng thì R8 tự giữ hàm. File còn trống thì giữ một dòng chú thích "No keep rules (plan T-031 removed the setZoomRatio rule)", để file vẫn có mà không đổi `build.gradle.kts` của module.

**Thứ tự làm (L-025).**

- Bước 3 (commit test):
  - `libs.versions.toml` và `build.gradle.kts`, vì test gọi `setZoomRatio` và cần Guava để biên dịch;
  - `CameraZoom.kt` dạng khung rỗng: `applyZoom` không làm gì;
  - `CameraZoomTest.kt`.
- Bước 4 (commit code): thân `applyZoom`, `CameraXController.kt`, `consumer-rules.pro`.

### 3.2. Danh sách file

| Thao tác | Đường dẫn | Nội dung thay đổi |
| --- | --- | --- |
| Sửa | `gradle/libs.versions.toml` | Phiên bản và thư viện `guava` `31.1-android` |
| Sửa | `feature/camera/build.gradle.kts` | `implementation(libs.guava)` |
| Tạo | `feature/camera/src/main/kotlin/com/vglstudio/camera/pose/line/guide/feature/camera/camera/CameraZoom.kt` | Hàm `applyZoom` |
| Sửa | `feature/camera/src/main/kotlin/com/vglstudio/camera/pose/line/guide/feature/camera/camera/CameraXController.kt` | `setZoom` gọi `applyZoom`; bỏ reflection |
| Sửa | `feature/camera/consumer-rules.pro` | Bỏ luật keep `setZoomRatio` |
| Tạo | `feature/camera/src/test/kotlin/com/vglstudio/camera/pose/line/guide/feature/camera/camera/CameraZoomTest.kt` | Unit test ở mục 4 |
| Sửa | `docs/features/camera/plans/T-031-camerax-listenablefuture.md` | Nhật ký thực hiện |
| Sửa | `docs/features/camera/tickets/T-031-camerax-listenablefuture.md` | Trạng thái |

Nơi khác đọc file plan sửa: `gradle/libs.versions.toml` được mọi module và `build-logic/` đọc qua catalog. Thêm một khóa mới không đổi khóa nào khác. `scripts/plan-check.py` đọc catalog để biết thư viện đã có, nên sau ticket này `com.google.guava:guava` được coi là đã có. Các file khác nhắc tới catalog không bị ảnh hưởng:
- `build-logic/settings.gradle.kts` chỉ nạp catalog cho `build-logic`.
- `build-logic/convention/src/main/kotlin/AndroidApplicationConventionPlugin.kt` chỉ nhắc catalog trong một chú thích (dòng 57).
- `scripts/tests/run.sh` dựng catalog giả của riêng nó trong thư mục tạm.

### 3.3. Cờ tính năng, quảng cáo, dữ liệu, quyền, thư viện

- Cờ tính năng: Không có.
- Quảng cáo: Không chạm.
- Sự kiện phân tích: Không chạm.
- File R2: Không chạm.
- **Quyền truy cập:** Không thêm.
- **Thư viện và SDK:** thêm `com.google.guava:guava:31.1-android`, giấy phép Apache 2.0, vào classpath biên dịch của `:feature:camera` (`implementation`).
  - Bản này đã có trong APK ở runtime qua Firebase Analytics (`firebase-analytics` 23.2.0, `play-services-measurement`). Đo ngày 2026-10-23: runtime classpath của `:feature:camera` vẫn là `guava:31.1-android`, trước và sau khi thêm. Vì vậy APK không thêm mã nào.
  - Guava là thư viện Java thuần, không có `AndroidManifest.xml`, nên không kéo quyền nào vào manifest gộp. Kiểm ở bước 5: so danh sách `uses-permission` trong `app/build/intermediates/merged_manifest/stagingDebug/processStagingDebugMainManifest/AndroidManifest.xml` sau `./gradlew :app:processStagingDebugMainManifest`, trước và sau thay đổi; phải giống hệt.
  - Không thêm `concurrent-futures-ktx` (lý do ở mục 3.1).

### 3.4. Phương án dự phòng

- **Điểm chưa chắc:** một bản Firebase sau này kéo Guava bản khác ở runtime; khi đó bản `31.1-android` của catalog có thể bị nâng lên lúc giải phụ thuộc.
- **Điều kiện chuyển:** ở bước 5, runtime classpath của `:app` (`./gradlew :app:dependencies --configuration stagingDebugRuntimeClasspath`) cho Guava khác `31.1-android`, hoặc có thêm artifact Guava khác.
- **Cách làm thay:** đặt Guava là `compileOnly(libs.guava)` thay cho `implementation`: classpath biên dịch có lớp, còn runtime giữ đúng bản Firebase kéo vào. Ghi lý do vào nhật ký. Test ở mục 4 giữ nguyên (`testImplementation` nhận Guava qua runtime classpath của test).

## 4. Kiểm thử

| Tiêu chí chấp nhận | Loại test | Tên test hoặc cách kiểm tra |
| --- | --- | --- |
| KB-4, mức zoom chọn tới CameraX | Unit test | `CameraZoomTest.applyZoom_setsTheRatioOnTheCameraControl`: `val control = mockk<CameraControl>(relaxed = true)`; `applyZoom(control, 2f, RecordingAppLogger())`; `verify(exactly = 1) { control.setZoomRatio(2f) }`. Khung rỗng của bước 3 không gọi gì nên test thất bại ở `verify` |
| KB-4, lỗi zoom không làm app dừng | Unit test | `CameraZoomTest.applyZoom_logsAWarning_whenCameraXThrows`: `every { control.setZoomRatio(any()) } throws IllegalStateException("closed")`; không ném ra ngoài; `logger.entries` có đúng một dòng `WARN`, tag `Camera`, câu `Zoom could not be set`, `error is IllegalStateException`. Bước đối chứng (L-035): cùng test gọi thêm `applyZoom(control, 1f, logger)` với `every { control.setZoomRatio(1f) } returns mockk(relaxed = true)` và khẳng định số dòng log vẫn là 1, để khung rỗng (không log gì) thất bại ở số dòng chứ không qua |
| KB-4, giữ hành vi T-013 | Unit test, screenshot test | Các test hiện có của `:feature:camera` (`CameraViewModelTest` với `FakeCameraController`, `CameraScreenTest`, `CameraScreenScreenshotTest`) qua mà không sửa |
| Không thêm quyền, APK không thêm thư viện | Lệnh kiểm tra ở bước 5 | So `uses-permission` của manifest gộp trước và sau (mục 3.3); `./gradlew :app:dependencies --configuration stagingDebugRuntimeClasspath` có Guava đúng `31.1-android` |
| Zoom chạy ở bản minify | Kiểm tra thủ công | Trên bản staging (minify như bản phát hành), mở camera, bấm 2x rồi 1x (và 0.5x nếu máy có): khung xem trước đổi theo; `adb logcat \| grep ' VGL\.'` không có `Zoom could not be set` |

**Ngoại lệ viết test trước:** sửa `CameraXController.setZoom`, `consumer-rules.pro` và hai file build không có test riêng. `CameraXController` cần camera thật, Robolectric không dựng được. Phần logic nằm trong `applyZoom`, đã có test; phần nối kiểm bằng bước thủ công ở trên.

### 4.1. Chạy thử

- **Đã chạy thử:** ngày 2026-10-23, trên nhánh `claude/bai-hoc-RUN-20261023-02` (commit 54853d8), không commit, đã hoàn tác bằng `git checkout`:
  - thêm `guava = "31.1-android"` và thư viện `guava` vào catalog, `implementation(libs.guava)` vào `feature/camera/build.gradle.kts`;
  - thay `setZoomRatio.invoke(control, ratio)` bằng `control.setZoomRatio(ratio)`.
- **Lệnh:** `./gradlew :feature:camera:compileDebugKotlin` và `./gradlew :feature:camera:dependencies --configuration debugRuntimeClasspath` (JDK 21, Android SDK cài tạm). Chưa chạy `scripts/check.sh`; tiêu chí E là 1, nên mục này không bắt buộc. Phiên chạy `scripts/check.sh` ở bước 5.
- **Kết quả:**
  - Biên dịch qua (BUILD SUCCESSFUL); gọi thẳng `setZoomRatio` không còn lỗi thiếu lớp `ListenableFuture`.
  - Runtime classpath vẫn chỉ có `com.google.guava:guava:31.1-android`, cùng `listenablefuture:9999.0-empty-to-avoid-conflict-with-guava`, như trước khi thêm.
  - Chưa chạy: unit test mới, `assembleStagingRelease`, manifest gộp.

## 5. Chấm rủi ro

**Điểm chặn bị chạm:** điểm 2 (thêm phụ thuộc trực tiếp vào thư viện bên thứ ba, dù cùng bản đã có ở runtime). Plan xếp **Cao**, cần duyệt chéo. Nếu không tính điểm chặn, năm tiêu chí dưới đây cho 3 điểm.

| Tiêu chí | Điểm (0 đến 3) | Lý do |
| --- | --- | --- |
| A. Phạm vi thay đổi | 1 | Một module feature cùng một dòng của catalog |
| B. Độ rõ của yêu cầu | 0 | Hành vi không đổi; cách làm đã đo |
| C. Khả năng kiểm chứng tự động | 1 | Logic có unit test; zoom ở bản minify kiểm thủ công |
| D. Ảnh hưởng chéo | 0 | Catalog thêm khóa mới, không module nào khác dùng |
| E. Độ mới của kỹ thuật | 1 | MockK và `CameraControl` đã dùng; chỉ đổi cách gọi |
| **Tổng** | 3 | **Mức: Cao (điểm chặn 2)** |

## 6. Ảnh hưởng chéo

- File hoặc thành phần dùng chung bị chạm: `gradle/libs.versions.toml` (thêm khóa).
- Task khác có thể xung đột: ticket camera sau này sửa `CameraXController.kt` (T-015, T-017, T-020); không task nào trong danh sách chạy hiện có.
- Đề xuất thứ tự chạy: task độc lập, chạy sớm. Ticket nhỏ, giúp các ticket camera sau dùng được hàm trả `ListenableFuture` mà không cần reflection, ví dụ lấy nét chạm (`startFocusAndMetering`).

## 7. Điều kiện dừng riêng

- Thêm Guava làm manifest gộp có thêm quyền: dừng, không merge.
- Runtime classpath của `:app` có Guava khác `31.1-android`, và phương án dự phòng ở mục 3.4 vẫn không giữ được bản đó: dừng.
- Một test hiện có của `:feature:camera` thất bại sau thay đổi: dừng, không sửa test cũ.

## 8. Câu hỏi cho người duyệt

1. **Thêm Guava thay vì giữ reflection.** Guava đã có trong APK; plan chỉ đưa nó lên classpath biên dịch, và CameraX cùng Firebase vẫn quyết phiên bản ở runtime. Đề xuất của AI: thêm, vì reflection kèm luật keep dễ vỡ ở bản minify, và các ticket camera sau (lấy nét, phơi sáng) cũng cần gọi hàm trả `ListenableFuture`.
- Trả lời: Đồng ý với đề xuất của AI.
2. **`implementation` hay `compileOnly`.** `compileOnly` không bao giờ đổi runtime nhưng sẽ hỏng nếu Firebase thôi kéo Guava. Đề xuất của AI: `implementation`, giữ `compileOnly` làm phương án dự phòng ở mục 3.4.
- Trả lời: Đồng ý với đề xuất của AI.
3. **Mức rủi ro.** Plan xếp Cao chỉ vì điểm chặn 2. Cần một dev khác duyệt chéo.
- Trả lời: Đồng ý với đề xuất của AI.

## 9. Quyết định duyệt

<!-- Người duyệt điền. AI không điền mục này. -->

- **Quyết định:** Duyệt
- **Người duyệt:** Nhật, **duyệt chéo:** Tuấn
- **Ghi chú:** 

## 10. Nhật ký thực hiện

<!-- AI ghi khi task đã có kết quả cuối (đã merge hoặc đã dừng), trên nhánh báo cáo của phiên (auto-run.md, bước 10). Mục mới thêm vào cuối, không sửa mục cũ. -->

### 2026-10-23: đã làm, chờ merge (PR #52)

- Phiên có người theo dõi, nhánh `claude/T-031-camerax-listenablefuture`.
- Bước 3, commit 9b00c1e: commit gồm catalog, `build.gradle.kts`, khung rỗng `applyZoom` và `CameraZoomTest`. Hai test thất bại đúng lý do:
  - `setZoomRatio(2.0)` không được gọi;
  - có 0 dòng log thay vì 1, nhờ bước đối chứng L-035.
- Bước 4, commit ae01f91: thân `applyZoom`; `CameraXController.setZoom` gọi `applyZoom`, bỏ reflection và import thừa. `consumer-rules.pro` chỉ còn một dòng chú thích.
- Bước 5: `scripts/check.sh` QUA.
  - Test JVM: 743 test, 0 lỗi (`:feature:camera`: 222); 65 ảnh screenshot; script test qua hết.
  - Lần chạy đầu thất bại ở 6 script test của `release.sh` với "không tìm thấy gh". Nguyên nhân là môi trường: `gh` không có trong PATH của lệnh. Thêm `/opt/homebrew/bin` vào PATH rồi chạy lại thì qua, không đổi code.
- Manifest gộp `stagingDebug`: `uses-permission` giống hệt trước và sau (12 dòng).
- `:app` `stagingDebugRuntimeClasspath`: chỉ có `guava:31.1-android`, thêm một cạnh trực tiếp từ `:feature:camera`. Không cần phương án dự phòng ở mục 3.4.
- Còn lại:
  - người merge bằng `scripts/merge-if-green.sh 52`;
  - kiểm tay zoom trên bản staging minify (mục 4);
  - chuyển ticket sang `on-staging` sau khi merge.
