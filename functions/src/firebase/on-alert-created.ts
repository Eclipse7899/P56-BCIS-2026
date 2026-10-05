import {onDocumentCreated} from "firebase-functions/v2/firestore";
import {getMessaging, Message} from "firebase-admin/messaging";
import {region} from "../region";

interface AlertRecord {
  title?: string;
  description?: string;
  severity?: string;
}

export const onAlertCreated = onDocumentCreated(
  {
    document: "alerts/{alertId}",
    region,
    database: "default",
  },
  async (event) => {
    const alert = (event.data?.data() ?? {}) as AlertRecord;
    const alertId = event.params.alertId;

    const message: Message = {
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
