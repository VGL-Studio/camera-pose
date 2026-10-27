---
ticket: T-065
title: "`setup-check` kiểm catalog tư thế mà máy tải (catalog mặc định và khóa `poses_catalog_path` trên Remote Config) cùng các file khởi đầu của nó"
status: done                 # draft | approved | stopped | done
version: 2                   # tăng khi plan được sửa lại sau phản hồi hoặc sau khi dừng
risk_score:                  # tổng điểm theo risk-scoring.md, để trống nếu chạm điểm chặn
risk_level: high             # low | medium | high
token_estimate: 7.6M         # cỡ M, số gợi ý của scripts/token-usage.py estimate (2026-10-27), không nâng tay (L-081)
blocking_points: [ci, process]
run_order:                   # AI điền sau họp đánh giá rủi ro
reviewer: Nhật               # người duyệt
cross_reviewer: Tuấn         # bắt buộc khi risk_level là high
approved_at: 2026-10-27
---

# PLAN T-065: `SETUP-CHECK` KIỂM CATALOG TƯ THẾ MÀ MÁY TẢI

## 1. Tóm tắt

Thêm phần kiểm catalog tư thế vào kiểm tra thiết lập `scripts/setup-check.sh` (mỗi sáng ở job `setup-check` của `env-canary.yml`, trước mỗi phiên ở `start-run.yml`). Phần riêng của app nằm ở file mới `scripts/setup-check-app.sh`, được `setup-check.sh` gọi ở cuối: kiểm catalog mặc định của app và, với `--staging`, mọi giá trị của khóa `poses_catalog_path` trên Remote Config staging (đọc bằng `firebase-tools remoteconfig:get`, tài khoản chỉ đọc, bí mật `STAGING_REMOTE_CONFIG_CREDENTIALS` đã có). Mỗi catalog phải trả 200, đọc được, không đòi app mới hơn, và ảnh nhỏ cùng nét viền của mọi tư thế `starter` phải trả 200. `setup-check.sh` của dự án mẫu thêm ba điều chung: hỏi lại một lần khi `000`, `429`, `5xx`; với file trả 404 hay 403, hỏi lại kèm tham số chống bộ nhớ đệm để báo "CDN còn giữ câu trả lời cũ, xóa bộ nhớ đệm của URL này"; gọi file kiểm của app nếu repo có. Để R2 tăng từ 1 lên khoảng 110 lệnh mỗi lần không làm phiên dừng vô cớ: `env-canary` kiểm lại sau 90 giây trước khi mở issue, `start-run` xanh thì đóng issue `setup-broken` cũ, R2 không trả lời được 3 file liền thì dừng phần catalog, phần catalog có ngân sách thời gian. Lệnh `npx firebase-tools` chạy không có token GitHub bên cạnh. Code và 81 test mới đã chạy thật trên code hiện tại (73 thất bại đúng lý do, 8 đối chứng qua), trên bản cài thử (qua hết; `scripts/tests/run.sh` 810 ok; `scripts/check.sh` qua) và với 40 đột biến; workflow chạy thử thật ba lần trên nhánh tạm (mục 4.1).

**Phiên bản 2.** Phiên bản 1 (PR #180, không merge, chưa duyệt, đã đóng) sửa `scripts/preflight.sh`; PR #179 (đồng bộ dự án mẫu, đợt 1) đã gỡ `preflight` và thay bằng `setup-check` của dự án mẫu, nên plan viết lại trên cấu trúc mới, giữ mọi điều đã chứng minh ở bản 1 (lệnh `remoteconfig:get -o`, lỗi của công cụ ra stdout, các mức `LỖI` và `CHÚ Ý`, mọi loại giá trị của khóa, hai vòng rà soát độc lập), thêm phần catalog mặc định cùng gợi ý CDN theo sự cố ngày 2026-10-27, và sửa theo hai vòng rà soát độc lập của bản 2 (vòng 1: 2 chặn, 8 nên sửa; vòng 2: 0 chặn, 3 nên sửa). Cách tiếp tục: làm lại từ đầu trên nhánh mới (bản 1 không có code nào trên nhánh chính).

## 2. Hiểu yêu cầu

Không có spec nghiệp vụ. Yêu cầu là hai sự cố:

- 2026-10-26 (plan T-062, mục 4.1 và câu hỏi 4; Tuấn chọn làm ticket riêng): khóa `poses_catalog_path` trỏ tới `poses/catalog/v3/catalog-c2e0ce0d.json`, file trả 404, app dừng ở màn tải; kiểm tra chỉ biết đường dẫn viết cứng.
- 2026-10-27 (rà soát PR #179): catalog mặc định `poses/catalog/v1/catalog-587b5394.json` trả 200 nhưng 8 ảnh khởi đầu `borrowed/poses/img/v1/...` trả 404, `ui-test` (bản staging của CI dùng file Firebase giả, nên dùng catalog mặc định) đỏ trên mọi PR có code. Tải ảnh lên lại lúc 01:42 UTC rồi mà địa chỉ thường vẫn trả 404 (`cf-cache-status: HIT`, `cache-control: max-age=31536000`); hỏi kèm tham số thì 200; Tuấn xóa bộ nhớ đệm thì hết.

| Yêu cầu | Tiêu chí chấp nhận | Cách hiểu của AI |
| --- | --- | --- |
| Catalog mặc định luôn được kiểm | Khi ảnh nhỏ hay nét viền của một tư thế `starter` trong catalog mặc định không trả 200 thì `setup-check.sh` (cả khi không có `--staging`) trả 1, dòng `LỖI - file khởi đầu <file> của catalog <đường dẫn> trả '<mã>' ...`; catalog mặc định trả khác 200 thì `LỖI - catalog mặc định của app <đường dẫn> trả ...`. Catalog không có tư thế `starter` nào thì `CHÚ Ý` | Máy không có giá trị Remote Config và bản staging của CI dùng nó. Không cần bí mật, nên chạy được cả trên máy dev (`docs/ops/setup-new-app.md`) |
| Bắt sự cố ngày 26 | Khi một giá trị của khóa (mặc định, theo điều kiện, `rolloutValue`, trong `parameterGroups`) trỏ tới file trả 404 thì `--staging` trả 1, dòng `LỖI - khóa poses_catalog_path trỏ tới catalog <đường dẫn> trả '404' ...` | Không đánh giá được điều kiện (phiên bản app, phần trăm, thuộc tính người dùng) ở đây; kiểm mọi giá trị là chặt hơn và đơn giản. Project staging chỉ có app staging, production ở project khác (ADR-0007) |
| Giống cách app chọn | Không có khóa, không có giá trị mặc định, `useInAppDefault` hay giá trị sai dạng (`AssetPath.isValid`) thì khóa trỏ về catalog mặc định, không có dòng `CHÚ Ý` (trừ giá trị sai dạng); giá trị sai dạng, giá trị cá nhân hóa và không đọc được giá trị nào có dòng `CHÚ Ý` | Khóa, đường dẫn mặc định và `APP_SCHEMA` đọc thẳng từ `PosesSettings.kt`, `CatalogParser.kt` (test đọc bằng cùng lệnh `sed`, nên task sau đổi catalog mặc định không làm đỏ test); quy tắc đường dẫn chép sang bash, có test cùng danh sách ví dụ của `AssetPathTest` |
| Catalog dùng được | Catalog không phải JSON có `poses`: `LỖI`; `minAppSchema` lớn hơn `APP_SCHEMA`: `LỖI` khi catalog mặc định hay giá trị mặc định của khóa dùng nó, chỉ `CHÚ Ý` khi chỉ giá trị theo điều kiện dùng nó. Một phần tử của `poses` hay `preview` không phải object không che các file khởi đầu khác; catalog trả 200 mà đọc không được thì tải lại một lần | Giá trị theo điều kiện có thể nhắm đúng bản app mới hơn |
| CDN giữ 404 cũ | Khi một file (file bắt buộc, catalog hay file khởi đầu) trả 404 hay 403 mà hỏi lại kèm `?setup-check-nocache=<số>` thì trả 200, dòng `LỖI` có thêm câu "File đã có trên R2 nhưng CDN còn giữ câu trả lời cũ cho đúng địa chỉ này: xóa bộ nhớ đệm của URL này (Cloudflare: Caching, Configuration, Custom Purge) ...". File thật sự thiếu, hay mã khác (503) thì không có câu này | Đúng chẩn đoán đã làm bằng tay ngày 27. Vẫn là `LỖI`: app trên máy nhận đúng câu trả lời 404 đó |
| Báo rõ khi thiếu quyền | Biến `REMOTE_CONFIG_CREDENTIALS` có nhưng rỗng (bí mật chưa tạo) thì `LỖI - thiếu bí mật STAGING_REMOTE_CONFIG_CREDENTIALS ...`; lỗi của công cụ có `HTTP Error: 403`, `PERMISSION_DENIED` hay `missing the following required permissions` (đọc cả stdout và stderr) thì `LỖI - ... (cần vai trò Firebase Remote Config Viewer, hoặc API Remote Config chưa bật)` | Việc người làm một lần. Không khớp `403` trần |
| Không chặn phiên vì lỗi tạm hay lỗi đồng bộ | Biến không được workflow truyền (lần đồng bộ dự án mẫu bỏ mất dòng): chỉ `CHÚ Ý`. Công cụ lỗi vì lý do khác, quá 120 giây (`timeout`, mã 124), ghi ra tệp không phải JSON, hay bí mật Firebase không có mã project: chỉ `CHÚ Ý`, catalog mặc định vẫn được kiểm. R2 trả `000`, `429`, `5xx` một lần thì hỏi lại một lần. R2 không trả lời được (`000`, `429` hay `5xx` sau lần hỏi lại) 3 file liền thì một dòng `LỖI - R2 không trả lời được ...` và phần catalog dừng; file trả lời được ở giữa đặt lại bộ đếm. Phần catalog quá ngân sách (mặc định 240 giây, gồm đọc Remote Config) thì dừng với `CHÚ Ý`. `env-canary` đỏ thì kiểm lại sau 90 giây trước khi mở issue; `start-run` xanh thì đóng issue `setup-broken` đang mở | Đỏ ở `start-run.yml` thì phiên không bắt đầu; issue `setup-broken` còn mở thì phiên mở bằng `claude --cloud` dừng ở kiểm tra đầu phiên (`docs/auto-run.md`, mục 2). Một lần lỗi mạng không được chặn cả ngày; R2 treo hay lỗi không được làm job hết giờ (khoảng 110 file, mỗi file tới 45 giây; mục 3.4, điểm 2) |
| Không lộ bí mật | Mã project, nội dung file khóa và lỗi của công cụ không bao giờ in ra | Lỗi của `firebase-tools` ghi vào tệp tạm trong thư mục tạm của `setup-check.sh`, chỉ dùng làm điều kiện |
| Thử được, không tốn hạn mức | `SETUP_CHECK_EXTRA_CATALOG=<đường dẫn>` thêm một đường dẫn kiểm như giá trị mặc định của khóa (chỉ cho lần chạy thử). Chạy tay `env-canary` với ô `setup_only` chỉ chạy job `setup-check`, không kích hoạt routine | Chạy tay `env-canary` không có ô này còn kích hoạt routine `env-canary` (tốn hạn mức) |
| Người biết phải làm gì | `docs/ops/staging-and-runs.md`: tải catalog và file của nó lên R2 trước rồi mới đổi khóa (kể cả giá trị theo điều kiện chưa bật); đổi xong thì chạy lại **env-canary** với **setup_only**; CDN giữ 404 cũ thì xóa bộ nhớ đệm | Lần chạy buổi sáng không thấy thay đổi buổi chiều |

Thiết kế tham chiếu: Không có thiết kế riêng.

ADR liên quan: ADR-0003 (Remote Config), ADR-0007 (staging, production tách project), ADR-0009 (bí mật chỉ của staging, quyền nhỏ nhất), ADR-0012 (Firebase), ADR-0015 (đường dẫn R2 có phiên bản), ADR-0016 (bí mật của các workflow, `origin: template`), ADR-0030 (đồng bộ dự án mẫu về app).

## 3. Giải pháp

### 3.1. Cách làm

**Việc của người (đã xong):** Tuấn đã tạo tài khoản dịch vụ `remote-config-reader` chỉ có vai trò Firebase Remote Config Viewer trên project staging và bí mật `STAGING_REMOTE_CONFIG_CREDENTIALS` (2026-10-27 01:19:45Z). Lần chạy thử ở mục 4.1 đọc được Remote Config thật bằng bí mật này.

**Vì sao tách file của app.** `scripts/setup-check.sh` là file của dự án mẫu (đồng bộ bằng `scripts/template-sync.sh`, ADR-0030); dự án mẫu không có `:core:poses`. Phần chỉ có ở app (catalog tư thế) nằm ở `scripts/setup-check-app.sh`, file mà dự án mẫu không có nên lần đồng bộ sau không đè. `setup-check.sh` chỉ thêm phần chung: `http_status` (hỏi lại), `cdn_hint`, và gọi file kiểm của app ở cuối (`. scripts/setup-check-app.sh` khi file có; biến `SETUP_CHECK_APP_CHECKS` thay đường dẫn, `none` trong test của dự án mẫu). File được gọi bằng `.`, không chạy riêng, để dùng chung `ok`, `problem`, `note` và bộ đếm lỗi; vì vậy phần đọc Remote Config và `r2_get` ghi kết quả vào tệp `$work/catalog-sources` và biến `code`, không chạy trong `$(...)` (đột biến 31). Mỗi dòng của `$work/catalog-sources` là `<app|default|conditional> <đường dẫn>`, để biết một catalog chỉ do giá trị theo điều kiện dùng (`minAppSchema` cao hơn thì chỉ `CHÚ Ý`).

**Giữ thay đổi khi đồng bộ dự án mẫu.** Cho tới khi dự án mẫu nhận đề xuất, lần đồng bộ sau phải giữ các thay đổi của plan này khi gộp từng file của mẫu; danh sách ghi ở cuối `docs/template-sync.txt` (file của app). Nếu một lần gộp vẫn bỏ mất dòng `REMOTE_CONFIG_CREDENTIALS` của workflow thì kiểm tra chỉ chú ý (không chặn phiên); bỏ mất lời gọi `setup-check-app.sh` thì phần catalog không chạy, như trước task này.

**Đề xuất về dự án mẫu.** Phiên không mở issue ở repo khác. Báo cáo phiên ghi vào mục Cần người kiểm tra: "mở issue `from-app` ở VGL-Studio/app-project-template trong tuần (`docs/ops/from-app.md`)" với nội dung: ba thay đổi chung của `setup-check.sh` (hỏi lại `000`, `429`, `5xx`; gợi ý CDN cho 404 và 403; móc `scripts/setup-check-app.sh`) cùng test và `SETUP_CHECK_APP_CHECKS=none`; cách chung để workflow truyền một bí mật riêng của app cho phần kiểm riêng; kiểm lại sau 90 giây ở `env-canary`, ô `setup_only`, `start-run` xanh thì đóng `setup-broken`, giới hạn 10 phút của job; chữ ở `docs/ops/staging-and-runs.md`; dòng `STAGING_REMOTE_CONFIG_CREDENTIALS` cho bảng bí mật của ADR-0016 (app không sửa ADR `origin: template`).

**`scripts/setup-check.sh`** (bản cài thử đã chạy, mục 4.1):

```diff
diff --git a/scripts/setup-check.sh b/scripts/setup-check.sh
index 5154c2c..d1d0df9 100755
--- a/scripts/setup-check.sh
+++ b/scripts/setup-check.sh
@@ -6,7 +6,12 @@
 #      resolvers validate DNSSEC, and a zone whose DNSSEC fails there does not resolve on many phones;
 #   3. with --staging: the STAGING_GOOGLE_SERVICES_JSON secret is not the fake file and has the
 #      <applicationId>.staging app, and every App Distribution group in STAGING_TESTER_GROUPS (default "team")
-#      exists, checked with the STAGING_APP_DISTRIBUTION_CREDENTIALS service account when gcloud is available.
+#      exists, checked with the STAGING_APP_DISTRIBUTION_CREDENTIALS service account when gcloud is available;
+#   4. the app's own checks in scripts/setup-check-app.sh, when the repository has that file (sourced at the end
+#      with ok, problem, note, http_status, cdn_hint, $STAGING, $work and $base_url; SETUP_CHECK_APP_CHECKS replaces
+#      the path, "none" in the template's own tests).
+# A file that does not answer 200 is asked once more with a cache-busting query: a 200 then means the file is on R2
+# but the CDN still serves an old 404 (camera-pose, 2026-10-27: a 404 cached for a year after the upload).
 #
 # Usage:  scripts/setup-check.sh [--staging]
 #   Without --staging it needs no secret: a developer runs it at the end of docs/ops/setup-new-app.md.
@@ -31,6 +36,8 @@ base_url="${SETUP_CHECK_ASSETS_BASE_URL:-$(grep -E '"ASSETS_BASE_URL", *"\\"http
   | head -n 1 | grep -oE 'https://[^\\"]+' || true)}"
 package="${SETUP_CHECK_PACKAGE:-$(sed -n 's/^ *applicationId = "\([^"]*\)".*/\1/p' app/build.gradle.kts | head -n 1)}"
 groups="${STAGING_TESTER_GROUPS:-team}"
+app_checks="${SETUP_CHECK_APP_CHECKS:-scripts/setup-check-app.sh}"
+retry_seconds="${SETUP_CHECK_RETRY_SECONDS:-5}"
 
 problems=0
 work="$(mktemp -d)"
@@ -45,6 +52,28 @@ problem() {
 note() {
   if [ -n "${GITHUB_ACTIONS:-}" ]; then echo "::warning::$*"; else echo "CHÚ Ý - $*"; fi
 }
+# HTTP status of a URL (curl options before the URL). 000, 429 and 5xx are asked once more, so one passing hiccup of
+# R2 does not open setup-broken for a whole day. Prints the code curl gave, empty when curl printed none.
+http_status() { # <url> [curl options]
+  local url="$1" code
+  shift
+  code="$(curl -sS --max-time 20 -w '%{http_code}' "$@" "$url" 2> /dev/null || true)"
+  if [[ -z "$code" || "$code" == "000" || "$code" == "429" || "$code" == 5?? ]]; then
+    sleep "$retry_seconds"
+    code="$(curl -sS --max-time 20 -w '%{http_code}' "$@" "$url" 2> /dev/null || true)"
+  fi
+  printf '%s' "$code"
+}
+# For a file that answered 404 or 403: prints a hint when the same file answers 200 with a cache-busting query,
+# that is, the file is on R2 and the CDN still serves an old answer for the plain URL. Other codes get no hint.
+cdn_hint() { # <url> <code of the plain URL>
+  local code
+  case "${2:-}" in 404 | 403) ;; *) return 0 ;; esac
+  code="$(http_status "$1?setup-check-nocache=$(date +%s)$RANDOM" -o /dev/null -I)"
+  if [ "$code" = "200" ]; then
+    printf ' File đã có trên R2 nhưng CDN còn giữ câu trả lời cũ cho đúng địa chỉ này: xóa bộ nhớ đệm của URL này (Cloudflare: Caching, Configuration, Custom Purge), và đặt Cache Rule cho mã 404 với thời hạn ngắn.'
+  fi
+}
 
 echo "== File bắt buộc trên R2 staging"
 if ! [[ "$base_url" =~ ^https://[^/]+/.*/$|^https://[^/]+/$ ]]; then
@@ -56,11 +85,11 @@ else
     path="${line%%#*}"
     path="$(echo "$path" | tr -d '[:space:]')"
     [ -n "$path" ] || continue
-    code="$(curl -sS -o /dev/null -I --max-time 20 -w '%{http_code}' "${base_url}${path}" 2> /dev/null || true)"
+    code="$(http_status "${base_url}${path}" -o /dev/null -I)"
     if [ "$code" = "200" ]; then
       ok "${base_url}${path}"
     else
-      problem "${base_url}${path} trả '${code:-không kết nối được}', cần 200. Đưa file lên R2 staging (docs/ops/setup-new-app.md, phần 3)."
+      problem "${base_url}${path} trả '${code:-không kết nối được}', cần 200. Đưa file lên R2 staging (docs/ops/setup-new-app.md, phần 3).$(cdn_hint "${base_url}${path}" "$code")"
     fi
   done < "$assets_list"
 fi
@@ -141,6 +170,11 @@ if [ "$STAGING" = "yes" ]; then
   fi
 fi
 
+if [ -f "$app_checks" ]; then
+  # shellcheck source=/dev/null
+  . "$app_checks"
+fi
+
 if [ "$problems" -gt 0 ]; then
   echo "== Thiết lập còn $problems lỗi."
   exit 1
```

**`scripts/setup-check-app.sh`** (mới, toàn bộ nội dung):

```bash
# Checks of camera-pose that the template's scripts/setup-check.sh does not have (plan T-065). Sourced by
# setup-check.sh at its end, with its helpers ok, problem, note, http_status and cdn_hint and its variables
# $STAGING, $work and $base_url; not run on its own.
#
# The pose catalog that devices download, and the starter files of each catalog (the app stops on the loading screen
# when one of them does not answer 200):
#   - always the app's default catalog (DEFAULT_CATALOG_PATH): devices without a Remote Config value and the
#     staging build of CI (fake Firebase file) use it. 2026-10-27: its starter images answered 404, ui-test was red
#     on every PR while the catalog itself answered 200;
#   - with --staging, every value of the Remote Config key poses_catalog_path in the staging project (default and
#     conditional values: conditions cannot be evaluated here). 2026-10-26: the key pointed to a catalog that
#     answered 404 while the required list still passed.
# Remote Config is read with firebase-tools and the STAGING_REMOTE_CONFIG_CREDENTIALS service account (read-only
# role), given in the environment variable REMOTE_CONFIG_CREDENTIALS. Secret values are never printed.
# Tests replace: SETUP_CHECK_POSES_SETTINGS, SETUP_CHECK_CATALOG_PARSER, SETUP_CHECK_FIREBASE_CMD,
# SETUP_CHECK_RC_TIMEOUT, SETUP_CHECK_CATALOG_SECONDS; SETUP_CHECK_EXTRA_CATALOG adds one more catalog path checked like a default value of the
# key (red-path trial only).

echo "== Catalog tư thế và bộ khởi đầu"

poses_settings="${SETUP_CHECK_POSES_SETTINGS:-core/poses/src/main/kotlin/com/vglstudio/camera/pose/line/guide/core/poses/internal/PosesSettings.kt}"
catalog_parser="${SETUP_CHECK_CATALOG_PARSER:-core/poses/src/main/kotlin/com/vglstudio/camera/pose/line/guide/core/poses/internal/CatalogParser.kt}"
# The key, the fallback path and the schema are read from the app code, so the check follows the app.
catalog_key="$(sed -nE 's/.*CATALOG_PATH_KEY = "([^"]+)".*/\1/p' "$poses_settings" 2> /dev/null | head -n 1 || true)"
default_catalog="$(sed -nE 's/.*DEFAULT_CATALOG_PATH = "([^"]+)".*/\1/p' "$poses_settings" 2> /dev/null | head -n 1 || true)"
app_schema="$(sed -nE 's/.*APP_SCHEMA = ([0-9]+).*/\1/p' "$catalog_parser" 2> /dev/null | head -n 1 || true)"
read -r -a firebase_cmd <<< "${SETUP_CHECK_FIREBASE_CMD:-npx --yes firebase-tools@15.32.1}"
rc_timeout="${SETUP_CHECK_RC_TIMEOUT:-120}"
# One line per catalog path and where it comes from: "app" (DEFAULT_CATALOG_PATH), "default" (default value of the
# key) or "conditional" (a conditional value). A path only conditional values use may target a newer app.
sources="$work/catalog-sources"
# After 3 files in a row without a usable answer (000, 429 or 5xx after the retry), R2 is taken as down: the catalog
# part stops, so a hung or failing R2 cannot use up the job's time (about 110 files, up to 45 seconds each with the
# retry). The whole part also stops after a time budget (default 240 seconds, Remote Config included), with a note:
# a slow R2 must not block every session.
r2_silent=0
r2_down=""
catalog_budget="${SETUP_CHECK_CATALOG_SECONDS:-240}"
catalog_start=$SECONDS

# Same rule as AssetPath.isValid of :core:assets: relative segments, and a version folder or a versioned file name.
valid_asset_path() {
  local path="$1" segment='[A-Za-z0-9][A-Za-z0-9._-]*'
  [[ "$path" =~ ^($segment/)*$segment$ ]] || return 1
  [[ "$path" =~ (^|/)v[0-9]+(\.[0-9]+)*/ ]] && return 0
  [[ "${path##*/}" =~ ^.+[._-](v[0-9]+(\.[0-9]+)*|[0-9a-f]{8,})\.[A-Za-z0-9]+$ ]]
}

# Every value a staging device can get for the key, as "<default|conditional> <value>". "@default" is the app's own
# default (no parameter, no default value, useInAppDefault); "@unknown" is a value kind the script does not read.
catalog_values() { # <template file>
  jq -r --arg k "$catalog_key" '
    def one: if . == null or .useInAppDefault == true then "@default"
      elif (.value | type) == "string" then .value
      elif (.rolloutValue.value | type) == "string" then .rolloutValue.value
      else "@unknown" end;
    ([.parameters[$k]?] + [.parameterGroups[]?.parameters[$k]?]) | map(select(. != null)) | .[0] as $p
    | if $p == null then "default @default"
      else ("default " + ($p.defaultValue | one)),
        (($p.conditionalValues // {}) | to_entries[] | "conditional " + (.value | one))
      end' "$1"
}

# Asks R2 for a URL like http_status and sets $code; counts files in a row without a usable answer (000, nothing, 429
# or 5xx after the retry); stops the part when the time budget is used up.
r2_get() { # <url> [curl options]
  if [ -z "$r2_down" ] && [ $((SECONDS - catalog_start)) -ge "$catalog_budget" ]; then
    r2_down=yes
    note "phần kiểm catalog quá ${catalog_budget} giây (R2 chậm); dừng, các file còn lại chưa được kiểm. Chạy lại sau ít phút."
  fi
  if [ -n "$r2_down" ]; then code=""; return 0; fi
  code="$(http_status "$@")"
  if [[ -z "$code" || "$code" == "000" || "$code" == "429" || "$code" == 5?? ]]; then
    r2_silent=$((r2_silent + 1))
    if [ "$r2_silent" -ge 3 ]; then
      r2_down=yes
      problem "R2 không trả lời được ${base_url} (3 file liền không kết nối được, 429 hay 5xx); dừng phần kiểm catalog. Chạy lại sau ít phút; còn lỗi thì kiểm R2 và tên miền."
    fi
  else
    r2_silent=0
  fi
}

# Writes the catalog paths of the key on Remote Config staging to $sources; a problem or a note when it cannot.
# Not run in a subshell, so problem() counts.
remote_catalog_paths() {
  local project template="$work/remote-config.json" errors="$work/remote-config.err" kind value found=""
  local -a get=("${firebase_cmd[@]}")
  if [ -z "${REMOTE_CONFIG_CREDENTIALS+x}" ]; then
    # The workflow does not pass the variable at all (for example a template sync dropped the line): not the
    # person's setup, so it must not block every session.
    note "không đọc được khóa $catalog_key: workflow không truyền biến REMOTE_CONFIG_CREDENTIALS (plan T-065, docs/template-sync.txt); chỉ kiểm catalog mặc định."
    return
  fi
  if [ -z "$REMOTE_CONFIG_CREDENTIALS" ]; then
    problem "thiếu bí mật STAGING_REMOTE_CONFIG_CREDENTIALS (đọc khóa $catalog_key trên Remote Config staging; docs/ops/staging-and-runs.md, mục 1.1 và 1.3)."
    return
  fi
  project="$(jq -r '.project_info.project_id // empty' "$work/google-services.json" 2> /dev/null || true)"
  if [ -z "$project" ]; then
    note "không đọc được khóa $catalog_key: chưa có google-services.json staging hợp lệ; chỉ kiểm catalog mặc định."
    return
  fi
  printf '%s' "$REMOTE_CONFIG_CREDENTIALS" > "$work/remote-config-credentials.json"
  # A time limit, so a hang cannot use up the job's time before the issue is opened or closed.
  if command -v timeout > /dev/null 2>&1; then get=(timeout -k 10 "$rc_timeout" "${firebase_cmd[@]}"); fi
  # firebase-tools prints its errors on stdout, so both streams go to the file; the template goes to -o.
  if ! GOOGLE_APPLICATION_CREDENTIALS="$work/remote-config-credentials.json" "${get[@]}" \
      remoteconfig:get --project "$project" -o "$template" > "$errors" 2>&1 \
      || ! jq -e 'type == "object"' "$template" > /dev/null 2>&1; then
    # Only these texts mean a missing role or API; any other failure (network, npm, timeout 124) is a tool failure.
    if grep -qE 'HTTP Error: 403|PERMISSION_DENIED|missing the following required permissions' "$errors" 2> /dev/null; then
      problem "tài khoản của STAGING_REMOTE_CONFIG_CREDENTIALS không đọc được Remote Config staging (cần vai trò Firebase Remote Config Viewer, hoặc API Remote Config chưa bật)."
    else
      note "không đọc được khóa $catalog_key (lệnh firebase-tools không chạy được); chỉ kiểm catalog mặc định."
    fi
    return
  fi
  while read -r kind value; do
    case "$value" in
      @default) echo "$kind $default_catalog" >> "$sources"; found=yes ;;
      @unknown) note "khóa $catalog_key có một giá trị script không đọc được (cá nhân hóa?); kiểm bằng tay." ;;
      *)
        found=yes
        if valid_asset_path "$value"; then
          echo "$kind $value" >> "$sources"
        else
          note "khóa $catalog_key có giá trị \"$value\" không hợp lệ; app dùng catalog mặc định $default_catalog."
          echo "$kind $default_catalog" >> "$sources"
        fi
        ;;
    esac
  done < <(catalog_values "$template" 2> /dev/null || true)
  [ -n "$found" ] || note "không đọc được giá trị nào của khóa $catalog_key trong template Remote Config; chỉ kiểm catalog mặc định."
}

# Checks one catalog and its starter files. <where> says where the path comes from, for the messages; <strict> is
# "no" when only conditional values use the path: a catalog for a newer app is then only a note.
check_catalog() { # <path> <where> <strict>
  local path="$1" where="$2" strict="$3" catalog="$work/catalog.json" file count=0 bad=0
  rm -f "$catalog"
  r2_get "${base_url}${path}" -o "$catalog"
  [ -z "$r2_down" ] || return 0
  if [ "$code" != "200" ]; then
    problem "$where $path trả '${code:-không kết nối được}', cần 200: app dừng ở màn tải. Sửa khóa $catalog_key trên Remote Config staging hoặc đưa file lên R2.$(cdn_hint "${base_url}${path}" "$code")"
    return
  fi
  if ! jq -e '.poses | type == "array"' "$catalog" > /dev/null 2>&1; then
    # A file cut short in transit is downloaded once more before it counts as broken.
    rm -f "$catalog"
    r2_get "${base_url}${path}" -o "$catalog"
    [ -z "$r2_down" ] || return 0
  fi
  if [ "$code" != "200" ]; then
    problem "$where $path trả '${code:-không kết nối được}' ở lần tải lại, cần 200: app dừng ở màn tải.$(cdn_hint "${base_url}${path}" "$code")"
    return
  fi
  if ! jq -e '.poses | type == "array"' "$catalog" > /dev/null 2>&1; then
    problem "$where $path không phải catalog đọc được (JSON, có danh sách poses)."
    return
  fi
  if [ -n "$app_schema" ] && jq -e --argjson app "$app_schema" '(.minAppSchema // 0) > $app' "$catalog" > /dev/null 2>&1; then
    if [ "$strict" = "yes" ]; then
      problem "$where $path cần app mới hơn: minAppSchema lớn hơn $app_schema của app."
    else
      note "$where $path (chỉ giá trị theo điều kiện dùng) cần app mới hơn $app_schema: đúng khi điều kiện chọn bản app mới; kiểm điều kiện bằng tay."
    fi
    return
  fi
  while IFS= read -r file; do
    count=$((count + 1))
    r2_get "${base_url}${file}" -o /dev/null -I
    [ -z "$r2_down" ] || return 0
    if [ "$code" != "200" ]; then
      problem "file khởi đầu $file của catalog $path trả '${code:-không kết nối được}', cần 200: app dừng ở màn tải bộ khởi đầu.$(cdn_hint "${base_url}${file}" "$code")"
      bad=$((bad + 1))
    fi
  done < <(jq -r '.poses[] | objects | select(.starter == true) | (.preview | objects | .small), .outline | strings' \
    "$catalog" 2> /dev/null || true)
  if [ "$count" -eq 0 ]; then
    note "$where $path không có tư thế starter nào: bộ khởi đầu của app sẽ trống."
  elif [ "$bad" -eq 0 ]; then
    ok "$where $path và $count file khởi đầu trả 200"
  fi
}

if [ -z "$catalog_key" ] || [ -z "$default_catalog" ]; then
  problem "không đọc được khóa và đường dẫn catalog mặc định trong $poses_settings."
elif ! [[ "$base_url" =~ ^https://[^/]+/ ]]; then
  : # setup-check.sh already reported the missing staging address.
else
  echo "app $default_catalog" > "$sources"
  if [ "$STAGING" = "yes" ]; then
    remote_catalog_paths
  fi
  [ -z "${SETUP_CHECK_EXTRA_CATALOG:-}" ] || echo "default $SETUP_CHECK_EXTRA_CATALOG" >> "$sources"
  while IFS= read -r catalog_path; do
    if [ -z "$catalog_path" ] || [ -n "$r2_down" ]; then continue; fi
    strict=no
    if grep -qxF -e "app $catalog_path" -e "default $catalog_path" "$sources"; then strict=yes; fi
    if grep -qxF -e "default $catalog_path" -e "conditional $catalog_path" "$sources"; then
      check_catalog "$catalog_path" "khóa $catalog_key trỏ tới catalog" "$strict"
    else
      check_catalog "$catalog_path" "catalog mặc định của app" "$strict"
    fi
  done < <(cut -d ' ' -f 2- "$sources" | sort -u)
fi
```

**Workflow** (`.github/workflows/env-canary.yml`, `.github/workflows/start-run.yml`):

- Hai workflow đưa bí mật vào biến `REMOTE_CONFIG_CREDENTIALS`, như hai bí mật có sẵn (`setup-check.sh` ghi nó vào tệp trong thư mục tạm `umask 077` và xóa khi xong). Chú thích đầu file nêu bí mật và điều kiểm mới.
- Không có token GitHub cạnh `npx firebase-tools` (mục 3.3): `actions/checkout` với `persist-credentials: false`, và script chạy bằng `env -u GH_TOKEN -u GITHUB_TOKEN scripts/setup-check.sh --staging`. `setup-check.sh` không cần token GitHub.
- `env-canary.yml`: ô `setup_only` (boolean) cho chạy tay; job `fire` có `if: ${{ !inputs.setup_only }}` (lịch buổi sáng không có ô nên vẫn chạy). Job `setup-check` đỏ thì kiểm lại một lần sau 90 giây rồi mới mở issue; `timeout-minutes` từ 5 lên 15: một lần chạy tốt nhất khoảng 85 giây, xấu nhất khoảng 300 giây (`remoteconfig:get` tới 130 giây, phần catalog dừng ở 240 giây hay sau 3 file không trả lời được), hai lần cộng 90 giây là khoảng 690 giây.
- `start-run.yml`: quyền `issues: write`; bước riêng "Đóng issue setup-broken cũ" ngay sau bước kiểm tra (vẫn trước "Kích hoạt routine", chỉ chạy khi kiểm tra xanh) đóng mọi issue `setup-broken` đang mở (bình luận rồi đóng; không đóng được thì chỉ `::warning::`); `timeout-minutes` từ 5 lên 10 (một lần chạy, xấu nhất khoảng 300 giây).

```diff
diff --git a/.github/workflows/env-canary.yml b/.github/workflows/env-canary.yml
index e197f01..b725921 100644
--- a/.github/workflows/env-canary.yml
+++ b/.github/workflows/env-canary.yml
@@ -5,7 +5,10 @@
 #   ENV_CANARY_FIRE_URL    fire URL of the env-canary routine's API trigger
 #   ENV_CANARY_FIRE_TOKEN  bearer token of that trigger; it can only start that one routine
 # Not fired while an auto-run session is running: the canary would compete with it for the same usage limit.
-# A second job, setup-check, checks the staging setup, R2 files and DNS on the GitHub runner (see below).
+# A second job, setup-check, checks the staging setup, R2 files and DNS on the GitHub runner (see below); in
+# camera-pose also the pose catalog and its starter files (scripts/setup-check-app.sh, plan T-065), with the
+# read-only STAGING_REMOTE_CONFIG_CREDENTIALS secret. A manual run with setup_only checks only that, without firing
+# the routine (no Claude usage).
 # Changing this file is a high-risk plan; cross-review required.
 name: env-canary
 
@@ -14,6 +17,11 @@ on:
     # 01:00 UTC is 08:00 in Vietnam: the result is there when the team starts work.
     - cron: "0 1 * * *"
   workflow_dispatch:
+    inputs:
+      setup_only:
+        description: Chỉ kiểm thiết lập (job setup-check), không kích hoạt routine env-canary
+        type: boolean
+        default: false
 
 concurrency:
   group: env-canary
@@ -29,29 +37,48 @@ permissions:
 jobs:
   # Checks on a GitHub runner, every morning, what the cloud environment cannot see: the required files on R2,
   # the DNS of the static file host through 1.1.1.1 and 8.8.8.8, the Firebase staging secret and the tester
-  # groups (scripts/setup-check.sh; app lessons L-002, L-011, L-012, L-027 of camera-pose). Red opens an issue
+  # groups (scripts/setup-check.sh; app lessons L-002, L-011, L-012, L-027 of camera-pose), and in camera-pose the
+  # pose catalog of the default path and of poses_catalog_path on Remote Config staging with its starter files
+  # (scripts/setup-check-app.sh, plan T-065). A red result is checked once more after 90 seconds, so one passing R2
+  # hiccup does not open the issue (about 110 R2 requests per run). Red opens an issue
   # labelled setup-broken, or comments on the open one; green closes it. A separate label from env-broken,
   # because scripts/env-canary.sh closes env-broken issues when the cloud environment is green.
   # Runs even during an auto-run session: it costs no Claude usage.
   setup-check:
     runs-on: ubuntu-latest
-    timeout-minutes: 5
+    # 15, not 5: the pose catalog check of scripts/setup-check-app.sh downloads firebase-tools (up to 2 minutes for
+    # remoteconfig:get) and asks R2 for every starter file of each catalog, and a red result is checked twice with
+    # 90 seconds between (worst case about 10 minutes; plan T-065).
+    timeout-minutes: 15
     permissions:
       contents: read
       issues: write
     steps:
       - uses: actions/checkout@v5
+        with:
+          # No token in .git/config: scripts/setup-check.sh runs npx firebase-tools (plan T-065).
+          persist-credentials: false
 
       - name: Kiểm tra thiết lập staging, file R2 và DNS
         env:
           GH_TOKEN: ${{ github.token }}
           GOOGLE_SERVICES: ${{ secrets.STAGING_GOOGLE_SERVICES_JSON }}
           DISTRIBUTION_CREDENTIALS: ${{ secrets.STAGING_APP_DISTRIBUTION_CREDENTIALS }}
+          # Read-only Remote Config staging account: the pose catalog the key points to (scripts/setup-check-app.sh).
+          REMOTE_CONFIG_CREDENTIALS: ${{ secrets.STAGING_REMOTE_CONFIG_CREDENTIALS }}
           STAGING_TESTER_GROUPS: ${{ vars.STAGING_TESTER_GROUPS || 'team' }}
         run: |
           result=0
-          scripts/setup-check.sh --staging > "${RUNNER_TEMP}/setup-check.log" 2>&1 || result=$?
+          # No GitHub token next to npx firebase-tools (third-party packages): this job writes issues (plan T-065).
+          env -u GH_TOKEN -u GITHUB_TOKEN scripts/setup-check.sh --staging > "${RUNNER_TEMP}/setup-check.log" 2>&1 || result=$?
           cat "${RUNNER_TEMP}/setup-check.log"
+          if [ "${result}" != "0" ]; then
+            echo "Kiểm tra đỏ; kiểm lại một lần sau 90 giây (lỗi tạm của R2 hay mạng)."
+            sleep 90
+            result=0
+            env -u GH_TOKEN -u GITHUB_TOKEN scripts/setup-check.sh --staging > "${RUNNER_TEMP}/setup-check.log" 2>&1 || result=$?
+            cat "${RUNNER_TEMP}/setup-check.log"
+          fi
           label="setup-broken"
           open_issues="$(gh api "repos/${GITHUB_REPOSITORY}/issues?state=open&labels=${label}&per_page=10" --jq '[.[].number] | join(" ")')"
           commit="${GITHUB_SHA::7}"
@@ -81,6 +108,7 @@ jobs:
           exit 1
 
   fire:
+    if: ${{ !inputs.setup_only }}
     runs-on: ubuntu-latest
     timeout-minutes: 5
     steps:
diff --git a/.github/workflows/start-run.yml b/.github/workflows/start-run.yml
index 61f285c..49f4e2a 100644
--- a/.github/workflows/start-run.yml
+++ b/.github/workflows/start-run.yml
@@ -4,7 +4,9 @@
 #   ROUTINE_FIRE_URL    https://api.anthropic.com/v1/claude_code/routines/<trig_ id>/fire, from the routine's API trigger
 #   ROUTINE_FIRE_TOKEN  bearer token of that trigger; it can only start this one routine
 # Before firing, scripts/setup-check.sh --staging also reads the staging secrets of staging.yml
-# (STAGING_GOOGLE_SERVICES_JSON, STAGING_APP_DISTRIBUTION_CREDENTIALS) and the STAGING_TESTER_GROUPS variable.
+# (STAGING_GOOGLE_SERVICES_JSON, STAGING_APP_DISTRIBUTION_CREDENTIALS), the STAGING_TESTER_GROUPS variable and, in
+# camera-pose, the read-only STAGING_REMOTE_CONFIG_CREDENTIALS for the pose catalog check (plan T-065). A green check
+# closes an open setup-broken issue, so a session opened with claude --cloud after it does not stop on an old one.
 # The fire endpoint is a research preview behind a beta header, so its contract may still change.
 # It has no idempotency key: every successful call starts a new session, so it is never retried.
 # Changing this file is a high-risk plan; cross-review required.
@@ -28,7 +30,8 @@ defaults:
 
 permissions:
   contents: read
-  issues: read
+  # write: a green setup check closes an open setup-broken issue (plan T-065).
+  issues: write
   checks: read
   # main-guard-status.sh reads when a pending main-guard run was created (Actions API); without it the time is unknown.
   actions: read
@@ -37,9 +40,13 @@ jobs:
   start:
     if: github.ref == 'refs/heads/main'
     runs-on: ubuntu-latest
-    timeout-minutes: 5
+    # 10, not 5: scripts/setup-check.sh --staging includes the pose catalog check of scripts/setup-check-app.sh.
+    timeout-minutes: 10
     steps:
       - uses: actions/checkout@v5
+        with:
+          # No token in .git/config: scripts/setup-check.sh runs npx firebase-tools (plan T-065).
+          persist-credentials: false
 
       - name: Kiểm tra trước khi bắt đầu phiên
         env:
@@ -90,13 +97,30 @@ jobs:
         env:
           GOOGLE_SERVICES: ${{ secrets.STAGING_GOOGLE_SERVICES_JSON }}
           DISTRIBUTION_CREDENTIALS: ${{ secrets.STAGING_APP_DISTRIBUTION_CREDENTIALS }}
+          # Read-only Remote Config staging account: the pose catalog the key points to (scripts/setup-check-app.sh).
+          REMOTE_CONFIG_CREDENTIALS: ${{ secrets.STAGING_REMOTE_CONFIG_CREDENTIALS }}
           STAGING_TESTER_GROUPS: ${{ vars.STAGING_TESTER_GROUPS || 'team' }}
         run: |
-          if ! scripts/setup-check.sh --staging; then
+          # No GitHub token next to npx firebase-tools (third-party packages): this job may write issues (plan T-065).
+          if ! env -u GH_TOKEN -u GITHUB_TOKEN scripts/setup-check.sh --staging; then
             echo "::error::Thiết lập chưa đủ cho một phiên: phiên sẽ dừng ở cuối (bản staging) hoặc ở test emulator đầu tiên. Sửa các lỗi trên (docs/ops/staging-and-runs.md, docs/ops/setup-new-app.md) rồi bắt đầu lại."
             exit 1
           fi
 
+      # The check above is green: an open setup-broken issue is old (the problem is gone, or it was an R2 hiccup);
+      # close it, as the morning check of env-canary.yml would, so a session opened with claude --cloud after it does
+      # not stop on it (plan T-065). A failure to close only warns.
+      - name: Đóng issue setup-broken cũ
+        env:
+          GH_TOKEN: ${{ github.token }}
+        run: |
+          for number in $(gh api "repos/${GITHUB_REPOSITORY}/issues?state=open&labels=setup-broken&per_page=10" --jq '.[].number' || true); do
+            { gh api "repos/${GITHUB_REPOSITORY}/issues/${number}/comments" -X POST \
+                -f body="Kiểm tra thiết lập của start-run xanh trên \`${GITHUB_SHA::7}\`. Đóng mục này." > /dev/null &&
+              gh api "repos/${GITHUB_REPOSITORY}/issues/${number}" -X PATCH -f state=closed > /dev/null; } ||
+              echo "::warning::không đóng được issue ${number}"
+          done
+
       - name: Kích hoạt routine
         env:
           FIRE_URL: ${{ secrets.ROUTINE_FIRE_URL }}
```

**Tài liệu** (`docs/ops/staging-and-runs.md` mục 1.1 bước 6, 1.3, 2.3, 2.4; `CLAUDE.md` mục Lệnh; `docs/template-sync.txt`, đúng chữ):

```diff
diff --git a/CLAUDE.md b/CLAUDE.md
index e95342c..8297ba6 100644
--- a/CLAUDE.md
+++ b/CLAUDE.md
@@ -38,7 +38,7 @@ Quy tắc phụ thuộc (ADR-0001, kiểm tra tự động bằng `checkModuleDe
 - Ghi một phiên thử trên máy để AI phân tích: `scripts/device-session.sh start <tên>`, thử app, `scripts/device-session.sh stop` (`docs/ops/test-sessions.md`).
 - Sinh lại trang quy tắc nền tảng `docs/foundation.md` sau khi thêm hay đổi trạng thái ADR: `scripts/foundation.py` (`docs/CLAUDE.md`, mục Khi viết ADR).
 - Xem trước trang tiến độ `docs/STATUS.md` trên máy: `scripts/status.py`. Trang không có trong git; `status-page.yml` sinh nó trên nhánh `status` sau mỗi lần merge tài liệu (`docs/CLAUDE.md`, mục Trang tiến độ).
-- Kiểm thiết lập của app (file R2 bắt buộc, DNS của tên miền file tĩnh): `scripts/setup-check.sh`; danh sách file ở `docs/ops/required-assets.txt`.
+- Kiểm thiết lập của app (file R2 bắt buộc, DNS của tên miền file tĩnh, catalog tư thế theo Remote Config và bộ khởi đầu của nó): `scripts/setup-check.sh`; danh sách file ở `docs/ops/required-assets.txt`, phần kiểm riêng của app ở `scripts/setup-check-app.sh`.
 - Merge khi nhánh chậm hơn nhánh chính: `scripts/merge-if-green.sh <số PR> --update` tự cập nhật nhánh, chờ CI rồi merge.
 - Kiểm tra cả danh sách chạy trước khi duyệt (spec đã chốt, phụ thuộc, kịch bản có test, plan cùng sửa một file): `scripts/run-check.py docs/runs/RUN-YYYYMMDD-NN.md` (`docs/ops/pre-run-review.md`).
 - Chờ một workflow GitHub Actions xong trong một lệnh: `scripts/wait-run.sh --pr <số> [--sha <commit>]` hoặc `--workflow <file.yml> --since <giờ UTC>` (`docs/auto-run.md`, bước 8).
diff --git a/docs/ops/staging-and-runs.md b/docs/ops/staging-and-runs.md
index 634609d..333b320 100644
--- a/docs/ops/staging-and-runs.md
+++ b/docs/ops/staging-and-runs.md
@@ -19,6 +19,7 @@ Bí mật thêm ở **Settings > Secrets and variables > Actions > Secrets** c
    5. Gặp lỗi `Service account key creation is disabled` thì quản trị viên tổ chức Google Cloud tắt chính sách `iam.disableServiceAccountKeyCreation` cho project staging.
    6. Cất file khóa vào trình quản lý mật khẩu, xóa bản trong thư mục tải về sau khi thêm bí mật (mục 1.3). Lộ khóa thì xóa khóa ở tab **Keys**, tạo khóa mới, cập nhật bí mật.
 5. Kiểm tra `google-services.json` có app với package `<applicationId>.staging`; workflow `staging` báo lỗi nếu không có.
+6. Tạo tài khoản dịch vụ chỉ đọc Remote Config, để `scripts/setup-check.sh --staging` kiểm catalog tư thế mà khóa `poses_catalog_path` trỏ tới (plan T-065; chỉ có ở camera-pose): như bước 4, tên `remote-config-reader`, vai trò **Firebase Remote Config Viewer** (`roles/firebaseremoteconfig.viewer`), không vai trò nào khác; tạo khóa JSON, cất như bước 4.6.
 
 ### 1.2. Khóa ký staging
 
@@ -38,6 +39,7 @@ Cất `staging.keystore` và mật khẩu vào trình quản lý mật khẩu c
 | --- | --- |
 | `STAGING_GOOGLE_SERVICES_JSON` | Toàn bộ nội dung `google-services.json` của staging |
 | `STAGING_APP_DISTRIBUTION_CREDENTIALS` | Toàn bộ nội dung file khóa JSON của tài khoản dịch vụ |
+| `STAGING_REMOTE_CONFIG_CREDENTIALS` | Toàn bộ nội dung file khóa JSON của tài khoản `remote-config-reader` (mục 1.1, bước 6) |
 | `STAGING_KEYSTORE_BASE64` | Nội dung `staging.keystore.b64` |
 | `STAGING_KEYSTORE_PASSWORD` | Mật khẩu keystore (cũng là mật khẩu của khóa) |
 | `STAGING_KEY_ALIAS` | `staging` |
@@ -47,6 +49,7 @@ Thêm bằng terminal của mình (lệnh không in giá trị ra):
 ```sh
 gh secret set STAGING_GOOGLE_SERVICES_JSON < google-services.json
 gh secret set STAGING_APP_DISTRIBUTION_CREDENTIALS < <file khóa JSON>
+gh secret set STAGING_REMOTE_CONFIG_CREDENTIALS < <file khóa JSON của remote-config-reader>
 gh secret set STAGING_KEYSTORE_BASE64 < staging.keystore.b64
 gh secret set STAGING_KEYSTORE_PASSWORD     # nhập mật khẩu khi được hỏi
 gh secret set STAGING_KEY_ALIAS --body staging
@@ -109,7 +112,7 @@ Mã này chỉ kích hoạt được đúng routine đó, không đọc được
 
 1. Danh sách chạy `docs/runs/RUN-YYYYMMDD-NN.md` có trạng thái `approved` đã được merge vào nhánh chính.
 2. **Actions > start-run > Run workflow**, nhánh `main`, nhập đường dẫn danh sách chạy.
-3. Workflow từ chối nếu danh sách chưa `approved`, nhánh chính đang đỏ (có issue `main-broken` đang mở), môi trường cloud đang lỗi (có issue `env-broken` đang mở, mục 2.4), hoặc `main-guard` chưa qua trên commit gần nhất có đổi code của nhánh chính (`scripts/main-guard-status.sh`, L-076). Workflow cũng chạy `scripts/setup-check.sh --staging` trên máy của GitHub và từ chối khi: bí mật `STAGING_GOOGLE_SERVICES_JSON` thiếu, là file giả (`fake-staging-project`) hay không có app `<applicationId>.staging`; một nhóm trong `STAGING_TESTER_GROUPS` (mặc định `team`) chưa có trên App Distribution; một file trong `docs/ops/required-assets.txt` không trả `200`; tên miền file tĩnh không phân giải được qua 1.1.1.1 hay 8.8.8.8. Các lỗi này trước đây chỉ lộ ra ở cuối phiên, khi phát bản staging (L-059). Nhóm người test được kiểm bằng API của App Distribution với tài khoản dịch vụ ở mục 1.1; API không trả lời (ví dụ thiếu quyền) thì chỉ báo chú ý. Khi thành công, phần tóm tắt của workflow có đường dẫn tới phiên.
+3. Workflow từ chối nếu danh sách chưa `approved`, nhánh chính đang đỏ (có issue `main-broken` đang mở), môi trường cloud đang lỗi (có issue `env-broken` đang mở, mục 2.4), hoặc `main-guard` chưa qua trên commit gần nhất có đổi code của nhánh chính (`scripts/main-guard-status.sh`, L-076). Workflow cũng chạy `scripts/setup-check.sh --staging` trên máy của GitHub và từ chối khi: bí mật `STAGING_GOOGLE_SERVICES_JSON` thiếu, là file giả (`fake-staging-project`) hay không có app `<applicationId>.staging`; một nhóm trong `STAGING_TESTER_GROUPS` (mặc định `team`) chưa có trên App Distribution; một file trong `docs/ops/required-assets.txt` không trả `200`; tên miền file tĩnh không phân giải được qua 1.1.1.1 hay 8.8.8.8; catalog tư thế mặc định của app, hay catalog mà một giá trị của khóa `poses_catalog_path` trên Remote Config staging trỏ tới, hay một file khởi đầu của các catalog đó không trả `200` (`scripts/setup-check-app.sh`, plan T-065). Kiểm tra xanh thì workflow đóng issue `setup-broken` đang mở, để phiên mở sau đó không dừng vì một issue cũ. Các lỗi này trước đây chỉ lộ ra ở cuối phiên, khi phát bản staging (L-059). Nhóm người test được kiểm bằng API của App Distribution với tài khoản dịch vụ ở mục 1.1; API không trả lời (ví dụ thiếu quyền) thì chỉ báo chú ý. Khi thành công, phần tóm tắt của workflow có đường dẫn tới phiên.
 4. Bị từ chối vì `main-guard` chưa chạy hay chưa có kết quả (`CHƯA KIỂM`, thường vì GitHub không phát sự kiện push sau một lần merge; cũng báo khi lần chạy thất bại mà không có issue `main-broken`, tức lỗi hạ tầng): **Actions > main-guard > Run workflow** trên `main`, **Re-run all jobs** trên lần chạy thất bại, hoặc lệnh mà thông báo in ra,, chờ xanh, rồi chạy lại `start-run`. Đang chạy (`CHỜ`): chờ xong rồi chạy lại.
 
 Lưu ý:
@@ -134,7 +137,12 @@ Thiết lập một lần:
 2. Mở trigger **API** của routine này, **Generate token**, thêm hai bí mật: `gh secret set ENV_CANARY_FIRE_URL` và `gh secret set ENV_CANARY_FIRE_TOKEN`.
 3. Chạy thử: **Actions > env-canary > Run workflow**. Mở phiên trong đường dẫn ở phần tóm tắt, xem script báo xanh.
 
-Cùng workflow có job thứ hai, `setup-check`, chạy `scripts/setup-check.sh --staging` trên máy của GitHub (không tốn hạn mức Claude, chạy cả khi có phiên đang chạy): file R2 bắt buộc, DNS, bí mật Firebase staging và nhóm người test, như `start-run` (mục 2.3). Đỏ thì mở issue `setup-broken` (hoặc thêm bình luận vào issue đang mở), xanh thì tự đóng. Nhãn riêng vì `scripts/env-canary.sh` đóng mọi issue `env-broken` khi môi trường cloud xanh.
+Cùng workflow có job thứ hai, `setup-check`, chạy `scripts/setup-check.sh --staging` trên máy của GitHub (không tốn hạn mức Claude, chạy cả khi có phiên đang chạy): file R2 bắt buộc, DNS, bí mật Firebase staging và nhóm người test, như `start-run` (mục 2.3). Đỏ thì kiểm lại một lần sau 90 giây; vẫn đỏ thì mở issue `setup-broken` (hoặc thêm bình luận vào issue đang mở), xanh thì tự đóng. Nhãn riêng vì `scripts/env-canary.sh` đóng mọi issue `env-broken` khi môi trường cloud xanh. Chạy tay chỉ phần này: **Actions > env-canary > Run workflow**, chọn **setup_only** (không kích hoạt routine, không tốn hạn mức Claude).
+
+**Catalog tư thế (camera-pose, plan T-065).** Kiểm tra trên gồm `scripts/setup-check-app.sh`: catalog mặc định của app (`DEFAULT_CATALOG_PATH`, bản staging của CI dùng nó) và mọi giá trị của khóa `poses_catalog_path` trên Remote Config staging, kể cả giá trị theo điều kiện; mỗi catalog phải trả `200`, đọc được, không đòi app mới hơn (giá trị chỉ theo điều kiện thì chỉ chú ý), và ảnh nhỏ cùng nét viền của mọi tư thế `starter` phải trả `200`. Một file trả 404 mà hỏi lại kèm tham số chống bộ nhớ đệm thì trả `200` nghĩa là file đã có nhưng Cloudflare còn giữ câu trả lời 404 cũ: xóa bộ nhớ đệm của URL đó (2026-10-27, một 404 được giữ với `max-age=31536000`).
+
+- **Đổi khóa `poses_catalog_path` hay tải catalog mới:** tải catalog và mọi file của nó lên R2 trước, rồi mới thêm hay đổi giá trị của khóa, kể cả giá trị theo điều kiện chưa bật (một giá trị trỏ tới file chưa có cũng làm kiểm tra đỏ và chặn mọi phiên). Xong thì chạy lại **env-canary** với **setup_only**, vì lần chạy buổi sáng không thấy thay đổi sau đó.
+- Không đọc được Remote Config (mạng, `firebase-tools` lỗi, quá 120 giây) chỉ là chú ý: catalog mặc định vẫn được kiểm. Thiếu bí mật hay tài khoản thiếu vai trò thì là lỗi. R2 không trả lời 3 file liền thì dừng phần catalog với một dòng lỗi.
 
 Khi có issue `env-broken`: đọc log trong issue. Thường là môi trường chưa dán setup script mới (dán lại theo mục 2.1); lỗi khác thì sửa nguyên nhân. Sau đó chạy lại **env-canary** bằng tay; lần chạy xanh tự đóng issue.
 
diff --git a/docs/template-sync.txt b/docs/template-sync.txt
index 9687064..82a8776 100644
--- a/docs/template-sync.txt
+++ b/docs/template-sync.txt
@@ -12,3 +12,16 @@ hold=gradle.properties 0eaeadbc2b04a94c81d9877aec4681b05a11587a
 # which the app does not have yet (ADR-1004). Take them with core/ads in a later sync, through an approved plan.
 hold=scripts/release.sh 0eaeadbc2b04a94c81d9877aec4681b05a11587a
 hold=docs/ops/release.md 0eaeadbc2b04a94c81d9877aec4681b05a11587a
+# Plan T-065 (pose catalog check) changed these template files; until the template takes the from-app proposal,
+# a sync must keep, when merging each file:
+#   scripts/setup-check.sh: http_status (retry 000, 429, 5xx), cdn_hint, the call of scripts/setup-check-app.sh at
+#     the end (SETUP_CHECK_APP_CHECKS);
+#   scripts/tests/run.sh: SETUP_CHECK_APP_CHECKS=none in setup_check(), and the block "camera-pose only (plan T-065)";
+#   .github/workflows/env-canary.yml: input setup_only and the if of job fire, REMOTE_CONFIG_CREDENTIALS, the second
+#     check after 90 seconds, persist-credentials: false and env -u GH_TOKEN around the script,
+#     timeout-minutes 15 of job setup-check;
+#   .github/workflows/start-run.yml: REMOTE_CONFIG_CREDENTIALS, persist-credentials: false and env -u GH_TOKEN around
+#     the script, issues: write and the step that closes setup-broken when green, timeout-minutes 10;
+#   docs/ops/staging-and-runs.md: sections 1.1 step 6, 1.3, 2.3, 2.4 (catalog).
+# scripts/setup-check-app.sh is the app's own file; a sync does not touch it. Without the REMOTE_CONFIG_CREDENTIALS
+# line the check only warns (the catalog of the key is not read); with the line and no secret it fails.
```

**Sau khi merge** (phiên làm task): chạy job `setup-check` trên nhánh chính một lần bằng đúng cách phiên gọi workflow (L-063): `gh api -X POST "repos/{owner}/{repo}/actions/workflows/env-canary.yml/dispatches" -f ref=main -f 'inputs[setup_only]=true'`, chờ bằng `scripts/wait-run.sh --workflow env-canary.yml --since <giờ>`. Với `setup_only`, job `fire` không chạy (không có ô này thì job `fire` chỉ bỏ qua khi nhánh báo cáo của phiên có danh sách chạy `running`).
- Xanh: ghi vào nhật ký.
- Bước đóng `setup-broken` của `start-run.yml` chạy thật lần đầu ở lần bắt đầu phiên kế tiếp bằng `start-run` (phiên không tự chạy `start-run`, vì nó kích hoạt một phiên mới); ghi vào Cần người kiểm tra: lần đầu có issue `setup-broken` mở mà `start-run` xanh, xem issue có bình luận và được đóng.
- Đỏ vì catalog hay file thật hỏng (dòng `LỖI - khóa poses_catalog_path trỏ tới catalog ...`, `LỖI - catalog mặc định ...`, `LỖI - file khởi đầu ...`, có hay không câu CDN): giữ issue `setup-broken`, ghi vào Cần người kiểm tra của báo cáo; đó là việc của người.
- Đỏ vì `000`, `429`, `5xx` hay `R2 không trả lời` còn lại sau lần kiểm lại: chạy lại một lần sau vài phút; xanh thì issue tự đóng; ghi vào nhật ký.
- Đỏ vì chính phần mới (thiếu bí mật dù bí mật có, lỗi quyền dù vai trò đúng, hay phân loại sai): hoàn tác bằng một PR thường, không phải đường `main-broken` (nhánh chính không đỏ): nhánh `claude/T-065-revert` từ nhánh chính, `git revert -m 1 <commit merge của task>`, đẩy bằng `scripts/save-branch.sh`, mở PR, chờ CI, `scripts/merge-if-green.sh`; rồi chạy lại job `setup-check` như trên để issue tự đóng; ghi lý do vào nhật ký và báo cáo.

### 3.2. Danh sách file

| Thao tác | Đường dẫn | Nội dung thay đổi |
| --- | --- | --- |
| Tạo | `scripts/setup-check-app.sh` | Phần kiểm catalog tư thế của app (mục 3.1) |
| Sửa | `scripts/setup-check.sh` | `http_status`, `cdn_hint`, gọi `scripts/setup-check-app.sh`, chú thích đầu file (mục 3.1) |
| Sửa | `scripts/tests/run.sh` | Khối `setup-check`: `SETUP_CHECK_APP_CHECKS=none` trong hàm `setup_check` của test có sẵn; khối mới "camera-pose only (plan T-065)" với các fake `curl`, `dig`, `gcloud`, `firebase` và 81 test (một test chỉ chạy khi máy có `timeout`) |
| Sửa | `.github/workflows/env-canary.yml` | Ô `setup_only`, `if` của job `fire`, biến `REMOTE_CONFIG_CREDENTIALS`, `persist-credentials: false`, `env -u GH_TOKEN` quanh script, kiểm lại sau 90 giây, `timeout-minutes: 15` của job `setup-check`, chú thích |
| Sửa | `.github/workflows/start-run.yml` | `issues: write`, biến `REMOTE_CONFIG_CREDENTIALS`, `persist-credentials: false`, `env -u GH_TOKEN` quanh script, bước mới đóng `setup-broken` khi xanh, `timeout-minutes: 10`, chú thích |
| Sửa | `docs/ops/staging-and-runs.md` | Mục 1.1, 1.3, 2.3, 2.4 (mục 3.1) |
| Sửa | `CLAUDE.md` | Dòng `setup-check.sh` ở mục Lệnh |
| Sửa | `docs/template-sync.txt` | Các thay đổi của plan này mà lần đồng bộ sau phải giữ (chú thích) |
| Sửa | `docs/features/quality-gate/tickets/T-065-setup-check-kiem-catalog-that.md` | Trạng thái |
| Sửa | `docs/features/quality-gate/plans/T-065-setup-check-kiem-catalog-that.md` | Nhật ký |

L-020, các file nhắc tới file mà plan sửa (`git grep -n 'setup-check'`, `env-canary.yml`, `start-run.yml`):

- `scripts/setup-check.sh` được `env-canary.yml`, `start-run.yml`, `scripts/tests/run.sh` (cả ba trong plan) gọi và được nhắc trong `docs/auto-run.md`, `docs/ops/setup-new-app.md`, `docs/ops/staging-and-runs.md`, `docs/CLAUDE.md`, `CLAUDE.md`, `docs/ops/required-assets.txt`. Cách gọi, mã thoát và dạng dòng (`ok   - `, `LỖI - `/`::error::`, `CHÚ Ý - `/`::warning::`, `== `) giữ nguyên, nên bước ghi issue của `env-canary.yml` (lọc `^(::error::|::warning::|ok   - )`) và các tài liệu không phải đổi; `docs/auto-run.md` mục 2 đọc nhãn `setup-broken`, không đổi. `setup-new-app.md` (của dự án mẫu) không nhắc catalog: file kiểm của app chỉ có ở app.
- `.github/workflows/env-canary.yml`, `start-run.yml` được nhắc trong chú thích và thông báo của `scripts/env-canary.sh`, `scripts/main-guard-status.sh`, `.github/workflows/resume-run.yml` và tài liệu; plan thêm một biến môi trường, đổi `timeout-minutes`, thêm ô `setup_only` (mặc định `false`), điều kiện `if` của job `fire` (lịch buổi sáng vẫn chạy) và một bước mới của `start-run.yml` (đóng `setup-broken`); không đổi tên workflow, job hay đầu vào có sẵn. `scripts/env-canary.sh` và `resume-run.yml` không gọi `env-canary.yml` theo đầu vào, `main-guard-status.sh` không đọc các bước, nên các file đó không bị ảnh hưởng.
- `scripts/tests/run.sh` được `scripts/check.sh` chạy và `.github/workflows/pr-check.yml` nhắc tới (bộ lọc đường dẫn và bước chạy test của script); plan chỉ sửa khối `setup-check` và thêm một khối, không đổi tên hay cách chạy, nên `pr-check.yml` không bị ảnh hưởng.
- `docs/ops/staging-and-runs.md` được nhắc theo số mục trong thông báo hay chú thích của `.github/workflows/device-session.yml`, `.github/workflows/staging.yml`, `scripts/cloud-env-check.sh` và nhiều tài liệu; plan chỉ thêm bước, dòng và đoạn vào mục 1.1, 1.3, 2.3, 2.4, không đổi số hay tên mục, nên các file đó không bị ảnh hưởng.
- `CLAUDE.md` được nhắc trong chú thích của `app/src/androidTest/kotlin/com/vglstudio/camera/pose/line/guide/AppLaunchTest.kt` và `app/src/androidTest/kotlin/com/vglstudio/camera/pose/line/guide/SettingsNavigationTest.kt` (bước 10, quy tắc test trên emulator); plan chỉ sửa một dòng ở mục Lệnh, không đổi quy tắc test, nên hai test đó không bị ảnh hưởng.

- `docs/template-sync.txt` được `scripts/template-sync.sh` của dự án mẫu đọc (dòng `base=`, `package=`, `hold=`; dòng bắt đầu bằng `#` là chú thích, như chú thích của `hold=scripts/release.sh` đã có) và được nhắc trong `docs/adr/1004-chua-ap-dung-quang-cao-adskit.md`; plan chỉ thêm dòng chú thích, không đổi dòng nào script đọc, nên ADR-1004 và lần đồng bộ không bị ảnh hưởng.
- Quyền `issues: write` của `start-run.yml` chỉ dùng cho bước đóng `setup-broken`; các bước khác chỉ đọc issue như trước.

### 3.3. Cờ tính năng, quảng cáo, dữ liệu, quyền, thư viện

- Cờ tính năng: Không có
- Quảng cáo: Không chạm
- Sự kiện phân tích: Không chạm
- File R2: chỉ đọc (`HEAD`, `GET`); không đổi cách đặt tên hay đường dẫn. Tham số `?setup-check-nocache=` chỉ để hỏi lại, không ghi gì
- **Quyền truy cập:** Không thêm (không đổi app). Workflow: `start-run.yml` từ `issues: read` lên `issues: write` (đóng `setup-broken`)
- **Thư viện và SDK:** Không thêm vào app. Trên máy của GitHub: `firebase-tools@15.32.1` qua `npx` (cùng phiên bản bản 1 đã chạy thử thật và `preflight.sh` cũ đã dùng), `jq`, `curl`, `timeout` có sẵn trên runner Ubuntu. `npx` kéo cả gói phụ thuộc không ghim phiên bản; job có `issues: write` (một gói lạ có token thì đóng được cả issue `main-broken`), nên token GitHub không ở cạnh lệnh đó: `persist-credentials: false` (không có token trong `.git/config`) và `env -u GH_TOKEN -u GITHUB_TOKEN` quanh script; việc đóng issue ở bước riêng (lần thử 3: "token in env" rỗng, "token in git config: no")

### 3.4. Phương án dự phòng

- **Điểm chưa chắc 1:** chữ thật của lỗi khi tài khoản thiếu vai trò. Lần thử ở bản 1 (project không tồn tại) cho `Error: Failed to get Firebase Remote Config template for project <id>.` ở stdout, không khớp mẫu `LỖI`. Nếu lỗi thiếu vai trò cũng có dạng đó thì tài khoản mất vai trò sau này chỉ ra `CHÚ Ý` (catalog mặc định vẫn được kiểm, phần Remote Config tắt lặng lẽ). Không có cách chuyển trong phiên; câu hỏi 6.
- **Điểm chưa chắc 2:** thời gian xấu nhất. Đã chặn: R2 không trả lời được 3 file liền thì dừng (khoảng 135 giây), phần catalog quá 240 giây thì dừng với `CHÚ Ý`; một lần chạy xấu nhất khoảng 300 giây (kiểm tra của mẫu, `remoteconfig:get` tới 130 giây, phần catalog), nên `start-run` (một lần) dưới 10 phút, `env-canary` (hai lần và 90 giây, khoảng 690 giây) dưới 15 phút. **Điều kiện chuyển:** lần chạy sau khi merge có job hết giờ. **Cách làm thay:** không đổi trong phiên; plan phiên bản 3 có người duyệt giảm ngân sách hay dùng `curl --parallel`. Tiêu chí và test của mục 4 giữ nguyên.
- **Điểm chưa chắc 3:** một CDN khác Cloudflare có thể coi tham số là cùng một địa chỉ (không bỏ qua bộ nhớ đệm): khi đó không có câu CDN, dòng `LỖI` vẫn đúng như hôm nay. Không cần làm thay.

## 4. Kiểm thử

Mọi test nằm trong `scripts/tests/run.sh`, chạy bằng `scripts/check.sh`. Bản đầy đủ của test ở `diff` dưới. Đường dẫn catalog mặc định và `APP_SCHEMA` trong test đọc từ `PosesSettings.kt`, `CatalogParser.kt` bằng cùng lệnh `sed` của script (`minAppSchema` "mới hơn" là `APP_SCHEMA + 1`).

| Tiêu chí chấp nhận | Loại test | Tên test hoặc cách kiểm tra |
| --- | --- | --- |
| Catalog mặc định | Test script | `the default catalog and its starter files pass`, `only the starter files are checked`, `a missing starter file of the default catalog stops`, `names the missing starter file`, `a default catalog that answers 404 stops`, `a catalog without starter poses warns`, `without --staging Remote Config is not read` (đối chứng) |
| CDN giữ 404 cũ | Test script | `a starter file the CDN still answers 404 for stops`, `says the CDN keeps an old answer`, `a 403 the CDN keeps gets the CDN hint`, `a required file the CDN still answers 404 for names the cache`, `a 503 gets no CDN hint`, `a file really missing gets no CDN hint` (đối chứng) |
| Catalog dùng được | Test script | `a catalog that is not JSON stops`, `a catalog cut short is downloaded once more`, `a catalog that answers 404 when downloaded again reports the 404`, `a catalog for a newer app stops`, `the default value pointing to a catalog for a newer app stops`, `a conditional-only catalog for a newer app only warns`, `a pose that is not an object does not hide the starter files`, `a preview that is not an object checks the outline only`, `no ok line for a catalog with a missing starter file` (đối chứng) |
| Lỗi tạm và R2 treo | Test script | `a 503 that passes on the retry is not a failure`, `a 502 that passes on the retry is not a failure`, `a 429 that passes on the retry is not a failure`, `a 429 seen twice counts`, `a required file that fails to connect once is asked again`, `R2 that stops answering ends the catalog part with one line` (một dòng `LỖI - R2 không trả lời`, chỉ 3 file được hỏi), `5xx in a row also stops the catalog part`, `files that answer between silent ones do not count as R2 down` (`000`, `200`, `000`, `200`, `000`: không dừng, cả 5 file được hỏi), `the catalog part stops with a note when its time is up` |
| Sự cố ngày 26, mọi loại giá trị | Test script | `a Remote Config catalog that answers 404 stops`, `names the key and the missing catalog`, `a conditional value that answers 404 stops`, `a rollout value that answers 404 stops`, `a key inside a parameter group is read`, `a Remote Config catalog that exists passes` (đối chứng), `the key's catalog and the default catalog are both downloaded once` |
| Như app | Test script | `no parameter means the default catalog only, without a note`, `useInAppDefault points the key to the default catalog, without a note`, `an invalid value warns and the key falls back to the default catalog, as the app does`, `a personalization value warns`, `no readable value warns and checks the default catalog`, `a path used by two values is checked once` |
| Thiếu bí mật, thiếu biến, thiếu vai trò, công cụ lỗi | Test script | `a missing Remote Config secret stops` (biến rỗng), `a workflow that does not pass the Remote Config variable only warns` (biến không có), `without the secret the default catalog is still checked`, `a Remote Config account without the role stops`, `a missing permissions error printed on stdout stops`, `a transient error with 403 inside a number only warns`, `a Remote Config command that cannot run only warns and checks the default catalog` (chữ lỗi thật của bản 1), `a Remote Config command that hits the time limit only warns`, `a Remote Config command that hangs is stopped and only warns` (khi máy có `timeout`), `a template that is not JSON only warns`, `a Firebase secret without project id only warns for Remote Config` |
| Đúng project, đúng bí mật | Test script | `the staging project is passed to remoteconfig:get`, `remoteconfig:get uses the Remote Config secret` |
| Đường đỏ cho lần thử | Test script | `the extra catalog of a trial run is checked` |
| Không lộ bí mật | Test script | `never prints secret values` (khóa `RC-MARKER-1` trong bí mật và trong lỗi giả, mã project `demo-staging`, token `fake-token`) |
| Móc của app | Test script | `sources the app checks file and counts its problems`; test có sẵn của `setup-check` chạy với `SETUP_CHECK_APP_CHECKS=none`, không đổi kỳ vọng |
| Quy tắc đường dẫn giống `AssetPath` | Test script | 7 test `... is a valid asset path`, 14 test `... is not a valid asset path` |
| Kiểm lại sau 90 giây ở `env-canary` | Kiểm tra thủ công (người: workflow, không có test; chạy trên GitHub) | Đã chạy thử trên nhánh tạm (mục 4.1, lần thử 2): đường đỏ in "kiểm lại một lần sau 90 giây", chạy lần hai, mã 1; đường xanh chạy một lần |
| `start-run` xanh đóng `setup-broken` | Kiểm tra thủ công (người: chỉ thấy khi bắt đầu một phiên bằng `start-run.yml` lúc có issue mở) | Lần đầu `start-run` chạy khi có issue `setup-broken` mở: issue có bình luận "Kiểm tra thiết lập của start-run xanh ..." và đóng. Phần gọi `gh api` giống hệt bước của `env-canary.yml` |
| Workflow đọc được Remote Config thật | Kiểm tra thủ công (người: cần bí mật staging, chạy trên GitHub; môi trường cloud bị proxy chặn R2, L-005) | Trước khi duyệt: đã chạy trên nhánh tạm (mục 4.1). Sau khi merge: một lần chạy xanh job `setup-check` trên nhánh chính với `setup_only` (mục 3.1, Sau khi merge); không chạy đường đỏ trên nhánh chính |

**Ngoại lệ viết test trước:** hai workflow, `docs/ops/staging-and-runs.md`, `CLAUDE.md`, `docs/template-sync.txt` (không có test; kiểm bằng lần chạy thủ công ở bảng trên).

Test thêm vào `scripts/tests/run.sh` (đã chạy, mục 4.1):

```diff
diff --git a/scripts/tests/run.sh b/scripts/tests/run.sh
index 14c21b9..d56ffa5 100755
--- a/scripts/tests/run.sh
+++ b/scripts/tests/run.sh
@@ -2003,7 +2003,7 @@ setup_check() { # <codes> [args...]; prints the exit code, keeps the output in $
   printf '%b' "$1" > "$work/setup/codes"; shift
   local code=0
   PATH="$work/setup-bin:$PATH" SETUP_FAKE="$work/setup" SETUP_CHECK_ASSETS_LIST="$work/setup/assets.txt" \
-    SETUP_CHECK_ASSETS_BASE_URL="$base" SETUP_CHECK_PACKAGE=com.demo GITHUB_ACTIONS='' \
+    SETUP_CHECK_ASSETS_BASE_URL="$base" SETUP_CHECK_PACKAGE=com.demo SETUP_CHECK_APP_CHECKS=none GITHUB_ACTIONS='' \
     scripts/setup-check.sh "$@" > "$work/setup.out" 2>&1 || code=$?
   echo "$code"
 }
@@ -2043,6 +2043,263 @@ expect "setup-check: rejects an unknown argument" 1 "$(setup_check "$all_files"
 expect "setup-check: reads the staging address of this repository" 1 \
   "$(grep -cE '"ASSETS_BASE_URL", *"\\"https://[^"]+/\\""' app/build.gradle.kts)"
 
+# camera-pose only (plan T-065): scripts/setup-check-app.sh, sourced at the end of setup-check.sh. The pose catalog
+# that devices download (the default one, and every value of poses_catalog_path on Remote Config staging) and its
+# starter files. curl, dig, gcloud and firebase-tools are fakes driven by files in $sca.
+sca="$work/sca"
+mkdir -p "$work/sca-bin"
+cat > "$work/sca-bin/curl" << 'SH'
+#!/usr/bin/env bash
+# Fake curl. The URL is the last argument; its key is the path with / as _. The code is the first line of
+# $SCA/codes/<key>, consumed one per call (the last line stays); default 200. A cache-busting query
+# (?setup-check-nocache=) reads $SCA/nocache/<key> instead, default 404. With -o <file> and a 200 the body is
+# $SCA/body/<key> when it exists. Every call is logged as "<GET|HEAD> <URL without query>" in $SCA/calls.
+url="${*: -1}"; plain="${url%%\?*}"; key="$(sed -E 's#^https?://[^/]+/##; s#/#_#g' <<< "$plain")"
+out=""; prev=""; method=GET
+for a in "$@"; do [[ "$prev" == "-o" ]] && out="$a"; [[ "$a" == -I ]] && method=HEAD; prev="$a"; done
+if [[ "$url" == *"?setup-check-nocache="* ]]; then
+  code="$(cat "$SCA/nocache/$key" 2> /dev/null || echo 404)"
+else
+  f="$SCA/codes/$key"
+  if [[ -s "$f" ]]; then code="$(head -n 1 "$f")"; [[ "$(wc -l < "$f")" -gt 1 ]] && sed -i.bak 1d "$f" && rm -f "$f.bak"; else code=200; fi
+  echo "$method $plain" >> "$SCA/calls"
+fi
+if [[ "$code" == 200 && -n "$out" && "$out" != /dev/null && -f "$SCA/body/$key" ]]; then cp "$SCA/body/$key" "$out"; fi
+printf '%s' "$code"
+SH
+cat > "$work/sca-bin/dig" << 'SH'
+#!/usr/bin/env bash
+echo 104.21.40.144
+SH
+cat > "$work/sca-bin/gcloud" << 'SH'
+#!/usr/bin/env bash
+[[ "$*" == *print-access-token* ]] && echo fake-token
+exit 0
+SH
+cat > "$work/sca-bin/firebase" << 'SH'
+#!/usr/bin/env bash
+# Fake firebase-tools remoteconfig:get: logs its arguments and whether the credentials file is the Remote Config
+# secret to $SCA/rc.args, prints $SCA/rc-out on stdout and $SCA/rc-err on stderr, sleeps $SCA/rc-sleep seconds,
+# exits with $SCA/rc-exit (default 0) and on 0 copies $SCA/remote-config.json to the -o file.
+gac=no; grep -q RC-MARKER-1 "${GOOGLE_APPLICATION_CREDENTIALS:-/dev/null}" 2> /dev/null && gac=yes
+echo "GAC=$gac $*" >> "$SCA/rc.args"
+out=""; prev=""; for a in "$@"; do [[ "$prev" == "-o" ]] && out="$a"; prev="$a"; done
+cat "$SCA/rc-out" 2> /dev/null || true
+cat "$SCA/rc-err" >&2 2> /dev/null || true
+[[ -f "$SCA/rc-sleep" ]] && sleep "$(cat "$SCA/rc-sleep")"
+code="$(cat "$SCA/rc-exit" 2> /dev/null || echo 0)"
+[[ "$code" == 0 && -f "$SCA/remote-config.json" ]] && cp "$SCA/remote-config.json" "$out"
+exit "$code"
+SH
+chmod +x "$work/sca-bin/"*
+# The default path and the schema come from the app code with the same sed as the script, so a later catalog task
+# that changes them does not turn these tests red.
+poses_kt=core/poses/src/main/kotlin/com/vglstudio/camera/pose/line/guide/core/poses/internal/PosesSettings.kt
+parser_kt=core/poses/src/main/kotlin/com/vglstudio/camera/pose/line/guide/core/poses/internal/CatalogParser.kt
+v1="$(sed -nE 's/.*DEFAULT_CATALOG_PATH = "([^"]+)".*/\1/p' "$poses_kt" | head -n 1)"
+v1key="${v1//\//_}"
+newer_schema=$(( $(sed -nE 's/.*APP_SCHEMA = ([0-9]+).*/\1/p' "$parser_kt" | head -n 1) + 1 ))
+v3=poses/catalog/v3/catalog-c2e0ce0d.json
+v3key=poses_catalog_v3_catalog-c2e0ce0d.json
+sca_catalog() { # [minAppSchema]: one starter pose and one other pose
+  printf '{"schema": 1, "minAppSchema": %s, "poses": [' "${1:-1}"
+  printf '{"id": "w01", "starter": true, "preview": {"small": "poses/img/v1/w01/preview_540.webp", "large": "poses/img/v1/w01/preview_1200.webp"}, "outline": "poses/img/v1/w01/outline.webp"},'
+  printf '{"id": "w02", "starter": false, "preview": {"small": "poses/img/v1/w02/preview_540.webp"}, "outline": "poses/img/v1/w02/outline.webp"}]}'
+}
+sca_rc() { # <parameter JSON of poses_catalog_path, or "" for no parameter>
+  if [[ -n "$1" ]]; then printf '{"parameters": {"poses_catalog_path": %s}, "conditions": []}' "$1" > "$sca/remote-config.json"
+  else echo '{"parameters": {"other_key": {"defaultValue": {"value": "x"}}}}' > "$sca/remote-config.json"; fi
+}
+sca_reset() { # a ready setup; a test then breaks one thing
+  rm -rf "$sca"; mkdir -p "$sca/codes" "$sca/nocache" "$sca/body"
+  printf '# Required files\n%s\n' "$v1" > "$sca/assets.txt"
+  sca_catalog > "$sca/body/$v1key"
+  sca_catalog > "$sca/body/$v3key"
+  sca_rc "{\"defaultValue\": {\"value\": \"$v1\"}}"
+}
+sca_rc_secret='{"type": "service_account", "private_key": "RC-MARKER-1"}'
+sca_json='{"project_info": {"project_id": "demo-staging", "project_number": "123"}, "client": [{"client_info": {"android_client_info": {"package_name": "com.demo.staging"}}}]}'
+sca_check() { # [args...]; prints the exit code, keeps the output in $work/sca.out
+  local code=0
+  PATH="$work/sca-bin:$PATH" SCA="$sca" SETUP_CHECK_ASSETS_LIST="$sca/assets.txt" \
+    SETUP_CHECK_ASSETS_BASE_URL="https://cdn.example.com/" SETUP_CHECK_PACKAGE=com.demo \
+    SETUP_CHECK_FIREBASE_CMD="$work/sca-bin/firebase" SETUP_CHECK_RETRY_SECONDS=0 GITHUB_ACTIONS='' \
+    GOOGLE_SERVICES="${SCA_GOOGLE-$sca_json}" DISTRIBUTION_CREDENTIALS='{"type": "service_account"}' \
+    REMOTE_CONFIG_CREDENTIALS="${SCA_RC-$sca_rc_secret}" \
+    bash -c '[ -z "${SCA_RC_UNSET:-}" ] || unset REMOTE_CONFIG_CREDENTIALS; exec scripts/setup-check.sh "$@"' _ "$@" \
+    > "$work/sca.out" 2>&1 || code=$?
+  cat "$work/sca.out" >> "$work/sca-all.out"
+  echo "$code"
+}
+sca_lines() { grep -c -- "$1" "$work/sca.out" || true; }
+sca_calls() { grep -c -- "$1" "$sca/calls" 2> /dev/null || true; }
+
+# The default catalog, also without --staging (a developer machine, setup-new-app.md).
+sca_reset
+expect "setup-check app: the default catalog and its starter files pass" "0 1" \
+  "$(sca_check) $(sca_lines "^ok   - catalog mặc định của app $v1 và 2 file khởi đầu trả 200")"
+expect "setup-check app: only the starter files are checked" "1 1 0" \
+  "$(sca_calls '^HEAD .*/w01/preview_540.webp$') $(sca_calls '^HEAD .*/w01/outline.webp$') $(sca_calls '/w02/')"
+expect "setup-check app: without --staging Remote Config is not read" 0 "$(cat "$sca/rc.args" 2> /dev/null | grep -c . || true)"
+sca_reset; echo 404 > "$sca/codes/poses_img_v1_w01_outline.webp"
+expect "setup-check app: a missing starter file of the default catalog stops" 1 "$(sca_check)"
+expect "setup-check app: names the missing starter file" 1 \
+  "$(sca_lines "^LỖI - file khởi đầu poses/img/v1/w01/outline.webp của catalog $v1 trả '404'")"
+expect "setup-check app: no ok line for a catalog with a missing starter file" 0 "$(sca_lines '^ok   - catalog ')"
+expect "setup-check app: a file really missing gets no CDN hint" 0 "$(sca_lines 'CDN')"
+sca_reset; echo 404 > "$sca/codes/poses_img_v1_w01_outline.webp"; echo 200 > "$sca/nocache/poses_img_v1_w01_outline.webp"
+expect "setup-check app: a starter file the CDN still answers 404 for stops" 1 "$(sca_check)"
+expect "setup-check app: says the CDN keeps an old answer" 1 "$(sca_lines '^LỖI - file khởi đầu .*CDN còn giữ câu trả lời cũ')"
+sca_reset; printf '404\n' > "$sca/codes/$v1key"
+expect "setup-check app: a default catalog that answers 404 stops" "1 1" \
+  "$(sca_check) $(sca_lines "^LỖI - catalog mặc định của app $v1 trả '404'")"
+sca_reset; echo 'not a catalog' > "$sca/body/$v1key"
+expect "setup-check app: a catalog that is not JSON stops" "1 1" \
+  "$(sca_check) $(sca_lines "^LỖI - catalog mặc định của app $v1 không phải catalog đọc được")"
+sca_reset; echo '{"poses": ' > "$sca/body/$v1key"
+expect "setup-check app: a catalog cut short is downloaded once more" "1 2" "$(sca_check) $(sca_calls "^GET .*/$v1\$")"
+sca_reset; sca_catalog "$newer_schema" > "$sca/body/$v1key"
+expect "setup-check app: a catalog for a newer app stops" "1 1" "$(sca_check) $(sca_lines '^LỖI - catalog .*cần app mới hơn')"
+sca_reset; printf '{"schema": 1, "minAppSchema": 1, "poses": ["broken", {"id": "w01", "starter": true, "preview": {"small": "poses/img/v1/w01/preview_540.webp"}, "outline": "poses/img/v1/w01/outline.webp"}]}' > "$sca/body/$v1key"
+expect "setup-check app: a pose that is not an object does not hide the starter files" "0 1" \
+  "$(sca_check) $(sca_lines '^ok   - catalog .* và 2 file khởi đầu')"
+sca_reset; printf '503\n200\n' > "$sca/codes/poses_img_v1_w01_outline.webp"
+expect "setup-check app: a 503 that passes on the retry is not a failure" 0 "$(sca_check)"
+sca_reset; printf '429\n200\n' > "$sca/codes/$v1key"
+expect "setup-check: a 429 that passes on the retry is not a failure" 0 "$(sca_check)"
+sca_reset; printf '429\n429\n200\n' > "$sca/codes/$v1key"
+expect "setup-check app: a 429 seen twice counts" 1 "$(sca_check)"
+sca_reset; printf '000\n200\n' > "$sca/codes/$v1key"
+expect "setup-check: a required file that fails to connect once is asked again" "0 2" \
+  "$(sca_check) $(sca_calls "^HEAD .*/$v1\$")"
+sca_reset; echo 404 > "$sca/codes/$v1key"; echo 200 > "$sca/nocache/$v1key"
+expect "setup-check: a required file the CDN still answers 404 for names the cache" 1 \
+  "$(sca_check > /dev/null; sca_lines "^LỖI - https://cdn.example.com/$v1 trả '404', cần 200.*CDN còn giữ")"
+
+# With --staging: every value of poses_catalog_path on Remote Config staging.
+sca_reset; sca_rc "{\"defaultValue\": {\"value\": \"$v3\"}}"; echo 404 > "$sca/codes/$v3key"
+expect "setup-check app: a Remote Config catalog that answers 404 stops" 1 "$(sca_check --staging)"
+expect "setup-check app: names the key and the missing catalog" 1 \
+  "$(sca_lines "^LỖI - khóa poses_catalog_path trỏ tới catalog $v3 trả '404'")"
+sca_reset; sca_rc "{\"defaultValue\": {\"value\": \"$v3\"}}"
+expect "setup-check app: a Remote Config catalog that exists passes" 0 "$(sca_check --staging)"
+expect "setup-check app: the key's catalog and the default catalog are both downloaded once" "1 1" \
+  "$(sca_calls "^GET .*/$v3\$") $(sca_calls "^GET .*/$v1\$")"
+expect "setup-check app: the staging project is passed to remoteconfig:get" 1 "$(grep -c -- '--project demo-staging' "$sca/rc.args")"
+expect "setup-check app: remoteconfig:get uses the Remote Config secret" 1 "$(grep -c '^GAC=yes ' "$sca/rc.args")"
+sca_reset; sca_rc "{\"defaultValue\": {\"value\": \"$v1\"}, \"conditionalValues\": {\"beta\": {\"value\": \"$v3\"}}}"; echo 404 > "$sca/codes/$v3key"
+expect "setup-check app: a conditional value that answers 404 stops" "1 1" \
+  "$(sca_check --staging) $(sca_lines "^LỖI - khóa poses_catalog_path trỏ tới catalog $v3")"
+sca_reset; sca_rc "{\"defaultValue\": {\"value\": \"$v1\"}, \"conditionalValues\": {\"half\": {\"rolloutValue\": {\"rolloutId\": \"r1\", \"value\": \"$v3\"}}}}"; echo 404 > "$sca/codes/$v3key"
+expect "setup-check app: a rollout value that answers 404 stops" "1 1" \
+  "$(sca_check --staging) $(sca_lines "^LỖI - khóa poses_catalog_path trỏ tới catalog $v3")"
+sca_reset; printf '{"parameterGroups": {"poses": {"parameters": {"poses_catalog_path": {"defaultValue": {"value": "%s"}}}}}}' "$v3" > "$sca/remote-config.json"; echo 404 > "$sca/codes/$v3key"
+expect "setup-check app: a key inside a parameter group is read" 1 "$(sca_check --staging)"
+sca_reset; sca_rc ""
+expect "setup-check app: no parameter means the default catalog only, without a note" "0 1 0 0" \
+  "$(sca_check --staging) $(sca_calls "^GET .*/$v1\$") $(sca_calls "^GET .*/v3/") $(sca_lines '^CHÚ Ý - ')"
+sca_reset; sca_rc '{"defaultValue": {"useInAppDefault": true}}'; echo 404 > "$sca/codes/$v1key"
+expect "setup-check app: useInAppDefault points the key to the default catalog, without a note" "1 0" \
+  "$(sca_check --staging > /dev/null; sca_lines "^LỖI - khóa poses_catalog_path trỏ tới catalog $v1") $(sca_lines '^CHÚ Ý - ')"
+sca_reset; sca_rc "{\"defaultValue\": {\"value\": \"$v3\"}, \"conditionalValues\": {\"old\": {\"value\": \"poses/catalog/latest.json\"}}}"
+expect "setup-check app: an invalid value warns and the key falls back to the default catalog, as the app does" "0 1 1 1" \
+  "$(sca_check --staging) $(sca_lines '^CHÚ Ý - khóa poses_catalog_path có giá trị "poses/catalog/latest.json" không hợp lệ') $(sca_calls "^GET .*/$v1\$") $(sca_lines "^ok   - khóa poses_catalog_path trỏ tới catalog $v1 ")"
+sca_reset; sca_rc "{\"defaultValue\": {\"value\": \"$v1\"}, \"conditionalValues\": {\"p\": {\"personalizationValue\": {\"personalizationId\": \"x\"}}}}"
+expect "setup-check app: a personalization value warns" "0 1" "$(sca_check --staging) $(sca_lines '^CHÚ Ý - .*cá nhân hóa')"
+sca_reset; sca_rc '{"defaultValue": {"personalizationValue": {}}}'
+expect "setup-check app: no readable value warns and checks the default catalog" "0 1 1" \
+  "$(sca_check --staging) $(sca_lines '^CHÚ Ý - không đọc được giá trị nào') $(sca_calls "^GET .*/$v1\$")"
+sca_reset; sca_rc "{\"defaultValue\": {\"value\": \"$v1\"}, \"conditionalValues\": {\"a\": {\"value\": \"$v1\"}}}"
+expect "setup-check app: a path used by two values is checked once" "0 1 1" \
+  "$(sca_check --staging) $(sca_calls "^GET .*/$v1\$") $(sca_lines '^ok   - khóa poses_catalog_path trỏ tới catalog ')"
+sca_reset
+expect "setup-check app: a missing Remote Config secret stops" "1 1" \
+  "$(SCA_RC='' sca_check --staging) $(sca_lines '^LỖI - thiếu bí mật STAGING_REMOTE_CONFIG_CREDENTIALS')"
+expect "setup-check app: without the secret the default catalog is still checked" 1 "$(sca_calls "^GET .*/$v1\$")"
+sca_reset; echo 1 > "$sca/rc-exit"; echo 'Error: Request had HTTP Error: 403, PERMISSION_DENIED RC-MARKER-1' > "$sca/rc-err"
+expect "setup-check app: a Remote Config account without the role stops" "1 1" \
+  "$(sca_check --staging) $(sca_lines '^LỖI - .*Firebase Remote Config Viewer, hoặc API Remote Config chưa bật')"
+sca_reset; echo 1 > "$sca/rc-exit"; echo 'Error: Authorization failed. This account is missing the following required permissions on project x' > "$sca/rc-out"
+expect "setup-check app: a missing permissions error printed on stdout stops" "1 1" \
+  "$(sca_check --staging) $(sca_lines '^LỖI - .*Firebase Remote Config Viewer')"
+sca_reset; echo 1 > "$sca/rc-exit"; echo 'Error: Server Error. request id 84031, connect ETIMEDOUT' > "$sca/rc-out"
+expect "setup-check app: a transient error with 403 inside a number only warns" "0 1" \
+  "$(sca_check --staging) $(sca_lines '^CHÚ Ý - không đọc được khóa poses_catalog_path (lệnh firebase-tools')"
+sca_reset; echo 1 > "$sca/rc-exit"; echo 'Error: Failed to get Firebase Remote Config template for project x.' > "$sca/rc-out"
+expect "setup-check app: a Remote Config command that cannot run only warns and checks the default catalog" "0 1 1" \
+  "$(sca_check --staging) $(sca_lines '^CHÚ Ý - không đọc được khóa poses_catalog_path (lệnh firebase-tools') $(sca_calls "^GET .*/$v1\$")"
+sca_reset; echo 124 > "$sca/rc-exit"
+expect "setup-check app: a Remote Config command that hits the time limit only warns" "0 1" \
+  "$(sca_check --staging) $(sca_lines '^CHÚ Ý - không đọc được khóa poses_catalog_path (lệnh firebase-tools')"
+if command -v timeout > /dev/null; then
+  sca_reset; echo 5 > "$sca/rc-sleep"
+  expect "setup-check app: a Remote Config command that hangs is stopped and only warns" "0 1" \
+    "$(SETUP_CHECK_RC_TIMEOUT=1 sca_check --staging) $(sca_lines '^CHÚ Ý - không đọc được khóa poses_catalog_path (lệnh firebase-tools')"
+fi
+sca_reset; echo 'not json' > "$sca/remote-config.json"
+expect "setup-check app: a template that is not JSON only warns" "0 1" \
+  "$(sca_check --staging) $(sca_lines '^CHÚ Ý - không đọc được khóa poses_catalog_path (lệnh firebase-tools')"
+sca_reset
+expect "setup-check app: a Firebase secret without project id only warns for Remote Config" "0 1" \
+  "$(SCA_GOOGLE="$(jq -c 'del(.project_info.project_id)' <<< "$sca_json")" sca_check --staging) $(sca_lines '^CHÚ Ý - không đọc được khóa poses_catalog_path: chưa có google-services.json')"
+sca_reset; echo 404 > "$sca/codes/poses_catalog_v9_catalog-00000000.json"
+expect "setup-check app: the extra catalog of a trial run is checked" "1 1" \
+  "$(SETUP_CHECK_EXTRA_CATALOG=poses/catalog/v9/catalog-00000000.json sca_check --staging) $(sca_lines '^LỖI - khóa poses_catalog_path trỏ tới catalog poses/catalog/v9/catalog-00000000.json')"
+sca_reset; sca_rc "{\"defaultValue\": {\"value\": \"$v1\"}, \"conditionalValues\": {\"new_app\": {\"value\": \"$v3\"}}}"; sca_catalog "$newer_schema" > "$sca/body/$v3key"
+expect "setup-check app: a conditional-only catalog for a newer app only warns" "0 1" \
+  "$(sca_check --staging) $(sca_lines "^CHÚ Ý - khóa poses_catalog_path trỏ tới catalog $v3 (chỉ giá trị theo điều kiện dùng) cần app mới hơn")"
+sca_reset; sca_rc "{\"defaultValue\": {\"value\": \"$v3\"}}"; sca_catalog "$newer_schema" > "$sca/body/$v3key"
+expect "setup-check app: the default value pointing to a catalog for a newer app stops" "1 1" \
+  "$(sca_check --staging) $(sca_lines "^LỖI - khóa poses_catalog_path trỏ tới catalog $v3 cần app mới hơn")"
+sca_reset
+expect "setup-check app: a workflow that does not pass the Remote Config variable only warns" "0 1 1" \
+  "$(SCA_RC_UNSET=1 sca_check --staging) $(sca_lines '^CHÚ Ý - .*workflow không truyền biến REMOTE_CONFIG_CREDENTIALS') $(sca_calls "^GET .*/$v1\$")"
+sca_reset; printf '{"schema": 1, "minAppSchema": 1, "poses": [{"id": "a", "starter": true, "preview": "x", "outline": "poses/img/v1/a/outline.webp"}]}' > "$sca/body/$v1key"
+expect "setup-check app: a preview that is not an object checks the outline only" "0 1 0" \
+  "$(sca_check) $(sca_lines '^ok   - catalog .* và 1 file khởi đầu') $(sca_calls '^HEAD .*/x$')"
+sca_reset; printf '{"schema": 1, "minAppSchema": 1, "poses": [{"id": "a", "starter": false, "outline": "poses/img/v1/a/outline.webp"}]}' > "$sca/body/$v1key"
+expect "setup-check app: a catalog without starter poses warns" "0 1 0" \
+  "$(sca_check) $(sca_lines '^CHÚ Ý - catalog mặc định của app .* không có tư thế starter nào') $(sca_lines '^ok   - catalog ')"
+sca_reset; printf '503\n503\n' > "$sca/codes/poses_img_v1_w01_outline.webp"; echo 200 > "$sca/nocache/poses_img_v1_w01_outline.webp"
+expect "setup-check app: a 503 gets no CDN hint" "1 0" "$(sca_check) $(sca_lines 'CDN')"
+sca_reset; printf '{"schema": 1, "minAppSchema": 1, "poses": [' > "$sca/body/$v1key"
+for i in 1 2 3 4; do printf '{"id": "p%s", "starter": true, "outline": "poses/img/v1/p%s/outline.webp"},' "$i" "$i" >> "$sca/body/$v1key"; printf '000\n' > "$sca/codes/poses_img_v1_p${i}_outline.webp"; done
+printf '{"id": "z", "starter": false}]}' >> "$sca/body/$v1key"
+expect "setup-check app: R2 that stops answering ends the catalog part with one line" "1 1 3" \
+  "$(sca_check) $(sca_lines '^LỖI - R2 không trả lời') $(sca_calls '^HEAD .*/p[0-9]/outline.webp$' | awk '{print $1 / 2}')"
+sca_starters() { # <n>: a default catalog with n starter poses p1..pn, outline only
+  local i
+  printf '{"schema": 1, "minAppSchema": 1, "poses": [' > "$sca/body/$v1key"
+  for i in $(seq 1 "$1"); do printf '{"id": "p%s", "starter": true, "outline": "poses/img/v1/p%s/outline.webp"},' "$i" "$i" >> "$sca/body/$v1key"; done
+  printf '{"id": "z", "starter": false}]}' >> "$sca/body/$v1key"
+}
+sca_reset; sca_starters 5; for i in 1 3 5; do echo 000 > "$sca/codes/poses_img_v1_p${i}_outline.webp"; done
+expect "setup-check app: files that answer between silent ones do not count as R2 down" "1 0 5" \
+  "$(sca_check) $(sca_lines 'R2 không trả lời') $(sca_calls '^HEAD .*/p[0-9]/outline.webp$' | awk '{print $1 - 3}')"
+sca_reset; sca_starters 4; for i in 1 2 3 4; do echo 503 > "$sca/codes/poses_img_v1_p${i}_outline.webp"; done
+expect "setup-check app: 5xx in a row also stops the catalog part" "1 1 3" \
+  "$(sca_check) $(sca_lines '^LỖI - R2 không trả lời được') $(sca_calls '^HEAD .*/p[0-9]/outline.webp$' | awk '{print $1 / 2}')"
+sca_reset; echo 403 > "$sca/codes/poses_img_v1_w01_outline.webp"; echo 200 > "$sca/nocache/poses_img_v1_w01_outline.webp"
+expect "setup-check app: a 403 the CDN keeps gets the CDN hint" "1 1" "$(sca_check) $(sca_lines '^LỖI - file khởi đầu .*CDN còn giữ câu trả lời cũ')"
+sca_reset; printf '502\n200\n' > "$sca/codes/poses_img_v1_w01_outline.webp"
+expect "setup-check app: a 502 that passes on the retry is not a failure" 0 "$(sca_check)"
+sca_reset
+expect "setup-check app: the catalog part stops with a note when its time is up" "0 1 0" \
+  "$(SETUP_CHECK_CATALOG_SECONDS=0 sca_check) $(sca_lines '^CHÚ Ý - phần kiểm catalog quá 0 giây') $(sca_calls "^GET .*/$v1\$")"
+sca_reset; printf '200\n200\n404\n' > "$sca/codes/$v1key"; echo '{"poses": ' > "$sca/body/$v1key"
+expect "setup-check app: a catalog that answers 404 when downloaded again reports the 404" "1 1" \
+  "$(sca_check) $(sca_lines "^LỖI - catalog mặc định của app $v1 trả '404' ở lần tải lại")"
+expect "setup-check app: never prints secret values" 0 "$(grep -c 'RC-MARKER-1\|demo-staging\|fake-token' "$work/sca-all.out" || true)"
+printf 'problem "app check ran"\n' > "$work/sca-hook.sh"
+expect "setup-check: sources the app checks file and counts its problems" "1 1" \
+  "$(SETUP_CHECK_APP_CHECKS="$work/sca-hook.sh" sca_check) $(sca_lines '^LỖI - app check ran')"
+for p in poses/catalog/v1/catalog-587b5394.json images/v2/hero.webp levels/v1.3/level-01.json images/hero.v2.webp images/hero-v12.png images/hero.3f2a9c1e.webp sounds/click_0123456789abcdef.ogg; do
+  expect "setup-check app: $p is a valid asset path" 0 "$(bash -c 'eval "$(sed -n "/^valid_asset_path()/,/^}/p" scripts/setup-check-app.sh)"; valid_asset_path "$1"; echo $?' _ "$p")"
+done
+for p in images/hero.webp hero.webp images/v2 images/hero.abc.webp images/latest/hero.webp "" /images/v2/hero.webp images//v2/hero.webp images/v2/../hero.webp ../v2/hero.webp .download/v2/hero.webp https://example.com/v2/hero.webp "images/v2/hero.webp?x=1" "images/v2/hero image.webp"; do
+  expect "setup-check app: '$p' is not a valid asset path" 1 "$(bash -c 'eval "$(sed -n "/^valid_asset_path()/,/^}/p" scripts/setup-check-app.sh)"; valid_asset_path "$1"; echo $?' _ "$p")"
+done
+
 # device-session.sh records a test session and counts crashes and ANRs; adb is replaced by scripts/tests/fake-adb.
 cp scripts/tests/fake-adb "$work/bin/adb"
 chmod +x "$work/bin/adb"
```

### 4.1. Chạy thử

- **Đã chạy thử:** 2026-10-27, worktree `wt-t065v2` từ nhánh chính `1fc049e` (sau PR #179), bản cài thử ở mục 3.1 và test ở mục 4 (không commit trên nhánh plan). Chạy lại toàn bộ sau rà soát độc lập của bản 2.
- **Khối test riêng** (`scripts/tests/sc-block.sh`, tệp tạm đã xóa: dòng 1 đến 26 của `run.sh` cộng khối từ "# setup-check.sh: required R2 files" tới trước "# device-session.sh records"), chạy không có và có lệnh `timeout` (máy viết plan là macOS, không có `timeout`; lần có `timeout` dùng một lệnh tạm bằng `perl` trong `PATH`, nhận cả `-k`, chỉ để chạy test treo).
  - **Code hiện tại** (`setup-check.sh` của nhánh chính, chưa có `setup-check-app.sh`; test mới): 25 qua, 73 thất bại. 17 test cũ của `setup-check` qua. Thất bại đúng lý do: phần catalog không chạy (`expected '0 1', got '0 0'`, `expected '1', got '0'`, `expected '1 1', got '0 0'`, `expected '1 1 3', got '0 0 0'`, `expected '1 0 5', got '0 0 0'`); `rc.args` không có (`expected '1', got ''`); 21 test quy tắc đường dẫn (`valid_asset_path: command not found`, mã 127); `a required file that fails to connect once is asked again` (`expected '0 2', got '1 1'`: chưa hỏi lại); `a 429 that passes on the retry is not a failure` (`expected '0', got '1'`); `sources the app checks file` (`expected '1 1', got '0 0'`). 8 đối chứng qua: `without --staging Remote Config is not read`, `no ok line for a catalog with a missing starter file`, `a file really missing gets no CDN hint`, `a 503 that passes on the retry is not a failure` và `a 502 that passes on the retry is not a failure` (code cũ không kiểm file khởi đầu; bắt đột biến 8 và 38), `a 429 seen twice counts`, `a Remote Config catalog that exists passes`, `never prints secret values`.
  - **Bản cài thử:** có `timeout` 98 qua, không `timeout` 97 qua (test treo bỏ qua), `failures=0`.
- **Toàn bộ `scripts/tests/run.sh`** trên bản cài thử (JDK 21): 810 ok, mã 0 (730 trước đó, cộng 80 test mới chạy được trên macOS).
- **`scripts/check.sh`** trên bản cài thử: qua, mã 0 (`== Cổng chất lượng: QUA`; 1168 test JVM, 0 lỗi; 97 ảnh screenshot; test của script trong đó).
- **Đột biến** (mỗi lần sửa một chỗ của bản cài thử, chạy lại khối có `timeout`); cả 40 bị bắt (số thất bại đo lại sau vòng rà soát 2):

| # | Đột biến | Test thất bại |
| --- | --- | --- |
| 1 | Bỏ `conditionalValues` | 13 |
| 2 | Bỏ `parameterGroups` | 1 |
| 3 | Giá trị sai dạng không đưa khóa về catalog mặc định | 1 |
| 4 | Lỗi thiếu quyền chỉ là chú ý | 2 |
| 5 | Bỏ kiểm `minAppSchema` | 3 |
| 6 | Kiểm mọi tư thế, không chỉ `starter` | 3 |
| 7 | Mẫu khớp `403` trần | 1 |
| 8 | Không hỏi lại `5xx`, `429` | 4 |
| 9 | Không hỏi lại `429` | 1 |
| 10 | Bỏ kiểm template là object JSON | 1 |
| 11 | In `ok` khi có file khởi đầu hỏng | 1 |
| 12 | Bỏ `objects` khi đọc `poses` | 1 |
| 13 | Không báo khi không đọc được giá trị nào | 1 |
| 14 | Bỏ `found=yes` ở giá trị `@default` (rà soát: M5) | 2 |
| 15 | Thiếu mã project là `LỖI` thay vì `CHÚ Ý` | 1 |
| 16 | Bỏ `rolloutValue` | 1 |
| 17 | Bỏ giới hạn thời gian | 1 |
| 18 | Lỗi của công cụ chỉ đọc từ stderr | 1 |
| 19 | Bí mật rỗng coi là có | 1 |
| 20 | Biến không truyền coi là thiếu bí mật | 1 |
| 21 | `remoteconfig:get` dùng nhầm bí mật App Distribution | 1 |
| 22 | Không gộp đường dẫn trùng (`sort -u`) | 4 |
| 23 | Không tải lại catalog bị cắt | 2 |
| 24 | Không kiểm catalog mặc định | 25 |
| 25 | Giá trị theo điều kiện cũng chặt (`minAppSchema`) | 1 |
| 26 | Giá trị mặc định của khóa không chặt | 1 |
| 27 | Bỏ câu CDN ở file khởi đầu | 2 |
| 28 | Bỏ câu CDN ở file bắt buộc | 1 |
| 29 | Câu CDN cho mọi mã (không chỉ 404, 403) | 1 |
| 30 | `setup-check.sh` không gọi file kiểm của app | 49 |
| 31 | Đọc Remote Config trong `( ... )` (bộ đếm lỗi mất) | 3 |
| 32 | Không dừng khi R2 không trả lời được | 2 |
| 33 | Đọc `.preview.small` không qua `objects` (rà soát: M23) | 1 |
| 34 | Không báo catalog không có tư thế `starter` | 1 |
| 35 | Bỏ `r2_silent=0` khi file trả lời được (rà soát vòng 2: B) | 1 |
| 36 | `429`, `5xx` không tính vào R2 không trả lời được | 1 |
| 37 | Bỏ ngân sách thời gian của phần catalog | 1 |
| 38 | Không hỏi lại `5xx` (chỉ `000`, `429`; rà soát vòng 2: F) | 3 |
| 39 | Câu CDN chỉ cho 404 (rà soát vòng 2: C) | 1 |
| 40 | Lần tải lại trả 404 báo là catalog hỏng | 1 |

- **Workflow, chạy thử thật** (không có issue `setup-broken` nào mở, không có phiên nào chạy hay sắp bắt đầu). Nhánh tạm từ `1fc049e` với code của mục 3.1 và một workflow tạm chạy khi đẩy nhánh đó, hai job `green` và `red` chạy đúng lệnh của bước trong job `setup-check` (cùng ba bí mật), chỉ in các dòng kiểm, không mở hay đóng issue nào. Không chạy thử bằng `env-canary.yml` vì trước task này chạy tay workflow đó còn kích hoạt routine `env-canary` và mở, đóng issue thật; phần ghi issue không đổi. Sau mỗi lần thử, nhánh tạm đã xóa và workflow tạm đã tắt (`trial-t065`, `trial-t065b`, `trial-t065c`: `disabled_manually`; Tuấn có thể bỏ qua hay xóa lịch sử của chúng trong Actions).
  - **Lần 1** (bản trước rà soát, 02:55 UTC, lần chạy 40441226351): đường xanh mã 0, 81 giây; đường đỏ mã 1, 79 giây.
  - **Lần 2** (bản này, 03:30 UTC, lần chạy 40444507136; bước giống bước mới của `env-canary`, có kiểm lại sau 90 giây):
    - **Đường xanh:** mã 0, 83 giây, chạy một lần. `ok   - catalog mặc định của app poses/catalog/v1/catalog-587b5394.json và 8 file khởi đầu trả 200`; `ok   - khóa poses_catalog_path trỏ tới catalog poses/catalog/v3/catalog-512aa695.json và 100 file khởi đầu trả 200`; `== Thiết lập: QUA`. Giá trị khóa đọc từ Remote Config thật.
    - **Đường đỏ** (`SETUP_CHECK_EXTRA_CATALOG=poses/catalog/v9/catalog-00000000.json`): lần một `##[error]khóa poses_catalog_path trỏ tới catalog poses/catalog/v9/catalog-00000000.json trả '404', cần 200: app dừng ở màn tải. ...` (không có câu CDN: file thật sự không có), `== Thiết lập còn 1 lỗi.`; rồi "Kiểm tra đỏ; kiểm lại một lần sau 90 giây", lần hai cùng kết quả; mã 1, 226 giây cả bước, trong giới hạn 10 phút.
    - Không có `firebase-debug.log` ở cả hai job.
  - **Lần 3** (sau rà soát vòng 2, 04:00 UTC, lần chạy 40447240480; checkout `persist-credentials: false`, script chạy bằng `env -u GH_TOKEN -u GITHUB_TOKEN`, như bước mới của `env-canary`): đường xanh, mã 0, 85 giây, cùng hai dòng `ok   - ...` của catalog như lần 2; trong `env -u` không có token ("token in env" rỗng), `.git/config` không có token ("token in git config: no"); `npx firebase-tools` vẫn đọc được Remote Config.
  - **Tham số chống bộ nhớ đệm trên Cloudflare thật:** `HEAD .../borrowed/poses/img/v1/w01/outline.webp?setup-check-nocache=<số>` trả 200 với `cf-cache-status: DYNAMIC` (không qua bộ nhớ đệm); ngày 27 trước khi xóa bộ nhớ đệm, cùng file hỏi kèm tham số trả 200 (`MISS`) trong khi địa chỉ thường trả 404 (`HIT`). Câu CDN chưa gặp lại trên R2 thật vì hiện không có file nào bị giữ 404.
- **Chưa kiểm được:** chữ thật của lỗi thiếu vai trò (mục 3.4, điểm 1; câu hỏi 6); bước đóng `setup-broken` của `start-run.yml` (chỉ chạy khi bắt đầu phiên thật bằng `start-run`).

## 5. Chấm rủi ro

**Điểm chặn bị chạm:** 7 (CI: `.github/workflows/env-canary.yml`, `start-run.yml`; quy trình phiên tự chạy: điều kiện bắt đầu phiên).

| Tiêu chí | Điểm (0 đến 3) | Lý do |
| --- | --- | --- |
| A. Phạm vi thay đổi | 2 | Một script mới, một script của dự án mẫu, hai workflow, test, ba tài liệu |
| B. Độ rõ của yêu cầu | 1 | Hai sự cố rõ; còn câu hỏi 6 không đổi cách làm chính |
| C. Khả năng kiểm chứng tự động | 1 | Script có 81 test và 40 đột biến; chỉ phần workflow (kiểm lại, đóng issue) và `remoteconfig:get` thật kiểm bằng lần chạy tay |
| D. Ảnh hưởng chéo | 3 | `setup-check` quyết định phiên có bắt đầu không (`start-run`, nhãn `setup-broken`); sửa file của dự án mẫu mà lần đồng bộ sau phải giữ |
| E. Độ mới của kỹ thuật | 1 | `remoteconfig:get` đã chạy thật ở bản 1 và hai lần thử này; móc file kiểm của app chỉ là một lệnh `.` |
| **Tổng** | 8 | **Mức: Cao** (điểm chặn 7) |

## 6. Ảnh hưởng chéo

- File hoặc thành phần dùng chung bị chạm: `scripts/setup-check.sh` (của dự án mẫu), `env-canary.yml`, `start-run.yml`; `scripts/tests/run.sh` (khối `setup-check` và khối mới); `docs/template-sync.txt`.
- Task khác trong sprint có thể xung đột: T-063 (`scripts/tests/run.sh`, khối `token-usage`, khác khối; plan T-063 cần phiên bản 2 sau PR #179), T-062 (`device-session.yml`, khác file), T-064 (không chung file).
- Lần đồng bộ dự án mẫu sau: giữ các thay đổi ghi ở `docs/template-sync.txt` khi gộp, cho tới khi dự án mẫu nhận đề xuất (mục 3.1).
- Chạy cùng lúc: `env-canary` đang chờ 90 giây trước lần kiểm lại trong khi `start-run` bắt đầu một phiên. `start-run` tự kiểm và đóng issue cũ; `env-canary` có thể mở issue sau đó nếu lần kiểm lại vẫn đỏ, và phiên `claude --cloud` mở sau đó sẽ dừng ở kiểm tra đầu phiên. Chấp nhận: hai kiểm tra cùng lúc mà khác kết quả nghĩa là R2 đang chập chờn; issue mở đúng là việc của người.
- Đề xuất thứ tự chạy: độc lập, chạy được bất kỳ lúc nào; đặt sau T-062, T-063.

## 7. Điều kiện dừng riêng

- Ngay trước task, có issue `setup-broken` đang mở: phiên đã dừng ở kiểm tra đầu phiên (`docs/auto-run.md`, mục 2), nên không tới task này; không cần điều kiện riêng.
- Bí mật `STAGING_REMOTE_CONFIG_CREDENTIALS` không còn (lần chạy job `setup-check` sau khi merge báo `thiếu bí mật`): hoàn tác theo mục 3.1, Sau khi merge, và ghi vào Cần người kiểm tra.

## 8. Câu hỏi cho người duyệt

1. Phần riêng của app nằm ở file mới `scripts/setup-check-app.sh`; `setup-check.sh` của dự án mẫu chỉ thêm móc gọi nó, hỏi lại và câu CDN (đề xuất về dự án mẫu, danh sách giữ khi đồng bộ ở `docs/template-sync.txt`). **AI đề xuất: đồng ý**, để lần đồng bộ sau không đè phần của app và dự án mẫu nhận được phần chung.
2. Catalog mặc định luôn được kiểm, kể cả khi khóa trên Remote Config trỏ catalog khác và không có `--staging`. **AI đề xuất: đồng ý**: bản staging của CI và máy chưa có giá trị Remote Config dùng nó (sự cố ngày 27); thêm khoảng 9 lệnh.
3. `start-run.yml` xanh thì đóng issue `setup-broken` đang mở (cần `issues: write`, ở bước riêng không có `npx`), để phiên mở bằng `claude --cloud` sau đó không dừng vì một issue cũ (thay cho câu "phiên tự chạy lại kiểm tra" của bản 1). Giá phải trả: Cloudflare giữ bộ nhớ đệm theo từng trung tâm dữ liệu, nên máy của `start-run` có thể nhận 200 trong khi máy của `env-canary` (và một phần người dùng) vẫn nhận 404 cũ; khi đó `start-run` đóng một sự cố thật, và lần chạy `env-canary` sáng hôm sau mở lại. **AI đề xuất: đồng ý**: phiên không dừng vì issue cũ quan trọng hơn, và sự cố thật vẫn hiện lại trong một ngày.
4. Catalog hay file khởi đầu hỏng là `LỖI` (chặn phiên ở `start-run`, mở `setup-broken` sau lần kiểm lại 90 giây). **AI đề xuất: `LỖI`**: người sửa trong vài phút, còn phiên chạy với catalog hỏng thì `ui-test` của mọi task đỏ (ngày 27) hay `device-session` hỏng mọi kịch bản.
5. Ghi chú: `CHÚ Ý` của phần Remote Config lặp lại nhiều ngày (công cụ hỏng lâu) không mở issue; nếu thấy cần, một ticket sau có thể biến `CHÚ Ý` lặp lại quá 3 ngày thành issue.
6. Lỗi thiếu vai trò có thể chỉ ra `CHÚ Ý` (mục 3.4, điểm 1): giữ như plan, hay coi mọi lỗi `Failed to get Firebase Remote Config template` khi bí mật có mặt là `LỖI`? **AI đề xuất: giữ như plan**: coi là `LỖI` thì một lỗi mạng tạm của công cụ cũng chặn phiên cả ngày; catalog mặc định vẫn được kiểm. **Đề xuất Tuấn thử một lần trước khi duyệt:** gỡ vai trò Firebase Remote Config Viewer của `remote-config-reader`, báo AI chạy lại lần thử trên nhánh tạm, xem dòng ra `LỖI` hay `CHÚ Ý`, rồi gán lại vai trò; ra `CHÚ Ý` thì AI thêm chữ lỗi thật vào mẫu trước khi duyệt.

## 9. Quyết định duyệt

<!-- Người duyệt điền. AI không điền mục này. -->

- **Quyết định:** Duyệt
- **Người duyệt:** Nhật, **duyệt chéo:** Tuấn
- **Ghi chú:** Ngày 2026-10-27, Tuấn trả lời câu hỏi cùng AI: chấp nhận mọi đề xuất của AI ở mục 8. Câu 1: logic riêng của app trong `scripts/setup-check-app.sh`. Câu 2: luôn kiểm catalog mặc định và file khởi đầu. Câu 3: `start-run.yml` đóng `setup-broken` khi chính nó kiểm xanh, chấp nhận cái giá đã nêu. Câu 4: catalog hay file khởi đầu hỏng là `LỖI`. Câu 6: không thử gỡ vai trò trước khi duyệt; thiếu vai trò thật có thể chỉ ra `CHÚ Ý`.

## 10. Nhật ký thực hiện

<!-- AI ghi sau khi chạy task. -->

### RUN-20261027-02, phiên bản plan 2

- **Kết quả:** đã merge (2026-10-27 20:14 UTC, commit `6f3c01d` trên nhánh chính).
- **Nhánh, PR:** `claude/T-065-setup-check-catalog`, PR #200.
- **Đã làm:** phiên chính tự làm. `diff` của `scripts/setup-check.sh`, hai workflow, tài liệu và khối test áp sạch (`git apply --check`) trên nhánh chính `890c4ff`, trừ khối `docs/template-sync.txt`; `scripts/setup-check-app.sh` chép nguyên khối mục 3.1 (quyền 644, chỉ gọi bằng `.`). ADR-1005/L3 ở `docs/template-sync.txt`: khối chỉ có dòng `+`, ba dòng ngữ cảnh trên còn nguyên văn, cuối file nay có dòng `# T-068: ...` (T-068 merge trước trong phiên) nên `git apply` từ chối; đặt nguyên 13 dòng `+` (không đổi chữ) ngay sau dòng của T-068, như danh sách chạy ghi. Rà soát so từng byte: các file khác giống bản của plan.
- **Test viết trước:** 81 test (commit `e41d073`): 73 thất bại đúng lý do, 25 qua (17 test cũ, 8 đối chứng), đúng mục 4.1. Sau code: `scripts/tests/run.sh` 1048 ok, cả khi chạy với `GIT_CONFIG_GLOBAL=/dev/null`.
- **Rà soát độc lập:** 0 chặn, 0 nên sửa, 2 ghi chú: `issues: write` ở cấp workflow nên hai bước có sẵn của `start-run.yml` cũng cầm token có quyền ghi (đúng `diff` đã duyệt); `env -u GH_TOKEN` chỉ giảm rủi ro của `npx`, đề xuất về mẫu nên tách `setup-check` thành job riêng không có `issues: write`.
- **Kết quả CI:** `scripts/check.sh` trên máy: QUA. `pr-check` lần chạy 40575330240: xanh ở vòng đầu. Tổng 1 vòng CI.
- **Sau khi merge:** `env-canary.yml` chạy tay với `setup_only` lúc 20:14 UTC (lần chạy 40576244925): job `setup-check` xanh, `fire` bỏ qua; "catalog mặc định của app poses/catalog/v1/catalog-587b5394.json và 8 file khởi đầu trả 200", "khóa poses_catalog_path trỏ tới catalog poses/catalog/v3/catalog-512aa695.json và 100 file khởi đầu trả 200", "== Thiết lập: QUA".
- **Lệch so với plan:** ADR-1005/L3 như trên. Ngoài mục 3.2: không có.
- **Việc còn lại:** kiểm tra thủ công: lần đầu `start-run` xanh khi có issue `setup-broken` mở, xem issue có bình luận và được đóng. Đề xuất về dự án mẫu (mục 3.1), kèm ghi chú 2 của rà soát. Người xóa tay nhánh `claude/T-065-setup-check-catalog` (L-092).
- **Cần người xem:** hai ghi chú của rà soát.
