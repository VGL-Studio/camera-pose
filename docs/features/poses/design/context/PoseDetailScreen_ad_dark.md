# SỐ LIỆU THIẾT KẾ: PoseDetailScreen_ad_dark

Sinh bởi `scripts/figma-export.py`. Không sửa tay; thiết kế đổi thì chạy lại script (ADR-0020).

## 1. Nguồn

- Link: https://www.figma.com/design/ZTXse6375eT3KXDUJPBTpe/?node-id=28-2215
- Frame: `28:2215`, giao diện tối
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
| kiểu chữ | 11/16, đậm 500 | labelSmall |
| kiểu chữ | 12/16, đậm 400 | bodySmall |
| kiểu chữ | 14/20, đậm 400 | bodyMedium |
| kiểu chữ | 14/20, đậm 500 | titleSmall, labelLarge |
| kiểu chữ | 16/24, đậm 600 | titleMedium |
| kiểu chữ | 20/20, đậm 400 | ngoài hệ thống |
| kiểu chữ | 22/28, đậm 800 | ngoài hệ thống |
| kiểu chữ | 24/24, đậm 400 | ngoài hệ thống |
| màu chữ | #111111 | ngoài hệ thống |
| màu chữ | #B5B0A8 | ngoài hệ thống |
| màu chữ | #FFD6E2 | ngoài hệ thống |
| màu chữ | #FFFFFF | ngoài hệ thống |
| màu nền | #121212 | ngoài hệ thống |
| màu nền | #262626 | ngoài hệ thống |
| màu nền | #4A4744 | ngoài hệ thống |
| màu nền | #6B1D35 | ngoài hệ thống |
| màu nền | #FF5C8A | ngoài hệ thống |
| màu nền | #FFC83D | ngoài hệ thống |
| màu nền | #FFFFFF | ngoài hệ thống |

## 3. Cấu trúc và số liệu

Toạ độ và cỡ tính theo dp, gốc là góc trên trái của khung. Thứ tự từ trên xuống như trong Figma.

- Frame "PoseDetailScreen_ad_dark", vị trí 0,0, cỡ 411 x 891, nền #121212 (ngoài hệ thống)
  - Instance "TopAppBar", vị trí 0,24, cỡ 411 x 64, xếp ngang, cách nhau 4 (xs), lề trên/phải/dưới/trái 0/4 (xs)/0/4 (xs), căn MIN/CENTER
    - Instance "leading", vị trí 4,32, cỡ 48 x 48, bo góc 999, xếp ngang, cách nhau 0, lề trên/phải/dưới/trái 0/0/0/0, căn CENTER/CENTER
      - Text "icon", vị trí 16,44, cỡ 24 x 24, chữ "arrow_back", kiểu ngoài hệ thống (24/24, đậm 400), màu #FFFFFF (ngoài hệ thống)
    - Text "title", vị trí 56,42, cỡ 299 x 28, chữ "Tựa lan can", kiểu ngoài hệ thống (22/28, đậm 800), màu #FFFFFF (ngoài hệ thống)
    - Instance "trailing", vị trí 359,32, cỡ 48 x 48, bo góc 999, xếp ngang, cách nhau 0, lề trên/phải/dưới/trái 0/0/0/0, căn CENTER/CENTER
      - Text "icon", vị trí 371,44, cỡ 24 x 24, chữ "bookmark", kiểu ngoài hệ thống (24/24, đậm 400), màu #FFFFFF (ngoài hệ thống)
  - Frame "content", vị trí 0,88, cỡ 411 x 722, xếp dọc, cách nhau 16 (md), lề trên/phải/dưới/trái 0/16 (md)/0/16 (md), căn MIN/MIN
    - Frame "image", vị trí 16,88, cỡ 379 x 380, nền #FF5C8A (ngoài hệ thống), viền #FFFFFF (ngoài hệ thống), dày 2.0, bo góc 16
      - Frame "pose-outline", vị trí 130,128, cỡ 150 x 300
        - Vector "Vector", vị trí 186,138, cỡ 40 x 40, viền #111111 (ngoài hệ thống), dày 3.0
        - Vector "Vector", vị trí 145,183, cỡ 108 x 232, viền #111111 (ngoài hệ thống), dày 3.0
    - Text "meta", vị trí 16,484, cỡ 379 x 20, chữ "Một người · Nữ · Toàn thân · 4 bước", kiểu bodyMedium (14/20, đậm 400), màu #B5B0A8 (ngoài hệ thống)
    - Text "Các bước", vị trí 16,520, cỡ 379 x 24, chữ "Các bước", kiểu titleMedium (16/24, đậm 600), màu #FFFFFF (ngoài hệ thống)
    - Frame "steps", vị trí 16,560, cỡ 379 x 136, xếp dọc, cách nhau 8 (sm), lề trên/phải/dưới/trái 0/0/0/0, căn MIN/MIN
      - Frame "step 1", vị trí 16,560, cỡ 379 x 28, xếp ngang, cách nhau 8 (sm), lề trên/phải/dưới/trái 0/0/0/0, căn MIN/MIN
        - Frame "number", vị trí 16,560, cỡ 28 x 28, nền #FFC83D (ngoài hệ thống), viền #FFFFFF (ngoài hệ thống), dày 2.0, bo góc 999, xếp ngang, cách nhau 0, lề trên/phải/dưới/trái 0/0/0/0, căn CENTER/CENTER
          - Text "1", vị trí 28,564, cỡ 5 x 20, chữ "1", kiểu titleSmall, labelLarge (14/20, đậm 500), màu #111111 (ngoài hệ thống)
        - Text "Đứng chéo 45° so với máy, chân", vị trí 52,560, cỡ 343 x 20, chữ "Đứng chéo 45° so với máy, chân trước hơi khuỵu.", kiểu bodyMedium (14/20, đậm 400), màu #FFFFFF (ngoài hệ thống)
      - Frame "step 2", vị trí 16,596, cỡ 379 x 28, xếp ngang, cách nhau 8 (sm), lề trên/phải/dưới/trái 0/0/0/0, căn MIN/MIN
        - Frame "number", vị trí 16,596, cỡ 28 x 28, nền #FFC83D (ngoài hệ thống), viền #FFFFFF (ngoài hệ thống), dày 2.0, bo góc 999, xếp ngang, cách nhau 0, lề trên/phải/dưới/trái 0/0/0/0, căn CENTER/CENTER
          - Text "2", vị trí 26,600, cỡ 9 x 20, chữ "2", kiểu titleSmall, labelLarge (14/20, đậm 500), màu #111111 (ngoài hệ thống)
        - Text "Tay trái chống hông, khuỷu tay", vị trí 52,596, cỡ 343 x 20, chữ "Tay trái chống hông, khuỷu tay mở ra sau.", kiểu bodyMedium (14/20, đậm 400), màu #FFFFFF (ngoài hệ thống)
      - Frame "step 3", vị trí 16,632, cỡ 379 x 28, xếp ngang, cách nhau 8 (sm), lề trên/phải/dưới/trái 0/0/0/0, căn MIN/MIN
        - Frame "number", vị trí 16,632, cỡ 28 x 28, nền #FFC83D (ngoài hệ thống), viền #FFFFFF (ngoài hệ thống), dày 2.0, bo góc 999, xếp ngang, cách nhau 0, lề trên/phải/dưới/trái 0/0/0/0, căn CENTER/CENTER
          - Text "3", vị trí 26,636, cỡ 9 x 20, chữ "3", kiểu titleSmall, labelLarge (14/20, đậm 500), màu #111111 (ngoài hệ thống)
        - Text "Tay phải thả lỏng dọc thân.", vị trí 52,632, cỡ 343 x 20, chữ "Tay phải thả lỏng dọc thân.", kiểu bodyMedium (14/20, đậm 400), màu #FFFFFF (ngoài hệ thống)
      - Frame "step 4", vị trí 16,668, cỡ 379 x 28, xếp ngang, cách nhau 8 (sm), lề trên/phải/dưới/trái 0/0/0/0, căn MIN/MIN
        - Frame "number", vị trí 16,668, cỡ 28 x 28, nền #FFC83D (ngoài hệ thống), viền #FFFFFF (ngoài hệ thống), dày 2.0, bo góc 999, xếp ngang, cách nhau 0, lề trên/phải/dưới/trái 0/0/0/0, căn CENTER/CENTER
          - Text "4", vị trí 26,672, cỡ 9 x 20, chữ "4", kiểu titleSmall, labelLarge (14/20, đậm 500), màu #111111 (ngoài hệ thống)
        - Text "Xoay vai về phía máy, cằm hơi ", vị trí 52,668, cỡ 343 x 20, chữ "Xoay vai về phía máy, cằm hơi hạ.", kiểu bodyMedium (14/20, đậm 400), màu #FFFFFF (ngoài hệ thống)
    - Frame "Common mistake", vị trí 16,712, cỡ 379 x 98, nền #6B1D35 (ngoài hệ thống), viền #FFFFFF (ngoài hệ thống), dày 2.0, bo góc 16, xếp ngang, cách nhau 8 (sm), lề trên/phải/dưới/trái 16 (md)/16 (md)/16 (md)/16 (md), căn MIN/MIN
      - Text "icon", vị trí 34,730, cỡ 24 x 24, chữ "warning", kiểu ngoài hệ thống (24/24, đậm 400), màu #FFD6E2 (ngoài hệ thống)
      - Frame "text", vị trí 66,730, cỡ 311 x 62, xếp dọc, cách nhau 2 (ngoài hệ thống), lề trên/phải/dưới/trái 0/0/0/0, căn MIN/MIN
        - Text "Lỗi hay gặp", vị trí 66,730, cỡ 311 x 20, chữ "Lỗi hay gặp", kiểu titleSmall, labelLarge (14/20, đậm 500), màu #FFD6E2 (ngoài hệ thống)
        - Text "Rút vai lên sát tai làm cổ ngắ", vị trí 66,752, cỡ 311 x 40, chữ "Rút vai lên sát tai làm cổ ngắn lại. Thả vai xuống trước khi chụp.", kiểu bodyMedium (14/20, đậm 400), màu #FFD6E2 (ngoài hệ thống)
  - Frame "Bottom bar", vị trí 0,737, cỡ 411 x 96, nền #121212 (ngoài hệ thống)
    - Rectangle "divider", vị trí 0,737, cỡ 411 x 2, nền #4A4744 (ngoài hệ thống)
    - Instance "Button", vị trí 16,753, cỡ 379 x 60, nền #FFFFFF (ngoài hệ thống), viền #FFFFFF (ngoài hệ thống), dày 2.0, bo góc 14, xếp ngang, cách nhau 8 (sm), lề trên/phải/dưới/trái 16 (md)/24 (lg)/16 (md)/24 (lg), căn CENTER/CENTER
      - Text "icon", vị trí 117,773, cỡ 20 x 20, chữ "photo_camera", kiểu ngoài hệ thống (20/20, đậm 400), màu #111111 (ngoài hệ thống)
      - Text "label", vị trí 145,771, cỡ 149 x 24, chữ "Chụp với tư thế này", kiểu titleMedium (16/24, đậm 600), màu #111111 (ngoài hệ thống)
  - Instance "AdSlot", vị trí 0,833, cỡ 411 x 58, nền #262626 (ngoài hệ thống), xếp ngang, cách nhau 8 (sm), lề trên/phải/dưới/trái 0/0/0/0, căn CENTER/CENTER
    - Rectangle "divider", vị trí 0,833, cỡ 411 x 2, nền #4A4744 (ngoài hệ thống)
    - Frame "tag", vị trí 42,851, cỡ 66 x 22, viền #B5B0A8 (ngoài hệ thống), dày 1.0, bo góc 10, xếp ngang, cách nhau 0, lề trên/phải/dưới/trái 2 (ngoài hệ thống)/4 (xs)/2 (ngoài hệ thống)/4 (xs), căn MIN/MIN
      - Text "Quảng cáo", vị trí 47,854, cỡ 56 x 16, chữ "Quảng cáo", kiểu labelSmall (11/16, đậm 500), màu #B5B0A8 (ngoài hệ thống)
    - Text "note", vị trí 116,854, cỡ 253 x 16, chữ "Banner thích ứng (AdMob), cao khoảng 50 dp", kiểu bodySmall (12/16, đậm 400), màu #B5B0A8 (ngoài hệ thống)

## 4. Code tham khảo

Không có. Script không sinh code; AI viết Compose theo mục 2 và 3.
