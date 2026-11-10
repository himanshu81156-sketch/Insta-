const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const ALICE_UID = "alice_123";
const BOB_UID = "bob_456";
const CHARLIE_UID = "charlie_789";

const [emulatorHost, emulatorPortStr] = (process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085").split(":");
const emulatorPort = parseInt(emulatorPortStr, 10);

before(async () => {
  const rules = fs.readFileSync("./firestore.rules", "utf8");
  testEnv = await initializeTestEnvironment({
    projectId: PROJECT_ID,
    firestore: {
      rules,
      host: emulatorHost,
      port: emulatorPort,
    },
  });
});

after(async () => {
  if (testEnv) {
    await testEnv.cleanup();
  }
});

beforeEach(async () => {
  if (testEnv) {
    await testEnv.clearFirestore();
  }
});

test("Unauthenticated user: cannot read users or posts", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthDb.collection("users").get());
  await assertFails(unauthDb.collection("posts").get());
});

test("Authenticated user: can create own profile, cannot spoof another user profile", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const ts = new Date();

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).set({
      userId: ALICE_UID,
      username: "alice_insta",
      displayName: "Alice",
      bio: "Hello Mera Insta",
      isOnline: true,
      createdAt: ts,
      updatedAt: ts,
    })
  );

  await assertFails(
    aliceDb.collection("users").doc(BOB_UID).set({
      userId: BOB_UID,
      username: "bob_fake",
      displayName: "Fake Bob",
      isOnline: true,
      createdAt: ts,
      updatedAt: ts,
    })
  );
});

test("Authenticated user: follow system requires valid target user and matching followerId", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const ts = new Date();

  await assertSucceeds(
    bobDb.collection("users").doc(BOB_UID).set({
      userId: BOB_UID,
      username: "bob_insta",
      displayName: "Bob",
      isOnline: true,
      createdAt: ts,
      updatedAt: ts,
    })
  );

  await assertSucceeds(
    aliceDb.collection("follows").doc(`${ALICE_UID}_${BOB_UID}`).set({
      followerId: ALICE_UID,
      followingId: BOB_UID,
      createdAt: ts,
    })
  );

  // Cannot create follow for someone else
  await assertFails(
    aliceDb.collection("follows").doc(`${BOB_UID}_${ALICE_UID}`).set({
      followerId: BOB_UID,
      followingId: ALICE_UID,
      createdAt: ts,
    })
  );
});

test("Notifications: only recipient can list/read their notifications", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const charlieDb = testEnv.authenticatedContext(CHARLIE_UID).firestore();
  const ts = new Date();

  await assertSucceeds(
    aliceDb.collection("notifications").doc("notif_1").set({
      recipientId: BOB_UID,
      senderId: ALICE_UID,
      senderUsername: "alice_insta",
      type: "follow",
      message: "alice_insta ne tumhe Follow kiya",
      isRead: false,
      createdAt: ts,
    })
  );

  await assertSucceeds(
    bobDb.collection("notifications").where("recipientId", "==", BOB_UID).get()
  );

  // Charlie cannot read Bob's notifications
  await assertFails(
    charlieDb.collection("notifications").doc("notif_1").get()
  );
});

test("Chats: only participants can read/write messages", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const charlieDb = testEnv.authenticatedContext(CHARLIE_UID).firestore();
  const ts = new Date();
  const chatId = `${ALICE_UID}_${BOB_UID}`;

  await assertSucceeds(
    aliceDb.collection("chats").doc(chatId).set({
      participantIds: [ALICE_UID, BOB_UID],
      lastMessage: "Namaste!",
      lastSenderId: ALICE_UID,
      typingUserIds: [],
      updatedAt: ts,
    })
  );

  await assertSucceeds(
    aliceDb.collection("chats").doc(chatId).collection("messages").doc("msg_1").set({
      senderId: ALICE_UID,
      recipientId: BOB_UID,
      participantIds: [ALICE_UID, BOB_UID],
      type: "text",
      text: "Namaste!",
      isSeen: false,
      createdAt: ts,
    })
  );

  await assertFails(
    charlieDb.collection("chats").doc(chatId).get()
  );
});
