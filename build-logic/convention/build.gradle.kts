plugins {
    `kotlin-dsl`
}

group = "com.alongside.buildlogic"

repositories {
    google()
    mavenCentral()
    gradlePluginPortal()
}

dependencies {
    compileOnly(libs.android.gradlePlugin)
    compileOnly(libs.kotlin.gradlePlugin)
    compileOnly(libs.compose.gradlePlugin)
    compileOnly(libs.ksp.gradlePlugin)
    compileOnly(libs.roborazzi.gradlePlugin)
    compileOnly(libs.room.gradlePlugin)
}

gradlePlugin {
    plugins {
        register("kmpLibrary") {
            id = "convention.kmp.library"
            implementationClass = "KmpLibraryPlugin"
        }
        register("kmpLibraryCompose") {
            id = "convention.kmp.library.compose"
            implementationClass = "KmpLibraryComposePlugin"
        }
        register("roborazzi") {
            id = "convention.roborazzi"
            implementationClass = "RoborazziConventionPlugin"
        }
        register("androidApplication") {
            id = "convention.android.application"
            implementationClass = "AndroidApplicationPlugin"
        }
        register("koin") {
            id = "convention.koin"
            implementationClass = "KoinPlugin"
        }
        register("room") {
            id = "convention.room"
            implementationClass = "RoomPlugin"
        }
    }
}
