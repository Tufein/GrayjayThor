# Reproducible Bluejay dependency setup

The successor keeps the complete upstream Bluejay application. Its September 2026
commit `bddaa22334aba2f42e263716f2cf9265dd95a07d` upgraded the Android toolchain and
also pointed at dependency revisions unavailable from the published dependency
repositories. Direct Git fetches of both revisions return `not our ref`.

| Dependency | Unavailable Bluejay revision | Published revision used |
| --- | --- | --- |
| FutoPay | `e7f0993c7fd1bf7a06718ff59f2a581d59153034` | `d571092959c5816596a585141e4dc0771d651f83` |
| Polycentric Android | `651c2efd7837f7509a6eaa381c73900758ea6f6b` | `a4e5881c8a8fb19329d3b961a44b17cc7a4a6c10` |

These published revisions are the exact dependency pins recorded in Bluejay
immediately before that toolchain commit. They retain the original FUTO namespaces,
which the application's imports already use. Their source checkouts remain
unchanged. `scripts/dependency-toolchain.init.gradle` aligns their Gradle plugins
with the application's AGP 9.2.1, Kotlin 2.4.0 and protobuf plugin 0.10.0, and
selects the existing classic Kotlin/Android DSL mode for the composite builds.
No missing commit is reconstructed or claimed to be equivalent.

The root `.gitmodules` now uses explicit HTTPS FUTO repository URLs. Relative
GitLab-era URLs previously resolved under `github.com/tsutsen`, where the required
repositories do not exist. Stable source plugins keep their existing pinned
revisions.

## Prepare and build

Install a full JDK 21, Git LFS, Android SDK **platform 37.0** and build tools
**37.0.0**. The SDK manager package IDs are `platforms;android-37.0` and
`build-tools;37.0.0`; the older `platforms;android-37` package ID does not exist.

From the repository root:

```sh
./scripts/prepare-dependencies.sh
./gradlew --init-script scripts/dependency-toolchain.init.gradle help
```

Pass the same init script to all direct Gradle build, test and Android Studio
Gradle invocations that include these dependency builds. The successor's build
wrapper handles this automatically. The preparation script is repeatable and
refuses to switch a dependency revision if it would overwrite local changes.

## Native archives

The GitHub mirror's LFS endpoint currently responds `Resource not accessible by
integration`. FUTO's original Grayjay LFS endpoint serves the exact same pinned
archives. The preparation script downloads them through Git LFS and verifies:

| Archive | SHA-256 |
| --- | --- |
| `app/aar/ffmpeg-kit.aar` | `22c06ca0d1a5808b2fc0a12227d5915b3126bc0b9b1305cf6bab855f2ec6fcbb` |
| `app/aar/sender-sdk-minimal-0.4.1.aar` | `50b53a5d297d0c3008b3359a452a5c9e7e9f530533ec197e3dd1e9f08f9e84ad` |

The Gradle wrapper distribution is also pinned to its official SHA-256 in
`gradle/wrapper/gradle-wrapper.properties`. TLS and archive verification remain
enabled.

## Validation performed

- Dependency preparation succeeded, including all stable source plugins.
- Repeating preparation succeeded with the same revisions and archive hashes.
- `./gradlew --init-script scripts/dependency-toolchain.init.gradle help`
  configured the root application and both composite dependency builds successfully.

Application build and test results are recorded separately by the successor's
build validation; configuration success alone does not validate playback.
