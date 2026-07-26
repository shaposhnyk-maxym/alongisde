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
            // Recap slide composables (M20.3.5) render RecapSlide domain types directly - the
            // first core:ui component to need a domain-model dependency, per the roadmap's own
            // design (feature-agnostic content composables that core:domain's recap deck maps
            // onto 1:1, so feature:recap's RecapContainer needs no per-slide UI logic of its own).
            // `api`, not `implementation`: RecapSlide sub-types appear in these composables'
            // public signatures, so consumers (feature:recap, playground) need them on their own
            // compile classpath too, same reasoning as core:domain's own api(projects.core.model).
            api(projects.core.model)
            // RecapSlide.DayHighlight/ClosestMoment's `date: LocalDate` - same transitive gap as
            // playground's build.gradle.kts: core:model only declares kotlinx-datetime as
            // `implementation`, so referencing the type by name needs it declared here too.
            implementation(libs.findLibrary("kotlinx-datetime").get())
            implementation(libs.findLibrary("coil-compose").get())
            implementation(libs.findLibrary("coil-network-ktor3").get())
        }
    }
}
