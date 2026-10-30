---
id: FB-20261030-01
title: Quảng cáo native toàn màn hình (native_combo) tải lỗi mã 0 rồi thử lại mỗi 5 giây trên emulator
reporter: AI (RUN-20261030-02, device-session)
source: staging
build: main 990dd59 (device-session.yml, lần chạy 41086766754)
feature: ads
ticket:
severity: nhe
status: new
created: 2026-10-30
---

# FB-20261030-01: QUẢNG CÁO NATIVE TOÀN MÀN HÌNH THỬ LẠI MỖI 5 GIÂY KHI TẢI LỖI MÃ 0

## Điều đã xảy ra

Trong `device-session` cuối phiên RUN-20261030-02, app ghi 75 dòng cảnh báo `W VGL.Ads : Native full-screen native_combo failed to load: 0`, mỗi dòng kèm `Retry native_combo in 5000 ms (failures N, code 0)` với N từ 1 tới 10. Cứ khoảng 5 giây có một lần tải lại, từ 16:16 tới 16:24 UTC, trong mọi kịch bản. Phiên trước (RUN-20261030-01, cùng khung quảng cáo của T-074) chỉ có 18 cảnh báo và không có dòng nào như vậy. Không có crash, ANR hay lỗi của app; mọi kịch bản đều đạt.

Chưa rõ đây là lỗi hay hành vi đúng. Mã 0 là lỗi nội bộ của SDK, thường gặp với quảng cáo thử trên emulator. Khoảng thử lại không tăng dần (luôn 5000 ms), nên trên máy thật hết quảng cáo thì app có thể gửi nhiều yêu cầu.

## Các bước làm lại

1. Chạy `device-session.yml` trên nhánh chính (`990dd59`).
2. Tải artifact `device-session`, mở `app-log.txt`.
3. `grep "native_combo" app-log.txt`.

## Mong đợi

Tải lỗi thì thử lại theo luật đã duyệt của khung quảng cáo (spec ads, ADR-0026); nếu luật muốn giãn dần thì khoảng chờ tăng sau mỗi lần lỗi. Cần chủ feature ads xem lại luật thử lại của `native_combo` và vì sao cảnh báo này mới xuất hiện (cờ Remote Config đổi, hay SDK quảng cáo thử trả lỗi).

## Ảnh chụp, video

Không có. Bằng chứng: `app-log.txt` của artifact `device-session` lần chạy 41086766754, các dòng từ `10-30 16:16:45.422` (pid 7636).

## Thiết bị

- Máy: emulator của CI (`device-session.yml`)
- Phiên bản Android: theo cấu hình của workflow
- Ngôn ngữ, chế độ tối: mặc định, một kịch bản đổi sang tối và `en-US`

## Phân loại

<!-- Chủ feature điền, có thể nhờ AI đề xuất. -->
