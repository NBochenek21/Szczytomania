package pl.szczytomania.map

import pl.szczytomania.BuildConfig

object MapStyle {

    // Styl turystyczny MapTiler (dane OpenStreetMap) – wymaga klucza w local.properties.
    private const val MAPTILER_OUTDOOR = "https://api.maptiler.com/maps/outdoor-v2/style.json?key="

    // Bez klucza używamy darmowego stylu OpenFreeMap (też OpenStreetMap), żeby mapa działała od razu.
    private const val OPENFREEMAP_LIBERTY = "https://tiles.openfreemap.org/styles/liberty"

    val url: String
        get() = BuildConfig.MAPTILER_API_KEY
            .takeIf { it.isNotBlank() }
            ?.let { MAPTILER_OUTDOOR + it }
            ?: OPENFREEMAP_LIBERTY
}
