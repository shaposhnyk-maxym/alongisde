plugins {
    alias(libs.plugins.convention.kmp.library)
    alias(libs.plugins.kotlin.serialization)
}

kotlin {
    android {
        namespace = "com.alongside.core.network"
    }
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation(libs.findLibrary("ktor-client-core").get())
            implementation(libs.findLibrary("ktor-client-content-negotiation").get())
            implementation(libs.findLibrary("ktor-serialization-kotlinx-json").get())
            implementation(libs.findLibrary("ktor-client-logging").get())
            implementation(libs.findLibrary("kotlinx-serialization-json").get())
        }
        androidMain.dependencies {
            implementation(libs.findLibrary("ktor-client-okhttp").get())
        }
        jvmMain.dependencies {
            implementation(libs.findLibrary("ktor-client-okhttp").get())
        }
        iosMain.dependencies {
            implementation(libs.findLibrary("ktor-client-darwin").get())
        }
        jvmTest.dependencies {
            implementation(libs.findLibrary("ktor-client-mock").get())
        }
    }
}
