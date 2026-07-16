/** Pure unit tests only (payload builders) - no emulator required. Emulator-backed tests run
 * separately via `npm run test:emulator` / jest.emulator.config.js. */
module.exports = {
  preset: "ts-jest",
  testEnvironment: "node",
  testPathIgnorePatterns: ["/node_modules/", "\\.emulator\\.test\\.ts$"],
};
