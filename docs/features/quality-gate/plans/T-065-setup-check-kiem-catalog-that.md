---
ticket: T-065
title: "`setup-check` kiểm catalog tư thế mà máy tải (catalog mặc định và khóa `poses_catalog_path` trên Remote Config) cùng các file khởi đầu của nó"
status: draft                # draft | approved | stopped | done
version: 2                   # tăng khi plan được sửa lại sau phản hồi hoặc sau khi dừng
risk_score:                  # tổng điểm theo risk-scoring.md, để trống nếu chạm điểm chặn
risk_level: high             # low | medium | high
token_estimate: 7.6M         # cỡ M, số gợi ý của scripts/token-usage.py estimate (2026-10-27), không nâng tay (L-081)
blocking_points: [ci, process]
run_order:                   # AI điền sau họp đánh giá rủi ro
reviewer:                    # người duyệt
cross_reviewer:              # bắt buộc khi risk_level là high
approved_at:
---

# PLAN T-065: `SETUP-CHECK` KIỂM CATALOG TƯ THẾ MÀ MÁY TẢI

## 1. Tóm tắt

Thêm phần kiểm catalog tư thế vào kiểm tra thiết lập `scripts/setup-check.sh` (mỗi sáng ở job `setup-check` của `env-canary.yml`, trước mỗi phiên ở `start-run.yml`). Phần riêng của app nằm ở file mới `scripts/setup-check-app.sh`, được `setup-check.sh` gọi ở cuối: kiểm catalog mặc định của app và, với `--staging`, mọi giá trị của khóa `poses_catalog_path` trên Remote Config staging (đọc bằng `firebase-tools remoteconfig:get`, tài khoản chỉ đọc, bí mật `STAGING_REMOTE_CONFIG_CREDENTIALS` đã có). Mỗi catalog phải trả 200, đọc được, không đòi app mới hơn, và ảnh nhỏ cùng nét viền của mọi tư thế `starter` phải trả 200. `setup-check.sh` của dự án mẫu thêm ba điều chung: thử lại một lần khi `000`, `429`, `5xx`; hỏi lại kèm tham số chống bộ nhớ đệm khi một file không trả 200, để báo "CDN còn giữ 404 cũ, xóa bộ nhớ đệm của URL này"; gọi file kiểm của app nếu repo có. Code và 68 test mới đã chạy thật trên code hiện tại (61 thất bại đúng lý do, 7 đối chứng qua), trên bản cài thử (qua hết, `scripts/tests/run.sh` 797 ok) và với 26 đột biến; workflow chạy thử thật trên nhánh tạm: đường xanh và đường đỏ đều đúng (mục 4.1).

**Phiên bản 2.** Phiên bản 1 (PR #180, không merge, chưa duyệt) sửa `scripts/preflight.sh`; PR #179 (đồng bộ dự án mẫu, đợt 1) đã gỡ `preflight` và thay bằng `setup-check` của dự án mẫu, nên plan viết lại trên cấu trúc mới, giữ mọi điều đã chứng minh ở bản 1 (lệnh `remoteconfig:get -o`, lỗi của công cụ ra stdout, các mức `LỖI` và `CHÚ Ý`, mọi loại giá trị của khóa, hai vòng rà soát độc lập) và thêm phần catalog mặc định cùng gợi ý CDN theo sự cố ngày 2026-10-27. Cách tiếp tục: làm lại từ đầu trên nhánh mới (bản 1 không có code nào trên nhánh chính).

## 2. Hiểu yêu cầu

Không có spec nghiệp vụ. Yêu cầu là hai sự cố:

- 2026-10-26 (plan T-062, mục 4.1 và câu hỏi 4; Tuấn chọn làm ticket riêng): khóa `poses_catalog_path` trỏ tới `poses/catalog/v3/catalog-c2e0ce0d.json`, file trả 404, app dừng ở màn tải; kiểm tra chỉ biết đường dẫn viết cứng.
- 2026-10-27 (rà soát PR #179): catalog mặc định `poses/catalog/v1/catalog-587b5394.json` trả 200 nhưng 8 ảnh khởi đầu `borrowed/poses/img/v1/...` trả 404, `ui-test` (bản staging của CI dùng file Firebase giả, nên dùng catalog mặc định) đỏ trên mọi PR có code. Tải ảnh lên lại lúc 01:42 UTC rồi mà địa chỉ thường vẫn trả 404 (`cf-cache-status: HIT`, `cache-control: max-age=31536000`); hỏi kèm tham số thì 200; Tuấn xóa bộ nhớ đệm thì hết.

| Yêu cầu | Tiêu chí chấp nhận | Cách hiểu của AI |
| --- | --- | --- |
| Catalog mặc định luôn được kiểm | Khi ảnh nhỏ hay nét viền của một tư thế `starter` trong catalog mặc định không trả 200 thì `setup-check.sh` (cả khi không có `--staging`) trả 1, dòng `LỖI - file khởi đầu <file> của catalog <đường dẫn> trả '<mã>' ...`; catalog mặc định trả khác 200 thì `LỖI - catalog mặc định của app <đường dẫn> trả ...` | Máy không có giá trị Remote Config và bản staging của CI dùng nó. Không cần bí mật, nên chạy được cả trên máy dev (`docs/ops/setup-new-app.md`) |
| Bắt sự cố ngày 26 | Khi một giá trị của khóa (mặc định, theo điều kiện, `rolloutValue`, trong `parameterGroups`) trỏ tới file trả 404 thì `--staging` trả 1, dòng `LỖI - khóa poses_catalog_path trỏ tới catalog <đường dẫn> trả '404' ...` | Không đánh giá được điều kiện (phiên bản app, phần trăm, thuộc tính người dùng) ở đây; kiểm mọi giá trị là chặt hơn và đơn giản. Project staging chỉ có app staging, production ở project khác (ADR-0007) |
| Giống cách app chọn | Không có khóa, không có giá trị mặc định, `useInAppDefault` hay giá trị sai dạng (`AssetPath.isValid`) thì khóa trỏ về catalog mặc định; giá trị sai dạng, giá trị cá nhân hóa và không đọc được giá trị nào có thêm dòng `CHÚ Ý` | Khóa, đường dẫn mặc định và `APP_SCHEMA` đọc thẳng từ `PosesSettings.kt`, `CatalogParser.kt`; quy tắc đường dẫn chép sang bash, có test cùng danh sách ví dụ của `AssetPathTest` |
| Catalog dùng được | Catalog không phải JSON có `poses`, hay `minAppSchema` lớn hơn `APP_SCHEMA`, thì `LỖI`. Một phần tử của `poses` không phải object không che các file khởi đầu khác; catalog trả 200 mà đọc không được thì tải lại một lần | Cả hai lỗi cũng làm app dừng ở màn tải |
| CDN giữ 404 cũ | Khi một file (file bắt buộc, catalog hay file khởi đầu) không trả 200 mà hỏi lại kèm `?setup-check-nocache=<số>` thì trả 200, dòng `LỖI` có thêm câu "File đã có trên R2 nhưng CDN còn giữ câu trả lời cũ cho đúng địa chỉ này: xóa bộ nhớ đệm của URL này (Cloudflare: Caching, Configuration, Custom Purge) ...". File thật sự thiếu thì không có câu này | Đúng chẩn đoán đã làm bằng tay ngày 27. Vẫn là `LỖI`: app trên máy nhận đúng câu trả lời 404 đó |
| Báo rõ khi thiếu quyền | Thiếu bí mật thì `LỖI - thiếu bí mật STAGING_REMOTE_CONFIG_CREDENTIALS ...`; lỗi của công cụ có `HTTP Error: 403`, `PERMISSION_DENIED` hay `missing the following required permissions` (đọc cả stdout và stderr) thì `LỖI - ... (cần vai trò Firebase Remote Config Viewer, hoặc API Remote Config chưa bật)` | Việc người làm một lần. Không khớp `403` trần |
| Không chặn phiên vì lỗi tạm | Công cụ lỗi vì lý do khác, quá 120 giây (`timeout`, mã 124), ghi ra tệp không phải JSON, hay bí mật Firebase không có mã project: chỉ `CHÚ Ý`, catalog mặc định vẫn được kiểm. R2 trả `000`, `429`, `5xx` một lần thì hỏi lại một lần trước khi tính là lỗi | Đỏ ở `start-run.yml` thì phiên không bắt đầu, đỏ ở `env-canary.yml` thì mở `setup-broken`; một lần lỗi mạng không được chặn cả ngày |
| Không lộ bí mật | Mã project, nội dung khóa và lỗi của công cụ không bao giờ in ra | Lỗi của `firebase-tools` ghi vào tệp tạm trong thư mục tạm của `setup-check.sh`, chỉ dùng làm điều kiện |
| Thử được đường đỏ | `SETUP_CHECK_EXTRA_CATALOG=<đường dẫn>` thêm một đường dẫn kiểm như một giá trị của khóa | Chỉ cho lần chạy thử; không thêm ô vào `env-canary.yml`, vì chạy tay `env-canary` còn kích hoạt routine `env-canary` (tốn hạn mức) |
| Người biết phải làm gì | `docs/ops/staging-and-runs.md`: tải catalog và file của nó lên R2 trước rồi mới đổi khóa (kể cả giá trị theo điều kiện chưa bật); đổi xong thì chạy lại **env-canary**; CDN giữ 404 cũ thì xóa bộ nhớ đệm | Lần chạy buổi sáng không thấy thay đổi buổi chiều |

Thiết kế tham chiếu: Không có thiết kế riêng.

ADR liên quan: ADR-0003 (Remote Config), ADR-0007 (staging, production tách project), ADR-0009 (bí mật chỉ của staging, quyền nhỏ nhất), ADR-0012 (Firebase), ADR-0015 (đường dẫn R2 có phiên bản), ADR-0016 (bí mật của các workflow, `origin: template`), ADR-0030 (đồng bộ dự án mẫu về app).

## 3. Giải pháp

### 3.1. Cách làm

**Việc của người (đã xong):** Tuấn đã tạo tài khoản dịch vụ `remote-config-reader` chỉ có vai trò Firebase Remote Config Viewer trên project staging và bí mật `STAGING_REMOTE_CONFIG_CREDENTIALS` (2026-10-27 01:19:45Z). Lần chạy thử ở mục 4.1 đọc được Remote Config thật bằng bí mật này.

**Vì sao tách file của app.** `scripts/setup-check.sh` là file của dự án mẫu (đồng bộ bằng `scripts/template-sync.sh`, ADR-0030); dự án mẫu không có `:core:poses`. Phần chỉ có ở app (catalog tư thế) nằm ở `scripts/setup-check-app.sh`, file mà dự án mẫu không có nên lần đồng bộ sau không đè. `setup-check.sh` chỉ thêm phần chung: `http_status` (thử lại), `cdn_hint`, và gọi file kiểm của app ở cuối (`. scripts/setup-check-app.sh` khi file có; biến `SETUP_CHECK_APP_CHECKS` thay đường dẫn trong test). File được gọi bằng `.`, không chạy riêng, để dùng chung `ok`, `problem`, `note` và bộ đếm lỗi; vì vậy phần đọc Remote Config ghi kết quả vào tệp `$work/remote-paths`, không chạy trong `$(...)` (đột biến 26).

**Đề xuất về dự án mẫu** (`docs/ops/from-app.md`; ghi vào mô tả PR, mở issue `from-app` sau khi merge): ba thay đổi chung của `setup-check.sh` và test của chúng (thử lại `000`, `429`, `5xx`; gợi ý CDN; móc `scripts/setup-check-app.sh` cho phần kiểm riêng của từng app), dòng `STAGING_REMOTE_CONFIG_CREDENTIALS` cho bảng bí mật của ADR-0016 ở bản của dự án mẫu (app không sửa ADR `origin: template`), và giới hạn thời gian 10 phút của job khi app có phần kiểm riêng. Cho tới khi dự án mẫu nhận, lần đồng bộ sau phải giữ ba thay đổi này khi gộp `setup-check.sh` (`docs/template-sync.txt` không cần `hold=`: chỉ một file của mẫu bị sửa, gộp tay như `docs/auto-run.md` ở PR #179).

**`scripts/setup-check.sh`** (bản cài thử đã chạy, mục 4.1):

```diff
diff --git a/scripts/setup-check.sh b/scripts/setup-check.sh
index 5154c2c..8c499e6 100755
--- a/scripts/setup-check.sh
+++ b/scripts/setup-check.sh
@@ -6,7 +6,11 @@
 #      resolvers validate DNSSEC, and a zone whose DNSSEC fails there does not resolve on many phones;
 #   3. with --staging: the STAGING_GOOGLE_SERVICES_JSON secret is not the fake file and has the
 #      <applicationId>.staging app, and every App Distribution group in STAGING_TESTER_GROUPS (default "team")
-#      exists, checked with the STAGING_APP_DISTRIBUTION_CREDENTIALS service account when gcloud is available.
+#      exists, checked with the STAGING_APP_DISTRIBUTION_CREDENTIALS service account when gcloud is available;
+#   4. the app's own checks in scripts/setup-check-app.sh, when the repository has that file (sourced at the end
+#      with ok, problem, note, http_status, cdn_hint, $STAGING, $work and $base_url).
+# A file that does not answer 200 is asked once more with a cache-busting query: a 200 then means the file is on R2
+# but the CDN still serves an old 404 (camera-pose, 2026-10-27: a 404 cached for a year after the upload).
 #
 # Usage:  scripts/setup-check.sh [--staging]
 #   Without --staging it needs no secret: a developer runs it at the end of docs/ops/setup-new-app.md.
@@ -31,6 +35,8 @@ base_url="${SETUP_CHECK_ASSETS_BASE_URL:-$(grep -E '"ASSETS_BASE_URL", *"\\"http
   | head -n 1 | grep -oE 'https://[^\\"]+' || true)}"
 package="${SETUP_CHECK_PACKAGE:-$(sed -n 's/^ *applicationId = "\([^"]*\)".*/\1/p' app/build.gradle.kts | head -n 1)}"
 groups="${STAGING_TESTER_GROUPS:-team}"
+app_checks="${SETUP_CHECK_APP_CHECKS:-scripts/setup-check-app.sh}"
+retry_seconds="${SETUP_CHECK_RETRY_SECONDS:-5}"
 
 problems=0
 work="$(mktemp -d)"
@@ -45,6 +51,27 @@ problem() {
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
+# For a file that does not answer 200: prints a hint when the same file answers 200 with a cache-busting query,
+# that is, the file is on R2 and the CDN still serves an old answer for the plain URL.
+cdn_hint() { # <url>
+  local code
+  code="$(http_status "$1?setup-check-nocache=$(date +%s)$RANDOM" -o /dev/null -I)"
+  if [ "$code" = "200" ]; then
+    printf ' File đã có trên R2 nhưng CDN còn giữ câu trả lời cũ cho đúng địa chỉ này: xóa bộ nhớ đệm của URL này (Cloudflare: Caching, Configuration, Custom Purge), và đặt Cache Rule cho mã 404 với thời hạn ngắn.'
+  fi
+}
 
 echo "== File bắt buộc trên R2 staging"
 if ! [[ "$base_url" =~ ^https://[^/]+/.*/$|^https://[^/]+/$ ]]; then
@@ -56,11 +83,11 @@ else
     path="${line%%#*}"
     path="$(echo "$path" | tr -d '[:space:]')"
     [ -n "$path" ] || continue
-    code="$(curl -sS -o /dev/null -I --max-time 20 -w '%{http_code}' "${base_url}${path}" 2> /dev/null || true)"
+    code="$(http_status "${base_url}${path}" -o /dev/null -I)"
     if [ "$code" = "200" ]; then
       ok "${base_url}${path}"
     else
-      problem "${base_url}${path} trả '${code:-không kết nối được}', cần 200. Đưa file lên R2 staging (docs/ops/setup-new-app.md, phần 3)."
+      problem "${base_url}${path} trả '${code:-không kết nối được}', cần 200. Đưa file lên R2 staging (docs/ops/setup-new-app.md, phần 3).$(cdn_hint "${base_url}${path}")"
     fi
   done < "$assets_list"
 fi
@@ -141,6 +168,11 @@ if [ "$STAGING" = "yes" ]; then
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
# Tests replace: SETUP_CHECK_FIREBASE_CMD, SETUP_CHECK_RC_TIMEOUT; SETUP_CHECK_EXTRA_CATALOG adds one more catalog
# path checked like a value of the key (red-path trial only).

echo "== Catalog tư thế và bộ khởi đầu"

poses_settings=core/poses/src/main/kotlin/com/vglstudio/camera/pose/line/guide/core/poses/internal/PosesSettings.kt
catalog_parser=core/poses/src/main/kotlin/com/vglstudio/camera/pose/line/guide/core/poses/internal/CatalogParser.kt
# The key, the fallback path and the schema are read from the app code, so the check follows the app.
catalog_key="$(sed -nE 's/.*CATALOG_PATH_KEY = "([^"]+)".*/\1/p' "$poses_settings" 2> /dev/null | head -n 1 || true)"
default_catalog="$(sed -nE 's/.*DEFAULT_CATALOG_PATH = "([^"]+)".*/\1/p' "$poses_settings" 2> /dev/null | head -n 1 || true)"
app_schema="$(sed -nE 's/.*APP_SCHEMA = ([0-9]+).*/\1/p' "$catalog_parser" 2> /dev/null | head -n 1 || true)"
read -r -a firebase_cmd <<< "${SETUP_CHECK_FIREBASE_CMD:-npx --yes firebase-tools@15.32.1}"
rc_timeout="${SETUP_CHECK_RC_TIMEOUT:-120}"

# Same rule as AssetPath.isValid of :core:assets: relative segments, and a version folder or a versioned file name.
valid_asset_path() {
  local path="$1" segment='[A-Za-z0-9][A-Za-z0-9._-]*'
  [[ "$path" =~ ^($segment/)*$segment$ ]] || return 1
  [[ "$path" =~ (^|/)v[0-9]+(\.[0-9]+)*/ ]] && return 0
  [[ "${path##*/}" =~ ^.+[._-](v[0-9]+(\.[0-9]+)*|[0-9a-f]{8,})\.[A-Za-z0-9]+$ ]]
}

# Every value a staging device can get for the key. "@default" is the app's own default (no parameter, no default
# value, useInAppDefault); "@unknown" is a value kind the script does not read (personalization).
catalog_values() { # <template file>
  jq -r --arg k "$catalog_key" '
    ([.parameters[$k]?] + [.parameterGroups[]?.parameters[$k]?]) | map(select(. != null)) | .[0] as $p
    | if $p == null then "@default"
      else ([$p.defaultValue] + [($p.conditionalValues // {}) | to_entries[] | .value]) | .[]
        | if . == null or .useInAppDefault == true then "@default"
          elif (.value | type) == "string" then .value
          elif (.rolloutValue.value | type) == "string" then .rolloutValue.value
          else "@unknown" end
      end' "$1"
}

# Writes the catalog paths of the key on Remote Config staging to $work/remote-paths, one per line; a problem or a
# note when it cannot. Not run in a subshell, so problem() counts.
remote_catalog_paths() {
  local project template="$work/remote-config.json" errors="$work/remote-config.err" value
  local -a get=("${firebase_cmd[@]}")
  if [ -z "${REMOTE_CONFIG_CREDENTIALS:-}" ]; then
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
  local found=""
  while IFS= read -r value; do
    case "$value" in
      @default) echo "$default_catalog" >> "$work/remote-paths"; found=yes ;;
      @unknown) note "khóa $catalog_key có một giá trị script không đọc được (cá nhân hóa?); kiểm bằng tay." ;;
      *)
        found=yes
        if valid_asset_path "$value"; then
          echo "$value" >> "$work/remote-paths"
        else
          note "khóa $catalog_key có giá trị \"$value\" không hợp lệ; app dùng catalog mặc định $default_catalog."
          echo "$default_catalog" >> "$work/remote-paths"
        fi
        ;;
    esac
  done < <(catalog_values "$template" 2> /dev/null || true)
  [ -n "$found" ] || note "không đọc được giá trị nào của khóa $catalog_key trong template Remote Config; chỉ kiểm catalog mặc định."
}

# Checks one catalog and its starter files; <where> says where the path comes from, for the messages.
check_catalog() { # <path> <where>
  local path="$1" where="$2" catalog="$work/catalog.json" code file count=0 bad=0
  rm -f "$catalog"
  code="$(http_status "${base_url}${path}" -o "$catalog")"
  if [ "$code" != "200" ]; then
    problem "$where $path trả '${code:-không kết nối được}', cần 200: app dừng ở màn tải. Sửa khóa $catalog_key trên Remote Config staging hoặc đưa file lên R2.$(cdn_hint "${base_url}${path}")"
    return
  fi
  if ! jq -e '.poses | type == "array"' "$catalog" > /dev/null 2>&1; then
    # A file cut short in transit is downloaded once more before it counts as broken.
    rm -f "$catalog"
    code="$(http_status "${base_url}${path}" -o "$catalog")"
  fi
  if [ "$code" != "200" ] || ! jq -e '.poses | type == "array"' "$catalog" > /dev/null 2>&1; then
    problem "$where $path không phải catalog đọc được (JSON, có danh sách poses)."
    return
  fi
  if [ -n "$app_schema" ] && jq -e --argjson app "$app_schema" '(.minAppSchema // 0) > $app' "$catalog" > /dev/null 2>&1; then
    problem "$where $path cần app mới hơn: minAppSchema lớn hơn $app_schema của app."
    return
  fi
  while IFS= read -r file; do
    count=$((count + 1))
    code="$(http_status "${base_url}${file}" -o /dev/null -I)"
    if [ "$code" != "200" ]; then
      problem "file khởi đầu $file của catalog $path trả '${code:-không kết nối được}', cần 200: app dừng ở màn tải bộ khởi đầu.$(cdn_hint "${base_url}${file}")"
      bad=$((bad + 1))
    fi
  done < <(jq -r '.poses[] | objects | select(.starter == true) | (.preview | objects | .small), .outline | strings' \
    "$catalog" 2> /dev/null || true)
  [ "$bad" -gt 0 ] || ok "$where $path và $count file khởi đầu trả 200"
}

if [ -z "$catalog_key" ] || [ -z "$default_catalog" ]; then
  problem "không đọc được khóa và đường dẫn catalog mặc định trong $poses_settings."
elif ! [[ "$base_url" =~ ^https://[^/]+/ ]]; then
  : # setup-check.sh already reported the missing staging address.
else
  : > "$work/remote-paths"
  if [ "$STAGING" = "yes" ]; then
    remote_catalog_paths
  fi
  [ -z "${SETUP_CHECK_EXTRA_CATALOG:-}" ] || echo "$SETUP_CHECK_EXTRA_CATALOG" >> "$work/remote-paths"
  remote_paths="$(cat "$work/remote-paths")"
  while IFS= read -r catalog_path; do
    [ -n "$catalog_path" ] || continue
    if grep -qxF "$catalog_path" <<< "$remote_paths"; then
      check_catalog "$catalog_path" "khóa $catalog_key trỏ tới catalog"
    else
      check_catalog "$catalog_path" "catalog mặc định của app"
    fi
  done < <(printf '%s\n%s\n' "$default_catalog" "$remote_paths" | sort -u)
fi
```

**Workflow** (`.github/workflows/env-canary.yml`, job `setup-check`; `.github/workflows/start-run.yml`): đưa bí mật vào biến `REMOTE_CONFIG_CREDENTIALS`, như hai bí mật có sẵn (`setup-check.sh` ghi nó vào tệp trong thư mục tạm `umask 077` và xóa khi xong); giới hạn thời gian của job từ 5 lên 10 phút: lần chạy thử mất 79 đến 81 giây cho cả `setup-check.sh` (khoảng 110 lệnh `HEAD` và tải `firebase-tools`), còn `remoteconfig:get` có thể tới 120 giây trước khi bị dừng, và mỗi file có thể được hỏi lại.

```diff
diff --git a/.github/workflows/env-canary.yml b/.github/workflows/env-canary.yml
index e197f01..3bf069b 100644
--- a/.github/workflows/env-canary.yml
+++ b/.github/workflows/env-canary.yml
@@ -35,7 +35,9 @@ jobs:
   # Runs even during an auto-run session: it costs no Claude usage.
   setup-check:
     runs-on: ubuntu-latest
-    timeout-minutes: 5
+    # 10, not 5: the pose catalog check of scripts/setup-check-app.sh downloads firebase-tools (up to 2 minutes for
+    # remoteconfig:get) and asks R2 for every starter file of each catalog.
+    timeout-minutes: 10
     permissions:
       contents: read
       issues: write
@@ -47,6 +49,8 @@ jobs:
           GH_TOKEN: ${{ github.token }}
           GOOGLE_SERVICES: ${{ secrets.STAGING_GOOGLE_SERVICES_JSON }}
           DISTRIBUTION_CREDENTIALS: ${{ secrets.STAGING_APP_DISTRIBUTION_CREDENTIALS }}
+          # Read-only Remote Config staging account: the pose catalog the key points to (scripts/setup-check-app.sh).
+          REMOTE_CONFIG_CREDENTIALS: ${{ secrets.STAGING_REMOTE_CONFIG_CREDENTIALS }}
           STAGING_TESTER_GROUPS: ${{ vars.STAGING_TESTER_GROUPS || 'team' }}
         run: |
           result=0
diff --git a/.github/workflows/start-run.yml b/.github/workflows/start-run.yml
index 61f285c..b8a87b4 100644
--- a/.github/workflows/start-run.yml
+++ b/.github/workflows/start-run.yml
@@ -37,7 +37,8 @@ jobs:
   start:
     if: github.ref == 'refs/heads/main'
     runs-on: ubuntu-latest
-    timeout-minutes: 5
+    # 10, not 5: scripts/setup-check.sh --staging includes the pose catalog check of scripts/setup-check-app.sh.
+    timeout-minutes: 10
     steps:
       - uses: actions/checkout@v5
 
@@ -90,6 +91,8 @@ jobs:
         env:
           GOOGLE_SERVICES: ${{ secrets.STAGING_GOOGLE_SERVICES_JSON }}
           DISTRIBUTION_CREDENTIALS: ${{ secrets.STAGING_APP_DISTRIBUTION_CREDENTIALS }}
+          # Read-only Remote Config staging account: the pose catalog the key points to (scripts/setup-check-app.sh).
+          REMOTE_CONFIG_CREDENTIALS: ${{ secrets.STAGING_REMOTE_CONFIG_CREDENTIALS }}
           STAGING_TESTER_GROUPS: ${{ vars.STAGING_TESTER_GROUPS || 'team' }}
         run: |
           if ! scripts/setup-check.sh --staging; then
```

**`docs/ops/staging-and-runs.md`:** mục 1.1, bước 6 mới (tài khoản `remote-config-reader`); mục 1.3, dòng bí mật và lệnh `gh secret set`; mục 2.3, điều từ chối mới của `start-run`; mục 2.4, đoạn "Catalog tư thế (camera-pose, plan T-065)" (điều kiểm, gợi ý CDN, "tải file lên trước rồi mới đổi khóa, đổi xong chạy lại **env-canary**", lỗi tạm chỉ là chú ý). **`CLAUDE.md`**, mục Lệnh: dòng `setup-check.sh` nhắc catalog và `scripts/setup-check-app.sh`.

Chữ thêm vào (bản cài thử, đã kiểm `plan-check` và `secret-scan`):

- Mục 1.1, sau bước 5: "6. Tạo tài khoản dịch vụ chỉ đọc Remote Config, để `scripts/setup-check.sh --staging` kiểm catalog tư thế mà khóa `poses_catalog_path` trỏ tới (plan T-065; chỉ có ở camera-pose): như bước 4, tên `remote-config-reader`, vai trò **Firebase Remote Config Viewer** (`roles/firebaseremoteconfig.viewer`), không vai trò nào khác; tạo khóa JSON, cất như bước 4.6."
- Mục 1.3, bảng: "| `STAGING_REMOTE_CONFIG_CREDENTIALS` | Toàn bộ nội dung file khóa JSON của tài khoản `remote-config-reader` (mục 1.1, bước 6) |" sau dòng `STAGING_APP_DISTRIBUTION_CREDENTIALS`; khối lệnh: "gh secret set STAGING_REMOTE_CONFIG_CREDENTIALS < <file khóa JSON của remote-config-reader>".
- Mục 2.3, bước 3, sau "tên miền file tĩnh không phân giải được qua 1.1.1.1 hay 8.8.8.8": "; catalog tư thế mặc định của app, hay catalog mà một giá trị của khóa `poses_catalog_path` trên Remote Config staging trỏ tới, hay một file khởi đầu của các catalog đó không trả `200` (`scripts/setup-check-app.sh`, plan T-065)".
- Mục 2.4, sau đoạn về job `setup-check`: "**Catalog tư thế (camera-pose, plan T-065).** Kiểm tra trên gồm `scripts/setup-check-app.sh`: catalog mặc định của app (`DEFAULT_CATALOG_PATH`, bản staging của CI dùng nó) và mọi giá trị của khóa `poses_catalog_path` trên Remote Config staging, kể cả giá trị theo điều kiện; mỗi catalog phải trả `200`, đọc được, không đòi app mới hơn, và ảnh nhỏ cùng nét viền của mọi tư thế `starter` phải trả `200`. Một file không trả `200` mà hỏi lại kèm tham số chống bộ nhớ đệm thì trả `200` nghĩa là file đã có nhưng Cloudflare còn giữ câu trả lời 404 cũ: xóa bộ nhớ đệm của URL đó (2026-10-27, một 404 được giữ với `max-age=31536000`)." và hai gạch đầu dòng: "**Đổi khóa `poses_catalog_path` hay tải catalog mới:** tải catalog và mọi file của nó lên R2 trước, rồi mới thêm hay đổi giá trị của khóa, kể cả giá trị theo điều kiện chưa bật (một giá trị trỏ tới file chưa có cũng làm kiểm tra đỏ và chặn mọi phiên). Xong thì chạy lại **env-canary** bằng tay, vì lần chạy buổi sáng không thấy thay đổi sau đó." và "Không đọc được Remote Config (mạng, `firebase-tools` lỗi, quá 120 giây) chỉ là chú ý: catalog mặc định vẫn được kiểm. Thiếu bí mật hay tài khoản thiếu vai trò thì là lỗi."
- `CLAUDE.md`, mục Lệnh, dòng `setup-check.sh` thành: "Kiểm thiết lập của app (file R2 bắt buộc, DNS của tên miền file tĩnh, catalog tư thế theo Remote Config và bộ khởi đầu của nó): `scripts/setup-check.sh`; danh sách file ở `docs/ops/required-assets.txt`, phần kiểm riêng của app ở `scripts/setup-check-app.sh`."

**Sau khi merge** (phiên làm task): chạy job `setup-check` trên nhánh chính một lần bằng `gh workflow run env-canary.yml --ref main` (job `fire` bỏ qua khi phiên đang chạy, nên không tốn hạn mức trong phiên), chờ bằng `scripts/wait-run.sh --workflow env-canary.yml --since <giờ>`.
- Xanh: ghi vào nhật ký.
- Đỏ vì catalog hay file thật hỏng (dòng `LỖI - khóa poses_catalog_path trỏ tới catalog ...`, `LỖI - catalog mặc định ...`, `LỖI - file khởi đầu ...`, có hay không câu CDN): giữ issue `setup-broken`, ghi vào Cần người kiểm tra của báo cáo; đó là việc của người.
- Đỏ vì `000`, `429`, `5xx` còn lại sau lần hỏi lại: chạy lại một lần sau vài phút; xanh thì issue tự đóng; ghi vào nhật ký.
- Đỏ vì chính phần mới (thiếu bí mật dù bí mật có, lỗi quyền dù vai trò đúng, hay phân loại sai): revert commit merge của task theo `docs/auto-run.md`, mục 3, Khi nhánh chính đỏ vì task của phiên, chạy lại để issue tự đóng, ghi lý do vào nhật ký và báo cáo.

### 3.2. Danh sách file

| Thao tác | Đường dẫn | Nội dung thay đổi |
| --- | --- | --- |
| Tạo | `scripts/setup-check-app.sh` | Phần kiểm catalog tư thế của app (mục 3.1) |
| Sửa | `scripts/setup-check.sh` | `http_status`, `cdn_hint`, gọi `scripts/setup-check-app.sh`, chú thích đầu file (mục 3.1) |
| Sửa | `scripts/tests/run.sh` | Khối `setup-check`: `SETUP_CHECK_APP_CHECKS=none` trong hàm `setup_check` của test có sẵn; khối mới "camera-pose only (plan T-065)" với các fake `curl`, `dig`, `gcloud`, `firebase` và 68 test (một test chỉ chạy khi máy có `timeout`) |
| Sửa | `.github/workflows/env-canary.yml` | Job `setup-check`: biến `REMOTE_CONFIG_CREDENTIALS`, `timeout-minutes: 10` |
| Sửa | `.github/workflows/start-run.yml` | Bước "Kiểm tra thiết lập staging, file R2 và DNS": biến `REMOTE_CONFIG_CREDENTIALS`; `timeout-minutes: 10` |
| Sửa | `docs/ops/staging-and-runs.md` | Mục 1.1, 1.3, 2.3, 2.4 (mục 3.1) |
| Sửa | `CLAUDE.md` | Dòng `setup-check.sh` ở mục Lệnh |
| Sửa | `docs/features/quality-gate/tickets/T-065-setup-check-kiem-catalog-that.md` | Trạng thái |
| Sửa | `docs/features/quality-gate/plans/T-065-setup-check-kiem-catalog-that.md` | Nhật ký |

L-020, các file nhắc tới file mà plan sửa (`git grep -n 'setup-check'`, `env-canary.yml`, `start-run.yml`):

- `scripts/setup-check.sh` được `env-canary.yml`, `start-run.yml`, `scripts/tests/run.sh` (cả ba trong plan) gọi và được nhắc trong `docs/auto-run.md`, `docs/ops/setup-new-app.md`, `docs/ops/staging-and-runs.md`, `docs/CLAUDE.md`, `CLAUDE.md`, `docs/ops/required-assets.txt`. Cách gọi, mã thoát và dạng dòng (`ok   - `, `LỖI - `/`::error::`, `CHÚ Ý - `/`::warning::`, `== `) giữ nguyên, nên bước ghi issue của `env-canary.yml` (lọc `^(::error::|::warning::|ok   - )`) và các tài liệu không phải đổi; `docs/auto-run.md` mục 2 đọc nhãn `setup-broken`, không đổi. `setup-new-app.md` (của dự án mẫu) không nhắc catalog: file kiểm của app chỉ có ở app.
- `.github/workflows/env-canary.yml`, `start-run.yml` được nhắc trong chú thích và thông báo của `scripts/env-canary.sh`, `scripts/main-guard-status.sh`, `.github/workflows/resume-run.yml` và tài liệu; plan chỉ thêm một biến môi trường và đổi `timeout-minutes`, không đổi tên workflow, job, đầu vào hay bước, nên các file đó không bị ảnh hưởng.
- `scripts/tests/run.sh` được `scripts/check.sh` chạy và `.github/workflows/pr-check.yml` nhắc tới (bộ lọc đường dẫn và bước chạy test của script); plan chỉ sửa khối `setup-check` và thêm một khối, không đổi tên hay cách chạy, nên `pr-check.yml` không bị ảnh hưởng.
- `docs/ops/staging-and-runs.md` được nhắc theo số mục trong thông báo hay chú thích của `.github/workflows/device-session.yml`, `.github/workflows/staging.yml`, `scripts/cloud-env-check.sh` và nhiều tài liệu; plan chỉ thêm bước, dòng và đoạn vào mục 1.1, 1.3, 2.3, 2.4, không đổi số hay tên mục, nên các file đó không bị ảnh hưởng.
- `CLAUDE.md` được nhắc trong chú thích của `app/src/androidTest/kotlin/com/vglstudio/camera/pose/line/guide/AppLaunchTest.kt` và `app/src/androidTest/kotlin/com/vglstudio/camera/pose/line/guide/SettingsNavigationTest.kt` (bước 10, quy tắc test trên emulator); plan chỉ sửa một dòng ở mục Lệnh, không đổi quy tắc test, nên hai test đó không bị ảnh hưởng.

### 3.3. Cờ tính năng, quảng cáo, dữ liệu, quyền, thư viện

- Cờ tính năng: Không có
- Quảng cáo: Không chạm
- Sự kiện phân tích: Không chạm
- File R2: chỉ đọc (`HEAD`, `GET`); không đổi cách đặt tên hay đường dẫn. Tham số `?setup-check-nocache=` chỉ để hỏi lại, không ghi gì
- **Quyền truy cập:** Không thêm (không đổi app)
- **Thư viện và SDK:** Không thêm vào app. Trên máy của GitHub: `firebase-tools@15.32.1` qua `npx` (cùng phiên bản bản 1 đã chạy thử thật và `preflight.sh` cũ đã dùng), `jq`, `curl`, `timeout` có sẵn trên runner Ubuntu

### 3.4. Phương án dự phòng

- **Điểm chưa chắc 1:** chữ thật của lỗi khi tài khoản thiếu vai trò. Lần thử ở bản 1 (project không tồn tại) cho `Error: Failed to get Firebase Remote Config template for project <id>.` ở stdout, không khớp mẫu `LỖI`. Nếu lỗi thiếu vai trò cũng có dạng đó thì tài khoản mất vai trò sau này chỉ ra `CHÚ Ý` (catalog mặc định vẫn được kiểm, phần Remote Config tắt lặng lẽ). Không có cách chuyển trong phiên; câu hỏi 6.
- **Điểm chưa chắc 2:** một ngày R2 chậm làm job vượt 10 phút: bước ghi issue không chạy, `start-run` không bắt đầu phiên. **Điều kiện chuyển:** lần chạy `env-canary` sau khi merge có job `setup-check` hết giờ. **Cách làm thay:** không đổi trong phiên; plan phiên bản 3 có người duyệt thêm giới hạn tổng thời gian cho phần catalog (ví dụ chỉ kiểm 20 file khởi đầu đầu tiên mỗi catalog) hay `curl --parallel`. Tiêu chí và test của mục 4 giữ nguyên.
- **Điểm chưa chắc 3:** một CDN khác Cloudflare có thể coi tham số là cùng một địa chỉ (không bỏ qua bộ nhớ đệm): khi đó không có câu CDN, dòng `LỖI` vẫn đúng như hôm nay. Không cần làm thay.

## 4. Kiểm thử

Mọi test nằm trong `scripts/tests/run.sh`, chạy bằng `scripts/check.sh`. Bản đầy đủ của test ở `diff` dưới.

| Tiêu chí chấp nhận | Loại test | Tên test hoặc cách kiểm tra |
| --- | --- | --- |
| Catalog mặc định | Test script | `the default catalog and its starter files pass`, `only the starter files are checked`, `a missing starter file of the default catalog stops`, `names the missing starter file`, `a default catalog that answers 404 stops`, `without --staging Remote Config is not read` (đối chứng) |
| CDN giữ 404 cũ | Test script | `a starter file the CDN still answers 404 for stops`, `says the CDN keeps an old answer`, `a required file the CDN still answers 404 for names the cache`, `a file really missing gets no CDN hint` (đối chứng) |
| Catalog dùng được | Test script | `a catalog that is not JSON stops`, `a catalog cut short is downloaded once more`, `a catalog for a newer app stops`, `a pose that is not an object does not hide the starter files`, `no ok line for a catalog with a missing starter file` (đối chứng) |
| Lỗi tạm của R2 | Test script | `a 503 that passes on the retry is not a failure`, `a 429 that passes on the retry is not a failure`, `a 429 seen twice counts`, `a required file that fails to connect once is asked again` |
| Sự cố ngày 26, mọi loại giá trị | Test script | `a Remote Config catalog that answers 404 stops`, `names the key and the missing catalog`, `a conditional value that answers 404 stops`, `a rollout value that answers 404 stops`, `a key inside a parameter group is read`, `a Remote Config catalog that exists passes` (đối chứng), `the key's catalog and the default catalog are both downloaded once` |
| Như app | Test script | `no parameter means the default catalog only`, `useInAppDefault points the key to the default catalog`, `an invalid value warns and the key falls back to the default catalog, as the app does`, `a personalization value warns`, `no readable value warns and checks the default catalog`, `a path used by two values is checked once` |
| Thiếu bí mật, thiếu vai trò, công cụ lỗi | Test script | `a missing Remote Config secret stops`, `without the secret the default catalog is still checked`, `a Remote Config account without the role stops`, `a missing permissions error printed on stdout stops`, `a transient error with 403 inside a number only warns`, `a Remote Config command that cannot run only warns and checks the default catalog` (chữ lỗi thật của bản 1), `a Remote Config command that hits the time limit only warns`, `a Remote Config command that hangs is stopped and only warns` (khi máy có `timeout`), `a template that is not JSON only warns`, `a Firebase secret without project id only warns for Remote Config` |
| Đúng project, đúng bí mật | Test script | `the staging project is passed to remoteconfig:get`, `remoteconfig:get uses the Remote Config secret` |
| Đường đỏ cho lần thử | Test script | `the extra catalog of a trial run is checked` |
| Không lộ bí mật | Test script | `never prints secret values` (khóa `RC-MARKER-1` trong bí mật và trong lỗi giả, mã project `demo-staging`, token `fake-token`) |
| Móc của app | Test script | `sources the app checks file and counts its problems`; test có sẵn của `setup-check` chạy với `SETUP_CHECK_APP_CHECKS=none`, không đổi kỳ vọng |
| Quy tắc đường dẫn giống `AssetPath` | Test script | 7 test `... is a valid asset path`, 14 test `... is not a valid asset path` |
| Workflow đọc được Remote Config thật | Kiểm tra thủ công (người: cần bí mật staging, chạy trên GitHub; môi trường cloud bị proxy chặn R2, L-005) | Trước khi duyệt: đã chạy trên nhánh tạm (mục 4.1). Sau khi merge: một lần chạy xanh job `setup-check` trên nhánh chính (mục 3.1, Sau khi merge); không chạy đường đỏ trên nhánh chính |

**Ngoại lệ viết test trước:** hai workflow, `docs/ops/staging-and-runs.md`, `CLAUDE.md` (không có test; kiểm bằng lần chạy thủ công ở bảng trên).

Test thêm vào `scripts/tests/run.sh` (đã chạy, mục 4.1):

```diff
diff --git a/scripts/tests/run.sh b/scripts/tests/run.sh
index 14c21b9..a5cf127 100755
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
@@ -2043,6 +2043,213 @@ expect "setup-check: rejects an unknown argument" 1 "$(setup_check "$all_files"
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
+v1=poses/catalog/v1/catalog-587b5394.json
+v1key=poses_catalog_v1_catalog-587b5394.json
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
+    scripts/setup-check.sh "$@" > "$work/sca.out" 2>&1 || code=$?
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
+sca_reset; sca_catalog 2 > "$sca/body/$v1key"
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
+expect "setup-check app: no parameter means the default catalog only" "0 1 0" \
+  "$(sca_check --staging) $(sca_calls "^GET .*/$v1\$") $(sca_calls "^GET .*/v3/")"
+sca_reset; sca_rc '{"defaultValue": {"useInAppDefault": true}}'; echo 404 > "$sca/codes/$v1key"
+expect "setup-check app: useInAppDefault points the key to the default catalog" 1 \
+  "$(sca_check --staging > /dev/null; sca_lines "^LỖI - khóa poses_catalog_path trỏ tới catalog $v1")"
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

- **Đã chạy thử:** 2026-10-27, worktree `wt-t065v2` từ nhánh chính `1fc049e` (sau PR #179), bản cài thử ở mục 3.1 và test ở mục 4 (không commit trên nhánh plan).
- **Khối test riêng** (`scripts/tests/sc-block.sh`, tệp tạm đã xóa: dòng 1 đến 26 của `run.sh` cộng khối từ "# setup-check.sh: required R2 files" tới trước "# device-session.sh records"), chạy hai lần: không có và có lệnh `timeout` (máy viết plan là macOS, không có `timeout`; lần thứ hai dùng một lệnh `timeout` tạm bằng `perl` trong `PATH`, nhận cả `-k`, chỉ để chạy test treo).
  - **Code hiện tại** (`setup-check.sh` của nhánh chính, chưa có `setup-check-app.sh`; test mới): 24 qua, 61 thất bại. 17 test cũ của `setup-check` qua. Thất bại đúng lý do: phần catalog không chạy (`expected '0 1', got '0 0'`, `expected '1', got '0'`, `expected '1 1', got '0 0'`); `rc.args` không có (`expected '1', got ''`); 21 test quy tắc đường dẫn (`valid_asset_path: command not found`, mã 127); `a required file that fails to connect once is asked again` (`expected '0 2', got '1 1'`: chưa hỏi lại); `a 429 that passes on the retry is not a failure`; `sources the app checks file` (`expected '1 1', got '0 0'`). 7 đối chứng qua: `without --staging Remote Config is not read`, `no ok line for a catalog with a missing starter file`, `a file really missing gets no CDN hint`, `a 503 that passes on the retry is not a failure` (code cũ không kiểm file khởi đầu; bắt đột biến 8), `a 429 seen twice counts`, `a Remote Config catalog that exists passes`, `never prints secret values`.
  - **Bản cài thử:** có `timeout` 85 qua, không `timeout` 84 qua (test treo bỏ qua), `failures=0`.
- **Toàn bộ `scripts/tests/run.sh`** trên bản cài thử (JDK 21): 797 ok, mã 0 (730 trước đó, cộng 67 test mới chạy được trên macOS).
- **`scripts/check.sh`** trên bản cài thử: qua, mã 0 (`== Cổng chất lượng: QUA`; 1168 test JVM, 0 lỗi; 97 ảnh screenshot; test của script trong đó).
- **Đột biến** (mỗi lần sửa một chỗ của bản cài thử, chạy lại khối có `timeout`):

| # | Đột biến | Test thất bại |
| --- | --- | --- |
| 1 | Bỏ `conditionalValues` | 4 |
| 2 | Bỏ `parameterGroups` | 1 |
| 3 | Giá trị sai dạng không đưa khóa về catalog mặc định | 1 |
| 4 | Lỗi thiếu quyền chỉ là chú ý | 2 |
| 5 | Bỏ kiểm `minAppSchema` | 1 |
| 6 | Kiểm mọi tư thế, không chỉ `starter` | 2 |
| 7 | Mẫu khớp `403` trần | 1 |
| 8 | Không hỏi lại `5xx`, `429` | 1 |
| 9 | Không hỏi lại `429` | 1 |
| 10 | Bỏ kiểm template là object JSON | 1 |
| 11 | In `ok` khi có file khởi đầu hỏng | 1 |
| 12 | Bỏ `objects` khi đọc `poses` | 1 |
| 13 | Không báo khi không đọc được giá trị nào | 1 |
| 14 | Thiếu mã project là `LỖI` thay vì `CHÚ Ý` | 1 |
| 15 | Bỏ `rolloutValue` | 1 |
| 16 | Bỏ giới hạn thời gian | 1 |
| 17 | Lỗi của công cụ chỉ đọc từ stderr | 1 |
| 18 | Bí mật rỗng coi là có | 1 |
| 19 | `remoteconfig:get` dùng nhầm bí mật App Distribution | 1 |
| 20 | Không gộp đường dẫn trùng (`sort -u`) | 4 |
| 21 | Không tải lại catalog bị cắt | 1 |
| 22 | Không kiểm catalog mặc định khi có `--staging` | 4 |
| 23 | Bỏ câu CDN ở file khởi đầu | 1 |
| 24 | Bỏ câu CDN ở file bắt buộc | 1 |
| 25 | `setup-check.sh` không gọi file kiểm của app | 37 |
| 26 | Đọc Remote Config trong `( ... )` (bộ đếm lỗi mất) | 3 |

Lần đầu đột biến 3 và 9 sống: thêm kỳ vọng dòng `ok   - khóa poses_catalog_path trỏ tới catalog <mặc định>` vào test giá trị sai dạng, và test `a 429 that passes on the retry is not a failure`; chạy lại thì cả hai bị bắt.

- **Workflow, chạy thử thật** (2026-10-27 02:55 UTC; không có issue `setup-broken` nào mở, không có phiên nào chạy hay sắp bắt đầu). Nhánh tạm `trial/T-065-v2` từ `1fc049e` với code của mục 3.1 và một workflow tạm `trial-t065.yml` (chạy khi đẩy nhánh đó, hai job `green` và `red` chạy đúng lệnh của job `setup-check`, cùng ba bí mật, chỉ in các dòng kiểm, không mở hay đóng issue nào). Lần chạy 40441226351; nhánh tạm đã xóa sau đó. Không chạy thử bằng `env-canary.yml` vì chạy tay workflow đó còn kích hoạt routine `env-canary` (tốn hạn mức) và mở, đóng issue thật; phần ghi issue không đổi.
  - **Đường xanh:** mã 0, 81 giây. `ok   - catalog mặc định của app poses/catalog/v1/catalog-587b5394.json và 8 file khởi đầu trả 200`; `ok   - khóa poses_catalog_path trỏ tới catalog poses/catalog/v3/catalog-512aa695.json và 100 file khởi đầu trả 200`; `== Thiết lập: QUA`. Giá trị khóa đọc từ Remote Config thật.
  - **Đường đỏ** (`SETUP_CHECK_EXTRA_CATALOG=poses/catalog/v9/catalog-00000000.json`): mã 1, 79 giây. `LỖI - khóa poses_catalog_path trỏ tới catalog poses/catalog/v9/catalog-00000000.json trả '404', cần 200: app dừng ở màn tải. ...`, không có câu CDN (file thật sự không có); `== Thiết lập còn 1 lỗi.`
  - Không có `firebase-debug.log` ở cả hai job.
  - **Tham số chống bộ nhớ đệm trên Cloudflare thật:** `HEAD .../borrowed/poses/img/v1/w01/outline.webp?setup-check-nocache=<số>` trả 200 với `cf-cache-status: DYNAMIC` (không qua bộ nhớ đệm); ngày 27 trước khi xóa bộ nhớ đệm, cùng file hỏi kèm tham số trả 200 (`MISS`) trong khi địa chỉ thường trả 404 (`HIT`). Câu CDN chưa gặp lại trên R2 thật vì hiện không có file nào bị giữ 404.
- **Chưa kiểm được:** chữ thật của lỗi thiếu vai trò (mục 3.4, điểm 1; câu hỏi 6).

## 5. Chấm rủi ro

**Điểm chặn bị chạm:** 7 (CI: `.github/workflows/env-canary.yml`, `start-run.yml`; quy trình phiên tự chạy: điều kiện bắt đầu phiên).

| Tiêu chí | Điểm (0 đến 3) | Lý do |
| --- | --- | --- |
| A. Phạm vi thay đổi | 2 | Một script mới, một script của dự án mẫu, hai workflow, test, hai tài liệu |
| B. Độ rõ của yêu cầu | 1 | Hai sự cố rõ; còn câu hỏi 3, 6 không đổi cách làm chính |
| C. Khả năng kiểm chứng tự động | 2 | Script có test; workflow và `remoteconfig:get` thật chỉ kiểm bằng lần chạy tay |
| D. Ảnh hưởng chéo | 2 | `setup-check` quyết định phiên có bắt đầu không; sửa file của dự án mẫu |
| E. Độ mới của kỹ thuật | 1 | `remoteconfig:get` đã chạy thật ở bản 1 và lần thử này; móc file kiểm của app là cách mới nhưng chỉ một lệnh `.` |
| **Tổng** | 8 | **Mức: Cao** (điểm chặn 7) |

## 6. Ảnh hưởng chéo

- File hoặc thành phần dùng chung bị chạm: `scripts/setup-check.sh` (của dự án mẫu), `env-canary.yml`, `start-run.yml`; `scripts/tests/run.sh` (khối `setup-check` và khối mới).
- Task khác trong sprint có thể xung đột: T-063 (`scripts/tests/run.sh`, khối `token-usage`, khác khối; plan T-063 cần phiên bản 2 sau PR #179), T-062 (`device-session.yml`, khác file), T-064 (không chung file).
- Lần đồng bộ dự án mẫu sau: giữ thay đổi của `setup-check.sh` khi gộp, cho tới khi dự án mẫu nhận đề xuất (mục 3.1).
- Đề xuất thứ tự chạy: độc lập, chạy được bất kỳ lúc nào; đặt sau T-062, T-063.

## 7. Điều kiện dừng riêng

- Ngay trước task, có issue `setup-broken` đang mở: chạy task bình thường (task không làm issue đó tệ hơn), nhưng phần Sau khi merge chỉ ghi kết quả, không kết luận theo issue cũ.
- Bí mật `STAGING_REMOTE_CONFIG_CREDENTIALS` không còn (lần chạy `env-canary` gần nhất báo `thiếu bí mật` sau khi merge): revert theo mục 3.1, Sau khi merge.

## 8. Câu hỏi cho người duyệt

1. Phần riêng của app nằm ở file mới `scripts/setup-check-app.sh`, `setup-check.sh` của dự án mẫu chỉ thêm móc gọi nó, thử lại và câu CDN (đề xuất về dự án mẫu). **AI đề xuất: đồng ý**, để lần đồng bộ sau không đè phần của app và dự án mẫu nhận được phần chung.
2. Catalog mặc định luôn được kiểm, kể cả khi khóa trên Remote Config trỏ catalog khác và không có `--staging`. **AI đề xuất: đồng ý**: bản staging của CI và máy chưa có giá trị Remote Config dùng nó (sự cố ngày 27); thêm khoảng 9 lệnh.
3. Phiên mở bằng `claude --cloud` chỉ đọc nhãn `setup-broken` (`docs/auto-run.md`, mục 2), nên thay đổi Remote Config hay R2 sau lần chạy buổi sáng vẫn lọt; `start-run.yml` thì chạy lại kiểm tra. Có cho phiên `claude --cloud` tự chạy job `setup-check` lúc bắt đầu không? **AI đề xuất: chưa**, chỉ thêm dòng "đổi khóa thì chạy lại env-canary" ở `docs/ops/staging-and-runs.md`; nếu muốn, làm ticket riêng.
4. Catalog hay file khởi đầu hỏng là `LỖI` (chặn phiên ở `start-run`, mở `setup-broken`). **AI đề xuất: `LỖI`**: người sửa trong vài phút, còn phiên chạy với catalog hỏng thì `ui-test` của mọi task đỏ (ngày 27) hay `device-session` hỏng mọi kịch bản.
5. Ghi chú: `CHÚ Ý` của phần Remote Config lặp lại nhiều ngày (công cụ hỏng lâu) không mở issue; nếu thấy cần, một ticket sau có thể biến `CHÚ Ý` lặp lại quá 3 ngày thành issue.
6. Lỗi thiếu vai trò có thể chỉ ra `CHÚ Ý` (mục 3.4, điểm 1): giữ như plan, hay coi mọi lỗi `Failed to get Firebase Remote Config template` khi bí mật có mặt là `LỖI`? **AI đề xuất: giữ như plan**: coi là `LỖI` thì một lỗi mạng tạm của công cụ cũng chặn phiên cả ngày; catalog mặc định vẫn được kiểm. Muốn chắc, Tuấn thử một lần bằng tay (gỡ vai trò của `remote-config-reader`, chạy job `setup-check` trên nhánh tạm, gán lại).

## 9. Quyết định duyệt

<!-- Người duyệt điền. AI không điền mục này. -->

- **Quyết định:** Duyệt / Dừng / Duyệt có điều kiện
- **Người duyệt:** <tên>, **duyệt chéo:** <tên, nếu rủi ro cao>
- **Ghi chú:** <điều kiện, yêu cầu sửa, trả lời câu hỏi ở mục 8>

## 10. Nhật ký thực hiện

<!-- AI ghi sau khi chạy task. -->
