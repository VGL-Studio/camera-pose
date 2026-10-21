# SỐ LIỆU THIẾT KẾ: LoadingScreen_progress_light

Sinh bởi `scripts/figma-export.py`. Không sửa tay; thiết kế đổi thì chạy lại script (ADR-0020).

## 1. Nguồn

- Link: https://www.figma.com/design/6Xyp5CRP04IzFSIwFHU8Df/?node-id=2-150
- Frame: `2:150`, giao diện sáng
- Ngày lấy: 2026-10-16

## 2. Giá trị dùng

Đối chiếu với `docs/design-system/tokens.json` theo giá trị. "ngoài hệ thống" nghĩa là không có token
nào trùng; plan quyết định làm tròn về token gần nhất hay thêm token.

| Loại | Giá trị | Token |
| --- | --- | --- |
| khoảng cách | 10 | ngoài hệ thống |
| khoảng cách | 14 | ngoài hệ thống |
| khoảng cách | 24 | lg |
| khoảng cách | 7 | ngoài hệ thống |
| kiểu chữ | 11/13, đậm 400 | ngoài hệ thống |
| kiểu chữ | 12/19, đậm 400 | ngoài hệ thống |
| kiểu chữ | 13/16, đậm 500 | ngoài hệ thống |
| kiểu chữ | 14/17, đậm 400 | ngoài hệ thống |
| kiểu chữ | 14/21, đậm 400 | ngoài hệ thống |
| kiểu chữ | 32/38, đậm 400 | ngoài hệ thống |
| màu chữ | #147D68 | ngoài hệ thống |
| màu chữ | #17352F | ngoài hệ thống |
| màu chữ | #6D8079 | ngoài hệ thống |
| màu nền | #147D68 | ngoài hệ thống |
| màu nền | #DFEAE4 | ngoài hệ thống |
| màu nền | #EBF3EE | ngoài hệ thống |
| màu nền | #F7FAF8 | ngoài hệ thống |

## 3. Cấu trúc và số liệu

Toạ độ và cỡ tính theo dp, gốc là góc trên trái của khung. Thứ tự từ trên xuống như trong Figma.

- Frame "LoadingScreen_progress_light", vị trí 0,0, cỡ 411 x 891, nền #F7FAF8 (ngoài hệ thống)
  - Frame "Footer note", vị trí 46,846, cỡ 318 x 13, xếp ngang, cách nhau 7 (ngoài hệ thống), lề trên/phải/dưới/trái 0/0/0/0, căn CENTER/CENTER
    - Frame "lock-keyhole", vị trí 106,846, cỡ 12 x 12
      - Vector "Vector", vị trí 108,847, cỡ 9 x 10, viền #6D8079 (ngoài hệ thống), dày 1.5
    - Text "Privacy message", vị trí 126,846, cỡ 179 x 13, chữ "Không gian riêng cho việc của bạn", kiểu ngoài hệ thống (11/13, đậm 400), màu #6D8079 (ngoài hệ thống)
  - Text "Loading guidance", vị trí 46,546, cỡ 318 x 19, chữ "Đang chuẩn bị không gian của bạn…", kiểu ngoài hệ thống (12/19, đậm 400), màu #6D8079 (ngoài hệ thống)
  - Frame "Loading progress", vị trí 46,484, cỡ 318 x 39, xếp dọc, cách nhau 14 (ngoài hệ thống), lề trên/phải/dưới/trái 0/0/0/0, căn MIN/MIN
    - Frame "Progress details", vị trí 46,484, cỡ 318 x 17, xếp ngang, cách nhau 0, lề trên/phải/dưới/trái 0/0/0/0, căn SPACE_BETWEEN/CENTER
      - Text "Progress status", vị trí 46,484, cỡ 97 x 16, chữ "Đang tải dữ liệu", kiểu ngoài hệ thống (13/16, đậm 500), màu #6D8079 (ngoài hệ thống)
      - Text "Progress percentage", vị trí 336,484, cỡ 29 x 17, chữ "68%", kiểu ngoài hệ thống (14/17, đậm 400), màu #147D68 (ngoài hệ thống)
    - Frame "Progress track", vị trí 46,515, cỡ 318 x 8, nền #DFEAE4 (ngoài hệ thống), bo góc 100
      - Rectangle "Completed progress", vị trí 46,515, cỡ 216 x 8, nền #147D68 (ngoài hệ thống), bo góc 100
  - Frame "App identity", vị trí 46,220, cỡ 318 x 225, xếp dọc, cách nhau 24 (lg), lề trên/phải/dưới/trái 0/0/0/0, căn MIN/CENTER
    - Frame "Logo surround", vị trí 140,220, cỡ 132 x 132, nền #EBF3EE (ngoài hệ thống), bo góc 100, xếp ngang, cách nhau 0, lề trên/phải/dưới/trái 0/0/0/0, căn CENTER/CENTER
      - Frame "Todo logo", vị trí 162,242, cỡ 88 x 88, nền #147D68 (ngoài hệ thống), bo góc 26, xếp ngang, cách nhau 0, lề trên/phải/dưới/trái 0/0/0/0, căn CENTER/CENTER
        - Vector "Task completion mark", vị trí 180,262, cỡ 52 x 48, viền #FFFFFF (onPrimary, onSecondary, onTertiary, onError), dày 4.0
    - Frame "App name and promise", vị trí 46,376, cỡ 318 x 69, xếp dọc, cách nhau 10 (ngoài hệ thống), lề trên/phải/dưới/trái 0/0/0/0, căn MIN/CENTER
      - Text "App name", vị trí 46,376, cỡ 318 x 38, chữ "Todo App", kiểu ngoài hệ thống (32/38, đậm 400), màu #17352F (ngoài hệ thống)
      - Text "App promise", vị trí 46,424, cỡ 318 x 21, chữ "Mọi việc vào đúng nhịp.", kiểu ngoài hệ thống (14/21, đậm 400), màu #6D8079 (ngoài hệ thống)

## 4. Code tham khảo

Không có. Script không sinh code; AI viết Compose theo mục 2 và 3.
