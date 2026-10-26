---
ticket: T-056
title: Màn lớn theo hướng của máy, điện thoại khóa dọc
status: draft                # draft | approved | stopped | done
version: 1                   # tăng khi plan được sửa lại sau phản hồi hoặc sau khi dừng
risk_score: 11               # tổng điểm theo risk-scoring.md, để trống nếu chạm điểm chặn
risk_level: high             # low | medium | high
token_estimate: 16M          # tổng token dự kiến, ví dụ 20M; số gợi ý: scripts/token-usage.py estimate (docs/ops/token-usage.md)
blocking_points: []          # các điểm chặn bị chạm, ví dụ [ads, sdk]
run_order:                   # AI điền sau họp đánh giá rủi ro
reviewer:                    # người duyệt
cross_reviewer:              # bắt buộc khi risk_level là high
approved_at:
---

# PLAN T-056: MÀN LỚN THEO HƯỚNG CỦA MÁY, ĐIỆN THOẠI KHÓA DỌC

## 1. Tóm tắt

Manifest giữ `android:screenOrientation="portrait"`, nên điện thoại vẫn mở thẳng ở chiều dọc, không dựng lại activity. Một hàm mới của vỏ app, `Activity.applyScreenOrientation()`, gọi trong `MainActivity.onCreate`: khi cạnh ngắn của màn từ 600 dp thì đặt `requestedOrientation = SCREEN_ORIENTATION_UNSPECIFIED` (app theo hướng của máy, như Android 16 vốn làm trên màn lớn), dưới 600 dp thì đặt lại `SCREEN_ORIENTATION_PORTRAIT`. Cạnh ngắn đọc từ ba nguồn (cấu hình của activity, cấu hình của hệ thống, `maximumWindowMetrics` từ Android 11), vì app đang bị đóng khung chỉ thấy cửa sổ nhỏ của mình. Kiểm chứng bằng test Robolectric cho cách chọn và cho activity thật ở bốn khung, test chặn bố cục khi màn lớn nằm ngang, và một kịch bản emulator mới.

`token_estimate` 16M thay cho số gợi ý 7.8M (trung vị cỡ M của `scripts/token-usage.py estimate`): từ RUN-20261026-01, token của phiên con (rà soát độc lập) tính vào task (L-073), và hai task cỡ M của phiên đó tốn 15.0M và 19.7M.

## 2. Hiểu yêu cầu

Ticket sửa FB-20261024-01 và xử lý KB-11 trong `features/camera/spec.md`, theo phần đề xuất đổi spec trong PR này (mục 2, KB-11 hai tiêu chí mới, câu 7 phần bổ sung, câu 15). Spec camera đang `changed`: plan chỉ được duyệt sau khi người phụ trách sản phẩm xác nhận lại (mục 8, câu 2).

| Kịch bản | Tiêu chí chấp nhận | Cách hiểu của AI |
| --- | --- | --- |
| KB-11 (mới) | Khi app mở trên màn lớn (cạnh ngắn từ 600 dp) thì app dùng cả màn hình, không bị hệ thống đóng khung với hai dải đen, kể cả khi màn nằm ngang; xoay máy thì app xoay theo, mọi màn vẫn đủ nút chính trong màn hình | "Màn lớn" là cạnh ngắn (`smallestScreenWidthDp`) từ 600 dp, cùng ngưỡng 600 dp mà camera, xem lại và thư viện dùng để chọn bố cục mở gập (theo chiều rộng). App bị đóng khung khi khóa dọc trên màn nằm ngang (Android 12L đến 15); bỏ khóa trên màn lớn thì hết đóng khung. Đóng khung là việc của hệ thống, JVM không mô phỏng được: kiểm bằng kịch bản emulator và máy thật. "Đủ nút chính trong màn hình" kiểm bằng test chặn bố cục ở khung nằm ngang (mục 4) |
| KB-11 (mới) | Khi gập lại (cạnh ngắn dưới 600 dp) thì app trở về chiều dọc dù máy đang nằm ngang | Gập lại làm activity dựng lại (manifest không khai báo `configChanges`), `onCreate` gọi lại hàm và đặt lại `PORTRAIT`. Yêu cầu hướng của activity được hệ thống giữ qua lần dựng lại, nên phải đặt lại rõ ràng, không dựa vào manifest |
| KB-11 (cũ) | Khi gập lại thì camera và màn xem lại dùng bố cục điện thoại, giữ tư thế và các lựa chọn | Không đổi: mọi trạng thái nằm trong ViewModel (T-020). Xoay máy trên màn lớn cũng là một lần dựng lại như gập mở; camera đóng ở `onStop` và mở lại ở `onStart` (`CameraScreen`, `LifecycleStartEffect`), nên `Preview` dựng lại theo hướng mới của màn. Đếm ngược đang chạy thì bị hủy khi activity dừng, như khi ra nền (KB-8) |
| poses KB-7 | Khi máy gập mở ra thì thư viện hiện 3 cột | Đã đúng trên khung `w673dp-h841dp` (T-024); phản hồi này là trường hợp app bị đóng khung nên chiều rộng còn dưới 600 dp. Bỏ khóa trên màn lớn thì thư viện có đủ chiều rộng. Test chặn ở khung nằm ngang `w841dp-h673dp` |

Thiết kế tham chiếu: không có thiết kế riêng cho màn lớn nằm ngang; dùng lại bố cục mở gập của `features/camera/design/README.md` và `features/poses/design/README.md` (mục 8, câu 1). Không thêm ảnh chuẩn mới.

ADR liên quan: ADR-0001 (vỏ app chỉ ghép feature; hàm mới nằm trong `:app`), ADR-0020 (khung `_unfolded` 673 x 841).

## 3. Giải pháp

### 3.1. Cách làm

**Hàm chọn hướng** (`app/src/main/kotlin/com/vglstudio/camera/pose/line/guide/ui/ScreenOrientation.kt`, cùng gói với `UiAutomatorTags.kt`):

```kotlin
/** From this smallest width the screen is large (an unfolded foldable, a tablet): the Material "compact" limit. */
internal const val LARGE_SCREEN_MIN_SMALLEST_WIDTH_DP = 600

/**
 * Portrait on a phone; on a large screen the app follows the device (spec camera, KB-11; plan T-056). The screen is
 * large when any of [smallestWidthsDp] is: a portrait app letterboxed on a landscape inner screen sees its own small
 * window, while the display still reports its real size.
 */
internal fun screenOrientationFor(smallestWidthsDp: List<Int>): Int {
    val large = smallestWidthsDp.any {
        it != Configuration.SMALLEST_SCREEN_WIDTH_DP_UNDEFINED && it >= LARGE_SCREEN_MIN_SMALLEST_WIDTH_DP
    }
    return if (large) ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED else ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
}

/**
 * Applies [screenOrientationFor] to this activity. The manifest keeps portrait, so a phone starts without a relaunch;
 * only a large screen changes it, once. The request is kept across a relaunch, so it is set only when it differs.
 */
fun Activity.applyScreenOrientation() {
    val widths = mutableListOf(
        resources.configuration.smallestScreenWidthDp,
        Resources.getSystem().configuration.smallestScreenWidthDp,
    )
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        val bounds = windowManager.maximumWindowMetrics.bounds
        val density = resources.displayMetrics.density
        if (bounds.width() > 0 && bounds.height() > 0) {
            widths += (minOf(bounds.width(), bounds.height()) / density).toInt()
        }
    }
    val orientation = screenOrientationFor(widths)
    if (requestedOrientation != orientation) requestedOrientation = orientation
}
```

- Vì sao ba nguồn: app khóa dọc bị đóng khung trên màn nằm ngang thì cấu hình của activity có thể chỉ là cửa sổ đóng khung (ví dụ khoảng 486 dp trong FB-20261024-01), dưới 600 dp. Cấu hình của hệ thống (`Resources.getSystem()`) và `maximumWindowMetrics` cho kích thước của màn. Có một nguồn từ 600 dp thì là màn lớn. Điện thoại có cả ba nguồn dưới 600 dp nên không bao giờ bị mở khóa nhầm.
- Vì sao giữ khóa dọc trong manifest: điện thoại (gần hết người dùng) khởi động thẳng ở chiều dọc, không dựng lại activity lúc mở app. Chỉ màn lớn đang nằm ngang dựng lại một lần, lúc đổi từ `PORTRAIT` sang `UNSPECIFIED`. Hướng ngược lại (bỏ khóa trong manifest, khóa dọc trong `onCreate` trên điện thoại) làm điện thoại cầm ngang lúc mở app dựng lại activity: phương án dự phòng ở mục 3.4.
- Chỉ đặt khi khác giá trị đang có: yêu cầu hướng được hệ thống giữ qua lần dựng lại, nên lần `onCreate` sau trên cùng màn không đặt lại và không lặp.

**`MainActivity.onCreate`**: gọi `applyScreenOrientation()` ngay sau `super.onCreate(savedInstanceState)`, trước `enableEdgeToEdge()`, kèm một dòng chú thích (FB-20261024-01, plan T-056). Không đổi gì khác.

**`CameraXController`**: chỉ sửa chú thích của `orientationListener` ("The app is locked to portrait, so ..."), vì từ task này app không còn khóa dọc trên màn lớn. Cách tính `targetRotation` theo cảm biến (góc so với hướng tự nhiên của máy) đúng cho mọi hướng của màn, nên không đổi code: ảnh chụp đứng thẳng theo cách cầm máy.

**Kịch bản emulator** `app/src/deviceScenarios/T-056-man-lon-xoay-ngang.txt`, nội dung đúng như khối dưới (đã thử `scripts/device-scenario.py check`, qua): vào thư viện; `device rotate landscape` trên điện thoại (app vẫn dọc, ảnh `phone-landscape`); `device unfold` khi vẫn xoay ngang (app theo màn nằm ngang, ảnh `unfolded-landscape`); `device fold` (app trở về dọc, ảnh `folded-landscape`). Emulator của `device-session.yml` là cấu hình điện thoại nên không đóng khung app (hệ thống xoay màn theo yêu cầu dọc của app); kịch bản vì thế kiểm hướng mà app yêu cầu, thấy qua hướng của ảnh: trước task, cả ba ảnh đều dọc; sau task, `unfolded-landscape` nằm ngang. Đóng khung thật kiểm trên máy thật (mục 4).

```text
ticket: T-056
criterion: Màn lớn theo hướng của máy, điện thoại khóa dọc (plan T-056, mục 4, kiểm tra thủ công 1, 2), bằng wm size thay cho gập thật
timeout: 150
fresh
consent decline 30
wait id=onboarding_screen | id=camera_screen 60
tap_if id=onboarding_enable_camera
need id=camera_poses 15
tap id=camera_poses
wait id=poses_grid 20
# (1) điện thoại xoay ngang: app vẫn dọc (ảnh phone-landscape cao hơn rộng)
device rotate landscape
wait id=poses_grid 10
shot phone-landscape
# (2) mở gập rồi xoay ngang: app theo màn hình, không khóa dọc (ảnh unfolded-landscape rộng hơn cao, 3 cột, không viền đen)
device unfold
wait id=poses_grid 10
shot unfolded-landscape
# (3) gập lại khi vẫn xoay ngang: app trở về dọc, giữ màn thư viện
device fold
wait id=poses_grid 10
shot folded-landscape
```

### 3.2. Danh sách file

| Thao tác | Đường dẫn | Nội dung thay đổi |
| --- | --- | --- |
| Tạo | `app/src/main/kotlin/com/vglstudio/camera/pose/line/guide/ui/ScreenOrientation.kt` | `screenOrientationFor`, `Activity.applyScreenOrientation()` như mục 3.1 (bước 3: khung rỗng trả `PORTRAIT` và không làm gì) |
| Sửa | `app/src/main/kotlin/com/vglstudio/camera/pose/line/guide/MainActivity.kt` | Gọi `applyScreenOrientation()` sau `super.onCreate` |
| Sửa | `feature/camera/src/main/kotlin/com/vglstudio/camera/pose/line/guide/feature/camera/camera/CameraXController.kt` | Chỉ sửa chú thích của `orientationListener` |
| Tạo | `app/src/test/kotlin/com/vglstudio/camera/pose/line/guide/ui/ScreenOrientationTest.kt` | 8 test của mục 4 |
| Sửa | `feature/camera/src/test/kotlin/com/vglstudio/camera/pose/line/guide/feature/camera/CameraScreenUnfoldedTest.kt` | Test chặn `landscape_controlsInsideWindow` ở hai khung nằm ngang |
| Sửa | `feature/camera/src/test/kotlin/com/vglstudio/camera/pose/line/guide/feature/camera/review/ReviewScreenUnfoldedTest.kt` | Test chặn `landscape_saveInsideWindow` ở hai khung nằm ngang |
| Sửa | `feature/poses/src/test/kotlin/com/vglstudio/camera/pose/line/guide/feature/poses/PosesScreenTest.kt` | Test chặn `landscape_hasThreeColumns` |
| Sửa | `feature/poses/src/test/kotlin/com/vglstudio/camera/pose/line/guide/feature/poses/detail/PoseDetailScreenTest.kt` | Test chặn `landscape_bottomBarVisible` |
| Tạo | `app/src/deviceScenarios/T-056-man-lon-xoay-ngang.txt` | Kịch bản emulator, đúng khối ở mục 3.1; chạy `scripts/device-scenario.py check` |

`AndroidManifest.xml` không đổi (giữ `android:screenOrientation="portrait"`). `scripts/tests/run.sh` (dòng 1111) đếm `exposeTestTagsForUiAutomator()` trong `MainActivity.kt`: task thêm một dòng `import ...applyScreenOrientation` và một lời gọi `applyScreenOrientation()`, không thêm lời gọi `exposeTestTagsForUiAutomator()`, nên số đếm vẫn là 1; không ảnh hưởng (L-020). Không file nào khác đọc hướng của activity (`grep -rn 'screenOrientation\|requestedOrientation' --include='*.kt' --include='*.xml' app feature core` chỉ thấy manifest).

### 3.3. Cờ tính năng, quảng cáo, dữ liệu, quyền, thư viện

- Cờ tính năng: Không có. Hành vi của vỏ app, không bật tắt theo Remote Config.
- Quảng cáo: Không chạm. Hộp thoại đồng ý (`ads.gatherConsent(this)` trong `onCreate`) đã chạy lại ở mỗi lần dựng lại activity (đổi chế độ tối, xoay trên Android 16), task không đổi điều đó.
- Sự kiện phân tích: Không chạm.
- File R2: Không chạm.
- **Quyền truy cập:** Không thêm. `setRequestedOrientation`, `Resources.getSystem()`, `WindowManager.getMaximumWindowMetrics()` không cần quyền.
- **Thư viện và SDK:** Không thêm. Chỉ dùng API của Android (`android.app.Activity`, `android.content.res`, `android.view.WindowManager` từ API 30); không dùng `androidx.window`.

### 3.4. Phương án dự phòng

- **Điểm chưa chắc:** trên máy thật, khi app khóa dọc bị đóng khung, cả ba nguồn có thể đều chỉ thấy cửa sổ đóng khung (hệ thống "sandbox" các API kích thước cho app ở chế độ tương thích). Khi đó app vẫn khóa dọc. JVM không kiểm được; chỉ lộ ra ở máy thật (mục 4, kiểm tra thủ công 2), sau phiên.
- **Điều kiện chuyển:** không chuyển trong phiên, vì phiên không có máy thật hay emulator. Trong phiên chỉ chuyển khi một test của mục 4 không qua được sau 2 lần sửa vì cách đọc kích thước (ví dụ Robolectric không trả cấu hình hệ thống theo `qualifiers`).
- **Cách làm thay:** bỏ `android:screenOrientation` khỏi `AndroidManifest.xml` (dự phòng) và để `applyScreenOrientation()` chỉ đặt `PORTRAIT` khi cả ba nguồn dưới 600 dp. App không khóa lúc khởi động nên không bị đóng khung, kích thước đọc được luôn là kích thước thật; đổi lại, điện thoại cầm ngang lúc mở app dựng lại activity một lần. Test `manifest_keepsPortrait_soPhonesStartWithoutARelaunch` khi đó đổi kỳ vọng thành `SCREEN_ORIENTATION_UNSPECIFIED` và ghi vào "Lệch so với plan"; các test khác giữ nguyên. Nếu máy thật cho thấy phương án chính không chạy (sau phiên), phương án này thành plan phiên bản 2.

| Thao tác (dự phòng) | Đường dẫn | Nội dung thay đổi |
| --- | --- | --- |
| Sửa | `app/src/main/AndroidManifest.xml` | (dự phòng) Bỏ `android:screenOrientation="portrait"` của `MainActivity` |

## 4. Kiểm thử

Test của `ScreenOrientationTest` chạy với `@Config(application = Application::class)` (như `UiAutomatorTagsTest`, Hilt và Firebase không chạy) và `Robolectric.buildActivity(ComponentActivity::class.java).setup().get()`; `MainActivity` cần Hilt nên test dùng activity thường, còn chỗ gọi trong `MainActivity` là một dòng (ngoại lệ bên dưới).

| Tiêu chí chấp nhận | Loại test | Tên test hoặc cách kiểm tra |
| --- | --- | --- |
| Điện thoại khóa dọc | Unit test (Robolectric) | `ScreenOrientationTest.phone_isPortrait`: `screenOrientationFor(listOf(411, 411)) == SCREEN_ORIENTATION_PORTRAIT`. Qua ngay trên khung rỗng (đối chứng, cặp với `foldedAgain_locksToPortrait`) |
| Ngưỡng 600 dp | Unit test | `width599_isPortrait_width600_followsTheDevice`: `listOf(599)` cho `PORTRAIT`, `listOf(600)` cho `SCREEN_ORIENTATION_UNSPECIFIED` |
| Màn lớn đang bị đóng khung vẫn được mở khóa | Unit test | `letterboxedWindowOnALargeDisplay_followsTheDevice`: `listOf(486, 673)` cho `UNSPECIFIED` |
| Giá trị không xác định bị bỏ qua | Unit test | `undefinedWidth_isIgnored`: `listOf(SMALLEST_SCREEN_WIDTH_DP_UNDEFINED, 411)` cho `PORTRAIT`, `listOf(SMALLEST_SCREEN_WIDTH_DP_UNDEFINED, 673)` cho `UNSPECIFIED` |
| KB-11 mới, tiêu chí 1: mở gập thì mở khóa | Unit test (activity thật) | `@Config(qualifiers = "w673dp-h841dp-xxhdpi") unfolded_unlocksThePortraitOfTheManifest`: đặt `requestedOrientation = PORTRAIT` (như manifest), gọi `applyScreenOrientation()`, mong `UNSPECIFIED` |
| KB-11 mới, tiêu chí 1: màn lớn nằm ngang | Unit test (activity thật) | `@Config(qualifiers = "w841dp-h673dp-land-xxhdpi") unfoldedLandscape_unlocks`: như trên |
| KB-11 mới, tiêu chí 2: gập lại thì khóa dọc | Unit test (activity thật) | `@Config(qualifiers = "w411dp-h891dp-xxhdpi") foldedAgain_locksToPortrait`: đặt `requestedOrientation = UNSPECIFIED` (như sau khi mở gập), gọi hàm, mong `PORTRAIT`. Đối chứng: khung rỗng không làm gì nên test thất bại |
| Điện thoại khởi động không dựng lại | Unit test (test chặn) | `manifest_keepsPortrait_soPhonesStartWithoutARelaunch`: `packageManager.getActivityInfo(ComponentName(activity, MainActivity::class.java), 0).screenOrientation == SCREEN_ORIENTATION_PORTRAIT`. Qua ngay (test chặn: manifest không được đổi) |
| KB-11 mới, tiêu chí 1: camera đủ nút khi màn lớn nằm ngang | Compose UI test (test chặn) | `CameraScreenUnfoldedTest.landscape_controlsInsideWindow`, hai lần với `@Config(qualifiers = "vi-w841dp-h673dp-land-xxhdpi")` và `"vi-w960dp-h600dp-land-xxhdpi"` (hai hàm test): mọi phần `PREVIEW`, `FLIP`, `SHUTTER`, `LAST_PHOTO`, `ZOOM`, `CHIPS`, `STRIP`, `POSES`, `SETTINGS` có `bottom <= root.bottom` và `right <= root.right` (`getUnclippedBoundsInRoot`); khung xem trước giữ 3:4 (`height / width` trong 1.333 ± 0.02). Qua ngay: số đo ở mục 4.1 |
| Màn xem lại đủ nút khi nằm ngang | Compose UI test (test chặn) | `ReviewScreenUnfoldedTest.landscape_saveInsideWindow`, hai khung như trên: `PHOTO`, `RETAKE`, `SHARE`, `NEXT_POSE`, `SAVE`, `CLOSE` nằm trong root; ảnh giữ 3:4 (± 0.02). Qua ngay |
| poses KB-7: thư viện 3 cột khi màn lớn nằm ngang | Compose UI test (test chặn) | `PosesScreenTest.landscape_hasThreeColumns`, `@Config(qualifiers = "vi-w841dp-h673dp-land-xxhdpi")`, `setScreen(ready(cards = manyCards()))`: `p01`, `p02`, `p03` cùng `top`, đi dần sang phải; `p04` dưới `p01`, cùng `left`. Qua ngay |
| Chi tiết tư thế: thanh nút thấy được khi nằm ngang | Compose UI test (test chặn) | `PoseDetailScreenTest.landscape_bottomBarVisible`, cùng khung, `setScreen(ready(detail = longDetail()), width = { 841.dp })`: `BOTTOM_BAR.bottom == root.bottom` (± 0.5), `BOTTOM_BAR.top >= IMAGE.top`, ảnh bên trái thanh nút (`IMAGE.right <= BOTTOM_BAR.left`). Qua ngay |
| KB-11 mới, cả hai tiêu chí: hướng app yêu cầu khi xoay, mở gập, gập lại | Kiểm tra thủ công (emulator: `app/src/deviceScenarios/T-056-man-lon-xoay-ngang.txt`) | `device-session` cuối phiên chạy tệp. Phiên mở ba ảnh: `phone-landscape` và `folded-landscape` cao hơn rộng (app dọc), `unfolded-landscape` rộng hơn cao, thư viện 3 cột, không có dải đen. Ảnh `unfolded-landscape` dọc là `lỗi` của task, soạn phản hồi |
| KB-11 mới, tiêu chí 1: hết đóng khung trên máy thật | Kiểm tra thủ công (máy thật: gập có bản lề) | Bản staging trên máy gập có màn trong nằm ngang (ví dụ Pixel Fold) hoặc máy tính bảng, Android 14 hay 15: mở app khi máy nằm ngang, app dùng cả màn hình; xoay máy, app xoay theo; gập lại, app dọc. Nếu vẫn đóng khung: báo lại, chuyển sang phương án mục 3.4 bằng plan phiên bản 2 |
| Camera khi màn lớn nằm ngang (spec camera, câu 15) | Kiểm tra thủ công (máy thật: camera thật) | Trên máy ở dòng trên: chụp một ảnh khi máy nằm ngang và một ảnh khi dọc; ảnh đứng thẳng theo cách cầm máy; ghi lại phần ảnh rộng hơn khung xem trước để người phụ trách sản phẩm quyết ticket `ViewPort` |

Mỗi dòng "Kiểm tra thủ công" ghi nơi chạy (L-062); `scripts/plan-check.py` báo lỗi dòng thiếu khi plan còn `draft` (quy tắc 11).

**Kết quả mong đợi ở bước 3** (khung rỗng `screenOrientationFor` trả `PORTRAIT`, `applyScreenOrientation()` không làm gì; đã chạy thật, mục 4.1): 6 test thất bại đúng lý do (`width599_...`, `letterboxed...`, `undefinedWidth_...`, `unfolded_...`, `unfoldedLandscape_...`: `expected:<-1> but was:<1>`; `foldedAgain_...`: `expected:<1> but was:<-1>`), 2 qua (`phone_isPortrait` đối chứng, `manifest_...` test chặn). 6 test chặn bố cục qua ngay (số đo mục 4.1). Bước 4 viết đúng mục 3.1 thì cả 8 test của `ScreenOrientationTest` qua.

**Ngoại lệ viết test trước:** (1) dòng gọi `applyScreenOrientation()` trong `MainActivity.onCreate`: `MainActivity` cần Hilt, không dựng được trong test JVM của `:app`; chỗ gọi kiểm bằng rà soát độc lập và kịch bản emulator. (2) Chú thích của `CameraXController`. (3) Tệp kịch bản: chạy ở `device-session` cuối phiên, không "thất bại trước" được. (4) 6 test chặn bố cục qua ngay: bố cục mở gập đã đúng khi nằm ngang; test giữ cho task sau không làm hỏng.

### 4.1. Chạy thử

- **Đã chạy thử (2026-10-26, nhánh `claude/plan-T-056-huong-man-lon` trên `origin/main` 3bf61da, không commit):**
  1. Số đo bố cục trên code hiện tại, test tạm Robolectric `NATIVE` (`getUnclippedBoundsInRoot`, dp, trái,trên..phải,dưới):
     - Camera `vi-w841dp-h673dp-land-xxhdpi`: root 841x673; `PREVIEW` 16,72..340,505 (324 x 433, tỷ lệ 1.336); `FLIP` 575,176..623,224; `SHUTTER` 561,250..637,326; `LAST_PHOTO` 575,374..623,422; `ZOOM` 531,453..667,501; `CHIPS` 0,505..841,557; `STRIP` 0,557..841,637; `POSES` 16,16..64,64; `SETTINGS` 292,16..340,64. Mọi phần trong root.
     - Camera `vi-w960dp-h600dp-land-xxhdpi`: `PREVIEW` 16,72..286,432 (270 x 360); `STRIP` 0,484..960,564; mọi phần trong root. Đối chiếu `vi-w1280dp-h800dp-land-xhdpi`: `STRIP` 0,684..1280,764. Khung `vi-w673dp-h841dp-xxhdpi` (đối chiếu với T-020): `PREVIEW` 16,72..466,673, `STRIP` 0,725..673,805.
     - Xem lại `w841dp-h673dp`: `PHOTO` 16,64..457,653 (441 x 589, tỷ lệ 1.336), `SAVE` 473,597..610,653; `w960dp-h600dp`: `PHOTO` 16,64..403,580, `SAVE` 419,524..556,580; mọi phần trong root.
     - Thư viện `w841dp-h673dp`: `p01` 16,160..275,543, `p02` 291,160..550,543, `p03` 566,160..825,543, `p04` 16,559..275,943: 3 cột, rộng 259.
     - Chi tiết `w841dp-h673dp` (`longDetail()`): root 0,0..841,673; `IMAGE` 16,80..413,520; `BOTTOM_BAR` 429,583..841,673.
     - Nhận xét: khi nằm ngang, khung xem trước nhỏ lại (324 x 433 thay vì 450 x 600) và giữa khung với cột nút có khoảng trống; đủ nút, không cắt. Chưa có thiết kế riêng (mục 8, câu 1).
  2. Giá trị Robolectric đọc được (test tạm, `Robolectric.buildActivity(ComponentActivity)`, `@Config(application = Application::class)`): ở `w673dp-h841dp-xxhdpi` và `w841dp-h673dp-land-xxhdpi`, `activity.resources.configuration.smallestScreenWidthDp`, cấu hình của application và `Resources.getSystem()` đều là 673; ở `w411dp-h891dp-xxhdpi` đều là 411. `windowManager.maximumWindowMetrics.bounds` là 0 x 0 trên Robolectric, nên hàm bỏ qua nguồn này khi rỗng. `requestedOrientation` mặc định -1, đặt `PORTRAIT` đọc lại 1. Manifest gộp của `MainActivity`: `screenOrientation` 1.
  3. Test của mục 4 chạy thật trên khung rỗng (`./gradlew :app:testStagingDebugUnitTest --tests '*ScreenOrientationTest*'`): 6 thất bại đúng lý do, 2 qua, như đoạn "Kết quả mong đợi ở bước 3".
  4. Code của mục 3.1 cùng 8 test, `scripts/check.sh` trên nhánh: lần 1 lỗi `ktlint` (dòng quá 120 ký tự, bản đầu của `screenOrientationFor` và của test `undefinedWidth_isIgnored`); đã sửa, khối ở mục 3.1 là bản đã qua `ktlint`. Lần 2: `Cổng chất lượng: QUA`, 1072 test JVM, 0 lỗi (`:app` 48 test, gồm 8 test của `ScreenOrientationTest`), 95 ảnh screenshot không đổi.
- **Lệnh:** `scripts/check.sh`; `./gradlew :app:testStagingDebugUnitTest --tests '*ScreenOrientationTest*'`; `./gradlew :feature:camera:testDebugUnitTest`, `:feature:poses:testDebugUnitTest` với test tạm; `scripts/device-scenario.py check app/src/deviceScenarios/T-056-man-lon-xoay-ngang.txt` (qua).
- **Kết quả:** như trên. Chưa chạy trên emulator hay máy thật: máy này không còn emulator (đã gỡ theo yêu cầu của Tuấn sau T-049), và `device-session.yml` chỉ chạy trên nhánh chính. Phương án dự phòng mục 3.4 chưa thử.

## 5. Chấm rủi ro

**Điểm chặn bị chạm:** Không có (không thêm quyền, thư viện, quảng cáo, sự kiện hay dữ liệu; manifest không đổi ở phương án chính).

| Tiêu chí | Điểm (0 đến 3) | Lý do |
| --- | --- | --- |
| A. Phạm vi thay đổi | 2 | Hai file code ở `:app`, một chú thích ở `:feature:camera`, test ở ba module |
| B. Độ rõ của yêu cầu | 2 | Spec camera đổi, chờ xác nhận; câu 15 (camera nằm ngang) còn mở; không có thiết kế cho màn lớn nằm ngang |
| C. Khả năng kiểm chứng tự động | 2 | Cách chọn hướng có test; đóng khung của hệ thống chỉ kiểm trên emulator (sau phiên) và máy thật |
| D. Ảnh hưởng chéo | 3 | Đổi hướng màn hình của cả app, mọi feature dựa vào |
| E. Độ mới của kỹ thuật | 2 | Đặt hướng lúc chạy chưa có trong dự án; API phổ biến |
| **Tổng** | **11** | **Mức: Cao** (duyệt chéo bắt buộc) |

## 6. Ảnh hưởng chéo

- File hoặc thành phần dùng chung bị chạm: `MainActivity.kt` (vỏ app); test của `:feature:camera` (`CameraScreenUnfoldedTest.kt`, `ReviewScreenUnfoldedTest.kt`) và `:feature:poses` (`PosesScreenTest.kt`, `PoseDetailScreenTest.kt`), chỉ thêm hàm test; `CameraXController.kt`, chỉ chú thích.
- Task khác trong phiên có thể xung đột: task dịch chữ hay test bố cục bản dịch (nếu có trong cùng danh sách) có thể sửa `CameraScreenUnfoldedTest.kt` hay `PosesScreenTest.kt`: khối thêm vào là hàm test riêng, chạy tuần tự thì task sau cập nhật nhánh. Task trình chạy kịch bản (nếu có) sửa `scripts/device-scenario.py`: tệp kịch bản này chỉ dùng lệnh đã có (`device rotate`, `device unfold`, `device fold`, `shot`).
- Đề xuất thứ tự chạy: sau các task chỉ đổi script; trước hay sau task bản dịch đều được.

## 7. Điều kiện dừng riêng

- Test `manifest_keepsPortrait_soPhonesStartWithoutARelaunch` hay test của điện thoại cho thấy điện thoại không còn khóa dọc: dừng, vì đổi hành vi của mọi người dùng điện thoại.
- Cần đổi `AndroidManifest.xml` ngoài dòng `screenOrientation` của phương án dự phòng, hay cần `configChanges`: dừng.

## 8. Câu hỏi cho người duyệt

1. **Bố cục khi màn lớn nằm ngang.** Chưa có thiết kế; bố cục mở gập vẫn đủ nút (mục 4.1) nhưng khung xem trước nhỏ lại và có khoảng trống. Đề xuất của AI: dùng bố cục mở gập như hiện nay (Android 16 trên màn lớn đã như vậy), không thêm ảnh chuẩn; thiết kế riêng nếu cần thì là ticket sau. Ảnh chụp rộng hơn khung xem trước khi nằm ngang (spec camera, câu 15): đề xuất chấp nhận ở task này, ticket `ViewPort` riêng.
2. **Xác nhận đổi spec camera** (PR này: mục 2, KB-11 hai tiêu chí mới, câu 7 phần bổ sung, câu 15). Đề xuất của AI: người phụ trách sản phẩm xác nhận, spec về `confirmed`, rồi mới duyệt plan.
3. **Giữ khóa dọc trong manifest hay bỏ** (mục 3.1 và 3.4). Đề xuất của AI: giữ (điện thoại không dựng lại lúc mở app); rủi ro là trên máy thật app bị đóng khung có thể không đọc được kích thước thật, khi đó plan phiên bản 2 bỏ khóa trong manifest.
4. **Kiểm trên máy thật trước khi phát hành.** Nhóm có máy gập hay máy tính bảng Android 14, 15 không? Nếu không, đề xuất: thử trên emulator cấu hình máy tính bảng (`wm set-ignore-orientation-request true` để giống máy thật) trong một phiên có người theo dõi, ghi kết quả vào phản hồi.

## 9. Quyết định duyệt

<!-- Người duyệt điền. AI không điền mục này. -->

- **Quyết định:**
- **Người duyệt:**
- **Ghi chú:**

## 10. Nhật ký thực hiện

<!-- AI ghi khi task đã có kết quả cuối (đã merge hoặc đã dừng), trên nhánh báo cáo của phiên (auto-run.md, bước 10). Mục mới thêm vào cuối, không sửa mục cũ. -->
