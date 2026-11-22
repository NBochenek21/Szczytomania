package pl.szczytomania.notifications

/**
 * Wiadomość push od Cloud Function (firebase/functions/notifications.js). Przychodzą same dane –
 * tekst powiadomienia składa aplikacja, bo ma plik szczytów (nazwa i wysokość szczytu).
 */
sealed interface PushMessage {
    /** Uid znajomego, którego dotyczy powiadomienie – po dotknięciu otwiera się jego profil. */
    val friendUid: String
    val nick: String

    data class FriendConquest(override val friendUid: String, override val nick: String, val peakId: String) : PushMessage
    data class FriendInvite(override val friendUid: String, override val nick: String) : PushMessage
    data class FriendAccepted(override val friendUid: String, override val nick: String) : PushMessage

    companion object {
        /** null – wiadomość nieznanego typu (np. z nowszej wersji funkcji) albo bez nadawcy. */
        fun parse(data: Map<String, String>): PushMessage? {
            val uid = data["uid"]?.takeIf { it.isNotBlank() } ?: return null
            val nick = data["nick"].orEmpty()
            return when (data["type"]) {
                "friend_conquest" -> FriendConquest(uid, nick, data["peakId"]?.takeIf { it.isNotBlank() } ?: return null)
                "friend_invite" -> FriendInvite(uid, nick)
                "friend_accepted" -> FriendAccepted(uid, nick)
                else -> null
            }
        }
    }
}
