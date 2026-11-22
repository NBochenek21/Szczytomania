package pl.szczytomania.map.offline

import org.junit.Assert.assertEquals
import org.junit.Test

class OfflineHintTest {

    // Rysy (Tatry) i Tarnica (Bieszczady).
    private val rysy = 49.1796 to 20.0881
    private val tarnica = 49.0750 to 22.7270

    @Test
    fun nothingWhenOnline() {
        assertEquals(OfflineHint.NONE, offlineHint(true, rysy.first, rysy.second, 12.0, emptySet()))
    }

    @Test
    fun downloadedAndMissingAreas() {
        val downloaded = setOf("tatry")
        assertEquals(OfflineHint.AREA_DOWNLOADED, offlineHint(false, rysy.first, rysy.second, 12.0, downloaded))
        assertEquals(OfflineHint.AREA_MISSING, offlineHint(false, tarnica.first, tarnica.second, 12.0, downloaded))
    }

    @Test
    fun generalHintWhenZoomedOutOrOutsideMountains() {
        assertEquals(OfflineHint.OFFLINE, offlineHint(false, tarnica.first, tarnica.second, 6.0, emptySet()))
        // Warszawa – poza regionami gór.
        assertEquals(OfflineHint.OFFLINE, offlineHint(false, 52.23, 21.01, 12.0, emptySet()))
        assertEquals(OfflineHint.OFFLINE, offlineHint(false, null, null, null, emptySet()))
    }
}
