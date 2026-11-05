package pl.szczytomania.data.firestore

import org.junit.Assert.assertEquals
import org.junit.Test

class ConquestRepositoryTest {

    // Ta sama zamiana jest w firebase/firestore.rules – inna wersja tutaj oznacza odrzucone zapisy.
    @Test
    fun documentIdReplacesSlash() {
        assertEquals("node_4366169405", ConquestRepository.documentId("node/4366169405"))
        assertEquals("way_123", ConquestRepository.documentId("way/123"))
        assertEquals("test_agh", ConquestRepository.documentId("test/agh"))
    }
}
