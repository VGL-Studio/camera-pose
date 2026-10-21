#!/usr/bin/env bash
# Setup script for the cloud environment of the auto-run routine (ADR-0009).
# Paste this file into the "Setup script" field of the cloud environment.
# Needs the Custom network level: the default allowlist plus dl.google.com.
# After changing this file, paste it again into the environment: the cached environment is rebuilt from it.
# The environment already ships JDK and Gradle; this script only installs the Android SDK.
# Safe to run any number of times: every step checks what is already there.

set -euo pipefail

# Bump on every change. The auto-run session compares it with ~/.cloud-setup-version
# (scripts/cloud-env-check.sh) and stops at once when the environment still runs an older copy.
SETUP_VERSION=3

SDK_ROOT="${HOME}/android-sdk"
CMDLINE_TOOLS_ZIP="commandlinetools-linux-13114758_latest.zip"
SDKMANAGER="${SDK_ROOT}/cmdline-tools/latest/bin/sdkmanager"

if [ ! -x "${SDKMANAGER}" ]; then
  # Download and unpack in a temp dir, then move into place in one step,
  # so a failed earlier run never leaves a half-installed directory behind.
  work_dir="$(mktemp -d)"
  trap 'rm -rf "${work_dir}"' EXIT
  curl -fsSL "https://dl.google.com/android/repository/${CMDLINE_TOOLS_ZIP}" -o "${work_dir}/tools.zip"
  unzip -q -o "${work_dir}/tools.zip" -d "${work_dir}/unpacked"
  mkdir -p "${SDK_ROOT}/cmdline-tools"
  rm -rf "${SDK_ROOT}/cmdline-tools/latest"
  mv "${work_dir}/unpacked/cmdline-tools" "${SDK_ROOT}/cmdline-tools/latest"
fi

export ANDROID_HOME="${SDK_ROOT}"
yes | "${SDKMANAGER}" --licenses > /dev/null || true
# sdkmanager skips packages that are already installed.
"${SDKMANAGER}" --install \
  "platform-tools" \
  "platforms;android-36" \
  "build-tools;36.0.0" > /dev/null

# Tell Gradle and later shells where the SDK is. Each line is added only once.
for line in "export ANDROID_HOME=${SDK_ROOT}" "export ANDROID_SDK_ROOT=${SDK_ROOT}"; do
  grep -qxF "${line}" "${HOME}/.bashrc" 2>/dev/null || echo "${line}" >> "${HOME}/.bashrc"
done

# Maven Central answers 429 (too many requests) to cloud sessions: their shared addresses hit its rate limit
# (RUN-20261015-02 ended on it). Downloads go to Google's mirror of Maven Central instead, through an init script
# in the Gradle user home, so the repo's build files stay unchanged. The mirror's host matches *.googleapis.com
# in the default allowlist of cloud environments. The init script covers three paths:
# - every mavenCentral() repository (settings, plugin management, buildscript, projects);
# - plugin resolution: gradlePluginPortal() redirects library downloads (kotlin-stdlib for kotlin-dsl in
#   build-logic) to Maven Central, so the mirror is put first in every pluginManagement (RUN-20261016-01, L-018);
# - Robolectric, which downloads its Android jars itself: a system property on every Test task. Set here and not
#   through JAVA_TOOL_OPTIONS, which the environment already uses for its proxy (L-019).
# GRADLE_REFRESH_DEPENDENCIES=true makes Gradle ask the network again after a failed download instead of
# reusing the cached failure (auto-run.md, step 5).
# The SDK location: shells that do not read ~/.bashrc have no ANDROID_HOME, and Gradle then fails with
# "SDK location not found", easy to miss behind -q (L-026). The init script writes sdk.dir into the
# build's local.properties (ignored by git) when neither the variable nor the file gives the SDK.
mkdir -p "${HOME}/.gradle/init.d"
cat > "${HOME}/.gradle/init.d/maven-central-mirror.gradle" << 'GRADLE'
// Written by scripts/cloud-setup.sh: cloud sessions only.
def central = 'https://repo.maven.apache.org/maven2/'
def mirror = 'https://maven-central.storage-download.googleapis.com/maven2/'
def useMirror = { RepositoryHandler repositories ->
    repositories.withType(MavenArtifactRepository).configureEach { repository ->
        if (repository.url.toString() == central) {
            repository.url = mirror
        }
    }
}
if (System.getenv('GRADLE_REFRESH_DEPENDENCIES') == 'true') {
    gradle.startParameter.refreshDependencies = true
}
beforeSettings { settings ->
    def sdk = new File(System.getenv('CLOUD_ANDROID_SDK') ?: "${System.getProperty('user.home')}/android-sdk")
    def localProperties = new File(settings.rootDir, 'local.properties')
    def knowsSdk = System.getenv('ANDROID_HOME') || System.getenv('ANDROID_SDK_ROOT') ||
        (localProperties.exists() && localProperties.text.contains('sdk.dir='))
    if (!knowsSdk && sdk.isDirectory()) {
        localProperties << "sdk.dir=${sdk.absolutePath}\n"
    }
    // Declared before the settings file runs, so the mirror comes first. The plugin portal is added again
    // because Gradle only falls back to it when pluginManagement declares no repository at all.
    settings.pluginManagement.repositories {
        maven { url = mirror; name = 'MavenCentralMirror' }
        gradlePluginPortal()
    }
    useMirror(settings.buildscript.repositories)
    useMirror(settings.pluginManagement.repositories)
    useMirror(settings.dependencyResolutionManagement.repositories)
}
allprojects {
    useMirror(buildscript.repositories)
    useMirror(repositories)
    tasks.withType(Test).configureEach {
        systemProperty 'robolectric.dependency.repo.url', mirror
    }
}
GRADLE

# Version 1 put the Robolectric mirror in JAVA_TOOL_OPTIONS through ~/.bashrc, which overrides the proxy
# options of the environment. Remove that line if an older run left it.
sed -i.bak '/^export JAVA_TOOL_OPTIONS=-Drobolectric.dependency.repo.url=/d' "${HOME}/.bashrc" 2>/dev/null || true
rm -f "${HOME}/.bashrc.bak"

# Other hosts can still answer 429 or time out. Let Gradle retry longer, with backoff, instead of failing the
# build. User-level properties, so the repo's gradle.properties stays unchanged.
GRADLE_USER_PROPERTIES="${HOME}/.gradle/gradle.properties"
mkdir -p "${HOME}/.gradle"
for line in \
  "systemProp.org.gradle.internal.repository.max.retries=8" \
  "systemProp.org.gradle.internal.repository.initial.backoff=1000"; do
  grep -qxF "${line}" "${GRADLE_USER_PROPERTIES}" 2>/dev/null || echo "${line}" >> "${GRADLE_USER_PROPERTIES}"
done

echo "${SETUP_VERSION}" > "${HOME}/.cloud-setup-version"
echo "Android SDK đã sẵn sàng tại ${SDK_ROOT}. Setup script phiên bản ${SETUP_VERSION}."
