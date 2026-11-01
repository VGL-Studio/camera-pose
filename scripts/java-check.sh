#!/usr/bin/env bash
# Checks, before Gradle starts, that the Java which ./gradlew will use can run this build (app lesson L-022 of
# camera-pose, template lesson L-059). A Java that is too new makes Gradle stop with an error that only says
# its version number ("26"), which tells the developer nothing.
#
# Bounds come from the build configuration, so they follow the project when it is upgraded:
#   lower: the highest JavaVersion.VERSION_<n> in build-logic (the build compiles for that Java);
#   upper: the newest Java that the Gradle version of gradle/wrapper/gradle-wrapper.properties can run on
#          (https://docs.gradle.org/current/userguide/compatibility.html). A Gradle newer than the table has
#          no upper bound here; Gradle reports it itself.
# Like ./gradlew, uses $JAVA_HOME/bin/java when JAVA_HOME is set, otherwise java from PATH.
# Exit 0 fits, 1 does not fit or no Java found. Called by scripts/check.sh.
# Messages are in Vietnamese for the team; comments are in English.
set -euo pipefail
cd "$(dirname "$0")/.."

wrapper="gradle/wrapper/gradle-wrapper.properties"
min_java="$({ grep -rhoE 'JavaVersion\.VERSION_[0-9]+' build-logic --include='*.kts' --include='*.kt' || true; } \
  | sed 's/.*_//' | sort -n | tail -n 1)"
min_java="${min_java:-17}"
gradle_version="$(sed -n 's/^distributionUrl=.*gradle-\([0-9][0-9.]*\)-.*$/\1/p' "$wrapper" 2> /dev/null || true)"

# Newest Java that each Gradle version runs on; empty when this table does not know the version.
max_java_for_gradle() {
  local major minor
  major="${1%%.*}"
  minor="$(echo "$1" | cut -d. -f2)"
  case "$major" in
    8)
      if [ "$minor" -ge 14 ]; then echo 24
      elif [ "$minor" -ge 10 ]; then echo 23
      elif [ "$minor" -ge 8 ]; then echo 22
      elif [ "$minor" -ge 5 ]; then echo 21
      else echo 20
      fi ;;
    9)
      if [ "$minor" -ge 4 ]; then echo ""
      elif [ "$minor" -ge 1 ]; then echo 25
      else echo 24
      fi ;;
    *) echo "" ;;
  esac
}
max_java=""
[ -z "$gradle_version" ] || max_java="$(max_java_for_gradle "$gradle_version")"

java_cmd="java"
[ -z "${JAVA_HOME:-}" ] || java_cmd="${JAVA_HOME}/bin/java"

studio_hint() {
  local mac="/Applications/Android Studio.app/Contents/jbr/Contents/Home"
  echo "Dùng JDK đi kèm Android Studio (docs/ops/setup-new-app.md, phần 1):" >&2
  if [ -d "$mac" ]; then
    echo "  export JAVA_HOME=\"$mac\"" >&2
  else
    echo "  macOS: export JAVA_HOME=\"$mac\"" >&2
    echo "  Linux: export JAVA_HOME=\"\$HOME/android-studio/jbr\" (thư mục cài Android Studio)" >&2
  fi
}

if ! command -v "$java_cmd" > /dev/null 2>&1; then
  echo "LỖI: không tìm thấy Java ($java_cmd). Dự án cần Java ${min_java}${max_java:+ đến $max_java}." >&2
  studio_hint
  exit 1
fi

# java.specification.version is "17", "21", ... (or "1.8" for Java 8).
spec="$("$java_cmd" -XshowSettings:properties -version 2>&1 | sed -n 's/^ *java\.specification\.version = //p' | head -n 1)"
version="${spec#1.}"
if ! [[ "$version" =~ ^[0-9]+$ ]]; then
  echo "LỖI: không đọc được phiên bản của $java_cmd." >&2
  studio_hint
  exit 1
fi

if [ "$version" -lt "$min_java" ]; then
  echo "LỖI: Java $version quá cũ ($java_cmd). Dự án build cho Java $min_java (build-logic), cần Java $min_java trở lên." >&2
  studio_hint
  exit 1
fi
if [ -n "$max_java" ] && [ "$version" -gt "$max_java" ]; then
  echo "LỖI: Java $version quá mới ($java_cmd). Gradle $gradle_version chỉ chạy trên Java $min_java đến $max_java." >&2
  studio_hint
  exit 1
fi
echo "Java $version ($java_cmd): hợp với Gradle ${gradle_version:-?}, cần Java ${min_java}${max_java:+ đến $max_java}."
