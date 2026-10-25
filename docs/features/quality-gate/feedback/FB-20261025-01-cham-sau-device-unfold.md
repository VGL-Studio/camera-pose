---
id: FB-20261025-01
title: Kịch bản T-024 lỗi ở bước 14 vì lệnh chạm sau `device unfold` không tới chip "Đã lưu"
reporter: AI (phân tích device-session của RUN-20261025-01, TS-20261025-01)
source: staging
build: nhánh chính bb3b286 (device-session.yml, lần chạy 40179423644)
feature: quality-gate
ticket: T-048                # trình chạy kịch bản; kịch bản của T-024
severity: nen-sua            # mức cho dòng `lỗi` của kịch bản (auto-run.md mục 4); mẫu phản hồi chưa có mức này
status: new
created: 2026-10-25
---

# FB-20261025-01: KỊCH BẢN T-024 LỖI Ở BƯỚC 14 VÌ LỆNH CHẠM SAU `device unfold` KHÔNG TỚI CHIP "ĐÃ LƯU"

## Điều đã xảy ra

Ở `device-session` cuối RUN-20261025-01, lần chạy thật đầu tiên của trình chạy kịch bản mới (T-048), `T-024-thu-vien-mo-gap.txt` lỗi ở bước 14, giống lần 1 của RUN-20261024-02:

```text
plan T-024-thu-vien-mo-gap.txt note step=10 t=32.3 ổn định sau 8.9 giây
plan T-024-thu-vien-mo-gap.txt step=10 device unfold ok
plan T-024-thu-vien-mo-gap.txt step=11 wait id=poses_grid 10 ok
plan T-024-thu-vien-mo-gap.txt note step=13 t=41.8 tap 593,383 Đã lưu
plan T-024-thu-vien-mo-gap.txt step=13 tap str=poses_chip_saved ok
plan T-024-thu-vien-mo-gap.txt step=14 wait str=poses_chip_saved selected=true 5 FAIL không thấy phần tử
```

Bằng chứng (artifact `device-session` của lần chạy 40179423644, thư mục `scenarios/T-024-thu-vien-mo-gap/`):

- **Kết luận:** lệnh chạm không tới chip. App ghi `VGL.Poses: Filter: <tên>` mỗi khi chọn chip (`PosesViewModel.kt:96`). Sau dấu `scenario=plan file=T-024-thu-vien-mo-gap.txt` (09:24:25.802) không có dòng `Filter:` nào; dòng `VGL.Poses` cuối là `Pose saved: true` lúc 09:24:56.107 (bước 8, chạm trước `device unfold`, có tác dụng).
- **Kết luận:** trình chạy chạm đúng chỗ theo bản dump. `fail.xml`: chip "Đã lưu" có nút cha chạm được `[496,321][690,447]`, `selected="false"`; điểm chạm `593,383` nằm giữa vùng đó. Chip "Tất cả" vẫn chọn (nút cha `selected="true"`), đúng như `fail.png`. Quy tắc `selected` theo nút cha của T-048 đọc đúng.
- **Kết luận:** màn hình đã ổn định trước khi chạm (`ổn định sau 8.9 giây`), nên giả thuyết của plan T-048 (bản dump cũ ngay sau khi Activity dựng lại) không phải nguyên nhân.
- **Giả thuyết (chưa kiểm):** tọa độ của `adb shell input tap` và tọa độ `bounds` của `uiautomator` không cùng hệ khi có `wm size`. Màn hình thật của emulator CI là 320x640 (`after-start.png`), `device unfold` đặt `wm size 1766x2208`, còn `fail.png` là 1766x1920 và nút gốc của dump là `[246,0][1521,1920]` (app bị đóng khung, FB-20261024-01). Mọi lệnh chạm trước `device unfold` (tọa độ dưới 320) đều có tác dụng; lệnh chạm duy nhất sau đó thì không.

Kịch bản T-022 đạt (có `device night on`, `device locale en-US`, nhưng không chạm sau hai lệnh đó). T-020 `bỏ qua (môi trường)` vì emulator CI không có camera sau, như dự kiến.

## Các bước làm lại

1. Chạy `device-session.yml` trên nhánh chính (hay `scripts/device-session.sh scenarios --yes` với emulator giống CI, màn 320x640).
2. Xem bảng "Kịch bản theo plan" của `summary.md`, dòng T-024, và `scenarios/T-024-thu-vien-mo-gap/fail.png`, `fail.xml`.
3. Kiểm giả thuyết: sau `adb shell wm size 1766x2208`, chạm `input tap` vào giữa một chip theo `bounds` của `uiautomator dump`, xem app có ghi `VGL.Poses: Filter:` không; thử lại với tọa độ đổi theo tỉ lệ màn thật.

## Mong đợi

Sau `device unfold`, lệnh `tap str=poses_chip_saved` chọn được chip "Đã lưu" (app ghi `Filter:`), bước 14 đạt. Kịch bản T-024 đạt thì quy tắc 11 của `plan-check.py` mới đổi thành lỗi được (plan T-048, mục 6).

## Ảnh chụp, video

Không đính kèm. `fail.png`, `fail.xml`, `unfolded.png` nằm trong artifact `device-session` của lần chạy 40179423644 (GitHub giữ artifact theo thời hạn của repo).

## Thiết bị

- Máy: emulator của `device-session.yml` (API 34, màn 320x640, không camera sau)
- Phiên bản Android: 14 (API 34)
- Ngôn ngữ, chế độ tối: mặc định của emulator

## Phân loại

<!-- Chủ feature điền, có thể nhờ AI đề xuất. -->

- **Loại:** đề xuất của AI: lỗi của trình chạy kịch bản hay của cách giả lập gập bằng `wm size`, không phải lỗi của app; cần kiểm giả thuyết ở bước 3 để chắc.
- **Xử lý:**
- **Lý do:**
- **Liên kết:** plan T-048, mục 3.4 (phương án dự phòng chưa gồm trường hợp này); FB-20261024-01.
