import com.android.build.api.dsl.KotlinMultiplatformAndroidLibraryTarget
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
    override fun apply(target: Project) {
        with(target) {
            pluginManager.apply("io.github.takahirom.roborazzi")

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
