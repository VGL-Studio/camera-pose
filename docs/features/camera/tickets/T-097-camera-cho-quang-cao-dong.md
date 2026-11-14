---
id: T-097
title: Camera chờ quảng cáo toàn màn hình đóng rồi mới mở khi quay lại app
feature: camera
spec: features/camera/spec.md
scenarios: [KB-1, KB-8, KB-9]
owner: Nhật
status: on-staging
risk: high
size: M
depends_on: []
sprint:
plan: features/camera/plans/T-097-camera-cho-quang-cao-dong.md
auto_run:
created: 2026-11-13
---

# T-097: CAMERA CHỜ QUẢNG CÁO TOÀN MÀN HÌNH ĐÓNG RỒI MỚI MỞ KHI QUAY LẠI APP

## Mô tả

Người dùng quay lại app lúc đang ở camera và quảng cáo khi quay lại app (`resume_inter`) hiện: camera được mở ngay dưới quảng cáo và chạy suốt lúc quảng cáo hiện, vì `AdActivity` của AdMob trong suốt nên màn camera vẫn ở trạng thái STARTED (FB-20261113-02, phiên thử TS-20261113-01). Mục 5 của spec camera nói "Đóng quảng cáo thì camera mở lại như khi quay lại app thường". Nhật chọn ngày 2026-11-13 áp cho mọi quảng cáo toàn màn hình phủ lên camera, kể cả quảng cáo có thưởng mở khóa tư thế (spec camera mục 5 và KB-8, tiêu chí 4, sửa trong PR của plan T-097, chờ xác nhận). Sau task này, camera không chạy trong lúc quảng cáo toàn màn hình phủ lên, và chạy lại khi quảng cáo đóng.

## Phạm vi

- Màn camera (`feature/camera`, `CameraScreen.kt`, `CameraViewModel`): việc mở camera (`LifecycleStartEffect`, `CameraScreen.kt:135-143`) chờ tới khi không có quảng cáo toàn màn hình đang hiện; quảng cáo hiện khi camera đang mở thì đóng camera như khi rời màn. Trạng thái "đang hiện quảng cáo toàn màn hình" đọc qua một interface của `feature/camera` (mẫu `CountdownInterruptions`), phần cài đặt cấp ở `:app` bằng Hilt từ `AdsController.fullScreenAdShowing` (Nhật chọn ngày 2026-11-13); feature không đọc `AdsController` hay `LocalAdsController` (ADR-0045, mục 4), không sửa `core/ads`.
- Áp cho mọi quảng cáo toàn màn hình phủ lên camera: quảng cáo khi quay lại app (`resume`, `resume_inter`) và quảng cáo có thưởng mở khóa tư thế. Màn xem lại không dùng camera (`ReviewScreen.kt` không gắn camera), nên không đổi.
- Test: quảng cáo đang hiện thì camera không mở; quảng cáo đóng thì camera mở; app ra nền giữa lúc quảng cáo hiện rồi trở lại; cờ hay khóa quảng cáo tắt thì camera mở như hôm nay.

**Không bao gồm:**

- Đổi luật hiện quảng cáo khi quay lại app (A-11, A-12; câu 6 của spec ads). FB-20261113-03 về hai quảng cáo cách 6 giây đã chọn không xử lý.
- Quảng cáo trong app (`inter_pose_detail`, `review_interstitial`): camera đã không ở STARTED khi chúng hiện, plan chỉ kiểm lại.

## Rủi ro sơ bộ

Cao: đổi vòng đời của camera trên màn chính, hai module (`feature/camera`, `:app`); cần duyệt chéo. Không chạm `core/ads`. Không thêm quyền, thư viện hay sự kiện phân tích.

## Định nghĩa hoàn thành

- [ ] Mục 5 và KB-8, tiêu chí 4 của spec camera đạt: camera không chạy trong lúc quảng cáo toàn màn hình phủ lên, đóng quảng cáo thì camera mở lại như khi quay lại app thường; KB-1 (camera mở khi vào màn) và KB-8, tiêu chí 3 (ra nền rồi quay lại thì camera tự chạy lại) không đổi khi không có quảng cáo.
- [ ] Có test tự động cho các đường trong mục Phạm vi, kể cả ra nền rồi trở lại.
- [ ] CI qua toàn bộ, đã merge vào nhánh chính, có trong bản staging.
- [ ] Thử trên máy thật theo các bước của FB-20261113-02 và với quảng cáo có thưởng: chấm báo camera tắt trong lúc quảng cáo hiện; không có `Camera ready` nào giữa `Camera closed: full-screen ad` (hay `Camera waits for the full-screen ad`) và `Full-screen turn finished`; có `Camera ready` sau `Full-screen turn finished`.
