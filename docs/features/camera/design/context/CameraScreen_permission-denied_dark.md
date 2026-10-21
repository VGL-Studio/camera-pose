# SỐ LIỆU THIẾT KẾ: CameraScreen_permission-denied_dark

Sinh bởi `scripts/figma-export.py`. Không sửa tay; thiết kế đổi thì chạy lại script (ADR-0020).

## 1. Nguồn

- Link: https://www.figma.com/design/ZTXse6375eT3KXDUJPBTpe/?node-id=16-1027
- Frame: `16:1027`, giao diện tối
- Ngày lấy: 2026-10-21

## 2. Giá trị dùng

Đối chiếu với `docs/design-system/tokens.json` theo giá trị. "ngoài hệ thống" nghĩa là không có token
nào trùng; plan quyết định làm tròn về token gần nhất hay thêm token.

| Loại | Giá trị | Token |
| --- | --- | --- |
| khoảng cách | 16 | md |
| khoảng cách | 24 | lg |
| khoảng cách | 8 | sm |
| kiểu chữ | 14/14, đậm 400 | ngoài hệ thống |
| kiểu chữ | 14/20, đậm 400 | bodyMedium |
| kiểu chữ | 14/20, đậm 500 | titleSmall, labelLarge |
| kiểu chữ | 16/24, đậm 600 | titleMedium |
| kiểu chữ | 20/20, đậm 400 | ngoài hệ thống |
| kiểu chữ | 22/28, đậm 800 | ngoài hệ thống |
| kiểu chữ | 24/24, đậm 400 | ngoài hệ thống |
| kiểu chữ | 36/36, đậm 400 | ngoài hệ thống |
| màu chữ | #111111 | ngoài hệ thống |
| màu chữ | #B5B0A8 | ngoài hệ thống |
| màu chữ | #FFFFFF | ngoài hệ thống |
| màu nền | #0A0A0B | ngoài hệ thống |
| màu nền | #1E1E1E | ngoài hệ thống |
| màu nền | #2A2A2A | ngoài hệ thống |
| màu nền | #FFC83D | ngoài hệ thống |
| màu nền | #FFFFFF | ngoài hệ thống |

## 3. Cấu trúc và số liệu

Toạ độ và cỡ tính theo dp, gốc là góc trên trái của khung. Thứ tự từ trên xuống như trong Figma.

- Frame "CameraScreen_permission-denied_dark", vị trí 0,0, cỡ 411 x 891, nền #0A0A0B (ngoài hệ thống)
  - Frame "Top bar", vị trí 16,32, cỡ 379 x 48, xếp ngang, cách nhau 0, lề trên/phải/dưới/trái 0/0/0/0, căn SPACE_BETWEEN/CENTER
    - Instance "CameraIconButton", vị trí 16,32, cỡ 48 x 48, nền #FFFFFF (ngoài hệ thống), viền #111111 (ngoài hệ thống), dày 2.0, bo góc 999, xếp ngang, cách nhau 0, lề trên/phải/dưới/trái 0/0/0/0, căn CENTER/CENTER
      - Text "icon", vị trí 28,44, cỡ 24 x 24, chữ "grid_view", kiểu ngoài hệ thống (24/24, đậm 400), màu #111111 (ngoài hệ thống)
    - Frame "tools", vị trí 347,32, cỡ 48 x 48, xếp ngang, cách nhau 8 (sm), lề trên/phải/dưới/trái 0/0/0/0, căn MIN/MIN
      - Instance "CameraIconButton", vị trí 347,32, cỡ 48 x 48, nền #FFFFFF (ngoài hệ thống), viền #111111 (ngoài hệ thống), dày 2.0, bo góc 999, xếp ngang, cách nhau 0, lề trên/phải/dưới/trái 0/0/0/0, căn CENTER/CENTER
        - Text "icon", vị trí 359,44, cỡ 24 x 24, chữ "settings", kiểu ngoài hệ thống (24/24, đậm 400), màu #111111 (ngoài hệ thống)
  - Frame "Preview 3:4", vị trí 0,92, cỡ 411 x 548, nền #0A0A0B (ngoài hệ thống)
    - Rectangle "camera-feed (ảnh tạm)", vị trí 0,92, cỡ 411 x 548, nền #0A0A0B (ngoài hệ thống)
    - Instance "StateMessage", vị trí 40,218, cỡ 331 x 296, xếp dọc, cách nhau 16 (md), lề trên/phải/dưới/trái 24 (lg)/0/24 (lg)/0, căn MIN/CENTER
      - Frame "icon circle", vị trí 170,242, cỡ 72 x 72, nền #FFC83D (ngoài hệ thống), viền #FFFFFF (ngoài hệ thống), dày 2.0, bo góc 999, xếp ngang, cách nhau 0, lề trên/phải/dưới/trái 0/0/0/0, căn CENTER/CENTER
        - Text "icon", vị trí 188,260, cỡ 36 x 36, chữ "no_photography", kiểu ngoài hệ thống (36/36, đậm 400), màu #111111 (ngoài hệ thống)
      - Text "title", vị trí 40,330, cỡ 331 x 28, chữ "Chưa có quyền dùng camera", kiểu ngoài hệ thống (22/28, đậm 800), màu #FFFFFF (ngoài hệ thống)
      - Text "body", vị trí 40,374, cỡ 331 x 40, chữ "Cho phép camera trong Cài đặt máy để chụp theo tư thế.", kiểu bodyMedium (14/20, đậm 400), màu #B5B0A8 (ngoài hệ thống)
      - Instance "button", vị trí 125,430, cỡ 161 x 60, nền #FFFFFF (ngoài hệ thống), viền #FFFFFF (ngoài hệ thống), dày 2.0, bo góc 14, xếp ngang, cách nhau 8 (sm), lề trên/phải/dưới/trái 16 (md)/24 (lg)/16 (md)/24 (lg), căn CENTER/CENTER
        - Text "icon", vị trí 151,450, cỡ 20 x 20, chữ "settings", kiểu ngoài hệ thống (20/20, đậm 400), màu #111111 (ngoài hệ thống)
        - Text "label", vị trí 179,448, cỡ 81 x 24, chữ "Mở cài đặt", kiểu titleMedium (16/24, đậm 600), màu #111111 (ngoài hệ thống)
  - Frame "Group chips", vị trí 16,656, cỡ 581 x 36, xếp ngang, cách nhau 8 (sm), lề trên/phải/dưới/trái 0/0/0/0, căn MIN/MIN
    - Instance "Chip", vị trí 16,656, cỡ 76 x 36, nền #FFC83D (ngoài hệ thống), viền #FFFFFF (ngoài hệ thống), dày 2.0, bo góc 18, xếp ngang, cách nhau 0, lề trên/phải/dưới/trái 0/16 (md)/0/16 (md), căn MIN/CENTER
      - Text "label", vị trí 34,664, cỡ 40 x 20, chữ "Tất cả", kiểu titleSmall, labelLarge (14/20, đậm 500), màu #111111 (ngoài hệ thống)
      - Instance "SelectedBadge", vị trí 78,647, cỡ 22 x 22, nền #FFC83D (ngoài hệ thống), viền #FFFFFF (ngoài hệ thống), dày 2.0, bo góc 999, xếp ngang, cách nhau 0, lề trên/phải/dưới/trái 0/0/0/0, căn CENTER/CENTER
        - Text "icon", vị trí 82,651, cỡ 14 x 14, chữ "check", kiểu ngoài hệ thống (14/14, đậm 400), màu #111111 (ngoài hệ thống)
    - Instance "Chip", vị trí 100,656, cỡ 56 x 36, nền #1E1E1E (ngoài hệ thống), viền #FFFFFF (ngoài hệ thống), dày 2.0, bo góc 18, xếp ngang, cách nhau 0, lề trên/phải/dưới/trái 0/16 (md)/0/16 (md), căn MIN/CENTER
      - Text "label", vị trí 118,664, cỡ 20 x 20, chữ "Nữ", kiểu titleSmall, labelLarge (14/20, đậm 500), màu #FFFFFF (ngoài hệ thống)
    - Instance "Chip", vị trí 164,656, cỡ 68 x 36, nền #1E1E1E (ngoài hệ thống), viền #FFFFFF (ngoài hệ thống), dày 2.0, bo góc 18, xếp ngang, cách nhau 0, lề trên/phải/dưới/trái 0/16 (md)/0/16 (md), căn MIN/CENTER
      - Text "label", vị trí 182,664, cỡ 32 x 20, chữ "Nam", kiểu titleSmall, labelLarge (14/20, đậm 500), màu #FFFFFF (ngoài hệ thống)
    - Instance "Chip", vị trí 240,656, cỡ 87 x 36, nền #1E1E1E (ngoài hệ thống), viền #FFFFFF (ngoài hệ thống), dày 2.0, bo góc 18, xếp ngang, cách nhau 0, lề trên/phải/dưới/trái 0/16 (md)/0/16 (md), căn MIN/CENTER
      - Text "label", vị trí 258,664, cỡ 51 x 20, chữ "Cặp đôi", kiểu titleSmall, labelLarge (14/20, đậm 500), màu #FFFFFF (ngoài hệ thống)
    - Instance "Chip", vị trí 335,656, cỡ 82 x 36, nền #1E1E1E (ngoài hệ thống), viền #FFFFFF (ngoài hệ thống), dày 2.0, bo góc 18, xếp ngang, cách nhau 0, lề trên/phải/dưới/trái 0/16 (md)/0/16 (md), căn MIN/CENTER
      - Text "label", vị trí 353,664, cỡ 46 x 20, chữ "Bạn bè", kiểu titleSmall, labelLarge (14/20, đậm 500), màu #FFFFFF (ngoài hệ thống)
    - Instance "Chip", vị trí 425,656, cỡ 82 x 36, nền #1E1E1E (ngoài hệ thống), viền #FFFFFF (ngoài hệ thống), dày 2.0, bo góc 18, xếp ngang, cách nhau 0, lề trên/phải/dưới/trái 0/16 (md)/0/16 (md), căn MIN/CENTER
      - Text "label", vị trí 443,664, cỡ 46 x 20, chữ "Du lịch", kiểu titleSmall, labelLarge (14/20, đậm 500), màu #FFFFFF (ngoài hệ thống)
    - Instance "Chip", vị trí 515,656, cỡ 82 x 36, nền #1E1E1E (ngoài hệ thống), viền #FFFFFF (ngoài hệ thống), dày 2.0, bo góc 18, xếp ngang, cách nhau 0, lề trên/phải/dưới/trái 0/16 (md)/0/16 (md), căn MIN/CENTER
      - Text "label", vị trí 533,664, cỡ 46 x 20, chữ "Cà phê", kiểu titleSmall, labelLarge (14/20, đậm 500), màu #FFFFFF (ngoài hệ thống)
  - Frame "Pose strip", vị trí 16,708, cỡ 568 x 64, xếp ngang, cách nhau 8 (sm), lề trên/phải/dưới/trái 0/0/0/0, căn MIN/MIN
    - Instance "PoseThumb", vị trí 16,708, cỡ 64 x 64
      - Frame "image", vị trí 16,708, cỡ 64 x 64, nền #2A2A2A (ngoài hệ thống), viền #FFFFFF (ngoài hệ thống), dày 2.0, bo góc 14
        - Frame "pose-outline", vị trí 35,714, cỡ 26 x 52
          - Vector "Vector", vị trí 45,716, cỡ 7 x 7, viền #B5B0A8 (ngoài hệ thống), dày 1.499333381652832
          - Vector "Vector", vị trí 38,724, cỡ 19 x 40, viền #B5B0A8 (ngoài hệ thống), dày 1.499333381652832
    - Instance "PoseThumb", vị trí 88,708, cỡ 64 x 64
      - Frame "image", vị trí 88,708, cỡ 64 x 64, nền #2A2A2A (ngoài hệ thống), viền #FFFFFF (ngoài hệ thống), dày 2.0, bo góc 14
        - Frame "pose-outline", vị trí 107,714, cỡ 26 x 52
          - Vector "Vector", vị trí 117,716, cỡ 7 x 7, viền #B5B0A8 (ngoài hệ thống), dày 1.499333381652832
          - Vector "Vector", vị trí 110,724, cỡ 19 x 40, viền #B5B0A8 (ngoài hệ thống), dày 1.499333381652832
    - Instance "PoseThumb", vị trí 160,708, cỡ 64 x 64
      - Frame "image", vị trí 160,708, cỡ 64 x 64, nền #2A2A2A (ngoài hệ thống), viền #FFFFFF (ngoài hệ thống), dày 2.0, bo góc 14
        - Frame "pose-outline", vị trí 179,714, cỡ 26 x 52
          - Vector "Vector", vị trí 189,716, cỡ 7 x 7, viền #B5B0A8 (ngoài hệ thống), dày 1.499333381652832
          - Vector "Vector", vị trí 182,724, cỡ 19 x 40, viền #B5B0A8 (ngoài hệ thống), dày 1.499333381652832
    - Instance "PoseThumb", vị trí 232,708, cỡ 64 x 64
      - Frame "image", vị trí 232,708, cỡ 64 x 64, nền #2A2A2A (ngoài hệ thống), viền #FFFFFF (ngoài hệ thống), dày 2.0, bo góc 14
        - Frame "pose-outline", vị trí 251,714, cỡ 26 x 52
          - Vector "Vector", vị trí 261,716, cỡ 7 x 7, viền #B5B0A8 (ngoài hệ thống), dày 1.499333381652832
          - Vector "Vector", vị trí 254,724, cỡ 19 x 40, viền #B5B0A8 (ngoài hệ thống), dày 1.499333381652832
    - Instance "PoseThumb", vị trí 304,708, cỡ 64 x 64
      - Frame "image", vị trí 304,708, cỡ 64 x 64, nền #2A2A2A (ngoài hệ thống), viền #FFFFFF (ngoài hệ thống), dày 2.0, bo góc 14
        - Frame "pose-outline", vị trí 323,714, cỡ 26 x 52
          - Vector "Vector", vị trí 333,716, cỡ 7 x 7, viền #B5B0A8 (ngoài hệ thống), dày 1.499333381652832
          - Vector "Vector", vị trí 326,724, cỡ 19 x 40, viền #B5B0A8 (ngoài hệ thống), dày 1.499333381652832
    - Instance "PoseThumb", vị trí 376,708, cỡ 64 x 64
      - Frame "image", vị trí 376,708, cỡ 64 x 64, nền #2A2A2A (ngoài hệ thống), viền #FFFFFF (ngoài hệ thống), dày 2.0, bo góc 14
        - Frame "pose-outline", vị trí 395,714, cỡ 26 x 52
          - Vector "Vector", vị trí 405,716, cỡ 7 x 7, viền #B5B0A8 (ngoài hệ thống), dày 1.499333381652832
          - Vector "Vector", vị trí 398,724, cỡ 19 x 40, viền #B5B0A8 (ngoài hệ thống), dày 1.499333381652832
    - Instance "PoseThumb", vị trí 448,708, cỡ 64 x 64
      - Frame "image", vị trí 448,708, cỡ 64 x 64, nền #2A2A2A (ngoài hệ thống), viền #FFFFFF (ngoài hệ thống), dày 2.0, bo góc 14
        - Frame "pose-outline", vị trí 467,714, cỡ 26 x 52
          - Vector "Vector", vị trí 477,716, cỡ 7 x 7, viền #B5B0A8 (ngoài hệ thống), dày 1.499333381652832
          - Vector "Vector", vị trí 470,724, cỡ 19 x 40, viền #B5B0A8 (ngoài hệ thống), dày 1.499333381652832
    - Instance "PoseThumb", vị trí 520,708, cỡ 64 x 64
      - Frame "image", vị trí 520,708, cỡ 64 x 64, nền #2A2A2A (ngoài hệ thống), viền #FFFFFF (ngoài hệ thống), dày 2.0, bo góc 14
        - Frame "pose-outline", vị trí 539,714, cỡ 26 x 52
          - Vector "Vector", vị trí 549,716, cỡ 7 x 7, viền #B5B0A8 (ngoài hệ thống), dày 1.499333381652832
          - Vector "Vector", vị trí 542,724, cỡ 19 x 40, viền #B5B0A8 (ngoài hệ thống), dày 1.499333381652832
  - Rectangle "Last photo (ảnh tạm)", vị trí 44,798, cỡ 48 x 48, nền #1E1E1E (ngoài hệ thống), viền #FFFFFF (ngoài hệ thống), dày 2.0, bo góc 14
  - Ellipse "Shutter ring", vị trí 168,784, cỡ 76 x 76, viền #FFFFFF (ngoài hệ thống), dày 4.0
  - Ellipse "Shutter", vị trí 174,791, cỡ 62 x 62, nền #FFFFFF (ngoài hệ thống)
  - Instance "CameraIconButton", vị trí 319,798, cỡ 48 x 48, nền #FFFFFF (ngoài hệ thống), viền #111111 (ngoài hệ thống), dày 2.0, bo góc 999, xếp ngang, cách nhau 0, lề trên/phải/dưới/trái 0/0/0/0, căn CENTER/CENTER
    - Text "icon", vị trí 331,810, cỡ 24 x 24, chữ "flip_camera_android", kiểu ngoài hệ thống (24/24, đậm 400), màu #111111 (ngoài hệ thống)

## 4. Code tham khảo

Không có. Script không sinh code; AI viết Compose theo mục 2 và 3.
