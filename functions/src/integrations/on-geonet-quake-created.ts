import {onDocumentCreated} from "firebase-functions/v2/firestore";
import {db} from "../db";
import {region} from "../region";
import {AlertDoc, alertDocSchema} from "../alert-schema";
import {AlertTypeDto, SeverityDto} from "../models";

interface GeonetRecord {
  magnitude: number | string;
  depth: number | string;
  locality: string;
  latitude: number | string;
  longitude: number | string;
  time: string;
}

const locationPrefix = /^\d+\s*km\s+\w+\s+of\s+/i;

const alertLifetimeMs = 60 * 60 * 1000;

function severityForMagnitude(magnitude: number): SeverityDto {
  if (magnitude >= 5) {
    return SeverityDto.SEVERITY_HIGH;
  }
  if (magnitude >= 4) {
    return SeverityDto.SEVERITY_MEDIUM;
  }
  return SeverityDto.SEVERITY_LOW;
}

function radiusMetresForMagnitude(magnitude: number): number {
  return Math.round(100 * Math.pow(10, magnitude / 2));
}

export const onGeonetQuakeCreated = onDocumentCreated(
  {
    document: "geonet/{geonetId}",
    region: region.value(),
    database: "default",
  },
  async (event) => {
    const geonet = event.data?.data() as GeonetRecord | undefined;
    const geonetId = event.params.geonetId;

    if (!geonet) {
      return;
    }

    const magnitude = Number(geonet.magnitude);
    const depth = Math.round(Number(geonet.depth));
    const place = String(geonet.locality).replace(locationPrefix, "");
    const description =
      `M ${magnitude.toFixed(1)} earthquake ${depth}km deep near ${place}`;
    const timestamp = new Date(geonet.time).getTime();

    const alert: AlertDoc = alertDocSchema.parse({
      id: geonetId,
      timestamp,
      title: "Earthquake Warning",
      description,
      type: AlertTypeDto.ALERT_TYPE_EARTHQUAKE,
      severity: severityForMagnitude(magnitude),
      shape: {
        circle: {
          center: {
            latitude: Number(geonet.latitude),
            longitude: Number(geonet.longitude),
          },
          radius: radiusMetresForMagnitude(magnitude),
        },
      },
      version: timestamp,
      expiresAt: timestamp + alertLifetimeMs,
      invalidated: false,
    });

    await db.collection("alerts").doc(geonetId).set(alert);
    console.log(`Created alert for GeoNet event ${geonetId}`);
  }
);
