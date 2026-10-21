# SỐ LIỆU THIẾT KẾ: OnboardingScreen_default_dark

Sinh bởi `scripts/figma-export.py`. Không sửa tay; thiết kế đổi thì chạy lại script (ADR-0020).

## 1. Nguồn

- Link: https://www.figma.com/design/ZTXse6375eT3KXDUJPBTpe/?node-id=20-1550
- Frame: `20:1550`, giao diện tối
- Ngày lấy: 2026-10-21

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
| kiểu chữ | 14/20, đậm 400 | bodyMedium |
| kiểu chữ | 16/24, đậm 400 | bodyLarge |
| kiểu chữ | 16/24, đậm 600 | titleMedium |
| kiểu chữ | 20/20, đậm 400 | ngoài hệ thống |
| kiểu chữ | 22/22, đậm 400 | ngoài hệ thống |
| kiểu chữ | 28/28, đậm 400 | ngoài hệ thống |
| kiểu chữ | 28/36, đậm 800 | ngoài hệ thống |
| màu chữ | #111111 | ngoài hệ thống |
| màu chữ | #B5B0A8 | ngoài hệ thống |
| màu chữ | #FFFFFF | ngoài hệ thống |
| màu nền | #121212 | ngoài hệ thống |
| màu nền | #3DDC97 | ngoài hệ thống |
| màu nền | #4D8DFF | ngoài hệ thống |
| màu nền | #FF5C8A | ngoài hệ thống |
| màu nền | #FFC83D | ngoài hệ thống |
| màu nền | #FFFFFF | ngoài hệ thống |

## 3. Cấu trúc và số liệu

Toạ độ và cỡ tính theo dp, gốc là góc trên trái của khung. Thứ tự từ trên xuống như trong Figma.

- Frame "OnboardingScreen_default_dark", vị trí 0,0, cỡ 411 x 891, nền #121212 (ngoài hệ thống), xếp dọc, cách nhau 24 (lg), lề trên/phải/dưới/trái 56 (ngoài hệ thống)/24 (lg)/24 (lg)/24 (lg), căn MIN/MIN
  - Frame "illustration", vị trí 24,56, cỡ 363 x 280
    - Frame "sticker women", vị trí 52,105, cỡ 176 x 219, nền #FF5C8A (ngoài hệ thống), viền #FFFFFF (ngoài hệ thống), dày 2.0, bo góc 16
      - Frame "pose-outline", vị trí 96,140, cỡ 89 x 148
        - Vector "Vector", vị trí 122,149, cỡ 21 x 21, viền #111111 (ngoài hệ thống), dày 2.00083327293396
        - Vector "Vector", vị trí 106,168, cỡ 65 x 114, viền #111111 (ngoài hệ thống), dày 2.00083327293396
    - Frame "sticker couple", vị trí 130,86, cỡ 150 x 200, nền #3DDC97 (ngoài hệ thống), viền #FFFFFF (ngoài hệ thống), dày 2.0, bo góc 16
      - Frame "pose-outline", vị trí 170,116, cỡ 70 x 140
        - Vector "Vector", vị trí 196,121, cỡ 19 x 19, viền #111111 (ngoài hệ thống), dày 2.00083327293396
        - Vector "Vector", vị trí 177,142, cỡ 50 x 108, viền #111111 (ngoài hệ thống), dày 2.00083327293396
    - Frame "sticker friends", vị trí 182,126, cỡ 176 x 219, nền #4D8DFF (ngoài hệ thống), viền #FFFFFF (ngoài hệ thống), dày 2.0, bo góc 16
      - Frame "pose-outline", vị trí 226,161, cỡ 89 x 148
        - Vector "Vector", vị trí 268,169, cỡ 21 x 21, viền #111111 (ngoài hệ thống), dày 2.00083327293396
        - Vector "Vector", vị trí 234,188, cỡ 65 x 114, viền #111111 (ngoài hệ thống), dày 2.00083327293396
    - Frame "badge", vị trí 260,70, cỡ 56 x 56, nền #FFC83D (ngoài hệ thống), viền #FFFFFF (ngoài hệ thống), dày 2.0, bo góc 999, xếp ngang, cách nhau 0, lề trên/phải/dưới/trái 0/0/0/0, căn CENTER/CENTER
      - Text "icon", vị trí 274,84, cỡ 28 x 28, chữ "photo_camera", kiểu ngoài hệ thống (28/28, đậm 400), màu #111111 (ngoài hệ thống)
  - Text "title", vị trí 24,360, cỡ 363 x 72, chữ "Chụp dáng đẹp, không cần đoán", kiểu ngoài hệ thống (28/36, đậm 800), màu #FFFFFF (ngoài hệ thống)
  - Text "body", vị trí 24,456, cỡ 363 x 72, chữ "Chọn một tư thế, đứng vào nét viền rồi bấm chụp. Ảnh lưu vào thư viện máy, không kèm nét viền.", kiểu bodyLarge (16/24, đậm 400), màu #B5B0A8 (ngoài hệ thống)
  - Frame "features", vị trí 24,552, cỡ 363 x 152, xếp dọc, cách nhau 16 (md), lề trên/phải/dưới/trái 0/0/0/0, căn MIN/MIN
    - Frame "feature", vị trí 24,552, cỡ 363 x 40, xếp ngang, cách nhau 16 (md), lề trên/phải/dưới/trái 0/0/0/0, căn MIN/CENTER
      - Frame "icon circle", vị trí 24,552, cỡ 40 x 40, nền #FFC83D (ngoài hệ thống), viền #FFFFFF (ngoài hệ thống), dày 2.0, bo góc 999, xếp ngang, cách nhau 0, lề trên/phải/dưới/trái 0/0/0/0, căn CENTER/CENTER
        - Text "icon", vị trí 33,561, cỡ 22 x 22, chữ "accessibility_new", kiểu ngoài hệ thống (22/22, đậm 400), màu #111111 (ngoài hệ thống)
      - Text "Hơn 200 tư thế: một người, cặp", vị trí 80,562, cỡ 307 x 20, chữ "Hơn 200 tư thế: một người, cặp đôi, nhóm bạn", kiểu bodyMedium (14/20, đậm 400), màu #FFFFFF (ngoài hệ thống)
    - Frame "feature", vị trí 24,608, cỡ 363 x 40, xếp ngang, cách nhau 16 (md), lề trên/phải/dưới/trái 0/0/0/0, căn MIN/CENTER
      - Frame "icon circle", vị trí 24,608, cỡ 40 x 40, nền #FFC83D (ngoài hệ thống), viền #FFFFFF (ngoài hệ thống), dày 2.0, bo góc 999, xếp ngang, cách nhau 0, lề trên/phải/dưới/trái 0/0/0/0, căn CENTER/CENTER
        - Text "icon", vị trí 33,617, cỡ 22 x 22, chữ "tune", kiểu ngoài hệ thống (22/22, đậm 400), màu #111111 (ngoài hệ thống)
      - Text "Chỉnh độ mờ, cỡ và vị trí nét ", vị trí 80,618, cỡ 307 x 20, chữ "Chỉnh độ mờ, cỡ và vị trí nét viền", kiểu bodyMedium (14/20, đậm 400), màu #FFFFFF (ngoài hệ thống)
    - Frame "feature", vị trí 24,664, cỡ 363 x 40, xếp ngang, cách nhau 16 (md), lề trên/phải/dưới/trái 0/0/0/0, căn MIN/CENTER
      - Frame "icon circle", vị trí 24,664, cỡ 40 x 40, nền #FFC83D (ngoài hệ thống), viền #FFFFFF (ngoài hệ thống), dày 2.0, bo góc 999, xếp ngang, cách nhau 0, lề trên/phải/dưới/trái 0/0/0/0, căn CENTER/CENTER
        - Text "icon", vị trí 33,673, cỡ 22 x 22, chữ "bookmark", kiểu ngoài hệ thống (22/22, đậm 400), màu #111111 (ngoài hệ thống)
      - Text "60 tư thế dùng được khi không ", vị trí 80,674, cỡ 307 x 20, chữ "Lưu tư thế yêu thích để chụp lại sau", kiểu bodyMedium (14/20, đậm 400), màu #FFFFFF (ngoài hệ thống)
  - Frame "spacer", vị trí 24,728, cỡ 363 x 1
  - Instance "Button", vị trí 24,753, cỡ 363 x 60, nền #FFFFFF (ngoài hệ thống), viền #FFFFFF (ngoài hệ thống), dày 2.0, bo góc 14, xếp ngang, cách nhau 8 (sm), lề trên/phải/dưới/trái 16 (md)/24 (lg)/16 (md)/24 (lg), căn CENTER/CENTER
    - Text "icon", vị trí 147,773, cỡ 20 x 20, chữ "photo_camera", kiểu ngoài hệ thống (20/20, đậm 400), màu #111111 (ngoài hệ thống)
    - Text "label", vị trí 175,771, cỡ 89 x 24, chữ "Bật camera", kiểu titleMedium (16/24, đậm 600), màu #111111 (ngoài hệ thống)
  - Text "note", vị trí 24,837, cỡ 363 x 32, chữ "App cần quyền camera để hiện khung xem trước. Ảnh chỉ lưu trên máy của bạn.", kiểu bodySmall (12/16, đậm 400), màu #B5B0A8 (ngoài hệ thống)

## 4. Code tham khảo

Không có. Script không sinh code; AI viết Compose theo mục 2 và 3.
