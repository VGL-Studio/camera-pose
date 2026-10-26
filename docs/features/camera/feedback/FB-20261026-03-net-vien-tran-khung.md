---
id: FB-20261026-03
title: Nét viền tư thế phóng to hay kéo ra ngoài thì vẽ tràn ra khỏi khung xem trước
reporter: Tuấn
source: staging
build: bản staging trên máy thật của Tuấn (chưa ghi số phiên bản)
feature: camera
ticket: T-060
severity: nang
status: fixed
created: 2026-10-26
---

# FB-20261026-03: NÉT VIỀN TƯ THẾ PHÓNG TO HAY KÉO RA NGOÀI THÌ VẼ TRÀN RA KHỎI KHUNG XEM TRƯỚC

## Điều đã xảy ra

Khung tư thế có thể phóng to hoặc kéo tràn ra khỏi khung camera. Tuấn chưa rõ đây là tính năng hay lỗi.

## Các bước làm lại

1. Mở camera với một tư thế.
2. Mở hai ngón để phóng nét viền lên 160%, hoặc kéo nét viền sát mép khung.

## Mong đợi

Nét viền chỉ vẽ trong khung xem trước, như bản mở gập.

## Ảnh chụp, video

Không có.

## Thiết bị

- Máy: điện thoại của Tuấn (chưa ghi dòng máy)
- Phiên bản Android: chưa ghi
- Ngôn ngữ, chế độ tối: chưa ghi

## Phân loại

- **Loại:** lỗi so với thiết kế (một phần).
  - Đúng spec: cỡ 40% đến 160% (KB-3), và được kéo một phần ra ngoài khung miễn còn một phần trong khung (KB-3, trường hợp đặc biệt). Ở 160%, hộp nét viền cao 640 dp trong khung 548 dp, nên phần thừa là có.
  - Lỗi: trên điện thoại, khung xem trước không cắt phần vẽ ra ngoài (`CameraScreen.kt:313-318`, `PoseOverlay.kt:108-114` dùng `graphicsLayer` không cắt). Nét viền vẽ đè lên thanh trên và lộ qua phần nền trong suốt của hàng chip, dải tư thế, hàng nút chụp. Bố cục mở gập có cắt (`CameraUnfolded.kt:117`); thiết kế đặt nét viền trong "Khung xem trước"; ảnh chụp không có nét viền nên phần ngoài khung chỉ gây hiểu lầm.
- **Xử lý:** sửa ở phiên tự chạy kế tiếp (T-060). Tuấn chọn ngày 2026-10-26: cắt theo khung xem trước, giữ giới hạn cỡ và kéo như spec. Không đổi spec hay Figma.
- **Liên kết:** [T-060](../tickets/T-060-cat-net-vien-theo-khung.md), [plan T-060](../plans/T-060-cat-net-vien-theo-khung.md)
