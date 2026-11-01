# Bluejay Thor changes

## 0.7.1-thor.1 — development preview

Based on the complete Bluejay 0.7.0 source, commit
`6a762681cf489aee20f744ae357e247c92ae4b5b`.

- Add the Thor application flavor with its own package ID and app label.
- Enable the second-screen dashboard and controller input by default on fresh
  Thor installs; preserve saved preferences and custom bindings.
- Default both Thor seek directions to ten seconds.
- Observe display changes while the main activity is in the foreground. Retain
  an eligible output, replace stale outputs and recover from window/display races.
- Exclude the activity's own display, private outputs and sleeping or invalid
  displays from companion selection. Reevaluate configuration and PiP transitions.
- Cancel lifecycle retries and dismiss the companion when leaving the foreground.
- Forward gamepad input from the companion window and dispose its composition
  when dismissed, retaining normal touch and mouse behavior.
- Release held digital/analog inputs when the application stops while preserving
  binding-popup ownership; prevent pointer/touchpad coordinates from being interpreted as sticks.
- Skip upstream launch telemetry in Thor builds.
- Resolve Material You colors only on Android 12/API 31 or newer; older devices
  use the built-in light/dark palettes so filled controls retain readable contrast.
- Keep successor app updates separate from upstream APK delivery. Preserve source
  plugin updates and original project attribution.

### Validation

- `scripts/build-thor.sh --console=plain` completed successfully, including a
  repeated build using the same dependency setup.
- **67 JVM tests passed, zero failures or skips:** 38 application policy,
  settings and update tests, plus 29 gesture tests.
- **13 Android instrumentation tests passed, zero failures or skips**, on an
  Android 9 / API 28 x86_64 software emulator. These cover companion key/motion
  dispatch, pointer fallthrough, held-input release, direction reversal and
  light/dark theme contrast on pre-Material-You Android.
- Normal app startup and the Bluejay welcome screen were checked on API 28;
  the fallback palette renders readable blue controls. See the
  [startup screenshot](docs/validation/bluejay-thor-api28-welcome.png).
- Thor debug APKs built for arm64-v8a and x86_64, version `0.7.1-thor.1` / `7001`,
  application ID `com.tufein.bluejay.thor`. The Android test APK also built.
- arm64 APK signature and 16 KiB ZIP alignment verified with build tools 37.0.0.
- Dependency preparation succeeded repeatedly with both native archives matching
  their pinned SHA-256 hashes; dependency source checkouts remain unmodified.

One pre-existing gesture test still expected the old normal bottom-center
`NONE` default. It now follows upstream's intentional `MORPH_VERTICAL` change
from commit `ebab6e1fc98d4039dd59082aa1c2069439554dd3`, with an additional check
that explicit user-selected `NONE` continues to work. Production gesture
behavior is unchanged.

Physical AYN Thor display hotplug, long playback sessions, online sources,
casting and production release signing remain outside this validation.
Publication status is separate from this validation record.
