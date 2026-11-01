# Bluejay Thor

**[Download the ARM64 APK for the Thor](https://github.com/Tufein/GrayjayThor/raw/e9e65af9a83b9e7b25bc394d11c8427a37e963e8/BluejayThor-0.7.1-thor.1-arm64-v8a-debug.apk)** — unofficial debug preview `0.7.1-thor.1`.

An unofficial continuation of [Bluejay](https://github.com/tsutsen/bluejay) for
Android handhelds with two screens, including the AYN Thor.

This project uses Bluejay's complete application: its Compose interface, source
plugins, video player, downloads, library, themes, gestures and companion-screen
dashboard. The original GrayjayThor checkout is retained separately as a reference;
it is not the application built here.

## What changes in this continuation

- **Ready for two screens:** the Thor flavor enables the companion screen and
  controller support on a fresh installation. Both remain configurable.
- **Recover after screen changes:** display connection, disconnection, sleep and
  wake events refresh the companion. A stale display window is replaced safely.
- **Respect the foreground:** pending retries cannot reopen the companion after
  the app moves into the background.
- **Release held controls:** backgrounding the app releases active controller
  inputs, so a missed key-up cannot leave speed-hold or stick actions latched.
- **Controller input on either screen:** the companion routes gamepad keys and
  stick input to the same player controls as the main screen, while ordinary
  touch and mouse input keep working normally.
- **Separate installation:** `com.tufein.bluejay.thor` can coexist with the original
  Bluejay and GrayjayThor applications.
- **Separate app updates:** the Thor flavor does not download the original
  Bluejay's APKs. Source-plugin updates remain available as before.
- **Avoid inherited launch telemetry:** Thor builds do not send launch statistics
  to the original project's telemetry service.

The existing Bluejay interface and its light/dark themes, configurable dashboard,
comments, queue, recommendations, playlists, home feed and controller mappings
remain the basis of the app. In the Thor flavor both seek directions default to
10 seconds, with START for play/pause and L1/R1 for seeking. Loading saved
preferences preserves explicit user choices and controller mappings.

## Development build

The preview version is **0.7.1-thor.1**, based on upstream Bluejay **0.7.0** at
`6a762681cf489aee20f744ae357e247c92ae4b5b`. This is an unofficial development
build of this continuation, not an official Bluejay release.

Use the existing checkout. The Android SDK, a full JDK and Git LFS are required;
toolchain versions are pinned in the repository. Reproducible dependency setup
and build instructions are in [DEVELOPMENT.md](DEVELOPMENT.md).

The Thor build creates arm64-v8a and x86_64 debug APKs. Install the arm64 APK on the
Thor. A physical device check is still required before distributing a release.
No successor release server or production signing key is configured.

## Sources and attribution

Bluejay is developed by tsutsen and builds on [FUTO's Grayjay](https://gitlab.futo.org/videostreaming/grayjay).
Their notices and the [original license](LICENSE.md) remain in this source tree.
The [upstream README](UPSTREAM-README.md) preserves the original project description,
features and screenshots. This continuation is not an official FUTO or Bluejay product.

The source history is retained. See [CHANGELOG-THOR.md](CHANGELOG-THOR.md) for the
changes and validation of this continuation.
