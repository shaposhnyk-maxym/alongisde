pluginManagement {
    includeBuild("build-logic")
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}

plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "Alongside"

enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

include(":core:model")
include(":core:domain")
include(":core:database")
include(":core:network")
include(":core:ui")
include(":data")

include(":feature:auth")
include(":feature:onboarding")
include(":feature:pairing")
include(":feature:diary")
include(":feature:matcher")
include(":feature:places")
include(":feature:recap")
include(":feature:settings")

include(":app")
include(":androidApp")
include(":playground")
