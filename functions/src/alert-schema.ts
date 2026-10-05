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
