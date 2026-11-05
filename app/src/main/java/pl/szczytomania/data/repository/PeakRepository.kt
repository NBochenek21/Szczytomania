package pl.szczytomania.data.repository

import android.content.res.AssetManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import pl.szczytomania.data.model.Peak
import pl.szczytomania.data.model.PeakSet
import pl.szczytomania.data.peaks.PeakFileParser
import pl.szczytomania.data.peaks.PeakSetFileParser

/** Szczyty i gotowe zestawy wczytywane z plików w aplikacji – dostępne także bez zasięgu. */
class PeakRepository private constructor(private val assets: AssetManager) {

    private val mutex = Mutex()
    private var peaks: List<Peak>? = null
    private var sets: List<PeakSet>? = null

    suspend fun getAll(): List<Peak> = mutex.withLock {
        peaks ?: withContext(Dispatchers.IO) {
            // Build debug ma dodatkowy plik z testowym szczytem (src/debug/assets/peaks/).
            assets.list(PEAKS_DIR).orEmpty().filter { it.endsWith(".json") }.flatMap { file ->
                assets.open("$PEAKS_DIR/$file").bufferedReader().use { PeakFileParser.parse(it.readText()) }
            }
        }.also { peaks = it }
    }

    suspend fun getSets(): List<PeakSet> = mutex.withLock {
        sets ?: withContext(Dispatchers.IO) {
            assets.open(SETS_FILE).bufferedReader().use { PeakSetFileParser.parse(it.readText()) }
        }.also { sets = it }
    }

    companion object {
        private const val PEAKS_DIR = "peaks"
        private const val SETS_FILE = "sets/sets.json"

        @Volatile private var instance: PeakRepository? = null

        /** Jedna kopia na aplikację – każdy plik jest czytany raz, nawet gdy korzysta z niego kilka ekranów. */
        fun get(assets: AssetManager): PeakRepository =
            instance ?: synchronized(this) { instance ?: PeakRepository(assets).also { instance = it } }
    }
}
