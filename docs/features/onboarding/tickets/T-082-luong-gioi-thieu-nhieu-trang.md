---
id: T-082
title: Luồng giới thiệu 4 trang thay màn chào (chưa có quảng cáo)
feature: onboarding
spec: features/onboarding/spec.md   # confirmed 2026-10-31, câu 4, 5, 7
scenarios: [KB-1, KB-5]      # KB-1 đổi số chạm theo câu 5
owner: Nhật
status: planned
risk: medium
size: L
depends_on: [T-085]          # trang premium hiện theo cờ feature_premium_poses_enabled của T-085
sprint:
plan: features/onboarding/plans/T-082-luong-gioi-thieu-nhieu-trang.md
created: 2026-10-31
---

# T-082: LUỒNG GIỚI THIỆU 4 TRANG THAY MÀN CHÀO

## Mô tả

Màn chào một trang đổi thành luồng giới thiệu nhiều trang, theo yêu cầu của Nhật ngày 2026-10-31. Luồng có 4 trang (câu 4 của spec onboarding). Trang 3 giới thiệu tư thế premium và chỉ hiện khi cờ `feature_premium_poses_enabled` bật; cờ tắt thì luồng có 3 trang.

Người dùng mới đi qua các trang bằng nút "Tiếp", rồi tới trang cuối. Không có nút "Bỏ qua" (câu 5). Trang cuối là trang xin quyền camera, giữ nút "Bật camera" và hành vi của KB-1, KB-2. Ticket này chưa có quảng cáo: thẻ native ở T-083. Ticket chỉ chừa sẵn vùng đáy trang để nút "Tiếp" không nhảy khi thẻ quảng cáo có mặt.

## Phạm vi

- **Thiết kế (trước plan):**
  - Dựng các trang trên Figma theo hướng Sticker: sáng, tối, khung thấp `w360dp-h640dp`, mở gập.
  - Có frame trạng thái `ad` có thẻ 250 dp ở mọi trang, và trang 1 lúc chờ quảng cáo (nút "Tiếp" mờ), để T-083 dùng.
  - Lập gói bàn giao trong `docs/features/onboarding/design/` (`docs/ops/design-workflow.md`).
- **`:feature:onboarding`:**
  - Pager hay danh sách trang, nút "Tiếp" ở cùng một chỗ trên mọi trang.
  - Trang 3 (premium) theo cờ `feature_premium_poses_enabled`.
  - Nút quay lại của máy về trang trước.
  - Trang cuối giữ nút "Bật camera", xin quyền và ghi `onboarding_complete`.
- Sự kiện `onboarding_step_viewed` (`step`), khai báo hằng `EVENT_` (câu 7).
- Chữ mới trong `strings.xml`, đủ 7 ngôn ngữ (câu 1).
- **Test:**
  - ViewModel: trang hiện tại, sang trang, quay lại, trang cuối, 3 hay 4 trang theo cờ, sự kiện mỗi trang một lần.
  - Giao diện, và screenshot cho mỗi ảnh thiết kế.
  - Ảnh `_small` và `_unfolded`.
  - Sửa test của màn chào cũ theo ADR-0031 khi câu khẳng định "một màn" trái spec mới.

**Không bao gồm:**

- Thẻ native `native_onboarding`: T-083.
- Trang 1 chờ quảng cáo tối đa 5 giây: T-083 (câu 8).

## Rủi ro sơ bộ

Đổi giao diện và luồng của lần mở đầu tiên, nơi đo tỷ lệ cho phép camera. Không thêm quảng cáo, quyền, SDK hay thư viện. Có thiết kế mới. Rủi ro trung bình.

## Định nghĩa hoàn thành

- [ ] Đạt KB-5 của spec onboarding, KB-1 tới KB-4 vẫn đạt theo spec đã sửa.
- [ ] Mỗi ảnh thiết kế có screenshot test cùng tên; chạy `scripts/design-compare.sh onboarding`.
- [ ] CI qua toàn bộ, đã merge vào nhánh chính, có trong bản staging.
- [ ] Thử trên máy thật bản staging sau khi xóa dữ liệu app: đi hết luồng, cho phép và từ chối camera, quay lại, xoay máy; tắt cờ premium trên Firebase staging thì còn 3 trang.
