---
id: T-081
title: Biểu tượng app
feature: loading
spec: features/loading/spec.md
scenarios: [KB-6]
owner: Nhật
status: on-staging              # backlog | ready | planned | approved | on-staging | accepted | released | stopped | cancelled
risk: low
size: S
depends_on: []
sprint:
plan: features/loading/plans/T-081-bieu-tuong-app.md
auto_run:
created: 2026-10-31
---

# T-081: BIỂU TƯỢNG APP

## Mô tả

App đang dùng biểu tượng Android mặc định: manifest chưa khai báo `android:icon`, `:app` chưa có tài nguyên `mipmap` nào. Ticket thêm biểu tượng riêng theo ảnh Nhật đưa trong phiên làm nhanh ngày 2026-10-31: lớp hình (máy ảnh và ảnh tư thế) đã được AI thu nhỏ vào vùng an toàn, lớp nền cam chuyển màu (KB-6). Sau khi xong, màn hình chính của máy, danh sách app và màn hình chào của hệ thống (Android 12 trở lên) hiện biểu tượng mới.

## Phạm vi

- `:app`: biểu tượng dạng adaptive icon (lớp nền và lớp hình), lớp hình là ảnh PNG theo năm mật độ màn hình, lớp nền là dải màu vẽ bằng XML.
- `AndroidManifest.xml` của `:app`: thêm `android:icon` và `android:roundIcon`.
- Test Robolectric: app dùng biểu tượng mới, biểu tượng là adaptive icon, lớp hình đúng cỡ và nằm trong vùng an toàn ở mọi mật độ.
- Lưu ảnh gốc vào `docs/brand/app-icon/`.

**Không bao gồm:**

- Biểu tượng một màu (Android 13 trở lên): chưa có ảnh.
- Logo tạm trên màn hình loading (`BrandLogo` của `:core:designsystem`).
- Tải biểu tượng lên Google Play.

## Rủi ro sơ bộ

Thấp. Chỉ thêm tài nguyên và hai thuộc tính trong `<application>` của manifest ở `:app`; không thêm quyền, thư viện hay SDK; không chạm module core.

## Định nghĩa hoàn thành

- [ ] Mọi tiêu chí chấp nhận của KB-6 đạt.
- [ ] Có test tự động cho phần khai báo biểu tượng và vùng an toàn của lớp hình.
- [ ] CI qua toàn bộ, đã merge vào nhánh chính.
- [ ] Có trong bản staging, dev đã xem biểu tượng trên máy.

<!--
Cỡ task. Plan dựa vào cỡ để ghi token_estimate; số token thật theo cỡ ở docs/metrics/token-usage.csv (docs/ops/token-usage.md):
S: thay đổi nhỏ trong một module, lặp lại mẫu có sẵn.
M: một màn hình hoặc một luồng xử lý mới trong một module.
L: nhiều màn hình hoặc nhiều phần. Ticket cỡ L nên cân nhắc tách nhỏ, vì phải chạy xong trong một lượt của phiên tự chạy.
-->
