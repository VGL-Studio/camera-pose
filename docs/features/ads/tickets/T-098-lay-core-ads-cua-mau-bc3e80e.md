---
id: T-098
title: Lấy core/ads và hai tài liệu ads của dự án mẫu lên mốc bc3e80e (kích thước native mới, MAX toàn màn hình chưa bật)
feature: ads
spec: features/ads/spec.md   # câu 20 (changed 2026-11-16, chờ trả lời); MAX theo câu 16; giao diện quảng cáo theo khung (mục 4)
scenarios: []                # spec ads không có kịch bản riêng (mục 3); tiêu chí của các thanh đáy ở KB-4, KB-6 của spec poses, KB-1 của spec settings, KB-9 và KB-11 của spec camera, KB-6 của spec onboarding
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

Đợt đồng bộ dự án mẫu `698c5b3..bc3e80e` (PR #351) giữ lại `core/ads`, `docs/ops/ads-guide.md` và `docs/ops/ads-business-rules.md` ở mốc `698c5b3` (dòng `hold=` trong `docs/template-sync.txt`), vì phần mới làm hỏng biên dịch test của app và đổi ảnh chuẩn (`docs/ops/to-app.md` của dự án mẫu, mục 5). Ticket này đưa ba đường dẫn đó lên mốc `bc3e80e`. Sau khi xong, thẻ native và thanh đáy của app có kích thước theo thiết kế mới của khung. Lớp thu gọn tắt ở màn ngang và chia đôi màn hình. Khe native chưa có quảng cáo thử lại giãn dần. App có sẵn interstitial, rewarded và app open của MAX, nhưng chưa bật vì bản build chưa có khóa MAX.

## Phạm vi

- **Điểm bắt đầu (người hay AI có người theo dõi, trước phiên):** chạy `scripts/template-sync.sh --app <camera-pose> --take <đường dẫn>` từ repo dự án mẫu, lần lượt cho `core/ads`, `docs/ops/ads-guide.md`, `docs/ops/ads-business-rules.md`. Mỗi lần chạy tạo một nhánh `template-sync/take-...`; gộp ba lần vào một nhánh, giải xung đột, rồi đẩy nhánh. Plan ghi nhánh đó là điểm bắt đầu (`to-app.md`, mục 5, bước 3 và 4). Mục 3.1 của plan dán `diff` của phần đã gộp, vì phiên của app không đọc được repo mẫu.
- **Phần mẫu đổi trong `core/ads`** (`git log 698c5b3..bc3e80e -- core/ads` ở dự án mẫu):
  - Kích thước native theo cạnh ngắn của cửa sổ (T-065, KB-32): `BottomAdBarDefaults.nativeReservation` thành hàm `nativeReservation()`, thêm `nativeSlotHeight()`; `NativeAdDimens.heightOf` nhận thêm `nativeScreenSize()`.
  - Lớp thu gọn tắt ở màn ngang và chia đôi màn hình (T-107, CL-05).
  - Thử lại native giãn dần 5, 10, 20, 40, 60 giây, đặt lại khi quay lại foreground hay khi mạng về (T-103, O-05).
  - Interstitial, rewarded, app open của MAX và doanh thu MAX (T-069, T-106, T-070, T-072). Không có khóa MAX thì MAX không khởi động, mọi vị trí tải từ AdMob như hôm nay.
- **Giữ phần riêng của app trong `core/ads`** theo `docs/template-sync.txt`:
  - dấu phân cách của `AdSlot` và `PrivacyOptionsButton(content)`;
  - hàm public `startupAdPlacementFor`;
  - chiều cao `CARD` khi giữ chỗ trong `NativeAdSlot.kt` (gộp với tham số `nativeScreenSize()` mới);
  - năm test T-080 của `InListAdsTest`;
  - thuộc tính `collapsibleLayer` của `AdPlacement` (T-094, `review_bottom`) cùng test của nó, trừ khi mẫu đã nhận issue from-app VGL-Studio/app-project-template#498. Cách tắt lớp thu gọn của T-107 phải gộp được với `collapsibleLayer = false`;
  - test của mẫu dùng `SAMPLE_NATIVE_SMALL` đổi sang `POSES_BOTTOM`;
  - ảnh chuẩn của `core/ads` lấy bản mẫu (`AdsTestTheme`), không ghi lại theo theme của app.
- **Test của feature:** sửa 10 file test đang gọi `BottomAdBarDefaults.nativeReservation` dạng thuộc tính và các thẻ native giả theo kích thước cũ:
  - `feature/poses`: `PosesScreenTest`, `PosesScreenScreenshotTest`, `PoseDetailScreenTest`, `PoseDetailScreenScreenshotTest`;
  - `feature/camera`: `ReviewScreenTest`, `ReviewScreenScreenshotTest`;
  - `feature/settings`: `SettingsScreenTest`, `SettingsScreenScreenshotTest`;
  - `feature/onboarding`: `OnboardingScreenTest`, `OnboardingScreenScreenshotTest` dùng `NativeAdCard` hay `NativeAdPlaceholder`, plan kiểm xem có đổi không.

  Câu khẳng định về chiều cao khe hay vị trí nội dung mà kích thước mới làm đổi thì viết lại theo ADR-0031, giữ ý định, không nới.
- **Spec và thiết kế (trước plan, theo câu 20):** ba spec màn bỏ con số 80 dp ở tiêu chí thanh đáy. Ảnh thiết kế trạng thái `ad` của thư viện, chi tiết, cài đặt, màn xem lại vẽ lại khe theo số mới, rồi lập lại gói bàn giao.
- **Ảnh chuẩn:** plan liệt kê ở mục 3.2 từng ảnh chuẩn của `feature/poses`, `feature/camera`, `feature/settings`, `feature/onboarding` (cùng ảnh `_unfolded`, `_small`) có quảng cáo được ghi lại. Ảnh khác không đổi. So với thiết kế của từng feature (`scripts/design-compare.sh <feature>`): thẻ native cao hơn có thể làm nội dung chính bị đẩy lên, cần kiểm nút chính không bị che ở khung thấp `w360dp-h640dp`.
- **`app/`:** mẫu chỉ đổi chú thích và kiểm ký tự của `maxSdkKey`, `maxAdUnits` trong `app/build.gradle.kts`. Plan chọn lấy tay phần kiểm ký tự hay để lại, ghi vào `docs/template-sync.txt`.
- **`docs/template-sync.txt`:** xóa ba dòng `hold=` (script tự xóa khi `--take`), cập nhật ghi chú phần app giữ và bỏ ghi chú về độ lệch của `ads-guide.md`.
- **Tài liệu ads:** `docs/ops/ads-guide.md` đặt lại tên của app như đợt trước: `PosesScreenScreenshotTest`, `poses_bottom`, `native_onboarding`.
- **Kiểm tay trên máy:** `docs/ops/manual-test.md`, mục Quảng cáo: thanh đáy và thẻ native có kích thước mới, xoay ngang thì lớp thu gọn không hiện.

**Không bao gồm:**

- Bật MAX ở staging hay production (khóa MAX, mã đơn vị MAX, nhóm mediation): quyết định sau, qua ticket riêng (câu 16 của spec).
- Vị trí quảng cáo mới, đổi luật tần suất hay khóa Remote Config.
- Phần `app/` khác mà mẫu giữ riêng (màn xem thử `AdGalleryActivity`, câu 4 của spec).

## Rủi ro sơ bộ

Chạm điểm chặn quảng cáo và module core (`risk-scoring.md`), đổi ảnh chuẩn của bốn feature. Không thêm thư viện hay quyền: `core/ads/build.gradle.kts` và `gradle/libs.versions.toml` không đổi trong khoảng này, MAX dùng SDK AppLovin đã có. Plan chỉ soạn khi câu 20 của spec ads đã trả lời và spec chốt lại. Câu 20 hỏi nhận kích thước native mới, lớp thu gọn ở màn ngang, thử lại giãn dần, MAX toàn màn hình chưa bật, và `ad_platform` đã gửi từ PR #351. Nếu nhận kích thước mới, tiêu chí "80 dp" ở spec poses, camera, settings phải đổi theo, và ảnh thiết kế trạng thái `ad` phải vẽ lại trước khi duyệt plan. Ticket cỡ L: nếu plan thấy không vừa một lượt, tách phần lấy code và sửa test (biên dịch qua, ảnh chuẩn của `core/ads`) khỏi phần ghi lại ảnh chuẩn của feature.

## Định nghĩa hoàn thành

- [ ] `docs/template-sync.txt` không còn dòng `hold=` của `core/ads`, `docs/ops/ads-guide.md`, `docs/ops/ads-business-rules.md`.
- [ ] Phần riêng của app trong `core/ads` còn đủ, có test như trước.
- [ ] Thanh đáy và thẻ native của các màn có quảng cáo có kích thước mới; nút chính không bị che ở khung thấp; màn ngang không có lớp thu gọn.
- [ ] Không có khóa MAX thì mọi vị trí tải từ AdMob như trước (test của khung).
- [ ] `scripts/check.sh` qua, CI qua toàn bộ, đã merge vào nhánh chính.
- [ ] Có trong bản staging, đã thử trên máy thật theo `manual-test.md`.
