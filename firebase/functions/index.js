// Cloud Functions Szczytomanii: powiadomienia push (Firebase Cloud Messaging).
// Wdrożenie (z katalogu głównego repozytorium): firebase deploy --only functions
//
// Funkcje reagują na zapisy w Firestore i wysyłają powiadomienie na telefony zainteresowanych osób.
// Tokeny telefonów są w users/{uid}/devices/{token} – zapisuje je aplikacja (notifications/PushTokens.kt).

const { onDocumentCreated, onDocumentUpdated } = require("firebase-functions/v2/firestore");
const { setGlobalOptions } = require("firebase-functions/v2");
const logger = require("firebase-functions/logger");
const { initializeApp } = require("firebase-admin/app");
const { getFirestore } = require("firebase-admin/firestore");
const { getMessaging } = require("firebase-admin/messaging");
const notifications = require("./notifications");

initializeApp();

// Region musi pasować do lokalizacji bazy Firestore (konsola: Firestore Database → Ustawienia).
// us-central1 pasuje do baz w us-central1 i nam5. Przy bazie w Europie (eur3) – europe-west1.
// maxInstances – zabezpieczenie przed kosztami przy błędzie w pętli.
setGlobalOptions({ region: "us-central1", maxInstances: 5 });

async function nickOf(uid) {
  const doc = await getFirestore().collection("users").doc(uid).get();
  return doc.get("nick") ?? "";
}

/** Wysyła [data] na wszystkie telefony osób [uids] i usuwa tokeny, które już nie działają. */
async function sendToUsers(uids, data) {
  const db = getFirestore();
  const snapshots = await Promise.all(uids.map((uid) => db.collection("users").doc(uid).collection("devices").get()));
  const devices = snapshots.flatMap((snapshot) => snapshot.docs);
  if (devices.length === 0) return;

  // Id dokumentu to token. Limit FCM to 500 tokenów na wysyłkę – przy kilku znajomych z zapasem.
  const tokens = devices.map((device) => device.id);
  const result = await getMessaging().sendEachForMulticast({
    tokens,
    data,
    // Wysoki priorytet: wiadomość z samymi danymi dociera też, gdy telefon jest uśpiony.
    android: { priority: "high" },
  });

  const stale = new Set(notifications.staleTokens(tokens, result.responses));
  await Promise.all(devices.filter((device) => stale.has(device.id)).map((device) => device.ref.delete()));
  logger.info(`${data.type}: wysłano ${result.successCount}/${tokens.length}, usunięto nieaktualnych tokenów: ${stale.size}`);
}

// Znajomy zdobył szczyt. Zdobycz zapisana w górach bez zasięgu trafia na serwer później –
// wtedy też dopiero idzie powiadomienie.
exports.onConquestCreated = onDocumentCreated("users/{uid}/conquests/{conquestId}", async (event) => {
  const conquest = event.data?.data();
  if (!conquest) return;
  const uid = event.params.uid;

  // Bez filtra po statusie: zapytanie array-contains + status wymagałoby dodatkowego indeksu.
  const friendships = await getFirestore().collection("friendships").where("users", "array-contains", uid).get();
  const friends = notifications.acceptedFriendUids(friendships.docs.map((doc) => doc.data()), uid);
  if (friends.length === 0) return;

  await sendToUsers(friends, notifications.conquestMessage({ uid, nick: await nickOf(uid), peakId: conquest.peakId }));
});

// Ktoś zaprasza do znajomych – powiadomienie dla zaproszonego.
exports.onFriendInvite = onDocumentCreated("friendships/{friendshipId}", async (event) => {
  const friendship = event.data?.data();
  if (!friendship || friendship.status !== "pending") return;
  await sendToUsers([friendship.to], notifications.inviteMessage({ uid: friendship.from, nick: await nickOf(friendship.from) }));
});

// Zaproszenie przyjęte – powiadomienie dla osoby, która zapraszała.
exports.onFriendAccepted = onDocumentUpdated("friendships/{friendshipId}", async (event) => {
  const before = event.data?.before.data();
  const after = event.data?.after.data();
  if (!notifications.isAcceptance(before, after)) return;
  await sendToUsers([after.from], notifications.acceptedMessage({ uid: after.to, nick: await nickOf(after.to) }));
});
