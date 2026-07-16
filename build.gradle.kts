import io.gitlab.arturbosch.detekt.Detekt
import io.gitlab.arturbosch.detekt.report.ReportMergeTask
import org.gradle.api.artifacts.VersionCatalogsExtension
import org.gradle.kotlin.dsl.getByType
import org.jlleitschuh.gradle.ktlint.KtlintExtension
import org.jlleitschuh.gradle.ktlint.tasks.BaseKtLintCheckTask

plugins {
    alias(libs.plugins.kotlin.multiplatform) apply false
    alias(libs.plugins.kotlin.jvm) apply false
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.kotlin.multiplatform.library) apply false
    alias(libs.plugins.compose.multiplatform) apply false
    alias(libs.plugins.compose.compiler) apply false
    alias(libs.plugins.ktlint) apply false
    alias(libs.plugins.roborazzi) apply false
    alias(libs.plugins.detekt)
}

val detektMergeSarif by tasks.registering(ReportMergeTask::class) {
    output.set(layout.buildDirectory.file("reports/detekt/merge.sarif"))
}

val versionCatalog = extensions.getByType<VersionCatalogsExtension>().named("libs")

fun isGeneratedPath(file: File): Boolean = file.path.contains("${File.separatorChar}build${File.separatorChar}")

subprojects {
    apply(plugin = "org.jlleitschuh.gradle.ktlint")
    apply(plugin = "io.gitlab.arturbosch.detekt")

    extensions.configure<KtlintExtension> {
        filter {
            exclude("**/generated/**")
        }
    }

    // KtlintExtension.filter{} has no effect on generated Compose resource
    // accessors: BaseKtLintCheckTask.source is a flat ConfigurableFileCollection
    // (not a rooted file tree), so Ant-style glob excludes never get a relative
    // path to match against. Filter the already-resolved file collection directly
    // instead — this works regardless of how the files were unioned together.
    tasks.withType<BaseKtLintCheckTask>().configureEach {
        doFirst {
            // Idempotent: re-wrapping an already-filtered FileCollection in another
            // filter{} layer on every build (daemon reuse means this configuration
            // re-runs repeatedly against the same live task objects) compounds into
            // a StackOverflowError once the wrapper chain gets deep enough. Only
            // filter if there's actually something to remove.
            if (source.any(::isGeneratedPath)) {
                setSource(source.filter { file -> !isGeneratedPath(file) })
            }
        }
    }

    dependencies {
        add("detektPlugins", versionCatalog.findLibrary("detekt-rules-compose").get())
    }

    extensions.configure<io.gitlab.arturbosch.detekt.extensions.DetektExtension> {
        config.setFrom("$rootDir/config/detekt.yml")
        buildUponDefaultConfig = true
    }

    tasks.withType<Detekt>().configureEach {
        reports {
            sarif.required.set(true)
            sarif.outputLocation.set(layout.buildDirectory.file("reports/detekt/${name}.sarif"))
        }
        finalizedBy(detektMergeSarif)
        // Same generated-Compose-resources problem as ktlint above, plus detekt's
        // own plugin re-assigns `source` from an afterEvaluate block of its own —
        // filtering in configureEach{} loses the race against that. doFirst{} runs
        // after all configuration (every afterEvaluate) has finished, so it wins.
        // The idempotency check (only filter if there's a generated path present)
        // matters even more here than for ktlint, since daemon reuse across builds
        // would otherwise compound source.filter{} wrapping into a
        // StackOverflowError once the FilteredFileCollection chain gets deep enough.
        onlyIf {
            source.any { file -> !isGeneratedPath(file) }
        }
        doFirst {
            if (source.any(::isGeneratedPath)) {
                setSource(source.filter { file -> !isGeneratedPath(file) })
            }
        }
    }

    detektMergeSarif.configure {
        dependsOn(tasks.withType<Detekt>())
        input.from(provider { tasks.withType<Detekt>().map { it.reports.sarif.outputLocation.get() } })
    }
}
