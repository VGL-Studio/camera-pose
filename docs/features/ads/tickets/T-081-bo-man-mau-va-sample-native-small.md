---
id: T-081
title: Bỏ `:feature:sample`, vị trí `sample_native_small` và cờ `feature_sample_enabled`
feature: ads
spec: features/ads/spec.md   # confirmed; câu 4 (bỏ quảng cáo của feature/sample); màn chính sau loading theo mục 2 của features/loading/spec.md
scenarios: []                # không đổi kịch bản nào: màn mẫu không có trong spec của app
owner: Tuấn
status: backlog
risk: high
size: M
depends_on: [T-079]          # T-079 sửa trước AdPlacement.kt, StagingRemoteValuesModuleTest.kt và bảng khóa
sprint:
plan:
created: 2026-10-31
---

# T-081: BỎ MÀN MẪU, VỊ TRÍ `SAMPLE_NATIVE_SMALL` VÀ CỜ `FEATURE_SAMPLE_ENABLED`

## Mô tả

Nhật quyết định ngày 2026-10-31: bỏ `:feature:sample` và vị trí `sample_native_small`. Lý do: bản production không có màn mẫu, nên không tạo mã quảng cáo AdMob cho vị trí đó. Hiện `ConfiguredAdUnitIds.missing` đòi mã cho mọi vị trí không phải `gallery_*`, kể cả `sample_native_small`.

ADR-0017 của dự án mẫu cho phép bỏ: feature mẫu được xóa bằng một ticket khi feature thật đầu tiên thay màn bắt đầu. ADR-1007 của app thì ghi giữ `sample_native_small` vì test của khung dùng nó. Ticket này thay vế đó bằng một ADR mới của app.

Sau ticket này, người dùng không thấy gì khác:

- Màn chính sau loading vẫn là màn chào hoặc camera (spec loading, mục 2).
- Danh sách mã quảng cáo cần tạo cho production còn 10 vị trí.

## Phạm vi

- **Module:** xóa thư mục `feature/sample`, dòng `include(":feature:sample")` trong `settings.gradle.kts` và dòng `implementation(project(":feature:sample"))` trong `app/build.gradle.kts`.
- **Điều hướng của `:app`:**
  - Bỏ `sampleScreen()`, `SampleDestination` khỏi `AppNavHost`, và `MainScreen.SAMPLE`.
  - Viết lại các câu khẳng định `MainScreen.SAMPLE` của `MainScreenTest` theo ADR-0031.
  - **Câu hỏi cho plan:** hiện `chooseMainScreen` mở màn mẫu khi màn chào không áp dụng và cờ `feature_camera_enabled` tắt. Bỏ màn mẫu thì trường hợp này mở màn nào?
    - Đề xuất của AI: mở camera. Spec loading mục 2 chỉ có màn chào và camera; cờ camera tắt từ xa không có màn nào khác để thay.
    - Plan ghi rõ khi đó cờ camera còn tác dụng gì, hoặc đề xuất bỏ cờ trong một ticket khác.
- **Cờ `feature_sample_enabled`:** bỏ `FeatureFlag.SAMPLE` của `:core:config` và dòng của nó trong `StagingFeatureFlagsModule`. `DefaultFeatureFlagsTest` dùng cờ khác cho cùng câu kiểm, đổi dữ liệu theo ADR-0029.
- **Vị trí `sample_native_small`:**
  - Bỏ dòng `SAMPLE_NATIVE_SMALL` của `AdPlacement`.
  - Khoảng 20 file test của `core/ads` dùng nó làm vị trí mẫu (`AdTelemetryTest`, `AdUnitIdsTest`, `AdsFlagsTest`, `AdsSettingsTest`, `BottomAdBarTest`, `InListAdsTest`, `FakeNativeAds`...). Các test này chuyển sang `POSES_BOTTOM`: cùng định dạng `NATIVE_SMALL`, không phải `stagingOnly`. Câu kiểm giữ nguyên ý; chỉ đổi khóa và tên trong chuỗi mong đợi (ADR-0029).
  - `StagingRemoteValuesModuleTest` bỏ câu kiểm `is_show_sample_native_small`.
  - `scripts/ads-plan.py` bỏ dòng `KNOWN_PLACEMENTS` của màn mẫu.
- **Bảng khóa:** sinh lại `docs/remote-config-keys.md` và `docs/remote-config-template.json`. Hai khóa `feature_sample_enabled` và `is_show_sample_native_small` mất khỏi bảng.
- **Script và test của script:**
  - `scripts/tests/run.sh` (phần `DesignCompare`) lấy ảnh chuẩn của một màn còn lại thay `SampleScreen_ready_light.png` và `SampleScreen_ready_dark.png`, cần một cặp sáng và tối.
  - Chú thích ví dụ trong `scripts/failure-kind.sh` và các dòng log giả `:feature:sample:...` của `run.sh` đổi sang một module còn lại.
- **Hướng dẫn:**
  - `CLAUDE.md`, mục Viết một feature mới, thay "Làm theo đúng mẫu `:feature:sample`" bằng một feature thật của app có đủ `data/`, `di/`, `UiState`, `ViewModel`, `navigation/`. Ví dụ `:feature:settings`; plan kiểm nó có đủ từng bước của mục đó không.
  - Kiểm `docs/ops/ads-guide.md` và tài liệu khác còn dẫn màn mẫu như hướng dẫn đang dùng. Plan, nhật ký và báo cáo cũ giữ nguyên.
- **ADR:** viết ADR-1008 (`origin: app`, `supersedes: ADR-1007`, chỉ vế "Vị trí `gallery_*` và `sample_native_small`"):
  - App không có `:feature:sample` và `sample_native_small`; vị trí `gallery_*` vẫn giữ như ADR-1007.
  - Đợt đồng bộ với dự án mẫu (`docs/ops/to-app.md`) bỏ qua `feature/sample` và dòng `SAMPLE_NATIVE_SMALL`. Test của khung lấy từ mẫu thì đổi sang `POSES_BOTTOM`.
  - Chạy `scripts/foundation.py`.
- **Đề xuất về dự án mẫu (`docs/ops/from-app.md`):** test của khung nên dùng một vị trí riêng của test (`AdPlacementDeclaration` đã cho test tự khai báo vị trí) hay vị trí `gallery_*`, không dùng vị trí của màn mẫu. Khi đó app bỏ màn mẫu mà không phải sửa test của khung.

**Không bao gồm:**

- Mã quảng cáo thật và `scripts/release.sh`: T-075. T-075 cần biết danh sách vị trí đã bỏ `sample_native_small`.
- Bỏ vị trí `gallery_*`: giữ theo ADR-1007.
- Khóa `feature_sample_enabled`, `is_show_sample_native_small` còn trên Firebase staging hay production: người xóa tay nếu muốn. App không đọc nữa nên không ảnh hưởng.

## Rủi ro sơ bộ

Ticket chạm điểm chặn của `risk-scoring.md`:

- Bỏ một vị trí quảng cáo khỏi `AdPlacement`.
- Sửa module core `:core:ads` (test của khung), `:core:config`.
- Đổi cách `:app` chọn màn chính.

Không thêm SDK, thư viện hay quyền. Phần lớn là xóa và đổi tên vị trí trong test, làm cơ học, nhưng số file nhiều. Rủi ro cao, cần duyệt chéo (Tuấn và Nhật).

## Định nghĩa hoàn thành

- [ ] Không còn `feature/sample`, `SAMPLE_NATIVE_SMALL`, `FeatureFlag.SAMPLE`, `MainScreen.SAMPLE` trong code. `git grep -i "feature:sample\|sample_native_small\|feature_sample_enabled"` chỉ còn tài liệu cũ (plan, nhật ký, báo cáo, ADR).
- [ ] Màn chính sau loading theo câu trả lời của câu hỏi ở mục Phạm vi, có test.
- [ ] Test của khung vẫn kiểm cùng hành vi với vị trí `POSES_BOTTOM`; không test nào bị xóa chỉ vì vị trí mẫu mất.
- [ ] `ConfiguredAdUnitIds.missing` không còn đòi mã cho `sample_native_small`.
- [ ] Bảng khóa sinh lại khớp code; `scripts/tests/run.sh` qua.
- [ ] ADR-1008 được duyệt; `docs/foundation.md` sinh lại.
- [ ] CI qua toàn bộ, đã merge vào nhánh chính, có trong bản staging.
- [ ] Thử trên máy thật bản staging: mở app lần đầu và lần sau vào đúng màn chào hay camera.
