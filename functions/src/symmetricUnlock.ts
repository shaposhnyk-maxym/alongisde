import type { Change, FirestoreEvent } from "firebase-functions/v2/firestore";
import { onDocumentWritten } from "firebase-functions/v2/firestore";
import type { DocumentSnapshot, Firestore } from "firebase-admin/firestore";
import { FieldValue, getFirestore } from "firebase-admin/firestore";
import { getMessaging, Messaging } from "firebase-admin/messaging";
import { buildPartnerDayReadyPayload } from "./pushPayload";

/**
 * Provisional Firestore document shape for `diaryEntries` - the real client-side mapping from
 * core:model's DiaryEntry to Firestore fields is core:network/data's job (M9), not yet decided.
 * `date` is assumed to be the ISO-8601 calendar date string ("YYYY-MM-DD"); revisit this file if
 * M9 lands on a different representation.
 */
interface DiaryEntryDoc {
  tripId: string;
  userId: string;
  date: string;
}

interface TripDoc {
  ownerId: string;
  memberId: string | null;
}

interface PushTokenDoc {
  token: string;
}

export interface SymmetricUnlockDeps {
  firestore: Firestore;
  messaging: Pick<Messaging, "send">;
}

function defaultDeps(): SymmetricUnlockDeps {
  return { firestore: getFirestore(), messaging: getMessaging() };
}

function partnerIdOf(trip: TripDoc, writerUserId: string): string | null {
  if (writerUserId === trip.ownerId) {
    return trip.memberId;
  }
  if (writerUserId === trip.memberId) {
    return trip.ownerId;
  }
  return null;
}

/**
 * Marks tripId+date as "already notified" exactly once, via a transaction so two near-simultaneous
 * triggers (owner's and partner's writes racing) can't both send. Returns true if a notification
 * was already sent before this call (caller should skip sending again).
 */
async function alreadyNotified(firestore: Firestore, tripId: string, date: string): Promise<boolean> {
  const markerRef = firestore.collection("notifiedDayReady").doc(`${tripId}_${date}`);
  return firestore.runTransaction(async (tx) => {
    const marker = await tx.get(markerRef);
    if (marker.exists) {
      return true;
    }
    tx.set(markerRef, { notifiedAt: FieldValue.serverTimestamp() });
    return false;
  });
}

async function sendToRecipient(deps: SymmetricUnlockDeps, recipientId: string, payload: ReturnType<typeof buildPartnerDayReadyPayload>): Promise<void> {
  const tokenSnap = await deps.firestore.collection("pushTokens").doc(recipientId).get();
  if (!tokenSnap.exists) {
    return;
  }
  const { token } = tokenSnap.data() as PushTokenDoc;
  await deps.messaging.send({ token, notification: payload.notification, data: payload.data });
}

/**
 * Core trigger logic, exported separately from the `onDocumentWritten` wrapper so tests can call
 * it directly against the Firestore emulator (via injected `deps`) without needing to construct a
 * real Cloud Functions event dispatch.
 */
export async function handleDiaryEntryWrite(
  after: DocumentSnapshot | undefined,
  deps: SymmetricUnlockDeps = defaultDeps(),
): Promise<void> {
  if (!after || !after.exists) {
    return; // deletion, or no document - nothing to check
  }
  const entry = after.data() as DiaryEntryDoc;

  const tripSnap = await deps.firestore.collection("trips").doc(entry.tripId).get();
  if (!tripSnap.exists) {
    return;
  }
  const trip = tripSnap.data() as TripDoc;
  const partnerId = partnerIdOf(trip, entry.userId);
  if (!partnerId) {
    return; // writer isn't a recognized trip member, or the trip isn't paired yet
  }

  const partnerEntries = await deps.firestore
    .collection("diaryEntries")
    .where("tripId", "==", entry.tripId)
    .where("userId", "==", partnerId)
    .where("date", "==", entry.date)
    .limit(1)
    .get();
  if (partnerEntries.empty) {
    return; // partner hasn't written their entry for this date yet
  }

  if (await alreadyNotified(deps.firestore, entry.tripId, entry.date)) {
    return; // already sent for this trip+date - don't spam on every subsequent edit
  }

  const payload = buildPartnerDayReadyPayload(entry.tripId, entry.date);
  const recipientIds = [trip.ownerId, trip.memberId].filter((id): id is string => Boolean(id));
  await Promise.all(recipientIds.map((recipientId) => sendToRecipient(deps, recipientId, payload)));
}

export const onDiaryEntryWritten = onDocumentWritten(
  "diaryEntries/{diaryEntryId}",
  (event: FirestoreEvent<Change<DocumentSnapshot> | undefined, { diaryEntryId: string }>) =>
    handleDiaryEntryWrite(event.data?.after),
);
