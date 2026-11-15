package pl.szczytomania.ui.screens.settings

import android.net.ConnectivityManager
import androidx.core.content.getSystemService
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import pl.szczytomania.map.offline.MapRegion
import pl.szczytomania.map.offline.MapRegions
import pl.szczytomania.map.offline.OfflineMaps
import pl.szczytomania.map.offline.RegionDownload
import java.io.File

data class RegionItem(val region: MapRegion, val download: RegionDownload)

data class OfflineMapsUiState(
    /** Pobrane, potem pobierane, potem dostępne – jak na mockupie. */
    val regions: List<RegionItem> = emptyList(),
    val usedBytes: Long = 0,
    val freeBytes: Long = 0,
)

class OfflineMapsViewModel(
    private val offlineMaps: OfflineMaps,
    private val storageDir: File,
    private val connectivity: ConnectivityManager?,
) : ViewModel() {

    val state: StateFlow<OfflineMapsUiState> = offlineMaps.state
        .map { downloads -> uiState(downloads, storageDir.usableSpace) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), OfflineMapsUiState())

    /** Telefon jest na danych komórkowych (albo innej sieci z limitem) – przed pobraniem pytamy. */
    val isOnMeteredNetwork: Boolean get() = connectivity?.isActiveNetworkMetered == true

    fun download(region: MapRegion) = offlineMaps.download(region)

    fun delete(region: MapRegion) = offlineMaps.delete(region)

    companion object {
        val Factory = viewModelFactory {
            initializer {
                val application = checkNotNull(this[APPLICATION_KEY])
                OfflineMapsViewModel(
                    offlineMaps = OfflineMaps.get(application),
                    storageDir = application.filesDir,
                    connectivity = application.getSystemService<ConnectivityManager>(),
                )
            }
        }
    }
}

internal fun uiState(downloads: Map<String, RegionDownload>, freeBytes: Long): OfflineMapsUiState {
    val items = MapRegions.all.map { RegionItem(it, downloads[it.id] ?: RegionDownload.None) }
    return OfflineMapsUiState(
        regions = items.sortedBy { item ->
            when (item.download) {
                is RegionDownload.Done -> 0
                is RegionDownload.InProgress -> 1
                RegionDownload.None -> 2
            }
        },
        usedBytes = items.sumOf { item ->
            when (val download = item.download) {
                is RegionDownload.Done -> download.bytes
                is RegionDownload.InProgress -> download.completedBytes
                RegionDownload.None -> 0L
            }
        },
        freeBytes = freeBytes,
    )
}

/** "84" – megabajty dziesiętne, jak w ustawieniach Androida. */
internal fun megabytes(bytes: Long): Long = (bytes + 500_000) / 1_000_000
