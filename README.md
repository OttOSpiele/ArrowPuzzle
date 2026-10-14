# OttOSpiele / ArrowPuzzle

An ad-free, offline-first arrow puzzle for Android.

## Development status

The `develop` branch contains the initial **B.1 bootstrap**: straight-line arrows,
a constructive solvable-level generator, life and hint counters, progression, retries
of the same level and voluntary step-down.

**Not yet ported from the previous prototype:** multi-segment arrow geometry,
KEY_ARROW / ALTERNATING_CHAIN / BRANCH patterns, persistence, and polished animations.
This version has not yet been verified with a successful Android CI build.

## GitHub Actions: download the debug APK

The workflow at [`.github/workflows/android.yml`](.github/workflows/android.yml) runs
for pushes to `develop`, pull requests targeting `develop` or `main`, and manual runs.

1. Open **Actions** in this repository.
2. Select **Android Debug APK**.
3. Open the newest successful workflow run.
4. Download the **OttOSpiele-ArrowPuzzle-debug** artifact at the bottom of the run.
5. Extract the ZIP and install `app-debug.apk` on your Android device.

The APK is a **debug build**, not a Play Store release. No release signing is configured.

## Local build (optional)

Requires JDK 17, Android SDK 35, and Gradle 8.11.1.

```sh
gradle :game-core:test :app:assembleDebug
```

The debug APK is produced at `app/build/outputs/apk/debug/app-debug.apk`.
The Gradle wrapper is not yet included; CI installs the pinned Gradle version using
`gradle/actions/setup-gradle`.
