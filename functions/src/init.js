const {initializeApp} = require("firebase-admin/app");
const {getFirestore} = require("firebase-admin/firestore");

initializeApp();

const db = getFirestore("default");

module.exports = {db};
