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
    alias(libs.plugins.ksp) apply false
    alias(libs.plugins.room) apply false
    alias(libs.plugins.detekt)
}

val detektMergeSarif by tasks.registering(ReportMergeTask::class) {
    output.set(layout.buildDirectory.file("reports/detekt/merge.sarif"))
}

val versionCatalog = extensions.getByType<VersionCatalogsExtension>().named("libs")

subprojects {
    apply(plugin = "org.jlleitschuh.gradle.ktlint")
    apply(plugin = "io.gitlab.arturbosch.detekt")

    extensions.configure<KtlintExtension> {
        filter {
            exclude("**/generated/**")
        }
    }

    // KtlintExtension.filter{} has no effect on generated Compose resource
    // accessors, and mutating `source` at execution time (doFirst) trips the
    // configuration cache ("Task.project invocation ... unsupported" /
    // "cannot serialize Gradle script object references" — tried both).
    // BaseKtLintCheckTask implements PatternFilterable directly; calling
    // exclude() on the task itself at configuration time is the one approach
    // that's both correct and configuration-cache-safe.
    tasks.withType<BaseKtLintCheckTask>().configureEach {
        exclude { entry -> entry.file.path.contains("${File.separatorChar}build${File.separatorChar}") }
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
        // Same generated-Compose-resources problem as ktlint above — Detekt also
        // extends SourceTask (implements PatternFilterable directly), so the same
        // configuration-time exclude() applies and stays configuration-cache-safe.
        exclude { entry -> entry.file.path.contains("${File.separatorChar}build${File.separatorChar}") }
    }

    detektMergeSarif.configure {
        dependsOn(tasks.withType<Detekt>())
        input.from(provider { tasks.withType<Detekt>().map { it.reports.sarif.outputLocation.get() } })
    }
}
