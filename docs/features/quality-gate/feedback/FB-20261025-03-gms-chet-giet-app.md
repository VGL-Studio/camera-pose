---
id: FB-20261025-03
title: Kịch bản T-022 lỗi ở bước 2 vì Google Play services trên emulator chết và hệ thống giết app theo
reporter: AI (phân tích device-session của RUN-20261026-01, TS-20261025-03)
source: staging
build: nhánh chính cc5822f (device-session.yml, lần chạy 40226750845)
feature: quality-gate
ticket: T-022                # tệp kịch bản lỗi; trình chạy kịch bản của T-048, T-052
severity: nhe
status: new
created: 2026-10-25
---

# FB-20261025-03: KỊCH BẢN T-022 LỖI Ở BƯỚC 2 VÌ GOOGLE PLAY SERVICES TRÊN EMULATOR CHẾT VÀ HỆ THỐNG GIẾT APP THEO

## Điều đã xảy ra

Ở `device-session` cuối RUN-20261026-01, tệp `T-022-chi-tiet-sang-camera.txt` lỗi ở bước 2:

```text
plan T-022-chi-tiet-sang-camera.txt note step=1 t=0.1 fresh: chờ 3 giây sau khi dừng app (máy vừa trả về gốc)
plan T-022-chi-tiet-sang-camera.txt step=1 fresh ok
plan T-022-chi-tiet-sang-camera.txt step=2 consent decline 30 FAIL không thấy hộp thoại đồng ý hay màn hình đầu của app
```

`fail.xml` và `fail.png` của bước 2 là màn hình chủ của launcher: app đã không còn chạy.

Bằng chứng (artifact `device-session` của lần chạy 40226750845, `logcat.txt`):

- **Kết luận:** bước 1 đúng như T-052 sửa. Dừng app lúc 19:53:04.568, `Destroy timeout of remove-task` lúc 19:53:05.807, trình chạy chờ 3 giây rồi mở lúc 19:53:07.808 (`Start proc 9002`, `Displayed ... +2s625ms`). Lần mở này không bị giết như FB-20261025-02.
- **Kết luận:** 19:53:18.423 `Process com.google.android.gms (pid 2782) has died`, 19:53:18.558 `Process com.google.android.gms.persistent (pid 5174) has died`, rồi 19:53:18.629 `Killing 9002:com.vglstudio.camera.pose.line.guide.staging/u0a192 (adj 0): depends on provider com.google.android.gms/.fonts.provider.FontsProvider in dying proc com.google.android.gms.persistent`. Hệ thống giết app vì app đang giữ provider phông chữ của Google Play services, không phải vì app crash (crash 0, ANR 0).
- **Giả thuyết (chưa kiểm):** app (hay một SDK trong app, ví dụ hộp thoại đồng ý quảng cáo dùng WebView) dùng phông chữ tải về qua `FontsProvider` của Google Play services. Vì sao Google Play services chết trên emulator thì log không ghi.
- Tệp `T-024-thu-vien-mo-gap.txt` chạy ngay sau đó đạt (149 giây), cũng có `fresh: chờ 3 giây` và không phải mở lại.

## Các bước làm lại

1. Chạy `device-session.yml` trên nhánh chính, xem dòng của T-022 trong bảng "Kịch bản theo plan" của `summary.md`.
2. Tìm trong `logcat.txt` sau dấu `scenario=plan file=T-022-chi-tiet-sang-camera.txt` các dòng `has died` của `com.google.android.gms` và `depends on provider`.

## Mong đợi

Tệp T-022 đạt như ở TS-20261025-02. Nếu Google Play services chết giữa chừng thì trình chạy ghi rõ "app bị hệ thống giết theo Google Play services" (môi trường), không ghi `lỗi` của kịch bản.

## Ảnh chụp, video

Không đính kèm. `fail.png`, `fail.xml` và `logcat.txt` nằm trong artifact `device-session` của lần chạy 40226750845.

## Thiết bị

- Máy: emulator của `device-session.yml` (API 34, màn 320x640, không camera sau)
- Phiên bản Android: 14 (API 34)
- Ngôn ngữ, chế độ tối: mặc định của emulator (`en-US`)

## Phân loại

<!-- Chủ feature điền, có thể nhờ AI đề xuất. -->

- **Loại:** đề xuất của AI: lỗi môi trường của emulator, không phải lỗi của app hay của cách sửa T-052. Nếu lặp lại ở phiên sau thì xem có nên để trình chạy nhận ra dòng `depends on provider ... in dying proc` và ghi `bỏ qua (môi trường)`, hay kiểm app có thật cần `FontsProvider` của Google Play services không.
- **Xử lý:**
- **Lý do:**
- **Liên kết:** plan T-052 (chờ sau khi dừng app), FB-20261025-02, plan T-048 (mở lại khi `fresh` không có tiến trình).
