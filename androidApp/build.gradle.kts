plugins {
    alias(libs.plugins.convention.android.application)
    alias(libs.plugins.google.services)
}

android {
    namespace = "com.alongside.androidapp"
    defaultConfig {
        applicationId = "com.alongside.androidapp"
        versionCode = 1
        versionName = "0.1.0"
    }
}

val versionCatalog = extensions.getByType<org.gradle.api.artifacts.VersionCatalogsExtension>().named("libs")

dependencies {
    implementation(projects.app)
    implementation(projects.feature.auth)
    implementation(projects.feature.onboarding)
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
}
