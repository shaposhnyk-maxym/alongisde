plugins {
    alias(libs.plugins.convention.kmp.library)
}

kotlin {
    android {
        namespace = "com.alongside.data"
    }
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation(projects.core.domain)
            implementation(projects.core.model)
            implementation(projects.core.database)
            implementation(projects.core.network)
            implementation(libs.findLibrary("kotlinx-datetime").get())
            implementation(libs.findLibrary("kotlinx-serialization-json").get())
            implementation(libs.findLibrary("koin-core").get())
        }
        jvmTest.dependencies {
            implementation(libs.findLibrary("room-runtime").get())
            implementation(libs.findLibrary("sqlite-bundled").get())
            implementation(libs.findLibrary("ktor-client-core").get())
            implementation(libs.findLibrary("ktor-client-mock").get())
        }
    }
}
