const { onDocumentCreated } = require("firebase-functions/v2/firestore");
const { initializeApp } = require("firebase-admin/app");
const { getFirestore, GeoPoint, Timestamp } = require("firebase-admin/firestore");
const { onRequest } = require("firebase-functions/v2/https");
const { getMessaging } = require("firebase-admin/messaging");

initializeApp();
const db = getFirestore("default");
const { onDocumentCreated } = require("firebase-functions/v2/firestore");
const { initializeApp } = require("firebase-admin/app");
const { getFirestore, GeoPoint, Timestamp } = require("firebase-admin/firestore");
const { onRequest } = require("firebase-functions/v2/https");
const { getMessaging } = require("firebase-admin/messaging");

initializeApp();
const db = getFirestore("default");

exports.geonetToAlert = onDocumentCreated(
  {
    document: "geonet/{geonetId}",
    region: "australia-southeast1", // match your Firestore location
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
exports.geonetToAlert = onDocumentCreated(
  {
    document: "geonet/{geonetId}",
    region: "australia-southeast1", // match your Firestore location
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