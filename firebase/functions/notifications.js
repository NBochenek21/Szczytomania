// Logika powiadomień bez Firebase – testy w test/notifications.test.js.
//
// Wysyłamy same dane (data message), bez gotowego tekstu: aplikacja ma plik szczytów, więc sama
// składa treść z nazwą i wysokością szczytu – także gdy szczyt dojdzie w nowszej wersji pliku.
// Wartości w danych FCM muszą być tekstem.

const TYPE = {
  FRIEND_CONQUEST: "friend_conquest",
  FRIEND_INVITE: "friend_invite",
  FRIEND_ACCEPTED: "friend_accepted",
};

// Odpowiedzi FCM, po których token na pewno już nie działa (aplikacja odinstalowana, wylogowanie).
const STALE_TOKEN_ERRORS = new Set([
  "messaging/registration-token-not-registered",
  "messaging/invalid-registration-token",
]);

/** Uid znajomych (zaakceptowane znajomości) użytkownika [uid] z dokumentów `friendships`. */
function acceptedFriendUids(friendships, uid) {
  const friends = friendships
    .filter((f) => f.status === "accepted" && Array.isArray(f.users) && f.users.includes(uid))
    .flatMap((f) => f.users.filter((other) => other !== uid));
  return [...new Set(friends)];
}

function conquestMessage({ uid, nick, peakId }) {
  return { type: TYPE.FRIEND_CONQUEST, uid, nick: nick ?? "", peakId: peakId ?? "" };
}

function inviteMessage({ uid, nick }) {
  return { type: TYPE.FRIEND_INVITE, uid, nick: nick ?? "" };
}

function acceptedMessage({ uid, nick }) {
  return { type: TYPE.FRIEND_ACCEPTED, uid, nick: nick ?? "" };
}

/** Czy zmiana dokumentu znajomości to akceptacja zaproszenia. */
function isAcceptance(before, after) {
  return before?.status === "pending" && after?.status === "accepted";
}

/** Tokeny do usunięcia po wysyłce: [responses] są w tej samej kolejności co [tokens]. */
function staleTokens(tokens, responses) {
  return tokens.filter((_, i) => {
    const response = responses[i];
    return response && !response.success && STALE_TOKEN_ERRORS.has(response.error?.code);
  });
}

module.exports = { TYPE, acceptedFriendUids, conquestMessage, inviteMessage, acceptedMessage, isAcceptance, staleTokens };
