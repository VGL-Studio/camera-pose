---
id: T-100
title: Gửi sự kiện phễu quảng cáo sang Tenjin, mỗi sự kiện một công tắc từ xa
feature: ads
spec: features/ads/spec.md   # câu 21, ý b, c
scenarios: [KB-T1]           # tiêu chí sự kiện phễu và công tắc từ xa
owner: Nhật
status: planned
risk: high
size: S
depends_on: [T-099]
sprint:
plan: features/ads/plans/T-100-tenjin-phieu-quang-cao.md
auto_run:
created: 2026-11-17
---

# T-100: GỬI SỰ KIỆN PHỄU QUẢNG CÁO SANG TENJIN

## Mô tả

Lớp theo dõi Tenjin của T-099 nhận thêm `AdFunnel` và gửi các sự kiện phễu theo ADR-1010, mục 5, không tham số. Mỗi sự kiện có khóa `is_log_tenjin_<tên>` mặc định bật, đọc ở mỗi sự kiện.

## Phạm vi

- Ánh xạ sự kiện phễu của khung sang tên Tenjin (bỏ `af_`), kể cả `inters_displayed_{N}_times` với N từ 1 tới 20.
- Mười khóa công tắc còn lại `is_log_tenjin_*` trong `:core:analytics`, sinh lại `docs/remote-config-keys.md`.
- Unit test: tên đúng, không tham số, công tắc tắt thì không gửi, đổi giữa phiên có hiệu lực từ sự kiện kế tiếp, không khóa, chưa khởi động hay chưa có tín hiệu đồng ý đã rõ của T-099 thì không gửi; đọc đồng ý TCF ngay trước mỗi sự kiện, không đồng ý thì không gửi.

**Không bao gồm:**

- SDK, khởi động, doanh thu (T-099).

## Rủi ro sơ bộ

Cao: gửi thêm sự kiện phân tích ra dịch vụ ngoài và sửa module core dùng chung, dù không thêm thư viện hay quyền.

## Định nghĩa hoàn thành

- [ ] Tiêu chí sự kiện phễu và công tắc từ xa của KB-T1 đạt.
- [ ] Có test tự động cho từng sự kiện và công tắc.
- [ ] CI qua toàn bộ, đã merge vào nhánh chính.
- [ ] Có trong bản staging.
- [ ] Đã được nghiệm thu chéo và nghiệm thu cuối.
