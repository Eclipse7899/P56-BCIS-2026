# SafeReach architecture

This document describes the architecture that is implemented in this repository, with
special attention to the local mesh. It is written as a map for developers and AI agents:
start with the diagrams, then use the file map and invariants when changing code.

## System scope

SafeReach is an Android application for receiving and displaying disaster alerts. It has
two alert ingress paths:

* **Firebase Cloud Messaging (FCM):** an Internet-connected fallback or origin path.
* **Bluetooth Low Energy (BLE):** the local, store-and-forward mesh path between nearby
  Android devices.

The application is Kotlin/Compose, uses Hilt for injection, and supports Android API 29+.
The mesh is currently **BLE-only in production code**. `mesh/wifi/WifiCoordinator.kt` is
an empty placeholder; no Wi-Fi transport, routing protocol, or Wi-Fi failover is
implemented.

## Runtime topology

```text
                             Internet
                                |
                         Firebase Cloud Messaging
                                |
                    FirebaseAlertService (Android service)
                                |
                                v
                   AlertStateRepository (StateFlow)
                                |
              +-----------------+-----------------+
              |                                   |
              v                                   v
       Compose UI / Mapbox                 MeshService (foreground)
                                                  |
                                          BleOrchestrator
                                   +--------------+--------------+
                                   |                             |
                            BleScanController             BleAdvController
                                   |                             |
                           BleBeaconScanAdapter          BleBeaconAdvAdapter
                                   |                             |
                  Android BLE scan PendingIntent       Android BLE advertising set
                                   |                             |
                        BleScanReceiver       <---- BLE service-data broadcast ---->
                                   |
                            BleResultQueue
                                   |
                            MeshService
```

Every device runs the same software. A device with a current alert scans and advertises
at the same time; a device without an alert scans only. There is no central mesh server,
peer connection, socket, GATT session, or explicit route table.

## Application layers

```text
Android entry points
  App, MainActivity, FirebaseAlertService, MeshService, BleScanReceiver
        |
Domain and wire models
  Alert, Shape, MeshPacket<T>, SignedMessage, MeshMeta<T>
        |
State boundary
  AlertStateRepository, MeshStatusRepository
        |
Transport coordination
  BleOrchestrator
        |
Transport state machines and Android adapters
  BleScanController -> BleBeaconScanAdapter
  BleAdvController  -> BleBeaconAdvAdapter
        |
Android Bluetooth LE APIs
```

The Compose UI does not call Bluetooth APIs directly. It observes repositories through
`DebugViewModel` and `SafeReachApp`. `MainActivity` is responsible for requesting runtime
permissions and starting `MeshService`; the service owns the mesh for its lifetime.

## Mesh lifecycle

1. `MainActivity.onCreate` calls `ensureBleServiceRunning`.
2. `BlePermissions` determines the API-specific Bluetooth, location, and notification
   permissions. If they are granted, the activity starts `MeshService` as a foreground
   service; otherwise it requests them.
3. `MeshService.onCreate` creates one `BleAdvController`, one `BleScanController`, and one
   `BleOrchestrator` on a supervisor-backed background scope.
4. The service registers `BleStateReceiver`, seeds both controllers with the current Bluetooth
   enabled state, and starts collecting their state flows.
5. `onStartCommand` requests `BleIntent.Passive` and calls `startForeground`. The service
   returns `START_STICKY`, so Android may recreate it after process pressure.
6. The service observes `AlertStateRepository.meshMeta`:
   * `null` means passive mode: scan, do not advertise.
   * a signed message means active mode: scan and advertise the encoded packet.
7. `onDestroy` marks repository state as stopped and requests `BleIntent.Off`.

The manifest declares `MeshService` as a non-exported foreground service of type
`connectedDevice`. The ongoing low-importance notification communicates whether the mesh
is active, passive, disabled, or failed.

## Orchestrator and state machines

`BleOrchestrator` is a desired-state reconciler. It does not perform Bluetooth I/O. It
combines the desired `BleIntent` with the scanner and advertiser states, derives a public
`BleState`, and sends only the commands needed to converge the controllers.

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
  advertiser Idle -> Start(payload)
  advertiser Advertising + changed payload -> update(payload)
  resulting state: Active(payload) when both are running
```

Any disabled child produces `BleState.Disabled`; any failed child produces
`BleState.Failed`. A Bluetooth state broadcast moves both controllers to `Disabled` when
Bluetooth is off and allows them to return to `Idle` when it is on. Controller commands and
callbacks are serialized through coroutine channels. Advertising operations are additionally
serialized with a `Mutex` and bounded by `BLE_ADVERTISING_TIMEOUT` (2 seconds).

### Scan path

`BleBeaconScanAdapter` starts a filtered BLE scan using:

* the SafeReach service UUID
  `d5013063-3e8b-44f9-b051-040d01d97ac4`;
* non-legacy scanning on all supported LE PHYs;
* balanced scan mode and aggressive matching;
* a `PendingIntent` delivered to `BleScanReceiver`.

`BleScanReceiver` extracts service data for the SafeReach UUID, records the current time
and Bluetooth device address, and puts a `BleScanResult` on the process-wide
`BleResultQueue`. the scanner controller does not process packet contents; `MeshService` is
the consumer and validation boundary.

### Advertisement path

`BleBeaconAdvAdapter` uses a non-connectable extended advertising set. It advertises the
same SafeReach service UUID and places the payload in service data. The primary PHY is
coded LE, the secondary PHY is LE 1M, the interval is high, and transmit power is high.
The device name and transmit power are not included.

Advertising is payload-replaceable: when an alert changes, the existing advertising set is
updated instead of starting another set. Payload-size and platform capability failures are
reported through `BleAdvFail` and become an orchestrator failure state.

## Wire format and forwarding

The BLE service-data bytes have this logical structure:

```text
MeshPacket<SignedMessage>
  hopCount: Long
  data: SignedMessage
    data: ByteArray       // ProtoBuf-encoded Alert
    signature: ByteArray  // Ed25519 signature over data
```

`Alert` contains a timestamp, version, expiry time, invalidation flag, title, description,
type, severity, and geographic shape (circle or polygon). The version identifies the newest
alert; the timestamp is display-only alert metadata. A node removes a non-invalidated alert
when its expiry time is reached. Immediate invalidation is represented by a higher-version
alert with `invalidated = true`, which remains in the message flow long enough to be relayed.
`MeshMeta` is local metadata and is not transmitted; it records when the device received the
packet, the source Bluetooth address, the hop count, and the signed payload.

### Outgoing alert

The debug flow currently creates an alert by:

1. ProtoBuf-encoding `Alert`;
2. signing those bytes with `SigningManager`;
3. storing `MeshMeta(hopCount = 0, receivedFrom = null, ...)` in
   `AlertStateRepository`;
4. causing `MeshService` to encode `MeshPacket(hopCount = 1, data = signedMessage)`;
5. sending those bytes to `BleOrchestrator` as `BleIntent.Active`.

FCM also stores a received signed message with hop count 0, so it enters the same
repository-to-advertisement path.

### Incoming and forwarding path

```text
BLE advertisement
  -> BleScanReceiver
  -> BleResultQueue
  -> MeshService
  -> ProtoBuf decode MeshPacket<SignedMessage>
  -> Ed25519 verification of SignedMessage.data
  -> ProtoBuf decode Alert
  -> compare Alert.version with current alert
  -> update AlertStateRepository if newer
  -> repository observer increments hopCount and re-advertises
```

Invalid or unverifiable BLE packets are logged and discarded. A received alert replaces
the current alert only when its version is newer; older or duplicate packets do not trigger
a new advertisement. Because the repository stores only one current alert and one message,
this is a latest-alert relay rather than a queue of independent messages.

Alerts with an expiry time are removed locally when the deadline is reached. This does not
need a new network message because every node receives the same expiry deadline. An immediate
invalidation is different: it is a signed, higher-version alert update and is advertised
through the normal repository message flow so neighboring nodes remove it as well.

## Security and trust boundaries

`SigningManager` loads a bundled public Ed25519 key from `res/raw/public_key.pem`.
Debug builds may also load a locally generated private key from debug resources; release
builds deliberately cannot sign. The Gradle `generateSigningKeys` task creates the pair
and warns that the private key must not be committed.

For BLE ingress, the trust boundary is `MeshService`: it verifies the signature before
decoding and applying the alert. Bluetooth addresses are metadata only and are not an
identity or authentication mechanism. BLE transport is broadcast and non-connectable;
confidentiality and sender identity are not provided by the transport itself.

The current FCM handler decodes the `SignedMessage` and alert and writes them to the
repository, but does not call `SigningManager.verify`. Treat this as a deliberate
implementation gap to resolve before relying on FCM as a trusted alert source.

## Permissions and operating constraints

Required permissions vary by Android version:

* Android 12+: `BLUETOOTH_SCAN`, `BLUETOOTH_CONNECT`, and `BLUETOOTH_ADVERTISE`.
* Older Android versions: legacy Bluetooth permissions plus location permissions.
* Android 13+: `POST_NOTIFICATIONS` for the foreground-service notification.
* Foreground service and connected-device service declarations are in
  `AndroidManifest.xml`.

The user must keep Bluetooth enabled. Android scan behavior also depends on device
location settings and battery optimization. The UI exposes Bluetooth-disabled and battery
optimization banners, but `LocationStateReceiver` currently observes provider changes
without updating mesh state.

## Repository and UI data flow

```text
AlertStateRepository.activeAlerts/meshMeta
  -> DebugViewModel.uiState
  -> DebugScreen

MeshStatusRepository
  -> DebugViewModel.uiState
  -> DebugScreen
  -> SafeReachApp (Bluetooth-disabled banner)

AlertStateRepository.activeAlerts
  -> HomeViewModel
  -> HomeScreen
  -> Mapbox map and alert shape rendering
```

Repositories are process-local `StateFlow` singletons owned by Hilt: each one is a
`@Singleton` class with an `@Inject` constructor, so services, activities, and view models
receive the same instance through constructor or field injection instead of reaching into a
static object. They are not a durable database: alerts and mesh status are lost when the
process is recreated. Firebase authentication, Mapbox rendering, and navigation are
application concerns around the mesh, not mesh transport components.

## Source map

| Responsibility                               | Main files                                                                                |
|----------------------------------------------|-------------------------------------------------------------------------------------------|
| Android startup and permissions              | `MainActivity.kt`, `App.kt`, `mesh/ble/BlePermissions.kt`, `AndroidManifest.xml`          |
| Mesh service lifecycle and packet processing | `mesh/MeshService.kt`                                                                     |
| Desired-state coordination                   | `mesh/ble/BleOrchestrator.kt`, `mesh/ble/fsm/*`                                           |
| BLE scanning                                 | `mesh/ble/scanning/BleScanController.kt`, `BleBeaconScanAdapter.kt`, `BleScanReceiver.kt` |
| BLE advertising                              | `mesh/ble/advertisement/BleAdvController.kt`, `BleBeaconAdvAdapter.kt`                    |
| Cross-component handoff                      | `mesh/BleResultQueue.kt`                                                                  |
| Mesh and alert state                         | `mesh/MeshStatusRepository.kt`, `AlertStateRepository.kt`                                 |
| Wire and domain models                       | `Alert.kt`, `Constants.kt`                                                                |
| Signing and verification                     | `verification/Signing.kt`, `verification/SigningManager.kt`                               |
| Cloud ingress                                | `FirebaseAlertService.kt`                                                                 |
| Mesh diagnostics                             | `debug/DebugViewModel.kt`, `debug/DebugScreen.kt`                                         |
| Planned, not implemented                     | `mesh/wifi/WifiCoordinator.kt`                                                            |

## Invariants for future changes

1. Keep BLE API calls inside the adapters and keep state transitions inside the controllers.
2. Send orchestrator intents rather than manipulating controllers from UI code.
3. Preserve the SafeReach service UUID and ProtoBuf field numbers when changing the wire
   format; changing either breaks interoperability with existing devices.
4. Verify signatures before accepting locally broadcast data.
5. Treat `MeshMeta` as local observation metadata, not part of the wire protocol.
6. Preserve version-based latest-alert behavior and expiry/invalidation semantics when
   adding transports or storage.
7. Any new transport should feed the same validated message path and repository state,
   rather than duplicating alert application logic.

## Known limitations and extension points

The current implementation is intentionally small and has important boundaries:

* Wi-Fi transport is not implemented.
* There is no explicit maximum hop count, packet expiry, message identifier, or
  deduplication set. Timestamp comparison limits repeated updates but is not a complete
  routing protocol.
* There is no durable queue or persistence across process death.
* BLE payload size is constrained by Android advertising capabilities.
* FCM and BLE currently have different signature-validation behavior.
* `LocationStateReceiver` does not currently feed location state into the orchestrator.
* Release builds can verify messages but cannot originate newly signed debug alerts.

These are architecture decisions or gaps, not assumptions to hide in a new transport.
Future mesh work should define packet identity, hop/TTL policy, expiry, deduplication,
transport selection, and trust/key distribution before adding multi-transport forwarding.
