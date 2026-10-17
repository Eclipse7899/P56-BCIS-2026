# SafeReach architecture

This document describes the architecture that is implemented in this repository, with
special attention to the local mesh. It is written as a map for developers and AI agents:
start with the diagrams, then use the file map and invariants when changing code.

## System scope

SafeReach is an Android application for receiving and displaying disaster alerts. It has
three alert ingress paths:

* **Firebase Cloud Messaging (FCM):** an Internet-connected push path for newly signed
  alerts.
* **Alerts HTTP API:** an Internet-connected pull path that bootstraps the local database on
  launch from the `alertsApi` Cloud Function.
* **Bluetooth Low Energy (BLE):** the local, store-and-forward mesh path between nearby
  Android devices.

The application is Kotlin/Compose, uses Hilt for injection, and supports Android API 29+.
The mesh is currently **BLE-only in production code**: no Wi-Fi transport, routing
protocol, or Wi-Fi failover is implemented.

## Cloud backend

The `functions/` package is a TypeScript Firebase Cloud Functions codebase (Node 24) that
owns alert authoring and delivery:

* `fetchGeonetData` runs every 15 minutes (Pacific/Auckland) and pulls earthquake events
  from the GeoNet API into the `geonet` collection.
* `onGeonetQuakeCreated` converts a GeoNet event into an `alerts` document (an Earthquake
  Warning, severity derived from magnitude, a circular shape, and a one-hour expiry).
* `onAlertCreated` reads the `SIGNING_KEY` secret, signs the alert's `MessageDataDto` with
  Ed25519, and broadcasts the base64 `SignedMessageDto` over FCM to the `alerts` topic.
* `alertsApi` is an HTTPS `onRequest` endpoint that returns recent alerts from the `alerts`
  collection as JSON; the app polls it on launch through `AlertSyncManager`.

The proto field numbers here are the wire contract the Android clients decode (see the wire
format section), so changes must stay in sync across `protobuf/models.proto`, the generated
Kotlin types, and `functions/src/models.ts`.

Adding a further alert source (a new ingestion + mapping pair that writes into `alerts`) is
described step by step in [functions/INTEGRATIONS.md](./functions/INTEGRATIONS.md).

## Runtime topology

```text
                             Internet
                         +-------+--------+
                         |                |
                 Firebase Cloud     Alerts HTTP API
                   Messaging      (alertsApi function)
                         |                |
              FirebaseAlert   AlertSyncManager
             MessagingService  (runs on app launch)
                         |                |
                         +-------+--------+
                                 |
                                 v
                     AlertRepository (StateFlow + Room)
                                |
              +-----------------+-----------------+
              |                                   |
              v                                   v
       Compose UI / Mapbox             MeshForegroundService (foreground)
                                                  |
                                          BleOrchestrator
                                   +--------------+--------------+
                                   |                             |
                            BleScanController          BleAdvertisingController
                                   |                             |
                          BleBeaconScanAdapter          BleBeaconAdvAdapter
                                   |                             |
                  Android BLE scan PendingIntent     Android BLE advertising set
                                   |                             |
                        BleScanReceiver      <---- BLE service-data broadcast ---->
                                   |
                          BleScanResultQueue
                                   |
                        MeshForegroundService
```

Every device runs the same software. A device with a current alert scans and advertises
at the same time; a device without an alert scans only. There is no central mesh server,
peer connection, socket, GATT session, or explicit route table.

## Application layers

```text
Android entry points
  SafeReachApplication, MainActivity, FirebaseAlertMessagingService,
  MeshForegroundService, BleScanReceiver
        |
Domain and wire models
  Alert, Shape, SignedMessage, ReceivedMessage(AlertBatch), proto *Dto
        |
State boundary
  AlertRepository, MeshStatusStore
        |
Transport coordination
  BleOrchestrator
        |
Transport state machines and Android adapters
  BleScanController      -> BleBeaconScanAdapter
  BleAdvertisingController -> BleBeaconAdvAdapter
        |
Android Bluetooth LE APIs
```

The Compose UI does not call Bluetooth APIs directly. It observes `AlertRepository` and
`MeshStatusStore` through `DebugViewModel`, `HomeViewModel`, `AlertHistoryViewModel`, and
`SafeReachAppViewModel`.
`MainActivity` is responsible for requesting runtime permissions and starting
`MeshForegroundService`; the service owns the mesh for its lifetime.

## Android entry points and lifecycle

1. `SafeReachApplication` (`App.kt`) is the `@HiltAndroidApp` application class. Its
   `onCreate` initializes preferences, then launches `AlertSyncManager.sync()` on the
   application scope to refresh the local alerts from the alerts HTTP API.
2. `MainActivity.onCreate` calls `ensureBleServiceRunning`.
3. `BlePermissions` determines the API-specific Bluetooth, location, and notification
   permissions. If they are granted, the activity starts `MeshForegroundService` as a
   foreground service; otherwise it requests them via `requiredRuntimePermissions()`.
4. `MainActivity` renders `MainScreen` (the authentication gate), which switches to
   `SafeReachApp` once the user is signed in.
5. `MeshForegroundService.onCreate` creates one `BleAdvertisingController`, one
   `BleScanController`, and one `BleOrchestrator` on a supervisor-backed background
   scope. It registers `BleStateReceiver`, seeds both controllers with the current
   Bluetooth state, and starts collecting their state flows into `MeshStatusStore`.
6. `onStartCommand` requests `BleIntent.Passive` and calls `startForeground`. The service
   returns `START_STICKY`, so Android may recreate it after process pressure.
7. The service observes `AlertRepository.receivedMessage`:
   * `null` means passive mode: scan, do not advertise.
   * a message means active mode: scan and advertise the encoded signed message.
8. `onDestroy` marks runtime state stopped and requests `BleIntent.Off`.

The manifest declares `MeshForegroundService` as a non-exported foreground service of type
`connectedDevice`. The ongoing low-importance notification communicates whether the mesh
is active, passive, or failed.

## State and persistence

`AlertRepository` (`service/AlertRepository.kt`) is the single source of truth for alerts.
It is a plain `@Singleton` held by Hilt (not an Android `Service`) and exposes:

* `receivedMessage: StateFlow<ReceivedMessage?>` — the current stored signed message,
  rebuilt from the latest row of the `signed_messages` table.
* `activeAlerts: StateFlow<List<Alert>>` — the currently active alerts shown on the map,
  rebuilt from the `alerts` table and filtered by `Alert.isActive()`.
* `allAlerts: StateFlow<List<Alert>>` — every stored alert, used by the alert history.

`addSigned(signedMessage)` is the single ingestion entry point for signed messages: it
verifies the Ed25519 signature, upserts each contained alert (if newer), and stores the
signed message. `clearSigned()` clears the stored message. Both FCM and BLE ingress go
through `addSigned`, so both paths are signature-checked.

Important exception: for debug convenience, `addSigned` **skips signature verification when
`BuildConfig.DEBUG` is true** (`AlertRepository.kt`). Debug builds therefore accept forged
signed messages and must not be treated as a trustworthy verifier; release builds always
enforce verification.

`syncAlerts(alerts)` applies unsigned alerts pulled from the alerts HTTP API. Because the API
returns documents from our own Cloud Function over HTTPS, this path does not run the Ed25519
verification that `addSigned` performs; it still applies version-based upserts and notifies
for newly active alerts. Whenever `addAlert` inserts or updates an alert that is active,
`AlertRepository` asks `AlertNotifier` to post the system notification (subject to the
in-app notification preference).

`MeshStatusStore` (`service/MeshStatusStore.kt`) is a plain `@Singleton` runtime status
holder (not an Android `Service`) with `isServiceRunning`, `bleOrchestratorState:
AggregateBleState`, `advertiserState: BleAdvertisingState`, and `scannerState: BleScanState`.

Persistence is Room via `AppDatabase` (`data/`):

* `alerts` — `AlertEntity`, with `Shape` serialized through `ShapeTypeConverters`.
* `signed_messages` — `SignedMessageEntity`.
* Access is through `AlertDao` and `SignedMessageDao`; `AppDatabase` is provided by
  `DatabaseModule` (Hilt). The database is currently version 5 and uses destructive
  migration.

Protobuf wire types live in the generated `com.p56.safereach.proto` package (suffixed
`*Dto`); domain models in `com.p56.safereach.model` are unsuffixed. Mapping is explicit in
`model/ProtoMappers.kt` (proto ↔ domain: `toDomain()` / `toDto()`) and `data/AlertEntity.kt`
+ `data/SignedMessageEntity.kt` (entity ↔ domain: `toDomain()` / `toEntity()`).

## Orchestrator and state machines

`BleOrchestrator` is a desired-state reconciler. It does not perform Bluetooth I/O. It
combines the desired `BleIntent` with the scanner and advertiser states, derives a public
`AggregateBleState`, and sends only the intents needed to converge the controllers.

```text
BleIntent.Off
  scanner Scanning -> Stop
  advertiser Advertising -> Stop
  resulting state: Off when both are Idle

BleIntent.Passive
  scanner Idle -> Start
  advertiser Advertising -> Stop
  resulting state: Passive when scanner is Scanning and advertiser is Idle

BleIntent.Active(payload)
  scanner Idle -> Start
  advertiser Idle/Failed -> Start(payload)
  advertiser Advertising -> Start(payload) (replaces the advertising data)
  resulting state: Active(payload) when both are running
```

A child that cannot reach its desired state surfaces as `PartialFail` (one child) or
`CompleteFail` (both children), while transitional or inconsistent child states surface as
`Unknown`. A Bluetooth state broadcast (`BleStateReceiver`) feeds the current Bluetooth
state to the controllers, which only start when Bluetooth is enabled and otherwise stay
`Idle`. Controller intents and callbacks are serialized through coroutine channels.
Advertising operations are additionally serialized with a `Mutex` and bounded by
`Constants.BLE_ADVERTISING_TIMEOUT` (2 seconds).

### Scan path

`BleBeaconScanAdapter` starts a filtered BLE scan using:

* the first SafeReach service UUID
  `d5013063-3e8b-44f9-b051-040d01d97ac4`;
* non-legacy scanning on all supported LE PHYs, non-legacy scan mode `BALANCED`, match
  mode `AGGRESSIVE`, no report delay;
* a `PendingIntent` delivered to `BleScanReceiver` with action
  `Constants.BLE_SCAN_INTENT_ACTION`.

`BleScanReceiver` reassembles the payload by concatenating the service data of all six
SafeReach UUIDs in order, records the current time and Bluetooth device address, and puts a
`BleScanResult(receivedAt, receivedFrom, payload)` on the process-wide
`BleScanResultQueue.channel`. The scanner controller does not process packet contents;
`MeshForegroundService` is the consumer and validation boundary.

### Advertisement path

`BleBeaconAdvAdapter` uses a non-connectable, non-scannable, non-legacy extended
advertising set. It advertises the first SafeReach service UUID and places the payload in
the service-data fields. The primary and secondary PHY are both coded LE, the interval is
medium, and transmit power is high; the device name and transmit-power level are not
included.

The payload is split into `Constants.MAX_DATA_PAYLOAD` (220-byte) fragments, one per
SafeReach service-data slot. A payload that needs more fragments than there are
`Constants.SERVICE_UUIDS` (6) fails with `BleAdvertisingFail.DataTooLarge`. Advertising is
payload-replaceable: when the alert changes, the existing advertising set is updated
instead of starting another set. Platform capability and size failures are reported through
`BleAdvertisingFail` and become an orchestrator failure state.

## Wire format and forwarding

The BLE advertisement carries a ProtoBuf-encoded `SignedMessageDto`. There is no separate
transport envelope in the current implementation.

```text
SignedMessageDto
  data: bytes         // ProtoBuf-encoded MessageDataDto
  signature: bytes    // Ed25519 signature over data

MessageDataDto
  msg: repeated AlertDto   // field 2
  valid_until: uint64      // field 3
  timestamp: uint64        // field 4

AlertDto
  id, timestamp, title, description, type, severity, shape, version, expires_at, invalidated
```

`Alert` contains a timestamp, version, expiry time, invalidation flag, title, description,
type, severity, and geographic shape (circle or polygon). The version identifies the newest
alert; the timestamp is display-only alert metadata. A node removes a non-invalidated alert
when its expiry time is reached. Immediate invalidation is represented by a higher-version
alert with `invalidated = true`, which remains in the message flow long enough to be relayed.

### Outgoing alert

The debug flow currently creates an alert by:

1. wrapping one `Alert` in an `AlertBatch`;
2. ProtoBuf-encoding it (`AlertBatch.toDto()`);
3. wrapping the bytes in a `SignedMessage` with an empty signature and
   `receivedFrom = "Debug"` (a debug build holds no private key, and `addSigned` bypasses
   verification when `BuildConfig.DEBUG` is true);
4. storing it in `AlertRepository` (`addSigned`);
5. causing `MeshForegroundService` to encode the `SignedMessageDto` and send it to
   `BleOrchestrator` as `BleIntent.Active`.

Authentic alerts are signed on the backend instead: `onAlertCreated` signs the encoded
`MessageDataDto` and pushes the `SignedMessageDto` over FCM, which stores it through the
same repository-to-advertisement path.

### Incoming and forwarding path

```text
BLE advertisement
  -> BleScanReceiver
  -> BleScanResultQueue
  -> MeshForegroundService
  -> ProtoBuf decode SignedMessageDto
  -> Ed25519 verification of SignedMessage.payload (in AlertRepository.addSigned)
  -> ProtoBuf decode MessageDataDto
  -> upsert each Alert if newer
  -> update AlertRepository
  -> repository observer re-advertises the stored message
```

Invalid or unverifiable packets are logged and discarded. A received alert replaces a
stored alert only when its version is newer; older or duplicate packets do not become the
active message. Because the repository stores one current message (and its alerts), this is
a latest-message relay rather than a queue of independent messages.

## Security and trust boundaries

`SigningManager` loads a bundled public Ed25519 key from `res/raw/public_key.pem` and uses it
to verify incoming messages. Release builds cannot originate newly signed alerts. (The
`Signing` helper still contains a private-key parser, but it is not currently wired into the
app; locally authored debug alerts are created without a real signature and rely on the
debug verification bypass described above.)

Authentic alerts are signed server-side: the `onAlertCreated` Cloud Function signs each
alert with the private key supplied through the `SIGNING_KEY` secret before broadcasting it,
and the app verifies that signature with the matching bundled public key.

For the FCM and BLE ingress paths the trust boundary is `AlertRepository.addSigned`: in
release builds it verifies the signature before decoding and applying alerts. Debug builds
skip that verification (see `addSigned` above), so a debug build is not a trustworthy
verifier. Bluetooth addresses are metadata only and are not an identity or authentication
mechanism. BLE transport is broadcast and non-connectable; confidentiality and sender
identity are not provided by the transport.

The alerts HTTP API is a separate pull path. Its responses are accepted without a signature
because they arrive over HTTPS from the `alertsApi` Cloud Function, which reads the trusted
`alerts` Firestore collection. `AlertRepository.syncAlerts` therefore bypasses the Ed25519
check used by `addSigned`; it is the trust boundary to revisit if that assumption changes.

## Permissions and operating constraints

Required permissions vary by Android version:

* Android 12+: `BLUETOOTH_SCAN`, `BLUETOOTH_CONNECT`, and `BLUETOOTH_ADVERTISE`.
* Older Android versions: legacy Bluetooth permissions plus location permissions.
* Android 13+: `POST_NOTIFICATIONS` for the foreground-service notification.
* Foreground service and connected-device service declarations are in
  `AndroidManifest.xml`.

The user must keep Bluetooth enabled. Android scan behavior also depends on device
location settings and battery optimization, so the manifest also declares
`REQUEST_IGNORE_BATTERY_OPTIMIZATIONS`. The UI exposes notification, connected-device,
location-permission, battery-optimization, Bluetooth-disabled, and Bluetooth-incompatibility
banners, and re-checks the notification and permission banners whenever the app resumes.

## UI data flow

```text
AlertRepository.receivedMessage / activeAlerts
  -> DebugViewModel.uiState
  -> DebugScreen

MeshStatusStore
  -> DebugViewModel.uiState
  -> DebugScreen
  -> SafeReachApp (Bluetooth-disabled and Bluetooth-incompatibility banners)

AlertRepository.activeAlerts
  -> HomeViewModel
  -> HomeScreen
  -> Mapbox map and alert shape rendering

AlertRepository.allAlerts
  -> AlertHistoryViewModel
  -> AlertHistoryScreen

AlertRepository.addAlert(new active alert)
  -> AlertNotifier
  -> system notification

MeshStatusStore.bleOrchestratorState
  -> SafeReachAppViewModel
  -> SafeReachApp
```

Repositories and stores are process-local `StateFlow` singletons owned by Hilt: each one is
a `@Singleton` class with an `@Inject` constructor, so services, activities, and view models
receive the same instance through constructor or field injection instead of reaching into a
static object. Alerts are backed by Room and survive process recreation; mesh status is
runtime-only. Firebase authentication, Mapbox rendering, and navigation are application
concerns around the mesh, not mesh transport components.

## Source map

| Responsibility                               | Main files                                                                                           |
|----------------------------------------------|------------------------------------------------------------------------------------------------------|
| Android startup and permissions              | `MainActivity.kt`, `App.kt`, `mesh/ble/BlePermissions.kt`, `AndroidManifest.xml`                     |
| Mesh service lifecycle and packet processing | `android/MeshForegroundService.kt`                                                                   |
| Desired-state coordination                   | `mesh/ble/BleOrchestrator.kt`, `mesh/ble/fsm/*`                                                      |
| BLE scanning                                 | `mesh/ble/scanning/BleScanController.kt`, `BleBeaconScanAdapter.kt`, `BleScanReceiver.kt`            |
| BLE advertising                              | `mesh/ble/advertisement/BleAdvertisingController.kt`, `BleBeaconAdvAdapter.kt`                       |
| Cross-component handoff                      | `mesh/BleScanResultQueue.kt`                                                                         |
| Alert and mesh runtime state                 | `service/AlertRepository.kt`, `service/MeshStatusStore.kt`                                           |
| Persistence (Room)                           | `data/AppDatabase.kt`, `AlertDao.kt`, `SignedMessageDao.kt`, `DatabaseModule.kt`                     |
| Domain and wire mapping                      | `model/*.kt`, `model/ProtoMappers.kt`, `data/AlertEntity.kt`, `data/SignedMessageEntity.kt`          |
| Constants                                    | `Constants.kt`                                                                                       |
| Signing and verification                     | `verification/Signing.kt`, `verification/SigningManager.kt`                                          |
| Cloud ingress (push)                         | `android/FirebaseAlertMessagingService.kt`                                                           |
| Cloud ingress (pull)                         | `data/remote/AlertApiClient.kt`, `service/AlertSyncManager.kt`                                       |
| Notifications                                | `android/AlertNotifier.kt`, `ui/profile/NotificationPreferences.kt`                                  |
| Authentication                               | `ui/auth/MainScreen.kt`, `SignInScreen.kt`, `RegisterScreen.kt`, `ForgotPasswordScreen.kt`           |
| Alert history                                | `ui/alert/AlertHistoryScreen.kt`, `ui/alert/AlertHistoryViewModel.kt`                                |
| User preferences                             | `ui/theme/ThemePreferences.kt`, `ui/home/MapCameraPreferences.kt`                                    |
| Offline maps                                 | `service/OfflineRegionManager.kt`, `map/OfflineRegionViewModel.kt`, `map/OfflineRegionPanel.kt`      |
| Retry policy                                 | `mesh/ble/RetryBackoff.kt`                                                                           |
| Cloud functions (backend)                    | `functions/src/index.ts`, `firebase/alerts-api.ts`, `firebase/on-alert-created.ts`, `integrations/*` |
| UI shell and navigation                      | `auth/MainScreen.kt`, `navigation/SafeReachApp.kt`                                                   |
| Mesh diagnostics                             | `debug/DebugViewModel.kt`, `debug/DebugScreen.kt`                                                    |

## Invariants for future changes

1. Keep BLE API calls inside the adapters and keep state transitions inside the controllers.
2. Send orchestrator commands rather than manipulating controllers from UI code.
3. Preserve the SafeReach service UUIDs, the fragment-slot ordering, and ProtoBuf field
   numbers when changing the wire format; changing them breaks interoperability with
   existing devices.
4. Verify signatures before accepting locally broadcast data.
5. Preserve version-based latest-alert behavior and expiry/invalidation semantics when
   adding transports or storage.
6. Any new transport should feed the same validated message path (`AlertRepository.addSigned`)
   rather than duplicating alert application logic. The alerts HTTP API is a deliberate
   exception: it is a trusted HTTPS pull path applied through `AlertRepository.syncAlerts`.

## Known limitations and extension points

The current implementation is intentionally small and has important boundaries:

* Wi-Fi transport is not implemented.
* There is no explicit hop count, packet expiry, message identifier, or deduplication set.
  Version comparison limits repeated updates but is not a complete routing protocol.
* BLE payload size is constrained by Android advertising capabilities and the six
  service-data fragment slots.
* Polygon-shaped alerts are stored and listed but are not drawn on the Home map; only
  circles are rendered there.
* Alerts synced from the HTTP API do not populate `AlertRepository.receivedMessage`, so they
  are shown on the map but are not re-advertised over the BLE mesh.
* Release builds can verify messages but cannot originate newly signed debug alerts.

These are architecture decisions or gaps, not assumptions to hide in a new transport.
Future mesh work should define packet identity, hop/TTL policy, expiry, deduplication,
transport selection, and trust/key distribution before adding multi-transport forwarding.
