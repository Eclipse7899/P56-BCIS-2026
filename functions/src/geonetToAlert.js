const {onDocumentCreated} = require("firebase-functions/v2/firestore");
const {GeoPoint, Timestamp} = require("firebase-admin/firestore");
const {db} = require("./init");
const {region} = require("./region");

module.exports = onDocumentCreated(
  {
    document: "geonet/{geonetId}",
    region: region,
    database: "default",
  },
  async (event) => {
    const geonet = event.data.data();
    const geonetId = event.params.geonetId;

    const magnitude = Number(geonet.magnitude);
    const depth = Math.round(Number(geonet.depth));

    // "5 km north of Wellington" -> "Wellington"
    const place = String(geonet.locality).replace(/^\d+\s*km\s+\w+\s+of\s+/i, "");

    const alert = {
      title: "Earthquake Warning",
      description: `M ${magnitude.toFixed(1)} earthquake ${depth}km deep near ${place}`,
      location: new GeoPoint(Number(geonet.latitude), Number(geonet.longitude)),
      magnitude: magnitude,
      severity: magnitude >= 5 ? "high" : magnitude >= 4 ? "medium" : "low",
      source: "GeoNet",
      status: "active",
      timestamp: Timestamp.fromDate(new Date(geonet.time)),
    };

    // Using the geonet ID as the doc ID prevents duplicate alerts on retries
    await db.collection("alerts").doc(geonetId).set(alert);
    console.log(`Created alert for GeoNet event ${geonetId}`);
  }
);
