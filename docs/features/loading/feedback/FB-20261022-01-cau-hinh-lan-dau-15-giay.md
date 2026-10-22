---
id: FB-20261022-01
title: Lần mở đầu sau khi cài, tải cấu hình vẫn hết 15 giây rồi báo lỗi trên emulator
reporter: AI (phiên tự chạy RUN-20261022-01, phiên thử TS-20261022-01)
source: staging
build: 0.1.0-staging, commit 0c2243e
feature: loading
ticket: T-010
severity: nang
status: wontfix
created: 2026-10-22
---

# FB-20261022-01: LẦN MỞ ĐẦU SAU KHI CÀI, TẢI CẤU HÌNH VẪN HẾT 15 GIÂY RỒI BÁO LỖI TRÊN EMULATOR

## Điều đã xảy ra

Cùng triệu chứng với FB-20261017-02 (`wontfix`), sau khi ADR-1001 nâng mức chờ lên 15 giây. Ở lần mở nguội đầu tiên sau khi cài, bước tải cấu hình hết giờ và màn loading dừng ở 0% chờ "Thử lại". Lần mở thứ hai, cấu hình tải xong trong 5,5 giây, catalog và bộ khởi đầu xong trong 4 giây.

Bằng chứng (`app-log.txt` của artifact `device-session`, lần chạy 39724404079):

```text
04:58:09.338 I VGL.Loading: Loading started
04:58:24.343 W VGL.RemoteConfig: Refresh timed out after 15000 ms
04:58:24.343 W VGL.Loading: Task REMOTE_CONFIG failed
04:58:29.275 I VGL.Loading: Loading started          (lần mở thứ hai)
04:58:34.792 I VGL.Loading: Task REMOTE_CONFIG done
```

Kết luận: lần mở đầu trên emulator CI hết 15 giây ở bước cấu hình. Giả thuyết (chưa kiểm chứng): Firebase Installations đăng ký lần đầu chậm trên emulator vừa khởi động; ở bước mất mạng cùng phiên, log có `Firebase Installations Service is unavailable`. Không kết luận về máy thật từ emulator.

## Các bước làm lại

1. Cài bản staging mới (hoặc xóa dữ liệu app), có mạng.
2. Mở app, chờ 20 giây.
3. Xem log bằng `scripts/device-session.sh start`, mở app, `scripts/device-session.sh stop`, hoặc chạy workflow `device-session.yml`.

## Mong đợi

Có mạng thì lần mở đầu chuẩn bị xong và vào màn hình chính (spec loading, KB-1), không báo lỗi.

## Ảnh chụp, video

Artifact `device-session` của lần chạy 39724404079 (giữ 7 ngày).

## Thiết bị

- Máy: emulator Google sdk_gphone64_x86_64
- Phiên bản Android: 14 (API 34)
- Ngôn ngữ, chế độ tối: mặc định của emulator

## Phân loại

<!-- Chủ feature điền, có thể nhờ AI đề xuất. -->

Đề xuất của AI: kiểm tra trên vài máy thật mới cài trước khi quyết. Nếu máy thật không gặp thì giữ `wontfix` như FB-20261017-02; nếu gặp thì cân nhắc cho lần mở đầu tiếp tục với giá trị mặc định khi cấu hình hết giờ (đổi ADR-0018, cần người phụ trách sản phẩm).

Kết quả kiểm trên máy thật (Nhật, 2026-10-22, bản 0.1.0-staging.3): loading chạy bình thường, không gặp hết giờ ở lần mở đầu. Giữ `wontfix` như FB-20261017-02, theo đề xuất ở trên.
