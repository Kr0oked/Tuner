# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

Simple tuner app for Android (Kotlin + Jetpack Compose), published on F-Droid and Google Play.
Package: `com.bobek.tuner`.

## Build & Test Commands

```bash
# Unit tests
./gradlew test

# Instrumented tests (requires connected device/emulator)
./gradlew connectedAndroidTest

# Build debug APK
./gradlew assembleDebug

# Build release APK (requires signing env vars)
fastlane android apk

# Run lint
./gradlew lint

# Screenshots via Fastlane; each lane grabs a light (1.png) and dark (2.png) shot
fastlane android grab_screens          # boots each Screenshots_* emulator in turn and grabs all device types
fastlane android grab_screen_phone     # requires a connected/already-running device
fastlane android grab_screen_seven_inch
fastlane android grab_screen_ten_inch
```

Fastlane release builds require env vars: `KEYSTORE_FILE`, `KEYSTORE_PASSWORD`, `KEY_ALIAS`, `KEY_PASSWORD`.
Google Play deployment additionally requires `ANDROID_JSON_KEY_FILE`.

## Architecture

The app follows MVVM in a single-Activity Compose setup with audio capture running on a background coroutine:

- **`TunerApplication`** — Hilt entry point
- **`MainActivity`** — Single Compose activity; hosts `AppViewModel` and `TunerViewModel`
- **`AppViewModel`** — Night mode preference via `StateFlow`; reads from `SettingsRepository`
- **`TunerViewModel`** — All tuner state via `StateFlow`; manages `AudioRecord` session on an IO coroutine; keeps screen
  on while listening; loaded from `SettingsRepository` on init

### Key Packages

| Package     | Responsibility                                                                                                                                       |
|-------------|------------------------------------------------------------------------------------------------------------------------------------------------------|
| `domain/`   | `PitchDetector` — YIN algorithm (de Cheveigné & Kawahara, 2002) at 44.1kHz PCM FLOAT; `DetectedNote` — note name, octave, frequency, cents deviation |
| `data/`     | `AppNightMode`, `PreferenceChoice` — immutable preference models                                                                                     |
| `settings/` | `DataStoreSettingsRepository` — persists preferences via Jetpack DataStore; injected via Hilt                                                        |
| `ui/`       | Jetpack Compose screens: `tuner/`, `settings/`, `licenses/`, `theme/`                                                                                |

### Data Flow

Microphone → `AudioRecord` (IO coroutine in `TunerViewModel`) → `PitchDetector` (YIN) → `FrequencySmoothing` (EMA) →
`NoteConfirmationGate` (3-frame debounce) → `TunerState` `StateFlow` → `TunerScreen`

Settings changes are debounced 1 second before being written to DataStore.

## Tech Stack

- **UI:** Jetpack Compose + Material3, Navigation Compose
- **DI:** Hilt + KSP
- **Persistence:** DataStore Preferences
- **Audio:** Android `AudioRecord` (PCM FLOAT, 44.1kHz), YIN pitch detection
- **Build:** AGP 9.x, Kotlin 2.x, Java 11 toolchain
- **Testing:** JUnit4, Compose UI Test, kotlinx-coroutines-test, Fastlane Screengrab

## Branch Notes

`master` is the main branch used for releases and PRs.

## Translations

Translations are managed via Weblate. Do not manually edit `strings.xml` files in locale-specific resource directories —
changes come in through automated PRs from Weblate.
