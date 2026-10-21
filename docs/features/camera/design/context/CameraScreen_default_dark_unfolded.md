# SỐ LIỆU THIẾT KẾ: CameraScreen_default_dark_unfolded

Sinh bởi `scripts/figma-export.py`. Không sửa tay; thiết kế đổi thì chạy lại script (ADR-0020).

## 1. Nguồn

- Link: https://www.figma.com/design/ZTXse6375eT3KXDUJPBTpe/?node-id=16-1187
- Frame: `16:1187`, giao diện tối
- Ngày lấy: 2026-10-21

## 2. Giá trị dùng

Đối chiếu với `docs/design-system/tokens.json` theo giá trị. "ngoài hệ thống" nghĩa là không có token
nào trùng; plan quyết định làm tròn về token gần nhất hay thêm token.

| Loại | Giá trị | Token |
| --- | --- | --- |
| khoảng cách | 16 | md |
| khoảng cách | 4 | xs |
| khoảng cách | 8 | sm |
| kiểu chữ | 14/14, đậm 400 | ngoài hệ thống |
| kiểu chữ | 14/20, đậm 400 | bodyMedium |
| kiểu chữ | 14/20, đậm 500 | titleSmall, labelLarge |
| kiểu chữ | 16/24, đậm 600 | titleMedium |
| kiểu chữ | 24/24, đậm 400 | ngoài hệ thống |
| màu chữ | #111111 | ngoài hệ thống |
| màu chữ | #FFFFFF | ngoài hệ thống |
| màu nền | #000000 | ngoài hệ thống |
| màu nền | #0A0A0B | ngoài hệ thống |
| màu nền | #1E1E1E | ngoài hệ thống |
| màu nền | #2A2A2A | ngoài hệ thống |
| màu nền | #FFC83D | ngoài hệ thống |
| màu nền | #FFFFFF | ngoài hệ thống |

## 3. Cấu trúc và số liệu

Toạ độ và cỡ tính theo dp, gốc là góc trên trái của khung. Thứ tự từ trên xuống như trong Figma.

- Frame "CameraScreen_default_dark_unfolded", vị trí 0,0, cỡ 673 x 841, nền #0A0A0B (ngoài hệ thống)
  - Frame "Top bar", vị trí 16,16, cỡ 450 x 48, xếp ngang, cách nhau 0, lề trên/phải/dưới/trái 0/0/0/0, căn SPACE_BETWEEN/CENTER
    - Instance "CameraIconButton", vị trí 16,16, cỡ 48 x 48, nền #FFFFFF (ngoài hệ thống), viền #111111 (ngoài hệ thống), dày 2.0, bo góc 999, xếp ngang, cách nhau 0, lề trên/phải/dưới/trái 0/0/0/0, căn CENTER/CENTER
      - Text "icon", vị trí 28,28, cỡ 24 x 24, chữ "grid_view", kiểu ngoài hệ thống (24/24, đậm 400), màu #111111 (ngoài hệ thống)
    - Frame "tools", vị trí 250,16, cỡ 216 x 48, xếp ngang, cách nhau 8 (sm), lề trên/phải/dưới/trái 0/0/0/0, căn MIN/MIN
      - Instance "CameraIconButton", vị trí 250,16, cỡ 48 x 48, nền #FFFFFF (ngoài hệ thống), viền #111111 (ngoài hệ thống), dày 2.0, bo góc 999, xếp ngang, cách nhau 0, lề trên/phải/dưới/trái 0/0/0/0, căn CENTER/CENTER
        - Text "icon", vị trí 262,28, cỡ 24 x 24, chữ "flash_off", kiểu ngoài hệ thống (24/24, đậm 400), màu #111111 (ngoài hệ thống)
      - Instance "CameraIconButton", vị trí 306,16, cỡ 48 x 48, nền #FFC83D (ngoài hệ thống), viền #111111 (ngoài hệ thống), dày 2.0, bo góc 999, xếp ngang, cách nhau 0, lề trên/phải/dưới/trái 0/0/0/0, căn CENTER/CENTER
        - Text "label", vị trí 320,28, cỡ 19 x 24, chữ "3s", kiểu titleMedium (16/24, đậm 600), màu #111111 (ngoài hệ thống)
      - Instance "CameraIconButton", vị trí 362,16, cỡ 48 x 48, nền #FFC83D (ngoài hệ thống), viền #111111 (ngoài hệ thống), dày 2.0, bo góc 999, xếp ngang, cách nhau 0, lề trên/phải/dưới/trái 0/0/0/0, căn CENTER/CENTER
        - Text "icon", vị trí 374,28, cỡ 24 x 24, chữ "grid_on", kiểu ngoài hệ thống (24/24, đậm 400), màu #111111 (ngoài hệ thống)
      - Instance "CameraIconButton", vị trí 418,16, cỡ 48 x 48, nền #FFFFFF (ngoài hệ thống), viền #111111 (ngoài hệ thống), dày 2.0, bo góc 999, xếp ngang, cách nhau 0, lề trên/phải/dưới/trái 0/0/0/0, căn CENTER/CENTER
        - Text "icon", vị trí 430,28, cỡ 24 x 24, chữ "settings", kiểu ngoài hệ thống (24/24, đậm 400), màu #111111 (ngoài hệ thống)
  - Frame "Preview 3:4", vị trí 16,72, cỡ 450 x 600, nền #1E1E1E (ngoài hệ thống), bo góc 16
    - Rectangle "camera-feed (ảnh tạm)", vị trí 16,72, cỡ 450 x 600, nền #1E1E1E (ngoài hệ thống)
    - Instance "GridOverlay", vị trí 16,72, cỡ 450 x 600
      - Rectangle "line v1", vị trí 166,72, cỡ 1 x 600, nền #FFFFFF (ngoài hệ thống)
      - Rectangle "line v2", vị trí 316,72, cỡ 1 x 600, nền #FFFFFF (ngoài hệ thống)
      - Rectangle "line h1", vị trí 16,272, cỡ 450 x 1, nền #FFFFFF (ngoài hệ thống)
      - Rectangle "line h2", vị trí 16,472, cỡ 450 x 1, nền #FFFFFF (ngoài hệ thống)
    - Frame "Pose overlay", vị trí 141,192, cỡ 200 x 400
      - Vector "Vector", vị trí 214,205, cỡ 53 x 53, viền #FFFFFF (ngoài hệ thống), dày 3.499999761581421
      - Vector "Vector", vị trí 161,265, cỡ 144 x 310, viền #FFFFFF (ngoài hệ thống), dày 3.499999761581421
    - Instance "StepCard", vị trí 32,88, cỡ 418 x 68, nền #FFFFFF (ngoài hệ thống), viền #111111 (ngoài hệ thống), dày 2.0, bo góc 16, xếp ngang, cách nhau 8 (sm), lề trên/phải/dưới/trái 8 (sm)/4 (xs)/8 (sm)/16 (md), căn MIN/CENTER
      - Frame "badge", vị trí 50,106, cỡ 41 x 32, nền #FFC83D (ngoài hệ thống), viền #111111 (ngoài hệ thống), dày 2.0, bo góc 999, xếp ngang, cách nhau 0, lề trên/phải/dưới/trái 4 (xs)/8 (sm)/4 (xs)/8 (sm), căn MIN/MIN
        - Text "step", vị trí 60,112, cỡ 21 x 20, chữ "2/4", kiểu titleSmall, labelLarge (14/20, đậm 500), màu #111111 (ngoài hệ thống)
      - Text "text", vị trí 99,112, cỡ 249 x 20, chữ "Xoay vai 45° về phía máy, cằm hơi hạ.", kiểu bodyMedium (14/20, đậm 400), màu #111111 (ngoài hệ thống)
      - Frame "chevron_left", vị trí 356,98, cỡ 40 x 48, xếp ngang, cách nhau 0, lề trên/phải/dưới/trái 0/0/0/0, căn CENTER/CENTER
        - Text "icon", vị trí 364,110, cỡ 24 x 24, chữ "chevron_left", kiểu ngoài hệ thống (24/24, đậm 400), màu #111111 (ngoài hệ thống)
      - Frame "chevron_right", vị trí 404,98, cỡ 40 x 48, xếp ngang, cách nhau 0, lề trên/phải/dưới/trái 0/0/0/0, căn CENTER/CENTER
        - Text "icon", vị trí 412,110, cỡ 24 x 24, chữ "chevron_right", kiểu ngoài hệ thống (24/24, đậm 400), màu #111111 (ngoài hệ thống)
    - Frame "Overlay tools", vị trí 402,222, cỡ 48 x 216, xếp dọc, cách nhau 8 (sm), lề trên/phải/dưới/trái 0/0/0/0, căn MIN/MIN
      - Instance "CameraIconButton", vị trí 402,222, cỡ 48 x 48, nền #FFFFFF (ngoài hệ thống), viền #111111 (ngoài hệ thống), dày 2.0, bo góc 999, xếp ngang, cách nhau 0, lề trên/phải/dưới/trái 0/0/0/0, căn CENTER/CENTER
        - Text "icon", vị trí 414,234, cỡ 24 x 24, chữ "tune", kiểu ngoài hệ thống (24/24, đậm 400), màu #111111 (ngoài hệ thống)
      - Instance "CameraIconButton", vị trí 402,278, cỡ 48 x 48, nền #FFFFFF (ngoài hệ thống), viền #111111 (ngoài hệ thống), dày 2.0, bo góc 999, xếp ngang, cách nhau 0, lề trên/phải/dưới/trái 0/0/0/0, căn CENTER/CENTER
        - Text "icon", vị trí 414,290, cỡ 24 x 24, chữ "flip", kiểu ngoài hệ thống (24/24, đậm 400), màu #111111 (ngoài hệ thống)
      - Instance "CameraIconButton", vị trí 402,334, cỡ 48 x 48, nền #FFFFFF (ngoài hệ thống), viền #111111 (ngoài hệ thống), dày 2.0, bo góc 999, xếp ngang, cách nhau 0, lề trên/phải/dưới/trái 0/0/0/0, căn CENTER/CENTER
        - Text "icon", vị trí 414,346, cỡ 24 x 24, chữ "lock_open", kiểu ngoài hệ thống (24/24, đậm 400), màu #111111 (ngoài hệ thống)
      - Instance "CameraIconButton", vị trí 402,390, cỡ 48 x 48, nền #FFC83D (ngoài hệ thống), viền #111111 (ngoài hệ thống), dày 2.0, bo góc 999, xếp ngang, cách nhau 0, lề trên/phải/dưới/trái 0/0/0/0, căn CENTER/CENTER
        - Text "icon", vị trí 414,402, cỡ 24 x 24, chữ "visibility", kiểu ngoài hệ thống (24/24, đậm 400), màu #111111 (ngoài hệ thống)
  - Frame "Group chips", vị trí 16,688, cỡ 581 x 36, xếp ngang, cách nhau 8 (sm), lề trên/phải/dưới/trái 0/0/0/0, căn MIN/MIN
    - Instance "Chip", vị trí 16,688, cỡ 76 x 36, nền #FFC83D (ngoài hệ thống), viền #FFFFFF (ngoài hệ thống), dày 2.0, bo góc 18, xếp ngang, cách nhau 0, lề trên/phải/dưới/trái 0/16 (md)/0/16 (md), căn MIN/CENTER
      - Text "label", vị trí 34,696, cỡ 40 x 20, chữ "Tất cả", kiểu titleSmall, labelLarge (14/20, đậm 500), màu #111111 (ngoài hệ thống)
      - Instance "SelectedBadge", vị trí 78,679, cỡ 22 x 22, nền #FFC83D (ngoài hệ thống), viền #FFFFFF (ngoài hệ thống), dày 2.0, bo góc 999, xếp ngang, cách nhau 0, lề trên/phải/dưới/trái 0/0/0/0, căn CENTER/CENTER
        - Text "icon", vị trí 82,683, cỡ 14 x 14, chữ "check", kiểu ngoài hệ thống (14/14, đậm 400), màu #111111 (ngoài hệ thống)
    - Instance "Chip", vị trí 100,688, cỡ 56 x 36, nền #1E1E1E (ngoài hệ thống), viền #FFFFFF (ngoài hệ thống), dày 2.0, bo góc 18, xếp ngang, cách nhau 0, lề trên/phải/dưới/trái 0/16 (md)/0/16 (md), căn MIN/CENTER
      - Text "label", vị trí 118,696, cỡ 20 x 20, chữ "Nữ", kiểu titleSmall, labelLarge (14/20, đậm 500), màu #FFFFFF (ngoài hệ thống)
    - Instance "Chip", vị trí 164,688, cỡ 68 x 36, nền #1E1E1E (ngoài hệ thống), viền #FFFFFF (ngoài hệ thống), dày 2.0, bo góc 18, xếp ngang, cách nhau 0, lề trên/phải/dưới/trái 0/16 (md)/0/16 (md), căn MIN/CENTER
      - Text "label", vị trí 182,696, cỡ 32 x 20, chữ "Nam", kiểu titleSmall, labelLarge (14/20, đậm 500), màu #FFFFFF (ngoài hệ thống)
    - Instance "Chip", vị trí 240,688, cỡ 87 x 36, nền #1E1E1E (ngoài hệ thống), viền #FFFFFF (ngoài hệ thống), dày 2.0, bo góc 18, xếp ngang, cách nhau 0, lề trên/phải/dưới/trái 0/16 (md)/0/16 (md), căn MIN/CENTER
      - Text "label", vị trí 258,696, cỡ 51 x 20, chữ "Cặp đôi", kiểu titleSmall, labelLarge (14/20, đậm 500), màu #FFFFFF (ngoài hệ thống)
    - Instance "Chip", vị trí 335,688, cỡ 82 x 36, nền #1E1E1E (ngoài hệ thống), viền #FFFFFF (ngoài hệ thống), dày 2.0, bo góc 18, xếp ngang, cách nhau 0, lề trên/phải/dưới/trái 0/16 (md)/0/16 (md), căn MIN/CENTER
      - Text "label", vị trí 353,696, cỡ 46 x 20, chữ "Bạn bè", kiểu titleSmall, labelLarge (14/20, đậm 500), màu #FFFFFF (ngoài hệ thống)
    - Instance "Chip", vị trí 425,688, cỡ 82 x 36, nền #1E1E1E (ngoài hệ thống), viền #FFFFFF (ngoài hệ thống), dày 2.0, bo góc 18, xếp ngang, cách nhau 0, lề trên/phải/dưới/trái 0/16 (md)/0/16 (md), căn MIN/CENTER
      - Text "label", vị trí 443,696, cỡ 46 x 20, chữ "Du lịch", kiểu titleSmall, labelLarge (14/20, đậm 500), màu #FFFFFF (ngoài hệ thống)
    - Instance "Chip", vị trí 515,688, cỡ 82 x 36, nền #1E1E1E (ngoài hệ thống), viền #FFFFFF (ngoài hệ thống), dày 2.0, bo góc 18, xếp ngang, cách nhau 0, lề trên/phải/dưới/trái 0/16 (md)/0/16 (md), căn MIN/CENTER
      - Text "label", vị trí 533,696, cỡ 46 x 20, chữ "Cà phê", kiểu titleSmall, labelLarge (14/20, đậm 500), màu #FFFFFF (ngoài hệ thống)
  - Frame "Pose strip", vị trí 16,740, cỡ 568 x 64, xếp ngang, cách nhau 8 (sm), lề trên/phải/dưới/trái 0/0/0/0, căn MIN/MIN
    - Instance "PoseThumb", vị trí 16,740, cỡ 64 x 64
      - Frame "image", vị trí 16,740, cỡ 64 x 64, nền #2A2A2A (ngoài hệ thống), viền #FFFFFF (ngoài hệ thống), dày 2.0, bo góc 14
        - Frame "pose-outline", vị trí 35,746, cỡ 26 x 52
          - Vector "Vector", vị trí 45,748, cỡ 7 x 7, viền #B5B0A8 (ngoài hệ thống), dày 1.499333381652832
          - Vector "Vector", vị trí 38,756, cỡ 19 x 40, viền #B5B0A8 (ngoài hệ thống), dày 1.499333381652832
    - Instance "PoseThumb", vị trí 88,740, cỡ 64 x 64
      - Frame "image", vị trí 88,740, cỡ 64 x 64, nền #2A2A2A (ngoài hệ thống), viền #FFC83D (ngoài hệ thống), dày 3.0, bo góc 14
        - Frame "pose-outline", vị trí 107,746, cỡ 26 x 52
          - Vector "Vector", vị trí 117,748, cỡ 7 x 7, viền #FFFFFF (ngoài hệ thống), dày 1.499333381652832
          - Vector "Vector", vị trí 110,756, cỡ 19 x 40, viền #FFFFFF (ngoài hệ thống), dày 1.499333381652832
      - Instance "SelectedBadge", vị trí 137,733, cỡ 22 x 22, nền #FFC83D (ngoài hệ thống), viền #FFFFFF (ngoài hệ thống), dày 2.0, bo góc 999, xếp ngang, cách nhau 0, lề trên/phải/dưới/trái 0/0/0/0, căn CENTER/CENTER
        - Text "icon", vị trí 141,737, cỡ 14 x 14, chữ "check", kiểu ngoài hệ thống (14/14, đậm 400), màu #111111 (ngoài hệ thống)
    - Instance "PoseThumb", vị trí 160,740, cỡ 64 x 64
      - Frame "image", vị trí 160,740, cỡ 64 x 64, nền #2A2A2A (ngoài hệ thống), viền #FFFFFF (ngoài hệ thống), dày 2.0, bo góc 14
        - Frame "pose-outline", vị trí 179,746, cỡ 26 x 52
          - Vector "Vector", vị trí 189,748, cỡ 7 x 7, viền #B5B0A8 (ngoài hệ thống), dày 1.499333381652832
          - Vector "Vector", vị trí 182,756, cỡ 19 x 40, viền #B5B0A8 (ngoài hệ thống), dày 1.499333381652832
    - Instance "PoseThumb", vị trí 232,740, cỡ 64 x 64
      - Frame "image", vị trí 232,740, cỡ 64 x 64, nền #2A2A2A (ngoài hệ thống), viền #FFFFFF (ngoài hệ thống), dày 2.0, bo góc 14
        - Frame "pose-outline", vị trí 251,746, cỡ 26 x 52
          - Vector "Vector", vị trí 261,748, cỡ 7 x 7, viền #B5B0A8 (ngoài hệ thống), dày 1.499333381652832
          - Vector "Vector", vị trí 254,756, cỡ 19 x 40, viền #B5B0A8 (ngoài hệ thống), dày 1.499333381652832
    - Instance "PoseThumb", vị trí 304,740, cỡ 64 x 64
      - Frame "image", vị trí 304,740, cỡ 64 x 64, nền #2A2A2A (ngoài hệ thống), viền #FFFFFF (ngoài hệ thống), dày 2.0, bo góc 14
        - Frame "pose-outline", vị trí 323,746, cỡ 26 x 52
          - Vector "Vector", vị trí 333,748, cỡ 7 x 7, viền #B5B0A8 (ngoài hệ thống), dày 1.499333381652832
          - Vector "Vector", vị trí 326,756, cỡ 19 x 40, viền #B5B0A8 (ngoài hệ thống), dày 1.499333381652832
    - Instance "PoseThumb", vị trí 376,740, cỡ 64 x 64
      - Frame "image", vị trí 376,740, cỡ 64 x 64, nền #2A2A2A (ngoài hệ thống), viền #FFFFFF (ngoài hệ thống), dày 2.0, bo góc 14
        - Frame "pose-outline", vị trí 395,746, cỡ 26 x 52
          - Vector "Vector", vị trí 405,748, cỡ 7 x 7, viền #B5B0A8 (ngoài hệ thống), dày 1.499333381652832
          - Vector "Vector", vị trí 398,756, cỡ 19 x 40, viền #B5B0A8 (ngoài hệ thống), dày 1.499333381652832
    - Instance "PoseThumb", vị trí 448,740, cỡ 64 x 64
      - Frame "image", vị trí 448,740, cỡ 64 x 64, nền #2A2A2A (ngoài hệ thống), viền #FFFFFF (ngoài hệ thống), dày 2.0, bo góc 14
        - Frame "pose-outline", vị trí 467,746, cỡ 26 x 52
          - Vector "Vector", vị trí 477,748, cỡ 7 x 7, viền #B5B0A8 (ngoài hệ thống), dày 1.499333381652832
          - Vector "Vector", vị trí 470,756, cỡ 19 x 40, viền #B5B0A8 (ngoài hệ thống), dày 1.499333381652832
    - Instance "PoseThumb", vị trí 520,740, cỡ 64 x 64
      - Frame "image", vị trí 520,740, cỡ 64 x 64, nền #2A2A2A (ngoài hệ thống), viền #FFFFFF (ngoài hệ thống), dày 2.0, bo góc 14
        - Frame "pose-outline", vị trí 539,746, cỡ 26 x 52
          - Vector "Vector", vị trí 549,748, cỡ 7 x 7, viền #B5B0A8 (ngoài hệ thống), dày 1.499333381652832
          - Vector "Vector", vị trí 542,756, cỡ 19 x 40, viền #B5B0A8 (ngoài hệ thống), dày 1.499333381652832
  - Rectangle "Last photo (ảnh tạm)", vị trí 554,500, cỡ 48 x 48, nền #1E1E1E (ngoài hệ thống), viền #FFFFFF (ngoài hệ thống), dày 2.0, bo góc 14
  - Ellipse "Shutter ring", vị trí 540,334, cỡ 76 x 76, viền #FFFFFF (ngoài hệ thống), dày 4.0
  - Ellipse "Shutter", vị trí 546,341, cỡ 62 x 62, nền #FFFFFF (ngoài hệ thống)
  - Instance "CameraIconButton", vị trí 554,226, cỡ 48 x 48, nền #FFFFFF (ngoài hệ thống), viền #111111 (ngoài hệ thống), dày 2.0, bo góc 999, xếp ngang, cách nhau 0, lề trên/phải/dưới/trái 0/0/0/0, căn CENTER/CENTER
    - Text "icon", vị trí 566,238, cỡ 24 x 24, chữ "flip_camera_android", kiểu ngoài hệ thống (24/24, đậm 400), màu #111111 (ngoài hệ thống)
  - Instance "ZoomControl", vị trí 510,620, cỡ 136 x 48, nền #000000 (ngoài hệ thống), bo góc 999, xếp ngang, cách nhau 4 (xs), lề trên/phải/dưới/trái 4 (xs)/4 (xs)/4 (xs)/4 (xs), căn MIN/CENTER
    - Frame "zoom 0.5", vị trí 514,624, cỡ 40 x 40, bo góc 999, xếp ngang, cách nhau 0, lề trên/phải/dưới/trái 0/0/0/0, căn CENTER/CENTER
      - Text "0.5", vị trí 523,634, cỡ 21 x 20, chữ "0.5", kiểu titleSmall, labelLarge (14/20, đậm 500), màu #FFFFFF (ngoài hệ thống)
    - Frame "zoom 1x", vị trí 558,624, cỡ 40 x 40, nền #FFC83D (ngoài hệ thống), viền #111111 (ngoài hệ thống), dày 2.0, bo góc 999, xếp ngang, cách nhau 0, lề trên/phải/dưới/trái 0/0/0/0, căn CENTER/CENTER
      - Text "1x", vị trí 572,634, cỡ 12 x 20, chữ "1x", kiểu titleSmall, labelLarge (14/20, đậm 500), màu #111111 (ngoài hệ thống)
    - Frame "zoom 2", vị trí 602,624, cỡ 40 x 40, bo góc 999, xếp ngang, cách nhau 0, lề trên/phải/dưới/trái 0/0/0/0, căn CENTER/CENTER
      - Text "2", vị trí 617,634, cỡ 9 x 20, chữ "2", kiểu titleSmall, labelLarge (14/20, đậm 500), màu #FFFFFF (ngoài hệ thống)

## 4. Code tham khảo

Không có. Script không sinh code; AI viết Compose theo mục 2 và 3.
