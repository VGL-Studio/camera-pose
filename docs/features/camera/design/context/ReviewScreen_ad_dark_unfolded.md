# SỐ LIỆU THIẾT KẾ: ReviewScreen_ad_dark_unfolded

Sinh bởi `scripts/figma-export.py`. Không sửa tay; thiết kế đổi thì chạy lại script (ADR-0020).

## 1. Nguồn

- Link: https://www.figma.com/design/ZTXse6375eT3KXDUJPBTpe/?node-id=105-3119
- Frame: `105:3119`, giao diện tối
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
| kiểu chữ | 14/20, đậm 500 | titleSmall, labelLarge |
| kiểu chữ | 16/24, đậm 600 | titleMedium |
| kiểu chữ | 20/20, đậm 400 | ngoài hệ thống |
| kiểu chữ | 22/28, đậm 800 | titleLarge |
| kiểu chữ | 24/24, đậm 400 | ngoài hệ thống |
| kiểu chữ | 240/240, đậm 400 | ngoài hệ thống |
| màu chữ | #111111 | onSecondary, onSecondaryContainer, inverseOnSurface, onSuccessContainer, category/onCategory, camera/onControl |
| màu chữ | #3A0A18 | onTertiary |
| màu chữ | #B5B0A8 | onSurfaceVariant, camera/onSurfaceVariant |
| màu chữ | #FFD6E2 | onTertiaryContainer |
| màu chữ | #FFFFFF | secondary, onBackground, onSurface, outline, camera/onCamera, camera/control, camera/overlayStroke |
| màu nền | #0A0A0B | camera/background |
| màu nền | #181818 | surfaceContainerLow |
| màu nền | #1E1E1E | surface, surfaceContainer, camera/surface |
| màu nền | #2E2E2E | surfaceContainerHighest |
| màu nền | #6B1D35 | tertiaryContainer |
| màu nền | #FF8AAA | tertiary |
| màu nền | #FFC83D | secondaryContainer, shadowSticker, category/men |
| màu nền | #FFFFFF | secondary, onBackground, onSurface, outline, camera/onCamera, camera/control, camera/overlayStroke |

## 3. Cấu trúc và số liệu

Toạ độ và cỡ tính theo dp, gốc là góc trên trái của khung. Thứ tự từ trên xuống như trong Figma.

- Frame "ReviewScreen_ad_dark_unfolded", vị trí 0,0, cỡ 673 x 841, nền #0A0A0B (camera/background)
  - Instance "TopAppBar", vị trí 0,8, cỡ 673 x 64, xếp ngang, cách nhau 4 (xs), lề trên/phải/dưới/trái 0/4 (xs)/0/4 (xs), căn MIN/CENTER
    - Instance "leading", vị trí 4,16, cỡ 48 x 48, bo góc 999, xếp ngang, cách nhau 0, lề trên/phải/dưới/trái 0/0/0/0, căn CENTER/CENTER
      - Text "icon", vị trí 16,28, cỡ 24 x 24, chữ "close", kiểu ngoài hệ thống (24/24, đậm 400), màu #FFFFFF (secondary, onBackground, onSurface, outline, camera/onCamera, camera/control, camera/overlayStroke)
    - Text "title", vị trí 56,26, cỡ 613 x 28, chữ "Xem lại", kiểu titleLarge (22/28, đậm 800), màu #FFFFFF (secondary, onBackground, onSurface, outline, camera/onCamera, camera/control, camera/overlayStroke)
  - Frame "Ảnh vừa chụp (ảnh tạm)", vị trí 16,72, cỡ 450 x 600, nền #1E1E1E (surface, surfaceContainer, camera/surface), viền #FFFFFF (secondary, onBackground, onSurface, outline, camera/onCamera, camera/control, camera/overlayStroke), dày 2.0, bo góc 16
    - Text "icon", vị trí 121,432, cỡ 240 x 240, chữ "person", kiểu ngoài hệ thống (240/240, đậm 400), màu #B5B0A8 (onSurfaceVariant, camera/onSurfaceVariant)
  - Frame "actions", vị trí 482,72, cỡ 127 x 192, xếp dọc, cách nhau 24 (lg), lề trên/phải/dưới/trái 0/0/0/0, căn MIN/MIN
    - Frame "action Chụp lại", vị trí 482,72, cỡ 110 x 48, xếp ngang, cách nhau 8 (sm), lề trên/phải/dưới/trái 0/0/0/0, căn MIN/CENTER
      - Frame "button", vị trí 482,72, cỡ 48 x 48, nền #FFFFFF (secondary, onBackground, onSurface, outline, camera/onCamera, camera/control, camera/overlayStroke), viền #111111 (onSecondary, onSecondaryContainer, inverseOnSurface, onSuccessContainer, category/onCategory, camera/onControl), dày 2.0, bo góc 999, xếp ngang, cách nhau 0, lề trên/phải/dưới/trái 0/0/0/0, căn CENTER/CENTER
        - Text "icon", vị trí 494,84, cỡ 24 x 24, chữ "replay", kiểu ngoài hệ thống (24/24, đậm 400), màu #111111 (onSecondary, onSecondaryContainer, inverseOnSurface, onSuccessContainer, category/onCategory, camera/onControl)
      - Text "Chụp lại", vị trí 538,86, cỡ 54 x 20, chữ "Chụp lại", kiểu titleSmall, labelLarge (14/20, đậm 500), màu #FFFFFF (secondary, onBackground, onSurface, outline, camera/onCamera, camera/control, camera/overlayStroke)
    - Frame "action Chia sẻ", vị trí 482,144, cỡ 105 x 48, xếp ngang, cách nhau 8 (sm), lề trên/phải/dưới/trái 0/0/0/0, căn MIN/CENTER
      - Frame "button", vị trí 482,144, cỡ 48 x 48, nền #FFFFFF (secondary, onBackground, onSurface, outline, camera/onCamera, camera/control, camera/overlayStroke), viền #111111 (onSecondary, onSecondaryContainer, inverseOnSurface, onSuccessContainer, category/onCategory, camera/onControl), dày 2.0, bo góc 999, xếp ngang, cách nhau 0, lề trên/phải/dưới/trái 0/0/0/0, căn CENTER/CENTER
        - Text "icon", vị trí 494,156, cỡ 24 x 24, chữ "share", kiểu ngoài hệ thống (24/24, đậm 400), màu #111111 (onSecondary, onSecondaryContainer, inverseOnSurface, onSuccessContainer, category/onCategory, camera/onControl)
      - Text "Chia sẻ", vị trí 538,158, cỡ 49 x 20, chữ "Chia sẻ", kiểu titleSmall, labelLarge (14/20, đậm 500), màu #FFFFFF (secondary, onBackground, onSurface, outline, camera/onCamera, camera/control, camera/overlayStroke)
    - Frame "action Tư thế tiếp", vị trí 482,216, cỡ 127 x 48, xếp ngang, cách nhau 8 (sm), lề trên/phải/dưới/trái 0/0/0/0, căn MIN/CENTER
      - Frame "button", vị trí 482,216, cỡ 48 x 48, nền #FFFFFF (secondary, onBackground, onSurface, outline, camera/onCamera, camera/control, camera/overlayStroke), viền #111111 (onSecondary, onSecondaryContainer, inverseOnSurface, onSuccessContainer, category/onCategory, camera/onControl), dày 2.0, bo góc 999, xếp ngang, cách nhau 0, lề trên/phải/dưới/trái 0/0/0/0, căn CENTER/CENTER
        - Text "icon", vị trí 494,228, cỡ 24 x 24, chữ "skip_next", kiểu ngoài hệ thống (24/24, đậm 400), màu #111111 (onSecondary, onSecondaryContainer, inverseOnSurface, onSuccessContainer, category/onCategory, camera/onControl)
      - Text "Tư thế tiếp", vị trí 538,230, cỡ 71 x 20, chữ "Tư thế tiếp", kiểu titleSmall, labelLarge (14/20, đậm 500), màu #FFFFFF (secondary, onBackground, onSurface, outline, camera/onCamera, camera/control, camera/overlayStroke)
  - Instance "Button", vị trí 482,669, cỡ 175 x 60, nền #FFFFFF (secondary, onBackground, onSurface, outline, camera/onCamera, camera/control, camera/overlayStroke), viền #FFFFFF (secondary, onBackground, onSurface, outline, camera/onCamera, camera/control, camera/overlayStroke), dày 2.0, bo góc 14, xếp ngang, cách nhau 8 (sm), lề trên/phải/dưới/trái 16 (md)/24 (lg)/16 (md)/24 (lg), căn CENTER/CENTER
    - Text "icon", vị trí 525,689, cỡ 20 x 20, chữ "download", kiểu ngoài hệ thống (20/20, đậm 400), màu #111111 (onSecondary, onSecondaryContainer, inverseOnSurface, onSuccessContainer, category/onCategory, camera/onControl)
    - Text "label", vị trí 553,687, cỡ 61 x 24, chữ "Lưu ảnh", kiểu titleMedium (16/24, đậm 600), màu #111111 (onSecondary, onSecondaryContainer, inverseOnSurface, onSuccessContainer, category/onCategory, camera/onControl)
  - Instance "NativeAdSmall", vị trí 16,745, cỡ 641 x 80, nền #181818 (surfaceContainerLow), viền #4A4744 (outlineVariant), dày 1.0, bo góc 12, xếp ngang, cách nhau 12 (ngoài hệ thống), lề trên/phải/dưới/trái 0/24 (lg)/0/12 (ngoài hệ thống), căn MIN/CENTER
    - Frame "icon", vị trí 29,767, cỡ 36 x 36, nền #FFC83D (secondaryContainer, shadowSticker, category/men), bo góc 8
    - Frame "text", vị trí 77,766, cỡ 463 x 38, xếp dọc, cách nhau 2 (ngoài hệ thống), lề trên/phải/dưới/trái 0/0/0/0, căn MIN/MIN
      - Text "headline", vị trí 77,766, cỡ 463 x 20, chữ "Headline of the ad", kiểu titleSmall, labelLarge (14/20, đậm 500), màu #FFFFFF (secondary, onBackground, onSurface, outline, camera/onCamera, camera/control, camera/overlayStroke)
      - Frame "badge line", vị trí 77,788, cỡ 463 x 16, xếp ngang, cách nhau 6 (ngoài hệ thống), lề trên/phải/dưới/trái 0/0/0/0, căn MIN/CENTER
        - Frame "badge", vị trí 77,788, cỡ 21 x 16, nền #FF8AAA (tertiary), bo góc 4, xếp ngang, cách nhau 0, lề trên/phải/dưới/trái 0/4 (xs)/0/4 (xs), căn MIN/CENTER
          - Text "Ad", vị trí 81,788, cỡ 13 x 16, chữ "Ad", kiểu ngoài hệ thống (10/16, đậm 500), màu #3A0A18 (onTertiary)
        - Text "advertiser", vị trí 104,788, cỡ 436 x 16, chữ "Advertiser or short body", kiểu bodySmall (12/16, đậm 400), màu #B5B0A8 (onSurfaceVariant, camera/onSurfaceVariant)
    - Frame "cta", vị trí 552,768, cỡ 80 x 34, nền #6B1D35 (tertiaryContainer), bo góc 8, xếp ngang, cách nhau 0, lề trên/phải/dưới/trái 0/16 (md)/0/16 (md), căn CENTER/CENTER
      - Text "Install", vị trí 573,775, cỡ 38 x 20, chữ "Install", kiểu ngoài hệ thống (13/20, đậm 500), màu #FFD6E2 (onTertiaryContainer)
    - Frame "AdChoices", vị trí 637,749, cỡ 16 x 16, nền #2E2E2E (surfaceContainerHighest), bo góc 4

## 4. Code tham khảo

Không có. Script không sinh code; AI viết Compose theo mục 2 và 3.
