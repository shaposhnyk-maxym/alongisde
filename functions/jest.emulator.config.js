/** Only the `*.emulator.test.ts` files - these need `firebase emulators:exec` running the
 * Firestore emulator (see the `test:emulator` script in package.json). */
module.exports = {
  preset: "ts-jest",
  testEnvironment: "node",
  testMatch: ["**/__tests__/**/*.emulator.test.ts"],
};
