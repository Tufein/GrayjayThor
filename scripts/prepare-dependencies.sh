#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "${BASH_SOURCE[0]}")/.."

# Bluejay's September 2026 toolchain update referenced unpublished dependency
# commits. Use its last published dependency revisions; the companion Gradle
# init script aligns their plugins without modifying their source checkouts.
prepare_dependency() {
    local path="$1" revision="$2" url="$3"
    if [[ ! -e "$path/.git" ]]; then
        git clone --no-checkout --depth 1 "$url" "$path"
        git -C "$path" fetch --depth 1 "$url" "$revision"
        git -C "$path" checkout --detach "$revision"
        git submodule absorbgitdirs "$path"
    fi
    local current
    current=$(git -C "$path" rev-parse --verify HEAD)
    if [[ "$current" != "$revision" ]]; then
        if ! git -C "$path" diff --quiet || ! git -C "$path" diff --cached --quiet || \
           [[ -n "$(git -C "$path" ls-files --others --exclude-standard)" ]]; then
            printf 'Local changes in %s; preserve them before changing dependency revisions.\n' "$path" >&2
            return 1
        fi
        if ! git -C "$path" cat-file -e "$revision^{commit}" 2>/dev/null; then
            git -C "$path" fetch --depth 1 "$url" "$revision"
        fi
        git -C "$path" checkout --detach "$revision"
    fi
}

git submodule init -- dep/futopay dep/polycentricandroid

prepare_dependency dep/futopay d571092959c5816596a585141e4dc0771d651f83 \
    https://gitlab.futo.org/videostreaming/futopayclientlibraries.git
prepare_dependency dep/polycentricandroid a4e5881c8a8fb19329d3b961a44b17cc7a4a6c10 \
    https://gitlab.futo.org/videostreaming/polycentricandroid.git

git submodule sync --recursive
git submodule update --init --recursive --depth 1 --jobs 4 -- app/src/stable/assets/sources

git lfs install --local
# These identical, hash-pinned upstream artifacts are still hosted by FUTO.
# The GitHub mirror's LFS integration does not grant downloads of its copies.
git -c lfs.url=https://gitlab.futo.org/videostreaming/grayjay.git/info/lfs \
    lfs pull --include='app/aar/ffmpeg-kit.aar,app/aar/sender-sdk-minimal-0.4.1.aar'
printf '%s\n' \
    '22c06ca0d1a5808b2fc0a12227d5915b3126bc0b9b1305cf6bab855f2ec6fcbb  app/aar/ffmpeg-kit.aar' \
    '50b53a5d297d0c3008b3359a452a5c9e7e9f530533ec197e3dd1e9f08f9e84ad  app/aar/sender-sdk-minimal-0.4.1.aar' \
    | sha256sum --check
