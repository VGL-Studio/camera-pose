---
id: T-082
title: Luồng giới thiệu 3 đến 5 trang thay màn chào (chưa có quảng cáo)
feature: onboarding
spec: features/onboarding/spec.md   # changed 2026-10-31, chờ câu 4, 5, 7
scenarios: [KB-5]
owner: Nhật
status: backlog
risk: medium
size: L
depends_on: []
sprint:
plan:
created: 2026-10-31
---

# T-082: LUỒNG GIỚI THIỆU 3 ĐẾN 5 TRANG THAY MÀN CHÀO

## Mô tả

Màn chào một trang đổi thành luồng giới thiệu nhiều trang, theo yêu cầu của Nhật ngày 2026-10-31. Số trang và chữ của từng trang theo câu 4 của spec onboarding.

Người dùng mới đi qua các trang bằng nút "Tiếp", rồi tới trang cuối. Trang cuối là trang xin quyền camera, giữ nút "Bật camera" và hành vi của KB-1, KB-2. Ticket này chưa có quảng cáo: thẻ native ở T-083. Ticket chỉ chừa sẵn vùng đáy trang để nút "Tiếp" không nhảy khi thẻ quảng cáo có mặt.

## Phạm vi

- **Thiết kế (trước plan):**
  - Dựng các trang trên Figma theo hướng Sticker: sáng, tối, khung thấp `w360dp-h640dp`, mở gập.
  - Có một frame trạng thái `ad` có thẻ 250 dp, để T-083 dùng.
  - Lập gói bàn giao trong `docs/features/onboarding/design/` (`docs/ops/design-workflow.md`).
- **`:feature:onboarding`:**
  - Pager hay danh sách trang, nút "Tiếp" ở cùng một chỗ trên mọi trang.
  - Nút "Bỏ qua" nếu câu 5 chọn.
  - Nút quay lại của máy về trang trước.
  - Trang cuối giữ nút "Bật camera", xin quyền và ghi `onboarding_complete`.
- Sự kiện `onboarding_step_viewed` nếu câu 7 chọn.
- Chữ mới trong `strings.xml`, đủ 7 ngôn ngữ (câu 1).
- **Test:**
  - ViewModel: trang hiện tại, sang trang, quay lại, trang cuối.
  - Giao diện, và screenshot cho mỗi ảnh thiết kế.
  - Ảnh `_small` và `_unfolded`.
  - Sửa test của màn chào cũ theo ADR-0031 khi câu khẳng định "một màn" trái spec mới.

**Không bao gồm:**

- Thẻ native `native_onboarding`: T-083.
- Trang giới thiệu tư thế premium: nếu câu 4 chọn có trang này, nó cần T-085 có dữ liệu premium trước. Plan T-082 nói trang đó ở ticket nào.

## Rủi ro sơ bộ

Đổi giao diện và luồng của lần mở đầu tiên, nơi đo tỷ lệ cho phép camera. Không thêm quảng cáo, quyền, SDK hay thư viện. Có thiết kế mới. Rủi ro trung bình.

## Định nghĩa hoàn thành

- [ ] Đạt KB-5 của spec onboarding, KB-1 tới KB-4 vẫn đạt theo spec đã sửa.
- [ ] Mỗi ảnh thiết kế có screenshot test cùng tên; chạy `scripts/design-compare.sh onboarding`.
- [ ] CI qua toàn bộ, đã merge vào nhánh chính, có trong bản staging.
- [ ] Thử trên máy thật bản staging sau khi xóa dữ liệu app: đi hết luồng, cho phép và từ chối camera, quay lại, xoay máy.
