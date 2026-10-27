---
id: T-065
title: "`setup-check` kiểm catalog tư thế mà máy tải (catalog mặc định và khóa `poses_catalog_path` trên Remote Config) cùng các file khởi đầu của nó"
feature: quality-gate
spec:                        # không có spec nghiệp vụ; yêu cầu là hai sự cố catalog ngày 2026-10-26 và 2026-10-27 (plan T-062, mục 4.1 và câu hỏi 4; rà soát PR #179)
scenarios: []
owner: Tuấn
status: planned
risk: high
size: M
depends_on: []
sprint:
plan: features/quality-gate/plans/T-065-setup-check-kiem-catalog-that.md
created: 2026-10-26
---

# T-065: `SETUP-CHECK` KIỂM CATALOG TƯ THẾ MÀ MÁY TẢI

## Mô tả

Hai sự cố làm app staging dừng ở màn tải mà kiểm tra thiết lập vẫn xanh:

- 2026-10-26: khóa `poses_catalog_path` trên Remote Config staging trỏ tới `poses/catalog/v3/catalog-c2e0ce0d.json`, file này trả 404. Kiểm tra chỉ biết đường dẫn viết cứng `poses/catalog/v1/...`.
- 2026-10-27: catalog mặc định `poses/catalog/v1/catalog-587b5394.json` trả 200, nhưng 8 ảnh của bộ khởi đầu (`borrowed/poses/img/v1/...`) trả 404, nên `ui-test` của mọi PR có code đỏ. Tải ảnh lên lại rồi mà Cloudflare vẫn trả 404 cũ (giữ với `max-age=31536000`) cho tới khi người xóa bộ nhớ đệm.

Sau ticket này, `scripts/setup-check.sh` (mỗi sáng ở `env-canary.yml`, trước mỗi phiên ở `start-run.yml`) kiểm catalog mặc định của app và mọi giá trị của khóa `poses_catalog_path` trên Remote Config staging, kể cả giá trị theo điều kiện: catalog trả 200, đọc được, không đòi app mới hơn, ảnh nhỏ và nét viền của mọi tư thế `starter` trả 200. Sai thì đỏ, dòng `LỖI` nêu đường dẫn; file đã có mà CDN còn giữ 404 cũ thì dòng `LỖI` nói rõ phải xóa bộ nhớ đệm của URL nào.

PR #180 (bản đầu, trên `scripts/preflight.sh`) đã đóng: PR #179 thay `preflight` bằng `setup-check` của dự án mẫu.

## Phạm vi

- `scripts/setup-check-app.sh` (mới, chỉ có ở app): phần kiểm catalog, đọc template Remote Config bằng `firebase-tools remoteconfig:get` với tài khoản dịch vụ chỉ đọc.
- `scripts/setup-check.sh` (của dự án mẫu): thử lại một lần khi `000`, `429`, `5xx`; gợi ý xóa bộ nhớ đệm CDN; gọi `scripts/setup-check-app.sh` khi repo có file đó. Đề xuất về dự án mẫu.
- `.github/workflows/env-canary.yml`, `.github/workflows/start-run.yml`: bí mật `STAGING_REMOTE_CONFIG_CREDENTIALS` (đã có, Tuấn tạo ngày 2026-10-27), giới hạn thời gian của job 10 phút.
- Test trong `scripts/tests/run.sh`.
- `docs/ops/staging-and-runs.md`, `CLAUDE.md`.

**Không bao gồm:**

- Phiên tự chạy tự chạy lại kiểm tra lúc bắt đầu phiên mở bằng `claude --cloud` (câu hỏi 3 của plan).
- Biến chú ý lặp lại nhiều ngày thành issue (câu hỏi 5).

## Rủi ro sơ bộ

Cao: sửa CI (`.github/`) và điều kiện bắt đầu phiên (`start-run.yml`; điểm chặn 7), sửa một script của dự án mẫu. Kiểm tra đỏ vô cớ thì chặn mọi phiên, nên lỗi tạm (mạng, công cụ) chỉ là chú ý.

## Định nghĩa hoàn thành

- [ ] Test của script trong `scripts/tests/run.sh` qua; `scripts/check.sh` qua.
- [ ] CI qua toàn bộ, đã merge vào nhánh chính.
- [ ] Trước khi duyệt, trên nhánh thử (workflow tạm, không mở hay đóng issue): đường xanh có dòng `ok   - ... catalog ... và <n> file khởi đầu trả 200` cho catalog của khóa và catalog mặc định; đường đỏ (`SETUP_CHECK_EXTRA_CATALOG` trả 404) có dòng `LỖI - khóa poses_catalog_path trỏ tới catalog ...`.
- [ ] Sau khi merge: một lần chạy xanh của job `setup-check` trên nhánh chính (`env-canary`); không chạy đường đỏ trên nhánh chính.
