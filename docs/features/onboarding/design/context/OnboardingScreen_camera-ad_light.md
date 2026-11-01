# SỐ LIỆU THIẾT KẾ: OnboardingScreen_camera-ad_light

Sinh bởi `scripts/figma-export.py`. Không sửa tay; thiết kế đổi thì chạy lại script (ADR-0020).

## 1. Nguồn

- Link: https://www.figma.com/design/ZTXse6375eT3KXDUJPBTpe/?node-id=69-2597
- Frame: `69:2597`, giao diện sáng
- Ngày lấy: 2026-10-31

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
| khoảng cách | 56 | ngoài hệ thống |
| khoảng cách | 6 | ngoài hệ thống |
| khoảng cách | 8 | sm |
| kiểu chữ | 11/16, đậm 500 | labelSmall |
| kiểu chữ | 12/16, đậm 400 | bodySmall |
| kiểu chữ | 13/20, đậm 500 | ngoài hệ thống |
| kiểu chữ | 14/20, đậm 400 | bodyMedium |
| kiểu chữ | 14/20, đậm 500 | titleSmall, labelLarge |
| kiểu chữ | 16/24, đậm 600 | titleMedium |
| kiểu chữ | 20/20, đậm 400 | ngoài hệ thống |
| kiểu chữ | 22/22, đậm 400 | ngoài hệ thống |
| kiểu chữ | 28/36, đậm 800 | headlineMedium |
| kiểu chữ | 48/48, đậm 400 | ngoài hệ thống |
| màu chữ | #111111 | secondary, onSecondaryContainer, onTertiary, onBackground, onSurface, outline, onSuccessContainer, category/onCategory, camera/onControl |
| màu chữ | #4A0F22 | onTertiaryContainer |
| màu chữ | #5A5650 | onSurfaceVariant |
| màu chữ | #FFFFFF | onPrimary, onSecondary, onError, surface, inverseOnSurface, surfaceBright, surfaceContainerLowest, onSuccess, camera/onCamera, camera/control, camera/overlayStroke |
| màu nền | #111111 | secondary, onSecondaryContainer, onTertiary, onBackground, onSurface, outline, onSuccessContainer, category/onCategory, camera/onControl |
| màu nền | #D9D4C8 | outlineVariant |
| màu nền | #EAE6DC | surfaceContainerHighest |
| màu nền | #F6F3EB | surfaceContainer |
| màu nền | #FBF8F1 | surfaceContainerLow |
| màu nền | #FF5C8A | tertiary, category/women |
| màu nền | #FFC83D | secondaryContainer, shadowSticker, category/men |
| màu nền | #FFD6E2 | tertiaryContainer |
| màu nền | #FFFDF8 | background |

## 3. Cấu trúc và số liệu

Toạ độ và cỡ tính theo dp, gốc là góc trên trái của khung. Thứ tự từ trên xuống như trong Figma.

- Frame "OnboardingScreen_camera-ad_light", vị trí 0,0, cỡ 411 x 891, nền #FFFDF8 (background), xếp dọc, cách nhau 0, lề trên/phải/dưới/trái 56 (ngoài hệ thống)/24 (lg)/24 (lg)/24 (lg), căn MIN/MIN
  - Frame "page indicator", vị trí 24,56, cỡ 72 x 8, xếp ngang, cách nhau 8 (sm), lề trên/phải/dưới/trái 0/0/0/0, căn MIN/CENTER
    - Instance "PageDot", vị trí 24,56, cỡ 8 x 8, nền #D9D4C8 (outlineVariant), bo góc 999
    - Instance "PageDot", vị trí 40,56, cỡ 8 x 8, nền #D9D4C8 (outlineVariant), bo góc 999
    - Instance "PageDot", vị trí 56,56, cỡ 8 x 8, nền #D9D4C8 (outlineVariant), bo góc 999
    - Instance "PageDot", vị trí 72,56, cỡ 24 x 8, nền #111111 (secondary, onSecondaryContainer, onTertiary, onBackground, onSurface, outline, onSuccessContainer, category/onCategory, camera/onControl), bo góc 999
  - Frame "page content", vị trí 24,64, cỡ 363 x 413, xếp dọc, cách nhau 24 (lg), lề trên/phải/dưới/trái 24 (lg)/0/24 (lg)/0, căn MIN/MIN
    - Instance "badge", vị trí 24,88, cỡ 96 x 96, nền #FFC83D (secondaryContainer, shadowSticker, category/men), viền #111111 (secondary, onSecondaryContainer, onTertiary, onBackground, onSurface, outline, onSuccessContainer, category/onCategory, camera/onControl), dày 2.0, bo góc 999, xếp ngang, cách nhau 0, lề trên/phải/dưới/trái 0/0/0/0, căn CENTER/CENTER
      - Text "icon", vị trí 48,112, cỡ 48 x 48, chữ "photo_camera", kiểu ngoài hệ thống (48/48, đậm 400), màu #111111 (secondary, onSecondaryContainer, onTertiary, onBackground, onSurface, outline, onSuccessContainer, category/onCategory, camera/onControl)
    - Text "title", vị trí 24,208, cỡ 363 x 36, chữ "Sẵn sàng chụp", kiểu headlineMedium (28/36, đậm 800), màu #111111 (secondary, onSecondaryContainer, onTertiary, onBackground, onSurface, outline, onSuccessContainer, category/onCategory, camera/onControl)
    - Frame "features", vị trí 24,268, cỡ 363 x 40, xếp dọc, cách nhau 16 (md), lề trên/phải/dưới/trái 0/0/0/0, căn MIN/MIN
      - Instance "feature", vị trí 24,268, cỡ 363 x 40, xếp ngang, cách nhau 16 (md), lề trên/phải/dưới/trái 0/0/0/0, căn MIN/CENTER
        - Frame "icon circle", vị trí 24,268, cỡ 40 x 40, nền #FFC83D (secondaryContainer, shadowSticker, category/men), viền #111111 (secondary, onSecondaryContainer, onTertiary, onBackground, onSurface, outline, onSuccessContainer, category/onCategory, camera/onControl), dày 2.0, bo góc 999, xếp ngang, cách nhau 0, lề trên/phải/dưới/trái 0/0/0/0, căn CENTER/CENTER
          - Text "icon", vị trí 33,277, cỡ 22 x 22, chữ "tune", kiểu ngoài hệ thống (22/22, đậm 400), màu #111111 (secondary, onSecondaryContainer, onTertiary, onBackground, onSurface, outline, onSuccessContainer, category/onCategory, camera/onControl)
        - Text "label", vị trí 80,278, cỡ 307 x 20, chữ "Chỉnh độ mờ, cỡ và vị trí nét viền", kiểu bodyMedium (14/20, đậm 400), màu #111111 (secondary, onSecondaryContainer, onTertiary, onBackground, onSurface, outline, onSuccessContainer, category/onCategory, camera/onControl)
  - Instance "ad slot (Native/Step)", vị trí 24,477, cỡ 363 x 250, nền #FBF8F1 (surfaceContainerLow), viền #D9D4C8 (outlineVariant), dày 1.0, bo góc 12, xếp dọc, cách nhau 8 (sm), lề trên/phải/dưới/trái 12 (ngoài hệ thống)/12 (ngoài hệ thống)/12 (ngoài hệ thống)/12 (ngoài hệ thống), căn MIN/MIN
    - Frame "media", vị trí 36,489, cỡ 339 x 136, nền #F6F3EB (surfaceContainer), bo góc 8
    - Frame "head", vị trí 36,633, cỡ 339 x 38, xếp ngang, cách nhau 8 (sm), lề trên/phải/dưới/trái 0/0/0/0, căn MIN/CENTER
      - Frame "icon", vị trí 36,636, cỡ 32 x 32, nền #FFC83D (secondaryContainer, shadowSticker, category/men), bo góc 8
      - Frame "text", vị trí 76,633, cỡ 299 x 38, xếp dọc, cách nhau 2 (ngoài hệ thống), lề trên/phải/dưới/trái 0/0/0/0, căn MIN/MIN
        - Text "headline", vị trí 76,633, cỡ 299 x 20, chữ "Headline of the ad", kiểu ngoài hệ thống (13/20, đậm 500), màu #111111 (secondary, onSecondaryContainer, onTertiary, onBackground, onSurface, outline, onSuccessContainer, category/onCategory, camera/onControl)
        - Frame "badge line", vị trí 76,655, cỡ 299 x 16, xếp ngang, cách nhau 6 (ngoài hệ thống), lề trên/phải/dưới/trái 0/0/0/0, căn MIN/CENTER
          - Frame "badge", vị trí 76,655, cỡ 23 x 16, nền #FF5C8A (tertiary, category/women), bo góc 4, xếp ngang, cách nhau 0, lề trên/phải/dưới/trái 0/4 (xs)/0/4 (xs), căn MIN/CENTER
            - Text "Ad", vị trí 80,655, cỡ 15 x 16, chữ "Ad", kiểu labelSmall (11/16, đậm 500), màu #111111 (secondary, onSecondaryContainer, onTertiary, onBackground, onSurface, outline, onSuccessContainer, category/onCategory, camera/onControl)
          - Text "advertiser", vị trí 105,655, cỡ 270 x 16, chữ "Advertiser or short body", kiểu bodySmall (12/16, đậm 400), màu #5A5650 (onSurfaceVariant)
    - Frame "cta", vị trí 36,679, cỡ 339 x 36, nền #FFD6E2 (tertiaryContainer), bo góc 8, xếp ngang, cách nhau 0, lề trên/phải/dưới/trái 0/16 (md)/0/16 (md), căn CENTER/CENTER
      - Text "Install", vị trí 185,687, cỡ 41 x 20, chữ "Install", kiểu titleSmall, labelLarge (14/20, đậm 500), màu #4A0F22 (onTertiaryContainer)
    - Frame "AdChoices", vị trí 367,481, cỡ 16 x 16, nền #EAE6DC (surfaceContainerHighest), bo góc 4
  - Frame "note", vị trí 24,727, cỡ 363 x 56, xếp dọc, cách nhau 0, lề trên/phải/dưới/trái 24 (lg)/0/0/0, căn MIN/MIN
    - Text "note", vị trí 24,751, cỡ 363 x 32, chữ "App cần quyền camera để hiện khung xem trước. Ảnh chỉ lưu trên máy của bạn.", kiểu bodySmall (12/16, đậm 400), màu #5A5650 (onSurfaceVariant)
  - Frame "primary button", vị trí 24,783, cỡ 363 x 84, xếp dọc, cách nhau 0, lề trên/phải/dưới/trái 24 (lg)/0/0/0, căn MIN/MIN
    - Instance "Button Bật camera", vị trí 24,807, cỡ 363 x 60, nền #111111 (secondary, onSecondaryContainer, onTertiary, onBackground, onSurface, outline, onSuccessContainer, category/onCategory, camera/onControl), viền #111111 (secondary, onSecondaryContainer, onTertiary, onBackground, onSurface, outline, onSuccessContainer, category/onCategory, camera/onControl), dày 2.0, bo góc 14, xếp ngang, cách nhau 8 (sm), lề trên/phải/dưới/trái 16 (md)/24 (lg)/16 (md)/24 (lg), căn CENTER/CENTER
      - Text "icon", vị trí 147,827, cỡ 20 x 20, chữ "photo_camera", kiểu ngoài hệ thống (20/20, đậm 400), màu #FFFFFF (onPrimary, onSecondary, onError, surface, inverseOnSurface, surfaceBright, surfaceContainerLowest, onSuccess, camera/onCamera, camera/control, camera/overlayStroke)
      - Text "label", vị trí 175,825, cỡ 89 x 24, chữ "Bật camera", kiểu titleMedium (16/24, đậm 600), màu #FFFFFF (onPrimary, onSecondary, onError, surface, inverseOnSurface, surfaceBright, surfaceContainerLowest, onSuccess, camera/onCamera, camera/control, camera/overlayStroke)

## 4. Code tham khảo

Không có. Script không sinh code; AI viết Compose theo mục 2 và 3.
