package pl.szczytomania.data.peaks

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import pl.szczytomania.data.model.PeakSet

/** Zawartość pliku `assets/sets/sets.json` z gotowymi zestawami gór. */
@Serializable
data class PeakSetFile(
    val version: Int,
    val sets: List<PeakSet>,
)

object PeakSetFileParser {

    const val SUPPORTED_VERSION = 1

    private val json = Json { ignoreUnknownKeys = true }

    fun parse(text: String): List<PeakSet> {
        val file = json.decodeFromString<PeakSetFile>(text)
        require(file.version == SUPPORTED_VERSION) {
            "Nieobsługiwana wersja pliku zestawów: ${file.version} (obsługiwana: $SUPPORTED_VERSION)"
        }
        return file.sets
    }
}
