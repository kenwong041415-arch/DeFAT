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
        // core-domain must stay Android-free; fail the build on any android import
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
