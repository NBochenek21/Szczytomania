package pl.szczytomania.data.peaks

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import pl.szczytomania.data.model.Peak
import pl.szczytomania.data.model.PeakSet

/** Zawartość pliku `assets/peaks/peaks.json`. */
@Serializable
data class PeakFile(
    /** Wersja formatu – podbijamy ją przy zmianach, które psują zgodność. */
    val version: Int,
    /** Źródło danych, np. "© OpenStreetMap contributors (ODbL)". */
    val source: String,
    val peaks: List<Peak>,
)

/** Zawartość pliku `assets/sets/sets.json` z gotowymi zestawami gór. */
@Serializable
data class PeakSetFile(val version: Int, val sets: List<PeakSet>)

object PeakFileParser {

    const val SUPPORTED_VERSION = 1

    // Nieznane pola są pomijane, więc do pliku można dopisywać nowe dane bez psucia starszych wersji aplikacji.
    internal val json = Json { ignoreUnknownKeys = true }

    fun parse(text: String): List<Peak> {
        val file = json.decodeFromString<PeakFile>(text)
        require(file.version == SUPPORTED_VERSION) {
            "Nieobsługiwana wersja pliku szczytów: ${file.version} (obsługiwana: $SUPPORTED_VERSION)"
        }
        return file.peaks
    }
}

object PeakSetFileParser {
    fun parse(text: String): List<PeakSet> {
        val file = PeakFileParser.json.decodeFromString<PeakSetFile>(text)
        require(file.version == PeakFileParser.SUPPORTED_VERSION) {
            "Nieobsługiwana wersja pliku zestawów: ${file.version} (obsługiwana: ${PeakFileParser.SUPPORTED_VERSION})"
        }
        return file.sets
    }
}
