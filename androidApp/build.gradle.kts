plugins {
    alias(libs.plugins.convention.android.application)
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
    implementation(versionCatalog.findLibrary("activity-compose").get())
}
