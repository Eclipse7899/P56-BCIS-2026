package com.p56.safereach.ui.map

import androidx.lifecycle.ViewModel
import com.mapbox.maps.CoordinateBounds
import com.p56.safereach.service.MAX_OFFLINE_REGION_SIZE_KM
import com.p56.safereach.service.OfflineRegion
import com.p56.safereach.service.OfflineRegionManager
import com.p56.safereach.service.RegionSizeKm
import com.p56.safereach.service.sizeKm
import dagger.hilt.android.lifecycle.HiltViewModel
import jakarta.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlin.math.abs

private const val BOUNDS_EPSILON_DEGREES = 1e-3

sealed interface OfflineDownloadState {
    data object Idle : OfflineDownloadState
    data class Downloading(val progress: Float) : OfflineDownloadState
    data class Completed(val regionName: String) : OfflineDownloadState
    data class Failed(val message: String) : OfflineDownloadState
}

data class OfflineRegionUiState(
    val sizeKm: RegionSizeKm? = null,
    val downloadState: OfflineDownloadState = OfflineDownloadState.Idle,
    val isClearing: Boolean = false,
) {
    val isTooLarge: Boolean
        get() = sizeKm?.isWithinLimit == false

    val canDownload: Boolean
        get() = sizeKm?.isWithinLimit == true && downloadState !is OfflineDownloadState.Downloading
}

@HiltViewModel
class OfflineRegionViewModel @Inject constructor(
    private val offlineRegionManager: OfflineRegionManager,
) : ViewModel() {

    private val _uiState = MutableStateFlow(OfflineRegionUiState())
    val uiState: StateFlow<OfflineRegionUiState> = _uiState.asStateFlow()

    private var selection: CoordinateBounds? = null

    private var selectionBackup: CoordinateBounds? = null

    private var downloadingRegions: Set<String> = emptySet()

    fun onViewportChanged(bounds: CoordinateBounds) {
        if (selection?.matches(bounds) == true) return
        selection = bounds
        _uiState.update { it.copy(sizeKm = bounds.sizeKm()) }
    }

    fun beginSelection() {
        selectionBackup = selection
    }

    fun cancelSelection() {
        selection = selectionBackup
        _uiState.update { it.copy(sizeKm = selection?.sizeKm()) }
    }

    fun downloadSelection() {
        val bounds = selection ?: return
        val size = bounds.sizeKm()
        if (!size.isWithinLimit) {
            _uiState.update {
                it.copy(downloadState = OfflineDownloadState.Failed(tooLargeMessage(size)))
            }
            return
        }
        if (_uiState.value.downloadState is OfflineDownloadState.Downloading) return

        val region = OfflineRegion(
            id = "region-${System.currentTimeMillis()}",
            name = regionName(size),
            bounds = bounds
        )

        offlineRegionManager.ensureStylePackDownloaded()
        _uiState.update { it.copy(downloadState = OfflineDownloadState.Downloading(0f)) }

        offlineRegionManager.downloadRegion(
            region = region,
            downloadingRegions = downloadingRegions,
            onDownloadingRegionsUpdate = { downloadingRegions = it },
            onProgress = { _, progress ->
                _uiState.update {
                    it.copy(downloadState = OfflineDownloadState.Downloading(progress))
                }
            },
            onCompletion = { _, result ->
                result.fold(
                    onSuccess = {
                        _uiState.update {
                            it.copy(downloadState = OfflineDownloadState.Completed(region.name))
                        }
                    },
                    onFailure = { error ->
                        _uiState.update {
                            it.copy(
                                downloadState = OfflineDownloadState.Failed(
                                    error.message ?: "Download failed"
                                )
                            )
                        }
                    }
                )
            }
        )
    }

    fun clearDownloads() {
        _uiState.update { it.copy(isClearing = true) }
        offlineRegionManager.clearAllRegions {
            _uiState.update {
                it.copy(isClearing = false, downloadState = OfflineDownloadState.Idle)
            }
        }
    }

    fun dismissMessage() {
        _uiState.update { it.copy(downloadState = OfflineDownloadState.Idle) }
    }
}

private fun regionName(size: RegionSizeKm): String =
    "%.1f x %.1f km".format(size.widthKm, size.heightKm)

private fun tooLargeMessage(size: RegionSizeKm): String =
    "Area is %.1f x %.1f km. The maximum is %.0f x %.0f km, zoom in to shrink it.".format(
        size.widthKm,
        size.heightKm,
        MAX_OFFLINE_REGION_SIZE_KM,
        MAX_OFFLINE_REGION_SIZE_KM
    )

private fun CoordinateBounds.matches(
    other: CoordinateBounds,
    epsilon: Double = BOUNDS_EPSILON_DEGREES
): Boolean {
    val self = listOf(
        southwest.longitude(),
        southwest.latitude(),
        northeast.longitude(),
        northeast.latitude()
    )
    val them = listOf(
        other.southwest.longitude(),
        other.southwest.latitude(),
        other.northeast.longitude(),
        other.northeast.latitude()
    )
    return self.zip(them).all { (mine, theirs) -> abs(mine - theirs) < epsilon }
}
