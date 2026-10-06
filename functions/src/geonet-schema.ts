import {Timestamp} from "firebase-admin/firestore";
import {z} from "zod";

const epochMillis = z
  .union([z.instanceof(Timestamp), z.date(), z.number().int()])
  .transform((value) => {
    if (value instanceof Timestamp) {
      return value.toMillis();
    }
    if (value instanceof Date) {
      return value.getTime();
    }
    return value;
  });

export const geonetDocSchema = z.strictObject({
  id: z.string(),
  magnitude: z.number(),
  depth: z.number(),
  latitude: z.number(),
  longitude: z.number(),
  time: epochMillis,
  locality: z.string().optional(),
});

export type GeonetDoc = z.infer<typeof geonetDocSchema>;
