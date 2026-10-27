#!/usr/bin/env bash
# Builds the signed production release (Android App Bundle) on the release owner's machine (ADR-0009, ADR-0017).
# The person then uploads the bundle in Play Console. Nothing here runs on GitHub.
#
# Usage:  scripts/release.sh [--commit <sha>] [--config <folder>]
#   --commit  commit of main to release; default: the latest commit of origin/main.
#   --config  release folder copied from the team's password manager;
#             default: ~/.vgl-release/<applicationId>.
#
# The release folder (never inside the repo, deleted after the release):
#   google-services.json   Firebase config of the production project
#   upload.jks             upload key of the app on Google Play
#   release.properties     admobAppId=ca-app-pub-<publisher>~<app>
#                          adUnits=<placement key>=ca-app-pub-<publisher>/<unit>;<placement key>=...
#                          assetsBaseUrl=https://<production address on R2>/
#                          uploadKeyAlias=<alias of the upload key>
# Passwords of the upload key are asked for when the script runs; they are never stored in the folder.
#
# Only commits of main that passed main-guard.yml are built (commits touching docs only reuse the run of
# the code below them). The build runs in a temporary worktree, so local changes never reach a release.
#
# Written during the template phase only. AI never changes this file afterwards (ADR-0009).
# Messages are in Vietnamese for the team; comments are in English.

set -euo pipefail
cd "$(dirname "$0")/.."

die() { echo "TỪ CHỐI: $*" >&2; exit 1; }
step() { echo "== $*"; }

TEST_PUBLISHER="3940256099942544"
STAGING_PROJECT_ID="fake-staging-project"
BUNDLE="app/build/outputs/bundle/productionRelease/app-production-release.aab"
MAPPING="app/build/outputs/mapping/productionRelease/mapping.txt"

commit_arg=""
config_dir=""
while [ "$#" -gt 0 ]; do
  case "$1" in
    --commit) [ "$#" -ge 2 ] || die "thiếu giá trị cho --commit."; commit_arg="$2"; shift 2 ;;
    --config) [ "$#" -ge 2 ] || die "thiếu giá trị cho --config."; config_dir="$2"; shift 2 ;;
    *) die "tham số không hợp lệ: '$1'. Cách dùng: scripts/release.sh [--commit <sha>] [--config <thư mục>]" ;;
  esac
done

for tool in git gh jq keytool; do
  command -v "$tool" > /dev/null || die "không tìm thấy $tool."
done

# Reads `key=value` from release.properties without executing the file.
property() {
  local value
  value="$(sed -n "s/^[[:space:]]*$1[[:space:]]*=[[:space:]]*//p" "$config_dir/release.properties" | tail -n 1)"
  printf '%s' "$value" | sed 's/[[:space:]]*$//'
}

app_id_of() { sed -nE 's/^[[:space:]]*applicationId = "([^"]+)"$/\1/p' | head -n 1; }

# 1. The release folder: present, outside the repo, with values that look like production values.
step "Kiểm tra thư mục phát hành"
if [ -z "$config_dir" ]; then
  local_app_id="$(app_id_of < app/build.gradle.kts)"
  [ -n "$local_app_id" ] || die "không đọc được applicationId trong app/build.gradle.kts."
  config_dir="$HOME/.vgl-release/$local_app_id"
fi
[ -d "$config_dir" ] || die "không có thư mục phát hành $config_dir. Xem docs/ops/release.md."
config_dir="$(cd "$config_dir" && pwd -P)"
repo_dir="$(pwd -P)"
case "$config_dir/" in
  "$repo_dir"/*) die "thư mục phát hành nằm trong repo ($config_dir). Chuyển nó ra ngoài repo." ;;
esac
for file in google-services.json upload.jks release.properties; do
  [ -f "$config_dir/$file" ] || die "thiếu $config_dir/$file. Xem docs/ops/release.md."
done

admob_app_id="$(property admobAppId)"
ad_units="$(property adUnits)"
assets_base_url="$(property assetsBaseUrl)"
upload_key_alias="$(property uploadKeyAlias)"

[[ "$admob_app_id" =~ ^ca-app-pub-[0-9]+~[0-9]+$ ]] ||
  die "admobAppId phải có dạng ca-app-pub-<số>~<số> (mã ứng dụng AdMob, không phải mã quảng cáo)."
[[ "$admob_app_id" != *"$TEST_PUBLISHER"* ]] || die "admobAppId đang là mã thử của Google."
[[ "$ad_units" != *"$TEST_PUBLISHER"* ]] || die "adUnits có mã quảng cáo thử của Google."
configured_keys=""
if [ -n "$ad_units" ]; then
  IFS=';' read -r -a entries <<< "$ad_units"
  for entry in "${entries[@]}"; do
    entry="$(printf '%s' "$entry" | tr -d '[:space:]')"
    [ -n "$entry" ] || continue
    [[ "$entry" =~ ^[a-z0-9_]+=ca-app-pub-[0-9]+/[0-9]+$ ]] ||
      die "mục adUnits '$entry' phải có dạng <khóa vị trí>=ca-app-pub-<số>/<số>."
    configured_keys="$configured_keys ${entry%%=*}"
  done
fi
[[ "$assets_base_url" =~ ^https://[^/]+/.*$ ]] && [[ "$assets_base_url" == */ ]] ||
  die "assetsBaseUrl phải là địa chỉ https và kết thúc bằng '/' (ADR-0015)."
[ -n "$upload_key_alias" ] || die "thiếu uploadKeyAlias trong release.properties."

# 2. The commit: on main, and checked green by main-guard.yml.
step "Kiểm tra commit phát hành"
git fetch --quiet --tags origin main
if [ -n "$commit_arg" ]; then
  target="$(git rev-parse --verify --quiet "$commit_arg^{commit}")" || die "không tìm thấy commit $commit_arg."
  git merge-base --is-ancestor "$target" origin/main || die "commit ${target::7} không nằm trên nhánh main."
else
  target="$(git rev-parse origin/main)"
fi

# main-guard.yml ignores docs-only pushes, so the guarded commit is the newest one changing code at or below
# the target. --first-parent keeps the merge commits that were actually pushed to main.
guarded="$(git log -1 --first-parent --format=%H "$target" -- . ':(exclude)docs/**' ':(exclude)*.md')"
[ -n "$guarded" ] || die "không tìm thấy commit có code ở dưới ${target::7}."
guard_conclusions="$(gh run list --workflow main-guard.yml --commit "$guarded" --limit 20 \
  --json status,conclusion --jq '[.[] | select(.status == "completed") | .conclusion] | join(",")')"
[[ ",$guard_conclusions," == *",success,"* ]] ||
  die "commit ${guarded::7} chưa qua main-guard.yml (kết quả: '${guard_conclusions:-chưa có}'). Chờ nhánh main xanh."
broken="$(gh issue list --label main-broken --state open --json number --jq 'map(.number) | join(",")')"
[ -z "$broken" ] || die "nhánh main đang đỏ (issue main-broken #$broken đang mở)."

version_code="$(git show "$target:app/build.gradle.kts" | sed -nE 's/^[[:space:]]*versionCode = ([0-9]+)$/\1/p')"
version_name="$(git show "$target:app/build.gradle.kts" | sed -nE 's/^[[:space:]]*versionName = "([^"]+)"$/\1/p')"
[[ "$version_code" =~ ^[0-9]+$ ]] && [ -n "$version_name" ] ||
  die "không đọc được versionCode và versionName trong defaultConfig của app/build.gradle.kts."
tag="release/$version_code"
if git rev-parse --verify --quiet "refs/tags/$tag" > /dev/null; then
  die "versionCode $version_code đã được phát hành (tag $tag). Tăng versionCode bằng một PR rồi chạy lại."
fi

# 3. The release folder matches the code being released.
step "Đối chiếu cấu hình với commit ${target::7}"
app_id="$(git show "$target:app/build.gradle.kts" | app_id_of)"
jq -e --arg pkg "$app_id" 'any(.client[]; .client_info.android_client_info.package_name == $pkg)' \
  "$config_dir/google-services.json" > /dev/null ||
  die "google-services.json không có app $app_id."
! jq -e --arg id "$STAGING_PROJECT_ID" '.project_info.project_id == $id' "$config_dir/google-services.json" > /dev/null ||
  die "google-services.json là file cấu hình giả của staging."
placement_file="$(git ls-tree -r --name-only "$target" core/ads/src/main | grep '/AdPlacement\.kt$' | head -n 1)"
[ -n "$placement_file" ] || die "không tìm thấy AdPlacement.kt trong :core:ads."
placements="$(git show "$target:$placement_file" | sed -nE 's/^[[:space:]]*[A-Z0-9_]+\("([a-z0-9_]+)", AdFormat\.[A-Z]+\),?$/\1/p')"
missing=""
for key in $placements; do
  case " $configured_keys " in *" $key "*) ;; *) missing="$missing $key" ;; esac
done
# A placement without an ad unit would silently show no ad (ConfiguredAdUnitIds.missing, ADR-0013).
[ -z "$missing" ] || die "adUnits thiếu mã quảng cáo cho vị trí:$missing."

cat << EOF
   App:        $app_id
   Phiên bản:  $version_name (versionCode $version_code)
   Commit:     ${target::7} $(git log -1 --format=%s "$target")
   Vị trí quảng cáo: ${placements:-không có}
EOF

# 4. Passwords, asked for on the terminal only.
read -r -s -p "Mật khẩu keystore upload.jks: " keystore_password < /dev/tty; echo
read -r -s -p "Mật khẩu của khóa (Enter nếu giống mật khẩu keystore): " key_password < /dev/tty; echo
[ -n "$keystore_password" ] || die "chưa nhập mật khẩu keystore."
key_password="${key_password:-$keystore_password}"
RELEASE_STOREPASS="$keystore_password" keytool -list -keystore "$config_dir/upload.jks" -storepass:env RELEASE_STOREPASS -alias "$upload_key_alias" \
  > /dev/null 2>&1 < /dev/null ||
  die "không mở được khóa '$upload_key_alias' trong upload.jks (sai mật khẩu hoặc tên khóa)."

# 5. Build in a clean worktree of the target commit.
work="$(mktemp -d)"
cleanup() {
  git worktree remove --force "$work/src" > /dev/null 2>&1 || true
  rm -rf "$work"
}
trap cleanup EXIT
step "Build bản phát hành từ ${target::7}"
git worktree add --quiet --detach "$work/src" "$target"
if [ -f local.properties ]; then
  cp local.properties "$work/src/"
elif [ -z "${ANDROID_HOME:-}" ]; then
  # Default SDK folders of Android Studio on macOS and Linux.
  for sdk in "$HOME/Library/Android/sdk" "$HOME/Android/Sdk"; do
    [ ! -d "$sdk" ] || { export ANDROID_HOME="$sdk"; break; }
  done
  [ -n "${ANDROID_HOME:-}" ] || die "không tìm thấy Android SDK. Đặt biến ANDROID_HOME rồi chạy lại."
fi
cp "$config_dir/google-services.json" "$work/src/app/src/production/google-services.json"
(
  cd "$work/src"
  # Values go through the environment, so they never appear in the process list.
  export ORG_GRADLE_PROJECT_admobAppId="$admob_app_id"
  export ORG_GRADLE_PROJECT_adUnits="$ad_units"
  export ORG_GRADLE_PROJECT_assetsBaseUrl="$assets_base_url"
  export ORG_GRADLE_PROJECT_uploadKeystoreFile="$config_dir/upload.jks"
  export ORG_GRADLE_PROJECT_uploadKeystorePassword="$keystore_password"
  export ORG_GRADLE_PROJECT_uploadKeyAlias="$upload_key_alias"
  export ORG_GRADLE_PROJECT_uploadKeyPassword="$key_password"
  ./gradlew --no-daemon --console=plain :app:bundleProductionRelease
)
unset keystore_password key_password

# 6. Check the bundle is signed, keep it with its mapping file, and mark the version as released.
[ -f "$work/src/$BUNDLE" ] || die "không tìm thấy $BUNDLE sau khi build."
certificate="$(keytool -printcert -jarfile "$work/src/$BUNDLE" 2> /dev/null | sed -nE 's/^[[:space:]]*SHA256: //p' | head -n 1)"
[ -n "$certificate" ] || die "bản build chưa được ký."
out="release-output/$version_code"
mkdir -p "$out"
cp "$work/src/$BUNDLE" "$out/$app_id-$version_name-$version_code.aab"
[ ! -f "$work/src/$MAPPING" ] || cp "$work/src/$MAPPING" "$out/mapping.txt"

git tag -a "$tag" "$target" -m "Phát hành $version_name (versionCode $version_code)"
git push --quiet origin "$tag"

cat << EOF
== Xong
   File: $out/$app_id-$version_name-$version_code.aab
   Chứng chỉ ký (SHA-256): $certificate
   Tag: $tag (đã đẩy lên GitHub)

Việc tiếp theo (docs/ops/release.md):
   1. So chứng chỉ ở trên với "Upload key certificate" trong Play Console > Test and release > App integrity.
   2. Tải file .aab lên kênh Internal testing, kiểm tra trên máy thật, rồi chuyển sang Production.
   3. Xóa thư mục phát hành $config_dir khỏi máy.
EOF
