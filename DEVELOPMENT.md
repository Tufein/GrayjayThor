# Developing Bluejay Thor

The active application is this complete Bluejay-based checkout. The earlier
GrayjayThor checkout is a separate reference and is not an application dependency.
Use the existing isolated checkout; create a worktree only if explicitly requested.

## Tools

- Full JDK 21, including `javac`.
- Git and Git LFS.
- Android command-line tools with `ANDROID_HOME` pointing to the SDK.
- Android platform **37.0** and build tools **37.0.0**.
- The checked-in Gradle 9.5.1 wrapper; no global Gradle installation is required.

For a local Android SDK:

```sh
sdkmanager 'platforms;android-37.0' 'build-tools;37.0.0' 'platform-tools'
export JAVA_HOME=/path/to/jdk-21
export ANDROID_HOME=/path/to/android-sdk
```

The package ID contains `.0`: `platforms;android-37` does not exist in the current
SDK repository. Use the pinned toolchain rather than lowering the compile SDK to
make dependency validation pass.

## Build

From the repository root, the successor's wrapper prepares dependencies and runs
the supported build workflow:

```sh
./scripts/build-thor.sh
```

For direct Gradle tasks, prepare dependencies first and always include the
composite-build toolchain init script:

```sh
./scripts/prepare-dependencies.sh
./gradlew --init-script scripts/dependency-toolchain.init.gradle help
./gradlew --init-script scripts/dependency-toolchain.init.gradle \
  :app:assembleThorDebug -PtargetAbis=arm64-v8a,x86_64 --max-workers=4
```

Thor APKs are written to `app/build/outputs/apk/thor/debug/`. Use the arm64-v8a
APK on a physical Thor and x86_64 on a matching emulator. Debug builds are test
builds; official releases require their own signing and release validation.

## Dependencies

The public Bluejay revision referenced two unpublished dependency commits and
GitLab-relative submodule URLs that no longer resolved from its GitHub mirror.
The setup now uses these published predecessor revisions, which were recorded
immediately before Bluejay's toolchain migration:

- FutoPay: `d571092959c5816596a585141e4dc0771d651f83`.
- Polycentric Android: `a4e5881c8a8fb19329d3b961a44b17cc7a4a6c10`.

The root init script aligns their plugins without editing either dependency's
source checkout. Stable source plugins retain their upstream pins. Native
archives are restored from FUTO's original LFS endpoint and checked against the
exact hashes pinned by Bluejay. See [dependency recovery](docs/dependency-recovery.md)
for the original unavailable revisions, hashes and validation evidence.

Dependency preparation is repeatable. It refuses to change an existing dependency
revision if that checkout contains local changes. Do not reset or delete such
changes to force a build through.

## Cloud environment

This prepared environment has a full Temurin JDK at `/workspace/jdk`, Android SDK
at `/workspace/android-sdk` and a shared Gradle user directory with the session's
proxy settings. Activate them before direct commands:

```sh
export JAVA_HOME=/workspace/jdk
export ANDROID_HOME=/workspace/android-sdk
export GRADLE_USER_HOME=/workspace/gradle-home
export PATH="$JAVA_HOME/bin:$ANDROID_HOME/platform-tools:$PATH"
export JAVA_TOOL_OPTIONS=-Djavax.net.ssl.trustStore=/etc/ssl/certs/java/cacerts
```

Keep the inherited network proxy and CA trust enabled. SDK manager downloads in
this environment use `--proxy=http --proxy_host=proxy --proxy_port=8080`.

## Validation

`scripts/build-thor.sh` runs the 67 targeted JVM checks alongside both APK builds.
With a matching Android device or emulator connected, run the focused Android
regressions with the same init script:

```sh
./gradlew --init-script scripts/dependency-toolchain.init.gradle \
  :app:connectedThorDebugAndroidTest -PtargetAbis=arm64-v8a,x86_64 \
  -Pandroid.testInstrumentationRunnerArguments.class=com.tsutsen.platformplayer.activities.CompanionPresentationInputTest,com.tsutsen.platformplayer.activities.GamepadInputResetTest,com.tsutsen.platformplayer.theme.BluejayThemeContrastTest \
  --max-workers=4
```

This subset covers companion input routing, held-input reset and actual theme
color roles. Android 9/API 28 exercises the pre-Material-You color fallback;
Material You remains enabled on Android 12/API 31 or newer. Use a physical Thor
for actual dual-screen, controller hardware and video-playback validation. A JVM policy test or successful APK build does not
establish physical Thor display behavior. Preserve test runner exit statuses and
check that tests executed; a zero-test run is not validation.

Gradle is the compilation authority. `AGENTS.md` describes how to recognize and
refresh stale Kotlin language-server diagnostics after dependency changes.
