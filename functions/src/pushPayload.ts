/**
 * Push payload construction, kept separate from the Firestore trigger so it can be unit-tested
 * without any emulator (per docs/roadmap.md M17: "Юніт-тести побудови payload пуша").
 *
 * Copy below is a placeholder - revisit once feature UI work (M12 Timeline / M20 Recap) settles
 * on real product copy and i18n.
 */

export interface PushPayload {
  notification: {
    title: string;
    body: string;
  };
  data: Record<string, string>;
}

/** Fired once both trip members have a synced diary entry for the same date. */
export function buildPartnerDayReadyPayload(tripId: string, date: string): PushPayload {
  return {
    notification: {
      title: "Обидва щоденники готові",
      body: `Запис за ${date} тепер можна переглянути.`,
    },
    data: {
      type: "partner-day-ready",
      tripId,
      date,
    },
  };
}

/**
 * Countdown-to-reunion reminder. No trigger wires this up yet (docs/backend-plan.md scopes only
 * one Cloud Function - the symmetric-unlock trigger below) - this builder exists so its payload
 * shape can be pinned by a unit test ahead of whatever eventually calls it.
 */
export function buildDaysUntilReunionPayload(daysRemaining: number): PushPayload {
  const body =
    daysRemaining <= 0
      ? "Сьогодні той самий день!"
      : daysRemaining === 1
        ? "Залишився 1 день до зустрічі!"
        : `Залишилось ${daysRemaining} днів до зустрічі!`;
  return {
    notification: {
      title: "Зустріч наближається",
      body,
    },
    data: {
      type: "days-until-reunion",
      daysRemaining: String(daysRemaining),
    },
  };
}
