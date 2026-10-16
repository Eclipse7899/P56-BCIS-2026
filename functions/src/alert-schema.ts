import {Timestamp} from "firebase-admin/firestore";
import {z} from "zod";
import {AlertType, Severity} from "./models";

const uint32 = z
  .number()
  .int()
  .nonnegative()
  .max(0xffffffff);

const uint64 = z
  .number()
  .int()
  .nonnegative()
  .max(Number.MAX_SAFE_INTEGER);

export const alertDocSchema = z.strictObject({
  id: uint32,
  timestamp: uint64,
  title: z.string(),
  description: z.string(),
  type: z.enum(AlertType),
  severity: z.enum(Severity),
  latitude: z.number(),
  longitude: z.number(),
  radius: uint32,
  expiresAt: uint64,
  invalidated: z.boolean(),
});

export type AlertDoc = z.infer<typeof alertDocSchema>;

function toEpochMillis(value: unknown, field: string): number {
  if (value instanceof Timestamp) {
    return value.toMillis();
  }
  if (value instanceof Date) {
    return value.getTime();
  }
  if (typeof value === "number") {
    return value;
  }
  throw new Error(
    `Expected a Firestore Timestamp or epoch millis for "${field}"`
  );
}

export function alertFromFirestore(
  data: Record<string, unknown>
): AlertDoc {
  return alertDocSchema.parse({
    ...data,
    timestamp: toEpochMillis(data.timestamp, "timestamp"),
    expiresAt: toEpochMillis(data.expiresAt, "expiresAt"),
  });
}

export function alertToFirestore(alert: AlertDoc): Record<string, unknown> {
  return {
    ...alert,
    timestamp: Timestamp.fromMillis(alert.timestamp),
    expiresAt: Timestamp.fromMillis(alert.expiresAt),
  };
}
