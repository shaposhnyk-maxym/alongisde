plugins {
    alias(libs.plugins.convention.kmp.library)
}

kotlin {
    android {
        namespace = "com.alongside.core.domain"
    }
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            api(projects.core.model)
            implementation(libs.findLibrary("kotlinx-coroutines-core").get())
            implementation(libs.findLibrary("kotlinx-datetime").get())
        }
    }
}
