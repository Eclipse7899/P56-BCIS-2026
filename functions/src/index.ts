import {setGlobalOptions} from "firebase-functions";

export {onGeonetQuakeCreated} from "./integrations/on-geonet-quake-created";
export {alertsApi} from "./firebase/alerts-api";
export {onAlertCreated} from "./firebase/on-alert-created";

setGlobalOptions({maxInstances: 10});
