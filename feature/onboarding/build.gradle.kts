plugins {
    alias(libs.plugins.convention.kmp.library.compose)
    alias(libs.plugins.convention.roborazzi)
    alias(libs.plugins.convention.koin)
}

kotlin {
    android {
        namespace = "com.alongside.feature.onboarding"
    }
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation(projects.core.ui)
            implementation(libs.findLibrary("orbit-core").get())
            implementation(libs.findLibrary("orbit-viewmodel").get())
            implementation(libs.findLibrary("orbit-compose").get())
        }
        commonTest.dependencies {
            implementation(libs.findLibrary("orbit-test").get())
        }
        androidMain.dependencies {
            implementation(libs.findLibrary("activity-compose").get())
        }
        // androidHostTest doesn't automatically see commonMain's `implementation` dependencies
        // (see RoborazziConventionPlugin.kt) - OnboardingScreenNavigationTest/
        // OnboardingPermissionRecoveryTest need core:ui's AlongsideTheme directly.
        getByName("androidHostTest").dependencies {
            implementation(projects.core.ui)
            implementation(libs.findLibrary("orbit-core").get())
            implementation(libs.findLibrary("orbit-viewmodel").get())
            implementation(libs.findLibrary("orbit-compose").get())
        }
    }
}
