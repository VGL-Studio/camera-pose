---
id: T-075
title: Lấy release.sh và release.md của dự án mẫu theo khung quảng cáo mới (mã quảng cáo thật, khóa AppsFlyer production)
feature: ads
spec:                        # không có spec nghiệp vụ; làm theo ADR-0009, ADR-0017, ADR-0027 của dự án mẫu
scenarios: []
owner: Tuấn
status: backlog
risk: high
size: S
depends_on: [T-074]
sprint:
plan:
created: 2026-10-28
---

# T-075: LẤY RELEASE.SH VÀ RELEASE.MD CỦA DỰ ÁN MẪU THEO KHUNG QUẢNG CÁO MỚI

## Mô tả

`scripts/release.sh` và `docs/ops/release.md` của app đang giữ ở commit `0eaeadb` của dự án mẫu (dòng `hold=` trong `docs/template-sync.txt`). Bản mới của dự án mẫu kiểm cả mã quảng cáo thật cho mọi vị trí của `AdPlacement` lẫn khóa AppsFlyer production, mà app chưa có khi chưa xong T-074. Sau ticket này, app phát bản production bằng đúng script của dự án mẫu.

## Phạm vi

- Chạy `scripts/template-sync.sh --app <camera-pose> --take scripts/release.sh` rồi `--take docs/ops/release.md`. Bỏ hai dòng `hold=` cùng ghi chú đi kèm trong `docs/template-sync.txt`.
- Khối test `release.sh` trong `scripts/tests/run.sh`: lấy bản của dự án mẫu thay cho khối của app, đang giữ để khớp `release.sh` cũ (to-app.md mục 6).
- Danh sách mã quảng cáo thật và khóa AppsFlyer production mà người phụ trách phải chuẩn bị, theo `docs/ops/release.md` mới. AI chỉ ghi danh sách tên, không đọc hay ghi giá trị.

**Không bao gồm:**

- Tạo mã quảng cáo thật trên AdMob, khóa AppsFlyer hay cấu hình production. Người làm, ngoài repo (ADR-0009).
- Phát bản production.

## Rủi ro sơ bộ

Chạm `scripts/release.sh` và quy trình phát hành production (ADR-0009, ADR-0017). Phiên tự chạy không được chạm file này nếu không có plan rủi ro cao được duyệt chéo. Ticket nhỏ nhưng rủi ro cao.

## Định nghĩa hoàn thành

- [ ] `scripts/release.sh`, `docs/ops/release.md` và khối test của chúng giống bản dự án mẫu ở base của app; không còn `hold=` cho hai file.
- [ ] `scripts/tests/run.sh` qua.
- [ ] Danh sách tên mã quảng cáo thật và khóa cần chuẩn bị nằm trong mô tả PR, không có giá trị nào.
- [ ] CI qua toàn bộ, đã merge vào nhánh chính.
