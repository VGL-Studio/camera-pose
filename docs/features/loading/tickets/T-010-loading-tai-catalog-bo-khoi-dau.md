---
id: T-010
title: Màn loading tải catalog và bộ tư thế khởi đầu
feature: loading
spec: features/loading/spec.md
scenarios: [KB-1, KB-2, KB-3, KB-4]
owner: Nhật
status: approved
risk: high
size: M
depends_on: [T-009]
sprint:
plan: features/loading/plans/T-010-loading-tai-catalog-bo-khoi-dau.md
created: 2026-10-21
---

# T-010: MÀN LOADING TẢI CATALOG VÀ BỘ TƯ THẾ KHỞI ĐẦU

## Mô tả

Thay ba file mẫu của dự án mẫu bằng catalog tư thế và bộ khởi đầu 60 tư thế; thanh tiến trình chia 10%, 10%, 80%; chờ 15 giây cho mỗi thứ (ADR-1001). Chuẩn bị xong thì sang màn chào hoặc camera thay cho màn hình mẫu.

## Phạm vi

- Ba phần việc: cấu hình, catalog, bộ khởi đầu (tải phần còn thiếu, tiến trình tăng theo từng file).
- `RemoteConfigRefresher` chờ 15 giây ở app này (ADR-1001).
- Điều hướng sau loading: màn chào khi chưa qua màn chào và cờ `feature_onboarding_enabled` bật, ngược lại sang camera. Khi màn chào hoặc camera chưa có, giữ màn hình mẫu.
- Bỏ ba file `test/v1/*` khỏi phần việc.

**Không bao gồm:**

- Giao diện (T-008).

## Rủi ro sơ bộ

Đổi cách mở app và thứ tự khởi tạo; dùng đường dẫn R2 của `:core:poses` (điểm chặn 8). Rủi ro cao.

## Định nghĩa hoàn thành

- [ ] Mọi tiêu chí chấp nhận của các kịch bản liệt kê ở trên đạt.
- [ ] Có test tự động cho phần logic và giao diện chính.
- [ ] CI qua toàn bộ, đã merge vào nhánh chính.
- [ ] Có trong bản staging, đúng với thiết kế đã duyệt.
- [ ] Đã được nghiệm thu chéo và nghiệm thu cuối.
