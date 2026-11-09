# SỐ LIỆU THIẾT KẾ: OnboardingScreen_intro-ad-wait_dark

Sinh bởi `scripts/figma-export.py`. Không sửa tay; thiết kế đổi thì chạy lại script (ADR-0020).

## 1. Nguồn

- Link: https://www.figma.com/design/ZTXse6375eT3KXDUJPBTpe/?node-id=95-3284
- Frame: `95:3284`, giao diện tối
- Ngày lấy: 2026-11-09

## 2. Giá trị dùng

Đối chiếu với `docs/design-system/tokens.json` theo giá trị. "ngoài hệ thống" nghĩa là không có token
nào trùng; plan quyết định làm tròn về token gần nhất hay thêm token.

| Loại | Giá trị | Token |
| --- | --- | --- |
| khoảng cách | 12 | ngoài hệ thống |
| khoảng cách | 16 | md |
| khoảng cách | 2 | ngoài hệ thống |
| khoảng cách | 24 | lg |
| khoảng cách | 56 | ngoài hệ thống |
| khoảng cách | 8 | sm |
| kiểu chữ | 16/24, đậm 400 | bodyLarge |
| kiểu chữ | 16/24, đậm 600 | titleMedium |
| kiểu chữ | 28/36, đậm 800 | headlineMedium |
| màu chữ | #B5B0A8 | onSurfaceVariant, camera/onSurfaceVariant |
| màu chữ | #FFFFFF | secondary, onBackground, onSurface, outline, camera/onCamera, camera/control, camera/overlayStroke |
| màu nền | #121212 | background, surfaceDim |
| màu nền | #181818 | surfaceContainerLow |
| màu nền | #2E2E2E | surfaceContainerHighest |
| màu nền | #4A4744 | outlineVariant |
| màu nền | #FFFFFF | secondary, onBackground, onSurface, outline, camera/onCamera, camera/control, camera/overlayStroke |
| độ mờ của lớp | 0.12 | - |
| độ mờ của lớp | 0.38 | - |

## 3. Cấu trúc và số liệu

Toạ độ và cỡ tính theo dp, gốc là góc trên trái của khung. Thứ tự từ trên xuống như trong Figma.

- Frame "OnboardingScreen_intro-ad-wait_dark", vị trí 0,0, cỡ 411 x 891, nền #121212 (background, surfaceDim), xếp dọc, cách nhau 0, lề trên/phải/dưới/trái 56 (ngoài hệ thống)/24 (lg)/24 (lg)/24 (lg), căn MIN/MIN
  - Frame "page indicator", vị trí 24,56, cỡ 72 x 8, xếp ngang, cách nhau 8 (sm), lề trên/phải/dưới/trái 0/0/0/0, căn MIN/CENTER
    - Instance "PageDot", vị trí 24,56, cỡ 24 x 8, nền #FFFFFF (secondary, onBackground, onSurface, outline, camera/onCamera, camera/control, camera/overlayStroke), bo góc 999
    - Instance "PageDot", vị trí 56,56, cỡ 8 x 8, nền #4A4744 (outlineVariant), bo góc 999
    - Instance "PageDot", vị trí 72,56, cỡ 8 x 8, nền #4A4744 (outlineVariant), bo góc 999
    - Instance "PageDot", vị trí 88,56, cỡ 8 x 8, nền #4A4744 (outlineVariant), bo góc 999
  - Frame "page content", vị trí 24,64, cỡ 363 x 469, xếp dọc, cách nhau 24 (lg), lề trên/phải/dưới/trái 24 (lg)/0/24 (lg)/0, căn MIN/MIN
    - Frame "illustration", vị trí 24,88, cỡ 363 x 289, xếp ngang, cách nhau 0, lề trên/phải/dưới/trái 0/0/0/0, căn CENTER/CENTER
      - Instance "OnboardingArt/intro", vị trí 64,88, cỡ 283 x 289
    - Text "title", vị trí 24,401, cỡ 363 x 36, chữ "Dáng đẹp, không cần đoán", kiểu headlineMedium (28/36, đậm 800), màu #FFFFFF (secondary, onBackground, onSurface, outline, camera/onCamera, camera/control, camera/overlayStroke)
    - Text "body", vị trí 24,461, cỡ 363 x 48, chữ "Chưa biết tạo dáng thế nào? App gợi ý dáng và chỉ bạn đứng ra sao.", kiểu bodyLarge (16/24, đậm 400), màu #B5B0A8 (onSurfaceVariant, camera/onSurfaceVariant)
  - Instance "ad slot (Native/Step)", vị trí 24,533, cỡ 363 x 250, nền #181818 (surfaceContainerLow), viền #4A4744 (outlineVariant), dày 1.0, bo góc 12, xếp dọc, cách nhau 8 (sm), lề trên/phải/dưới/trái 12 (ngoài hệ thống)/12 (ngoài hệ thống)/12 (ngoài hệ thống)/12 (ngoài hệ thống), căn MIN/MIN
    - Frame "media", vị trí 36,545, cỡ 339 x 142, nền #2E2E2E (surfaceContainerHighest), bo góc 8
    - Frame "head", vị trí 36,695, cỡ 339 x 32, xếp ngang, cách nhau 8 (sm), lề trên/phải/dưới/trái 0/0/0/0, căn MIN/MIN
      - Frame "icon", vị trí 36,695, cỡ 32 x 32, nền #2E2E2E (surfaceContainerHighest), bo góc 8
      - Frame "lines", vị trí 76,695, cỡ 299 x 30, xếp dọc, cách nhau 8 (sm), lề trên/phải/dưới/trái 2 (ngoài hệ thống)/0/0/0, căn MIN/MIN
        - Frame "line long", vị trí 76,697, cỡ 299 x 10, nền #2E2E2E (surfaceContainerHighest), bo góc 5
        - Frame "line short", vị trí 76,715, cỡ 180 x 10, nền #2E2E2E (surfaceContainerHighest), bo góc 5
    - Frame "cta", vị trí 36,735, cỡ 339 x 36, nền #2E2E2E (surfaceContainerHighest), bo góc 8
  - Frame "primary button", vị trí 24,783, cỡ 363 x 84, xếp dọc, cách nhau 0, lề trên/phải/dưới/trái 24 (lg)/0/0/0, căn MIN/MIN
    - Instance "Button", vị trí 24,807, cỡ 363 x 60, viền #4A4744 (outlineVariant), dày 2.0, bo góc 14, xếp ngang, cách nhau 8 (sm), lề trên/phải/dưới/trái 16 (md)/24 (lg)/16 (md)/24 (lg), căn CENTER/CENTER
      - Rectangle "container", vị trí 24,807, cỡ 363 x 60, nền #FFFFFF (secondary, onBackground, onSurface, outline, camera/onCamera, camera/control, camera/overlayStroke), bo góc 14, độ mờ của lớp 0.12
      - Text "label", vị trí 189,825, cỡ 33 x 24, chữ "Tiếp", kiểu titleMedium (16/24, đậm 600), màu #FFFFFF (secondary, onBackground, onSurface, outline, camera/onCamera, camera/control, camera/overlayStroke), độ mờ của lớp 0.38

## 4. Code tham khảo

Không có. Script không sinh code; AI viết Compose theo mục 2 và 3.
