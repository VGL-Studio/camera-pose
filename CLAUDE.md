# HƯỚNG DẪN AI LÀM VIỆC VỚI DỰ ÁN

Đây là app Camera Pose: Lines and Guides, tạo từ dự án mẫu của nhóm. Quy trình làm việc với tài liệu (spec, ticket, plan, phiên tự chạy) nằm ở [docs/CLAUDE.md](docs/CLAUDE.md); đọc file đó trước khi làm bất kỳ task nào.

## Công nghệ

- Kotlin, Jetpack Compose, một Activity, Navigation Compose với đích điều hướng kiểu `@Serializable`.
- ViewModel với `StateFlow`, luồng dữ liệu một chiều. Hilt cho tiêm phụ thuộc. DataStore cho lưu trữ cục bộ.
- minSdk 26, compileSdk và targetSdk 36. Phiên bản thư viện nằm ở `gradle/libs.versions.toml`.

## Cấu trúc module

| Module | Vai trò |
| --- | --- |
| `:app` | Vỏ ứng dụng, điều hướng, ghép các feature, Crashlytics. Hai biến thể `staging` và `production` (ADR-0007, ADR-0012) |
| `:core:designsystem` | Theme, màu, chữ, khoảng cách, thành phần giao diện dùng chung. Bản gốc của hệ thống thiết kế (ADR-0008) |
| `:core:data` | DataStore dùng chung |
| `:core:config` | Cờ tính năng và tham số kinh doanh (`RemoteSettings`), giá trị từ Firebase Remote Config (ADR-0003, ADR-0012) |
| `:core:analytics` | Ghi sự kiện phân tích vào Firebase Analytics (ADR-0012) |
| `:core:ads` | Vị trí quảng cáo, hộp thoại đồng ý, rào chắn chính sách, AdMob (ADR-0004, ADR-0013, ADR-0014) |
| `:core:assets` | Tải file tĩnh từ R2, giữ trên máy để dùng khi mất mạng (ADR-0006, ADR-0015) |
| `:core:logging` | Ghi log hành vi của app vào logcat và Crashlytics để phân tích khi có lỗi (ADR-0019) |
| `:core:poses` | Mô hình tư thế, tải catalog và ảnh tư thế từ R2 (chờ tối đa 15 giây), danh sách đã lưu và đã dùng gần đây, thứ tự hiển thị. Chỉ có ở app này: feature dùng bằng một dòng `implementation(project(":core:poses"))` trong `build.gradle.kts` của feature (T-009) |
| `:feature:<ten>` | Mỗi feature một module |
| `build-logic` | Convention plugin dùng chung cho mọi module |

Quy tắc phụ thuộc (ADR-0001, kiểm tra tự động bằng `checkModuleDependencies` cho mọi module): feature chỉ phụ thuộc core, không bao giờ phụ thuộc feature khác; core chỉ phụ thuộc core; không module nào phụ thuộc `:app`. Cần dùng chung giữa các feature thì đề xuất đưa vào một module core.

## Lệnh

- Kiểm tra toàn bộ, giống hệt CI: `scripts/check.sh`.
- Ghi lại ảnh chuẩn screenshot khi plan cho phép: `./gradlew recordRoborazziDebug`.
- Xem log của app trên máy thật hoặc emulator: `adb logcat -v threadtime | grep ' VGL\.'` (ADR-0019).
- Khảo sát app mẫu cho app clone (điều khiển máy có chặn quảng cáo và nút mua): `scripts/app-explore/ax.sh check`, rồi làm theo `docs/ops/clone-study.md` (ADR-0021).
- Tìm mã quảng cáo thật, khóa hay token lọt vào repo (test của script cũng chạy nó): `scripts/secret-scan.py`.
- Ghi một phiên thử trên máy để AI phân tích: `scripts/device-session.sh start <tên>`, thử app, `scripts/device-session.sh stop` (`docs/ops/test-sessions.md`).
- Sinh lại trang quy tắc nền tảng `docs/foundation.md` sau khi thêm hay đổi trạng thái ADR: `scripts/foundation.py` (`docs/CLAUDE.md`, mục Khi viết ADR).
- Cập nhật trang tiến độ `docs/STATUS.md` sau khi đổi trạng thái tài liệu: `scripts/status.py` (`docs/CLAUDE.md`, mục Trang tiến độ).
- Kiểm tra cả danh sách chạy trước khi duyệt (spec đã chốt, phụ thuộc, kịch bản có test, plan cùng sửa một file): `scripts/run-check.py docs/runs/RUN-YYYYMMDD-NN.md` (`docs/ops/pre-run-review.md`).
- Chờ một workflow GitHub Actions xong trong một lệnh: `scripts/wait-run.sh --pr <số>` hoặc `--workflow <file.yml> --since <giờ UTC>` (`docs/auto-run.md`, bước 8).
- Đếm token AI theo task của một phiên: `scripts/token-usage.py report --since <giờ bắt đầu>`; số gợi ý cho `token_estimate` của plan: `scripts/token-usage.py estimate` (`docs/ops/token-usage.md`).
- Môi trường cloud chưa có `ANDROID_HOME` thì đặt `export ANDROID_HOME=$HOME/android-sdk` (SDK do `scripts/cloud-setup.sh` cài).
- `app/src/staging/google-services.json` là file cấu hình Firebase giả, chỉ để build qua được; bản build từ file này không gửi số liệu nào (ADR-0012). Không sửa, không thay bằng file thật.
- Môi trường cloud không có emulator. Test trên emulator (`app/src/androidTest`) chỉ chạy trong CI.

## Viết một feature mới

Làm theo đúng mẫu `:feature:sample`:

1. `build.gradle.kts` chỉ áp dụng plugin `template.android.feature`, khai báo `namespace`.
2. `data/`: interface repository và lớp cài đặt `internal`, gắn bằng một Hilt module trong `di/`.
3. `<Ten>UiState`: sealed interface mô tả mọi trạng thái màn hình.
4. `<Ten>ViewModel`: nhận repository, `Analytics`, `AppLogger`, `FeatureFlags`; trả `StateFlow<UiState>`; hành động của người dùng là hàm thường.
   - Ghi log bằng `AppLogger` (ADR-0019): đổi trạng thái chính, kết quả hành động của người dùng, lỗi đã bắt, lần thử lại. Thẻ log là tên feature. Không ghi dữ liệu cá nhân, nội dung file hay chữ người dùng nhập. Unit test dùng `RecordingAppLogger`.
   - `FeatureFlags.enabled()` là một `Flow`: theo dõi nó, không đọc một lần (ADR-0003).
   - Ghi dữ liệu có thể ném `IOException`: bắt lỗi trong ViewModel, đưa vào `UiState` để màn hình báo cho người dùng, không để app dừng đột ngột. Luồng đọc DataStore tự thử lại khi gặp `IOException`.
5. `<Ten>Screen.kt`: một composable `<Ten>Route` nối với ViewModel, một composable `<Ten>Screen` không trạng thái nhận `uiState` và các callback.
6. `navigation/`: một đích `@Serializable` và hàm mở rộng `NavGraphBuilder.<ten>Screen()`. Đăng ký trong `AppNavHost` của `:app`. Điều hướng đứng sau một lời gọi `suspend` (đọc DataStore, tải file) chạy trên luồng chính: `scope.launch(Dispatchers.Main.immediate) { ... }`, vì coroutine có thể được tiếp tục trên luồng nền và `navigate` khi đó dừng app (L-030).
7. Quảng cáo chỉ qua `AdSlot(placement)` (banner) và `rememberShowInterstitial(placement)` (toàn màn hình, chỉ gọi ở điểm dừng tự nhiên) của `:core:ads`, với vị trí đã có trong `AdPlacement` theo spec đã duyệt (ADR-0013). Vị trí mặc định tắt, chỉ hiện quảng cáo khi khóa `ads_<vị trí>_enabled` được bật trên Remote Config (ADR-0014).
8. File tĩnh trên R2 chỉ qua `RemoteAssets.load(AssetPath(...))` của `:core:assets`, gọi từ ViewModel; kết quả `Unavailable` đưa vào `UiState` để người dùng thử lại. Đường dẫn tương đối, có phiên bản hoặc mã băm; nội dung đổi thì dùng đường dẫn mới (ADR-0015).
9. Cờ tính năng mới khai báo trong `FeatureFlag` của `:core:config`, luôn mặc định tắt. Muốn thấy feature trên bản staging thì thêm cờ vào `StagingFeatureFlagsModule` trong `app/src/staging`.
10. Test, viết trước code (docs/auto-run.md, bước 3):
   - Unit test cho ViewModel, dùng repository giả và `MainDispatcherRule`. Khi kiểm tra lỗi, so loại lỗi (`assertTrue(error is IOException)`), không so đúng đối tượng lỗi: coroutine có thể chép lại ngoại lệ khi khôi phục stack trace.
   - Test giao diện cho `<Ten>Screen` chạy bằng Robolectric. Test có kiểm tra kích thước dùng `@GraphicsMode(GraphicsMode.Mode.NATIVE)`, vì chế độ đồ họa mặc định đo sai cỡ chữ; thành phần Material tự nới vùng chạm hay vùng ngữ nghĩa (ví dụ `LinearProgressIndicator`) thì đo qua phần tử bọc ngoài mang `testTag`. Test kiểm màu điểm ảnh vẽ `decorView` của activity (`(composeRule as AndroidComposeTestRule<*, *>).activity.window.decorView`) vào một `Bitmap` bằng `draw(Canvas(bitmap))`, rồi đọc `getPixel` tại `boundsInWindow` của node cộng độ lệch đổi từ dp, điểm đo ở giữa vùng màu, xa mép và góc bo. Không dùng `captureToImage()` (hết giờ chờ trên Robolectric `NATIVE`) hay `captureRoboImage` ghi ra chỗ khác `src/test/screenshots/` (`scripts/check.sh` từ chối); vẽ cả cửa sổ còn đo được điểm ngoài node, ví dụ bóng đổ (L-006).
   - Screenshot test bằng Roborazzi. Mỗi lệnh `captureRoboImage` ghi rõ đường dẫn, dạng `"src/test/screenshots/<Ten>.png"`; `scripts/check.sh` báo lỗi nếu thiếu. Khung `@Config(qualifiers = "w411dp-h891dp-xxhdpi")`, là khung chuẩn của thiết kế. Mỗi màn hình có thêm ít nhất một ảnh ở khung thấp `w360dp-h640dp-xxhdpi`, ở trạng thái nhiều nội dung nhất, tên `<Ten>Screen_<trạng thái>_small.png`: nội dung dài hơn màn hình thì cuộn được, nút chính không bị che hay cắt (L-028). App hỗ trợ điện thoại màn hình gập: mỗi ảnh thiết kế `_unfolded` có screenshot test cùng tên ở khung `w673dp-h841dp-xxhdpi` (ADR-0020).
   - Trạng thái có hiệu ứng chạy mãi (vòng chờ `CircularProgressIndicator`, nút ở trạng thái `loading`, khung xương nhấp nháy) không chụp bằng `captureRoboImage(path) { ... }`: màn hình không bao giờ rảnh, Robolectric vẽ khung hình tới khi máy test hết bộ nhớ rồi mới chụp, nên test chạy khoảng 4 phút và ảnh đổi theo cỡ bộ nhớ, chập chờn trên CI (L-029). Dừng đồng hồ và chụp ở một thời điểm cố định: `runComposeUiTest { mainClock.autoAdvance = false; setContent { ... }; mainClock.advanceTimeBy(<số ms cố định>); onRoot().captureRoboImage(path) }`, với `@OptIn(ExperimentalTestApi::class)`. Cũng được: một lớp test riêng có `createComposeRule()`, như `CameraScreenStartingScreenshotTest`. Không thêm `createComposeRule()` vào lớp còn test `captureRoboImage(path) { ... }` khác: rule mở thêm một cửa sổ, Roborazzi chuyển sang chụp cả màn hình và mọi ảnh khác lệch.
   - Nút chữ (`TextButton`) đặt trên nền khác màu (`Snackbar`, thẻ màu, hộp thoại) tự đặt màu chữ `primary`, đè màu mà thành phần cha truyền xuống: truyền `colors` rõ cho nút, và có test đo màu chữ của nút trên ảnh vẽ ra (ví dụ vẽ view gốc ra `Bitmap` rồi đọc điểm ảnh), vì ảnh chuẩn ghi lại cả màu sai (L-032).
   - Feature có thiết kế (`docs/features/<feature>/design/screens/`): mỗi ảnh thiết kế có một screenshot test cùng tên; số liệu lấy từ `design/context/`, không đoán từ ảnh; chữ hiển thị theo bảng chữ của thiết kế; chạy `scripts/design-compare.sh <feature>` (ADR-0020, `docs/ops/design-workflow.md`).
   - Test trên emulator (`app/src/androidTest`) chỉ kiểm tra trạng thái ổn định: màn hình đích sau khi chờ có giới hạn (`waitUntil`). Không khẳng định một màn hình hay trạng thái chỉ hiện thoáng qua (loading, hiệu ứng, thông báo tự ẩn); phần đó kiểm tra bằng test giao diện trên JVM, nơi điều khiển được thời gian. Bản staging mới cài hiện hộp thoại đồng ý quảng cáo phủ lên app (ADR-0013), nên kiểm tra màn hình có trong cây giao diện (`onAllNodesWithTag(...).fetchSemanticsNodes()`), không dùng `assertIsDisplayed`.

## Quy ước code

- Tên lớp, hàm, biến và chú thích trong code viết bằng tiếng Anh, kể cả chú thích trong script và workflow. Thông báo mà script in ra cho nhóm và tài liệu trong `docs/` viết bằng tiếng Việt.
- Chữ hiển thị cho người dùng đặt trong `res/values/strings.xml`, không viết cứng trong code.
- Màu, chữ, khoảng cách lấy từ `:core:designsystem` (`AppTheme`, `MaterialTheme`, `Spacing`). Không viết cứng mã màu hay cỡ chữ.
- Feature không gọi trực tiếp SDK quảng cáo, phân tích, Remote Config, tải file từ R2 hay `android.util.Log`; luôn đi qua `:core:ads`, `:core:analytics`, `:core:config`, `:core:assets`, `:core:logging`.
- Định dạng code theo ktlint (kiểu Android Studio, xem `.editorconfig`).
- Commit: chỉ `git add` theo đường dẫn các file đã sửa, không dùng `git add -A` hay `git add .`; xem `git status --short` hoặc danh sách file của PR trước khi đẩy.

## Không được làm

- Không sửa `.github/`, `scripts/check.sh`, `scripts/merge-if-green.sh`, `build-logic/` hay `gradle/libs.versions.toml`, trừ khi đó chính là nội dung của một plan rủi ro cao đã được duyệt chéo. Thay đổi có ích cho mọi app ở các phần dùng chung (quy trình, CI, script, cấu hình build, module core) phải được đề xuất về dự án mẫu (`docs/ops/from-app.md`); ghi điều này vào mô tả PR.
- Không thêm quyền truy cập vào `AndroidManifest.xml`, không thêm SDK, vị trí quảng cáo hay sự kiện phân tích ngoài spec đã duyệt.
- Không merge bằng cách nào khác ngoài `scripts/merge-if-green.sh`. Không đóng, sửa hay bình luận để gỡ issue mang nhãn `main-broken`.
- Không đọc, in ra hay chuyển đi bí mật của repo. Không tạo hay sửa cấu hình production (ADR-0007, ADR-0009).
- Không chạm vào `scripts/release.sh`, không đọc hay tạo thư mục phát hành production (ADR-0009, ADR-0017).
- Không làm cho test qua bằng cách sửa test, xóa test, cập nhật ảnh chuẩn screenshot ngoài plan, hay tắt quy tắc lint.
