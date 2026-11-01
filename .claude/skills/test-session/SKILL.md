---
name: test-session
description: Phân tích một phiên thử trên máy thật hay emulator (test-sessions/TS-YYYYMMDD-NN), viết báo cáo có bằng chứng log và tách phát hiện thành phản hồi. Dùng khi dev vừa ghi xong phiên bằng scripts/device-session.sh.
argument-hint: <TS-YYYYMMDD-NN hoặc thư mục>
---

# /test-session: phân tích một phiên thử

Tham số: `$ARGUMENTS` (mã hay thư mục phiên thử). Không có thì lấy thư mục mới nhất trong `test-sessions/` và hỏi dev xác nhận.

Chưa có phiên thử: hướng dẫn dev ghi bằng `scripts/device-session.sh start <tên>`, thử app, `scripts/device-session.sh stop` (`docs/ops/test-sessions.md`, mục 2).

1. Đọc `docs/CLAUDE.md`, mục Khi phân tích một phiên thử, và `docs/ops/test-sessions.md`, mục 3. Làm đúng thứ tự đọc file ở đó.
2. Đọc spec của feature đã thử, dựng lại diễn biến theo dòng `VGL.Session`, đối chiếu từng kịch bản.
3. Ngoài lỗi, tìm chỗ người dùng có vẻ lúng túng (bấm lại nhiều lần, quay ra quay vào, chờ lâu). Mỗi phát hiện có dòng log kèm giờ làm bằng chứng; tách kết luận và giả thuyết; log không đủ thì ghi Log còn thiếu.
4. Báo cáo `docs/test-sessions/TS-YYYYMMDD-NN.md` theo `docs/templates/test-session.md`; mỗi phát hiện, đề xuất một phản hồi `new` theo `docs/templates/feedback.md` trong `docs/features/<feature>/feedback/`, liên kết về báo cáo.
5. Bỏ mã định danh máy, tài khoản, tên mạng, đường dẫn có tên người; không đưa log thô vào repo; chạy `python3 scripts/secret-scan.py`.
6. Đưa dev xem báo cáo, nhất là trích đoạn log, trước khi commit. Sau đó gợi ý `/triage-feedback`.
