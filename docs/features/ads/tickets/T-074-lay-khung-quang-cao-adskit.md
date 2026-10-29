---
id: T-074
title: Lấy khung quảng cáo AdsKit và AppsFlyer của dự án mẫu về app, chuyển ba vị trí quảng cáo hiện có sang khung mới
feature: ads
spec: features/ads/spec.md   # draft tới khi mục 9 có đủ câu trả lời; vị trí của từng màn vẫn ở mục 5 của features/camera/spec.md và features/poses/spec.md
scenarios: []
owner: Tuấn
status: backlog
risk: high
size: L
depends_on: []               # cần PR đồng bộ đợt 3 (VGL-Studio/camera-pose#208) đã merge, để base của docs/template-sync.txt là f674414
sprint:
plan:
created: 2026-10-28
---

# T-074: LẤY KHUNG QUẢNG CÁO ADSKIT VÀ APPSFLYER CỦA DỰ ÁN MẪU VỀ APP, CHUYỂN BA VỊ TRÍ QUẢNG CÁO HIỆN CÓ SANG KHUNG MỚI

## Mô tả

App đang dùng bộ quảng cáo cũ của dự án mẫu (commit `0eaeadb`): banner, interstitial, hộp thoại đồng ý, khóa `ads_<vị trí>_enabled`. ADR-1004 giữ app ở bộ cũ cho tới khi lấy `core/ads` và `core/analytics` qua một plan được duyệt. Dự án mẫu đã xong khung quảng cáo theo AdsKit PARTNER:
- nhịp hiển thị, mediation tám mạng;
- native, rewarded, app open, quảng cáo trong danh sách và trong luồng;
- AppsFlyer, ghi số liệu doanh thu.

Bộ này đã lên staging và được thử trên máy thật tới T-049 của dự án mẫu (`0.1.0-staging.18`).

Sau ticket này, app dùng khung mới. Ba vị trí hiện có (`review_interstitial`, `poses_banner`, `poses_detail_banner`) chạy qua `AdPlacement` và khóa `is_show_<vị trí>`, theo luật của `docs/ops/ads-business-rules.md`. Bản staging dùng mã thử của Google. ADR-1004 chuyển `superseded`.

## Phạm vi

- **Lấy code của dự án mẫu** theo `docs/ops/to-app.md` của dự án mẫu, mục 5. Người hay AI có người theo dõi chạy `scripts/template-sync.sh --app <camera-pose> --take <đường dẫn>` cho từng phần, gộp xung đột, đẩy nhánh `template-sync/take-...`. Plan lấy nhánh đó làm điểm bắt đầu.
  - `core/ads`: 254 file đổi từ `0eaeadb`.
  - `core/analytics`: AppsFlyer, ADR-0027.
  - `core/config`: `RemoteSettings`, bảng khóa, ADR-0023.
  - Các core mà `core/ads` cần ở bản mới: `core/network`, `core/designsystem`, `core/data`, `core/logging`. Plan kiểm bằng build và ghi phần nào phải lấy.
  - `gradle/libs.versions.toml`: chín adapter mediation, Unity Ads, AppsFlyer.
  - `settings.gradle.kts`: kho Maven của Pangle và Mintegral.
  - Phần `app/` mà khung cần:
    - `AdsHost`;
    - khởi tạo sau đồng ý;
    - `APPSFLYER_DEV_KEY` rỗng ở staging;
    - `app/lint.xml`;
    - màn xem thử quảng cáo staging, nếu giữ (câu 4 của spec);
    - `StagingRemoteValuesModule` của app **không** lấy nguyên bản mẫu: bản mẫu bật sẵn quảng cáo mở app, quay lại app, combo native sau interstitial và native đáy ở staging. Theo câu 6 của spec, app giữ tắt các vị trí toàn app; có test kiểm `StagingRemoteValuesModule` không bật `show_open_ads`, `show_open_ads_first_open`, `show_resum_ads`, `is_show_native_after_inter`, `is_show_native_bottom`.
- **Chuyển ba vị trí của app sang `AdPlacement` mới.**
  - `review_interstitial`: interstitial.
  - `poses_banner`, `poses_detail_banner`: banner.
  - `SAMPLE_BOTTOM_BANNER` và `feature/sample`: bỏ hay giữ theo câu hỏi 4.
  - Các chỗ gọi: `ReviewScreen.kt`; `PosesScreen.kt:85` và `PoseDetailScreen.kt:51` (qua `PosesBottomBanner` ở `PosesComponents.kt`); `SettingsScreen.kt`, `SettingsViewModel.kt` (nút quyền riêng tư).
  - Luật tần suất của `review_interstitial` (`AdsSettings.breakCapDefaults` và các khóa `ads_review_interstitial_*`) theo câu 1 của spec.
  - Khóa Remote Config đổi từ `ads_<vị trí>_enabled` sang `is_show_<vị trí>`.
  - Bảng khóa sinh lại theo `docs/ops/remote-config.md`.
- **Chép câu trả lời của `features/ads/spec.md` mục 9 vào spec màn:**
  - mục 5 của `features/camera/spec.md` và `features/poses/spec.md`: khóa, luật tần suất;
  - mục 6 của các spec có AppsFlyer: dữ liệu gửi đi, quyền mà SDK kéo vào.
- **Ảnh chuẩn screenshot.** Ảnh của màn có quảng cáo ghi lại theo plan, lệnh có `--rerun` (L-114 của dự án mẫu). Ảnh mới của khung (catalog native, màn xem thử) theo plan.
- **ADR và tài liệu.**
  - ADR-1004 chuyển `superseded`.
  - Bỏ các dòng `hold=` của các đường dẫn đã lấy trong `docs/template-sync.txt`.
  - Sinh lại `docs/foundation.md`.
  - Ghi chú trong `docs/template-sync.txt` về `release.sh` để lại cho T-075.

**Không bao gồm:**

- `scripts/release.sh`, `docs/ops/release.md`, mã quảng cáo thật, khóa AppsFlyer production: ticket T-075.
- Thêm vị trí quảng cáo mới (native, rewarded, app open, trong danh sách). Mỗi vị trí mới cần spec được duyệt và ticket riêng.
- Các phần khác của dự án mẫu đã đổi từ `0eaeadb` mà khung quảng cáo không cần, ví dụ `feature/loading`, `core/assets`. Plan liệt kê phần nào để lại, cho một đợt `--take` khác.

## Rủi ro sơ bộ

Chạm gần hết điểm chặn của `risk-scoring.md`:
- quảng cáo;
- SDK mới (chín adapter mediation, Unity Ads, AppsFlyer);
- quyền mà các SDK kéo vào manifest gộp;
- dữ liệu gửi đi (AppsFlyer, sự kiện doanh thu quảng cáo);
- module core;
- `gradle/libs.versions.toml`, `settings.gradle.kts`.

Rủi ro cao, cần duyệt chéo (Tuấn và Nhật). Phần chép từ dự án mẫu rất lớn (khoảng 37 nghìn dòng ở `core/ads`, gồm test và ảnh chuẩn). Plan nên tách thành nhiều lượt trong cùng danh sách chạy:
1. lấy code và build qua;
2. nối vỏ app và chuyển vị trí;
3. spec, ảnh chuẩn, tài liệu.

Mỗi lượt có PR riêng.

## Câu hỏi cho người

Ở mục 9 của `features/ads/spec.md` (sáu câu). Spec để `draft` tới khi đủ câu trả lời, nên `scripts/plan-queue.py` không chọn ticket này (và T-075 phụ thuộc nó) cho hàng chờ plan.

## Định nghĩa hoàn thành

- [ ] Ba vị trí hiện có chạy qua khung mới, đúng spec đã cập nhật; khóa `is_show_<vị trí>` mặc định tắt.
- [ ] Hộp thoại đồng ý và khởi tạo SDK đúng ADR-0004, ADR-0013 của dự án mẫu (đã đồng bộ); không có quảng cáo ở camera, màn chào hay loading, kể cả ở bản staging: các vị trí toàn app theo câu 6 của spec, có test.
- [ ] Manifest gộp chỉ có quyền mà spec đã duyệt; bản release staging build qua với R8.
- [ ] Test của khung (lấy từ dự án mẫu) và test của app qua; ảnh chuẩn đã ghi lại theo plan.
- [ ] ADR-1004 `superseded`; `docs/template-sync.txt` không còn `hold=` cho các đường dẫn đã lấy.
- [ ] CI qua toàn bộ, đã merge vào nhánh chính, có trong bản staging.
- [ ] Thử trên máy thật: banner ở thư viện và chi tiết tư thế, interstitial khi rời màn xem lại theo luật đã chốt, không bấm quảng cáo; đã nghiệm thu chéo.
