---
id: FB-20261025-02
title: Lần mở đầu của `fresh` ở tệp kịch bản thứ hai và thứ ba không có tiến trình, trình chạy phải mở lại
reporter: AI (phân tích device-session của RUN-20261025-02, TS-20261025-02)
source: staging
build: nhánh chính 5ce6f51 (device-session.yml, lần chạy 40208468872)
feature: quality-gate
ticket: T-049                # trình chạy kịch bản; nghi lệnh trả máy mới của T-049
severity: nhe
status: new
created: 2026-10-25
---

# FB-20261025-02: LẦN MỞ ĐẦU CỦA `fresh` Ở TỆP KỊCH BẢN THỨ HAI VÀ THỨ BA KHÔNG CÓ TIẾN TRÌNH, TRÌNH CHẠY PHẢI MỞ LẠI

## Điều đã xảy ra

Ở `device-session` cuối RUN-20261025-02, cả 3 kịch bản không `lỗi` (T-022 đạt, T-024 đạt, T-020 bỏ qua vì không có camera sau). Nhưng bước 1 (`fresh`) của T-022 và T-024 đều phải mở app lại:

```text
plan T-022-chi-tiet-sang-camera.txt note step=1 t=0.0 fresh: lần mở 1 không có tiến trình (am start -W hết 15 giây), mở lại
plan T-024-thu-vien-mo-gap.txt note step=1 t=0.1 fresh: lần mở 1 không có tiến trình (am start -W hết 15 giây), mở lại
```

Bằng chứng (artifact `device-session` của lần chạy 40208468872, `logcat.txt`):

- **Kết luận:** lần mở đầu không có tiến trình thật, nên `pidof` rỗng là đúng; không phải do cách đọc `pidof` mới của T-049. Sau dấu `scenario=plan file=T-022-chi-tiet-sang-camera.txt` (16:04:34.005): `START u0 ... MainActivity` lúc 16:04:35.196, rồi ngay 16:04:35.342 `ActivityManager: Killing 0:com.vglstudio.camera.pose.line.guide.staging/u0a192 (adj -10000): remove task`, không có `Start proc`. Lần mở thứ hai lúc 16:04:55.210 có `Start proc 8842` và `Displayed ... +1s531ms`. T-024 giống hệt: `START` 16:07:09.780, `remove task` 16:07:09.915, lần hai 16:07:29.837 có `Start proc 11192`.
- **Kết luận:** tệp đầu (T-020, sau monkey) mở được ngay lần đầu (`Start proc 7636` lúc 16:03:47.849). Ở RUN-20261025-01 (TS-20261025-01), cả 3 `fresh` đều có tiến trình ngay lần đầu.
- **Giả thuyết (chưa kiểm):** lệnh trả máy mới `wm reset-letterbox-style isEducationEnabled` (T-049) chạy sau mỗi tệp, ngay trước `fresh` của tệp sau, làm hệ thống gỡ task vừa mở. Đây là điểm khác duy nhất của trình chạy giữa hai phiên; tệp đầu không có lần trả máy nào trước nó.

Không làm hỏng kịch bản: trình chạy mở lại (T-048) và cả hai tệp đạt, nhưng mỗi tệp tốn thêm khoảng 20 giây.

## Các bước làm lại

1. Chạy `device-session.yml` trên nhánh chính, xem mục "Ghi chú của trình chạy" của `summary.md`.
2. Trên emulator giống CI: `adb shell wm reset-letterbox-style isEducationEnabled`, rồi ngay sau đó `adb shell pm clear <gói>` và `adb shell am start -W -n <gói>/<Activity>`; xem logcat có `remove task` không. Đối chứng: bỏ lệnh `wm reset-letterbox-style`.

## Mong đợi

`fresh` của mọi tệp có tiến trình ngay lần mở đầu, như RUN-20261025-01.

## Ảnh chụp, video

Không đính kèm. `logcat.txt` nằm trong artifact `device-session` của lần chạy 40208468872.

## Thiết bị

- Máy: emulator của `device-session.yml` (API 34, màn 320x640, không camera sau)
- Phiên bản Android: 14 (API 34)
- Ngôn ngữ, chế độ tối: mặc định của emulator

## Phân loại

<!-- Chủ feature điền, có thể nhờ AI đề xuất. -->

- **Loại:** đề xuất của AI: hành vi của trình chạy kịch bản trên emulator, không phải lỗi của app; cần kiểm giả thuyết ở bước 2.
- **Xử lý:** T-052 (RUN-20261026-01, PR #144): `fresh` của tệp đứng sau một lần trả máy chờ `DEVICE_SCENARIO_STOP_SETTLE` giây (mặc định 3) sau khi dừng app rồi mới mở. Ghi bởi phiên làm T-052, theo quyết định của Tuấn ở plan T-052, mục 9 (câu 3).
- **Lý do:** chẩn đoán ở plan T-052, mục 2. Kết luận: dừng app ngay sau lần trả máy làm hệ thống chờ Activity cũ hủy xong; khoảng 1,2 đến 1,4 giây sau khi dừng app, `Destroy timeout of remove-task` giết tiến trình theo tên của task cũ, trúng lần mở mới đang khởi động (`Killing 0: ... remove task`). Giả thuyết về lệnh `wm reset-letterbox-style` (T-049) không có bằng chứng trực tiếp; cách sửa không phụ thuộc giả thuyết đó.
- **Liên kết:** plan T-049 (lệnh trả máy mới), plan T-048 (mở lại khi `fresh` không có tiến trình), plan T-052 (cách sửa, mục 2 chẩn đoán, mục 3.4 phương án dự phòng).
