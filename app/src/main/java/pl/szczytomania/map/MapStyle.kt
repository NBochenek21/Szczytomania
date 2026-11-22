package pl.szczytomania.map

import pl.szczytomania.BuildConfig
import pl.szczytomania.map.offline.PolandOutline

object MapStyle {

    // Styl turystyczny MapTiler (dane OpenStreetMap) – wymaga klucza w local.properties.
    private const val MAPTILER_OUTDOOR = "https://api.maptiler.com/maps/outdoor-v2/style.json?key="

    // Bez klucza używamy darmowego stylu OpenFreeMap (też OpenStreetMap), żeby mapa działała od razu.
    private const val OPENFREEMAP_LIBERTY = "https://tiles.openfreemap.org/styles/liberty"

    private val hasMapTilerKey get() = BuildConfig.MAPTILER_API_KEY.isNotBlank()

    val url: String
        get() = if (hasMapTilerKey) MAPTILER_OUTDOOR + BuildConfig.MAPTILER_API_KEY else OPENFREEMAP_LIBERTY

    private const val MAPTILER_GLYPHS = "https://api.maptiler.com/fonts/{fontstack}/{range}.pbf?key="
    private const val OPENFREEMAP_GLYPHS = "https://tiles.openfreemap.org/fonts/{fontstack}/{range}.pbf"

    /**
     * Styl zapasowy, gdy właściwego nie da się wczytać (pierwsze uruchomienie bez internetu i bez
     * pobranego regionu): jasne tło i kontur Polski – na nim szczyty i pozycja użytkownika.
     * Czcionki te same co w głównym stylu – jeśli są w pamięci telefonu, widać też nazwy szczytów.
     */
    val fallbackJson: String
        get() {
            val glyphs = if (hasMapTilerKey) MAPTILER_GLYPHS + BuildConfig.MAPTILER_API_KEY else OPENFREEMAP_GLYPHS
            val outline = (PolandOutline.points + PolandOutline.points.first())
                .joinToString(",") { (lon, lat) -> "[$lon,$lat]" }
            return """
                {"version":8,"name":"Szczytomania bez internetu","glyphs":"$glyphs",
                 "sources":{"polska":{"type":"geojson","data":{"type":"Feature","properties":{},
                   "geometry":{"type":"LineString","coordinates":[$outline]}}}},
                 "layers":[
                   {"id":"tlo","type":"background","paint":{"background-color":"#EEE8D8"}},
                   {"id":"granica","type":"line","source":"polska","paint":{"line-color":"#8A8170","line-width":1.5}}
                 ]}
            """.trimIndent()
        }

    /** Dostawca kafelków – do podpisu "Mapy: …" pod pobranymi regionami. */
    val provider: String
        get() = if (hasMapTilerKey) "MapTiler" else "OpenFreeMap"
}
