import androidx.room.gradle.RoomExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.dependencies
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension

/**
 * Room 2.8.4 KMP support does not auto-wire per-target KSP configurations the
 * way the classic Android-only Room+KSP combo does (verified against the
 * official multiplatform setup guide) — each Kotlin target needs its own
 * `ksp<Target>` dependency on room-compiler, added manually below. Applied
 * alongside `convention.kmp.library` (never in place of it), same layering
 * as RoborazziConventionPlugin/KoinPlugin.
 */
class RoomPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            with(pluginManager) {
                apply("com.google.devtools.ksp")
                apply("androidx.room")
            }

            extensions.configure<KotlinMultiplatformExtension> {
                // Room's @ConstructedBy pattern (KSP-generated `actual`s) is the
                // official KMP setup, but expect/actual classes are still Beta —
                // suppress the compiler warning it triggers on every build.
                targets.configureEach {
                    compilations.configureEach {
                        compileTaskProvider.configure {
                            compilerOptions {
                                freeCompilerArgs.add("-Xexpect-actual-classes")
                            }
                        }
                    }
                }

                sourceSets.commonMain.dependencies {
                    implementation(libs.findLibrary("room-runtime").get())
                    implementation(libs.findLibrary("sqlite-bundled").get())
                }
            }

            extensions.configure<RoomExtension> {
                schemaDirectory("$projectDir/schemas")
            }

            val roomCompiler = libs.findLibrary("room-compiler").get()
            dependencies {
                add("kspAndroid", roomCompiler)
                add("kspJvm", roomCompiler)
                add("kspIosArm64", roomCompiler)
                add("kspIosSimulatorArm64", roomCompiler)
            }
        }
    }
}
