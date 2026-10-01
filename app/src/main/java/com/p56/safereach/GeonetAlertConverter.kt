package com.p56.safereach

import android.util.Log
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentChange
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.GeoPoint
import java.time.Instant
import java.util.Date

fun startGeonetListener() {
    val db = FirebaseFirestore.getInstance()

    db.collection("geonet").addSnapshotListener { snapshots, error ->
        if (error != null || snapshots == null) {
            Log.e("GeonetConverter", "Listen failed", error)
            return@addSnapshotListener
        }

        for (change in snapshots.documentChanges) {
            if (change.type != DocumentChange.Type.ADDED) continue
            val doc = change.document

            val magnitude = doc.getDouble("magnitude") ?: continue
            val depth = Math.round(doc.getDouble("depth") ?: 0.0)
            val lat = doc.getDouble("latitude") ?: continue
            val lon = doc.getDouble("longitude") ?: continue
            val locality = doc.getString("locality") ?: ""
            val time = doc.getString("time") ?: continue

            // "5 km north of Wellington" -> "Wellington"
            val place = locality.replace(
                Regex("^\\d+\\s*km\\s+\\w+\\s+of\\s+", RegexOption.IGNORE_CASE), ""
            )

            val alert = hashMapOf(
                "title" to "Earthquake Warning",
                "description" to "M ${"%.1f".format(magnitude)} earthquake ${depth}km deep near $place",
                "location" to GeoPoint(lat, lon),
                "magnitude" to magnitude,
                "severity" to when {
                    magnitude >= 5 -> "high"
                    magnitude >= 4 -> "medium"
                    else -> "low"
                },
                "source" to "GeoNet",
                "status" to "active",
                "timestamp" to Timestamp(Date.from(Instant.parse(time)))
            )

            // Using the geonet ID as the doc ID prevents duplicate alerts
            db.collection("alerts").document(doc.id).set(alert)
                .addOnSuccessListener { Log.d("GeonetConverter", "Alert created for ${doc.id}") }
                .addOnFailureListener { e -> Log.e("GeonetConverter", "Failed for ${doc.id}", e) }
        }
    }
}

