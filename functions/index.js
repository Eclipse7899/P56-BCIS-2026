const { onDocumentCreated } = require("firebase-functions/v2/firestore");
const { initializeApp } = require("firebase-admin/app");
const { getFirestore, GeoPoint, Timestamp } = require("firebase-admin/firestore");
const { onRequest } = require("firebase-functions/v2/https");
const { getMessaging } = require("firebase-admin/messaging");
const { onSchedule } = require("firebase-functions/v2/scheduler");

initializeApp();
const db = getFirestore("default");

exports.geonetToAlert = onDocumentCreated(
  {
    document: "geonet/{geonetId}",
    region: "australia-southeast1",
    database: "default",
  },
  async (event) => {
    const geonet = event.data.data();
    const geonetId = event.params.geonetId;

    const magnitude = Number(geonet.magnitude);
    const depth = Math.round(Number(geonet.depth));

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

    await db.collection("alerts").doc(geonetId).set(alert);
    console.log(`Created alert for GeoNet event ${geonetId}`);
  }
);

exports.api = onRequest({ region: "australia-southeast1" }, async (req, res) => {
  if (req.method === "GET" && req.path === "/alerts") {
    const requestedCount = parseInt(req.query.count, 10);
    const count = Number.isInteger(requestedCount)
      ? Math.min(Math.max(requestedCount, 1), 100)
      : 50;

    const snapshot = await db
      .collection("alerts")
      .orderBy("timestamp", "desc")
      .limit(count)
      .get();

    const messages = snapshot.docs.map((doc) => ({
      id: doc.id,
      ...doc.data(),
    }));

    res.json({ messages });
    return;
  }

  res.status(404).json({ error: "Not found" });
});

exports.alertBroadcast = onDocumentCreated(
  {
    document: "alerts/{alertId}",
    region: "australia-southeast1",
    database: "default",
  },
  async (event) => {
    const alert = event.data.data();
    const alertId = event.params.alertId;

    const message = {
      notification: {
        title: alert.title || "New Alert",
        body: alert.description || "",
      },
      data: {
        alertId: alertId,
        severity: alert.severity || "",
      },
      topic: "alerts",
    };

    await getMessaging().send(message);
    console.log(`Broadcast notification for alert ${alertId}`);
  }
);

exports.fetchGeonetData = onSchedule(
  {
    schedule: "every 15 minutes",
    timeZone: "Pacific/Auckland",
    region: "australia-southeast1",
  },
  async (event) => {
    const response = await fetch("https://api.geonet.org.nz/quake?MMI=3", {
      headers: { Accept: "application/vnd.geo+json;version=2" },
    });

    if (!response.ok) {
      console.error(`GeoNet API error: ${response.status}`);
      return;
    }

    const data = await response.json();
    const quakes = data.features || [];

    let written = 0;
    for (const quake of quakes) {
      const props = quake.properties;
      const [longitude, latitude] = quake.geometry.coordinates;

      const geonetDoc = {
        locality: props.locality,
        latitude: latitude,
        longitude: longitude,
        depth: props.depth,
        magnitude: props.magnitude,
        time: props.time,
      };

      await db.collection("geonet").doc(props.publicID).set(geonetDoc, { merge: true });
      written++;
    }

    console.log(`Fetched ${quakes.length} quakes, wrote ${written} geonet documents`);
  }
);