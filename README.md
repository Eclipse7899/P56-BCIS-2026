# SafeReach

SafeReach is an Android disaster-management app for the AUT BCIS 2026 project. It focuses on emergency alerts, local mesh communication, and a Compose-based user interface.

**Package Name:** `com.p56.safereach`

## Documentation

- **[ARCHITECTURE.md](./ARCHITECTURE.md)** - Current architecture, data flow, and dependency layout
- **[DEVELOPMENT.md](./DEVELOPMENT.md)** - Development workflow, testing, and contribution standards

## Quick Start

### Prerequisites

- Android Studio with the Android SDK installed
- JDK 17
- Android Gradle Plugin / Gradle wrapper from the repo
- Firebase configuration for the app

### Setup

```bash
# Clone
git clone https://github.com/Eclipse7899/P56-BCIS-2026.git
cd P56-BCIS-2026

# Generate debug signing keys
./gradlew generateSigningKeysDebug

# Sync and build the project
./gradlew assembleDebug
```

### Common build commands

```bash
# Debug build
./gradlew assembleDebug

# Release build
./gradlew assembleRelease

# Unit tests
./gradlew test

# Instrumented tests
./gradlew connectedAndroidTest
```

## Current project configuration

- **Minimum SDK:** Android 10 (API 29)
- **Target SDK:** Android 15 (API 36)
- **Compile SDK:** Android 15 (API 37 via AGP release config)
- **Language:** Kotlin
- **UI Framework:** Jetpack Compose + Material 3
- **Architecture:** Compose UI with ViewModels, repositories, and Hilt
- **Realtime communication:** Firebase Cloud Messaging and BLE mesh service
- **Mapping:** Mapbox SDK + Compose map extension

## Current architecture summary

The app is built around a Compose UI layer, state repositories, and background services:

- `MainActivity` starts the app and ensures the BLE mesh service is running.
- `SafeReachApp` hosts the top-level navigation and screen switching.
- `HomeScreen`, `ProfileScreen`, and `DebugScreen` render the main UI.
- `DebugViewModel` owns debug screen state and exposes the current mesh/alert state.
- `AlertStateRepository` stores the active alerts keyed by id, plus the mesh metadata of the last accepted packet.
- `MeshStatusRepository` tracks BLE service and subsystem states.
- `MeshService` receives and broadcasts mesh messages and updates repository state.
- `FirebaseAlertService` decodes incoming Firebase alerts and stores them in the same alert state flow.

## Contributing

1. Create a feature branch.
2. Follow the guidance in [DEVELOPMENT.md](./DEVELOPMENT.md).
3. Update docs when behavior or architecture changes.
4. Validate with the relevant Gradle task before opening a PR.

