---
id: T-043
title: Áp dụng ADR-1003 vào thang rủi ro và quy trình phiên tự chạy
feature: quality-gate
spec:                        # không có spec nghiệp vụ; yêu cầu là ADR-1003 (L-058, L-059)
scenarios: []
owner: Tuấn
status: on-staging
risk: high
size: S
depends_on: []
sprint:
plan: features/quality-gate/plans/T-043-ap-dung-adr-1003.md
created: 2026-10-24
---

# T-043: ÁP DỤNG ADR-1003 VÀO THANG RỦI RO VÀ QUY TRÌNH PHIÊN TỰ CHẠY

## Mô tả

ADR-1003 (accepted 2026-10-24) cho phiên nối chỗ gọi mà task khác cùng danh sách đổi, và bắt buộc phiên tự sửa điểm nên sửa trong phạm vi plan, đều có test viết trước (L-058, L-059). ADR chỉ có hiệu lực khi `docs/risk-scoring.md` (mục 4, điều kiện 1) và `docs/auto-run.md` (bước 6, bước 10) dẫn tới nó. Sau ticket này, hai tài liệu đó ghi quy tắc mới với đủ giới hạn của ADR.

## Phạm vi

- `docs/risk-scoring.md`: mục 4, điều kiện 1 và một đoạn "Không phải dừng: nối chỗ gọi"; mục 5, một dòng lịch sử.
- `docs/auto-run.md`: bước 6, dòng "Nên sửa" và đoạn rà soát phần ngoài mục 3.2; bước 10, nhật ký ghi phần ngoài mục 3.2 và phần sửa theo rà soát.

**Không bao gồm:**

- Kiểm tra tự động giới hạn 3 file và 40 dòng (ticket riêng nếu cần).
- Đưa về dự án mẫu: issue `from-app` riêng.

## Rủi ro sơ bộ

Cao: đổi điều kiện dừng của phiên tự chạy (quy trình, ADR-1003).

## Định nghĩa hoàn thành

- [ ] Mọi tiêu chí chấp nhận của các kịch bản liệt kê ở trên đạt.
- [ ] Có test tự động cho phần logic và giao diện chính.
- [ ] CI qua toàn bộ, đã merge vào nhánh chính.
- [ ] Đã được nghiệm thu chéo và nghiệm thu cuối.
