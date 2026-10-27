---
ticket: T-065
title: "`preflight` kiểm catalog mà máy staging thật sự tải (khóa `poses_catalog_path` trên Remote Config) và các file khởi đầu của nó"
status: draft                # draft | approved | stopped | done
version: 1                   # tăng khi plan được sửa lại sau phản hồi hoặc sau khi dừng
risk_score:                  # tổng điểm theo risk-scoring.md, để trống nếu chạm điểm chặn
risk_level: high             # low | medium | high
token_estimate: 7.6M         # cỡ M, số gợi ý của scripts/token-usage.py estimate (2026-10-26), không nâng tay (L-081)
blocking_points: [ci, process]
run_order:                   # AI điền sau họp đánh giá rủi ro
reviewer:                    # người duyệt
cross_reviewer:              # bắt buộc khi risk_level là high
approved_at:
---

# PLAN T-065: `PREFLIGHT` KIỂM CATALOG MÀ MÁY STAGING THẬT SỰ TẢI

## 1. Tóm tắt

`scripts/preflight.sh` thêm điều kiểm thứ 5: đọc template Remote Config của project staging bằng `firebase-tools remoteconfig:get` (tài khoản dịch vụ chỉ đọc, bí mật mới `STAGING_REMOTE_CONFIG_CREDENTIALS`), lấy mọi giá trị của khóa `poses_catalog_path` (giá trị mặc định và từng giá trị theo điều kiện), tải từng catalog trên R2 và kiểm: trả 200, là JSON có `poses`, `minAppSchema` không lớn hơn của app, và ảnh nhỏ cùng nét viền của mọi tư thế `starter` đều trả 200. Khóa không có, không có giá trị mặc định, `useInAppDefault` hay giá trị sai dạng thì kiểm đường dẫn mặc định của app, đúng như `DefaultPoseCatalogRepository`. Sai thì dòng `LỖI` nêu tên khóa và đường dẫn, `preflight.yml` mở issue `setup-broken` như mọi điều kiểm khác. Phần script và 62 test mới (cùng 2 kỳ vọng cũ đổi) đã chạy thật trên code hiện tại (62 thất bại đúng lý do, 2 đối chứng qua) và trên bản cài thử (87 qua), cùng 21 đột biến (mục 4.1). Workflow đã chạy thử thật trên nhánh thử sau khi Tuấn tạo bí mật: đường xanh, đường đỏ và xanh lại đều đúng (mục 4.1).

## 2. Hiểu yêu cầu

Không có spec nghiệp vụ. Yêu cầu là sự cố ngày 2026-10-26 (plan T-062, mục 4.1 và câu hỏi 4; Tuấn chọn làm ticket riêng): khóa `poses_catalog_path` trên Remote Config staging trỏ tới `poses/catalog/v3/catalog-c2e0ce0d.json`, file trả 404, app dừng ở màn tải; `preflight.yml` xanh vì chỉ kiểm `poses/catalog/v1/catalog-587b5394.json` viết cứng trong `scripts/required-assets.txt`.

| Yêu cầu | Tiêu chí chấp nhận | Cách hiểu của AI |
| --- | --- | --- |
| Bắt được sự cố hôm nay | Khi khóa trỏ tới một file trả 404 thì `preflight.sh` trả mã 1 và có dòng `LỖI: khóa poses_catalog_path trỏ tới <đường dẫn>, file này trả 404 ...` | Kiểm đúng file mà máy tải, không phải danh sách viết cứng |
| Mọi máy staging, kể cả theo điều kiện | Khi giá trị theo một điều kiện trỏ tới file 404 thì cũng đỏ | Không đánh giá điều kiện (phiên bản app, phần trăm, thuộc tính người dùng) được ở đây; kiểm mọi giá trị là chặt hơn và đơn giản. Project staging chỉ có app staging (điều kiểm 1), production ở project khác (ADR-0007), nên mọi giá trị trong template đều có thể tới một máy staging |
| Giống cách app chọn | Khi không có khóa, không có giá trị mặc định, `useInAppDefault`, giá trị sai dạng (`AssetPath.isValid` của `:core:assets`), hay không đọc được giá trị nào (ví dụ chỉ có giá trị cá nhân hóa) thì kiểm `DEFAULT_CATALOG_PATH`; giá trị sai dạng và giá trị cá nhân hóa có thêm dòng `CHÚ Ý` | Khóa và đường dẫn mặc định đọc thẳng từ `PosesSettings.kt`, nên script theo code của app. Quy tắc đường dẫn chép sang bash, có test đối chiếu cùng danh sách ví dụ của `AssetPathTest` |
| Catalog dùng được | Khi catalog không phải JSON có `poses`, hay `minAppSchema` lớn hơn `APP_SCHEMA` của `CatalogParser.kt`, thì đỏ. Một phần tử của `poses` không phải object không che các file khởi đầu khác | Hai lỗi khác cũng làm app dừng ở màn tải. `jq` lỗi thì không im lặng ra 0 file và in `QUA` |
| Bộ khởi đầu | Khi ảnh nhỏ (`preview.small`) hay nét viền (`outline`) của một tư thế `starter` trả khác 200 thì đỏ, nêu tên file | Đúng hai file mà `loadStarterSet` tải. Tư thế không phải `starter` không kiểm (tải lúc dùng) |
| Báo rõ khi thiếu quyền | Khi thiếu bí mật thì `LỖI: thiếu bí mật STAGING_REMOTE_CONFIG_CREDENTIALS ...`; khi lỗi của công cụ có `HTTP Error: 403`, `PERMISSION_DENIED` hay `missing the following required permissions` (thông báo của `firebase-tools` khi tài khoản thiếu quyền) thì `LỖI: ... (cần vai trò Firebase Remote Config Viewer, hoặc API Remote Config chưa bật)` | Hai việc người làm một lần. Không khớp `403` trần: một lỗi tạm có số `84031` không được thành `LỖI` |
| Không chặn phiên vì lỗi tạm | Khi lệnh `firebase-tools` không chạy được vì lý do khác, chạy quá 120 giây (`timeout`, mã 124), hay ghi ra tệp không phải JSON, thì chỉ `CHÚ Ý` và vẫn kiểm đường dẫn mặc định. Khi R2 trả `000`, `429` hay `5xx` một lần thì thử lại một lần trước khi tính là lỗi | Như điều kiểm nhóm người test (plan T-030, mục 3.4): không tệ hơn hôm nay. Không có giới hạn thời gian thì lệnh treo làm job hết 10 phút trước bước "Ghi kết quả": không mở hay đóng issue nào. Điều kiểm 5 gửi nhiều request hơn khoảng 10 lần, nên một lần lỗi tạm của R2 không được chặn cả ngày; thay đổi này cũng áp dụng cho điều kiểm 4 (trước đây chỉ thử lại `000`) |
| Không lộ bí mật | Mã project, nội dung khóa và lỗi của công cụ không bao giờ in ra | Lỗi của `firebase-tools` ghi vào tệp tạm, chỉ dùng làm điều kiện |
| Thử được đường đỏ | Chạy tay `preflight.yml` với ô `extra_catalog` thì đường dẫn đó được kiểm như một giá trị của khóa | Như ô `extra_asset` có sẵn |
| Người biết phải chạy lại | `docs/ops/staging-and-runs.md`, mục 2.8: đổi khóa `poses_catalog_path` hay tải catalog mới thì chạy lại **preflight** | Lần chạy buổi sáng không thấy thay đổi buổi chiều (hôm nay đổi sau 14:36 UTC) |

Thiết kế tham chiếu: Không có thiết kế riêng.

ADR liên quan: ADR-0003 (Remote Config), ADR-0007 (staging, production tách project), ADR-0009 (bí mật chỉ của staging), ADR-0012 (Firebase), ADR-0015 (đường dẫn R2 có phiên bản), ADR-0016 (bí mật của các workflow).

## 3. Giải pháp

### 3.1. Cách làm

**Việc Tuấn làm trước khi duyệt (một lần, project staging):**

1. Firebase Console, project **staging** (không phải production), bánh răng > **Project settings** > tab **Service accounts** > **Manage service account permissions**.
2. **+ Create service account**, tên `remote-config-reader`. **Select a role**: gõ `Remote Config`, chọn **Firebase Remote Config Viewer** (`roles/firebaseremoteconfig.viewer`). Không gán vai trò nào khác. **Done**.
3. Email của tài khoản > tab **Keys** > **Add key** > **Create new key** > **JSON**.
4. `gh secret set STAGING_REMOTE_CONFIG_CREDENTIALS --repo VGL-Studio/camera-pose < <file khóa JSON>`, rồi chuyển file khóa ra khỏi máy vào trình quản lý mật khẩu (`docs/ops/staging-and-runs.md`, mục 1.3).
5. Báo AI để chạy thử workflow trên nhánh thử (mục 4.1), lúc không có issue `setup-broken` nào đang mở và không có phiên nào sắp bắt đầu: lần chạy đỏ của nhánh thử mở `setup-broken` cho cả repo, lần chạy xanh đóng cả issue thật đang mở.

Merge PR của task khi chưa có bí mật thì `preflight` đỏ ngay (`LỖI: thiếu bí mật ...`), issue `setup-broken` chặn mọi phiên. Vì vậy danh sách chạy chỉ đưa T-065 vào khi bí mật đã có và lần chạy thử đã xanh (mục 7).

**`scripts/preflight.sh`** (bản cài thử đã chạy, mục 4.1):

- Đọc `CATALOG_PATH_KEY`, `DEFAULT_CATALOG_PATH` từ `PosesSettings.kt` và `APP_SCHEMA` từ `CatalogParser.kt` bằng `sed`, như `ASSETS_BASE_URL` đọc từ `app/build.gradle.kts`.
- Hàm `status_of` gom lệnh `curl`, thử lại một lần khi `000`, `429` hay `5xx`; `check_assets` dùng lại hàm này. Hành vi của điều kiểm 4 đổi một chút: trước đây chỉ thử lại `000`; test cũ vẫn qua.
- `remoteconfig:get` chạy dưới `timeout -k 10 120` khi máy có lệnh `timeout` (runner Ubuntu có; macOS không có thì chạy không giới hạn, như `cloud-env-check.sh`, `device-session.sh`).
- `valid_asset_path`: cùng quy tắc với `AssetPath.isValid`.
- `catalog_values`: `jq` lấy giá trị của khóa ở `parameters` hay trong `parameterGroups`, giá trị mặc định và mọi `conditionalValues`; `value`, `rolloutValue.value`; `@default` cho không có khóa, không có giá trị, `useInAppDefault`; `@unknown` (ví dụ giá trị cá nhân hóa) thì `CHÚ Ý`.
- Chú thích của điều kiểm 4 đổi thành "000, 429 and 5xx are retried once before counting as a failure (status_of)".
- Lỗi của `remoteconfig:get` đọc từ cả stdout và stderr (`> <tệp lỗi> 2>&1`): `firebase-tools` in lỗi ra stdout; template ghi vào tệp `-o`. Tệp lỗi ở thư mục tạm, xóa khi script xong, không in ra. `firebase-tools` có thể ghi `firebase-debug.log` vào thư mục làm việc của runner khi lỗi; workflow không tải tệp này lên artifact nào, runner bị xóa sau job.
- Catalog trả 200 mà không đọc được (bị cắt giữa đường) thì tải lại một lần qua `status_of` rồi mới tính là hỏng.
- `check_remote_catalog`: điều kiểm thứ 5, gọi sau `check_assets`. Lỗi của `jq` không in ra; danh sách tư thế đọc bằng `.poses[] | objects`; đọc template xong mà không có đường dẫn nào thì `CHÚ Ý` và kiểm đường dẫn mặc định. Không có giá trị nào của khóa trả về lỗi thì dòng `QUA: catalog <đường dẫn> (khóa poses_catalog_path) và <n> file khởi đầu đều trả 200.`, mỗi đường dẫn khác nhau một dòng.
- Biến môi trường mới: `REMOTE_CONFIG_CREDENTIALS_FILE`, `EXTRA_CATALOG_PATH`. Không dùng `mapfile` (bash 3.2 của máy macOS).

```diff
diff --git a/scripts/preflight.sh b/scripts/preflight.sh
index fa2f17b..dab5394 100755
--- a/scripts/preflight.sh
+++ b/scripts/preflight.sh
@@ -3,7 +3,9 @@
 #   - the staging Firebase secret is a real file with the staging app (L-002, L-012);
 #   - the App Distribution tester groups exist (L-012);
 #   - the static files domain resolves through public DNS servers (L-027);
-#   - every required file on R2 answers 200 (L-011).
+#   - every required file on R2 answers 200 (L-011);
+#   - the catalog that staging devices get (poses_catalog_path on Remote Config, every value of the key) and its
+#     starter files answer 200 (plan T-065).
 # Runs on GitHub (preflight.yml, start-run.yml): the claude --cloud proxy blocks R2 (L-005).
 # No arguments; inputs come from the environment:
 #   GOOGLE_SERVICES_FILE           file the workflow wrote from STAGING_GOOGLE_SERVICES_JSON
@@ -13,6 +15,8 @@
 #   ASSETS_BASE_URL                default: the staging value in app/build.gradle.kts
 #   DNS_SERVERS                    default "1.1.1.1 8.8.8.8"
 #   REQUIRED_ASSETS                default scripts/required-assets.txt
+#   REMOTE_CONFIG_CREDENTIALS_FILE file the workflow wrote from STAGING_REMOTE_CONFIG_CREDENTIALS (read-only role)
+#   EXTRA_CATALOG_PATH             one more catalog path checked like a value of the key (red-path trial only)
 #   DIG_CMD, CURL_CMD, FIREBASE_CMD, PREFLIGHT_RETRY_SECONDS replace the real commands and wait in tests.
 # Exit codes: 0 every check passed (warnings allowed), 1 at least one check failed.
 # Secret values are never printed: jq results are only used as conditions, command output is discarded.
@@ -26,9 +30,21 @@ TESTER_GROUPS="${TESTER_GROUPS:-team}"
 DNS_SERVERS="${DNS_SERVERS:-1.1.1.1 8.8.8.8}"
 REQUIRED_ASSETS="${REQUIRED_ASSETS:-scripts/required-assets.txt}"
 RETRY_SECONDS="${PREFLIGHT_RETRY_SECONDS:-5}"
+# The key and the fallback path are read from the app code, so the check follows the app (plan T-065).
+POSES_SETTINGS=core/poses/src/main/kotlin/com/vglstudio/camera/pose/line/guide/core/poses/internal/PosesSettings.kt
+CATALOG_PARSER=core/poses/src/main/kotlin/com/vglstudio/camera/pose/line/guide/core/poses/internal/CatalogParser.kt
+CATALOG_PATH_KEY="$(sed -nE 's/.*CATALOG_PATH_KEY = "([^"]+)".*/\1/p' "$POSES_SETTINGS" 2> /dev/null | head -n 1)"
+DEFAULT_CATALOG_PATH="$(sed -nE 's/.*DEFAULT_CATALOG_PATH = "([^"]+)".*/\1/p' "$POSES_SETTINGS" 2> /dev/null | head -n 1)"
+APP_SCHEMA="$(sed -nE 's/.*APP_SCHEMA = ([0-9]+).*/\1/p' "$CATALOG_PARSER" 2> /dev/null | head -n 1)"
+tmp_dir="$(mktemp -d)"
+trap 'rm -rf "$tmp_dir"' EXIT
 read -r -a DIG <<< "${DIG_CMD:-dig}"
 read -r -a CURL <<< "${CURL_CMD:-curl}"
 read -r -a FIREBASE <<< "${FIREBASE_CMD:-npx --yes firebase-tools@15.32.1}"
+# remoteconfig:get gets a time limit, so a hang cannot kill the job before preflight.yml records the result.
+REMOTE_CONFIG_TIMEOUT="${PREFLIGHT_REMOTE_CONFIG_TIMEOUT:-120}"
+REMOTE_CONFIG_GET=("${FIREBASE[@]}")
+if command -v timeout > /dev/null; then REMOTE_CONFIG_GET=(timeout -k 10 "$REMOTE_CONFIG_TIMEOUT" "${FIREBASE[@]}"); fi
 
 passed=0
 failed=0
@@ -119,7 +135,20 @@ check_dns() {
   return 0
 }
 
-# 4. Required files on R2 (L-011). 000 (no connection) is retried once before counting as a failure.
+# HTTP status of a file on R2; 000 (no connection), 429 and 5xx are retried once (plan T-065: one passing hiccup must
+# not open setup-broken for a whole day). Extra curl options go before the URL.
+status_of() { # <path> [curl options]
+  local path="$1" code
+  shift
+  code="$("${CURL[@]}" -sS --max-time 20 -w '%{http_code}' "$@" "${base_url%/}/$path" 2> /dev/null || true)"
+  if [[ -z "$code" || "$code" == "000" || "$code" == "429" || "$code" == 5?? ]]; then
+    sleep "$RETRY_SECONDS"
+    code="$("${CURL[@]}" -sS --max-time 20 -w '%{http_code}' "$@" "${base_url%/}/$path" 2> /dev/null || true)"
+  fi
+  echo "${code:-000}"
+}
+
+# 4. Required files on R2 (L-011). 000, 429 and 5xx are retried once before counting as a failure (status_of).
 check_assets() {
   local count=0 bad=0 code
   if [[ -z "$base_url" ]]; then
@@ -134,13 +163,9 @@ check_assets() {
     path="$(sed -E 's/^[[:space:]]+//; s/[[:space:]]+$//' <<< "$path")"
     [[ -z "$path" || "$path" == \#* ]] && continue
     count=$((count + 1))
-    code="$("${CURL[@]}" -sS -o /dev/null -I --max-time 20 -w '%{http_code}' "${base_url%/}/$path" 2> /dev/null || true)"
-    if [[ "$code" == "000" || -z "$code" ]]; then
-      sleep "$RETRY_SECONDS"
-      code="$("${CURL[@]}" -sS -o /dev/null -I --max-time 20 -w '%{http_code}' "${base_url%/}/$path" 2> /dev/null || true)"
-    fi
+    code="$(status_of "$path" -o /dev/null -I)"
     if [[ "$code" != "200" ]]; then
-      fail "$path trả ${code:-000} (cần 200; L-011)."
+      fail "$path trả $code (cần 200; L-011)."
       bad=$((bad + 1))
     fi
   done < "$REQUIRED_ASSETS"
@@ -148,10 +173,122 @@ check_assets() {
   return 0
 }
 
+# Same rule as AssetPath.isValid of :core:assets: relative segments, and a version folder or a versioned file name.
+valid_asset_path() {
+  local path="$1" segment='[A-Za-z0-9][A-Za-z0-9._-]*'
+  [[ "$path" =~ ^($segment/)*$segment$ ]] || return 1
+  [[ "$path" =~ (^|/)v[0-9]+(\.[0-9]+)*/ ]] && return 0
+  [[ "${path##*/}" =~ ^.+[._-](v[0-9]+(\.[0-9]+)*|[0-9a-f]{8,})\.[A-Za-z0-9]+$ ]]
+}
+
+# Every value a staging device can get for the key: the default value and each conditional value, because the
+# conditions (app version, percent, user properties) cannot be evaluated here. "@default" is the app's own default
+# (no parameter, no default value, useInAppDefault); "@unknown" is a value kind the script does not read.
+catalog_values() { # <template file>
+  jq -r --arg k "$CATALOG_PATH_KEY" '
+    ([.parameters[$k]?] + [.parameterGroups[]?.parameters[$k]?]) | map(select(. != null)) | .[0] as $p
+    | if $p == null then "@default"
+      else ([$p.defaultValue] + [($p.conditionalValues // {}) | to_entries[] | .value]) | .[]
+        | if . == null or .useInAppDefault == true then "@default"
+          elif (.value | type) == "string" then .value
+          elif (.rolloutValue.value | type) == "string" then .rolloutValue.value
+          else "@unknown" end
+      end' "$1"
+}
+
+# 5. The catalog that staging devices get (plan T-065): Remote Config may point to a file that is not on R2 while the
+# required list above still passes (2026-10-26: poses/catalog/v3/... answered 404, the app stopped on the loading
+# screen). A missing or unreadable key means the app's default path, as in DefaultPoseCatalogRepository.
+check_remote_catalog() {
+  local credentials="${REMOTE_CONFIG_CREDENTIALS_FILE:-}" project template="$tmp_dir/remote-config.json" value
+  local paths="" code catalog count bad file
+  if [[ -z "$CATALOG_PATH_KEY" || -z "$DEFAULT_CATALOG_PATH" ]]; then
+    fail "không đọc được khóa và đường dẫn catalog mặc định trong $POSES_SETTINGS."
+    return
+  fi
+  if [[ -z "$credentials" || ! -s "$credentials" ]]; then
+    fail "thiếu bí mật STAGING_REMOTE_CONFIG_CREDENTIALS (đọc khóa $CATALOG_PATH_KEY trên Remote Config staging; docs/ops/staging-and-runs.md, mục 2.8)."
+    return
+  fi
+  project="$(jq -r '.project_info.project_id // empty' "${GOOGLE_SERVICES_FILE:-/dev/null}" 2> /dev/null || true)"
+  if [[ -z "$project" ]]; then
+    warn "không đọc được khóa $CATALOG_PATH_KEY: bí mật Firebase không có mã project; chỉ kiểm đường dẫn mặc định."
+    paths="$DEFAULT_CATALOG_PATH"
+  elif ! GOOGLE_APPLICATION_CREDENTIALS="$credentials" "${REMOTE_CONFIG_GET[@]}" \
+      remoteconfig:get --project "$project" -o "$template" > "$tmp_dir/remote-config.err" 2>&1 \
+      || ! jq -e 'type == "object"' "$template" > /dev/null 2>&1; then
+    # firebase-tools prints its errors on stdout, so both streams go to the file (the template goes to -o).
+    # Only these texts mean a missing role or API; any other failure (network, npm, timeout 124) is a tool failure.
+    if grep -qE 'HTTP Error: 403|PERMISSION_DENIED|missing the following required permissions' \
+        "$tmp_dir/remote-config.err" 2> /dev/null; then
+      fail "tài khoản của STAGING_REMOTE_CONFIG_CREDENTIALS không đọc được Remote Config staging (cần vai trò Firebase Remote Config Viewer, hoặc API Remote Config chưa bật)."
+      return
+    fi
+    warn "không đọc được khóa $CATALOG_PATH_KEY (lệnh firebase-tools không chạy được); chỉ kiểm đường dẫn mặc định."
+    paths="$DEFAULT_CATALOG_PATH"
+  else
+    while IFS= read -r value; do
+      case "$value" in
+        @default) paths+=$'\n'"$DEFAULT_CATALOG_PATH" ;;
+        @unknown) warn "khóa $CATALOG_PATH_KEY có một giá trị script không đọc được (cá nhân hóa?); kiểm bằng tay." ;;
+        *)
+          if valid_asset_path "$value"; then
+            paths+=$'\n'"$value"
+          else
+            warn "khóa $CATALOG_PATH_KEY có giá trị \"$value\" không hợp lệ; app dùng đường dẫn mặc định $DEFAULT_CATALOG_PATH."
+            paths+=$'\n'"$DEFAULT_CATALOG_PATH"
+          fi
+          ;;
+      esac
+    done < <(catalog_values "$template" 2> /dev/null)
+    if [[ -z "$paths" ]]; then
+      warn "không đọc được giá trị nào của khóa $CATALOG_PATH_KEY trong template Remote Config; chỉ kiểm đường dẫn mặc định."
+      paths="$DEFAULT_CATALOG_PATH"
+    fi
+  fi
+  [[ -n "${EXTRA_CATALOG_PATH:-}" ]] && paths+=$'\n'"$EXTRA_CATALOG_PATH"
+  while IFS= read -r path; do
+    [[ -z "$path" ]] && continue
+    catalog="$tmp_dir/catalog.json"
+    rm -f "$catalog"
+    code="$(status_of "$path" -o "$catalog")"
+    if [[ "$code" != "200" ]]; then
+      fail "khóa $CATALOG_PATH_KEY trỏ tới $path, file này trả $code (cần 200): app dừng ở màn tải. Sửa khóa trên Remote Config staging hoặc tải file lên R2."
+      continue
+    fi
+    if ! jq -e '.poses | type == "array"' "$catalog" > /dev/null 2>&1; then
+      # A file cut short in transit is downloaded once more before it counts as broken.
+      rm -f "$catalog"
+      code="$(status_of "$path" -o "$catalog")"
+    fi
+    if [[ "$code" != "200" ]] || ! jq -e '.poses | type == "array"' "$catalog" > /dev/null 2>&1; then
+      fail "catalog $path (khóa $CATALOG_PATH_KEY) không phải catalog đọc được (JSON, có danh sách poses)."
+      continue
+    fi
+    if [[ -n "$APP_SCHEMA" ]] && jq -e --argjson app "$APP_SCHEMA" '(.minAppSchema // 0) > $app' "$catalog" > /dev/null 2>&1; then
+      fail "catalog $path (khóa $CATALOG_PATH_KEY) cần app mới hơn: minAppSchema lớn hơn $APP_SCHEMA của app."
+      continue
+    fi
+    count=0
+    bad=0
+    while IFS= read -r file; do
+      count=$((count + 1))
+      code="$(status_of "$file" -o /dev/null -I)"
+      if [[ "$code" != "200" ]]; then
+        fail "file khởi đầu $file của catalog $path trả $code (cần 200): app dừng ở màn tải bộ khởi đầu."
+        bad=$((bad + 1))
+      fi
+    done < <(jq -r '.poses[] | objects | select(.starter == true) | (.preview | objects | .small), .outline | strings' "$catalog" 2> /dev/null)
+    [[ "$bad" -eq 0 ]] && pass "catalog $path (khóa $CATALOG_PATH_KEY) và $count file khởi đầu đều trả 200."
+  done < <(sort -u <<< "$paths")
+  return 0
+}
+
 check_google_services
 check_tester_groups
 check_dns
 check_assets
+check_remote_catalog
 
 say "Tổng: $passed điều qua, $failed điều lỗi."
 if [[ -n "${GITHUB_STEP_SUMMARY:-}" ]]; then
```

**ADR-0016** có bảng bí mật của repo nhưng `origin: template`, nên app không sửa (`docs/CLAUDE.md`, Khi viết ADR). Bí mật mới ghi ở `docs/ops/staging-and-runs.md`, mục 1.3; đề xuất về dự án mẫu thêm dòng vào bảng của ADR-0016 ở bản của dự án mẫu.

**Workflow** (`.github/workflows/preflight.yml`, `.github/workflows/start-run.yml`): đưa bí mật vào tệp tạm `umask 077`, xóa khi xong, như hai bí mật có sẵn; `preflight.yml` thêm ô `extra_catalog`.

```diff
--- a/.github/workflows/preflight.yml
+++ b/.github/workflows/preflight.yml
@@ header comment
-# Firebase staging secret, App Distribution tester groups, public DNS of the static files domain, required R2 files.
+# Firebase staging secret, App Distribution tester groups, public DNS of the static files domain, required R2 files,
+# and the catalog that staging devices get (poses_catalog_path on Remote Config) with its starter files (plan T-065).
@@ on.workflow_dispatch.inputs
       extra_asset:
         description: Đường dẫn R2 thêm vào danh sách cho lần chạy này (chỉ để kiểm đường đỏ), mặc định rỗng
         required: false
         default: ''
+      extra_catalog:
+        description: Đường dẫn catalog kiểm như một giá trị của khóa poses_catalog_path (chỉ để kiểm đường đỏ), mặc định rỗng
+        required: false
+        default: ''
@@ step "Kiểm tra môi trường trước phiên"
           DISTRIBUTION_CREDENTIALS: ${{ secrets.STAGING_APP_DISTRIBUTION_CREDENTIALS }}
+          REMOTE_CONFIG_CREDENTIALS: ${{ secrets.STAGING_REMOTE_CONFIG_CREDENTIALS }}
           TESTER_GROUPS: ${{ vars.STAGING_TESTER_GROUPS || 'team' }}
           EXTRA_ASSET: ${{ inputs.extra_asset }}
+          EXTRA_CATALOG_PATH: ${{ inputs.extra_catalog }}
         run: |
           umask 077
           export GOOGLE_SERVICES_FILE="${RUNNER_TEMP}/google-services.json"
           export DISTRIBUTION_CREDENTIALS_FILE="${RUNNER_TEMP}/distribution-credentials.json"
-          trap 'rm -f "${GOOGLE_SERVICES_FILE}" "${DISTRIBUTION_CREDENTIALS_FILE}"' EXIT
+          export REMOTE_CONFIG_CREDENTIALS_FILE="${RUNNER_TEMP}/remote-config-credentials.json"
+          trap 'rm -f "${GOOGLE_SERVICES_FILE}" "${DISTRIBUTION_CREDENTIALS_FILE}" "${REMOTE_CONFIG_CREDENTIALS_FILE}"' EXIT
           printf '%s' "${GOOGLE_SERVICES}" > "${GOOGLE_SERVICES_FILE}"
           printf '%s' "${DISTRIBUTION_CREDENTIALS}" > "${DISTRIBUTION_CREDENTIALS_FILE}"
+          printf '%s' "${REMOTE_CONFIG_CREDENTIALS}" > "${REMOTE_CONFIG_CREDENTIALS_FILE}"

--- a/.github/workflows/start-run.yml
+++ b/.github/workflows/start-run.yml
@@ step "Kiểm tra môi trường staging"
           DISTRIBUTION_CREDENTIALS: ${{ secrets.STAGING_APP_DISTRIBUTION_CREDENTIALS }}
+          REMOTE_CONFIG_CREDENTIALS: ${{ secrets.STAGING_REMOTE_CONFIG_CREDENTIALS }}
           TESTER_GROUPS: ${{ vars.STAGING_TESTER_GROUPS || 'team' }}
@@
           export DISTRIBUTION_CREDENTIALS_FILE="${RUNNER_TEMP}/distribution-credentials.json"
-          trap 'rm -f "${GOOGLE_SERVICES_FILE}" "${DISTRIBUTION_CREDENTIALS_FILE}"' EXIT
+          export REMOTE_CONFIG_CREDENTIALS_FILE="${RUNNER_TEMP}/remote-config-credentials.json"
+          trap 'rm -f "${GOOGLE_SERVICES_FILE}" "${DISTRIBUTION_CREDENTIALS_FILE}" "${REMOTE_CONFIG_CREDENTIALS_FILE}"' EXIT
           printf '%s' "${GOOGLE_SERVICES}" > "${GOOGLE_SERVICES_FILE}"
           printf '%s' "${DISTRIBUTION_CREDENTIALS}" > "${DISTRIBUTION_CREDENTIALS_FILE}"
+          printf '%s' "${REMOTE_CONFIG_CREDENTIALS}" > "${REMOTE_CONFIG_CREDENTIALS_FILE}"
```

**`docs/ops/staging-and-runs.md`:**

- Mục 1.1: bước 6 mới, tạo tài khoản `remote-config-reader` như trên.
- Mục 1.3: dòng `STAGING_REMOTE_CONFIG_CREDENTIALS` trong bảng bí mật và lệnh `gh secret set`.
- Mục 2.8: gạch đầu dòng thứ 5 (catalog mà khóa `poses_catalog_path` trỏ tới và file khởi đầu của nó trả 200, kiểm mọi giá trị kể cả theo điều kiện), ô `extra_catalog` ở dòng Chạy tay, và dòng mới: "**Đổi khóa `poses_catalog_path` hay tải catalog mới:** tải catalog và các file của nó lên R2 trước, rồi mới thêm hay đổi giá trị của khóa, kể cả giá trị theo điều kiện (một giá trị theo điều kiện soạn sẵn trỏ tới file chưa tải lên cũng làm preflight đỏ và chặn mọi phiên); xong thì chạy lại **preflight** ngay, vì lần chạy buổi sáng không thấy thay đổi sau đó".

**Đề xuất về dự án mẫu** (`docs/ops/from-app.md`, ghi vào mô tả PR, mở issue sau khi merge): dự án mẫu không có `:core:poses`; phần chung là "kiểm các file R2 mà một khóa Remote Config trỏ tới", với khóa và đường dẫn mặc định khai báo theo app; thêm dòng `STAGING_REMOTE_CONFIG_CREDENTIALS` vào bảng bí mật của ADR-0016.

**Sau khi merge** (phiên làm task): phiên chạy `preflight.yml` trên nhánh chính (`gh workflow run preflight.yml --ref main`, chờ bằng `scripts/wait-run.sh --workflow preflight.yml --since <giờ>`).
- Xanh: ghi vào nhật ký.
- Đỏ vì catalog thật hỏng (dòng `LỖI: khóa poses_catalog_path trỏ tới ...`, `LỖI: file khởi đầu ...`, `LỖI: catalog ...`): giữ issue `setup-broken`, ghi vào Cần người kiểm tra của báo cáo; đó là việc của người.
- Đỏ vì `000`, `429` hay `5xx` còn lại sau lần thử lại: chạy lại `preflight.yml` một lần sau vài phút; xanh thì issue tự đóng; ghi vào nhật ký.
- Đỏ vì chính điều kiểm 5 (thiếu bí mật, lỗi quyền dù bí mật đúng, hay phân loại sai): revert commit merge của task theo `docs/auto-run.md`, mục 3, Khi nhánh chính đỏ vì task của phiên, chạy lại `preflight.yml` để issue tự đóng, ghi lý do vào nhật ký và báo cáo.

### 3.2. Danh sách file

| Thao tác | Đường dẫn | Nội dung thay đổi |
| --- | --- | --- |
| Sửa | `scripts/preflight.sh` | Điều kiểm thứ 5 `check_remote_catalog`, `status_of`, `valid_asset_path`, `catalog_values` (mục 3.1) |
| Sửa | `scripts/tests/run.sh` | Khối preflight: `fake-curl` ghi phương thức và tải nội dung từ `$PF/body/`, `fake-firebase` trả `remoteconfig:get`, `preflight_reset` có template và catalog, 62 test mới (một test chỉ chạy khi máy có `timeout`), 2 kỳ vọng cũ đổi (mục 4) |
| Sửa | `.github/workflows/preflight.yml` | Bí mật `STAGING_REMOTE_CONFIG_CREDENTIALS`, ô `extra_catalog`, chú thích đầu file |
| Sửa | `.github/workflows/start-run.yml` | Bí mật `STAGING_REMOTE_CONFIG_CREDENTIALS` ở bước "Kiểm tra môi trường staging" |
| Sửa | `docs/ops/staging-and-runs.md` | Mục 1.1, 1.3, 2.8 (mục 3.1) |
| Sửa | `docs/features/quality-gate/tickets/T-065-preflight-kiem-catalog-that.md` | Trạng thái |
| Sửa | `docs/features/quality-gate/plans/T-065-preflight-kiem-catalog-that.md` | Nhật ký |

L-020, các file nhắc tới file mà plan sửa:

- `scripts/preflight.sh` được `preflight.yml`, `start-run.yml` (cả hai trong plan) và `scripts/tests/run.sh` (trong plan) gọi; `scripts/required-assets.txt` không đổi. Mã thoát và các dòng `QUA`, `LỖI`, `CHÚ Ý`, `Tổng` giữ dạng cũ, nên bước "Ghi kết quả" của `preflight.yml` (lọc `^(QUA|LỖI|CHÚ Ý|Tổng):`) không phải đổi. `docs/auto-run.md` mục 2 đọc kết luận của `preflight.yml`, không đổi.
- `.github/workflows/start-run.yml` được nhắc trong chú thích hay thông báo của `.github/workflows/resume-run.yml`, `scripts/env-canary.sh`, `scripts/main-guard-status.sh`; không file nào đọc nội dung của nó. Plan chỉ thêm một bí mật vào bước kiểm môi trường, không đổi tên workflow, đầu vào hay bước khác, nên các file đó không bị ảnh hưởng.
- `docs/ops/staging-and-runs.md` được nhắc trong chú thích hay thông báo của `.github/workflows/device-session.yml`, `.github/workflows/env-canary.yml`, `.github/workflows/resume-run.yml`, `.github/workflows/staging.yml`, `scripts/cloud-env-check.sh`, `scripts/env-canary.sh`; các thông báo đó trỏ tới mục 1, 2, 2.8 hiện có. Plan chỉ thêm bước và dòng vào mục 1.1, 1.3, 2.8, không đổi số hay tên mục, nên các file đó không bị ảnh hưởng.
- `scripts/tests/run.sh` được `scripts/check.sh` chạy và được nhắc trong chú thích của `scripts/main-guard-status.sh`, `scripts/merge-if-green.sh`, `scripts/wait-run.sh`; plan chỉ sửa khối preflight, không đổi cách chạy, nên các file đó không bị ảnh hưởng.

### 3.3. Cờ tính năng, quảng cáo, dữ liệu, quyền, thư viện

- Cờ tính năng: Không có
- Quảng cáo: Không chạm
- Sự kiện phân tích: Không chạm
- File R2: chỉ đọc (`HEAD`, `GET`); không đổi cách đặt tên hay đường dẫn
- **Quyền truy cập:** Không thêm (không đổi app)
- **Thư viện và SDK:** Không thêm. `firebase-tools@15.32.1` đã dùng ở `preflight.sh` (`FIREBASE_CMD`), lệnh `remoteconfig:get` thuộc cùng gói; `jq`, `curl` có sẵn trên runner

### 3.4. Phương án dự phòng

- **Điểm chưa chắc:** dạng lệnh `firebase-tools remoteconfig:get --project <id> -o <tệp>` và dạng JSON nó ghi (giống REST API `GET .../remoteConfig`: `parameters`, `parameterGroups`, `conditionalValues`) chưa chạy được thật, vì chưa có bí mật và máy viết plan không có Node.
- **Điều kiện chuyển:** lần chạy thử (mục 4.1) cho `CHÚ Ý: không đọc được khóa ...` dù bí mật đúng, hay tệp ghi ra không có `parameters`.
- **Cách làm thay:** lấy token bằng chính `firebase-tools` không được thì gọi REST API: `curl -fsS -H "Authorization: Bearer $(...)" https://firebaseremoteconfig.googleapis.com/v1/projects/<id>/remoteConfig -o <tệp>`, với token lấy từ tài khoản dịch vụ bằng bước `google-github-actions/auth` hay lệnh `gcloud` có sẵn trên runner. Đây là thay đổi workflow mới, nên đi qua plan phiên bản 2 có người duyệt, không đổi trong phiên. Tiêu chí và test của mục 4 giữ nguyên (test giả lập lệnh, không phụ thuộc cách lấy).
- **Điểm chưa chắc 2:** tổ chức Google Cloud có thể bật chính sách `iam.disableServiceAccountKeyCreation`, làm bước tạo khóa JSON báo `Service account key creation is disabled` (như `docs/ops/staging-and-runs.md`, mục 1.1, bước 4.5).
- **Cách làm thay:** quản trị viên tắt chính sách đó cho project staging; không thì plan dừng ở bước Tuấn tạo bí mật, chưa duyệt.

## 4. Kiểm thử

Mọi test nằm trong khối preflight của `scripts/tests/run.sh`, chạy bằng `scripts/check.sh` (`scripts/tests/run.sh`). Bản đầy đủ của test ở `diff` dưới.

| Tiêu chí chấp nhận | Loại test | Tên test hoặc cách kiểm tra |
| --- | --- | --- |
| Bắt sự cố hôm nay | Test script | `preflight: a Remote Config catalog path that answers 404 is refused`, `names the key and the missing catalog` |
| Theo điều kiện | Test script | `a conditional value that answers 404 is refused`, `names the conditional catalog` |
| Tải đúng catalog của khóa, không tải mặc định | Test script | `the catalog of the key is the one downloaded` (một `GET` của v3, không `GET` v1) |
| Catalog có thật thì qua | Test script, đối chứng | `a Remote Config catalog that exists passes` |
| Bộ khởi đầu | Test script | `the starter files of the catalog are checked, not the others`, `a missing starter file is refused`, `names the missing starter file` |
| Catalog cần app mới hơn | Test script | `a catalog for a newer app is refused`, `says the catalog needs a newer app` |
| Như app: không có khóa, `useInAppDefault`, giá trị sai dạng | Test script | `no parameter means the default path`, `useInAppDefault checks the app default path`, `an invalid path warns and checks the default, as the app does` |
| Khóa trong nhóm tham số | Test script | `a key inside a parameter group is read` |
| Thiếu bí mật, thiếu vai trò, công cụ lỗi | Test script | `a missing Remote Config secret is refused`, `names the Remote Config secret`, `a Remote Config account without the role is refused`, `names the missing role`, `a Remote Config command that cannot run only warns` |
| Project staging được truyền | Test script | `the staging project is passed to remoteconfig:get` |
| Phân loại lỗi của công cụ | Test script | `the firebase-tools missing permissions text is refused`, `a transient error with 403 inside a number only warns`, `a Remote Config command that cannot run only warns and checks the default`, `a Remote Config command that hits the time limit only warns` (mã 124), `a Remote Config command that hangs is stopped and only warns` (chỉ chạy khi máy có lệnh `timeout`), `a template that is not JSON only warns`, `a Firebase secret without project id only warns for Remote Config` |
| Catalog hỏng, rollout, cá nhân hóa | Test script | `a catalog that is not JSON is refused`, `a rollout value that answers 404 is refused`, `a personalization value warns`, `no readable value checks the default path` |
| Không in `QUA` sai | Test script | `no QUA line for a catalog with a missing starter file` (đối chứng), `a pose that is not an object does not hide the starter files` |
| Lỗi tạm của R2 | Test script | `a 503 that passes on the retry is not a failure`, `a 429 that passes on the retry is not a failure`, `a catalog cut short is downloaded once more` |
| Lỗi quyền in ra stdout, bí mật rỗng, đúng bí mật | Test script | `a missing permissions error printed on stdout is refused`, `an empty Remote Config secret file is refused` (workflow luôn ghi tệp, kể cả khi bí mật rỗng), `remoteconfig:get uses the Remote Config credentials` |
| Một đường dẫn dùng ở nhiều giá trị | Test script | `a path used by two values is checked once` (một `GET`, một dòng `QUA: catalog`) |
| Ô `extra_catalog` | Test script | `the extra catalog path of a manual run is checked` |
| Quy tắc đường dẫn giống `AssetPath` | Test script | 7 test `... is a valid asset path`, 14 test `... is not a valid asset path`, cùng danh sách ví dụ của `AssetPathTest` |
| Không lộ bí mật | Test script, có sẵn | `never prints secret values`: nay gồm cả lỗi giả của công cụ có `MARKER-SECRET-1` và mã project `proj-test-1` |
| Hai kỳ vọng cũ đổi theo điều kiểm mới | Test script, sửa | `a ready environment has one QUA line per check`: 4 thành 5; `reports every failing check`: 2 thành 3 (đường dẫn mặc định 404 nay đỏ ở cả điều kiểm 4 và 5). Sửa ở commit test của bước 3, thất bại trên code cũ (mục 4.1) |
| Workflow đọc được Remote Config thật | Kiểm tra thủ công (người: cần bí mật do Tuấn tạo; chạy trên GitHub, môi trường cloud bị proxy chặn R2, L-005) | Trước khi duyệt: chạy `preflight.yml` trên nhánh thử: xanh, có dòng `QUA: catalog poses/catalog/v1/catalog-587b5394.json (khóa poses_catalog_path) ...` (hay đường dẫn đang đặt); chạy lại với `extra_catalog=poses/catalog/v9/catalog-00000000.json`: đỏ, issue `setup-broken` có dòng `LỖI: khóa poses_catalog_path trỏ tới poses/catalog/v9/...`, chạy lại không ô thì issue tự đóng. Sau khi merge: chỉ một lần chạy xanh `preflight.yml` trên nhánh chính (không chạy đường đỏ trên nhánh chính: lần chạy đỏ mở `setup-broken` và làm hỏng điều kiện "xanh trong 26 giờ" của phiên) |

Test thêm vào `scripts/tests/run.sh` (đã chạy, mục 4.1):

```diff
diff --git a/scripts/tests/run.sh b/scripts/tests/run.sh
index fb6ab8c..94a7bef 100755
--- a/scripts/tests/run.sh
+++ b/scripts/tests/run.sh
@@ -1695,13 +1695,30 @@ SH
 cat > "$work/bin/fake-curl" << 'SH'
 #!/usr/bin/env bash
 # The status code for a URL is the first line of $PF/curl/<path with / as _>, consumed one per call; default 200.
+# With -o <file> and a 200, the body is $PF/body/<same key> when it exists (plan T-065).
 url="${*: -1}"; key="$(sed -E 's#^https?://[^/]+/##; s#/#_#g' <<< "$url")"; f="$PF/curl/$key"
+out=""; prev=""; for a in "$@"; do [[ "$prev" == "-o" ]] && out="$a"; prev="$a"; done
 if [[ -s "$f" ]]; then code="$(head -n 1 "$f")"; [[ "$(wc -l < "$f")" -gt 1 ]] && sed -i.bak 1d "$f"; else code=200; fi
+if [[ "$code" == 200 && -n "$out" && "$out" != /dev/null && -f "$PF/body/$key" ]]; then cp "$PF/body/$key" "$out"; fi
+method=GET; for a in "$@"; do [[ "$a" == -I ]] && method=HEAD; done
+echo "$method $url" >> "$PF/curl.urls"
 printf '%s' "$code"
 SH
 cat > "$work/bin/fake-firebase" << 'SH'
 #!/usr/bin/env bash
 # Prints $PF/groups.json and exits with the code in $PF/firebase-exit (default 0).
+# remoteconfig:get copies $PF/remote-config.json to the -o file, prints $PF/rc-err to stderr and exits with the code
+# in $PF/rc-exit (default 0); its arguments go to $PF/rc.args (plan T-065).
+if [[ " $* " == *" remoteconfig:get "* ]]; then
+  echo "GAC=${GOOGLE_APPLICATION_CREDENTIALS:-} $*" >> "$PF/rc.args"
+  out=""; prev=""; for a in "$@"; do [[ "$prev" == "-o" ]] && out="$a"; prev="$a"; done
+  cat "$PF/rc-err" >&2 2>/dev/null || true
+  cat "$PF/rc-out" 2>/dev/null || true
+  [[ -f "$PF/rc-sleep" ]] && sleep "$(cat "$PF/rc-sleep")"
+  code="$(cat "$PF/rc-exit" 2>/dev/null || echo 0)"
+  [[ "$code" == 0 && -f "$PF/remote-config.json" ]] && cp "$PF/remote-config.json" "$out"
+  exit "$code"
+fi
 code="$(cat "$PF/firebase-exit" 2>/dev/null || echo 0)"
 [[ "$code" == 0 ]] && cat "$PF/groups.json"
 exit "$code"
@@ -1720,12 +1737,25 @@ preflight_reset() { # sets up a ready environment; a test then breaks one thing
   printf '104.21.40.144\n172.67.153.9\n' > "$pf/dig-1.1.1.1"
   printf '104.21.40.144\n' > "$pf/dig-8.8.8.8"
   printf '# Required files\n\nposes/catalog/v1/catalog-587b5394.json\n' > "$pf/assets.txt"
+  echo '{"type": "service_account", "private_key": "MARKER-SECRET-1"}' > "$pf/rc-credentials.json"
+  remote_config '{"defaultValue": {"value": "poses/catalog/v1/catalog-587b5394.json"}}'
+  mkdir -p "$pf/body"
+  catalog_body > "$pf/body/poses_catalog_v1_catalog-587b5394.json"
+}
+remote_config() { # <parameter JSON of poses_catalog_path, or "" for no parameter>
+  if [[ -n "$1" ]]; then printf '{"parameters": {"poses_catalog_path": %s}, "conditions": []}' "$1" > "$pf/remote-config.json"
+  else echo '{"parameters": {"other_key": {"defaultValue": {"value": "x"}}}}' > "$pf/remote-config.json"; fi
+}
+catalog_body() { # [minAppSchema]: one starter pose and one other pose
+  printf '{"schema": 1, "minAppSchema": %s, "poses": [' "${1:-1}"
+  printf '{"id": "w01", "starter": true, "preview": {"small": "poses/img/v1/w01/preview_540.webp", "large": "poses/img/v1/w01/preview_1200.webp"}, "outline": "poses/img/v1/w01/outline.webp"},'
+  printf '{"id": "w02", "starter": false, "preview": {"small": "poses/img/v1/w02/preview_540.webp", "large": "poses/img/v1/w02/preview_1200.webp"}, "outline": "poses/img/v1/w02/outline.webp"}]}'
 }
 preflight() { # prints the exit code; output in $pf/out, step summary in $pf/summary
   local code=0
   PF="$pf" GOOGLE_SERVICES_FILE="${PF_GOOGLE:-$pf/google-services.json}" \
     DISTRIBUTION_CREDENTIALS_FILE="$pf/credentials.json" TESTER_GROUPS="${PF_GROUPS:-team}" \
-    REQUIRED_ASSETS="$pf/assets.txt" DIG_CMD="$work/bin/fake-dig" CURL_CMD="$work/bin/fake-curl" \
+    REQUIRED_ASSETS="$pf/assets.txt" REMOTE_CONFIG_CREDENTIALS_FILE="${PF_RC:-$pf/rc-credentials.json}" DIG_CMD="$work/bin/fake-dig" CURL_CMD="$work/bin/fake-curl" \
     FIREBASE_CMD="$work/bin/fake-firebase" PREFLIGHT_RETRY_SECONDS=0 GITHUB_STEP_SUMMARY="$pf/summary" \
     scripts/preflight.sh > "$pf/out" 2>&1 || code=$?
   cat "$pf/out" >> "$work/preflight-all.out"; cat "$pf/summary" >> "$work/preflight-all.out" 2>/dev/null || true
@@ -1736,7 +1766,7 @@ pf_lines() { grep -c "$1" "$pf/out" || true; }
 
 preflight_reset
 expect "preflight: a ready environment passes" 0 "$(preflight)"
-expect "preflight: a ready environment has one QUA line per check" 4 "$(pf_lines '^QUA: ')"
+expect "preflight: a ready environment has one QUA line per check" 5 "$(pf_lines '^QUA: ')"
 expect "preflight: the domain is read from app/build.gradle.kts" 2 "$(grep -c 'camera-pose.vgl.io.vn' "$pf/dig.args" 2>/dev/null || true)"
 expect "preflight: existing tester groups pass" 0 "$(PF_GROUPS=team,qa preflight)"
 
@@ -1773,8 +1803,102 @@ expect "preflight: a connection that fails once is retried" 0 "$(preflight)"
 preflight_reset; printf '000\n000\n' > "$pf/curl/poses_catalog_v1_catalog-587b5394.json"
 expect "preflight: a connection that keeps failing is refused" 1 "$(preflight)"
 
+# The catalog that staging devices get: poses_catalog_path on Remote Config (plan T-065).
+v3=poses/catalog/v3/catalog-c2e0ce0d.json
+preflight_reset; remote_config "{\"defaultValue\": {\"value\": \"$v3\"}}"; echo 404 > "$pf/curl/poses_catalog_v3_catalog-c2e0ce0d.json"
+expect "preflight: a Remote Config catalog path that answers 404 is refused" 1 "$(preflight)"
+expect "preflight: names the key and the missing catalog" 1 "$(pf_lines "^LỖI: khóa poses_catalog_path trỏ tới $v3, file này trả 404")"
+preflight_reset; remote_config "{\"defaultValue\": {\"value\": \"poses/catalog/v1/catalog-587b5394.json\"}, \"conditionalValues\": {\"beta\": {\"value\": \"$v3\"}}}"
+echo 404 > "$pf/curl/poses_catalog_v3_catalog-c2e0ce0d.json"
+expect "preflight: a conditional value that answers 404 is refused" 1 "$(preflight)"
+expect "preflight: names the conditional catalog" 1 "$(pf_lines "^LỖI: khóa poses_catalog_path trỏ tới $v3")"
+preflight_reset; remote_config "{\"defaultValue\": {\"value\": \"$v3\"}}"; catalog_body > "$pf/body/poses_catalog_v3_catalog-c2e0ce0d.json"
+expect "preflight: a Remote Config catalog that exists passes" 0 "$(preflight)"
+expect "preflight: the catalog of the key is the one downloaded" "1 0" \
+  "$(grep -c "^GET .*/$v3\$" "$pf/curl.urls" || true) $(grep -c '^GET .*/poses/catalog/v1/' "$pf/curl.urls" || true)"
+expect "preflight: the starter files of the catalog are checked, not the others" "1 1 0" \
+  "$(grep -c '^HEAD .*/w01/preview_540.webp$' "$pf/curl.urls") $(grep -c '^HEAD .*/w01/outline.webp$' "$pf/curl.urls") $(grep -c '/w02/' "$pf/curl.urls" || true)"
+preflight_reset; echo 404 > "$pf/curl/poses_img_v1_w01_outline.webp"
+expect "preflight: a missing starter file is refused" 1 "$(preflight)"
+expect "preflight: names the missing starter file" 1 "$(pf_lines '^LỖI: file khởi đầu poses/img/v1/w01/outline.webp của catalog')"
+preflight_reset; catalog_body 2 > "$pf/body/poses_catalog_v1_catalog-587b5394.json"
+expect "preflight: a catalog for a newer app is refused" 1 "$(preflight)"
+expect "preflight: says the catalog needs a newer app" 1 "$(pf_lines '^LỖI: catalog .*cần app mới hơn')"
+preflight_reset; remote_config ""
+expect "preflight: no parameter means the default path" "0 1" "$(preflight) $(grep -c '^GET .*/poses/catalog/v1/catalog-587b5394.json$' "$pf/curl.urls" || true)"
+preflight_reset; remote_config '{"defaultValue": {"useInAppDefault": true}}'; echo 404 > "$pf/curl/poses_catalog_v1_catalog-587b5394.json"
+expect "preflight: useInAppDefault checks the app default path" 1 "$(preflight > /dev/null; pf_lines '^LỖI: khóa poses_catalog_path trỏ tới poses/catalog/v1/catalog-587b5394.json')"
+preflight_reset; remote_config "{\"defaultValue\": {\"value\": \"$v3\"}, \"conditionalValues\": {\"old\": {\"value\": \"poses/catalog/latest.json\"}}}"
+catalog_body > "$pf/body/poses_catalog_v3_catalog-c2e0ce0d.json"
+expect "preflight: an invalid path warns and checks the default, as the app does" "0 1 1" \
+  "$(preflight) $(pf_lines '^CHÚ Ý: khóa poses_catalog_path có giá trị \"poses/catalog/latest.json\" không hợp lệ') $(grep -c '^GET .*/poses/catalog/v1/catalog-587b5394.json$' "$pf/curl.urls" || true)"
+preflight_reset; printf '{"parameterGroups": {"poses": {"parameters": {"poses_catalog_path": {"defaultValue": {"value": "%s"}}}}}}' "$v3" > "$pf/remote-config.json"
+echo 404 > "$pf/curl/poses_catalog_v3_catalog-c2e0ce0d.json"
+expect "preflight: a key inside a parameter group is read" 1 "$(preflight)"
+preflight_reset
+expect "preflight: a missing Remote Config secret is refused" 1 "$(PF_RC="$pf/none.json" preflight)"
+expect "preflight: names the Remote Config secret" 1 "$(pf_lines '^LỖI: thiếu bí mật STAGING_REMOTE_CONFIG_CREDENTIALS')"
+preflight_reset; : > "$pf/rc-empty.json"
+expect "preflight: an empty Remote Config secret file is refused" "1 1" "$(PF_RC="$pf/rc-empty.json" preflight) $(pf_lines '^LỖI: thiếu bí mật STAGING_REMOTE_CONFIG_CREDENTIALS')"
+preflight_reset; echo 1 > "$pf/rc-exit"; echo 'Error: Authorization failed. This account is missing the following required permissions on project x' > "$pf/rc-out"
+expect "preflight: a missing permissions error printed on stdout is refused" "1 1" "$(preflight) $(pf_lines '^LỖI: .*Firebase Remote Config Viewer')"
+preflight_reset; printf '429\n200\n' > "$pf/curl/poses_catalog_v1_catalog-587b5394.json"
+expect "preflight: a 429 that passes on the retry is not a failure" 0 "$(preflight)"
+preflight_reset; remote_config '{"defaultValue": {"value": "poses/catalog/v1/catalog-587b5394.json"}, "conditionalValues": {"a": {"value": "poses/catalog/v1/catalog-587b5394.json"}}}'
+expect "preflight: a path used by two values is checked once" "0 1 1" "$(preflight) $(grep -c '^GET .*/poses/catalog/v1/catalog-587b5394.json$' "$pf/curl.urls" || true) $(pf_lines '^QUA: catalog ')"
+preflight_reset; printf '200\n200\n' > "$pf/curl/poses_catalog_v1_catalog-587b5394.json"; echo '{"poses": ' > "$pf/body/poses_catalog_v1_catalog-587b5394.json"
+expect "preflight: a catalog cut short is downloaded once more" "1 2" "$(preflight) $(grep -c '^GET .*/poses/catalog/v1/catalog-587b5394.json$' "$pf/curl.urls" || true)"
+preflight_reset; echo 1 > "$pf/rc-exit"; echo 'Error: Request had HTTP Error: 403, PERMISSION_DENIED MARKER-SECRET-1' > "$pf/rc-err"
+expect "preflight: a Remote Config account without the role is refused" 1 "$(preflight)"
+expect "preflight: names the missing role" 1 "$(pf_lines '^LỖI: .*Firebase Remote Config Viewer')"
+preflight_reset; echo 1 > "$pf/rc-exit"; echo 'Error: Authorization failed. This account is missing the following required permissions on project proj-test-1: cloudconfig.configs.get' > "$pf/rc-err"
+expect "preflight: the firebase-tools missing permissions text is refused" "1 1" "$(preflight) $(pf_lines '^LỖI: .*Firebase Remote Config Viewer, hoặc API Remote Config chưa bật')"
+preflight_reset; echo 1 > "$pf/rc-exit"; echo 'Error: Server Error. request id 84031, connect ETIMEDOUT' > "$pf/rc-err"
+expect "preflight: a transient error with 403 inside a number only warns" "0 1" "$(preflight) $(pf_lines '^CHÚ Ý: không đọc được khóa poses_catalog_path (lệnh firebase-tools')"
+preflight_reset; echo 1 > "$pf/rc-exit"; echo 'npm ERR! network timeout' > "$pf/rc-err"
+expect "preflight: a Remote Config command that cannot run only warns and checks the default" "0 1 1" \
+  "$(preflight) $(pf_lines '^CHÚ Ý: không đọc được khóa poses_catalog_path (lệnh firebase-tools') $(grep -c '^GET .*/poses/catalog/v1/catalog-587b5394.json$' "$pf/curl.urls" || true)"
+preflight_reset; echo 124 > "$pf/rc-exit"
+expect "preflight: a Remote Config command that hits the time limit only warns" "0 1" "$(preflight) $(pf_lines '^CHÚ Ý: không đọc được khóa poses_catalog_path (lệnh firebase-tools')"
+if command -v timeout > /dev/null; then
+  preflight_reset; echo 5 > "$pf/rc-sleep"
+  expect "preflight: a Remote Config command that hangs is stopped and only warns" "0 1" \
+    "$(PREFLIGHT_REMOTE_CONFIG_TIMEOUT=1 preflight) $(pf_lines '^CHÚ Ý: không đọc được khóa poses_catalog_path (lệnh firebase-tools')"
+fi
+preflight_reset; echo 'not json' > "$pf/remote-config.json"
+expect "preflight: a template that is not JSON only warns" "0 1" "$(preflight) $(pf_lines '^CHÚ Ý: không đọc được khóa poses_catalog_path (lệnh firebase-tools')"
+preflight_reset; echo 'not a catalog' > "$pf/body/poses_catalog_v1_catalog-587b5394.json"
+expect "preflight: a catalog that is not JSON is refused" "1 1" "$(preflight) $(pf_lines '^LỖI: catalog poses/catalog/v1/catalog-587b5394.json (khóa poses_catalog_path) không phải catalog đọc được')"
+preflight_reset; remote_config "{\"defaultValue\": {\"value\": \"poses/catalog/v1/catalog-587b5394.json\"}, \"conditionalValues\": {\"half\": {\"rolloutValue\": {\"rolloutId\": \"r1\", \"value\": \"$v3\"}}}}"
+echo 404 > "$pf/curl/poses_catalog_v3_catalog-c2e0ce0d.json"
+expect "preflight: a rollout value that answers 404 is refused" "1 1" "$(preflight) $(pf_lines "^LỖI: khóa poses_catalog_path trỏ tới $v3")"
+preflight_reset; remote_config '{"defaultValue": {"value": "poses/catalog/v1/catalog-587b5394.json"}, "conditionalValues": {"p": {"personalizationValue": {"personalizationId": "x"}}}}'
+expect "preflight: a personalization value warns" "0 1" "$(preflight) $(pf_lines '^CHÚ Ý: .*cá nhân hóa')"
+preflight_reset; remote_config '{"conditionalValues": {"p": {"personalizationValue": {"personalizationId": "x"}}}, "defaultValue": {"personalizationValue": {}}}'
+expect "preflight: no readable value checks the default path" "0 1" "$(preflight) $(grep -c '^GET .*/poses/catalog/v1/catalog-587b5394.json$' "$pf/curl.urls" || true)"
+preflight_reset; echo 404 > "$pf/curl/poses_img_v1_w01_outline.webp"
+expect "preflight: no QUA line for a catalog with a missing starter file" 0 "$(preflight > /dev/null; pf_lines '^QUA: catalog ')"
+preflight_reset; printf '{"schema": 1, "minAppSchema": 1, "poses": ["broken", {"id": "w01", "starter": true, "preview": {"small": "poses/img/v1/w01/preview_540.webp"}, "outline": "poses/img/v1/w01/outline.webp"}]}' > "$pf/body/poses_catalog_v1_catalog-587b5394.json"
+expect "preflight: a pose that is not an object does not hide the starter files" "0 1" "$(preflight) $(pf_lines '^QUA: catalog .* và 2 file khởi đầu')"
+preflight_reset; printf '503\n200\n' > "$pf/curl/poses_catalog_v1_catalog-587b5394.json"
+expect "preflight: a 503 that passes on the retry is not a failure" 0 "$(preflight)"
+preflight_reset; google_services proj-test-1 "$staging_client" | jq 'del(.project_info.project_id)' > "$pf/google-services.json"
+expect "preflight: a Firebase secret without project id only warns for Remote Config" "0 1" "$(preflight) $(pf_lines '^CHÚ Ý: không đọc được khóa poses_catalog_path: bí mật Firebase không có mã project')"
+preflight_reset; echo 404 > "$pf/curl/poses_catalog_v9_catalog-00000000.json"
+expect "preflight: the extra catalog path of a manual run is checked" "1 1" \
+  "$(EXTRA_CATALOG_PATH=poses/catalog/v9/catalog-00000000.json preflight) $(pf_lines '^LỖI: khóa poses_catalog_path trỏ tới poses/catalog/v9/catalog-00000000.json')"
+preflight_reset
+expect "preflight: the staging project is passed to remoteconfig:get" 1 "$(preflight > /dev/null; grep -c -- '--project proj-test-1' "$pf/rc.args" || true)"
+expect "preflight: remoteconfig:get uses the Remote Config credentials" 1 "$(grep -c "^GAC=$pf/rc-credentials.json " "$pf/rc.args" || true)"
+for p in poses/catalog/v1/catalog-587b5394.json images/v2/hero.webp levels/v1.3/level-01.json images/hero.v2.webp images/hero-v12.png images/hero.3f2a9c1e.webp sounds/click_0123456789abcdef.ogg; do
+  expect "preflight: $p is a valid asset path" 0 "$(bash -c 'eval "$(sed -n "/^valid_asset_path()/,/^}/p" scripts/preflight.sh)"; valid_asset_path "$1"; echo $?' _ "$p")"
+done
+for p in images/hero.webp hero.webp images/v2 images/hero.abc.webp images/latest/hero.webp "" /images/v2/hero.webp images//v2/hero.webp images/v2/../hero.webp ../v2/hero.webp .download/v2/hero.webp https://example.com/v2/hero.webp "images/v2/hero.webp?x=1" "images/v2/hero image.webp"; do
+  expect "preflight: '$p' is not a valid asset path" 1 "$(bash -c 'eval "$(sed -n "/^valid_asset_path()/,/^}/p" scripts/preflight.sh)"; valid_asset_path "$1"; echo $?' _ "$p")"
+done
+
 preflight_reset; google_services fake-staging-project "$staging_client" > "$pf/google-services.json"; echo 404 > "$pf/curl/poses_catalog_v1_catalog-587b5394.json"
-expect "preflight: reports every failing check" 2 "$(preflight > /dev/null; pf_lines '^LỖI: ')"
+expect "preflight: reports every failing check" 3 "$(preflight > /dev/null; pf_lines '^LỖI: ')"
 expect "preflight: never prints secret values" 0 "$(grep -c 'MARKER-SECRET-1\|proj-test-1\|987654321' "$work/preflight-all.out" || true)"
 
 # java-check.sh stops check.sh before Gradle when Java cannot run the project's Gradle (L-022).
```

**Ngoại lệ viết test trước:** hai workflow và `docs/ops/staging-and-runs.md` (không có test; kiểm bằng lần chạy thủ công ở bảng trên).

### 4.1. Chạy thử

- **Đã chạy thử:** 2026-10-26, worktree `wt-t065` từ nhánh chính `f464d7f`, bản cài thử của `scripts/preflight.sh` và test ở mục 3.1, mục 4 (không commit). Chạy riêng khối preflight của `scripts/tests/run.sh` (dòng 1 đến 26 và khối từ "# preflight.sh checks the staging" tới trước "# java-check.sh"), vì máy viết plan không có JDK nên `scripts/check.sh` và toàn bộ `run.sh` (có test cần Java) không chạy được; phiên chạy `scripts/check.sh` như mọi task.
- **Lệnh:** `bash scripts/tests/pf-block.sh` (tệp tạm, đã xóa).
- **Đã chạy lại sau hai vòng rà soát độc lập** (2026-10-26, nhánh chính `191ebd7`): hai lần, không và có lệnh `timeout` (máy viết plan là macOS, không có `timeout`; lần thứ hai dùng một lệnh `timeout` tạm bằng `perl alarm`, nhận cả `-k`, trong `PATH`, chỉ để chạy test treo).
- **Kết quả trên code hiện tại** (test mới, `preflight.sh` cũ): có `timeout`: 25 qua, 62 thất bại; không `timeout`: 25 qua, 61 thất bại (test treo bỏ qua). Thất bại đúng lý do: 2 kỳ vọng cũ đổi (`expected '5', got '4'`, `expected '3', got '2'`); 39 test mới của điều kiểm 5 (`expected '1', got '0'`, `expected '0 1', got '0 0'`; `a 503`, `a 429 that passes on the retry`: điều kiểm 4 cũ không thử lại; `the staging project is passed`, `uses the Remote Config credentials`: không có `rc.args`); 21 test quy tắc đường dẫn (`valid_asset_path: command not found`, mã 127). Đối chứng qua: `a Remote Config catalog that exists passes`, `no QUA line for a catalog with a missing starter file`; 23 test cũ không đổi qua.
- **Kết quả trên bản cài thử:** có `timeout` 87 qua, không `timeout` 86 qua, `failures=0` (23 test cũ không đổi, 2 kỳ vọng sửa, 62 hay 61 test mới).
- **Đột biến** (mỗi lần sửa một chỗ của bản cài thử, chạy lại khối có `timeout`):

| # | Đột biến | Thất bại |
| --- | --- | --- |
| 1 | Bỏ `conditionalValues` | 5 |
| 2 | Bỏ `parameterGroups` | 1 |
| 3 | Giá trị sai dạng không quay về đường dẫn mặc định | 1 (test giá trị sai dạng nay có giá trị mặc định hợp lệ cạnh giá trị sai; trước đó 0) |
| 4 | Lỗi thiếu quyền chỉ cảnh báo | 3 |
| 5 | Bỏ kiểm `minAppSchema` | 2 |
| 6 | Kiểm mọi tư thế, không chỉ `starter` | 1 |
| 7 | Mẫu khớp `403` trần | 1 |
| 8 | Không thử lại `5xx`, `429` | 1 |
| 9 | Bỏ kiểm template là object JSON | 1 |
| 10 | In `QUA` khi có file khởi đầu hỏng | 1 |
| 11 | Bỏ `objects` khi đọc `poses` | 1 |
| 12 | Không quay về mặc định khi không đọc được giá trị nào | 1 |
| 13 | Thiếu mã project là `LỖI` thay vì `CHÚ Ý` | 1 |
| 14 | Bỏ `rolloutValue` | 1 |
| 15 | Bỏ giới hạn thời gian | 1 |
| 16 | Lỗi của công cụ chỉ đọc từ stderr | 1 |
| 17 | Tệp bí mật rỗng coi là có (`-s` thành `-f`) | 1 |
| 18 | `remoteconfig:get` dùng nhầm bí mật App Distribution | 1 |
| 19 | Không thử lại `429` | 1 |
| 20 | Không gộp đường dẫn trùng (`sort -u`) | 1 |
| 21 | Không tải lại catalog bị cắt | 1 |

- **Workflow, chạy thử thật** (2026-10-27, sau khi Tuấn tạo bí mật `STAGING_REMOTE_CONFIG_CREDENTIALS` lúc 01:19:45Z; lúc đó không có issue `setup-broken` nào mở và không có phiên nào chạy hay sắp bắt đầu). Nhánh thử `trial/T-065` từ nhánh chính `3d751fe` với code của mục 3.1 (`preflight.sh`, `run.sh`, hai workflow), chạy `gh workflow run preflight.yml --ref trial/T-065`; nhánh thử đã xóa sau đó.
  - **Đường xanh**, lần chạy 40432255124: xanh. `QUA: catalog poses/catalog/v3/catalog-512aa695.json (khóa poses_catalog_path) và 100 file khởi đầu đều trả 200.`; `Tổng: 5 điều qua, 0 điều lỗi.` Giá trị khóa đọc từ Remote Config thật (Tuấn đã đổi sang catalog v3 mới, có trên R2). Điều kiểm 5 mất khoảng 43 giây (tải `firebase-tools` đã có từ điều kiểm 2, 100 lệnh `HEAD`), cả job dưới 2 phút, trong giới hạn 10 phút.
  - **`remoteconfig:get --project <id> -o <tệp>`:** chạy đúng như plan: mã 0, tệp `-o` là template JSON đọc được bằng `catalog_values`. Không phải dùng phương án dự phòng của mục 3.4.
  - **Luồng lỗi** (bước tạm của nhánh thử, cùng bí mật, project không tồn tại; chỉ in số dòng, mã thoát và dòng đầu đã che): mã 1, stdout 2 dòng, stderr 0 dòng, không có tệp `-o`, không có `firebase-debug.log`. Dòng lỗi ở stdout: `Error: Failed to get Firebase Remote Config template for project <probe>.` Vậy `firebase-tools` in lỗi ra stdout, đúng lý do của `> <tệp lỗi> 2>&1` (rà soát vòng 2, điểm 1). Thông báo này không khớp mẫu `HTTP Error: 403|PERMISSION_DENIED|missing the following required permissions`, nên lỗi đó là `CHÚ Ý`.
  - **Chưa kiểm được:** chữ thật của lỗi thiếu vai trò trên project staging. Cách thử (gọi `remoteconfig:get` bằng tài khoản App Distribution, không có vai trò Remote Config) bị môi trường chặn vì dùng một bí mật khác để dò, nên không chạy. Nếu `firebase-tools` cũng gói lỗi `403` thành `Failed to get Firebase Remote Config template ...` thì tài khoản mất vai trò sau này chỉ ra `CHÚ Ý` (điều kiểm 5 thực ra chỉ kiểm đường dẫn mặc định), không mở `setup-broken`. Câu hỏi 6.
  - **Đường đỏ**, lần chạy 40433373539 với `extra_catalog=poses/catalog/v9/catalog-00000000.json`: đỏ. `LỖI: khóa poses_catalog_path trỏ tới poses/catalog/v9/catalog-00000000.json, file này trả 404 (cần 200): app dừng ở màn tải. Sửa khóa trên Remote Config staging hoặc tải file lên R2.`; `Tổng: 5 điều qua, 1 điều lỗi.` Bước "Ghi kết quả" mở issue #182 `setup-broken`.
  - **Xanh lại**, lần chạy 40433553124: xanh, issue #182 tự đóng. Sau đó không còn issue `setup-broken` nào mở.

## 5. Chấm rủi ro

**Điểm chặn bị chạm:** 7 (CI: `.github/workflows/preflight.yml`, `start-run.yml`; quy trình phiên tự chạy: điều kiện bắt đầu phiên).

| Tiêu chí | Điểm (0 đến 3) | Lý do |
| --- | --- | --- |
| A. Phạm vi thay đổi | 2 | Một script, hai workflow, test, một tài liệu |
| B. Độ rõ của yêu cầu | 1 | Sự cố rõ; còn câu hỏi 2, 3 không đổi cách làm chính |
| C. Khả năng kiểm chứng tự động | 2 | Script có test; workflow và lệnh `remoteconfig:get` thật chỉ kiểm bằng lần chạy tay |
| D. Ảnh hưởng chéo | 2 | `preflight` quyết định phiên có bắt đầu không |
| E. Độ mới của kỹ thuật | 2 | `remoteconfig:get` lần đầu dùng trong dự án |
| **Tổng** | 9 | **Mức: Cao** (điểm chặn 7) |

## 6. Ảnh hưởng chéo

- File hoặc thành phần dùng chung bị chạm: `scripts/preflight.sh`, `preflight.yml`, `start-run.yml`; T-063 sửa `scripts/tests/run.sh` ở khối token-usage, khác khối.
- Task khác trong sprint có thể xung đột: T-063 (`scripts/tests/run.sh`, khối khác), T-062 (`device-session.yml`, khác file).
- Đề xuất thứ tự chạy: sau T-062, T-063; chỉ khi bí mật đã có và lần chạy thử xanh.

## 7. Điều kiện dừng riêng

- Ngay trước task, lần chạy gần nhất của `preflight.yml` trên nhánh thử với thay đổi này chưa xanh, hay bí mật `STAGING_REMOTE_CONFIG_CREDENTIALS` chưa có (bước "Kiểm tra môi trường trước phiên" báo `thiếu bí mật`): bỏ qua task, ghi lý do. Merge khi chưa có bí mật làm `preflight` đỏ và chặn mọi phiên sau.

## 8. Câu hỏi cho người duyệt

1. Duyệt sau khi Tuấn tạo bí mật và lần chạy thử workflow (mục 4.1) xanh, như T-062? **AI đề xuất: có.** Tạo bí mật trước cũng không ảnh hưởng gì khi workflow chưa đọc nó.
2. Tài khoản dịch vụ riêng `remote-config-reader` (chỉ vai trò Firebase Remote Config Viewer) hay gán thêm vai trò đó cho tài khoản App Distribution có sẵn (không thêm bí mật)? **AI đề xuất: tài khoản riêng**, mỗi bí mật một quyền nhỏ nhất (ADR-0009); thêm một bí mật là việc một lần.
3. Phiên mở bằng `claude --cloud` chỉ đọc kết quả `preflight.yml` trong 26 giờ (`docs/auto-run.md`, mục 2), nên thay đổi Remote Config sau lần chạy buổi sáng vẫn lọt. Có cho phiên tự chạy lại `preflight.yml` lúc bắt đầu không? **AI đề xuất: chưa**, chỉ thêm dòng "đổi khóa thì chạy lại preflight" ở `docs/ops/staging-and-runs.md`: catalog hỏng không làm task hỏng (CI dùng file Firebase giả, đường dẫn mặc định), và một `preflight` đỏ lúc bắt đầu sẽ dừng cả phiên vì một việc chỉ ảnh hưởng `device-session` và bản staging. Nếu muốn, làm ticket riêng.
4. Catalog hỏng (điều kiểm 5) là `LỖI` (mở `setup-broken`, chặn phiên) như file R2 bắt buộc, hay chỉ `CHÚ Ý`? **AI đề xuất: `LỖI`**: người sửa trong vài phút, còn phiên chạy với catalog hỏng thì `device-session` hỏng mọi kịch bản và bản staging không dùng được.
5. Ghi chú: `CHÚ Ý` của điều kiểm 5 lặp lại nhiều ngày (công cụ hỏng lâu, điều kiểm thực ra không chạy) không mở issue; nếu thấy cần, một ticket sau có thể biến `CHÚ Ý` lặp lại quá 3 ngày thành issue riêng.
6. Lỗi thiếu vai trò có thể chỉ ra `CHÚ Ý` (mục 4.1, Chưa kiểm được): giữ như plan, hay coi mọi lỗi `Failed to get Firebase Remote Config template` khi bí mật có mặt là `LỖI`? **AI đề xuất: giữ như plan** cho task này: coi là `LỖI` thì cả lỗi mạng tạm của công cụ cũng chặn phiên cả ngày. Muốn chắc thì Tuấn thử một lần bằng tay (gỡ vai trò của `remote-config-reader`, chạy **preflight** trên nhánh thử, gán lại), hay để ticket "`CHÚ Ý` lặp lại thành issue" ở câu 5 phủ trường hợp này.

## 9. Quyết định duyệt

<!-- Người duyệt điền. AI không điền mục này. -->

- **Quyết định:** Duyệt / Dừng / Duyệt có điều kiện
- **Người duyệt:** <tên>, **duyệt chéo:** <tên, nếu rủi ro cao>
- **Ghi chú:** <điều kiện, yêu cầu sửa, trả lời câu hỏi ở mục 8>

## 10. Nhật ký thực hiện

<!-- AI ghi sau khi chạy task. -->
