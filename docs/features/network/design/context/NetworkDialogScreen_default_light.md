# SỐ LIỆU THIẾT KẾ: NetworkDialogScreen_default_light

Sinh bởi `scripts/figma-export.py`. Không sửa tay; thiết kế đổi thì chạy lại script (ADR-0020).

## 1. Nguồn

- Link: https://www.figma.com/design/ZTXse6375eT3KXDUJPBTpe/?node-id=29-2170
- Frame: `29:2170`, giao diện sáng
- Ngày lấy: 2026-10-21

## 2. Giá trị dùng

Đối chiếu với `docs/design-system/tokens.json` theo giá trị. "ngoài hệ thống" nghĩa là không có token
nào trùng; plan quyết định làm tròn về token gần nhất hay thêm token.

| Loại | Giá trị | Token |
| --- | --- | --- |
| khoảng cách | 16 | md |
| khoảng cách | 24 | lg |
| khoảng cách | 8 | sm |
| kiểu chữ | 14/20, đậm 400 | bodyMedium |
| kiểu chữ | 16/24, đậm 600 | titleMedium |
| kiểu chữ | 20/20, đậm 400 | ngoài hệ thống |
| kiểu chữ | 22/28, đậm 800 | ngoài hệ thống |
| kiểu chữ | 32/32, đậm 400 | ngoài hệ thống |
| màu chữ | #111111 | ngoài hệ thống |
| màu chữ | #5A5650 | ngoài hệ thống |
| màu chữ | #FFFFFF | onPrimary, onSecondary, onTertiary, onError, surfaceContainerLowest |
| màu nền | #000000 | ngoài hệ thống |
| màu nền | #111111 | ngoài hệ thống |
| màu nền | #FFC83D | ngoài hệ thống |
| màu nền | #FFFDF8 | ngoài hệ thống |
| màu nền | #FFFFFF | onPrimary, onSecondary, onTertiary, onError, surfaceContainerLowest |

## 3. Cấu trúc và số liệu

Toạ độ và cỡ tính theo dp, gốc là góc trên trái của khung. Thứ tự từ trên xuống như trong Figma.

- Frame "NetworkDialogScreen_default_light", vị trí 0,0, cỡ 411 x 891, nền #FFFDF8 (ngoài hệ thống)
  - Rectangle "scrim", vị trí 0,0, cỡ 411 x 891, nền #000000 (ngoài hệ thống)
  - Instance "NetworkDialog", vị trí 40,300, cỡ 331 x 292, nền #FFFFFF (onPrimary, onSecondary, onTertiary, onError, surfaceContainerLowest), viền #111111 (ngoài hệ thống), dày 2.0, bo góc 24, xếp dọc, cách nhau 16 (md), lề trên/phải/dưới/trái 24 (lg)/24 (lg)/24 (lg)/24 (lg), căn MIN/CENTER
    - Frame "icon circle", vị trí 174,326, cỡ 64 x 64, nền #FFC83D (ngoài hệ thống), viền #111111 (ngoài hệ thống), dày 2.0, bo góc 999, xếp ngang, cách nhau 0, lề trên/phải/dưới/trái 0/0/0/0, căn CENTER/CENTER
      - Text "icon", vị trí 190,342, cỡ 32 x 32, chữ "wifi_off", kiểu ngoài hệ thống (32/32, đậm 400), màu #111111 (ngoài hệ thống)
    - Text "title", vị trí 66,406, cỡ 279 x 28, chữ "Mất kết nối mạng", kiểu ngoài hệ thống (22/28, đậm 800), màu #111111 (ngoài hệ thống)
    - Text "body", vị trí 66,450, cỡ 279 x 40, chữ "App cần mạng để hoạt động. Kiểm tra kết nối Internet rồi thử lại.", kiểu bodyMedium (14/20, đậm 400), màu #5A5650 (ngoài hệ thống)
    - Instance "button", vị trí 66,506, cỡ 279 x 60, nền #111111 (ngoài hệ thống), viền #111111 (ngoài hệ thống), dày 2.0, bo góc 14, xếp ngang, cách nhau 8 (sm), lề trên/phải/dưới/trái 16 (md)/24 (lg)/16 (md)/24 (lg), căn CENTER/CENTER
      - Text "icon", vị trí 166,526, cỡ 20 x 20, chữ "refresh", kiểu ngoài hệ thống (20/20, đậm 400), màu #FFFFFF (onPrimary, onSecondary, onTertiary, onError, surfaceContainerLowest)
      - Text "label", vị trí 194,524, cỡ 51 x 24, chữ "Thử lại", kiểu titleMedium (16/24, đậm 600), màu #FFFFFF (onPrimary, onSecondary, onTertiary, onError, surfaceContainerLowest)

## 4. Code tham khảo

Không có. Script không sinh code; AI viết Compose theo mục 2 và 3.
