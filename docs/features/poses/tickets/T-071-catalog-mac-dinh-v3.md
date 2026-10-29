---
id: T-071
title: Catalog mặc định của app là catalog tư thế thật (v3), bỏ 6 tư thế mượn
feature: poses
spec: features/poses/spec.md
scenarios: [KB-1, KB-5]      # kịch bản trong spec mà ticket này xử lý
owner: Nhật
status: on-staging              # backlog | ready | planned | approved | on-staging | accepted | released | stopped | cancelled
risk: high                   # chạm module core (:core:poses), risk-scoring.md mục 1, điểm 7
size: S                      # S | M | L, xem giải thích ở cuối
depends_on: []               # các ticket phải xong trước, ví dụ [T-010]
sprint:
plan: features/poses/plans/T-071-catalog-mac-dinh-v3.md
created: 2026-10-28
---

# T-071: CATALOG MẶC ĐỊNH CỦA APP LÀ CATALOG TƯ THẾ THẬT (V3), BỎ 6 TƯ THẾ MƯỢN

## Mô tả

Nội dung tư thế thật đã có trên R2 staging: catalog `poses/catalog/v3/catalog-512aa695.json`, 350 tư thế, 50 tư thế của bộ khởi đầu, không có ảnh mượn. Khóa Remote Config `poses_catalog_path` của staging đã trỏ tới file này (lần chạy `env-canary` ngày 2026-10-28). Đường dẫn mặc định trong code (`DEFAULT_CATALOG_PATH`) vẫn là catalog mẫu `poses/catalog/v1/catalog-587b5394.json` với 6 tư thế mượn của SimplePose. Máy chưa nhận được Remote Config, bản build dùng cấu hình Firebase giả (CI, emulator của `device-session`) và bản production sau này vì vậy vẫn hiện tư thế mượn. Sau ticket này, mọi bản build đều dùng catalog thật khi không có giá trị Remote Config, và `docs/borrowed-assets.md` không còn dòng `borrowed` nào.

Nguồn: đề xuất xếp lại backlog BR-20261028-01, dòng 2; Nhật xác nhận ngày 2026-10-28 rằng nội dung tư thế đã làm đủ và đã thay.

## Phạm vi

- Đổi `DEFAULT_CATALOG_PATH` sang `poses/catalog/v3/catalog-512aa695.json`.
- Thêm bản sao của catalog v3 vào file mẫu của test `:core:poses`, để CI kiểm parser đọc đủ nội dung thật và đường dẫn mặc định khớp mã băm của file.
- `docs/ops/required-assets.txt`: file R2 bắt buộc là catalog v3.
- `docs/borrowed-assets.md`: 18 dòng chuyển sang `replaced`.

**Không bao gồm:**

- Đưa file lên R2, đổi Remote Config: dev đã làm.
- Xóa file mượn trên R2 staging: file R2 không bao giờ bị xóa (ADR-0006).
- Đổi spec (ước tính dung lượng catalog ở mục 7): câu hỏi trong plan, mục 8.

## Rủi ro sơ bộ

Chạm module core `:core:poses` (điểm chặn 7), nên rủi ro cao, cần duyệt chéo. Code chỉ đổi một hằng số. Rủi ro thật là test emulator của CI và `device-session`: các test này dùng cấu hình Firebase giả, nên từ nay tải catalog 1,6 MB và 100 file của bộ khởi đầu thay cho 9 file.

## Định nghĩa hoàn thành

- [ ] Mọi tiêu chí chấp nhận của các kịch bản liệt kê ở trên đạt.
- [ ] Có test tự động cho phần logic và giao diện chính.
- [ ] CI qua toàn bộ, đã merge vào nhánh chính.
- [ ] Có trong bản staging, đúng với thiết kế đã duyệt.
- [ ] Đã được nghiệm thu chéo và nghiệm thu cuối.

<!--
Cỡ task. Plan dựa vào cỡ để ghi token_estimate; số token thật theo cỡ ở docs/metrics/token-usage.csv (docs/ops/token-usage.md):
S: thay đổi nhỏ trong một module, lặp lại mẫu có sẵn.
M: một màn hình hoặc một luồng xử lý mới trong một module.
L: nhiều màn hình hoặc nhiều phần. Ticket cỡ L nên cân nhắc tách nhỏ, vì phải chạy xong trong một lượt của phiên tự chạy.
-->
