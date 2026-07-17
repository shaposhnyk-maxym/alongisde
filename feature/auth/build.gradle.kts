plugins {
    alias(libs.plugins.convention.kmp.library.compose)
    alias(libs.plugins.convention.roborazzi)
    alias(libs.plugins.convention.koin)
}

kotlin {
    android {
        namespace = "com.alongside.feature.auth"
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
        }
        androidMain.dependencies {
            implementation(libs.findLibrary("androidx-credentials").get())
            implementation(libs.findLibrary("androidx-credentials-play-services-auth").get())
            implementation(libs.findLibrary("googleid").get())
            implementation(libs.findLibrary("kotlinx-coroutines-android").get())
        }
        // androidHostTest doesn't automatically see commonMain's `implementation` dependencies
        // (see RoborazziConventionPlugin.kt) - AuthContainerTest/FakeAuthSessionRepository need
        // core:domain's (and, transitively via its api(), core:model's) auth types directly.
        getByName("androidHostTest").dependencies {
            implementation(projects.core.domain)
        }
    }
}
