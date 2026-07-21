plugins {
    alias(libs.plugins.convention.kmp.library.compose)
    alias(libs.plugins.convention.roborazzi)
    alias(libs.plugins.convention.koin)
}

kotlin {
    android {
        namespace = "com.alongside.feature.places"
    }
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation(projects.core.domain)
            implementation(projects.core.ui)
            implementation(libs.findLibrary("orbit-core").get())
            implementation(libs.findLibrary("orbit-viewmodel").get())
            implementation(libs.findLibrary("orbit-compose").get())
        }
        commonTest.dependencies {
            implementation(libs.findLibrary("orbit-test").get())
            implementation(libs.findLibrary("kotlinx-datetime").get())
        }
        // androidHostTest doesn't automatically see commonMain's `implementation` dependencies
        // (see RoborazziConventionPlugin.kt) - a Robolectric navigation test would need these
        // directly, same as feature:pairing's build file already documents.
        getByName("androidHostTest").dependencies {
            implementation(projects.core.domain)
            implementation(projects.core.ui)
            implementation(libs.findLibrary("orbit-core").get())
            implementation(libs.findLibrary("orbit-viewmodel").get())
            implementation(libs.findLibrary("orbit-compose").get())
        }
    }
}
