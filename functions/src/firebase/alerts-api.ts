import {onRequest} from "firebase-functions/v2/https";
import {db} from "../db";
import {alertFromFirestore} from "../alert-schema";

export const alertsApi = onRequest(
  {region: "australia-southeast1"},
  async (req, res) => {
    if (req.method === "GET" && req.path === "/alerts") {
      const requestedCount = parseInt(String(req.query.count), 10);
      const count = Number.isInteger(requestedCount) ?
        Math.min(Math.max(requestedCount, 1), 100) :
        50;

      const snapshot = await db
        .collection("alerts")
        .orderBy("timestamp", "desc")
        .limit(count)
        .get();

      const messages = snapshot.docs.map(
        (doc) => alertFromFirestore(doc.data(), doc.id)
      );

      res.json({messages});
      return;
    }

    res.status(404).json({error: "Not found"});
  }
);
