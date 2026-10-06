import {onSchedule} from "firebase-functions/v2/scheduler";
import {z} from "zod";
import {db} from "../db";
import {geonetDocSchema} from "../geonet-schema";

const geoNetQuakeSchema = z.object({
  properties: z.object({
    publicID: z.string(),
    locality: z.string().optional(),
    magnitude: z.number(),
    depth: z.number(),
    time: z.string(),
  }),
  geometry: z.object({
    coordinates: z.tuple([z.number(), z.number()]),
  }),
});

const geoNetResponseSchema = z.object({
  features: z.array(geoNetQuakeSchema).default([]),
});

export const fetchGeonetData = onSchedule(
  {
    schedule: "every 15 minutes",
    timeZone: "Pacific/Auckland",
    region: "australia-southeast1",
  },
  async () => {
    const response = await fetch("https://api.geonet.org.nz/quake?MMI=3", {
      headers: {Accept: "application/vnd.geo+json;version=2"},
    });

    if (!response.ok) {
      console.error(`GeoNet API error: ${response.status}`);
      return;
    }

    const data = geoNetResponseSchema.parse(await response.json());
    const quakes = data.features;

    let written = 0;
    for (const quake of quakes) {
      const {publicID, locality, magnitude, depth, time} = quake.properties;
      const [longitude, latitude] = quake.geometry.coordinates;

      const geonetDoc = geonetDocSchema.parse({
        id: publicID,
        magnitude,
        depth,
        latitude,
        longitude,
        time: Date.parse(time),
        locality,
      });

      await db
        .collection("geonet")
        .doc(publicID)
        .set(geonetDoc, {merge: true});
      written++;
    }

    console.log(
      `Fetched ${quakes.length} quakes, wrote ${written} geonet documents`
    );
  }
);
