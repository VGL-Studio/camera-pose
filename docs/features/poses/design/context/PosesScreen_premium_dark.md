# SỐ LIỆU THIẾT KẾ: PosesScreen_premium_dark

Sinh bởi `scripts/figma-export.py`. Không sửa tay; thiết kế đổi thì chạy lại script (ADR-0020).

## 1. Nguồn

- Link: https://www.figma.com/design/ZTXse6375eT3KXDUJPBTpe/?node-id=78-2604
- Frame: `78:2604`, giao diện tối
- Ngày lấy: 2026-10-31

## 2. Giá trị dùng

Đối chiếu với `docs/design-system/tokens.json` theo giá trị. "ngoài hệ thống" nghĩa là không có token
nào trùng; plan quyết định làm tròn về token gần nhất hay thêm token.

| Loại | Giá trị | Token |
| --- | --- | --- |
| instance ghi đè component | PoseCard | màu nền |
| khoảng cách | 16 | md |
| khoảng cách | 2 | ngoài hệ thống |
| khoảng cách | 24 | lg |
| khoảng cách | 4 | xs |
| khoảng cách | 8 | sm |
| kiểu chữ | 12/16, đậm 400 | bodySmall |
| kiểu chữ | 14/14, đậm 400 | ngoài hệ thống |
| kiểu chữ | 14/20, đậm 500 | titleSmall, labelLarge |
| kiểu chữ | 16/24, đậm 600 | titleMedium |
| kiểu chữ | 20/20, đậm 400 | ngoài hệ thống |
| kiểu chữ | 22/28, đậm 800 | titleLarge |
| kiểu chữ | 24/24, đậm 400 | ngoài hệ thống |
| màu chữ | #111111 | onSecondary, onSecondaryContainer, inverseOnSurface, onSuccessContainer, category/onCategory, camera/onControl |
| màu chữ | #B5B0A8 | onSurfaceVariant, camera/onSurfaceVariant |
| màu chữ | #FFD6E2 | onTertiaryContainer |
| màu chữ | #FFFFFF | secondary, onBackground, onSurface, outline, camera/onCamera, camera/control, camera/overlayStroke |
| màu nền | #121212 | background, surfaceDim |
| màu nền | #1E1E1E | surface, surfaceContainer, camera/surface |
| màu nền | #3DDC97 | success, successContainer, category/couple |
| màu nền | #4D8DFF | category/friends |
| màu nền | #6B1D35 | tertiaryContainer |
| màu nền | #B8E986 | category/cafe |
| màu nền | #FF5C8A | category/women |
| màu nền | #FF8A3D | category/travel |
| màu nền | #FFC83D | secondaryContainer, shadowSticker, category/men |

## 3. Cấu trúc và số liệu

Toạ độ và cỡ tính theo dp, gốc là góc trên trái của khung. Thứ tự từ trên xuống như trong Figma.

- Frame "PosesScreen_premium_dark", vị trí 0,0, cỡ 411 x 891, nền #121212 (background, surfaceDim), xếp dọc, cách nhau 8 (sm), lề trên/phải/dưới/trái 24 (lg)/0/0/0, căn MIN/MIN
  - Instance "TopAppBar", vị trí 0,24, cỡ 411 x 64, xếp ngang, cách nhau 4 (xs), lề trên/phải/dưới/trái 0/4 (xs)/0/4 (xs), căn MIN/CENTER
    - Instance "leading", vị trí 4,32, cỡ 48 x 48, bo góc 999, xếp ngang, cách nhau 0, lề trên/phải/dưới/trái 0/0/0/0, căn CENTER/CENTER
      - Text "icon", vị trí 16,44, cỡ 24 x 24, chữ "arrow_back", kiểu ngoài hệ thống (24/24, đậm 400), màu #FFFFFF (secondary, onBackground, onSurface, outline, camera/onCamera, camera/control, camera/overlayStroke)
    - Text "title", vị trí 56,42, cỡ 299 x 28, chữ "Thư viện tư thế", kiểu titleLarge (22/28, đậm 800), màu #FFFFFF (secondary, onBackground, onSurface, outline, camera/onCamera, camera/control, camera/overlayStroke)
    - Instance "trailing", vị trí 359,32, cỡ 48 x 48, bo góc 999, xếp ngang, cách nhau 0, lề trên/phải/dưới/trái 0/0/0/0, căn CENTER/CENTER
      - Text "icon", vị trí 371,44, cỡ 24 x 24, chữ "settings", kiểu ngoài hệ thống (24/24, đậm 400), màu #FFFFFF (secondary, onBackground, onSurface, outline, camera/onCamera, camera/control, camera/overlayStroke)
  - Frame "Subtitle", vị trí 0,96, cỡ 166 x 16, xếp ngang, cách nhau 0, lề trên/phải/dưới/trái 0/16 (md)/0/16 (md), căn MIN/MIN
    - Text "214 tư thế · 60 tư thế dùng đư", vị trí 16,96, cỡ 134 x 16, chữ "214 tư thế trong 8 nhóm", kiểu bodySmall (12/16, đậm 400), màu #B5B0A8 (onSurfaceVariant, camera/onSurfaceVariant)
  - Frame "Group chips", vị trí 0,120, cỡ 709 x 52, xếp ngang, cách nhau 8 (sm), lề trên/phải/dưới/trái 8 (sm)/16 (md)/8 (sm)/16 (md), căn MIN/MIN
    - Instance "Chip", vị trí 16,128, cỡ 76 x 36, nền #FFC83D (secondaryContainer, shadowSticker, category/men), viền #FFFFFF (secondary, onBackground, onSurface, outline, camera/onCamera, camera/control, camera/overlayStroke), dày 2.0, bo góc 18, xếp ngang, cách nhau 0, lề trên/phải/dưới/trái 0/16 (md)/0/16 (md), căn MIN/CENTER
      - Text "label", vị trí 34,136, cỡ 40 x 20, chữ "Tất cả", kiểu titleSmall, labelLarge (14/20, đậm 500), màu #111111 (onSecondary, onSecondaryContainer, inverseOnSurface, onSuccessContainer, category/onCategory, camera/onControl)
      - Instance "SelectedBadge", vị trí 78,119, cỡ 22 x 22, nền #FFC83D (secondaryContainer, shadowSticker, category/men), viền #FFFFFF (secondary, onBackground, onSurface, outline, camera/onCamera, camera/control, camera/overlayStroke), dày 2.0, bo góc 999, xếp ngang, cách nhau 0, lề trên/phải/dưới/trái 0/0/0/0, căn CENTER/CENTER
        - Text "icon", vị trí 82,123, cỡ 14 x 14, chữ "check", kiểu ngoài hệ thống (14/14, đậm 400), màu #111111 (onSecondary, onSecondaryContainer, inverseOnSurface, onSuccessContainer, category/onCategory, camera/onControl)
    - Instance "Chip", vị trí 100,128, cỡ 79 x 36, nền #1E1E1E (surface, surfaceContainer, camera/surface), viền #FFFFFF (secondary, onBackground, onSurface, outline, camera/onCamera, camera/control, camera/overlayStroke), dày 2.0, bo góc 18, xếp ngang, cách nhau 0, lề trên/phải/dưới/trái 0/16 (md)/0/16 (md), căn MIN/CENTER
      - Text "label", vị trí 118,136, cỡ 43 x 20, chữ "Đã lưu", kiểu titleSmall, labelLarge (14/20, đậm 500), màu #FFFFFF (secondary, onBackground, onSurface, outline, camera/onCamera, camera/control, camera/overlayStroke)
    - Instance "Chip", vị trí 187,128, cỡ 56 x 36, nền #1E1E1E (surface, surfaceContainer, camera/surface), viền #FFFFFF (secondary, onBackground, onSurface, outline, camera/onCamera, camera/control, camera/overlayStroke), dày 2.0, bo góc 18, xếp ngang, cách nhau 0, lề trên/phải/dưới/trái 0/16 (md)/0/16 (md), căn MIN/CENTER
      - Text "label", vị trí 205,136, cỡ 20 x 20, chữ "Nữ", kiểu titleSmall, labelLarge (14/20, đậm 500), màu #FFFFFF (secondary, onBackground, onSurface, outline, camera/onCamera, camera/control, camera/overlayStroke)
    - Instance "Chip", vị trí 251,128, cỡ 68 x 36, nền #1E1E1E (surface, surfaceContainer, camera/surface), viền #FFFFFF (secondary, onBackground, onSurface, outline, camera/onCamera, camera/control, camera/overlayStroke), dày 2.0, bo góc 18, xếp ngang, cách nhau 0, lề trên/phải/dưới/trái 0/16 (md)/0/16 (md), căn MIN/CENTER
      - Text "label", vị trí 269,136, cỡ 32 x 20, chữ "Nam", kiểu titleSmall, labelLarge (14/20, đậm 500), màu #FFFFFF (secondary, onBackground, onSurface, outline, camera/onCamera, camera/control, camera/overlayStroke)
    - Instance "Chip", vị trí 327,128, cỡ 87 x 36, nền #1E1E1E (surface, surfaceContainer, camera/surface), viền #FFFFFF (secondary, onBackground, onSurface, outline, camera/onCamera, camera/control, camera/overlayStroke), dày 2.0, bo góc 18, xếp ngang, cách nhau 0, lề trên/phải/dưới/trái 0/16 (md)/0/16 (md), căn MIN/CENTER
      - Text "label", vị trí 345,136, cỡ 51 x 20, chữ "Cặp đôi", kiểu titleSmall, labelLarge (14/20, đậm 500), màu #FFFFFF (secondary, onBackground, onSurface, outline, camera/onCamera, camera/control, camera/overlayStroke)
    - Instance "Chip", vị trí 422,128, cỡ 82 x 36, nền #1E1E1E (surface, surfaceContainer, camera/surface), viền #FFFFFF (secondary, onBackground, onSurface, outline, camera/onCamera, camera/control, camera/overlayStroke), dày 2.0, bo góc 18, xếp ngang, cách nhau 0, lề trên/phải/dưới/trái 0/16 (md)/0/16 (md), căn MIN/CENTER
      - Text "label", vị trí 440,136, cỡ 46 x 20, chữ "Bạn bè", kiểu titleSmall, labelLarge (14/20, đậm 500), màu #FFFFFF (secondary, onBackground, onSurface, outline, camera/onCamera, camera/control, camera/overlayStroke)
    - Instance "Chip", vị trí 512,128, cỡ 91 x 36, nền #1E1E1E (surface, surfaceContainer, camera/surface), viền #FFFFFF (secondary, onBackground, onSurface, outline, camera/onCamera, camera/control, camera/overlayStroke), dày 2.0, bo góc 18, xếp ngang, cách nhau 0, lề trên/phải/dưới/trái 0/16 (md)/0/16 (md), căn MIN/CENTER
      - Text "label", vị trí 530,136, cỡ 55 x 20, chữ "Gia đình", kiểu titleSmall, labelLarge (14/20, đậm 500), màu #FFFFFF (secondary, onBackground, onSurface, outline, camera/onCamera, camera/control, camera/overlayStroke)
    - Instance "Chip", vị trí 611,128, cỡ 82 x 36, nền #1E1E1E (surface, surfaceContainer, camera/surface), viền #FFFFFF (secondary, onBackground, onSurface, outline, camera/onCamera, camera/control, camera/overlayStroke), dày 2.0, bo góc 18, xếp ngang, cách nhau 0, lề trên/phải/dưới/trái 0/16 (md)/0/16 (md), căn MIN/CENTER
      - Text "label", vị trí 629,136, cỡ 46 x 20, chữ "Du lịch", kiểu titleSmall, labelLarge (14/20, đậm 500), màu #FFFFFF (secondary, onBackground, onSurface, outline, camera/onCamera, camera/control, camera/overlayStroke)
  - Frame "Grid", vị trí 0,180, cỡ 411 x 888, xếp dọc, cách nhau 16 (md), lề trên/phải/dưới/trái 4 (xs)/16 (md)/0/16 (md), căn MIN/MIN
    - Frame "Row 1", vị trí 16,184, cỡ 379 x 284, xếp ngang, cách nhau 16 (md), lề trên/phải/dưới/trái 0/0/0/0, căn MIN/MIN
      - Instance "PoseCard", vị trí 16,184, cỡ 182 x 284, nền #1E1E1E (surface, surfaceContainer, camera/surface), viền #FFFFFF (secondary, onBackground, onSurface, outline, camera/onCamera, camera/control, camera/overlayStroke), dày 2.0, CHÚ Ý: ghi đè màu nền của component gốc, bo góc 16, xếp dọc, cách nhau 8 (sm), lề trên/phải/dưới/trái 8 (sm)/8 (sm)/8 (sm)/8 (sm), căn MIN/MIN
        - Frame "image", vị trí 26,194, cỡ 162 x 210, nền #FF5C8A (category/women), viền #FFFFFF (secondary, onBackground, onSurface, outline, camera/onCamera, camera/control, camera/overlayStroke), dày 2.0, bo góc 10
          - Frame "pose-outline", vị trí 68,219, cỡ 80 x 160
            - Vector "Vector", vị trí 98,224, cỡ 21 x 21, viền #111111 (onSecondary, onSecondaryContainer, inverseOnSurface, onSuccessContainer, category/onCategory, camera/onControl), dày 2.0
            - Vector "Vector", vị trí 76,248, cỡ 57 x 124, viền #111111 (onSecondary, onSecondaryContainer, inverseOnSurface, onSuccessContainer, category/onCategory, camera/onControl), dày 2.0
          - Frame "save", vị trí 142,196, cỡ 48 x 48
            - Frame "save button", vị trí 148,202, cỡ 36 x 36, nền #1E1E1E (surface, surfaceContainer, camera/surface), viền #FFFFFF (secondary, onBackground, onSurface, outline, camera/onCamera, camera/control, camera/overlayStroke), dày 2.0, bo góc 999, xếp ngang, cách nhau 0, lề trên/phải/dưới/trái 0/0/0/0, căn CENTER/CENTER
              - Text "icon", vị trí 156,210, cỡ 20 x 20, chữ "bookmark", kiểu ngoài hệ thống (20/20, đậm 400), màu #FFFFFF (secondary, onBackground, onSurface, outline, camera/onCamera, camera/control, camera/overlayStroke)
          - Instance "PremiumBadge", vị trí 32,204, cỡ 32 x 32, nền #6B1D35 (tertiaryContainer), viền #FFFFFF (secondary, onBackground, onSurface, outline, camera/onCamera, camera/control, camera/overlayStroke), dày 2.0, bo góc 999, xếp ngang, cách nhau 0, lề trên/phải/dưới/trái 0/0/0/0, căn CENTER/CENTER
            - Text "icon", vị trí 38,210, cỡ 20 x 20, chữ "workspace_premium", kiểu ngoài hệ thống (20/20, đậm 400), màu #FFD6E2 (onTertiaryContainer)
        - Frame "text", vị trí 26,412, cỡ 162 x 46, xếp dọc, cách nhau 2 (ngoài hệ thống), lề trên/phải/dưới/trái 0/4 (xs)/4 (xs)/4 (xs), căn MIN/MIN
          - Text "name", vị trí 30,412, cỡ 154 x 24, chữ "Tựa lan can", kiểu titleMedium (16/24, đậm 600), màu #FFFFFF (secondary, onBackground, onSurface, outline, camera/onCamera, camera/control, camera/overlayStroke)
          - Text "meta", vị trí 30,438, cỡ 154 x 16, chữ "Một người · 4 bước", kiểu bodySmall (12/16, đậm 400), màu #B5B0A8 (onSurfaceVariant, camera/onSurfaceVariant)
      - Instance "PoseCard", vị trí 214,184, cỡ 182 x 284, nền #1E1E1E (surface, surfaceContainer, camera/surface), viền #FFFFFF (secondary, onBackground, onSurface, outline, camera/onCamera, camera/control, camera/overlayStroke), dày 2.0, CHÚ Ý: ghi đè màu nền của component gốc, bo góc 16, xếp dọc, cách nhau 8 (sm), lề trên/phải/dưới/trái 8 (sm)/8 (sm)/8 (sm)/8 (sm), căn MIN/MIN
        - Frame "image", vị trí 224,194, cỡ 162 x 210, nền #FFC83D (secondaryContainer, shadowSticker, category/men), viền #FFFFFF (secondary, onBackground, onSurface, outline, camera/onCamera, camera/control, camera/overlayStroke), dày 2.0, bo góc 10
          - Frame "pose-outline", vị trí 266,219, cỡ 80 x 160
            - Vector "Vector", vị trí 295,224, cỡ 21 x 21, viền #111111 (onSecondary, onSecondaryContainer, inverseOnSurface, onSuccessContainer, category/onCategory, camera/onControl), dày 2.0
            - Vector "Vector", vị trí 274,248, cỡ 57 x 124, viền #111111 (onSecondary, onSecondaryContainer, inverseOnSurface, onSuccessContainer, category/onCategory, camera/onControl), dày 2.0
          - Frame "save", vị trí 340,196, cỡ 48 x 48
            - Frame "save button", vị trí 346,202, cỡ 36 x 36, nền #FFC83D (secondaryContainer, shadowSticker, category/men), viền #FFFFFF (secondary, onBackground, onSurface, outline, camera/onCamera, camera/control, camera/overlayStroke), dày 2.0, bo góc 999, xếp ngang, cách nhau 0, lề trên/phải/dưới/trái 0/0/0/0, căn CENTER/CENTER
              - Text "icon", vị trí 354,210, cỡ 20 x 20, chữ "bookmark_added", kiểu ngoài hệ thống (20/20, đậm 400), màu #111111 (onSecondary, onSecondaryContainer, inverseOnSurface, onSuccessContainer, category/onCategory, camera/onControl)
          - Instance "PremiumBadge", vị trí 230,204, cỡ 32 x 32, nền #6B1D35 (tertiaryContainer), viền #FFFFFF (secondary, onBackground, onSurface, outline, camera/onCamera, camera/control, camera/overlayStroke), dày 2.0, bo góc 999, xếp ngang, cách nhau 0, lề trên/phải/dưới/trái 0/0/0/0, căn CENTER/CENTER
            - Text "icon", vị trí 236,210, cỡ 20 x 20, chữ "workspace_premium", kiểu ngoài hệ thống (20/20, đậm 400), màu #FFD6E2 (onTertiaryContainer)
        - Frame "text", vị trí 224,412, cỡ 162 x 46, xếp dọc, cách nhau 2 (ngoài hệ thống), lề trên/phải/dưới/trái 0/4 (xs)/4 (xs)/4 (xs), căn MIN/MIN
          - Text "name", vị trí 228,412, cỡ 154 x 24, chữ "Nhìn qua vai", kiểu titleMedium (16/24, đậm 600), màu #FFFFFF (secondary, onBackground, onSurface, outline, camera/onCamera, camera/control, camera/overlayStroke)
          - Text "meta", vị trí 228,438, cỡ 154 x 16, chữ "Một người · 3 bước", kiểu bodySmall (12/16, đậm 400), màu #B5B0A8 (onSurfaceVariant, camera/onSurfaceVariant)
    - Frame "Row 2", vị trí 16,484, cỡ 379 x 284, xếp ngang, cách nhau 16 (md), lề trên/phải/dưới/trái 0/0/0/0, căn MIN/MIN
      - Instance "PoseCard", vị trí 16,484, cỡ 182 x 284, nền #1E1E1E (surface, surfaceContainer, camera/surface), viền #FFFFFF (secondary, onBackground, onSurface, outline, camera/onCamera, camera/control, camera/overlayStroke), dày 2.0, CHÚ Ý: ghi đè màu nền của component gốc, bo góc 16, xếp dọc, cách nhau 8 (sm), lề trên/phải/dưới/trái 8 (sm)/8 (sm)/8 (sm)/8 (sm), căn MIN/MIN
        - Frame "image", vị trí 26,494, cỡ 162 x 210, nền #3DDC97 (success, successContainer, category/couple), viền #FFFFFF (secondary, onBackground, onSurface, outline, camera/onCamera, camera/control, camera/overlayStroke), dày 2.0, bo góc 10
          - Frame "pose-outline", vị trí 68,519, cỡ 80 x 160
            - Vector "Vector", vị trí 98,524, cỡ 21 x 21, viền #111111 (onSecondary, onSecondaryContainer, inverseOnSurface, onSuccessContainer, category/onCategory, camera/onControl), dày 2.0
            - Vector "Vector", vị trí 76,548, cỡ 57 x 124, viền #111111 (onSecondary, onSecondaryContainer, inverseOnSurface, onSuccessContainer, category/onCategory, camera/onControl), dày 2.0
          - Frame "save", vị trí 142,496, cỡ 48 x 48
            - Frame "save button", vị trí 148,502, cỡ 36 x 36, nền #1E1E1E (surface, surfaceContainer, camera/surface), viền #FFFFFF (secondary, onBackground, onSurface, outline, camera/onCamera, camera/control, camera/overlayStroke), dày 2.0, bo góc 999, xếp ngang, cách nhau 0, lề trên/phải/dưới/trái 0/0/0/0, căn CENTER/CENTER
              - Text "icon", vị trí 156,510, cỡ 20 x 20, chữ "bookmark", kiểu ngoài hệ thống (20/20, đậm 400), màu #FFFFFF (secondary, onBackground, onSurface, outline, camera/onCamera, camera/control, camera/overlayStroke)
        - Frame "text", vị trí 26,712, cỡ 162 x 46, xếp dọc, cách nhau 2 (ngoài hệ thống), lề trên/phải/dưới/trái 0/4 (xs)/4 (xs)/4 (xs), căn MIN/MIN
          - Text "name", vị trí 30,712, cỡ 154 x 24, chữ "Bước đi tự nhiên", kiểu titleMedium (16/24, đậm 600), màu #FFFFFF (secondary, onBackground, onSurface, outline, camera/onCamera, camera/control, camera/overlayStroke)
          - Text "meta", vị trí 30,738, cỡ 154 x 16, chữ "Cặp đôi · 5 bước", kiểu bodySmall (12/16, đậm 400), màu #B5B0A8 (onSurfaceVariant, camera/onSurfaceVariant)
      - Instance "PoseCard", vị trí 214,484, cỡ 182 x 284, nền #1E1E1E (surface, surfaceContainer, camera/surface), viền #FFFFFF (secondary, onBackground, onSurface, outline, camera/onCamera, camera/control, camera/overlayStroke), dày 2.0, CHÚ Ý: ghi đè màu nền của component gốc, bo góc 16, xếp dọc, cách nhau 8 (sm), lề trên/phải/dưới/trái 8 (sm)/8 (sm)/8 (sm)/8 (sm), căn MIN/MIN
        - Frame "image", vị trí 224,494, cỡ 162 x 210, nền #4D8DFF (category/friends), viền #FFFFFF (secondary, onBackground, onSurface, outline, camera/onCamera, camera/control, camera/overlayStroke), dày 2.0, bo góc 10
          - Frame "pose-outline", vị trí 266,519, cỡ 80 x 160
            - Vector "Vector", vị trí 295,524, cỡ 21 x 21, viền #111111 (onSecondary, onSecondaryContainer, inverseOnSurface, onSuccessContainer, category/onCategory, camera/onControl), dày 2.0
            - Vector "Vector", vị trí 274,548, cỡ 57 x 124, viền #111111 (onSecondary, onSecondaryContainer, inverseOnSurface, onSuccessContainer, category/onCategory, camera/onControl), dày 2.0
          - Frame "save", vị trí 340,496, cỡ 48 x 48
            - Frame "save button", vị trí 346,502, cỡ 36 x 36, nền #1E1E1E (surface, surfaceContainer, camera/surface), viền #FFFFFF (secondary, onBackground, onSurface, outline, camera/onCamera, camera/control, camera/overlayStroke), dày 2.0, bo góc 999, xếp ngang, cách nhau 0, lề trên/phải/dưới/trái 0/0/0/0, căn CENTER/CENTER
              - Text "icon", vị trí 354,510, cỡ 20 x 20, chữ "bookmark", kiểu ngoài hệ thống (20/20, đậm 400), màu #FFFFFF (secondary, onBackground, onSurface, outline, camera/onCamera, camera/control, camera/overlayStroke)
          - Instance "PremiumBadge", vị trí 230,504, cỡ 32 x 32, nền #6B1D35 (tertiaryContainer), viền #FFFFFF (secondary, onBackground, onSurface, outline, camera/onCamera, camera/control, camera/overlayStroke), dày 2.0, bo góc 999, xếp ngang, cách nhau 0, lề trên/phải/dưới/trái 0/0/0/0, căn CENTER/CENTER
            - Text "icon", vị trí 236,510, cỡ 20 x 20, chữ "workspace_premium", kiểu ngoài hệ thống (20/20, đậm 400), màu #FFD6E2 (onTertiaryContainer)
        - Frame "text", vị trí 224,712, cỡ 162 x 46, xếp dọc, cách nhau 2 (ngoài hệ thống), lề trên/phải/dưới/trái 0/4 (xs)/4 (xs)/4 (xs), căn MIN/MIN
          - Text "name", vị trí 228,712, cỡ 154 x 24, chữ "Ngồi bậc thềm", kiểu titleMedium (16/24, đậm 600), màu #FFFFFF (secondary, onBackground, onSurface, outline, camera/onCamera, camera/control, camera/overlayStroke)
          - Text "meta", vị trí 228,738, cỡ 154 x 16, chữ "Bạn bè · 4 bước", kiểu bodySmall (12/16, đậm 400), màu #B5B0A8 (onSurfaceVariant, camera/onSurfaceVariant)
    - Frame "Row 3", vị trí 16,784, cỡ 379 x 284, xếp ngang, cách nhau 16 (md), lề trên/phải/dưới/trái 0/0/0/0, căn MIN/MIN
      - Instance "PoseCard", vị trí 16,784, cỡ 182 x 284, nền #1E1E1E (surface, surfaceContainer, camera/surface), viền #FFFFFF (secondary, onBackground, onSurface, outline, camera/onCamera, camera/control, camera/overlayStroke), dày 2.0, CHÚ Ý: ghi đè màu nền của component gốc, bo góc 16, xếp dọc, cách nhau 8 (sm), lề trên/phải/dưới/trái 8 (sm)/8 (sm)/8 (sm)/8 (sm), căn MIN/MIN
        - Frame "image", vị trí 26,794, cỡ 162 x 210, nền #FF8A3D (category/travel), viền #FFFFFF (secondary, onBackground, onSurface, outline, camera/onCamera, camera/control, camera/overlayStroke), dày 2.0, bo góc 10
          - Frame "pose-outline", vị trí 68,819, cỡ 80 x 160
            - Vector "Vector", vị trí 98,824, cỡ 21 x 21, viền #111111 (onSecondary, onSecondaryContainer, inverseOnSurface, onSuccessContainer, category/onCategory, camera/onControl), dày 2.0
            - Vector "Vector", vị trí 76,848, cỡ 57 x 124, viền #111111 (onSecondary, onSecondaryContainer, inverseOnSurface, onSuccessContainer, category/onCategory, camera/onControl), dày 2.0
          - Frame "save", vị trí 142,796, cỡ 48 x 48
            - Frame "save button", vị trí 148,802, cỡ 36 x 36, nền #1E1E1E (surface, surfaceContainer, camera/surface), viền #FFFFFF (secondary, onBackground, onSurface, outline, camera/onCamera, camera/control, camera/overlayStroke), dày 2.0, bo góc 999, xếp ngang, cách nhau 0, lề trên/phải/dưới/trái 0/0/0/0, căn CENTER/CENTER
              - Text "icon", vị trí 156,810, cỡ 20 x 20, chữ "bookmark", kiểu ngoài hệ thống (20/20, đậm 400), màu #FFFFFF (secondary, onBackground, onSurface, outline, camera/onCamera, camera/control, camera/overlayStroke)
        - Frame "text", vị trí 26,1012, cỡ 162 x 46, xếp dọc, cách nhau 2 (ngoài hệ thống), lề trên/phải/dưới/trái 0/4 (xs)/4 (xs)/4 (xs), căn MIN/MIN
          - Text "name", vị trí 30,1012, cỡ 154 x 24, chữ "Tay chống hông", kiểu titleMedium (16/24, đậm 600), màu #FFFFFF (secondary, onBackground, onSurface, outline, camera/onCamera, camera/control, camera/overlayStroke)
          - Text "meta", vị trí 30,1038, cỡ 154 x 16, chữ "Du lịch · 3 bước", kiểu bodySmall (12/16, đậm 400), màu #B5B0A8 (onSurfaceVariant, camera/onSurfaceVariant)
      - Instance "PoseCard", vị trí 214,784, cỡ 182 x 284, nền #1E1E1E (surface, surfaceContainer, camera/surface), viền #FFFFFF (secondary, onBackground, onSurface, outline, camera/onCamera, camera/control, camera/overlayStroke), dày 2.0, CHÚ Ý: ghi đè màu nền của component gốc, bo góc 16, xếp dọc, cách nhau 8 (sm), lề trên/phải/dưới/trái 8 (sm)/8 (sm)/8 (sm)/8 (sm), căn MIN/MIN
        - Frame "image", vị trí 224,794, cỡ 162 x 210, nền #B8E986 (category/cafe), viền #FFFFFF (secondary, onBackground, onSurface, outline, camera/onCamera, camera/control, camera/overlayStroke), dày 2.0, bo góc 10
          - Frame "pose-outline", vị trí 266,819, cỡ 80 x 160
            - Vector "Vector", vị trí 295,824, cỡ 21 x 21, viền #111111 (onSecondary, onSecondaryContainer, inverseOnSurface, onSuccessContainer, category/onCategory, camera/onControl), dày 2.0
            - Vector "Vector", vị trí 274,848, cỡ 57 x 124, viền #111111 (onSecondary, onSecondaryContainer, inverseOnSurface, onSuccessContainer, category/onCategory, camera/onControl), dày 2.0
          - Frame "save", vị trí 340,796, cỡ 48 x 48
            - Frame "save button", vị trí 346,802, cỡ 36 x 36, nền #1E1E1E (surface, surfaceContainer, camera/surface), viền #FFFFFF (secondary, onBackground, onSurface, outline, camera/onCamera, camera/control, camera/overlayStroke), dày 2.0, bo góc 999, xếp ngang, cách nhau 0, lề trên/phải/dưới/trái 0/0/0/0, căn CENTER/CENTER
              - Text "icon", vị trí 354,810, cỡ 20 x 20, chữ "bookmark", kiểu ngoài hệ thống (20/20, đậm 400), màu #FFFFFF (secondary, onBackground, onSurface, outline, camera/onCamera, camera/control, camera/overlayStroke)
        - Frame "text", vị trí 224,1012, cỡ 162 x 46, xếp dọc, cách nhau 2 (ngoài hệ thống), lề trên/phải/dưới/trái 0/4 (xs)/4 (xs)/4 (xs), căn MIN/MIN
          - Text "name", vị trí 228,1012, cỡ 154 x 24, chữ "Cầm ly cà phê", kiểu titleMedium (16/24, đậm 600), màu #FFFFFF (secondary, onBackground, onSurface, outline, camera/onCamera, camera/control, camera/overlayStroke)
          - Text "meta", vị trí 228,1038, cỡ 154 x 16, chữ "Cà phê · 3 bước", kiểu bodySmall (12/16, đậm 400), màu #B5B0A8 (onSurfaceVariant, camera/onSurfaceVariant)

## 4. Code tham khảo

Không có. Script không sinh code; AI viết Compose theo mục 2 và 3.
