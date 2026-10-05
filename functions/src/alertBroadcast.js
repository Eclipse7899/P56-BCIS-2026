const {onDocumentCreated} = require("firebase-functions/v2/firestore");
const {getMessaging} = require("firebase-admin/messaging");
const {region} = require("./region");

module.exports = onDocumentCreated(
  {
    document: "alerts/{alertId}",
    region: region,
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
