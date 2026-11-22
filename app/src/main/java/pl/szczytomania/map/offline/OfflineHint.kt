package pl.szczytomania.map.offline

/** Informacja na mapie, gdy telefon nie ma internetu. */
enum class OfflineHint {
    /** Jest internet – nic nie pokazujemy. */
    NONE,

    /** Brak internetu, mapa oddalona albo poza regionami gór. */
    OFFLINE,

    /** Brak internetu, ale oglądany obszar jest pobrany – mapa działa w pełni. */
    AREA_DOWNLOADED,

    /** Brak internetu, a oglądanego regionu gór nie ma w telefonie – mapa będzie pusta. */
    AREA_MISSING,
}

// Bliżej tego przybliżenia oglądamy konkretne pasmo – ma sens mówić, czy jest pobrane.
private const val AREA_MIN_ZOOM = 8.0

/** Co pokazać przy środku mapy [lat], [lon] i przybliżeniu [zoom]. [downloaded] – id pobranych regionów. */
fun offlineHint(online: Boolean, lat: Double?, lon: Double?, zoom: Double?, downloaded: Set<String>): OfflineHint {
    if (online) return OfflineHint.NONE
    if (lat == null || lon == null || zoom == null || zoom < AREA_MIN_ZOOM) return OfflineHint.OFFLINE
    val regions = MapRegions.all.filter { it.contains(lat, lon) }
    return when {
        regions.isEmpty() -> OfflineHint.OFFLINE
        regions.any { it.id in downloaded } -> OfflineHint.AREA_DOWNLOADED
        else -> OfflineHint.AREA_MISSING
    }
}
