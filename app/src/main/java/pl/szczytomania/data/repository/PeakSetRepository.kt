package pl.szczytomania.data.repository

import android.content.res.AssetManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import pl.szczytomania.data.model.PeakSet
import pl.szczytomania.data.peaks.PeakSetFileParser

/** Gotowe zestawy gór z pliku w aplikacji – dostępne także bez zasięgu. */
class PeakSetRepository(private val assets: AssetManager) {

    private val mutex = Mutex()
    private var cache: List<PeakSet>? = null

    suspend fun getAll(): List<PeakSet> = mutex.withLock {
        cache ?: withContext(Dispatchers.IO) {
            assets.open(SETS_FILE).bufferedReader().use { PeakSetFileParser.parse(it.readText()) }
        }.also { cache = it }
    }

    private companion object {
        const val SETS_FILE = "sets/sets.json"
    }
}
