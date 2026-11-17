---
id: T-101
title: Chuẩn bị phát hành có Tenjin: khóa trong release.sh, Data safety, chính sách quyền riêng tư, báo UA
feature: ads
spec: features/ads/spec.md   # câu 21, ý e, h
scenarios: [KB-T1]           # tiêu chí cài từ chiến dịch với bản phát hành có khóa
owner: Nhật
status: backlog
risk: high
size: S
depends_on: [T-099, T-100]
sprint:
plan:
auto_run: false              # chạm scripts/release.sh và Play Console: người làm
created: 2026-11-17
---

# T-101: CHUẨN BỊ PHÁT HÀNH CÓ TENJIN

## Mô tả

Việc của người trước bản production đầu tiên có khóa Tenjin. Sau ticket này, bản phát hành gửi số liệu sang Tenjin và các khai báo trên Play khớp với dữ liệu app gửi.

## Phạm vi

- Tạo app trên Tenjin (production và một app thử nghiệm riêng), giữ khóa trong trình quản lý mật khẩu.
- Thêm bước nhận `-PtenjinSdkKey` vào `scripts/release.sh` (tùy chọn, như khóa MAX), đọc khóa `tenjinSdkKey` của `release.properties` trong thư mục phát hành; cùng lúc cập nhật khối hợp đồng `release.sh` và build của `scripts/tests/run.sh` (đang khẳng định `release.sh` xuất đúng 10 tham số Gradle).
- Khai Tenjin trong An toàn dữ liệu của Play Console (mã quảng cáo, mã thiết bị hay mã cài đặt, tương tác với app và quảng cáo, chẩn đoán; đủ danh sách ở spec ads mục 6) và cập nhật chính sách quyền riêng tư.
- Báo nhóm UA ngày bật Tenjin.
- Thử trên máy thật bản có khóa thử nghiệm: Tenjin ghi lượt cài và doanh thu.

**Không bao gồm:**

- Code của app (T-099, T-100).

## Rủi ro sơ bộ

Cao: chạm `scripts/release.sh` và cấu hình phát hành, chỉ người làm (ADR-0049).

## Định nghĩa hoàn thành

- [ ] Bản phát hành có khóa ghi lượt cài và doanh thu trên Tenjin.
- [ ] Data safety và chính sách quyền riêng tư đã có Tenjin trước khi bản lên Play.
- [ ] Nhóm UA đã được báo ngày bật.
