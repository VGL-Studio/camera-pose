---
id: T-046
title: Tiêu chí kiểm thủ công chạy được trên emulator thành kịch bản device-session
feature: quality-gate
spec:                        # không có spec nghiệp vụ; yêu cầu là bài học L-062 (docs/lessons.md)
scenarios: []
owner: Tuấn
status: on-staging
risk: high
size: M
depends_on: []
sprint:
plan: features/quality-gate/plans/T-046-kiem-tay-tren-emulator.md
created: 2026-10-24
---

# T-046: TIÊU CHÍ KIỂM THỦ CÔNG CHẠY ĐƯỢC TRÊN EMULATOR THÀNH KỊCH BẢN DEVICE-SESSION

## Mô tả

Mỗi phiên để lại nhiều mục kiểm thủ công; phần lớn không cần máy thật (xoay màn hình khi quảng cáo đang chờ, banner lỗi rồi làm mới, luồng chi tiết tư thế sang camera, gập mở trên emulator gập) nên lỗi chỉ lộ khi có người thử (L-062). Sau ticket này, mẫu plan ghi mỗi tiêu chí kiểm thủ công là "máy thật" hay "emulator"; phần emulator thành kịch bản cho `device-session.yml`, phiên chạy cuối phiên và đọc kết quả; chỉ phần máy thật còn cho người.

## Phạm vi

- `docs/templates/plan.md` mục 4: nơi chạy của mỗi dòng kiểm thủ công, ghi trong cột "Loại test" (`emulator`, `máy thật`, `người`) thay cho một cột "Chạy ở" mới (plan T-046, mục 8, câu 7).
- Cách viết kịch bản cho `device-session` (`docs/ops/test-sessions.md`) và cách phiên đọc kết quả (`docs/auto-run.md` mục 4).
- Kịch bản đầu tiên cho các mục còn chờ của RUN-20261023-04 chạy được trên emulator: T-022, T-024, phần bố cục của T-020.

**Không bao gồm:**

- Kiểm FPS, camera thật, Play In-App Review (cần máy thật).
- T-018, T-023: quảng cáo chỉ hiện khi Remote Config bật vị trí, mà bản staging dùng file Firebase giả (L-002); giữ "máy thật: Remote Config" (plan T-046, mục 8, câu 4 và 7).
- Emulator gập thật (sửa `device-session.yml`): ticket riêng.
- Đưa về dự án mẫu: issue `from-app` riêng.

## Rủi ro sơ bộ

Cao: sửa `device-session.yml` hay script của nó nếu cần (điểm chặn 7).

## Định nghĩa hoàn thành

- [ ] Mọi tiêu chí chấp nhận của các kịch bản liệt kê ở trên đạt.
- [ ] Có test tự động cho phần logic và giao diện chính.
- [ ] CI qua toàn bộ, đã merge vào nhánh chính.
- [ ] Đã được nghiệm thu chéo và nghiệm thu cuối.
