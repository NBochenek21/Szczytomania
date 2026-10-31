package pl.szczytomania.data.repository

import android.content.res.AssetManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import pl.szczytomania.data.model.Peak
import pl.szczytomania.data.peaks.PeakFileParser

/** Szczyty wczytywane z plików w aplikacji – dostępne także bez zasięgu. */
class PeakRepository(private val assets: AssetManager) {

    private val mutex = Mutex()
    private var cache: List<Peak>? = null

    suspend fun getAll(): List<Peak> = mutex.withLock {
        cache ?: withContext(Dispatchers.IO) {
            // Build debug ma dodatkowy plik z testowym szczytem (src/debug/assets/peaks/).
            assets.list(PEAKS_DIR).orEmpty().filter { it.endsWith(".json") }.flatMap { file ->
                assets.open("$PEAKS_DIR/$file").bufferedReader().use { PeakFileParser.parse(it.readText()) }
            }
        }.also { cache = it }
    }

    private companion object {
        const val PEAKS_DIR = "peaks"
    }
}
