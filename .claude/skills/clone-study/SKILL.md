---
name: clone-study
description: Khảo sát các app mẫu đã cài trên máy để làm app clone (ADR-0021), điều khiển máy chỉ qua scripts/app-explore/ax.sh, rồi viết báo cáo và bản đề xuất app.
argument-hint: <thư mục làm việc>
disable-model-invocation: true
---

# /clone-study: khảo sát app mẫu

Tham số: `$ARGUMENTS` (thư mục làm việc, ví dụ `~/AndroidStudioProjects/app-research/<chủ-đề>/`).

Chỉ chạy khi có người theo dõi. Đọc `docs/ops/clone-study.md` và `docs/CLAUDE.md`, mục Khi khảo sát app mẫu (app clone); các ranh giới ở đó là bắt buộc.

1. Thiếu thông tin của lời nhắn mở đầu (mục 3 của `docs/ops/clone-study.md`: app muốn làm, app mẫu đã cài, thư mục làm việc) thì hỏi dev.
2. Mọi lệnh `ax.sh` gọi bằng đường dẫn tuyệt đối tới `scripts/app-explore/ax.sh` của repo này và chạy trong thư mục làm việc, để `incidents.log` nằm ở đó. `ax.sh check` phải in `input: ok` và `network: ok`; không thì dừng và nhờ dev theo mục 1.
3. Liệt kê app đã cài (`adb shell pm list packages -3`), xác nhận danh sách với dev, kéo APK, chạy `scripts/app-explore/apk-screens.py` cho từng app.
4. Khảo sát trên máy: mỗi app một agent con, lần lượt, lời giao việc chép nguyên mục 4 của `docs/ops/clone-study.md`; đọc APK và tìm trên web song song (mục 4, 5). Chỉ điều khiển máy qua `ax.sh`; không bấm quảng cáo, không mua, không đăng nhập tài khoản cá nhân.
5. Báo cáo theo `docs/templates/reference-app.md`, bản đề xuất theo `docs/templates/app-proposal.md` (`draft`) trong `research/` của thư mục làm việc. `ax.sh newfiles` cuối mỗi app; ghi mọi dòng mới của `incidents.log`.
6. Chạy `python3 scripts/secret-scan.py` trước khi chép báo cáo vào repo. Chỉ dev ghi `approved`, chốt tên và tên gói. Sau đó dev tạo repo app bằng `scripts/new-app.sh` và chép `research/*.md` vào `docs/research/` (bước 7 của bảng tổng quan); bước tiếp: thiết kế theo `docs/ops/design-workflow.md`, mục 8.
