---
ticket: T-081
title: Bỏ `:feature:sample`, vị trí `sample_native_small` và cờ `feature_sample_enabled`
status: approved             # draft | approved | stopped | done
version: 1                   # tăng khi plan được sửa lại sau phản hồi hoặc sau khi dừng
risk_score:                  # tổng điểm theo risk-scoring.md, để trống nếu chạm điểm chặn
risk_level: high             # low | medium | high
token_estimate: 9.0M         # cỡ M theo số gợi ý của scripts/token-usage.py estimate (2026-10-31); không nâng tay
blocking_points: [ads, analytics, core]
run_order:                   # AI điền sau họp đánh giá rủi ro
reviewer: Tuấn               # người duyệt
cross_reviewer: Nhật         # bắt buộc khi risk_level là high
approved_at: 2026-10-31
lane:                        # để trống: phiên tự chạy; quick: đường làm nhanh có người theo dõi (ADR-0034)
---

# PLAN T-081: BỎ `:FEATURE:SAMPLE`, VỊ TRÍ `SAMPLE_NATIVE_SMALL` VÀ CỜ `FEATURE_SAMPLE_ENABLED`

## 1. Tóm tắt

Xóa module `:feature:sample` cùng mọi phần nối của nó:

- đích `SampleDestination` và `MainScreen.SAMPLE` của `:app`;
- cờ `FeatureFlag.SAMPLE` (`feature_sample_enabled`) và sự kiện `sample_increment`;
- vị trí `AdPlacement.SAMPLE_NATIVE_SMALL` (`sample_native_small`).

Sau loading, app mở màn chào lần đầu (khi cờ màn chào bật), mọi trường hợp còn lại mở camera, kể cả khi cờ `feature_camera_enabled` tắt (câu 1 ở mục 8). Khoảng 25 file test của `:core:ads` và một test của `:core:analytics` dùng vị trí mẫu chuyển sang `POSES_BOTTOM` (cùng định dạng `NATIVE_SMALL`, không chỉ có ở staging), giữ nguyên ý. Test của script đang đọc ảnh và chuỗi của màn mẫu đổi sang module còn lại. ADR-1008 (`proposed`, có sẵn trong PR plan này) thay vế "vị trí `gallery_*` và `sample_native_small`" của ADR-1007.

Người dùng không thấy gì khác: màn chính vẫn là màn chào hay camera; `ConfiguredAdUnitIds.missing` không còn đòi mã cho `sample_native_small`, nên danh sách mã cần tạo cho production (T-075) chỉ có vị trí của app.

Kiểm chứng: `MainScreenTest` viết lại theo luật mới, có đối chứng đã chạy; test có sẵn của khung qua với `POSES_BOTTOM`; `AdPlacementAppTest.noSamplePlacement`; bảng khóa sinh lại; `scripts/tests/run.sh` qua. Toàn bộ phần code đã chạy thử với `scripts/check.sh` (mục 4.1).

## 2. Hiểu yêu cầu

Ticket không xử lý kịch bản mới (`scenarios: []`). Nó dựa trên:

| Nguồn | Nội dung | Cách hiểu của AI |
| --- | --- | --- |
| `features/ads/spec.md` (confirmed), câu 4 | Bỏ `SAMPLE_BOTTOM_BANNER` của `feature/sample`; không mang `AdGalleryActivity` | Màn mẫu không có trong app; vị trí của nó (nay là `sample_native_small`) bỏ theo. Nhật quyết định ngày 2026-10-31 (ticket): production không có màn mẫu, không tạo mã AdMob cho vị trí đó |
| `features/loading/spec.md`, mục 2 | Chuẩn bị xong thì chuyển sang màn chào (lần đầu) hoặc camera; "màn hình chính" là một trong hai | `chooseMainScreen` chỉ còn hai kết quả: `ONBOARDING` khi màn chào áp dụng và chưa xong, `CAMERA` cho mọi trường hợp khác |
| `features/loading/plans/T-010-...md`, mục 8 câu 3 | Cờ camera tắt thì vào màn mẫu, "khi gỡ `:feature:sample` thì cần quyết định lại, ghi vào ticket đó" | Quyết định lại ở câu 1 mục 8 của plan này: mở camera, cờ camera không còn được đọc |
| ADR-0017 (mẫu) | Feature mẫu được xóa bằng một ticket khi feature thật đầu tiên thay màn bắt đầu | Đúng trường hợp này |
| ADR-1007 (app) | Giữ `gallery_*` và `sample_native_small` vì test của khung dùng | ADR-1008 thay vế này cho `sample_native_small`; `gallery_*` giữ |

Thiết kế tham chiếu: Không có thiết kế riêng (không đổi giao diện màn nào còn lại).

ADR liên quan: ADR-0001 (phụ thuộc module), ADR-0003 (cờ tính năng), ADR-0013, ADR-0014 (vị trí theo spec, mặc định tắt), ADR-0017 (bỏ feature mẫu), ADR-0023 (bảng khóa), ADR-0029 (dữ liệu của test cũ), ADR-0030 (đồng bộ dự án mẫu), ADR-0031 (test cũ trái plan), ADR-1007, ADR-1008 (mới).

## 3. Giải pháp

### 3.1. Cách làm

Một PR, nhánh `claude/T-081-bo-man-mau` từ nhánh chính **sau T-080** (mục 6). Code dưới đây là code đã chạy thử (mục 4.1), viết trên nhánh chính trước T-080.

**Module.**

- `git rm -r feature/sample` (14 file nguồn và test, 3 ảnh chuẩn `SampleScreen_*.png`, `consumer-rules.pro`, `robolectric.properties`).
- `settings.gradle.kts` bỏ `include(":feature:sample")`; `app/build.gradle.kts` bỏ `implementation(project(":feature:sample"))`.
- Không file nào khác đọc `feature/sample` ngoài các file ở mục 3.2 (`grep -rn "feature/sample\|feature:sample\|feature.sample" --exclude-dir=build . | grep -v "^./docs/\(runs\|features/.*/plans\|reports\|adr\)"`, 2026-10-31). `.github/` không nhắc module này.

**Điều hướng của `:app` (câu 1).**

`MainScreen.kt`:

```kotlin
/** The screens the app can open after loading (spec loading, section 2). */
internal enum class MainScreen { ONBOARDING, CAMERA }

/** Onboarding is available when its destination is registered in [AppNavHost] and its flag is on. */
internal data class MainScreenInputs(val onboardingAvailable: Boolean, val onboardingDone: Boolean)

/**
 * Onboarding the first time, then the camera (spec loading, section 2). The camera is the screen of every other case:
 * the app has no other main screen since the sample screen left it (plan T-081, ADR-1008).
 */
internal fun chooseMainScreen(inputs: MainScreenInputs): MainScreen = when {
    inputs.onboardingAvailable && !inputs.onboardingDone -> MainScreen.ONBOARDING
    else -> MainScreen.CAMERA
}
```

`MainScreenChooser.inputs()` chỉ đọc cờ màn chào. Trước đây màn chào cần cả cờ camera, vì màn chào chỉ dẫn tới camera (plan T-012, câu 2). Nay camera luôn có, nên điều kiện đó không còn nghĩa:

```kotlin
     * The camera is always there, so onboarding only needs its own flag; feature_camera_enabled is not read (plan
     * T-081, question 1). The stored status is read only when onboarding is available; a read error shows onboarding
     * rather than skip it (plan T-012, question 3).
     */
    private suspend fun inputs(): MainScreenInputs {
        val onboarding = featureFlags.enabled(FeatureFlag.ONBOARDING).first()
        val onboardingDone = onboarding && readOnboardingDone()
        return MainScreenInputs(onboardingAvailable = onboarding, onboardingDone = onboardingDone)
    }
```

`AppNavHost.kt` bỏ hai dòng import `feature.sample.navigation`, dòng `sampleScreen()` và nhánh `MainScreen.SAMPLE -> SampleDestination` của `destination()`. Luồng `scope.launch(Dispatchers.Main.immediate)` của `loadingScreen` không đổi.

**Cờ `feature_camera_enabled` sau task.** Không code nào đọc cờ nữa. `FeatureFlag.CAMERA` và dòng của nó trong `StagingFeatureFlagsModule` giữ nguyên trong task này; chỉ mô tả của cờ đổi (nói cờ không còn tác dụng), nên dòng trong bảng khóa đổi mô tả. Cờ bỏ hẳn ở ticket T-088 (Nhật quyết 2026-10-31; spec camera mục 8 đã sửa).

**Cờ `feature_sample_enabled`.**

- `FeatureFlag.kt` bỏ dòng `SAMPLE(...)`; đổi mô tả của `CAMERA` thành `"No longer read: the camera is always the start screen (T-081); removed in T-088."` (spec camera mục 8, sửa 2026-10-31); `StagingFeatureFlagsModule` bỏ hàm `sample()`.
- `DefaultFeatureFlagsTest`: bốn test dùng `FeatureFlag.SAMPLE` làm cờ mẫu đổi sang `FeatureFlag.LOADING` (cờ của dự án mẫu, còn ở cả hai phía). Câu kiểm giữ nguyên (ADR-0029).

**Vị trí `sample_native_small`.**

- `AdPlacement.kt` bỏ dòng `SAMPLE_NATIVE_SMALL(...)` và KDoc của nó.
- `StagingRemoteValuesModule.kt`: chú thích "the gallery and sample placements of the framework stay off" thành "the gallery placements of the framework stay off".
- Đổi tên cơ học trong các file test ở mục 3.2 (nhóm "Đổi tên"): `SAMPLE_NATIVE_SMALL` thành `POSES_BOTTOM`, chuỗi `sample_native_small` thành `poses_bottom`. Ví dụ: `"Bottom bar sample_native_small: off"` thành `"Bottom bar poses_bottom: off"`; `"ads_sample_native_small_enabled"` (tên khóa cũ đợt 2 mà khung không được đọc, `AdsFlagsTest`) thành `"ads_poses_bottom_enabled"`. Bản chạy thử làm bằng một script Python thay chuỗi trong 26 file, rồi sửa tay bốn chỗ dưới.
- **Luật chung khi đổi tên làm trùng (rà soát độc lập):** đổi tên làm trùng một mục `poses_bottom` đã có (trong chuỗi cấu hình, danh sách mong đợi, tập khóa) thì bỏ mục `sample_*`, không đổi tên (dữ liệu, ADR-0029). Áp cả cho test mà ticket chạy trước thêm vào:
  - T-084 (plan approved, chạy trước ticket này): `AdUnitIdsTest.missingLayers_listEveryBottomBarLayerWithoutUnit` có `sample_native_small/banner`, `sample_native_small/native_collap` cạnh `poses_bottom/...`. Bỏ hai mục `sample_native_small/...`.
  - T-083 (plan nháp): `StartupAdChoiceTest` dùng `SAMPLE_NATIVE_SMALL`; đổi tên theo luật trên.
  - Phiên tìm theo chuỗi (`git grep -n "SAMPLE_NATIVE_SMALL\|sample_native_small"`), không theo số dòng: T-080, T-084 sửa cùng file nên số dòng lệch.
- Sửa tay (không phải đổi tên đơn thuần):
  - `AdUnitIdsTest.configuredUnits_areParsedPerPlacementKey`: hai chuỗi cấu hình đã có `poses_bottom=unit/8`, đổi tên sẽ cho khóa trùng. Chuỗi mới (trên nhánh chính trước T-080; sau T-080 giữ thêm `;poses_in_collection=unit/12` mà T-080 thêm vào cuối hai chuỗi):

    ```kotlin
            " poses_bottom = unit/1 ; other_key=unit/2;open_app=unit/3;open_app_inter=unit/4;" +
                "native_combo=unit/5;resume=unit/6;resume_inter=unit/7;" +
                // The other placements of this app (plan T-074, ADR-0029; plan T-081).
                "pose_detail_bottom=unit/9;review_interstitial=unit/10;inter_pose_detail=unit/11",
        )
        assertEquals("unit/1", ids.of(AdPlacement.POSES_BOTTOM))
    ```

    Chuỗi `withoutAppWide` thành `" poses_bottom = unit/1 ; other_key=unit/2;" + "pose_detail_bottom=unit/9;review_interstitial=unit/10;inter_pose_detail=unit/11"`. Khoảng trắng quanh `=` và `;` của khóa đầu giữ, để test vẫn kiểm việc cắt khoảng trắng.
  - `AdPlacementAppTest.noSampleBottomBanner` thành `noSamplePlacement` (câu viết sẵn ở mục 3.5).
  - `StagingRemoteValuesModuleTest.noGalleryNorSamplePlacement_isShown` thành `noGalleryPlacement_isShown` (mục 3.5).
  - `AdsSettingsTest.all_isDerivedFromDeclaredPlacements`: chú thích "with the switch of the sample placement" thành "with the switch of the library bottom bar".
- `FirebaseAdMetricsTest` (`:core:analytics`) chỉ dùng chuỗi `"sample_native_small"` làm giá trị `ad_placement`: đổi thành `"poses_bottom"`.

**Sau T-080.** T-080 thêm test vào `InListAdsTest`, `AdsControllerNativeComboTest`, `PoseCardTest`, `PosesScreenTest`. Theo plan T-080 (mục 4), test của khung dùng `GALLERY_IN_COLLECTION` và `GALLERY_INLIST_MEDIUM`, không dùng `SAMPLE_NATIVE_SMALL`. Phần T-080 chạm vị trí mẫu chỉ là hai chuỗi của `AdUnitIdsTest` (thêm `poses_in_collection=unit/12`). Dù vậy, phiên chạy `grep -rln "SAMPLE_NATIVE_SMALL\|sample_native_small" core app feature` sau khi tạo nhánh và đổi tên mọi file tìm thấy theo cùng luật; file test nào ngoài mục 3.2 thì theo ADR-0029 (dữ liệu của test cũ).

**Bảng khóa.** `./gradlew :app:testStagingDebugUnitTest -PremoteConfigKeys.update=true`. Chạy thử: `docs/remote-config-keys.md` mất đúng hai dòng (`feature_sample_enabled`, `is_show_sample_native_small`) và dòng `feature_camera_enabled` đổi mô tả (phiên bản sau rà soát; chạy thử là bản trước khi đổi mô tả); `docs/remote-config-template.json` mất hai khối (10 dòng). Không sửa tay.

**Script và test của script (L-075, đã chạy, mục 4.1).**

- `scripts/ads-plan.py`:
  - `KNOWN_PLACEMENTS = {}` kèm chú thích "camera-pose has no sample screen (ADR-1008), so none";
  - bỏ mục `feature_sample_enabled` của bảng khóa và mục `sample_increment` của `KNOWN_EVENTS`. Hai bảng chỉ để tra chữ; không còn khóa hay sự kiện đó trong code.
- `scripts/failure-kind.sh`, dòng 71 (chú thích ví dụ): `':feature:sample:testDebugUnitTest'` thành `':feature:settings:testDebugUnitTest'`.
- `scripts/tests/playbook/none.log`: `:feature:sample:compileDebugKotlin` thành `:feature:settings:compileDebugKotlin`, `feature/sample/SampleViewModel.kt` thành `feature/settings/SettingsViewModel.kt`. Tệp là log giả cho trường hợp "không khớp mục nào"; tên module không ảnh hưởng.
- `scripts/tests/run.sh`:
  - khối `DesignCompare` (dòng 1001 tới 1005): `feature/sample/src/test/screenshots/SampleScreen_ready_light.png` thành `feature/settings/src/test/screenshots/SettingsScreen_default_light.png`, `SampleScreen_ready_dark.png` thành `SettingsScreen_default_dark.png`. Đây là một cặp sáng và tối có thật của cùng màn hình;
  - bốn log giả của `failure-kind` (dòng 513 tới 523): `:feature:sample:` thành `:feature:settings:`;
  - test `str=` (dòng 3064): `'tap str=sample_increment' 'tap str=sample_title'` thành `'tap str=poses_chip_all' 'tap str=poses_title'`; test `selected=true` (dòng 3067): `'expect text=Tap selected=true'` thành `'expect text=All selected=true'`;
  - chú thích dòng 3196 "runs on the sample feature" thành "runs on the pose library"; số dòng mong đợi của ví dụ `test-sessions.md` (dòng 3198) từ `"0 11"` thành `"0 13"`.
- `scripts/tests/fixtures/device-scenario/grid.xml`: node chữ `text="Tap"` thành `text="All"` (giá trị của `poses_chip_all`); `content-desc="Sample feature"` thành `content-desc="Pose library"` (giá trị của `poses_title`). Tọa độ không đổi, nên hai câu kiểm tọa độ (`140 300`, `325 445`) giữ. `str=` đọc `strings.xml` của repo thật, nên chuỗi phải có trong một module còn lại.
- `docs/ops/test-sessions.md`, mục 5, ví dụ ngắn chạy trên màn mẫu (`sample_screen`, `sample_increment`). `run.sh` chạy `device-scenario.py check` trên ví dụ này, check đòi tag và chuỗi có trong mã nguồn. Ví dụ mới (13 dòng, đã qua check):

  ```text
  ticket: T-NNN
  criterion: Chọn "Đã lưu" ở thư viện tư thế thì nút lọc được chọn, xoay màn hình vẫn giữ lựa chọn
  timeout: 120
  fresh
  consent decline 30
  wait id=onboarding_screen | id=camera_screen 60
  tap_if id=onboarding_enable_camera
  tap id=camera_poses
  wait id=poses_grid 20
  tap str=poses_chip_saved in=poses_screen
  device rotate landscape
  expect str=poses_chip_saved selected=true
  shot landscape
  ```

- Không đổi: test của `release.sh` trong `run.sh` (dòng 1092, 1094) dùng chuỗi `sample_bottom_banner` làm dữ liệu cấu hình, không đọc `AdPlacement`, và `scripts/release.sh` giữ ở mốc cũ (ADR-1007, T-075). `scripts/tests/fixtures/device-scenario/` khác không nhắc màn mẫu.

**Hướng dẫn và tài liệu.**

- `CLAUDE.md`, mục Viết một feature mới, dòng 60: "Làm theo đúng mẫu `:feature:sample`:" thành câu ở câu 2 mục 8. `:feature:settings` có đủ bước 1 (chỉ plugin `template.android.feature` và `namespace`, cộng một phụ thuộc `:core:data` như màn mẫu), 2 (interface `AppVersionProvider`, `LanguageSettingsMode` trong `data/`, lớp cài đặt `internal`, gắn bằng `SettingsModule` trong `di/`), 3 (`SettingsUiState` sealed), 5 (`SettingsRoute`, `SettingsScreen`), 6 (`navigation/`), 10 (`MainDispatcherRule`, test Robolectric, ảnh chuẩn có `_small`). Thiếu ở bước 4: `SettingsViewModel` không nhận `Analytics`, `FeatureFlags` (spec settings không có sự kiện; cờ do `:app` đọc), và `data/` không có repository đọc ghi (dữ liệu ở `UserPreferences` của `:core:data`). `:feature:loading` có repository `PreparationRepository` với lớp cài đặt `internal`, nhận `Analytics`, `AppLogger`, `FeatureFlags`, gọi `RemoteAssets` (bước 8), nhưng đọc cờ một lần bằng `first()` (có chủ đích, plan T-001 câu 3). Nên đề xuất dẫn cả hai.
- `CLAUDE.md`, bảng module: không có dòng `:feature:sample` riêng (dòng `:feature:<ten>` chung), không đổi.
- `docs/ops/ads-guide.md`:
  - dòng 102: "như `bottomBar` của `SampleScreen` (mặc định `BottomAdBarScaffold`)" thành "như thanh đáy của `PosesScreen` (`BottomAdBarScaffold(AdPlacement.POSES_BOTTOM)`; ảnh chuẩn vẽ `BottomAdBarLayout` với `NativeAdCard` giả, T-079)";
  - dòng 128: "như `SampleScreenScreenshotTest`" thành "như `PosesScreenScreenshotTest`";
  - dòng 157: `Native sample_native_small retry in 5000 ms` thành `Native poses_bottom retry in 5000 ms`.
- `docs/template-sync.txt`: thêm hai dòng chú thích (chỉ chú thích, không đổi `base=`, `hold=`):

  ```
  # feature/sample, FeatureFlag.SAMPLE and AdPlacement.SAMPLE_NATIVE_SMALL are gone (T-081, ADR-1008): a sync drops them,
  # and template tests of core/ads that use SAMPLE_NATIVE_SMALL take POSES_BOTTOM.
  ```

- Không sửa: `docs/ops/to-app.md` (cột "Cả hai cùng sửa" nhắc `feature/sample` là của dự án mẫu, mẫu vẫn có module này); `docs/ops/ads-business-rules.md` mục MC-01 và ví dụ khóa (nói về dự án mẫu); `docs/ops/release.md` (giữ mốc cũ); spec ads, spec loading (confirmed, câu 4 mục 8); plan, nhật ký, báo cáo cũ; ADR-1007 (accepted).
- ADR-1008 và `docs/foundation.md` sinh lại đã có trong PR plan này (`status: proposed`). Người duyệt chấp nhận ADR khi duyệt plan, rồi chạy `scripts/foundation.py`; phiên không sửa ADR.

**Sau khi merge (việc của người, không nằm trong PR).** Khóa `feature_sample_enabled`, `is_show_sample_native_small` còn trên Firebase staging hay production thì người xóa tay nếu muốn; app không đọc nữa. Chạy lại `python3 scripts/ads-plan.py --keep <file trên Drive>`: dòng `sample_native_small` mất khỏi danh sách mã cần tạo của T-075.

**Đề xuất về dự án mẫu (mô tả PR, `docs/ops/from-app.md`).** Test của khung dùng một vị trí riêng của test (`AdPlacementDeclaration` đã cho test tự khai báo) hay vị trí `gallery_*`; test của script dùng tệp mẫu trong `scripts/tests/fixtures/` thay cho ảnh và chuỗi của `feature/sample`. Khi đó app bỏ màn mẫu mà không phải sửa test của mẫu.

### 3.2. Danh sách file

| Thao tác | Đường dẫn | Nội dung thay đổi |
| --- | --- | --- |
| Xóa | `feature/sample/` | Cả thư mục: build.gradle.kts, consumer-rules.pro, 7 file Kotlin của src/main (SampleScreen, SampleUiState, SampleViewModel, data/CounterRepository, data/DataStoreCounterRepository, di/SampleModule, navigation/SampleNavigation), src/main/res/values/strings.xml, 5 file Kotlin của src/test (MainDispatcherRule, SampleScreenScreenshotTest, SampleScreenTest, SampleViewModelTest, data/DataStoreCounterRepositoryTest), robolectric.properties, src/test/screenshots/.gitkeep |
| Xóa | `feature/sample/src/test/screenshots/SampleScreen_disabled_light.png` | Ảnh chuẩn của màn mẫu |
| Xóa | `feature/sample/src/test/screenshots/SampleScreen_ready_dark.png` | Ảnh chuẩn của màn mẫu |
| Xóa | `feature/sample/src/test/screenshots/SampleScreen_ready_light.png` | Ảnh chuẩn của màn mẫu |
| Sửa | `settings.gradle.kts` | Bỏ `include(":feature:sample")` |
| Sửa | `app/build.gradle.kts` | Bỏ `implementation(project(":feature:sample"))` |
| Sửa | `app/src/main/kotlin/com/vglstudio/camera/pose/line/guide/navigation/MainScreen.kt` | `MainScreen { ONBOARDING, CAMERA }`, bỏ `cameraAvailable`, `else -> CAMERA` |
| Sửa | `app/src/main/kotlin/com/vglstudio/camera/pose/line/guide/navigation/MainScreenChooser.kt` | Chỉ đọc cờ màn chào |
| Sửa | `app/src/main/kotlin/com/vglstudio/camera/pose/line/guide/navigation/AppNavHost.kt` | Bỏ `sampleScreen()`, `SampleDestination`, nhánh `SAMPLE` |
| Sửa | `app/src/test/kotlin/com/vglstudio/camera/pose/line/guide/navigation/MainScreenTest.kt` | Viết lại theo mục 3.5 và 4; `FakeFeatureFlags` ghi các cờ đã đọc |
| Sửa | `app/src/staging/kotlin/com/vglstudio/camera/pose/line/guide/di/StagingFeatureFlagsModule.kt` | Bỏ `sample()` |
| Sửa | `app/src/staging/kotlin/com/vglstudio/camera/pose/line/guide/di/StagingRemoteValuesModule.kt` | Chú thích: bỏ "and sample" |
| Sửa | `app/src/testStaging/kotlin/com/vglstudio/camera/pose/line/guide/di/StagingRemoteValuesModuleTest.kt` | `noGalleryPlacement_isShown` (mục 3.5) |
| Sửa | `core/config/src/main/kotlin/com/vglstudio/camera/pose/line/guide/core/config/FeatureFlag.kt` | Bỏ `SAMPLE`; đổi mô tả của `CAMERA` (cờ không còn tác dụng) |
| Sửa | `core/config/src/test/kotlin/com/vglstudio/camera/pose/line/guide/core/config/DefaultFeatureFlagsTest.kt` | `FeatureFlag.SAMPLE` thành `FeatureFlag.LOADING` (dữ liệu, ADR-0029) |
| Sửa | `core/ads/src/main/kotlin/com/vglstudio/camera/pose/line/guide/core/ads/AdPlacement.kt` | Bỏ `SAMPLE_NATIVE_SMALL` và KDoc |
| Sửa | `core/ads/src/test/kotlin/com/vglstudio/camera/pose/line/guide/core/ads/AdUnitIdsTest.kt` | Sửa tay hai chuỗi cấu hình (mục 3.1), còn lại đổi tên |
| Sửa | `core/ads/src/test/kotlin/com/vglstudio/camera/pose/line/guide/core/ads/AdPlacementAppTest.kt` | `noSamplePlacement` (mục 3.5) |
| Sửa | `core/ads/src/test/kotlin/com/vglstudio/camera/pose/line/guide/core/ads/AdPlacementTest.kt` | Đổi tên (`otherPlacements_areNotStagingOnly`, `StartupAdTurn` từ chối vị trí không phải app-wide) |
| Sửa | `core/ads/src/test/kotlin/com/vglstudio/camera/pose/line/guide/core/ads/AdsSettingsTest.kt` | Đổi tên; chú thích của `all_isDerivedFromDeclaredPlacements` |
| Sửa | `core/ads/src/test/kotlin/com/vglstudio/camera/pose/line/guide/core/ads/AdTelemetryRulesTest.kt` | Đổi tên |
| Sửa | `core/ads/src/test/kotlin/com/vglstudio/camera/pose/line/guide/core/ads/AdTelemetryTest.kt` | Đổi tên (30 chỗ, kể cả hằng `NATIVE_SLOT`) |
| Sửa | `core/ads/src/test/kotlin/com/vglstudio/camera/pose/line/guide/core/ads/AdsFlagsTest.kt` | Đổi tên, kể cả `ads_poses_bottom_enabled` |
| Sửa | `core/ads/src/test/kotlin/com/vglstudio/camera/pose/line/guide/core/ads/BottomAdBarTest.kt` | Đổi tên, kể cả chuỗi log `Bottom bar poses_bottom: ...` |
| Sửa | `core/ads/src/test/kotlin/com/vglstudio/camera/pose/line/guide/core/ads/FakeNativeAds.kt` | `FAKE_KEY = "poses_bottom"` |
| Sửa | `core/ads/src/test/kotlin/com/vglstudio/camera/pose/line/guide/core/ads/FreshAdCacheTest.kt` | Đổi tên |
| Sửa | `core/ads/src/test/kotlin/com/vglstudio/camera/pose/line/guide/core/ads/FullScreenAdsTest.kt` | Đổi tên |
| Sửa | `core/ads/src/test/kotlin/com/vglstudio/camera/pose/line/guide/core/ads/InListAdsStateTest.kt` | Đổi tên |
| Sửa | `core/ads/src/test/kotlin/com/vglstudio/camera/pose/line/guide/core/ads/InListAdsTest.kt` | Đổi tên (5 chỗ; test mới của T-080 không dùng vị trí mẫu) |
| Sửa | `core/ads/src/test/kotlin/com/vglstudio/camera/pose/line/guide/core/ads/LoadAttemptCounterTest.kt` | Đổi tên |
| Sửa | `core/ads/src/test/kotlin/com/vglstudio/camera/pose/line/guide/core/ads/NativeAdCellTest.kt` | Đổi tên |
| Sửa | `core/ads/src/test/kotlin/com/vglstudio/camera/pose/line/guide/core/ads/NativeAdLoadingTest.kt` | Đổi tên |
| Sửa | `core/ads/src/test/kotlin/com/vglstudio/camera/pose/line/guide/core/ads/NativeAdPoolsTest.kt` | Đổi tên |
| Sửa | `core/ads/src/test/kotlin/com/vglstudio/camera/pose/line/guide/core/ads/NativeAdRefreshTest.kt` | Đổi tên |
| Sửa | `core/ads/src/test/kotlin/com/vglstudio/camera/pose/line/guide/core/ads/NativeAdRetryTest.kt` | Đổi tên |
| Sửa | `core/ads/src/test/kotlin/com/vglstudio/camera/pose/line/guide/core/ads/NativeAdSlotTest.kt` | Đổi tên |
| Sửa | `core/ads/src/test/kotlin/com/vglstudio/camera/pose/line/guide/core/ads/NativeAdSourceTest.kt` | Đổi tên |
| Sửa | `core/ads/src/test/kotlin/com/vglstudio/camera/pose/line/guide/core/ads/NativeAdSourcesTest.kt` | Đổi tên |
| Sửa | `core/ads/src/test/kotlin/com/vglstudio/camera/pose/line/guide/core/ads/RewardedActionTest.kt` | Đổi tên |
| Sửa | `core/ads/src/test/kotlin/com/vglstudio/camera/pose/line/guide/core/ads/SdkBannerAdViewTest.kt` | Đổi tên |
| Sửa | `core/ads/src/test/kotlin/com/vglstudio/camera/pose/line/guide/core/ads/StartupAdChoiceTest.kt` | Đổi tên, kể cả `is_show_poses_bottom` |
| Sửa | `core/analytics/src/test/kotlin/com/vglstudio/camera/pose/line/guide/core/analytics/FirebaseAdMetricsTest.kt` | Chuỗi `ad_placement` thành `poses_bottom` |
| Sửa | `docs/remote-config-keys.md` | Sinh lại: mất `feature_sample_enabled`, `is_show_sample_native_small`; mô tả mới của `feature_camera_enabled` |
| Sửa | `docs/remote-config-template.json` | Sinh lại cùng lệnh |
| Sửa | `scripts/ads-plan.py` | `KNOWN_PLACEMENTS = {}`; bỏ mục `feature_sample_enabled`, `sample_increment` |
| Sửa | `scripts/failure-kind.sh` | Chú thích dòng 71 |
| Sửa | `scripts/tests/run.sh` | Ảnh của `DesignCompare`, log giả của `failure-kind`, chuỗi `str=`, chữ `selected=true`, số dòng ví dụ |
| Sửa | `scripts/tests/fixtures/device-scenario/grid.xml` | `text="All"`, `content-desc="Pose library"` |
| Sửa | `scripts/tests/playbook/none.log` | Tên module trong log giả |
| Sửa | `docs/ops/test-sessions.md` | Ví dụ ngắn của mục 5 trên thư viện tư thế |
| Sửa | `docs/ops/ads-guide.md` | Dòng 102, 128, 157 (ví dụ màn mẫu) |
| Sửa | `CLAUDE.md` | Mục Viết một feature mới, dòng 60 (câu 2 mục 8) |
| Sửa | `docs/template-sync.txt` | Hai dòng chú thích |

ADR-1008 (`docs/adr/1008-khong-co-man-mau.md`) và `docs/foundation.md` đã có trong PR plan này; phiên không sửa.

Nơi khác đọc các file bị đổi (L-020 của mẫu):

- `RemoteConfigKeysTest` so hai bảng khóa với `FeatureFlag.entries`, `AdsSettings.all()`: bảng sinh lại trong cùng commit code nên qua (chạy thử).
- `scripts/release.sh` đọc `AdPlacement.kt` theo từng dòng: chỉ mất một dòng; không chạy trong task (ADR-0009). `scripts/status.py`, mục 6 của trang tiến độ, đọc `AdPlacement.kt` cùng luật: sẽ không còn liệt kê `sample_native_small`.
- `scripts/check.sh` đọc `*/src/test/screenshots/`: ba ảnh bị xóa cùng module, không còn test nào ghi chúng.
- `app/src/androidTest` (`AppLaunchTest`, `SettingsNavigationTest`) chờ `onboarding_screen` hay `camera_screen`, không nhắc màn mẫu. Kịch bản emulator (`*/src/deviceScenarios/*.txt`) không nhắc màn mẫu.
- `scripts/existing-tests.py`: xem mục 3.5.
- `.github/workflows/pr-check.yml` nhắc `settings.gradle.kts` và `scripts/tests/run.sh` (đổi file build thì build bản release; chạy test của script): không đổi cách đọc, PR của task chạy các bước đó như thường. Bỏ một module không thêm thư viện hay luật R8.
- `.github/workflows/main-guard.yml` và `scripts/push-probe.sh` gọi `scripts/failure-kind.sh`: plan chỉ sửa một dòng chú thích của script, không đổi hành vi.
- `.github/workflows/device-session.yml` và `scripts/device-session.sh` dẫn `docs/ops/test-sessions.md`: chỉ đổi ví dụ ngắn của mục 5, không đổi cú pháp hay tham số mà hai file đọc.
- `scripts/tests/app.sh` được `scripts/tests/run.sh` nạp; `scripts/local-setup.sh` nhắc `run.sh` và `CLAUDE.md` trong hướng dẫn: plan không đổi cách nạp hay tên file.
- `CLAUDE.md` được nhắc trong chú thích của `app/src/androidTest/kotlin/com/vglstudio/camera/pose/line/guide/AppLaunchTest.kt`, `app/src/androidTest/kotlin/com/vglstudio/camera/pose/line/guide/SettingsNavigationTest.kt`, `core/ads/src/test/kotlin/com/vglstudio/camera/pose/line/guide/core/ads/NativeFullScreenOverlayScreenshotTest.kt` và trong `scripts/run-watchdog.py`: các chỗ đó dẫn mục khác (test trên emulator, Roborazzi, lệnh), plan chỉ đổi dòng 60 của mục Viết một feature mới.
- `scripts/setup-check-app.sh` nhắc `docs/template-sync.txt` ở chú thích; `scripts/setup-check.sh` đọc `app/build.gradle.kts` để lấy cấu hình của app (tên gói, tên miền): plan chỉ thêm chú thích vào `template-sync.txt` và bỏ một dòng `implementation` của `app/build.gradle.kts`, không chạm phần hai script đọc.
- `app/src/test/kotlin/com/vglstudio/camera/pose/line/guide/config/RemoteConfigKeysTest.kt` so `docs/remote-config-keys.md`, `docs/remote-config-template.json` với code: hai file sinh lại trong commit code nên test qua (dòng đầu của danh sách này).

### 3.3. Cờ tính năng, quảng cáo, dữ liệu, quyền, thư viện

- Cờ tính năng: bỏ `feature_sample_enabled` (`FeatureFlag.SAMPLE`). `feature_camera_enabled` không còn được đọc, giữ khai báo (câu 1).
- Quảng cáo: bỏ vị trí `sample_native_small` (spec ads câu 4; ticket). Không thêm vị trí. Vị trí `gallery_*` giữ theo ADR-1007.
- Sự kiện phân tích: bỏ `sample_increment` cùng màn mẫu (`SampleViewModel.EVENT_INCREMENT`). Không thêm, không đổi sự kiện nào khác. Bỏ một sự kiện là điểm chặn 4 (câu 3).
- File R2: Không chạm.
- **Quyền truy cập:** Không thêm. Màn mẫu không khai báo quyền nào (`feature/sample` không có `AndroidManifest.xml`). Không thêm: `CAMERA` (plan chỉ nhắc cờ `FeatureFlag.CAMERA`; quyền camera đã có từ T-013, không đổi).
- **Thư viện và SDK:** Không thêm. Bỏ một module nội bộ; `:feature:sample` chỉ phụ thuộc `:core:data` và plugin `template.android.feature`, các module khác vẫn dùng chúng.

### 3.4. Phương án dự phòng

Không có. Mọi phần đã chạy thử với `scripts/check.sh` (mục 4.1). Phần còn chưa chạy là bản sau T-080, chỉ khác ở dữ liệu của `AdUnitIdsTest` và các test mới mà T-080 thêm; mục 3.1 ghi luật đổi tên cho chúng.

### 3.5. Hành vi đã có bị đổi

| Hành vi cũ | Hành vi mới (câu của mục 3.1) | Test có sẵn khẳng định hành vi cũ | Giữ, hay câu khẳng định mới |
| --- | --- | --- | --- |
| Màn chào không áp dụng (đã xong, hay cờ tắt) và cờ camera tắt thì mở màn mẫu | Mọi trường hợp không phải màn chào mở camera | `MainScreenTest.onboardingAvailableButDone_cameraUnavailable_choosesSample`, `nothingAvailable_choosesSample`, `chooserBeforeOnboardingAndCamera_choosesSample`, `chooser_cameraFlagOff_choosesSample` | Viết lại (test viết trước của plan, không phải lượt ADR-0031 của phiên): `onboarded_choosesCamera`: `choose(onboarding = true, done = true) == CAMERA`; `onboardingUnavailable_choosesCamera`: `choose(false, false)` và `choose(false, true)` đều `CAMERA`; `chooserWithNoFlag_choosesCamera`: không cờ nào bật, chưa qua màn chào, `chooser.choose() == CAMERA`; `chooser_cameraFlagOff_choosesCamera`: chỉ cờ màn chào bật, đã qua màn chào, `choose() == CAMERA` và các cờ đã đọc đúng bằng `[ONBOARDING]`. Thêm `everyScreen_isOnboardingOrCamera`: `MainScreen.entries == [ONBOARDING, CAMERA]` |
| Màn chào cần cả cờ màn chào và cờ camera (plan T-012, câu 2) | Màn chào chỉ cần cờ của nó | `MainScreenTest.chooser_onboardingOnCameraOff_choosesSample` (khẳng định `SAMPLE`, `status.reads == 0`) | Viết lại thành `chooser_onboardingOnCameraOff_choosesOnboarding`: chỉ cờ màn chào bật, chưa qua màn chào: `ONBOARDING`, `status.reads == 1` |
| `MainScreenInputs` có `cameraAvailable` | Không còn trường này | `MainScreenTest.notOnboardedAndOnboardingAvailable_choosesOnboarding` (gọi `choose(..., camera = true/false)`), `onboardedAndCameraAvailable_choosesCamera`, `onboardingUnavailable_cameraAvailable_choosesCamera` | Hàm phụ `choose(onboarding, done)` bỏ tham số `camera`; câu `ONBOARDING` khi chưa qua màn chào giữ; hai test sau đổi tên như dòng trên. Các test `chooser_onboardingAndCameraOn_notDone_choosesOnboarding`, `chooser_onboardingDone_choosesCamera`, `chooser_onboardingFlagOff_choosesCamera_withoutReadingStatus`, `chooser_statusReadFails_showsOnboarding_andLogsWarning`, `chooser_logsChosenScreen` giữ nguyên (`BOTH_FLAGS` vẫn hợp lệ) |
| Có vị trí `sample_native_small` | Không còn | `AdPlacementAppTest.noSampleBottomBanner` (đối chứng "vị trí mẫu có trong danh sách") | Viết lại thành `noSamplePlacement`: đối chứng `entries.any { it.key == "poses_bottom" }`; với cả `"sample_bottom_banner"` và `"sample_native_small"`: `entries.none { it.key == key }` |
| `StagingRemoteValuesModuleTest` lọc vị trí "không của app" bằng `stagingOnly` hay tiền tố `sample_`, đối chứng tên khóa `is_show_sample_native_small` | Chỉ `gallery_*` | `StagingRemoteValuesModuleTest.noGalleryNorSamplePlacement_isShown` | Đổi tên thành `noGalleryPlacement_isShown`; lọc `entries.filter { it.stagingOnly }`; đối chứng `AdsSettings.placementEnabled(GALLERY_NATIVE_SMALL).name == "is_show_gallery_native_small"`; vòng kiểm giữ. Câu "tập giá trị staging đúng bằng tập của app" (`localValues_areExactlyTheAppSet`) không đổi |
| `ConfiguredAdUnitIds.missing` có `sample_native_small` khi chuỗi cấu hình thiếu nó | Không còn | `AdUnitIdsTest.configuredUnits_areParsedPerPlacementKey`, `placementWithoutUnit_isReportedMissingAndShowsNoAd` | Dữ liệu (ADR-0029): hai chuỗi như mục 3.1; `placementWithoutUnit_...` so với `entries.filterNot { stagingOnly }` nên tự đúng |
| `AdsSettings.all()` có `is_show_sample_native_small` | Không còn | `AdsSettingsTest.appWidePlacements_haveNoSwitchKey`, `all_isDerivedFromDeclaredPlacements`; `AdsFlagsTest` (khóa đã đọc), `StartupAdChoiceTest` | Đổi tên: các đối chứng dùng `is_show_poses_bottom`, cùng ý "một vị trí không app-wide có khóa bật" |
| `FeatureFlag.entries` có `SAMPLE` | Không còn | `DefaultFeatureFlagsTest.locallyEnabledFlag_isOn`, `locallyEnabledFlag_staysOnWhenRemoteSaysOff`, `remoteValueTrue_turnsFlagOn`, `remoteValueActivatedLater_reachesCollectors`; `RemoteConfigKeysTest.everyFeatureFlagAndPlacement_isListed` | Dữ liệu: cờ mẫu của bốn test là `LOADING`; câu kiểm giữ. `RemoteConfigKeysTest` qua sau khi sinh lại bảng |
| Khoảng 25 test của khung dùng vị trí mẫu | Dùng `POSES_BOTTOM` | Các file nhóm "Đổi tên" ở mục 3.2 | Giữ ý; chỉ đổi hằng và chuỗi mong đợi (ADR-0029). Không test nào kiểm số vị trí hay thứ tự `entries` (`grep -rn "entries.first()\|entries\[0\]\|\.ordinal" core app`: không có kết quả, 2026-10-31) |
| `str=` và ví dụ của `test-sessions.md` dùng chuỗi, tag của màn mẫu | Dùng chuỗi, tag của thư viện tư thế | `run.sh`: "device-scenario: str= matches text and content-desc", "selected=true matches selected or checked", "the example of test-sessions.md passes check" | Giữ ý; đổi dữ liệu như mục 3.1 (đã chạy, mục 4.1) |

`scripts/existing-tests.py` chạy ngày 2026-10-31 trên plan này (bỏ kết quả của `.claude/worktrees/`): 584 test dùng tên của 7 file Kotlin plan sửa; 21 "Cần xem trước", 293 "Nên xem". Đã đọc nhóm "Cần xem trước" và các test của `MainScreenTest`, `AdPlacementTest`, `AdPlacementAppTest`, `AdPlacementSourceTest`, `RemoteConfigKeysTest`, `StagingRemoteValuesModuleTest` trong nhóm "Nên xem":

- Phần lớn khớp chỉ vì chữ `AdPlacement` hay `camera` (tên gói, tên module). Các test đó không dùng vị trí mẫu, cờ mẫu hay `MainScreen.SAMPLE`: giữ. Test có dùng vị trí mẫu đều nằm trong nhóm "Đổi tên" ở mục 3.2 (tìm bằng `grep -rl "SAMPLE_NATIVE_SMALL\|sample_native_small" core app`, 26 file).
- `AdPlacementSourceTest.releaseScript_readsEveryShippedPlacement` đọc `AdPlacement.kt` theo luật của `release.sh`: bớt một dòng không đổi luật: giữ (qua trong chạy thử).
- `AdPlacementTest.stagingOnly_isExactlyGalleryPrefix`, `galleryPlacements_coverEveryFormatOnce`, `inListGalleryPlacements_declareNativeEvery8`: lọc theo tiền tố `gallery_` hay liệt kê vị trí của khung, không có `SAMPLE_NATIVE_SMALL`: giữ.
- `RemoteConfigKeysTest.everyPlacementShowKey_isInTableOff`, `everyFeatureFlagAndPlacement_isListed`: đọc `entries`, qua sau khi sinh lại bảng: giữ.
- `MainScreenTest.chooser_cameraFlagOn_choosesCamera`: cờ camera bật vẫn `CAMERA`: giữ.
- `MainScreenTest` có 14 test trước, 13 sau: hai test `SAMPLE` của hàm thuần (`onboardingAvailableButDone_cameraUnavailable_choosesSample`, `nothingAvailable_choosesSample`) gộp vào `onboarded_choosesCamera`, `onboardingUnavailable_choosesCamera` vì đầu vào camera không còn; thêm `everyScreen_isOnboardingOrCamera`. Không trường hợp đầu vào nào mất câu kiểm.

## 4. Kiểm thử

| Tiêu chí chấp nhận | Loại test | Tên test hoặc cách kiểm tra |
| --- | --- | --- |
| Màn chính sau loading theo câu 1: không phải màn chào thì camera | Unit test | `MainScreenTest.onboarded_choosesCamera`, `onboardingUnavailable_choosesCamera`, `chooserWithNoFlag_choosesCamera`, `everyScreen_isOnboardingOrCamera` (câu ở mục 3.5) |
| Cờ camera tắt vẫn mở camera, cờ camera không được đọc | Unit test | `MainScreenTest.chooser_cameraFlagOff_choosesCamera`. Đối chứng đã chạy (mục 4.1): thêm lại `&& featureFlags.enabled(FeatureFlag.CAMERA).first()` thì đỏ (`expected:<[ONBOARDING]> but was:<[ONBOARDING, CAMERA]>`) |
| Màn chào chỉ cần cờ của nó | Unit test | `MainScreenTest.chooser_onboardingOnCameraOff_choosesOnboarding`. Đối chứng đã chạy: cùng phép đột biến thì đỏ (`expected:<ONBOARDING> but was:<CAMERA>`) |
| Lần mở sau vào camera, màn chào lỗi đọc vẫn hiện màn chào | Unit test có sẵn | `chooser_onboardingDone_choosesCamera`, `chooser_statusReadFails_showsOnboarding_andLogsWarning`, `chooser_onboardingFlagOff_choosesCamera_withoutReadingStatus` giữ nguyên |
| Không còn vị trí `sample_native_small` | Unit test | `AdPlacementAppTest.noSamplePlacement` (mục 3.5): đỏ trước khi bỏ dòng enum |
| `ConfiguredAdUnitIds.missing` không đòi mã cho `sample_native_small` | Unit test | `AdUnitIdsTest.configuredUnits_areParsedPerPlacementKey` (chuỗi mới không có `sample_native_small`, `missing` rỗng) và `placementWithoutUnit_isReportedMissingAndShowsNoAd` |
| Staging không bật vị trí ngoài của app | Unit test (testStaging) | `StagingRemoteValuesModuleTest.noGalleryPlacement_isShown`, `localValues_areExactlyTheAppSet` (không đổi) |
| Test của khung vẫn kiểm cùng hành vi với `POSES_BOTTOM`; không test nào bị xóa | Unit test, Compose UI test có sẵn | Mọi test của `:core:ads`, `:core:analytics`, `:core:config` qua. Số test JVM của ba module không giảm so với nhánh chính (chạy thử: không giảm; xem mục 4.1). Chỉ test của `feature/sample` mất cùng module |
| Bảng khóa khớp code | Unit test có sẵn | `RemoteConfigKeysTest.keysFile_matchesCode`, `templateFile_matchesCode`, `everyFeatureFlagAndPlacement_isListed` sau khi sinh lại |
| Không còn tên màn mẫu trong code | Kiểm tra bằng lệnh | `git grep -n -i "feature:sample\|sample_native_small\|feature_sample_enabled\|FeatureFlag.SAMPLE\|MainScreen.SAMPLE\|SampleDestination" -- ':!docs/runs' ':!docs/features/*/plans' ':!docs/features/*/tickets' ':!docs/reports' ':!docs/adr' ':!docs/foundation.md' ':!docs/lessons.md' ':!docs/features/*/spec.md'` chỉ còn dòng của `AdPlacementAppTest.noSamplePlacement` (chuỗi cần cho câu kiểm) và chú thích `docs/template-sync.txt`. Ghi kết quả vào nhật ký |
| Test của script (L-075) | Test của script | `scripts/tests/run.sh`: các test `design-compare: *`, `failure-kind: *`, `device-scenario: str= matches text and content-desc`, `selected=true matches selected or checked`, `the example of test-sessions.md passes check`, `ads-plan: *` qua (đã chạy, mục 4.1) |
| Mở app lần đầu và lần sau vào đúng màn chào hay camera trên bản staging | Kiểm tra thủ công (máy thật: khác, định nghĩa hoàn thành của ticket đòi thử bản staging trên máy thật) | Cài bản staging mới (xóa dữ liệu), mở: màn chào; bấm "Bật camera": camera; đóng app, mở lại: camera. Trên emulator, `AppLaunchTest` và mọi kịch bản có sẵn đã chờ `onboarding_screen` hay `camera_screen` sau loading |

Commit test (bước 3), theo `docs/auto-run.md` bước 3:

- `MainScreenTest` mới, cùng khung rỗng để biên dịch được: `MainScreenInputs` bỏ `cameraAvailable`; `chooseMainScreen` giữ `else -> MainScreen.SAMPLE`; `MainScreenChooser` giữ `&& camera`. Các test dự kiến đỏ ở câu khẳng định: `onboarded_choosesCamera`, `onboardingUnavailable_choosesCamera`, `everyScreen_isOnboardingOrCamera`, `chooserWithNoFlag_choosesCamera`, `chooser_cameraFlagOff_choosesCamera`, `chooser_onboardingOnCameraOff_choosesOnboarding`. Ghi lý do thất bại thật vào nhật ký.
- `AdPlacementAppTest.noSamplePlacement` đỏ (`sample_native_small must be removed`).
- `StagingRemoteValuesModuleTest.noGalleryPlacement_isShown` qua ngay (test chặn). Phép đột biến (L-108): thêm `AdPlacement.GALLERY_NATIVE_SMALL` vào `appPlacementsShown()` thì đỏ; chạy một lần, ghi nhật ký.
- Phần đổi tên của nhóm "Đổi tên" và `DefaultFeatureFlagsTest` biên dịch và qua cả trước lẫn sau code (`POSES_BOTTOM`, `LOADING` đã có): đi cùng commit test.
- Commit code (bước 4): xóa module, `AdPlacement`, `FeatureFlag`, `AppNavHost`, `StagingFeatureFlagsModule`, bảng khóa sinh lại, script, tài liệu.

**Ngoại lệ viết test trước:**

- Xóa `feature/sample` và dòng của nó trong hai file Gradle, `StagingFeatureFlagsModule`, `AppNavHost`: xóa code, không có hành vi mới để kiểm ngoài `MainScreenTest` và việc build qua.
- Đổi tên vị trí trong test của khung và `FirebaseAdMetricsTest`: dữ liệu của test cũ (ADR-0029), qua trước và sau.
- Bảng khóa sinh lại; `docs/ops/ads-guide.md`, `docs/ops/test-sessions.md`, `CLAUDE.md`, `docs/template-sync.txt`: tài liệu. Ví dụ của `test-sessions.md` có test của script.
- `scripts/ads-plan.py`, `failure-kind.sh`, `none.log`: bảng tra chữ và chú thích; test có sẵn của script qua.

### 4.1. Chạy thử

- **Đã chạy thử:** 2026-10-31, toàn bộ phần code và script của mục 3.1 (trừ `CLAUDE.md`, `ads-guide.md`, `template-sync.txt`), trên nhánh tạm `tmp/T-081-try` từ nhánh chính `4a5ee7e` (trước T-080), commit tạm `b39131d`, không commit vào nhánh plan, đã xóa sau khi chạy. Đổi tên trong test bằng một script Python thay chuỗi (26 file), rồi sửa tay `AdUnitIdsTest`, `AdPlacementAppTest`, `StagingRemoteValuesModuleTest`, chú thích `AdsSettingsTest`.
- **Lệnh và kết quả:**
  - Đối chứng của test script, trước khi sửa `run.sh` (module đã xóa): `scripts/tests/run.sh` 17:41 tới 17:42 UTC, exit 1, dừng ở khối `DesignCompare`: `cp: feature/sample/src/test/screenshots/SampleScreen_ready_light.png: No such file or directory`. `python3 -I scripts/device-scenario.py check` trên tệp có `tap str=sample_increment` và `wait id=sample_screen 5`: exit 1, "str=sample_increment không có trong res/values/strings.xml của module nào", "id=sample_screen không có trong mã nguồn".
  - Sau khi sửa: `scripts/tests/run.sh` 17:43 tới 17:49 UTC, exit 0, "All script tests passed" (1131 dòng `ok`). Các test của mục 4 đều `ok`: `design-compare: identical images differ by 0%`, `light against dark differs`, `missing screenshot is listed`, `writes side-by-side images`; bốn `failure-kind:` dùng `:feature:settings:`; `device-scenario: str= matches text and content-desc`; `selected=true matches selected or checked`; `the example of test-sessions.md passes check` (13 dòng); 18 test `ads-plan:`.
  - `./gradlew -q :app:testStagingDebugUnitTest -PremoteConfigKeys.update=true` 17:49 tới 17:50 UTC, exit 0: bảng khóa mất đúng hai dòng, mẫu JSON mất 10 dòng.
  - Đối chứng của `MainScreenTest`: thêm lại `&& featureFlags.enabled(FeatureFlag.CAMERA).first()` vào `MainScreenChooser`, `./gradlew -q :app:testStagingDebugUnitTest --tests '*MainScreenTest*'`: 13 test, 2 đỏ đúng lý do (`chooser_onboardingOnCameraOff_choosesOnboarding`: `expected:<ONBOARDING> but was:<CAMERA>`; `chooser_cameraFlagOff_choosesCamera`: `expected:<[ONBOARDING]> but was:<[ONBOARDING, CAMERA]>`). Lần đầu `chooser_cameraFlagOff_choosesCamera` dùng `emptySet()` nên vẫn xanh với phép đột biến (`&&` dừng sớm); sửa test cho cờ màn chào bật, câu ở mục 3.5 là bản đã sửa. Bỏ phép đột biến thì 13/13 qua.
  - `scripts/check.sh` trên nhánh tạm (qua `check-locked.sh`), lần 1 bắt đầu 17:52 UTC: thất bại ở `:app:ktlintMainSourceSetCheck` (`MainScreenInputs` hai tham số viết trên nhiều dòng, quy tắc `standard:class-signature`); không test nào đỏ trước đó. Sửa: viết trên một dòng (đoạn code ở mục 3.1 đã theo cách này).
  - `scripts/check.sh` lần 2, bắt đầu 18:09 UTC: `Cổng chất lượng: QUA`, exit 0. Test JVM 2405, 0 lỗi (`./app` 106, `./core/ads` 1100, `./core/analytics` 38, `./core/config` 50; không còn `./feature/sample`); 135 ảnh screenshot (ba ảnh của màn mẫu mất cùng module, không ảnh nào khác đổi); lint, script tests và build staging qua. `./core/ads` 1100 bằng số của nhánh chính trước T-080 (chạy thử của plan T-080 ghi 1102 gồm 2 test tạm).
  - Nhánh tạm `tmp/T-081-try` đã xóa sau khi chạy.

## 5. Chấm rủi ro

**Điểm chặn bị chạm:**

- quảng cáo: bỏ vị trí `sample_native_small` (điểm 1);
- thu thập dữ liệu: bỏ sự kiện `sample_increment` cùng màn mẫu (điểm 4);
- module core dùng chung `:core:ads` (test của khung), `:core:config`, `:core:analytics` (test), cấu hình build (`settings.gradle.kts`, `app/build.gradle.kts`), script của quy trình (điểm 7).

Rủi ro Cao, cần duyệt chéo (Tuấn và Nhật). Điểm dưới đây chỉ để tham khảo.

| Tiêu chí | Điểm (0 đến 3) | Lý do |
| --- | --- | --- |
| A. Phạm vi thay đổi | 3 | Nhiều module (`:app`, `:core:ads`, `:core:config`, `:core:analytics`, một feature bị xóa), script và tài liệu; đổi enum công khai `AdPlacement`, `FeatureFlag` |
| B. Độ rõ của yêu cầu | 1 | Ticket rõ; câu 1 (màn khi cờ camera tắt) có đề xuất, plan viết theo đề xuất |
| C. Khả năng kiểm chứng tự động | 0 | Mọi tiêu chí có test JVM hay test của script; bước trên máy thật chỉ xác nhận lại |
| D. Ảnh hưởng chéo | 2 | Sửa nhiều file test mà T-080 cũng sửa; `AdPlacement.kt`, bảng khóa mà các plan quảng cáo khác cũng sửa |
| E. Độ mới của kỹ thuật | 0 | Xóa module và đổi tên; đã chạy thử toàn bộ |
| **Tổng** | 6 (tham khảo) | **Mức: Cao** (điểm chặn) |

## 6. Ảnh hưởng chéo

- File hoặc thành phần dùng chung bị chạm: `AdPlacement.kt` và khoảng 25 test của `:core:ads` (đồng bộ từ dự án mẫu); `FeatureFlag.kt`; `settings.gradle.kts`, `app/build.gradle.kts`; `MainScreen.kt`, `MainScreenChooser.kt`, `AppNavHost.kt`; bảng khóa; `scripts/tests/run.sh` và tệp mẫu của nó (của dự án mẫu); `CLAUDE.md`; `docs/ops/ads-guide.md`, `docs/ops/test-sessions.md`.
- Task khác có thể xung đột:
  - **T-080** (plan approved, chạy ở RUN-20261031-02 trước ticket này) sửa cùng các file: `InListAdsTest.kt`, `AdUnitIdsTest.kt`, `AdPlacementTest.kt`, `AdPlacementAppTest.kt`, `AdPlacement.kt`, `StagingRemoteValuesModuleTest.kt`, `StagingRemoteValuesModule.kt`, `docs/remote-config-keys.md`, `docs/remote-config-template.json`, `docs/ops/ads-guide.md` (dòng 128 trùng). Plan này chạy **sau T-080**, không cùng lúc; phiên tạo nhánh từ nhánh chính đã có T-080 và đổi tên cả phần T-080 thêm (mục 3.1, Sau T-080).
  - **T-075** (mã quảng cáo thật, `release.sh`) cần danh sách vị trí đã bỏ `sample_native_small`: chạy T-081 trước T-075.
  - Các plan soạn cùng ngày (T-082, T-084, T-085, T-086) có thể thêm vị trí vào `AdPlacement.kt`, khóa staging hay sửa `AdUnitIdsTest`, `StagingRemoteValuesModuleTest`. Plan nào chạy sau T-081 không được dùng `SAMPLE_NATIVE_SMALL`; plan nào viết dữ liệu `AdUnitIdsTest` theo chuỗi cũ (có `sample_native_small = unit/1`) thì phiên sau cập nhật dữ liệu theo ADR-0029.
- Đề xuất thứ tự chạy: sau T-080, trước T-075. Một task, một PR.
- Phần chung (`core/ads` test, `scripts/tests/run.sh`, `docs/ops/test-sessions.md`) đề xuất về dự án mẫu theo `docs/ops/from-app.md`, ghi trong mô tả PR.

## 7. Điều kiện dừng riêng

- Bảng khóa sinh lại có thay đổi khác ngoài việc mất `feature_sample_enabled`, `is_show_sample_native_small` và mô tả mới của `feature_camera_enabled`: dừng, vì nghĩa là khung hay cờ khác đã đổi ngoài plan.
- `git status --short -- '*/src/test/screenshots/'` có ảnh nào đổi hay mất ngoài ba ảnh `feature/sample/src/test/screenshots/SampleScreen_*.png`: dừng, không ghi lại ảnh.
- Một test của `:core:ads` hay `:core:analytics` đỏ sau khi đổi tên mà không phải vì một chuỗi hay hằng còn sót (ví dụ test phụ thuộc thứ tự `AdPlacement.entries` hay số vị trí): dừng; không đổi câu khẳng định ngoài mục 3.5.
- Số test JVM của `:core:ads`, `:core:config`, `:core:analytics` sau task ít hơn trên nhánh chính: dừng (không test nào được mất ngoài test của `feature/sample`).

## 8. Câu hỏi cho người duyệt

1. **Màn nào khi màn chào không áp dụng và cờ `feature_camera_enabled` tắt?** Hiện app mở màn mẫu. Bản production có cờ camera mặc định tắt, nên nếu Remote Config production chưa bật cờ, người dùng thấy màn mẫu.
   - Đề xuất của AI (plan viết theo): mở camera. Spec loading mục 2 chỉ có màn chào và camera. Hệ quả: cờ camera không còn được đọc; màn chào chỉ cần cờ của nó (trước đây cần cả cờ camera, plan T-012 câu 2, vì màn chào chỉ dẫn tới camera). Cờ vẫn khai báo và có trong bảng khóa. Đề xuất một ticket sau bỏ hẳn `feature_camera_enabled` (`FeatureFlag.CAMERA`, `StagingFeatureFlagsModule`, bảng khóa, spec camera mục 8).
   - Cách khác: giữ màn chào cần cả hai cờ. Khi đó cờ camera tắt chỉ còn tác dụng bỏ qua màn chào, vẫn vào camera; mục 3.1 giữ dòng `&& camera` và hai test của dòng 2 mục 3.5 giữ câu cũ (đổi `SAMPLE` thành `CAMERA`).
2. **Feature mẫu trong `CLAUDE.md`.** Không feature nào của app có đủ mọi bước. Đề xuất thay dòng 60 bằng: "Làm theo mẫu `:feature:settings` (bố cục module, `UiState`, `Route` và `Screen`, `navigation/`, test); phần repository có lớp cài đặt `internal`, `Analytics` và `AppLogger` xem `:feature:loading`; theo dõi cờ dạng `Flow` xem `PoseMatchViewModel` của `:feature:camera`; tải file trên R2 qua `RemoteAssets` xem `:core:poses`:". Sửa theo rà soát độc lập: không feature nào gọi `RemoteAssets`, và `LoadingViewModel` đọc cờ một lần bằng `first()`, trái bước 4. Nhật đồng ý hướng settings + loading ngày 2026-10-31.
3. **Bỏ sự kiện `sample_increment`.** Đây là sự kiện của màn mẫu, staging từng gửi khi bấm nút đếm; production không có màn mẫu. Điểm chặn 4 nên người phụ trách sản phẩm cần biết. Đề xuất: đồng ý bỏ.
4. **ADR-1008 trong PR plan.** ADR-1008 (`proposed`) và `docs/foundation.md` sinh lại có trong PR này để người duyệt đọc cùng plan. ADR-1007 (`accepted`) không sửa được, nên `foundation.md` vẫn còn vế "không gọi vị trí `gallery_*` hay `sample_native_small`" của ADR-1007 cạnh vế của ADR-1008 (không mâu thuẫn, chỉ thừa). Đề xuất: người chấp nhận ADR-1008 khi duyệt plan (đổi `status: accepted`, chạy `scripts/foundation.py`); khi nào ADR-1007 có bản thay toàn phần thì vế đó mất. Hay muốn phiên tạo ADR thay vì PR plan?
5. **Spec loading còn chữ "màn dự phòng"** (mục 5, đoạn quảng cáo mở app: "rồi mới vào màn chính (camera, onboarding hay màn dự phòng)"). Spec đã `confirmed`, plan không sửa. Đề xuất: người sửa chữ này khi spec loading đổi lần sau, hay cho một PR tài liệu nhỏ; không ảnh hưởng hành vi.

**Rà soát độc lập (2026-10-31, agent ngữ cảnh mới, chỉ đọc): 0 chặn, 3 nên sửa, 8 ghi chú.** `plan-check.py` qua, `foundation.py --check` khớp.

- **Nên sửa, đã sửa:**
  - luật chung khi đổi tên làm trùng, kèm test của T-084 và T-083 (mục 3.1);
  - câu thay cho `CLAUDE.md` (câu 2): bỏ `RemoteAssets` khỏi `:feature:loading`, dẫn `PoseMatchViewModel` cho cờ dạng `Flow`.
- **Nên sửa, Nhật đã quyết (2026-10-31):** sửa spec camera mục 8 (cờ không còn tác dụng, bỏ ở T-088, xác nhận lại), plan đổi mô tả cờ trong `FeatureFlag.kt` và bảng khóa, tạo ticket T-088. Ghi chú gốc: spec camera mục 8 ghi `feature_camera_enabled` là "Bật để dùng camera làm màn chính". Theo câu 1 (luôn mở camera), không code nào đọc cờ này nữa, trái spec đã chốt; mô tả cờ trong `docs/remote-config-keys.md` cũng sai (câu 6 dưới).
- **Ghi chú:**
  - Khung rỗng: `chooser_cameraFlagOn_choosesCamera`, `chooser_onboardingDone_choosesCamera`, `chooser_onboardingFlagOff_choosesCamera_withoutReadingStatus` cũng đỏ trên khung (không nhánh nào trả `CAMERA`); đỏ đúng lý do, ghi vào nhật ký.
  - `DataStoreUserPreferences.kt:21` còn chú thích "as the sample feature"; phiên có thể sửa theo ADR-0029 (chỉ chú thích). `NativeAdCardTest.kt:54` là file của mẫu, để nguyên.
  - Định nghĩa hoàn thành đòi không còn `sample_native_small` trong code; `AdPlacementAppTest.noSamplePlacement` giữ chuỗi này có chủ đích (mục 4). Người duyệt chấp nhận ngoại lệ.
  - Chạy thử chưa gồm `:app:assembleStagingRelease`; `pr-check` chạy bước này vì PR đổi `settings.gradle.kts`; `consumer-rules.pro` của module rỗng.
  - `AppNavHost` ở ngoại lệ viết test trước: chỉ xóa một nhánh `when` và `sampleScreen()`; build và `AppLaunchTest` bao phần này.
  - Bỏ lọc `sample_` ở `StagingRemoteValuesModuleTest` vẫn giữ ý nhờ `noSamplePlacement`.
  - ADR-1008 `proposed` hiện trong `foundation.md` khi PR plan merge; không xung đột.

## 9. Quyết định duyệt

- **Quyết định:** Duyệt
- **Người duyệt:** Tuấn, **duyệt chéo:** Nhật
- **Ghi chú:** Nhật chọn "Tuấn và Nhật duyệt" khi AI hỏi, ngày 2026-10-31. Trả lời mục 8:
  - câu 1: luôn mở camera; cờ `feature_camera_enabled` không còn tác dụng, spec camera mục 8 sửa và xác nhận lại, bỏ cờ ở T-088;
  - câu 2: hướng dẫn trong `CLAUDE.md` dùng `:feature:settings` cộng `:feature:loading` (câu đã sửa theo rà soát);
  - câu 3: bỏ sự kiện `sample_increment`;
  - câu 4: ADR-1008 trong PR plan, chuyển `accepted` khi plan được duyệt (đã chuyển);
  - câu 5: chữ "màn dự phòng" ở spec loading người sửa sau.
  - Chấp nhận ngoại lệ: `AdPlacementAppTest.noSamplePlacement` giữ chuỗi `sample_native_small`.

## 10. Nhật ký thực hiện

<!-- AI ghi khi task đã có kết quả cuối (đã merge hoặc đã dừng), trên nhánh báo cáo của phiên (auto-run.md, bước 10). Mục mới thêm vào cuối, không sửa mục cũ. -->
