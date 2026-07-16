import { deleteApp, initializeApp } from "firebase-admin/app";
import type { Firestore } from "firebase-admin/firestore";
import { getFirestore } from "firebase-admin/firestore";
import type { Messaging } from "firebase-admin/messaging";
import { handleDiaryEntryWrite, SymmetricUnlockDeps } from "../symmetricUnlock";

process.env.FIRESTORE_EMULATOR_HOST = process.env.FIRESTORE_EMULATOR_HOST ?? "127.0.0.1:8080";

const app = initializeApp({ projectId: "demo-alongside" });
const firestore: Firestore = getFirestore(app);

afterAll(async () => {
  await deleteApp(app);
});

function fakeMessaging(): Pick<Messaging, "send"> & { send: jest.Mock } {
  return { send: jest.fn().mockResolvedValue("message-id") } as unknown as Pick<Messaging, "send"> & {
    send: jest.Mock;
  };
}

async function clearFirestore(): Promise<void> {
  const collections = ["trips", "diaryEntries", "pushTokens", "notifiedDayReady"];
  for (const name of collections) {
    const snap = await firestore.collection(name).get();
    await Promise.all(snap.docs.map((doc) => doc.ref.delete()));
  }
}

describe("handleDiaryEntryWrite (Firestore emulator)", () => {
  beforeEach(async () => {
    await clearFirestore();
  });

  it("does nothing when the partner has no entry for that date yet", async () => {
    await firestore.collection("trips").doc("trip-1").set({ ownerId: "owner-1", memberId: "member-1" });
    const entryRef = firestore.collection("diaryEntries").doc("entry-owner");
    await entryRef.set({ tripId: "trip-1", userId: "owner-1", date: "2026-07-16" });
    const after = await entryRef.get();
    const messaging = fakeMessaging();
    const deps: SymmetricUnlockDeps = { firestore, messaging };

    await handleDiaryEntryWrite(after, deps);

    expect(messaging.send).not.toHaveBeenCalled();
  });

  it("notifies both members once both have an entry for the same date", async () => {
    await firestore.collection("trips").doc("trip-1").set({ ownerId: "owner-1", memberId: "member-1" });
    await firestore.collection("pushTokens").doc("owner-1").set({ token: "token-owner" });
    await firestore.collection("pushTokens").doc("member-1").set({ token: "token-member" });
    await firestore
      .collection("diaryEntries")
      .doc("entry-owner")
      .set({ tripId: "trip-1", userId: "owner-1", date: "2026-07-16" });
    const memberEntryRef = firestore.collection("diaryEntries").doc("entry-member");
    await memberEntryRef.set({ tripId: "trip-1", userId: "member-1", date: "2026-07-16" });
    const after = await memberEntryRef.get();
    const messaging = fakeMessaging();
    const deps: SymmetricUnlockDeps = { firestore, messaging };

    await handleDiaryEntryWrite(after, deps);

    expect(messaging.send).toHaveBeenCalledTimes(2);
    const sentTokens = messaging.send.mock.calls.map((call) => call[0].token).sort();
    expect(sentTokens).toEqual(["token-member", "token-owner"]);
  });

  it("does not send a duplicate notification for a second write to the same trip+date", async () => {
    await firestore.collection("trips").doc("trip-1").set({ ownerId: "owner-1", memberId: "member-1" });
    await firestore.collection("pushTokens").doc("owner-1").set({ token: "token-owner" });
    await firestore.collection("pushTokens").doc("member-1").set({ token: "token-member" });
    await firestore
      .collection("diaryEntries")
      .doc("entry-owner")
      .set({ tripId: "trip-1", userId: "owner-1", date: "2026-07-16" });
    const memberEntryRef = firestore.collection("diaryEntries").doc("entry-member");
    await memberEntryRef.set({ tripId: "trip-1", userId: "member-1", date: "2026-07-16" });
    const messaging = fakeMessaging();
    const deps: SymmetricUnlockDeps = { firestore, messaging };
    await handleDiaryEntryWrite(await memberEntryRef.get(), deps);

    // Partner edits their entry again (still the same trip+date) - must not re-notify.
    await memberEntryRef.set({ note: "edited" }, { merge: true });
    await handleDiaryEntryWrite(await memberEntryRef.get(), deps);

    expect(messaging.send).toHaveBeenCalledTimes(2); // still just the original 2, not 4
  });

  it("skips a recipient with no registered push token", async () => {
    await firestore.collection("trips").doc("trip-1").set({ ownerId: "owner-1", memberId: "member-1" });
    await firestore.collection("pushTokens").doc("owner-1").set({ token: "token-owner" });
    // member-1 deliberately has no push token document.
    await firestore
      .collection("diaryEntries")
      .doc("entry-owner")
      .set({ tripId: "trip-1", userId: "owner-1", date: "2026-07-16" });
    const memberEntryRef = firestore.collection("diaryEntries").doc("entry-member");
    await memberEntryRef.set({ tripId: "trip-1", userId: "member-1", date: "2026-07-16" });
    const after = await memberEntryRef.get();
    const messaging = fakeMessaging();
    const deps: SymmetricUnlockDeps = { firestore, messaging };

    await handleDiaryEntryWrite(after, deps);

    expect(messaging.send).toHaveBeenCalledTimes(1);
    expect(messaging.send.mock.calls[0][0].token).toBe("token-owner");
  });

  it("does nothing for a deleted document", async () => {
    const messaging = fakeMessaging();
    const deps: SymmetricUnlockDeps = { firestore, messaging };

    await handleDiaryEntryWrite(undefined, deps);

    expect(messaging.send).not.toHaveBeenCalled();
  });

  it("does nothing when the writer is not a recognized trip member", async () => {
    await firestore.collection("trips").doc("trip-1").set({ ownerId: "owner-1", memberId: "member-1" });
    const entryRef = firestore.collection("diaryEntries").doc("entry-stranger");
    await entryRef.set({ tripId: "trip-1", userId: "stranger-1", date: "2026-07-16" });
    const after = await entryRef.get();
    const messaging = fakeMessaging();
    const deps: SymmetricUnlockDeps = { firestore, messaging };

    await handleDiaryEntryWrite(after, deps);

    expect(messaging.send).not.toHaveBeenCalled();
  });
});
