# SỐ LIỆU THIẾT KẾ: ReviewScreen_default_dark_unfolded

Sinh bởi `scripts/figma-export.py`. Không sửa tay; thiết kế đổi thì chạy lại script (ADR-0020).

## 1. Nguồn

- Link: https://www.figma.com/design/ZTXse6375eT3KXDUJPBTpe/?node-id=18-103
- Frame: `18:103`, giao diện tối
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
| kiểu chữ | 14/20, đậm 500 | titleSmall, labelLarge |
| kiểu chữ | 16/24, đậm 600 | titleMedium |
| kiểu chữ | 20/20, đậm 400 | ngoài hệ thống |
| kiểu chữ | 22/28, đậm 800 | ngoài hệ thống |
| kiểu chữ | 24/24, đậm 400 | ngoài hệ thống |
| kiểu chữ | 240/240, đậm 400 | ngoài hệ thống |
| màu chữ | #111111 | ngoài hệ thống |
| màu chữ | #B5B0A8 | ngoài hệ thống |
| màu chữ | #FFFFFF | ngoài hệ thống |
| màu nền | #0A0A0B | ngoài hệ thống |
| màu nền | #1E1E1E | ngoài hệ thống |
| màu nền | #FFFFFF | ngoài hệ thống |

## 3. Cấu trúc và số liệu

Toạ độ và cỡ tính theo dp, gốc là góc trên trái của khung. Thứ tự từ trên xuống như trong Figma.

- Frame "ReviewScreen_default_dark_unfolded", vị trí 0,0, cỡ 673 x 841, nền #0A0A0B (ngoài hệ thống)
  - Instance "TopAppBar", vị trí 0,8, cỡ 673 x 64, xếp ngang, cách nhau 4 (xs), lề trên/phải/dưới/trái 0/4 (xs)/0/4 (xs), căn MIN/CENTER
    - Instance "leading", vị trí 4,16, cỡ 48 x 48, bo góc 999, xếp ngang, cách nhau 0, lề trên/phải/dưới/trái 0/0/0/0, căn CENTER/CENTER
      - Text "icon", vị trí 16,28, cỡ 24 x 24, chữ "close", kiểu ngoài hệ thống (24/24, đậm 400), màu #FFFFFF (ngoài hệ thống)
    - Text "title", vị trí 56,26, cỡ 613 x 28, chữ "Xem lại", kiểu ngoài hệ thống (22/28, đậm 800), màu #FFFFFF (ngoài hệ thống)
  - Frame "Ảnh vừa chụp (ảnh tạm)", vị trí 16,72, cỡ 450 x 600, nền #1E1E1E (ngoài hệ thống), viền #FFFFFF (ngoài hệ thống), dày 2.0, bo góc 16
    - Text "icon", vị trí 121,432, cỡ 240 x 240, chữ "person", kiểu ngoài hệ thống (240/240, đậm 400), màu #B5B0A8 (ngoài hệ thống)
  - Frame "actions", vị trí 482,72, cỡ 127 x 192, xếp dọc, cách nhau 24 (lg), lề trên/phải/dưới/trái 0/0/0/0, căn MIN/MIN
    - Frame "action Chụp lại", vị trí 482,72, cỡ 110 x 48, xếp ngang, cách nhau 8 (sm), lề trên/phải/dưới/trái 0/0/0/0, căn MIN/CENTER
      - Frame "button", vị trí 482,72, cỡ 48 x 48, nền #FFFFFF (ngoài hệ thống), viền #111111 (ngoài hệ thống), dày 2.0, bo góc 999, xếp ngang, cách nhau 0, lề trên/phải/dưới/trái 0/0/0/0, căn CENTER/CENTER
        - Text "icon", vị trí 494,84, cỡ 24 x 24, chữ "replay", kiểu ngoài hệ thống (24/24, đậm 400), màu #111111 (ngoài hệ thống)
      - Text "Chụp lại", vị trí 538,86, cỡ 54 x 20, chữ "Chụp lại", kiểu titleSmall, labelLarge (14/20, đậm 500), màu #FFFFFF (ngoài hệ thống)
    - Frame "action Chia sẻ", vị trí 482,144, cỡ 105 x 48, xếp ngang, cách nhau 8 (sm), lề trên/phải/dưới/trái 0/0/0/0, căn MIN/CENTER
      - Frame "button", vị trí 482,144, cỡ 48 x 48, nền #FFFFFF (ngoài hệ thống), viền #111111 (ngoài hệ thống), dày 2.0, bo góc 999, xếp ngang, cách nhau 0, lề trên/phải/dưới/trái 0/0/0/0, căn CENTER/CENTER
        - Text "icon", vị trí 494,156, cỡ 24 x 24, chữ "share", kiểu ngoài hệ thống (24/24, đậm 400), màu #111111 (ngoài hệ thống)
      - Text "Chia sẻ", vị trí 538,158, cỡ 49 x 20, chữ "Chia sẻ", kiểu titleSmall, labelLarge (14/20, đậm 500), màu #FFFFFF (ngoài hệ thống)
    - Frame "action Tư thế tiếp", vị trí 482,216, cỡ 127 x 48, xếp ngang, cách nhau 8 (sm), lề trên/phải/dưới/trái 0/0/0/0, căn MIN/CENTER
      - Frame "button", vị trí 482,216, cỡ 48 x 48, nền #FFFFFF (ngoài hệ thống), viền #111111 (ngoài hệ thống), dày 2.0, bo góc 999, xếp ngang, cách nhau 0, lề trên/phải/dưới/trái 0/0/0/0, căn CENTER/CENTER
        - Text "icon", vị trí 494,228, cỡ 24 x 24, chữ "skip_next", kiểu ngoài hệ thống (24/24, đậm 400), màu #111111 (ngoài hệ thống)
      - Text "Tư thế tiếp", vị trí 538,230, cỡ 71 x 20, chữ "Tư thế tiếp", kiểu titleSmall, labelLarge (14/20, đậm 500), màu #FFFFFF (ngoài hệ thống)
  - Instance "Button", vị trí 482,761, cỡ 175 x 60, nền #FFFFFF (ngoài hệ thống), viền #FFFFFF (ngoài hệ thống), dày 2.0, bo góc 14, xếp ngang, cách nhau 8 (sm), lề trên/phải/dưới/trái 16 (md)/24 (lg)/16 (md)/24 (lg), căn CENTER/CENTER
    - Text "icon", vị trí 525,781, cỡ 20 x 20, chữ "download", kiểu ngoài hệ thống (20/20, đậm 400), màu #111111 (ngoài hệ thống)
    - Text "label", vị trí 553,779, cỡ 61 x 24, chữ "Lưu ảnh", kiểu titleMedium (16/24, đậm 600), màu #111111 (ngoài hệ thống)

## 4. Code tham khảo

Không có. Script không sinh code; AI viết Compose theo mục 2 và 3.
