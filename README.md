# Bluejay Thor 0.7.3-thor.1

[Download ARM64 APK](BluejayThor-0.7.3-thor.1-arm64-v8a-debug.apk?download=1)

Unofficial debug preview for AYN Thor, Android 9+. Same signing certificate as
previous previews; install over 0.7.2 without removing app data.

YouTube playback now resolves the video source before optional plugin requests,
uses Media3's network-aware initial SABR bandwidth estimate, and starts the active
player at a 750 ms buffered threshold, with 1500 ms for rebuffering and 30–60 s
ahead buffering when the source allows. Buffer thresholds are not measured load
times; actual Thor/YouTube performance still needs device validation.

93 JVM tests passed. ARM64 signature and 16 KiB ZIP alignment verified.

[Source and changelog](https://github.com/Tufein/GrayjayThor/tree/v0.7.3-thor.1)

Use Git LFS when cloning this asset branch; verify the APK with SHA256SUMS.
