# SỐ LIỆU THIẾT KẾ: PoseDetailScreen_ad_light

Sinh bởi `scripts/figma-export.py`. Không sửa tay; thiết kế đổi thì chạy lại script (ADR-0020).

## 1. Nguồn

- Link: https://www.figma.com/design/ZTXse6375eT3KXDUJPBTpe/?node-id=28-2171
- Frame: `28:2171`, giao diện sáng
- Ngày lấy: 2026-10-30

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
| kiểu chữ | 16/24, đậm 600 | titleMedium |
| kiểu chữ | 20/20, đậm 400 | ngoài hệ thống |
| kiểu chữ | 22/28, đậm 800 | titleLarge |
| kiểu chữ | 24/24, đậm 400 | ngoài hệ thống |
| màu chữ | #111111 | secondary, onSecondaryContainer, onTertiary, onBackground, onSurface, outline, onSuccessContainer, category/onCategory, camera/onControl |
| màu chữ | #4A0F22 | onTertiaryContainer |
| màu chữ | #5A5650 | onSurfaceVariant |
| màu chữ | #FFFFFF | onPrimary, onSecondary, onError, surface, inverseOnSurface, surfaceBright, surfaceContainerLowest, onSuccess, camera/onCamera, camera/control, camera/overlayStroke |
| màu nền | #111111 | secondary, onSecondaryContainer, onTertiary, onBackground, onSurface, outline, onSuccessContainer, category/onCategory, camera/onControl |
| màu nền | #D9D4C8 | outlineVariant |
| màu nền | #EAE6DC | surfaceContainerHighest |
| màu nền | #FBF8F1 | surfaceContainerLow |
| màu nền | #FF5C8A | tertiary, category/women |
| màu nền | #FFC83D | secondaryContainer, shadowSticker, category/men |
| màu nền | #FFD6E2 | tertiaryContainer |
| màu nền | #FFFDF8 | background |

## 3. Cấu trúc và số liệu

Toạ độ và cỡ tính theo dp, gốc là góc trên trái của khung. Thứ tự từ trên xuống như trong Figma.

- Frame "PoseDetailScreen_ad_light", vị trí 0,0, cỡ 411 x 891, nền #FFFDF8 (background)
  - Instance "TopAppBar", vị trí 0,24, cỡ 411 x 64, xếp ngang, cách nhau 4 (xs), lề trên/phải/dưới/trái 0/4 (xs)/0/4 (xs), căn MIN/CENTER
    - Instance "leading", vị trí 4,32, cỡ 48 x 48, bo góc 999, xếp ngang, cách nhau 0, lề trên/phải/dưới/trái 0/0/0/0, căn CENTER/CENTER
      - Text "icon", vị trí 16,44, cỡ 24 x 24, chữ "arrow_back", kiểu ngoài hệ thống (24/24, đậm 400), màu #111111 (secondary, onSecondaryContainer, onTertiary, onBackground, onSurface, outline, onSuccessContainer, category/onCategory, camera/onControl)
    - Text "title", vị trí 56,42, cỡ 299 x 28, chữ "Tựa lan can", kiểu titleLarge (22/28, đậm 800), màu #111111 (secondary, onSecondaryContainer, onTertiary, onBackground, onSurface, outline, onSuccessContainer, category/onCategory, camera/onControl)
    - Instance "trailing", vị trí 359,32, cỡ 48 x 48, bo góc 999, xếp ngang, cách nhau 0, lề trên/phải/dưới/trái 0/0/0/0, căn CENTER/CENTER
      - Text "icon", vị trí 371,44, cỡ 24 x 24, chữ "bookmark", kiểu ngoài hệ thống (24/24, đậm 400), màu #111111 (secondary, onSecondaryContainer, onTertiary, onBackground, onSurface, outline, onSuccessContainer, category/onCategory, camera/onControl)
  - Frame "content", vị trí 0,88, cỡ 411 x 707, xếp dọc, cách nhau 16 (md), lề trên/phải/dưới/trái 0/16 (md)/0/16 (md), căn MIN/MIN
    - Frame "image", vị trí 16,88, cỡ 379 x 380, nền #FF5C8A (tertiary, category/women), viền #111111 (secondary, onSecondaryContainer, onTertiary, onBackground, onSurface, outline, onSuccessContainer, category/onCategory, camera/onControl), dày 2.0, bo góc 16
      - Frame "pose-outline", vị trí 130,128, cỡ 150 x 300
        - Vector "Vector", vị trí 186,138, cỡ 40 x 40, viền #111111 (secondary, onSecondaryContainer, onTertiary, onBackground, onSurface, outline, onSuccessContainer, category/onCategory, camera/onControl), dày 3.0
        - Vector "Vector", vị trí 145,183, cỡ 108 x 232, viền #111111 (secondary, onSecondaryContainer, onTertiary, onBackground, onSurface, outline, onSuccessContainer, category/onCategory, camera/onControl), dày 3.0
    - Text "meta", vị trí 16,484, cỡ 379 x 20, chữ "Một người · Nữ · Toàn thân · 4 bước", kiểu bodyMedium (14/20, đậm 400), màu #5A5650 (onSurfaceVariant)
    - Text "Các bước", vị trí 16,520, cỡ 379 x 24, chữ "Các bước", kiểu titleMedium (16/24, đậm 600), màu #111111 (secondary, onSecondaryContainer, onTertiary, onBackground, onSurface, outline, onSuccessContainer, category/onCategory, camera/onControl)
    - Frame "steps", vị trí 16,560, cỡ 379 x 136, xếp dọc, cách nhau 8 (sm), lề trên/phải/dưới/trái 0/0/0/0, căn MIN/MIN
      - Frame "step 1", vị trí 16,560, cỡ 379 x 28, xếp ngang, cách nhau 8 (sm), lề trên/phải/dưới/trái 0/0/0/0, căn MIN/MIN
        - Frame "number", vị trí 16,560, cỡ 28 x 28, nền #FFC83D (secondaryContainer, shadowSticker, category/men), viền #111111 (secondary, onSecondaryContainer, onTertiary, onBackground, onSurface, outline, onSuccessContainer, category/onCategory, camera/onControl), dày 2.0, bo góc 999, xếp ngang, cách nhau 0, lề trên/phải/dưới/trái 0/0/0/0, căn CENTER/CENTER
          - Text "1", vị trí 28,564, cỡ 5 x 20, chữ "1", kiểu titleSmall, labelLarge (14/20, đậm 500), màu #111111 (secondary, onSecondaryContainer, onTertiary, onBackground, onSurface, outline, onSuccessContainer, category/onCategory, camera/onControl)
        - Text "Đứng chéo 45° so với máy, chân", vị trí 52,560, cỡ 343 x 20, chữ "Đứng chéo 45° so với máy, chân trước hơi khuỵu.", kiểu bodyMedium (14/20, đậm 400), màu #111111 (secondary, onSecondaryContainer, onTertiary, onBackground, onSurface, outline, onSuccessContainer, category/onCategory, camera/onControl)
      - Frame "step 2", vị trí 16,596, cỡ 379 x 28, xếp ngang, cách nhau 8 (sm), lề trên/phải/dưới/trái 0/0/0/0, căn MIN/MIN
        - Frame "number", vị trí 16,596, cỡ 28 x 28, nền #FFC83D (secondaryContainer, shadowSticker, category/men), viền #111111 (secondary, onSecondaryContainer, onTertiary, onBackground, onSurface, outline, onSuccessContainer, category/onCategory, camera/onControl), dày 2.0, bo góc 999, xếp ngang, cách nhau 0, lề trên/phải/dưới/trái 0/0/0/0, căn CENTER/CENTER
          - Text "2", vị trí 26,600, cỡ 9 x 20, chữ "2", kiểu titleSmall, labelLarge (14/20, đậm 500), màu #111111 (secondary, onSecondaryContainer, onTertiary, onBackground, onSurface, outline, onSuccessContainer, category/onCategory, camera/onControl)
        - Text "Tay trái chống hông, khuỷu tay", vị trí 52,596, cỡ 343 x 20, chữ "Tay trái chống hông, khuỷu tay mở ra sau.", kiểu bodyMedium (14/20, đậm 400), màu #111111 (secondary, onSecondaryContainer, onTertiary, onBackground, onSurface, outline, onSuccessContainer, category/onCategory, camera/onControl)
      - Frame "step 3", vị trí 16,632, cỡ 379 x 28, xếp ngang, cách nhau 8 (sm), lề trên/phải/dưới/trái 0/0/0/0, căn MIN/MIN
        - Frame "number", vị trí 16,632, cỡ 28 x 28, nền #FFC83D (secondaryContainer, shadowSticker, category/men), viền #111111 (secondary, onSecondaryContainer, onTertiary, onBackground, onSurface, outline, onSuccessContainer, category/onCategory, camera/onControl), dày 2.0, bo góc 999, xếp ngang, cách nhau 0, lề trên/phải/dưới/trái 0/0/0/0, căn CENTER/CENTER
          - Text "3", vị trí 26,636, cỡ 9 x 20, chữ "3", kiểu titleSmall, labelLarge (14/20, đậm 500), màu #111111 (secondary, onSecondaryContainer, onTertiary, onBackground, onSurface, outline, onSuccessContainer, category/onCategory, camera/onControl)
        - Text "Tay phải thả lỏng dọc thân.", vị trí 52,632, cỡ 343 x 20, chữ "Tay phải thả lỏng dọc thân.", kiểu bodyMedium (14/20, đậm 400), màu #111111 (secondary, onSecondaryContainer, onTertiary, onBackground, onSurface, outline, onSuccessContainer, category/onCategory, camera/onControl)
      - Frame "step 4", vị trí 16,668, cỡ 379 x 28, xếp ngang, cách nhau 8 (sm), lề trên/phải/dưới/trái 0/0/0/0, căn MIN/MIN
        - Frame "number", vị trí 16,668, cỡ 28 x 28, nền #FFC83D (secondaryContainer, shadowSticker, category/men), viền #111111 (secondary, onSecondaryContainer, onTertiary, onBackground, onSurface, outline, onSuccessContainer, category/onCategory, camera/onControl), dày 2.0, bo góc 999, xếp ngang, cách nhau 0, lề trên/phải/dưới/trái 0/0/0/0, căn CENTER/CENTER
          - Text "4", vị trí 26,672, cỡ 9 x 20, chữ "4", kiểu titleSmall, labelLarge (14/20, đậm 500), màu #111111 (secondary, onSecondaryContainer, onTertiary, onBackground, onSurface, outline, onSuccessContainer, category/onCategory, camera/onControl)
        - Text "Xoay vai về phía máy, cằm hơi ", vị trí 52,668, cỡ 343 x 20, chữ "Xoay vai về phía máy, cằm hơi hạ.", kiểu bodyMedium (14/20, đậm 400), màu #111111 (secondary, onSecondaryContainer, onTertiary, onBackground, onSurface, outline, onSuccessContainer, category/onCategory, camera/onControl)
    - Frame "Common mistake", vị trí 16,712, cỡ 379 x 98, nền #FFD6E2 (tertiaryContainer), viền #111111 (secondary, onSecondaryContainer, onTertiary, onBackground, onSurface, outline, onSuccessContainer, category/onCategory, camera/onControl), dày 2.0, bo góc 16, xếp ngang, cách nhau 8 (sm), lề trên/phải/dưới/trái 16 (md)/16 (md)/16 (md)/16 (md), căn MIN/MIN
      - Text "icon", vị trí 34,730, cỡ 24 x 24, chữ "warning", kiểu ngoài hệ thống (24/24, đậm 400), màu #4A0F22 (onTertiaryContainer)
      - Frame "text", vị trí 66,730, cỡ 311 x 62, xếp dọc, cách nhau 2 (ngoài hệ thống), lề trên/phải/dưới/trái 0/0/0/0, căn MIN/MIN
        - Text "Lỗi hay gặp", vị trí 66,730, cỡ 311 x 20, chữ "Lỗi hay gặp", kiểu titleSmall, labelLarge (14/20, đậm 500), màu #4A0F22 (onTertiaryContainer)
        - Text "Rút vai lên sát tai làm cổ ngắ", vị trí 66,752, cỡ 311 x 40, chữ "Rút vai lên sát tai làm cổ ngắn lại. Thả vai xuống trước khi chụp.", kiểu bodyMedium (14/20, đậm 400), màu #4A0F22 (onTertiaryContainer)
  - Frame "Bottom bar", vị trí 0,699, cỡ 411 x 96, nền #FFFDF8 (background)
    - Rectangle "divider", vị trí 0,699, cỡ 411 x 2, nền #D9D4C8 (outlineVariant)
    - Instance "Button", vị trí 16,715, cỡ 379 x 60, nền #111111 (secondary, onSecondaryContainer, onTertiary, onBackground, onSurface, outline, onSuccessContainer, category/onCategory, camera/onControl), viền #111111 (secondary, onSecondaryContainer, onTertiary, onBackground, onSurface, outline, onSuccessContainer, category/onCategory, camera/onControl), dày 2.0, bo góc 14, xếp ngang, cách nhau 8 (sm), lề trên/phải/dưới/trái 16 (md)/24 (lg)/16 (md)/24 (lg), căn CENTER/CENTER
      - Text "icon", vị trí 117,735, cỡ 20 x 20, chữ "photo_camera", kiểu ngoài hệ thống (20/20, đậm 400), màu #FFFFFF (onPrimary, onSecondary, onError, surface, inverseOnSurface, surfaceBright, surfaceContainerLowest, onSuccess, camera/onCamera, camera/control, camera/overlayStroke)
      - Text "label", vị trí 145,733, cỡ 149 x 24, chữ "Chụp với tư thế này", kiểu titleMedium (16/24, đậm 600), màu #FFFFFF (onPrimary, onSecondary, onError, surface, inverseOnSurface, surfaceBright, surfaceContainerLowest, onSuccess, camera/onCamera, camera/control, camera/overlayStroke)
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
