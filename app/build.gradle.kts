plugins {
    alias(libs.plugins.convention.kmp.library.compose)
    alias(libs.plugins.convention.koin)
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
        }
        androidMain.dependencies {
            implementation(project.dependencies.platform(libs.findLibrary("firebase-bom").get()))
            implementation(libs.findLibrary("firebase-messaging").get())
        }
    }
}
