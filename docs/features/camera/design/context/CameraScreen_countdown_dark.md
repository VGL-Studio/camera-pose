# SỐ LIỆU THIẾT KẾ: CameraScreen_countdown_dark

Sinh bởi `scripts/figma-export.py`. Không sửa tay; thiết kế đổi thì chạy lại script (ADR-0020).

## 1. Nguồn

- Link: https://www.figma.com/design/ZTXse6375eT3KXDUJPBTpe/?node-id=16-835
- Frame: `16:835`, giao diện tối
- Ngày lấy: 2026-10-26

## 2. Giá trị dùng

Đối chiếu với `docs/design-system/tokens.json` theo giá trị. "ngoài hệ thống" nghĩa là không có token
nào trùng; plan quyết định làm tròn về token gần nhất hay thêm token.

| Loại | Giá trị | Token |
| --- | --- | --- |
| instance ghi đè component | CameraIconButton | độ mờ |
| instance ghi đè component | GridOverlay | màu nền |
| instance ghi đè component | StepCard | độ mờ |
| instance ghi đè component | ZoomControl | màu nền |
| khoảng cách | 16 | md |
| khoảng cách | 4 | xs |
| khoảng cách | 8 | sm |
| kiểu chữ | 14/14, đậm 400 | ngoài hệ thống |
| kiểu chữ | 14/20, đậm 400 | bodyMedium |
| kiểu chữ | 14/20, đậm 500 | titleSmall, labelLarge |
| kiểu chữ | 16/24, đậm 600 | titleMedium |
| kiểu chữ | 24/24, đậm 400 | ngoài hệ thống |
| kiểu chữ | 28/34, đậm 400 | ngoài hệ thống |
| kiểu chữ | 57/64, đậm 800 | displayLarge |
| màu chữ | #111111 | onSecondary, onSecondaryContainer, inverseOnSurface, onSuccessContainer, category/onCategory, camera/onControl |
| màu chữ | #FFFFFF | secondary, onBackground, onSurface, outline, camera/onCamera, camera/control, camera/overlayStroke |
| màu nền | #000000 (độ mờ 0.35, lớp tô 0.35) | camera/scrim |
| màu nền | #000000 (độ mờ 0.45, lớp tô 0.45) | camera/scrim |
| màu nền | #0A0A0B | camera/background |
| màu nền | #1E1E1E | surface, surfaceContainer, camera/surface |
| màu nền | #2A2A2A | surfaceVariant |
| màu nền | #FFC83D | secondaryContainer, shadowSticker, category/men |
| màu nền | #FFFFFF | secondary, onBackground, onSurface, outline, camera/onCamera, camera/control, camera/overlayStroke |
| độ mờ của lớp | 0.38 | - |

## 3. Cấu trúc và số liệu

Toạ độ và cỡ tính theo dp, gốc là góc trên trái của khung. Thứ tự từ trên xuống như trong Figma.

- Frame "CameraScreen_countdown_dark", vị trí 0,0, cỡ 411 x 891, nền #0A0A0B (camera/background)
  - Frame "Top bar", vị trí 16,32, cỡ 379 x 48, xếp ngang, cách nhau 0, lề trên/phải/dưới/trái 0/0/0/0, căn SPACE_BETWEEN/CENTER
    - Instance "CameraIconButton", vị trí 16,32, cỡ 48 x 48, nền #FFFFFF (secondary, onBackground, onSurface, outline, camera/onCamera, camera/control, camera/overlayStroke), viền #111111 (onSecondary, onSecondaryContainer, inverseOnSurface, onSuccessContainer, category/onCategory, camera/onControl), dày 2.0, bo góc 999, xếp ngang, cách nhau 0, lề trên/phải/dưới/trái 0/0/0/0, căn CENTER/CENTER
      - Text "icon", vị trí 28,44, cỡ 24 x 24, chữ "grid_view", kiểu ngoài hệ thống (24/24, đậm 400), màu #111111 (onSecondary, onSecondaryContainer, inverseOnSurface, onSuccessContainer, category/onCategory, camera/onControl)
    - Frame "tools", vị trí 179,32, cỡ 216 x 48, xếp ngang, cách nhau 8 (sm), lề trên/phải/dưới/trái 0/0/0/0, căn MIN/MIN
      - Instance "CameraIconButton", vị trí 179,32, cỡ 48 x 48, nền #FFFFFF (secondary, onBackground, onSurface, outline, camera/onCamera, camera/control, camera/overlayStroke), viền #111111 (onSecondary, onSecondaryContainer, inverseOnSurface, onSuccessContainer, category/onCategory, camera/onControl), dày 2.0, CHÚ Ý: ghi đè độ mờ của component gốc, bo góc 999, xếp ngang, cách nhau 0, lề trên/phải/dưới/trái 0/0/0/0, căn CENTER/CENTER, độ mờ của lớp 0.38
        - Text "icon", vị trí 191,44, cỡ 24 x 24, chữ "flash_off", kiểu ngoài hệ thống (24/24, đậm 400), màu #111111 (onSecondary, onSecondaryContainer, inverseOnSurface, onSuccessContainer, category/onCategory, camera/onControl)
      - Instance "CameraIconButton", vị trí 235,32, cỡ 48 x 48, nền #FFC83D (secondaryContainer, shadowSticker, category/men), viền #111111 (onSecondary, onSecondaryContainer, inverseOnSurface, onSuccessContainer, category/onCategory, camera/onControl), dày 2.0, CHÚ Ý: ghi đè độ mờ của component gốc, bo góc 999, xếp ngang, cách nhau 0, lề trên/phải/dưới/trái 0/0/0/0, căn CENTER/CENTER, độ mờ của lớp 0.38
        - Text "label", vị trí 250,44, cỡ 19 x 24, chữ "3s", kiểu titleMedium (16/24, đậm 600), màu #111111 (onSecondary, onSecondaryContainer, inverseOnSurface, onSuccessContainer, category/onCategory, camera/onControl)
      - Instance "CameraIconButton", vị trí 291,32, cỡ 48 x 48, nền #FFC83D (secondaryContainer, shadowSticker, category/men), viền #111111 (onSecondary, onSecondaryContainer, inverseOnSurface, onSuccessContainer, category/onCategory, camera/onControl), dày 2.0, CHÚ Ý: ghi đè độ mờ của component gốc, bo góc 999, xếp ngang, cách nhau 0, lề trên/phải/dưới/trái 0/0/0/0, căn CENTER/CENTER, độ mờ của lớp 0.38
        - Text "icon", vị trí 303,44, cỡ 24 x 24, chữ "grid_on", kiểu ngoài hệ thống (24/24, đậm 400), màu #111111 (onSecondary, onSecondaryContainer, inverseOnSurface, onSuccessContainer, category/onCategory, camera/onControl)
      - Instance "CameraIconButton", vị trí 347,32, cỡ 48 x 48, nền #FFFFFF (secondary, onBackground, onSurface, outline, camera/onCamera, camera/control, camera/overlayStroke), viền #111111 (onSecondary, onSecondaryContainer, inverseOnSurface, onSuccessContainer, category/onCategory, camera/onControl), dày 2.0, bo góc 999, xếp ngang, cách nhau 0, lề trên/phải/dưới/trái 0/0/0/0, căn CENTER/CENTER
        - Text "icon", vị trí 359,44, cỡ 24 x 24, chữ "settings", kiểu ngoài hệ thống (24/24, đậm 400), màu #111111 (onSecondary, onSecondaryContainer, inverseOnSurface, onSuccessContainer, category/onCategory, camera/onControl)
  - Frame "Preview 3:4", vị trí 0,92, cỡ 411 x 548, nền #1E1E1E (surface, surfaceContainer, camera/surface)
    - Rectangle "camera-feed (ảnh tạm)", vị trí 0,92, cỡ 411 x 548, nền #1E1E1E (surface, surfaceContainer, camera/surface)
    - Instance "GridOverlay", vị trí 0,92, cỡ 411 x 548, CHÚ Ý: ghi đè màu nền của component gốc
      - Rectangle "line v1", vị trí 137,92, cỡ 1 x 548, nền #FFFFFF (secondary, onBackground, onSurface, outline, camera/onCamera, camera/control, camera/overlayStroke)
      - Rectangle "line v2", vị trí 274,92, cỡ 1 x 548, nền #FFFFFF (secondary, onBackground, onSurface, outline, camera/onCamera, camera/control, camera/overlayStroke)
      - Rectangle "line h1", vị trí 0,275, cỡ 411 x 1, nền #FFFFFF (secondary, onBackground, onSurface, outline, camera/onCamera, camera/control, camera/overlayStroke)
      - Rectangle "line h2", vị trí 0,457, cỡ 411 x 1, nền #FFFFFF (secondary, onBackground, onSurface, outline, camera/onCamera, camera/control, camera/overlayStroke)
    - Frame "Pose overlay", vị trí 106,186, cỡ 200 x 400
      - Vector "Vector", vị trí 179,199, cỡ 53 x 53, viền #FFFFFF (secondary, onBackground, onSurface, outline, camera/onCamera, camera/control, camera/overlayStroke), dày 3.499999761581421
      - Vector "Vector", vị trí 125,259, cỡ 144 x 310, viền #FFFFFF (secondary, onBackground, onSurface, outline, camera/onCamera, camera/control, camera/overlayStroke), dày 3.499999761581421
    - Instance "StepCard", vị trí 16,108, cỡ 379 x 68, nền #FFFFFF (secondary, onBackground, onSurface, outline, camera/onCamera, camera/control, camera/overlayStroke), viền #111111 (onSecondary, onSecondaryContainer, inverseOnSurface, onSuccessContainer, category/onCategory, camera/onControl), dày 2.0, CHÚ Ý: ghi đè độ mờ của component gốc, bo góc 16, xếp ngang, cách nhau 8 (sm), lề trên/phải/dưới/trái 8 (sm)/4 (xs)/8 (sm)/16 (md), căn MIN/CENTER
      - Frame "badge", vị trí 34,126, cỡ 41 x 32, nền #FFC83D (secondaryContainer, shadowSticker, category/men), viền #111111 (onSecondary, onSecondaryContainer, inverseOnSurface, onSuccessContainer, category/onCategory, camera/onControl), dày 2.0, bo góc 999, xếp ngang, cách nhau 0, lề trên/phải/dưới/trái 4 (xs)/8 (sm)/4 (xs)/8 (sm), căn MIN/MIN
        - Text "step", vị trí 44,132, cỡ 21 x 20, chữ "2/4", kiểu titleSmall, labelLarge (14/20, đậm 500), màu #111111 (onSecondary, onSecondaryContainer, inverseOnSurface, onSuccessContainer, category/onCategory, camera/onControl)
      - Text "text", vị trí 83,122, cỡ 210 x 40, chữ "Xoay vai 45° về phía máy, cằm hơi hạ.", kiểu bodyMedium (14/20, đậm 400), màu #111111 (onSecondary, onSecondaryContainer, inverseOnSurface, onSuccessContainer, category/onCategory, camera/onControl)
      - Frame "chevron_left", vị trí 301,118, cỡ 40 x 48, xếp ngang, cách nhau 0, lề trên/phải/dưới/trái 0/0/0/0, căn CENTER/CENTER, độ mờ của lớp 0.38
        - Text "icon", vị trí 309,130, cỡ 24 x 24, chữ "chevron_left", kiểu ngoài hệ thống (24/24, đậm 400), màu #111111 (onSecondary, onSecondaryContainer, inverseOnSurface, onSuccessContainer, category/onCategory, camera/onControl)
      - Frame "chevron_right", vị trí 349,118, cỡ 40 x 48, xếp ngang, cách nhau 0, lề trên/phải/dưới/trái 0/0/0/0, căn CENTER/CENTER, độ mờ của lớp 0.38
        - Text "icon", vị trí 357,130, cỡ 24 x 24, chữ "chevron_right", kiểu ngoài hệ thống (24/24, đậm 400), màu #111111 (onSecondary, onSecondaryContainer, inverseOnSurface, onSuccessContainer, category/onCategory, camera/onControl)
    - Frame "Overlay tools", vị trí 347,242, cỡ 48 x 216, xếp dọc, cách nhau 8 (sm), lề trên/phải/dưới/trái 0/0/0/0, căn MIN/MIN, độ mờ của lớp 0.38
      - Instance "CameraIconButton", vị trí 347,242, cỡ 48 x 48, nền #FFFFFF (secondary, onBackground, onSurface, outline, camera/onCamera, camera/control, camera/overlayStroke), viền #111111 (onSecondary, onSecondaryContainer, inverseOnSurface, onSuccessContainer, category/onCategory, camera/onControl), dày 2.0, bo góc 999, xếp ngang, cách nhau 0, lề trên/phải/dưới/trái 0/0/0/0, căn CENTER/CENTER
        - Text "icon", vị trí 359,254, cỡ 24 x 24, chữ "tune", kiểu ngoài hệ thống (24/24, đậm 400), màu #111111 (onSecondary, onSecondaryContainer, inverseOnSurface, onSuccessContainer, category/onCategory, camera/onControl)
      - Instance "CameraIconButton", vị trí 347,298, cỡ 48 x 48, nền #FFFFFF (secondary, onBackground, onSurface, outline, camera/onCamera, camera/control, camera/overlayStroke), viền #111111 (onSecondary, onSecondaryContainer, inverseOnSurface, onSuccessContainer, category/onCategory, camera/onControl), dày 2.0, bo góc 999, xếp ngang, cách nhau 0, lề trên/phải/dưới/trái 0/0/0/0, căn CENTER/CENTER
        - Text "icon", vị trí 359,310, cỡ 24 x 24, chữ "flip", kiểu ngoài hệ thống (24/24, đậm 400), màu #111111 (onSecondary, onSecondaryContainer, inverseOnSurface, onSuccessContainer, category/onCategory, camera/onControl)
      - Instance "CameraIconButton", vị trí 347,354, cỡ 48 x 48, nền #FFFFFF (secondary, onBackground, onSurface, outline, camera/onCamera, camera/control, camera/overlayStroke), viền #111111 (onSecondary, onSecondaryContainer, inverseOnSurface, onSuccessContainer, category/onCategory, camera/onControl), dày 2.0, bo góc 999, xếp ngang, cách nhau 0, lề trên/phải/dưới/trái 0/0/0/0, căn CENTER/CENTER
        - Text "icon", vị trí 359,366, cỡ 24 x 24, chữ "lock_open", kiểu ngoài hệ thống (24/24, đậm 400), màu #111111 (onSecondary, onSecondaryContainer, inverseOnSurface, onSuccessContainer, category/onCategory, camera/onControl)
      - Instance "CameraIconButton", vị trí 347,410, cỡ 48 x 48, nền #FFC83D (secondaryContainer, shadowSticker, category/men), viền #111111 (onSecondary, onSecondaryContainer, inverseOnSurface, onSuccessContainer, category/onCategory, camera/onControl), dày 2.0, bo góc 999, xếp ngang, cách nhau 0, lề trên/phải/dưới/trái 0/0/0/0, căn CENTER/CENTER
        - Text "icon", vị trí 359,422, cỡ 24 x 24, chữ "visibility", kiểu ngoài hệ thống (24/24, đậm 400), màu #111111 (onSecondary, onSecondaryContainer, inverseOnSurface, onSuccessContainer, category/onCategory, camera/onControl)
    - Instance "ZoomControl", vị trí 138,576, cỡ 136 x 48, nền #000000 (độ mờ 0.45, lớp tô 0.45) (camera/scrim), CHÚ Ý: ghi đè màu nền của component gốc, bo góc 999, xếp ngang, cách nhau 4 (xs), lề trên/phải/dưới/trái 4 (xs)/4 (xs)/4 (xs)/4 (xs), căn MIN/CENTER
      - Frame "zoom 0.5", vị trí 142,580, cỡ 40 x 40, bo góc 999, xếp ngang, cách nhau 0, lề trên/phải/dưới/trái 0/0/0/0, căn CENTER/CENTER
        - Text "0.5", vị trí 151,590, cỡ 21 x 20, chữ "0.5", kiểu titleSmall, labelLarge (14/20, đậm 500), màu #FFFFFF (secondary, onBackground, onSurface, outline, camera/onCamera, camera/control, camera/overlayStroke)
      - Frame "zoom 1x", vị trí 186,580, cỡ 40 x 40, nền #FFC83D (secondaryContainer, shadowSticker, category/men), viền #111111 (onSecondary, onSecondaryContainer, inverseOnSurface, onSuccessContainer, category/onCategory, camera/onControl), dày 2.0, bo góc 999, xếp ngang, cách nhau 0, lề trên/phải/dưới/trái 0/0/0/0, căn CENTER/CENTER
        - Text "1x", vị trí 200,590, cỡ 12 x 20, chữ "1x", kiểu titleSmall, labelLarge (14/20, đậm 500), màu #111111 (onSecondary, onSecondaryContainer, inverseOnSurface, onSuccessContainer, category/onCategory, camera/onControl)
      - Frame "zoom 2", vị trí 230,580, cỡ 40 x 40, bo góc 999, xếp ngang, cách nhau 0, lề trên/phải/dưới/trái 0/0/0/0, căn CENTER/CENTER
        - Text "2", vị trí 245,590, cỡ 9 x 20, chữ "2", kiểu titleSmall, labelLarge (14/20, đậm 500), màu #FFFFFF (secondary, onBackground, onSurface, outline, camera/onCamera, camera/control, camera/overlayStroke)
    - Rectangle "scrim", vị trí 0,92, cỡ 411 x 548, nền #000000 (độ mờ 0.35, lớp tô 0.35) (camera/scrim)
    - Frame "countdown", vị trí 142,302, cỡ 128 x 128, nền #FFC83D (secondaryContainer, shadowSticker, category/men), viền #111111 (onSecondary, onSecondaryContainer, inverseOnSurface, onSuccessContainer, category/onCategory, camera/onControl), dày 2.0, bo góc 999, xếp ngang, cách nhau 0, lề trên/phải/dưới/trái 0/0/0/0, căn CENTER/CENTER
      - Text "count", vị trí 188,334, cỡ 34 x 64, chữ "3", kiểu displayLarge (57/64, đậm 800), màu #111111 (onSecondary, onSecondaryContainer, inverseOnSurface, onSuccessContainer, category/onCategory, camera/onControl)
  - Frame "Group chips", vị trí 16,656, cỡ 581 x 36, xếp ngang, cách nhau 8 (sm), lề trên/phải/dưới/trái 0/0/0/0, căn MIN/MIN, độ mờ của lớp 0.38
    - Instance "Chip", vị trí 16,656, cỡ 76 x 36, nền #FFC83D (secondaryContainer, shadowSticker, category/men), viền #FFFFFF (secondary, onBackground, onSurface, outline, camera/onCamera, camera/control, camera/overlayStroke), dày 2.0, bo góc 18, xếp ngang, cách nhau 0, lề trên/phải/dưới/trái 0/16 (md)/0/16 (md), căn MIN/CENTER
      - Text "label", vị trí 34,664, cỡ 40 x 20, chữ "Tất cả", kiểu titleSmall, labelLarge (14/20, đậm 500), màu #111111 (onSecondary, onSecondaryContainer, inverseOnSurface, onSuccessContainer, category/onCategory, camera/onControl)
      - Instance "SelectedBadge", vị trí 78,647, cỡ 22 x 22, nền #FFC83D (secondaryContainer, shadowSticker, category/men), viền #FFFFFF (secondary, onBackground, onSurface, outline, camera/onCamera, camera/control, camera/overlayStroke), dày 2.0, bo góc 999, xếp ngang, cách nhau 0, lề trên/phải/dưới/trái 0/0/0/0, căn CENTER/CENTER
        - Text "icon", vị trí 82,651, cỡ 14 x 14, chữ "check", kiểu ngoài hệ thống (14/14, đậm 400), màu #111111 (onSecondary, onSecondaryContainer, inverseOnSurface, onSuccessContainer, category/onCategory, camera/onControl)
    - Instance "Chip", vị trí 100,656, cỡ 56 x 36, nền #1E1E1E (surface, surfaceContainer, camera/surface), viền #FFFFFF (secondary, onBackground, onSurface, outline, camera/onCamera, camera/control, camera/overlayStroke), dày 2.0, bo góc 18, xếp ngang, cách nhau 0, lề trên/phải/dưới/trái 0/16 (md)/0/16 (md), căn MIN/CENTER
      - Text "label", vị trí 118,664, cỡ 20 x 20, chữ "Nữ", kiểu titleSmall, labelLarge (14/20, đậm 500), màu #FFFFFF (secondary, onBackground, onSurface, outline, camera/onCamera, camera/control, camera/overlayStroke)
    - Instance "Chip", vị trí 164,656, cỡ 68 x 36, nền #1E1E1E (surface, surfaceContainer, camera/surface), viền #FFFFFF (secondary, onBackground, onSurface, outline, camera/onCamera, camera/control, camera/overlayStroke), dày 2.0, bo góc 18, xếp ngang, cách nhau 0, lề trên/phải/dưới/trái 0/16 (md)/0/16 (md), căn MIN/CENTER
      - Text "label", vị trí 182,664, cỡ 32 x 20, chữ "Nam", kiểu titleSmall, labelLarge (14/20, đậm 500), màu #FFFFFF (secondary, onBackground, onSurface, outline, camera/onCamera, camera/control, camera/overlayStroke)
    - Instance "Chip", vị trí 240,656, cỡ 87 x 36, nền #1E1E1E (surface, surfaceContainer, camera/surface), viền #FFFFFF (secondary, onBackground, onSurface, outline, camera/onCamera, camera/control, camera/overlayStroke), dày 2.0, bo góc 18, xếp ngang, cách nhau 0, lề trên/phải/dưới/trái 0/16 (md)/0/16 (md), căn MIN/CENTER
      - Text "label", vị trí 258,664, cỡ 51 x 20, chữ "Cặp đôi", kiểu titleSmall, labelLarge (14/20, đậm 500), màu #FFFFFF (secondary, onBackground, onSurface, outline, camera/onCamera, camera/control, camera/overlayStroke)
    - Instance "Chip", vị trí 335,656, cỡ 82 x 36, nền #1E1E1E (surface, surfaceContainer, camera/surface), viền #FFFFFF (secondary, onBackground, onSurface, outline, camera/onCamera, camera/control, camera/overlayStroke), dày 2.0, bo góc 18, xếp ngang, cách nhau 0, lề trên/phải/dưới/trái 0/16 (md)/0/16 (md), căn MIN/CENTER
      - Text "label", vị trí 353,664, cỡ 46 x 20, chữ "Bạn bè", kiểu titleSmall, labelLarge (14/20, đậm 500), màu #FFFFFF (secondary, onBackground, onSurface, outline, camera/onCamera, camera/control, camera/overlayStroke)
    - Instance "Chip", vị trí 425,656, cỡ 82 x 36, nền #1E1E1E (surface, surfaceContainer, camera/surface), viền #FFFFFF (secondary, onBackground, onSurface, outline, camera/onCamera, camera/control, camera/overlayStroke), dày 2.0, bo góc 18, xếp ngang, cách nhau 0, lề trên/phải/dưới/trái 0/16 (md)/0/16 (md), căn MIN/CENTER
      - Text "label", vị trí 443,664, cỡ 46 x 20, chữ "Du lịch", kiểu titleSmall, labelLarge (14/20, đậm 500), màu #FFFFFF (secondary, onBackground, onSurface, outline, camera/onCamera, camera/control, camera/overlayStroke)
    - Instance "Chip", vị trí 515,656, cỡ 82 x 36, nền #1E1E1E (surface, surfaceContainer, camera/surface), viền #FFFFFF (secondary, onBackground, onSurface, outline, camera/onCamera, camera/control, camera/overlayStroke), dày 2.0, bo góc 18, xếp ngang, cách nhau 0, lề trên/phải/dưới/trái 0/16 (md)/0/16 (md), căn MIN/CENTER
      - Text "label", vị trí 533,664, cỡ 46 x 20, chữ "Cà phê", kiểu titleSmall, labelLarge (14/20, đậm 500), màu #FFFFFF (secondary, onBackground, onSurface, outline, camera/onCamera, camera/control, camera/overlayStroke)
  - Frame "Pose strip", vị trí 16,708, cỡ 568 x 64, xếp ngang, cách nhau 8 (sm), lề trên/phải/dưới/trái 0/0/0/0, căn MIN/MIN, độ mờ của lớp 0.38
    - Instance "PoseThumb", vị trí 16,708, cỡ 64 x 64
      - Frame "image", vị trí 16,708, cỡ 64 x 64, nền #2A2A2A (surfaceVariant), viền #FFFFFF (secondary, onBackground, onSurface, outline, camera/onCamera, camera/control, camera/overlayStroke), dày 2.0, bo góc 14
        - Frame "pose-outline", vị trí 35,714, cỡ 26 x 52
          - Vector "Vector", vị trí 45,716, cỡ 7 x 7, viền #B5B0A8 (onSurfaceVariant, camera/onSurfaceVariant), dày 1.499333381652832
          - Vector "Vector", vị trí 38,724, cỡ 19 x 40, viền #B5B0A8 (onSurfaceVariant, camera/onSurfaceVariant), dày 1.499333381652832
    - Instance "PoseThumb", vị trí 88,708, cỡ 64 x 64
      - Frame "image", vị trí 88,708, cỡ 64 x 64, nền #2A2A2A (surfaceVariant), viền #FFC83D (secondaryContainer, shadowSticker, category/men), dày 3.0, bo góc 14
        - Frame "pose-outline", vị trí 107,714, cỡ 26 x 52
          - Vector "Vector", vị trí 117,716, cỡ 7 x 7, viền #FFFFFF (secondary, onBackground, onSurface, outline, camera/onCamera, camera/control, camera/overlayStroke), dày 1.499333381652832
          - Vector "Vector", vị trí 110,724, cỡ 19 x 40, viền #FFFFFF (secondary, onBackground, onSurface, outline, camera/onCamera, camera/control, camera/overlayStroke), dày 1.499333381652832
      - Instance "SelectedBadge", vị trí 137,701, cỡ 22 x 22, nền #FFC83D (secondaryContainer, shadowSticker, category/men), viền #FFFFFF (secondary, onBackground, onSurface, outline, camera/onCamera, camera/control, camera/overlayStroke), dày 2.0, bo góc 999, xếp ngang, cách nhau 0, lề trên/phải/dưới/trái 0/0/0/0, căn CENTER/CENTER
        - Text "icon", vị trí 141,705, cỡ 14 x 14, chữ "check", kiểu ngoài hệ thống (14/14, đậm 400), màu #111111 (onSecondary, onSecondaryContainer, inverseOnSurface, onSuccessContainer, category/onCategory, camera/onControl)
    - Instance "PoseThumb", vị trí 160,708, cỡ 64 x 64
      - Frame "image", vị trí 160,708, cỡ 64 x 64, nền #2A2A2A (surfaceVariant), viền #FFFFFF (secondary, onBackground, onSurface, outline, camera/onCamera, camera/control, camera/overlayStroke), dày 2.0, bo góc 14
        - Frame "pose-outline", vị trí 179,714, cỡ 26 x 52
          - Vector "Vector", vị trí 189,716, cỡ 7 x 7, viền #B5B0A8 (onSurfaceVariant, camera/onSurfaceVariant), dày 1.499333381652832
          - Vector "Vector", vị trí 182,724, cỡ 19 x 40, viền #B5B0A8 (onSurfaceVariant, camera/onSurfaceVariant), dày 1.499333381652832
    - Instance "PoseThumb", vị trí 232,708, cỡ 64 x 64
      - Frame "image", vị trí 232,708, cỡ 64 x 64, nền #2A2A2A (surfaceVariant), viền #FFFFFF (secondary, onBackground, onSurface, outline, camera/onCamera, camera/control, camera/overlayStroke), dày 2.0, bo góc 14
        - Frame "pose-outline", vị trí 251,714, cỡ 26 x 52
          - Vector "Vector", vị trí 261,716, cỡ 7 x 7, viền #B5B0A8 (onSurfaceVariant, camera/onSurfaceVariant), dày 1.499333381652832
          - Vector "Vector", vị trí 254,724, cỡ 19 x 40, viền #B5B0A8 (onSurfaceVariant, camera/onSurfaceVariant), dày 1.499333381652832
    - Instance "PoseThumb", vị trí 304,708, cỡ 64 x 64
      - Frame "image", vị trí 304,708, cỡ 64 x 64, nền #2A2A2A (surfaceVariant), viền #FFFFFF (secondary, onBackground, onSurface, outline, camera/onCamera, camera/control, camera/overlayStroke), dày 2.0, bo góc 14
        - Frame "pose-outline", vị trí 323,714, cỡ 26 x 52
          - Vector "Vector", vị trí 333,716, cỡ 7 x 7, viền #B5B0A8 (onSurfaceVariant, camera/onSurfaceVariant), dày 1.499333381652832
          - Vector "Vector", vị trí 326,724, cỡ 19 x 40, viền #B5B0A8 (onSurfaceVariant, camera/onSurfaceVariant), dày 1.499333381652832
    - Instance "PoseThumb", vị trí 376,708, cỡ 64 x 64
      - Frame "image", vị trí 376,708, cỡ 64 x 64, nền #2A2A2A (surfaceVariant), viền #FFFFFF (secondary, onBackground, onSurface, outline, camera/onCamera, camera/control, camera/overlayStroke), dày 2.0, bo góc 14
        - Frame "pose-outline", vị trí 395,714, cỡ 26 x 52
          - Vector "Vector", vị trí 405,716, cỡ 7 x 7, viền #B5B0A8 (onSurfaceVariant, camera/onSurfaceVariant), dày 1.499333381652832
          - Vector "Vector", vị trí 398,724, cỡ 19 x 40, viền #B5B0A8 (onSurfaceVariant, camera/onSurfaceVariant), dày 1.499333381652832
    - Instance "PoseThumb", vị trí 448,708, cỡ 64 x 64
      - Frame "image", vị trí 448,708, cỡ 64 x 64, nền #2A2A2A (surfaceVariant), viền #FFFFFF (secondary, onBackground, onSurface, outline, camera/onCamera, camera/control, camera/overlayStroke), dày 2.0, bo góc 14
        - Frame "pose-outline", vị trí 467,714, cỡ 26 x 52
          - Vector "Vector", vị trí 477,716, cỡ 7 x 7, viền #B5B0A8 (onSurfaceVariant, camera/onSurfaceVariant), dày 1.499333381652832
          - Vector "Vector", vị trí 470,724, cỡ 19 x 40, viền #B5B0A8 (onSurfaceVariant, camera/onSurfaceVariant), dày 1.499333381652832
    - Instance "PoseThumb", vị trí 520,708, cỡ 64 x 64
      - Frame "image", vị trí 520,708, cỡ 64 x 64, nền #2A2A2A (surfaceVariant), viền #FFFFFF (secondary, onBackground, onSurface, outline, camera/onCamera, camera/control, camera/overlayStroke), dày 2.0, bo góc 14
        - Frame "pose-outline", vị trí 539,714, cỡ 26 x 52
          - Vector "Vector", vị trí 549,716, cỡ 7 x 7, viền #B5B0A8 (onSurfaceVariant, camera/onSurfaceVariant), dày 1.499333381652832
          - Vector "Vector", vị trí 542,724, cỡ 19 x 40, viền #B5B0A8 (onSurfaceVariant, camera/onSurfaceVariant), dày 1.499333381652832
  - Rectangle "Last photo (ảnh tạm)", vị trí 44,798, cỡ 48 x 48, nền #1E1E1E (surface, surfaceContainer, camera/surface), viền #FFFFFF (secondary, onBackground, onSurface, outline, camera/onCamera, camera/control, camera/overlayStroke), dày 2.0, bo góc 14
  - Ellipse "Shutter ring", vị trí 168,784, cỡ 76 x 76, viền #FFFFFF (secondary, onBackground, onSurface, outline, camera/onCamera, camera/control, camera/overlayStroke), dày 4.0
  - Ellipse "Shutter (hủy hẹn giờ)", vị trí 174,791, cỡ 62 x 62, nền #FFFFFF (secondary, onBackground, onSurface, outline, camera/onCamera, camera/control, camera/overlayStroke)
  - Text "cancel", vị trí 192,808, cỡ 28 x 28, chữ "close", kiểu ngoài hệ thống (28/34, đậm 400), màu #111111 (onSecondary, onSecondaryContainer, inverseOnSurface, onSuccessContainer, category/onCategory, camera/onControl)
  - Instance "CameraIconButton", vị trí 319,798, cỡ 48 x 48, nền #FFFFFF (secondary, onBackground, onSurface, outline, camera/onCamera, camera/control, camera/overlayStroke), viền #111111 (onSecondary, onSecondaryContainer, inverseOnSurface, onSuccessContainer, category/onCategory, camera/onControl), dày 2.0, bo góc 999, xếp ngang, cách nhau 0, lề trên/phải/dưới/trái 0/0/0/0, căn CENTER/CENTER
    - Text "icon", vị trí 331,810, cỡ 24 x 24, chữ "flip_camera_android", kiểu ngoài hệ thống (24/24, đậm 400), màu #111111 (onSecondary, onSecondaryContainer, inverseOnSurface, onSuccessContainer, category/onCategory, camera/onControl)

## 4. Code tham khảo

Không có. Script không sinh code; AI viết Compose theo mục 2 và 3.
