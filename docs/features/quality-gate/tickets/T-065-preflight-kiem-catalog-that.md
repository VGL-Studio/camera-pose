---
id: T-065
title: "`preflight` kiểm catalog mà máy staging thật sự tải (khóa `poses_catalog_path` trên Remote Config) và các file khởi đầu của nó"
feature: quality-gate
spec:                        # không có spec nghiệp vụ; yêu cầu là sự cố catalog v3 ngày 2026-10-26 (plan T-062, mục 4.1 và câu hỏi 4)
scenarios: []
owner: Tuấn
status: planned
risk: high
size: M
depends_on: []
sprint:
plan: features/quality-gate/plans/T-065-preflight-kiem-catalog-that.md
created: 2026-10-26
---

# T-065: `PREFLIGHT` KIỂM CATALOG MÀ MÁY STAGING THẬT SỰ TẢI

## Mô tả

Ngày 2026-10-26, khóa `poses_catalog_path` trên Remote Config của staging trỏ tới `poses/catalog/v3/catalog-c2e0ce0d.json`, file này trả 404 trên R2. Mọi bản staging dừng ở màn tải ("Our server isn't responding"), và lần chạy thử `device-session` của T-062 hỏng cả 5 kịch bản. `preflight.yml` vẫn xanh, vì nó chỉ kiểm đường dẫn viết cứng `poses/catalog/v1/...` trong `scripts/required-assets.txt`. Sau ticket này, `preflight` đọc khóa đó trên Remote Config staging (mọi giá trị, kể cả giá trị theo điều kiện), tải catalog mà khóa trỏ tới, kiểm catalog đọc được và các file khởi đầu của nó đều trả 200; sai thì đỏ, mở issue `setup-broken` với dòng `LỖI` nêu tên khóa và đường dẫn.

## Phạm vi

- `scripts/preflight.sh`: điều kiểm thứ 5, đọc template Remote Config bằng `firebase-tools remoteconfig:get` với tài khoản dịch vụ chỉ đọc.
- `.github/workflows/preflight.yml`, `.github/workflows/start-run.yml`: đưa bí mật mới `STAGING_REMOTE_CONFIG_CREDENTIALS` vào bước kiểm.
- Test trong `scripts/tests/run.sh`.
- `docs/ops/staging-and-runs.md`: cách tạo tài khoản và bí mật, điều kiểm mới, "đổi khóa thì chạy lại preflight".

**Không bao gồm:**

- Phiên tự chạy tự chạy lại `preflight.yml` lúc bắt đầu phiên (câu hỏi 3 của plan).
- Ẩn nút đổi camera khi máy chỉ có một camera (T-064).

## Rủi ro sơ bộ

Cao: sửa CI (`.github/`) và cách phiên bắt đầu (điểm chặn 7), thêm một bí mật mới. Tuấn phải tạo tài khoản dịch vụ và bí mật trước khi merge, không thì `preflight` đỏ và mọi phiên bị chặn.

## Định nghĩa hoàn thành

- [ ] Test của script trong `scripts/tests/run.sh` qua; `scripts/check.sh` qua.
- [ ] CI qua toàn bộ, đã merge vào nhánh chính.
- [ ] Trước khi duyệt, trên nhánh thử: `preflight.yml` chạy tay xanh, có dòng `QUA: catalog ... (khóa poses_catalog_path)`; chạy tay với ô `extra_catalog` là một đường dẫn trả 404 cho dòng `LỖI: khóa poses_catalog_path trỏ tới ...` đúng.
- [ ] Sau khi merge: chỉ một lần chạy xanh `preflight.yml` trên nhánh chính; không chạy đường đỏ trên nhánh chính (lần chạy đỏ mở `setup-broken` và làm hỏng điều kiện "xanh trong 26 giờ" của phiên).
