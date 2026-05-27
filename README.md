# SafeReach

A disaster management Android application for the AUT BCIS 2026 project, designed to help users stay connected and safe during emergency situations.

**Package Name:** `com.p56.safereach`

## Description

SafeReach is built with modern Android technologies including Jetpack Compose, Material Design 3, and Firebase Cloud Messaging to provide real-time emergency alerts and user connectivity during disasters.

## Documentation

- **[ARCHITECTURE.md](./ARCHITECTURE.md)** - System architecture, component overview, and dependencies
- **[DEVELOPMENT.md](./DEVELOPMENT.md)** - Development standards, testing procedures, and deployment guidelines

## Quick Start

### Prerequisites
- Android Studio Jellyfish or newer
- JDK 11+
- Gradle 9.2.1+
- Firebase project configuration

### Building the Project

```bash
# Build debug APK
./gradlew assembleDebug

# Build release APK
./gradlew assembleRelease

# Run tests
./gradlew test

# Run linting
./gradlew ktlint
```

## Project Details

- **Minimum SDK:** Android 10 (API 29)
- **Target SDK:** Android 15 (API 36)
- **Language:** Kotlin
- **UI Framework:** Jetpack Compose
- **Theme:** Material Design 3

## Development Workflow

1. Create a feature branch: `git checkout -b feature/feature-name`
2. Follow coding standards from [DEVELOPMENT.md](./DEVELOPMENT.md)
3. Write tests for your changes
4. Format code: `./gradlew ktlintFormat`
5. Open a pull request for review
6. Merge after approval and CI/CD passes

## Version Control

- **Main Branch** - Production code (PR required)
- **Development Branch** - Sprint features (PR required)
- **Feature/\*** - Feature development
- **Fix/\*** - Bug fixes

See [DEVELOPMENT.md](./DEVELOPMENT.md#version-control--git-workflow) for detailed workflow.

