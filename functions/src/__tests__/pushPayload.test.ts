import { buildDaysUntilReunionPayload, buildPartnerDayReadyPayload } from "../pushPayload";

describe("buildPartnerDayReadyPayload", () => {
  it("includes the type, tripId and date in data", () => {
    const payload = buildPartnerDayReadyPayload("trip-1", "2026-07-16");

    expect(payload.data).toEqual({
      type: "partner-day-ready",
      tripId: "trip-1",
      date: "2026-07-16",
    });
  });

  it("mentions the date in the notification body", () => {
    const payload = buildPartnerDayReadyPayload("trip-1", "2026-07-16");

    expect(payload.notification.body).toContain("2026-07-16");
  });
});

describe("buildDaysUntilReunionPayload", () => {
  it("uses singular phrasing for exactly 1 day remaining", () => {
    const payload = buildDaysUntilReunionPayload(1);

    expect(payload.notification.body).toBe("Залишився 1 день до зустрічі!");
  });

  it("uses plural phrasing for more than 1 day remaining", () => {
    const payload = buildDaysUntilReunionPayload(5);

    expect(payload.notification.body).toContain("5");
  });

  it("announces reunion day itself when zero days remain", () => {
    const payload = buildDaysUntilReunionPayload(0);

    expect(payload.notification.body).toBe("Сьогодні той самий день!");
  });

  it("includes the type and daysRemaining in data", () => {
    const payload = buildDaysUntilReunionPayload(3);

    expect(payload.data).toEqual({
      type: "days-until-reunion",
      daysRemaining: "3",
    });
  });
});
