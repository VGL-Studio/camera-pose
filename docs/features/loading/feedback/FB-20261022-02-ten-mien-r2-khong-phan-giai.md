---
id: FB-20261022-02
title: Có mạng mà không tải được catalog vì tên miền file tĩnh không phân giải được
reporter: AI (phân tích phiên thử TS-20261022-02)
source: staging
build: 0.1.0-staging.3
feature: loading
ticket: T-010
severity: chan
status: fixed
created: 2026-10-22
---

# FB-20261022-02: CÓ MẠNG MÀ KHÔNG TẢI ĐƯỢC CATALOG VÌ TÊN MIỀN FILE TĨNH KHÔNG PHÂN GIẢI ĐƯỢC

## Điều đã xảy ra

Trên máy thật, cấu hình tải xong sau 1,2 giây nhưng catalog tư thế thất bại ở lần đầu và cả 4 lần "Thử lại"; app không qua được màn loading. Từ máy dev, tên miền `camera-pose.vgl.io.vn` không phân giải được qua Cloudflare (1.1.1.1) và Google (8.8.8.8): cả vùng cha `io.vn` trả `SERVFAIL` vì không qua bước kiểm DNSSEC; tắt kiểm (`dig +cd`) thì phân giải đúng về Cloudflare. Chi tiết và log: `docs/test-sessions/TS-20261022-02.md`, P-1.

Lỗi nằm ở vùng `io.vn`, không ở code app hay cấu hình R2. Nhưng mọi người dùng có DNS kiểm DNSSEC (Cloudflare, Google, nhiều nhà mạng) sẽ không dùng được app trong lúc lỗi còn.

## Các bước làm lại

1. Trên máy dev: `dig SOA io.vn @1.1.1.1` và `dig camera-pose.vgl.io.vn @1.1.1.1`. `SERVFAIL` là lỗi còn.
2. Trên điện thoại: đặt DNS riêng `one.one.one.one` (Cài đặt, DNS riêng tư), cài bản staging mới hoặc xóa dữ liệu app.
3. Mở app: catalog báo lỗi dù có mạng.

## Mong đợi

Có mạng thì lần mở đầu chuẩn bị xong và vào màn hình chính (spec loading, KB-1).

## Ảnh chụp, video

Không có. Log: `test-sessions/TS-20261022-01/` trên máy người thử.

## Thiết bị

- Máy: Xiaomi 23021RAAEG
- Phiên bản Android: 15 (API 35)
- Ngôn ngữ, chế độ tối: không ghi

## Phân loại

<!-- Chủ feature điền, có thể nhờ AI đề xuất. -->

Đề xuất của AI, cần người phụ trách sản phẩm và người quản lý hạ tầng quyết:

1. Ngay: kiểm lại bằng lệnh ở bước 1. Còn lỗi thì báo nhà đăng ký tên miền `io.vn` (hoặc VNNIC); trong lúc chờ, người thử đặt DNS riêng `dns.quad9.net` (Quad9 phân giải được lúc 06:56 UTC).
2. Trước bản production: cân nhắc đặt file tĩnh ở tên miền thuộc vùng có DNSSEC ổn định, hoặc có tên miền dự phòng (ví dụ địa chỉ `r2.dev` của bucket) mà `:core:assets` thử khi tên miền chính không phân giải được. Phương án dự phòng chạm vào `:core:assets` dùng chung, nên đề xuất về dự án mẫu (`docs/ops/from-app.md`).
3. Thêm kiểm tra phân giải tên miền qua 1.1.1.1 và 8.8.8.8 vào `env-canary.yml` (bài học L-011).

Kết quả: Nhật đã xử lý lỗi tên miền `.io.vn` (2026-10-22). Kiểm lại từ máy dev: `dig SOA io.vn @1.1.1.1` trả `NOERROR`, `camera-pose.vgl.io.vn` phân giải được qua 8.8.8.8, catalog trả HTTP 200. Mục 2 và 3 của đề xuất (tên miền dự phòng, kiểm DNS trong `env-canary.yml`) vẫn nên cân nhắc trước bản production (bài học L-027).
