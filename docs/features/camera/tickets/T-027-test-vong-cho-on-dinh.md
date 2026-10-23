---
id: T-027
title: Screenshot test của trạng thái có vòng chờ chụp ở đồng hồ dừng, không còn chập chờn
feature: camera
spec: features/camera/spec.md
scenarios: []                # không đổi hành vi của app; sửa cách test chụp ảnh (L-029)
owner: Nhật
status: planned
risk: low
size: S
depends_on: [T-014, T-016]
sprint:
plan: features/camera/plans/T-027-test-vong-cho-on-dinh.md
created: 2026-10-22
---

# T-027: SCREENSHOT TEST CỦA TRẠNG THÁI CÓ VÒNG CHỜ CHỤP Ở ĐỒNG HỒ DỪNG, KHÔNG CÒN CHẬP CHỜN

## Mô tả

`CameraScreenScreenshotTest > starting_dark` thất bại một lần trên `main-guard` và kết thúc phiên RUN-20261023-01 (issue #35, L-029, L-033). Chạy thử ngày 2026-10-22 xác nhận nguyên nhân: vòng chờ chạy mãi nên màn hình không bao giờ rảnh, Robolectric vẽ khung hình khoảng 4 phút tới khi máy test hết bộ nhớ rồi mới chụp, và vị trí vòng chờ lúc chụp phụ thuộc cỡ bộ nhớ. Sau ticket này, các ảnh của trạng thái có vòng chờ được chụp ở một thời điểm cố định của đồng hồ test: giống hệt nhau mọi lần, chạy dưới một giây. Người dùng không thấy gì khác.

## Phạm vi

- `CameraScreenScreenshotTest.starting_dark`: chụp bằng `runComposeUiTest` với đồng hồ dừng; ghi lại ảnh chuẩn `CameraScreen_starting_dark.png`.
- `ReviewScreenScreenshotTest.saving_dark` (T-016 tạo, nút "Lưu vào thư viện" có vòng chờ khi đang lưu): cùng cách, ghi lại ảnh chuẩn `ReviewScreen_saving_dark.png`.
- Chạy lại `scripts/design-compare.sh camera` cho hai ảnh này.

**Không bao gồm:**

- Code của app (`CameraControls.kt`, `PrimaryButton.kt`): không đổi vòng chờ.
- Chạy lại test hỏng trên `main-guard` trước khi mở `main-broken` (T-028).

## Rủi ro sơ bộ

Thấp: chỉ đổi hai test và hai ảnh chuẩn trong một module. Cách chụp bằng `runComposeUiTest` lần đầu dùng trong dự án, nhưng đã chạy thử và đo (plan, mục 4.1).

## Định nghĩa hoàn thành

- [ ] Mọi tiêu chí chấp nhận của các kịch bản liệt kê ở trên đạt.
- [ ] Có test tự động cho phần logic và giao diện chính.
- [ ] CI qua toàn bộ, đã merge vào nhánh chính.
- [ ] Có trong bản staging, đúng với thiết kế đã duyệt.
- [ ] Đã được nghiệm thu chéo và nghiệm thu cuối.
