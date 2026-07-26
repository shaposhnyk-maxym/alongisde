plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.compose.multiplatform)
    alias(libs.plugins.compose.compiler)
}

dependencies {
    implementation(projects.core.ui)
    implementation(compose.desktop.currentOs)
    implementation(compose.runtime)
    implementation(compose.foundation)
    implementation(compose.material3)
    // RecapSlide.DayHighlight/ClosestMoment's `date: LocalDate` (core:model, M20.3.5 fixtures) -
    // core:model only declares kotlinx-datetime as `implementation`, so it's not on this module's
    // classpath transitively via core:ui's api(projects.core.model).
    implementation(libs.kotlinx.datetime)
}

compose.desktop {
    application {
        mainClass = "com.alongside.playground.MainKt"
    }
}
