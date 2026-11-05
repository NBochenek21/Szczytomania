package pl.szczytomania.data.model

import kotlinx.serialization.Serializable

/** Gotowy zestaw gór z pliku `assets/sets/sets.json`, np. Korona Gór Polski. */
@Serializable
data class PeakSet(
    /** Stały identyfikator, np. "kgp" – posłuży do zapisu postępu i wyzwań. */
    val id: String,
    val name: String,
    val description: String,
    /** Identyfikatory szczytów z `peaks.json`, w kolejności wyświetlania (od najwyższego). */
    val peakIds: List<String>,
    /** Napis na plakietce, np. "Odznaka klasyczna" – tylko dla wyróżnionych zestawów. */
    val badge: String? = null,
)
