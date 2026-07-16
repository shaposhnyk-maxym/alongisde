plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.compose.multiplatform)
    alias(libs.plugins.compose.compiler)
}

dependencies {
    implementation(projects.core.ui)
    implementation(compose.desktop.currentOs)
}

compose.desktop {
    application {
        mainClass = "com.alongside.playground.MainKt"
    }
}
