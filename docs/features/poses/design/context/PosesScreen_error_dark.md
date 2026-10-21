# SỐ LIỆU THIẾT KẾ: PosesScreen_error_dark

Sinh bởi `scripts/figma-export.py`. Không sửa tay; thiết kế đổi thì chạy lại script (ADR-0020).

## 1. Nguồn

- Link: https://www.figma.com/design/ZTXse6375eT3KXDUJPBTpe/?node-id=20-1655
- Frame: `20:1655`, giao diện tối
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
| kiểu chữ | 12/16, đậm 400 | bodySmall |
| kiểu chữ | 14/14, đậm 400 | ngoài hệ thống |
| kiểu chữ | 14/20, đậm 400 | bodyMedium |
| kiểu chữ | 14/20, đậm 500 | titleSmall, labelLarge |
| kiểu chữ | 16/24, đậm 600 | titleMedium |
| kiểu chữ | 20/20, đậm 400 | ngoài hệ thống |
| kiểu chữ | 22/28, đậm 800 | ngoài hệ thống |
| kiểu chữ | 24/24, đậm 400 | ngoài hệ thống |
| kiểu chữ | 36/36, đậm 400 | ngoài hệ thống |
| màu chữ | #111111 | ngoài hệ thống |
| màu chữ | #B5B0A8 | ngoài hệ thống |
| màu chữ | #FFFFFF | ngoài hệ thống |
| màu nền | #121212 | ngoài hệ thống |
| màu nền | #1E1E1E | ngoài hệ thống |
| màu nền | #FFC83D | ngoài hệ thống |
| màu nền | #FFFFFF | ngoài hệ thống |

## 3. Cấu trúc và số liệu

Toạ độ và cỡ tính theo dp, gốc là góc trên trái của khung. Thứ tự từ trên xuống như trong Figma.

- Frame "PosesScreen_error_dark", vị trí 0,0, cỡ 411 x 891, nền #121212 (ngoài hệ thống), xếp dọc, cách nhau 8 (sm), lề trên/phải/dưới/trái 24 (lg)/0/0/0, căn MIN/MIN
  - Instance "TopAppBar", vị trí 0,24, cỡ 411 x 64, xếp ngang, cách nhau 4 (xs), lề trên/phải/dưới/trái 0/4 (xs)/0/4 (xs), căn MIN/CENTER
    - Instance "leading", vị trí 4,32, cỡ 48 x 48, bo góc 999, xếp ngang, cách nhau 0, lề trên/phải/dưới/trái 0/0/0/0, căn CENTER/CENTER
      - Text "icon", vị trí 16,44, cỡ 24 x 24, chữ "arrow_back", kiểu ngoài hệ thống (24/24, đậm 400), màu #FFFFFF (ngoài hệ thống)
    - Text "title", vị trí 56,42, cỡ 299 x 28, chữ "Thư viện tư thế", kiểu ngoài hệ thống (22/28, đậm 800), màu #FFFFFF (ngoài hệ thống)
    - Instance "trailing", vị trí 359,32, cỡ 48 x 48, bo góc 999, xếp ngang, cách nhau 0, lề trên/phải/dưới/trái 0/0/0/0, căn CENTER/CENTER
      - Text "icon", vị trí 371,44, cỡ 24 x 24, chữ "settings", kiểu ngoài hệ thống (24/24, đậm 400), màu #FFFFFF (ngoài hệ thống)
  - Frame "Subtitle", vị trí 0,96, cỡ 166 x 16, xếp ngang, cách nhau 0, lề trên/phải/dưới/trái 0/16 (md)/0/16 (md), căn MIN/MIN
    - Text "214 tư thế · 60 tư thế dùng đư", vị trí 16,96, cỡ 134 x 16, chữ "214 tư thế trong 8 nhóm", kiểu bodySmall (12/16, đậm 400), màu #B5B0A8 (ngoài hệ thống)
  - Frame "Group chips", vị trí 0,120, cỡ 709 x 52, xếp ngang, cách nhau 8 (sm), lề trên/phải/dưới/trái 8 (sm)/16 (md)/8 (sm)/16 (md), căn MIN/MIN
    - Instance "Chip", vị trí 16,128, cỡ 76 x 36, nền #FFC83D (ngoài hệ thống), viền #FFFFFF (ngoài hệ thống), dày 2.0, bo góc 18, xếp ngang, cách nhau 0, lề trên/phải/dưới/trái 0/16 (md)/0/16 (md), căn MIN/CENTER
      - Text "label", vị trí 34,136, cỡ 40 x 20, chữ "Tất cả", kiểu titleSmall, labelLarge (14/20, đậm 500), màu #111111 (ngoài hệ thống)
      - Instance "SelectedBadge", vị trí 78,119, cỡ 22 x 22, nền #FFC83D (ngoài hệ thống), viền #FFFFFF (ngoài hệ thống), dày 2.0, bo góc 999, xếp ngang, cách nhau 0, lề trên/phải/dưới/trái 0/0/0/0, căn CENTER/CENTER
        - Text "icon", vị trí 82,123, cỡ 14 x 14, chữ "check", kiểu ngoài hệ thống (14/14, đậm 400), màu #111111 (ngoài hệ thống)
    - Instance "Chip", vị trí 100,128, cỡ 79 x 36, nền #1E1E1E (ngoài hệ thống), viền #FFFFFF (ngoài hệ thống), dày 2.0, bo góc 18, xếp ngang, cách nhau 0, lề trên/phải/dưới/trái 0/16 (md)/0/16 (md), căn MIN/CENTER
      - Text "label", vị trí 118,136, cỡ 43 x 20, chữ "Đã lưu", kiểu titleSmall, labelLarge (14/20, đậm 500), màu #FFFFFF (ngoài hệ thống)
    - Instance "Chip", vị trí 187,128, cỡ 56 x 36, nền #1E1E1E (ngoài hệ thống), viền #FFFFFF (ngoài hệ thống), dày 2.0, bo góc 18, xếp ngang, cách nhau 0, lề trên/phải/dưới/trái 0/16 (md)/0/16 (md), căn MIN/CENTER
      - Text "label", vị trí 205,136, cỡ 20 x 20, chữ "Nữ", kiểu titleSmall, labelLarge (14/20, đậm 500), màu #FFFFFF (ngoài hệ thống)
    - Instance "Chip", vị trí 251,128, cỡ 68 x 36, nền #1E1E1E (ngoài hệ thống), viền #FFFFFF (ngoài hệ thống), dày 2.0, bo góc 18, xếp ngang, cách nhau 0, lề trên/phải/dưới/trái 0/16 (md)/0/16 (md), căn MIN/CENTER
      - Text "label", vị trí 269,136, cỡ 32 x 20, chữ "Nam", kiểu titleSmall, labelLarge (14/20, đậm 500), màu #FFFFFF (ngoài hệ thống)
    - Instance "Chip", vị trí 327,128, cỡ 87 x 36, nền #1E1E1E (ngoài hệ thống), viền #FFFFFF (ngoài hệ thống), dày 2.0, bo góc 18, xếp ngang, cách nhau 0, lề trên/phải/dưới/trái 0/16 (md)/0/16 (md), căn MIN/CENTER
      - Text "label", vị trí 345,136, cỡ 51 x 20, chữ "Cặp đôi", kiểu titleSmall, labelLarge (14/20, đậm 500), màu #FFFFFF (ngoài hệ thống)
    - Instance "Chip", vị trí 422,128, cỡ 82 x 36, nền #1E1E1E (ngoài hệ thống), viền #FFFFFF (ngoài hệ thống), dày 2.0, bo góc 18, xếp ngang, cách nhau 0, lề trên/phải/dưới/trái 0/16 (md)/0/16 (md), căn MIN/CENTER
      - Text "label", vị trí 440,136, cỡ 46 x 20, chữ "Bạn bè", kiểu titleSmall, labelLarge (14/20, đậm 500), màu #FFFFFF (ngoài hệ thống)
    - Instance "Chip", vị trí 512,128, cỡ 91 x 36, nền #1E1E1E (ngoài hệ thống), viền #FFFFFF (ngoài hệ thống), dày 2.0, bo góc 18, xếp ngang, cách nhau 0, lề trên/phải/dưới/trái 0/16 (md)/0/16 (md), căn MIN/CENTER
      - Text "label", vị trí 530,136, cỡ 55 x 20, chữ "Gia đình", kiểu titleSmall, labelLarge (14/20, đậm 500), màu #FFFFFF (ngoài hệ thống)
    - Instance "Chip", vị trí 611,128, cỡ 82 x 36, nền #1E1E1E (ngoài hệ thống), viền #FFFFFF (ngoài hệ thống), dày 2.0, bo góc 18, xếp ngang, cách nhau 0, lề trên/phải/dưới/trái 0/16 (md)/0/16 (md), căn MIN/CENTER
      - Text "label", vị trí 629,136, cỡ 46 x 20, chữ "Du lịch", kiểu titleSmall, labelLarge (14/20, đậm 500), màu #FFFFFF (ngoài hệ thống)
  - Frame "State", vị trí 0,180, cỡ 411 x 711, xếp dọc, cách nhau 0, lề trên/phải/dưới/trái 0/0/0/0, căn CENTER/CENTER
    - Instance "StateMessage", vị trí 40,398, cỡ 331 x 276, xếp dọc, cách nhau 16 (md), lề trên/phải/dưới/trái 24 (lg)/0/24 (lg)/0, căn MIN/CENTER
      - Frame "icon circle", vị trí 170,422, cỡ 72 x 72, nền #FFC83D (ngoài hệ thống), viền #FFFFFF (ngoài hệ thống), dày 2.0, bo góc 999, xếp ngang, cách nhau 0, lề trên/phải/dưới/trái 0/0/0/0, căn CENTER/CENTER
        - Text "icon", vị trí 188,440, cỡ 36 x 36, chữ "error", kiểu ngoài hệ thống (36/36, đậm 400), màu #111111 (ngoài hệ thống)
      - Text "title", vị trí 40,510, cỡ 331 x 28, chữ "Không tải được thư viện", kiểu ngoài hệ thống (22/28, đậm 800), màu #FFFFFF (ngoài hệ thống)
      - Text "body", vị trí 40,554, cỡ 331 x 20, chữ "Đã có lỗi khi tải thư viện. Thử lại sau ít phút.", kiểu bodyMedium (14/20, đậm 400), màu #B5B0A8 (ngoài hệ thống)
      - Instance "button", vị trí 140,590, cỡ 131 x 60, nền #FFFFFF (ngoài hệ thống), viền #FFFFFF (ngoài hệ thống), dày 2.0, bo góc 14, xếp ngang, cách nhau 8 (sm), lề trên/phải/dưới/trái 16 (md)/24 (lg)/16 (md)/24 (lg), căn CENTER/CENTER
        - Text "icon", vị trí 166,610, cỡ 20 x 20, chữ "refresh", kiểu ngoài hệ thống (20/20, đậm 400), màu #111111 (ngoài hệ thống)
        - Text "label", vị trí 194,608, cỡ 51 x 24, chữ "Thử lại", kiểu titleMedium (16/24, đậm 600), màu #111111 (ngoài hệ thống)

## 4. Code tham khảo

Không có. Script không sinh code; AI viết Compose theo mục 2 và 3.
