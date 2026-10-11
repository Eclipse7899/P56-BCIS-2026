# SafeReach Development and Testing Procedures

This document outlines coding standards, development workflows, testing procedures, and deployment pipelines for the SafeReach project.

---

## Table of Contents

1. [Code Style Guide](#code-style-guide)
2. [Code Quality Guidelines](#code-quality-guidelines)
3. [Security Requirements](#security-requirements)
4. [Version Control & Git Workflow](#version-control--git-workflow)
5. [Testing Strategy](#testing-strategy)
6. [CI/CD Pipeline](#cicd-pipeline)
7. [Cloud Functions (Backend)](#cloud-functions-backend)
8. [Definition of Done](#definition-of-done)

---

## Code Style Guide

### Naming Conventions

SafeReach follows strict naming conventions for Kotlin and TypeScript to ensure consistency and readability across the codebase.

#### Kotlin

| Element        | Convention                             | Example                                          |
|----------------|----------------------------------------|--------------------------------------------------|
| **Classes**    | PascalCase                             | `class UserRepository`                           |
| **Interfaces** | PascalCase                             | `interface DataSource`                           |
| **Enums**      | PascalCase (class), UPPERCASE (values) | `enum class AppDestinations { HOME, PROFILE, SETTINGS, DEBUG }` |
| **Functions**  | camelCase                              | `fun getUserData()`                              |
| **Variables**  | camelCase                              | `val userName`, `var isVisible`                  |
| **Constants**  | UPPERCASE_WITH_UNDERSCORES             | `const val MAX_RETRIES = 3`                      |
| **Packages**   | lowercase, no special characters       | `com.p56.safereach.ui.theme`                     |

#### Project-specific conventions

Beyond the general style rules above, SafeReach uses the following suffixes so that a
type's role is obvious from its name:

| Suffix / pattern        | Meaning                                                        | Examples                                                  |
|-------------------------|----------------------------------------------------------------|-----------------------------------------------------------|
| `*Dto`                  | Protobuf wire type in `com.p56.safereach.proto` (generated)    | `AlertDto`, `SignedMessageDto`, `MessageDataDto`          |
| *(no suffix)*           | Domain model in `com.p56.safereach.model`                      | `Alert`, `Shape`, `SignedMessage`, `ReceivedMessage`, `AlertBatch` |
| `toDomain()`            | Mapper into a domain model (from proto **or** Room entity)     | `model/ProtoMappers.kt`, `data/AlertEntity.kt`            |
| `toEntity()`            | Mapper from a domain model into a Room entity                  | `Alert.toEntity()`                                        |
| `toDto()`               | Mapper from a domain model into a Protobuf wire type           | `Alert.toDto()`                                           |
| `*Entity` / `*Dao`      | Room entity / data-access object                               | `AlertEntity`, `AlertDao`                                 |
| `*Repository`/`*Store`/`*Manager` | Hilt `@Singleton` state holders that are **not** Android services. A stateless helper may also use `*Manager` (for example `AlertSyncManager`). | `AlertRepository`, `MeshStatusStore`, `OfflineRegionManager` |
| `*Service`              | Reserved for real `android.app.Service` subclasses             | `MeshForegroundService`, `FirebaseAlertMessagingService`  |
| `*Intent`               | Desired-state input to a state machine in `*/fsm`              | `BleIntent`, `BleScanIntent`, `BleAdvertisingIntent`      |
| `*State` / `*Event`     | Observed condition / transition trigger of a state machine     | `AggregateBleState`, `BleScanState`, `BleAdvertisingEvent` |
| `*Controller` / `*Adapter` | State-machine owner / Android platform gateway              | `BleScanController`, `BleBeaconScanAdapter`               |
| `*Route` / `*Screen` / `*Content` | Compose stateful entry / screen / stateless body    | `HomeRoute`, `HomeScreen`, `AlertHistoryScreen`          |

Note: `SignedMessage.receivedFrom` and `receivedAt` are also populated for locally
authored messages (for example the debug flow uses `receivedFrom = "Debug"`), so they mean
"origin/observation metadata", not strictly "arrived over the network".

#### TypeScript

| Element        | Convention                             | Example                            |
|----------------|----------------------------------------|------------------------------------|
| **Classes**    | PascalCase                             | `class UserService`                |
| **Interfaces** | PascalCase                             | `interface User`                   |
| **Enums**      | PascalCase (class), UPPERCASE (values) | `enum Status { ACTIVE, INACTIVE }` |
| **Functions**  | camelCase                              | `function getUserData()`           |
| **Variables**  | camelCase                              | `const userName = ""`              |
| **Constants**  | UPPERCASE_WITH_UNDERSCORES             | `const MAX_RETRIES = 3`            |
| **Namespaces** | lowercase, kebab-case for files        | `user-service.ts`                  |

### Formatting Standards

- **Indentation:** 4 spaces (Kotlin), 2 spaces (TypeScript)
- **Line Length:** Maximum 120 characters
- **Imports:** Organize alphabetically and group by type
- **Blank Lines:** Use strategically to separate logical sections (max 2 consecutive)

---

## Code Quality Guidelines

### General Principles

1. **Single Responsibility Principle (SRP)**
   - Each function/class should have one reason to change
   - Break down large functions into smaller, focused units
   - Example:
   ```kotlin
   // Multiple responsibilities
   fun handleUserLogin(email: String, password: String): Boolean {
       validateEmail(email)
       validatePassword(password)
       sendVerificationCode()
       updateDatabase()
       logAnalytics()
       return true
   }
   
   // Single responsibility
   fun authenticateUser(credentials: LoginCredentials): Result<AuthToken> {
       val validatedCredentials = validateCredentials(credentials)
       return repository.authenticate(validatedCredentials)
   }
   ```

2. **Meaningful Naming**
   - Prefer verbose over ambiguous names
   - Names should reveal intent
   - Avoid single-letter variables (except in loops/streams)
   ```kotlin
   // Bad
   val u = User()
   val x = calculateValue()
   
   // Good
   val currentUser = User()
   val totalUserScore = calculateUserScore(user)
   ```

3. **Self-Explanatory Code**
   - Code should be readable without extensive comments
   - Use comments only when necessary to explain "why", not "what"
   - Example:
   ```kotlin
   // Bad: Comment explains what code does
   i++ // increment i
   
   // Good: Code is clear, comment explains why
   retryCount++ // exponential backoff strategy requires incremental retry count
   ```

4. **Documentation Comments**
   - Use KDoc (Kotlin) and TSDoc (TypeScript) for public APIs
   - Document parameters, return values, and exceptions
   ```kotlin
   /**
    * Authenticates a user with the provided credentials.
    *
    * @param email The user's email address
    * @param password The user's password
    * @return A Result containing the authentication token or an error
    * @throws InvalidCredentialsException If credentials are invalid
    */
   fun authenticate(email: String, password: String): Result<AuthToken>
   ```

5. **Magic Numbers & Strings**
   - Extract magic numbers to named constants
   - Keep constants in a dedicated file
   ```kotlin
   // Bad
   if (retries > 3) { /* ... */ }
   
   // Good
   const val MAX_RETRY_ATTEMPTS = 3
   if (retries > MAX_RETRY_ATTEMPTS) { /* ... */ }
   ```

6. **DRY Principle (Don't Repeat Yourself)**
   - Extract common logic into reusable functions/classes
   - Avoid code duplication across modules
   ```kotlin
   // Bad: Duplicated validation logic
   fun validateUserEmail(email: String): Boolean { /* ... */ }
   fun validateAdminEmail(email: String): Boolean { /* ... */ }
   
   // Good: Reusable validation
   fun isValidEmail(email: String): Boolean { /* ... */ }
   ```

7. **Error Handling**
   - Avoid silent failures
   - Handle expected errors explicitly
   - Use sealed classes or Result types for error representation
   ```kotlin
   // Bad: Silent failure
   try {
       val data = fetchData()
       processData(data)
   } catch (e: Exception) {
       // ignored
   }
   
   // Good: Explicit error handling
   when (val result = fetchData()) {
       is Success -> processData(result.data)
       is Failure -> logger.error("Failed to fetch data", result.exception)
   }
   ```

8. **Pure Functions**
   - Prefer pure functions for testability
   - Minimize side effects
   ```kotlin
   // Bad: Has side effects
   fun calculateTotal(items: List<Item>): Double {
       val total = items.sumOf { it.price }
       database.saveTotal(total)  // side effect
       return total
   }
   
   // Good: Pure function
   fun calculateTotal(items: List<Item>): Double {
       return items.sumOf { it.price }
   }
   ```

---

## Security Requirements

### Input Verification

- **Always validate user input** before processing
- **Sanitize external data** from APIs, files, or user input
- **Use type safety** to prevent injection attacks

```kotlin
// Good: Input validation
fun createUser(email: String, age: Int): Result<User> {
    if (!isValidEmail(email)) {
        return Failure(InvalidEmailException())
    }
    if (age < 18 || age > 150) {
        return Failure(InvalidAgeException())
    }
    return Success(User(email, age))
}
```

### Secret Management

- **Never store secrets in codebase**
  - API keys
  - Database credentials
  - Authentication tokens
  - Private keys
  
- **Use environment variables** for sensitive configuration
  - Create `.env` files (add to `.gitignore`)
  - Use GitHub Secrets for CI/CD
  
```kotlin
// Bad: Hardcoded secrets
const val FIREBASE_API_KEY = "AIzaSyD1234567890"

// Good: Environment variables
val firebaseApiKey = System.getenv("FIREBASE_API_KEY")
```

The project already follows this pattern for alert signing: the Android app ships only a
public key (`app/src/main/res/raw/public_key.pem`) and CI injects it from the `PUBLIC_KEY`
GitHub secret, while the Cloud Function reads the matching private key from the
`SIGNING_KEY` parameter. Never commit the private key.

### Additional Security Practices

- Keep dependencies updated and scan for CVEs
- Use HTTPS for all network communications
- Implement proper authentication and authorization
- Log sensitive errors securely
- Follow OWASP guidelines for Android and web development

---

## Version Control & Git Workflow

All code changes are tracked on GitHub, including Android code and Firebase edge functions.

### Commit Best Practices

#### Commit Message Guidelines

- **Keep commits small** - One logical change per commit
- **Use clear, descriptive messages** - Follow conventional commits format
- **Format:** `<type>(<scope>): <subject>`

```
Examples:
- feat(auth): add two-factor authentication
- fix(ui): resolve navigation button misalignment
- docs(readme): update setup instructions
- refactor(database): optimize query performance
- test(unit): add tests for UserRepository
```

#### Pre-Commit Checklist

1. Use "Reformat Code" in Android Studio before committing
   - Windows/Linux: `Ctrl + Alt + L`
   - macOS: `Cmd + Option + L`
2. Ensure all files are properly formatted
3. Run linting to catch style issues
4. Verify tests pass locally

```bash
# Before committing
./gradlew test         # Run unit tests
./gradlew assembleDebug  # Confirm the app still builds
```

> Note: ktlint formatting is a project goal but is **not currently configured** in the
> Gradle build (there is no `ktlint` plugin/task). Until it is added, format Kotlin with
> Android Studio's "Reformat Code" action rather than a Gradle task.

### Pull Request Workflow

#### Opening a PR

1. **Create a descriptive PR title** following conventional commits
2. **Provide PR description** with:
   - Summary of changes
   - Related issue number
   - Testing approach
   - Screenshots (for UI changes)

3. **Request reviews** from at least 2 team members for main branch

#### PR Approval Requirements

- **Development Branch:**
  - Linting passes
  - Unit tests pass
  - Integration tests pass
  
- **Main Branch:**
  - Unit tests pass
  - Integration tests pass
  - Peer review approved
  - CI/CD pipeline passes

#### After Merge

- **Delete the feature branch** immediately after merge
- Verify deployment in staging/production environment

### Branch Strategy

#### Main Branch
- **Purpose:** Production code
- **When to update:** After sprint completion or feature batch
- **Push policy:** Never push directly, only through approved PRs
- **Requirements:**
  - All tests passing
  - Peer review approved
  - Code review from tech lead

#### Development Branch
- **Purpose:** Completed features during sprint
- **Push policy:** Use PRs, except for small fixes
- **Requirements:**
  - Linting passed
  - Unit and integration tests passed
  - Code review from team member

#### Feature/\* Branches
- **Format:** `feature/feature-name` or `feature/ISSUE-123-description`
- **Purpose:** In-progress feature development
- **Base:** Create from `development` branch
- **Example:** `feature/add-emergency-alerts`

#### Fix/\* Branches
- **Format:** `fix/bug-description` or `fix/ISSUE-456-description`
- **Purpose:** Bug fixes and issue resolution
- **Base:** Create from `development` or `main` (if critical)
- **Example:** `fix/notification-crash`

#### Merge Conflict Resolution

- **Always resolve conflicts in the feature/fix branch**, not in main or development
- Update your branch with latest changes:
  ```bash
  git fetch origin
  git rebase origin/development
  # Resolve conflicts
  git add .
  git rebase --continue
  git push --force-with-lease
  ```

---

## Testing Strategy

### Testing Pyramid

```
       /\
      /  \  E2E Tests (Manual)
     /    \
    /------\
   /        \  Integration Tests
  /          \
 /            \
/---------- ----\
|                | Unit Tests
|________________|
```

### Linting

#### Tools

- **Kotlin:** ktlint (see status note below)
- **Kotlin / Android:** Android Lint (`./gradlew lint`)
- **TypeScript (Cloud Functions):** ESLint (`npm --prefix functions run lint`)

#### What Gets Checked

- Code style consistency
- Code formatting
- Naming conventions
- Best practices
- Code smells detection

#### Running Linting

```bash
# Kotlin - Android Lint (available today)
./gradlew lint

# ktlint - NOT YET CONFIGURED in this repo; planned, but no ktlint Gradle task exists yet.
# Once the plugin is added, the intended commands are:
#   ./gradlew ktlint          # Check
#   ./gradlew ktlintFormat    # Auto-fix

# TypeScript (Cloud Functions) - configured in functions/
npm --prefix functions run lint    # Check (eslint --ext .js,.ts .)
npm --prefix functions run build   # Type-check/compile (tsc)
```

### Unit Tests

#### Purpose
- Small, function-focused tests
- Fast execution
- Test logic in isolation

#### What to Test

**Do Test:**
- Utility functions
- Core business logic (authentication, serialization)
- Edge cases
- Valid and invalid inputs
- Error scenarios

**Don't Test:**
- UI components (use UI testing instead)
- Mundane logic (getters/setters)
- Library functionality (third-party code)

#### Test Coverage

Each test should handle three scenarios:

```kotlin
@Test
fun validateEmail_validEmail_returnsTrue() {
    // Valid case
    assertTrue(validateEmail("user@example.com"))
}

@Test
fun validateEmail_invalidFormat_returnsFalse() {
    // Invalid case
    assertFalse(validateEmail("invalid-email"))
}

@Test
fun validateEmail_emptyString_returnsFalse() {
    // Edge case
    assertFalse(validateEmail(""))
}
```

#### Running Unit Tests

```bash
# All unit tests
./gradlew test

# Specific test class
./gradlew test --tests UserRepositoryTest

# Generate coverage report
./gradlew testDebugUnitTest
```

Existing unit tests cover the BLE retry/backoff policy (`RetryBackoffTest`), the offline
region size limits (`OfflineRegionSizeTest`), and the advertising failure mappings
(`BleAdvertisingFailTest`).

### Integration Tests

#### Purpose
- Test interaction between components
- Verify service contracts
- Validate API communication

#### What to Test

**Do Test:**
- API communication (with mocked responses)
- Firebase messaging to Android interaction
- Data serialization/deserialization across boundaries
- Database operations

#### Mocking Strategy

Mock external services and data:

```kotlin
@Test
fun authenticate_validCredentials_returnsToken() {
    // Mock Firebase authentication
    val mockFirebase = mockk<FirebaseAuth>()
    every { mockFirebase.signInWithEmailAndPassword(any(), any()) } returns Task.forResult(mockk<AuthResult>())
    
    val authService = AuthService(mockFirebase)
    val result = authService.authenticate("user@example.com", "password")
    
    assertTrue(result is Success)
}
```

#### Running Integration Tests

```bash
# Instrumented tests on device/emulator
./gradlew connectedAndroidTest

# Specific test
./gradlew connectedAndroidTest --tests com.p56.safereach.AuthIntegrationTest
```

### E2E (End-to-End) Testing

#### Purpose
- Full workflow testing from UI to database
- Conducted manually by team members
- Catch UI issues and hard-to-find bugs

#### What to Test

- Complete user journeys
- Feature workflows
- Cross-feature interactions
- Real device behavior

#### Process

1. **Test Plan Creation:** Define user scenarios
2. **Manual Testing:** Execute on real device/emulator
3. **Bug Documentation:** Report issues with steps to reproduce
4. **Validation:** Verify user stories are met

#### Test Checklist Example

```
Feature: User Emergency Alert
- [ ] User receives notification on lock screen
- [ ] Notification is actionable (tap to open)
- [ ] Alert details display correctly
- [ ] Can dismiss alert
- [ ] Can share alert with contacts
- [ ] Works on different Android versions (29, 30, 35, 36)
```

---

## CI/CD Pipeline

### Overview

GitHub Actions automates build, test, lint, and artifact export. Two workflows live in
`.github/workflows`:

- **`development-check.yml`** ("Dev Branch Pull Request Check") runs on pushes and pull
  requests to `development`. It installs JDK 17, materializes
  `app/src/main/res/raw/public_key.pem` from the `PUBLIC_KEY` secret, then runs
  `./gradlew lint`, `./gradlew build`, and `./gradlew test`.
- **`build.yml`** ("Test and Build main branch") runs on pushes to `main`. It performs the
  same lint/build/test steps, then `./gradlew assembleRelease` and uploads the release APK
  as the `SafeReach-release-apk` artifact.

Both workflows ignore changes to documentation, `functions/**`, `firebase.json`, and
`.firebaserc`, so backend-only changes do not trigger the Android pipeline.

### Failure Handling

| Status          | Action                                |
|-----------------|---------------------------------------|
| **Lint Fails**  | PR blocked, fix and push again        |
| **Tests Fail**  | PR blocked, fix tests and push        |
| **Build Fails** | PR blocked, debug build configuration |
| **All Pass**    | PR ready for review and merge         |

### Branch-Specific Rules

#### Development Branch
```yaml
- Requires: Linting 
- Requires: Unit Tests 
- Requires: Integration Tests 
- Requires: 1 Approval 
```

#### Main Branch
```yaml
- Requires: All Development checks 
- Requires: 2 Approvals 
- Requires: Build Success 
```

### Workflow Commands

```bash
# View GitHub Actions locally (optional)
act -j build

# Check workflow status
gh workflow list
gh run list
```

---

## Cloud Functions (Backend)

The `functions/` directory is a TypeScript Firebase Cloud Functions codebase (Node 24) that
owns GeoNet ingestion, alert authoring, signing, and the `alertsApi` endpoint (see
[ARCHITECTURE.md](./ARCHITECTURE.md#cloud-backend)).

### Prerequisites

- Node.js 24
- The Firebase CLI (`npm install -g firebase-tools`)
- A `SIGNING_KEY` parameter holding the Ed25519 private key, set with
  `firebase functions:secrets:set SIGNING_KEY` (or as an environment parameter)

### Common commands

```bash
# Install dependencies
npm --prefix functions install

# Regenerate the protobuf-ts types from protobuf/models.proto after changing it
npm --prefix functions run proto:generate

# Type-check / compile to lib/
npm --prefix functions run build

# Lint
npm --prefix functions run lint

# Run the emulator suite
npm --prefix functions run serve

# Deploy the functions
npm --prefix functions run deploy
```

Guard the wire contract: any change to `protobuf/models.proto` must be regenerated for both
Kotlin (the Gradle `protobuf` task) and TypeScript (`proto:generate`), and must keep the same
field numbers so mixed-version devices and the backend stay compatible.

---

## Definition of Done

A feature is **only considered complete** when ALL the following criteria are met:

### Development Phase
- **Code Implemented** - Feature fully developed and functional
- **Code Formatted** - Ran "Reformat Code" before committing
- **Meaningful Commits** - Small commits with clear messages

### Testing Phase
- **Unit Tests Written** - All logic covered with unit tests
- **Integration Tests** - Created if API/service integration required
- **E2E Tests Passed** - Manual testing completed and validated
- **Edge Cases Handled** - Tested boundary conditions

### Quality Assurance
- **Linting Passes** - No style or formatting issues
- **Build Passes** - APK builds successfully
- **No Test Failures** - All automated tests pass
- **No Warnings** - Build has no critical warnings

### Code Review & Approval
- **Peer Review Approved** - At least 2 developers reviewed (main branch)
- **CI/CD Pipeline Passes** - All automated checks successful
- **PR Merged** - Code merged to appropriate branch

### Deployment
- **Staging Verification** - Tested in staging environment
- **Production Ready** - Feature ready for user release

### Documentation
- **Code Documented** - KDoc/TSDoc comments added
- **Docs Updated** - README/ARCHITECTURE/DEVELOPMENT updated when behavior or architecture changes

---

## Quick Reference Checklist

### Before Creating PR

- [ ] Code is formatted (Ctrl+Alt+L or Cmd+Option+L)
- [ ] Linting passes (`./gradlew lint`; ktlint once it is configured)
- [ ] Unit tests pass (`./gradlew test`)
- [ ] Integration tests pass (`./gradlew connectedAndroidTest`)
- [ ] Commits follow conventional commit format
- [ ] Branch is up-to-date with base branch

### Before Merging to Main

- [ ] PR has 2+ approvals
- [ ] All CI/CD checks pass
- [ ] E2E testing completed
- [ ] No merge conflicts
- [ ] Branch will be deleted after merge

### After Merge

- [ ] Verify deployment
- [ ] Delete feature branch
- [ ] Update issue status
- [ ] Notify team if urgent hotfix

---

## Resources & Tools

### Kotlin Linting & Formatting
- [KLint Official Docs](https://ktlint.github.io/)
- [Kotlin Style Guide](https://kotlinlang.org/docs/coding-conventions.html)

### TypeScript Linting & Formatting
- [Prettier Documentation](https://prettier.io/)
- [ESLint Documentation](https://eslint.org/)

### Testing Frameworks
- [JUnit 4 Documentation](https://junit.org/junit4/)
- [Espresso Testing Guide](https://developer.android.com/training/testing/espresso)
- [Compose UI Testing](https://developer.android.com/jetpack/compose/testing)

### Git & GitHub
- [Conventional Commits](https://www.conventionalcommits.org/)
- [GitHub Flow Guide](https://guides.github.com/introduction/flow/)

### Security
- [OWASP Top 10 Mobile](https://owasp.org/www-project-mobile-top-10/)
- [Android Security Best Practices](https://developer.android.com/training/articles/security-tips)


