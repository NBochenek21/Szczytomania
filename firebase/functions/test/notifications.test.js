const test = require("node:test");
const assert = require("node:assert/strict");
const n = require("../notifications");

test("znajomi to tylko zaakceptowane znajomości, bez powtórzeń i bez siebie", () => {
  const friendships = [
    { users: ["me", "ola"], status: "accepted" },
    { users: ["jan", "me"], status: "accepted" },
    { users: ["me", "zaproszony"], status: "pending" },
    { users: ["me", "ola"], status: "accepted" },
    { users: ["obcy", "inny"], status: "accepted" },
    { status: "accepted" },
  ];
  assert.deepEqual(n.acceptedFriendUids(friendships, "me"), ["ola", "jan"]);
});

test("wiadomości mają typ i same pola tekstowe", () => {
  const message = n.conquestMessage({ uid: "u1", nick: "kuba", peakId: "node/4366169405" });
  assert.deepEqual(message, { type: "friend_conquest", uid: "u1", nick: "kuba", peakId: "node/4366169405" });
  assert.equal(n.inviteMessage({ uid: "u1", nick: undefined }).nick, "");
  assert.equal(n.acceptedMessage({ uid: "u2", nick: "ola" }).type, "friend_accepted");
  for (const value of Object.values(message)) assert.equal(typeof value, "string");
});

test("akceptacja to tylko zmiana pending → accepted", () => {
  assert.equal(n.isAcceptance({ status: "pending" }, { status: "accepted" }), true);
  assert.equal(n.isAcceptance({ status: "accepted" }, { status: "accepted" }), false);
  assert.equal(n.isAcceptance(undefined, { status: "accepted" }), false);
});

test("usuwane są tylko tokeny, których FCM już nie zna", () => {
  const tokens = ["ok", "stary", "zly", "chwilowy"];
  const responses = [
    { success: true },
    { success: false, error: { code: "messaging/registration-token-not-registered" } },
    { success: false, error: { code: "messaging/invalid-registration-token" } },
    { success: false, error: { code: "messaging/internal-error" } },
  ];
  assert.deepEqual(n.staleTokens(tokens, responses), ["stary", "zly"]);
});
