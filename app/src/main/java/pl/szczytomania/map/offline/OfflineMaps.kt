package pl.szczytomania.map.offline

import android.content.Context
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.maplibre.android.MapLibre
import org.maplibre.android.geometry.LatLngBounds
import org.maplibre.android.offline.OfflineManager
import org.maplibre.android.offline.OfflineRegion
import org.maplibre.android.offline.OfflineRegionError
import org.maplibre.android.offline.OfflineRegionStatus
import org.maplibre.android.offline.OfflineTilePyramidRegionDefinition
import pl.szczytomania.map.MapStyle

/** Stan regionu na telefonie. */
sealed interface RegionDownload {
    data object None : RegionDownload

    /**
     * Pobieranie trwa ([active]) albo zostało przerwane, np. przez zamknięcie aplikacji.
     * [waitingForNetwork] – MapLibre ponowi pobieranie, gdy wróci połączenie.
     */
    data class InProgress(
        val completedBytes: Long,
        val fraction: Float,
        val active: Boolean,
        val waitingForNetwork: Boolean = false,
    ) : RegionDownload

    data class Done(val bytes: Long, val downloadedAt: Long) : RegionDownload
}

/** Dane zapisane razem z regionem w bazie MapLibre. */
@Serializable
private data class RegionMetadata(val region: String, val downloadedAt: Long? = null)

/**
 * Pobrane mapy regionów. Mapa korzysta z nich sama: bez internetu MapLibre bierze kafelki z bazy
 * na telefonie, o ile styl mapy jest ten sam co przy pobieraniu.
 *
 * Jedna kopia na aplikację – pobieranie trwa dalej po wyjściu z ekranu "Mapy offline".
 * Wywołania MapLibre i ich odpowiedzi idą przez główny wątek.
 */
class OfflineMaps private constructor(context: Context) {

    private val manager: OfflineManager
    private val pixelRatio = context.resources.displayMetrics.density
    private val regions = mutableMapOf<String, OfflineRegion>()

    private val _state = MutableStateFlow<Map<String, RegionDownload>>(emptyMap())
    val state: StateFlow<Map<String, RegionDownload>> = _state.asStateFlow()

    init {
        MapLibre.getInstance(context)
        manager = OfflineManager.getInstance(context)
        load()
    }

    fun download(region: MapRegion) {
        setState(region.id, RegionDownload.InProgress(0, 0f, active = true))
        regions[region.id]?.let {
            start(region.id, it)
            return
        }
        val definition = OfflineTilePyramidRegionDefinition(
            MapStyle.url,
            LatLngBounds.from(region.north, region.east, region.south, region.west),
            MapRegion.MIN_ZOOM.toDouble(),
            MapRegion.MAX_ZOOM.toDouble(),
            pixelRatio,
        )
        manager.createOfflineRegion(
            definition,
            encode(RegionMetadata(region.id)),
            object : OfflineManager.CreateOfflineRegionCallback {
                override fun onCreate(offlineRegion: OfflineRegion) {
                    regions[region.id] = offlineRegion
                    start(region.id, offlineRegion)
                }

                override fun onError(error: String) {
                    Log.e(TAG, "Nie udało się utworzyć regionu ${region.id}: $error")
                    setState(region.id, RegionDownload.None)
                }
            },
        )
    }

    /** Usuwa pobrany region albo przerywa pobieranie. */
    fun delete(region: MapRegion) {
        setState(region.id, RegionDownload.None)
        regions.remove(region.id)?.let(::deleteFromDatabase)
    }

    private fun load() {
        manager.listOfflineRegions(object : OfflineManager.ListOfflineRegionsCallback {
            override fun onList(offlineRegions: Array<OfflineRegion>?) {
                offlineRegions.orEmpty().forEach { offlineRegion ->
                    val metadata = decode(offlineRegion.metadata)
                    val styleUrl = (offlineRegion.definition as? OfflineTilePyramidRegionDefinition)?.styleURL
                    // Region pobrany dla innego stylu (np. przed dopisaniem klucza MapTiler) mapa i tak pominie.
                    if (metadata == null || MapRegions.byId(metadata.region) == null || styleUrl != MapStyle.url) {
                        deleteFromDatabase(offlineRegion)
                        return@forEach
                    }
                    regions[metadata.region] = offlineRegion
                    offlineRegion.getStatus(object : OfflineRegion.OfflineRegionStatusCallback {
                        override fun onStatus(status: OfflineRegionStatus?) {
                            status ?: return
                            setState(metadata.region, status.toDownload(active = false, metadata.downloadedAt))
                        }

                        override fun onError(error: String?) {
                            Log.e(TAG, "Nie udało się odczytać stanu regionu ${metadata.region}: $error")
                        }
                    })
                }
            }

            override fun onError(error: String) {
                Log.e(TAG, "Nie udało się wczytać pobranych regionów: $error")
            }
        })
    }

    private fun start(id: String, offlineRegion: OfflineRegion) {
        offlineRegion.setObserver(object : OfflineRegion.OfflineRegionObserver {
            override fun onStatusChanged(status: OfflineRegionStatus) {
                // Region mógł zostać usunięty, zanim doszła ta wiadomość.
                if (regions[id] !== offlineRegion) return
                if (!status.isComplete) {
                    setState(id, status.toDownload(active = true, downloadedAt = null))
                    return
                }
                offlineRegion.setDownloadState(OfflineRegion.STATE_INACTIVE)
                val now = System.currentTimeMillis()
                offlineRegion.updateMetadata(encode(RegionMetadata(id, now)), object : OfflineRegion.OfflineRegionUpdateMetadataCallback {
                    override fun onUpdate(metadata: ByteArray) = Unit
                    override fun onError(error: String) {
                        Log.e(TAG, "Nie udało się zapisać daty pobrania $id: $error")
                    }
                })
                setState(id, RegionDownload.Done(status.completedResourceSize, now))
            }

            override fun onError(error: OfflineRegionError) {
                // Błędy pojedynczych kafelków MapLibre ponawia sam – pokazujemy tylko brak sieci.
                Log.w(TAG, "Pobieranie $id: ${error.reason} ${error.message}")
                if (error.reason == OfflineRegionError.REASON_CONNECTION) {
                    _state.update { states ->
                        val current = states[id] as? RegionDownload.InProgress ?: return@update states
                        states + (id to current.copy(waitingForNetwork = true))
                    }
                }
            }

            override fun mapboxTileCountLimitExceeded(limit: Long) = Unit // Dotyczy tylko kafelków Mapbox.
        })
        offlineRegion.setDownloadState(OfflineRegion.STATE_ACTIVE)
    }

    private fun deleteFromDatabase(offlineRegion: OfflineRegion) {
        offlineRegion.setDownloadState(OfflineRegion.STATE_INACTIVE)
        offlineRegion.delete(object : OfflineRegion.OfflineRegionDeleteCallback {
            // Usunięcie tylko oznacza miejsce jako wolne – pakowanie bazy zwalnia je na dysku.
            override fun onDelete() = manager.packDatabase(object : OfflineManager.FileSourceCallback {
                override fun onSuccess() = Unit
                override fun onError(message: String) {
                    Log.e(TAG, "Nie udało się spakować bazy map: $message")
                }
            })

            override fun onError(error: String) {
                Log.e(TAG, "Nie udało się usunąć regionu: $error")
            }
        })
    }

    private fun setState(id: String, download: RegionDownload) = _state.update { it + (id to download) }

    companion object {
        private const val TAG = "OfflineMaps"
        private val json = Json { ignoreUnknownKeys = true }

        @Volatile private var instance: OfflineMaps? = null

        fun get(context: Context): OfflineMaps =
            instance ?: synchronized(this) {
                instance ?: OfflineMaps(context.applicationContext).also { instance = it }
            }

        private fun encode(metadata: RegionMetadata) = json.encodeToString(metadata).toByteArray()

        private fun decode(bytes: ByteArray): RegionMetadata? =
            runCatching { json.decodeFromString<RegionMetadata>(bytes.decodeToString()) }.getOrNull()
    }
}

private fun OfflineRegionStatus.toDownload(active: Boolean, downloadedAt: Long?): RegionDownload =
    if (isComplete) {
        RegionDownload.Done(completedResourceSize, downloadedAt ?: 0)
    } else {
        RegionDownload.InProgress(
            completedBytes = completedResourceSize,
            fraction = if (requiredResourceCount > 0) completedResourceCount.toFloat() / requiredResourceCount else 0f,
            active = active,
        )
    }
