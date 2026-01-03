pluginManagement {
    repositories {
        gradlePluginPortal()
        mavenCentral()
        maven {
            url = uri("https://repo1.maven.org/maven2/")
            name = "MavenCentralMirror"
        }
    }
}

rootProject.name = "proto-wrapper"

// Modules for Gradle build
include("proto-wrapper-core")
include("proto-wrapper-gradle-plugin")

// Maven-only modules NOT included in Gradle build:
// - proto-wrapper-maven-plugin (depends on Maven Plugin API)
// - examples/maven-example

// Standalone Gradle project for integration tests
// Uses composite build for plugin resolution
includeBuild("proto-wrapper-integration-tests")

// examples/gradle-example - built separately:
//   cd examples/gradle-example && ./gradlew build
// Its settings.gradle.kts uses includeBuild("../..") for plugin resolution
