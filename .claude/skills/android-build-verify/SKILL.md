---
name: android-build-verify
description: How to build, test, install and drive the Vraj Kheti Hisab Android app on Windows — Gradle tasks, adb, emulator, and mobile-mcp device checks. Load before running builds/tests or verifying a change on a device.
---

# Build & verify (Windows)

Prereqs: JDK 17, Android SDK (platform-tools on PATH), an emulator "Pixel_API_35" or a USB-debugging Galaxy S22.

1. Build: `./gradlew assembleDebug` (PowerShell: `.\gradlew.bat assembleDebug`).
2. Unit tests: `./gradlew testDebugUnitTest`. Reports: `app/build/reports/tests/`.
3. Device tests: `adb devices` → `./gradlew connectedDebugAndroidTest`.
4. Install & launch:
   - `adb install -r app/build/outputs/apk/debug/app-debug.apk`
   - `adb shell am start -n com.vraj.khetihisab/.MainActivity`
5. Logs: `adb logcat -s VrajKheti:* AndroidRuntime:E`.
6. UI checks: use the `mobile-mcp` tools (list devices → launch app → screenshot → tap/type). Compare each screenshot with the matching Design canvas artboard.

FLAG_SECURE blocks screenshots. Debug builds read `BuildConfig.ALLOW_SCREENSHOTS = true`; release builds must keep it false.

Report the exact commands you ran and their pass/fail output.
