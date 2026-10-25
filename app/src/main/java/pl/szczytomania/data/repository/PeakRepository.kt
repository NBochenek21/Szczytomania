package pl.szczytomania.data.repository

import android.content.res.AssetManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import pl.szczytomania.data.model.Peak
import pl.szczytomania.data.peaks.PeakFileParser

/** Szczyty wczytywane z pliku w aplikacji – dostępne także bez zasięgu. */
class PeakRepository(private val assets: AssetManager) {

    private val mutex = Mutex()
    private var cache: List<Peak>? = null

    suspend fun getAll(): List<Peak> = mutex.withLock {
        cache ?: withContext(Dispatchers.IO) {
            assets.open(PEAKS_FILE).bufferedReader().use { PeakFileParser.parse(it.readText()) }
        }.also { cache = it }
    }

    companion object {
        const val PEAKS_FILE = "peaks/peaks.json"
    }
}
