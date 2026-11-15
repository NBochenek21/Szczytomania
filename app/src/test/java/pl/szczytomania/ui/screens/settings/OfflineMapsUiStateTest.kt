package pl.szczytomania.ui.screens.settings

import org.junit.Assert.assertEquals
import org.junit.Test
import pl.szczytomania.map.offline.MapRegions
import pl.szczytomania.map.offline.RegionDownload

class OfflineMapsUiStateTest {

    @Test
    fun downloadedFirstThenInProgressThenAvailable() {
        val state = uiState(
            downloads = mapOf(
                "bieszczady" to RegionDownload.InProgress(38_000_000, 0.62f, active = true),
                "karkonosze" to RegionDownload.Done(84_000_000, 0),
            ),
            freeBytes = 0,
        )
        assertEquals(listOf("karkonosze", "bieszczady"), state.regions.take(2).map { it.region.id })
        assertEquals(MapRegions.all.size, state.regions.size)
        assertEquals(RegionDownload.None, state.regions.last().download)
    }

    @Test
    fun usedSpaceCountsDownloadedAndPartialMaps() {
        val state = uiState(
            downloads = mapOf(
                "tatry" to RegionDownload.Done(126_000_000, 0),
                "gorce-pieniny" to RegionDownload.InProgress(38_000_000, 0.5f, active = false),
                "nieznany-region" to RegionDownload.Done(1_000_000, 0),
            ),
            freeBytes = 0,
        )
        assertEquals(164_000_000L, state.usedBytes)
    }

    @Test
    fun megabytesAreRounded() {
        assertEquals(0L, megabytes(0))
        assertEquals(1L, megabytes(500_000))
        assertEquals(84L, megabytes(84_400_000))
    }
}
