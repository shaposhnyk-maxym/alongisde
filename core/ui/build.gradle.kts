plugins {
    alias(libs.plugins.convention.kmp.library.compose)
    alias(libs.plugins.convention.roborazzi)
}

kotlin {
    android {
        namespace = "com.alongside.core.ui"
    }
    // jvm() target is already added by convention.kmp.library; it also
    // doubles as the desktop target so :playground (plain kotlin("jvm") +
    // Compose Desktop, per docs/kmp-module-architecture.md) can depend on
    // this module.
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation(libs.findLibrary("coil-compose").get())
            implementation(libs.findLibrary("coil-network-ktor3").get())
        }
    }
}
