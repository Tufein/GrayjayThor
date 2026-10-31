#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "${BASH_SOURCE[0]}")/.."

if [[ -z "${ANDROID_HOME:-}" ]]; then
    echo 'Stel ANDROID_HOME in op de Android SDK-map voordat je bouwt.' >&2
    exit 1
fi

git submodule sync --recursive
git submodule update --init --recursive --depth 1 --jobs 4 -- \
    dep/futopay app/src/stable/assets/sources
git lfs install --local
git lfs pull --include='app/aar/ffmpeg-kit.aar'

./gradlew :app:assembleThorDebug :app:testThorDebugUnitTest \
    -PtargetAbis=arm64-v8a,x86_64 --tests 'com.futo.platformplayer.thor.*' "$@"
