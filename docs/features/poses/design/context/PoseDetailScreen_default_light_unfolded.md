# SỐ LIỆU THIẾT KẾ: PoseDetailScreen_default_light_unfolded

Sinh bởi `scripts/figma-export.py`. Không sửa tay; thiết kế đổi thì chạy lại script (ADR-0020).

## 1. Nguồn

- Link: https://www.figma.com/design/ZTXse6375eT3KXDUJPBTpe/?node-id=20-629
- Frame: `20:629`, giao diện sáng
- Ngày lấy: 2026-10-21

## 2. Giá trị dùng

Đối chiếu với `docs/design-system/tokens.json` theo giá trị. "ngoài hệ thống" nghĩa là không có token
nào trùng; plan quyết định làm tròn về token gần nhất hay thêm token.

| Loại | Giá trị | Token |
| --- | --- | --- |
| khoảng cách | 16 | md |
| khoảng cách | 2 | ngoài hệ thống |
| khoảng cách | 24 | lg |
| khoảng cách | 4 | xs |
| khoảng cách | 8 | sm |
| kiểu chữ | 14/20, đậm 400 | bodyMedium |
| kiểu chữ | 14/20, đậm 500 | titleSmall, labelLarge |
| kiểu chữ | 16/24, đậm 600 | titleMedium |
| kiểu chữ | 20/20, đậm 400 | ngoài hệ thống |
| kiểu chữ | 22/28, đậm 800 | ngoài hệ thống |
| kiểu chữ | 24/24, đậm 400 | ngoài hệ thống |
| màu chữ | #111111 | ngoài hệ thống |
| màu chữ | #4A0F22 | ngoài hệ thống |
| màu chữ | #5A5650 | ngoài hệ thống |
| màu chữ | #FFFFFF | onPrimary, onSecondary, onTertiary, onError, surfaceContainerLowest |
| màu nền | #111111 | ngoài hệ thống |
| màu nền | #D9D4C8 | ngoài hệ thống |
| màu nền | #FF5C8A | ngoài hệ thống |
| màu nền | #FFC83D | ngoài hệ thống |
| màu nền | #FFD6E2 | ngoài hệ thống |
| màu nền | #FFFDF8 | ngoài hệ thống |

## 3. Cấu trúc và số liệu

Toạ độ và cỡ tính theo dp, gốc là góc trên trái của khung. Thứ tự từ trên xuống như trong Figma.

- Frame "PoseDetailScreen_default_light_unfolded", vị trí 0,0, cỡ 673 x 841, nền #FFFDF8 (ngoài hệ thống)
  - Instance "TopAppBar", vị trí 0,8, cỡ 673 x 64, xếp ngang, cách nhau 4 (xs), lề trên/phải/dưới/trái 0/4 (xs)/0/4 (xs), căn MIN/CENTER
    - Instance "leading", vị trí 4,16, cỡ 48 x 48, bo góc 999, xếp ngang, cách nhau 0, lề trên/phải/dưới/trái 0/0/0/0, căn CENTER/CENTER
      - Text "icon", vị trí 16,28, cỡ 24 x 24, chữ "arrow_back", kiểu ngoài hệ thống (24/24, đậm 400), màu #111111 (ngoài hệ thống)
    - Text "title", vị trí 56,26, cỡ 561 x 28, chữ "Tựa lan can", kiểu ngoài hệ thống (22/28, đậm 800), màu #111111 (ngoài hệ thống)
    - Instance "trailing", vị trí 621,16, cỡ 48 x 48, bo góc 999, xếp ngang, cách nhau 0, lề trên/phải/dưới/trái 0/0/0/0, căn CENTER/CENTER
      - Text "icon", vị trí 633,28, cỡ 24 x 24, chữ "bookmark", kiểu ngoài hệ thống (24/24, đậm 400), màu #111111 (ngoài hệ thống)
  - Frame "image", vị trí 16,80, cỡ 315 x 440, nền #FF5C8A (ngoài hệ thống), viền #111111 (ngoài hệ thống), dày 2.0, bo góc 16
    - Frame "pose-outline", vị trí 98,150, cỡ 150 x 300
      - Vector "Vector", vị trí 154,160, cỡ 40 x 40, viền #111111 (ngoài hệ thống), dày 3.0
      - Vector "Vector", vị trí 113,205, cỡ 108 x 232, viền #111111 (ngoài hệ thống), dày 3.0
  - Frame "content", vị trí 347,80, cỡ 310 x 338, xếp dọc, cách nhau 16 (md), lề trên/phải/dưới/trái 0/0/0/0, căn MIN/MIN
    - Text "meta", vị trí 347,80, cỡ 310 x 20, chữ "Một người · Nữ · Toàn thân · 4 bước", kiểu bodyMedium (14/20, đậm 400), màu #5A5650 (ngoài hệ thống)
    - Text "Các bước", vị trí 347,116, cỡ 310 x 24, chữ "Các bước", kiểu titleMedium (16/24, đậm 600), màu #111111 (ngoài hệ thống)
    - Frame "steps", vị trí 347,156, cỡ 310 x 148, xếp dọc, cách nhau 8 (sm), lề trên/phải/dưới/trái 0/0/0/0, căn MIN/MIN
      - Frame "step 1", vị trí 347,156, cỡ 310 x 40, xếp ngang, cách nhau 8 (sm), lề trên/phải/dưới/trái 0/0/0/0, căn MIN/MIN
        - Frame "number", vị trí 347,156, cỡ 28 x 28, nền #FFC83D (ngoài hệ thống), viền #111111 (ngoài hệ thống), dày 2.0, bo góc 999, xếp ngang, cách nhau 0, lề trên/phải/dưới/trái 0/0/0/0, căn CENTER/CENTER
          - Text "1", vị trí 358,160, cỡ 5 x 20, chữ "1", kiểu titleSmall, labelLarge (14/20, đậm 500), màu #111111 (ngoài hệ thống)
        - Text "Đứng chéo 45° so với máy, chân", vị trí 383,156, cỡ 274 x 40, chữ "Đứng chéo 45° so với máy, chân trước hơi khuỵu.", kiểu bodyMedium (14/20, đậm 400), màu #111111 (ngoài hệ thống)
      - Frame "step 2", vị trí 347,204, cỡ 310 x 28, xếp ngang, cách nhau 8 (sm), lề trên/phải/dưới/trái 0/0/0/0, căn MIN/MIN
        - Frame "number", vị trí 347,204, cỡ 28 x 28, nền #FFC83D (ngoài hệ thống), viền #111111 (ngoài hệ thống), dày 2.0, bo góc 999, xếp ngang, cách nhau 0, lề trên/phải/dưới/trái 0/0/0/0, căn CENTER/CENTER
          - Text "2", vị trí 356,208, cỡ 9 x 20, chữ "2", kiểu titleSmall, labelLarge (14/20, đậm 500), màu #111111 (ngoài hệ thống)
        - Text "Tay trái chống hông, khuỷu tay", vị trí 383,204, cỡ 274 x 20, chữ "Tay trái chống hông, khuỷu tay mở ra sau.", kiểu bodyMedium (14/20, đậm 400), màu #111111 (ngoài hệ thống)
      - Frame "step 3", vị trí 347,240, cỡ 310 x 28, xếp ngang, cách nhau 8 (sm), lề trên/phải/dưới/trái 0/0/0/0, căn MIN/MIN
        - Frame "number", vị trí 347,240, cỡ 28 x 28, nền #FFC83D (ngoài hệ thống), viền #111111 (ngoài hệ thống), dày 2.0, bo góc 999, xếp ngang, cách nhau 0, lề trên/phải/dưới/trái 0/0/0/0, căn CENTER/CENTER
          - Text "3", vị trí 356,244, cỡ 9 x 20, chữ "3", kiểu titleSmall, labelLarge (14/20, đậm 500), màu #111111 (ngoài hệ thống)
        - Text "Tay phải thả lỏng dọc thân.", vị trí 383,240, cỡ 274 x 20, chữ "Tay phải thả lỏng dọc thân.", kiểu bodyMedium (14/20, đậm 400), màu #111111 (ngoài hệ thống)
      - Frame "step 4", vị trí 347,276, cỡ 310 x 28, xếp ngang, cách nhau 8 (sm), lề trên/phải/dưới/trái 0/0/0/0, căn MIN/MIN
        - Frame "number", vị trí 347,276, cỡ 28 x 28, nền #FFC83D (ngoài hệ thống), viền #111111 (ngoài hệ thống), dày 2.0, bo góc 999, xếp ngang, cách nhau 0, lề trên/phải/dưới/trái 0/0/0/0, căn CENTER/CENTER
          - Text "4", vị trí 356,280, cỡ 9 x 20, chữ "4", kiểu titleSmall, labelLarge (14/20, đậm 500), màu #111111 (ngoài hệ thống)
        - Text "Xoay vai về phía máy, cằm hơi ", vị trí 383,276, cỡ 274 x 20, chữ "Xoay vai về phía máy, cằm hơi hạ.", kiểu bodyMedium (14/20, đậm 400), màu #111111 (ngoài hệ thống)
    - Frame "Common mistake", vị trí 347,320, cỡ 310 x 98, nền #FFD6E2 (ngoài hệ thống), viền #111111 (ngoài hệ thống), dày 2.0, bo góc 16, xếp ngang, cách nhau 8 (sm), lề trên/phải/dưới/trái 16 (md)/16 (md)/16 (md)/16 (md), căn MIN/MIN
      - Text "icon", vị trí 365,338, cỡ 24 x 24, chữ "warning", kiểu ngoài hệ thống (24/24, đậm 400), màu #4A0F22 (ngoài hệ thống)
      - Frame "text", vị trí 397,338, cỡ 242 x 62, xếp dọc, cách nhau 2 (ngoài hệ thống), lề trên/phải/dưới/trái 0/0/0/0, căn MIN/MIN
        - Text "Lỗi hay gặp", vị trí 397,338, cỡ 242 x 20, chữ "Lỗi hay gặp", kiểu titleSmall, labelLarge (14/20, đậm 500), màu #4A0F22 (ngoài hệ thống)
        - Text "Rút vai lên sát tai làm cổ ngắ", vị trí 397,360, cỡ 242 x 40, chữ "Rút vai lên sát tai làm cổ ngắn lại. Thả vai xuống trước khi chụp.", kiểu bodyMedium (14/20, đậm 400), màu #4A0F22 (ngoài hệ thống)
  - Frame "Bottom bar", vị trí 347,745, cỡ 326 x 96, nền #FFFDF8 (ngoài hệ thống)
    - Rectangle "divider", vị trí 347,745, cỡ 326 x 2, nền #D9D4C8 (ngoài hệ thống)
    - Instance "Button", vị trí 363,761, cỡ 294 x 60, nền #111111 (ngoài hệ thống), viền #111111 (ngoài hệ thống), dày 2.0, bo góc 14, xếp ngang, cách nhau 8 (sm), lề trên/phải/dưới/trái 16 (md)/24 (lg)/16 (md)/24 (lg), căn CENTER/CENTER
      - Text "icon", vị trí 422,781, cỡ 20 x 20, chữ "photo_camera", kiểu ngoài hệ thống (20/20, đậm 400), màu #FFFFFF (onPrimary, onSecondary, onTertiary, onError, surfaceContainerLowest)
      - Text "label", vị trí 450,779, cỡ 149 x 24, chữ "Chụp với tư thế này", kiểu titleMedium (16/24, đậm 600), màu #FFFFFF (onPrimary, onSecondary, onTertiary, onError, surfaceContainerLowest)

## 4. Code tham khảo

Không có. Script không sinh code; AI viết Compose theo mục 2 và 3.
