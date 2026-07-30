# SafeReach - Architecture & Dependencies

## Project Overview

**SafeReach** is a disaster management Android application developed for the AUT BCIS 2026 project. The app is designed to help users stay connected and safe during emergency situations.

- **Package Name:** `com.p56.safereach`
- **Minimum SDK:** 29
- **Target SDK:** 37
- **Compile SDK:** 37
- **Language:** Kotlin with Jetpack Compose

---

## Architecture Overview

### UI Architecture

SafeReach follows a **modern Android architecture** based on **Jetpack Compose** and **Material Design 3**, emphasizing declarative UI and state management.

| Layer                   | Technology / Component | Details                                                                 |
|-------------------------|------------------------|-------------------------------------------------------------------------|
| **Application Layer**   | **MainActivity**       | Entry point of the application                                          |
| **UI Layer**            | **Jetpack Compose**    | • SafeReachApp<br>• Navigation Suite Scaffold<br>• Composable Functions |
| **Theme Layer**         | **Material Design 3**  | • SafeReachTheme<br>• Color Schemes<br>• Typography                     |
| **Backend Integration** | **Firebase**           | • Cloud Messaging<br>• Real-time Notifications<br>• Cloud Functions     |

#### Navigation Structure

The app uses `AppDestinations` enum to manage navigation:

```kotlin
enum class AppDestinations(
    val label: String,
    val icon: Int,
) {
    HOME("Home", R.drawable.ic_home),
    FAVORITES("Favorites", R.drawable.ic_favorite),
    PROFILE("Profile", R.drawable.ic_account_box),
}
```

### Cloud Integration

The backend is powered by **Firebase**, providing serverless architecture for real-time communication and notifications.

- Cloud functions are used for backend logic and to trigger notifications.
- Firebase Cloud Messaging is used for push notifications to the app.

---

## Key Features & Capabilities

### 1. **BLE Nearby broadcast / receiving**
- Uses Bluetooth Low Energy (BLE) for device discovery
- Broadcasts alerts to nearby devices
- Scans continuously for incoming alerts

### 2. **Serverless Backend**
- Firebase Cloud Functions for backend logic
- No dedicated server required, reducing maintenance overhead

### 3. **Real-time Communications**
- Firebase Cloud Messaging integration for push notifications
- Enables sending emergency alerts to users

### 4. **Modern UI Toolkit**
- Fully built with Jetpack Compose for declarative UI
- Preview capabilities for development

---

## Dependencies

### Dependency Management

The project uses **Gradle Version Catalog** (`gradle/libs.versions.toml`) for centralized dependency management, improving maintainability and consistency.

#### Dependencies Update Information

All dependencies are specified in `gradle/libs.versions.toml` for easier maintenance and updates. To update a dependency:

1. Update the version in `libs.versions.toml`
2. Rebuild the project
3. The changes will automatically apply across all modules

### **Firebase Integration**
- **Firebase BOM**: `Latest`
- **Firebase Cloud Messaging**: `(BOM)`
  - Push notifications for emergency alerts
  - Real-time user communication during disasters
- **Firebase Cloud Functions**: `(BOM)`
  - Serverless functions for backend logic

### Testing Dependencies

| Library                                | Version   | Purpose                  |
|----------------------------------------|-----------|--------------------------|
| `junit:junit`                          | `4.13.2`  | Unit testing framework   |
| `androidx.test.ext:junit`              | `1.3.0`   | Android JUnit extensions |
| `androidx.test.espresso:espresso-core` | `3.7.0`   | UI testing framework     |
| `androidx.compose.ui:ui-test-junit4`   | (via BOM) | Compose UI testing       |

---

## Build Configuration

### Build Variants

- **Debug Build** - Full debugging capabilities
- **Release Build** - Optimized with ProGuard (enabled if minification needed)

### Build Commands

```bash
# Build debug APK
./gradlew assembleDebug

# Build release APK
./gradlew assembleRelease

# Run tests
./gradlew test

# Run instrumented tests
./gradlew connectedAndroidTest

# Clean build
./gradlew clean build
```

### Deployment Commands

```bash
# Deploy firebase resources
firebase deploy
```

### Development Tools
- Android Studio (Recommended)
- Kotlin 2.3.21 compiler
- Gradle 9.2.1
- JDK 11+

---

## Development Standards & Procedures

For comprehensive information on development practices, testing strategies, and deployment procedures, refer to **[DEVELOPMENT.md](./DEVELOPMENT.md)**.

This document covers:
- **Code Style Guide** - Naming conventions for Kotlin & TypeScript (PascalCase, camelCase, UPPERCASE)
- **Code Quality Guidelines** - Single responsibility, meaningful naming, DRY principle, pure functions
- **Security Requirements** - Input verification, environment variable usage, secret management
- **Version Control** - Git workflow, commit standards, branching strategy (main/development/feature/fix)
- **Testing Strategy** - Unit tests, integration tests, E2E tests, linting with KLint/Prettier
- **CI/CD Pipeline** - Automated testing and validation with GitHub Actions
- **Definition of Done** - Complete checklist for feature completion

---

## Contributing Guidelines

When adding new dependencies:
1. Add version to `gradle/libs.versions.toml`
2. Add library reference in the same file
3. Use aliases in `build.gradle.kts`
4. Keep dependencies up to date and regularly scan for security vulnerabilities

---

## License & References

- [Jetpack Compose Documentation](https://developer.android.com/develop/ui/compose)
- [Material Design 3](https://m3.material.io/)
- [Firebase Documentation](https://firebase.google.com/docs)#
- [Android Gradle Plugin Guide](https://developer.android.com/build/releases/gradle-plugin)
