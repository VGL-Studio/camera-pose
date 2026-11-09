# SỐ LIỆU THIẾT KẾ: OnboardingScreen_camera_dark

Sinh bởi `scripts/figma-export.py`. Không sửa tay; thiết kế đổi thì chạy lại script (ADR-0020).

## 1. Nguồn

- Link: https://www.figma.com/design/ZTXse6375eT3KXDUJPBTpe/?node-id=95-2954
- Frame: `95:2954`, giao diện tối
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
| kiểu chữ | 12/16, đậm 400 | bodySmall |
| kiểu chữ | 16/24, đậm 400 | bodyLarge |
| kiểu chữ | 16/24, đậm 600 | titleMedium |
| kiểu chữ | 20/20, đậm 400 | ngoài hệ thống |
| kiểu chữ | 28/36, đậm 800 | headlineMedium |
| màu chữ | #111111 | onSecondary, onSecondaryContainer, inverseOnSurface, onSuccessContainer, category/onCategory, camera/onControl |
| màu chữ | #B5B0A8 | onSurfaceVariant, camera/onSurfaceVariant |
| màu chữ | #FFFFFF | secondary, onBackground, onSurface, outline, camera/onCamera, camera/control, camera/overlayStroke |
| màu nền | #121212 | background, surfaceDim |
| màu nền | #4A4744 | outlineVariant |
| màu nền | #FFFFFF | secondary, onBackground, onSurface, outline, camera/onCamera, camera/control, camera/overlayStroke |

## 3. Cấu trúc và số liệu

Toạ độ và cỡ tính theo dp, gốc là góc trên trái của khung. Thứ tự từ trên xuống như trong Figma.

- Frame "OnboardingScreen_camera_dark", vị trí 0,0, cỡ 411 x 891, nền #121212 (background, surfaceDim), xếp dọc, cách nhau 0, lề trên/phải/dưới/trái 56 (ngoài hệ thống)/24 (lg)/24 (lg)/24 (lg), căn MIN/MIN
  - Frame "page indicator", vị trí 24,56, cỡ 72 x 8, xếp ngang, cách nhau 8 (sm), lề trên/phải/dưới/trái 0/0/0/0, căn MIN/CENTER
    - Instance "PageDot", vị trí 24,56, cỡ 8 x 8, nền #4A4744 (outlineVariant), bo góc 999
    - Instance "PageDot", vị trí 40,56, cỡ 8 x 8, nền #4A4744 (outlineVariant), bo góc 999
    - Instance "PageDot", vị trí 56,56, cỡ 8 x 8, nền #4A4744 (outlineVariant), bo góc 999
    - Instance "PageDot", vị trí 72,56, cỡ 24 x 8, nền #FFFFFF (secondary, onBackground, onSurface, outline, camera/onCamera, camera/control, camera/overlayStroke), bo góc 999
  - Frame "page content", vị trí 24,64, cỡ 363 x 663, xếp dọc, cách nhau 24 (lg), lề trên/phải/dưới/trái 24 (lg)/0/24 (lg)/0, căn MIN/MIN
    - Frame "illustration", vị trí 24,88, cỡ 363 x 360, xếp ngang, cách nhau 0, lề trên/phải/dưới/trái 0/0/0/0, căn CENTER/CENTER
      - Instance "OnboardingArt/camera", vị trí 29,88, cỡ 353 x 360
    - Text "title", vị trí 24,472, cỡ 363 x 36, chữ "Sẵn sàng chụp rồi!", kiểu headlineMedium (28/36, đậm 800), màu #FFFFFF (secondary, onBackground, onSurface, outline, camera/onCamera, camera/control, camera/overlayStroke)
    - Text "body", vị trí 24,532, cỡ 363 x 48, chữ "Chọn một dáng, đứng vào nét viền và chụp tấm ảnh đầu tiên của bạn.", kiểu bodyLarge (16/24, đậm 400), màu #B5B0A8 (onSurfaceVariant, camera/onSurfaceVariant)
  - Frame "note", vị trí 24,727, cỡ 363 x 56, xếp dọc, cách nhau 0, lề trên/phải/dưới/trái 24 (lg)/0/0/0, căn MIN/MIN
    - Text "note", vị trí 24,751, cỡ 363 x 32, chữ "App cần quyền camera để hiện khung xem trước. Ảnh chỉ lưu trên máy của bạn.", kiểu bodySmall (12/16, đậm 400), màu #B5B0A8 (onSurfaceVariant, camera/onSurfaceVariant)
  - Frame "primary button", vị trí 24,783, cỡ 363 x 84, xếp dọc, cách nhau 0, lề trên/phải/dưới/trái 24 (lg)/0/0/0, căn MIN/MIN
    - Instance "Button Bật camera", vị trí 24,807, cỡ 363 x 60, nền #FFFFFF (secondary, onBackground, onSurface, outline, camera/onCamera, camera/control, camera/overlayStroke), viền #FFFFFF (secondary, onBackground, onSurface, outline, camera/onCamera, camera/control, camera/overlayStroke), dày 2.0, bo góc 14, xếp ngang, cách nhau 8 (sm), lề trên/phải/dưới/trái 16 (md)/24 (lg)/16 (md)/24 (lg), căn CENTER/CENTER
      - Text "icon", vị trí 147,827, cỡ 20 x 20, chữ "photo_camera", kiểu ngoài hệ thống (20/20, đậm 400), màu #111111 (onSecondary, onSecondaryContainer, inverseOnSurface, onSuccessContainer, category/onCategory, camera/onControl)
      - Text "label", vị trí 175,825, cỡ 89 x 24, chữ "Bật camera", kiểu titleMedium (16/24, đậm 600), màu #111111 (onSecondary, onSecondaryContainer, inverseOnSurface, onSuccessContainer, category/onCategory, camera/onControl)

## 4. Code tham khảo

Không có. Script không sinh code; AI viết Compose theo mục 2 và 3.
