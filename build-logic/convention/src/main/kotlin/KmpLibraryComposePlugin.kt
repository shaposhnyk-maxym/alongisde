import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.getByType
import org.jetbrains.compose.ComposePlugin
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension

class KmpLibraryComposePlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            pluginManager.apply("convention.kmp.library")
            pluginManager.apply("org.jetbrains.compose")
            pluginManager.apply("org.jetbrains.kotlin.plugin.compose")

            extensions.configure<KotlinMultiplatformExtension> {
                val compose = extensions.getByType<ComposePlugin.Dependencies>()
                sourceSets.commonMain.dependencies {
                    implementation(compose.runtime)
                    implementation(compose.foundation)
                    implementation(compose.material3)
                    implementation(compose.ui)
                    implementation(compose.components.resources)
                    // Real androidx.compose.ui.tooling.preview.Preview, multiplatform-ready as of
                    // Compose Multiplatform 1.11 (supersedes the now-deprecated
                    // org.jetbrains.compose.ui.tooling.preview.Preview wrapper) - also what
                    // ComposablePreviewScanner/Roborazzi's generateComposePreviewRobolectricTests
                    // scan for (see RoborazziConventionPlugin.kt).
                    implementation(libs.findLibrary("compose-ui-tooling-preview").get())
                    // AnimatedVisibility/fadeIn/slideInVertically, used by StaggerRevealColumn.
                    implementation(compose.animation)
                }

                sourceSets.getByName("androidMain").dependencies {
                    // Required for Android Studio to host and render Compose Previews; provides the
                    // actual androidx.compose.ui.tooling.ComposeViewAdapter class LayoutLib looks for.
                    implementation(libs.findLibrary("compose-ui-tooling").get())
                }
            }
        }
    }
}
