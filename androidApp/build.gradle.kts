import java.util.Properties

plugins {
    alias(libs.plugins.convention.android.application)
    alias(libs.plugins.google.services)
}

// Unlike Firebase (google-services.json, committed, auto-processed), Google Places and Gemini
// have no auto-generated key delivery - both read from local.properties (gitignored, not committed).
// See docs/local-setup.md for the keys this project expects there.
val localProperties =
    Properties().apply {
        val file = rootProject.file("local.properties")
        if (file.exists()) file.inputStream().use { load(it) }
    }

android {
    namespace = "com.alongside.androidapp"
    defaultConfig {
        applicationId = "com.alongside.androidapp"
        versionCode = 1
        versionName = "0.1.0"
        buildConfigField(
            "String",
            "GOOGLE_PLACES_API_KEY",
            "\"${localProperties.getProperty("GOOGLE_PLACES_API_KEY", "")}\"",
        )
        buildConfigField("String", "GEMINI_API_KEY", "\"${localProperties.getProperty("GEMINI_API_KEY", "")}\"")
    }
    buildFeatures {
        buildConfig = true
    }
    // First test source set this module has ever needed (previously a thin, test-free
    // composition root - see docs/roadmap.md M13.2) - Robolectric needs the merged manifest's
    // resources to resolve the ACTION_SEND intent-filter against a real PackageManager.
    testOptions {
        unitTests {
            isIncludeAndroidResources = true
        }
    }
}

val versionCatalog = extensions.getByType<org.gradle.api.artifacts.VersionCatalogsExtension>().named("libs")

dependencies {
    implementation(projects.app)
    implementation(projects.data)
    implementation(projects.feature.auth)
    implementation(projects.feature.onboarding)
    implementation(projects.feature.pairing)
    implementation(projects.feature.diary)
    implementation(projects.feature.places)
    implementation(projects.core.network)
    implementation(projects.core.database)
    implementation(projects.core.domain)
    implementation(projects.core.ui)
    implementation(versionCatalog.findLibrary("activity-compose").get())
    implementation(versionCatalog.findLibrary("koin-core").get())
    implementation(versionCatalog.findLibrary("koin-android").get())
    implementation(versionCatalog.findLibrary("koin-compose-viewmodel").get())
    implementation(versionCatalog.findLibrary("ktor-client-core").get())
    implementation(versionCatalog.findLibrary("room-runtime").get())
    implementation(versionCatalog.findLibrary("androidx-work-runtime-ktx").get())
    testImplementation(versionCatalog.findLibrary("junit").get())
    testImplementation(versionCatalog.findLibrary("robolectric").get())
    testImplementation(versionCatalog.findLibrary("androidx-work-testing").get())
    testImplementation(versionCatalog.findLibrary("kotlinx-datetime").get())
}
