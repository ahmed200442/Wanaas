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
const ADMIN_UID = "owner_admin_789";

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

test("Unauthenticated user: cannot read or create chat rooms, members, or fadfada posts", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthDb.collection("chat_rooms").get());
  await assertFails(unauthDb.collection("members").get());
  await assertFails(unauthDb.collection("fadfada_posts").get());
});

test("Authenticated user: can create room, sync Supabase member profile, enter room, and publish presence event", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const now = new Date(Date.now() - 1000);

  await assertSucceeds(
    aliceDb.collection("members").doc(ALICE_UID).set({
      userId: ALICE_UID,
      displayName: "أحمد المصري",
      avatarEmoji: "👑",
      roleBadge: "VIP عضو",
      supabaseSynced: true,
      updatedAt: now,
    })
  );

  await assertSucceeds(
    aliceDb.collection("chat_rooms").doc("room_alice").set({
      roomId: "room_alice",
      roomName: "Alice Voice Lounge",
      creatorId: ALICE_UID,
      timestamp: now,
    })
  );

  await assertSucceeds(
    aliceDb
      .collection("chat_rooms")
      .doc("room_alice")
      .collection("active_members")
      .doc(ALICE_UID)
      .set({
        userId: ALICE_UID,
        roomId: "room_alice",
        memberName: "أحمد المصري",
        avatarEmoji: "👑",
        roleBadge: "VIP عضو",
        supabaseLinked: true,
        joinedAt: now,
      })
  );

  await assertSucceeds(
    aliceDb
      .collection("chat_rooms")
      .doc("room_alice")
      .collection("presence_events")
      .doc("evt_1")
      .set({
        eventId: "evt_1",
        roomId: "room_alice",
        userId: ALICE_UID,
        memberName: "أحمد المصري",
        avatarEmoji: "👑",
        eventType: "JOIN",
        createdAt: now,
      })
  );
});

test("Anonymous Fadfada: regular members can read anonymous post but CANNOT read author name/email; App Owner Admin CAN read author name/email", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const ownerDb = testEnv
    .authenticatedContext(ADMIN_UID, { email: "hamadanagy1979@gmail.com" })
    .firestore();
  const now = new Date(Date.now() - 1000);

  // Alice creates anonymous fadfada post (without name or email)
  await assertSucceeds(
    aliceDb.collection("fadfada_posts").doc("fadfada_1").set({
      postId: "fadfada_1",
      authorId: ALICE_UID,
      content: "يا رب فرج قريب وراحة بال",
      moodTag: "💭 فضفضة عامة",
      heartsCount: 0,
      timestamp: now,
    })
  );

  // Alice submits her real name and email to the admin-only identity collection
  await assertSucceeds(
    aliceDb.collection("fadfada_admin_identities").doc("fadfada_1").set({
      postId: "fadfada_1",
      authorId: ALICE_UID,
      authorName: "أحمد المصري",
      authorEmail: "alice@example.com",
      timestamp: now,
    })
  );

  // Bob (regular member) CAN read the anonymous post
  await assertSucceeds(bobDb.collection("fadfada_posts").doc("fadfada_1").get());

  // Bob (regular member) CANNOT read or list the author's name and email from fadfada_admin_identities
  await assertFails(
    bobDb.collection("fadfada_admin_identities").doc("fadfada_1").get()
  );
  await assertFails(bobDb.collection("fadfada_admin_identities").get());

  // App Owner Admin CAN read and list the author's name and email from fadfada_admin_identities
  await assertSucceeds(
    ownerDb.collection("fadfada_admin_identities").doc("fadfada_1").get()
  );
  await assertSucceeds(ownerDb.collection("fadfada_admin_identities").get());
});
