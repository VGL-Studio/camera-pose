---
id: T-077
title: Hộp thoại mất mạng hiện ngay khi quay lại app lúc không có mạng
feature: network
spec: features/network/spec.md
scenarios: [KB-1, KB-3]
owner: Nhật
status: backlog
risk: medium
size: S
depends_on: []
sprint:
plan:
auto_run:
created: 2026-10-28
---

# T-077: HỘP THOẠI MẤT MẠNG HIỆN NGAY KHI QUAY LẠI APP LÚC KHÔNG CÓ MẠNG

## Mô tả

Hiện nay người dùng đưa app ra nền, tắt mạng, rồi quay lại app thì hộp thoại mất mạng chỉ hiện sau thời gian chờ của KB-1 (câu hỏi 2), hay khi một lần tải thất bại. Trong khoảng đó người dùng thao tác trên app như thể có mạng. KB-3 chỉ nói trường hợp hộp thoại đã hiện trước khi ra nền.

Dự án mẫu (T-017 của mẫu) có `NetworkDialogController.onAppForeground()`: `:app` gọi ở mỗi lần activity bắt đầu, bỏ qua lần đầu (đi sau loading, đã kiểm), và nếu lúc đó không có mạng thì hiện hộp thoại ngay, không chờ. Ticket này mang hành vi đó về app, giữ phần còn lại của controller riêng của app (T-011).

## Phạm vi

- Spec network: thêm một tiêu chí (hay một kịch bản) "quay lại app mà không có mạng thì hộp thoại hiện ngay"; câu hỏi cho người nếu cần.
- `core/network`: thêm `onAppForeground()` vào `NetworkDialogController` của app theo bản mẫu (bỏ qua lần bắt đầu đầu tiên và khi chưa qua loading), không lấy cả file mẫu. Lý do ghi log theo kiểu của app, không thêm sự kiện phân tích.
- `:app`: `MainActivity.onStart` gọi hàm này.
- Test: unit test cho controller (lần đầu bỏ qua, chưa qua loading bỏ qua, có mạng không hiện, không có mạng hiện ngay); kịch bản emulator hay device-session nếu plan thấy cần.

**Không bao gồm:**

- Đổi định nghĩa "có mạng": T-076.
- Tự đóng hộp thoại khi có mạng lại, `LostAfterDelay`, sự kiện `network_lost_shown` của mẫu.
- Tương tác với quảng cáo khi quay lại app (`resume`, T-074): plan kiểm thứ tự giữa hộp thoại và quảng cáo nếu T-074 đã merge (khung mới chờ quảng cáo toàn màn hình đóng qua `setFullScreenAdShowing`).

## Rủi ro sơ bộ

Chạm module core (`:core:network`, điểm chặn 7 của `risk-scoring.md`) và đổi spec network đã xác nhận. Rủi ro: hộp thoại hiện oan khi quay lại app trong lúc máy đang nối lại mạng (Wi-Fi vừa bật), nên plan cân nhắc có giữ một khoảng chờ ngắn không.

## Định nghĩa hoàn thành

- [ ] Tiêu chí mới trong spec network đạt; spec được xác nhận lại.
- [ ] Có test tự động cho phần logic.
- [ ] CI qua toàn bộ, đã merge vào nhánh chính.
- [ ] Có trong bản staging; đã nghiệm thu chéo.
