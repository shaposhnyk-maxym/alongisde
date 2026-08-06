import org.jetbrains.kotlin.gradle.plugin.mpp.NativeBuildType

plugins {
    alias(libs.plugins.convention.kmp.library.compose)
    alias(libs.plugins.convention.koin)
    // NavKeys are @Serializable so rememberNavBackStack can save/restore them across
    // process death.
    alias(libs.plugins.kotlin.serialization)
}

kotlin {
    android {
        namespace = "com.alongside.app"
    }
    // KmpLibraryPlugin (build-logic) only registers a DEBUG framework for every module - Release
    // linking was deliberately skipped project-wide (heavy on the K/N linker, App Store publishing
    // was out of scope). `app` is the one module that actually exports its .framework to Xcode
    // (OTHER_LDFLAGS/FRAMEWORK_SEARCH_PATHS in iosApp/project.yml), so it's the only one that needs
    // a RELEASE variant too - for `Product > Archive`, which always builds Release regardless of
    // App Store scope (confirmed live 2026-08-06: archiving failed with "Unable to resolve module
    // dependency: 'app'" since no Release framework existed to embed).
    listOf(iosArm64(), iosSimulatorArm64()).forEach { iosTarget ->
        iosTarget.binaries.framework(listOf(NativeBuildType.RELEASE)) {
            baseName = project.name
            isStatic = true
        }
    }
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation(projects.core.domain)
            implementation(projects.core.ui)
            implementation(projects.feature.auth)
            implementation(projects.feature.onboarding)
            implementation(projects.feature.pairing)
            implementation(projects.feature.diary)
            implementation(projects.feature.places)
            implementation(projects.feature.matcher)
            implementation(projects.feature.settings)
            implementation(projects.feature.recap)
            implementation(libs.findLibrary("navigation3-runtime").get())
            implementation(libs.findLibrary("orbit-core").get())
            implementation(libs.findLibrary("orbit-viewmodel").get())
            implementation(libs.findLibrary("orbit-compose").get())
            implementation(libs.findLibrary("kotlinx-datetime").get())
            implementation(compose.materialIconsExtended)
        }
        commonTest.dependencies {
            implementation(libs.findLibrary("orbit-test").get())
        }
        androidMain.dependencies {
            implementation(project.dependencies.platform(libs.findLibrary("firebase-bom").get()))
            implementation(libs.findLibrary("firebase-messaging").get())
            // navigation3-ui (NavDisplay with animations/predictive back) ships no iOS
            // artifacts at 1.1.0-alpha01 - Android gets the real NavDisplay, the other
            // targets fall back to AlongsideNavDisplay's plain top-entry renderer.
            implementation(libs.findLibrary("navigation3-ui").get())
        }
        // Android's equivalent DI wiring (AndroidAppModule.kt) lives in the separate `androidApp`
        // Gradle module, which declares these directly - there is no `iosApp` Gradle module
        // (Xcode isn't Gradle), so the iOS Koin bootstrap lives in this module's iosMain instead
        // (see MainViewController.kt/IosAppModule.kt), and needs these deps only here.
        iosMain.dependencies {
            implementation(projects.core.database)
            implementation(projects.core.network)
            implementation(projects.data)
        }
    }
}
