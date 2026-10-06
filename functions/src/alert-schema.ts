import {Timestamp} from "firebase-admin/firestore";
import {z} from "zod";
import {AlertTypeDto, SeverityDto} from "./models";

const uint64 = z
  .number()
  .int()
  .nonnegative()
  .max(Number.MAX_SAFE_INTEGER);

const latLongSchema = z.strictObject({
  latitude: z.number(),
  longitude: z.number(),
});

const circleSchema = z.strictObject({
  center: latLongSchema,
  radius: z.number().nonnegative(),
});

const polygonSchema = z.strictObject({
  points: z.array(latLongSchema),
});

const shapeSchema = z.union([
  z.strictObject({circle: circleSchema}),
  z.strictObject({polygon: polygonSchema}),
]);

export const alertDocSchema = z.strictObject({
  id: z.string(),
  timestamp: uint64,
  title: z.string(),
  description: z.string(),
  type: z.enum(AlertTypeDto),
  severity: z.enum(SeverityDto),
  shape: shapeSchema.optional(),
  version: uint64,
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
  data: Record<string, unknown>,
  id: string
): AlertDoc {
  return alertDocSchema.parse({
    ...data,
    id,
    timestamp: toEpochMillis(data.timestamp, "timestamp"),
    version: toEpochMillis(data.version, "version"),
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
