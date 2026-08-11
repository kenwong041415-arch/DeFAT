// Intentionally empty of plugin declarations.
//
// The Android Gradle Plugin is NOT declared here with `apply false`, because
// doing so resolves the AGP jar from Google's Maven repo during root-project
// configuration. The development container has no Android SDK and no access
// to dl.google.com, so keeping AGP out of the root build is what allows
//     gradle --configure-on-demand :core-domain:test
// to run locally. Each module declares the plugins it needs, with versions
// from gradle/libs.versions.toml.
