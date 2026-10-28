---
id: T-070
title: Trình chạy kịch bản đóng bảng của hệ thống che app (bảng thông báo, cài đặt nhanh)
feature: quality-gate
spec:                        # không có spec nghiệp vụ; yêu cầu là FB-20261027-02
scenarios: []
owner: Tuấn
status: on-staging
risk: high
size: S
depends_on: [T-054]
sprint:
plan: features/quality-gate/plans/T-070-dong-bang-he-thong.md
created: 2026-10-28
---

# T-070: TRÌNH CHẠY KỊCH BẢN ĐÓNG BẢNG CỦA HỆ THỐNG CHE APP (BẢNG THÔNG BÁO, CÀI ĐẶT NHANH)

## Mô tả

Ở `device-session` cuối RUN-20261027-02, cả sáu tệp kịch bản theo plan lỗi ở bước 2 `consent decline 30`: bảng cài đặt nhanh của hệ thống mở đè lên app, mọi lần dump chỉ có node của `com.android.systemui`, trong khi app vẫn chạy phía dưới (FB-20261027-02). Nhiều khả năng monkey, chạy ngay trước, vuốt từ mép trên và để bảng mở. Sau ticket này, `fresh` đóng bảng trước khi mở app, và một lần dump chỉ có giao diện hệ thống thì trình chạy đóng bảng một lần rồi đọc lại, nên kịch bản bắt đầu từ màn hình của app.

## Phạm vi

- `scripts/device-scenario.py`: `fresh` chạy `adb shell cmd statusbar collapse` trước khi mở app; lần dump chỉ có node của `com.android.systemui` thì đóng bảng một lần, đọc lại, ghi `note`.
- `scripts/device-session.sh`: mục "Ghi chú của trình chạy" của `summary.md` liệt kê cả dòng `note` về bảng của hệ thống.
- `scripts/tests/run.sh`, `scripts/tests/fake-adb`, ảnh dump mẫu `scripts/tests/fixtures/device-scenario/shade.xml`: test mới.
- `docs/ops/test-sessions.md` mục 5.
- FB-20261027-02 sang `planned`.
- Đề xuất về dự án mẫu (phạm vi `chung`): trình chạy kịch bản là file của dự án mẫu.

**Không bao gồm:**

- Đổi tham số của monkey trong `scripts/device-session.sh` (ví dụ chặn vuốt từ mép trên): chưa chắc monkey là nguyên nhân, và đóng bảng ở trình chạy đủ cho mọi nguyên nhân.
- `.github/`, app, các tệp kịch bản trong `app/src/deviceScenarios/`.

## Rủi ro sơ bộ

Chạm điểm chặn 7 (CI và quy trình phiên tự chạy): trình chạy kịch bản chạy ở `device-session` cuối mỗi phiên. `device-session.yml` chỉ chạy trên nhánh chính, nên lệnh `cmd statusbar collapse` chỉ kiểm thật được ở `device-session` sau khi merge.

## Định nghĩa hoàn thành

- [ ] `fresh` đóng bảng của hệ thống trước khi mở app; dump chỉ có giao diện hệ thống thì đóng bảng một lần và đọc lại.
- [ ] Có test tự động cho phần logic chính, đã thất bại trên code cũ.
- [ ] CI qua toàn bộ, đã merge vào nhánh chính.
- [ ] `device-session` sau khi merge: không tệp nào lỗi vì bảng của hệ thống; tiêu chí kịch bản của T-062 và T-064 được kiểm lại.
- [ ] Đã được nghiệm thu chéo và nghiệm thu cuối.
