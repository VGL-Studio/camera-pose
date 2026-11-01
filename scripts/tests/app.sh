#!/usr/bin/env bash
# camera-pose's own script tests. scripts/tests/run.sh sources this file before its summary line, in the same shell:
# it uses expect, $work, the fakes and helpers of run.sh, and its failures count in the same total
# (docs/ops/to-app.md of the template, section 6). Tests of template files stay in run.sh.

# plan-check.py --touches on the app's real plans (ADR-1005, T-067): a plan naming workflows only in prose does not
# touch them; T-062 changes a workflow.
expect "plan-check --touches: the real T-067 plan (workflows only in prose) -> 1" 1 \
  "$(touches .github/workflows/ docs/features/quality-gate/plans/T-067-tu-quyet-roi-bao.md)"
expect "plan-check --touches: a real plan that changes a workflow (T-062) -> 0" 0 \
  "$(touches .github/workflows/ docs/features/quality-gate/plans/T-062-camera-gia-tren-emulator-ci.md)"

# camera-pose only (plan T-065): scripts/setup-check-app.sh, sourced at the end of setup-check.sh. The pose catalog
# that devices download (the default one, and every value of poses_catalog_path on Remote Config staging) and its
# starter files. curl, dig, gcloud and firebase-tools are fakes driven by files in $sca.
sca="$work/sca"
mkdir -p "$work/sca-bin"
cat > "$work/sca-bin/curl" << 'SH'
#!/usr/bin/env bash
# Fake curl. The URL is the last argument; its key is the path with / as _. The code is the first line of
# $SCA/codes/<key>, consumed one per call (the last line stays); default 200. A cache-busting query
# (?setup-check-nocache=) reads $SCA/nocache/<key> instead, default 404. With -o <file> and a 200 the body is
# $SCA/body/<key> when it exists. Every call is logged as "<GET|HEAD> <URL without query>" in $SCA/calls.
url="${*: -1}"; plain="${url%%\?*}"; key="$(sed -E 's#^https?://[^/]+/##; s#/#_#g' <<< "$plain")"
out=""; prev=""; method=GET
for a in "$@"; do [[ "$prev" == "-o" ]] && out="$a"; [[ "$a" == -I ]] && method=HEAD; prev="$a"; done
if [[ "$url" == *"?setup-check-nocache="* ]]; then
  code="$(cat "$SCA/nocache/$key" 2> /dev/null || echo 404)"
else
  f="$SCA/codes/$key"
  if [[ -s "$f" ]]; then code="$(head -n 1 "$f")"; [[ "$(wc -l < "$f")" -gt 1 ]] && sed -i.bak 1d "$f" && rm -f "$f.bak"; else code=200; fi
  echo "$method $plain" >> "$SCA/calls"
fi
if [[ "$code" == 200 && -n "$out" && "$out" != /dev/null && -f "$SCA/body/$key" ]]; then cp "$SCA/body/$key" "$out"; fi
printf '%s' "$code"
SH
cat > "$work/sca-bin/dig" << 'SH'
#!/usr/bin/env bash
echo 104.21.40.144
SH
cat > "$work/sca-bin/gcloud" << 'SH'
#!/usr/bin/env bash
[[ "$*" == *print-access-token* ]] && echo fake-token
exit 0
SH
cat > "$work/sca-bin/firebase" << 'SH'
#!/usr/bin/env bash
# Fake firebase-tools remoteconfig:get: logs its arguments and whether the credentials file is the Remote Config
# secret to $SCA/rc.args, prints $SCA/rc-out on stdout and $SCA/rc-err on stderr, sleeps $SCA/rc-sleep seconds,
# exits with $SCA/rc-exit (default 0) and on 0 copies $SCA/remote-config.json to the -o file.
gac=no; grep -q RC-MARKER-1 "${GOOGLE_APPLICATION_CREDENTIALS:-/dev/null}" 2> /dev/null && gac=yes
echo "GAC=$gac $*" >> "$SCA/rc.args"
out=""; prev=""; for a in "$@"; do [[ "$prev" == "-o" ]] && out="$a"; prev="$a"; done
cat "$SCA/rc-out" 2> /dev/null || true
cat "$SCA/rc-err" >&2 2> /dev/null || true
[[ -f "$SCA/rc-sleep" ]] && sleep "$(cat "$SCA/rc-sleep")"
code="$(cat "$SCA/rc-exit" 2> /dev/null || echo 0)"
[[ "$code" == 0 && -f "$SCA/remote-config.json" ]] && cp "$SCA/remote-config.json" "$out"
exit "$code"
SH
chmod +x "$work/sca-bin/"*
# The default path and the schema come from the app code with the same sed as the script, so a later catalog task
# that changes them does not turn these tests red.
poses_kt=core/poses/src/main/kotlin/com/vglstudio/camera/pose/line/guide/core/poses/internal/PosesSettings.kt
parser_kt=core/poses/src/main/kotlin/com/vglstudio/camera/pose/line/guide/core/poses/internal/CatalogParser.kt
v1="$(sed -nE 's/.*DEFAULT_CATALOG_PATH = "([^"]+)".*/\1/p' "$poses_kt" | head -n 1)"
v1key="${v1//\//_}"
newer_schema=$(( $(sed -nE 's/.*APP_SCHEMA = ([0-9]+).*/\1/p' "$parser_kt" | head -n 1) + 1 ))
# Stand-in for a catalog that only Remote Config points to. It sits in a folder the default catalog does not use
# (the default moved to v3 in T-071), so a download of it is told apart from the default by folder.
v3=poses/catalog/v9/catalog-c2e0ce0d.json
v3key=poses_catalog_v9_catalog-c2e0ce0d.json
sca_catalog() { # [minAppSchema]: one starter pose and one other pose
  printf '{"schema": 1, "minAppSchema": %s, "poses": [' "${1:-1}"
  printf '{"id": "w01", "starter": true, "preview": {"small": "poses/img/v1/w01/preview_540.webp", "large": "poses/img/v1/w01/preview_1200.webp"}, "outline": "poses/img/v1/w01/outline.webp"},'
  printf '{"id": "w02", "starter": false, "preview": {"small": "poses/img/v1/w02/preview_540.webp"}, "outline": "poses/img/v1/w02/outline.webp"}]}'
}
sca_rc() { # <parameter JSON of poses_catalog_path, or "" for no parameter>
  if [[ -n "$1" ]]; then printf '{"parameters": {"poses_catalog_path": %s}, "conditions": []}' "$1" > "$sca/remote-config.json"
  else echo '{"parameters": {"other_key": {"defaultValue": {"value": "x"}}}}' > "$sca/remote-config.json"; fi
}
sca_reset() { # a ready setup; a test then breaks one thing
  rm -rf "$sca"; mkdir -p "$sca/codes" "$sca/nocache" "$sca/body"
  printf '# Required files\n%s\n' "$v1" > "$sca/assets.txt"
  sca_catalog > "$sca/body/$v1key"
  sca_catalog > "$sca/body/$v3key"
  sca_rc "{\"defaultValue\": {\"value\": \"$v1\"}}"
}
sca_rc_secret='{"type": "service_account", "private_key": "RC-MARKER-1"}'
sca_json='{"project_info": {"project_id": "demo-staging", "project_number": "123"}, "client": [{"client_info": {"android_client_info": {"package_name": "com.demo.staging"}}}]}'
sca_check() { # [args...]; prints the exit code, keeps the output in $work/sca.out
  local code=0
  PATH="$work/sca-bin:$PATH" SCA="$sca" SETUP_CHECK_ASSETS_LIST="$sca/assets.txt" \
    SETUP_CHECK_ASSETS_BASE_URL="https://cdn.example.com/" SETUP_CHECK_PACKAGE=com.demo \
    SETUP_CHECK_FIREBASE_CMD="$work/sca-bin/firebase" SETUP_CHECK_RETRY_SECONDS=0 GITHUB_ACTIONS='' \
    GOOGLE_SERVICES="${SCA_GOOGLE-$sca_json}" DISTRIBUTION_CREDENTIALS='{"type": "service_account"}' \
    REMOTE_CONFIG_CREDENTIALS="${SCA_RC-$sca_rc_secret}" \
    bash -c '[ -z "${SCA_RC_UNSET:-}" ] || unset REMOTE_CONFIG_CREDENTIALS; exec scripts/setup-check.sh "$@"' _ "$@" \
    > "$work/sca.out" 2>&1 || code=$?
  cat "$work/sca.out" >> "$work/sca-all.out"
  echo "$code"
}
sca_lines() { grep -c -- "$1" "$work/sca.out" || true; }
sca_calls() { grep -c -- "$1" "$sca/calls" 2> /dev/null || true; }

# The default catalog, also without --staging (a developer machine, setup-new-app.md).
sca_reset
expect "setup-check app: the default catalog and its starter files pass" "0 1" \
  "$(sca_check) $(sca_lines "^ok   - catalog mặc định của app $v1 và 2 file khởi đầu trả 200")"
expect "setup-check app: only the starter files are checked" "1 1 0" \
  "$(sca_calls '^HEAD .*/w01/preview_540.webp$') $(sca_calls '^HEAD .*/w01/outline.webp$') $(sca_calls '/w02/')"
expect "setup-check app: without --staging Remote Config is not read" 0 "$(cat "$sca/rc.args" 2> /dev/null | grep -c . || true)"
sca_reset; echo 404 > "$sca/codes/poses_img_v1_w01_outline.webp"
expect "setup-check app: a missing starter file of the default catalog stops" 1 "$(sca_check)"
expect "setup-check app: names the missing starter file" 1 \
  "$(sca_lines "^LỖI - file khởi đầu poses/img/v1/w01/outline.webp của catalog $v1 trả '404'")"
expect "setup-check app: no ok line for a catalog with a missing starter file" 0 "$(sca_lines '^ok   - catalog ')"
expect "setup-check app: a file really missing gets no CDN hint" 0 "$(sca_lines 'CDN')"
sca_reset; echo 404 > "$sca/codes/poses_img_v1_w01_outline.webp"; echo 200 > "$sca/nocache/poses_img_v1_w01_outline.webp"
expect "setup-check app: a starter file the CDN still answers 404 for stops" 1 "$(sca_check)"
expect "setup-check app: says the CDN keeps an old answer" 1 "$(sca_lines '^LỖI - file khởi đầu .*CDN còn giữ câu trả lời cũ')"
sca_reset; printf '404\n' > "$sca/codes/$v1key"
expect "setup-check app: a default catalog that answers 404 stops" "1 1" \
  "$(sca_check) $(sca_lines "^LỖI - catalog mặc định của app $v1 trả '404'")"
sca_reset; echo 'not a catalog' > "$sca/body/$v1key"
expect "setup-check app: a catalog that is not JSON stops" "1 1" \
  "$(sca_check) $(sca_lines "^LỖI - catalog mặc định của app $v1 không phải catalog đọc được")"
sca_reset; echo '{"poses": ' > "$sca/body/$v1key"
expect "setup-check app: a catalog cut short is downloaded once more" "1 2" "$(sca_check) $(sca_calls "^GET .*/$v1\$")"
sca_reset; sca_catalog "$newer_schema" > "$sca/body/$v1key"
expect "setup-check app: a catalog for a newer app stops" "1 1" "$(sca_check) $(sca_lines '^LỖI - catalog .*cần app mới hơn')"
sca_reset; printf '{"schema": 1, "minAppSchema": 1, "poses": ["broken", {"id": "w01", "starter": true, "preview": {"small": "poses/img/v1/w01/preview_540.webp"}, "outline": "poses/img/v1/w01/outline.webp"}]}' > "$sca/body/$v1key"
expect "setup-check app: a pose that is not an object does not hide the starter files" "0 1" \
  "$(sca_check) $(sca_lines '^ok   - catalog .* và 2 file khởi đầu')"
sca_reset; printf '503\n200\n' > "$sca/codes/poses_img_v1_w01_outline.webp"
expect "setup-check app: a 503 that passes on the retry is not a failure" 0 "$(sca_check)"
sca_reset; printf '429\n200\n' > "$sca/codes/$v1key"
expect "setup-check: a 429 that passes on the retry is not a failure" 0 "$(sca_check)"
sca_reset; printf '429\n429\n200\n' > "$sca/codes/$v1key"
expect "setup-check app: a 429 seen twice counts" 1 "$(sca_check)"
sca_reset; printf '000\n200\n' > "$sca/codes/$v1key"
expect "setup-check: a required file that fails to connect once is asked again" "0 2" \
  "$(sca_check) $(sca_calls "^HEAD .*/$v1\$")"
sca_reset; echo 404 > "$sca/codes/$v1key"; echo 200 > "$sca/nocache/$v1key"
expect "setup-check: a required file the CDN still answers 404 for names the cache" 1 \
  "$(sca_check > /dev/null; sca_lines "^LỖI - https://cdn.example.com/$v1 trả '404', cần 200.*CDN còn giữ")"

# With --staging: every value of poses_catalog_path on Remote Config staging.
sca_reset; sca_rc "{\"defaultValue\": {\"value\": \"$v3\"}}"; echo 404 > "$sca/codes/$v3key"
expect "setup-check app: a Remote Config catalog that answers 404 stops" 1 "$(sca_check --staging)"
expect "setup-check app: names the key and the missing catalog" 1 \
  "$(sca_lines "^LỖI - khóa poses_catalog_path trỏ tới catalog $v3 trả '404'")"
sca_reset; sca_rc "{\"defaultValue\": {\"value\": \"$v3\"}}"
expect "setup-check app: a Remote Config catalog that exists passes" 0 "$(sca_check --staging)"
expect "setup-check app: the key's catalog and the default catalog are both downloaded once" "1 1" \
  "$(sca_calls "^GET .*/$v3\$") $(sca_calls "^GET .*/$v1\$")"
expect "setup-check app: the staging project is passed to remoteconfig:get" 1 "$(grep -c -- '--project demo-staging' "$sca/rc.args")"
expect "setup-check app: remoteconfig:get uses the Remote Config secret" 1 "$(grep -c '^GAC=yes ' "$sca/rc.args")"
sca_reset; sca_rc "{\"defaultValue\": {\"value\": \"$v1\"}, \"conditionalValues\": {\"beta\": {\"value\": \"$v3\"}}}"; echo 404 > "$sca/codes/$v3key"
expect "setup-check app: a conditional value that answers 404 stops" "1 1" \
  "$(sca_check --staging) $(sca_lines "^LỖI - khóa poses_catalog_path trỏ tới catalog $v3")"
sca_reset; sca_rc "{\"defaultValue\": {\"value\": \"$v1\"}, \"conditionalValues\": {\"half\": {\"rolloutValue\": {\"rolloutId\": \"r1\", \"value\": \"$v3\"}}}}"; echo 404 > "$sca/codes/$v3key"
expect "setup-check app: a rollout value that answers 404 stops" "1 1" \
  "$(sca_check --staging) $(sca_lines "^LỖI - khóa poses_catalog_path trỏ tới catalog $v3")"
sca_reset; printf '{"parameterGroups": {"poses": {"parameters": {"poses_catalog_path": {"defaultValue": {"value": "%s"}}}}}}' "$v3" > "$sca/remote-config.json"; echo 404 > "$sca/codes/$v3key"
expect "setup-check app: a key inside a parameter group is read" 1 "$(sca_check --staging)"
sca_reset; sca_rc ""
expect "setup-check app: no parameter means the default catalog only, without a note" "0 1 0 0" \
  "$(sca_check --staging) $(sca_calls "^GET .*/$v1\$") $(sca_calls "^GET .*/${v3%/*}/") $(sca_lines '^CHÚ Ý - ')"
sca_reset; sca_rc '{"defaultValue": {"useInAppDefault": true}}'; echo 404 > "$sca/codes/$v1key"
expect "setup-check app: useInAppDefault points the key to the default catalog, without a note" "1 0" \
  "$(sca_check --staging > /dev/null; sca_lines "^LỖI - khóa poses_catalog_path trỏ tới catalog $v1") $(sca_lines '^CHÚ Ý - ')"
sca_reset; sca_rc "{\"defaultValue\": {\"value\": \"$v3\"}, \"conditionalValues\": {\"old\": {\"value\": \"poses/catalog/latest.json\"}}}"
expect "setup-check app: an invalid value warns and the key falls back to the default catalog, as the app does" "0 1 1 1" \
  "$(sca_check --staging) $(sca_lines '^CHÚ Ý - khóa poses_catalog_path có giá trị "poses/catalog/latest.json" không hợp lệ') $(sca_calls "^GET .*/$v1\$") $(sca_lines "^ok   - khóa poses_catalog_path trỏ tới catalog $v1 ")"
sca_reset; sca_rc "{\"defaultValue\": {\"value\": \"$v1\"}, \"conditionalValues\": {\"p\": {\"personalizationValue\": {\"personalizationId\": \"x\"}}}}"
expect "setup-check app: a personalization value warns" "0 1" "$(sca_check --staging) $(sca_lines '^CHÚ Ý - .*cá nhân hóa')"
sca_reset; sca_rc '{"defaultValue": {"personalizationValue": {}}}'
expect "setup-check app: no readable value warns and checks the default catalog" "0 1 1" \
  "$(sca_check --staging) $(sca_lines '^CHÚ Ý - không đọc được giá trị nào') $(sca_calls "^GET .*/$v1\$")"
sca_reset; sca_rc "{\"defaultValue\": {\"value\": \"$v1\"}, \"conditionalValues\": {\"a\": {\"value\": \"$v1\"}}}"
expect "setup-check app: a path used by two values is checked once" "0 1 1" \
  "$(sca_check --staging) $(sca_calls "^GET .*/$v1\$") $(sca_lines '^ok   - khóa poses_catalog_path trỏ tới catalog ')"
sca_reset
expect "setup-check app: a missing Remote Config secret stops" "1 1" \
  "$(SCA_RC='' sca_check --staging) $(sca_lines '^LỖI - thiếu bí mật STAGING_REMOTE_CONFIG_CREDENTIALS')"
expect "setup-check app: without the secret the default catalog is still checked" 1 "$(sca_calls "^GET .*/$v1\$")"
sca_reset; echo 1 > "$sca/rc-exit"; echo 'Error: Request had HTTP Error: 403, PERMISSION_DENIED RC-MARKER-1' > "$sca/rc-err"
expect "setup-check app: a Remote Config account without the role stops" "1 1" \
  "$(sca_check --staging) $(sca_lines '^LỖI - .*Firebase Remote Config Viewer, hoặc API Remote Config chưa bật')"
sca_reset; echo 1 > "$sca/rc-exit"; echo 'Error: Authorization failed. This account is missing the following required permissions on project x' > "$sca/rc-out"
expect "setup-check app: a missing permissions error printed on stdout stops" "1 1" \
  "$(sca_check --staging) $(sca_lines '^LỖI - .*Firebase Remote Config Viewer')"
sca_reset; echo 1 > "$sca/rc-exit"; echo 'Error: Server Error. request id 84031, connect ETIMEDOUT' > "$sca/rc-out"
expect "setup-check app: a transient error with 403 inside a number only warns" "0 1" \
  "$(sca_check --staging) $(sca_lines '^CHÚ Ý - không đọc được khóa poses_catalog_path (lệnh firebase-tools')"
sca_reset; echo 1 > "$sca/rc-exit"; echo 'Error: Failed to get Firebase Remote Config template for project x.' > "$sca/rc-out"
expect "setup-check app: a Remote Config command that cannot run only warns and checks the default catalog" "0 1 1" \
  "$(sca_check --staging) $(sca_lines '^CHÚ Ý - không đọc được khóa poses_catalog_path (lệnh firebase-tools') $(sca_calls "^GET .*/$v1\$")"
sca_reset; echo 124 > "$sca/rc-exit"
expect "setup-check app: a Remote Config command that hits the time limit only warns" "0 1" \
  "$(sca_check --staging) $(sca_lines '^CHÚ Ý - không đọc được khóa poses_catalog_path (lệnh firebase-tools')"
if command -v timeout > /dev/null; then
  sca_reset; echo 5 > "$sca/rc-sleep"
  expect "setup-check app: a Remote Config command that hangs is stopped and only warns" "0 1" \
    "$(SETUP_CHECK_RC_TIMEOUT=1 sca_check --staging) $(sca_lines '^CHÚ Ý - không đọc được khóa poses_catalog_path (lệnh firebase-tools')"
fi
sca_reset; echo 'not json' > "$sca/remote-config.json"
expect "setup-check app: a template that is not JSON only warns" "0 1" \
  "$(sca_check --staging) $(sca_lines '^CHÚ Ý - không đọc được khóa poses_catalog_path (lệnh firebase-tools')"
sca_reset
expect "setup-check app: a Firebase secret without project id only warns for Remote Config" "0 1" \
  "$(SCA_GOOGLE="$(jq -c 'del(.project_info.project_id)' <<< "$sca_json")" sca_check --staging) $(sca_lines '^CHÚ Ý - không đọc được khóa poses_catalog_path: chưa có google-services.json')"
sca_reset; echo 404 > "$sca/codes/poses_catalog_v9_catalog-00000000.json"
expect "setup-check app: the extra catalog of a trial run is checked" "1 1" \
  "$(SETUP_CHECK_EXTRA_CATALOG=poses/catalog/v9/catalog-00000000.json sca_check --staging) $(sca_lines '^LỖI - khóa poses_catalog_path trỏ tới catalog poses/catalog/v9/catalog-00000000.json')"
sca_reset; sca_rc "{\"defaultValue\": {\"value\": \"$v1\"}, \"conditionalValues\": {\"new_app\": {\"value\": \"$v3\"}}}"; sca_catalog "$newer_schema" > "$sca/body/$v3key"
expect "setup-check app: a conditional-only catalog for a newer app only warns" "0 1" \
  "$(sca_check --staging) $(sca_lines "^CHÚ Ý - khóa poses_catalog_path trỏ tới catalog $v3 (chỉ giá trị theo điều kiện dùng) cần app mới hơn")"
sca_reset; sca_rc "{\"defaultValue\": {\"value\": \"$v3\"}}"; sca_catalog "$newer_schema" > "$sca/body/$v3key"
expect "setup-check app: the default value pointing to a catalog for a newer app stops" "1 1" \
  "$(sca_check --staging) $(sca_lines "^LỖI - khóa poses_catalog_path trỏ tới catalog $v3 cần app mới hơn")"
sca_reset
expect "setup-check app: a workflow that does not pass the Remote Config variable only warns" "0 1 1" \
  "$(SCA_RC_UNSET=1 sca_check --staging) $(sca_lines '^CHÚ Ý - .*workflow không truyền biến REMOTE_CONFIG_CREDENTIALS') $(sca_calls "^GET .*/$v1\$")"
sca_reset; printf '{"schema": 1, "minAppSchema": 1, "poses": [{"id": "a", "starter": true, "preview": "x", "outline": "poses/img/v1/a/outline.webp"}]}' > "$sca/body/$v1key"
expect "setup-check app: a preview that is not an object checks the outline only" "0 1 0" \
  "$(sca_check) $(sca_lines '^ok   - catalog .* và 1 file khởi đầu') $(sca_calls '^HEAD .*/x$')"
sca_reset; printf '{"schema": 1, "minAppSchema": 1, "poses": [{"id": "a", "starter": false, "outline": "poses/img/v1/a/outline.webp"}]}' > "$sca/body/$v1key"
expect "setup-check app: a catalog without starter poses warns" "0 1 0" \
  "$(sca_check) $(sca_lines '^CHÚ Ý - catalog mặc định của app .* không có tư thế starter nào') $(sca_lines '^ok   - catalog ')"
sca_reset; printf '503\n503\n' > "$sca/codes/poses_img_v1_w01_outline.webp"; echo 200 > "$sca/nocache/poses_img_v1_w01_outline.webp"
expect "setup-check app: a 503 gets no CDN hint" "1 0" "$(sca_check) $(sca_lines 'CDN')"
sca_reset; printf '{"schema": 1, "minAppSchema": 1, "poses": [' > "$sca/body/$v1key"
for i in 1 2 3 4; do printf '{"id": "p%s", "starter": true, "outline": "poses/img/v1/p%s/outline.webp"},' "$i" "$i" >> "$sca/body/$v1key"; printf '000\n' > "$sca/codes/poses_img_v1_p${i}_outline.webp"; done
printf '{"id": "z", "starter": false}]}' >> "$sca/body/$v1key"
expect "setup-check app: R2 that stops answering ends the catalog part with one line" "1 1 3" \
  "$(sca_check) $(sca_lines '^LỖI - R2 không trả lời') $(sca_calls '^HEAD .*/p[0-9]/outline.webp$' | awk '{print $1 / 2}')"
sca_starters() { # <n>: a default catalog with n starter poses p1..pn, outline only
  local i
  printf '{"schema": 1, "minAppSchema": 1, "poses": [' > "$sca/body/$v1key"
  for i in $(seq 1 "$1"); do printf '{"id": "p%s", "starter": true, "outline": "poses/img/v1/p%s/outline.webp"},' "$i" "$i" >> "$sca/body/$v1key"; done
  printf '{"id": "z", "starter": false}]}' >> "$sca/body/$v1key"
}
sca_reset; sca_starters 5; for i in 1 3 5; do echo 000 > "$sca/codes/poses_img_v1_p${i}_outline.webp"; done
expect "setup-check app: files that answer between silent ones do not count as R2 down" "1 0 5" \
  "$(sca_check) $(sca_lines 'R2 không trả lời') $(sca_calls '^HEAD .*/p[0-9]/outline.webp$' | awk '{print $1 - 3}')"
sca_reset; sca_starters 4; for i in 1 2 3 4; do echo 503 > "$sca/codes/poses_img_v1_p${i}_outline.webp"; done
expect "setup-check app: 5xx in a row also stops the catalog part" "1 1 3" \
  "$(sca_check) $(sca_lines '^LỖI - R2 không trả lời được') $(sca_calls '^HEAD .*/p[0-9]/outline.webp$' | awk '{print $1 / 2}')"
sca_reset; echo 403 > "$sca/codes/poses_img_v1_w01_outline.webp"; echo 200 > "$sca/nocache/poses_img_v1_w01_outline.webp"
expect "setup-check app: a 403 the CDN keeps gets the CDN hint" "1 1" "$(sca_check) $(sca_lines '^LỖI - file khởi đầu .*CDN còn giữ câu trả lời cũ')"
sca_reset; printf '502\n200\n' > "$sca/codes/poses_img_v1_w01_outline.webp"
expect "setup-check app: a 502 that passes on the retry is not a failure" 0 "$(sca_check)"
sca_reset
expect "setup-check app: the catalog part stops with a note when its time is up" "0 1 0" \
  "$(SETUP_CHECK_CATALOG_SECONDS=0 sca_check) $(sca_lines '^CHÚ Ý - phần kiểm catalog quá 0 giây') $(sca_calls "^GET .*/$v1\$")"
sca_reset; printf '200\n200\n404\n' > "$sca/codes/$v1key"; echo '{"poses": ' > "$sca/body/$v1key"
expect "setup-check app: a catalog that answers 404 when downloaded again reports the 404" "1 1" \
  "$(sca_check) $(sca_lines "^LỖI - catalog mặc định của app $v1 trả '404' ở lần tải lại")"
# Premium poses (T-085, spec poses question 6): the default pose and the starter poses are never premium.
sca_premium() { # <catalog file>: a free or premium first pose a, a starter pose b, then a free pose c
  printf '{"schema": 1, "minAppSchema": 1, "poses": [{"id": "a"%s}, {"id": "b", "starter": true%s, "outline": "poses/img/v1/b/outline.webp"}, {"id": "c"%s}]}' \
    "$2" "$3" "$4" > "$1"
}
sca_reset; sca_premium "$sca/body/$v1key" ', "premium": true' '' ''
expect "setup-check app: a premium default pose stops, naming it" "1 1 0" \
  "$(sca_check) $(sca_lines "^LỖI - catalog mặc định của app $v1 có tư thế premium ở chỗ không được (mặc định a)") $(sca_lines '^ok   - catalog ')"
sca_reset; sca_premium "$sca/body/$v1key" '' ', "premium": true' ''
expect "setup-check app: a premium starter pose stops, naming it, and its files are still checked" "1 1 1" \
  "$(sca_check) $(sca_lines "^LỖI - catalog mặc định của app $v1 có tư thế premium ở chỗ không được (khởi đầu b)") $(sca_calls '^HEAD .*/b/outline.webp$')"
sca_reset; sca_rc "{\"defaultValue\": {\"value\": \"$v3\"}}"; sca_premium "$sca/body/$v3key" '' ', "premium": true' ''
expect "setup-check app: a premium starter pose in the Remote Config catalog stops" "1 1" \
  "$(sca_check --staging) $(sca_lines "^LỖI - khóa poses_catalog_path trỏ tới catalog $v3 có tư thế premium ở chỗ không được (khởi đầu b)")"
sca_reset; sca_premium "$sca/body/$v1key" '' '' ''
expect "setup-check app: a catalog without the premium field has no premium line" "0 0" "$(sca_check) $(sca_lines 'premium')"
sca_reset; sca_premium "$sca/body/$v1key" '' '' ', "premium": true'
expect "setup-check app: a premium pose that is neither the default nor a starter passes" "0 0 1" \
  "$(sca_check) $(sca_lines 'premium') $(sca_lines "^ok   - catalog mặc định của app $v1 và 1 file khởi đầu")"
expect "setup-check app: never prints secret values" 0 "$(grep -c 'RC-MARKER-1\|demo-staging\|fake-token' "$work/sca-all.out" || true)"
printf 'problem "app check ran"\n' > "$work/sca-hook.sh"
expect "setup-check: sources the app checks file and counts its problems" "1 1" \
  "$(SETUP_CHECK_APP_CHECKS="$work/sca-hook.sh" sca_check) $(sca_lines '^LỖI - app check ran')"
for p in poses/catalog/v1/catalog-587b5394.json images/v2/hero.webp levels/v1.3/level-01.json images/hero.v2.webp images/hero-v12.png images/hero.3f2a9c1e.webp sounds/click_0123456789abcdef.ogg; do
  expect "setup-check app: $p is a valid asset path" 0 "$(bash -c 'eval "$(sed -n "/^valid_asset_path()/,/^}/p" scripts/setup-check-app.sh)"; valid_asset_path "$1"; echo $?' _ "$p")"
done
for p in images/hero.webp hero.webp images/v2 images/hero.abc.webp images/latest/hero.webp "" /images/v2/hero.webp images//v2/hero.webp images/v2/../hero.webp ../v2/hero.webp .download/v2/hero.webp https://example.com/v2/hero.webp "images/v2/hero.webp?x=1" "images/v2/hero image.webp"; do
  expect "setup-check app: '$p' is not a valid asset path" 1 "$(bash -c 'eval "$(sed -n "/^valid_asset_path()/,/^}/p" scripts/setup-check-app.sh)"; valid_asset_path "$1"; echo $?' _ "$p")"
done
