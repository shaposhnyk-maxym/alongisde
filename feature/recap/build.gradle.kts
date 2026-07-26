plugins {
    alias(libs.plugins.convention.kmp.library.compose)
    alias(libs.plugins.convention.roborazzi)
    alias(libs.plugins.convention.koin)
}

kotlin {
    android {
        namespace = "com.alongside.feature.recap"
    }
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation(projects.core.domain)
            implementation(projects.core.ui)
            implementation(libs.findLibrary("kotlinx-datetime").get())
            implementation(libs.findLibrary("orbit-core").get())
            implementation(libs.findLibrary("orbit-viewmodel").get())
            implementation(libs.findLibrary("orbit-compose").get())
        }
        commonTest.dependencies {
            implementation(libs.findLibrary("orbit-test").get())
        }
        // androidHostTest doesn't automatically see commonMain's `implementation` dependencies
        // (see RoborazziConventionPlugin.kt) - RecapSlideContentTest builds real slide fixtures
        // and drives the screen's slide-content dispatcher, so it needs these directly.
        getByName("androidHostTest").dependencies {
            implementation(projects.core.domain)
            implementation(projects.core.ui)
            implementation(libs.findLibrary("kotlinx-datetime").get())
            implementation(libs.findLibrary("orbit-core").get())
            implementation(libs.findLibrary("orbit-viewmodel").get())
            implementation(libs.findLibrary("orbit-compose").get())
        }
    }
}
