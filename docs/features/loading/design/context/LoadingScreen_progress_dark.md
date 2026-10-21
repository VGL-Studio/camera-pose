# SỐ LIỆU THIẾT KẾ: LoadingScreen_progress_dark

Sinh bởi `scripts/figma-export.py`. Không sửa tay; thiết kế đổi thì chạy lại script (ADR-0020).

## 1. Nguồn

- Link: https://www.figma.com/design/ZTXse6375eT3KXDUJPBTpe/?node-id=25-2089
- Frame: `25:2089`, giao diện tối
- Ngày lấy: 2026-10-21

## 2. Giá trị dùng

Đối chiếu với `docs/design-system/tokens.json` theo giá trị. "ngoài hệ thống" nghĩa là không có token
nào trùng; plan quyết định làm tròn về token gần nhất hay thêm token.

| Loại | Giá trị | Token |
| --- | --- | --- |
| khoảng cách | 8 | sm |
| kiểu chữ | 11/16, đậm 500 | labelSmall |
| kiểu chữ | 12/16, đậm 400 | bodySmall |
| kiểu chữ | 14/14, đậm 400 | ngoài hệ thống |
| kiểu chữ | 14/20, đậm 400 | bodyMedium |
| kiểu chữ | 14/20, đậm 500 | titleSmall, labelLarge |
| kiểu chữ | 32/40, đậm 800 | ngoài hệ thống |
| màu chữ | #B5B0A8 | ngoài hệ thống |
| màu chữ | #FFFFFF | ngoài hệ thống |
| màu nền | #121212 | ngoài hệ thống |
| màu nền | #2A2A2A | ngoài hệ thống |
| màu nền | #FFC83D | ngoài hệ thống |

## 3. Cấu trúc và số liệu

Toạ độ và cỡ tính theo dp, gốc là góc trên trái của khung. Thứ tự từ trên xuống như trong Figma.

- Frame "LoadingScreen_progress_dark", vị trí 0,0, cỡ 411 x 891, nền #121212 (ngoài hệ thống)
  - Frame "content", vị trí 46,200, cỡ 318 x 368, xếp dọc, cách nhau 0, lề trên/phải/dưới/trái 0/0/0/0, căn MIN/CENTER
    - Instance "BrandLogo", vị trí 150,200, cỡ 112 x 112, nền #FFC83D (ngoài hệ thống), viền #FFFFFF (ngoài hệ thống), dày 2.0, bo góc 24, xếp ngang, cách nhau 0, lề trên/phải/dưới/trái 0/0/0/0, căn CENTER/CENTER
      - Frame "pose-outline", vị trí 186,216, cỡ 40 x 80
        - Vector "Vector", vị trí 200,219, cỡ 11 x 11, viền #111111 (ngoài hệ thống), dày 3.0
        - Vector "Vector", vị trí 189,231, cỡ 29 x 62, viền #111111 (ngoài hệ thống), dày 3.0
    - Frame "gap", vị trí 205,312, cỡ 1 x 24
    - Text "app name", vị trí 46,336, cỡ 318 x 80, chữ "Camera Pose: Lines and Guides", kiểu ngoài hệ thống (32/40, đậm 800), màu #FFFFFF (ngoài hệ thống)
    - Frame "gap", vị trí 205,416, cỡ 1 x 8
    - Text "slogan", vị trí 46,424, cỡ 318 x 20, chữ "Tạo dáng theo nét viền, chụp đẹp ngay.", kiểu bodyMedium (14/20, đậm 400), màu #B5B0A8 (ngoài hệ thống)
    - Frame "gap", vị trí 205,444, cỡ 1 x 40
    - Frame "progress label", vị trí 46,484, cỡ 318 x 20, xếp ngang, cách nhau 0, lề trên/phải/dưới/trái 0/0/0/0, căn SPACE_BETWEEN/MIN
      - Text "label", vị trí 46,484, cỡ 104 x 20, chữ "Đang tải dữ liệu", kiểu titleSmall, labelLarge (14/20, đậm 500), màu #B5B0A8 (ngoài hệ thống)
      - Text "percent", vị trí 334,484, cỡ 31 x 20, chữ "68%", kiểu bodyMedium (14/20, đậm 400), màu #FFFFFF (ngoài hệ thống)
    - Frame "gap", vị trí 205,504, cỡ 1 x 16
    - Instance "ProgressBar", vị trí 46,520, cỡ 318 x 8, nền #2A2A2A (ngoài hệ thống), viền #FFFFFF (ngoài hệ thống), dày 1.0, bo góc 999
      - Rectangle "progress", vị trí 46,520, cỡ 216 x 8, nền #FFC83D (ngoài hệ thống), bo góc 999
    - Frame "gap", vị trí 205,528, cỡ 1 x 24
    - Text "hint", vị trí 46,552, cỡ 318 x 16, chữ "Đang chuẩn bị bộ tư thế…", kiểu bodySmall (12/16, đậm 400), màu #B5B0A8 (ngoài hệ thống)
  - Frame "footer", vị trí 118,843, cỡ 175 x 16, xếp ngang, cách nhau 8 (sm), lề trên/phải/dưới/trái 0/0/0/0, căn MIN/CENTER
    - Text "icon", vị trí 118,844, cỡ 14 x 14, chữ "lock", kiểu ngoài hệ thống (14/14, đậm 400), màu #B5B0A8 (ngoài hệ thống)
    - Text "footer", vị trí 140,843, cỡ 153 x 16, chữ "Ảnh chỉ lưu trên máy của bạn", kiểu labelSmall (11/16, đậm 500), màu #B5B0A8 (ngoài hệ thống)

## 4. Code tham khảo

Không có. Script không sinh code; AI viết Compose theo mục 2 và 3.
