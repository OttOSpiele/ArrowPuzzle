# OttOSpiele / ArrowPuzzle

A completely ad-free, offline-first children's arrow puzzle for Android.

## State of the project

Branch `develop` contains **B.1 bootstrap**, a deliberately reduced, build-oriented first version.
It currently includes basic straight-line arrows, a constructive solvable-level generator,
life and hint counters, progression, retry of the same level, and manual step-down.

**Still to port from the earlier B.1 source package:** the multi-segment geometry,
KEY_ARROW/ALTERNATING_CHAIN/BRANCH pattern families, persistence and polished animations.
This branch has NOT been built on an Android SDK in the ChatGPT environment.
It must not be mistaken for a validated or release-ready application.

## Android build

JDK 17 and Android SDK API 35 are required. Use Gradle 8.11.1:

```sh
gradle :game-core:test :app:assembleDebug
```

Output: `app/build/outputs/apk/debug/app-debug.apk`.

The Gradle wrapper *properties* file is provided; `gradlew` and its wrapper JAR
are not committed, so the workflow downloads a pinned Gradle distribution itself.

## Codemagic

1. Add `OttOSpiele/ArrowPuzzle` in https://codemagic.io/.
2. Choose the `codemagic.yaml` configuration on branch `develop`.
3. Run workflow `android-debug`.
4. Download `app-debug.apk` from Build artifacts once CI succeeds.

Only the debug APK is produced; no Google Play signing or publication is set up.
