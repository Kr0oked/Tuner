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

# Screenshots via Fastlane (requires device)
fastlane android grab_screen_phone_1
fastlane android grab_screen_phone_2
fastlane android grab_screen_seven_inch_1
fastlane android grab_screen_seven_inch_2
fastlane android grab_screen_ten_inch_1
fastlane android grab_screen_ten_inch_2
```

Fastlane release builds require env vars: `KEYSTORE_FILE`, `KEYSTORE_PASSWORD`, `KEY_ALIAS`, `KEY_PASSWORD`.
Google Play deployment additionally requires `ANDROID_JSON_KEY_FILE`.

## Architecture

TODO

### Key Packages

TODO

### Data Flow

TODO

## Tech Stack

TODO

## Branch Notes

`master` is the main branch used for releases and PRs.

## Translations

Translations are managed via Weblate. Do not manually edit `strings.xml` files in locale-specific resource directories —
changes come in through automated PRs from Weblate.
