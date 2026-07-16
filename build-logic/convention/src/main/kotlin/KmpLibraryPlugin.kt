import com.android.build.api.dsl.KotlinMultiplatformAndroidLibraryTarget
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.plugin.mpp.NativeBuildType

internal const val ALONGSIDE_COMPILE_SDK = 36
internal const val ALONGSIDE_MIN_SDK = 26

/**
 * AGP 9 removed support for combining `com.android.library`/`com.android.application`
 * with `org.jetbrains.kotlin.multiplatform` in the same module (confirmed against the
 * real, currently-released AGP 9.3.0 — CLAUDE.md/the architecture doc were written
 * assuming the old combination still worked). The replacement is the unified
 * `com.android.kotlin.multiplatform.library` plugin, which registers its DSL as a
 * runtime `ExtensionAware` extension named "android" on `KotlinMultiplatformExtension`
 * rather than a real interface member — .gradle.kts scripts get an auto-generated
 * accessor for that (`kotlin { android { ... } }` reads naturally there), but
 * precompiled plugin classes like this one don't get that codegen, so the extension
 * has to be looked up explicitly by type.
 * See https://developer.android.com/kotlin/multiplatform/plugin.
 */
class KmpLibraryPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            with(pluginManager) {
                apply("org.jetbrains.kotlin.multiplatform")
                apply("com.android.kotlin.multiplatform.library")
            }

            extensions.configure<KotlinMultiplatformExtension> {
                extensions.configure<KotlinMultiplatformAndroidLibraryTarget> {
                    compileSdk = ALONGSIDE_COMPILE_SDK
                    minSdk = ALONGSIDE_MIN_SDK
                    compilerOptions {
                        jvmTarget.set(JvmTarget.JVM_17)
                    }
                    withHostTestBuilder {}.configure {
                        isIncludeAndroidResources = true
                    }
                }

                // Every module gets a jvm() target too: core:database/core:network/data
                // need it directly for ci.yml's `:module:jvmTest` (Room in-memory /
                // Ktor MockEngine tests on a plain JVM), and anything they depend on
                // (core:model, core:domain) needs a jvm variant to publish for those
                // consumers to resolve against — Gradle variant matching is per-target,
                // not transitive-by-convenience.
                jvm()

                // iosX64 (Intel simulator) intentionally excluded: Compose Multiplatform
                // stopped publishing iosX64 artifacts for runtime/ui after the 1.11.0-alpha
                // line (verified live against Maven — only iosArm64/iosSimulatorArm64 get
                // stable 1.11.0+ releases), so declaring that target breaks dependency
                // resolution for any module pulling in Compose.
                // DEBUG-only: release framework linking is heavy (OOMs the K/N linker on
                // modules with many Compose dependencies) and isn't needed — App Store
                // publishing is explicitly out of scope per CLAUDE.md.
                listOf(iosArm64(), iosSimulatorArm64()).forEach { iosTarget ->
                    iosTarget.binaries.framework(listOf(NativeBuildType.DEBUG)) {
                        baseName = project.name
                        isStatic = true
                    }
                }

                sourceSets.commonMain.dependencies {
                    implementation(libs.findLibrary("kotlinx-coroutines-core").get())
                }
                sourceSets.commonTest.dependencies {
                    implementation(kotlin("test"))
                    implementation(libs.findLibrary("kotlinx-coroutines-test").get())
                }
            }
        }
    }
}
