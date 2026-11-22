package pl.szczytomania.notifications

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** Format wiadomości musi zgadzać się z firebase/functions/notifications.js. */
class PushMessageTest {

    @Test
    fun parsesFriendConquest() {
        val data = mapOf("type" to "friend_conquest", "uid" to "u1", "nick" to "kuba", "peakId" to "node/4366169405")
        assertEquals(PushMessage.FriendConquest("u1", "kuba", "node/4366169405"), PushMessage.parse(data))
    }

    @Test
    fun parsesInviteAndAcceptance() {
        assertEquals(PushMessage.FriendInvite("u2", "ola"), PushMessage.parse(mapOf("type" to "friend_invite", "uid" to "u2", "nick" to "ola")))
        assertEquals(PushMessage.FriendAccepted("u3", ""), PushMessage.parse(mapOf("type" to "friend_accepted", "uid" to "u3")))
    }

    @Test
    fun ignoresUnknownOrIncompleteMessages() {
        assertNull(PushMessage.parse(mapOf("type" to "challenge_won", "uid" to "u1")))
        assertNull(PushMessage.parse(mapOf("type" to "friend_invite", "uid" to " ")))
        assertNull(PushMessage.parse(mapOf("type" to "friend_conquest", "uid" to "u1")))
        assertNull(PushMessage.parse(emptyMap()))
    }
}
