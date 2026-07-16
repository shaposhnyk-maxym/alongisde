import com.android.build.api.dsl.ApplicationExtension
import org.gradle.api.JavaVersion
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure

/**
 * AGP 9 no longer allows the Kotlin Multiplatform plugin and
 * com.android.application in the same module, so androidApp stays a thin,
 * Android-only shell that depends on the KMP `app` module for everything
 * else (see docs/kmp-module-architecture.md).
 */
class AndroidApplicationPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            with(pluginManager) {
                apply("com.android.application")
                // AGP 9's built-in Kotlin support means org.jetbrains.kotlin.android
                // is no longer applied here — only the Compose compiler plugin.
                apply("org.jetbrains.kotlin.plugin.compose")
            }

            extensions.configure<ApplicationExtension> {
                compileSdk = ALONGSIDE_COMPILE_SDK
                defaultConfig {
                    minSdk = ALONGSIDE_MIN_SDK
                    targetSdk = ALONGSIDE_COMPILE_SDK
                }
                compileOptions {
                    sourceCompatibility = JavaVersion.VERSION_17
                    targetCompatibility = JavaVersion.VERSION_17
                }
                lint {
                    // "Lint Vital" auto-runs against the release build type as part
                    // of `build`/`assemble`. App Store publishing is explicitly out
                    // of scope for now (CLAUDE.md), release builds aren't produced
                    // yet, and its whole-graph file-collection composition across
                    // 17 modules triggers a StackOverflowError in this AGP/Gradle
                    // combination — not worth chasing for a build type we don't ship.
                    checkReleaseBuilds = false
                }
            }
        }
    }
}
