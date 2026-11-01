# SỐ LIỆU THIẾT KẾ: PoseUnlockDialogScreen_default_light

Sinh bởi `scripts/figma-export.py`. Không sửa tay; thiết kế đổi thì chạy lại script (ADR-0020).

## 1. Nguồn

- Link: https://www.figma.com/design/ZTXse6375eT3KXDUJPBTpe/?node-id=78-2707
- Frame: `78:2707`, giao diện sáng
- Ngày lấy: 2026-10-31

## 2. Giá trị dùng

Đối chiếu với `docs/design-system/tokens.json` theo giá trị. "ngoài hệ thống" nghĩa là không có token
nào trùng; plan quyết định làm tròn về token gần nhất hay thêm token.

| Loại | Giá trị | Token |
| --- | --- | --- |
| khoảng cách | 16 | md |
| khoảng cách | 24 | lg |
| khoảng cách | 8 | sm |
| kiểu chữ | 14/20, đậm 400 | bodyMedium |
| kiểu chữ | 14/20, đậm 500 | titleSmall, labelLarge |
| kiểu chữ | 16/24, đậm 600 | titleMedium |
| kiểu chữ | 24/32, đậm 800 | headlineSmall |
| màu chữ | #111111 | secondary, onSecondaryContainer, onTertiary, onBackground, onSurface, outline, onSuccessContainer, category/onCategory, camera/onControl |
| màu chữ | #5A5650 | onSurfaceVariant |
| màu chữ | #FFFFFF | onPrimary, onSecondary, onError, surface, inverseOnSurface, surfaceBright, surfaceContainerLowest, onSuccess, camera/onCamera, camera/control, camera/overlayStroke |
| màu nền | #2F6BFF | primary, surfaceTint |
| màu nền | #F0ECE3 | surfaceContainerHigh |
| màu nền | #FFFDF8 | background |

## 3. Cấu trúc và số liệu

Toạ độ và cỡ tính theo dp, gốc là góc trên trái của khung. Thứ tự từ trên xuống như trong Figma.

- Frame "PoseUnlockDialogScreen_default_light", vị trí 0,0, cỡ 411 x 891, nền #FFFDF8 (background), xếp dọc, cách nhau 0, lề trên/phải/dưới/trái 0/0/0/0, căn CENTER/CENTER
  - Instance "PremiumUnlockDialog", vị trí 50,302, cỡ 312 x 288, nền #F0ECE3 (surfaceContainerHigh), bo góc 28, xếp dọc, cách nhau 16 (md), lề trên/phải/dưới/trái 24 (lg)/24 (lg)/24 (lg)/24 (lg), căn MIN/MIN
    - Text "title", vị trí 74,326, cỡ 264 x 32, chữ "Mở khóa tư thế", kiểu headlineSmall (24/32, đậm 800), màu #111111 (secondary, onSecondaryContainer, onTertiary, onBackground, onSurface, outline, onSuccessContainer, category/onCategory, camera/onControl)
    - Frame "content (PremiumUnlockContent)", vị trí 74,374, cỡ 264 x 72, xếp dọc, cách nhau 8 (sm), lề trên/phải/dưới/trái 0/0/0/0, căn MIN/MIN
      - Text "pose name", vị trí 74,374, cỡ 264 x 24, chữ "Tựa lan can", kiểu titleMedium (16/24, đậm 600), màu #111111 (secondary, onSecondaryContainer, onTertiary, onBackground, onSurface, outline, onSuccessContainer, category/onCategory, camera/onControl)
      - Text "message", vị trí 74,406, cỡ 264 x 40, chữ "Xem một quảng cáo ngắn để mở tư thế này, rồi dùng mãi về sau trên máy này.", kiểu bodyMedium (14/20, đậm 400), màu #5A5650 (onSurfaceVariant)
    - Frame "buttons (khung)", vị trí 74,462, cỡ 264 x 104, xếp dọc, cách nhau 8 (sm), lề trên/phải/dưới/trái 0/0/0/0, căn MIN/MIN
      - Frame "watch (Button 56, bo 28)", vị trí 74,462, cỡ 264 x 56, nền #2F6BFF (primary, surfaceTint), bo góc 28, xếp ngang, cách nhau 0, lề trên/phải/dưới/trái 0/0/0/0, căn CENTER/CENTER
        - Text "label", vị trí 154,480, cỡ 103 x 20, chữ "Xem quảng cáo", kiểu titleSmall, labelLarge (14/20, đậm 500), màu #FFFFFF (onPrimary, onSecondary, onError, surface, inverseOnSurface, surfaceBright, surfaceContainerLowest, onSuccess, camera/onCamera, camera/control, camera/overlayStroke)
      - Frame "no thanks (TextButton 40)", vị trí 74,526, cỡ 264 x 40, xếp ngang, cách nhau 0, lề trên/phải/dưới/trái 0/0/0/0, căn CENTER/CENTER
        - Text "label", vị trí 182,536, cỡ 46 x 20, chữ "Để sau", kiểu titleSmall, labelLarge (14/20, đậm 500), màu #5A5650 (onSurfaceVariant)

## 4. Code tham khảo

Không có. Script không sinh code; AI viết Compose theo mục 2 và 3.
