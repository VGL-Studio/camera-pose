# SỐ LIỆU THIẾT KẾ: ReviewScreen_save-error_dark

Sinh bởi `scripts/figma-export.py`. Không sửa tay; thiết kế đổi thì chạy lại script (ADR-0020).

## 1. Nguồn

- Link: https://www.figma.com/design/ZTXse6375eT3KXDUJPBTpe/?node-id=18-74
- Frame: `18:74`, giao diện tối
- Ngày lấy: 2026-10-21

## 2. Giá trị dùng

Đối chiếu với `docs/design-system/tokens.json` theo giá trị. "ngoài hệ thống" nghĩa là không có token
nào trùng; plan quyết định làm tròn về token gần nhất hay thêm token.

| Loại | Giá trị | Token |
| --- | --- | --- |
| khoảng cách | 16 | md |
| khoảng cách | 24 | lg |
| khoảng cách | 4 | xs |
| khoảng cách | 8 | sm |
| kiểu chữ | 14/20, đậm 400 | bodyMedium |
| kiểu chữ | 14/20, đậm 500 | titleSmall, labelLarge |
| kiểu chữ | 16/24, đậm 600 | titleMedium |
| kiểu chữ | 20/20, đậm 400 | ngoài hệ thống |
| kiểu chữ | 22/28, đậm 800 | ngoài hệ thống |
| kiểu chữ | 24/24, đậm 400 | ngoài hệ thống |
| kiểu chữ | 240/240, đậm 400 | ngoài hệ thống |
| màu chữ | #111111 | ngoài hệ thống |
| màu chữ | #2F6BFF | ngoài hệ thống |
| màu chữ | #B5B0A8 | ngoài hệ thống |
| màu chữ | #FFFFFF | ngoài hệ thống |
| màu nền | #0A0A0B | ngoài hệ thống |
| màu nền | #1E1E1E | ngoài hệ thống |
| màu nền | #F3F0E8 | ngoài hệ thống |
| màu nền | #FFFFFF | ngoài hệ thống |

## 3. Cấu trúc và số liệu

Toạ độ và cỡ tính theo dp, gốc là góc trên trái của khung. Thứ tự từ trên xuống như trong Figma.

- Frame "ReviewScreen_save-error_dark", vị trí 0,0, cỡ 411 x 891, nền #0A0A0B (ngoài hệ thống)
  - Instance "TopAppBar", vị trí 0,24, cỡ 411 x 64, xếp ngang, cách nhau 4 (xs), lề trên/phải/dưới/trái 0/4 (xs)/0/4 (xs), căn MIN/CENTER
    - Instance "leading", vị trí 4,32, cỡ 48 x 48, bo góc 999, xếp ngang, cách nhau 0, lề trên/phải/dưới/trái 0/0/0/0, căn CENTER/CENTER
      - Text "icon", vị trí 16,44, cỡ 24 x 24, chữ "close", kiểu ngoài hệ thống (24/24, đậm 400), màu #FFFFFF (ngoài hệ thống)
    - Text "title", vị trí 56,42, cỡ 351 x 28, chữ "Xem lại", kiểu ngoài hệ thống (22/28, đậm 800), màu #FFFFFF (ngoài hệ thống)
  - Frame "Ảnh vừa chụp (ảnh tạm)", vị trí 16,88, cỡ 379 x 505, nền #1E1E1E (ngoài hệ thống), viền #FFFFFF (ngoài hệ thống), dày 2.0, bo góc 16
    - Text "icon", vị trí 86,353, cỡ 240 x 240, chữ "person", kiểu ngoài hệ thống (240/240, đậm 400), màu #B5B0A8 (ngoài hệ thống)
  - Frame "actions", vị trí 40,617, cỡ 331 x 72, xếp ngang, cách nhau 0, lề trên/phải/dưới/trái 0/0/0/0, căn SPACE_BETWEEN/MIN
    - Frame "action Chụp lại", vị trí 40,617, cỡ 54 x 72, xếp dọc, cách nhau 4 (xs), lề trên/phải/dưới/trái 0/0/0/0, căn MIN/CENTER
      - Frame "button", vị trí 43,617, cỡ 48 x 48, nền #FFFFFF (ngoài hệ thống), viền #111111 (ngoài hệ thống), dày 2.0, bo góc 999, xếp ngang, cách nhau 0, lề trên/phải/dưới/trái 0/0/0/0, căn CENTER/CENTER
        - Text "icon", vị trí 55,629, cỡ 24 x 24, chữ "replay", kiểu ngoài hệ thống (24/24, đậm 400), màu #111111 (ngoài hệ thống)
      - Text "Chụp lại", vị trí 40,669, cỡ 54 x 20, chữ "Chụp lại", kiểu titleSmall, labelLarge (14/20, đậm 500), màu #FFFFFF (ngoài hệ thống)
    - Frame "action Chia sẻ", vị trí 172,617, cỡ 49 x 72, xếp dọc, cách nhau 4 (xs), lề trên/phải/dưới/trái 0/0/0/0, căn MIN/CENTER
      - Frame "button", vị trí 173,617, cỡ 48 x 48, nền #FFFFFF (ngoài hệ thống), viền #111111 (ngoài hệ thống), dày 2.0, bo góc 999, xếp ngang, cách nhau 0, lề trên/phải/dưới/trái 0/0/0/0, căn CENTER/CENTER
        - Text "icon", vị trí 185,629, cỡ 24 x 24, chữ "share", kiểu ngoài hệ thống (24/24, đậm 400), màu #111111 (ngoài hệ thống)
      - Text "Chia sẻ", vị trí 172,669, cỡ 49 x 20, chữ "Chia sẻ", kiểu titleSmall, labelLarge (14/20, đậm 500), màu #FFFFFF (ngoài hệ thống)
    - Frame "action Tư thế tiếp", vị trí 300,617, cỡ 71 x 72, xếp dọc, cách nhau 4 (xs), lề trên/phải/dưới/trái 0/0/0/0, căn MIN/CENTER
      - Frame "button", vị trí 312,617, cỡ 48 x 48, nền #FFFFFF (ngoài hệ thống), viền #111111 (ngoài hệ thống), dày 2.0, bo góc 999, xếp ngang, cách nhau 0, lề trên/phải/dưới/trái 0/0/0/0, căn CENTER/CENTER
        - Text "icon", vị trí 324,629, cỡ 24 x 24, chữ "skip_next", kiểu ngoài hệ thống (24/24, đậm 400), màu #111111 (ngoài hệ thống)
      - Text "Tư thế tiếp", vị trí 300,669, cỡ 71 x 20, chữ "Tư thế tiếp", kiểu titleSmall, labelLarge (14/20, đậm 500), màu #FFFFFF (ngoài hệ thống)
  - Instance "Button", vị trí 16,811, cỡ 379 x 60, nền #FFFFFF (ngoài hệ thống), viền #FFFFFF (ngoài hệ thống), dày 2.0, bo góc 14, xếp ngang, cách nhau 8 (sm), lề trên/phải/dưới/trái 16 (md)/24 (lg)/16 (md)/24 (lg), căn CENTER/CENTER
    - Text "icon", vị trí 128,831, cỡ 20 x 20, chữ "download", kiểu ngoài hệ thống (20/20, đậm 400), màu #111111 (ngoài hệ thống)
    - Text "label", vị trí 156,829, cỡ 126 x 24, chữ "Lưu vào thư viện", kiểu titleMedium (16/24, đậm 600), màu #111111 (ngoài hệ thống)
  - Instance "Snackbar", vị trí 16,739, cỡ 379 x 56, nền #F3F0E8 (ngoài hệ thống), bo góc 16, xếp ngang, cách nhau 8 (sm), lề trên/phải/dưới/trái 8 (sm)/8 (sm)/8 (sm)/16 (md), căn MIN/CENTER
    - Text "text", vị trí 32,757, cỡ 287 x 20, chữ "Không lưu được ảnh: bộ nhớ máy đã đầy.", kiểu bodyMedium (14/20, đậm 400), màu #111111 (ngoài hệ thống)
    - Frame "action", vị trí 327,747, cỡ 60 x 40, xếp ngang, cách nhau 0, lề trên/phải/dưới/trái 0/8 (sm)/0/8 (sm), căn CENTER/CENTER
      - Text "action", vị trí 335,757, cỡ 44 x 20, chữ "Thử lại", kiểu titleSmall, labelLarge (14/20, đậm 500), màu #2F6BFF (ngoài hệ thống)

## 4. Code tham khảo

Không có. Script không sinh code; AI viết Compose theo mục 2 và 3.
