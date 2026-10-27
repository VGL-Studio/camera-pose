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
