---
id: T-075
title: Lấy release.sh và release.md của dự án mẫu theo khung quảng cáo mới (mã quảng cáo thật; chưa có AppsFlyer)
feature: ads
spec:                        # không có spec nghiệp vụ; làm theo ADR-0009, ADR-0017, ADR-0027 của dự án mẫu
scenarios: []
owner: Tuấn
status: backlog
risk: high
size: S
depends_on: [T-074]          # và đợt đồng bộ có VGL-Studio/app-project-template#261 (khóa AppsFlyer tùy chọn); chưa có thì không làm
sprint:
plan:                        # không có plan cho phiên tự chạy; người làm hay AI có người theo dõi (mục Rủi ro sơ bộ)
auto_run: false              # người làm (mục Rủi ro sơ bộ); plan-queue.py, run-check.py đọc trường này từ khi đồng bộ VGL-Studio/app-project-template#255
created: 2026-10-28
---

# T-075: LẤY RELEASE.SH VÀ RELEASE.MD CỦA DỰ ÁN MẪU THEO KHUNG QUẢNG CÁO MỚI

## Mô tả

`scripts/release.sh` và `docs/ops/release.md` của app đang giữ ở commit `0eaeadb` của dự án mẫu (dòng `hold=` trong `docs/template-sync.txt`). Bản mới của dự án mẫu kiểm cả mã quảng cáo thật cho mọi vị trí của `AdPlacement` lẫn khóa AppsFlyer production, mà app chưa có khi chưa xong T-074. Sau ticket này, app phát bản production bằng đúng script của dự án mẫu. AppsFlyer chưa bật ở app (câu 3 của `features/ads/spec.md`), mà bước 1 của `release.sh` mẫu luôn đòi khóa dev AppsFlyer (`appsFlyerDevKey`, `docs/ops/release.md` mục 2.3 của mẫu). App không sửa `release.sh`, nên đã đề xuất về dự án mẫu cho khóa này là tùy chọn khi app không có SDK AppsFlyer: VGL-Studio/app-project-template#261 (Nhật quyết ngày 2026-10-28). Ticket chỉ làm sau khi bản mẫu có thay đổi đó và app đã đồng bộ base chứa nó.

## Phạm vi

- Kiểm trước: base của `docs/template-sync.txt` đã chứa thay đổi của VGL-Studio/app-project-template#261. Chưa chứa thì dừng, không lấy `release.sh`.
- Chạy `scripts/template-sync.sh --app <camera-pose> --take scripts/release.sh` rồi `--take docs/ops/release.md`. Bỏ hai dòng `hold=` cùng ghi chú đi kèm trong `docs/template-sync.txt`.
- Khối test `release.sh` trong `scripts/tests/run.sh`: lấy bản của dự án mẫu thay cho khối của app, đang giữ để khớp `release.sh` cũ (to-app.md mục 6).
- Danh sách mã quảng cáo thật (không có khóa AppsFlyer, vì app chưa bật AppsFlyer) mà người phụ trách phải chuẩn bị, theo `docs/ops/release.md` mới. AI chỉ ghi danh sách tên, không đọc hay ghi giá trị.

**Không bao gồm:**

- Tạo mã quảng cáo thật trên AdMob hay cấu hình production. Người làm, ngoài repo (ADR-0009). App không có khóa AppsFlyer.
- Sửa `release.sh` ở app cho hợp với app không có AppsFlyer: sửa ở dự án mẫu (#261).
- Phát bản production.

## Rủi ro sơ bộ

Chạm `scripts/release.sh` và quy trình phát hành production (ADR-0009, ADR-0017). `CLAUDE.md` ("Không chạm vào `scripts/release.sh`") và `docs/auto-run.md` ("Phiên không chạm vào `scripts/release.sh`") không có ngoại lệ. Vì vậy ticket này **không bao giờ vào danh sách chạy tự động**: người làm, hoặc AI trong phiên có người theo dõi chạy `template-sync.sh --take` theo `docs/ops/to-app.md` của dự án mẫu. Rủi ro cao.

## Định nghĩa hoàn thành

- [ ] `scripts/release.sh`, `docs/ops/release.md` và khối test của chúng giống bản dự án mẫu ở base của app; không còn `hold=` cho hai file.
- [ ] `scripts/tests/run.sh` qua.
- [ ] `release.sh` chạy thử khô (không phát hành) với thư mục phát hành không có `appsFlyerDevKey` không dừng ở bước 1; người làm, ngoài repo.
- [ ] Danh sách tên mã quảng cáo thật cần chuẩn bị (mọi vị trí của `AdPlacement`, kể cả `open_app`, `open_app_inter`, `resume`, `resume_inter`, `native_combo` dù vị trí đó tắt, theo `docs/ops/ads-guide.md`) nằm trong mô tả PR, không có giá trị nào.
- [ ] CI qua toàn bộ, đã merge vào nhánh chính.
