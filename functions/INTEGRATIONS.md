# Adding an alert source to SafeReach

How to add a new upstream source of data to the `functions/` backend so it reaches SafeReach
clients as alerts: one function builds an alert document and writes it into `alerts`. Do not
add a new collection, a per-source schema file, or a separate mapper — that two-stage shape is
what the original GeoNet integration happens to do, and it is not the model to copy.

---

## 1. The pipeline you are plugging into

```text
┌────────────────────────────────────┐
│            [API]                   │
│         Upstream API               │
└─────────────────┬──────────────────┘
                  │
                  ▼
┌────────────────────────────────────┐
│          [FUNCTION]                │
│       fetch<Source>Data            │
│   onSchedule / onRequest           │
│                                    │
│  parse upstream payload            │
│  build the AlertDoc in place       │
└─────────────────┬──────────────────┘
                  │  alertDocSchema.parse
                  │  alertToFirestore
                  ▼
┌────────────────────────────────────┐
│          [DATABASE]                │
│        alerts Collection           │
└─────────────────┬──────────────────┘
                  │
          ┌───────┴────────┐
          │                │
          ▼                ▼
┌───────────────────┐  ┌────────────────────┐
│    [FUNCTION]     │  │       [API]        │
│  onAlertCreated   │  │    alertsApi       │
│                   │  │                    │
│  Signs alerts     │  │ HTTP pull on launch│
│  Sends FCM        │  └─────────┬──────────┘
└─────────┬─────────┘            │
          │                      │
          ▼                      │
┌───────────────────┐            │
│   [MESSAGING]     │            │
│  FCM Topic        │            │
│  "alerts"         │            │
└─────────┬─────────┘            │
          │                      │
          └──────────┬───────────┘
                     │
                     ▼
┌────────────────────────────────────┐
│             [CLIENT]               │
│         Android Clients            │
│                                    │
│  Receives FCM notifications        │
│  Fetches alerts via HTTP           │
└────────────────────────────────────┘
```

An integration only has to produce a valid `alerts` document. Signing, FCM, the pull API and
the clients are source-agnostic.

## 2. Decide the shape of the integration

| Shape             | Use when                                                                     | Trigger             |
| ----------------- | ---------------------------------------------------------------------------- | ------------------- |
| Scheduled poller  | The source is a public REST/GeoJSON feed (GeoNet, MetService, council feeds) | `onSchedule`        |
| Webhook           | The source can push to an HTTPS endpoint you control                         | `onRequest`         |
| Firestore trigger | Documents already land in Firestore from somewhere else                      | `onDocumentCreated` |

All three write `alerts` directly. Only the trigger differs.

---

## 3. Step 1 — decide whether you need a new `AlertType`

`functions/src/models.ts` is generated from `protobuf/models.proto`:

```proto
enum AlertType { EARTHQUAKE = 0; TSUNAMI = 1; FIRE = 2; FLOOD = 3; }
enum Severity  { LOW = 0; MEDIUM = 1; HIGH = 2; }
```

Can the source use one of those four? **Yes** (e.g. a second earthquake feed) → skip to step 2,
no client change at all. **No** (e.g. volcanic activity) → append a value:

```proto
enum AlertType { EARTHQUAKE = 0; TSUNAMI = 1; FIRE = 2; FLOOD = 3; VOLCANIC = 4; }
```

- Never renumber or remove an existing value — it is the contract with installed clients.
  Append only, with an explicit number.
- Regenerate both sides:
  ```bash
  npm --prefix functions run proto:generate   # -> functions/src/models.ts
  ./gradlew :app:build                        # Kotlin protobuf task -> generated *Dto types
  ```
- Add it to the app enum in `app/src/main/java/com/p56/safereach/model/Alert.kt`:
  ```kotlin
  VOLCANIC(4, "Volcanic activity"),
  ```
  Old clients fall back to `AlertType.UNKNOWN` rather than dropping the alert, but that is a
  transitional state, not a reason to skip the app change.

`functions/src/models.ts` is generated — never hand-edit it.

---

## 4. Step 2 — build the alert and write it from one function

New file: `functions/src/integrations/<source>-data.ts`. It does the whole job: fetch,
validate the payload, construct an `AlertDoc`, and `set` it into `alerts`.

```ts
import { onSchedule } from "firebase-functions/v2/scheduler";
import { crc32 } from "zlib";
import { db } from "../db";
import { AlertDoc, alertDocSchema, alertToFirestore } from "../alert-schema";
import { AlertType, Severity } from "../models";

// Stand-in for your API call.
declare function fetchUpstreamVolcanoes(): Promise<{
  features: {
    properties: {
      volcanoID: string;
      volcanoTitle: string;
      level: number;
      activity: string;
    };
    geometry: { coordinates: [number, number] };
  }[];
}>;

// Generate an alert ID
function alertIdFor(sourceId: string): number {
  return crc32(sourceId) >>> 0;
}

function severityForLevel(level: number): Severity {
  if (level >= 3) {
    return Severity.HIGH;
  }
  if (level >= 2) {
    return Severity.MEDIUM;
  }
  return Severity.LOW;
}

export const fetchVolcanoData = onSchedule(
  {
    schedule: "every 30 minutes",
    timeZone: "Pacific/Auckland",
    region: "australia-southeast1",
  },
  async () => {
    // Your API call goes here
    const data = await fetchUpstreamVolcanoes();

    const observedAt = Date.now();
    const alertLifetimeMs = 24 * 60 * 60 * 1000;
    let written = 0;

    for (const feature of data.features) {
      const { volcanoID, volcanoTitle, level, activity } = feature.properties;
      const [longitude, latitude] = feature.geometry.coordinates;
      const docId = `volcano-${volcanoID}-l${level}`;

      try {
        // Populate the alert document
        const alert: AlertDoc = alertDocSchema.parse({
          id: alertIdFor(docId),
          timestamp: observedAt,
          title: `Volcanic Alert Level ${level}`,
          description: `${volcanoTitle}: ${activity}`,
          type: AlertType.VOLCANIC,
          severity: severityForLevel(level),
          latitude,
          longitude,
          radius: level * 5000,
          expiresAt: observedAt + alertLifetimeMs,
          invalidated: false,
        });

        // Insert into the alerts collection
        await db
          .collection("alerts")
          .doc(docId)
          .set(alertToFirestore(alert), { merge: true });
        written++;
      } catch (error) {
        console.error(`Skipping ${volcanoID}: ${String(error)}`);
      }
    }

    console.log(`Wrote ${written} of ${data.features.length} alerts`);
  },
);
```

`fetchUpstreamVolcanoes()` stands in for your source; delete the `declare` once you call a real
API. GeoJSON coordinates are `[longitude, latitude]` — destructure in that order.

Export it from `functions/src/index.ts`:

```ts
export { fetchVolcanoData } from "./integrations/fetch-volcano-data";
```

A function that is not exported there is not deployed, even if it compiles.

### Mandatory details

- `region: "australia-southeast1"` on every function. `setGlobalOptions` in `index.ts` sets
  `maxInstances` only, so omitting it deploys to `us-central1`.
- Write via `alertToFirestore(alertDocSchema.parse(...))`, never a hand-built object. The
  schema enforces the site constraints (`uint32` radius, integer epoch millis) and the mapper
  converts `timestamp` / `expiresAt` into Firestore `Timestamp`s — a plain number there breaks
  `alertsApi`'s `orderBy("timestamp", "desc")`, because Firestore sorts by type before value.
- The document ID is the alert's identity. Namespace it per source (`volcano-...`); GeoNet uses
  bare upstream IDs, so an un-namespaced ID can overwrite a GeoNet alert.
- The numeric `id` is the 32-bit wire identifier clients dedupe on: a CRC32 of the same string
  as the document ID, forced unsigned.
- `AbortSignal.timeout(...)` on the fetch and a `try`/`catch` per record, so one bad record and
  one hung request do not cost you the whole run. Log a count of what was written; it is
  currently the only signal that a feed is healthy.

### Idempotency

`onAlertCreated` is a **create** trigger, so it fires once per document ID and never again:

- Re-writing the same ID with `set(..., {merge: true})` refreshes the stored alert (rolling
  `timestamp` / `expiresAt` forward keeps a persistent hazard active) but does **not**
  re-broadcast.
- A new document ID is a new alert and is broadcast. Put whatever distinguishes one alert from
  the next into the ID — the source record ID for event feeds, `<id>-l<level>` for state feeds.

---

## 5. Step 3 — secrets, parameters and the signing key pair

- Use `defineSecret(...)` for real credentials (stored in Secret Manager, redacted from logs)
  and `defineString(...)` for non-sensitive config, as `on-alert-created.ts` does with
  `SIGNING_KEY`. `disallowLegacyRuntimeConfig: true` is set, so `functions:config:*` is not an
  option.
- Local values go in `functions/.env.disaster-monitoring-2d085`; `.env` and `.env.*` are
  gitignored. Production: `firebase functions:secrets:set <NAME>`.

### The signing key pair must match, or every alert is rejected

`SIGNING_KEY` in the functions and the public key in the app are two halves of **one** Ed25519
keypair. They must match:

| Half    | Lives in                                                          | Set with                                             |
| ------- | ----------------------------------------------------------------- | ---------------------------------------------------- |
| Private | `SIGNING_KEY` param, read by `onAlertCreated`                     | `firebase functions:secrets:set SIGNING_KEY`         |
| Public  | `app/src/main/res/raw/public_key.pem`, loaded by `SigningManager` | commit it; CI writes it from the `PUBLIC_KEY` secret |

Mismatch means the app rejects every alert: `AlertRepository.addSigned` runs
`signingManager.verify(...)`, logs `Failed to verify alert`, and returns — the alert is never
stored and never notified, with nothing shown to the user.

- `addSigned` skips verification entirely when `BuildConfig.DEBUG` is true, so a mismatch works
  fine in debug builds and fails only in release.
- The pull path (`alertsApi`) is unsigned and relies on HTTPS, so alerts still appear after a
  launch sync. The only symptom is that live pushes stop arriving.

So: same keypair on both sides, then confirm with a **release** build that a push actually
arrives — a launch sync proving the data is fine does not prove the signing does.

---

## 6. Step 4 — build, run, deploy, verify

```bash
npm --prefix functions run lint     # ESLint (google config): 2-space indent, double quotes, 80 cols
npm --prefix functions run build    # tsc, strict + noUnusedLocals
npm --prefix functions run deploy   # firebase deploy --only functions
```

`predeploy` runs lint then build, so either failing blocks the deploy. Run
`npx eslint --ext .ts src --fix` first: prettier-style brace spacing (`import { onSchedule }`)
fails the lint rule.

Use `emulators:start --only functions,firestore` for a local run — the `serve` script starts
functions only, which is not enough for a function that writes to Firestore.

Verify, in order:

1. The function writes the expected number of `alerts` documents, with `timestamp` and
   `expiresAt` stored as Firestore timestamps (not numbers or strings).
2. `onAlertCreated` logs `Broadcast notification for alert <id>` per new document.
3. A **release** build receives the FCM push, and `GET /alerts?count=5` returns the same alert.
4. Re-running the poller does not broadcast a second time for the same source state.

---

## 7. Traps in this codebase

1. **`onAlertCreated` never re-fires for an existing alert.** It is a create trigger, so
   correcting an alert (flipping `invalidated`, fixing a description) reaches clients only
   through the pull path on next launch. If a correction must be pushed, write a new document
   with a new ID, or add an `onDocumentWritten` trigger.
2. **A malformed alert document fails silently.** `alertFromFirestore` throws on a missing,
   mistyped or extra field (`z.strictObject`) and that exception kills the broadcast, while the
   document sits in `alerts` looking fine and shows up in the pull API. Always
   `alertDocSchema.parse` before `set`, and log the failure.
3. **Schema changes need a backfill.** A new required field in `alertDocSchema` makes every
   existing alert document fail to parse on the next broadcast. Make it `.optional()`, or
   backfill first.
4. **`crc32` returns a signed int** and can collide across sources. Force unsigned with
   `>>> 0` and include the source name in the hashed string.
5. **Region is per-function, `database` is per-trigger.** Every function needs
   `region: "australia-southeast1"`; every Firestore trigger needs `database: "default"`
   (which is what `db.ts` binds). A function that only imports `db` needs nothing extra.

---

## 8. Definition of done for the change

- [ ] `protobuf/models.proto` and `Alert.kt` updated (only if a new `AlertType`), regenerated
- [ ] One integration file under `functions/src/integrations/`, writing to `alerts` directly
- [ ] Writes go through `alertDocSchema.parse` + `alertToFirestore`, with a namespaced doc ID
- [ ] `region` set on the function, plus a fetch timeout and per-record error handling
- [ ] Function exported from `functions/src/index.ts`
- [ ] `npm --prefix functions run lint` and `run build` pass
- [ ] Emulator run shows one alert per source state and no re-broadcast on a re-poll
- [ ] Release build receives the push (signing key pair matches)
- [ ] `ARCHITECTURE.md` / `DEVELOPMENT.md` updated if behaviour or the function inventory changed
