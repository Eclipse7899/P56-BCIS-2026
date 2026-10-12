import {onDocumentCreated} from "firebase-functions/v2/firestore";
import {crc32} from "zlib";
import {db} from "../db";
import {AlertDoc, alertDocSchema, alertToFirestore} from "../alert-schema";
import {geonetDocSchema} from "../geonet-schema";
import {AlertType, Severity} from "../models";

const locationPrefix = /^\d+\s*km\s+\w+\s+of\s+/i;

const alertLifetimeMs = 60 * 60 * 1000;

function severityForMagnitude(magnitude: number): Severity {
  if (magnitude >= 5) {
    return Severity.HIGH;
  }
  if (magnitude >= 4) {
    return Severity.MEDIUM;
  }
  return Severity.LOW;
}

function radiusMetresForMagnitude(magnitude: number): number {
  return Math.round(100 * Math.pow(10, magnitude / 2));
}

function alertIdForGeonet(geonetId: string): number {
  return crc32(geonetId) >>> 0;
}

export const onGeonetQuakeCreated = onDocumentCreated(
  {
    document: "geonet/{geonetId}",
    region: "australia-southeast1",
    database: "default",
  },
  async (event) => {
    const geonet = geonetDocSchema.parse(event.data?.data() ?? {});
    const geonetId = event.params.geonetId;

    const magnitude = geonet.magnitude;
    const depth = Math.round(geonet.depth);
    const place = geonet.locality?.replace(locationPrefix, "");
    const description = place ?
      `M ${magnitude.toFixed(1)} earthquake ${depth}km deep near ${place}` :
      `M ${magnitude.toFixed(1)} earthquake ${depth}km deep`;
    const timestamp = geonet.time;

    const alert: AlertDoc = alertDocSchema.parse({
      id: alertIdForGeonet(geonetId),
      timestamp,
      title: "Earthquake Warning",
      description,
      type: AlertType.EARTHQUAKE,
      severity: severityForMagnitude(magnitude),
      latitude: geonet.latitude,
      longitude: geonet.longitude,
      radius: radiusMetresForMagnitude(magnitude),
      expiresAt: timestamp + alertLifetimeMs,
      invalidated: false,
    });

    await db
      .collection("alerts")
      .doc(geonetId)
      .set(alertToFirestore(alert));
    console.log(`Created alert for GeoNet event ${geonetId}`);
  }
);
