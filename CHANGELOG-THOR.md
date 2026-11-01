# Bluejay Thor changes

## Unreleased — rebuffer loop fix

- Root cause found from a Thor playback log: for non-SABR (unmuxed progressive)
  streams, "Auto" quality loaded the first source the plugin listed, i.e. 2160p
  VP9, which a normal connection cannot sustain (playback stalled with ~50 ms
  buffered). Auto now picks the highest source up to 1080p whose bitrate fits
  within 75% of the bandwidth estimate.
- Playback stalls longer than 10 s now save a diagnostic log to Downloads.
- SABR throughput estimation ignored every response slower than 2x real time,
  so on a connection slower than the stream the estimate never left its seed
  and the server kept choosing bitrates the network could not sustain. Slow
  responses are now counted when the buffer is nearly empty (clearly
  network-limited).
- Active player: resume after a stall only once 5 s is buffered (was 1.5 s),
  and start at 1.5 s, so playback no longer flips between a few seconds of
  video and another stall.

Not built or tested here (dependencies unavailable in the cloud container) and
not verified on a physical Thor.

## 0.7.3-thor.1 — YouTube startup and buffering

- Resolve the playable source before starting comments, recommendations and
  chapters, so optional plugin work cannot take the shared engine client first.
- Seed YouTube SABR with Media3's network-aware bandwidth estimate (converted
  from bits/s to bytes/s), replacing the fixed 104,857 bytes/s fallback on this
  playback route. Subsequent SABR transfer measurements remain in control.
- Configure the active Compose player with a 750 ms startup threshold and
  1,500 ms rebuffer threshold, while continuing to load 30–60 seconds ahead
  when the source permits. The old legacy player configuration did not apply
  to this player. These thresholds are buffer targets, not measured load times.
- Add startup regressions proving optional work follows source resolution and
  superseded resolutions do not issue optional requests.

Validation: Thor ARM64/x86_64 APKs and the Android test APK build successfully.
All 93 targeted JVM tests pass with zero failures or skips. ARM64 signature and
16 KiB ZIP alignment pass; the preview signing identity is unchanged.

Actual YouTube startup time and sustained playback on a physical Thor still
need verification; cloud tests cannot reproduce the device/CDN path.

## 0.7.2-thor.1 — playback and controller reliability

- Safe relative seeking when a stream's duration is unknown, including remote
  playback without a local decoder. Sub-500 ms clips no longer produce invalid
  seek ranges, and large offsets cannot wrap to a negative position.
- Remapping a controller action replaces its original key; explicit assignments
  take precedence over another action's default without reviving removed actions.
- Speed-up/slow-down holds combine deterministically across buttons and devices,
  restore the original rate in either release order, and honor explicit speed
  selections and controller disablement.
- Disconnecting a controller releases only that device's held keys and axes.
  Another connected controller retains its active input state.
- Cancel stale comment-page requests when switching/closing videos, reject late
  results and collapse duplicate scroll requests into one in-flight page load.
- Start live chat once per video instead of reconnecting on volume, speed or
  other unrelated player-state updates.

### Validation

- `scripts/build-thor.sh --console=plain` completed successfully.
- **91 JVM tests passed, zero failures or skips:** 47 application tests and
  44 playback-control/gesture tests, including 24 new regression tests.
- **16 Android instrumentation tests passed, zero failures or skips**, on an
  Android 9 / API 28 x86_64 emulator, including three new tests for independent
  controller disconnection and held-input cleanup.
- Thor debug APKs built for arm64-v8a and x86_64, version `0.7.2-thor.1` / `7002`.
- ARM64 APK signature and 16 KiB ZIP alignment verified; the signing certificate
  matches `0.7.1-thor.1` for in-place preview updates.

Physical Thor and online-provider/casting playback still require
hardware/service validation.

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
