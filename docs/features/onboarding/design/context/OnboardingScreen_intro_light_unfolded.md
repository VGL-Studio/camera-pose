# SỐ LIỆU THIẾT KẾ: OnboardingScreen_intro_light_unfolded

Sinh bởi `scripts/figma-export.py`. Không sửa tay; thiết kế đổi thì chạy lại script (ADR-0020).

## 1. Nguồn

- Link: https://www.figma.com/design/ZTXse6375eT3KXDUJPBTpe/?node-id=69-2427
- Frame: `69:2427`, giao diện sáng
- Ngày lấy: 2026-10-31

## 2. Giá trị dùng

Đối chiếu với `docs/design-system/tokens.json` theo giá trị. "ngoài hệ thống" nghĩa là không có token
nào trùng; plan quyết định làm tròn về token gần nhất hay thêm token.

| Loại | Giá trị | Token |
| --- | --- | --- |
| khoảng cách | 16 | md |
| khoảng cách | 24 | lg |
| khoảng cách | 56 | ngoài hệ thống |
| khoảng cách | 8 | sm |
| kiểu chữ | 16/24, đậm 400 | bodyLarge |
| kiểu chữ | 16/24, đậm 600 | titleMedium |
| kiểu chữ | 28/28, đậm 400 | ngoài hệ thống |
| kiểu chữ | 28/36, đậm 800 | headlineMedium |
| màu chữ | #111111 | secondary, onSecondaryContainer, onTertiary, onBackground, onSurface, outline, onSuccessContainer, category/onCategory, camera/onControl |
| màu chữ | #5A5650 | onSurfaceVariant |
| màu chữ | #FFFFFF | onPrimary, onSecondary, onError, surface, inverseOnSurface, surfaceBright, surfaceContainerLowest, onSuccess, camera/onCamera, camera/control, camera/overlayStroke |
| màu nền | #111111 | secondary, onSecondaryContainer, onTertiary, onBackground, onSurface, outline, onSuccessContainer, category/onCategory, camera/onControl |
| màu nền | #3DDC97 | successContainer, category/couple |
| màu nền | #4D8DFF | category/friends |
| màu nền | #D9D4C8 | outlineVariant |
| màu nền | #FF5C8A | tertiary, category/women |
| màu nền | #FFC83D | secondaryContainer, shadowSticker, category/men |
| màu nền | #FFFDF8 | background |

## 3. Cấu trúc và số liệu

Toạ độ và cỡ tính theo dp, gốc là góc trên trái của khung. Thứ tự từ trên xuống như trong Figma.

- Frame "OnboardingScreen_intro_light_unfolded", vị trí 0,0, cỡ 673 x 841, nền #FFFDF8 (background), xếp dọc, cách nhau 0, lề trên/phải/dưới/trái 56 (ngoài hệ thống)/24 (lg)/24 (lg)/24 (lg), căn MIN/MIN
  - Frame "page indicator", vị trí 24,56, cỡ 72 x 8, xếp ngang, cách nhau 8 (sm), lề trên/phải/dưới/trái 0/0/0/0, căn MIN/CENTER
    - Instance "PageDot", vị trí 24,56, cỡ 24 x 8, nền #111111 (secondary, onSecondaryContainer, onTertiary, onBackground, onSurface, outline, onSuccessContainer, category/onCategory, camera/onControl), bo góc 999
    - Instance "PageDot", vị trí 56,56, cỡ 8 x 8, nền #D9D4C8 (outlineVariant), bo góc 999
    - Instance "PageDot", vị trí 72,56, cỡ 8 x 8, nền #D9D4C8 (outlineVariant), bo góc 999
    - Instance "PageDot", vị trí 88,56, cỡ 8 x 8, nền #D9D4C8 (outlineVariant), bo góc 999
  - Frame "page content", vị trí 24,64, cỡ 625 x 669, xếp dọc, cách nhau 24 (lg), lề trên/phải/dưới/trái 24 (lg)/0/24 (lg)/0, căn MIN/MIN
    - Frame "illustration", vị trí 24,88, cỡ 625 x 280, xếp dọc, cách nhau 0, lề trên/phải/dưới/trái 0/0/0/0, căn MIN/CENTER
      - Frame "illustration art", vị trí 24,88, cỡ 625 x 280
        - Frame "sticker women", vị trí 52,137, cỡ 176 x 219, nền #FF5C8A (tertiary, category/women), viền #111111 (secondary, onSecondaryContainer, onTertiary, onBackground, onSurface, outline, onSuccessContainer, category/onCategory, camera/onControl), dày 2.0, bo góc 16
          - Frame "pose-outline", vị trí 96,172, cỡ 89 x 148
            - Vector "Vector", vị trí 122,181, cỡ 21 x 21, viền #111111 (secondary, onSecondaryContainer, onTertiary, onBackground, onSurface, outline, onSuccessContainer, category/onCategory, camera/onControl), dày 2.00083327293396
            - Vector "Vector", vị trí 106,200, cỡ 65 x 114, viền #111111 (secondary, onSecondaryContainer, onTertiary, onBackground, onSurface, outline, onSuccessContainer, category/onCategory, camera/onControl), dày 2.00083327293396
        - Frame "sticker couple", vị trí 130,118, cỡ 150 x 200, nền #3DDC97 (successContainer, category/couple), viền #111111 (secondary, onSecondaryContainer, onTertiary, onBackground, onSurface, outline, onSuccessContainer, category/onCategory, camera/onControl), dày 2.0, bo góc 16
          - Frame "pose-outline", vị trí 170,148, cỡ 70 x 140
            - Vector "Vector", vị trí 196,153, cỡ 19 x 19, viền #111111 (secondary, onSecondaryContainer, onTertiary, onBackground, onSurface, outline, onSuccessContainer, category/onCategory, camera/onControl), dày 2.00083327293396
            - Vector "Vector", vị trí 177,174, cỡ 50 x 108, viền #111111 (secondary, onSecondaryContainer, onTertiary, onBackground, onSurface, outline, onSuccessContainer, category/onCategory, camera/onControl), dày 2.00083327293396
        - Frame "sticker friends", vị trí 182,158, cỡ 176 x 219, nền #4D8DFF (category/friends), viền #111111 (secondary, onSecondaryContainer, onTertiary, onBackground, onSurface, outline, onSuccessContainer, category/onCategory, camera/onControl), dày 2.0, bo góc 16
          - Frame "pose-outline", vị trí 226,193, cỡ 89 x 148
            - Vector "Vector", vị trí 268,201, cỡ 21 x 21, viền #111111 (secondary, onSecondaryContainer, onTertiary, onBackground, onSurface, outline, onSuccessContainer, category/onCategory, camera/onControl), dày 2.00083327293396
            - Vector "Vector", vị trí 234,220, cỡ 65 x 114, viền #111111 (secondary, onSecondaryContainer, onTertiary, onBackground, onSurface, outline, onSuccessContainer, category/onCategory, camera/onControl), dày 2.00083327293396
        - Frame "badge", vị trí 260,102, cỡ 56 x 56, nền #FFC83D (secondaryContainer, shadowSticker, category/men), viền #111111 (secondary, onSecondaryContainer, onTertiary, onBackground, onSurface, outline, onSuccessContainer, category/onCategory, camera/onControl), dày 2.0, bo góc 999, xếp ngang, cách nhau 0, lề trên/phải/dưới/trái 0/0/0/0, căn CENTER/CENTER
          - Text "icon", vị trí 274,116, cỡ 28 x 28, chữ "photo_camera", kiểu ngoài hệ thống (28/28, đậm 400), màu #111111 (secondary, onSecondaryContainer, onTertiary, onBackground, onSurface, outline, onSuccessContainer, category/onCategory, camera/onControl)
    - Text "title", vị trí 24,392, cỡ 625 x 36, chữ "Chụp dáng đẹp, không cần đoán", kiểu headlineMedium (28/36, đậm 800), màu #111111 (secondary, onSecondaryContainer, onTertiary, onBackground, onSurface, outline, onSuccessContainer, category/onCategory, camera/onControl)
    - Text "body", vị trí 24,452, cỡ 625 x 48, chữ "Chọn một tư thế, đứng vào nét viền rồi bấm chụp. Ảnh lưu vào thư viện máy, không kèm nét viền.", kiểu bodyLarge (16/24, đậm 400), màu #5A5650 (onSurfaceVariant)
  - Frame "primary button", vị trí 24,733, cỡ 625 x 84, xếp dọc, cách nhau 0, lề trên/phải/dưới/trái 24 (lg)/0/0/0, căn MIN/MIN
    - Instance "Button Tiếp", vị trí 24,757, cỡ 625 x 60, nền #111111 (secondary, onSecondaryContainer, onTertiary, onBackground, onSurface, outline, onSuccessContainer, category/onCategory, camera/onControl), viền #111111 (secondary, onSecondaryContainer, onTertiary, onBackground, onSurface, outline, onSuccessContainer, category/onCategory, camera/onControl), dày 2.0, bo góc 14, xếp ngang, cách nhau 8 (sm), lề trên/phải/dưới/trái 16 (md)/24 (lg)/16 (md)/24 (lg), căn CENTER/CENTER
      - Text "label", vị trí 320,775, cỡ 33 x 24, chữ "Tiếp", kiểu titleMedium (16/24, đậm 600), màu #FFFFFF (onPrimary, onSecondary, onError, surface, inverseOnSurface, surfaceBright, surfaceContainerLowest, onSuccess, camera/onCamera, camera/control, camera/overlayStroke)

## 4. Code tham khảo

Không có. Script không sinh code; AI viết Compose theo mục 2 và 3.
