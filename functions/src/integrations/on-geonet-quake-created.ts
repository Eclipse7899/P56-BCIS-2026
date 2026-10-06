import {onDocumentCreated} from "firebase-functions/v2/firestore";
import {db} from "../db";
import {AlertDoc, alertDocSchema, alertToFirestore} from "../alert-schema";
import {geonetDocSchema} from "../geonet-schema";
import {AlertTypeDto, SeverityDto} from "../models";

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
      id: geonetId,
      timestamp,
      title: "Earthquake Warning",
      description,
      type: AlertTypeDto.ALERT_TYPE_EARTHQUAKE,
      severity: severityForMagnitude(magnitude),
      shape: {
        circle: {
          center: {
            latitude: geonet.latitude,
            longitude: geonet.longitude,
          },
          radius: radiusMetresForMagnitude(magnitude),
        },
      },
      version: timestamp,
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
