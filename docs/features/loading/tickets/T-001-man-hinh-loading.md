---
id: T-001
title: Màn hình loading có thanh tiến trình khi mở app
feature: loading
spec: features/loading/spec.md
scenarios: [KB-1, KB-2, KB-3, KB-4]
owner: Nhật
status: on-staging
risk: high
size: L
depends_on: []
sprint:
plan: features/loading/plans/T-001-man-hinh-loading.md
created: 2026-10-15
---

# T-001: MÀN HÌNH LOADING CÓ THANH TIẾN TRÌNH KHI MỞ APP

## Mô tả

Thêm feature `loading`: mỗi lần mở app, màn hình loading tải cấu hình mới nhất từ Remote Config và ba file từ R2, hiện thanh tiến trình có phần trăm, ít nhất 2 giây, rồi chuyển sang màn hình mẫu. Không tải được thì báo lỗi và cho thử lại. Sau khi xong, người test thấy màn hình này mỗi lần mở bản staging.

## Phạm vi

- Module mới `:feature:loading`: màn hình, ViewModel, dữ liệu, điều hướng, test.
- `:core:config`: thêm cách tải cấu hình mới nhất có giới hạn thời gian chờ, cho feature dùng; thêm cờ `feature_loading_enabled`.
- `:app`: màn hình loading là màn hình bắt đầu, chuyển sang màn hình mẫu khi xong; bật cờ cho bản staging; cập nhật test khởi động app trên emulator.
- Sự kiện `loading_completed` với `duration_ms`, `retry_count`.

**Không bao gồm:**

- Dùng nội dung của ba file ở màn hình khác.
- Đổi màn hình chào của hệ thống Android.

## Rủi ro sơ bộ

Chạm hai điểm chặn của risk-scoring.md: sự kiện phân tích mới (điểm 4) và module core dùng chung `:core:config` (điểm 7). Thay đổi màn hình bắt đầu của app và cách app dùng Remote Config (ADR-0018 thay một phần ADR-0012). Rủi ro cao, cần duyệt chéo.

## Định nghĩa hoàn thành

- [ ] Mọi tiêu chí chấp nhận của các kịch bản liệt kê ở trên đạt.
- [ ] Có test tự động cho phần logic và giao diện chính.
- [ ] CI qua toàn bộ, đã merge vào nhánh chính.
- [ ] Có trong bản staging, đúng với thiết kế đã duyệt.
- [ ] Đã được nghiệm thu chéo và nghiệm thu cuối.
