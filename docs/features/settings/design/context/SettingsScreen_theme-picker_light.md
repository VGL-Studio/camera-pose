# SỐ LIỆU THIẾT KẾ: SettingsScreen_theme-picker_light

Sinh bởi `scripts/figma-export.py`. Không sửa tay; thiết kế đổi thì chạy lại script (ADR-0020).

## 1. Nguồn

- Link: https://www.figma.com/design/ZTXse6375eT3KXDUJPBTpe/?node-id=28-2267
- Frame: `28:2267`, giao diện sáng
- Ngày lấy: 2026-10-21

## 2. Giá trị dùng

Đối chiếu với `docs/design-system/tokens.json` theo giá trị. "ngoài hệ thống" nghĩa là không có token
nào trùng; plan quyết định làm tròn về token gần nhất hay thêm token.

| Loại | Giá trị | Token |
| --- | --- | --- |
| khoảng cách | 16 | md |
| khoảng cách | 2 | ngoài hệ thống |
| khoảng cách | 24 | lg |
| khoảng cách | 4 | xs |
| khoảng cách | 8 | sm |
| kiểu chữ | 14/20, đậm 400 | bodyMedium |
| kiểu chữ | 14/20, đậm 500 | titleSmall, labelLarge |
| kiểu chữ | 16/24, đậm 400 | bodyLarge |
| kiểu chữ | 22/28, đậm 800 | ngoài hệ thống |
| kiểu chữ | 24/24, đậm 400 | ngoài hệ thống |
| màu chữ | #111111 | ngoài hệ thống |
| màu chữ | #5A5650 | ngoài hệ thống |
| màu nền | #000000 (độ mờ 0.50) | ngoài hệ thống |
| màu nền | #111111 | ngoài hệ thống |
| màu nền | #2F6BFF | ngoài hệ thống |
| màu nền | #FFC83D | ngoài hệ thống |
| màu nền | #FFFDF8 | ngoài hệ thống |
| màu nền | #FFFFFF | onPrimary, onSecondary, onTertiary, onError, surfaceContainerLowest |

## 3. Cấu trúc và số liệu

Toạ độ và cỡ tính theo dp, gốc là góc trên trái của khung. Thứ tự từ trên xuống như trong Figma.

- Frame "SettingsScreen_theme-picker_light", vị trí 0,0, cỡ 411 x 891, nền #FFFDF8 (ngoài hệ thống), xếp dọc, cách nhau 0, lề trên/phải/dưới/trái 24 (lg)/0/0/0, căn MIN/MIN
  - Instance "TopAppBar", vị trí 0,24, cỡ 411 x 64, xếp ngang, cách nhau 4 (xs), lề trên/phải/dưới/trái 0/4 (xs)/0/4 (xs), căn MIN/CENTER
    - Instance "leading", vị trí 4,32, cỡ 48 x 48, bo góc 999, xếp ngang, cách nhau 0, lề trên/phải/dưới/trái 0/0/0/0, căn CENTER/CENTER
      - Text "icon", vị trí 16,44, cỡ 24 x 24, chữ "arrow_back", kiểu ngoài hệ thống (24/24, đậm 400), màu #111111 (ngoài hệ thống)
    - Text "title", vị trí 56,42, cỡ 351 x 28, chữ "Cài đặt", kiểu ngoài hệ thống (22/28, đậm 800), màu #111111 (ngoài hệ thống)
  - Instance "SectionHeader", vị trí 0,88, cỡ 411 x 52, xếp ngang, cách nhau 0, lề trên/phải/dưới/trái 24 (lg)/16 (md)/8 (sm)/16 (md), căn MIN/MIN
    - Text "title", vị trí 16,112, cỡ 44 x 20, chữ "Chung", kiểu titleSmall, labelLarge (14/20, đậm 500), màu #5A5650 (ngoài hệ thống)
  - Instance "SettingsRow", vị trí 0,140, cỡ 411 x 64, xếp ngang, cách nhau 16 (md), lề trên/phải/dưới/trái 8 (sm)/16 (md)/8 (sm)/16 (md), căn MIN/CENTER
    - Text "leading icon", vị trí 16,160, cỡ 24 x 24, chữ "palette", kiểu ngoài hệ thống (24/24, đậm 400), màu #5A5650 (ngoài hệ thống)
    - Frame "text", vị trí 56,149, cỡ 299 x 46, xếp dọc, cách nhau 2 (ngoài hệ thống), lề trên/phải/dưới/trái 0/0/0/0, căn MIN/MIN
      - Text "title", vị trí 56,149, cỡ 299 x 24, chữ "Giao diện", kiểu bodyLarge (16/24, đậm 400), màu #111111 (ngoài hệ thống)
      - Text "value", vị trí 56,175, cỡ 299 x 20, chữ "Theo hệ thống", kiểu bodyMedium (14/20, đậm 400), màu #5A5650 (ngoài hệ thống)
    - Text "icon", vị trí 371,160, cỡ 24 x 24, chữ "chevron_right", kiểu ngoài hệ thống (24/24, đậm 400), màu #5A5650 (ngoài hệ thống)
  - Instance "SettingsRow", vị trí 0,204, cỡ 411 x 64, xếp ngang, cách nhau 16 (md), lề trên/phải/dưới/trái 8 (sm)/16 (md)/8 (sm)/16 (md), căn MIN/CENTER
    - Text "leading icon", vị trí 16,224, cỡ 24 x 24, chữ "volume_up", kiểu ngoài hệ thống (24/24, đậm 400), màu #5A5650 (ngoài hệ thống)
    - Frame "text", vị trí 56,224, cỡ 271 x 24, xếp dọc, cách nhau 2 (ngoài hệ thống), lề trên/phải/dưới/trái 0/0/0/0, căn MIN/MIN
      - Text "title", vị trí 56,224, cỡ 271 x 24, chữ "Âm khi chụp", kiểu bodyLarge (16/24, đậm 400), màu #111111 (ngoài hệ thống)
    - Instance "Switch", vị trí 343,220, cỡ 52 x 32, nền #2F6BFF (ngoài hệ thống), viền #111111 (ngoài hệ thống), dày 2.0, bo góc 999, xếp ngang, cách nhau 0, lề trên/phải/dưới/trái 0/4 (xs)/0/4 (xs), căn MAX/CENTER
      - Ellipse "knob", vị trí 367,225, cỡ 22 x 22, nền #FFFFFF (onPrimary, onSecondary, onTertiary, onError, surfaceContainerLowest), viền #111111 (ngoài hệ thống), dày 2.0
  - Instance "SettingsRow", vị trí 0,268, cỡ 411 x 64, xếp ngang, cách nhau 16 (md), lề trên/phải/dưới/trái 8 (sm)/16 (md)/8 (sm)/16 (md), căn MIN/CENTER
    - Text "leading icon", vị trí 16,288, cỡ 24 x 24, chữ "language", kiểu ngoài hệ thống (24/24, đậm 400), màu #5A5650 (ngoài hệ thống)
    - Frame "text", vị trí 56,277, cỡ 299 x 46, xếp dọc, cách nhau 2 (ngoài hệ thống), lề trên/phải/dưới/trái 0/0/0/0, căn MIN/MIN
      - Text "title", vị trí 56,277, cỡ 299 x 24, chữ "Ngôn ngữ", kiểu bodyLarge (16/24, đậm 400), màu #111111 (ngoài hệ thống)
      - Text "value", vị trí 56,303, cỡ 299 x 20, chữ "Tiếng Việt", kiểu bodyMedium (14/20, đậm 400), màu #5A5650 (ngoài hệ thống)
    - Text "icon", vị trí 371,288, cỡ 24 x 24, chữ "chevron_right", kiểu ngoài hệ thống (24/24, đậm 400), màu #5A5650 (ngoài hệ thống)
  - Instance "SectionHeader", vị trí 0,332, cỡ 411 x 52, xếp ngang, cách nhau 0, lề trên/phải/dưới/trái 24 (lg)/16 (md)/8 (sm)/16 (md), căn MIN/MIN
    - Text "title", vị trí 16,356, cỡ 99 x 20, chữ "Quyền riêng tư", kiểu titleSmall, labelLarge (14/20, đậm 500), màu #5A5650 (ngoài hệ thống)
  - Instance "SettingsRow", vị trí 0,384, cỡ 411 x 64, xếp ngang, cách nhau 16 (md), lề trên/phải/dưới/trái 8 (sm)/16 (md)/8 (sm)/16 (md), căn MIN/CENTER
    - Text "leading icon", vị trí 16,404, cỡ 24 x 24, chữ "shield_person", kiểu ngoài hệ thống (24/24, đậm 400), màu #5A5650 (ngoài hệ thống)
    - Frame "text", vị trí 56,404, cỡ 299 x 24, xếp dọc, cách nhau 2 (ngoài hệ thống), lề trên/phải/dưới/trái 0/0/0/0, căn MIN/MIN
      - Text "title", vị trí 56,404, cỡ 299 x 24, chữ "Tùy chọn quyền riêng tư", kiểu bodyLarge (16/24, đậm 400), màu #111111 (ngoài hệ thống)
    - Text "icon", vị trí 371,404, cỡ 24 x 24, chữ "open_in_new", kiểu ngoài hệ thống (24/24, đậm 400), màu #5A5650 (ngoài hệ thống)
  - Instance "SettingsRow", vị trí 0,448, cỡ 411 x 64, xếp ngang, cách nhau 16 (md), lề trên/phải/dưới/trái 8 (sm)/16 (md)/8 (sm)/16 (md), căn MIN/CENTER
    - Text "leading icon", vị trí 16,468, cỡ 24 x 24, chữ "policy", kiểu ngoài hệ thống (24/24, đậm 400), màu #5A5650 (ngoài hệ thống)
    - Frame "text", vị trí 56,468, cỡ 299 x 24, xếp dọc, cách nhau 2 (ngoài hệ thống), lề trên/phải/dưới/trái 0/0/0/0, căn MIN/MIN
      - Text "title", vị trí 56,468, cỡ 299 x 24, chữ "Chính sách quyền riêng tư", kiểu bodyLarge (16/24, đậm 400), màu #111111 (ngoài hệ thống)
    - Text "icon", vị trí 371,468, cỡ 24 x 24, chữ "open_in_new", kiểu ngoài hệ thống (24/24, đậm 400), màu #5A5650 (ngoài hệ thống)
  - Instance "SectionHeader", vị trí 0,512, cỡ 411 x 52, xếp ngang, cách nhau 0, lề trên/phải/dưới/trái 24 (lg)/16 (md)/8 (sm)/16 (md), căn MIN/MIN
    - Text "title", vị trí 16,536, cỡ 63 x 20, chữ "Thông tin", kiểu titleSmall, labelLarge (14/20, đậm 500), màu #5A5650 (ngoài hệ thống)
  - Instance "SettingsRow", vị trí 0,564, cỡ 411 x 64, xếp ngang, cách nhau 16 (md), lề trên/phải/dưới/trái 8 (sm)/16 (md)/8 (sm)/16 (md), căn MIN/CENTER
    - Text "leading icon", vị trí 16,584, cỡ 24 x 24, chữ "info", kiểu ngoài hệ thống (24/24, đậm 400), màu #5A5650 (ngoài hệ thống)
    - Frame "text", vị trí 56,573, cỡ 339 x 46, xếp dọc, cách nhau 2 (ngoài hệ thống), lề trên/phải/dưới/trái 0/0/0/0, căn MIN/MIN
      - Text "title", vị trí 56,573, cỡ 339 x 24, chữ "Phiên bản", kiểu bodyLarge (16/24, đậm 400), màu #111111 (ngoài hệ thống)
      - Text "value", vị trí 56,599, cỡ 339 x 20, chữ "1.0.0", kiểu bodyMedium (14/20, đậm 400), màu #5A5650 (ngoài hệ thống)
  - Rectangle "scrim", vị trí 0,0, cỡ 411 x 891, nền #000000 (độ mờ 0.50) (ngoài hệ thống)
  - Instance "ChoiceDialog", vị trí 40,302, cỡ 331 x 288, nền #FFFFFF (onPrimary, onSecondary, onTertiary, onError, surfaceContainerLowest), viền #111111 (ngoài hệ thống), dày 2.0, bo góc 24, xếp dọc, cách nhau 8 (sm), lề trên/phải/dưới/trái 24 (lg)/24 (lg)/24 (lg)/24 (lg), căn MIN/MIN
    - Text "title", vị trí 66,328, cỡ 279 x 28, chữ "Giao diện", kiểu ngoài hệ thống (22/28, đậm 800), màu #111111 (ngoài hệ thống)
    - Frame "options", vị trí 66,364, cỡ 279 x 144, xếp dọc, cách nhau 0, lề trên/phải/dưới/trái 0/0/0/0, căn MIN/MIN
      - Instance "RadioRow", vị trí 66,364, cỡ 279 x 48, xếp ngang, cách nhau 16 (md), lề trên/phải/dưới/trái 0/0/0/0, căn MIN/CENTER
        - Frame "radio", vị trí 66,376, cỡ 24 x 24, nền #FFC83D (ngoài hệ thống), viền #111111 (ngoài hệ thống), dày 2.0, bo góc 999, xếp ngang, cách nhau 0, lề trên/phải/dưới/trái 0/0/0/0, căn CENTER/CENTER
          - Ellipse "dot", vị trí 73,382, cỡ 10 x 10, nền #111111 (ngoài hệ thống)
        - Text "label", vị trí 106,376, cỡ 239 x 24, chữ "Theo hệ thống", kiểu bodyLarge (16/24, đậm 400), màu #111111 (ngoài hệ thống)
      - Instance "RadioRow", vị trí 66,412, cỡ 279 x 48, xếp ngang, cách nhau 16 (md), lề trên/phải/dưới/trái 0/0/0/0, căn MIN/CENTER
        - Frame "radio", vị trí 66,424, cỡ 24 x 24, nền #FFFFFF (onPrimary, onSecondary, onTertiary, onError, surfaceContainerLowest), viền #111111 (ngoài hệ thống), dày 2.0, bo góc 999, xếp ngang, cách nhau 0, lề trên/phải/dưới/trái 0/0/0/0, căn CENTER/CENTER
        - Text "label", vị trí 106,424, cỡ 239 x 24, chữ "Sáng", kiểu bodyLarge (16/24, đậm 400), màu #111111 (ngoài hệ thống)
      - Instance "RadioRow", vị trí 66,460, cỡ 279 x 48, xếp ngang, cách nhau 16 (md), lề trên/phải/dưới/trái 0/0/0/0, căn MIN/CENTER
        - Frame "radio", vị trí 66,472, cỡ 24 x 24, nền #FFFFFF (onPrimary, onSecondary, onTertiary, onError, surfaceContainerLowest), viền #111111 (ngoài hệ thống), dày 2.0, bo góc 999, xếp ngang, cách nhau 0, lề trên/phải/dưới/trái 0/0/0/0, căn CENTER/CENTER
        - Text "label", vị trí 106,472, cỡ 239 x 24, chữ "Tối", kiểu bodyLarge (16/24, đậm 400), màu #111111 (ngoài hệ thống)
    - Frame "actions", vị trí 66,516, cỡ 279 x 48, xếp ngang, cách nhau 0, lề trên/phải/dưới/trái 0/0/0/0, căn MAX/MIN
      - Frame "cancel", vị trí 286,516, cỡ 59 x 48, xếp ngang, cách nhau 0, lề trên/phải/dưới/trái 0/16 (md)/0/16 (md), căn CENTER/CENTER
        - Text "label", vị trí 302,530, cỡ 27 x 20, chữ "Hủy", kiểu titleSmall, labelLarge (14/20, đậm 500), màu #111111 (ngoài hệ thống)

## 4. Code tham khảo

Không có. Script không sinh code; AI viết Compose theo mục 2 và 3.
