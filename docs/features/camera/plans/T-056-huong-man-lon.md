---
ticket: T-056
title: Màn lớn theo hướng của máy, điện thoại khóa dọc
status: draft                # draft | approved | stopped | done
version: 1                   # tăng khi plan được sửa lại sau phản hồi hoặc sau khi dừng
risk_score: 13               # tổng điểm theo risk-scoring.md, để trống nếu chạm điểm chặn
risk_level: high             # low | medium | high
token_estimate: 18M          # tổng token dự kiến, ví dụ 20M; số gợi ý: scripts/token-usage.py estimate (docs/ops/token-usage.md)
blocking_points: []          # các điểm chặn bị chạm, ví dụ [ads, sdk]
run_order:                   # AI điền sau họp đánh giá rủi ro
reviewer:                    # người duyệt
cross_reviewer:              # bắt buộc khi risk_level là high
approved_at:
---

# PLAN T-056: MÀN LỚN THEO HƯỚNG CỦA MÁY, ĐIỆN THOẠI KHÓA DỌC

## 1. Tóm tắt

Manifest giữ `android:screenOrientation="portrait"`, nên điện thoại vẫn mở thẳng ở chiều dọc, không dựng lại activity. Một hàm mới của vỏ app, `Activity.applyScreenOrientation()`, gọi trong `MainActivity.onCreate`: khi cạnh ngắn của màn từ 600 dp thì đặt `requestedOrientation = SCREEN_ORIENTATION_UNSPECIFIED` (app theo hướng của máy, như Android 16 vốn làm trên màn lớn), dưới 600 dp thì đặt lại `SCREEN_ORIENTATION_PORTRAIT`. Cạnh ngắn đọc từ bốn nguồn (cấu hình của activity, cấu hình của hệ thống, `maximumWindowMetrics` từ Android 11, kích thước vật lý của tấm màn ở mật độ gốc), vì app đang bị đóng khung có thể chỉ thấy cửa sổ nhỏ của mình. Kiểm chứng bằng test Robolectric cho cách chọn, cho từng nguồn và cho activity thật, test chặn bố cục khi màn lớn nằm ngang ở bảy màn, và một kịch bản emulator mới (kèm nâng ngân sách kịch bản lên 1100 giây).

`token_estimate` 18M thay cho số gợi ý 7.8M (trung vị cỡ M của `scripts/token-usage.py estimate`): từ RUN-20261026-01, token của phiên con (rà soát độc lập) tính vào task (L-073), hai task cỡ M của phiên đó tốn 15.0M và 19.7M; task này thêm test ở bảy màn và đổi hai script.

## 2. Hiểu yêu cầu

Ticket sửa FB-20261024-01 và xử lý KB-11 trong `features/camera/spec.md`, theo phần đề xuất đổi spec trong PR này (mục 2, KB-11 hai tiêu chí mới, câu 7 phần bổ sung, câu 15). Spec camera đang `changed`: plan chỉ được duyệt sau khi người phụ trách sản phẩm xác nhận lại (mục 8, câu 2).

| Kịch bản | Tiêu chí chấp nhận | Cách hiểu của AI |
| --- | --- | --- |
| KB-11 (mới) | Khi app mở trên màn lớn (cạnh ngắn từ 600 dp: máy gập khi mở ra, máy tính bảng) thì app dùng cả màn hình, không bị hệ thống đóng khung với hai dải đen, kể cả khi màn nằm ngang; xoay máy thì app xoay theo, mọi màn vẫn đủ nút chính trong màn hình | "Màn lớn" là cạnh ngắn (`smallestScreenWidthDp`) từ 600 dp, cùng ngưỡng 600 dp mà camera, xem lại và thư viện dùng để chọn bố cục mở gập (theo chiều rộng). App bị đóng khung khi khóa dọc trên màn nằm ngang (Android 12L đến 15); bỏ khóa trên màn lớn thì hết đóng khung. Đóng khung là việc của hệ thống, JVM không mô phỏng được: kiểm bằng kịch bản emulator và máy thật. "Mọi màn vẫn đủ nút chính" kiểm bằng test chặn bố cục ở khung nằm ngang cho bảy màn: camera, xem lại, thư viện, chi tiết tư thế, màn chào, cài đặt, loading (mục 4). Robolectric không trừ thanh trạng thái và thanh điều hướng, nên camera, xem lại và màn chào còn được thử ở khung thấp hơn (`h520dp`, `h593dp`) |
| KB-11 (mới) | Khi gập lại (cạnh ngắn dưới 600 dp) thì app trở về chiều dọc dù máy đang nằm ngang | Gập lại làm activity dựng lại (manifest không khai báo `configChanges`), `onCreate` gọi lại hàm và đặt lại `PORTRAIT`. Yêu cầu hướng của activity được hệ thống giữ qua lần dựng lại, nên phải đặt lại rõ ràng, không dựa vào manifest |
| KB-11 (cũ) | Khi gập lại thì camera và màn xem lại dùng bố cục điện thoại, giữ tư thế và các lựa chọn | Không đổi: mọi trạng thái nằm trong ViewModel (T-020). Xoay máy trên màn lớn cũng là một lần dựng lại như gập mở; camera đóng ở `onStop` và mở lại ở `onStart` (`CameraScreen`, `LifecycleStartEffect`), nên `Preview` dựng lại theo hướng mới của màn. Đếm ngược đang chạy thì bị hủy khi activity dừng, như khi ra nền (KB-8). Khi màn nằm ngang, nét viền và điểm khớp lệch so với khung xem trước (spec camera, câu 15): không sửa ở task này |
| poses KB-7 | Khi máy gập mở ra thì thư viện hiện 3 cột | Đã đúng trên khung `w673dp-h841dp` (T-024); phản hồi này là trường hợp app bị đóng khung nên chiều rộng còn dưới 600 dp. Bỏ khóa trên màn lớn thì thư viện có đủ chiều rộng. Test chặn ở khung nằm ngang `w841dp-h673dp` |

Thiết kế tham chiếu: không có thiết kế riêng cho màn lớn nằm ngang; dùng lại bố cục mở gập của `features/camera/design/README.md` và `features/poses/design/README.md` (mục 8, câu 1). Không thêm ảnh chuẩn mới.

ADR liên quan: ADR-0001 (vỏ app chỉ ghép feature; hàm mới nằm trong `:app`), ADR-0020 (khung `_unfolded` 673 x 841).

## 3. Giải pháp

### 3.1. Cách làm

**Hàm chọn hướng** (`app/src/main/kotlin/com/vglstudio/camera/pose/line/guide/ui/ScreenOrientation.kt`, cùng gói với `UiAutomatorTags.kt`):

```kotlin
/** From this smallest width the screen is large (an unfolded foldable, a tablet): the Material "compact" limit. */
internal const val LARGE_SCREEN_MIN_SMALLEST_WIDTH_DP = 600

internal const val ORIENTATION_LOG_TAG = "Orientation"

/**
 * The smallest width in dp from four sources, 0 when a source is unknown: the activity's configuration, the system
 * configuration, the largest window (Android 11 and later) and the physical panel at its stable density.
 */
internal data class SmallestWidths(val activity: Int, val system: Int, val window: Int, val panel: Int) {
    fun all(): List<Int> = listOf(activity, system, window, panel)

    override fun toString() = "activity $activity, system $system, window $window, panel $panel"
}

/**
 * Portrait on a phone; on a large screen the app follows the device (spec camera, KB-11; plan T-056). The screen is
 * large when any of [smallestWidthsDp] is: a portrait app letterboxed on a landscape inner screen may see only its
 * own small window, while another source still reports the real size.
 */
internal fun screenOrientationFor(smallestWidthsDp: List<Int>): Int {
    val large = smallestWidthsDp.any {
        it != Configuration.SMALLEST_SCREEN_WIDTH_DP_UNDEFINED && it >= LARGE_SCREEN_MIN_SMALLEST_WIDTH_DP
    }
    return if (large) ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED else ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
}

/** The shorter side of a panel of [widthPx] x [heightPx] at [densityDpi], in dp; 0 when unknown. */
internal fun physicalSmallestWidthDp(widthPx: Int, heightPx: Int, densityDpi: Int): Int =
    if (widthPx <= 0 || heightPx <= 0 || densityDpi <= 0) {
        0
    } else {
        minOf(widthPx, heightPx) * DisplayMetrics.DENSITY_DEFAULT / densityDpi
    }

/**
 * The four sources of [SmallestWidths]. The panel is not scaled by the compatibility mode of a letterboxed app;
 * `wm size` on an emulator does not change it either.
 */
internal fun Activity.screenSmallestWidthsDp(
    stableDensityDpi: Int = DisplayMetrics.DENSITY_DEVICE_STABLE,
): SmallestWidths {
    var window = 0
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        val bounds = windowManager.maximumWindowMetrics.bounds
        if (bounds.width() > 0 && bounds.height() > 0) {
            window = (minOf(bounds.width(), bounds.height()) / resources.displayMetrics.density).toInt()
        }
    }
    // The display of the activity's window; Context.getDisplay() throws where a context has no display.
    @Suppress("DEPRECATION")
    val mode = runCatching { windowManager.defaultDisplay.mode }.getOrNull()
    val panel = mode?.let { physicalSmallestWidthDp(it.physicalWidth, it.physicalHeight, stableDensityDpi) } ?: 0
    return SmallestWidths(
        activity = resources.configuration.smallestScreenWidthDp,
        system = Resources.getSystem().configuration.smallestScreenWidthDp,
        window = window,
        panel = panel,
    )
}

/**
 * Applies [screenOrientationFor] to this activity and logs the sources and the choice (ADR-0019), so a test session
 * shows which source saw which size. The manifest keeps portrait, so a phone starts without a relaunch; only a large
 * screen changes it, once. The request is kept across a relaunch, so it is set only when it differs.
 * [stableDensityDpi] is for tests: Robolectric reports 160 whatever the density of the qualifiers.
 */
fun Activity.applyScreenOrientation(logger: AppLogger, stableDensityDpi: Int = DisplayMetrics.DENSITY_DEVICE_STABLE) {
    val widths = screenSmallestWidthsDp(stableDensityDpi)
    val orientation = screenOrientationFor(widths.all())
    val name = if (orientation == ActivityInfo.SCREEN_ORIENTATION_PORTRAIT) "portrait" else "unspecified"
    val changed = requestedOrientation != orientation
    if (changed) requestedOrientation = orientation
    logger.info(ORIENTATION_LOG_TAG, "Orientation $name, changed $changed (smallest width dp: $widths)")
}
```

- Vì sao bốn nguồn: app khóa dọc bị đóng khung trên màn nằm ngang thì cấu hình của activity chỉ là cửa sổ đóng khung (ví dụ khoảng 486 dp trong FB-20261024-01), dưới 600 dp. Ở chế độ tương thích, hệ thống có thể thu cả `Resources.getSystem()` và `maximumWindowMetrics` về khung đóng; nhất là đường chính: điện thoại đang dọc rồi mở gập. Kích thước vật lý của tấm màn (`Display.Mode.physicalWidth`, `physicalHeight`) chia cho mật độ gốc (`DisplayMetrics.DENSITY_DEVICE_STABLE`) không bị thu như vậy. Có một nguồn từ 600 dp thì là màn lớn (`any`). Điện thoại có cả bốn nguồn dưới 600 dp nên không bao giờ bị mở khóa nhầm; đổi "cỡ hiển thị" trong cài đặt máy không đổi mật độ gốc.
- Emulator của `device-session.yml` (tấm màn vật lý 320 x 640): `wm size` và `wm density` không đổi kích thước vật lý, nên nguồn thứ tư luôn nhỏ (320 px ở mật độ gốc của emulator, dưới 600 dp); sau `device unfold`, cấu hình của hệ thống là 673 dp nên `any` vẫn mở khóa, trước đó cả bốn nguồn nhỏ nên app khóa dọc. Máy gập thật: màn trong 1840 x 2208 ở 420 dpi là 700 dp.
- `Context.getDisplay()` ném lỗi với context không gắn màn hình (đã gặp trên Robolectric), nên hàm đọc `windowManager.defaultDisplay.mode` (màn của cửa sổ activity) trong `runCatching`: lỗi đọc màn không bao giờ làm app dừng lúc khởi động.
- Ghi log (ADR-0019): mỗi lần `onCreate`, một dòng `info` thẻ `Orientation`, ví dụ `Orientation unspecified, changed true (smallest width dp: activity 486, system 673, window 0, panel 700)`. Dòng này cho `device-session` và lần thử trên máy thật biết nguồn nào thấy bao nhiêu, nhất là khi app vẫn bị đóng khung (mục 3.4). Không có dữ liệu cá nhân: chỉ kích thước màn và hướng. `MainActivity` tiêm `AppLogger` bằng `@Inject lateinit var logger: AppLogger` như các trường khác (Hilt tiêm trong `super.onCreate`, trước lời gọi); `AppLogger` đã có binding ở `:core:logging` (`LoggingModule`), `:app` đã phụ thuộc `:core:logging`.
- Tham số `stableDensityDpi` chỉ cho test: Robolectric trả `DENSITY_DEVICE_STABLE` là 160 dù `qualifiers` là `xxhdpi`, nên test của activity truyền 480.
- Vì sao giữ khóa dọc trong manifest: điện thoại (gần hết người dùng) khởi động thẳng ở chiều dọc, không dựng lại activity lúc mở app. Chỉ màn lớn đang nằm ngang dựng lại một lần lúc khởi động, khi đổi từ `PORTRAIT` sang `UNSPECIFIED`: lần đó `ads.gatherConsent(this)` chạy hai lần (như mọi lần dựng lại), camera mở rồi đóng nếu màn đầu là camera, và màn hình có thể nhấp nháy một lần. Hướng ngược lại (bỏ khóa trong manifest, khóa dọc trong `onCreate` trên điện thoại) làm điện thoại cầm ngang lúc mở app dựng lại activity: phương án dự phòng ở mục 3.4.
- Chỉ đặt khi khác giá trị đang có: yêu cầu hướng được hệ thống giữ qua lần dựng lại, nên lần `onCreate` sau trên cùng màn không đặt lại và không lặp.

**`MainActivity`**: thêm `@Inject lateinit var logger: AppLogger`; trong `onCreate`, gọi `applyScreenOrientation(logger)` ngay sau `super.onCreate(savedInstanceState)`, trước `enableEdgeToEdge()`, kèm một dòng chú thích (FB-20261024-01, plan T-056). Không đổi gì khác.

**`CameraXController`**: chỉ sửa chú thích của `orientationListener` ("The app is locked to portrait, so ..."), vì từ task này app không còn khóa dọc trên màn lớn. Cách tính `targetRotation` theo cảm biến (góc so với hướng tự nhiên của máy) đúng cho mọi hướng của màn, nên không đổi code: ảnh chụp đứng thẳng theo cách cầm máy.

**Kịch bản emulator** `app/src/deviceScenarios/T-056-man-lon-xoay-ngang.txt`, nội dung đúng như khối dưới (đã thử `scripts/device-scenario.py check`, qua): vào thư viện; `device rotate landscape` trên điện thoại (ảnh `phone-landscape`); `device unfold` rồi `device rotate landscape` lần nữa (sau `wm size` app dựng lại hai lần, một lần vì kích thước, một lần vì hướng; lệnh xoay thứ hai không đổi gì mà chờ màn ổn định trước `wait` và `shot`), ảnh `unfolded-landscape`; `device fold` rồi `device rotate landscape` lần nữa (gập lại khi nằm ngang app cũng dựng lại hai lần; `wait_settled` chờ tối đa 10 giây, và RUN-20261025-02 ghi `fold` không ổn định sau 11 đến 12 giây), ảnh `folded-landscape`.

Cách đọc kết quả. Emulator này đã đóng khung app ở FB-20261025-01 (nút gốc `[246,0][1521,1920]`, lúc kích thước bị cắt), nên bước (2) có thể đi một trong hai đường:
- Hệ thống xoay màn theo yêu cầu dọc của app: trước task, `unfolded-landscape` cao hơn rộng; sau task, rộng hơn cao.
- Hệ thống giữ màn nằm ngang và đóng khung app: trước task, `unfolded-landscape` rộng hơn cao nhưng app là một cột dọc giữa hai dải đen; sau task, app kín màn.
Đạt (sau task): `unfolded-landscape` rộng hơn cao, app kín màn, không dải đen, thư viện 3 cột; `phone-landscape` và `folded-landscape` cao hơn rộng (hay rộng hơn cao mà app là cột dọc kín chiều cao, nếu hệ thống đóng khung điện thoại; ghi rõ đường nào trong báo cáo). `unfolded-landscape` có dải đen hay cao hơn rộng là `lỗi` của task, soạn phản hồi.

```text
ticket: T-056
criterion: Màn lớn theo hướng của máy, điện thoại khóa dọc (plan T-056, mục 4, kiểm tra thủ công 1), bằng wm size thay cho gập thật
timeout: 180
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
# (2) mở gập khi đang xoay ngang: app dựng lại hai lần (kích thước, rồi hướng); xoay lần nữa để chờ ổn định.
# Đạt: ảnh unfolded-landscape rộng hơn cao, app kín màn, 3 cột, không có dải đen.
device unfold
device rotate landscape
wait id=poses_grid 10
shot unfolded-landscape
# (3) gập lại khi vẫn xoay ngang: app trở về dọc, giữ màn thư viện; app dựng lại hai lần, xoay lần nữa để chờ ổn định
device fold
device rotate landscape
wait id=poses_grid 10
shot folded-landscape
```

**Ngân sách kịch bản** (`docs/ops/test-sessions.md`, mục 5, Hạn chót): ba tệp hiện có cần tối đa khoảng 850 giây (plan T-048, mục 3.1); tệp mới thêm tối đa 180 (timeout của tệp: 5 lệnh `device`, mỗi lệnh chờ ổn định tới 10 giây, cùng các bước như T-024; tệp T-024 chạy 149 giây với timeout 180) + 30 (trả máy) + 20 (phần chừa) = 230 giây, tổng khoảng 1080, vượt 900. `sorted()` chạy `app/` trước `feature/`, nên không nâng thì T-024 có thể thành `bỏ qua (hết thời gian)`. Nâng mặc định `DEVICE_SCENARIOS_BUDGET` từ 900 lên 1100. Tìm theo nội dung, không theo số dòng (T-054 merge trước sẽ làm lệch số dòng): `scripts/device-scenario.py`, dòng `self.budget = env_seconds("DEVICE_SCENARIOS_BUDGET", 900)` và câu `DEVICE_SCENARIOS_BUDGET seconds (default 900)` của chuỗi trợ giúp; `scripts/device-session.sh`, `${DEVICE_SCENARIOS_BUDGET:-900}` trong lệnh `timeout -k 30` và chú thích `default 900` ở đầu tệp; `docs/ops/test-sessions.md`, mục 5, đoạn **Hạn chót**: câu "(mặc định 900)" và câu "Ba tệp hiện có cần tối đa khoảng 850 giây ... vượt 900 giây" (thành bốn tệp, khoảng 1080 giây, plan T-056; vượt 1100 thì plan của tệp mới nâng tiếp). T-054 cũng thêm nội dung vào mục 5 của tài liệu này; sửa đúng hai câu trên, giữ phần T-054 thêm. `.github/workflows/device-session.yml` có `timeout-minutes: 50`: lượt `device-session` của RUN-20261026-01 mất 22 phút với ngân sách 900; thêm 230 giây còn khoảng 26 phút, vẫn đủ, không sửa `.github/`. `scripts/tests/run.sh` không kiểm số 900 (`grep -n 900 scripts/tests/run.sh` chỉ thấy mã ticket `T-900` và tọa độ của `app-explore`; các test ngân sách đặt `DEVICE_SCENARIOS_BUDGET` riêng), nên không sửa. `app/src/test/kotlin/com/vglstudio/camera/pose/line/guide/ui/UiAutomatorTagsTest.kt` (chú thích của lớp) và `scripts/tests/fake-adb` (chú thích đầu tệp) chỉ nhắc `scripts/device-scenario.py` trong chú thích (cách trình chạy chọn phần tử, dump giả), không phụ thuộc ngân sách: không ảnh hưởng (L-020).

### 3.2. Danh sách file

| Thao tác | Đường dẫn | Nội dung thay đổi |
| --- | --- | --- |
| Tạo | `app/src/main/kotlin/com/vglstudio/camera/pose/line/guide/ui/ScreenOrientation.kt` | `SmallestWidths`, `screenOrientationFor`, `physicalSmallestWidthDp`, `Activity.screenSmallestWidthsDp()`, `Activity.applyScreenOrientation(logger)` như mục 3.1 (bước 3: khung rỗng trả `PORTRAIT`, `0`, `SmallestWidths(0, 0, 0, 0)` với `all()` rỗng, không làm gì, không ghi log) |
| Sửa | `app/src/main/kotlin/com/vglstudio/camera/pose/line/guide/MainActivity.kt` | Tiêm `AppLogger`, gọi `applyScreenOrientation(logger)` sau `super.onCreate` |
| Sửa | `feature/camera/src/main/kotlin/com/vglstudio/camera/pose/line/guide/feature/camera/camera/CameraXController.kt` | Chỉ sửa chú thích của `orientationListener` |
| Tạo | `app/src/test/kotlin/com/vglstudio/camera/pose/line/guide/ui/ScreenOrientationTest.kt` | 12 test của mục 4 |
| Sửa | `feature/camera/src/test/kotlin/com/vglstudio/camera/pose/line/guide/feature/camera/CameraScreenUnfoldedTest.kt` | Test chặn `landscape_controlsInsideWindow` ở bốn khung nằm ngang |
| Sửa | `feature/camera/src/test/kotlin/com/vglstudio/camera/pose/line/guide/feature/camera/review/ReviewScreenUnfoldedTest.kt` | Test chặn `landscape_saveInsideWindow` ở bốn khung nằm ngang |
| Sửa | `feature/poses/src/test/kotlin/com/vglstudio/camera/pose/line/guide/feature/poses/PosesScreenTest.kt` | Test chặn `landscape_hasThreeColumns` |
| Sửa | `feature/poses/src/test/kotlin/com/vglstudio/camera/pose/line/guide/feature/poses/detail/PoseDetailScreenTest.kt` | Test chặn `landscape_bottomBarVisible` |
| Sửa | `feature/onboarding/src/test/kotlin/com/vglstudio/camera/pose/line/guide/feature/onboarding/OnboardingScreenTest.kt` | Test chặn `landscape_buttonAndNoteVisible` ở hai khung |
| Sửa | `feature/settings/src/test/kotlin/com/vglstudio/camera/pose/line/guide/feature/settings/SettingsScreenTest.kt` | Test chặn `landscape_versionRowReachable` ở hai khung |
| Sửa | `feature/loading/src/test/kotlin/com/vglstudio/camera/pose/line/guide/feature/loading/LoadingScreenTest.kt` | Test chặn `landscape_retryReachable` ở hai khung |
| Tạo | `app/src/deviceScenarios/T-056-man-lon-xoay-ngang.txt` | Kịch bản emulator, đúng khối ở mục 3.1; chạy `scripts/device-scenario.py check` |
| Sửa | `scripts/device-scenario.py` | Mặc định `DEVICE_SCENARIOS_BUDGET` 900 thành 1100, ở `env_seconds("DEVICE_SCENARIOS_BUDGET", 900)` và trong chuỗi trợ giúp (tìm theo nội dung) |
| Sửa | `scripts/device-session.sh` | `${DEVICE_SCENARIOS_BUDGET:-900}` thành `:-1100` và chú thích `default 900` ở đầu tệp (tìm theo nội dung) |
| Sửa | `docs/ops/test-sessions.md` | Mục 5, Hạn chót: mặc định 1100; bốn tệp cần khoảng 1080 giây (plan T-056); giữ phần T-054 thêm |

`AndroidManifest.xml` không đổi (giữ `android:screenOrientation="portrait"`). `scripts/tests/run.sh` (test `app: MainActivity and NetworkDialogHost call exposeTestTagsForUiAutomator`) đếm `exposeTestTagsForUiAutomator()` trong `MainActivity.kt`: task thêm hai dòng `import`, một trường `logger` và một lời gọi `applyScreenOrientation(logger)`, không thêm lời gọi `exposeTestTagsForUiAutomator()`, nên số đếm vẫn là 1; không ảnh hưởng (L-020). Không file nào khác đọc hướng của activity (`grep -rn 'screenOrientation\|requestedOrientation' --include='*.kt' --include='*.xml' app feature core` chỉ thấy manifest).

### 3.3. Cờ tính năng, quảng cáo, dữ liệu, quyền, thư viện

- Cờ tính năng: Không có. Hành vi của vỏ app, không bật tắt theo Remote Config.
- Quảng cáo: Không chạm. Hộp thoại đồng ý (`ads.gatherConsent(this)` trong `onCreate`) đã chạy lại ở mỗi lần dựng lại activity (đổi chế độ tối, xoay trên Android 16), task không đổi điều đó.
- Sự kiện phân tích: Không chạm. Dòng log `Orientation` là log hành vi (ADR-0019), không phải sự kiện phân tích.
- File R2: Không chạm.
- **Quyền truy cập:** Không thêm. `setRequestedOrientation`, `Resources.getSystem()`, `WindowManager.getMaximumWindowMetrics()` không cần quyền.
- **Thư viện và SDK:** Không thêm. Chỉ dùng API của Android (`android.app.Activity`, `android.content.res`, `android.view.WindowManager` từ API 30); không dùng `androidx.window`.

### 3.4. Phương án dự phòng

- **Điểm chưa chắc:** trên máy thật, app khóa dọc bị đóng khung có thể bị thu kích thước ở ba nguồn đầu. Nguồn thứ tư (tấm màn vật lý) được thêm cho trường hợp đó, nhưng chưa kiểm trên máy thật. Nếu cả bốn nguồn đều nhỏ, app vẫn khóa dọc. JVM không kiểm được; chỉ lộ ra ở máy thật (mục 4), sau phiên.
- **Điều kiện chuyển:** không chuyển trong phiên vì lý do trên, vì phiên không có máy thật hay emulator. Trong phiên chỉ chuyển khi một test của mục 4 không qua được sau 2 lần sửa vì cách đọc kích thước (ví dụ Robolectric không trả cấu hình hệ thống theo `qualifiers`).
- **Cách làm thay:** bỏ `android:screenOrientation` khỏi `AndroidManifest.xml` (dự phòng) và để `applyScreenOrientation()` chỉ đặt `PORTRAIT` khi cả bốn nguồn dưới 600 dp. App không khóa lúc khởi động nên lần mở đầu không bị đóng khung; đổi lại, điện thoại cầm ngang lúc mở app dựng lại activity một lần. Phương án này không chữa được đường "điện thoại đang dọc rồi mở gập": yêu cầu `PORTRAIT` đặt lúc gập vẫn còn khi mở gập, nên app vẫn có thể bị đóng khung và bị thu kích thước như phương án chính; nguồn thứ tư là cách duy nhất cho đường đó. Test `manifest_keepsPortrait_soPhonesStartWithoutARelaunch` khi đó đổi kỳ vọng thành `SCREEN_ORIENTATION_UNSPECIFIED` và ghi vào "Lệch so với plan"; các test khác giữ nguyên. Nếu máy thật cho thấy phương án chính không chạy (sau phiên), plan phiên bản 2 quyết từ kết quả đó.

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
| Nguồn thứ tư: tấm màn vật lý ra dp | Unit test | `physicalPanel_inDp`: `physicalSmallestWidthDp(1840, 2208, 420) == 700`, `(1080, 2400, 420) == 411`, `(0, 2208, 420) == 0`, `(1840, 2208, 0) == 0` |
| KB-11 mới, tiêu chí 1: mở gập thì mở khóa | Unit test (activity thật) | `@Config(qualifiers = "w673dp-h841dp-xxhdpi") unfolded_unlocksThePortraitOfTheManifest`: đặt `requestedOrientation = PORTRAIT` (như manifest), gọi `applyScreenOrientation(logger, stableDensityDpi = 480)` (`logger` là `RecordingAppLogger`), mong `UNSPECIFIED` |
| KB-11 mới, tiêu chí 1: màn lớn nằm ngang | Unit test (activity thật) | `@Config(qualifiers = "w841dp-h673dp-land-xxhdpi") unfoldedLandscape_unlocks`: như trên |
| KB-11 mới, tiêu chí 2: gập lại thì khóa dọc | Unit test (activity thật) | `@Config(qualifiers = "w411dp-h891dp-xxhdpi") foldedAgain_locksToPortrait`: đặt `requestedOrientation = UNSPECIFIED` (như sau khi mở gập), gọi hàm (480), mong `PORTRAIT`. Đối chứng: khung rỗng không làm gì nên test thất bại |
| Nguồn thứ hai: cấu hình hệ thống khi activity bị đóng khung | Unit test (activity thật) | `@Config(qualifiers = "w486dp-h673dp-xxhdpi") letterboxedActivity_readsTheSystemConfiguration`: `PORTRAIT`, gọi hàm (480) thì vẫn `PORTRAIT` (đối chứng: mọi nguồn 486); rồi đặt riêng `Resources.getSystem().configuration.smallestScreenWidthDp = 673` (`updateConfiguration`, trả lại trong `finally`): `screenSmallestWidthsDp(480).system == 673`, gọi hàm thì `UNSPECIFIED` |
| Nguồn thứ tư có trong danh sách | Unit test (activity thật) | `@Config(qualifiers = "w486dp-h673dp-xxhdpi") physicalPanel_isOneOfTheSources`: `screenSmallestWidthsDp(480).panel == 486` (1458 x 2019 px ở 480 dpi); `applyScreenOrientation(logger, stableDensityDpi = 160)` (tấm màn 1458 px đọc ở 160 dpi là 1458 dp) cho `UNSPECIFIED` |
| Ghi log nguồn và hướng (ADR-0019) | Unit test (activity thật) | `@Config(qualifiers = "w673dp-h841dp-xxhdpi") apply_logsTheSourcesAndTheChoice`: `PORTRAIT`, gọi hàm hai lần (480); `logger.entries` đúng bằng hai `LogEntry(INFO, "Orientation", ...)`: `Orientation unspecified, changed true (smallest width dp: activity 673, system 673, window 0, panel 673)` rồi `... changed false (...)` |
| Điện thoại khởi động không dựng lại | Unit test (test chặn) | `manifest_keepsPortrait_soPhonesStartWithoutARelaunch`: `packageManager.getActivityInfo(ComponentName(activity, MainActivity::class.java), 0).screenOrientation == SCREEN_ORIENTATION_PORTRAIT`. Qua ngay (test chặn: manifest không được đổi) |
| KB-11 mới, tiêu chí 1: camera đủ nút khi màn lớn nằm ngang | Compose UI test (test chặn) | `CameraScreenUnfoldedTest.landscape_controlsInsideWindow_<khung>`, bốn hàm với `@Config(qualifiers = ...)`: `"vi-w841dp-h673dp-land-xxhdpi"`, `"vi-w960dp-h600dp-land-xxhdpi"`, `"vi-w841dp-h593dp-land-xxhdpi"`, `"vi-w960dp-h520dp-land-xxhdpi"` (hai khung sau là hai khung đầu trừ thanh hệ thống): mọi phần `PREVIEW`, `FLIP`, `SHUTTER`, `LAST_PHOTO`, `ZOOM`, `CHIPS`, `STRIP`, `POSES`, `SETTINGS` có `bottom <= root.bottom` và `right <= root.right` (`getUnclippedBoundsInRoot`); khung xem trước giữ 3:4 (`height / width` trong 1.333 ± 0.02). Qua ngay: số đo ở mục 4.1 |
| Màn xem lại đủ nút khi nằm ngang | Compose UI test (test chặn) | `ReviewScreenUnfoldedTest.landscape_saveInsideWindow_<khung>`, bốn khung như trên: `PHOTO`, `RETAKE`, `SHARE`, `NEXT_POSE`, `SAVE`, `CLOSE` nằm trong root; ảnh giữ 3:4 (± 0.02). Qua ngay |
| poses KB-7: thư viện 3 cột khi màn lớn nằm ngang | Compose UI test (test chặn) | `PosesScreenTest.landscape_hasThreeColumns`, `@Config(qualifiers = "vi-w841dp-h673dp-land-xxhdpi")`, `setScreen(ready(cards = manyCards()))`: `p01`, `p02`, `p03` cùng `top`, đi dần sang phải; `p04` dưới `p01`, cùng `left`. Qua ngay |
| Chi tiết tư thế: thanh nút thấy được khi nằm ngang | Compose UI test (test chặn) | `PoseDetailScreenTest.landscape_bottomBarVisible`, cùng khung, `setScreen(ready(detail = longDetail()), width = { 841.dp })`: `BOTTOM_BAR.bottom == root.bottom` (± 0.5), `BOTTOM_BAR.top >= IMAGE.top`, ảnh bên trái thanh nút (`IMAGE.right <= BOTTOM_BAR.left`). Qua ngay |
| Màn chào đủ nút khi nằm ngang | Compose UI test (test chặn) | `OnboardingScreenTest.landscape_buttonAndNoteVisible_<khung>`, hai hàm `@Config(qualifiers = "vi-w960dp-h600dp-land-xxhdpi")` và `"vi-w960dp-h520dp-land-xxhdpi"`, `setScreen(onEnableCamera = { clicks++ })`: nút `ENABLE_CAMERA` và `NOTE` có `bottom <= root.bottom`, nút ở dưới vùng cuộn (`button.top >= CONTENT.bottom`), `FEATURE_SAVED` tới được bằng `performScrollTo()`, bấm nút thì `clicks == 1`. Qua ngay |
| Cài đặt tới được dòng cuối khi nằm ngang | Compose UI test (test chặn) | `SettingsScreenTest.landscape_versionRowReachable_<khung>`, hai khung như trên, giống `small_versionRowReachable`: dòng "Phiên bản" `performScrollTo().assertIsDisplayed()`, `bottom <= SCREEN.bottom`. Qua ngay |
| Loading tới được nút thử lại khi nằm ngang | Compose UI test (test chặn) | `LoadingScreenTest.landscape_retryReachable_<khung>`, hai khung như trên, `LoadingUiState.Failed(0.68f)`: `RETRY.performScrollTo().assertIsDisplayed()`, `bottom <= SCREEN.bottom`. Qua ngay |
| KB-11 mới, cả hai tiêu chí: hướng app yêu cầu khi xoay, mở gập, gập lại | Kiểm tra thủ công (emulator: `app/src/deviceScenarios/T-056-man-lon-xoay-ngang.txt`) | `device-session` cuối phiên chạy tệp. Phiên mở ba ảnh và đọc theo "Cách đọc kết quả" ở mục 3.1 (hai đường: hệ thống xoay màn hay đóng khung) |
| KB-11 mới, tiêu chí 1: hết đóng khung trên máy thật | Kiểm tra thủ công (máy thật: gập có bản lề) | Bản staging trên máy gập có màn trong nằm ngang (ví dụ Pixel Fold) hoặc máy tính bảng, Android 14 hay 15: (a) mở app khi máy nằm ngang, app dùng cả màn hình; (b) mở app trên màn ngoài, rồi mở gập khi đang nằm ngang, app dùng cả màn trong; (c) xoay máy, app xoay theo; (d) gập lại, app dọc. Nếu vẫn đóng khung: báo lại kèm bước, plan phiên bản 2 (mục 3.4) |
| Camera khi màn lớn nằm ngang (spec camera, câu 15) | Kiểm tra thủ công (máy thật: camera thật) | Trên máy ở dòng trên: chụp một ảnh khi máy nằm ngang và một ảnh khi dọc; ảnh đứng thẳng theo cách cầm máy; ghi lại phần ảnh rộng hơn khung xem trước, và độ lệch của nét viền, điểm khớp so với người trong khung (`LandmarkMapping` quy điểm về 0..1 của cả ảnh phân tích, còn khung 3:4 chỉ hiện phần giữa), để người phụ trách sản phẩm quyết ticket `ViewPort` |

Mỗi dòng "Kiểm tra thủ công" ghi nơi chạy (L-062); `scripts/plan-check.py` báo lỗi dòng thiếu khi plan còn `draft` (quy tắc 11).

**Kết quả mong đợi ở bước 3** (khung rỗng: `screenOrientationFor` trả `PORTRAIT`, `physicalSmallestWidthDp` trả 0, `screenSmallestWidthsDp` trả `SmallestWidths(0, 0, 0, 0)`, `applyScreenOrientation` không làm gì và không ghi log; đã chạy thật, mục 4.1): 10 test thất bại đúng lý do (`width599_...`, `letterboxedWindow...`, `undefinedWidth_...`, `unfolded_...`, `unfoldedLandscape_...`: `expected:<-1> but was:<1>`; `foldedAgain_...`: `expected:<1> but was:<-1>`; `physicalPanel_inDp`: `expected:<700> but was:<0>`; `physicalPanel_isOneOfTheSources`: `expected:<486> but was:<0>`; `letterboxedActivity_...`: đối chứng qua, rồi `expected:<673> but was:<0>`; `apply_logsTheSourcesAndTheChoice`: danh sách log rỗng), 2 qua (`phone_isPortrait` đối chứng, `manifest_...` test chặn). 16 test chặn bố cục qua ngay (số đo mục 4.1). Bước 4 viết đúng mục 3.1 thì cả 12 test của `ScreenOrientationTest` qua.

**Ngoại lệ viết test trước:** (1) trường `logger` và dòng gọi `applyScreenOrientation(logger)` trong `MainActivity`: `MainActivity` cần Hilt, không dựng được trong test JVM của `:app`; chỗ gọi kiểm bằng rà soát độc lập và kịch bản emulator. (2) Nguồn `maximumWindowMetrics`: Robolectric trả 0 x 0, test không kiểm được nguồn này; chỉ kiểm trên emulator và máy thật. (3) Chú thích của `CameraXController`. (4) Tệp kịch bản: chạy ở `device-session` cuối phiên, không "thất bại trước" được. (5) Ngân sách 1100 của hai script và tài liệu: giá trị cấu hình, kiểm ở `device-session` cuối phiên (mọi tệp có kết quả, không tệp nào `bỏ qua (hết thời gian)`). (6) 16 test chặn bố cục qua ngay: bố cục mở gập đã đúng khi nằm ngang; test giữ cho task sau không làm hỏng.

### 4.1. Chạy thử

- **Đã chạy thử (2026-10-26, nhánh `claude/plan-T-056-huong-man-lon` trên `origin/main` 3bf61da, không commit):**
  1. Số đo bố cục trên code hiện tại, test tạm Robolectric `NATIVE` (`getUnclippedBoundsInRoot`, dp, trái,trên..phải,dưới):
     - Camera `vi-w841dp-h673dp-land-xxhdpi`: root 841x673; `PREVIEW` 16,72..340,505 (324 x 433, tỷ lệ 1.336); `FLIP` 575,176..623,224; `SHUTTER` 561,250..637,326; `LAST_PHOTO` 575,374..623,422; `ZOOM` 531,453..667,501; `CHIPS` 0,505..841,557; `STRIP` 0,557..841,637; `POSES` 16,16..64,64; `SETTINGS` 292,16..340,64. Mọi phần trong root.
     - Camera `vi-w960dp-h600dp-land-xxhdpi`: `PREVIEW` 16,72..286,432 (270 x 360); `STRIP` 0,484..960,564; mọi phần trong root. Đối chiếu `vi-w1280dp-h800dp-land-xhdpi`: `STRIP` 0,684..1280,764. Khung `vi-w673dp-h841dp-xxhdpi` (đối chiếu với T-020): `PREVIEW` 16,72..466,673, `STRIP` 0,725..673,805.
     - Xem lại `w841dp-h673dp`: `PHOTO` 16,64..457,653 (441 x 589, tỷ lệ 1.336), `SAVE` 473,597..610,653; `w960dp-h600dp`: `PHOTO` 16,64..403,580, `SAVE` 419,524..556,580; mọi phần trong root.
     - Thư viện `w841dp-h673dp`: `p01` 16,160..275,543, `p02` 291,160..550,543, `p03` 566,160..825,543, `p04` 16,559..275,943: 3 cột, rộng 259.
     - Chi tiết `w841dp-h673dp` (`longDetail()`): root 0,0..841,673; `IMAGE` 16,80..413,520; `BOTTOM_BAR` 429,583..841,673.
     - Khung trừ thanh hệ thống: camera `w841dp-h593dp`: `PREVIEW` 16,72..280,425, `SHUTTER` 531,210..607,286, `ZOOM` 501,373..637,421, `STRIP` 0,477..841,557; `w960dp-h520dp`: `PREVIEW` 16,72..226,352, `SHUTTER` 563,174..639,250, `STRIP` 0,404..960,484. Xem lại `w841dp-h593dp`: `PHOTO` 16,64..397,573, `SAVE` 413,517..550,573; `w960dp-h520dp`: `PHOTO` 16,64..343,500, `SAVE` 359,444..496,500. Mọi phần trong root.
     - Màn chào `w960dp-h600dp`: nút 24,480..936,536, `NOTE` 24,560..936,576, `CONTENT` 24,56..936,456, `FEATURE_SAVED` tới được; `w960dp-h520dp`: nút 24,400..936,456, `NOTE` 24,480..936,496, `CONTENT` 24,56..936,376, tới được. Cài đặt: dòng "Phiên bản" sau khi cuộn 0,536..960,600 (`h600`), 0,456..960,520 (`h520`). Loading `Failed`: nút thử lại 321,496..639,552 (`h600`, không cần cuộn), 321,464..639,520 sau khi cuộn (`h520`).
     - Nhận xét: khi nằm ngang, khung xem trước nhỏ lại (324 x 433 thay vì 450 x 600) và giữa khung với cột nút có khoảng trống; đủ nút, không cắt. Chưa có thiết kế riêng (mục 8, câu 1).
  2. Giá trị Robolectric đọc được (test tạm, `Robolectric.buildActivity(ComponentActivity)`, `@Config(application = Application::class)`): ở `w673dp-h841dp-xxhdpi` và `w841dp-h673dp-land-xxhdpi`, `activity.resources.configuration.smallestScreenWidthDp`, cấu hình của application và `Resources.getSystem()` đều là 673; ở `w411dp-h891dp-xxhdpi` đều là 411. `windowManager.maximumWindowMetrics.bounds` là 0 x 0 trên Robolectric, nên hàm bỏ qua nguồn này khi rỗng. `requestedOrientation` mặc định -1, đặt `PORTRAIT` đọc lại 1. Manifest gộp của `MainActivity`: `screenOrientation` 1.
  3. Test của mục 4 (12 test của `ScreenOrientationTest`) chạy thật trên khung rỗng (`./gradlew :app:testStagingDebugUnitTest --tests '*ScreenOrientationTest*'`): 10 thất bại đúng lý do, 2 qua, như đoạn "Kết quả mong đợi ở bước 3". Trên code của mục 3.1: 12 qua.
  4. Bỏ từng nguồn khỏi code của mục 3.1 (đột biến, không commit): đặt `system = 0` thì `letterboxedActivity_readsTheSystemConfiguration` (`expected:<673> but was:<0>`) và `apply_logsTheSourcesAndTheChoice` thất bại; đặt `panel = 0` thì `physicalPanel_isOneOfTheSources` (`expected:<486> but was:<0>`) và `apply_logsTheSourcesAndTheChoice` thất bại. Nguồn `maximumWindowMetrics` không đột biến được trên Robolectric (0 x 0), ghi ở ngoại lệ.
  5. Robolectric với `w486dp-h673dp-xxhdpi` (bản trả danh sách, trước khi có `SmallestWidths`): `screenSmallestWidthsDp` là `[486, 486, 1458]` khi đọc mật độ gốc của Robolectric (160), `defaultDisplay.mode` là 1458 x 2019; sau `updateConfiguration` của `Resources.getSystem()` thành 673: cấu hình hệ thống 673, của activity vẫn 486, danh sách `[486, 673, 1458]`. `activity.display` ném `UnsupportedOperationException` ("Tried to obtain display from a Context not associated with one") trên Robolectric: vì thế code đọc `windowManager.defaultDisplay.mode` trong `runCatching`.
  6. Code của mục 3.1 và test của mục 4, `scripts/check.sh` trên nhánh: lần 1 lỗi `ktlint` (dòng quá 120 ký tự, bản đầu của `screenOrientationFor` và của test `undefinedWidth_isIgnored`); đã sửa, khối ở mục 3.1 là bản đã qua `ktlint`. Lần 2 (bản ba nguồn, 8 test): `Cổng chất lượng: QUA`, 1072 test JVM, 0 lỗi. Lần 3 (bản bốn nguồn, 11 test): `Cổng chất lượng: QUA`, 1075 test JVM, 0 lỗi. Lần 4 (bản của mục 3.1, có log, 12 test): `Cổng chất lượng: QUA`, 1076 test JVM, 0 lỗi (`:app` 52 test), 95 ảnh screenshot không đổi.
- **Lệnh:** `scripts/check.sh`; `./gradlew :app:testStagingDebugUnitTest --tests '*ScreenOrientationTest*'`; `./gradlew :feature:camera:testDebugUnitTest`, `:feature:poses:testDebugUnitTest` với test tạm; `./gradlew :feature:onboarding:testDebugUnitTest`, `:feature:settings:testDebugUnitTest`, `:feature:loading:testDebugUnitTest` với test tạm; `scripts/device-scenario.py check app/src/deviceScenarios/T-056-man-lon-xoay-ngang.txt` (bản có `device rotate landscape` thứ hai, qua).
- **Kết quả:** như trên. Chưa chạy trên emulator hay máy thật: máy này không còn emulator (đã gỡ theo yêu cầu của Tuấn sau T-049), và `device-session.yml` chỉ chạy trên nhánh chính. Phương án dự phòng mục 3.4 chưa thử.

## 5. Chấm rủi ro

**Điểm chặn bị chạm:** Không có (không thêm quyền, thư viện, quảng cáo, sự kiện hay dữ liệu; manifest không đổi ở phương án chính).

| Tiêu chí | Điểm (0 đến 3) | Lý do |
| --- | --- | --- |
| A. Phạm vi thay đổi | 3 | Nhiều module: code ở `:app`, chú thích ở `:feature:camera`, test ở `:app`, `:feature:camera`, `:feature:poses`, `:feature:onboarding`, `:feature:settings`, `:feature:loading`, và hai script của `device-session` |
| B. Độ rõ của yêu cầu | 2 | Spec camera đổi, chờ xác nhận; câu 15 (camera nằm ngang) còn mở; không có thiết kế cho màn lớn nằm ngang |
| C. Khả năng kiểm chứng tự động | 2 | Cách chọn hướng có test; đóng khung của hệ thống chỉ kiểm trên emulator (sau phiên) và máy thật |
| D. Ảnh hưởng chéo | 3 | Đổi hướng màn hình của cả app, mọi feature dựa vào |
| E. Độ mới của kỹ thuật | 3 | Đặt hướng lúc chạy chưa có trong dự án; cách hệ thống thu kích thước của app bị đóng khung ít tài liệu, chưa kiểm được trên máy |
| **Tổng** | **13** | **Mức: Cao** (duyệt chéo bắt buộc) |

## 6. Ảnh hưởng chéo

- File hoặc thành phần dùng chung bị chạm: `MainActivity.kt` (vỏ app); test của `:feature:camera`, `:feature:poses`, `:feature:onboarding`, `:feature:settings`, `:feature:loading`, chỉ thêm hàm test; `CameraXController.kt`, chỉ chú thích; `scripts/device-scenario.py`, `scripts/device-session.sh`, `docs/ops/test-sessions.md`, chỉ số ngân sách.
- Task khác trong phiên có thể xung đột: plan T-054 (trình chạy kịch bản) cùng danh sách cũng sửa `scripts/device-scenario.py` và `scripts/tests/run.sh`, ở khối khác (nhận ra app bị giết theo Google Play services, đặt ngôn ngữ máy). T-056 chỉ đổi số mặc định của ngân sách trong `scripts/device-scenario.py` (dòng `env_seconds("DEVICE_SCENARIOS_BUDGET", 900)` và câu của chuỗi trợ giúp, tìm theo nội dung vì số dòng lệch sau khi T-054 merge), không sửa `scripts/tests/run.sh` (L-014: hai plan cùng sửa một file, khối riêng, chạy tuần tự, task sau cập nhật nhánh theo nhánh chính). T-054 có thể chạy lại một tệp kịch bản khi app bị giết: thời gian đó cũng nằm trong ngân sách; plan T-054 xét lại ngân sách 1100 nếu cần. `docs/ops/test-sessions.md` mục 5: T-054 cũng thêm nội dung sát đoạn Hạn chót; T-056 chỉ sửa hai câu về ngân sách. Task test bố cục bản dịch (nếu có) có thể sửa `CameraScreenUnfoldedTest.kt`, `PosesScreenTest.kt` hay test của màn chào, cài đặt: khối thêm vào là hàm test riêng.
- Đề xuất thứ tự chạy: T-054 trước T-056 (T-056 thêm tệp kịch bản thứ tư; `device-session` cuối phiên chạy cả hai thay đổi); trước hay sau task bản dịch đều được.

## 7. Điều kiện dừng riêng

- `phone_isPortrait` hay `foldedAgain_locksToPortrait` đỏ sau bước 4 (điện thoại không còn khóa dọc) mà không sửa được trong giới hạn sửa lỗi: dừng, vì đổi hành vi của mọi người dùng điện thoại.
- Chuyển sang phương án dự phòng ở mục 3.4 (bỏ khóa trong manifest, đổi kỳ vọng của `manifest_keepsPortrait_soPhonesStartWithoutARelaunch` thành `UNSPECIFIED`) không phải điều kiện dừng: plan đã duyệt cả phương án này; ghi vào "Lệch so với plan".
- Cần đổi `AndroidManifest.xml` ngoài dòng `screenOrientation` của phương án dự phòng, hay cần `configChanges`: dừng.
- Cần sửa `.github/workflows/device-session.yml` (ví dụ `timeout-minutes`): dừng.

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
