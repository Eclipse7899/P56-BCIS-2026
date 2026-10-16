import {onDocumentCreated} from "firebase-functions/v2/firestore";
import {getMessaging, Message} from "firebase-admin/messaging";
import {defineString} from "firebase-functions/params";
import {createPrivateKey, sign} from "crypto";
import {AlertBatchDto, AlertDto, SignedMessageDto} from "../models";
import {alertFromFirestore} from "../alert-schema";

const signingKey = defineString("SIGNING_KEY");

// Coordinates travel as fixed-point integers scaled by 1e5 in the wire format.
const coordinateScale = 1e5;

function toFixedPoint(value: number): number {
  return Math.round(value * coordinateScale);
}

function secondsUntil(epochMillis: number, nowMillis: number): number {
  return Math.max(0, Math.round((epochMillis - nowMillis) / 1000));
}

export const onAlertCreated = onDocumentCreated(
  {
    document: "alerts/{alertId}",
    region: "australia-southeast1",
    database: "default",
  },
  async (event) => {
    const privateKey = createPrivateKey({
      key: signingKey.value(),
      format: "pem",
    });

    const alert = alertFromFirestore(event.data?.data() ?? {});
    const sentAt = Date.now();

    const alertDto: AlertDto = {
      id: alert.id,
      timestamp: BigInt(alert.timestamp),
      title: alert.title,
      description: alert.description,
      type: alert.type,
      severity: alert.severity,
      latitudeE5: toFixedPoint(alert.latitude),
      longitudeE5: toFixedPoint(alert.longitude),
      radiusM: alert.radius,
      expiresInS: secondsUntil(alert.expiresAt, alert.timestamp),
      invalidated: alert.invalidated,
    };

    const batch: AlertBatchDto = {
      alerts: [alertDto],
      timestamp: BigInt(sentAt),
      expiresInS: secondsUntil(alert.expiresAt, sentAt),
    };

    const serializedBatch = AlertBatchDto.toBinary(batch);
    const alertSignature = sign(undefined, serializedBatch, privateKey);

    const signed: SignedMessageDto = {
      data: serializedBatch,
      signature: alertSignature,
    };

    const serializedMessage = SignedMessageDto.toBinary(signed);
    const base64 = Buffer.from(serializedMessage).toString("base64");

    const message: Message = {
      android: {
        priority: "high",
      },
      data: {
        alerts: base64,
      },
      topic: "alerts",
    };

    await getMessaging().send(message);
    console.log(`Broadcast notification for alert ${alert.id}`);
  }
);
