import com.android.build.api.dsl.KotlinMultiplatformAndroidLibraryTarget
import com.github.takahirom.roborazzi.ExperimentalRoborazziApi
import io.github.takahirom.roborazzi.RoborazziExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension

/**
 * Screenshot testing via Roborazzi + Robolectric + ComposablePreviewScanner,
 * applied to core:ui and every feature:* module per CLAUDE.md's test
 * strategy. Registers each module's `verifyRoborazziDebug` task; with zero
 * `@Preview`s (true until M4) it runs and passes trivially.
 */
class RoborazziConventionPlugin : Plugin<Project> {
    @OptIn(ExperimentalRoborazziApi::class)
    override fun apply(target: Project) {
        with(target) {
            pluginManager.apply("io.github.takahirom.roborazzi")

            // Modules with multiple Roborazzi test targets (jvm/androidHostTest/iosSimulatorArm64)
            // share one build/intermediates/roborazzi dir by default, and Roborazzi's own
            // finalize tasks can race on it under `org.gradle.parallel=true` - manifests as
            // `finalizeTestRoborazziJvm` throwing NoSuchFileException on that shared directory
            // once Gradle's build cache lets one target's tasks finish (and clean up) before
            // another target's finalize task reads it. `separateOutputDirs` gives every task its
            // own subdirectory so they stop contending. See takahirom/roborazzi's docs/FAQ on
            // concurrent Roborazzi tasks racing on a shared intermediates directory.
            extensions.configure<RoborazziExtension> {
                separateOutputDirs.set(true)
            }

            extensions.configure<KotlinMultiplatformExtension> {
                extensions.configure<KotlinMultiplatformAndroidLibraryTarget> {
                    androidResources { enable = true }
                }

                sourceSets.getByName("androidHostTest").dependencies {
                    implementation(kotlin("test"))
                    implementation(libs.findLibrary("robolectric").get())
                    implementation(libs.findLibrary("roborazzi").get())
                    implementation(libs.findLibrary("roborazzi-compose").get())
                    implementation(libs.findLibrary("roborazzi-junit-rule").get())
                    implementation(libs.findLibrary("composable-preview-scanner-android").get())
                }
            }
        }
    }
}
