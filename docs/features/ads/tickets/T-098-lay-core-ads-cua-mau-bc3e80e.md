---
id: T-098
title: Lấy core/ads và hai tài liệu ads của dự án mẫu lên mốc bc3e80e (kích thước native mới, MAX toàn màn hình chưa bật)
feature: ads
spec: features/ads/spec.md   # câu 20 (Nhật trả lời và xác nhận 2026-11-16); MAX theo câu 16; giao diện quảng cáo theo khung (mục 4)
scenarios: []                # spec ads không có kịch bản riêng (mục 3); ticket đổi tiêu chí thanh đáy ở KB-6 của spec poses, KB-1 của spec settings, KB-9 và KB-11 của spec camera
owner: Nhật
status: backlog
risk: high
size: L
depends_on: []
sprint:
plan:
auto_run:
created: 2026-11-16
---

# T-098: LẤY CORE/ADS VÀ HAI TÀI LIỆU ADS CỦA DỰ ÁN MẪU LÊN MỐC BC3E80E

## Mô tả

Đợt đồng bộ dự án mẫu `698c5b3..bc3e80e` (PR #351) giữ `core/ads`, `docs/ops/ads-guide.md` và `docs/ops/ads-business-rules.md` ở mốc `698c5b3` (dòng `hold=` trong `docs/template-sync.txt`). Phần mới làm hỏng biên dịch test của app và đổi ảnh chuẩn (`docs/ops/to-app.md`, mục 5). Ticket này đưa ba đường dẫn đó lên mốc `bc3e80e`.

Sau khi xong:
- thẻ native và thanh đáy của app có kích thước theo thiết kế mới của khung;
- lớp thu gọn không mở khi cửa sổ không đủ chỗ;
- native chưa có quảng cáo thì thử lại giãn dần;
- app có sẵn interstitial, rewarded, app open của MAX, nhưng chưa bật vì bản build chưa có khóa MAX.

## Điều kiện trước plan

- Câu 20 của spec ads đã trả lời và spec chốt lại.
- Nếu nhận kích thước mới (câu 20, ý a), phải xong trước khi duyệt plan:
  - tiêu chí "80 dp" ở spec poses (KB-6), camera (KB-9) và settings (KB-1) đã đổi;
  - ảnh thiết kế trạng thái `ad` của thư viện, chi tiết, cài đặt, màn xem lại (cả `ReviewScreen_ad_dark_unfolded`) đã vẽ lại theo component thẻ native mới của khung, gói bàn giao đã lập lại. Xong 2026-11-16: component `NativeAdSmall` trên Figma theo `Native/Small` của mẫu, 8 frame `ad` xuất lại, Nhật duyệt.
- Người, hay AI có người theo dõi, đã đẩy nhánh điểm bắt đầu theo phần Điểm bắt đầu dưới đây.

## Phạm vi

- **Điểm bắt đầu (trước phiên, `to-app.md`, mục 5, bước 3 và 4):**
  - Từ repo dự án mẫu, chạy `scripts/template-sync.sh --app <camera-pose> --take <đường dẫn>` lần lượt cho `core/ads`, `docs/ops/ads-guide.md`, `docs/ops/ads-business-rules.md`. Mỗi lần chạy tạo một nhánh `template-sync/take-...`.
  - Gộp ba nhánh vào một nhánh, giải xung đột, rồi đẩy nhánh đó.
  - Plan ghi nhánh đó là điểm bắt đầu. Mục 3.1 của plan dán `diff` của phần đã gộp, vì phiên của app không đọc được repo mẫu.
- **Phần mẫu đổi trong `core/ads`** (`git log 698c5b3..bc3e80e -- core/ads` ở dự án mẫu; mã ticket ở đây là của dự án mẫu):
  - Kích thước native theo cạnh ngắn của cửa sổ (T-065 của mẫu, D-02):
    - khe đáy 90 tới 200 dp, 126 dp ở 411 x 891 dp, 200 dp ở khung mở gập;
    - `BottomAdBarDefaults.nativeReservation` thành hàm `nativeReservation()`, thêm `nativeSlotHeight()`;
    - `NativeAdDimens.heightOf` nhận thêm `nativeScreenSize()`;
    - lớp thu gọn là vùng ảnh cộng khe: 299 dp ở khung chuẩn, thay cho 250 dp; 400 dp ở khung mở gập đứng dọc.
  - Lớp thu gọn co vùng ảnh khi cửa sổ thấp, không mở khi vẫn không đủ chỗ: chia đôi màn hình, máy gập hay máy tính bảng nằm ngang; máy gập đứng dọc thì lớp vẫn mở (T-107 của mẫu, CL-05).
  - Thử lại native giãn dần 5, 10, 20, 40, 60 giây cho khe, ô trong lưới, pool và native toàn màn hình; đặt lại khi quay lại foreground hay khi mạng về (T-103 của mẫu, O-05, S-24).
  - Interstitial, rewarded, app open của MAX và doanh thu MAX (T-069, T-106, T-070, T-072 của mẫu). Không có khóa MAX thì MAX không khởi động, mọi vị trí tải từ AdMob như hôm nay.
- **Giữ phần riêng của app trong `core/ads`** (`docs/template-sync.txt`):
  - dấu phân cách của `AdSlot` (`AdSlotTest`) và `PrivacyOptionsButton(content)`;
  - hàm public `startupAdPlacementFor` (`StagingRemoteValuesModuleTest` của `app/src/testStaging`);
  - chiều cao `CARD` khi giữ chỗ trong `NativeAdSlot.kt`, gộp với tham số `nativeScreenSize()` mới;
  - năm test T-080 của `InListAdsTest`;
  - thuộc tính `collapsibleLayer` của `AdPlacement` (`review_bottom`, câu 19 ý e của spec), trừ khi dự án mẫu đã nhận issue from-app VGL-Studio/app-project-template#498. Gồm:
    - trường khai báo và `require` trong `AdPlacement.kt`;
    - `layerFormats` trong `AdUnitIds.kt`;
    - tham số `collapsibleLayer` của `bottomAdBarLayers`, `rememberBottomAdBarLayers`, `rememberBottomAdBarState` trong `BottomAdBar.kt`;
    - test của nó: `AdPlacementTest`, `AdPlacementAppTest`, `AdUnitIdsTest`, `BottomAdBarTest`, `AdsControllerNativeStockTest`, `AdPlacementSourceTest`.

    `BottomAdBar.kt` và `BottomAdBarTest` đổi nhiều ở mẫu (T-107 của mẫu): cách tắt lớp khi cửa sổ thấp phải gộp được với `collapsibleLayer = false`.
  - test của mẫu chỉnh theo app:
    - `AdSlotTest`: `reloadMillis = 0`;
    - `NativeFlowStepTest`: `allowed = true`;
    - `AdsControllerComboScopeTest`: `attachFullScreenHost`;
    - `AdsSettingsTest`: theo mẫu, trừ `POSES_BOTTOM`;
    - test dùng `SAMPLE_NATIVE_SMALL` đổi sang `POSES_BOTTOM`, bỏ dòng trùng.
  - ví dụ `poses_bottom` trong KDoc của `AdUnitIds.kt`;
  - ảnh chuẩn của `core/ads` lấy bản mẫu (`AdsTestTheme`), không ghi lại theo theme của app.
- **Test của feature:**
  - Sửa 8 file đang gọi `BottomAdBarDefaults.nativeReservation` dạng thuộc tính và dựng thanh đáy giả theo kích thước cũ:
    - `feature/poses`: `PosesScreenTest`, `PosesScreenScreenshotTest`, `PoseDetailScreenTest`, `PoseDetailScreenScreenshotTest`;
    - `feature/camera`: `ReviewScreenTest`, `ReviewScreenScreenshotTest`;
    - `feature/settings`: `SettingsScreenTest`, `SettingsScreenScreenshotTest`.
  - Hai file của `feature/onboarding` (`OnboardingScreenTest`, `OnboardingScreenScreenshotTest`) chỉ dùng `NativeAdCard` và `NativeAdPlaceholder` của thẻ bước. Chữ ký giữ nguyên, nên plan chỉ kiểm.
  - Câu khẳng định về chiều cao khe hay vị trí nội dung mà kích thước mới làm đổi thì viết lại theo ADR-0031, giữ ý định, không nới.
- **Ảnh chuẩn:**
  - Mục 3.2 của plan liệt kê từng ảnh chuẩn có thanh đáy native của `feature/poses`, `feature/camera`, `feature/settings` được ghi lại, cả ảnh `_unfolded` và `_small`. Ảnh khác không đổi.
  - Screenshot test cùng tên ảnh thiết kế theo thiết kế mới; chạy `scripts/design-compare.sh` cho từng feature.
  - Đo ở khung thấp `w360dp-h640dp` để nút chính không bị che.
- **`docs/template-sync.txt`:** script tự xóa ba dòng `hold=` khi `--take`. Cập nhật ghi chú phần app giữ, bỏ ghi chú về độ lệch của `ads-guide.md`.
- **`docs/ops/ads-guide.md`:** đặt lại tên của app như đợt trước: `PosesScreenScreenshotTest`, `poses_bottom`, `native_onboarding`.
- **`docs/ops/manual-test.md`, mục 11.1:** sửa các bước "khoảng 80 dp" và "lớp nổi lớn (250 dp)" theo kích thước mới. Thêm bước, sau khi bật `is_show_native_collap` trên Firebase staging (bản staging để tắt): máy gập mở ra đứng dọc thì lớp mở, cao khoảng 400 dp; máy gập nằm ngang hay chia đôi màn hình thì lớp co vùng ảnh hay không mở. App khóa dọc trên điện thoại, nên không có bước xoay điện thoại.

**Không bao gồm:**

- Vẽ lại thiết kế và sửa tiêu chí ba spec màn: làm trước plan (Điều kiện trước plan).
- Bật MAX ở staging hay production (khóa MAX, mã đơn vị MAX, nhóm mediation): quyết định sau, qua ticket riêng (câu 16 của spec).
- `app/build.gradle.kts`: mẫu chỉ đổi chú thích và thêm kiểm ký tự `maxSdkKey`, `maxAdUnits` trong task kiểm của bản production. Phần này thuộc cấu hình build production, nên lấy cùng ticket bật MAX.
- Vị trí quảng cáo mới, đổi luật tần suất hay khóa Remote Config.
- Màn xem thử quảng cáo `AdGalleryActivity` của mẫu (câu 4 của spec).

## Rủi ro sơ bộ

Chạm điểm chặn quảng cáo và module core (`risk-scoring.md`), đổi ảnh chuẩn của ba feature. Không thêm thư viện hay quyền: `core/ads/build.gradle.kts`, manifest của `core/ads` và `gradle/libs.versions.toml` không đổi trong khoảng này, MAX dùng SDK AppLovin đã có.

Lấy code thì thanh đáy cao lên ngay, nên phần code, test và ảnh chuẩn của feature phải đi cùng một lượt. Không tách được thành hai ticket chạy riêng.

## Định nghĩa hoàn thành

- [ ] `docs/template-sync.txt` không còn dòng `hold=` của `core/ads`, `docs/ops/ads-guide.md`, `docs/ops/ads-business-rules.md`.
- [ ] Phần riêng của app trong `core/ads` còn đủ, có test như trước.
- [ ] Thanh đáy và thẻ native của các màn có quảng cáo có kích thước mới, đúng thiết kế đã duyệt (`design-compare.sh`); nút chính không bị che ở khung thấp.
- [ ] Không có khóa MAX thì mọi vị trí tải từ AdMob như trước (test của khung).
- [ ] `scripts/check.sh` qua, CI qua toàn bộ, đã merge vào nhánh chính.
- [ ] Có trong bản staging, đã thử trên máy thật theo `manual-test.md`.
- [ ] Đã được nghiệm thu chéo và nghiệm thu cuối.
