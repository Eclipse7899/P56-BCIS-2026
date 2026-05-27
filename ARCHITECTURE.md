# SafeReach - Architecture & Dependencies

## Project Overview

**SafeReach** is a disaster management Android application developed for the AUT BCIS 2026 project. The app is designed to help users stay connected and safe during emergency situations.

- **Package Name:** `com.p56.safereach`
- **Minimum SDK:** 29
- **Target SDK:** 36
- **Compile SDK:** 36
- **Language:** Kotlin with Jetpack Compose

---

## Architecture Overview

### Architectural Pattern: Composable-Based UI Architecture

SafeReach follows a **modern Android architecture** based on **Jetpack Compose** and **Material Design 3**, emphasizing declarative UI and state management.

```
┌─────────────────────────────────────────────┐
│         Application Layer                    │
│         (MainActivity)                       │
├─────────────────────────────────────────────┤
│         UI Layer                             │
│         (Jetpack Compose)                    │
│         - SafeReachApp                       │
│         - Navigation Suite Scaffold          │
│         - Composable Functions               │
├─────────────────────────────────────────────┤
│         Theme Layer                          │
│         (Material Design 3)                  │
│         - SafeReachTheme                     │
│         - Color Schemes                      │
│         - Typography                         │
├─────────────────────────────────────────────┤
│         Backend Integration                  │
│         (Firebase)                           │
│         - Cloud Messaging                    │
│         - Real-time Notifications            │
└─────────────────────────────────────────────┘
```

### Navigation Structure

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

## Dependencies

### Dependency Management

The project uses **Gradle Version Catalog** (`gradle/libs.versions.toml`) for centralized dependency management, improving maintainability and consistency.

### Core Dependencies

#### **Android Framework**
- **AGP** (Android Gradle Plugin): `9.2.1`
- **Java Version Compatibility:** `11`
- **Kotlin Compiler:** `2.3.21`

#### **Jetpack Libraries**
| Library | Version | Purpose |
|---------|---------|---------|
| `androidx.core:core-ktx` | `1.18.0` | Kotlin extensions for Android core functionality |
| `androidx.lifecycle:lifecycle-runtime-ktx` | `2.10.0` | Lifecycle-aware components |
| `androidx.activity:activity-compose` | `1.13.0` | Compose integration for activities |

#### **Jetpack Compose UI Framework**
- **Compose BOM** (Bill of Materials): `2026.05.01`
- **Material Components:**
  - `androidx.compose.material3:material3` - Material Design 3 components
  - `androidx.compose.material3:material3-adaptive-navigation-suite` - Adaptive navigation
  
- **Core UI Components:**
  - `androidx.compose.ui:ui` - Core UI primitives
  - `androidx.compose.ui:ui-graphics` - Graphics utilities
  - `androidx.compose.ui:ui-tooling` - Development tools
  - `androidx.compose.ui:ui-tooling-preview` - Preview functionality

#### **Firebase Integration**
- **Firebase BOM**: `34.13.0`
- **Firebase Cloud Messaging**: `25.0.2`
  - Push notifications for emergency alerts
  - Real-time user communication during disasters

### Testing Dependencies

| Library | Version | Purpose |
|---------|---------|---------|
| `junit:junit` | `4.13.2` | Unit testing framework |
| `androidx.test.ext:junit` | `1.3.0` | Android JUnit extensions |
| `androidx.test.espresso:espresso-core` | `3.7.0` | UI testing framework |
| `androidx.compose.ui:ui-test-junit4` | (via BOM) | Compose UI testing |

### Debug Dependencies

- `androidx.compose.ui:ui-test-manifest` - Test manifest for UI tests
- `androidx.compose.ui:ui-tooling` - Debugging tools (only in debug builds)

---

## Build Configuration

### Gradle Build Files

#### Root `build.gradle.kts`
Configures project-wide settings and plugins.

#### App `build.gradle.kts`
```kotlin
// Application ID
applicationId = "com.p56.safereach"

// API Levels
minSdk = 29          // Supports Android 10+
targetSdk = 36       // Targets Android 15
compileSdk = 36      // Compiles against Android 15

// Features
buildFeatures {
    compose = true   // Enables Jetpack Compose
}

// Compile Options
compileOptions {
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11
}
```

### Build Variants

- **Debug Build** - Full debugging capabilities
- **Release Build** - Optimized with ProGuard (enabled if minification needed)

## Key Features & Capabilities

### 1. **Adaptive Navigation**
- Uses Material Design 3's `NavigationSuiteScaffold`
- Automatically adapts UI based on screen size and device orientation
- Supports seamless navigation between three main screens

### 2. **Theme System**
- Material Design 3 theming with dynamic colors (Android 12+)
- Support for dark and light modes
- System-aware theme detection

### 3. **Real-time Communications**
- Firebase Cloud Messaging integration for push notifications
- Enables sending emergency alerts to users

### 4. **Modern UI Toolkit**
- Fully built with Jetpack Compose for declarative UI
- Preview capabilities for development
- Edge-to-edge display support

---

## Development Workflow

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

### Development Tools
- Android Studio (Recommended)
- Kotlin 2.3.21 compiler
- Gradle 9.2.1
- JDK 11+

---

## Dependencies Update Information

All dependencies are specified in `gradle/libs.versions.toml` for easier maintenance and updates. To update a dependency:

1. Update the version in `libs.versions.toml`
2. Rebuild the project
3. The changes will automatically apply across all modules

### Latest Versions (as of May 2026)
- **Compose BOM:** 2026.05.01 (Latest)
- **Firebase:** 34.13.0 (Stable)
- **Android Gradle Plugin:** 9.2.1 (Latest)
- **Kotlin:** 2.3.21 (Latest)

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

## Future Development Considerations

1. **State Management:** Consider implementing MVVM or MVI patterns with ViewModel
2. **Data Persistence:** Add Room database for local data storage
3. **Networking:** Implement Retrofit for API communication
4. **Dependency Injection:** Consider Hilt for dependency management
5. **Feature Modules:** Modularize code into feature-specific modules
6. **Navigation Architecture:** Implement Navigation Compose for type-safe routing

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
