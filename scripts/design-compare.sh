#!/usr/bin/env bash
# Compares the approved design images of a feature with its screenshot test images (ADR-0020).
# Usage: scripts/design-compare.sh <feature>
#   design images:     docs/features/<feature>/design/screens/<name>.png
#   screenshot images: feature/<feature>/src/test/screenshots/<name>.png
#   result:            docs/features/<feature>/design/review/ (side-by-side images and summary.md)
# A design image without its numbers in design/context/<name>.md is listed in the summary.
# The result is for review, not a gate: it never fails because images differ.
set -euo pipefail
cd "$(dirname "$0")/.."

[ "$#" -eq 1 ] || { echo "Cách dùng: scripts/design-compare.sh <feature>" >&2; exit 2; }
feature="$1"
design_dir="docs/features/${feature}/design/screens"
screenshot_dir="feature/${feature}/src/test/screenshots"
review_dir="docs/features/${feature}/design/review"
[ -d "${design_dir}" ] || { echo "Không có ${design_dir}." >&2; exit 1; }
[ -d "${screenshot_dir}" ] || { echo "Không có ${screenshot_dir}. Chạy scripts/check.sh trước." >&2; exit 1; }

java_cmd="java"
[ -z "${JAVA_HOME:-}" ] || java_cmd="${JAVA_HOME}/bin/java"
rm -rf "${review_dir}"
"${java_cmd}" -Djava.awt.headless=true scripts/design-compare/DesignCompare.java \
  "${design_dir}" "${screenshot_dir}" "${review_dir}"

context_dir="docs/features/${feature}/design/context"
missing_context=()
for image in "${design_dir}"/*.png; do
  name="$(basename "${image}" .png)"
  [ -f "${context_dir}/${name}.md" ] || missing_context+=("${name}")
done
if [ "${#missing_context[@]}" -gt 0 ]; then
  {
    echo
    echo "Thiếu số liệu trong design/context/: ${missing_context[*]}"
  } | tee -a "${review_dir}/summary.md"
fi
