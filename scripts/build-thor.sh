#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "${BASH_SOURCE[0]}")/.."

: "${ANDROID_HOME:?Set ANDROID_HOME to the Android SDK directory (platform 37.0).}"
if [[ -n "${JAVA_HOME:-}" ]]; then
    [[ -x "$JAVA_HOME/bin/javac" ]] || { echo 'JAVA_HOME must point to a full JDK, not a JRE.' >&2; exit 1; }
else
    command -v javac >/dev/null || { echo 'A full JDK 17+ is required; set JAVA_HOME.' >&2; exit 1; }
fi

./scripts/prepare-dependencies.sh
./gradlew --init-script scripts/dependency-toolchain.init.gradle \
    :app:assembleThorDebug :app:assembleThorDebugAndroidTest \
    :feature:player:impl:testDebugUnitTest \
    :app:testThorDebugUnitTest \
    --tests 'com.tsutsen.platformplayer.activities.*' \
    --tests '*SuccessorDefaultsTest' \
    --tests '*PluginAppIdentityTest' \
    --tests '*SuccessorUpdateTest' \
    --tests '*SettingsRepositoryImplTest' \
    --tests 'com.tsutsen.platformplayer.player.*' \
    -PtargetAbis=arm64-v8a,x86_64 --max-workers=4 "$@"
