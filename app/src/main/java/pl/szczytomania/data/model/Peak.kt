package pl.szczytomania.data.model

import kotlinx.serialization.Serializable

/** Szczyt z pliku `assets/peaks/peaks.json` (dane z OpenStreetMap). */
@Serializable
data class Peak(
    /** Identyfikator obiektu w OpenStreetMap, np. "node/4366169405" – nie zmienia się między importami. */
    val id: String,
    val name: String,
    /** Wysokość w metrach n.p.m. */
    val elevation: Int,
    val lat: Double,
    val lon: Double,
    /** Pasmo górskie, np. "Tatry". */
    val range: String,
    /** Czy szczyt należy do Korony Gór Polski. Brak pola w pliku = false. */
    val kgp: Boolean = false,
)
