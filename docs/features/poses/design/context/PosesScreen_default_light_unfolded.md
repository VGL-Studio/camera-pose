# SỐ LIỆU THIẾT KẾ: PosesScreen_default_light_unfolded

Sinh bởi `scripts/figma-export.py`. Không sửa tay; thiết kế đổi thì chạy lại script (ADR-0020).

## 1. Nguồn

- Link: https://www.figma.com/design/ZTXse6375eT3KXDUJPBTpe/?node-id=20-370
- Frame: `20:370`, giao diện sáng
- Ngày lấy: 2026-10-21

## 2. Giá trị dùng

Đối chiếu với `docs/design-system/tokens.json` theo giá trị. "ngoài hệ thống" nghĩa là không có token
nào trùng; plan quyết định làm tròn về token gần nhất hay thêm token.

| Loại | Giá trị | Token |
| --- | --- | --- |
| khoảng cách | 16 | md |
| khoảng cách | 2 | ngoài hệ thống |
| khoảng cách | 4 | xs |
| khoảng cách | 8 | sm |
| kiểu chữ | 12/16, đậm 400 | bodySmall |
| kiểu chữ | 14/14, đậm 400 | ngoài hệ thống |
| kiểu chữ | 14/20, đậm 500 | titleSmall, labelLarge |
| kiểu chữ | 16/24, đậm 600 | titleMedium |
| kiểu chữ | 20/20, đậm 400 | ngoài hệ thống |
| kiểu chữ | 22/28, đậm 800 | ngoài hệ thống |
| kiểu chữ | 24/24, đậm 400 | ngoài hệ thống |
| màu chữ | #111111 | ngoài hệ thống |
| màu chữ | #5A5650 | ngoài hệ thống |
| màu nền | #3DDC97 | ngoài hệ thống |
| màu nền | #4D8DFF | ngoài hệ thống |
| màu nền | #5ED3F3 | ngoài hệ thống |
| màu nền | #B8E986 | ngoài hệ thống |
| màu nền | #FF5C8A | ngoài hệ thống |
| màu nền | #FF8A3D | ngoài hệ thống |
| màu nền | #FFA8D0 | ngoài hệ thống |
| màu nền | #FFC83D | ngoài hệ thống |
| màu nền | #FFFDF8 | ngoài hệ thống |
| màu nền | #FFFFFF | onPrimary, onSecondary, onTertiary, onError, surfaceContainerLowest |

## 3. Cấu trúc và số liệu

Toạ độ và cỡ tính theo dp, gốc là góc trên trái của khung. Thứ tự từ trên xuống như trong Figma.

- Frame "PosesScreen_default_light_unfolded", vị trí 0,0, cỡ 673 x 841, nền #FFFDF8 (ngoài hệ thống), xếp dọc, cách nhau 8 (sm), lề trên/phải/dưới/trái 8 (sm)/0/0/0, căn MIN/MIN
  - Instance "TopAppBar", vị trí 0,8, cỡ 673 x 64, xếp ngang, cách nhau 4 (xs), lề trên/phải/dưới/trái 0/4 (xs)/0/4 (xs), căn MIN/CENTER
    - Instance "leading", vị trí 4,16, cỡ 48 x 48, bo góc 999, xếp ngang, cách nhau 0, lề trên/phải/dưới/trái 0/0/0/0, căn CENTER/CENTER
      - Text "icon", vị trí 16,28, cỡ 24 x 24, chữ "arrow_back", kiểu ngoài hệ thống (24/24, đậm 400), màu #111111 (ngoài hệ thống)
    - Text "title", vị trí 56,26, cỡ 561 x 28, chữ "Thư viện tư thế", kiểu ngoài hệ thống (22/28, đậm 800), màu #111111 (ngoài hệ thống)
    - Instance "trailing", vị trí 621,16, cỡ 48 x 48, bo góc 999, xếp ngang, cách nhau 0, lề trên/phải/dưới/trái 0/0/0/0, căn CENTER/CENTER
      - Text "icon", vị trí 633,28, cỡ 24 x 24, chữ "settings", kiểu ngoài hệ thống (24/24, đậm 400), màu #111111 (ngoài hệ thống)
  - Frame "Subtitle", vị trí 0,80, cỡ 166 x 16, xếp ngang, cách nhau 0, lề trên/phải/dưới/trái 0/16 (md)/0/16 (md), căn MIN/MIN
    - Text "214 tư thế · 60 tư thế dùng đư", vị trí 16,80, cỡ 134 x 16, chữ "214 tư thế trong 8 nhóm", kiểu bodySmall (12/16, đậm 400), màu #5A5650 (ngoài hệ thống)
  - Frame "Group chips", vị trí 0,104, cỡ 709 x 52, xếp ngang, cách nhau 8 (sm), lề trên/phải/dưới/trái 8 (sm)/16 (md)/8 (sm)/16 (md), căn MIN/MIN
    - Instance "Chip", vị trí 16,112, cỡ 76 x 36, nền #FFC83D (ngoài hệ thống), viền #111111 (ngoài hệ thống), dày 2.0, bo góc 18, xếp ngang, cách nhau 0, lề trên/phải/dưới/trái 0/16 (md)/0/16 (md), căn MIN/CENTER
      - Text "label", vị trí 34,120, cỡ 40 x 20, chữ "Tất cả", kiểu titleSmall, labelLarge (14/20, đậm 500), màu #111111 (ngoài hệ thống)
      - Instance "SelectedBadge", vị trí 78,103, cỡ 22 x 22, nền #FFC83D (ngoài hệ thống), viền #111111 (ngoài hệ thống), dày 2.0, bo góc 999, xếp ngang, cách nhau 0, lề trên/phải/dưới/trái 0/0/0/0, căn CENTER/CENTER
        - Text "icon", vị trí 82,107, cỡ 14 x 14, chữ "check", kiểu ngoài hệ thống (14/14, đậm 400), màu #111111 (ngoài hệ thống)
    - Instance "Chip", vị trí 100,112, cỡ 79 x 36, nền #FFFFFF (onPrimary, onSecondary, onTertiary, onError, surfaceContainerLowest), viền #111111 (ngoài hệ thống), dày 2.0, bo góc 18, xếp ngang, cách nhau 0, lề trên/phải/dưới/trái 0/16 (md)/0/16 (md), căn MIN/CENTER
      - Text "label", vị trí 118,120, cỡ 43 x 20, chữ "Đã lưu", kiểu titleSmall, labelLarge (14/20, đậm 500), màu #111111 (ngoài hệ thống)
    - Instance "Chip", vị trí 187,112, cỡ 56 x 36, nền #FFFFFF (onPrimary, onSecondary, onTertiary, onError, surfaceContainerLowest), viền #111111 (ngoài hệ thống), dày 2.0, bo góc 18, xếp ngang, cách nhau 0, lề trên/phải/dưới/trái 0/16 (md)/0/16 (md), căn MIN/CENTER
      - Text "label", vị trí 205,120, cỡ 20 x 20, chữ "Nữ", kiểu titleSmall, labelLarge (14/20, đậm 500), màu #111111 (ngoài hệ thống)
    - Instance "Chip", vị trí 251,112, cỡ 68 x 36, nền #FFFFFF (onPrimary, onSecondary, onTertiary, onError, surfaceContainerLowest), viền #111111 (ngoài hệ thống), dày 2.0, bo góc 18, xếp ngang, cách nhau 0, lề trên/phải/dưới/trái 0/16 (md)/0/16 (md), căn MIN/CENTER
      - Text "label", vị trí 269,120, cỡ 32 x 20, chữ "Nam", kiểu titleSmall, labelLarge (14/20, đậm 500), màu #111111 (ngoài hệ thống)
    - Instance "Chip", vị trí 327,112, cỡ 87 x 36, nền #FFFFFF (onPrimary, onSecondary, onTertiary, onError, surfaceContainerLowest), viền #111111 (ngoài hệ thống), dày 2.0, bo góc 18, xếp ngang, cách nhau 0, lề trên/phải/dưới/trái 0/16 (md)/0/16 (md), căn MIN/CENTER
      - Text "label", vị trí 345,120, cỡ 51 x 20, chữ "Cặp đôi", kiểu titleSmall, labelLarge (14/20, đậm 500), màu #111111 (ngoài hệ thống)
    - Instance "Chip", vị trí 422,112, cỡ 82 x 36, nền #FFFFFF (onPrimary, onSecondary, onTertiary, onError, surfaceContainerLowest), viền #111111 (ngoài hệ thống), dày 2.0, bo góc 18, xếp ngang, cách nhau 0, lề trên/phải/dưới/trái 0/16 (md)/0/16 (md), căn MIN/CENTER
      - Text "label", vị trí 440,120, cỡ 46 x 20, chữ "Bạn bè", kiểu titleSmall, labelLarge (14/20, đậm 500), màu #111111 (ngoài hệ thống)
    - Instance "Chip", vị trí 512,112, cỡ 91 x 36, nền #FFFFFF (onPrimary, onSecondary, onTertiary, onError, surfaceContainerLowest), viền #111111 (ngoài hệ thống), dày 2.0, bo góc 18, xếp ngang, cách nhau 0, lề trên/phải/dưới/trái 0/16 (md)/0/16 (md), căn MIN/CENTER
      - Text "label", vị trí 530,120, cỡ 55 x 20, chữ "Gia đình", kiểu titleSmall, labelLarge (14/20, đậm 500), màu #111111 (ngoài hệ thống)
    - Instance "Chip", vị trí 611,112, cỡ 82 x 36, nền #FFFFFF (onPrimary, onSecondary, onTertiary, onError, surfaceContainerLowest), viền #111111 (ngoài hệ thống), dày 2.0, bo góc 18, xếp ngang, cách nhau 0, lề trên/phải/dưới/trái 0/16 (md)/0/16 (md), căn MIN/CENTER
      - Text "label", vị trí 629,120, cỡ 46 x 20, chữ "Du lịch", kiểu titleSmall, labelLarge (14/20, đậm 500), màu #111111 (ngoài hệ thống)
  - Frame "Grid", vị trí 0,164, cỡ 673 x 888, xếp dọc, cách nhau 16 (md), lề trên/phải/dưới/trái 4 (xs)/16 (md)/0/16 (md), căn MIN/MIN
    - Frame "Row 1", vị trí 16,168, cỡ 641 x 284, xếp ngang, cách nhau 16 (md), lề trên/phải/dưới/trái 0/0/0/0, căn MIN/MIN
      - Instance "PoseCard", vị trí 16,168, cỡ 203 x 284, nền #FFFFFF (onPrimary, onSecondary, onTertiary, onError, surfaceContainerLowest), viền #111111 (ngoài hệ thống), dày 2.0, bo góc 16, xếp dọc, cách nhau 8 (sm), lề trên/phải/dưới/trái 8 (sm)/8 (sm)/8 (sm)/8 (sm), căn MIN/MIN
        - Frame "image", vị trí 26,178, cỡ 183 x 210, nền #FF5C8A (ngoài hệ thống), viền #111111 (ngoài hệ thống), dày 2.0, bo góc 10
          - Frame "pose-outline", vị trí 80,203, cỡ 80 x 160
            - Vector "Vector", vị trí 109,208, cỡ 21 x 21, viền #111111 (ngoài hệ thống), dày 2.0
            - Vector "Vector", vị trí 87,232, cỡ 57 x 124, viền #111111 (ngoài hệ thống), dày 2.0
          - Frame "save", vị trí 163,180, cỡ 48 x 48
            - Frame "save button", vị trí 169,186, cỡ 36 x 36, nền #FFFFFF (onPrimary, onSecondary, onTertiary, onError, surfaceContainerLowest), viền #111111 (ngoài hệ thống), dày 2.0, bo góc 999, xếp ngang, cách nhau 0, lề trên/phải/dưới/trái 0/0/0/0, căn CENTER/CENTER
              - Text "icon", vị trí 177,194, cỡ 20 x 20, chữ "bookmark", kiểu ngoài hệ thống (20/20, đậm 400), màu #111111 (ngoài hệ thống)
        - Frame "text", vị trí 26,396, cỡ 183 x 46, xếp dọc, cách nhau 2 (ngoài hệ thống), lề trên/phải/dưới/trái 0/4 (xs)/4 (xs)/4 (xs), căn MIN/MIN
          - Text "name", vị trí 30,396, cỡ 175 x 24, chữ "Tựa lan can", kiểu titleMedium (16/24, đậm 600), màu #111111 (ngoài hệ thống)
          - Text "meta", vị trí 30,422, cỡ 175 x 16, chữ "Một người · 4 bước", kiểu bodySmall (12/16, đậm 400), màu #5A5650 (ngoài hệ thống)
      - Instance "PoseCard", vị trí 235,168, cỡ 203 x 284, nền #FFFFFF (onPrimary, onSecondary, onTertiary, onError, surfaceContainerLowest), viền #111111 (ngoài hệ thống), dày 2.0, bo góc 16, xếp dọc, cách nhau 8 (sm), lề trên/phải/dưới/trái 8 (sm)/8 (sm)/8 (sm)/8 (sm), căn MIN/MIN
        - Frame "image", vị trí 245,178, cỡ 183 x 210, nền #FFC83D (ngoài hệ thống), viền #111111 (ngoài hệ thống), dày 2.0, bo góc 10
          - Frame "pose-outline", vị trí 298,203, cỡ 80 x 160
            - Vector "Vector", vị trí 328,208, cỡ 21 x 21, viền #111111 (ngoài hệ thống), dày 2.0
            - Vector "Vector", vị trí 306,232, cỡ 57 x 124, viền #111111 (ngoài hệ thống), dày 2.0
          - Frame "save", vị trí 382,180, cỡ 48 x 48
            - Frame "save button", vị trí 388,186, cỡ 36 x 36, nền #FFC83D (ngoài hệ thống), viền #111111 (ngoài hệ thống), dày 2.0, bo góc 999, xếp ngang, cách nhau 0, lề trên/phải/dưới/trái 0/0/0/0, căn CENTER/CENTER
              - Text "icon", vị trí 396,194, cỡ 20 x 20, chữ "bookmark_added", kiểu ngoài hệ thống (20/20, đậm 400), màu #111111 (ngoài hệ thống)
        - Frame "text", vị trí 245,396, cỡ 183 x 46, xếp dọc, cách nhau 2 (ngoài hệ thống), lề trên/phải/dưới/trái 0/4 (xs)/4 (xs)/4 (xs), căn MIN/MIN
          - Text "name", vị trí 249,396, cỡ 175 x 24, chữ "Nhìn qua vai", kiểu titleMedium (16/24, đậm 600), màu #111111 (ngoài hệ thống)
          - Text "meta", vị trí 249,422, cỡ 175 x 16, chữ "Một người · 3 bước", kiểu bodySmall (12/16, đậm 400), màu #5A5650 (ngoài hệ thống)
      - Instance "PoseCard", vị trí 454,168, cỡ 203 x 284, nền #FFFFFF (onPrimary, onSecondary, onTertiary, onError, surfaceContainerLowest), viền #111111 (ngoài hệ thống), dày 2.0, bo góc 16, xếp dọc, cách nhau 8 (sm), lề trên/phải/dưới/trái 8 (sm)/8 (sm)/8 (sm)/8 (sm), căn MIN/MIN
        - Frame "image", vị trí 464,178, cỡ 183 x 210, nền #3DDC97 (ngoài hệ thống), viền #111111 (ngoài hệ thống), dày 2.0, bo góc 10
          - Frame "pose-outline", vị trí 518,203, cỡ 80 x 160
            - Vector "Vector", vị trí 547,208, cỡ 21 x 21, viền #111111 (ngoài hệ thống), dày 2.0
            - Vector "Vector", vị trí 525,232, cỡ 57 x 124, viền #111111 (ngoài hệ thống), dày 2.0
          - Frame "save", vị trí 601,180, cỡ 48 x 48
            - Frame "save button", vị trí 607,186, cỡ 36 x 36, nền #FFFFFF (onPrimary, onSecondary, onTertiary, onError, surfaceContainerLowest), viền #111111 (ngoài hệ thống), dày 2.0, bo góc 999, xếp ngang, cách nhau 0, lề trên/phải/dưới/trái 0/0/0/0, căn CENTER/CENTER
              - Text "icon", vị trí 615,194, cỡ 20 x 20, chữ "bookmark", kiểu ngoài hệ thống (20/20, đậm 400), màu #111111 (ngoài hệ thống)
        - Frame "text", vị trí 464,396, cỡ 183 x 46, xếp dọc, cách nhau 2 (ngoài hệ thống), lề trên/phải/dưới/trái 0/4 (xs)/4 (xs)/4 (xs), căn MIN/MIN
          - Text "name", vị trí 468,396, cỡ 175 x 24, chữ "Bước đi tự nhiên", kiểu titleMedium (16/24, đậm 600), màu #111111 (ngoài hệ thống)
          - Text "meta", vị trí 468,422, cỡ 175 x 16, chữ "Cặp đôi · 5 bước", kiểu bodySmall (12/16, đậm 400), màu #5A5650 (ngoài hệ thống)
    - Frame "Row 2", vị trí 16,468, cỡ 641 x 284, xếp ngang, cách nhau 16 (md), lề trên/phải/dưới/trái 0/0/0/0, căn MIN/MIN
      - Instance "PoseCard", vị trí 16,468, cỡ 203 x 284, nền #FFFFFF (onPrimary, onSecondary, onTertiary, onError, surfaceContainerLowest), viền #111111 (ngoài hệ thống), dày 2.0, bo góc 16, xếp dọc, cách nhau 8 (sm), lề trên/phải/dưới/trái 8 (sm)/8 (sm)/8 (sm)/8 (sm), căn MIN/MIN
        - Frame "image", vị trí 26,478, cỡ 183 x 210, nền #4D8DFF (ngoài hệ thống), viền #111111 (ngoài hệ thống), dày 2.0, bo góc 10
          - Frame "pose-outline", vị trí 80,503, cỡ 80 x 160
            - Vector "Vector", vị trí 109,508, cỡ 21 x 21, viền #111111 (ngoài hệ thống), dày 2.0
            - Vector "Vector", vị trí 87,532, cỡ 57 x 124, viền #111111 (ngoài hệ thống), dày 2.0
          - Frame "save", vị trí 163,480, cỡ 48 x 48
            - Frame "save button", vị trí 169,486, cỡ 36 x 36, nền #FFFFFF (onPrimary, onSecondary, onTertiary, onError, surfaceContainerLowest), viền #111111 (ngoài hệ thống), dày 2.0, bo góc 999, xếp ngang, cách nhau 0, lề trên/phải/dưới/trái 0/0/0/0, căn CENTER/CENTER
              - Text "icon", vị trí 177,494, cỡ 20 x 20, chữ "bookmark", kiểu ngoài hệ thống (20/20, đậm 400), màu #111111 (ngoài hệ thống)
        - Frame "text", vị trí 26,696, cỡ 183 x 46, xếp dọc, cách nhau 2 (ngoài hệ thống), lề trên/phải/dưới/trái 0/4 (xs)/4 (xs)/4 (xs), căn MIN/MIN
          - Text "name", vị trí 30,696, cỡ 175 x 24, chữ "Ngồi bậc thềm", kiểu titleMedium (16/24, đậm 600), màu #111111 (ngoài hệ thống)
          - Text "meta", vị trí 30,722, cỡ 175 x 16, chữ "Bạn bè · 4 bước", kiểu bodySmall (12/16, đậm 400), màu #5A5650 (ngoài hệ thống)
      - Instance "PoseCard", vị trí 235,468, cỡ 203 x 284, nền #FFFFFF (onPrimary, onSecondary, onTertiary, onError, surfaceContainerLowest), viền #111111 (ngoài hệ thống), dày 2.0, bo góc 16, xếp dọc, cách nhau 8 (sm), lề trên/phải/dưới/trái 8 (sm)/8 (sm)/8 (sm)/8 (sm), căn MIN/MIN
        - Frame "image", vị trí 245,478, cỡ 183 x 210, nền #FF8A3D (ngoài hệ thống), viền #111111 (ngoài hệ thống), dày 2.0, bo góc 10
          - Frame "pose-outline", vị trí 298,503, cỡ 80 x 160
            - Vector "Vector", vị trí 328,508, cỡ 21 x 21, viền #111111 (ngoài hệ thống), dày 2.0
            - Vector "Vector", vị trí 306,532, cỡ 57 x 124, viền #111111 (ngoài hệ thống), dày 2.0
          - Frame "save", vị trí 382,480, cỡ 48 x 48
            - Frame "save button", vị trí 388,486, cỡ 36 x 36, nền #FFFFFF (onPrimary, onSecondary, onTertiary, onError, surfaceContainerLowest), viền #111111 (ngoài hệ thống), dày 2.0, bo góc 999, xếp ngang, cách nhau 0, lề trên/phải/dưới/trái 0/0/0/0, căn CENTER/CENTER
              - Text "icon", vị trí 396,494, cỡ 20 x 20, chữ "bookmark", kiểu ngoài hệ thống (20/20, đậm 400), màu #111111 (ngoài hệ thống)
        - Frame "text", vị trí 245,696, cỡ 183 x 46, xếp dọc, cách nhau 2 (ngoài hệ thống), lề trên/phải/dưới/trái 0/4 (xs)/4 (xs)/4 (xs), căn MIN/MIN
          - Text "name", vị trí 249,696, cỡ 175 x 24, chữ "Tay chống hông", kiểu titleMedium (16/24, đậm 600), màu #111111 (ngoài hệ thống)
          - Text "meta", vị trí 249,722, cỡ 175 x 16, chữ "Du lịch · 3 bước", kiểu bodySmall (12/16, đậm 400), màu #5A5650 (ngoài hệ thống)
      - Instance "PoseCard", vị trí 454,468, cỡ 203 x 284, nền #FFFFFF (onPrimary, onSecondary, onTertiary, onError, surfaceContainerLowest), viền #111111 (ngoài hệ thống), dày 2.0, bo góc 16, xếp dọc, cách nhau 8 (sm), lề trên/phải/dưới/trái 8 (sm)/8 (sm)/8 (sm)/8 (sm), căn MIN/MIN
        - Frame "image", vị trí 464,478, cỡ 183 x 210, nền #B8E986 (ngoài hệ thống), viền #111111 (ngoài hệ thống), dày 2.0, bo góc 10
          - Frame "pose-outline", vị trí 518,503, cỡ 80 x 160
            - Vector "Vector", vị trí 547,508, cỡ 21 x 21, viền #111111 (ngoài hệ thống), dày 2.0
            - Vector "Vector", vị trí 525,532, cỡ 57 x 124, viền #111111 (ngoài hệ thống), dày 2.0
          - Frame "save", vị trí 601,480, cỡ 48 x 48
            - Frame "save button", vị trí 607,486, cỡ 36 x 36, nền #FFFFFF (onPrimary, onSecondary, onTertiary, onError, surfaceContainerLowest), viền #111111 (ngoài hệ thống), dày 2.0, bo góc 999, xếp ngang, cách nhau 0, lề trên/phải/dưới/trái 0/0/0/0, căn CENTER/CENTER
              - Text "icon", vị trí 615,494, cỡ 20 x 20, chữ "bookmark", kiểu ngoài hệ thống (20/20, đậm 400), màu #111111 (ngoài hệ thống)
        - Frame "text", vị trí 464,696, cỡ 183 x 46, xếp dọc, cách nhau 2 (ngoài hệ thống), lề trên/phải/dưới/trái 0/4 (xs)/4 (xs)/4 (xs), căn MIN/MIN
          - Text "name", vị trí 468,696, cỡ 175 x 24, chữ "Cầm ly cà phê", kiểu titleMedium (16/24, đậm 600), màu #111111 (ngoài hệ thống)
          - Text "meta", vị trí 468,722, cỡ 175 x 16, chữ "Cà phê · 3 bước", kiểu bodySmall (12/16, đậm 400), màu #5A5650 (ngoài hệ thống)
    - Frame "Row 3", vị trí 16,768, cỡ 641 x 284, xếp ngang, cách nhau 16 (md), lề trên/phải/dưới/trái 0/0/0/0, căn MIN/MIN
      - Instance "PoseCard", vị trí 16,768, cỡ 203 x 284, nền #FFFFFF (onPrimary, onSecondary, onTertiary, onError, surfaceContainerLowest), viền #111111 (ngoài hệ thống), dày 2.0, bo góc 16, xếp dọc, cách nhau 8 (sm), lề trên/phải/dưới/trái 8 (sm)/8 (sm)/8 (sm)/8 (sm), căn MIN/MIN
        - Frame "image", vị trí 26,778, cỡ 183 x 210, nền #5ED3F3 (ngoài hệ thống), viền #111111 (ngoài hệ thống), dày 2.0, bo góc 10
          - Frame "pose-outline", vị trí 80,803, cỡ 80 x 160
            - Vector "Vector", vị trí 109,808, cỡ 21 x 21, viền #111111 (ngoài hệ thống), dày 2.0
            - Vector "Vector", vị trí 87,832, cỡ 57 x 124, viền #111111 (ngoài hệ thống), dày 2.0
          - Frame "save", vị trí 163,780, cỡ 48 x 48
            - Frame "save button", vị trí 169,786, cỡ 36 x 36, nền #FFFFFF (onPrimary, onSecondary, onTertiary, onError, surfaceContainerLowest), viền #111111 (ngoài hệ thống), dày 2.0, bo góc 999, xếp ngang, cách nhau 0, lề trên/phải/dưới/trái 0/0/0/0, căn CENTER/CENTER
              - Text "icon", vị trí 177,794, cỡ 20 x 20, chữ "bookmark", kiểu ngoài hệ thống (20/20, đậm 400), màu #111111 (ngoài hệ thống)
        - Frame "text", vị trí 26,996, cỡ 183 x 46, xếp dọc, cách nhau 2 (ngoài hệ thống), lề trên/phải/dưới/trái 0/4 (xs)/4 (xs)/4 (xs), căn MIN/MIN
          - Text "name", vị trí 30,996, cỡ 175 x 24, chữ "Nắm tay đi dạo", kiểu titleMedium (16/24, đậm 600), màu #111111 (ngoài hệ thống)
          - Text "meta", vị trí 30,1022, cỡ 175 x 16, chữ "Gia đình · 4 bước", kiểu bodySmall (12/16, đậm 400), màu #5A5650 (ngoài hệ thống)
      - Instance "PoseCard", vị trí 235,768, cỡ 203 x 284, nền #FFFFFF (onPrimary, onSecondary, onTertiary, onError, surfaceContainerLowest), viền #111111 (ngoài hệ thống), dày 2.0, bo góc 16, xếp dọc, cách nhau 8 (sm), lề trên/phải/dưới/trái 8 (sm)/8 (sm)/8 (sm)/8 (sm), căn MIN/MIN
        - Frame "image", vị trí 245,778, cỡ 183 x 210, nền #FFA8D0 (ngoài hệ thống), viền #111111 (ngoài hệ thống), dày 2.0, bo góc 10
          - Frame "pose-outline", vị trí 298,803, cỡ 80 x 160
            - Vector "Vector", vị trí 328,808, cỡ 21 x 21, viền #111111 (ngoài hệ thống), dày 2.0
            - Vector "Vector", vị trí 306,832, cỡ 57 x 124, viền #111111 (ngoài hệ thống), dày 2.0
          - Frame "save", vị trí 382,780, cỡ 48 x 48
            - Frame "save button", vị trí 388,786, cỡ 36 x 36, nền #FFFFFF (onPrimary, onSecondary, onTertiary, onError, surfaceContainerLowest), viền #111111 (ngoài hệ thống), dày 2.0, bo góc 999, xếp ngang, cách nhau 0, lề trên/phải/dưới/trái 0/0/0/0, căn CENTER/CENTER
              - Text "icon", vị trí 396,794, cỡ 20 x 20, chữ "bookmark", kiểu ngoài hệ thống (20/20, đậm 400), màu #111111 (ngoài hệ thống)
        - Frame "text", vị trí 245,996, cỡ 183 x 46, xếp dọc, cách nhau 2 (ngoài hệ thống), lề trên/phải/dưới/trái 0/4 (xs)/4 (xs)/4 (xs), căn MIN/MIN
          - Text "name", vị trí 249,996, cỡ 175 x 24, chữ "Ngồi xổm chống cằm", kiểu titleMedium (16/24, đậm 600), màu #111111 (ngoài hệ thống)
          - Text "meta", vị trí 249,1022, cỡ 175 x 16, chữ "Trẻ em · 3 bước", kiểu bodySmall (12/16, đậm 400), màu #5A5650 (ngoài hệ thống)
      - Instance "PoseCard", vị trí 454,768, cỡ 203 x 284, nền #FFFFFF (onPrimary, onSecondary, onTertiary, onError, surfaceContainerLowest), viền #111111 (ngoài hệ thống), dày 2.0, bo góc 16, xếp dọc, cách nhau 8 (sm), lề trên/phải/dưới/trái 8 (sm)/8 (sm)/8 (sm)/8 (sm), căn MIN/MIN
        - Frame "image", vị trí 464,778, cỡ 183 x 210, nền #4D8DFF (ngoài hệ thống), viền #111111 (ngoài hệ thống), dày 2.0, bo góc 10
          - Frame "pose-outline", vị trí 518,803, cỡ 80 x 160
            - Vector "Vector", vị trí 547,808, cỡ 21 x 21, viền #111111 (ngoài hệ thống), dày 2.0
            - Vector "Vector", vị trí 525,832, cỡ 57 x 124, viền #111111 (ngoài hệ thống), dày 2.0
          - Frame "save", vị trí 601,780, cỡ 48 x 48
            - Frame "save button", vị trí 607,786, cỡ 36 x 36, nền #FFFFFF (onPrimary, onSecondary, onTertiary, onError, surfaceContainerLowest), viền #111111 (ngoài hệ thống), dày 2.0, bo góc 999, xếp ngang, cách nhau 0, lề trên/phải/dưới/trái 0/0/0/0, căn CENTER/CENTER
              - Text "icon", vị trí 615,794, cỡ 20 x 20, chữ "bookmark", kiểu ngoài hệ thống (20/20, đậm 400), màu #111111 (ngoài hệ thống)
        - Frame "text", vị trí 464,996, cỡ 183 x 46, xếp dọc, cách nhau 2 (ngoài hệ thống), lề trên/phải/dưới/trái 0/4 (xs)/4 (xs)/4 (xs), căn MIN/MIN
          - Text "name", vị trí 468,996, cỡ 175 x 24, chữ "Ôm vai bạn", kiểu titleMedium (16/24, đậm 600), màu #111111 (ngoài hệ thống)
          - Text "meta", vị trí 468,1022, cỡ 175 x 16, chữ "Bạn bè · 3 bước", kiểu bodySmall (12/16, đậm 400), màu #5A5650 (ngoài hệ thống)

## 4. Code tham khảo

Không có. Script không sinh code; AI viết Compose theo mục 2 và 3.
