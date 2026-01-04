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
// - proto-wrapper-maven-integration-tests
// - examples/maven-example

// examples/gradle-example - built separately:
//   cd examples/gradle-example && ./gradlew build
// Its settings.gradle.kts uses includeBuild("../..") for plugin resolution
