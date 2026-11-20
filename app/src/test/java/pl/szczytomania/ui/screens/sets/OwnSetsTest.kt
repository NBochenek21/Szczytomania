package pl.szczytomania.ui.screens.sets

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import pl.szczytomania.data.firestore.OwnSetRepository
import pl.szczytomania.data.model.Peak
import pl.szczytomania.data.model.PeakSet
import pl.szczytomania.ui.screens.profile.isValidNick
import pl.szczytomania.ui.screens.profile.isValidPassword

class OwnSetsTest {

    private val peaks = listOf(
        Peak("node/1", "Turbacz", 1310, 49.54, 20.11, "Gorce"),
        Peak("node/2", "Mogielica", 1171, 49.65, 20.27, "Beskid Wyspowy"),
        Peak("node/3", "Lubań", 1211, 49.48, 20.33, "Gorce"),
        Peak("node/4", "Skrzyczne", 1257, 49.68, 19.03, "Beskid Śląski"),
        Peak("node/5", "Pilsko", 1557, 49.54, 19.32, "Beskid Żywiecki"),
        Peak("node/6", "Gorc", 1228, 49.56, 20.25, "Gorce"),
    ).associateBy { it.id }

    @Test
    fun selectedPeaksAreSavedFromHighestAndUnknownSkipped() {
        assertEquals(listOf("node/1", "node/3", "node/2"), orderedForSet(setOf("node/2", "node/1", "node/999", "node/3"), peaks))
    }

    @Test
    fun setNameMustHave1To40Characters() {
        assertTrue(isValidSetName("Beskidzkie weekendy"))
        assertFalse(isValidSetName("   "))
        assertFalse(isValidSetName("x".repeat(41)))
    }

    @Test
    fun previewShowsFourNamesAndHowManyMore() {
        val set = PeakSet("own:a", "Moje", "", peaks.keys.toList())
        val (names, more) = SetProgress.of(set, peaks, emptySet()).previewNames()
        assertEquals("Turbacz, Mogielica, Lubań, Skrzyczne", names)
        assertEquals(2, more)
    }

    @Test
    fun ownSetIdsAreRecognised() {
        assertTrue(OwnSetRepository.isOwn("own:abc123"))
        assertFalse(OwnSetRepository.isOwn("kgp"))
        assertEquals("abc123", OwnSetRepository.documentId("own:abc123"))
    }

    @Test
    fun sharedSetsAreFoundAndFriendsNamed() {
        val shared = SetProgress.of(PeakSet("own:x", "Od Jakuba", "", listOf("node/1")), peaks, emptySet())
        val state = SetsUiState(
            isLoading = false,
            sharedWithMe = listOf(shared),
            sharing = mapOf("own:x" to SetSharing(ownerId = "uidJ", sharedWith = listOf("me"), mine = false)),
            friends = listOf("uidJ" to "Jakub"),
        )
        assertEquals("Od Jakuba", state.set("own:x")?.set?.name)
        assertEquals("Jakub", state.nick("uidJ"))
        assertEquals(null, state.nick("obcy"))
    }

    @Test
    fun accountValidationMatchesRegistrationRules() {
        assertTrue(isValidNick("mik"))
        assertFalse(isValidNick("ab"))
        assertFalse(isValidNick("x".repeat(21)))
        assertTrue(isValidPassword("12345678"))
        assertFalse(isValidPassword("1234567"))
    }
}
