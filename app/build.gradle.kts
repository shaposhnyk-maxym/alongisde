plugins {
    alias(libs.plugins.convention.kmp.library.compose)
    alias(libs.plugins.convention.koin)
    // NavKeys are @Serializable so rememberNavBackStack can save/restore them across
    // process death.
    alias(libs.plugins.kotlin.serialization)
}

kotlin {
    android {
        namespace = "com.alongside.app"
    }
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation(projects.core.domain)
            implementation(projects.core.ui)
            implementation(projects.feature.auth)
            implementation(projects.feature.onboarding)
            implementation(projects.feature.pairing)
            implementation(libs.findLibrary("navigation3-runtime").get())
            implementation(libs.findLibrary("orbit-core").get())
            implementation(libs.findLibrary("orbit-viewmodel").get())
            implementation(libs.findLibrary("orbit-compose").get())
        }
        androidMain.dependencies {
            implementation(project.dependencies.platform(libs.findLibrary("firebase-bom").get()))
            implementation(libs.findLibrary("firebase-messaging").get())
            // navigation3-ui (NavDisplay with animations/predictive back) ships no iOS
            // artifacts at 1.1.0-alpha01 - Android gets the real NavDisplay, the other
            // targets fall back to AlongsideNavDisplay's plain top-entry renderer.
            implementation(libs.findLibrary("navigation3-ui").get())
        }
    }
}
