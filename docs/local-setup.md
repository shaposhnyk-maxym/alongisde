# Local setup

## API keys

`local.properties` (gitignored, not committed) must define, in addition to `sdk.dir`:

```properties
GOOGLE_PLACES_API_KEY=your-google-maps-platform-key
GEMINI_API_KEY=your-gemini-api-key
```

Both are read by `androidApp/build.gradle.kts` into `BuildConfig` fields. Unlike Firebase
(`google-services.json`, committed, auto-processed by the Google Services Gradle plugin), these
two have no auto-generated key delivery - without them set, `feature:diary`'s Google Places
geocoding and Gemini vision-description calls fail with an auth error at runtime, but the build
itself still succeeds (the fields default to an empty string).
