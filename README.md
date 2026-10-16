# SafeReach

SafeReach is an Android disaster-management app for the AUT BCIS 2026 project. It focuses on emergency alerts, local mesh communication, and a Compose-based user interface.

Alerts reach the app from three sources: Firestore-backed Cloud Functions push them over Firebase Cloud Messaging, an HTTPS `alertsApi` Cloud Function is polled on launch, and nearby devices relay them over a Bluetooth Low Energy mesh. Firebase Authentication gates the UI, and Mapbox renders the alert map (including downloadable offline regions).

**Package Name:** `com.p56.safereach`

## Documentation

- **[ARCHITECTURE.md](./ARCHITECTURE.md)** - Current architecture, data flow, and dependency layout
- **[DEVELOPMENT.md](./DEVELOPMENT.md)** - Development workflow, testing, and contribution standards

## Quick Start

### Prerequisites

- Android Studio with the Android SDK installed
- JDK 17
- Android Gradle Plugin / Gradle wrapper from the repo
- Firebase configuration for the app (`google-services.json`)
- Node.js 24 and the Firebase CLI (only needed to build or deploy the Cloud Functions in `functions/`)

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
- **Target SDK:** Android 16 (API 36)
- **Compile SDK:** API 37 (`compileSdk = "release"` via the AGP release config)
- **Language:** Kotlin
- **UI Framework:** Jetpack Compose + Material 3
- **Architecture:** Compose UI with ViewModels, repositories, and Hilt
- **Persistence:** Room (alerts and signed messages) plus DataStore (preferences)
- **Authentication:** Firebase Authentication (email/password)
- **Realtime communication:** Firebase Cloud Messaging, the alerts HTTP API, and the BLE mesh service
- **Mapping:** Mapbox SDK + Compose map extension (with offline regions)
- **Backend:** Firebase Cloud Functions (TypeScript, Node 24)

## Current architecture summary

The app is built around a Compose UI layer, state holders, and background services:

- `MainActivity` starts the app and ensures the BLE mesh service is running.
- `MainScreen` is the Firebase Authentication gate (sign-in, register, and forgot-password) and renders `SafeReachApp` once a user is signed in.
- `SafeReachApp` hosts the top-level navigation (Home, Alerts, Profile, and a debug-only Debug tab) and the permission/health banners.
- `HomeRoute`/`HomeScreen` render the Mapbox map and the active-alert panel.
- `AlertHistoryRoute`/`AlertHistoryScreen` show every stored alert through `AlertHistoryViewModel`.
- `ProfileScreen` handles account details, notification and theme preferences, and offline-map management.
- `DebugRoute`/`DebugScreen` render mesh diagnostics and let a debug build author a local alert.
- `DebugViewModel` owns debug screen state and exposes the current mesh/alert state.
- `AlertRepository` stores the current signed message and the alerts (backed by Room) and triggers notifications for newly active alerts.
- `AlertNotifier` posts the high-priority system notification for a new active alert.
- `OfflineRegionManager`/`OfflineRegionViewModel` download and clear Mapbox offline regions.
- `MeshStatusStore` tracks the mesh service and subsystem states.
- `MeshForegroundService` receives and advertises mesh messages and updates `MeshStatusStore`.
- `FirebaseAlertMessagingService` decodes incoming Firebase alerts and stores them through the same `AlertRepository` path.
- `AlertSyncManager` pulls alerts from the `alertsApi` Cloud Function on app launch and applies them through `AlertRepository`.

## Contributing

1. Create a feature branch.
2. Follow the guidance in [DEVELOPMENT.md](./DEVELOPMENT.md).
3. Update docs when behavior or architecture changes.
4. Validate with the relevant Gradle task before opening a PR.

