import {onDocumentCreated} from "firebase-functions/v2/firestore";
import {GeoPoint, Timestamp} from "firebase-admin/firestore";
import {db} from "../db";
import {region} from "../region";

interface GeonetRecord {
  magnitude: number | string;
  depth: number | string;
  locality: string;
  latitude: number | string;
  longitude: number | string;
  time: string;
}

const locationPrefix = /^\d+\s*km\s+\w+\s+of\s+/i;

export const onGeonetQuakeCreated = onDocumentCreated(
  {
    document: "geonet/{geonetId}",
    region, // match your Firestore location
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

    const alert = {
      title: "Earthquake Warning",
      description,
      location: new GeoPoint(Number(geonet.latitude), Number(geonet.longitude)),
      magnitude: magnitude,
      severity: magnitude >= 5 ? "high" : magnitude >= 4 ? "medium" : "low",
      source: "GeoNet",
      status: "active",
      timestamp: Timestamp.fromDate(new Date(geonet.time)),
    };

    await db.collection("alerts").doc(geonetId).set(alert);
    console.log(`Created alert for GeoNet event ${geonetId}`);
  }
);
