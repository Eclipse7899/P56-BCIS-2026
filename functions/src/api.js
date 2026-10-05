const {onRequest} = require("firebase-functions/v2/https");
const {db} = require("./init");
const {region} = require("./region");

module.exports = onRequest({region: region}, async (req, res) => {
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

    res.json({messages});
    return;
  }

  res.status(404).json({error: "Not found"});
});
