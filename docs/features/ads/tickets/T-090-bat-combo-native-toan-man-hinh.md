---
id: T-090
title: Bật combo native toàn màn hình sau interstitial của màn
feature: ads
spec: features/ads/spec.md   # confirmed 2026-11-02; câu 18
scenarios: []                # spec ads không có kịch bản riêng (mục 3); tiêu chí theo câu 18 và luật K-01 tới K-08 của docs/ops/ads-business-rules.md
owner: Tuấn
status: backlog
risk: high
size: M
depends_on: []               # T-074, T-079 đã lên staging
sprint:
plan:
auto_run:
created: 2026-11-02
---

# T-090: BẬT COMBO NATIVE TOÀN MÀN HÌNH SAU INTERSTITIAL CỦA MÀN

## Mô tả

Sau mỗi interstitial của một màn (`review_interstitial` khi rời màn xem lại, `inter_pose_detail` khi bấm "Chụp với tư thế này"), người dùng thấy một quảng cáo native toàn màn hình có đếm ngược, đóng được bằng nút đóng hay nút Back, rồi về đúng chỗ interstitial dẫn tới. Interstitial dùng thay quảng cáo mở app hay quảng cáo khi quay lại app không có combo theo sau (spec ads, câu 18). Nguồn: phiên thử TS-20261102-01, P-1: khóa bị bật trên Firebase staging thì lượt combo mở mà không vẽ, giữ chặn quảng cáo quay lại app và không đóng được, vì `:app` chưa đặt `FullScreenAdHost` (plan T-074, câu 7).

## Phạm vi

- **Chỗ vẽ:** đặt `FullScreenAdHost()` một lần trong `MainActivity`, trong theme và trong `AdsHost`, sau `AppNavHost` (luật HD-05 của khung).
- **`resume_inter` không nối combo:** `ResumeAdTurns` hiện gọi `showInterstitial(..., combo = true)` (A-12). Đổi để `resume_inter` không nối combo. Interstitial lúc mở app đã không nối combo; plan kiểm lại cả `open_app_inter` khi `open_ad_type` là interstitial ở lần mở sau.
  - **Câu hỏi cho plan:** sửa thẳng trong `:core:ads` của app, hay thêm một công tắc của khung (giữ mặc định như mẫu) để dự án mẫu nhận lại được? Ghi đề xuất `from-app` vào mô tả PR (`docs/ops/from-app.md`).
- **Bản staging:** `StagingRemoteValuesModule` bật `is_show_native_after_inter`; sửa test của module theo ADR-0029, ADR-0031 (câu khẳng định cũ "không bật" trái câu 18).
- **Tài liệu đi cùng PR:**
  - Mục 5 của `features/camera/spec.md` và `features/poses/spec.md`: combo có thể hiện sau `review_interstitial` và `inter_pose_detail` (câu 18).
  - ADR-1007 có câu "`MainActivity` không đặt `FullScreenAdHost`". ADR đã `accepted` thì không sửa: plan đề xuất ADR mới của app thay câu đó, hoặc cách ghi khác theo `docs/CLAUDE.md`, mục Khi viết ADR.
  - `docs/ops/manual-test.md`: thêm mục kiểm combo trên máy thật (sau `review_interstitial`, sau `inter_pose_detail`, không có sau quảng cáo quay lại app dạng interstitial, nút Back trong lúc đếm ngược); sửa danh sách kiểm Firebase staging của mục 11, bỏ dòng "`is_show_native_after_inter` không đặt `true`".
- **Test, viết trước:**
  - Test Robolectric của vỏ app: lượt combo mở thì lớp phủ có trong cây giao diện; đóng thì về màn cũ, `Native combo closed` được ghi. Có test dựng lại activity trong lúc đếm ngược (L-061, L-091 của mẫu).
  - Unit test: `resume_inter` hoàn tất mà không gọi combo, kể cả khi khóa bật; `review_interstitial` và `inter_pose_detail` vẫn nối combo.
  - Kịch bản emulator nếu chạy được (emulator không có quảng cáo native thử: chỉ kiểm được lượt nạp lỗi tự đóng sau đếm ngược, K-04).

**Không bao gồm:**

- Mã quảng cáo thật của `native_combo` trong cấu hình phát hành và việc bật khóa trên Firebase production lúc phát hành (câu 18): thuộc T-075 và việc của người (ADR-0007, ADR-0009).
- Sửa khung để không mở lượt combo khi không có host nào vẽ (TS-20261102-01, P-1, việc 3): đề xuất về dự án mẫu bằng issue `from-app`, không làm ở app.

## Rủi ro sơ bộ

Cao: chạm quảng cáo, module core `:core:ads` (code lấy từ dự án mẫu, lệch luật A-12) và vỏ app (`MainActivity`). Không thêm SDK, quyền hay vị trí quảng cáo mới (`native_combo` đã có trong `AdPlacement`). Combo làm dài thêm mỗi lần interstitial: theo dõi giữ chân ngày 1 sau khi phát hành.

## Định nghĩa hoàn thành

- [ ] Combo hiện sau `review_interstitial` và `inter_pose_detail`, không hiện sau `resume_inter`, đúng luật K-01 tới K-08.
- [ ] Có test tự động cho phần logic và giao diện chính.
- [ ] CI qua toàn bộ, đã merge vào nhánh chính.
- [ ] Có trong bản staging, combo bật sẵn; kiểm trên máy thật theo mục mới của `manual-test.md`.
- [ ] Đã được nghiệm thu chéo và nghiệm thu cuối.
