plugins {
    alias(libs.plugins.kotlin.jvm)
}

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

kotlin {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
        allWarningsAsErrors.set(false)
    }
    sourceSets.all { languageSettings.progressiveMode = true }
}

dependencies {
    implementation(libs.kotlinx.coroutines.core)
    // Plain JVM annotation library (no Android dependency) so use cases can
    // carry @Inject constructors for Hilt — see plan §7.3.
    implementation("javax.inject:javax.inject:1")

    testImplementation(libs.junit)
    testImplementation(kotlin("test"))
    testImplementation(libs.kotlinx.coroutines.test)
}

tasks.withType<Test>().configureEach { useJUnit() }

/**
 * Enforces the module boundary from `docs/01` D1 / `docs/02`: this module is
 * the future Kotlin Multiplatform seed and must never reference the Android
 * SDK. Being a plain JVM module does not prevent it — `android.*` classes are
 * plain Java and would resolve if anyone added an Android dependency, so the
 * rule needs an actual check rather than a comment.
 */
val checkNoAndroidImports by tasks.registering {
    group = "verification"
    description = "Fails if :core-domain references android.* or androidx.*"

    val sources = fileTree("src") { include("**/*.kt") }
    inputs.files(sources).withPathSensitivity(PathSensitivity.RELATIVE)
    val marker = layout.buildDirectory.file("checkNoAndroidImports.txt")
    outputs.file(marker)

    doLast {
        val forbidden = Regex("""^\s*import\s+(android|androidx)\.""", RegexOption.MULTILINE)
        val offenders = sources.files
            .filter { forbidden.containsMatchIn(it.readText()) }
            .map { it.relativeTo(projectDir).path }

        if (offenders.isNotEmpty()) {
            throw GradleException(
                "core-domain must stay Android-free (docs/02), but these files " +
                    "import android.*/androidx.*:\n" +
                    offenders.joinToString("\n") { "  - $it" },
            )
        }
        marker.get().asFile.apply { parentFile.mkdirs() }.writeText("ok")
    }
}

tasks.named("check") { dependsOn(checkNoAndroidImports) }
tasks.named("test") { finalizedBy(checkNoAndroidImports) }
