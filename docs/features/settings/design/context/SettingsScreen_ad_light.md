# SỐ LIỆU THIẾT KẾ: SettingsScreen_ad_light

Sinh bởi `scripts/figma-export.py`. Không sửa tay; thiết kế đổi thì chạy lại script (ADR-0020).

## 1. Nguồn

- Link: https://www.figma.com/design/ZTXse6375eT3KXDUJPBTpe/?node-id=105-2937
- Frame: `105:2937`, giao diện sáng
- Ngày lấy: 2026-11-13

## 2. Giá trị dùng

Đối chiếu với `docs/design-system/tokens.json` theo giá trị. "ngoài hệ thống" nghĩa là không có token
nào trùng; plan quyết định làm tròn về token gần nhất hay thêm token.

| Loại | Giá trị | Token |
| --- | --- | --- |
| khoảng cách | 12 | ngoài hệ thống |
| khoảng cách | 16 | md |
| khoảng cách | 2 | ngoài hệ thống |
| khoảng cách | 24 | lg |
| khoảng cách | 4 | xs |
| khoảng cách | 6 | ngoài hệ thống |
| khoảng cách | 8 | sm |
| kiểu chữ | 10/16, đậm 500 | ngoài hệ thống |
| kiểu chữ | 12/16, đậm 400 | bodySmall |
| kiểu chữ | 13/20, đậm 500 | ngoài hệ thống |
| kiểu chữ | 14/20, đậm 400 | bodyMedium |
| kiểu chữ | 14/20, đậm 500 | titleSmall, labelLarge |
| kiểu chữ | 16/24, đậm 400 | bodyLarge |
| kiểu chữ | 22/28, đậm 800 | titleLarge |
| kiểu chữ | 24/24, đậm 400 | ngoài hệ thống |
| màu chữ | #111111 | secondary, onSecondaryContainer, onTertiary, onBackground, onSurface, outline, onSuccessContainer, category/onCategory, camera/onControl |
| màu chữ | #4A0F22 | onTertiaryContainer |
| màu chữ | #5A5650 | onSurfaceVariant |
| màu nền | #2F6BFF | primary, surfaceTint |
| màu nền | #EAE6DC | surfaceContainerHighest |
| màu nền | #FBF8F1 | surfaceContainerLow |
| màu nền | #FF5C8A | tertiary, category/women |
| màu nền | #FFC83D | secondaryContainer, shadowSticker, category/men |
| màu nền | #FFD6E2 | tertiaryContainer |
| màu nền | #FFFDF8 | background |
| màu nền | #FFFFFF | onPrimary, onSecondary, onError, surface, inverseOnSurface, surfaceBright, surfaceContainerLowest, onSuccess, camera/onCamera, camera/control, camera/overlayStroke |

## 3. Cấu trúc và số liệu

Toạ độ và cỡ tính theo dp, gốc là góc trên trái của khung. Thứ tự từ trên xuống như trong Figma.

- Frame "SettingsScreen_ad_light", vị trí 0,0, cỡ 411 x 891, nền #FFFDF8 (background), xếp dọc, cách nhau 0, lề trên/phải/dưới/trái 24 (lg)/0/0/0, căn MIN/MIN
  - Instance "TopAppBar", vị trí 0,24, cỡ 411 x 64, xếp ngang, cách nhau 4 (xs), lề trên/phải/dưới/trái 0/4 (xs)/0/4 (xs), căn MIN/CENTER
    - Instance "leading", vị trí 4,32, cỡ 48 x 48, bo góc 999, xếp ngang, cách nhau 0, lề trên/phải/dưới/trái 0/0/0/0, căn CENTER/CENTER
      - Text "icon", vị trí 16,44, cỡ 24 x 24, chữ "arrow_back", kiểu ngoài hệ thống (24/24, đậm 400), màu #111111 (secondary, onSecondaryContainer, onTertiary, onBackground, onSurface, outline, onSuccessContainer, category/onCategory, camera/onControl)
    - Text "title", vị trí 56,42, cỡ 351 x 28, chữ "Cài đặt", kiểu titleLarge (22/28, đậm 800), màu #111111 (secondary, onSecondaryContainer, onTertiary, onBackground, onSurface, outline, onSuccessContainer, category/onCategory, camera/onControl)
  - Instance "SectionHeader", vị trí 0,88, cỡ 411 x 52, xếp ngang, cách nhau 0, lề trên/phải/dưới/trái 24 (lg)/16 (md)/8 (sm)/16 (md), căn MIN/MIN
    - Text "title", vị trí 16,112, cỡ 44 x 20, chữ "Chung", kiểu titleSmall, labelLarge (14/20, đậm 500), màu #5A5650 (onSurfaceVariant)
  - Instance "SettingsRow", vị trí 0,140, cỡ 411 x 64, xếp ngang, cách nhau 16 (md), lề trên/phải/dưới/trái 8 (sm)/16 (md)/8 (sm)/16 (md), căn MIN/CENTER
    - Text "leading icon", vị trí 16,160, cỡ 24 x 24, chữ "palette", kiểu ngoài hệ thống (24/24, đậm 400), màu #5A5650 (onSurfaceVariant)
    - Frame "text", vị trí 56,149, cỡ 299 x 46, xếp dọc, cách nhau 2 (ngoài hệ thống), lề trên/phải/dưới/trái 0/0/0/0, căn MIN/MIN
      - Text "title", vị trí 56,149, cỡ 299 x 24, chữ "Giao diện", kiểu bodyLarge (16/24, đậm 400), màu #111111 (secondary, onSecondaryContainer, onTertiary, onBackground, onSurface, outline, onSuccessContainer, category/onCategory, camera/onControl)
      - Text "value", vị trí 56,175, cỡ 299 x 20, chữ "Theo hệ thống", kiểu bodyMedium (14/20, đậm 400), màu #5A5650 (onSurfaceVariant)
    - Text "icon", vị trí 371,160, cỡ 24 x 24, chữ "chevron_right", kiểu ngoài hệ thống (24/24, đậm 400), màu #5A5650 (onSurfaceVariant)
  - Instance "SettingsRow", vị trí 0,204, cỡ 411 x 64, xếp ngang, cách nhau 16 (md), lề trên/phải/dưới/trái 8 (sm)/16 (md)/8 (sm)/16 (md), căn MIN/CENTER
    - Text "leading icon", vị trí 16,224, cỡ 24 x 24, chữ "volume_up", kiểu ngoài hệ thống (24/24, đậm 400), màu #5A5650 (onSurfaceVariant)
    - Frame "text", vị trí 56,224, cỡ 271 x 24, xếp dọc, cách nhau 2 (ngoài hệ thống), lề trên/phải/dưới/trái 0/0/0/0, căn MIN/MIN
      - Text "title", vị trí 56,224, cỡ 271 x 24, chữ "Âm khi chụp", kiểu bodyLarge (16/24, đậm 400), màu #111111 (secondary, onSecondaryContainer, onTertiary, onBackground, onSurface, outline, onSuccessContainer, category/onCategory, camera/onControl)
    - Instance "Switch", vị trí 343,220, cỡ 52 x 32, nền #2F6BFF (primary, surfaceTint), viền #111111 (secondary, onSecondaryContainer, onTertiary, onBackground, onSurface, outline, onSuccessContainer, category/onCategory, camera/onControl), dày 2.0, bo góc 999, xếp ngang, cách nhau 0, lề trên/phải/dưới/trái 0/4 (xs)/0/4 (xs), căn MAX/CENTER
      - Ellipse "knob", vị trí 367,225, cỡ 22 x 22, nền #FFFFFF (onPrimary, onSecondary, onError, surface, inverseOnSurface, surfaceBright, surfaceContainerLowest, onSuccess, camera/onCamera, camera/control, camera/overlayStroke), viền #111111 (secondary, onSecondaryContainer, onTertiary, onBackground, onSurface, outline, onSuccessContainer, category/onCategory, camera/onControl), dày 2.0
  - Instance "SettingsRow", vị trí 0,268, cỡ 411 x 64, xếp ngang, cách nhau 16 (md), lề trên/phải/dưới/trái 8 (sm)/16 (md)/8 (sm)/16 (md), căn MIN/CENTER
    - Text "leading icon", vị trí 16,288, cỡ 24 x 24, chữ "language", kiểu ngoài hệ thống (24/24, đậm 400), màu #5A5650 (onSurfaceVariant)
    - Frame "text", vị trí 56,277, cỡ 299 x 46, xếp dọc, cách nhau 2 (ngoài hệ thống), lề trên/phải/dưới/trái 0/0/0/0, căn MIN/MIN
      - Text "title", vị trí 56,277, cỡ 299 x 24, chữ "Ngôn ngữ", kiểu bodyLarge (16/24, đậm 400), màu #111111 (secondary, onSecondaryContainer, onTertiary, onBackground, onSurface, outline, onSuccessContainer, category/onCategory, camera/onControl)
      - Text "value", vị trí 56,303, cỡ 299 x 20, chữ "Tiếng Việt", kiểu bodyMedium (14/20, đậm 400), màu #5A5650 (onSurfaceVariant)
    - Text "icon", vị trí 371,288, cỡ 24 x 24, chữ "chevron_right", kiểu ngoài hệ thống (24/24, đậm 400), màu #5A5650 (onSurfaceVariant)
  - Instance "SectionHeader", vị trí 0,332, cỡ 411 x 52, xếp ngang, cách nhau 0, lề trên/phải/dưới/trái 24 (lg)/16 (md)/8 (sm)/16 (md), căn MIN/MIN
    - Text "title", vị trí 16,356, cỡ 99 x 20, chữ "Quyền riêng tư", kiểu titleSmall, labelLarge (14/20, đậm 500), màu #5A5650 (onSurfaceVariant)
  - Instance "SettingsRow", vị trí 0,384, cỡ 411 x 64, xếp ngang, cách nhau 16 (md), lề trên/phải/dưới/trái 8 (sm)/16 (md)/8 (sm)/16 (md), căn MIN/CENTER
    - Text "leading icon", vị trí 16,404, cỡ 24 x 24, chữ "shield_person", kiểu ngoài hệ thống (24/24, đậm 400), màu #5A5650 (onSurfaceVariant)
    - Frame "text", vị trí 56,404, cỡ 299 x 24, xếp dọc, cách nhau 2 (ngoài hệ thống), lề trên/phải/dưới/trái 0/0/0/0, căn MIN/MIN
      - Text "title", vị trí 56,404, cỡ 299 x 24, chữ "Tùy chọn quyền riêng tư", kiểu bodyLarge (16/24, đậm 400), màu #111111 (secondary, onSecondaryContainer, onTertiary, onBackground, onSurface, outline, onSuccessContainer, category/onCategory, camera/onControl)
    - Text "icon", vị trí 371,404, cỡ 24 x 24, chữ "open_in_new", kiểu ngoài hệ thống (24/24, đậm 400), màu #5A5650 (onSurfaceVariant)
  - Instance "SettingsRow", vị trí 0,448, cỡ 411 x 64, xếp ngang, cách nhau 16 (md), lề trên/phải/dưới/trái 8 (sm)/16 (md)/8 (sm)/16 (md), căn MIN/CENTER
    - Text "leading icon", vị trí 16,468, cỡ 24 x 24, chữ "policy", kiểu ngoài hệ thống (24/24, đậm 400), màu #5A5650 (onSurfaceVariant)
    - Frame "text", vị trí 56,468, cỡ 299 x 24, xếp dọc, cách nhau 2 (ngoài hệ thống), lề trên/phải/dưới/trái 0/0/0/0, căn MIN/MIN
      - Text "title", vị trí 56,468, cỡ 299 x 24, chữ "Chính sách quyền riêng tư", kiểu bodyLarge (16/24, đậm 400), màu #111111 (secondary, onSecondaryContainer, onTertiary, onBackground, onSurface, outline, onSuccessContainer, category/onCategory, camera/onControl)
    - Text "icon", vị trí 371,468, cỡ 24 x 24, chữ "open_in_new", kiểu ngoài hệ thống (24/24, đậm 400), màu #5A5650 (onSurfaceVariant)
  - Instance "SectionHeader", vị trí 0,512, cỡ 411 x 52, xếp ngang, cách nhau 0, lề trên/phải/dưới/trái 24 (lg)/16 (md)/8 (sm)/16 (md), căn MIN/MIN
    - Text "title", vị trí 16,536, cỡ 63 x 20, chữ "Thông tin", kiểu titleSmall, labelLarge (14/20, đậm 500), màu #5A5650 (onSurfaceVariant)
  - Instance "SettingsRow", vị trí 0,564, cỡ 411 x 64, xếp ngang, cách nhau 16 (md), lề trên/phải/dưới/trái 8 (sm)/16 (md)/8 (sm)/16 (md), căn MIN/CENTER
    - Text "leading icon", vị trí 16,584, cỡ 24 x 24, chữ "info", kiểu ngoài hệ thống (24/24, đậm 400), màu #5A5650 (onSurfaceVariant)
    - Frame "text", vị trí 56,573, cỡ 339 x 46, xếp dọc, cách nhau 2 (ngoài hệ thống), lề trên/phải/dưới/trái 0/0/0/0, căn MIN/MIN
      - Text "title", vị trí 56,573, cỡ 339 x 24, chữ "Phiên bản", kiểu bodyLarge (16/24, đậm 400), màu #111111 (secondary, onSecondaryContainer, onTertiary, onBackground, onSurface, outline, onSuccessContainer, category/onCategory, camera/onControl)
      - Text "value", vị trí 56,599, cỡ 339 x 20, chữ "1.0.0", kiểu bodyMedium (14/20, đậm 400), màu #5A5650 (onSurfaceVariant)
  - Instance "NativeAdSmall", vị trí 16,795, cỡ 379 x 80, nền #FBF8F1 (surfaceContainerLow), viền #D9D4C8 (outlineVariant), dày 1.0, bo góc 12, xếp ngang, cách nhau 12 (ngoài hệ thống), lề trên/phải/dưới/trái 0/24 (lg)/0/12 (ngoài hệ thống), căn MIN/CENTER
    - Frame "icon", vị trí 29,817, cỡ 36 x 36, nền #FFC83D (secondaryContainer, shadowSticker, category/men), bo góc 8
    - Frame "text", vị trí 77,816, cỡ 201 x 38, xếp dọc, cách nhau 2 (ngoài hệ thống), lề trên/phải/dưới/trái 0/0/0/0, căn MIN/MIN
      - Text "headline", vị trí 77,816, cỡ 201 x 20, chữ "Headline of the ad", kiểu titleSmall, labelLarge (14/20, đậm 500), màu #111111 (secondary, onSecondaryContainer, onTertiary, onBackground, onSurface, outline, onSuccessContainer, category/onCategory, camera/onControl)
      - Frame "badge line", vị trí 77,838, cỡ 201 x 16, xếp ngang, cách nhau 6 (ngoài hệ thống), lề trên/phải/dưới/trái 0/0/0/0, căn MIN/CENTER
        - Frame "badge", vị trí 77,838, cỡ 21 x 16, nền #FF5C8A (tertiary, category/women), bo góc 4, xếp ngang, cách nhau 0, lề trên/phải/dưới/trái 0/4 (xs)/0/4 (xs), căn MIN/CENTER
          - Text "Ad", vị trí 81,838, cỡ 13 x 16, chữ "Ad", kiểu ngoài hệ thống (10/16, đậm 500), màu #111111 (secondary, onSecondaryContainer, onTertiary, onBackground, onSurface, outline, onSuccessContainer, category/onCategory, camera/onControl)
        - Text "advertiser", vị trí 104,838, cỡ 174 x 16, chữ "Advertiser or short body", kiểu bodySmall (12/16, đậm 400), màu #5A5650 (onSurfaceVariant)
    - Frame "cta", vị trí 290,818, cỡ 80 x 34, nền #FFD6E2 (tertiaryContainer), bo góc 8, xếp ngang, cách nhau 0, lề trên/phải/dưới/trái 0/16 (md)/0/16 (md), căn CENTER/CENTER
      - Text "Install", vị trí 311,825, cỡ 38 x 20, chữ "Install", kiểu ngoài hệ thống (13/20, đậm 500), màu #4A0F22 (onTertiaryContainer)
    - Frame "AdChoices", vị trí 375,799, cỡ 16 x 16, nền #EAE6DC (surfaceContainerHighest), bo góc 4

## 4. Code tham khảo

Không có. Script không sinh code; AI viết Compose theo mục 2 và 3.
