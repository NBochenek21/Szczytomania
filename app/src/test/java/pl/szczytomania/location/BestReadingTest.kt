package pl.szczytomania.location

import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

class BestReadingTest {

    private fun reading(accuracy: Float, isMock: Boolean = false) = GpsReading(50.0, 20.0, accuracy, isMock)

    @Test
    fun skipsInaccurateNetworkFixes() = runBlocking {
        val best = flowOf(reading(120f), reading(60f), reading(12f), reading(5f)).bestReading(1.seconds)
        assertEquals(12f, best?.accuracyMeters)
    }

    @Test
    fun returnsBestReadingAfterTimeout() = runBlocking {
        val updates = flow {
            emit(reading(120f))
            emit(reading(70f))
            emit(reading(90f))
            awaitCancellation()
        }
        assertEquals(70f, updates.bestReading(50.milliseconds)?.accuracyMeters)
    }

    @Test
    fun mockedReadingIsNotHiddenByABetterOne() = runBlocking {
        val best = flowOf(reading(120f), reading(200f, isMock = true), reading(5f)).bestReading(1.seconds)
        assertEquals(true, best?.isMock)
    }

    @Test
    fun noReadingsMeansNoSignal() = runBlocking {
        val silent = flow<GpsReading> { awaitCancellation() }
        assertNull(silent.bestReading(50.milliseconds))
    }

    @Test
    fun failedLocationServiceMeansNoSignal() = runBlocking {
        assertNull(emptyFlow<GpsReading>().bestReading(1.seconds))
    }
}
