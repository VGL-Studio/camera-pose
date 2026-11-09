# SỐ LIỆU THIẾT KẾ: OnboardingScreen_guide_light

Sinh bởi `scripts/figma-export.py`. Không sửa tay; thiết kế đổi thì chạy lại script (ADR-0020).

## 1. Nguồn

- Link: https://www.figma.com/design/ZTXse6375eT3KXDUJPBTpe/?node-id=95-2903
- Frame: `95:2903`, giao diện sáng
- Ngày lấy: 2026-11-09

## 2. Giá trị dùng

Đối chiếu với `docs/design-system/tokens.json` theo giá trị. "ngoài hệ thống" nghĩa là không có token
nào trùng; plan quyết định làm tròn về token gần nhất hay thêm token.

| Loại | Giá trị | Token |
| --- | --- | --- |
| khoảng cách | 16 | md |
| khoảng cách | 24 | lg |
| khoảng cách | 56 | ngoài hệ thống |
| khoảng cách | 8 | sm |
| kiểu chữ | 15/20, đậm 500 | ngoài hệ thống |
| kiểu chữ | 16/24, đậm 400 | bodyLarge |
| kiểu chữ | 16/24, đậm 600 | titleMedium |
| kiểu chữ | 28/36, đậm 800 | headlineMedium |
| màu chữ | #111111 | secondary, onSecondaryContainer, onTertiary, onBackground, onSurface, outline, onSuccessContainer, category/onCategory, camera/onControl |
| màu chữ | #5A5650 | onSurfaceVariant |
| màu chữ | #FFFFFF | onPrimary, onSecondary, onError, surface, inverseOnSurface, surfaceBright, surfaceContainerLowest, onSuccess, camera/onCamera, camera/control, camera/overlayStroke |
| màu nền | #111111 | secondary, onSecondaryContainer, onTertiary, onBackground, onSurface, outline, onSuccessContainer, category/onCategory, camera/onControl |
| màu nền | #D9D4C8 | outlineVariant |
| màu nền | #FFFDF8 | background |

## 3. Cấu trúc và số liệu

Toạ độ và cỡ tính theo dp, gốc là góc trên trái của khung. Thứ tự từ trên xuống như trong Figma.

- Frame "OnboardingScreen_guide_light", vị trí 0,0, cỡ 411 x 891, nền #FFFDF8 (background), xếp dọc, cách nhau 0, lề trên/phải/dưới/trái 56 (ngoài hệ thống)/24 (lg)/24 (lg)/24 (lg), căn MIN/MIN
  - Frame "page indicator", vị trí 24,56, cỡ 72 x 8, xếp ngang, cách nhau 8 (sm), lề trên/phải/dưới/trái 0/0/0/0, căn MIN/CENTER
    - Instance "PageDot", vị trí 24,56, cỡ 8 x 8, nền #D9D4C8 (outlineVariant), bo góc 999
    - Instance "PageDot", vị trí 40,56, cỡ 8 x 8, nền #D9D4C8 (outlineVariant), bo góc 999
    - Instance "PageDot", vị trí 56,56, cỡ 24 x 8, nền #111111 (secondary, onSecondaryContainer, onTertiary, onBackground, onSurface, outline, onSuccessContainer, category/onCategory, camera/onControl), bo góc 999
    - Instance "PageDot", vị trí 88,56, cỡ 8 x 8, nền #D9D4C8 (outlineVariant), bo góc 999
  - Frame "page content", vị trí 24,64, cỡ 363 x 719, xếp dọc, cách nhau 24 (lg), lề trên/phải/dưới/trái 24 (lg)/0/24 (lg)/0, căn MIN/MIN
    - Frame "illustration", vị trí 24,88, cỡ 363 x 360, xếp ngang, cách nhau 0, lề trên/phải/dưới/trái 0/0/0/0, căn CENTER/CENTER
      - Instance "OnboardingArt/guide", vị trí 47,88, cỡ 316 x 360
        - Text "bubble text", vị trí 225,130, cỡ 130 x 25, chữ "Đứng vào đây!", kiểu ngoài hệ thống (15/20, đậm 500), màu #111111 (secondary, onSecondaryContainer, onTertiary, onBackground, onSurface, outline, onSuccessContainer, category/onCategory, camera/onControl)
    - Text "title", vị trí 24,472, cỡ 363 x 36, chữ "Khớp nét viền, bấm chụp", kiểu headlineMedium (28/36, đậm 800), màu #111111 (secondary, onSecondaryContainer, onTertiary, onBackground, onSurface, outline, onSuccessContainer, category/onCategory, camera/onControl)
    - Text "body", vị trí 24,532, cỡ 363 x 48, chữ "Chỉnh độ mờ, cỡ và vị trí nét viền cho vừa khung hình. Ảnh lưu ra không kèm nét viền.", kiểu bodyLarge (16/24, đậm 400), màu #5A5650 (onSurfaceVariant)
  - Frame "primary button", vị trí 24,783, cỡ 363 x 84, xếp dọc, cách nhau 0, lề trên/phải/dưới/trái 24 (lg)/0/0/0, căn MIN/MIN
    - Instance "Button Tiếp", vị trí 24,807, cỡ 363 x 60, nền #111111 (secondary, onSecondaryContainer, onTertiary, onBackground, onSurface, outline, onSuccessContainer, category/onCategory, camera/onControl), viền #111111 (secondary, onSecondaryContainer, onTertiary, onBackground, onSurface, outline, onSuccessContainer, category/onCategory, camera/onControl), dày 2.0, bo góc 14, xếp ngang, cách nhau 8 (sm), lề trên/phải/dưới/trái 16 (md)/24 (lg)/16 (md)/24 (lg), căn CENTER/CENTER
      - Text "label", vị trí 189,825, cỡ 33 x 24, chữ "Tiếp", kiểu titleMedium (16/24, đậm 600), màu #FFFFFF (onPrimary, onSecondary, onError, surface, inverseOnSurface, surfaceBright, surfaceContainerLowest, onSuccess, camera/onCamera, camera/control, camera/overlayStroke)

## 4. Code tham khảo

Không có. Script không sinh code; AI viết Compose theo mục 2 và 3.
