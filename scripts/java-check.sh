#!/usr/bin/env bash
# Stops scripts/check.sh before Gradle when Java cannot run the project's Gradle (L-022).
# Gradle 8.14.3 runs on Java 17 to 24; CI uses 17. Change the range below when Gradle is upgraded.
# JAVA_CMD replaces the java command in tests.
# Changing this file changes the quality gate: high-risk plan, cross-review required.

set -uo pipefail

MIN_JAVA=17
MAX_JAVA=24
GRADLE_VERSION="8.14.3"

java_cmd="${JAVA_CMD:-${JAVA_HOME:+$JAVA_HOME/bin/}java}"
hint() {
  echo "Đặt JAVA_HOME tới JDK của Android Studio rồi chạy lại, ví dụ:"
  echo "  macOS: export JAVA_HOME=\"/Applications/Android Studio.app/Contents/jbr/Contents/Home\""
  echo "  Linux: export JAVA_HOME=\"\$HOME/android-studio/jbr\""
  echo "(docs/ops/setup-new-app.md, mục 1)"
}
refuse() {
  [[ -n "${GITHUB_ACTIONS:-}" ]] && echo "::error::$1"
  echo "LỖI: $1"
  hint
  exit 1
}

if ! version_line="$("$java_cmd" -version 2>&1 | head -n 1)" || [[ -z "$version_line" ]]; then
  refuse "không chạy được java ($java_cmd). Gradle $GRADLE_VERSION của dự án cần Java $MIN_JAVA tới $MAX_JAVA."
fi
version="$(sed -nE 's/.*version "([0-9][0-9._]*)".*/\1/p' <<< "$version_line")"
major="${version%%.*}"
# Java 8 and older print "1.8.0_392".
if [[ "$major" == "1" ]]; then
  major="$(cut -d. -f2 <<< "$version")"
fi
if ! [[ "$major" =~ ^[0-9]+$ ]]; then
  refuse "không đọc được phiên bản Java từ '$version_line'. Gradle $GRADLE_VERSION của dự án cần Java $MIN_JAVA tới $MAX_JAVA."
fi
if (( major < MIN_JAVA || major > MAX_JAVA )); then
  refuse "Java $major không chạy được Gradle $GRADLE_VERSION của dự án (cần $MIN_JAVA tới $MAX_JAVA)."
fi
echo "Java $major: dùng được với Gradle $GRADLE_VERSION."
