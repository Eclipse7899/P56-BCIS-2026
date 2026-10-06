import {onDocumentCreated} from "firebase-functions/v2/firestore";
import {getMessaging, Message} from "firebase-admin/messaging";
import {defineString} from "firebase-functions/params";
import {createPrivateKey, sign} from "crypto";
import {AlertDto, MessageDataDto, SignedMessageDto} from "../models";
import {alertFromFirestore} from "../alert-schema";

const signingKey = defineString("SIGNING_KEY");

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

    const alertId = event.params.alertId;
    const alert = alertFromFirestore(event.data?.data() ?? {}, alertId);

    const alertDto = AlertDto.fromJson(alert);
    const messageDataDto: MessageDataDto = {
      msg: [alertDto],
      validUntil: alertDto.expiresAt,
      timestamp: BigInt(Date.now()),
    };

    const serializedAlert = MessageDataDto.toBinary(messageDataDto);
    const alertSignature = sign(undefined, serializedAlert, privateKey);

    const signed: SignedMessageDto = {
      data: serializedAlert,
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
    console.log(`Broadcast notification for alert ${alertId}`);
  }
);
