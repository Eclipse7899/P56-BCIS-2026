package com.p56.safereach.service

import android.util.Log
import com.mapbox.bindgen.Value
import com.mapbox.common.TileRegion
import com.mapbox.common.TileRegionLoadOptions
import com.mapbox.common.TileStore
import com.mapbox.geojson.Geometry
import com.mapbox.geojson.Point
import com.mapbox.geojson.Polygon
import com.mapbox.maps.CoordinateBounds
import com.mapbox.maps.GlyphsRasterizationMode
import com.mapbox.maps.MapboxMapsOptions
import com.mapbox.maps.OfflineManager
import com.mapbox.maps.Style
import com.mapbox.maps.StylePackLoadOptions
import com.mapbox.maps.TilesetDescriptorOptions
import com.mapbox.turf.TurfConstants
import com.mapbox.turf.TurfMeasurement
import jakarta.inject.Inject
import jakarta.inject.Singleton
import java.util.concurrent.atomic.AtomicReference

const val MAX_OFFLINE_REGION_SIZE_KM = 200.0

data class RegionSizeKm(val widthKm: Double, val heightKm: Double) {
    val isWithinLimit: Boolean
        get() = widthKm <= MAX_OFFLINE_REGION_SIZE_KM && heightKm <= MAX_OFFLINE_REGION_SIZE_KM
}

fun CoordinateBounds.sizeKm(): RegionSizeKm {
    val southWest = Point.fromLngLat(southwest.longitude(), southwest.latitude())
    val northEast = Point.fromLngLat(northeast.longitude(), northeast.latitude())
    val southEast = Point.fromLngLat(northEast.longitude(), southWest.latitude())
    val northWest = Point.fromLngLat(southWest.longitude(), northEast.latitude())

    return RegionSizeKm(
        widthKm = TurfMeasurement.distance(southWest, southEast, TurfConstants.UNIT_KILOMETERS),
        heightKm = TurfMeasurement.distance(southWest, northWest, TurfConstants.UNIT_KILOMETERS)
    )
}

class OfflineRegionTooLargeException(val size: RegionSizeKm) : Exception(
    "Offline area is ${size.widthKm} x ${size.heightKm} km, the maximum is " +
            "$MAX_OFFLINE_REGION_SIZE_KM x $MAX_OFFLINE_REGION_SIZE_KM km"
)

data class OfflineRegion(
    val id: String,
    val name: String,
    val bounds: CoordinateBounds
) {
    val sizeKm: RegionSizeKm get() = bounds.sizeKm()

    val polygon: Polygon
        get() {
            val sw = bounds.southwest
            val ne = bounds.northeast
            val coords = listOf(
                Point.fromLngLat(sw.longitude(), sw.latitude()),
                Point.fromLngLat(ne.longitude(), sw.latitude()),
                Point.fromLngLat(ne.longitude(), ne.latitude()),
                Point.fromLngLat(sw.longitude(), ne.latitude()),
                Point.fromLngLat(sw.longitude(), sw.latitude())
            )
            return Polygon.fromLngLats(listOf(coords))
        }
}

@Singleton
class OfflineRegionManager @Inject constructor() {
    companion object {
        private const val TAG = "OfflineRegionManager"
        private const val STYLE_URI = Style.STANDARD
        private const val MIN_ZOOM = 10.0
        private const val MAX_ZOOM = 14.0
    }

    private val offlineManager: OfflineManager = OfflineManager()

    private val tileStore: TileStore = MapboxMapsOptions.tileStore
        ?: TileStore.create().also { MapboxMapsOptions.tileStore = it }

    fun ensureStylePackDownloaded() {
        try {
            val stylePackOptions = StylePackLoadOptions.Builder()
                .glyphsRasterizationMode(GlyphsRasterizationMode.IDEOGRAPHS_RASTERIZED_LOCALLY)
                .metadata(Value("mapbox-standard-stylepack"))
                .acceptExpired(false)
                .build()

            offlineManager.loadStylePack(
                STYLE_URI,
                stylePackOptions,
                { progress ->
                    Log.d(
                        TAG,
                        "Style pack progress: ${progress.completedResourceCount}/${progress.requiredResourceCount}"
                    )
                },
                { expected ->
                    expected.value?.let { stylePack ->
                        // Style pack download finishes successfully
                        Log.d(TAG, "Style pack downloaded: $stylePack")
                    }
                    expected.error?.let { error ->
                        Log.w(TAG, "Style pack download failed: $error")
                    }
                }
            )

        } catch (e: Throwable) {
            Log.w(TAG, "Style pack API not available or failed: ${e.message}")
        }
    }

    fun downloadRegion(
        region: OfflineRegion,
        downloadingRegions: Set<String>,
        onDownloadingRegionsUpdate: (Set<String>) -> Unit,
        onProgress: (String, Float) -> Unit,
        onCompletion: (String, Result<TileRegion>) -> Unit
    ) {
        if (downloadingRegions.contains(region.id)) {
            Log.d(TAG, "Region ${region.id} is already downloading; skipping")
            return
        }

        val size = region.sizeKm
        if (!size.isWithinLimit) {
            Log.w(
                TAG,
                "Refusing to download ${region.id}: ${size.widthKm} x ${size.heightKm} km " +
                        "exceeds the $MAX_OFFLINE_REGION_SIZE_KM km per-side limit"
            )
            onCompletion(region.id, Result.failure(OfflineRegionTooLargeException(size)))
            return
        }

        val tilesetDescriptorOptions = TilesetDescriptorOptions.Builder()
            .styleURI(STYLE_URI)
            .minZoom(MIN_ZOOM.toInt().toByte())
            .maxZoom(MAX_ZOOM.toInt().toByte())
            .build()

        val tilesetDescriptor = offlineManager.createTilesetDescriptor(tilesetDescriptorOptions)

        val loadOptions: TileRegionLoadOptions = TileRegionLoadOptions.Builder()
            .geometry(region.polygon as Geometry)
            .descriptors(listOf(tilesetDescriptor))
            .metadata(Value(region.name))
            .acceptExpired(true)
            .build()

        val updated = downloadingRegions.toMutableSet()
        updated.add(region.id)
        onDownloadingRegionsUpdate(updated)
        onProgress(region.id, 0.0f)


        tileStore.loadTileRegion(
            region.id,
            loadOptions,
            { progress ->
                val total = maxOf(progress.requiredResourceCount, 1)
                val progressVal = progress.completedResourceCount.toFloat() / total.toFloat()
                onProgress(region.id, progressVal)
            }
        ) { result ->
            val updatedAfter = updated.toMutableSet()
            updatedAfter.remove(region.id)
            onDownloadingRegionsUpdate(updatedAfter)

            if (result.isValue) {
                val tileRegion = result.value!!
                onCompletion(region.id, Result.success(tileRegion))
            } else {
                val error = result.error
                val throwable = Exception(
                    error?.let { "Tile region download failed: ${it.type} ${it.message}" }
                        ?: "Tile region download failed"
                )
                onCompletion(region.id, Result.failure(throwable))
            }
        }

    }

    fun clearAllRegions(onCompletion: () -> Unit) {
        tileStore.getAllTileRegions { result ->
            if (result.isValue) {
                val tileRegions = result.value ?: emptyList()
                if (tileRegions.isEmpty()) {
                    tileStore.clearAmbientCache { cacheResult ->
                        if (cacheResult.isValue) {
                            Log.i(TAG, "Cleared ${cacheResult.value} bytes from cache")
                        } else {
                            Log.e(TAG, "Failed to clear cache: ${cacheResult.error}")
                        }
                        onCompletion()
                    }
                }

                val removalsPending = AtomicReference(tileRegions.size)
                for (tileRegion in tileRegions) {
                    tileStore.removeTileRegion(tileRegion.id) { removeResult ->
                        if (removeResult.isValue) {
                            Log.i(TAG, "Removed region: ${tileRegion.id}")
                        } else {
                            Log.e(
                                TAG,
                                "Failed to remove region ${tileRegion.id}: ${removeResult.error}"
                            )
                        }
                        val remaining = removalsPending.updateAndGet { it - 1 }
                        if (remaining <= 0) {
                            tileStore.clearAmbientCache { cacheResult ->
                                if (cacheResult.isValue) {
                                    Log.i(TAG, "Cleared ${cacheResult.value} bytes from cache")
                                } else {
                                    Log.e(TAG, "Failed to clear cache: ${cacheResult.error}")
                                }
                                onCompletion()
                            }
                        }
                    }
                }
            } else {
                Log.e(TAG, "Failed to get tile regions: ${result.error}")
                onCompletion()
            }
        }
    }
}
