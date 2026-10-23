---
id: T-028
title: Nhánh chính tự hồi phục khi đỏ (ADR-1002)
feature: quality-gate
spec:                        # không có spec nghiệp vụ; yêu cầu là ADR-1002 (docs/adr/1002-main-tu-hoi-phuc.md)
scenarios: []
owner: Tuấn
status: approved
risk: high
size: M
depends_on: []
sprint:
plan: features/quality-gate/plans/T-028-main-tu-hoi-phuc.md
created: 2026-10-22
---

# T-028: NHÁNH CHÍNH TỰ HỒI PHỤC KHI ĐỎ (ADR-1002)

## Mô tả

Làm ba điểm của ADR-1002 cho cổng chất lượng của app:
- `main-guard` chạy lại `check.sh` một lần khi chỉ có test thất bại, và ghi issue `flaky` thay vì `main-broken` nếu lần chạy lại xanh;
- lần chạy xanh trên chính commit bị đổ lỗi cũng đóng `main-broken`;
- `merge-if-green.sh` merge được bản revert đúng khi `main-broken` đang mở, kiểm bằng nội dung.

Sau ticket này, một test chập chờn hay một lần merge hỏng của chính phiên không làm phiên dừng cả đêm (L-036, L-037, L-038).

## Phạm vi

- `scripts/failure-kind.sh` (mới): đọc log của `check.sh`, cho biết lỗi có phải chỉ là test thất bại không.
- `scripts/is-exact-revert.sh` (mới): kiểm một commit có đúng là bản revert của commit bị đổ lỗi trên nhánh chính mới nhất không.
- `.github/workflows/main-guard.yml`: chạy lại một lần, issue `flaky`, đóng issue khi `identical`.
- `scripts/merge-if-green.sh`: ngoại lệ cho bản revert đúng.
- `docs/auto-run.md`, `docs/CLAUDE.md`: phiên tự revert commit merge của chính phiên; chép issue `flaky` vào báo cáo.
- Test của script trong `scripts/tests/run.sh`.

**Không bao gồm:**

- Sửa test chập chờn `starting_dark` (T-027).
- Đưa về dự án mẫu: issue `from-app` riêng.

## Rủi ro sơ bộ

Cao: chạm CI và quy trình phiên tự chạy (risk-scoring.md, mục 1, điểm 7), sửa `.github/` và `scripts/merge-if-green.sh` (CLAUDE.md, mục Không được làm). Cần duyệt chéo.

## Định nghĩa hoàn thành

- [ ] ADR-1002 được duyệt (`accepted`).
- [ ] Test của script cho `failure-kind.sh`, `is-exact-revert.sh` và ngoại lệ của `merge-if-green.sh` qua.
- [ ] CI qua toàn bộ, đã merge vào nhánh chính.
- [ ] Kiểm tra thật một lần trên GitHub: chạy lại `main-guard` xanh trên commit bị đổ lỗi thì issue tự đóng.
