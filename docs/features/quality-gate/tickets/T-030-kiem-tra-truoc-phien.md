---
id: T-030
title: Kiểm tra môi trường trước phiên trên GitHub (bí mật staging, file R2, DNS) và phiên bản Java của check.sh
feature: quality-gate
spec:
scenarios: []
owner: Tuấn
status: approved
risk: high
size: M
depends_on: []
sprint:
plan: features/quality-gate/plans/T-030-kiem-tra-truoc-phien.md
created: 2026-10-23
---

# T-030: KIỂM TRA MÔI TRƯỜNG TRƯỚC PHIÊN TRÊN GITHUB VÀ PHIÊN BẢN JAVA CỦA CHECK.SH

## Mô tả

Nhiều lần, lỗi môi trường chỉ lộ ra khi phiên đã chạy, nên phiên phải dừng hoặc chạy mà không phát được bản staging:
- bí mật Firebase staging là file giả, hoặc nhóm người test chưa có (L-002, L-012);
- file R2 bắt buộc chưa được đưa lên (L-011);
- tên miền file tĩnh không phân giải được trên DNS công cộng (L-027).

Môi trường `claude --cloud` không gọi được R2 (L-005), nên các kiểm tra này chạy trong một workflow trên GitHub. Workflow chạy mỗi sáng và mỗi lần bắt đầu phiên; khi có lỗi, nó mở issue `setup-broken` để phiên không bắt đầu.

Ticket cũng thêm một bước kiểm phiên bản Java trước khi `scripts/check.sh` gọi Gradle (L-022). Sau ticket này, các lỗi trên lộ ra trước phiên, kèm cách sửa.

## Phạm vi

- `scripts/preflight.sh`: kiểm cấu trúc của bí mật Firebase staging (không in giá trị), nhóm người test của App Distribution, phân giải tên miền file tĩnh qua 1.1.1.1 và 8.8.8.8, các file R2 bắt buộc trong `scripts/required-assets.txt`.
- `.github/workflows/preflight.yml`: chạy script mỗi sáng và khi được gọi tay; mở, bình luận hay đóng issue `setup-broken`.
- `.github/workflows/start-run.yml`: chạy script và kiểm issue `setup-broken` trước khi kích hoạt routine.
- `scripts/java-check.sh`, gọi từ `scripts/check.sh` trước lệnh Gradle đầu tiên.
- Tài liệu: kiểm tra đầu phiên (`docs/auto-run.md` mục 2), `docs/ops/staging-and-runs.md`, `docs/ops/setup-new-app.md`.

**Không bao gồm:**

- Xung đột `docs/STATUS.md` ở mọi PR (L-016): chỉ đặt câu hỏi trong plan, chờ người quyết.
- Ghi tên lớp lỗi trong log của `:core:assets` (phần còn lại của L-027): module core, ticket riêng.

## Rủi ro sơ bộ

Cao: sửa `.github/` và `scripts/check.sh` (điểm chặn 7). Kiểm tra mới chỉ đọc bí mật trong workflow, không in ra; lệnh liệt kê nhóm người test của `firebase-tools` chưa chạy thử.

## Định nghĩa hoàn thành

- [ ] Mọi tiêu chí chấp nhận của các kịch bản liệt kê ở trên đạt.
- [ ] Có test tự động cho phần logic và giao diện chính.
- [ ] CI qua toàn bộ, đã merge vào nhánh chính.
- [ ] Có trong bản staging, đúng với thiết kế đã duyệt.
- [ ] Đã được nghiệm thu chéo và nghiệm thu cuối.
