plugins {
    alias(libs.plugins.convention.kmp.library.compose)
    alias(libs.plugins.convention.roborazzi)
    alias(libs.plugins.convention.koin)
}

kotlin {
    android {
        namespace = "com.alongside.feature.matcher"
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
        // (see RoborazziConventionPlugin.kt), but it does compile commonTest sources - Orbit's
        // types need to be resolvable there too, same as feature:pairing/feature:places already
        // document.
        getByName("androidHostTest").dependencies {
            implementation(projects.core.domain)
            implementation(projects.core.ui)
            implementation(libs.findLibrary("orbit-core").get())
            implementation(libs.findLibrary("orbit-viewmodel").get())
            implementation(libs.findLibrary("orbit-compose").get())
            implementation(libs.findLibrary("orbit-test").get())
            implementation(libs.findLibrary("kotlinx-datetime").get())
        }
    }
}
