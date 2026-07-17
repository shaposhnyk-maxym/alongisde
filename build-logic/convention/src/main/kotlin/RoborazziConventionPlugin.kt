import com.android.build.api.dsl.KotlinMultiplatformAndroidLibraryTarget
import com.github.takahirom.roborazzi.ExperimentalRoborazziApi
import io.github.takahirom.roborazzi.RoborazziExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.tasks.testing.Test
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.getByType
import org.jetbrains.compose.ComposePlugin
import org.jetbrains.compose.ExperimentalComposeLibrary
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension

/**
 * Screenshot testing via Roborazzi + Robolectric + ComposablePreviewScanner,
 * applied to core:ui and every feature:* module per CLAUDE.md's test
 * strategy. Registers each module's `verifyRoborazziDebug` task; with zero
 * `@Preview`s (true until M4) it runs and passes trivially.
 */
class RoborazziConventionPlugin : Plugin<Project> {
    @OptIn(ExperimentalRoborazziApi::class, ExperimentalComposeLibrary::class)
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
            //
            // generateComposePreviewRobolectricTests auto-generates a screenshot test per
            // @Preview found under this module's own package (derived from target.path, since
            // this plugin is shared by core:ui and every feature:* module, each with a distinct
            // namespace) - no hand-written scanner/test-loop code needed anywhere.
            val previewScanPackage = "com.alongside" + target.path.replace(":", ".")
            extensions.configure<RoborazziExtension> {
                separateOutputDirs.set(true)
                // Golden images live in the repo alongside the module (CLAUDE.md test strategy),
                // not under build/ (ephemeral/gitignored, regenerated on every clean build).
                outputDir.set(layout.projectDirectory.dir("screenshots"))
                generateComposePreviewRobolectricTests {
                    enable.set(true)
                    packages.set(listOf(previewScanPackage))
                    // @Preview composables are conventionally private (dev-tool only, not public
                    // API) - the scanner ignores private previews unless told otherwise.
                    includePrivatePreviews.set(true)
                    // Goldens recorded on macOS/arm64 differ from ubuntu/x86_64 CI renders by
                    // 1-2px of Skia anti-aliasing on rounded corners and glyph edges. The custom
                    // tester wraps the default one with a comparison tolerance instead of
                    // requiring goldens to be re-recorded in CI (see the tester's KDoc for the
                    // measured numbers). Its source lives in config/roborazzi/src and is added
                    // to androidHostTest below, so every module using this plugin compiles it.
                    testerQualifiedClassName.set("com.alongside.screenshot.ToleranceComposePreviewTester")
                    // The tolerance tester delegates testParameters() to the default
                    // AndroidComposePreviewTester, which reads scanOptions (incl.
                    // includePrivatePreviews) at runtime - safe to keep them plugin-side.
                    useScanOptionParametersInTester.set(true)
                }
            }

            // AGP 9's unified KMP android-library test DSL has no `testOptions.unitTests.all {}`
            // hook (unlike classic AGP), so this is set directly on the underlying Test tasks -
            // recommended by Roborazzi itself to avoid Robolectric pixel-copy fidelity issues.
            tasks.withType(Test::class.java).configureEach {
                systemProperty("robolectric.pixelCopyRenderMode", "hardware")
            }

            extensions.configure<KotlinMultiplatformExtension> {
                extensions.configure<KotlinMultiplatformAndroidLibraryTarget> {
                    androidResources { enable = true }
                }

                val compose = extensions.getByType<ComposePlugin.Dependencies>()
                // Shared screenshot-test support (the tolerance tester) - one source file
                // compiled into each module's host tests rather than a dedicated module.
                sourceSets.getByName("androidHostTest").kotlin
                    .srcDir(rootProject.file("config/roborazzi/src"))
                sourceSets.getByName("androidHostTest").dependencies {
                    implementation(kotlin("test"))
                    // androidHostTest doesn't automatically see commonMain's `implementation`
                    // Compose dependencies (unlike androidMain) - declared explicitly here so test
                    // code can call Compose UI directly (e.g. building sample content in tests).
                    implementation(compose.foundation)
                    implementation(compose.material3)
                    implementation(compose.uiTest)
                    implementation(libs.findLibrary("compose-ui-test-junit4").get())
                    implementation(libs.findLibrary("compose-ui-test-manifest").get())
                    implementation(libs.findLibrary("junit").get())
                    implementation(libs.findLibrary("robolectric").get())
                    implementation(libs.findLibrary("roborazzi").get())
                    implementation(libs.findLibrary("roborazzi-compose").get())
                    implementation(libs.findLibrary("roborazzi-junit-rule").get())
                    implementation(libs.findLibrary("roborazzi-compose-preview-scanner-support").get())
                    implementation(libs.findLibrary("composable-preview-scanner-android").get())
                }
            }
        }
    }
}
